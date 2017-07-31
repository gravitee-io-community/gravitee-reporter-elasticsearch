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
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.reporter.elastic.config.ElasticConfiguration;
import io.gravitee.reporter.elastic.engine.ReportEngine;
import io.gravitee.reporter.elastic.indexer.ElasticsearchBulkIndexer;
import io.gravitee.reporter.elastic.templating.freemarker.FreeMarkerComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Elasticsearch report engine. 
 * 
 * Use the POST http://_bulk url
 * 
 * @author Sebastien Devaux (Zenika)
 * @author Guillaume Waignier (Zenika)
 *
 */
public final class ElasticReportEngine implements ReportEngine {

	private final Logger LOGGER = LoggerFactory.getLogger(ElasticReportEngine.class);

	private final static String TYPE_REQUEST = "request";
	private final static String TYPE_HEALTH = "health";
	private final static String TYPE_MONITOR = "monitor";
	private final static String TYPE_LOG = "log";

	/**
	 * Component that aggregate lines into bulk request.
	 */
	@Autowired
	private ElasticsearchBulkIndexer elasticsearch;

	/**
	 * Configuration of Elasticsearch.
	 */
	@Autowired
	private ElasticConfiguration configuration;

	/**
	 * Templating tool.
	 */
	@Autowired
	private FreeMarkerComponent freeMarkerComponent;

	@Autowired
	private Node node;

	/** Index simple date format **/
	private DateTimeFormatter sdf;
	private DateTimeFormatter dtf;

	private static String hostname;

