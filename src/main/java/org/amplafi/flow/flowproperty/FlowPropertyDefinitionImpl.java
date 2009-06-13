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

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.amplafi.flow.*;
import org.amplafi.json.JsonSelfRenderer;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;


/**
 * Defines a property that will be assigned as part of a {@link Flow} or
 * {@link FlowActivity}. This allows the value to be available to the component
 * or page referenced by a {@link FlowActivity}.
 *
 * TODO:
 * * FPD have a flag that makes them read-only
 * * if the FPD is "changed" then then method that changes the FPD should returned a cloned FPD with the modification. (see the FPD.init* methods() )
 * * necessary to allow instances of Flows to modify the values on a per flow-instance basis.
 *
 * @author Patrick Moore
 */
public class FlowPropertyDefinitionImpl implements FlowPropertyDefinition {
    private static final String REQUIRED = "required";

    /**
     * Name of the property as used in the flow code.
     */
    private String name;

    /**
     * when a flow starts this should be the initial value set unless there is
     * already a value.
     */
    private String initial;

    /**
     * when a flow starts the initial value can be overridden by a passed in
     * value. A use case example is when a flow is used as an api. This prevents
     * an external api call from messing with the internal guts of a flow.
     */
    private Boolean initialOptional;

    /**
     * Used when there is no explicit flowPropertyValueProvider. Primary usecase is FlowProperties that have a default
     * way of determining their value. But wish to allow that default method to be changed. (for example, fsFinishText )
     */
    private FlowPropertyValueProvider factoryFlowPropertyValueProvider;
    private FlowPropertyValueProvider flowPropertyValueProvider;
    /**
     * Used if the UI component's parameter name is different from the FlowPropertyDefinition's name.
     * Useful when using a FlowActivity with components that cannot be changed or have not been changed.
     * For example, a standard tapestry or tacos component.
     * Or a component that is used in multiple places and changing the UI component itself could cause a ripple of
     * cascading problems and possible regressions.
     */
    private String uiComponentParameterName;

    /**
     * Property should not be persisted as string in the FlowState map. This is
     * useful caching values during this transaction. TODO maybe allow
     * non-entities that are Serializable to last beyond current transaction?
     */
    private Boolean cacheOnly;

    /**
     * on {@link FlowActivity#passivate(boolean, FlowStepDirection)} the object should be saved
     * back. This allows complex object to have their string representation
     * saved. Necessary for cases when setProperty() is not used to save value
     * changes, because the object stored in the property is being modified not
     * the usual case of where a new object is saved.
     */
    private Boolean saveBack;

    /**
     * if the property does not exist in the cache then create a new instance.
     * As a result, the property will never return a null.
     */
    private Boolean autoCreate;

    private DataClassDefinitionImpl dataClassDefinition;

    private Set<String> alternates;

    /**
     * data that should not be outputted or saved. for example, passwords.
     */
    private Boolean sensitive;

    private String validators;
    private PropertyRequired propertyRequired;
    private PropertyUsage propertyUsage;
    /**
     * once set no further changes to this {@link FlowPropertyDefinitionImpl} are permitted.
     * calling init* methods will return a new {@link FlowPropertyDefinitionImpl} that can be modified.
     * calling set* will result in an exception.
     */
    private boolean templateFlowPropertyDefinition;

    /**
     * Creates an unnamed String property.
     */
    public FlowPropertyDefinitionImpl() {
        dataClassDefinition = new DataClassDefinitionImpl();
        // Set the propertyUsage to 'use' so that properties created via xml are used and
        // passed through to following flows.
        // think this is
        this.propertyUsage = PropertyUsage.use;
    }

    public FlowPropertyDefinitionImpl(FlowPropertyDefinitionImpl clone) {
        this.setName(clone.name);
        dataClassDefinition = new DataClassDefinitionImpl(clone.dataClassDefinition);
        if (isNotEmpty(clone.alternates)) {
            alternates = new HashSet<String>();
            alternates.addAll(clone.alternates);
        }
        autoCreate = clone.autoCreate;
        cacheOnly = clone.cacheOnly;
        this.factoryFlowPropertyValueProvider = clone.factoryFlowPropertyValueProvider;
        this.flowPropertyValueProvider = clone.flowPropertyValueProvider;
        this.setInitial(clone.initial);
        this.setUiComponentParameterName(clone.uiComponentParameterName);
        if (clone.sensitive != null) {
            this.setSensitive(clone.sensitive);
        }
        validators = clone.validators;
        saveBack = clone.saveBack;
        initialOptional = clone.initialOptional;
        this.propertyRequired = clone.propertyRequired;
        this.propertyUsage = clone.propertyUsage;
    }

