package com.ist.DepChain.util;

import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileOutputStream;

public class RSAUtils {

    /**
     * Generates a 4096-bit RSA key pair.
     */
    public static KeyPair generateRSAKeyPair(int id) throws GeneralSecurityException, IOException {
        System.out.println("Generating RSA keys for node " + id);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096);
        KeyPair keys = keyGen.generateKeyPair();

        // Ensure the directory exists
        File keyDir = new File("src/main/java/com/ist/DepChain/keys/");
        if (!keyDir.exists()) {
            keyDir.mkdirs(); // Create the directory if it does not exist
        }

        // Write private key to file
        PrivateKey privKey = keys.getPrivate();
        byte[] privKeyEncoded = privKey.getEncoded();

        try (FileOutputStream privFos = new FileOutputStream("src/main/java/com/ist/DepChain/keys/" + id + "_priv.key")) {
            privFos.write(privKeyEncoded);
        }

        // Write public key to file
        PublicKey pubKey = keys.getPublic();
        byte[] pubKeyEncoded = pubKey.getEncoded();

        try (FileOutputStream pubFos = new FileOutputStream("src/main/java/com/ist/DepChain/keys/" + id + "_pub.key")) {
            pubFos.write(pubKeyEncoded);
        }

        return keys;
    }

    /**
     * Signs a message using the given private key.
     */
    public static String signMessage(String message, PrivateKey privateKey, String algorithm) throws Exception {
        System.out.println("Authenticating msg: " + message);

        Signature signMaker = Signature.getInstance(algorithm);
        signMaker.initSign(privateKey);
        signMaker.update(message.getBytes());
        return Base64.getEncoder().encodeToString(signMaker.sign());
    }

    /**
     * Verifies a signed message using the given public key.
     */
    public static boolean verifySignature(String message, String signature, PublicKey publicKey, String algorithm) throws Exception {
        Signature signChecker = Signature.getInstance(algorithm);
        signChecker.initVerify(publicKey);
        signChecker.update(message.getBytes());
        return signChecker.verify(Base64.getDecoder().decode(signature));
    }

    /**
     * Reads a public key from a file.
     */
    public static PublicKey readPublicKey(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    /**
     * Reads a private key from a file.
     */
    public static PrivateKey readPrivateKey(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}