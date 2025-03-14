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
import com.ist.DepChain.util.RSAUtils;

public class NodeStarter {

    private static AuthenticatedPerfectLink apLink;
    private static final int BASE_PORT = 5000;
    private static int id;
    public static NodeState nodestate;
    
    public static void main( String[] args ) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: NodeStarter <node_id>");
            return;
        }

        int my_id = Integer.valueOf(args[0]);
        int num_nodes = Integer.valueOf(args[1]);
        id = my_id;
        
        RSAUtils.generateRSAKeyPair(id);
        nodestate = new NodeState(my_id, num_nodes);

        nodestate.privateKey = RSAUtils.readPrivateKey("src/main/java/com/ist/DepChain/keys/" + id + "_priv.key");;

        // AuthenticatedPerfectLink used to communicate with other nodes
        DatagramSocket socket = new DatagramSocket(my_id + BASE_PORT);
        apLink = new AuthenticatedPerfectLink(socket, nodestate);

        BizantineConsensus bizantineConsensus = new BizantineConsensus(nodestate, apLink);

        Listener listener = new Listener(apLink, nodestate, bizantineConsensus);
        Thread thread = new Thread(listener);
        thread.start();

        CommandListener commandListener = new CommandListener(nodestate, apLink);
        Thread commandThread = new Thread(commandListener);
        commandThread.start();
    }
}

