package com.ist.DepChain.nodes;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.PublicKey;

import javax.xml.crypto.Data;

import com.ist.DepChain.links.AuthenticatedPerfectLink;

public class NodeStarter {

    private static AuthenticatedPerfectLink apLink;
    private static final int BASE_PORT = 5000;
    private static int id;
    
    public static void main( String[] args ) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: NodeStarter <node_id>");
            return;
        }
        NodeState nodestate = new NodeState();

        int my_id = Integer.valueOf(args[0]);
        id = my_id;

        // AuthenticatedPerfectLink used to communicate with other nodes
        apLink = new AuthenticatedPerfectLink(BASE_PORT + id);

        generateRSAKeys();
    }

    public static void generateRSAKeys() throws GeneralSecurityException, IOException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096);
        KeyPair keys = keyGen.generateKeyPair();

        PrivateKey privKey = keys.getPrivate();
        byte[] privKeyEncoded = privKey.getEncoded();

        try (FileOutputStream privFos = new FileOutputStream("src/main/java/com/ist/DepChain/keys/" + id + "_priv.key")) {
            privFos.write(privKeyEncoded);
        }

        PublicKey pubKey = keys.getPublic();
        byte[] pubKeyEncoded = pubKey.getEncoded();

        try (FileOutputStream pubFos = new FileOutputStream("src/main/java/com/ist/DepChain/keys/" + id + "_pub.key")) {
            pubFos.write(pubKeyEncoded);
        }

        
    }

    /**
     * Listener class that implements runnable to continuously wait for incoming messages
     */
    class Listener implements Runnable {

        public void run() {
            nodestate.acks.add(id);
        }

    }
}

