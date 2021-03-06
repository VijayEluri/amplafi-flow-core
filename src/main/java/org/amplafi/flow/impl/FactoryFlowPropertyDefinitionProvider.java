package org.amplafi.flow.impl;

import static org.amplafi.flow.FlowConstants.*;
import static org.amplafi.flow.flowproperty.PropertyScope.flowLocal;
import static org.amplafi.flow.flowproperty.PropertyScope.activityLocal;
import static org.amplafi.flow.flowproperty.PropertyUsage.consume;
import static org.amplafi.flow.flowproperty.PropertyUsage.io;
import static org.amplafi.flow.flowproperty.PropertyUsage.use;

import java.net.URI;
import org.amplafi.flow.FlowTransition;
import org.amplafi.flow.flowproperty.AbstractFlowPropertyDefinitionProvider;
import org.amplafi.flow.flowproperty.CancelTextFlowPropertyValueProvider;
import org.amplafi.flow.flowproperty.FlowPropertyDefinitionBuilder;
import org.amplafi.flow.flowproperty.FlowPropertyDefinitionProvider;
import org.amplafi.flow.flowproperty.FlowTransitionFlowPropertyValueProvider;
import org.amplafi.flow.flowproperty.MessageFlowPropertyValueProvider;

import com.sworddance.util.ApplicationIllegalStateException;
/**
 * This map is used to connect a standard property name i.e. "user" to a standard class (UserImpl)
 * This map solves the problem where a flowProperty*Provider or changelistener needs (or would like)
 * to have a property available but does not define it.
 *
 * Explicit NOTE: the flow propertydefinitions returned MUST not persist any changes to permanent storage.
 * This is easy to enforce at the primary level (i.e. no persister is called. ) but what about accessing a read-only property
 * that returns a db object and then changes the db object? Can we tell hibernate not to persist?
 */
/**
 * Used to provide standard flow properties that are used every where.
 *
 * TODO: In order for the properties defined here to work completely, the FlowStateImpl.initializeFlowProperty() method must also look at the properties defined
 * implicitly by FactoryFlowPropertyDefinitionProviders and other FlowPropertyDefinitionProvider that supply system/application-wide default definitions.
 * Currently properties defined in FactoryFlowPropertyDefinitionProviders are not getting their initial state correctly copied over from the initialFlowState when a flow is
 * started.
 *
 * @author patmoore
 *
 */
public class FactoryFlowPropertyDefinitionProvider extends AbstractFlowPropertyDefinitionProvider implements FlowPropertyDefinitionProvider {

