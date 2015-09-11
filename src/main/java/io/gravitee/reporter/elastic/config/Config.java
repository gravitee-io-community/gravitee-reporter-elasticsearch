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
import java.util.Properties;

import io.gravitee.reporter.elastic.model.HostAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import io.gravitee.reporter.elastic.model.Protocol;

/**
 * Elasticsearch client reporter configuration.
 *  
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 *
 */
public class Config {
	
	private static final String PORT_SEPARATOR = ":";

	@Autowired @Qualifier("graviteeProperties")
	private Properties properties;
	
	/**
	 *  Client communication protocol. 
	 */
	@Value("${elastic.protocol:TRANSPORT}")
	private Protocol protocol;
	
	/**
	 * Cluster name. Used only for node protocol
	 */
	@Value("${elastic.cluster.name:elasticsearch}")
	private String clusterName;
	
	/**
	 * Prefix index name. 
	 */
	@Value("${elastic.index.name:gravitee}")
	private String indexName;
	
	/**
	 * Prefix index name. 
	 */
	@Value("${elastic.type.name:request}")
	private String typeName;	
	
	/**
	 * Request actions max by bulk 
	 */
	@Value("${elastic.bulk.actions:1000}")
	private Integer bulkActions;
	
	/**
	 * Bulk size in Mo
	 */
	@Value("${elastic.bulk.size:5}")	
	private Long bulkSize;
	
	/**
	 * Bulk flush interval in seconds
	 */
	@Value("${elastic.bulk.flush_interval:1}")		
	private Long flushInterval;
	
	/**
	 * Accepted concurrent request
	 */
	@Value("${elastic.bulk.concurrent_requests:5}")		
	private Integer concurrentRequests ;

	
	/**
	 * Elasticsearch hosts
	 */
	private List<HostAddress> hostsAddresses;
	
	private List<String> hostsUrls;
	

	public Protocol getProtocol() {
		return protocol;
	}

	public String getClusterName() {
		return clusterName;
	}

	public List<HostAddress> getHostsAddresses() {
		if(hostsAddresses == null){
			hostsAddresses = initializeHostsAddresses();
		}
		return hostsAddresses;
	}
	
	public List<String> getHostsUrls() {
		if(hostsUrls == null){
			hostsUrls = initializeHostsUrls();
		}
		return hostsUrls;
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

	public String getIndexName() {
		return indexName;
	}


	public String getTypeName() {
		return typeName;
	}
	
	
	private List<HostAddress> initializeHostsAddresses(){
		
		String key = String.format("elastic.hosts[%s]", 0);
		List<HostAddress> res = new ArrayList<>();
		
		while (properties.containsKey(key)) {
			String serializedHost = properties.getProperty(key);
			
			if (serializedHost.contains(PORT_SEPARATOR)) {
				String[] hostParts = serializedHost.split(PORT_SEPARATOR);
				
				String hostname = hostParts[0].toLowerCase();
				Integer port = Integer.parseInt(hostParts[1].trim());
				
				res.add(new HostAddress(hostname, port));
			} else {
				res.add(new HostAddress(serializedHost.trim(), protocol.getDefaultPort()));
			}
			
			key = String.format("elastic.hosts[%s]", res.size());
		}
		
		// Use default host if required
		if(res.isEmpty()){
			res.add(new HostAddress("localhost", protocol.getDefaultPort()));
		}
		return res;
	}

	
	public List<String> initializeHostsUrls() {
		
		String key = String.format("elastic.hosts[%s]", 0);
		List<String> res = new ArrayList<>();
		
		while (properties.containsKey(key)) {
			res.add(properties.getProperty(key));
			key = String.format("elastic.hosts[%s]", res.size());
		}
		
		// Use default host if required
		if(res.isEmpty()){
			res.add("http://localhost:9200/");
		}
		return res;
	}
}
