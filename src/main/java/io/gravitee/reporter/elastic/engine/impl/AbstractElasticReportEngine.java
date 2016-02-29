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
import io.gravitee.reporter.api.http.RequestMetrics;
import io.gravitee.reporter.api.monitor.HealthStatus;
import io.gravitee.reporter.elastic.config.ElasticConfiguration;
import io.gravitee.reporter.elastic.engine.ReportEngine;
import org.elasticsearch.common.joda.time.format.DateTimeFormat;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Date;
import java.time.ZoneId;

public abstract class AbstractElasticReportEngine implements ReportEngine {

	@Autowired
	private ElasticConfiguration configuration;

	/** Index simple date format **/
	private java.time.format.DateTimeFormatter sdf;

	/** Document simple date format **/
	private DateTimeFormatter dtf;

	public AbstractElasticReportEngine() {
		this.sdf = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.systemDefault());
		this.dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
	}

	protected XContentBuilder getSource(RequestMetrics metrics) throws IOException{
		return XContentFactory.jsonBuilder()
				.startObject()
				.field("id", metrics.getRequestId())
				.field("uri", metrics.getRequestUri())
				.field("path", metrics.getRequestPath())
				.field("status", metrics.getResponseHttpStatus())
				.field("method", metrics.getRequestHttpMethod().toString())
				.field("request-content-type", metrics.getRequestContentType())
				.field("response-time", metrics.getProxyResponseTimeMs())
				.field("api-response-time", metrics.getApiResponseTimeMs() >= 0 ? metrics.getApiResponseTimeMs() : null)
				.field("response-content-type", metrics.getResponseContentType())
				.field("request-content-length", metrics.getRequestContentLength() >= 0 ? metrics.getRequestContentLength() : null)
				.field("response-content-length", metrics.getResponseContentLength() >= 0 ? metrics.getResponseContentLength() : null)
				.field("api-key", metrics.getApiKey())
				.field("api", metrics.getApi())
				.field("application", metrics.getApplication())
				.field("local-address", metrics.getRequestLocalAddress())
				.field("remote-address", metrics.getRequestRemoteAddress())
				.field("hostname", InetAddress.getLocalHost().getHostName())
				.field("@timestamp", Date.from(metrics.timestamp()), dtf)
				.endObject();
	}

	protected XContentBuilder getSource(HealthStatus healthStatus) throws IOException{
		return XContentFactory.jsonBuilder()
				.startObject()
				.field("api", healthStatus.getApi())
				.field("status", healthStatus.getStatus())
				.field("hostname", InetAddress.getLocalHost().getHostName())
				.field("@timestamp", Date.from(healthStatus.timestamp()), dtf)
				.endObject();
	}

	protected String getIndexName(Reportable reportable){
		return String.format("%s-%s", configuration.getIndexName(), sdf.format(reportable.timestamp()));
	}
}
