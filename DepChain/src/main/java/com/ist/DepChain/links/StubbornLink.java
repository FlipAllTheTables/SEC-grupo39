package com.ist.DepChain.links;

import java.net.DatagramPacket;
import java.net.SocketException;

import com.ist.DepChain.nodes.NodeState;

public class StubbornLink {

    private FairLossLink fairLossLink;
    private boolean acknowledged;
    public NodeState nodestate;

    public StubbornLink(int port) throws SocketException {
        fairLossLink = new FairLossLink(port);
    }

    public void send(String m, int port) {
        acknowledged = false;
        while(nodestate.acks. ) { //!acknowledged
            try {
                fairLossLink.send(m, port);
                break;
            } catch (Exception e) {
                continue;
            }
        }
    }

    public DatagramPacket deliver() {
        return null;
    }

    public void acknowledge() {
        acknowledged = true;
    }
    
}
