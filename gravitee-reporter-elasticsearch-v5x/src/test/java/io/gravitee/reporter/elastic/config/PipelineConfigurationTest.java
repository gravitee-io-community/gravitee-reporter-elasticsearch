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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;

/**
 *
 * @author Guillaume Gillon
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class PipelineConfigurationTest {

    @Mock
    private ElasticConfiguration config;

    @InjectMocks
    private PipelineConfiguration pipelineConfiguration;

    @Before
    public void init() {

    }

    @Test
    public void should_valid_pipeline_with_ingest_geoip() throws IOException {
        //String result = "{\"description\":\"Gravitee pipeline\",\"processors\":[{\"geoip\":{\"field\":\"remote-address\"}}]}";

        XContentBuilder builder1 = XContentFactory.jsonBuilder()
                .startObject()
                .field("description", "Gravitee pipeline")
                .startArray("processors")
                .startObject()
                .startObject("geoip")
                .field("field", "remote-address")
                .endObject()
                .endObject()

                .startObject()
                .startObject("set")
                .field("field", "geoip.city_name")
                .field("value", "Unknown")
                .field("override", false)
                .endObject()
                .endObject()

                .startObject()
                .startObject("set")
                .field("field", "geoip.continent_name")
                .field("value", "Unknown")
                .field("override", false)
                .endObject()
                .endObject()

                .startObject()
                .startObject("set")
                .field("field", "geoip.region_name")
                .field("value", "Unknown")
                .field("override", false)
                .endObject()
                .endObject()

                .endArray()
                .endObject();

        XContentBuilder builder2 = pipelineConfiguration.createPipeline();

        Assert.assertEquals(builder1.string(), builder2.string());
    }

    @Test
    public void should_return_pipeline_null() throws IOException {
        //String result = "{\"description\":\"Gravitee pipeline\",\"processors\":[{\"geoip\":{\"field\":\"remote-address\"}}]}";

        XContentBuilder builder2 = pipelineConfiguration.createPipeline();

        Assert.assertNull(pipelineConfiguration.getPipeline());
    }

    @Test
    public void should_return_pipeline_not_null() throws IOException {
        //String result = "{\"description\":\"Gravitee pipeline\",\"processors\":[{\"geoip\":{\"field\":\"remote-address\"}}]}";

        XContentBuilder builder2 = pipelineConfiguration.createPipeline();
        pipelineConfiguration.valid();

        Assert.assertEquals("gravitee_pipeline", pipelineConfiguration.getPipeline());
    }
}