    @Deprecated // most of these properties are related to UI so they should be able to be moved out and eventually removed
    public static final FactoryFlowPropertyDefinitionProvider FLOW_INSTANCE = new FactoryFlowPropertyDefinitionProvider(
        new FlowPropertyDefinitionBuilder(FSTITLE_TEXT).internalOnly().initAccess(flowLocal, use).initFlowPropertyValueProvider( MessageFlowPropertyValueProvider.INSTANCE ),
        new FlowPropertyDefinitionBuilder(FSNO_CANCEL, boolean.class).internalOnly().initAccess(flowLocal, use),
        new FlowPropertyDefinitionBuilder(FSFINISH_TEXT).internalOnly().initAccess(flowLocal, use).initFlowPropertyValueProvider( MessageFlowPropertyValueProvider.INSTANCE ),
        new FlowPropertyDefinitionBuilder(FSRETURN_TO_TEXT).internalOnly().initAccess(flowLocal, use).initFlowPropertyValueProvider( MessageFlowPropertyValueProvider.INSTANCE ),
        // io -- for now because need to communicate the next page to be displayed
        // TODO think about PropertyScope/PropertyUsage
        new FlowPropertyDefinitionBuilder(FSPAGE_NAME).internalOnly().initPropertyUsage(io),
        // TODO think about PropertyScope/PropertyUsage
        new FlowPropertyDefinitionBuilder(FSAFTER_PAGE).internalOnly().initPropertyUsage(io),
        new FlowPropertyDefinitionBuilder(FSDEFAULT_AFTER_PAGE).internalOnly(),
        new FlowPropertyDefinitionBuilder(FSDEFAULT_AFTER_CANCEL_PAGE).internalOnly(),
        new FlowPropertyDefinitionBuilder(FSHIDE_FLOW_CONTROL, boolean.class).internalOnly().initPropertyScope(flowLocal),
        new FlowPropertyDefinitionBuilder(FSACTIVATABLE, boolean.class).internalOnly().initAccess(flowLocal, consume),
        new FlowPropertyDefinitionBuilder(FSIMMEDIATE_SAVE, boolean.class).internalOnly(),

        new FlowPropertyDefinitionBuilder(FSAUTO_COMPLETE, boolean.class).internalOnly(),
        new FlowPropertyDefinitionBuilder(FSALT_FINISHED).internalOnly().initAccess(flowLocal, use),
        new FlowPropertyDefinitionBuilder(FSREDIRECT_URL, URI.class).internalOnly().initPropertyUsage(io),
        new FlowPropertyDefinitionBuilder(FSREFERRING_URL, URI.class).internalOnly().initPropertyUsage(use),
        new FlowPropertyDefinitionBuilder(FSCONTINUE_WITH_FLOW).internalOnly().initPropertyUsage(io),
        new FlowPropertyDefinitionBuilder(FSFLOW_TRANSITIONS).map(FlowTransition.class).internalOnly().initAccess(flowLocal, use),
        // HACK
        new FlowPropertyDefinitionBuilder(FlowTransitionFlowPropertyValueProvider.FLOW_TRANSITION).internalOnly(),

        new FlowPropertyDefinitionBuilder(FSRETURN_TO_FLOW).internalOnly().initPropertyUsage(consume),
        new FlowPropertyDefinitionBuilder(FSRETURN_TO_FLOW_TYPE).internalOnly().initPropertyUsage(consume),
        new FlowPropertyDefinitionBuilder(FSSUGGESTED_NEXT_FLOW_TYPE).map(FlowTransition.class).internalOnly().initAccess(flowLocal, use),
        // TODO think about PropertyScope/PropertyUsage
        new FlowPropertyDefinitionBuilder(FSNEXT_FLOW).internalOnly().initPropertyUsage(consume),
        // HACK
        new FlowPropertyDefinitionBuilder(CancelTextFlowPropertyValueProvider.CANCEL_TEXT).initFlowPropertyValueProvider(CancelTextFlowPropertyValueProvider.INSTANCE).internalOnly()
    );
    @Deprecated // most of these properties are related to UI so they should be able to be moved out and eventually removed
    public static final FactoryFlowPropertyDefinitionProvider FLOW_ACTIVITY_INSTANCE = new FactoryFlowPropertyDefinitionProvider(
        new FlowPropertyDefinitionBuilder(FATITLE_TEXT).internalOnly().initAccess(activityLocal, use),
        new FlowPropertyDefinitionBuilder(FAUPDATE_TEXT).internalOnly().initAccess(activityLocal, use),
        new FlowPropertyDefinitionBuilder(FANEXT_TEXT).internalOnly().initAccess(activityLocal, use),
        new FlowPropertyDefinitionBuilder(FAPREV_TEXT).internalOnly().initAccess(activityLocal, use),
        new FlowPropertyDefinitionBuilder(FAINVISIBLE, boolean.class).internalOnly().initAccess(activityLocal, consume)
    );
    public FactoryFlowPropertyDefinitionProvider() {

    }
    public FactoryFlowPropertyDefinitionProvider(FlowPropertyDefinitionBuilder...flowPropertyDefinitions) {
        super(flowPropertyDefinitions);
    }
    /**
    *
    * @param propertyName
    * @param standardDefinitionClass
    */
   public void addStandardPropertyDefinition(String propertyName, Class<?> standardDefinitionClass) {
       FlowPropertyDefinitionBuilder flowPropertyDefinitionBuilder = new FlowPropertyDefinitionBuilder(propertyName, standardDefinitionClass);
       addStandardPropertyDefinition(flowPropertyDefinitionBuilder);
   }

   /**
    * This property should be minimal.
    * @param flowPropertyDefinitionBuilder supply the default property.
    */
   public void addStandardPropertyDefinition(FlowPropertyDefinitionBuilder flowPropertyDefinitionBuilder) {
       String propertyName = flowPropertyDefinitionBuilder.getName();
       ApplicationIllegalStateException.checkState(!this.getFlowPropertyDefinitionNames().contains(propertyName), "Cannot redefine '", propertyName, ". It was already defined as a standard property.");
       this.addFlowPropertyDefinitionImplementators(flowPropertyDefinitionBuilder);
       // Note: alternate names are not automatically added.
   }

}
