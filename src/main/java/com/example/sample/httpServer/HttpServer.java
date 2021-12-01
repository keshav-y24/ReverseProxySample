package com.example.sample.httpServer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static java.net.HttpURLConnection.HTTP_OK;

/*
 * HttpServer class represents simple Http-Servers/Services running in a cluster
 *  which reverseProxy is calling to get a Json response.
 * No Json encoding is done here as that is left upon ReverseProxy to encode JSON using UTF_8
 */

@Log4j2
@Component
public class HttpServer implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if(exchange.getRequestMethod().equals("GET")) {
            Integer port = exchange.getLocalAddress().getPort();
            String JSON_RESPONSE = "{\"service\": \"my-company\", \"health\":\"UP\", \"port\":" + port + "}";

            OutputStream outputStream = exchange.getResponseBody();
            log.info("Hello  from server with port " + port);
            exchange.sendResponseHeaders(HTTP_OK, JSON_RESPONSE.getBytes().length);
            exchange.getResponseHeaders().set("Content-Type", String.format("application/json"));
            outputStream.write(JSON_RESPONSE.getBytes());
            outputStream.flush();
            outputStream.close();
        }
        else if(exchange.getRequestMethod().equals("POST")){
            //process POST methods here..
        }
    }

    public void run(String service, String serviceIP, String hostPort) {
        try {
            final com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(serviceIP, Integer.valueOf(hostPort)), 0);
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
            server.createContext("/"+ service, new HttpServer());
            server.setExecutor(threadPoolExecutor);
            server.start();
            log.info("HttpServer started for service {} on port {}", service, hostPort);
        } catch(IOException e) {
            log.error("error running server..", e);
            throw new RuntimeException(e);
        }
    }
}
