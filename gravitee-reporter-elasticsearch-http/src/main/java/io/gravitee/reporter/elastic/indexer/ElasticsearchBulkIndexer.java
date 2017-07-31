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
package io.gravitee.reporter.elastic.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.reporter.elastic.config.ElasticConfiguration;
import io.gravitee.reporter.elastic.config.Endpoint;
import io.gravitee.reporter.elastic.model.elasticsearch.Health;
import io.gravitee.reporter.elastic.model.exception.TechnicalException;
import io.gravitee.reporter.elastic.templating.freemarker.FreeMarkerComponent;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Utility Elasticsearch Spring bean used to call Elasticsearch using the REST
 * api.
 * 
 * @author Guillaume Waignier
 * @author Sebastien Devaux
 *
 */
public class ElasticsearchBulkIndexer {

	/** Logger. */
	private final Logger logger = LoggerFactory.getLogger(ElasticsearchBulkIndexer.class);

	// ES PATH
	private static final String URL_STATE_CLUSTER = "/_cluster/health";
	private static final String URL_TEMPLATE = "/_template";
	private static final String URL_BULK = "/_bulk";

	private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON + ";charset=UTF-8";

	private static final String HTTPS_SCHEME = "https";

	/**
	 * Configuration of Elasticsearch (cluster name, addresses, ...)
	 */
	@Autowired
	private ElasticConfiguration configuration;

	/**
	 * Templating tool.
	 */
	@Autowired
	private FreeMarkerComponent freeMarkerComponent;

	@Autowired
	private Vertx vertx;

	/**
	 * HTTP client.
	 */
	private HttpClient httpClient;

	/**
	 * JSON mapper.
	 */
	private ObjectMapper mapper;

	private PublishSubject<String> bulkObservable = PublishSubject.create();

	/**
	 * Authorization header if Elasticsearch is protected.
	 */
	private String authorizationHeader;

	private int majorVersion;

	/**
	 * Initialize the Async REST client.
	 */
	public void start() throws ExecutionException, InterruptedException, IOException, TechnicalException {
		if (! configuration.getEndpoints().isEmpty()) {
			final Endpoint endpoint = configuration.getEndpoints().get(0);
			final URI elasticEdpt = URI.create(endpoint.getUrl());

			HttpClientOptions options = new HttpClientOptions()
					.setDefaultHost(elasticEdpt.getHost())
					.setDefaultPort(elasticEdpt.getPort() != -1 ? elasticEdpt.getPort() :
							(HTTPS_SCHEME.equals(elasticEdpt.getScheme()) ? 443 : 80));

			if (HTTPS_SCHEME.equals(elasticEdpt.getScheme())) {
				options
						.setSsl(true)
						.setTrustAll(true);
			}

			this.httpClient = vertx.createHttpClient(options);

			this.mapper = new ObjectMapper();

			// Use the ElasticConfiguration class to define username and password
			// for ES
			// For example if Elasticsearch is protected by nginx
			if (this.configuration.getUsername() != null) {
				this.authorizationHeader = this.initEncodedAuthorization(this.configuration.getUsername(),
						this.configuration.getPassword());
			}

			try {
				this.majorVersion = getMajorVersion();
			} catch (Exception ex) {
				throw new TechnicalException("An error occurs while getting information from Elasticsearch at "
						+ elasticEdpt.toString(), ex);
			}

			this.ensureTemplate();

			bulkObservable
					.buffer(
							configuration.getFlushInterval(),
							TimeUnit.SECONDS,
							configuration.getBulkActions())
					.subscribe(this::index);
		}
	}

	/**
	 * Create the Basic HTTP auth
	 * 
	 * @param username
	 *            username
	 * @param password
	 *            password
	 * @return Basic auth string
	 */
	private String initEncodedAuthorization(final String username, final String password) {
		final String auth = username + ":" + password;
		final String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
		return "Basic " + encodedAuth;
	}

