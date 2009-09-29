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

import static org.amplafi.flow.FlowConstants.*;
import static org.testng.Assert.*;

import java.util.Map;

import org.amplafi.flow.flowproperty.AddToMapFlowPropertyValueProvider;
import org.amplafi.flow.flowproperty.FlowPropertyDefinitionImpl;
import org.amplafi.flow.translator.BaseFlowTranslatorResolver;
import org.amplafi.flow.translator.ShortFlowTranslator;
import org.amplafi.flow.impl.FlowImpl;
import org.amplafi.flow.impl.FlowActivityImpl;
import org.amplafi.flow.impl.BaseFlowManagement;
import org.amplafi.flow.impl.FlowDefinitionsManagerImpl;
import org.amplafi.flow.impl.FlowManagerImpl;
import org.amplafi.flow.impl.FlowStateImplementor;
import org.amplafi.flow.impl.TransitionFlowActivity;
import org.testng.annotations.Test;
import static org.amplafi.flow.flowproperty.PropertyScope.*;

/**
 * Tests to see that transitioning between flows ( other than morphing ) happens correctly.
 *
 * @author patmoore
 *
 */
public class TestFlowTransitions {

    private static final String FLOW_TYPE_1 = "ftype1";
    private static final String FLOW_TYPE_2 = "ftype2";
    private static final boolean TEST_ENABLED = true;
    @Test(enabled=TEST_ENABLED)
    public void testSimpleFlowTransitionMapChecking() {
        FlowImpl flow = new FlowImpl(FLOW_TYPE_1);
        FlowActivityImpl fa1 = new FlowActivityImpl();
        FlowPropertyDefinition definition = fa1.getPropertyDefinition(FSFLOW_TRANSITIONS);
        assertNull(definition);
        flow.addActivity(fa1);
        definition = fa1.getPropertyDefinition(FSFLOW_TRANSITIONS);
        assertNotNull(definition);
        String returnToFlowLookupKey = null;
        definition.setFlowPropertyValueProvider(new AddToMapFlowPropertyValueProvider<String,FlowTransition>(new FlowTransition("foo", FLOW_TYPE_2, "foo", TransitionType.alternate, null)));
        BaseFlowManagement baseFlowManagement = getFlowManagement(flow);
        FlowState flowState = baseFlowManagement.startFlowState(FLOW_TYPE_1, false, null, returnToFlowLookupKey);

        Map<String, FlowTransition> propValue = flowState.getCurrentActivity().getProperty(FSFLOW_TRANSITIONS);
        assertTrue( propValue.keySet().contains("foo"));
    }

