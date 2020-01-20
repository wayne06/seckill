package com.zs.seckill;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.security.MessageDigest;

public class SecureMethod {
    public static final String KEY_ALGORITHM = "RSA";
    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";

    public static final String PUBLIC_KEY = "RSAPublicKey";
    public static final String PRIVATE_KEY = "RSAPrivateKey";
    public static final String KEY_SHA = "SHA";
    public static final String KEY_MD5 = "MD5";

    public static byte[] decryptBASE64(String key) throws Exception {
        return (new BASE64Decoder()).decodeBuffer(key);
    }

    public static String encryptBASE64(byte[] key) throws Exception {
        return (new BASE64Encoder()).encode(key);
    }

    public static byte[] encryptMD5(byte[] data) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance(KEY_MD5);
        md5.update(data);
        return md5.digest();
    }


}
