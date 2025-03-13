package com.ist.DepChain.links;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import com.ist.DepChain.nodes.NodeState;

public class StubbornLink {

    private FairLossLink fairLossLink;
    public NodeState nodestate;
    public DatagramSocket socket;

    public StubbornLink(DatagramSocket socket, NodeState nodestate_) throws SocketException {
        fairLossLink = new FairLossLink(socket, nodestate);
        nodestate = nodestate_;
        this.socket = socket;
    }

    public void send(String m, int port) {
        String[] readableMessage = m.split("\\|");
        int seqNum = Integer.parseInt(readableMessage[2]);
        nodestate.acks.add(seqNum);
        StringBuilder buh = new StringBuilder();
        int i = 0;
        for (String mess : readableMessage) {
            if (i == readableMessage.length - 2) {
                break;
            }
            i++;
            buh.append(mess).append("|");
        }

        System.out.println("Sending message m: " + buh);
    
        while(nodestate.acks.contains(seqNum)) { //!acknowledged
            System.out.println(nodestate.acks);
            try {
                fairLossLink.send(m, port); // add seq to message
                Thread.sleep(3000);
            } catch (Exception e) {
                continue;
            }
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
