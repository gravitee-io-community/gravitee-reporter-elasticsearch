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
package io.gravitee.reporter.elastic.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.reporter.elastic.client.Client;
import io.gravitee.reporter.elastic.config.ElasticConfiguration;
import io.gravitee.reporter.elastic.config.Endpoint;
import io.gravitee.reporter.elastic.model.elasticsearch.Health;
import io.gravitee.reporter.elastic.model.elasticsearch.bulk.ESBulkResponse;
import io.gravitee.reporter.elastic.model.exception.TechnicalException;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClientRequest;
import io.vertx.reactivex.core.http.HttpClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpClient implements Client {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private static final String HTTPS_SCHEME = "https";
    private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON + ";charset=UTF-8";

    private static final String URL_ROOT = "/";
    private static final String URL_STATE_CLUSTER = "/_cluster/health";
    private static final String URL_BULK = "/_bulk";
    private static final String URL_TEMPLATE = "/_template";
    private static final String URL_INGEST = "/_ingest/pipeline";

    @Autowired
    private Vertx vertx;

    /**
     * Configuration of Elasticsearch (cluster name, addresses, ...)
     */
    @Autowired
    private ElasticConfiguration configuration;

    /**
     * HTTP client.
     */
    private io.vertx.reactivex.core.http.HttpClient httpClient;

    /**
     * The ES version.
     */
    private int version = -1;

    /**
     * Authorization header if Elasticsearch is protected.
     */
    private String authorizationHeader;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void prepare() throws Exception {
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

            // Read reporter configuration to authenticate calls to Elasticsearch (basic authentication only)
            if (this.configuration.getUsername() != null) {
                this.authorizationHeader = this.initEncodedAuthorization(this.configuration.getUsername(),
                        this.configuration.getPassword());
            }

            try {
                this.version = getVersion();
            } catch (Exception ex) {
                throw new TechnicalException("An error occurs while getting information from Elasticsearch at "
                        + elasticEdpt.toString(), ex);
            }
        }
    }

    public int getVersion() throws TechnicalException {
        if (this.version == -1) {
            HttpClientRequest req = httpClient.get(URL_ROOT);
            Response response = doRequest(req).blockingGet();

            if (response.response.statusCode() != HttpStatusCode.OK_200) {
                logger.error("Impossible to call Elasticsearch GET {}.", URL_ROOT);
                throw new TechnicalException(
                        "Impossible to call Elasticsearch. Elasticsearch response code is " + response.response.statusCode());
            }

            String body = response.body.toString();

            try {
                String version = mapper.readTree(body).path("version").path("number").asText();
                float result = Float.valueOf(version.substring(0, 3));
                this.version = Integer.valueOf(version.substring(0, 1));
                if (result < 2) {
                    logger.warn("Please upgrade to Elasticsearch 2 or later. version={}", version);
                }
            } catch (IOException ioe) {
                throw new TechnicalException(
                        "Unable to extract Elasticsearch version from response", ioe);
            }
        }

        return version;
    }

    /**
     * Get the cluster health
     *
     * @return the cluster health
     * @throws TechnicalException error occurs during ES call
     */
    public Single<Health> getClusterHealth() {
        HttpClientRequest req = httpClient.get(URL_STATE_CLUSTER);

        return doRequest(req)
                .flatMap(new Function<Response, SingleSource<? extends Health>>() {
                    @Override
                    public SingleSource<? extends Health> apply(Response response) throws Exception {
                        return Single.just(mapper.readValue(response.body(), Health.class));
                    }
                });
    }
    /*

            if (response.response.statusCode() != HttpStatusCode.OK_200) {
                logger.error("Impossible to call Elasticsearch GET {}.", URL_STATE_CLUSTER);
                throw new TechnicalException(
                        "Impossible to call Elasticsearch. Elasticsearch response code is " + response.response.statusCode() );
            }

            String body = response.body.toString();
            logger.debug("Response of ES for GET {} : {}", URL_STATE_CLUSTER, body);

            return this.mapper.readValue(body, Health.class);
        } catch (IOException e) {
            logger.error("Impossible to call Elasticsearch GET {}.", URL_STATE_CLUSTER, e);
            throw new TechnicalException("Impossible to call Elasticsearch. Error is " + e.getClass().getSimpleName(),
                    e);
        }
    }
    */

    @Override
    public Single<ESBulkResponse> bulk(final List<String> data) {
        if (data != null && !data.isEmpty()) {
            String content = data.stream().collect(Collectors.joining());

            HttpClientRequest req = httpClient.post(URL_BULK);
            req.putHeader(HttpHeaders.CONTENT_TYPE, "application/x-ndjson");

            return doRequest(req, content)
                    .flatMap(new Function<Response, SingleSource<? extends ESBulkResponse>>() {
                        @Override
                        public SingleSource<? extends ESBulkResponse> apply(Response response) throws Exception {
                            if (response.statusCode() != HttpStatusCode.OK_200) {
                                logger.error("Unable to bulk index data: status[{}], body[{}]",
                                        response.statusCode(), content);
                                return Single.error(new TechnicalException("Unable to bulk index data"));
                            }

                            return Single.just(mapper.readValue(response.body(), ESBulkResponse.class));
                        }
                    });
        }

        return Single.never();
    }

    @Override
    public Single<Boolean> putTemplate(String templateName, String template) {
        HttpClientRequest req = httpClient
                .put(URL_TEMPLATE + '/' + templateName)
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        return doRequest(req, template)
                .flatMap(new Function<Response, SingleSource<Boolean>>() {
                    @Override
                    public SingleSource<Boolean> apply(Response response) throws Exception {
                        if (response.statusCode() != HttpStatusCode.OK_200) {
                            logger.error("Unable to put template mapping: status[{}], template[{}]",
                                    response.statusCode(), template);
                            return Single.error(new TechnicalException("Unable to put template mapping"));
                        }

                        return Single.just(Boolean.TRUE);
                    }
                });
    }

    public Single<Response> post(final String url, final String body) {
        HttpClientRequest req = httpClient.post(url);
        req.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        return doRequest(req, body);
    }

    public Single<Response> put(final String url, final String body) {
        HttpClientRequest req = httpClient.put(url);
        req.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        return doRequest(req, body);
    }

    public Single<Response> get(final String url) {
        HttpClientRequest req = httpClient.get(url);

        return doRequest(req);
    }

    private Single<Response> doRequest(final HttpClientRequest request) {
        return doRequest(request, null);
    }

    private Single<Response> doRequest(final HttpClientRequest request, final String body) {
        logger.debug("Calling {} {}, with body {}", request.method(), request.absoluteURI(), body);
        addCommonHeaders(request);

        return Single.create(singleEmitter ->
                request
                        .exceptionHandler(singleEmitter::onError)
                        .toFlowable()
                        .doOnSubscribe(subscription -> {
                            if (body == null) {
                                request.end();
                            } else {
                                request.end(body);
                            }
                        })
                        .flatMapSingle(new Function<HttpClientResponse, Single<Response>>() {
                            @Override
                            public Single<Response> apply(HttpClientResponse resp) throws Exception {
                                return Single.create(sub -> {
                                    resp.exceptionHandler(new Handler<Throwable>() {
                                        @Override
                                        public void handle(Throwable throwable) {
                                            logger.error("An error occurs while calling Elasticsearch {} {}",
                                                    request.getRawMethod(), request.absoluteURI(), throwable);

                                            singleEmitter.onError(throwable);
                                        }
                                    });

                                    resp.bodyHandler(body -> singleEmitter.onSuccess(new Response(resp, body)));
                                });
                            }
                        })
                        .subscribe());
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

        // Basic authentication
        if (this.authorizationHeader != null) {
            request.putHeader(HttpHeaders.AUTHORIZATION, this.authorizationHeader);
        }
    }

    private class Response {
        final private HttpClientResponse response;
        final private Buffer body;

        Response(HttpClientResponse theResponse, Buffer theBody) {
            response = theResponse;
            body = theBody;
        }

        int statusCode() {
            return response.statusCode();
        }

        String body() {
            return body.toString();
        }
    }
}