    /**
     * start one flow then a subflow, check to make sure the flow returns.
     */
    @Test(enabled=TEST_ENABLED)
    public void testReturnToFlow() {
        FlowImpl flow1 = new FlowImpl(FLOW_TYPE_1);
        String defaultAfterPage1 = "default-after-page-for-"+FLOW_TYPE_1;
        String defaultPage1 = "page-of-"+FLOW_TYPE_1;
        flow1.setPageName(defaultPage1);
        flow1.setDefaultAfterPage(defaultAfterPage1);
        FlowActivityImpl fa1 = new FlowActivityImpl();
        flow1.addActivity(fa1);

        FlowImpl flow2 = new FlowImpl(FLOW_TYPE_2);
        String defaultAfterPage2 = "default-after-page-for-"+FLOW_TYPE_2;
        String defaultPage2 = "page-of-"+FLOW_TYPE_2;
        flow2.setPageName(defaultPage2);
        flow2.setDefaultAfterPage(defaultAfterPage2);
        FlowActivityImpl fa2_1 = new FlowActivityImpl();
        flow2.addActivity(fa2_1);
        Object returnToFlowLookupKey = true;
        BaseFlowManagement baseFlowManagement = getFlowManagement(flow1, flow2);
        FlowState flowState1 = baseFlowManagement.startFlowState(FLOW_TYPE_1, true, null, returnToFlowLookupKey);
        assertEquals(flowState1.getCurrentPage(), defaultPage1);
        FlowState flowState2 = baseFlowManagement.startFlowState(FLOW_TYPE_2, true, null, true);
        String lookupKey1 = flowState2.getPropertyAsObject(FSRETURN_TO_FLOW);
        assertEquals(flowState2.getCurrentPage(), defaultPage2);
        assertEquals(flowState1.getLookupKey(), lookupKey1, "the child flow does not have the parent flow as the return-to-flow ");
        String pageName = flowState2.finishFlow();
        assertEquals(pageName, defaultPage1, "the child flow when it completed did not redirect to the parent flow's page. flowState2="+flowState2);
        FlowState flowState1_again = baseFlowManagement.getCurrentFlowState();
        assertEquals(flowState1_again.getLookupKey(), flowState1.getLookupKey());
        flowState1_again.finishFlow();
        FlowState nothing = baseFlowManagement.getCurrentFlowState();
        assertNull(nothing);
    }
    /**
     * Test to see how properties are cleared/copied during flow transitions.
     * <ul>
     * <li>make sure that {@link #flowLocal} is respected.</li>
     * <li>make sure that cache is cleared on flow completion.</li>
     * </ul>
     */
    @Test
    public void testAvoidConflictsOnFlowTransitions() {
        FlowActivityImpl flowActivity1 = new FlowActivityImpl();
        // initialized by "first" flow ignored by second flow.
        String initializedByFirst = "initializedByFirst";
        flowActivity1.addPropertyDefinitions(new FlowPropertyDefinitionImpl(initializedByFirst).initPropertyUsage(PropertyUsage.initialize));

        FlowTestingUtils flowTestingUtils = new FlowTestingUtils();
        flowTestingUtils.addFlowDefinition("first", flowActivity1,
            new TransitionFlowActivity(null, "second", TransitionType.normal));

        FlowActivityImpl flowActivity2 = new FlowActivityImpl();
        // this property name is unknown to "first" flow so "first" flow should not affect this property value at all.
        // for second flow, the property is flowLocal/ internalState so the setting should only affect the flowLocal copy.
        String privatePropertyForSecondFlow = "privateForSecond";
        String globalSettingForSecondFlowPrivateProperty = "global_for_privateForSecond";
        FlowPropertyDefinitionImpl flowPropertyDefinition_secondflow_prop0 = new FlowPropertyDefinitionImpl(privatePropertyForSecondFlow, Boolean.class).initAccess(flowLocal, PropertyUsage.internalState);
        // first flow doesn't understand this property but it sets it for the second flow to use.
        String opaqueSecondFlowProperty = "secondFlowProperty";
        flowActivity2.addPropertyDefinitions(
            flowPropertyDefinition_secondflow_prop0,
            new FlowPropertyDefinitionImpl(opaqueSecondFlowProperty, String.class).initPropertyScope(flowLocal).initPropertyUsage(PropertyUsage.io)
            );
        flowTestingUtils.addFlowDefinition("second", flowActivity2);
        FlowManagement flowManagement = flowTestingUtils.getFlowManagement();

        FlowStateImplementor flowState = flowManagement.startFlowState("first", true, FlowUtils.INSTANCE.createState(privatePropertyForSecondFlow, globalSettingForSecondFlowPrivateProperty,
            initializedByFirst, "ShouldBeIgnored"), false);
        String opaqueValuePassedFromFirstToSecond = "opaque";
        flowState.setRawProperty(opaqueSecondFlowProperty, opaqueValuePassedFromFirstToSecond);
        assertEquals(flowState.getPropertyAsObject(initializedByFirst, String.class), null, "flowState="+flowState);
        String propertyValueInitializedByFirst = "realvalue";
        flowState.setPropertyAsObject(initializedByFirst, propertyValueInitializedByFirst);
        flowTestingUtils.advanceToEnd(flowState);
        FlowStateImplementor nextFlowState = flowManagement.getCurrentFlowState();
        assertNotNull(nextFlowState);
        // flowLocal namespace ignored the passed setting
        assertNull(nextFlowState.getPropertyAsObject(privatePropertyForSecondFlow, Boolean.class), "nextFlowState="+nextFlowState);
        String privatePropertyValueInSecondFlow = "true";
        nextFlowState.setPropertyAsObject(privatePropertyForSecondFlow, privatePropertyValueInSecondFlow);
        assertEquals(nextFlowState.getFlowTypeName(), "second");
        // but it is still there for others.
        assertEquals(nextFlowState.getRawProperty((String)null, privatePropertyForSecondFlow), globalSettingForSecondFlowPrivateProperty, "nextFlowState="+nextFlowState);
        assertEquals(nextFlowState.getPropertyAsObject(opaqueSecondFlowProperty, String.class), opaqueValuePassedFromFirstToSecond, "looking at="+opaqueSecondFlowProperty+"  nextFlowState="+nextFlowState);
        assertEquals(nextFlowState.getPropertyAsObject(privatePropertyForSecondFlow), Boolean.parseBoolean(privatePropertyValueInSecondFlow), "looking at="+privatePropertyForSecondFlow+ "  nextFlowState="+nextFlowState);
        assertEquals(nextFlowState.getRawProperty((String)null, initializedByFirst), propertyValueInitializedByFirst, "nextFlowState="+nextFlowState);
        flowTestingUtils.advanceToEnd(flowState);

    }
    /**
     * @param flow
     * @return
     */
    private BaseFlowManagement getFlowManagement(Flow... flow) {
        BaseFlowManagement baseFlowManagement = new BaseFlowManagement();
        baseFlowManagement.setFlowTranslatorResolver(getFlowTranslatorResolver());
        FlowDefinitionsManagerImpl flowDefinitionsManager = new FlowDefinitionsManagerImpl();
        flowDefinitionsManager.initializeService();
        FlowManagerImpl flowManagerImpl = new FlowManagerImpl();
        flowManagerImpl.setFlowDefinitionsManager(flowDefinitionsManager);
        baseFlowManagement.setFlowManager(flowManagerImpl);
        flowDefinitionsManager.setFlowTranslatorResolver(getFlowTranslatorResolver());
        flowDefinitionsManager.addDefinitions(flow);
        return baseFlowManagement;
    }
    private BaseFlowTranslatorResolver getFlowTranslatorResolver() {
        BaseFlowTranslatorResolver flowTranslatorResolver = new BaseFlowTranslatorResolver();
        flowTranslatorResolver.addStandardFlowTranslators();
        flowTranslatorResolver.initializeService();
        flowTranslatorResolver.addFlowTranslator(new ShortFlowTranslator());
        return flowTranslatorResolver;
    }
}
