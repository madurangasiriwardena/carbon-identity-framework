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

package org.wso2.carbon.identity.stepup.flow.testing.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.stepup.flow.testing.executor.AccessTokenSeedExecutor;
import org.wso2.carbon.identity.stepup.flow.testing.executor.UnverifiedInputSeedExecutor;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Step-Up Flow Testing Service Component.
 * <p>
 * Registers the stand-in seeding executor used while the real one is being built. This bundle is not part of the
 * step-up feature and is meant to be dropped onto a development server by hand.
 */
@Component(
        name = "step.up.flow.testing.service.component",
        immediate = true
)
public class StepUpFlowTestingServiceComponent {

    private static final Log LOG = LogFactory.getLog(StepUpFlowTestingServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            context.getBundleContext()
                    .registerService(Executor.class.getName(), new AccessTokenSeedExecutor(), null);
            context.getBundleContext()
                    .registerService(Executor.class.getName(), new UnverifiedInputSeedExecutor(), null);
            LOG.warn("Step-up flow testing bundle activated. This bundle seeds flows from unverified input and "
                    + "belongs only on a development server.");
        } catch (Throwable e) {
            LOG.error("Error occurred while activating StepUpFlowTestingServiceComponent.", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Step-Up Flow Testing Service Component bundle is deactivated.");
        }
    }

    @Reference(
            name = "RealmService",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        StepUpFlowTestingDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        StepUpFlowTestingDataHolder.getInstance().setRealmService(null);
    }
}
