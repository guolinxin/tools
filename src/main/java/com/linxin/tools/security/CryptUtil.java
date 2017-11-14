
package com.linxin.tools.security;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.apache.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

public class CryptUtil {
    private static Logger logger = Logger.getLogger(CryptUtil.class);
    protected static SecretKeySpec v1Key = null;
    protected static PBEParameterSpec v2obfuscator;
    protected static SecretKey v2Key;

    static {
        try {
            v1Key = new SecretKeySpec("12345678".getBytes(), "DES");
            v2obfuscator = new PBEParameterSpec(new byte[]{-96, -17, 103, -12, -30, -78, -99, 6}, 100);
            v2Key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(new PBEKeySpec("Td$53cr3tK3y^@!u3".toCharArray()));
        } catch (Exception var1) {
            logger.error("Exception while setting up encryption keys", var1);
        }
    }

    private CryptUtil() {
    }

    public static String decryptString(String encodedValue) throws Exception {
        if (encodedValue != null && !encodedValue.equals("")) {
            if (encodedValue.startsWith("v2_")) {
                return decryptV2String(encodedValue.substring(3));
            } else {
                return encodedValue.startsWith("v1_") ? decryptV1String(encodedValue.substring(3)) : decryptV1String(encodedValue);
            }
        } else {
            return "";
        }
    }

    public static String encryptString(String theValue) throws Exception {
        return theValue != null && !theValue.equals("") ? "v2_" + encryptV2String(theValue) : "";
    }

    public static String decryptV2String(String encodedValue) throws Exception {
        try {
            Cipher v2Cipher = Cipher.getInstance("PBEWithMD5AndDES");
            v2Cipher.init(2, v2Key, v2obfuscator);
            byte[] raw = Base64.decode(encodedValue);
            byte[] stringBytes = v2Cipher.doFinal(raw);
            return new String(stringBytes, "UTF8");
        } catch (GeneralSecurityException var4) {
            throw new RuntimeException("Exception while decrypting string", var4);
        } catch (UnsupportedEncodingException var5) {
            throw new RuntimeException("Exception while decrypting string", var5);
        }
    }

    public static String encryptV2String(String theValue) throws Exception {
        try {
            Cipher v2Cipher = Cipher.getInstance("PBEWithMD5AndDES");
            v2Cipher.init(1, v2Key, v2obfuscator);
            byte[] stringBytes = theValue.getBytes("UTF8");
            byte[] raw = v2Cipher.doFinal(stringBytes);
            return Base64.encode(raw);
        } catch (GeneralSecurityException var4) {
            throw new RuntimeException("Exception while encrypting string", var4);
        } catch (UnsupportedEncodingException var5) {
            throw new RuntimeException("Exception while encrypting string", var5);
        }
    }

    public static String decryptV1String(String encodedValue) throws Exception {
        try {
            Cipher v1Cipher = Cipher.getInstance("DES");
            v1Cipher.init(2, v1Key);
            byte[] raw = Base64.decode(encodedValue);
            byte[] stringBytes = v1Cipher.doFinal(raw);
            return new String(stringBytes, "UTF8");
        } catch (GeneralSecurityException var4) {
            throw new RuntimeException("Exception while decrypting string", var4);
        } catch (UnsupportedEncodingException var5) {
            throw new RuntimeException("Exception while decrypting string", var5);
        }
    }

    public static String encryptV1String(String theValue) throws Exception {
        try {
            Cipher v1Cipher = Cipher.getInstance("DES");
            v1Cipher.init(1, v1Key);
            byte[] stringBytes = theValue.getBytes("UTF8");
            byte[] raw = v1Cipher.doFinal(stringBytes);
            return Base64.encode(raw);
        } catch (GeneralSecurityException var4) {
            throw new RuntimeException("Exception while encrypting string", var4);
        } catch (UnsupportedEncodingException var5) {
            throw new RuntimeException("Exception while encrypting string", var5);
        }
    }


}