    /**
     * Creates an optional string property.
     *
     * @param name The name of the property.
     */
    public FlowPropertyDefinitionImpl(String name) {
        dataClassDefinition = new DataClassDefinitionImpl();
        this.setName(name);
    }

    /**
     * Creates a named String property having the given validators.
     *
     * @param name property name
     * @param validators validators for the property.
     */
    public FlowPropertyDefinitionImpl(String name, String validators) {
        this.setName(name);
        this.validators = validators;
        this.propertyRequired = isRequired()?PropertyRequired.advance: PropertyRequired.optional;
        dataClassDefinition = new DataClassDefinitionImpl();
    }

    /**
     * Creates an optional property of the given type.
     *
     * @param name
     * @param dataClass
     * @param collectionClasses
     */
    public FlowPropertyDefinitionImpl(String name, Class<?> dataClass, Class<?>...collectionClasses) {
        this(name, dataClass, PropertyRequired.optional, collectionClasses);
    }

    /**
     * Creates a string property of the given requirements.
     *
     * @param name
     * @param required
     */
    public FlowPropertyDefinitionImpl(String name, PropertyRequired required) {
        this(name, null, required);
    }

    /**
     * Creates a property of the given type and requirements.
     *
     * @param name
     * @param dataClass the underlying class that all the collection classes are wrapping.
     * @param required how required is this property?
     * @param collectionClasses
     */
    public FlowPropertyDefinitionImpl(String name, Class<? extends Object> dataClass, PropertyRequired required, Class<?>...collectionClasses) {
        this(name, required, new DataClassDefinitionImpl(dataClass, collectionClasses));
    }
    public FlowPropertyDefinitionImpl(String name, DataClassDefinitionImpl dataClassDefinition) {
        this(name, PropertyRequired.optional, dataClassDefinition);
    }
    public FlowPropertyDefinitionImpl(String name, PropertyRequired required, DataClassDefinitionImpl dataClassDefinition) {
        this.setName(name);
        this.propertyRequired = required;
        this.setRequired(required==PropertyRequired.advance);
        this.dataClassDefinition = dataClassDefinition;
    }

    public void setDefaultObject(Object defaultObject) {
        if ( !(defaultObject instanceof FlowPropertyValueProvider)) {
            FixedFlowPropertyValueProvider<FlowActivity> fixedFlowPropertyValueProvider = new FixedFlowPropertyValueProvider<FlowActivity>(defaultObject);
            fixedFlowPropertyValueProvider.convertable(this);
            if (dataClassDefinition.isDataClassDefined()) {
                if (!this.getDataClass().isPrimitive()) {
                    // really need to handle the autobox issue better.
                    this.getDataClass().cast(defaultObject);
                }
            } else if (defaultObject.getClass() != String.class) {
                setDataClass(defaultObject.getClass());
            }
            this.factoryFlowPropertyValueProvider = fixedFlowPropertyValueProvider;
        } else {
            this.factoryFlowPropertyValueProvider = (FlowPropertyValueProvider)defaultObject;
        }
    }

    /**
     *
     * @param flowActivity
     * @return defaultObject Should not save default object in
     *         {@link FlowPropertyDefinitionImpl} if it is mutable.
     */
    public Object getDefaultObject(FlowActivity flowActivity) {
        Object value;
        FlowPropertyValueProvider provider = getFlowPropertyValueProviderToUse();
        if ( provider != null) {
            value = provider.get(flowActivity, this);
        } else {
            // TODO -- may still want to call this if flowPropertyValueProvider returns null.
            // for example the property type is a primitive.
            value = this.dataClassDefinition.getFlowTranslator().getDefaultObject(flowActivity);
        }
        // TODO -- do we want to set the default object? or recalculate it each time?
        // might be important if the default object is to get modified or if a FPD is shared.
        return value;
    }

