package com.example.sample.loadBalance;

import com.example.sample.model.Host;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * RoundRobin class is used to select a node to forward request to in round-robin manner
 *
 * For this purpose Google's Iterables.cycle() is used which is made available by generating a singleton bean "roundRobinIterator".
 * This "roundRobinIteratorMap" contains entry of
 * Service1 -> all Iterables.cycle(List<Host>)
 * Service2 -> all Iterables.cycle(List<Host>)
 * Service3 -> all Iterables.cycle(List<Host>)
 *
 * It selects node/host from a "cycle" of nodes/hosts in round-robin manner.
 *
 * Multiset is also used to maintain count of number of times a service/node has been called. Host class represents a node here.
 * This can be seen as an effort to keep track of number of requests being sent to each node available
 *
 * Once threshold limit for a server-node is reached next node in Iterables.cycle(List<Host>).next() is selected to forward request to.
 *
 * In cases where all nodes have reached threshold traffic/requests. allServerBusy() method is used for this purpose.
 * Error is thrown. This is where scaling can shine. We can spin up new nodes/instances in such situations.
 *
 */
@Log4j2
@Component
public class RoundRobin {
    //THRESHOLD_COUNT_OF_CALLING_A_SERVICE can specify max amount of traffic a node/host can serve.
    private static final Integer THRESHOLD_COUNT_OF_CALLING_A_SERVICE = 10;
    private ApplicationContext context;
    private List<Host> hostList;
    private Map<String, Iterator<Host>> roundRobinCyclingIteratorMap;
    private Multiset<Host> counter;

    public RoundRobin(@Autowired ApplicationContext context){
        this.counter = HashMultiset.create();
        this.context = context;
        this.hostList = (List<Host>) context.getBean("hostList");
        this.roundRobinCyclingIteratorMap = (Map<String, Iterator<Host>>) context.getBean("roundRobinIterator");
    }

    public Host getHostUsingRoundRobin(final String serviceName) {
        Host nextServicePort = this.roundRobinCyclingIteratorMap.get(serviceName).next();
        log.info("nextServicePort using RoundRobin {}", nextServicePort);
        if (getNodeCallCountSoFar(nextServicePort) <= THRESHOLD_COUNT_OF_CALLING_A_SERVICE) {
            this.counter.add(nextServicePort);
        } else {
            if(allServerBusy(serviceName)) {
                log.info("All Servers are busy!!!!");
                throw new RuntimeException("All Servers are busy!! Scale-up!");
            }
            return null;
        }
        return nextServicePort;
    }

    private int getNodeCallCountSoFar(final Host hostSelectedUsingRB) {
        return this.counter.count(hostSelectedUsingRB);
    }

    //This will check if all nodes in a cluster have reached their threshold limit.
    private Boolean allServerBusy(String serviceName){
        List<Host> hostsAvailable = this.hostList.stream().filter(host->host.getServiceName().equals(serviceName)).collect(Collectors.toList());
        for(Host host: hostsAvailable){
            if(!this.counter.contains(host))
                return false;
        }
        return true;
    }
}
