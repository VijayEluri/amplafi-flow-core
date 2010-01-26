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

import static org.amplafi.flow.FlowConstants.*;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.amplafi.flow.FlowStepDirection;
import org.amplafi.flow.FlowConstants;
import org.amplafi.flow.FlowStateLifecycle;
import org.amplafi.flow.FlowManagement;
import org.amplafi.flow.FlowState;
import org.amplafi.flow.FlowTransition;
import org.amplafi.flow.TransitionType;
import org.amplafi.flow.flowproperty.AddToMapFlowPropertyValueProvider;
import org.amplafi.flow.flowproperty.FlowPropertyDefinitionImpl;
import org.apache.commons.lang.ObjectUtils;
import static org.amplafi.flow.flowproperty.PropertyScope.*;

/**
 * A FlowActivity used to launch a new flow.
 */
public class TransitionFlowActivity extends FlowActivityImpl {

    /**
     *
     */
    public static final String FS_FLOW_TRANSITION_LABEL = "fsFlowTransitionLabel";

    private static final long serialVersionUID = 4407898586831855506L;

    private String nextFlowId;

    private String nextFlowType;

    /**
     * Only trigger next flow if the finish key matches.
     */
    private String finishKey;

    /**
     * may be hard coded
     */
    private String fsFlowTransitionLabel;

    private TransitionType transitionType;

    public TransitionFlowActivity() {
//        setTransitionType(TransitionType.normal); TODO finish text needs to be handled better.

    }
    public TransitionFlowActivity(String finishKey, String nextFlowType, TransitionType transitionType) {
        setFinishKey(finishKey);
        setNextFlowType(nextFlowType);
        setTransitionType(transitionType);
    }

    @Override
    public void addStandardFlowPropertyDefinitions() {
        super.addStandardFlowPropertyDefinitions();
        this.addPropertyDefinitions(new FlowPropertyDefinitionImpl(FS_FLOW_TRANSITION_LABEL).initPropertyScope(activityLocal));
        initTransition();
    }
    @Override
    protected <T extends FlowActivityImpl>void copyTo(T instance) {
        super.copyTo(instance);
        TransitionFlowActivity transitionFlowActivity = (TransitionFlowActivity) instance;
        transitionFlowActivity.nextFlowId = nextFlowId;
        transitionFlowActivity.nextFlowType = nextFlowType;
        transitionFlowActivity.finishKey = finishKey;
        transitionFlowActivity.transitionType = transitionType;
    }

    @Override
    public void initializeFlow() {
        super.initializeFlow();
        setInvisible(isBlank(getPageName()) && isBlank(getComponentName()));
    }

    /**
     *
     */
    private void initTransition() {
        if (this.transitionType != null ) {
            // only will work if this is can be supplied when all the flows are known... probably should
            // always be a flowProperty reference.
//            if (isBlank(fsFlowTransitionLabel)) {
//                if ( isNotBlank(getNextFlowType())) {
//                    this.setFsFlowTransitionLabel(this.getFlowManagement().getFlowDefinition(getNextFlowType()).getLinkTitle());
//                }
//            }
            // HACK should really have FlowTransitions return a getFlowLauncher()
            this.handleFlowPropertyValueProvider(FSFLOW_TRANSITIONS,
                new AddToMapFlowPropertyValueProvider<String, FlowTransition>(new FlowTransition(getFinishKey(), null, getFsFlowTransitionLabel(), transitionType, null)));
        }
    }
    /**
     * advance to next FlowActivity if we have no component (or page) or the
     * superclass
     */
    @Override
    public boolean activate(FlowStepDirection flowStepDirection) {
        return super.activate(flowStepDirection) || (isBlank(getPageName()) && isBlank(getComponentName()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key) {
        if (FlowConstants.FSNEXT_FLOW.equals(key)) {
            T value = (T) this.getNextFlowType();
            if (value == null) {
                return (T) super.getProperty(key);
            } else {
                return value;
            }
        } else {
            return (T) super.getProperty(key);
        }
    }

    /**
     * HACK should really have FlowTransitions do the work.
     * @return the now current FlowState.
     */
    @Override
    public FlowState finishFlow(FlowState currentNextFlowState) {
        FlowState nextFlowState = super.finishFlow(currentNextFlowState);
        // maybe check to see if a new flow already exists?
        if (this.getFlowState().getFlowStateLifecycle() == FlowStateLifecycle.successful) {
            String finishType = getFlowState().getFinishKey();
            if (getTransitionType() == TransitionType.normal && finishType == null
                    || getFinishKey().equalsIgnoreCase(finishType)) {
                FlowManagement fm = getFlowManagement();
                if (isNotBlank(getNextFlowId())) {
                    FlowState possibleFlowState = fm.getFlowState(resolveIndirectReference(getNextFlowId()));
                    if ( possibleFlowState != null) {
                        nextFlowState = possibleFlowState;
                        fm.makeAfter(this.getFlowState(), possibleFlowState);
                    }
                } else {
                    String flowType = resolveIndirectReference(getNextFlowType());
                    if (isBlank(flowType)) {
                        // why clear the FSNEXT_FLOW ?
                        flowType = getResolvedIndirectReferenceProperty(FlowConstants.FSNEXT_FLOW);
                        setProperty(FlowConstants.FSNEXT_FLOW, null);
                    }
                    if (isNotBlank(flowType)) {
                        nextFlowState = this.createNewFlow(flowType);
                        fm.makeAfter(this.getFlowState(), nextFlowState);
                    }
                }
            }
        }
        return nextFlowState;
    }

    /**
     *
     * @param nextFlowId if prefixed with "fprop:" then the actual id is in a
     *        flow property.
     */
    public void setNextFlowId(String nextFlowId) {
        this.nextFlowId = nextFlowId;
    }

    public String getNextFlowId() {
        return nextFlowId;
    }

    /**
     *
     * @param nextFlowType if prefixed with "fprop:" then the actual flow type
     *        is in the flow property that follows the "fprop:"
     */
    public void setNextFlowType(String nextFlowType) {
        this.nextFlowType = nextFlowType;
    }

    public String getNextFlowType() {
        return nextFlowType;
    }

    public void setFinishKey(String finishKey) {
        this.finishKey = finishKey;
    }

    public String getFinishKey() {
        return finishKey != null ?finishKey:ObjectUtils.toString(this.getTransitionType(), null);
    }

    public void setType(String type) {
        setTransitionType(TransitionType.valueOf(type));
    }

    public void setTransitionType(TransitionType transitionType) {
        this.transitionType = transitionType;
    }

    public TransitionType getTransitionType() {
        return transitionType == null? TransitionType.normal: transitionType;
    }

    /**
     * @param fsFlowTransitionLabel the fsFlowTransitionLabel to set
     */
    public void setFsFlowTransitionLabel(String fsFlowTransitionLabel) {
        this.fsFlowTransitionLabel = fsFlowTransitionLabel;
    }

    /**
     * @return the fsFlowTransitionLabel
     */
    public String getFsFlowTransitionLabel() {
        return fsFlowTransitionLabel;
    }

}
