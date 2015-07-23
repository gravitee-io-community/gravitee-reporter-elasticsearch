package io.gravitee.reporter.elastic.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import io.gravitee.gateway.api.reporters.RequestResporter;
import io.gravitee.reporter.elastic.ElasticRequestReporter;
import io.gravitee.reporter.elastic.factories.ElasticBulkProcessorFactory;
import io.gravitee.reporter.elastic.factories.ElasticClientFactory;

@Configuration
public class ReporterConfiguration {
  
	@Bean
	public RequestResporter requestReporter(){
		return new ElasticRequestReporter();
	}
	
	@Bean
    public ElasticClientFactory elasticClientFactory() {
        return new ElasticClientFactory();
    }
	
    @Bean
    public ElasticBulkProcessorFactory elasticBulkProcessorFactory() {
        return new ElasticBulkProcessorFactory();
    }
  
    
    @Bean 
    public io.gravitee.reporter.elastic.config.Configuration configuration(){
    	return new io.gravitee.reporter.elastic.config.Configuration();
    }
    
    private final static String REPORTER_CONFIGURATION = "gravitee-reporter-es.conf";
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();

        String yamlConfiguration = System.getProperty(REPORTER_CONFIGURATION);
        Resource yamlResource = null;

        if (yamlConfiguration == null || yamlConfiguration.isEmpty()) {
            // Load the default (empty) configuration just to avoid undefined bean with Spring
            yamlResource = new ClassPathResource("/gravitee-reporter-es.yml");
        } else {
            yamlResource = new FileSystemResource(yamlConfiguration);
        }

        yaml.setResources(yamlResource);
        propertySourcesPlaceholderConfigurer.setProperties(yaml.getObject());
        return propertySourcesPlaceholderConfigurer;
    }
}
