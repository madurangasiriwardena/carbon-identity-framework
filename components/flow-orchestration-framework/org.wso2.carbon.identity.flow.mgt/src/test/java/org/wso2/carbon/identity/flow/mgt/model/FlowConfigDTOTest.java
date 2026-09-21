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

package org.wso2.carbon.identity.flow.mgt.model;

import org.testng.annotations.Test;
import org.wso2.carbon.identity.flow.mgt.Constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests of {@link FlowConfigDTO}.
 */
public class FlowConfigDTOTest {

    @Test(description = "A null map means the caller supplied no completion configurations, which is what a flow "
            + "type that supports none looks like. It must not fail.")
    public void testNullCompletionConfigsAreIgnored() {

        FlowConfigDTO flowConfig = new FlowConfigDTO();

        flowConfig.addAllFlowCompletionConfigs((Map<String, String>) null);

        assertTrue(flowConfig.getAllFlowCompletionConfigs().isEmpty());
    }

    @Test(description = "A null map leaves configurations already present untouched.")
    public void testNullCompletionConfigsLeaveExistingUntouched() {

        FlowConfigDTO flowConfig = new FlowConfigDTO();
        flowConfig.addFlowCompletionConfig(Constants.FlowCompletionConfig.IS_AUTO_LOGIN_ENABLED, "true");

        flowConfig.addAllFlowCompletionConfigs((Map<String, String>) null);

        assertEquals(flowConfig.getFlowCompletionConfig(Constants.FlowCompletionConfig.IS_AUTO_LOGIN_ENABLED),
                "true");
    }

    @Test(description = "An empty map adds nothing.")
    public void testEmptyCompletionConfigsAddNothing() {

        FlowConfigDTO flowConfig = new FlowConfigDTO();

        flowConfig.addAllFlowCompletionConfigs(Collections.emptyMap());

        assertTrue(flowConfig.getAllFlowCompletionConfigs().isEmpty());
    }

    @Test(description = "A populated map is added.")
    public void testCompletionConfigsAreAdded() {

        FlowConfigDTO flowConfig = new FlowConfigDTO();
        Map<String, String> configs = new HashMap<>();
        configs.put(Constants.FlowCompletionConfig.IS_AUTO_LOGIN_ENABLED.getConfig(), "true");

        flowConfig.addAllFlowCompletionConfigs(configs);

        assertEquals(flowConfig.getFlowCompletionConfig(Constants.FlowCompletionConfig.IS_AUTO_LOGIN_ENABLED),
                "true");
    }
}
