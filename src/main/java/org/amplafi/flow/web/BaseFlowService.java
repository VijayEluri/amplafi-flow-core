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
package org.amplafi.flow.web;

import static com.sworddance.util.CUtilities.isNotEmpty;
import static com.sworddance.util.CUtilities.put;
import static org.amplafi.flow.FlowConstants.FSREFERRING_URL;
import static org.amplafi.flow.launcher.FlowLauncher.ADVANCE_TO_END;
import static org.amplafi.flow.launcher.FlowLauncher.AS_FAR_AS_POSSIBLE;
import static org.amplafi.flow.launcher.FlowLauncher.FLOW_ID;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.amplafi.flow.FlowAppearance;
import org.amplafi.flow.FlowDefinitionsManager;
import org.amplafi.flow.FlowManagement;
import org.amplafi.flow.FlowManager;
import org.amplafi.flow.FlowRenderer;
import org.amplafi.flow.FlowState;
import org.amplafi.flow.FlowStateLifecycle;
import org.amplafi.flow.FlowUtils;
import org.amplafi.flow.ServicesConstants;
import org.amplafi.flow.flowproperty.FlowAppearanceFlowPropertyDefinitionProvider;
import org.amplafi.flow.impl.JsonFlowRenderer;
import org.amplafi.flow.validation.FlowValidationException;
import org.apache.commons.logging.Log;

import com.sworddance.util.ApplicationNullPointerException;
import com.sworddance.util.NotNullIterator;

@Deprecated
public class BaseFlowService implements FlowService {

    private static final String FLOW_NAME_SUFFIX = "Flow";

	public static final String USE_CURRENT = "current";

    protected static final String SCRIPT_CONTENT_TYPE = "text/javascript";

    private FlowManager flowManager;

    private Log log;

    /**
     * if {@link org.amplafi.flow.launcher.FlowLauncher#COMPLETE_FLOW} is not supplied - this is the
     * default value to use.
     */
    protected String defaultComplete;

    /**
     * the default way the results should be rendered. "html", "json", etc.
     */
    protected String renderResultDefault;

    private boolean assumeApiCall;

    protected FlowDefinitionsManager flowDefinitionsManager;

	private List<FlowRenderer> flowRenderers;

    public BaseFlowService() {
    }

    public BaseFlowService(FlowDefinitionsManager flowDefinitionsManager, FlowManager flowManager) {
        this.setFlowDefinitionsManager(flowDefinitionsManager);
        this.setFlowManager(flowManager);
        addFlowRenderer(new JsonFlowRenderer(flowDefinitionsManager));
    }