	static {
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = "unknown";
		}
	}

	public ElasticReportEngine() {
		this.sdf = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.systemDefault());
		this.dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS[XXX]").withZone(ZoneId.systemDefault());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void report(Reportable reportable) {
		Observable
				.just(reportable)
				.flatMap(reportable1 -> {
                    if (reportable1 instanceof Metrics) {
                        Metrics metrics = (Metrics) reportable1;

                        if (metrics.getLog() != null) {
                            return Observable.just(
                                    getSource(metrics),
                                    getSource(metrics.getLog())
                            );
                        } else {
                            return Observable.just(getSource(metrics));
                        }
                    } else if (reportable1 instanceof EndpointStatus) {
                        return Observable.just(getSource((EndpointStatus) reportable1));
                    } else if (reportable1 instanceof Monitor) {
                        return Observable.just(getSource((Monitor) reportable1));
                    }

                    return Observable.never();
                })
				.forEach(data -> elasticsearch.index(data));
	}

	/**
	 * Convert a {@link Metrics} into an ES bulk line.
	 *
	 * @param metrics A request metrics
	 * @return ES bulk line
	 */
	private String getSource(final Metrics metrics) {
		final Map<String, Object> data = new HashMap<>();

		data.put("index", this.getIndexName(metrics));
		data.put("documentType", TYPE_REQUEST);
		data.put("metrics", metrics);

		data.put(Fields.SPECIAL_TIMESTAMP, dtf.format(metrics.timestamp()));
		data.put(Fields.GATEWAY, node.id());
		data.put("apiResponseTime", metrics.getApiResponseTimeMs() >= 0 ? metrics.getApiResponseTimeMs() : null);
		data.put("proxyLatency", metrics.getProxyLatencyMs() >= 0 ? metrics.getProxyLatencyMs() : null);
		data.put("requestContentLength", metrics.getRequestContentLength() >= 0 ? metrics.getRequestContentLength() : null);
		data.put("responseContentLength", metrics.getResponseContentLength() >= 0 ? metrics.getResponseContentLength() : null);

		return freeMarkerComponent.generateFromTemplate("request.ftl", data);
	}

	/**
	 * Convert a {@link io.gravitee.reporter.api.log.Log} into an ES bulk line.
	 *
	 * @param log A request log
	 * @return ES bulk line
	 */
	private String getSource(final Log log) {
		final Map<String, Object> data = new HashMap<>();

		data.put("index", this.getIndexName(log));
		data.put("documentType", TYPE_LOG);

		data.put("log", log);
		data.put(Fields.SPECIAL_TIMESTAMP, dtf.format(log.timestamp()));
		data.put(Fields.GATEWAY, node.id());

		data.put("clientRequest", log.getClientRequest());
		data.put("clientResponse", log.getClientResponse());
		data.put("proxyRequest", log.getProxyRequest());
		data.put("proxyResponse", log.getProxyResponse());

		return freeMarkerComponent.generateFromTemplate("log.ftl", data);
	}

	/**
	 * Convert a {@link EndpointStatus} into an ES bulk line.
	 * @param endpointStatus the healthStatus
	 * @return ES bulk line
	 */
	private String getSource(final EndpointStatus endpointStatus) {
		final Map<String, Object> data = new HashMap<>();

		data.put("index", this.getIndexName(endpointStatus));
		data.put("documentType", TYPE_HEALTH);
		data.put("status", endpointStatus);
		data.put(Fields.GATEWAY, this.node.id());
		data.put(Fields.SPECIAL_TIMESTAMP, dtf.format(endpointStatus.timestamp()));

		return freeMarkerComponent.generateFromTemplate("health.ftl", data);
	}

	/**
	 * Convert a monitor into a ES bulk line.
	 * @param monitor the monitor metric
	 * @return ES bulk line
	 */
	private String getSource(final Monitor monitor) {
		final Map<String, Object> data = new HashMap<>();

		data.put("index", this.getIndexName(monitor));
		data.put("documentType", TYPE_MONITOR);

		data.put(Fields.HOSTNAME, hostname);
		data.put(Fields.SPECIAL_TIMESTAMP, dtf.format(monitor.timestamp()));
		data.put(Fields.GATEWAY, node.id());

		if (monitor.getOs() != null) {

			if (monitor.getOs().cpu != null) {
				data.put(Fields.PERCENT, monitor.getOs().cpu.getPercent());

				if (monitor.getOs().cpu.getLoadAverage() != null && Arrays.stream(monitor.getOs().cpu.getLoadAverage()).anyMatch(load -> load != -1)) {
					if (monitor.getOs().cpu.getLoadAverage()[0] != -1) {
						data.put(Fields.LOAD_AVERAGE_1M, monitor.getOs().cpu.getLoadAverage()[0]);
					}
					if (monitor.getOs().cpu.getLoadAverage()[1] != -1) {
						data.put(Fields.LOAD_AVERAGE_5M, monitor.getOs().cpu.getLoadAverage()[1]);
					}
					if (monitor.getOs().cpu.getLoadAverage()[2] != -1) {
						data.put(Fields.LOAD_AVERAGE_15M, monitor.getOs().cpu.getLoadAverage()[2]);
					}
				}
			}

			if (monitor.getOs().mem != null) {
				data.put("mem_" + Fields.TOTAL_IN_BYTES, monitor.getOs().mem.getTotal());
				data.put("mem_" + Fields.FREE_IN_BYTES, monitor.getOs().mem.getFree());
				data.put("mem_" + Fields.USED_IN_BYTES, monitor.getOs().mem.getUsed());
				data.put("mem_" + Fields.FREE_PERCENT, monitor.getOs().mem.getFreePercent());
				data.put("mem_" + Fields.USED_PERCENT, monitor.getOs().mem.getUsedPercent());
			}
		}

		if (monitor.getProcess() != null) {
			data.put("process_" + Fields.TIMESTAMP, monitor.getProcess().timestamp);
			data.put(Fields.OPEN_FILE_DESCRIPTORS, monitor.getProcess().openFileDescriptors);
			data.put(Fields.MAX_FILE_DESCRIPTORS, monitor.getProcess().maxFileDescriptors);
		}

		if (monitor.getJvm() != null) {
			data.put("jvm_" + Fields.TIMESTAMP, monitor.getJvm().timestamp);

			//TODO check timevalue
			data.put(Fields.UPTIME_IN_MILLIS, monitor.getJvm().uptime);

			if (monitor.getJvm().mem != null) {

				data.put(Fields.HEAP_USED_IN_BYTES, monitor.getJvm().mem.heapUsed);
				if (monitor.getJvm().mem.getHeapUsedPercent() >= 0) {
					data.put(Fields.HEAP_USED_PERCENT, monitor.getJvm().mem.getHeapUsedPercent());
				}
				data.put(Fields.HEAP_COMMITTED_IN_BYTES, monitor.getJvm().mem.heapCommitted);
				data.put(Fields.HEAP_MAX_IN_BYTES, monitor.getJvm().mem.heapMax);
				data.put(Fields.NON_HEAP_USED_IN_BYTES, monitor.getJvm().mem.nonHeapUsed);
				data.put(Fields.NON_HEAP_COMMITTED_IN_BYTES, monitor.getJvm().mem.nonHeapCommitted);

				data.put(Fields.POOLS, monitor.getJvm().mem.pools);
			}

			if (monitor.getJvm().threads != null) {
				data.put(Fields.COUNT, monitor.getJvm().threads.getCount());
				data.put(Fields.PEAK_COUNT, monitor.getJvm().threads.getPeakCount());
			}

			if (monitor.getJvm().gc != null) {
				data.put(Fields.COLLECTORS, monitor.getJvm().gc.collectors);
			}
		}

		return freeMarkerComponent.generateFromTemplate("monitor.ftl", data);
	}

	static final class Fields {
		static final String GATEWAY = "gateway";
		static final String HOSTNAME = "hostname";
		static final String SPECIAL_TIMESTAMP = "@timestamp";

		static final String TIMESTAMP = "timestamp";
		static final String PERCENT = "percent";
		static final String LOAD_AVERAGE_1M = "load_average_1m";
		static final String LOAD_AVERAGE_5M = "load_average_5m";
		static final String LOAD_AVERAGE_15M = "load_average_15m";

		static final String FREE_IN_BYTES = "free_in_bytes";
		static final String USED_IN_BYTES = "used_in_bytes";
		static final String TOTAL_IN_BYTES = "total_in_bytes";

		static final String FREE_PERCENT = "free_percent";
		static final String USED_PERCENT = "used_percent";

		static final String OPEN_FILE_DESCRIPTORS = "open_file_descriptors";
		static final String MAX_FILE_DESCRIPTORS = "max_file_descriptors";

		static final String UPTIME_IN_MILLIS = "uptime_in_millis";

		static final String HEAP_USED_IN_BYTES = "heap_used_in_bytes";
		static final String HEAP_USED_PERCENT = "heap_used_percent";
		static final String HEAP_MAX_IN_BYTES = "heap_max_in_bytes";
		static final String HEAP_COMMITTED_IN_BYTES = "heap_committed_in_bytes";

		static final String NON_HEAP_USED_IN_BYTES = "non_heap_used_in_bytes";
		static final String NON_HEAP_COMMITTED_IN_BYTES = "non_heap_committed_in_bytes";

		static final String POOLS = "pools";

		static final String COUNT = "count";
		static final String PEAK_COUNT = "peak_count";

		static final String COLLECTORS = "collectors";

	}

	/**
	 * Create the ES index name given the timestamp inside the metric.
	 * @param reportable the metric
	 * @return the ES index name
	 */
	private String getIndexName(Reportable reportable){
		return String.format("%s-%s", configuration.getIndexName(), sdf.format(reportable.timestamp()));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() throws Exception {
		LOGGER.info("Starting Elastic reporter engine...");
		
		this.elasticsearch.start();
		
		LOGGER.info("Starting Elastic reporter engine... DONE");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() {
		LOGGER.info("Stopping Elastic reporter engine...");

		//this.elasticsearch.stop();

		LOGGER.info("Stopping Elastic reporter engine... DONE");
	}
}
