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

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.reporter.Reporter;
/**
 * 
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 * 
 */
public class ElasticRequestReporter implements Reporter {
	
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    private BulkProcessor bulkProcessor;
    
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
    
	@Override
	public void report(Request request, Response response) {
		
		Date date = new Date();
				
		try {
			bulkProcessor.add(new IndexRequest("gravitee-"+sdf.format(date), "request").source(
				XContentFactory.jsonBuilder()
					.startObject()
						.field("id", request.id())
					    .field("uri", request.uri())
					    .field("path", request.path())
					    .field("status", response.status())
					    .field("method", request.method().toString())
					    .field("hostname", InetAddress.getLocalHost().getHostName())
					        //.field("@timestamp",new Date())
					.endObject()));
			
		} catch (IOException e) {
			LOGGER.error("Request {} report failed", request.id() ,e);
		}
	}

}