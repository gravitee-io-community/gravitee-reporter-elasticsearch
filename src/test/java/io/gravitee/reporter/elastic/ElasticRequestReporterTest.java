package io.gravitee.reporter.elastic;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.core.http.ServerRequest;
import io.gravitee.gateway.core.http.ServerResponse;
import io.gravitee.reporter.elastic.config.Configuration;
import io.gravitee.reporter.elastic.config.loader.PropertieConfigLoader;
import io.gravitee.reporter.elastic.model.TransportAddress;

import java.net.URI;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 *
 */
public class ElasticRequestReporterTest {

	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	public static final String CONFIG_PATH = "/config/local-elastic.properties";
	
	@Test
	public void singleReportTest(){

		try {
			String configPath = this.getClass().getResource(CONFIG_PATH).getPath();
			ElasticRequestReporter reporter = new ElasticRequestReporter(configPath);
			
			for(int i=0 ; i< 10000; i++){
				ServerRequest request = new ServerRequest();
				request.setRequestURI(URI.create("/customers/"));
				request.setMethod(HttpMethod.GET);
				
				ServerResponse response = new ServerResponse();
				response.setStatus(200);
				
				reporter.report(request, response);
			}
			
			
			
		} catch (Exception e) {
			
			LOGGER.error("Configuration loader error",e);
			Assert.fail("Error loading propertie configuration");
		}
	}
	
	
}
