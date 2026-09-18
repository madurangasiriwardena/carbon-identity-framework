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

package org.wso2.carbon.identity.stepup.mgt.internal.component;

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
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.stepup.mgt.api.service.StepUpDecisionService;
import org.wso2.carbon.identity.stepup.mgt.internal.service.impl.StepUpDecisionServiceImpl;

/**
 * Step-Up Management Service Component.
 */
@Component(
        name = "step.up.mgt.service.component",
        immediate = true
)
public class StepUpMgtServiceComponent {

    private static final Log LOG = LogFactory.getLog(StepUpMgtServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(StepUpDecisionService.class.getName(),
                    new StepUpDecisionServiceImpl(), null);
            LOG.debug("Step-Up Management Service Component bundle activated successfully.");
        } catch (Throwable e) {
            LOG.error("Error occurred while activating StepUpMgtServiceComponent.", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Step-Up Management Service Component bundle is deactivated.");
        }
    }

    @Reference(
            name = "configuration.manager",
            service = ConfigurationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetConfigurationManager")
    protected void setConfigurationManager(ConfigurationManager configurationManager) {

        StepUpMgtComponentServiceHolder.getInstance().setConfigurationManager(configurationManager);
    }

    protected void unsetConfigurationManager(ConfigurationManager configurationManager) {

        StepUpMgtComponentServiceHolder.getInstance().setConfigurationManager(null);
    }
}
