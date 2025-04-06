package com.ist.DepChain.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.ist.DepChain.nodes.NodeState;

public class KeysUtil {

    private static final String KEY_PATH = "src/main/java/com/ist/DepChain/keys/";
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    private KeysUtil() {}

        public static void generateRSAKeys(String name) throws GeneralSecurityException, IOException {
        //System.out.println("Generating RSA keys for node " + id);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096);
        KeyPair keys = keyGen.generateKeyPair();

        PrivateKey privKey = keys.getPrivate();
        byte[] privKeyEncoded = privKey.getEncoded();

        try (FileOutputStream privFos = new FileOutputStream(KEY_PATH + name + "_priv.key")) {
            privFos.write(privKeyEncoded);
        }

        PublicKey pubKey = keys.getPublic();
        byte[] pubKeyEncoded = pubKey.getEncoded();

        try (FileOutputStream pubFos = new FileOutputStream(KEY_PATH + name + "_pub.key")) {
            pubFos.write(pubKeyEncoded);
        }        
    }

    public static Key readRSA(String keyPath, String type) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encoded;
        try (FileInputStream fis = new FileInputStream(keyPath)) {
            encoded = new byte[fis.available()];
            fis.read(encoded);
        }
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        if (type.equals("pub") ){
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            return keyFactory.generatePublic(keySpec);
        }

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return keyFactory.generatePrivate(keySpec);
    }
    
    public static String generateSymKey(int node, NodeState nodestate) throws NoSuchAlgorithmException, IOException {
        // Generate key
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        SecretKey secretKey = keyGenerator.generateKey();

        // Encode key as Base64 (optional, for readability)
        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

        nodestate.sharedKeys.put(node, new SecretKeySpec(encodedKey.getBytes(), "AES"));

        //System.out.println("Key saved to " + KEY_FILE);
        return encodedKey;
    }
}
