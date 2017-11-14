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
import io.gravitee.reporter.elastic.engine.impl.ElasticReportEngine;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ElasticBulkProcessorFactory extends AbstractFactoryBean<BulkProcessor> {

    private final Logger LOGGER = LoggerFactory.getLogger(ElasticBulkProcessorFactory.class);

    private final static String FIELD_TYPE = "type";
    private final static String FIELD_TYPE_KEYWORD = "keyword";
    private final static String FIELD_TYPE_SHORT = "short";
    private final static String FIELD_TYPE_INTEGER = "integer";
    private final static String FIELD_TYPE_BOOLEAN = "boolean";
    private final static String FIELD_TYPE_OBJECT = "object";
    private final static String FIELD_INDEX = "index";
    private final static String FIELD_ENABLED = "enabled";
    private final static String FIELD_INDEX_NOT_ANALYZED = "false";

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
            request.subRequests().stream()
                    .map(IndicesRequest::indices)
                    .flatMap(Stream::of)
                    .forEach(this::create);
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
            client.admin().indices().prepareCreate(indexName)
                    .addMapping(ElasticReportEngine.TYPE_REQUEST, createRequestMapping())
                    .addMapping(ElasticReportEngine.TYPE_HEALTH, createHealthMapping())
                    .addMapping(ElasticReportEngine.TYPE_MONITOR, createMonitorMapping())
                    .addMapping(ElasticReportEngine.TYPE_LOG, createLogMapping())
                    .execute().actionGet();
            return true;
        } catch (ResourceAlreadyExistsException raee) {
            return false;
        } catch (Exception ex) {
            LOGGER.error("An error occurs while looking for index {}", indexName, ex);
        }
        return false;
    }

    private XContentBuilder createRequestMapping() throws IOException {
        return XContentFactory.jsonBuilder()
                    .startObject()
                    .startObject(ElasticReportEngine.TYPE_REQUEST)
                    .startObject("properties")
                    .startObject("gateway").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).endObject()
                    .startObject("transaction").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).endObject()
                    .startObject("api").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).endObject()
                    .startObject("application").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).endObject()
                    .startObject("plan").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).endObject()
                    .startObject("api-key").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("uri").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("path").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("endpoint").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).endObject()
                    .startObject("local-address").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("remote-address").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("method").field(FIELD_TYPE, FIELD_TYPE_SHORT).endObject()
                    .startObject("tenant").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).endObject()
                    .startObject("status").field(FIELD_TYPE, FIELD_TYPE_SHORT).endObject()
                    .startObject("response-time").field(FIELD_TYPE, FIELD_TYPE_INTEGER).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("api-response-time").field(FIELD_TYPE, FIELD_TYPE_INTEGER).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("proxy-latency").field(FIELD_TYPE, FIELD_TYPE_INTEGER).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("request-content-length").field(FIELD_TYPE, FIELD_TYPE_INTEGER).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("response-content-length").field(FIELD_TYPE, FIELD_TYPE_INTEGER).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .startObject("message").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                    .endObject()
                    .endObject()
                    .endObject();
    }

    private XContentBuilder createHealthMapping() throws IOException {
        return XContentFactory.jsonBuilder()
                            .startObject()
                            .startObject(ElasticReportEngine.TYPE_HEALTH)
                            .startObject("properties")
                            .startObject("gateway").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).endObject()
                            .startObject("api").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).endObject()
                            .startObject("endpoint").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).endObject()
                            .startObject("available").field(FIELD_TYPE, FIELD_TYPE_BOOLEAN).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                            .startObject("response-time").field(FIELD_TYPE, FIELD_TYPE_INTEGER).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                            .startObject("success").field(FIELD_TYPE, FIELD_TYPE_BOOLEAN).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                            .startObject("state").field(FIELD_TYPE, FIELD_TYPE_INTEGER).field(FIELD_INDEX, FIELD_INDEX_NOT_ANALYZED).endObject()
                            .startObject("steps").field(FIELD_TYPE, FIELD_TYPE_OBJECT).field(FIELD_ENABLED, false).endObject()
                            .endObject()
                            .endObject()
                            .endObject();
    }

    private XContentBuilder createMonitorMapping() throws IOException {
        return XContentFactory.jsonBuilder()
                            .startObject()
                            .startObject(ElasticReportEngine.TYPE_MONITOR)
                            .startObject("properties")
                            .startObject("gateway").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).endObject()
                            .startObject("hostname").field(FIELD_TYPE, FIELD_TYPE_KEYWORD).endObject()
                            .endObject()
                            .endObject()
                            .endObject();
    }

    private XContentBuilder createLogMapping() throws IOException {
        return XContentFactory.jsonBuilder()
                .startObject()
                .startObject(ElasticReportEngine.TYPE_LOG)
                .startObject("properties")
                .startObject("client-request").field(FIELD_TYPE, FIELD_TYPE_OBJECT).field(FIELD_ENABLED, false).endObject()
                .startObject("client-response").field(FIELD_TYPE, FIELD_TYPE_OBJECT).field(FIELD_ENABLED, false).endObject()
                .startObject("proxy-request").field(FIELD_TYPE, FIELD_TYPE_OBJECT).field(FIELD_ENABLED, false).endObject()
                .startObject("proxy-response").field(FIELD_TYPE, FIELD_TYPE_OBJECT).field(FIELD_ENABLED, false).endObject()
                .endObject()
                .endObject()
                .endObject();
    }
}