package com.ist.DepChain.links;

import java.net.SocketException;

public class AuthenticatedPerfectLink {

    private StubbornLink stubbornLink;

    public AuthenticatedPerfectLink(int port) throws SocketException {
        stubbornLink = new StubbornLink(port);
    }
    
}
