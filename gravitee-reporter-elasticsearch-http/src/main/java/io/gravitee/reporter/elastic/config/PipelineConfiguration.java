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

import io.gravitee.reporter.elastic.templating.freemarker.FreeMarkerComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Guillaume Gillon
 */
public class PipelineConfiguration {

    private final Logger LOGGER = LoggerFactory.getLogger(PipelineConfiguration.class);

    private static final List<String> ingestManaged = Collections.singletonList("geoip");

    private List<String> ingestPluginsValid;

    /**
     * Configuration of Elasticsearch
     */
    @Autowired
    private ElasticConfiguration configuration;

    /**
     * Templating tool.
     */
    @Autowired
    private FreeMarkerComponent freeMarkerComponent;

    private String pipeline;

    public String createPipeline(int majorVersion) {
        String template = null;

        List<String> ingestPlugins = this.configuration.getIngestPlugins();

        if (ingestPlugins == null) return null;
        if (majorVersion < 5) {
            LOGGER.error("Ingest is not managed for the elasticsearch version below 5");
            return null;
        }

        this.ingestPluginsValid = ingestPlugins.stream().filter(ingestManaged::contains).collect(Collectors.toList());

        if (ingestPlugins.size() != this.ingestPluginsValid.size()) {
            ingestPlugins.stream().filter(ingest -> !ingestManaged.contains(ingest))
                    .forEach(ingest -> LOGGER.error("Ingest {} is not managed in gravitee", ingest));
        }

        if (this.ingestPluginsValid != null && this.ingestPluginsValid.size() > 0) {
            String processors = this.ingestPluginsValid.stream()
                    .map(ingestPlug -> this.freeMarkerComponent.generateFromTemplate(ingestPlug + ".ftl"))
                    .collect(
                            Collectors.joining(","));

            Map<String,Object> processorsMap = new HashMap<>(1);
            processorsMap.put("processors", processors);
            template = this.freeMarkerComponent.generateFromTemplate("pipeline.ftl", processorsMap);

            this.pipeline = this.configuration.getPipelineName();
        }

        return template;
    }

    public List<String> getIngestPlugins() {
        return this.ingestPluginsValid;
    }

    public String getPipeline() {
        return this.pipeline;
    }

    public void removePipeline() {
        this.pipeline = null;
    }
}
