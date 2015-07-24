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
package io.gravitee.reporter.elastic.factories;

import java.util.List;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import io.gravitee.reporter.elastic.config.Configuration;
import io.gravitee.reporter.elastic.model.TransportAddress;


public class ElasticClientFactory extends AbstractFactoryBean<Client> {

	@Autowired
	private Configuration config;

	@Override
	public Class<Client> getObjectType() {
		return Client.class;
	}
	
	@Override
	protected Client createInstance() throws Exception {
		TransportClient client = new TransportClient();

		List<TransportAddress> adresses = config.getTransportAddresses();

		for (TransportAddress address : adresses) {
			client.addTransportAddress(new InetSocketTransportAddress(address.getHostname(), address.getPort()));
		}
		return client;

	}
}