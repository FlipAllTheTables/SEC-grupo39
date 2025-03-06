package com.ist.DepChain.links;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Base64;
import java.security.PrivateKey;
import java.security.Signature;

import com.ist.DepChain.nodes.NodeState;

public class AuthenticatedPerfectLink {

    private StubbornLink stubbornLink;
    private ArrayList<DatagramPacket> delivered;
    private final String signAlgo = "SHA256withRSA";
    private PrivateKey privKey;

    public AuthenticatedPerfectLink(DatagramSocket socket, NodeState nodeState, PrivateKey privKey) throws SocketException {
        stubbornLink = new StubbornLink(socket, nodeState);
        delivered = new ArrayList<>();
        this.privKey = privKey;
    }

    public void send(String m, int port) throws Exception {
        String signature = authenticate(m);
        stubbornLink.send(m + "|" + signature, port); // add signature to message m
    }

    public DatagramPacket deliver() throws Exception {
        System.out.println("APL: Delivering message");
        DatagramPacket dp = stubbornLink.deliver();
        if (verifyAuth(dp) && !delivered.contains(dp)) {
            delivered.add(dp);
            return dp;
        }
        return dp;
    }

    /**
     * Method that creates a digitial signature of message as a Base64 String
     */
    private String authenticate(String m) throws Exception {
        Signature signMaker = Signature.getInstance(signAlgo);
        signMaker.initSign(privKey);
        signMaker.update(m.getBytes());
        byte[] signature = signMaker.sign();
        return new String(Base64.getEncoder().encode(signature));
;
    }

    /**
     * Method that verifies if the digital signature inside a DatagramPacket matches the expected signature from
     * 
     */
    private boolean verifyAuth(DatagramPacket dp) {
        String message = new String(dp.getData(), 0, dp.getLength());

        return true;
    }

    public void sendAck(int seq, int port) {

    }
    
}
