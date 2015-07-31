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
package io.gravitee.reporters.elastic.engine.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.reporters.elastic.engine.ReportEngine;

public class JestReportEngine implements ReportEngine {

	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	@Override
	public void start() {
		LOGGER.info("Starting Elastic reporter engine...");
		
		//TODO implementation
		
		LOGGER.info("Starting Elastic reporter engine... DONE");
		
	}

	@Override
	public void stop() {
		
		LOGGER.info("Stopping Elastic reporter engine...");
		
		//TODO implementation
		
		LOGGER.info("Stopping Elastic reporter engine... DONE");
		
	}

	@Override
	public void report(Request request, Response response) {
		// TODO Auto-generated method stub
		
	}

}
