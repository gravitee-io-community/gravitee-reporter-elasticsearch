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
package io.gravitee.reporter.elastic;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.RequestReporter;
import io.gravitee.gateway.api.Response;
import io.gravitee.reporter.elastic.config.Configuration;
import io.gravitee.reporter.elastic.config.loader.PropertieConfigLoader;
import io.gravitee.reporter.elastic.model.TransportAddress;

import java.util.List;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 * 
 */
public class ElasticRequestReporter implements RequestReporter {
	
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    
    private Configuration config;
    
    private Client client;
    
	public ElasticRequestReporter() throws Exception{
		this(System.getProperty("reporter.conf.elastic", "/etc/gravitee.io/conf/elastic.properties"));
	}
	
	public ElasticRequestReporter(final String configurationPath) throws Exception{
		this.config = new PropertieConfigLoader().load(configurationPath);
		this.client = getClient();
	}
	
	
	

	@Override
	public void report(Request request, Response response) {
		
		try{
			LOGGER.debug("start report");
			
			BulkRequestBuilder bulkRequest = client.prepareBulk();
	
			IndexRequestBuilder indexRequestBuilder = this.client.prepareIndex("gravitee", "request");
			bulkRequest.add(indexRequestBuilder.setSource(
					XContentFactory.jsonBuilder()
					    .startObject()
					        .field("uri", request.uri())
					        .field("status", response.status())
					        .field("methode", request.method().toString())
					    .endObject()));
				    
			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			
			if (bulkResponse.hasFailures()) {
				LOGGER.error("ES Request report request failure");
			}
			
			LOGGER.debug("end report");
			
			// TODO : https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/bulk.html
			
		}catch(Exception e){
			LOGGER.error("ES Request report insertion failure", e);
		}
		
	}
	
	
	public TransportClient getClient(){

		TransportClient client = new TransportClient();
		
		List<TransportAddress> adresses = config.getTransportAddresses();
		
		for (TransportAddress address : adresses) {
			client.addTransportAddress(new InetSocketTransportAddress(address.getHostname(), address.getPort()));
		}
		return client;
		
	}
}