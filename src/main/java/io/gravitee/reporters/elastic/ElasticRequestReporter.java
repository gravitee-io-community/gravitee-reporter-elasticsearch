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
package io.gravitee.reporters.elastic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.reporter.Reporter;
import io.gravitee.reporters.elastic.engine.ReportEngine;

/**
 * 
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 * 
 */
public class ElasticRequestReporter implements Reporter {
	
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	  
	@Autowired
	private ReportEngine reportEngine;
	    
    public ElasticRequestReporter(){
    }
    
	@Override
	public void report(Request request, Response response) {	
		reportEngine.report(request, response);
	}
}