	private int getMajorVersion() throws ExecutionException, InterruptedException, IOException, TechnicalException {
		Observable<VertxHttpResponse> get = Observable.unsafeCreate(subscriber -> {
			HttpClientRequest req = httpClient.get("/");
			Observable<VertxHttpResponse> responseObservable = req
					.exceptionHandler(subscriber::onError)
					.toObservable()
					.flatMap(new Func1<HttpClientResponse, Observable<VertxHttpResponse>>() {
						@Override
						public Observable<VertxHttpResponse> call(HttpClientResponse httpClientResponse) {
							return Observable.unsafeCreate(subscriber1 -> httpClientResponse.bodyHandler(body -> {
								subscriber1.onNext(new VertxHttpResponse(httpClientResponse, body));
								subscriber1.onCompleted();
							}));
						}
					});

			responseObservable.subscribe(subscriber);
			req.end();
		});

		VertxHttpResponse single = get.toBlocking().single();

		if (single.response.statusCode() != HttpStatusCode.OK_200) {
			logger.error("Impossible to call Elasticsearch GET {}.", "/");
			throw new TechnicalException(
					"Impossible to call Elasticsearch. Elasticsearch response code is " + single.response.statusCode() );
		}

		String body = single.body.toString();

		String version = mapper.readTree(body).path("version").path("number").asText();
		float result = Float.valueOf(version.substring(0, 3));
		int major = Integer.valueOf(version.substring(0, 1));
		if (result < 2) {
			logger.warn("Please upgrade to Elasticsearch 2 or later. version={}", version);
		}

		return major;
	}

	/**
	 * Get the cluster health
	 * 
	 * @return the cluster health
	 * @throws TechnicalException
	 *             error occurs during ES call
	 */
	Health getClusterHealth() throws TechnicalException {
		try {
			Observable<VertxHttpResponse> get = Observable.unsafeCreate(subscriber -> {
				HttpClientRequest req = httpClient.get(URL_STATE_CLUSTER);
				Observable<VertxHttpResponse> responseObservable = req
						.exceptionHandler(subscriber::onError)
						.toObservable()
						.flatMap(new Func1<HttpClientResponse, Observable<VertxHttpResponse>>() {
							@Override
							public Observable<VertxHttpResponse> call(HttpClientResponse httpClientResponse) {
								return Observable.unsafeCreate(subscriber1 -> httpClientResponse.bodyHandler(body -> {
									subscriber1.onNext(new VertxHttpResponse(httpClientResponse, body));
									subscriber1.onCompleted();
								}));
							}
						});

				responseObservable.subscribe(subscriber);
				req.end();
			});

			VertxHttpResponse single = get.toBlocking().single();

			if (single.response.statusCode() != HttpStatusCode.OK_200) {
				logger.error("Impossible to call Elasticsearch GET {}.", URL_STATE_CLUSTER);
				throw new TechnicalException(
						"Impossible to call Elasticsearch. Elasticsearch response code is " + single.response.statusCode() );
			}

			String body = single.body.toString();
			logger.debug("Response of ES for GET {} : {}", URL_STATE_CLUSTER, body);

			return this.mapper.readValue(body, Health.class);
		} catch (IOException e) {
			logger.error("Impossible to call Elasticsearch GET {}.", URL_STATE_CLUSTER, e);
			throw new TechnicalException("Impossible to call Elasticsearch. Error is " + e.getClass().getSimpleName(),
					e);

		}
	}

