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

import io.gravitee.reporter.elastic.mock.ConfigurationTest;
import io.gravitee.reporter.elastic.model.elasticsearch.Health;
import io.gravitee.reporter.elastic.model.exception.TechnicalException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Test the component that calls ES
 * 
 * @author Guillaume Waignier
 * @author Sebastien Devaux
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ConfigurationTest.class})
public class ElasticsearchBulkIndexerTest {
	
	/**
	 * ES
	 */
    @Autowired
    private ElasticsearchBulkIndexer elasticsearchBulkIndexer;

    /**
     * Test health
     * @throws TechnicalException
     */
    @Test
    public void testGenerateFromTemplateWithoutData() throws TechnicalException, InterruptedException, ExecutionException, IOException {
        elasticsearchBulkIndexer.start();

    	// do the call
    	final Health health = this.elasticsearchBulkIndexer.getClusterHealth();
    	
    	// assert
    	Assert.assertEquals("gravitee_test", health.getClusterName());
    }
}
