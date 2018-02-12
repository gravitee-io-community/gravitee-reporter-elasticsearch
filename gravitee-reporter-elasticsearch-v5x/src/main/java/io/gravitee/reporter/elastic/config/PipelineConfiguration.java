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
package io.gravitee.reporter.elastic.config;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author Guillaume Gillon
 */
public class PipelineConfiguration {

    private final Logger LOGGER = LoggerFactory.getLogger(PipelineConfiguration.class);

    enum Processor {
        geoip("request", builder -> {
                return builder.append(
                        "{\"geoip\":{\"field\":\"remote-address\"}}," +
                        "{\"set\": {\"field\": \"geoip.city_name\",\"value\": \"Unknown\",\"override\": false}},"+
                                "{\"set\": {\"field\": \"geoip.continent_name\",\"value\": \"Unknown\",\"override\": false}}," +
                                "{\"set\": {\"field\": \"geoip.region_name\",\"value\": \"Unknown\",\"override\": false}}"
                );
        });

        private String indexType;
        private Function<StringBuilder, StringBuilder> contentBuilder;

        public final static List<String> ingestManaged = Arrays.stream(Processor.values()).map(Processor::name).collect(Collectors.toList());

        Processor(String indexType, Function<StringBuilder, StringBuilder> contentBuilder) {
            this.indexType = indexType;
            this.contentBuilder = contentBuilder;
        }
    }

    private final String pipeline = "gravitee_pipeline";

    private boolean valid = false;
    private boolean initialize = false;

    private static StringBuilder initPipeline() {
        StringBuilder stringBuilder = new StringBuilder();
        return stringBuilder.append("{\"description\":\"Gravitee pipeline\",\"processors\":[");
    }

    private static StringBuilder finisherPipeline(StringBuilder stringBuilder) {
        return stringBuilder.append("]}");
    }

    public XContentBuilder createPipeline() throws IOException {
        XContentBuilder template = null;


        StringBuilder stringBuilder = Processor.ingestManaged.stream()
                .map(processor -> Processor.valueOf(processor).contentBuilder)
                .collect(Collector.of(
                        PipelineConfiguration::initPipeline,
                        (response, processor) -> processor.apply(response),
                        StringBuilder::append,
                        PipelineConfiguration::finisherPipeline
                ));

        try (XContentParser parser =
                     XContentFactory.xContent(XContentType.JSON).createParser(stringBuilder.toString().getBytes())) {
            parser.nextToken();
            template = XContentFactory.jsonBuilder().copyCurrentStructure(parser);
        }

        return template;
    }

    public String getIngestManaged() {
        return Processor.ingestManaged.stream().collect(Collectors.joining(","));
    }

    public String getPipelineName() { return this.pipeline; }

    public String getPipeline() { return valid ? this.pipeline : null; }

    public void valid() {
        this.valid = true;
    }

    public boolean isValid() {
        return this.valid;
    }

    public void initialize() {
        this.initialize = true;
    }
    public boolean isInitialize() {
        return this.initialize;
    }
}
