package io.gravitee.reporter.elastic.factories;

import java.util.List;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import io.gravitee.reporter.elastic.config.Configuration;
import io.gravitee.reporter.elastic.model.TransportAddress;


public class ElasticClientFactory extends AbstractFactoryBean<Client> {

	@Autowired
	private Configuration config;

	@Override
	public Class<Client> getObjectType() {
		return Client.class;
	}
	
	@Override
	protected Client createInstance() throws Exception {
		TransportClient client = new TransportClient();

		List<TransportAddress> adresses = config.getTransportAddresses();

		for (TransportAddress address : adresses) {
			client.addTransportAddress(new InetSocketTransportAddress(address.getHostname(), address.getPort()));
		}
		return client;

	}
}