    /**
     * @return
     */
    private FlowPropertyValueProvider getFlowPropertyValueProviderToUse() {
        if ( this.flowPropertyValueProvider != null) {
            return this.flowPropertyValueProvider;
        } else {
            return this.factoryFlowPropertyValueProvider;
        }
    }

    public FlowPropertyDefinitionImpl initDefaultObject(Object defaultObject) {
        FlowPropertyDefinitionImpl flowPropertyDefinition = cloneIfTemplate(this.factoryFlowPropertyValueProvider, defaultObject);
        flowPropertyDefinition.setDefaultObject(defaultObject);
        return flowPropertyDefinition;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getValidators() {
        return validators;
    }

    public void setValidators(String validators) {
        this.validators = validators;
    }

    // TODO fix with template check
    public void addValidator(String validator) {
        if (StringUtils.isBlank(validators)) {
            setValidators(validator);
        } else {
            validators += "," + validator;
        }
    }

    public FlowPropertyDefinitionImpl validateWith(String... fields) {
        addValidator("flowField="+join(fields,"-"));
        return this;
    }

    @SuppressWarnings("hiding")
    public FlowPropertyDefinitionImpl initValidators(String validators) {
        FlowPropertyDefinitionImpl flowPropertyDefinition = cloneIfTemplate(this.validators, validators);
        flowPropertyDefinition.setValidators(validators);
        return flowPropertyDefinition;
    }

    public FlowTranslator<?> getTranslator() {
        return this.dataClassDefinition.getFlowTranslator();
    }

    /**
     * This is used to handle case of parameter name change and 'short' names
     * for uris and the like.
     *
     * @param alternateNames
     * @return this
     */
    public FlowPropertyDefinitionImpl addAlternateNames(String... alternateNames) {
        getAlternates().addAll(Arrays.asList(alternateNames));
        return this;
    }

    public void setInitial(String initial) {
        this.initial = initial;
        checkInitial(this.initial);
    }

    public String getInitial() {
        return initial;
    }

    public FlowPropertyDefinitionImpl initInitial(String initialValue) {
        FlowPropertyDefinitionImpl flowPropertyDefinition = cloneIfTemplate(this.initial, initialValue);
        flowPropertyDefinition.setInitial(initialValue);
        return flowPropertyDefinition;
    }

    public boolean isLocal() {
        return PropertyUsage.activityLocal == this.propertyUsage;
    }

    /**
     * also sets {@link PropertyUsage#flowLocal}
     * @return this
     */
    public FlowPropertyDefinitionImpl initCacheOnly() {
        setCacheOnly(true);
        setPropertyUsage(PropertyUsage.flowLocal);
        return this;
    }

    public void setUiComponentParameterName(String parameterName) {
        this.uiComponentParameterName = parameterName;
    }

    public String getUiComponentParameterName() {
        if (uiComponentParameterName == null) {
            return getName();
        }
        return uiComponentParameterName;
    }

    public FlowPropertyDefinitionImpl initParameterName(String parameterName) {
        setUiComponentParameterName(parameterName);
        return this;
    }

    public boolean isRequired() {
        return validators != null && validators.contains(REQUIRED);
    }

    public void setRequired(boolean required) {
        if (isRequired() != required) {
            if (required) {
                validators = StringUtils.isBlank(validators) ? REQUIRED : validators + ","
                        + REQUIRED;
            } else if (validators.length() == REQUIRED.length()) {
                validators = null;
            } else {
                int i = validators.indexOf(REQUIRED);
                if (i == 0) {
                    // remove trailing ','
                    validators = validators.substring(REQUIRED.length() + 1);
                } else {
                    // remove preceding ','
                    validators = validators.substring(0, i - 1)
                            + validators.substring(i + REQUIRED.length());
                }
            }
        }
    }

    public void setAutoCreate(boolean autoCreate) {
        this.autoCreate = autoCreate;
    }

    public boolean isAutoCreate() {
        if (isDefaultAvailable()) {
            return true;
        }
        return isDefaultByClassAvailable();
    }

    /**
     * @return
     */
    private boolean isDefaultByClassAvailable() {
        if ( autoCreate != null) {
            return getBoolean(autoCreate);
        }
        if (!dataClassDefinition.isCollection() ) {
            Class<?> dataClass = dataClassDefinition.getDataClass();
            if (dataClass.isPrimitive() ) {
                return true;
            } else if (dataClass.isInterface() || dataClass.isEnum()) {
                return false;
            }
        }
        return false;
    }
    public boolean isDefaultAvailable() {
        if ( this.getFlowPropertyValueProviderToUse() != null ) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return toComponentDef() +":"+this.dataClassDefinition;
    }

    public String toComponentDef() {
        return getUiComponentParameterName();
    }

    public static String toString(String paramName, String flowPropName) {
        return " " + paramName + "=\"fprop:" + flowPropName + "\" ";
    }

    public <T> String serialize(T object) {
        if ( this.dataClassDefinition.getFlowTranslator() == null) {
            return null;
        } else {
            Object serialized = this.dataClassDefinition.serialize(this, object);
            return ObjectUtils.toString(serialized, null);
        }
    }

    @SuppressWarnings("unchecked")
    public <V> V parse(String value) throws FlowException {
        return (V) this.dataClassDefinition.deserialize(this, value);
    }
    /**
     * @param propertyRequired the propertyRequired to set
     * @return this
     */
    @SuppressWarnings("hiding")
    public FlowPropertyDefinitionImpl initPropertyRequired(PropertyRequired propertyRequired) {
        this.setPropertyRequired(propertyRequired);
        return this;
    }
    /**
     * @param propertyRequired the propertyRequired to set
     */
    public void setPropertyRequired(PropertyRequired propertyRequired) {
        this.propertyRequired = propertyRequired;
    }

    /**
     * @return the propertyRequired
     */
    public PropertyRequired getPropertyRequired() {
        return propertyRequired == null?PropertyRequired.optional:propertyRequired;
    }

    /**
     * @param propertyUsage the propertyUsage to set
     */
    public void setPropertyUsage(PropertyUsage propertyUsage) {
        this.propertyUsage = setCheckTemplateState(this.propertyUsage, propertyUsage);
    }

    /**
     * @return the propertyUsage (default {@link PropertyUsage#consume}
     */
    public PropertyUsage getPropertyUsage() {
        return propertyUsage == null?PropertyUsage.consume:propertyUsage;
    }
    @SuppressWarnings("hiding")
    public FlowPropertyDefinitionImpl initPropertyUsage(PropertyUsage propertyUsage) {
        setPropertyUsage(propertyUsage);
        return this;
    }

    public void setCacheOnly(boolean cacheOnly) {
        // if its sensitive we have already forced this to cache only.
        // this prevents passwords from being saved into the flowstate db table.
        if (!isSensitive()) {
            this.cacheOnly = cacheOnly;
        }
    }

    /**
     * modifications to this property are not persisted, but are cached.
     *
     * @return true if modifications are not persisted.
     */
    public boolean isCacheOnly() {
        return getBoolean(cacheOnly);
    }

    /**
     * affects isEntity()
     *
     * @param dataClass
     */
    public void setDataClass(Class<? extends Object> dataClass) {
        if (dataClass == String.class) {
            dataClass = null;
        }
        this.dataClassDefinition.setDataClass(dataClass);
    }

    // HACK -- to fix later...
    public FlowPropertyDefinition initialize() {
        checkInitial(this.getInitial());
        return this;
    }

    /**
     * make sure the value can be
     */
    private void checkInitial(String value) {
        if ( !this.dataClassDefinition.isDeserializable(this, value)) {
            throw new IllegalStateException(this + " while checking initial value="+ value);
        }
    }

    /**
     * @param dataClassDefinition the dataClassDefinition to set
     */
    public void setDataClassDefinition(DataClassDefinitionImpl dataClassDefinition) {
        this.dataClassDefinition = dataClassDefinition;
    }

    /**
     * @return the dataClassDefinition
     */
    public DataClassDefinitionImpl getDataClassDefinition() {
        return dataClassDefinition;
    }

    public Class<? extends Object> getDataClass() {
        return dataClassDefinition.getDataClass();
    }

    public Class<? extends Object> getCollectionClass() {
        return this.dataClassDefinition.getCollection();
    }

    public Set<String> getAlternates() {
        if (alternates == null) {
            alternates = new HashSet<String>();
        }
        return alternates;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
        // TODO: rethink on handling this (commented out in order to access
        // values)
        /*
         * if ( sensitive) { this.cacheOnly = true; }
         */
    }

    public boolean isSensitive() {
        return getBoolean(sensitive);
    }

    public FlowPropertyDefinitionImpl initSensitive() {
        setSensitive(true);
        return this;
    }

    public void setInitialMode(boolean initialOptional) {
        this.initialOptional = initialOptional;
    }

    public boolean isInitialMode() {
        return getBoolean(initialOptional);
    }

    /**
     * not a reversable process.
     */
    public void setTemplateFlowPropertyDefinition() {
        this.templateFlowPropertyDefinition = true;
    }

    /**
     * @return the templateFlowPropertyDefinition
     */
    public boolean isTemplateFlowPropertyDefinition() {
        return templateFlowPropertyDefinition;
    }
    protected <T> T setCheckTemplateState(T oldObject, T newObject) {
        if ( templateFlowPropertyDefinition && !ObjectUtils.equals(oldObject, newObject)) {
            throw new IllegalStateException("Cannot change state of a Template FlowPropertyDefinition");
        }
        return newObject;
    }
    protected <T> FlowPropertyDefinitionImpl cloneIfTemplate(T oldObject, T newObject) {
        if ( !ObjectUtils.equals(oldObject, newObject) && templateFlowPropertyDefinition) {
            return this.clone();
        }
        return this;
    }

    public boolean isAssignableFrom(Class<?> clazz) {
        return this.dataClassDefinition.isAssignableFrom(clazz);
    }

    public boolean isMergeable(FlowPropertyDefinition property) {
        if (!(property instanceof FlowPropertyDefinitionImpl)) {
            return false;
        }
        FlowPropertyDefinitionImpl source = (FlowPropertyDefinitionImpl)property;
        boolean result = dataClassDefinition.isMergable(source.dataClassDefinition);
        result &= this.flowPropertyValueProvider == null || source.flowPropertyValueProvider == null || this.flowPropertyValueProvider.equals(source.flowPropertyValueProvider);
        return result;
    }

    /**
     * For any fields that are not already set in this
     * {@link FlowPropertyDefinitionImpl}, this use previous to supply any missing
     * values.
     *
     * @param property
     * @return true if there is no conflict in the dataClass, true if
     *         this.dataClass cannot be assigned by previous.dataClass
     *         instances.
     */
    public boolean merge(FlowPropertyDefinition property) {
        if (! (property instanceof FlowPropertyDefinitionImpl)) {
            return true;
        }
        FlowPropertyDefinitionImpl source = (FlowPropertyDefinitionImpl)property;
        boolean noMergeConflict = isMergeable(source);
        if (autoCreate == null && source.autoCreate != null) {
            this.setAutoCreate(source.autoCreate);
        }
        this.dataClassDefinition.merge(source.dataClassDefinition);
        if (cacheOnly == null && source.cacheOnly != null) {
            this.setCacheOnly(source.cacheOnly);
        }
        if ( isNotEmpty(source.alternates)) {
            if (alternates == null ) {
                alternates = new HashSet<String>(source.alternates);
            } else {
                alternates.addAll(source.alternates);
            }
        }

        if ( flowPropertyValueProvider == null && source.flowPropertyValueProvider != null ) {
            this.setFlowPropertyValueProvider(source.flowPropertyValueProvider);
        }
        if ( factoryFlowPropertyValueProvider == null && source.factoryFlowPropertyValueProvider != null ) {
            this.setDefaultObject(source.factoryFlowPropertyValueProvider);
        }
        if (initial == null && source.initial != null) {
            this.setInitial(source.initial);
        }
        if (saveBack == null && source.saveBack != null) {
            this.setSaveBack(source.saveBack);
        }
        if (uiComponentParameterName == null && source.uiComponentParameterName != null) {
            uiComponentParameterName = source.uiComponentParameterName;
        }
        if (sensitive == null && source.sensitive != null) {
            this.setSensitive(source.sensitive);
        }
        if (validators == null && source.validators != null) {
            validators = source.validators;
        }
        if (initialOptional == null && source.initialOptional != null) {
            this.setInitialMode(source.initialOptional);
        }
        // TODO : determine how to handle propertyRequired / PropertyUsage which vary between different FAs in the same Flow.
        return noMergeConflict;
    }

    @Override
    public FlowPropertyDefinitionImpl clone() {
        return new FlowPropertyDefinitionImpl(this);
    }

    public void setSaveBack(Boolean saveBack) {
        this.saveBack = saveBack;
    }

    public boolean isSaveBack() {
        if (saveBack != null) {
            return getBoolean(saveBack);
        } else {
            return getDataClassDefinition().isCollection() || JsonSelfRenderer.class.isAssignableFrom(getDataClassDefinition().getDataClass());
        }
    }

    private boolean getBoolean(Boolean b) {
        return b != null && b;
    }

    /**
     * @see #saveBack
     * @param saveBack
     * @return this
     */
    @SuppressWarnings("hiding")
    public FlowPropertyDefinitionImpl initSaveBack(Boolean saveBack) {
        setSaveBack(saveBack);
        return this;
    }

    /**
     * Sets autoCreate to true - meaning that if the property does not exist in
     * the cache, a new instance is created. <p/> Uses
     * {@link #setAutoCreate(boolean)}
     *
     * @return this
     */
    public FlowPropertyDefinitionImpl initAutoCreate() {
        setAutoCreate(true);
        return this;
    }

    /**
     * @param <FA>
     * @param flowPropertyValueProvider the flowPropertyValueProvider to set
     */
    public <FA extends FlowActivity>void setFlowPropertyValueProvider(FlowPropertyValueProvider<FA> flowPropertyValueProvider) {
        this.flowPropertyValueProvider = flowPropertyValueProvider;
    }

    /**
     * @param <FA>
     * @return the flowPropertyValueProvider
     */
    @SuppressWarnings("unchecked")
    public <FA extends FlowActivity> FlowPropertyValueProvider<FA> getFlowPropertyValueProvider() {
        return flowPropertyValueProvider;
    }

    /**
     * @param flowPropertyValueProvider
     * @return this
     */
    @SuppressWarnings("hiding")
    public FlowPropertyDefinitionImpl initFlowPropertyValueProvider(FlowPropertyValueProvider flowPropertyValueProvider) {
        setFlowPropertyValueProvider(flowPropertyValueProvider);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if ( o == null || ! (o instanceof FlowPropertyDefinitionImpl)) {
            return false;
        } else if (o == this) {
            return true;
        }
        FlowPropertyDefinitionImpl flowPropertyDefinition = (FlowPropertyDefinitionImpl) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder()
            .append(this.alternates, flowPropertyDefinition.alternates)
            .append(this.autoCreate, flowPropertyDefinition.autoCreate)
            .append(this.cacheOnly, flowPropertyDefinition.cacheOnly)
            .append(this.dataClassDefinition, flowPropertyDefinition.dataClassDefinition)
            .append(this.flowPropertyValueProvider, flowPropertyDefinition.flowPropertyValueProvider)
            .append(this.initial, flowPropertyDefinition.initial)
            .append(this.initialOptional, this.initialOptional)
            .append(this.name, flowPropertyDefinition.name)
            .append(this.uiComponentParameterName, flowPropertyDefinition.uiComponentParameterName)
                // use getter so that defaults can be calculated.
            .append(this.getPropertyRequired(), flowPropertyDefinition.getPropertyRequired())
            .append(this.getPropertyUsage(), flowPropertyDefinition.getPropertyUsage())
            .append(this.saveBack, flowPropertyDefinition.saveBack)
            .append(this.sensitive, flowPropertyDefinition.sensitive)
            .append(this.validators, flowPropertyDefinition.validators);
        return equalsBuilder.isEquals();
    }

    /**
     * @see org.amplafi.flow.FlowPropertyDefinition#isNamed(java.lang.String)
     */
    @Override
    public boolean isNamed(String possiblePropertyName) {
        if ( isBlank(possiblePropertyName)) {
            return false;
        } else {
            return getName().equals(possiblePropertyName) ||
                this.getAlternates().contains(possiblePropertyName);
        }
    }

}
