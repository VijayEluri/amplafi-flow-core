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

package org.amplafi.flow.flowproperty;

import org.amplafi.flow.Flow;
import org.amplafi.flow.FlowActivity;
import org.amplafi.flow.FlowPropertyDefinition;
import org.amplafi.flow.FlowPropertyValueProvider;
import org.amplafi.flow.FlowUtils;

/**
 * used to return standard message keys.
 * @author patmoore
 *
 */
public class MessageFlowPropertyValueProvider implements FlowPropertyValueProvider<FlowActivity> {
    private String standardPrefix;

    public static final MessageFlowPropertyValueProvider INSTANCE = new MessageFlowPropertyValueProvider("message:");
    /**
     * @param standardPrefix
     */
    public MessageFlowPropertyValueProvider(String standardPrefix) {
        this.standardPrefix =standardPrefix;
    }
    /**
     *
     * @see org.amplafi.flow.FlowPropertyValueProvider#get(org.amplafi.flow.flowproperty.FlowPropertyProvider, org.amplafi.flow.FlowPropertyDefinition)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(FlowActivity flowActivity, FlowPropertyDefinition flowPropertyDefinition) {
        StringBuilder standardKey = new StringBuilder(standardPrefix);
        Flow flow = flowActivity.getFlow();
        standardKey.append("flow.").append(FlowUtils.INSTANCE.toLowerCase(flow.getFlowPropertyProviderName())).append(".");
        if (flowPropertyDefinition.getPropertyScope() == PropertyScope.activityLocal) {
            standardKey.append(FlowUtils.INSTANCE.toLowerCase(flowActivity.getFlowPropertyProviderName())).append(".");
        }
        if (flowPropertyDefinition.getName().startsWith("fs") || flowPropertyDefinition.getName().startsWith("fa")) {
            standardKey.append(FlowUtils.INSTANCE.toLowerCase(flowPropertyDefinition.getName().substring(2)));
        } else {
            standardKey.append(FlowUtils.INSTANCE.toLowerCase(flowPropertyDefinition.getName()));
        }
        return (T) standardKey.toString();
    }

    @Override
    public String toString() {
        return getClass()+" standardPrefix="+this.standardPrefix;
    }
}
