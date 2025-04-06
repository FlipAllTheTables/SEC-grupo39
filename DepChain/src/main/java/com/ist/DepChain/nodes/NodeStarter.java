package com.ist.DepChain.nodes;
import java.net.DatagramSocket;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.io.IOException;

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.ist.DepChain.client.Client;
import com.ist.DepChain.links.AuthenticatedPerfectLink;
import com.ist.DepChain.util.KeysUtil;

import com.ist.DepChain.besu.ManageContracts;

public class NodeStarter {

    private static AuthenticatedPerfectLink apLink;
    private static final int BASE_PORT = 5000;
    private static int id;
    public static NodeState nodestate;
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;
    private static HashMap<String, String> methodIds;
    
    public static void main( String[] args ) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: NodeStarter <node_id> <num_nodes> <is_byzantine> <is_client>");
            return;
        }

        id = Integer.valueOf(args[0]);
        int num_nodes = Integer.valueOf(args[1]);
        
        int isByzantine = Integer.parseInt(args[2]);
        
        KeysUtil.generateRSAKeys(Integer.toString(id));
        nodestate = new NodeState(id, num_nodes, isByzantine);

        PrivateKey privKey = (PrivateKey) KeysUtil.readRSA("src/main/java/com/ist/DepChain/keys/" + id + "_priv.key", "priv");
        nodestate.privateKey = privKey;

        // AuthenticatedPerfectLink used to communicate with other nodes
        DatagramSocket socket = new DatagramSocket(id + BASE_PORT);
        apLink = new AuthenticatedPerfectLink(socket, nodestate, privKey);

        methodIds = new HashMap<>();
        ManageContracts manageContracts = new ManageContracts(nodestate, apLink);
        methodIds = manageContracts.parseGenesisBlock("src/main/java/com/ist/DepChain/genesis_block/genesis_block.json", nodestate);

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

        //System.out.println("Key saved to " + KEY_FILE);
        return encodedKey;
    }
}