	/**
	 * Put the ES template.
	 * 
	 * @throws TechnicalException
	 *             when a problem occur during the http call
	 */
	private void ensureTemplate() throws TechnicalException {
		try {
			String templateUrl = URL_TEMPLATE + "/gravitee";

			final Map<String, Object> data = new HashMap<>();
			data.put("indexName", this.configuration.getIndexName());
			final String template = this.freeMarkerComponent.generateFromTemplate("index-template-es-" + this.majorVersion + "x.ftl", data);

			logger.debug("PUT template : {}", template);

			Observable<VertxHttpResponse> get = Observable.unsafeCreate(subscriber -> {
				HttpClientRequest req = httpClient.put(templateUrl);
				Observable<VertxHttpResponse> responseObservable = req
						.exceptionHandler(subscriber::onError)
						.toObservable()
						.flatMap(new Func1<HttpClientResponse, Observable<VertxHttpResponse>>() {
							@Override
							public Observable<VertxHttpResponse> call(HttpClientResponse httpClientResponse) {
								return Observable.unsafeCreate(subscriber1 -> httpClientResponse.bodyHandler(body -> {
									subscriber1.onNext(new VertxHttpResponse(httpClientResponse, body));
									subscriber1.onCompleted();
								}));
							}
						});

				responseObservable.subscribe(subscriber);
				req.end(template);
			});

			VertxHttpResponse single = get.toBlocking().single();

			String body = single.body.toString();

			if (single.response.statusCode() != HttpStatusCode.OK_200) {
				logger.error("Impossible to call Elasticsearch PUT {}. Body is {}", templateUrl, body);
				throw new TechnicalException("Impossible to call Elasticsearch PUT " + templateUrl
						+ ". Response is " + single.response.statusCode());
			}

			logger.debug("Response of ES for PUT {} : {}",  URL_TEMPLATE + "/gravitee", body);
		} catch (final Exception e) {
			logger.error("Impossible to call Elasticsearch", e);
			throw new TechnicalException("Impossible to call Elasticsearch.", e);
		}
	}

	public void index(final String data) {
		bulkObservable.onNext(data);
	}

	/**
	 * Send a bulk request to ES
	 * 
	 * @param data
	 *            the list of data to bulk index
	 */
	private void index(final List<String> data) {
		if (data != null && ! data.isEmpty()) {
			try {
				String bulk = data.stream().collect(Collectors.joining());

				logger.debug("Try to call POST {}, with body {}", URL_BULK, bulk);

				HttpClientRequest req = httpClient.post(URL_BULK);
				req.putHeader(HttpHeaders.CONTENT_TYPE, "application/x-ndjson");
				addCommonHeaders(req);

				req
						.toObservable()
						.flatMap(new Func1<HttpClientResponse, Observable<VertxHttpResponse>>() {
							@Override
							public Observable<VertxHttpResponse> call(HttpClientResponse httpClientResponse) {
								return Observable.unsafeCreate(subscriber1 -> httpClientResponse.bodyHandler(body -> {
									subscriber1.onNext(new VertxHttpResponse(httpClientResponse, body));
									subscriber1.onCompleted();
								}));
							}
						})
						.subscribe(new Subscriber<VertxHttpResponse>() {
							@Override
							public void onCompleted() {

							}

							@Override
							public void onError(Throwable t) {
								logger.error("An error occurs while calling Elasticsearch POST {}", URL_BULK, t);
							}

							@Override
							public void onNext(VertxHttpResponse response) {
								String body = response.body.toString();

								logger.debug("Response of ES for POST {} : {}", URL_BULK, body);

								if (response.response.statusCode() != HttpStatusCode.OK_200) {
									logger.error("Impossible to call Elasticsearch POST {}. Body is {}", URL_BULK, body);
								}
							}
						});

				req.end(bulk);
			} catch (Exception ex) {
				logger.error("Unexpected error while bulk indexing data to Elasticsearch", ex);
			}
		}
	}

	/**
	 * Add the common header to call Elasticsearch.
	 * 
	 * @param request
	 *            the HTTP Client request
	 * @return HTTP Client request
	 */
	private void addCommonHeaders(final HttpClientRequest request) {
		request
				.putHeader(HttpHeaders.ACCEPT, CONTENT_TYPE)
				.putHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());

		// basic auth
		if (this.authorizationHeader != null) {
			request.putHeader(HttpHeaders.AUTHORIZATION, this.authorizationHeader);
		}
	}

	class VertxHttpResponse {
		final HttpClientResponse response;
		final Buffer             body;
		VertxHttpResponse( HttpClientResponse theResponse, Buffer theBody ) {
			response = theResponse;
			body = theBody;
		}
	}
}