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
package io.gravitee.reporter.elastic.engine;

import io.gravitee.reporter.api.Reportable;

/**
 * Report request execution.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 *
 */
public interface ReportEngine {

	/**
	 * Start reporting engine
	 * @throws Exception when a problem occurs with Elasticsearch
	 */
	void start() throws Exception;
	
	/**
	 * Stop reporting engine
	 */
	void stop();
	
	/**
	 * A reportable element
	 * 
	 * @param reportable
	 */
	void report(Reportable reportable);
}
