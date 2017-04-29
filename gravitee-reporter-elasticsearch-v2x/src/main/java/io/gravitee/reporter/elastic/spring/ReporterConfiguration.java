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

import io.gravitee.reporter.elastic.config.ElasticConfiguration;
import io.gravitee.reporter.elastic.engine.ReportEngine;
import io.gravitee.reporter.elastic.engine.impl.ElasticReportEngine;
import io.gravitee.reporter.elastic.spring.conditional.ElasticClientTransportCondition;
import io.gravitee.reporter.elastic.spring.factory.ElasticBulkProcessorFactory;
import io.gravitee.reporter.elastic.spring.factory.ElasticClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReporterConfiguration {

	@Bean @Conditional(ElasticClientTransportCondition.class)
    public ElasticClientFactory elasticClientFactory() {
        return new ElasticClientFactory();
    }
	
    @Bean @Conditional(ElasticClientTransportCondition.class)
    public ElasticBulkProcessorFactory elasticBulkProcessorFactory() {
        return new ElasticBulkProcessorFactory();
    }
	
	@Bean
	public ReportEngine reportEngine(){
		return new ElasticReportEngine();
	}

    @Bean 
    public ElasticConfiguration configuration(){
    	return new ElasticConfiguration();
    }
}
