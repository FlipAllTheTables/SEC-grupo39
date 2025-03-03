package com.ist.DepChain.links;

import java.net.DatagramPacket;
import java.net.SocketException;

public class AuthenticatedPerfectLink {

    private StubbornLink stubbornLink;

    public AuthenticatedPerfectLink(int port) throws SocketException {
        stubbornLink = new StubbornLink(port);
    }

    public void send(String m, int port) {}

    public DatagramPacket deliver() {
        return null;
    }
    
}
