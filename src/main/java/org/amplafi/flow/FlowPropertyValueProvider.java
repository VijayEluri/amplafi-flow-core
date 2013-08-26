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

package org.amplafi.flow;

import org.amplafi.flow.flowproperty.FlowPropertyDefinitionProvider;
import org.amplafi.flow.flowproperty.FlowPropertyProvider;
import org.amplafi.flow.flowproperty.FlowPropertyValuePersister;

/**
 * FlowPropertyValueProviders are used to supply property values to a Flow from the external environment that is decoupled from a given FlowActivity implementation.
 * FlowPropertyValueProviders are only called when there is no cached value in the FlowPropertyProvider, nor is there a serialized form of the property that can be deserialized
 * by a FlowPropertyDefinition's FlowTranslators.
 *
 *
 * This avoids having to override {@link FlowActivity#getProperty(String)}/ {@link FlowActivity#setProperty(String, Object)}.
 *
 * The advantages of using FlowPropertyValueProvider:
 * <ul>
 * <li>dependencies of one property to another property can be documented.</li>
 * <li>DRY principle</li>
 * <li>enables a property's value to be generated only when needed.</li>
 * <li>FlowActivities can just focus on what will be done with the properties, not how the properties are initialized.</li>
 * <li>Details about when the properties are initialized and how are encapsulated.</li>
 * </ul>
 * {@link FlowPropertyDefinitionProvider} is also usually implemented by FlowPropertyValueProvider, However, the separation must exist because
 * there are valid reasons to have a property defined but not provided, or to have several providers that depend on the context
 *
 * More information about properties can be found in {@ FlowPropertyDefinitionProvider}. 
 * To persist a property, don't do it in here, make sure to check {@link FlowPropertyValuePersister}
 * 
 * TODO: enable FlowPropertyValueProvider to be registered so they can be singletons with needed services injected vis the DI framework.
 * TODO: enable the FPVP to provide a template {@link FlowPropertyDefinition} that can be then customized with a given flow's specific
 * PropertyRequired values.
 *
 * TODO hook this in some how with the FlowPropertyBinding use of @Parameter(defaultValue="")
 * @author patmoore
 * @param <FPP> extends {@link FlowPropertyProvider}.
 *
 */
public interface FlowPropertyValueProvider<FPP extends FlowPropertyProvider> {
    /**
     *
     * @param <T> type of object returned
     * @param flowPropertyProvider current flowPropertyProvider -- be careful: flowPropertyProvider will not always be the flowPropertyProvider that created this FlowPropertyValueProvider.
     * May be null if {@link #getFlowPropertyProviderClass()} returned null )
     * @param flowPropertyDefinition
     * @return object generated by this provider
     */
    <T> T get(FPP flowPropertyProvider, FlowPropertyDefinition flowPropertyDefinition);
    /**
     *
     * @return class of object that must be passed to the {@link #get(FlowPropertyProvider, FlowPropertyDefinition)}. null means any class will work.
     * ( useful for case where get() does not actually use the {@link FlowPropertyProvider} )
     */
    Class<FPP> getFlowPropertyProviderClass();
    // added so we can avoid bad assignments
	boolean isHandling(FlowPropertyDefinition flowPropertyDefinition);
}
