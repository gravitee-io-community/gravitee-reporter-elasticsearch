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
package io.gravitee.reporter.elastic.engine.impl;


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.reporter.elastic.config.Config;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Index;

public class JestReportEngine extends AbstractElasticReportEngine {

	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	protected JestClient client;
	
	@Autowired
	private Config configuration;
	
	@Override
	public void start() {
		
		LOGGER.info("Starting Elastic reporter engine...");
		LOGGER.info("Starting Elastic reporter engine... DONE");
		
	}

	@Override
	public void stop() {
		
		LOGGER.info("Stopping Elastic reporter engine...");
		client.shutdownClient();
		LOGGER.info("Stopping Elastic reporter engine... DONE");
		
	}

	@Override
	public void report(Request request, Response response) {
		try{
			String indexName = getIndexName(request);
			
			String jsonObject = super.getSource(request, response).string();
	
			Index index = new Index.Builder(jsonObject).index(indexName).type(configuration.getTypeName()).build();
			client.executeAsync(index, new JestResultHandler<JestResult>() {
				public void failed(Exception ex) {
				}
	
				public void completed(JestResult result) {
				}
			});
			
		} catch (IOException e) {
			LOGGER.error("Request {} report failed", request.id() ,e);
		}
	}
	
	private void createIndex(){
		//client.execute(new CreateIndex.Builder("articles").build());
//		PutMapping putMapping = new PutMapping.Builder(
//		        "my_index",
//		        "my_type",
//		        "{ \"document\" : { \"properties\" : { \"message\" : {\"type\" : \"string\", \"store\" : \"yes\"} } } }"
//		).build();
//		client.execute(putMapping);
	}

	

}
