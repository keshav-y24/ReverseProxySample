package com.example.sample.loadBalance;

import com.example.sample.model.Host;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/*
 * RandomForward class is used to select a node to forward request to in random manner
 *
 * For this purpose randomGenerator singleton bean is used to contains an instance of Random java library
 *
 * Multiset is also used to keep count of number of times a service/node has been called. Host class represents a node here.
 * This can be seen as an effort to keep track of number of requests being sent to each node available
 *
 * Once threshold limit for a server-node is reached next node is selected randomly
 *
 * In cases where all nodes have reached threshold traffic/requests. allServerBusy() method is used for this purpose.
 * Error is thrown. This is where scaling can shine. We can spin up new nodes/instances in such situations.
 *
 */

@Log4j2
@Component
public class RandomForward {
    private static final Integer THRESHOLD_COUNT_OF_CALLING_A_SERVICE = 3;
    private ApplicationContext context;
    private Random randomGenerator;
    private List<Host> hostList;
    private Multiset<Host> counter;

    @Autowired
    public RandomForward(ApplicationContext context){
        this.counter = HashMultiset.create();
        this.context = context;
        this.randomGenerator = (Random) context.getBean("randomGenerator");
        this.hostList = ( List<Host>) context.getBean("hostList");
    }

    public Host getRandomlySelectedHost(final String serviceName){
        //extract List<Host> from globalList of Hosts where serviceName is "my-company"
        final List<Host> hostListOfService = this.hostList.stream().collect(Collectors.groupingBy(Host::getServiceName)).get(serviceName);
        final Integer index = Math.abs(this.randomGenerator.nextInt()) % hostListOfService.size();
        Host hostRandomlySelected = hostListOfService.get(index);
        log.info("nextServicePort using RandomForward {}", hostRandomlySelected);

        if (getNodeCallCountSoFar(hostRandomlySelected) <= THRESHOLD_COUNT_OF_CALLING_A_SERVICE) {
            this.counter.add(hostRandomlySelected);
        } else {
            if(allServerBusy(serviceName)) {
                log.info("All Servers are busy!!!!");
                throw new RuntimeException("All Servers are busy!! Scale-up!");
            }
            return null;
        }
        return hostRandomlySelected;
    }

    public int getNodeCallCountSoFar(final Host hostRandomlySelected) {
        return this.counter.count(hostRandomlySelected);
    }

    private Boolean allServerBusy(String serviceName){
        List<Host> hostsAvailable = this.hostList.stream().filter(host->host.getServiceName().equals(serviceName)).collect(Collectors.toList());
        for(Host host: hostsAvailable){
            if(!this.counter.contains(host))
                return false;
        }
        return true;
    }

}