    public void addFlowRenderer(FlowRenderer flowRenderer) {
    	if (flowRenderers == null) {
    		flowRenderers = new ArrayList<FlowRenderer>();
    	}
    	flowRenderers.add(flowRenderer);
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

    @Override
    public void service(FlowRequest flowRequest, FlowResponse flowResponse)  {
    	FlowRenderer renderer = getRenderer(flowRequest.getRenderResultType());
        if (flowRequest.isDescribeRequest()) {
			renderer.describeFlow(flowResponse.getWriter(), flowRequest.getFlowType());
        } else {
        	Map<String, String> initial = null;
        	if (!flowRequest.hasFlowState()) {
		        initial = FlowUtils.INSTANCE.createState();
		        if ( isAssumeApiCall()) {
		            initial.put(FlowAppearanceFlowPropertyDefinitionProvider.FLOW_APPEARANCE, FlowAppearance.apiCall.toString());
		        }
		        // TODO map cookie to the json flow state.
		        String cookieString = flowRequest.getParameter(ServicesConstants.COOKIE_OBJECT);
		        if (isNotBlank(cookieString)) {
		            initial.put(ServicesConstants.COOKIE_OBJECT, cookieString);
		        }

		        List<String> keyList = flowRequest.getParameterNames();
		        if (isNotEmpty(keyList)) {
		            for (String key : keyList) {
		                String value = flowRequest.getParameter(key);
		                if (value != null) {
		                    // we do allow the value to be blank ( value may be existence of parameter)
		                    initial.put(key, value);
		                }
		            }
		        }
		        put(initial, FSREFERRING_URL, flowRequest.getReferingUri());
        	} else {
        		initial = Collections.emptyMap();
        		//Only render when client is supposed to see the result.
        		if (!flowResponse.isRedirectSet()) {
        			flowResponse.render(renderer);
        		}
        	}
			doActualService(flowRequest, flowResponse, initial);
        }
    }

    protected void doActualService(FlowRequest request, FlowResponse flowResponse, Map<String, String> initial) {
        completeFlowState(request, flowResponse, initial);
    }

    private FlowState getFlowState(FlowRequest request, FlowResponse flowResponse, Map<String, String> initial) throws Exception {
        FlowState flowState = null;
        String flowId = request.getFlowId();
        String flowType = request.getFlowType();

        if (isNotBlank(flowId)) {
            flowState = getFlowManagement().getFlowState(flowId);
        }

		if (flowState != null) {
        	flowState.setAllProperties(initial);
        } else if (isNotBlank(flowType)) {
            if (!getFlowManager().isFlowDefined(flowType)) {
            	String typeWithSuffix = flowType + FLOW_NAME_SUFFIX;
				if (!getFlowManager().isFlowDefined(typeWithSuffix)) {
	                flowResponse.setError(flowType + ": no such flow type", null);
            	} else {
            	    flowResponse.setError(flowType + ": no such flow type. But "+ typeWithSuffix + " exists. Use the full name", null);
            	}
				return null;
            }

            if (USE_CURRENT.equals(flowId)) {
                flowState = getFlowManagement().getFirstFlowStateByType(flowType);
            }

            if (flowState == null) {
                boolean currentFlow = !request.isBackground();
				flowState = getFlowManagement().startFlowState(flowType, currentFlow, initial);
                if (flowState == null || flowState.getFlowStateLifecycle() == FlowStateLifecycle.failed) {
                    flowResponse.setError(flowType + ": could not start flow type", null);
                    return flowState;
                }
            }
        } else {
            String message = String.format("Query String for request didn't contain %s or %s. At least one needs to be specified.", ServicesConstants.FLOW_TYPE, FLOW_ID);
            flowResponse.setError(message, null);
            return null;
        }
        return flowState;
    }

    public void setFlowManager(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    public FlowManager getFlowManager() {
        return flowManager;
    }

    public FlowManagement getFlowManagement() {
        return getFlowManager().getFlowManagement();
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public Log getLog() {
        return log;
    }

    protected void advanceFlow(FlowState flowState) {
    	flowState.next();
    }

    public void setDefaultComplete(String defaultComplete) {
        this.defaultComplete = defaultComplete;
    }

    public String getDefaultComplete() {
        return defaultComplete;
    }

    public void setRenderResultDefault(String renderResultDefault) {
        this.renderResultDefault = renderResultDefault;
    }

    public String getRenderResultDefault() {
        return renderResultDefault;
    }

    /**
     * @param assumeApiCall the assumeApiCall to set
     */
    public void setAssumeApiCall(boolean assumeApiCall) {
        this.assumeApiCall = assumeApiCall;
    }

    /**
     * @return the assumeApiCall
     */
    public boolean isAssumeApiCall() {
        return assumeApiCall;
    }

    protected FlowState completeFlowState(FlowRequest flowRequest, FlowResponse flowResponse, Map<String, String> initial)  {
    	FlowState flowState = null;
    	try {
        	flowState = flowRequest.hasFlowState() ? flowRequest.getFlowState() : getFlowState(flowRequest, flowResponse, initial);
            if (flowState != null) {
            	//HACK there mu
                String complete = flowRequest.getCompleteType();
                if (complete == null) {
                    // blank means don't try to complete flow.
                    complete = defaultComplete;
                }
                if (ADVANCE_TO_END.equalsIgnoreCase(complete)) {
                    while (!flowState.isCompleted()) {
                        advanceFlow(flowState);
                    }
                } else if (AS_FAR_AS_POSSIBLE.equalsIgnoreCase(complete)) {
                    String advanceToActivity = flowRequest.getAdvanceToActivity();
                    if (advanceToActivity != null) {
                        while (!flowState.isCompleted() && !flowState.getCurrentActivityByName().equals(advanceToActivity)) {
                            advanceFlow(flowState);
                        }
                    } else {
                        while (!flowState.isCompleted() && !flowState.isFinishable()) {
                            advanceFlow(flowState);
                        }
                    }
                }
                initializeRequestedParameters(flowRequest.getPropertiesToInitialize(), flowState);
                flowResponse.setFlowState(flowState);
            }
        } catch (FlowValidationException e) {
        	flowResponse.setError("Validation error", e);
        	flowResponse.setFlowState(e.getFlowState());
        	flowResponse.setRedirectURI(e.getRedirectUri());
		} catch (Exception e) {
        	flowResponse.setError("Error", e);
            if (flowState != null && !flowState.isPersisted()) {
                getFlowManagement().dropFlowState(flowState);
                flowResponse.setFlowState(flowState);
            }
        }
        return flowState;
    }

    private FlowRenderer getRenderer(String renderResult) {
    	if (renderResult == null) {
    		renderResult = renderResultDefault;
    	}
    	ApplicationNullPointerException.notNull(flowRenderers, "No flowRenders supplied");
		for (FlowRenderer renderer : flowRenderers) {
			if (renderer.getRenderResultType().equals(renderResult)){
				return renderer;
			}
		}
    	throw new IllegalStateException("There is no flow renderer for requested render result type: " + renderResult);
	}

	/**
	 * Needed to initialize some common properties which might not be part of the flow. For example if a client needs to get
	 * current user name along with messages list flow request.
	 *
     * @param propertiesToInitialize
     * @param flowState
     */
    private void initializeRequestedParameters(Iterable<String> propertiesToInitialize, FlowState flowState) {
        //Now just request all the properties that client asked for.
        for(String key: NotNullIterator.<String>newNotNullIterator(propertiesToInitialize)) {
        	flowState.getProperty(key);
        }
    }

	public List<FlowRenderer> getFlowRenderers() {
		return flowRenderers;
	}

	public void setFlowRenderers(List<FlowRenderer> flowRenderers) {
		this.flowRenderers = flowRenderers;
	}

}