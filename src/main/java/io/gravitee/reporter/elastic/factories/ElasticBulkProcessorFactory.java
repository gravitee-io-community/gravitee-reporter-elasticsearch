package io.gravitee.reporter.elastic.factories;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
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

