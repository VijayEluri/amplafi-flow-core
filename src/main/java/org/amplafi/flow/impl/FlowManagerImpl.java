/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */

package org.amplafi.flow.impl;

import java.net.URI;
import org.amplafi.flow.FlowTranslatorResolver;
import org.amplafi.flow.Flow;
import org.amplafi.flow.FlowDefinitionsManager;
import org.amplafi.flow.FlowManagement;
import org.amplafi.flow.FlowManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 *
 */
public class FlowManagerImpl implements FlowManager {
    private FlowTranslatorResolver flowTranslatorResolver;
    private FlowDefinitionsManager flowDefinitionsManager;
    private Log log;
    private URI defaultHomePage;

    /**
     * @see org.amplafi.flow.FlowManager#getInstanceFromDefinition(java.lang.String)
     */
    @Override
    public Flow getInstanceFromDefinition(String flowTypeName) {
        Flow definition = flowDefinitionsManager.getFlowDefinition(flowTypeName);
        if (definition == null) {
            throw new IllegalArgumentException(flowTypeName + ": definition does not exist");
        }
        Flow inst = definition.createInstance();
        return inst;
    }

    public Log getLog() {
        if ( this.log == null ) {
            this.log = LogFactory.getLog(this.getClass());
        }
        return this.log;
    }

    /**
     * @see org.amplafi.flow.FlowManager#getFlowManagement()
     */
    @Override
    public FlowManagement getFlowManagement() {
        BaseFlowManagement baseFlowManagement = new BaseFlowManagement();
        baseFlowManagement.setFlowManager(this);
        baseFlowManagement.setFlowTranslatorResolver(getFlowTranslatorResolver());
        return baseFlowManagement;
    }

    /**
     * @see org.amplafi.flow.FlowDefinitionsManager#isFlowDefined(java.lang.String)
     */
    @Override
    public boolean isFlowDefined(String flowTypeName) {
        return this.getFlowDefinitionsManager().isFlowDefined(flowTypeName);
    }
    /**
     * @see org.amplafi.flow.FlowDefinitionsManager#isFlowDefined(java.lang.String)
     */
    @Override
    public Flow getFlowDefinition(String flowTypeName) {
        return this.getFlowDefinitionsManager().getFlowDefinition(flowTypeName);
    }
    public void setFlowTranslatorResolver(FlowTranslatorResolver flowTranslatorResolver) {
        this.flowTranslatorResolver = flowTranslatorResolver;
    }
    public FlowTranslatorResolver getFlowTranslatorResolver() {
        return flowTranslatorResolver;
    }

    /**
     * @param defaultHomePage the defaultHomePage to set
     */
    public void setDefaultHomePage(URI defaultHomePage) {
        this.defaultHomePage = defaultHomePage;
    }

    /**
     * @return the defaultHomePage
     */
    public URI getDefaultHomePage() {
        return defaultHomePage;
    }

    /**
     * @param flowDefinitionsManager the flowDefinitionsManager to set
     */
    public void setFlowDefinitionsManager(FlowDefinitionsManager flowDefinitionsManager) {
        this.flowDefinitionsManager = flowDefinitionsManager;
    }

    /**
     * @return the flowDefinitionsManager
     */
    public FlowDefinitionsManager getFlowDefinitionsManager() {
        return flowDefinitionsManager;
    }
}