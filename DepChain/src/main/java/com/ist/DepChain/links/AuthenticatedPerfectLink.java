package com.ist.DepChain.links;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import com.ist.DepChain.nodes.NodeState;
import com.ist.DepChain.util.RSAUtils;

public class AuthenticatedPerfectLink {

    private StubbornLink stubbornLink;
    private ArrayList<DatagramPacket> delivered;
    private final String signAlgo = "SHA256withRSA";
    private PrivateKey privKey;
    private NodeState nodeState;
    private static final int BASE_PORT = 5000;

    private static final Map<String, PublicKey> publicKeyCache = new HashMap<>(); // Cache for public keys

    public AuthenticatedPerfectLink(DatagramSocket socket, NodeState nodeState) throws SocketException {
        stubbornLink = new StubbornLink(socket, nodeState);
        delivered = new ArrayList<>();
        this.privKey = nodeState.privateKey;
        this.nodeState = nodeState;
    }

    public void send(String m, int port) throws Exception {
        String signature = RSAUtils.signMessage(m, privKey, signAlgo);
        stubbornLink.send(m + "|" + signature, port); // add signature to message m
    }

    public DatagramPacket deliver() throws Exception {
        DatagramPacket dp = stubbornLink.deliver();
        if (verifyAuth(dp) && !delivered.contains(dp)) {
            delivered.add(dp);
            return dp;
        } else {
            return null;
        }
    }

    /**
     * Verifies if the digital signature inside a DatagramPacket matches the expected signature.
     */
    private boolean verifyAuth(DatagramPacket dp) throws Exception {
        String packetString = new String(dp.getData(), 0, dp.getLength());
        String[] parts = packetString.split("\\|", 6);
        String command = parts[0];
        String sender = parts[1];
        String seqNum = parts[2];
        String message;
        String signature;
        StringBuilder content = new StringBuilder();

        if (command.equals("APPEND") || command.equals("INNIT") || command.equals("ACK") || command.equals("TEST")) {
            message = parts[3];
            signature = parts[4];
            content.append(command).append("|").append(sender).append("|")
                    .append(seqNum).append("|").append(message);
        } else {
            String consensusRun = parts[3];
            message = parts[4];
            signature = parts[5];
            content.append(command).append("|").append(sender).append("|")
                    .append(seqNum).append("|").append(consensusRun).append("|").append(message);
        }

        // Check if the public key is already cached
        PublicKey pubKey = publicKeyCache.get(sender);
        if (pubKey == null) {
            // Load and cache the key if not found
            pubKey = RSAUtils.readPublicKey("src/main/java/com/ist/DepChain/keys/" + sender + "_pub.key");
            publicKeyCache.put(sender, pubKey);
        }

        return RSAUtils.verifySignature(content.toString(),signature, pubKey, signAlgo);

        /*Signature signMaker = Signature.getInstance(signAlgo);
        signMaker.initVerify(pubKey);
        signMaker.update(content.toString().getBytes());

        return signMaker.verify(Base64.getDecoder().decode(signature.getBytes()));*/
    }

    public void sendAck(int seqNum, int senderId) throws Exception {
        String m = "ACK|" + nodeState.myId + "|" + seqNum + "|ack";
        String signature = RSAUtils.signMessage(m, privKey, signAlgo);
        stubbornLink.sendAck(m + "|" + signature, BASE_PORT + senderId);
    }
}