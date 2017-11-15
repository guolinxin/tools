package com.linxin.tools.security;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Random;


public class PBE {
    /**
     * Support following encryption modes
     * <p/>
     * <pre>
     * PBEWithMD5AndDES
     * PBEWithMD5AndTripleDES
     * PBEWithSHA1AndDESede
     * PBEWithSHA1AndRC2_40
     * </pre>
     */
    private final static String KEY_PBE = "PBEWITHMD5andDES";

    private final static int SALT_COUNT = 100;

    /**
     * Init salt
     *
     * @return
     */
    public static byte[] init() {
        byte[] salt = new byte[8];
        Random random = new Random();
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Get Key
     *
     * @param key
     * @return
     */
    public static Key stringToKey(String key) {
        try {
            PBEKeySpec keySpec = new PBEKeySpec(key.toCharArray());
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_PBE);
            return factory.generateSecret(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error get key, caused by: " + e.getCause());
        }

    }

    /**
     * Processing data encryption using Password Based Encryption
     *
     * @param data
     * @param key
     * @param salt
     * @return
     */
    public static String encryptPBE(String data, String key, byte[] salt) {
        if (StringUtils.isBlank(data)) {
            throw new IllegalArgumentException("Invalid input data");
        }

        try {
            // Get Key
            Key k = stringToKey(key);
            PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, SALT_COUNT);
            Cipher cipher = Cipher.getInstance(KEY_PBE);
            cipher.init(Cipher.ENCRYPT_MODE, k, parameterSpec);
            byte[] bytes = cipher.doFinal(data.getBytes());

            return Base64.getEncoder().encodeToString(bytes);

        } catch (NoSuchAlgorithmException
                | NoSuchPaddingException
                | InvalidAlgorithmParameterException
                | InvalidKeyException
                | BadPaddingException
                | IllegalBlockSizeException e) {

            throw new RuntimeException("Error encrypting data, caused by: " + e.getCause());
        }

    }

    /**
     * Processing data decryption using Password Based Encryption
     *
     * @param data
     * @param key
     * @param salt
     * @return
     */
    public static String decryptPBE(String data, String key, byte[] salt) {

        if (StringUtils.isBlank(data)) {
            throw new IllegalArgumentException("Invalid input data");
        }

        try {
            // Get key
            Key k = stringToKey(key);
            PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, SALT_COUNT);
            Cipher cipher = Cipher.getInstance(KEY_PBE);
            cipher.init(Cipher.DECRYPT_MODE, k, parameterSpec);

            byte[] decodedValue = Base64.getDecoder().decode(data);
            byte[] decryptedValue = cipher.doFinal(decodedValue);

            return new String(decryptedValue);

        } catch (NoSuchAlgorithmException
                | NoSuchPaddingException
                | InvalidAlgorithmParameterException
                | InvalidKeyException
                | BadPaddingException
                | IllegalBlockSizeException e) {

            throw new RuntimeException("Error decrypting data, caused by: " + e.getCause());
        }

    }


    public static void main(String[] args) {
        String pass = "wellie";
        String key = "ironman";

        byte[] salt = init();

        String encData = encryptPBE(pass, key, salt);
        String decData = decryptPBE(encData, key, salt);


        System.out.println("Before Encryption：" + pass);
        System.out.println("After Encryption：" + encData);
        System.out.println("After Decryption：" + decData);
    }
}
