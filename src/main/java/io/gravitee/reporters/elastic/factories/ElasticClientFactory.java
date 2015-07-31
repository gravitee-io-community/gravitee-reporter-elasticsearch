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
package io.gravitee.reporters.elastic.factories;

import java.util.List;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import io.gravitee.reporters.elastic.config.Config;
import io.gravitee.reporters.elastic.model.Protocol;
import io.gravitee.reporters.elastic.model.TransportAddress;


public class ElasticClientFactory extends AbstractFactoryBean<Client> {

	@Autowired
	private Config config;

	@Override
	public Class<Client> getObjectType() {
		return Client.class;
	}
	
	@Override
	protected Client createInstance() throws Exception {
		
		
		if(Protocol.TRANSPORT.equals(config.getProtocol())){
			
			Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", config.getClusterName()).build();
			
			TransportClient transportClient = new TransportClient(settings);
			
			List<TransportAddress> adresses = config.getTransportAddresses();
	
			for (TransportAddress address : adresses) {
				transportClient.addTransportAddress(new InetSocketTransportAddress(address.getHostname(), address.getPort()));
			}
			return transportClient;
			
		}else if (Protocol.NODE.equals(config.getProtocol())){
			
			Settings settings = ImmutableSettings.settingsBuilder()
					.put("cluster.name", config.getClusterName())
					.put("gateway.type","none")
				   // .put("index.number_of_shards",numberOfShards)
				   // .put("index.number_of_replicas",0)
					.build();
			
			
			Node node = NodeBuilder.nodeBuilder().settings(settings).client(true).node();
			return node.client();
		}
		
		throw new IllegalStateException(String.format("Unupported protocol [%s] for ElasticClient", config.getProtocol()));
	}
}