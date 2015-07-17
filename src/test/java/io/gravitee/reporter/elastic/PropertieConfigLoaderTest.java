package io.gravitee.reporter.elastic;

import io.gravitee.reporter.elastic.config.Configuration;
import io.gravitee.reporter.elastic.config.loader.PropertieConfigLoader;
import io.gravitee.reporter.elastic.model.TransportAddress;

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
public class PropertieConfigLoaderTest {

	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	public static final String CONFIG_PATH = "/config/elastic.properties";
	
	@Test
	public void configLoaderTest(){
		
		String configPath = this.getClass().getResource(CONFIG_PATH).getPath();
		
		PropertieConfigLoader configLoader = new PropertieConfigLoader();

		try {
			Configuration config = configLoader.load(configPath);
			Assert.assertNotNull(config);
			
			List<TransportAddress> hosts = config.getTransportAddresses();
			Assert.assertNotNull(hosts);
			
			
		} catch (Exception e) {
			LOGGER.error("Configuration loader error",e);

			Assert.fail("Error loading propertie configuration");
		}
	}
}
