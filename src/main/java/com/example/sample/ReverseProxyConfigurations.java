package com.example.sample;

import com.example.sample.model.Host;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/*
 *
 * ReverseProxyConfigurations is a Configuration class containing beans of singleton scope
 *
 * hostList bean - reads values from application.properties and maintain an immutable list of hosts available
 *
 * randomGenerator - return a singleton instance of Random() to be used in RandomForward.java to select a node/host randomly
 *
 * roundRobinIterator - creates a map of serviceName and Iterables.cycle(List<Host>)
 * ServiceName1 -> Host(ServiceName1, 127.0.0.1, 9090), Host(ServiceName2, 127.0.0.1, 9000), Host(ServiceName3, 127.0.0.1, 8000)
 * ServiceName2 -> HostX, HostY, HostZ
 *
 * For in-memory caching purposes to save on latency spring CacheManager is used which maintains a cache of "CachedSites"
 *
 */
@Configuration
@Log4j2
public class ReverseProxyConfigurations {

    private ApplicationContext context;

    @Autowired
    public ReverseProxyConfigurations(ApplicationContext context){
        this.context = context;
    }


    @Bean(name ="hostList")
    @Scope("singleton")
    public List<Host> getHostList() throws IOException {
        List<Host> hostList = new ArrayList<>();
        try {
            Properties properties = new Properties();
            properties.load(DemoApplication.class.getResourceAsStream("/application.properties"));
            properties.entrySet();
            List<String> ports = Arrays.asList(properties.get("service.hostPort").toString().split(","));
            for(String port: ports) {
                Host host = Host.builder().serviceName(properties.get("service").toString())
                    .serviceIP(properties.get("service.host").toString()).port(port).build();
                hostList.add(host);
            }
            log.info("all hosts added from application.properties to hostList", hostList);
        } catch(IOException e) {
            log.error("error while loading application.properties", e);
            throw new IOException(e);
        }
        return ImmutableList.copyOf(hostList);
    }

    @Bean(name ="randomGenerator")
    @Scope("singleton")
    public Random getRandomGenerator() {
        return new Random();
    }

    @Bean(name ="roundRobinIterator")
    @Scope("singleton")
    public ImmutableMap getRoundRobinIteratorMap() {
        List<Host> hostList = (List<Host>) context.getBean("hostList");
        Map<String, List<Host>> map = hostList.stream().collect(Collectors.groupingBy(Host::getServiceName));
        Map<String, Iterator<Host>> iteratorImmutableMap = map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e->Iterables.cycle(e.getValue()).iterator()));
        return ImmutableMap.copyOf(iteratorImmutableMap);
    }

    @Bean(name="cache")
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("CachedSites");
    }

}

