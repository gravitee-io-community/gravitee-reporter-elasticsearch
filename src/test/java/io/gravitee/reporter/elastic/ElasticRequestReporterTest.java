package io.gravitee.reporter.elastic;

import java.net.URI;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.core.http.ServerRequest;
import io.gravitee.gateway.core.http.ServerResponse;
import io.gravitee.reporter.elastic.config.ReporterConfiguration;

/**
 * 
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 *
 */

public class ElasticRequestReporterTest {

	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	public static final String CONFIG_PATH = "/config/local-elastic.properties";
	
	public ElasticRequestReporter reporter; 

	@Before 
	public void init(){
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ReporterConfiguration.class);
		ctx.refresh();
		this.reporter = ctx.getBean(ElasticRequestReporter.class);
	}
	
	
	
	@Test
	public void singleReportTest(){

		
		try {

			for(int i=0 ; i< 10000; i++){
				ServerRequest request = new ServerRequest();
				request.setRequestURI(URI.create("/customers/"));
				request.setMethod(HttpMethod.GET);
				
				ServerResponse response = new ServerResponse();
				response.setStatus(200);
				
				reporter.report(request, response);
			}
			
			Thread.sleep(5000);
			
		} catch (Exception e) {
			
			LOGGER.error("Configuration loader error",e);
			Assert.fail("Error loading propertie configuration");
		}
	}
	
	
}
