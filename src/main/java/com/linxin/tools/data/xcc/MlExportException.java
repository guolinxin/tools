package com.linxin.tools.data.xcc;

/**
 *
 */
public class MlExportException extends RuntimeException {

    public MlExportException(String msg) {
        super(msg);
    }

    public MlExportException(String msg, Exception e) {
        super(msg, e);
    }
}
