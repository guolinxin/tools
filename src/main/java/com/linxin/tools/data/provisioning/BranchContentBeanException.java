package com.linxin.tools.data.provisioning;

/**
 * Error in reflection operation on BranchContent bean class.
 * This is a "should not happen" error!
 */
public class BranchContentBeanException extends RuntimeException {

    public BranchContentBeanException(String message, Throwable cause) {
        super(message, cause);
    }

}
