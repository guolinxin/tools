
package com.linxin.tools.security;

import org.apache.log4j.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtil {
    private static Logger LOGGER = Logger.getLogger(EncryptUtil.class);

    protected static SecretKeySpec secretKey = null;
    protected static PBEParameterSpec obfuscator;
    protected static SecretKey v2Key;

    private EncryptUtil() {
    }

}