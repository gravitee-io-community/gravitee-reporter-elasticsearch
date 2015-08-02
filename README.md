[![Build Status](http://build.gravitee.io/jenkins/buildStatus/icon?job=gravitee-reporter-es)](http://build.gravitee.io/jenkins/job/gravitee-reporter-es/)

# gravitee-reporter-es

Report GraviteeIO Gateway request events to Elasticsearch Engine


## Build

This plugin require :  

* Maven 3
* JDK 8

Once built, a plugin archive file is generated in : target/gravitee-reporter-es-1.0.0-SNAPSHOT.zip


## Deploy

Just unzip the plugin archive in your gravitee plugin workspace ( default is : ${node.home}/plugins )


## Configuration 

The configuration is loaded from the common GraviteeIO Gateway configuration file (gravitee.yml)


Example : 

```YAML
elastic:
  protocol: TRANSPORT

  index:
    name: gravitee         # index name user (format used : ${name}-yyyy.MM.dd )

  type:                    # Object request type
    name: request         

  hosts: 
    - 'localhost'

  cluster:
    name: elasticsearch

  bulk:
    actions: 1000           # Number of requests action before flush
    size: 5                 # Size in Mo
    flush_interval: 1       # Flush interval in seconds
    concurrent_requests: 5  # Concurrent requests
```