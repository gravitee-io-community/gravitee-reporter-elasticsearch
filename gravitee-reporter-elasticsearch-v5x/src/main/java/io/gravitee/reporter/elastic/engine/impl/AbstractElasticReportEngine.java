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

import io.gravitee.common.node.Node;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.health.Step;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.gravitee.reporter.api.monitor.JvmInfo;
import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.reporter.elastic.config.ElasticConfiguration;
import io.gravitee.reporter.elastic.engine.ReportEngine;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Date;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public abstract class AbstractElasticReportEngine implements ReportEngine {

	@Autowired
	private ElasticConfiguration configuration;

	@Autowired
	private Node node;

	/** Index simple date format **/
	private java.time.format.DateTimeFormatter sdf;

	/** Document simple date format **/
	private DateTimeFormatter dtf;

	private static String hostname;

	static {
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = "unknown";
		}
	}

	AbstractElasticReportEngine() {
		this.sdf = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.systemDefault());
		this.dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
	}

	XContentBuilder getSource(Metrics metrics) throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder()
				.startObject()
				.field(Fields.GATEWAY, node.id())
				.field(Fields.SPECIAL_TIMESTAMP, Date.from(metrics.timestamp()), dtf)
				.field("transaction", metrics.getTransactionId())
				.field("method", metrics.getHttpMethod().code())
				.field("uri", metrics.getUri())
				.field("status", metrics.getStatus())
				.field("response-time", metrics.getProxyResponseTimeMs())
				.field("api-response-time", metrics.getApiResponseTimeMs() >= 0 ? metrics.getApiResponseTimeMs() : null)
				.field("proxy-latency", metrics.getProxyLatencyMs() >= 0 ? metrics.getProxyLatencyMs() : null)
				.field("request-content-length", metrics.getRequestContentLength() >= 0 ? metrics.getRequestContentLength() : null)
				.field("response-content-length", metrics.getResponseContentLength() >= 0 ? metrics.getResponseContentLength() : null)
				.field("api-key", metrics.getApiKey())
				.field("plan", metrics.getPlan())
				.field("api", metrics.getApi())
				.field("application", metrics.getApplication())
				.field("local-address", metrics.getLocalAddress())
				.field("remote-address", metrics.getRemoteAddress())
				.field("endpoint", metrics.getEndpoint());

		if (metrics.getMessage() != null) {
			builder.field("message", metrics.getMessage());
		}

		if (metrics.getTenant() != null) {
			builder.field("tenant", metrics.getTenant());
		}

		return builder.endObject();
	}

	XContentBuilder getSource(Log log) throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder().startObject();

		if (log.getClientRequest() != null) {
			builder.startObject("client-request")
					.field("method", log.getClientRequest().getMethod())
					.field("uri", log.getClientRequest().getUri())
					.field("headers", log.getClientRequest().getHeaders())
					.field("body", log.getClientRequest().getBody())
					.endObject();
		}

		if (log.getProxyRequest() != null) {
			builder.startObject("proxy-request")
					.field("method", log.getProxyRequest().getMethod())
					.field("uri", log.getProxyRequest().getUri())
					.field("headers", log.getProxyRequest().getHeaders())
					.field("body", log.getProxyRequest().getBody())
					.endObject();
		}

		if (log.getClientResponse() != null) {
			builder.startObject("client-response")
					.field("status", log.getClientResponse().getStatus())
					.field("headers", log.getClientResponse().getHeaders())
					.field("body", log.getClientResponse().getBody())
					.endObject();
		}

		if (log.getProxyResponse() != null) {
			builder.startObject("proxy-response")
					.field("status", log.getProxyResponse().getStatus())
					.field("headers", log.getProxyResponse().getHeaders())
					.field("body", log.getProxyResponse().getBody())
					.endObject();
		}

		return builder
				.field(Fields.SPECIAL_TIMESTAMP, Date.from(log.timestamp()), dtf)
				.endObject();
	}

	XContentBuilder getSource(EndpointStatus endpointStatus) throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder()
				.startObject()
				.field(Fields.GATEWAY, node.id())
				.field("api", endpointStatus.getApi())
				.field("endpoint", endpointStatus.getEndpoint())
				.field("response-time", endpointStatus.getResponseTime())
				.field("available", endpointStatus.isAvailable())
				.field("success", endpointStatus.isSuccess())
				.field("state", endpointStatus.getState());

		// Add steps result
		builder.startArray("steps");
		for (Step step : endpointStatus.getSteps()) {
			builder.startObject()
					.field("name", step.getName())
					.field("success", step.isSuccess())
					.startObject("request")
						.field("method", step.getRequest().getMethod())
						.field("uri", step.getRequest().getUri())
						.field("headers", step.getRequest().getHeaders())
						.field("body", step.getRequest().getBody())
					.endObject()
					.startObject("response")
						.field("status", step.getResponse().getStatus())
						.field("headers", step.getResponse().getHeaders())
						.field("body", step.getResponse().getBody())
					.endObject()
					.field("response-time", step.getResponseTime())
					.field("message", step.getMessage())
					.endObject();
		}
		builder.endArray();

		return builder
				.field(Fields.SPECIAL_TIMESTAMP, Date.from(endpointStatus.timestamp()), dtf)
				.endObject();
	}

	protected XContentBuilder getSource(Monitor monitor) throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder().startObject();

		if (monitor.getOs() != null) {
			builder.startObject(Fields.OS);

			if (monitor.getOs().cpu != null) {
				builder.startObject(Fields.CPU);
				builder.field(Fields.PERCENT, monitor.getOs().cpu.getPercent());
				if (monitor.getOs().cpu.getLoadAverage() != null && Arrays.stream(monitor.getOs().cpu.getLoadAverage()).anyMatch(load -> load != -1)) {
					builder.startObject(Fields.LOAD_AVERAGE);
					if (monitor.getOs().cpu.getLoadAverage()[0] != -1) {
						builder.field(Fields.LOAD_AVERAGE_1M, monitor.getOs().cpu.getLoadAverage()[0]);
					}
					if (monitor.getOs().cpu.getLoadAverage()[1] != -1) {
						builder.field(Fields.LOAD_AVERAGE_5M, monitor.getOs().cpu.getLoadAverage()[1]);
					}
					if (monitor.getOs().cpu.getLoadAverage()[2] != -1) {
						builder.field(Fields.LOAD_AVERAGE_15M, monitor.getOs().cpu.getLoadAverage()[2]);
					}
					builder.endObject();
				}

				builder.endObject();
			}

			if (monitor.getOs().mem != null) {
				builder.startObject(Fields.MEM);
				builder.byteSizeField(Fields.TOTAL_IN_BYTES, Fields.TOTAL, new ByteSizeValue(monitor.getOs().mem.getTotal()));
				builder.byteSizeField(Fields.FREE_IN_BYTES, Fields.FREE, new ByteSizeValue(monitor.getOs().mem.getFree()));
				builder.byteSizeField(Fields.USED_IN_BYTES, Fields.USED, new ByteSizeValue(monitor.getOs().mem.getUsed()));

				builder.field(Fields.FREE_PERCENT, monitor.getOs().mem.getFreePercent());
				builder.field(Fields.USED_PERCENT, monitor.getOs().mem.getUsedPercent());

				builder.endObject();
			}

			builder.endObject();
		}

		if (monitor.getProcess() != null) {
			builder.startObject(Fields.PROCESS);
			builder.field(Fields.TIMESTAMP, monitor.getProcess().timestamp);
			builder.field(Fields.OPEN_FILE_DESCRIPTORS, monitor.getProcess().openFileDescriptors);
			builder.field(Fields.MAX_FILE_DESCRIPTORS, monitor.getProcess().maxFileDescriptors);
			builder.endObject();
		}

		if (monitor.getJvm() != null) {
			builder.startObject(Fields.JVM);
			builder.field(Fields.TIMESTAMP, monitor.getJvm().timestamp);
			builder.timeValueField(Fields.UPTIME_IN_MILLIS, Fields.UPTIME, monitor.getJvm().uptime);
			if (monitor.getJvm().mem != null) {
				builder.startObject(Fields.MEM);

				builder.byteSizeField(Fields.HEAP_USED_IN_BYTES, Fields.HEAP_USED, monitor.getJvm().mem.heapUsed);
				if (monitor.getJvm().mem.getHeapUsedPercent() >= 0) {
					builder.field(Fields.HEAP_USED_PERCENT, monitor.getJvm().mem.getHeapUsedPercent());
				}
				builder.byteSizeField(Fields.HEAP_COMMITTED_IN_BYTES, Fields.HEAP_COMMITTED,
						monitor.getJvm().mem.heapCommitted);
				builder.byteSizeField(Fields.HEAP_MAX_IN_BYTES, Fields.HEAP_MAX,
						monitor.getJvm().mem.heapMax);
				builder.byteSizeField(Fields.NON_HEAP_USED_IN_BYTES, Fields.NON_HEAP_USED,
						monitor.getJvm().mem.nonHeapUsed);
				builder.byteSizeField(Fields.NON_HEAP_COMMITTED_IN_BYTES, Fields.NON_HEAP_COMMITTED,
						monitor.getJvm().mem.nonHeapCommitted);

				builder.startObject(Fields.POOLS);
				for (JvmInfo.MemoryPool pool : monitor.getJvm().mem.pools) {
					builder.startObject(pool.getName());
					builder.byteSizeField(Fields.USED_IN_BYTES, Fields.USED, pool.used);
					builder.byteSizeField(Fields.MAX_IN_BYTES, Fields.MAX, pool.max);

					builder.byteSizeField(Fields.PEAK_USED_IN_BYTES, Fields.PEAK_USED, pool.peakUsed);
					builder.byteSizeField(Fields.PEAK_MAX_IN_BYTES, Fields.PEAK_MAX, pool.peakMax);

					builder.endObject();
				}
				builder.endObject();

				builder.endObject();
			}

			if (monitor.getJvm().threads != null) {
				builder.startObject(Fields.THREADS);
				builder.field(Fields.COUNT, monitor.getJvm().threads.getCount());
				builder.field(Fields.PEAK_COUNT, monitor.getJvm().threads.getPeakCount());
				builder.endObject();
			}

			if (monitor.getJvm().gc != null) {
				builder.startObject(Fields.GC);

				builder.startObject(Fields.COLLECTORS);
				for (JvmInfo.GarbageCollector collector : monitor.getJvm().gc.collectors) {
					builder.startObject(collector.getName());
					builder.field(Fields.COLLECTION_COUNT, collector.getCollectionCount());
					builder.timeValueField(Fields.COLLECTION_TIME_IN_MILLIS, Fields.COLLECTION_TIME,
							new TimeValue(collector.collectionTime, TimeUnit.MILLISECONDS));
					builder.endObject();
				}
				builder.endObject();

				builder.endObject();
			}

			builder.endObject();
		}

		builder.field(Fields.GATEWAY, node.id())
				.field(Fields.HOSTNAME, hostname)
				.field(Fields.SPECIAL_TIMESTAMP, Date.from(monitor.timestamp()), dtf)
				.endObject();

		return builder;
	}

	static final class Fields {
		static final String GATEWAY = "gateway";
		static final String HOSTNAME = "hostname";
		static final String SPECIAL_TIMESTAMP = "@timestamp";

		static final String OS = "os";
		static final String TIMESTAMP = "timestamp";
		static final String CPU = "cpu";
		static final String PERCENT = "percent";
		static final String LOAD_AVERAGE = "load_average";
		static final String LOAD_AVERAGE_1M = "1m";
		static final String LOAD_AVERAGE_5M = "5m";
		static final String LOAD_AVERAGE_15M = "15m";

		static final String MEM = "mem";
		static final String SWAP = "swap";
		static final String FREE = "free";
		static final String FREE_IN_BYTES = "free_in_bytes";
		static final String USED = "used";
		static final String USED_IN_BYTES = "used_in_bytes";
		static final String TOTAL = "total";
		static final String TOTAL_IN_BYTES = "total_in_bytes";

		static final String FREE_PERCENT = "free_percent";
		static final String USED_PERCENT = "used_percent";

		static final String PROCESS = "process";
		static final String OPEN_FILE_DESCRIPTORS = "open_file_descriptors";
		static final String MAX_FILE_DESCRIPTORS = "max_file_descriptors";

		static final String TOTAL_IN_MILLIS = "total_in_millis";

		static final String TOTAL_VIRTUAL = "total_virtual";
		static final String TOTAL_VIRTUAL_IN_BYTES = "total_virtual_in_bytes";

		static final String JVM = "jvm";
		static final String UPTIME = "uptime";
		static final String UPTIME_IN_MILLIS = "uptime_in_millis";

		static final String HEAP_USED = "heap_used";
		static final String HEAP_USED_IN_BYTES = "heap_used_in_bytes";
		static final String HEAP_USED_PERCENT = "heap_used_percent";
		static final String HEAP_MAX = "heap_max";
		static final String HEAP_MAX_IN_BYTES = "heap_max_in_bytes";
		static final String HEAP_COMMITTED = "heap_committed";
		static final String HEAP_COMMITTED_IN_BYTES = "heap_committed_in_bytes";

		static final String NON_HEAP_USED = "non_heap_used";
		static final String NON_HEAP_USED_IN_BYTES = "non_heap_used_in_bytes";
		static final String NON_HEAP_COMMITTED = "non_heap_committed";
		static final String NON_HEAP_COMMITTED_IN_BYTES = "non_heap_committed_in_bytes";

		static final String POOLS = "pools";
		static final String MAX = "max";
		static final String MAX_IN_BYTES = "max_in_bytes";
		static final String PEAK_USED = "peak_used";
		static final String PEAK_USED_IN_BYTES = "peak_used_in_bytes";
		static final String PEAK_MAX = "peak_max";
		static final String PEAK_MAX_IN_BYTES = "peak_max_in_bytes";

		static final String THREADS = "threads";
		static final String COUNT = "count";
		static final String PEAK_COUNT = "peak_count";

		static final String GC = "gc";
		static final String COLLECTORS = "collectors";
		static final String COLLECTION_COUNT = "collection_count";
		static final String COLLECTION_TIME = "collection_time";
		static final String COLLECTION_TIME_IN_MILLIS = "collection_time_in_millis";

		static final String BUFFER_POOLS = "buffer_pools";
		static final String NAME = "name";
		static final String TOTAL_CAPACITY = "total_capacity";
		static final String TOTAL_CAPACITY_IN_BYTES = "total_capacity_in_bytes";

		static final String CLASSES = "classes";
		static final String CURRENT_LOADED_COUNT = "current_loaded_count";
		static final String TOTAL_LOADED_COUNT = "total_loaded_count";
		static final String TOTAL_UNLOADED_COUNT = "total_unloaded_count";
	}

	protected String getIndexName(Reportable reportable){
		return String.format("%s-%s", configuration.getIndexName(), sdf.format(reportable.timestamp()));
	}
}
