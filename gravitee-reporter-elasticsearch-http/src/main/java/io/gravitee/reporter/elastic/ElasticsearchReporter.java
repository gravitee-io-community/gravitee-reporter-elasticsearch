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

import io.gravitee.common.service.AbstractService;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.Reporter;
import io.gravitee.reporter.elastic.client.Client;
import io.gravitee.reporter.elastic.indexer.Indexer;
import io.gravitee.reporter.elastic.mapping.IndexPreparer;
import io.gravitee.reporter.elastic.spring.context.Elastic2xBeanRegistrer;
import io.gravitee.reporter.elastic.spring.context.Elastic5xBeanRegistrer;
import io.gravitee.reporter.elastic.spring.context.Elastic6xBeanRegistrer;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ElasticsearchReporter extends AbstractService implements Reporter {

	private final Logger logger = LoggerFactory.getLogger(ElasticsearchReporter.class);

	@Autowired
	private Client client;

	/**
	 * Indexer is settled in a lazy way as soon as the ES version has been discovered.
	 */
	private Indexer indexer;

	@Override
	protected void doStart() throws Exception {
		super.doStart();

		logger.info("Starting Elastic reporter engine...");

		int version = this.client.getVersion();
		boolean registered = true;

		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

		switch (version) {
			case 2:
				new Elastic2xBeanRegistrer().register(beanFactory);
				break;
			case 5:
				new Elastic5xBeanRegistrer().register(beanFactory);
				break;
			case 6:
				new Elastic6xBeanRegistrer().register(beanFactory);
				break;
			default:
				registered = false;
				logger.error("Version {} is not supported by this Elasticsearch connector", version);
		}

		if (registered) {
			IndexPreparer preparer = applicationContext.getBean(IndexPreparer.class);
			preparer.ensureTemplates();
			preparer.ensurePlugins();

			indexer = applicationContext.getBean(Indexer.class);
			logger.info("Starting Elastic reporter engine... DONE");
		} else {
			logger.info("Starting Elastic reporter engine... ERROR");
		}
	}

	@Override
	public void report(Reportable reportable) {
		rxReport(reportable);
	}

	public Single rxReport(Reportable reportable) {
		return indexer
				.index(reportable)
				.doOnError(throwable -> logger.error("An error occurs while indexing data into Elasticsearch"));
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();

		logger.info("Stopping Elastic reporter engine... DONE");
	}
}