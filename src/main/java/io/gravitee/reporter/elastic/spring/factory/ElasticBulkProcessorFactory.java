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
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class ElasticBulkProcessorFactory extends AbstractFactoryBean<BulkProcessor> {

    private final Logger LOGGER = LoggerFactory.getLogger(ElasticBulkProcessorFactory.class);

    private final static String FIELD_TYPE = "type";
    private final static String FIELD_TYPE_STRING = "string";
    private final static String FIELD_INDEX = "index";
    private final static String FIELD_INDEX_NOT_ANALYZED = "not_analyzed";

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
                        if (request.numberOfActions() > 0) {
                            initializeIndices(request);
                        }
                    }

                    @Override
                    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                    }

                    @Override
                    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                        LOGGER.error("Unexpected error while bulk-indexing data.", failure);
                    }
                })
                .setBulkActions(config.getBulkActions())
                .setBulkSize(new ByteSizeValue(-1))
                .setFlushInterval(TimeValue.timeValueSeconds(config.getFlushInterval()))
                .setConcurrentRequests(config.getConcurrentRequests())
                .build();
    }


    private void initializeIndices(BulkRequest request) {
        try {
            String[] indicesToCreate = request.subRequests().stream()
                    .map(IndicesRequest::indices)
                    .flatMap(Stream::of)
                    .distinct()
                    .filter(this::create)
                    .toArray(String[]::new);

            if (indicesToCreate.length > 0) {
                createRequestMapping(indicesToCreate);
                createHealthMapping(indicesToCreate);
                createMonitorMapping(indicesToCreate);
            }
        } catch (Exception ex) {
            LOGGER.error("An error occurs while creating indices", ex);
        }
    }

    /**
     * Create elasticsearch index.
     *
     * @param indexName Index name
     * @return true success
     */
    private boolean create(String indexName) {
        try {
            LOGGER.debug("Looking for index [{}]", indexName);
            client.admin().indices().prepareCreate(indexName).execute().actionGet();
            return true;
        } catch (org.elasticsearch.indices.IndexAlreadyExistsException iaee) {
            return false;
        } catch (Exception ex) {
            LOGGER.error("An error occurs while looking for index", indexName, ex);
        }
        return false;
    }

    private void createRequestMapping(String[] indicesToCreate) {
        String typeName = "request";

        try {
            createMapping(indicesToCreate, typeName,
                    XContentFactory.jsonBuilder()
                    .startObject()
                    .startObject(typeName)
                    .startObject("properties")
                    .startObject("id").field(FIELD_TYPE, FIELD_TYPE_STRING).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("api").field(FIELD_TYPE, FIELD_TYPE_STRING).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("application").field(FIELD_TYPE, FIELD_TYPE_STRING).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("api-key").field(FIELD_TYPE, FIELD_TYPE_STRING).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("hostname").field(FIELD_TYPE, FIELD_TYPE_STRING).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("uri").field(FIELD_TYPE, FIELD_TYPE_STRING).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("path").field(FIELD_TYPE, FIELD_TYPE_STRING).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .endObject()
                    .endObject()
                    .endObject()
                    .string());
        } catch (IOException ex) {
            LOGGER.error("Error creating indices mapping [{}]", indicesToCreate, ex);
        }
    }

    private void createHealthMapping(String[] indicesToCreate) {
        String typeName = "health";

        try {
            createMapping(indicesToCreate, typeName,
                    XContentFactory.jsonBuilder()
                            .startObject()
                            .startObject(typeName)
                            .startObject("properties")
                            .startObject("api").field(FIELD_TYPE, FIELD_TYPE_STRING).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                            .startObject("hostname").field(FIELD_TYPE, FIELD_TYPE_STRING).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                            .endObject()
                            .endObject()
                            .endObject()
                            .string());
        } catch (IOException ex) {
            LOGGER.error("Error creating indices mapping [{}]", indicesToCreate, ex);
        }
    }

    private void createMonitorMapping(String[] indicesToCreate) {
        String typeName = "monitor";

        try {
            createMapping(indicesToCreate, typeName,
                    XContentFactory.jsonBuilder()
                            .startObject()
                            .startObject(typeName)
                            .startObject("properties")
                            .startObject("gateway").field(FIELD_TYPE, FIELD_TYPE_STRING).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                            .startObject("hostname").field(FIELD_TYPE, FIELD_TYPE_STRING).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                            .endObject()
                            .endObject()
                            .endObject()
                            .string());
        } catch (IOException ex) {
            LOGGER.error("Error creating indices mapping [{}]", indicesToCreate, ex);
        }
    }

    private void createMapping(String[] indices, String type, String mapping) {
        try {
            LOGGER.debug("Applying mapping for [{}]/[{}]: {}", indices, type, mapping);
            client.admin().indices().preparePutMapping(indices)
                    .setType(type).setSource(mapping).execute().actionGet();
        } catch (ElasticsearchException eex) {
            LOGGER.error("Error creating indices mapping [{}]", indices, eex);
        }
    }
}

