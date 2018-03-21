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
package io.gravitee.reporter.elastic.spring;

import io.gravitee.reporter.elastic.client.Client;
import io.gravitee.reporter.elastic.client.http.HttpClient;
import io.gravitee.reporter.elastic.config.ElasticConfiguration;
import io.gravitee.reporter.elastic.config.PipelineConfiguration;
import io.gravitee.reporter.elastic.templating.freemarker.FreeMarkerComponent;
import io.vertx.reactivex.core.Vertx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReporterConfiguration {

    @Bean
    public Vertx vertxRx(io.vertx.core.Vertx vertx) {
        return Vertx.newInstance(vertx);
    }

    @Bean
    public Client httpClient() {
        return new HttpClient();
    }

    @Bean 
    public ElasticConfiguration configuration(){
    	return new ElasticConfiguration();
    }

    @Bean
    public PipelineConfiguration pipelineConfiguration() {
        return new PipelineConfiguration();
    }

    @Bean
    public FreeMarkerComponent freeMarckerComponent() {
        return new FreeMarkerComponent();
    }
}
