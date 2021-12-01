# Getting Started

## Project Overview
### Design 
* ReverseProxy is a spring-boot java app with Spring Boot Actuator for Metrics/Monitoring and Production-ready features. 
It supports ` HTTP/1.1`. Logging is done using Log4j2(lombok library)
* Modular structure is maintained and classes are written according to single responsibility principle 
except some beans which are made singleton to be used across these classes.
#### Conventions 
    URL paths are kept according exercise specifications "my-reverse-proxy-service" where my-reverse-proxy-service is exposed through several ports such as 9090, 9000
    DemoApplication is the main class.
- ReverseProxy runs on the tomcat and for the sake of testing upstream-servers(hostsList) are HttpServer which are started when tomcat is starting.
  These upstream-hosts are implementing  `HttpServer implements HttpHandler` to handle HTTP-requests according to their RequestMethodType `GET`, `POST` etc.
  
- DemoApplication class is the main class of the ReverseProxy
   * this class starts all nodes/hosts by fetching all available nodes using ApplicationContext
   * ApplicationContext is used to get singleton bean of hostsList
- ReverseProxyConfigurations is a Configuration class containing beans of singleton scope
   * hostList(upstream-servers list) bean - reads values from application.properties and maintain an immutable list of hosts available
   * randomGenerator - return a singleton instance of Random() to be used in RandomForward.java to select a node/host randomly
   * roundRobinIterator - creates a map of serviceName and Iterables.cycle(List<Host>)
   * `ServiceName1 -> Host(ServiceName1, 127.0.0.1, 9090), Host(ServiceName2, 127.0.0.1, 9000), Host(ServiceName3, 127.0.0.1, 8000)`
   * For in-memory caching purposes to save on latency spring CacheManager is used which maintains a cache of "CachedSites"
- EntryPoint is a RestControllerto map all GET/POST requests to all apis available. In this project one api is included which is "/my-reverse-proxy-service"
    * for the sake of simplicity only GET method is considered. It can be extended to POST/DELETE/PATCH requests as well.
    * HttpServletRequest is used for all header fields such as content-type, request_uri, method-type(GET/POST etc.), HTTP scheme such as according to project
    * requirement HTTP/1.1 is supported.
    * proxyType QueryParam is kept to choose reverse-proxy strategy either to be roundRobin or random
    `NOTICE- instead of a controller we can also have a HttpServer to do the job and separate requests coming on GET/PUT/POST/DELTE/PATCH basis or a ServerSocket localReverseProxy = new ServerSocket(LOCAL_PORT)`
    * @Qualifier is used to inject right instance of child-class of ReverseProxy interface which is implemented by two children `RandomForwardReverseProxy` and `RoundRobinReverseProxy`
- ReverseProxy Interface is implemented by RandomForwardReverseProxy and RoundRobinReverseProxy classes
    * runReverseProxy() takes in Host(upstream-server) to be called and returns ResponseEntity which contains response sent from Host(upstream-server)
    * @Cacheable("CachedSites") CachedSites is ConcurrentMapCache which is maintained by ConcurrentMapCacheManager
    * It can be extended by including eviction policy.
- RandomForwardReverseProxy class forwards request to available hosts in random manner
    * this class also takes care of server-intermediary errors such as 503 service unavailable 504 gateway timeout.
    * As these errors could mean serious problems with service-in-question Hence retries are limited to 3 times.
    * We can go on extending retries to all 5** errors.
    * Multi-Threading can lead to more numbers of retries than intended. We can make use of AtomicInteger retry = new AtomicInteger(0);
    * once connection establishes, inputStream is received from host.
    * Cache-Control is set to for 60 seconds. It depends from scenario to scenario. Some services have frequent write/update operation.
      In such cases cache-control max-age can be reduced.
    * JSON encoding is done using UTF_8 
- RoundRobinReverseProxy class forwards request to available hosts in round robin manner.
- RandomForward class is used to select a node to forward request to in random manner
   * For this purpose randomGenerator singleton bean is used to contain an instance of `Random` java library
   * Multiset is also used to keep count of number of times a service/node has been called. Host class represents a node here.
   * This can be seen as an effort to keep track of number of requests being sent to each node available
   * Once threshold limit for a server-node is reached next node is selected randomly
   * In cases where all nodes have reached threshold traffic/requests. allServerBusy() method is used for this purpose.
     Error is thrown. This is where **scaling** can shine. We can spin up new nodes/instances in such situations.
