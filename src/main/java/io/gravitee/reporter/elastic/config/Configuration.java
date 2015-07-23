/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.reporter.elastic.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import io.gravitee.reporter.elastic.model.Protocol;
import io.gravitee.reporter.elastic.model.TransportAddress;

/**
 * Elasticsearch client reporter configuration.
 *  
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 *
 */
@org.springframework.context.annotation.Configuration
public class Configuration {
	
	private static final String HOSTS_SEPARATOR = ",";

	private static final String PORT_SEPARATOR = ":";


	/**
	 *  Client communication protocol. 
	 */
	@Value("${elastic.protocol:NODE}")
	private Protocol protocol;
	
	/**
	 * Cluster name. Used only for node protocol
	 */
	@Value("${elastic.cluster.name:gravitee}")
	private String clusterName;
	
	@Value("${elastic.bulk.actions:1000}")
	private Integer bulkActions;
	
	@Value("${elastic.bulk.size:5}")	
	private Long bulkSize;
	
	@Value("${elastic.bulk.flush.interval:1}")		
	private Long flushInterval;
	
	@Value("${elastic.bulk.concurrent.request:5}")		
	private Integer concurrentRequests;

	@Value("${elastic.hosts:localhost}")		
	private String hosts;
	
	/**
	 * Elasticsearch hosts
	 */
	private List<TransportAddress> transportAddresses;
	

	public Protocol getProtocol() {
		return protocol;
	}


	public String getClusterName() {
		return clusterName;
	}

	public List<TransportAddress> getTransportAddresses() {
		if(transportAddresses == null){
			transportAddresses = unmarshallHosts(this.hosts);
		}
		return transportAddresses;
	}

	public Integer getBulkActions() {
		return bulkActions;
	}


	public Long getBulkSize() {
		return bulkSize;
	}


	public Long getFlushInterval() {
		return flushInterval;
	}


	public Integer getConcurrentRequests() {
		return concurrentRequests;
	}


	/**
	 * Unmarshall hostes under the format "hostname1:port1, hostname2"
	 * 
	 * @param serializedHosts
	 * 			Serialized transport addresses
	 * @param defaultPort
	 * @return
	 */
	private List<TransportAddress> unmarshallHosts(String serializedHosts) {

		List<TransportAddress> hosts = new ArrayList<TransportAddress>();
		String[] hostsParts = serializedHosts.split(HOSTS_SEPARATOR);

		for (String serializedHost : hostsParts) {

			TransportAddress host = null;

			if (serializedHost.contains(PORT_SEPARATOR)) {
				String[] hostParts = serializedHost.split(PORT_SEPARATOR);
				
				String hostname = hostParts[0].toLowerCase();
				Integer port = Integer.parseInt(hostParts[1].trim());
				
				host = new TransportAddress(hostname, port);
			} else {
				host = new TransportAddress(serializedHost.trim(), protocol.getDefaultPort());
			}
			hosts.add(host);
		}
		return hosts;

	}

}
