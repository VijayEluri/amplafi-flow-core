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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.amplafi.flow.FlowGroup;
import org.amplafi.flow.FlowImplementor;
import org.amplafi.flow.FlowState;
import org.amplafi.flow.definitions.DefinitionSource;
import org.amplafi.flow.definitions.MapDefinitionSource;
import org.amplafi.flow.flowproperty.FlowPropertyProviderImplementor;
import org.amplafi.flow.flowproperty.PropertyScope;

public class FlowGroupImpl extends BaseFlowPropertyProvider<FlowImplementor> implements FlowGroup, FlowPropertyProviderImplementor {

    private static final List<PropertyScope> LOCAL_PROPERTY_SCOPES = Arrays.asList(PropertyScope.global);

    private DefinitionSource<FlowImplementor> definitionSource = new MapDefinitionSource<FlowImplementor>();

    private FlowGroup primaryFlowGroup;

    /**
     *
     */
    public FlowGroupImpl() {
        super();
    }

    public FlowGroupImpl(String flowPropertyProviderName) {
        super(flowPropertyProviderName);
    }

    /**
     * @param definition
     */
    public FlowGroupImpl(FlowImplementor definition) {
        super(definition);
    }

    /**
     * @see org.amplafi.flow.definitions.DefinitionSource#getFlowDefinition(java.lang.String)
     */
    public FlowImplementor getFlowDefinition(String flowTypeName) {
        return definitionSource.getFlowDefinition(flowTypeName);
    }

    /**
     * @see org.amplafi.flow.definitions.DefinitionSource#getFlowDefinitions()
     */
    public Map<String, FlowImplementor> getFlowDefinitions() {
        return definitionSource.getFlowDefinitions();
    }

    /**
     * @see org.amplafi.flow.definitions.DefinitionSource#isFlowDefined(java.lang.String)
     */
    public boolean isFlowDefined(String flowTypeName) {
        return definitionSource.isFlowDefined(flowTypeName);
    }

    @Override
    public <FS extends FlowState> FS getFlowState() {
        // flow groups do not have a FlowState.
        return null;
    }

    /**
     * @param primaryFlowGroup the primaryFlowGroup to set
     */
    public void setPrimaryFlowGroup(FlowGroup primaryFlowGroup) {
        this.primaryFlowGroup = primaryFlowGroup;
    }

    /**
     * @return the primaryFlowGroup
     */
    public FlowGroup getPrimaryFlowGroup() {
        return primaryFlowGroup;
    }

    /**
     * @return the definitionSource
     */
    public DefinitionSource<FlowImplementor> getDefinitionSource() {
        return definitionSource;
    }
    @Override
    protected List<PropertyScope> getLocalPropertyScopes() {
        return LOCAL_PROPERTY_SCOPES;
    }
}
