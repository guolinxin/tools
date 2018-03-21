package com.linxin.tools.data.provisioning;

import com.linxin.tools.data.provisioning.model.Source;
import com.yell.provisioning.bindings.branches.content.BranchContent;
import com.yell.provisioning.bindings.branches.content.Image;
import com.yell.provisioning.bindings.branches.content.Images;
import com.yell.provisioning.bindings.branches.content.urls.BranchContentUrls;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This is not a Spring bean so that in can be used in AWS Lambdas.
 */
public class PPAPIService {

    private final PPAPIClient ppapiClient;

    public PPAPIService(PPAPIClient ppapiClient) {
        this.ppapiClient = ppapiClient;
    }


    public void copyContent(int sourceBranch, String sourceBranchClassification,
                            int targetBranch, String targetBranchClassification) throws Exception {
        BranchContentUrls ghostContent = ppapiClient.getContentURLsForBranch(sourceBranch);
        BranchContentUrls goldenContent = ppapiClient.getContentURLsForBranch(targetBranch);

        BranchContent branchContentToUpdate = calculateContentToUpdate(goldenContent, ghostContent,
                targetBranchClassification, sourceBranchClassification);

        writeBranchContent(targetBranch, branchContentToUpdate);
    }


    private void writeBranchContent(int goldenNatId, BranchContent mergedBranchContent)
            throws BranchContentBeanException {

        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(mergedBranchContent.getClass());

            // Iterate over all the attributes
            for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {

                if (descriptor.getWriteMethod() != null) {
                    Object value = descriptor.getReadMethod().invoke(mergedBranchContent);

                    if (value != null) {
                        Method method = value.getClass().getMethod("getSource");
                        Source source = Source.getEnum((String) method.invoke(value));

                        String type = value.getClass().getSimpleName();

                        if (type.equals("Images")) {
                            if (source == Source.generated || source == Source.unknown)
                                source = Source.webcaptureadmin;

                            Images images = (Images) value;
                            for (Image image : images.getImage()) {
                                image.setSource(Source.webcaptureadmin.getMoniker());
                            }
                        } else if (isMergedType(type) && source == Source.unknown) {
                            source = Source.webcaptureadmin;
                        }

                        BranchContent branchContent = new BranchContent();
                        descriptor.getWriteMethod().invoke(branchContent, value);

                        ppapiClient.writeBranchContent(branchContent, goldenNatId, source.getMoniker(), type);
                    }
                }
            }
        } catch (IntrospectionException | ReflectiveOperationException e) {
            throw new BranchContentBeanException("Unable to handle BranchContent " + mergedBranchContent.getClass(), e);
        }
    }


    private boolean isMergedType(final String type) {
        return type.equalsIgnoreCase("Videos") || type.equalsIgnoreCase("Urls");
    }


    private BranchContent calculateContentToUpdate(BranchContentUrls goldenContentUrls,
                                                   BranchContentUrls ghostContentUrls, String goldClassification,
                                                   String ghostClassification) throws BranchContentBeanException {

        BranchContent goldenBranchContent = coalesceBranchContentUrlsIntoBranchContent(goldenContentUrls);
        BranchContent ghostBranchContent = coalesceBranchContentUrlsIntoBranchContent(ghostContentUrls);

        return CommonUtilities.extractRequiredUpdates(goldenBranchContent, ghostBranchContent, goldClassification,
                ghostClassification);
    }


    public BranchContent coalesceBranchContentUrlsIntoBranchContent(BranchContentUrls branchContentUrls)
            throws BranchContentBeanException {
        BranchContent coalescedBranchContent = new BranchContent();

        List<String> processedTypes = new ArrayList<>();
        for (String branchContentUrl : branchContentUrls.getBranchContentUrl()) {

            String type = branchContentUrl.substring(branchContentUrl.lastIndexOf("-") + 1);

            if (!processedTypes.contains(type)) {
                Optional<BranchContent> branchContent = ppapiClient.getCanonicalBranchContent(branchContentUrl);

                if (branchContent.isPresent()) {
                    CommonUtilities.mergeBranchContent(branchContent.get(), coalescedBranchContent);

                    // Since we get the canonical there is no need to do a particular type more than once.
                    processedTypes.add(type);
                }
            }
        }

        return coalescedBranchContent;
    }

}
