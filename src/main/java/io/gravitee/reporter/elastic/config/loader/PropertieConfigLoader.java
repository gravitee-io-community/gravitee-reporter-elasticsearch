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
package io.gravitee.reporter.elastic.config.loader;

import io.gravitee.common.utils.PropertiesUtils;
import io.gravitee.reporter.elastic.config.Configuration;
import io.gravitee.reporter.elastic.model.Protocol;
import io.gravitee.reporter.elastic.model.TransportAddress;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load Elastic reporter configuration from propertie file.
 * 
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 *
 */
public class PropertieConfigLoader {

	private static final String HOSTS_SEPARATOR = ",";

	private static final String PORT_SEPARATOR = ":";

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	/**
	 * 
	 * @param configurationPath
	 * @return
	 * @throws Exception
	 */
	public Configuration load(String configurationPath) throws Exception {

		final Configuration config = new Configuration();

		try {
			final InputStream input = new FileInputStream(configurationPath);
			final Properties properties = new Properties();
			properties.load(input);

			config.setProtocol(Protocol.getByName(properties.getProperty("gravitee.io.elastic.protocol", Protocol.HTTP.name())));
			config.setClusterName(PropertiesUtils.getProperty(properties,"gravitee.io.elastic.cluster"));
			config.setWorkers(Integer.parseInt(properties.getProperty("gravitee.io.elastic.workers", "1")));

			final String serializedHosts = PropertiesUtils.getProperty( properties, "gravitee.io.elastic.hosts");
			config.setTransportAddresses(unmarshallHosts(serializedHosts, config.getProtocol().getDefaultPort()));
			
			return config;

		} catch (IOException e) {

			LOGGER.error("No Elasticsearch configuration can be read from {}", configurationPath, e);
			throw new Exception("Fail to load elastic configuration", e);
		}
	}

	/**
	 * Unmarshall hostes under the format "hostname1:port1, hostname2"
	 * 
	 * @param serializedHosts
	 * 			Serialized transport addresses
	 * @param defaultPort
	 * @return
	 */
	private List<TransportAddress> unmarshallHosts(String serializedHosts, Integer defaultPort) {

		List<TransportAddress> hosts = new ArrayList<TransportAddress>();
		String[] hostsParts = serializedHosts.split(HOSTS_SEPARATOR);

		for (String serializedHost : hostsParts) {

			TransportAddress host = null;

			if (serializedHost.contains(PORT_SEPARATOR)) {
				String[] hostParts = serializedHost.split(PORT_SEPARATOR);
				
				String hostname = hostParts[0].toLowerCase();
				Integer port = Integer.parseInt(hostParts[1].trim());
				
				host = new TransportAddress(hostname, port);
			} else {
				host = new TransportAddress(serializedHost.trim(), defaultPort);
			}
			hosts.add(host);
		}
		return hosts;

	}
}
