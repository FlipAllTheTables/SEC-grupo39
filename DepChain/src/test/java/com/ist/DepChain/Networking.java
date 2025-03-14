package com.ist.DepChain;

import com.ist.DepChain.links.AuthenticatedPerfectLink;
import com.ist.DepChain.links.FairLossLink;
import com.ist.DepChain.links.StubbornLink;
import com.ist.DepChain.nodes.NodeState;
import com.ist.DepChain.util.RSAUtils;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import static com.ist.DepChain.util.RSAUtils.generateRSAKeyPair;
import static org.junit.jupiter.api.Assertions.*;

class Networking {
    @Test
    void testFairLossLinkMessageSending() throws Exception {
        DatagramSocket senderSocket = new DatagramSocket();
        DatagramSocket receiverSocket = new DatagramSocket(5000); // Listening on port 5000
        FairLossLink fairLossLink = new FairLossLink(senderSocket, new NodeState(0,3));

        String message = "Hello, DepChain!";
        fairLossLink.send(message, 5000);

        byte[] buffer = new byte[10000];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        receiverSocket.receive(packet);

        String receivedMessage = new String(packet.getData(), 0, packet.getLength());
        assertEquals(message, receivedMessage, "Message should be received correctly");
    }

    @Test
    void testStubbornLinkRetriesUntilAck() throws Exception {
        DatagramSocket socket = new DatagramSocket();
        int myId = 1;
        int numNodes = 5;

        NodeState nodeState = new NodeState(myId, numNodes);
        StubbornLink stubbornLink = new StubbornLink(socket, nodeState);

        String message = "Test Retry";
        int testPort = 5000;

        // Simulate a successful ACK by adding seqNum to acks
        nodeState.acks.add(0);

        // Send should complete without infinite retries
        assertDoesNotThrow(() -> stubbornLink.send(message, testPort));
    }

   /* @Test
    void testAuthenticatedMessageSigningAndVerification() throws Exception {
        // Simulate a node state with an RSA private key
        int id = 1;
        NodeState nodeState = new NodeState(1,5);
        nodeState.privateKey = generateRSAKeyPair(id).getPrivate();

        DatagramSocket socket = new DatagramSocket();
        AuthenticatedPerfectLink authLink = new AuthenticatedPerfectLink(socket, nodeState);

        String message = "Secure Message";
        String signedMessage = RSAUtils.signMessage(message,nodeState.privateKey,"SHA256withRSA");

        assertTrue(RSAUtils.verifySignature(signedMessage), "Message should be successfully verified");
    }*/
}