- RoundRobin class is used to select a node to forward request to in round-robin manner
   * For this purpose Google's `Iterables.cycle()` is used which is made available by generating a singleton bean "roundRobinIterator".
   * This "roundRobinIteratorMap" contains entry of
   ` Service1 -> all Iterables.cycle(List<Host>)`
    `Service2 -> all Iterables.cycle(List<Host>)`
    `Service3 -> all Iterables.cycle(List<Host>)`
   * It selects node/host from a "cycle" of nodes/hosts in round-robin manner.
   * `Multiset` is also used to maintain count of number of times a service/node has been called. Host class represents a node here.
   * This can be seen as an effort to keep track of number of requests being sent to each node available
   * Once threshold limit for a server-node is reached next node in `Iterables.cycle(List<Host>).next()` is selected to forward request to.
   * In cases where all nodes have reached threshold traffic/requests. allServerBusy() method is used for this purpose.
     Error is thrown. This is where scaling can shine. We can spin up new nodes/instances in such situations.

#### ReverseProxy covers 
1. Exercise 1 - Implementation 
   - ReverseProxy is implemented using JAVA 7 and 8
   - It randomly forwards the requests to service-nodes 
   - It also forwards requests using Round-Robin which is optional-extension as per requirements
   - In-memory cache is also implemented which caches requests on basis of Host(serviceIP and port)
   
2. Exercise 3 - Monitoring
   SLI(Service Level Indicators). Below is a list of SLIs which are crucial in reliability, performance and scalability. 
    - Health metric which states whether reverseProxy is available to serve requests.
    - ResponseTime. Time taken by reverseProxy to forward request to available server and send response back to client.
      which can further be subdivided to see time taken by reverseProxy and by upstream servers to which request is forwarded.
      Latency metric can provide good insight as when traffic grows latency should be minimal. Servers can scale out to serve increased traffic. 
    - Traffic/Active-Connections. It is crucial for resource estimation if there is spike in traffic(resources needed can be added. 
      reverseProxy server can be **scaled**) or active connections or traffic has reduced.
    - Status - HTTP_STATUS such as 500/404/200 ca provide good overview on Service Health.
    - Requested URI along with STATUS - gives us information if application code is erroneous (HTTP_500 errors)
      or any page is missing (HTTP_404) may be due to bad routing etc. 
    - Number of Cache-HIT and MISS. Cache is used to improve on Latency if cache Miss is more than cache Hit. 
      there might be some problem with how cache is queried. 
      
- Spring Boot Actuator for Metrics/Monitoring
    above mentioned `Health`,  `ResponseTime`, `Status`,  `Requested URI along with STATUS` are provided using actuator endpoint. 
    - `http://localhost:8080/actuator/health/` whic gives info on 
    `{"status":"UP","components":{"diskSpace":{"status":"UP","details":{"total":499963174912,"free":327943409664,"threshold":10485760,"exists":true}},"ping":{"status":"UP"}},"groups":["custom"]}`
    - `http://localhost:8080/actuator/metrics/http.server.requests` gives info on all requests with their status and on max-time-taken to average time taken.
      `http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/my-reverse-proxy-service` to get more info on specific end-point.
      ``{"name":"http.server.requests","description":null,"baseUnit":"seconds","measurements":[{"statistic":"COUNT","value":22.0},{"statistic":"TOTAL_TIME","value":0.171976029},{"statistic":"MAX","value":0.0}],"availableTags":[{"tag":"exception","values":["None","MissingServletRequestParameterException","RuntimeException"]},{"tag":"method","values":["GET"]},{"tag":"outcome","values":["CLIENT_ERROR","SUCCESS","SERVER_ERROR"]},{"tag":"status","values":["400","500","200"]}]}``
    It covers  ->
    * Total number of requests processed, 
    * Total number of requests resulted in an OK response
    * Total number of requests resulted in a 4xx response
    * Total number of requests resulted in a 5xx response
    * Average response time of all requests
    * Max response time of all requests
    
    
    
## BUILD 
- `mvn clean install` for building the project
- ` mvn spring-boot:run` for running tomcat server
- `http://localhost:8080/my-reverse-proxy-service?proxyType=random` to call reverseProxy. 
where proxyType can be `random` or `roundRobin` depending on load-balancing strategy
- `curl -X POST http://localhost:8080/actuator/shutdown` for shutting down server gracefully. 



### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.4.3/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/2.4.3/maven-plugin/reference/html/#build-image)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/2.4.3/reference/htmlsingle/#production-ready)

### Guides
The following guides illustrate how to use some features concretely:
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)

