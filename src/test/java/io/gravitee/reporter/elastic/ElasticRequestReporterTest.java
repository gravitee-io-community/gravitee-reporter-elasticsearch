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

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.metrics.Metrics;
import io.gravitee.reporter.elastic.spring.ReporterConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 *
 */
public class ElasticRequestReporterTest {

	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	public static final String CONFIG_PATH = "/config/local-elastic.properties";

	public ElasticRequestReporter reporter;

	//@Before
	public void init() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ReporterConfiguration.class);
		ctx.refresh();
		this.reporter = ctx.getBean(ElasticRequestReporter.class);
	}
	
	@Test
	public void dummyTest() {
		//TODO correct reporter test
	}

	//@Test
	//@Ignore
	public void singleReportTest() {

		try {
			Metrics metrics = Mockito.mock(Metrics.class);

			Mockito.when(metrics.getRequestHttpMethod()).thenReturn(HttpMethod.GET);
			Mockito.when(metrics.getRequestPath()).thenReturn("/customers/");
			Mockito.when(metrics.getResponseHttpStatus()).thenReturn(200);

			reporter.report(metrics);

			Thread.sleep(5000);

		} catch (Exception e) {

			LOGGER.error("Configuration loader error", e);
			Assert.fail("Error reporting request");
		}
	}

}
