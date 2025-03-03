package com.ist.DepChain.links;

import java.net.DatagramPacket;
import java.net.SocketException;

public class StubbornLink {

    private FairLossLink fairLossLink;

    public StubbornLink(int port) throws SocketException {
        fairLossLink = new FairLossLink(port);
    }

    public void send(String m, int port) {}

    public DatagramPacket deliver() {
        return null;
    }
    
}
