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
package io.gravitee.reporters.elastic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import io.gravitee.reporters.elastic.conditional.ElasticClientCondition;
import io.gravitee.reporters.elastic.conditional.JestClientCondition;
import io.gravitee.reporters.elastic.engine.ReportEngine;
import io.gravitee.reporters.elastic.engine.impl.ElasticReportEngine;
import io.gravitee.reporters.elastic.engine.impl.JestReportEngine;
import io.gravitee.reporters.elastic.factories.ElasticBulkProcessorFactory;
import io.gravitee.reporters.elastic.factories.ElasticClientFactory;
import io.gravitee.reporters.elastic.factories.HttpClientFactory;
import io.gravitee.reporters.elastic.model.Protocol;

@Configuration
public class ReporterConfiguration {

	@Bean @Conditional(JestClientCondition.class)
    public HttpClientFactory httpClientFactory() {
        return new HttpClientFactory();
    }
	
	@Bean @Conditional(ElasticClientCondition.class)
    public ElasticClientFactory elasticClientFactory() {
        return new ElasticClientFactory();
    }
	
    @Bean @Conditional(ElasticClientCondition.class)
    public ElasticBulkProcessorFactory elasticBulkProcessorFactory() {
        return new ElasticBulkProcessorFactory();
    }
	
	@Bean
	public ReportEngine reportEngine(Config configuration){
		if(Protocol.HTTP.equals(configuration.getProtocol())){
			return new JestReportEngine();
		}
		return new ElasticReportEngine();
	}


    @Bean 
    public Config configuration(){
    	return new Config();
    }
}
