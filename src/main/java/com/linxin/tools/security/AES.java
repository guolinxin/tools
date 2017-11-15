package com.linxin.tools.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

public class AES {

    private static final String AES_MODE = "AES";

    // 16 characters long
    private static final String AES_KEY = "thereisnospoon16";

    /**
     * Generate key for encrypt / decrypt password
     *
     * @return Key
     */
    private static Key generateKey() {
        return new SecretKeySpec(AES_KEY.getBytes(), AES_MODE);
    }

    /**
     * Encrypt string with AES mode
     *
     * @param data
     * @return
     * @throws Exception
     */
    public static String encrypt(String data) throws Exception {
        Key key = generateKey();
        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptValue = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptValue);
    }

    /**
     * Decrypt string with AES mode
     *
     * @param data
     * @return
     * @throws Exception
     */
    public static String decrypt(String data) throws Exception {
        Key key = generateKey();
        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedValue = Base64.getDecoder().decode(data);
        byte[] decryptedValue = cipher.doFinal(decodedValue);
        return new String(decryptedValue);
    }

    public static void main(String[] args) {
        String password1 = "wellie";

        try {
            String encyptPass = AES.encrypt(password1);
            System.out.println(encyptPass);

            System.out.println("*****************");

            String decryptPass = AES.decrypt(encyptPass);

            System.out.println(decryptPass);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
