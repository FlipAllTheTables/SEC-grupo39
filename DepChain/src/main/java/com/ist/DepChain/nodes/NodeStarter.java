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

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.xml.crypto.Data;

import com.ist.DepChain.links.AuthenticatedPerfectLink;
import com.ist.DepChain.nodes.Listener;

public class NodeStarter {

    private static AuthenticatedPerfectLink apLink;
    private static final int BASE_PORT = 5000;
    private static int id;
    public static NodeState nodestate;
    
    public static void main( String[] args ) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: NodeStarter <node_id>");
            return;
        }

        int my_id = Integer.valueOf(args[0]);
        int num_nodes = Integer.valueOf(args[1]);
        
        boolean isBizantine = false;
        if (args[2].equals("1")) {
            isBizantine = true;      
        }

        id = my_id;
        
        generateRSAKeys();
        nodestate = new NodeState(my_id, num_nodes, isBizantine);

        PrivateKey privKey = (PrivateKey) readRSA("src/main/java/com/ist/DepChain/keys/" + id + "_priv.key", "priv");
        nodestate.privateKey = privKey;

        // AuthenticatedPerfectLink used to communicate with other nodes
        DatagramSocket socket = new DatagramSocket(my_id + BASE_PORT);
        apLink = new AuthenticatedPerfectLink(socket, nodestate, privKey);

        BizantineConsensus bizantineConsensus = new BizantineConsensus(nodestate, apLink);

        Listener listener = new Listener(apLink, nodestate, bizantineConsensus);
        Thread thread = new Thread(listener);
        thread.start();

        CommandListener commandListener = new CommandListener(nodestate, apLink);
        Thread commandThread = new Thread(commandListener);
        commandThread.start();
    }

    private static void generateRSAKeys() throws GeneralSecurityException, IOException {
        System.out.println("Generating RSA keys for node " + id);
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
}

