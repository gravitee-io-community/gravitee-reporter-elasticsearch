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

import io.gravitee.reporter.elastic.model.Protocol;
import io.gravitee.reporter.elastic.model.TransportAddress;

import java.util.List;

/**
 * Elasticsearch client reporter configuration.
 *  
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 *
 */
public class Configuration {
	
	/**
	 *  Client communication protocol. 
	 */
	private Protocol protocol;
	
	/**
	 * Cluster name. Used only for node protocol
	 */
	private String clusterName;
	
	/**
	 * Number of elasticsearch client workers.
	 */
	private Integer workers = 1;
	
	/**
	 * Elasticsearch hosts
	 */
	private List<TransportAddress> transportAddresses;
	

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public List<TransportAddress> getTransportAddresses() {
		return transportAddresses;
	}

	public void setTransportAddresses(List<TransportAddress> addresses) {
		this.transportAddresses = addresses;
	}

	public Integer getWorkers() {
		return workers;
	}

	public void setWorkers(Integer workers) {
		this.workers = workers;
	}
	
}
