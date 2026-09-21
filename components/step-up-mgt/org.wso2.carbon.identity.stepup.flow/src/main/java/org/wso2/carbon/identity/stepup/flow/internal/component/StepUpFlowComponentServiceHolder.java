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

package org.wso2.carbon.identity.stepup.flow.internal.component;

import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.stepup.mgt.api.service.StepUpDecisionService;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Step-Up Flow Component Service Holder.
 */
public class StepUpFlowComponentServiceHolder {

    private static final StepUpFlowComponentServiceHolder INSTANCE = new StepUpFlowComponentServiceHolder();

    private final Map<String, Executor> executors = new ConcurrentHashMap<>();
    private StepUpDecisionService stepUpDecisionService;
    private RealmService realmService;

    private StepUpFlowComponentServiceHolder() {

    }

    public static StepUpFlowComponentServiceHolder getInstance() {

        return INSTANCE;
    }

    public StepUpDecisionService getStepUpDecisionService() {

        return stepUpDecisionService;
    }

    public void setStepUpDecisionService(StepUpDecisionService stepUpDecisionService) {

        this.stepUpDecisionService = stepUpDecisionService;
    }

    public RealmService getRealmService() {

        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    /**
     * The flow executors registered in the container, by name.
     * <p>
     * This component keeps its own view of them because the engine's registry is internal to its bundle. It is used
     * only to resolve a completed node back to the authenticator it ran, so that the factors performed inside a
     * flow can be told apart from the evidence the flow was seeded with.
     *
     * @return The registered executors.
     */
    public Map<String, Executor> getExecutors() {

        return Collections.unmodifiableMap(executors);
    }

    public void addExecutor(Executor executor) {

        if (executor != null && executor.getName() != null) {
            executors.put(executor.getName(), executor);
        }
    }

    public void removeExecutor(Executor executor) {

        if (executor != null && executor.getName() != null) {
            executors.remove(executor.getName());
        }
    }
}
