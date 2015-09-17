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

import io.gravitee.common.http.GraviteeHttpHeader;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.reporter.elastic.config.Config;
import io.gravitee.reporter.elastic.engine.ReportEngine;
import org.elasticsearch.common.joda.time.format.DateTimeFormat;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;


public abstract class AbstractElasticReportEngine implements ReportEngine {

	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private Config configuration;

	/** Index simple date format **/
	private SimpleDateFormat sdf;

	/** Document simple date format **/
	private  DateTimeFormatter dtf;

	public AbstractElasticReportEngine(){
		this.sdf = new SimpleDateFormat("yyyy.MM.dd");
		this.sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

		this.dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
	}

	protected XContentBuilder getSource(Request request, Response response) throws IOException{
		return XContentFactory.jsonBuilder()
				.startObject()
				.field("id", request.id())
				.field("uri", request.uri())
				.field("path", request.path())
				.field("status", response.status())
				.field("method", request.method().toString())
				.field("request-content-type", request.headers().contentType())
				.field("response-content-type", response.headers().contentType())
				.field("request-content-length", request.headers().contentLength() >= 0 ? request.headers().contentLength() : null)
				.field("response-content-length", response.headers().contentLength() >= 0 ? response.headers().contentLength() : null)
				.field("api-key", request.headers().getFirst(GraviteeHttpHeader.X_GRAVITEE_API_KEY))
				.field("api-name", request.headers().getFirst(GraviteeHttpHeader.X_GRAVITEE_API_NAME))
				.field("local-address", request.localAddress())
				.field("remote-address", request.remoteAddress())
				.field("hostname", InetAddress.getLocalHost().getHostName())
				.field("@timestamp", Date.from(request.timestamp()), dtf)
				.endObject();
	}

	protected String getIndexName(Request request){
		return String.format("%s-%s",configuration.getIndexName(), sdf.format(request.timestamp()));
	}
}
