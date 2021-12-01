package com.example.sample.reverseProxy;

import com.example.sample.loadBalance.RandomForward;
import com.example.sample.model.Host;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/*
 * RandomForwardReverseProxy class is used to forward request to available nodes in random manner
 *
 * this class also takes care of server-intermediary errors such as 503 service unavailable 504 gateway timeout.
 * As these error could mean serious problem with service-in-question Hence retries are limited to 3 times.
 * We can go on extending retures to all 5** errors.
 *
 * Multi-Threading can lead to more number of retries than intended. We can make use of AtomicInteger retry = new AtomicInteger(0);
 *
 * once connection is established, inputStream is received from service-node.
 *
 * Cache-Control is set to for 60 seconds. It can depend from scenario to scenario. Some services have frequent write/update operation
 * In such cases cache-control max-age can be reduced.
 *
 * JSON encoding is done using ISO_8859_1 which covers latin
 *
 */

@Log4j2
@Component
public class RandomForwardReverseProxy implements ReverseProxy {
    private static final Integer NUMBER_OF_RETRIES = 3;
    private RandomForward randomForward;

    @Autowired
    public RandomForwardReverseProxy(RandomForward randomForward){
        this.randomForward = randomForward;
    }

    @Override
    public Host getHost(HttpServletRequest request) {
        //InputStreamResource inputStreamResource = null;
        String serviceName = request.getRequestURI().split("/")[1];
        log.info("inside randomForwardReverseProxy {}", serviceName);
        Host hostPortSelectedRandomly = null;
        while(hostPortSelectedRandomly == null) {
            hostPortSelectedRandomly = randomForward.getRandomlySelectedHost(serviceName);
        }
        return hostPortSelectedRandomly;
    }

    @Override
    public  ResponseEntity runReverseProxy(Host host) throws IOException {
        HttpURLConnection httpConnection = null;
        Host hostPortSelectedRandomly = host;
        String serviceName = hostPortSelectedRandomly.getServiceName();
        URL url = null;
        HttpStatus responseCode = HttpStatus.OK;
        HttpHeaders httpHeaders = new HttpHeaders();
        //retry will call service 3 times in case if service is throwing HTTP_STATUS_CODE = 500
        Integer retry = 0;
        //this logic is to check if a particular node is temp unavilable (50* status) or has been called upto threshold limit
        while(retry < NUMBER_OF_RETRIES) {
            try {

                log.info("hostPortSelectedRandomly {}", hostPortSelectedRandomly);
                url = new URL("http://" + hostPortSelectedRandomly.getServiceIP()
                    + ":" + hostPortSelectedRandomly.getPort() + "/" + serviceName);
                log.info("URL of the service {}", serviceName);

                httpConnection = (HttpURLConnection) url.openConnection();
                httpConnection.setRequestMethod("GET");
                httpConnection.setRequestProperty("Accept", "application/json");
                HttpURLConnection.setFollowRedirects(true);
                httpConnection.setDoOutput(true);
                httpConnection.setConnectTimeout(100);
                httpConnection.connect();
                log.info("httpConnection for service {} is made {}",serviceName, httpConnection);

                retry++;
                log.info("retry count {}", retry);

                //get input stream from service my-company, check for any intermediate server error such as 503 service unavailable
                // 504 gateway timeout
                InputStream inputStream = httpConnection.getInputStream();
                InputStream error = httpConnection.getErrorStream();
                if(error != null && httpConnection.getResponseCode() == 504 || httpConnection.getResponseCode() == 503) {
                    responseCode = HttpStatus.INTERNAL_SERVER_ERROR;
                    continue;
                } else {
                    String response = IOUtils.toString(inputStream);
                    byte[] bytes = response.getBytes(ISO_8859_1);
                    String encodedJsonStr = new String(bytes, UTF_8);
                    httpHeaders.setContentLength(encodedJsonStr.getBytes().length);
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
                    httpHeaders.setCacheControl(CacheControl.maxAge(Duration.ofSeconds(60)));
                    responseCode = HttpStatus.OK;
                    return new ResponseEntity(encodedJsonStr.getBytes(), httpHeaders, responseCode);
                }
            } catch (Exception e) {
                log.error("error while talking to service {} failing after retrying {} times", serviceName, retry);
                throw new IOException(e);
            }
        }
        return new ResponseEntity(responseCode);
    }
}
