package com.example.sample.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Host class represents a Node
 * serviceName can be "my-company"
 * IP can be 10.0.0.1
 * port can be 9090
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Host {
    String serviceName;
    String serviceIP;
    String port;
}
