/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.stepup.flow;

import org.wso2.carbon.identity.flow.mgt.model.ComponentDTO;
import org.wso2.carbon.identity.flow.mgt.model.DataDTO;
import org.wso2.carbon.identity.flow.mgt.model.GraphConfig;
import org.wso2.carbon.identity.flow.mgt.model.NodeConfig;
import org.wso2.carbon.identity.flow.mgt.model.NodeEdge;
import org.wso2.carbon.identity.flow.mgt.model.StepDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the shape of a flow the way the engine holds it, so the executors can be exercised against a graph rather
 * than against configuration they no longer read.
 */
public class TestGraphs {

    private final GraphConfig graph = new GraphConfig();

    public static TestGraphs builder() {

        return new TestGraphs();
    }

    /**
     * Adds a node with no page of its own.
     */
    public TestGraphs node(String id, String nextNodeId) {

        NodeConfig node = new NodeConfig.Builder().id(id).build();
        node.setNextNodeId(nextNodeId);
        graph.addNodeConfig(node);
        return this;
    }

    /**
     * Adds a node that fans out, with one edge per action.
     */
    public TestGraphs decision(String id, Map<String, String> actionToTarget) {

        NodeConfig node = new NodeConfig.Builder().id(id).build();
        actionToTarget.forEach((action, target) -> node.addEdge(new NodeEdge(id, target, action)));
        graph.addNodeConfig(node);
        return this;
    }

    /**
     * Adds a page that collects the given claims, nested inside a form the way a real definition nests them.
     */
    public TestGraphs page(String id, String nextNodeId, String... claims) {

        node(id, nextNodeId);
        List<ComponentDTO> fields = new ArrayList<>();
        for (String claim : claims) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("identifier", claim);
            fields.add(new ComponentDTO.Builder().id(id + "_field").type("INPUT").configs(configs).build());
        }
        ComponentDTO form = new ComponentDTO.Builder().id(id + "_form").type("FORM").components(fields).build();
        StepDTO step = new StepDTO.Builder()
                .id(id)
                .type("VIEW")
                .data(new DataDTO.Builder().components(Collections.singletonList(form)).build())
                .build();
        graph.addNodePageMapping(id, step);
        return this;
    }

    public GraphConfig build() {

        return graph;
    }
}
