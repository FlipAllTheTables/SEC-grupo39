package com.ist.DepChain.links;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import com.ist.DepChain.nodes.NodeState;

public class StubbornLink {

    private static final int MAX_RETRIES = 5; // Limit retries to prevent infinite loops
    private static final int RETRY_DELAY_MS = 3000; // 3-second wait between retries

    private FairLossLink fairLossLink;
    public NodeState nodestate;
    public DatagramSocket socket;

    public StubbornLink(DatagramSocket socket, NodeState nodestate_) throws SocketException {
        fairLossLink = new FairLossLink(socket, nodestate_);
        nodestate = nodestate_;
        this.socket = socket;
    }

    public void send(String m, int port) {
        String[] readableMessage = m.split("\\|");
        int seqNum = Integer.parseInt(readableMessage[2]);
        nodestate.acks.add(seqNum);
        StringBuilder messageBuilder = new StringBuilder();

        int i = 0;
        for (String part : readableMessage) {
            if (i == readableMessage.length - 2) {
                break;
            }
            i++;
            messageBuilder.append(part).append("|");
        }

        System.out.println("Sending message m: " + messageBuilder);

        int retryCount = 0;
        while (nodestate.acks.contains(seqNum) && retryCount < MAX_RETRIES) {
            System.out.println("Retrying (" + retryCount + "/" + MAX_RETRIES + "): " + messageBuilder);
            try {
                fairLossLink.send(m, port);
                Thread.sleep(RETRY_DELAY_MS);
            } catch (Exception e) {
                e.printStackTrace();
            }
            retryCount++;
        }

        if (nodestate.acks.contains(seqNum)) {
            System.err.println("Max retries reached. Message failed: " + messageBuilder);
            nodestate.acks.remove(Integer.valueOf(seqNum)); // Remove from ack tracking to prevent blocking
        }
    }

    public DatagramPacket deliver() throws Exception {
        return fairLossLink.deliver();
    }

    public void sendAck(String message, int port) throws Exception {
        System.out.println("Sending ack to message: " + message + " to port: " + port);
        fairLossLink.send(message, port);
    }
}