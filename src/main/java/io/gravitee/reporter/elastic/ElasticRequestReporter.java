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
import java.util.TimeZone;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.joda.time.format.DateTimeFormat;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.reporter.Reporter;
import io.gravitee.reporter.elastic.config.Configuration;
import io.gravitee.reporter.elastic.config.ReporterConfiguration;
/**
 * 
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 * 
 */
public class ElasticRequestReporter implements Reporter, ApplicationContextAware{
	
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private ApplicationContext reporterContext;
    
    private BulkProcessor bulkProcessor;
    
    private Configuration configuration;
  
    /** Index simple date format **/
    private SimpleDateFormat sdf;
    
    /** Document simple date format **/
    private  DateTimeFormatter dtf;
    
    public ElasticRequestReporter(){
	    this.sdf = new SimpleDateFormat("yyyy.MM.dd");
	    this.dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
		this.sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    
	@Override
	public void report(Request request, Response response) {
		
		Date date = new Date();
				
		try {
			
			String reqContentType = request.headers().get("Content-Type");
			String respContentType = response.headers().get("Content-Type");
			
			String reqContentLength = request.headers().get("Content-Length");
			String respContentLength = response.headers().get("Content-Length");
			
			String indexName =  String.format("%s-%s",configuration.getIndexName(), sdf.format(date));
			
			bulkProcessor.add(new IndexRequest(indexName, configuration.getTypeName()).source(
				XContentFactory.jsonBuilder()
					.startObject()
						.field("id", request.id())
					    .field("uri", request.uri())
					    .field("path", request.path())
					    .field("status", response.status())
					    .field("method", request.method().toString())
					    .field("request-content-type", reqContentType)
					    .field("response-content-type", respContentType)		    
					    .field("request-content-length", reqContentLength)
					    .field("response-content-length", respContentLength)					    
					    .field("hostname", InetAddress.getLocalHost().getHostName())
					    .field("@timestamp",date, dtf)
					.endObject()));
			
		} catch (IOException e) {
			LOGGER.error("Request {} report failed", request.id() ,e);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.init(context);
	}
	
    private void registerReporterContext(ApplicationContext parentContext) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setParent(parentContext);
        context.register(ReporterConfiguration.class);
        context.refresh();
        
        this.reporterContext = context;
    }
    
    private void init(ApplicationContext context){
    	this.registerReporterContext(context);
		this.bulkProcessor = this.reporterContext.getBean(BulkProcessor.class);
		this.configuration = this.reporterContext.getBean(Configuration.class);
    }

}