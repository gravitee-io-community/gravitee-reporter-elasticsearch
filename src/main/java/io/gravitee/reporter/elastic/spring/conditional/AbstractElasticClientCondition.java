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
package io.gravitee.reporter.elastic.spring.conditional;

import io.gravitee.reporter.elastic.model.Protocol;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

abstract class AbstractElasticClientCondition implements Condition {

	private static final String PROTOCOL_CONFIG_KEY = "reporters.elastic.protocol";

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String sProtocol = context.getEnvironment().getProperty(PROTOCOL_CONFIG_KEY, Protocol.TRANSPORT.name());
		Protocol protocol = Protocol.getByName(sProtocol);

		return clientProtocol().equals(protocol);
	}

	abstract Protocol clientProtocol();
}
