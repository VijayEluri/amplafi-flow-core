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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.amplafi.flow.impl.BaseFlowManagement;
import org.amplafi.flow.impl.FlowDefinitionsManagerImpl;
import org.amplafi.flow.impl.FlowImpl;
import org.amplafi.flow.impl.FlowManagerImpl;
import org.amplafi.flow.translator.BaseFlowTranslatorResolver;
import org.amplafi.flow.translator.FlowTranslator;
import org.amplafi.flow.translator.ShortFlowTranslator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.IAnswer;
import org.easymock.EasyMock;


/**
 * @author patmoore
 *
 */
public class FlowTestingUtils {

    private FlowDefinitionsManager flowDefinitionsManager;
    private FlowManager flowManager;

    private FlowTranslatorResolver flowTranslatorResolver;

    private FlowManagement flowManagement;

    private AtomicInteger counter = new AtomicInteger();

    private Log log = LogFactory.getLog(FlowTestingUtils.class);
    public FlowTestingUtils() {
        this(new FlowManagerImpl(), new FlowDefinitionsManagerImpl(), new BaseFlowTranslatorResolver());
    }
    public FlowTestingUtils(FlowManager flowManager, FlowDefinitionsManager flowDefinitionsManager, FlowTranslatorResolver flowTranslatorResolver, FlowManagement flowManagement) {
        this.flowDefinitionsManager = flowDefinitionsManager;
        this.flowTranslatorResolver = flowTranslatorResolver;
        this.flowManagement = flowManagement;
        this.flowManager = flowManager;
    }

    public FlowTestingUtils(FlowManagerImpl flowManager, FlowDefinitionsManagerImpl flowDefinitionsManager, BaseFlowTranslatorResolver flowTranslatorResolver) {
        this.flowDefinitionsManager = flowDefinitionsManager;

        this.flowTranslatorResolver = flowTranslatorResolver;
        if(flowTranslatorResolver.getLog() == null) {
            flowTranslatorResolver.setLog(log);
        }
        BaseFlowManagement baseFlowManagement = new BaseFlowManagement();
        this.flowManagement = baseFlowManagement;
        this.flowManager = flowManager;
        flowManager.setFlowTranslatorResolver(flowTranslatorResolver);
        flowManager.setFlowDefinitionsManager(flowDefinitionsManager);
        initializeService();
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    private void initializeService() {
        ((BaseFlowTranslatorResolver)flowTranslatorResolver).setFlowDefinitionsManager(this.flowDefinitionsManager);
        ((BaseFlowTranslatorResolver)flowTranslatorResolver).setFlowTranslators(Arrays.<FlowTranslator<?>>asList(
            new ShortFlowTranslator()
            ));
        ((FlowDefinitionsManagerImpl)flowDefinitionsManager).setFlowTranslatorResolver(flowTranslatorResolver);
        ((BaseFlowTranslatorResolver)flowTranslatorResolver).initializeService();
        ((FlowDefinitionsManagerImpl)flowDefinitionsManager).initializeService();
        ((BaseFlowManagement)this.flowManagement).setFlowManager(flowManager);
        ((BaseFlowManagement)this.flowManagement).setFlowTranslatorResolver(flowTranslatorResolver);

    }

    public <T extends FlowActivityImplementor> String addFlowDefinition(T...flowActivities) {
        String flowTypeName = "testflow"+counter.incrementAndGet()+":"+System.nanoTime();
        return this.addFlowDefinition(flowTypeName, flowActivities);
    }

    /**
     * Instantiates a FlowActivity of the given class, attaches it to a new (dummy)
     * flow (and optionally sets the flow's state).
     * @param clazz
     * @param state
     * @param <T>
     * @return a new T
     */
    public <T extends FlowActivityImplementor> T initActivity(Class<T> clazz, FlowState state)  {
        try {
            T activity = clazz.newInstance();
            FlowImpl flow = new FlowImpl();
            flow.addActivity(activity);
            if (state!=null) {
                flow.setFlowState(state);
            }
            return activity;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Instantiates a FlowActivity of the given class and attaches it to a new (dummy)
     * flow.
     * @param clazz
     * @param <T>
     * @return a new T
     */
    public <T extends FlowActivityImplementor> T initActivity(Class<T> clazz) {
        return initActivity(clazz, null);
    }
    public String addFlowDefinition(String flowTypeName, FlowActivityImplementor... activities) {
        final FlowImplementor def = new FlowImpl(flowTypeName, activities);
        getFlowDefinitionsManager().addDefinitions(def);
        return flowTypeName;
    }
    public FlowManager programFlowManager(String flowTypeName, FlowActivityImplementor... activities) {
        final FlowImplementor def = new FlowImpl(flowTypeName, activities);
        getFlowTranslatorResolver().resolveFlow(def);
        EasyMock.expect(getFlowManager().getFlowDefinition(flowTypeName)).andReturn(def).anyTimes();
        EasyMock.expect(getFlowManager().isFlowDefined(flowTypeName)).andReturn(true).anyTimes();
        EasyMock.expect(getFlowManager().getInstanceFromDefinition(flowTypeName)).andAnswer(new IAnswer<FlowImplementor>() {
            @Override
            public FlowImplementor answer() {
                return def.createInstance();
            }
        }).anyTimes();
        return getFlowManager();
    }
    /**
     * @return the flowDefinitionsManager
     */
    public FlowDefinitionsManager getFlowDefinitionsManager() {
        return flowDefinitionsManager;
    }

    /**
     * @return the flowTranslatorResolver
     */
    public FlowTranslatorResolver getFlowTranslatorResolver() {
        return flowTranslatorResolver;
    }

    /**
     * @param flowState
     */
    public void advanceToEnd(FlowState flowState) {
        while( flowState.hasNext()) {
            flowState.next();
        }
    }
    public void resolveAndInit(FlowPropertyDefinition definition) {
        flowTranslatorResolver.resolve("", definition);
    }

    /**
     * @return the flowManagement
     */
    public FlowManagement getFlowManagement() {
        return flowManagement;
    }
    /**
     * @param flowDefinitionsManager the flowDefinitionsManager to set
     */
    public void setFlowManager(FlowManager flowManager) {
        this.flowManager = flowManager;
    }
    /**
     * @return the flowDefinitionsManager
     */
    public FlowManager getFlowManager() {
        return flowManager;
    }

}
