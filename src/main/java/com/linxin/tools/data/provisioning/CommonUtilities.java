package com.linxin.tools.data.provisioning;

import com.linxin.tools.data.provisioning.model.Source;
import com.yell.provisioning.bindings.branches.content.BranchContent;
import com.yell.provisioning.bindings.content.State;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

public class CommonUtilities {

    @SuppressWarnings("unused")
    final static Logger logger = LoggerFactory.getLogger(CommonUtilities.class);

    public static void mergeBranchContent(BranchContent supplier, BranchContent target) throws BranchContentBeanException {
        Validate.notNull(target, "target can't be null");
        Validate.notNull(supplier, "supplier can't be null");

        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(target.getClass());

            // Iterate over all the attributes
            for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {

                // Only copy writable attributes
                if (descriptor.getWriteMethod() != null) {
                    Object supplierValue = descriptor.getReadMethod().invoke(supplier);

                    // Only carry on if the suppier's value is not null and the state is Active
                    if (supplierValue != null) {
                        descriptor.getWriteMethod().invoke(target, supplierValue);
                    }
                }
            }
        } catch (IntrospectionException | ReflectiveOperationException e) {
            throw new BranchContentBeanException("Unable to handle BranchContent " + target.getClass(), e);
        }
    }

    public static BranchContent extractRequiredUpdates(BranchContent goldBranchContent, BranchContent ghostBranchContent,
                                                       String goldClassification, String ghostClassification) throws BranchContentBeanException {
        Validate.notNull(goldBranchContent, "goldBranchContent can't be null");
        Validate.notNull(ghostBranchContent, "ghostBranchContent can't be null");
        Validate.notNull(goldClassification, "goldClassification can't be null");
        Validate.notNull(ghostClassification, "ghostClassification can't be null");

        BranchContent target = new BranchContent();

        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(BranchContent.class);

            // Iterate over all the attributes
            for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {

                // Only copy writable attributes
                if (descriptor.getWriteMethod() != null) {

                    String typeName = descriptor.getWriteMethod().getGenericParameterTypes()[0].getTypeName();
                    String simpleTypeName = typeName.substring(typeName.lastIndexOf(".") + 1);

                    Object goldValue = descriptor.getReadMethod().invoke(goldBranchContent);
                    Object ghostValue = descriptor.getReadMethod().invoke(ghostBranchContent);

                    NatData goldData = getNatData(goldValue);
                    NatData ghostData = getNatData(ghostValue);

//                DecisionData decisionData = new DecisionData(
//                        goldData.value, goldData.state, goldData.source, goldData.updatedDate, goldClassification,
//                        ghostData.value, ghostData.state, ghostData.source, ghostData.updatedDate, ghostClassification,
//                        simpleTypeName);
//
//                // Apply the 'common' rule(s) to decide whether to copy
//                Optional<Boolean> copy = new CommonCopyDecisionStrategy().execute(decisionData);
//
//                // If no decision has been made then invoke the datatype specific decision
//                if (!copy.isPresent()) {
//                    copy = StrategyFactory.createContentCopyDecisionStrategy(simpleTypeName).execute(decisionData);
//                }
//
//                if (copy.isPresent() && copy.get()) {
//                    StrategyFactory.createContentCopyExecutionStrategy(simpleTypeName).execute(decisionData, descriptor, target);
//                }
                }
            }
        } catch (IntrospectionException | ReflectiveOperationException e) {
            throw new BranchContentBeanException("Unable to handle BranchContent " + goldBranchContent.getClass(), e);
        }
        return target;
    }

    private static NatData getNatData(Object value)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (value == null) {
            return new NatData();
        } else {
            Method dateMethod = value.getClass().getMethod("getMlUpdated");
            Method sourceMethod = value.getClass().getMethod("getSource");
            Method stateMethod = value.getClass().getMethod("getState");
            return new NatData(
                    value,
                    (Date) dateMethod.invoke(value),
                    Source.getEnum((String) sourceMethod.invoke(value)),
                    (State) stateMethod.invoke(value));
        }
    }


    private static class NatData {
        private final Object value;
        private final Date updatedDate;
        private final Source source;
        private final State state;

        private NatData() {
            this(null, null, null, null);
        }

        private NatData(Object value, Date updatedDate, Source source, State state) {
            this.value = value;
            this.updatedDate = updatedDate;
            this.source = source;
            this.state = state;
        }
    }

}
