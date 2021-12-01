package com.example.sample.reverseProxy;

import com.example.sample.model.Host;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/*
*  Interface of ReverseProxy which is implemented by RandomForwardReverseProxy and RoundRobinReverseProxy classes
*
* runReverseProxy() takes in Host(upstream-server) to be called and returns ResponseEntity which contains response sent from Host(upstream-server)
*
* @Cacheable("CachedSites") CachedSites is ConcurrentMapCache which is maintained by ConcurrentMapCacheManager
* It can be extended by including eviction policy.
*
 */
public interface ReverseProxy {
    Host getHost(HttpServletRequest service);

    @Cacheable("CachedSites")
    ResponseEntity runReverseProxy(Host host) throws IOException;

}
