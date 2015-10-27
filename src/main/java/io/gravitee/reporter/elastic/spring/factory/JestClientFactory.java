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
package io.gravitee.reporter.elastic.spring.factory;

import io.gravitee.reporter.elastic.config.ElasticConfiguration;
import io.gravitee.reporter.elastic.model.Protocol;
import io.searchbox.client.JestClient;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.client.config.HttpClientConfig.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class JestClientFactory extends AbstractFactoryBean<JestClient> {

	private final Logger LOGGER = LoggerFactory.getLogger(JestClientFactory.class);

	@Autowired
	private ElasticConfiguration configuration;

	@Override
	public Class<JestClient> getObjectType() {
		return JestClient.class;
	}
	
	@Override
	protected JestClient createInstance() throws Exception {
		if (Protocol.HTTP.equals(configuration.getProtocol())) {
			Builder clientConfig = new HttpClientConfig.Builder(configuration.getHostsUrls()).multiThreaded(true);
			io.searchbox.client.JestClientFactory factory = new io.searchbox.client.JestClientFactory();
			factory.setHttpClientConfig(clientConfig.build());
			
			return factory.getObject();
		}

		LOGGER.error("Unsupported protocol [{}] for elastic client", configuration.getProtocol());
		throw new IllegalStateException(String.format("Unsupported protocol [%s] for Jest client", configuration.getProtocol()));
	}
}