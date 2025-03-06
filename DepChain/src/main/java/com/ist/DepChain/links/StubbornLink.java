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
        int seqNum = nodestate.seqNum++;
        nodestate.acks.add(seqNum);
        while(nodestate.acks.contains(seqNum)) { //!acknowledged
            try {
                fairLossLink.send(m, port); // add seq to message
            } catch (Exception e) {
                continue;
            }
        }
    }

    public DatagramPacket deliver() throws Exception {
        System.out.println("SL: Delivering message");
        return fairLossLink.deliver();
    }
}
