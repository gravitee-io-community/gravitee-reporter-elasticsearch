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
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import io.gravitee.reporter.elastic.config.Configuration;


public class ElasticBulkProcessorFactory extends AbstractFactoryBean<BulkProcessor> {

	@Autowired
	private Client client;
	
	@Autowired
	private Configuration config;

	@Override
	public Class<?> getObjectType() {
		return BulkProcessor.class;
	}
	
	@Override
	protected BulkProcessor createInstance() throws Exception {
	
		BulkProcessor bulkProcessor = BulkProcessor.builder(
		        client,  
		        new BulkProcessor.Listener() {

					@Override
					public void beforeBulk(long executionId, BulkRequest request) {
						// TODO Auto-generated method stub
					}

					@Override
					public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
						// TODO Auto-generated method stub
					}

					@Override
					public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
						// TODO Auto-generated method stub
					}})
				
		        .setBulkActions(config.getBulkActions()) 
		        .setBulkSize(new ByteSizeValue(config.getBulkSize(), ByteSizeUnit.MB)) 
		        .setFlushInterval(TimeValue.timeValueSeconds(config.getFlushInterval())) 
		        .setConcurrentRequests(config.getConcurrentRequests()) 
		        .build();
		
		return bulkProcessor;
	}


}

