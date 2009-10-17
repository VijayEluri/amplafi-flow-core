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

import java.util.LinkedHashMap;
import java.util.Map;

import org.amplafi.flow.FlowPropertyDefinition;
import org.amplafi.flow.flowproperty.FlowPropertyProvider;

/**
 * @author patmoore
 *
 */
public abstract class BaseFlowPropertyProvider<T extends FlowPropertyProvider> implements FlowPropertyProvider {

    private T definition;


    private Map<String, FlowPropertyDefinition> propertyDefinitions;

    public BaseFlowPropertyProvider() {
    }
    /**
     * @param definition
     */
    public BaseFlowPropertyProvider(T definition) {
        this.definition = definition;
    }

    public boolean isInstance() {
        return this.definition != null;
    }

    protected T getDefinition() {
        return definition;
    }

    protected void setDefinition(T definition) {
        this.definition = definition;
    }

    public void setPropertyDefinitions(Map<String, FlowPropertyDefinition> properties) {
        this.propertyDefinitions = properties;
    }

    public Map<String, FlowPropertyDefinition> getPropertyDefinitions() {
        if ( propertyDefinitions == null && this.isInstance() ) {
            // as is usually the case for instance flow activities.
            return this.definition.getPropertyDefinitions();
        }
        return propertyDefinitions;
    }
    /**
     * method used by hivemind to add in properties
     * @see org.amplafi.flow.flowproperty.FlowPropertyProvider#addPropertyDefinition(org.amplafi.flow.FlowPropertyDefinition)
     */
    public void addPropertyDefinition(FlowPropertyDefinition flowPropertyDefinition) {
        if ( flowPropertyDefinition == null ) {
            return;
        }
        if ( this.propertyDefinitions == null ) {
            if ( isInstance()) {
                this.propertyDefinitions =
                    new LinkedHashMap<String, FlowPropertyDefinition>();
                if ( this.definition.getPropertyDefinitions() != null) {
                    this.propertyDefinitions.putAll(this.definition.getPropertyDefinitions());
                }
            } else {
                this.propertyDefinitions = new LinkedHashMap<String, FlowPropertyDefinition>();
            }
        }
        FlowPropertyDefinition current = this.propertyDefinitions.get(flowPropertyDefinition.getName());
        if ( current != null ) {
            if ( !flowPropertyDefinition.merge(current) ) {
                throw new IllegalArgumentException(flowPropertyDefinition+": cannot be merged with "+current);
            }
        }
        this.propertyDefinitions.put(flowPropertyDefinition.getName(), flowPropertyDefinition);
    }
    public void addPropertyDefinitions(Iterable<FlowPropertyDefinition> flowPropertyDefinitions) {
        if ( flowPropertyDefinitions != null ) {
            for (FlowPropertyDefinition flowPropertyDefinition : flowPropertyDefinitions) {
                addPropertyDefinition(flowPropertyDefinition);
            }
        }
    }
    public void addPropertyDefinitions(FlowPropertyDefinition... flowPropertyDefinitions) {
        if ( flowPropertyDefinitions != null && flowPropertyDefinitions.length > 0) {
            for(FlowPropertyDefinition flowPropertyDefinition: flowPropertyDefinitions) {
                this.addPropertyDefinition(flowPropertyDefinition);
            }
        }
    }
}