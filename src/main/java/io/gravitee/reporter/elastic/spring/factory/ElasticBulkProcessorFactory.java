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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.gravitee.reporter.elastic.config.ElasticConfiguration;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequestBuilder;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class ElasticBulkProcessorFactory extends AbstractFactoryBean<BulkProcessor> {

	private Logger logger = LoggerFactory.getLogger(ElasticBulkProcessorFactory.class);
	
	@Autowired
	private Client client;
	
	@Autowired
	private ElasticConfiguration config;

	@Override
	public Class<?> getObjectType() {
		return BulkProcessor.class;
	}
	
	@Override
	protected BulkProcessor createInstance() throws Exception {
	
		return BulkProcessor.builder(
		        client,  
		        new BulkProcessor.Listener() {

					@Override
					public void beforeBulk(long executionId, BulkRequest request) {
						initAllIndexes(executionId, request);
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
	}

	
	private void initAllIndexes(long executionId, BulkRequest request){
		
		try{
			List<? extends IndicesRequest> indicesRequests = request.subRequests();
			HashSet<String> indexes = new HashSet<>();
			
			//Building used indices list
			for (IndicesRequest actionRequest : indicesRequests) {
				indexes.addAll(Arrays.asList(actionRequest.indices()));
			}
			
			createIndexesMapping(indexes);
			
		}catch(Exception e){
			logger.error("Fail to initialized indexes", e);
		}
	}

	/**
	 * 
	 * @param indexes
	 * @return
	 */
	private Collection<String> resolveMissingIndexes(Collection<String> indexes){
		
		// Search existing indexes
		 Collection<String> exisitingIndexes = searchIndexes(indexes);
					
		//Computing missing indexes
		Set<String> indexesMissing = new HashSet<>(indexes);
		indexesMissing.removeAll(Arrays.asList(exisitingIndexes));
		
		return indexesMissing;
	}
	
	/**
	 * Search index with the given name
	 * 
	 * @param indexes Index to check
	 * @return existing indexes from the list
	 */
	private Collection<String> searchIndexes(Collection<String> indexes){
	
		//TODO deal with missing index exceptions
		
		//Searching existing indexes
		String[] searchedIndexes = indexes.toArray(new String[indexes.size()]);
		GetIndexRequestBuilder builder = new GetIndexRequestBuilder(client.admin().indices(), searchedIndexes);
		String[] exisitingIndexes = builder.get().indices();
		
		return Arrays.asList(exisitingIndexes);
	}
	
	/**
	 * Create elasticsearch index.
	 * 
	 * @param indexName Index name
	 * @return true success
	 */
	private boolean createIndex(String indexName) {
		try {
			logger.debug("Trying to create index [{}]", indexName);
			client.admin().indices().prepareCreate(indexName).execute().actionGet();
			logger.debug("index created [{}]", indexName);

			return true;

		} catch (Exception e) {
			if (ExceptionsHelper.unwrapCause(e) instanceof IndexAlreadyExistsException) {
				logger.debug("Index [{}] already exists, skipping...", indexName);
			} else {
				logger.error("Index [{}] initialization failed.", indexName);
			}
		}
		return false;
	}

	/**
	 * Create index mappings.
	 * 
	 * @param indexesNames Index to be configured
	 */
	private void createIndexesMapping(Collection<String> indexesNames) {

		// If nothing to do, we skip
		if (indexesNames == null || indexesNames.isEmpty()) {
			logger.debug("No index to prepare");
			return;
		}

		
		//Index creation
		List<String> createdIndexes = new ArrayList<>();
		for (String indexName : indexesNames) {
			if (createIndex(indexName)) {
				createdIndexes.add(indexName);
			}
		}

		createRequestMapping(createdIndexes);
		createHealthMapping(createdIndexes);
	}

	private void createRequestMapping(List<String> createdIndexes) {
		String typeName = "request";

		// Index mapping configuration
		try {
			String mapping = XContentFactory.jsonBuilder()
					.startObject()
					.startObject(typeName)
					.startObject("properties")
					.startObject("id").field("type", "string").field("index", "not_analyzed").endObject()
					.startObject("api-name").field("type", "string").field("index", "not_analyzed").endObject()
					.startObject("api-key").field("type", "string").field("index", "not_analyzed").endObject()
					.startObject("hostname").field("type", "string").field("index", "not_analyzed").endObject()
					.startObject("uri").field("type", "string").field("index", "not_analyzed").endObject()
					.startObject("path").field("type", "string").field("index", "not_analyzed").endObject()
					.endObject().
							endObject()
					.endObject()
					.string();

			logger.debug("Applying default mapping for [{}]/[{}]: {}", createdIndexes, typeName, mapping);
			client.admin().indices().preparePutMapping(createdIndexes.toArray(new String[createdIndexes.size()]))
					.setType(typeName).setSource(mapping).execute().actionGet();

		} catch (IOException e) {
			logger.error("Error creating indexes mapping [{}]", createdIndexes);
		}
	}

	private void createHealthMapping(List<String> createdIndexes) {
		String typeName = "health";

		// Index mapping configuration
		try {
			String mapping = XContentFactory.jsonBuilder()
					.startObject()
					.startObject(typeName)
					.startObject("properties")
					.startObject("api").field("type", "string").field("index", "not_analyzed").endObject()
					.startObject("hostname").field("type", "string").field("index", "not_analyzed").endObject()
					.endObject().
							endObject()
					.endObject()
					.string();

			logger.debug("Applying default mapping for [{}]/[{}]: {}", createdIndexes, typeName, mapping);
			client.admin().indices().preparePutMapping(createdIndexes.toArray(new String[createdIndexes.size()]))
					.setType(typeName).setSource(mapping).execute().actionGet();

		} catch (IOException e) {
			logger.error("Error creating indexes mapping [{}]", createdIndexes);
		}
	}
}

