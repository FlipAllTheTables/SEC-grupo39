package com.ist.DepChain.nodes;
import java.net.DatagramSocket;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;

import com.ist.DepChain.client.Client;
import com.ist.DepChain.links.AuthenticatedPerfectLink;
import com.ist.DepChain.util.KeysUtil;

import com.ist.DepChain.besu.ManageContracts;

public class NodeStarter {

    private static AuthenticatedPerfectLink apLink;
    private static final int BASE_PORT = 5000;
    private static int id;
    public static NodeState nodestate;
    private static HashMap<String, String> methodIds;
    
    public static void main( String[] args ) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: NodeStarter <node_id> <num_nodes> <test_mode> <is_client>");
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

        ByzantineConsensus byzantineConsensus = new ByzantineConsensus(nodestate, apLink, manageContracts);

        Listener listener = new Listener(apLink, nodestate, byzantineConsensus);
        Thread thread = new Thread(listener);
        thread.start();

        // For each node that exists, send a message with "ESTABLISH" command to begin connection
        for (int i = 0; i < id && i < num_nodes; i++) {
            int index = i;
            new Thread(() -> {
                try{
                    String encodedKey = KeysUtil.generateSymKey(index, nodestate);
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

}

