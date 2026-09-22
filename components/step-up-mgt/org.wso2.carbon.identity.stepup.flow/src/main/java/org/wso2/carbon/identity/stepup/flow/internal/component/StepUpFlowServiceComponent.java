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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.stepup.flow.executor.OperationBranchExecutor;
import org.wso2.carbon.identity.stepup.flow.executor.OperationSelectionExecutor;
import org.wso2.carbon.identity.stepup.flow.executor.SecuredOperationCompletionExecutor;
import org.wso2.carbon.identity.stepup.flow.executor.SecuredOperationExecutor;
import org.wso2.carbon.identity.stepup.flow.executor.StepUpDecisionExecutor;
import org.wso2.carbon.identity.stepup.mgt.api.service.StepUpDecisionService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Step-Up Flow Service Component.
 */
@Component(
        name = "step.up.flow.service.component",
        immediate = true
)
public class StepUpFlowServiceComponent {

    private static final Log LOG = LogFactory.getLog(StepUpFlowServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(Executor.class.getName(), new OperationSelectionExecutor(), null);
            bundleContext.registerService(Executor.class.getName(), new OperationBranchExecutor(), null);
            bundleContext.registerService(Executor.class.getName(), new StepUpDecisionExecutor(), null);
            bundleContext.registerService(Executor.class.getName(), new SecuredOperationExecutor(), null);
            bundleContext.registerService(Executor.class.getName(), new SecuredOperationCompletionExecutor(), null);
            LOG.debug("Step-Up Flow Service Component bundle activated successfully.");
        } catch (Throwable e) {
            LOG.error("Error occurred while activating StepUpFlowServiceComponent.", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Step-Up Flow Service Component bundle is deactivated.");
        }
    }

    @Reference(
            name = "step.up.decision.service",
            service = StepUpDecisionService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetStepUpDecisionService")
    protected void setStepUpDecisionService(StepUpDecisionService stepUpDecisionService) {

        StepUpFlowComponentServiceHolder.getInstance().setStepUpDecisionService(stepUpDecisionService);
    }

    protected void unsetStepUpDecisionService(StepUpDecisionService stepUpDecisionService) {

        StepUpFlowComponentServiceHolder.getInstance().setStepUpDecisionService(null);
    }

    @Reference(
            name = "RealmService",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        StepUpFlowComponentServiceHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        StepUpFlowComponentServiceHolder.getInstance().setRealmService(null);
    }

    /**
     * Tracks the flow executors registered in the container.
     * <p>
     * The engine keeps its own registry, but it is internal to that bundle. This component needs the same view in
     * order to tell which of the nodes a flow completed were authentication factors the user actually performed.
     */
    @Reference(
            name = "flow.executor",
            service = Executor.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetExecutor")
    protected void setExecutor(Executor executor) {

        StepUpFlowComponentServiceHolder.getInstance().addExecutor(executor);
    }

    protected void unsetExecutor(Executor executor) {

        StepUpFlowComponentServiceHolder.getInstance().removeExecutor(executor);
    }
}
