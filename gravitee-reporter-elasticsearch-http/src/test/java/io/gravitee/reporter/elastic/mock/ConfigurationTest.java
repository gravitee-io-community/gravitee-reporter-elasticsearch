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
package io.gravitee.reporter.elastic.mock;

import io.gravitee.common.node.Node;
import io.gravitee.reporter.elastic.config.ElasticConfiguration;
import io.gravitee.reporter.elastic.config.Endpoint;
import io.gravitee.reporter.elastic.spring.ReporterConfiguration;
import io.vertx.core.Vertx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.Collections;

/**
 * Spring configuration for the test.
 * 
 * @author Guillaume Waignier
 * @author Sebastien Devaux
 *
 */
@Configuration
@Import(ReporterConfiguration.class)
public class ConfigurationTest {

    @Bean
    public Vertx vertx() {
        return Vertx.vertx();
    }

    @Bean
    public ElasticConfiguration configuration() {
        ElasticConfiguration elasticConfiguration = new ElasticConfiguration();
        elasticConfiguration.setEndpoints(Collections.singletonList(new Endpoint("http://localhost:" + elasticsearchNode().getHttpPort())));
        elasticConfiguration.setIngestPlugins(Arrays.asList("geoip"));
        return elasticConfiguration;
    }

    @Bean
    public Node node() {
        return new NodeMock();
    }

    @Bean
    public ElasticsearchNode elasticsearchNode() {
        return new ElasticsearchNode();
    }
}
