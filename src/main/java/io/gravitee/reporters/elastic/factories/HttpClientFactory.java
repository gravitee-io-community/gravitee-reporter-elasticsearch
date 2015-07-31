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

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import io.gravitee.reporters.elastic.config.Config;
import io.gravitee.reporters.elastic.model.Protocol;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.client.config.HttpClientConfig.Builder;


public class HttpClientFactory extends AbstractFactoryBean<JestClient> {

	@Autowired
	private Config config;

	@Override
	public Class<JestClient> getObjectType() {
		return JestClient.class;
	}
	
	@Override
	protected JestClient createInstance() throws Exception {

		if(Protocol.HTTP.equals(config.getProtocol())){
			
			Builder clientConfig = new HttpClientConfig.Builder(Arrays.asList(config.getHosts())).multiThreaded(true);
			JestClientFactory factory = new JestClientFactory();
			factory.setHttpClientConfig(clientConfig.build());
			
			return factory.getObject();
		}
		
		throw new IllegalStateException(String.format("Unupported protocol [%s] for JestClient", config.getProtocol()));
	}
}