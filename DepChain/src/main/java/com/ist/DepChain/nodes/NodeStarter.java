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
import java.util.HashMap;
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

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;
    private static final String KEY_FILE = "src/main/java/com/ist/DepChain/keys/";
    private static HashMap<String, String> methodIds;
    
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

        methodIds = new HashMap<>();
        ManageContracts manageContracts = new ManageContracts();
        methodIds = manageContracts.parseGenesisBlock("src/main/java/com/ist/DepChain/genesis_block/genesis_block.json", nodestate);

        // AuthenticatedPerfectLink used to communicate with other nodes
        DatagramSocket socket = new DatagramSocket(id + BASE_PORT);
        apLink = new AuthenticatedPerfectLink(socket, nodestate, privKey);

        if (args[3].equals("1")) {
            nodestate.isClient = true;
            new Client(apLink, nodestate, methodIds);
        }

        BizantineConsensus bizantineConsensus = new BizantineConsensus(nodestate, apLink, manageContracts);

        Listener listener = new Listener(apLink, nodestate, bizantineConsensus);
        Thread thread = new Thread(listener);
        thread.start();

        // For each node that exists, send a message with "ESTABLISH" command to begin connection
        for (int i = 0; i < id && i < num_nodes; i++) {
            int index = i;
            new Thread(() -> {
                try{
                    String encodedKey = generateSymKey(index);
                    nodestate.acks.put(index, new ArrayList<>()); // Create an association between node and awaited acknowledgements
                    String message = "ESTABLISH|" + id + "|" + nodestate.seqNum + "|" + encodedKey;
                    apLink.send(message, BASE_PORT + index);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
        }

        if(args[3].equals("0")){
            CommandListener commandListener = new CommandListener(nodestate, apLink);
            Thread commandThread = new Thread(commandListener);
            commandThread.start();
        }

    }

    public static String generateSymKey(int node) throws NoSuchAlgorithmException, IOException {
        // Generate key
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        SecretKey secretKey = keyGenerator.generateKey();

        // Encode key as Base64 (optional, for readability)
        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

        nodestate.sharedKeys.put(node, new SecretKeySpec(encodedKey.getBytes(), "AES"));

        System.out.println("Key saved to " + KEY_FILE);
        return encodedKey;
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

