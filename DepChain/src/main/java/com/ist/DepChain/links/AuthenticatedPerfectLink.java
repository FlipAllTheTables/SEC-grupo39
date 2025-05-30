package com.ist.DepChain.links;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;

import javax.crypto.Mac;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

import com.ist.DepChain.nodes.NodeState;

public class AuthenticatedPerfectLink {

    private StubbornLink stubbornLink;
    private ArrayList<DatagramPacket> delivered;
    private final String signAlgo = "SHA256withRSA";
    private PrivateKey privKey;
    private NodeState nodeState;
    private static final int BASE_PORT = 5000;

    public AuthenticatedPerfectLink(DatagramSocket socket, NodeState nodeState, PrivateKey privKey) throws SocketException {
        stubbornLink = new StubbornLink(socket, nodeState);
        delivered = new ArrayList<>();
        this.privKey = privKey;
        this.nodeState = nodeState;
    }

    public void send(String m, int port) throws Exception {
        String command = m.split("\\|", 5)[0];
        String signature;

        if(command.equals("ESTABLISH") || command.equals("STATE") || command.equals("TX")){
            signature = authenticateAsym(m);
        }
        else{
            signature = authenticateSym(m, port - BASE_PORT);
            //System.out.println("Signature l49 APL: " + signature);
        }
        if(nodeState.isByzantine == 2){
            Thread.sleep(5000);
        }
        if(nodeState.isByzantine == 3 && !command.equals("ESTABLISH")){
            return;
        }
        stubbornLink.send(m + "|" + signature, port); // add signature to message m
    }

    public DatagramPacket deliver() throws Exception {
        DatagramPacket dp = stubbornLink.deliver();
        if (verifyAuth(dp) && !delivered.contains(dp)) {
            delivered.add(dp);
            return dp;
        }
        else {
            return null;
        }
    }

    /**
     * Method that creates a digitial signature of message as a Base64 String
     */
    private String authenticateAsym(String m) throws Exception {
        Signature signMaker = Signature.getInstance(signAlgo);
        signMaker.initSign(privKey);
        signMaker.update(m.getBytes());
        byte[] signature = signMaker.sign();
        return new String(Base64.getEncoder().encode(signature));
    }

    private String authenticateSym(String m, int otherNode) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(nodeState.sharedKeys.get(otherNode));
        //System.out.println("Signing message: " + m);
        byte[] hmac = mac.doFinal(m.getBytes());
        return new String(Base64.getEncoder().encodeToString(hmac));
    }

    /**
     * Method that verifies if the digital signature inside a DatagramPacket matches the expected signature from
     * 
     */
    private boolean verifyAuth(DatagramPacket dp) throws Exception{
        //System.out.println("Verifying message: " + new String(dp.getData(), 0, dp.getLength()));
        String packeString = new String(dp.getData(), 0, dp.getLength());
        String command = packeString.split("\\|", 5)[0];
        String sender = packeString.split("\\|", 5)[1];
        String seqNum = packeString.split("\\|", 5)[2];
        String message;
        String signature;
        StringBuilder content = new StringBuilder();

        if (command.equals("APPEND") || command.equals("INNIT") || command.equals("ACK") || command.equals("READALL") || command.equals("INFO") || command.equals("TEST") || command.equals("ESTABLISH") || command.equals("TX") || command.equals("ABORT") || command.equals("FEEDBACK")) {
            message = packeString.split("\\|", 5)[3];
            signature = packeString.split("\\|", 5)[4];
            content.append(command).append("|").append(sender).append("|")
            .append(seqNum).append("|").append(message);
        }
        else {
            String consensusRun = packeString.split("\\|", 6)[3];
            message = packeString.split("\\|", 6)[4];
            signature = packeString.split("\\|", 6)[5];
            content.append(command).append("|").append(sender).append("|")
            .append(seqNum).append("|").append(consensusRun).append("|").append(message);
        }

        if(command.equals("ESTABLISH") || command.equals("STATE") || command.equals("TX")){
            Signature signMaker = Signature.getInstance(signAlgo);
            PublicKey pubKey = readPublicKey("src/main/java/com/ist/DepChain/keys/" + sender + "_pub.key");
            signMaker.initVerify(pubKey);
            signMaker.update(content.toString().getBytes());

            return signMaker.verify(Base64.getDecoder().decode(signature.getBytes()));
        }
        else {
            String computedSignature = authenticateSym(content.toString(), Integer.parseInt(sender));

            //System.out.println("Computed signature: " + computedSignature);
            //System.out.println("Received signature: " + signature);
            return computedSignature.equals(signature);
        }
    }

    private PublicKey readPublicKey(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filename)); // Read the binary key file
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    public void sendAck(int seqNum, int senderId) throws Exception {
        String m = "ACK|" + nodeState.myId + "|" + seqNum + "|ack";
        String signature = authenticateSym(m, senderId);
        stubbornLink.sendAck(m + "|" + signature, BASE_PORT + senderId);
    }
}
