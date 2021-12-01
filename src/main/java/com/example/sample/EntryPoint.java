package com.example.sample;

import com.example.sample.model.Host;
import com.example.sample.reverseProxy.ReverseProxy;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/*
 * RestController is to map all GET/POST requests to all apis available. In this project one api is included which is "/my-service/my-company"
 * for the sake of simplicity only GET method is considered. It can be extended to POST/DELETE/PATCH requests as well.
 *
 * HttpServletRequest is used for all header fields such as content-type, request_uri, method-type(GET/POST etc.), HTTP scheme such as according to project
 * requirement HTTP/1.1 is supported.
 *
 * proxyType QueryParam is kept to choose reverse-proxy strategy either to be roundRobin or random
 *
 */
@Log4j2
@RestController
public class EntryPoint {

    private ReverseProxy roundRobinReverseProxy;
    private ReverseProxy randomReverseProxy;

    @Autowired
    public EntryPoint(@Qualifier("roundRobinReverseProxy") ReverseProxy roundRobinReverseProxy,
        @Qualifier("randomForwardReverseProxy") ReverseProxy randomReverseProxy){
        this.roundRobinReverseProxy = roundRobinReverseProxy;
        this.randomReverseProxy = randomReverseProxy;
    }
    @GetMapping(value="/my-reverse-proxy-service")
    public ResponseEntity getServiceResponse(HttpServletRequest headers, @RequestParam String proxyType) throws IOException {
        log.info("inside getStatus method");
        if(proxyType.equals("roundRobin")) {
            Host host =  this.roundRobinReverseProxy.getHost(headers);
            return this.roundRobinReverseProxy.runReverseProxy(host);
        } else if(proxyType.equals("random")) {
            Host host =  this.randomReverseProxy.getHost(headers);
            return this.randomReverseProxy.runReverseProxy(host);
        } else
            return new ResponseEntity(HttpStatus.OK);

    }

    @RequestMapping(value="/",method= RequestMethod.GET)
    public ResponseEntity getService(HttpServletRequest headers) throws IOException {
        log.info("inside getStatus method");
        String success = "Hello!!";
        return new ResponseEntity(success, HttpStatus.OK);
    }
}
