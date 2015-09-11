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
package io.gravitee.reporter.elastic.conditional;

import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import io.gravitee.reporter.elastic.model.Protocol;

public class JestClientCondition extends AbstractPropertyCondition {
	
	private static final String PROTOCOL_CONFIG_KEY = "elastic.protocol";
	
	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		
		Protocol protocol = Protocol.getByName(properties.getProperty(PROTOCOL_CONFIG_KEY, Protocol.TRANSPORT.name()));
		
		return Protocol.HTTP.equals(protocol);
	}

}
