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
package io.gravitee.reporter.elastic.mock;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.node.Node;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume Waignier (Zenika)
 * @author Sebastien Devaux (Zenika)
 *
 */
@Component
public class NodeMock implements Node {

    @Override
    public String name() {
        return this.name();
    }

    @Override
    public Lifecycle.State lifecycleState() {
        return null;
    }

    @Override
    public Node start() throws Exception {
        return this;
    }

    @Override
    public Node stop() throws Exception {
        return this;
    }

}
