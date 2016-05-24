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
import io.gravitee.reporter.api.health.HealthStatus;
import io.gravitee.reporter.api.http.RequestMetrics;
import io.gravitee.reporter.api.monitor.JvmInfo;
import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.reporter.elastic.config.ElasticConfiguration;
import io.gravitee.reporter.elastic.engine.ReportEngine;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
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

	public AbstractElasticReportEngine() {
		this.sdf = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.systemDefault());
		this.dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
	}

	protected XContentBuilder getSource(RequestMetrics metrics) throws IOException {
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
				.field("endpoint", metrics.getEndpoint())
				.field(Fields.HOSTNAME, hostname)
				.field(Fields.SPECIAL_TIMESTAMP, Date.from(metrics.timestamp()), dtf)
				.endObject();
	}

	protected XContentBuilder getSource(HealthStatus healthStatus) throws IOException {
		return XContentFactory.jsonBuilder()
				.startObject()
				.field("api", healthStatus.getApi())
				.field("status", healthStatus.getStatus())
				.field("url", healthStatus.getUrl())
				.field("method", healthStatus.getMethod())
				.field("success", healthStatus.isSuccess())
				.field("message", healthStatus.getMessage())
				.field(Fields.HOSTNAME, hostname)
				.field(Fields.SPECIAL_TIMESTAMP, Date.from(healthStatus.timestamp()), dtf)
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
					builder.startObject(pool.getName(), XContentBuilder.FieldCaseConversion.NONE);
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
					builder.startObject(collector.getName(), XContentBuilder.FieldCaseConversion.NONE);
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

		builder.field(Fields.GATEWAY, monitor.gateway())
				.field(Fields.HOSTNAME, hostname)
				.field(Fields.SPECIAL_TIMESTAMP, Date.from(monitor.timestamp()), dtf)
				.endObject();

		return builder;
	}

	static final class Fields {
		static final XContentBuilderString GATEWAY = new XContentBuilderString("gateway");
		static final XContentBuilderString HOSTNAME = new XContentBuilderString("hostname");
		static final XContentBuilderString SPECIAL_TIMESTAMP = new XContentBuilderString("@timestamp");

		static final XContentBuilderString OS = new XContentBuilderString("os");
		static final XContentBuilderString TIMESTAMP = new XContentBuilderString("timestamp");
		static final XContentBuilderString CPU = new XContentBuilderString("cpu");
		static final XContentBuilderString PERCENT = new XContentBuilderString("percent");
		static final XContentBuilderString LOAD_AVERAGE = new XContentBuilderString("load_average");
		static final XContentBuilderString LOAD_AVERAGE_1M = new XContentBuilderString("1m");
		static final XContentBuilderString LOAD_AVERAGE_5M = new XContentBuilderString("5m");
		static final XContentBuilderString LOAD_AVERAGE_15M = new XContentBuilderString("15m");

		static final XContentBuilderString MEM = new XContentBuilderString("mem");
		static final XContentBuilderString SWAP = new XContentBuilderString("swap");
		static final XContentBuilderString FREE = new XContentBuilderString("free");
		static final XContentBuilderString FREE_IN_BYTES = new XContentBuilderString("free_in_bytes");
		static final XContentBuilderString USED = new XContentBuilderString("used");
		static final XContentBuilderString USED_IN_BYTES = new XContentBuilderString("used_in_bytes");
		static final XContentBuilderString TOTAL = new XContentBuilderString("total");
		static final XContentBuilderString TOTAL_IN_BYTES = new XContentBuilderString("total_in_bytes");

		static final XContentBuilderString FREE_PERCENT = new XContentBuilderString("free_percent");
		static final XContentBuilderString USED_PERCENT = new XContentBuilderString("used_percent");

		static final XContentBuilderString PROCESS = new XContentBuilderString("process");
		static final XContentBuilderString OPEN_FILE_DESCRIPTORS = new XContentBuilderString("open_file_descriptors");
		static final XContentBuilderString MAX_FILE_DESCRIPTORS = new XContentBuilderString("max_file_descriptors");

		static final XContentBuilderString TOTAL_IN_MILLIS = new XContentBuilderString("total_in_millis");

		static final XContentBuilderString TOTAL_VIRTUAL = new XContentBuilderString("total_virtual");
		static final XContentBuilderString TOTAL_VIRTUAL_IN_BYTES = new XContentBuilderString("total_virtual_in_bytes");

		static final XContentBuilderString JVM = new XContentBuilderString("jvm");
		static final XContentBuilderString UPTIME = new XContentBuilderString("uptime");
		static final XContentBuilderString UPTIME_IN_MILLIS = new XContentBuilderString("uptime_in_millis");

		static final XContentBuilderString HEAP_USED = new XContentBuilderString("heap_used");
		static final XContentBuilderString HEAP_USED_IN_BYTES = new XContentBuilderString("heap_used_in_bytes");
		static final XContentBuilderString HEAP_USED_PERCENT = new XContentBuilderString("heap_used_percent");
		static final XContentBuilderString HEAP_MAX = new XContentBuilderString("heap_max");
		static final XContentBuilderString HEAP_MAX_IN_BYTES = new XContentBuilderString("heap_max_in_bytes");
		static final XContentBuilderString HEAP_COMMITTED = new XContentBuilderString("heap_committed");
		static final XContentBuilderString HEAP_COMMITTED_IN_BYTES = new XContentBuilderString("heap_committed_in_bytes");

		static final XContentBuilderString NON_HEAP_USED = new XContentBuilderString("non_heap_used");
		static final XContentBuilderString NON_HEAP_USED_IN_BYTES = new XContentBuilderString("non_heap_used_in_bytes");
		static final XContentBuilderString NON_HEAP_COMMITTED = new XContentBuilderString("non_heap_committed");
		static final XContentBuilderString NON_HEAP_COMMITTED_IN_BYTES = new XContentBuilderString("non_heap_committed_in_bytes");

		static final XContentBuilderString POOLS = new XContentBuilderString("pools");
		static final XContentBuilderString MAX = new XContentBuilderString("max");
		static final XContentBuilderString MAX_IN_BYTES = new XContentBuilderString("max_in_bytes");
		static final XContentBuilderString PEAK_USED = new XContentBuilderString("peak_used");
		static final XContentBuilderString PEAK_USED_IN_BYTES = new XContentBuilderString("peak_used_in_bytes");
		static final XContentBuilderString PEAK_MAX = new XContentBuilderString("peak_max");
		static final XContentBuilderString PEAK_MAX_IN_BYTES = new XContentBuilderString("peak_max_in_bytes");

		static final XContentBuilderString THREADS = new XContentBuilderString("threads");
		static final XContentBuilderString COUNT = new XContentBuilderString("count");
		static final XContentBuilderString PEAK_COUNT = new XContentBuilderString("peak_count");

		static final XContentBuilderString GC = new XContentBuilderString("gc");
		static final XContentBuilderString COLLECTORS = new XContentBuilderString("collectors");
		static final XContentBuilderString COLLECTION_COUNT = new XContentBuilderString("collection_count");
		static final XContentBuilderString COLLECTION_TIME = new XContentBuilderString("collection_time");
		static final XContentBuilderString COLLECTION_TIME_IN_MILLIS = new XContentBuilderString("collection_time_in_millis");

		static final XContentBuilderString BUFFER_POOLS = new XContentBuilderString("buffer_pools");
		static final XContentBuilderString NAME = new XContentBuilderString("name");
		static final XContentBuilderString TOTAL_CAPACITY = new XContentBuilderString("total_capacity");
		static final XContentBuilderString TOTAL_CAPACITY_IN_BYTES = new XContentBuilderString("total_capacity_in_bytes");

		static final XContentBuilderString CLASSES = new XContentBuilderString("classes");
		static final XContentBuilderString CURRENT_LOADED_COUNT = new XContentBuilderString("current_loaded_count");
		static final XContentBuilderString TOTAL_LOADED_COUNT = new XContentBuilderString("total_loaded_count");
		static final XContentBuilderString TOTAL_UNLOADED_COUNT = new XContentBuilderString("total_unloaded_count");
	}

	protected String getIndexName(Reportable reportable){
		return String.format("%s-%s", configuration.getIndexName(), sdf.format(reportable.timestamp()));
	}
}
