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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;

import java.util.Map;

import static org.mockito.Mockito.when;

/**
 *
 * @author Guillaume Gillon
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class PipelineConfigurationTest {

    @Mock
    private ElasticConfiguration config;

    @Mock
    private FreeMarkerComponent freeMarkerComponent;

    @InjectMocks
    private PipelineConfiguration pipelineConfiguration;

    @Before
    public void init() {
        //MockitoAnnotations.initMocks(this);

        when(config.getPipelineName()).thenReturn("gravitee_pipeline");
        when(config.getPipelineName()).thenReturn("pipeline_id");

        when(freeMarkerComponent.generateFromTemplate("geoip.ftl"))
                .thenReturn("{\"geoip\" : {\"field\" : \"remote-address\"}}");
        Map<String,Object> processorsMap = new HashMap<>(1);
        processorsMap.put("processors", freeMarkerComponent.generateFromTemplate("geoip.ftl"));
        when(freeMarkerComponent.generateFromTemplate("pipeline.ftl",processorsMap)).thenReturn("{\"description\":\"Gravitee pipeline\",\"processors\":[{\"geoip\":{\"field\":\"remote-address\"}}]}");

    }

    @Test
    public void Should_no_valid_pipeline_with_ingest_test() {
        when(config.getIngestPlugins()).thenReturn(Arrays.asList("test"));

        String pipeline = pipelineConfiguration.createPipeline(5);

        Assert.assertNull(pipeline);
        Assert.assertNull(pipelineConfiguration.getPipeline());
    }

    @Test
    public void should_not_valid_pipeline_with_ingest_geoip_and_version_4() {
        when(config.getIngestPlugins()).thenReturn(Arrays.asList("geoip"));

        String pipeline = pipelineConfiguration.createPipeline(4);

        Assert.assertNull(pipeline);
    }

    @Test
    public void should_valid_pipeline_with_ingest_geoip() {
        String result = "{\"description\":\"Gravitee pipeline\",\"processors\":[{\"geoip\":{\"field\":\"remote-address\"}}]}";


        when(config.getIngestPlugins()).thenReturn(Arrays.asList("geoip"));

        String pipeline = pipelineConfiguration.createPipeline(5);

        Assert.assertEquals(result, pipeline);
    }

    @Test
    public void should_return_pipeline_name() {
        //String result = "{\"description\":\"Gravitee pipeline\",\"processors\":[{\"geoip\":{\"field\":\"remote-address\"}}]}";


        when(config.getIngestPlugins()).thenReturn(Arrays.asList("geoip"));
        when(config.getPipelineName()).thenReturn("pipeline_id");

        String builder2 = pipelineConfiguration.createPipeline(5);

        Assert.assertEquals("pipeline_id", pipelineConfiguration.getPipeline());
    }
}
