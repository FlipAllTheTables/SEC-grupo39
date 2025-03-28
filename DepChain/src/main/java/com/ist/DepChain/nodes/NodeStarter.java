package com.ist.DepChain.nodes;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.ArrayList;
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

import com.ist.DepChain.client.Client;
import com.ist.DepChain.links.AuthenticatedPerfectLink;
import com.ist.DepChain.nodes.Listener;
import com.ist.DepChain.*;
import com.ist.DepChain.besu.ManageContracts;

public class NodeStarter {

    private static AuthenticatedPerfectLink apLink;
    private static final int BASE_PORT = 5000;
    private static int id;
    public static NodeState nodestate;
    
    public static void main( String[] args ) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: NodeStarter <node_id> <num_nodes> <is_byzantine> <is_client>");
            return;
        }

        id = Integer.valueOf(args[0]);
        int num_nodes = Integer.valueOf(args[1]);
        
        boolean isByzantine = args[2].equals("1") ? true : false;
        
        generateRSAKeys();
        nodestate = new NodeState(id, num_nodes, isByzantine);

        PrivateKey privKey = (PrivateKey) readRSA("src/main/java/com/ist/DepChain/keys/" + id + "_priv.key", "priv");
        nodestate.privateKey = privKey;

        ManageContracts manageContracts = new ManageContracts();
        manageContracts.parseGenesisBlock("src/main/java/com/ist/DepChain/genesis/genesis.json");

        // AuthenticatedPerfectLink used to communicate with other nodes
        DatagramSocket socket = new DatagramSocket(id + BASE_PORT);
        apLink = new AuthenticatedPerfectLink(socket, nodestate, privKey);

        if (args[3].equals("1")) {
            nodestate.isClient = true;
            new Client(apLink, nodestate);
        }

        BizantineConsensus bizantineConsensus = new BizantineConsensus(nodestate, apLink);

        Listener listener = new Listener(apLink, nodestate, bizantineConsensus);
        Thread thread = new Thread(listener);
        thread.start();

        // For each node that exists, send a message with "ESTABLISH" command to begin connection
        for (int i = 0; i < id && i < num_nodes; i++) {
            nodestate.acks.put(i, new ArrayList<>()); // Create an association between node and awaited acknowledgements
            String message = "ESTABLISH|" + id + "|" + nodestate.seqNum + "|establish";
            apLink.send(message, BASE_PORT + i);
        }

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

