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


import io.gravitee.gateway.api.metrics.Metrics;
import io.gravitee.reporter.elastic.config.ElasticConfiguration;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public final class JestReportEngine extends AbstractElasticReportEngine {

	private final Logger LOGGER = LoggerFactory.getLogger(JestReportEngine.class);
	
	@Autowired
	protected JestClient client;
	
	@Autowired
	private ElasticConfiguration configuration;
	
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
	public void report(Metrics metrics) {
		try{
			String indexName = getIndexName(metrics);
			
			String jsonObject = super.getSource(metrics).string();
	
			Index index = new Index.Builder(jsonObject).index(indexName).type(configuration.getTypeName()).build();
			client.executeAsync(index, new JestResultHandler<JestResult>() {
				public void failed(Exception ex) {
				}
	
				public void completed(JestResult result) {
				}
			});
			
		} catch (IOException e) {
			LOGGER.error("Request {} report failed", metrics.getRequestId(), e);
		}
	}
}
