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

import org.wso2.carbon.identity.flow.execution.engine.graph.AuthenticationExecutor;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.mgt.model.ExecutorDTO;
import org.wso2.carbon.identity.flow.mgt.model.NodeConfig;

import java.util.Collections;
import java.util.List;

/**
 * Executor doubles shared by the tests of this component.
 */
public class TestExecutors {

    private TestExecutors() {

    }

    /**
     * Builds a completed node referring to the given executor.
     *
     * @param executorName Name of the executor the node ran.
     * @return The node configuration.
     */
    public static NodeConfig nodeFor(String executorName) {

        return new NodeConfig.Builder()
                .id(executorName + "_node")
                .executorConfig(new ExecutorDTO(executorName))
                .build();
    }

    /**
     * Stands in for an authenticator the user completes inside a flow.
     */
    public static class TestAuthenticationExecutor extends AuthenticationExecutor {

        private final String name;
        private final String amrValue;

        public TestAuthenticationExecutor(String name, String amrValue) {

            this.name = name;
            this.amrValue = amrValue;
        }

        @Override
        public String getAMRValue() {

            return amrValue;
        }

        @Override
        public String getName() {

            return name;
        }

        @Override
        public ExecutorResponse execute(FlowExecutionContext context) {

            return new ExecutorResponse();
        }

        @Override
        public List<String> getInitiationData() {

            return Collections.emptyList();
        }

        @Override
        public ExecutorResponse rollback(FlowExecutionContext context) {

            return new ExecutorResponse();
        }
    }

    /**
     * Stands in for an executor that does work other than authenticating the user.
     */
    public static class TestPlainExecutor implements Executor {

        private final String name;

        public TestPlainExecutor(String name) {

            this.name = name;
        }

        @Override
        public String getName() {

            return name;
        }

        @Override
        public ExecutorResponse execute(FlowExecutionContext context) {

            return new ExecutorResponse();
        }

        @Override
        public List<String> getInitiationData() {

            return Collections.emptyList();
        }

        @Override
        public ExecutorResponse rollback(FlowExecutionContext context) {

            return new ExecutorResponse();
        }
    }
}
