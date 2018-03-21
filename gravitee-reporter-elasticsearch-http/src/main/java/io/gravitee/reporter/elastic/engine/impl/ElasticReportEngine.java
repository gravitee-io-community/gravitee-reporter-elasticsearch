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
package io.gravitee.reporter.elastic.engine.impl;

import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.elastic.client.Client;
import io.gravitee.reporter.elastic.config.ElasticConfiguration;
import io.gravitee.reporter.elastic.config.PipelineConfiguration;
import io.gravitee.reporter.elastic.engine.ReportEngine;
import io.gravitee.reporter.elastic.indexer.Indexer;
import io.gravitee.reporter.elastic.spring.context.Elastic2xBeanRegistrer;
import io.gravitee.reporter.elastic.spring.context.Elastic5xBeanRegistrer;
import io.gravitee.reporter.elastic.spring.context.Elastic6xBeanRegistrer;
import io.gravitee.reporter.elastic.templating.freemarker.FreeMarkerComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Elasticsearch report engine. 
 * 
 * Use the POST http://_bulk url
 * 
 * @author Sebastien Devaux (Zenika)
 * @author Guillaume Waignier (Zenika)
 * @author Guillaume Gillon
 *
 */
public final class ElasticReportEngine implements ReportEngine, ApplicationContextAware {

	private final Logger LOGGER = LoggerFactory.getLogger(ElasticReportEngine.class);

	private ApplicationContext applicationContext;

	@Autowired
	private Client client;

	private Indexer indexer;

	/**
	 * Configuration of Elasticsearch.
	 */
	@Autowired
	private ElasticConfiguration configuration;

	/**
	 * Configuration of Pipeline.
	 */
	@Autowired
	private PipelineConfiguration pipelineConfiguration;

	/**
	 * Templating tool.
	 */
	@Autowired
	private FreeMarkerComponent freeMarkerComponent;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void report(Reportable reportable) {
		indexer.index(reportable);
		/*
		Single
				.just(reportable)
				.map(reportable1 -> {
                    if (reportable1 instanceof Metrics) {
						return getSource((Metrics) reportable1, pipelineConfiguration.getPipeline());
                    } else if (reportable1 instanceof EndpointStatus) {
                        return getSource((EndpointStatus) reportable1);
                    } else if (reportable1 instanceof Monitor) {
                        return getSource((Monitor) reportable1);
                    } else if (reportable1 instanceof Log) {
						return getSource((Log) reportable1);
					}

                    return null;
                })
				.doOnSuccess(data -> indexer.index(data));
		*/
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() throws Exception {
		LOGGER.info("Starting Elastic reporter engine...");

		int version = this.client.getVersion();
		boolean registered = true;
		AnnotationConfigApplicationContext versionAwareContext =
				(AnnotationConfigApplicationContext) this.applicationContext;

		switch (version) {
			case 2:
				versionAwareContext.register(Elastic2xBeanRegistrer.class);
				break;
			case 5:
				versionAwareContext.register(Elastic5xBeanRegistrer.class);
				break;
			case 6:
				versionAwareContext.register(Elastic6xBeanRegistrer.class);
				break;
			default:
				registered = false;
				LOGGER.error("Version {} of Elasticsearch is not supported by this connector", version);
		}

		if (registered) {
			versionAwareContext.refresh();
			LOGGER.info("Starting Elastic reporter engine... DONE");
		} else {
			LOGGER.info("Starting Elastic reporter engine... ERROR");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() {
		LOGGER.info("Stopping Elastic reporter engine...");

		//this.indexer.stop();

		LOGGER.info("Stopping Elastic reporter engine... DONE");
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
