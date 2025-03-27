package com.ist.DepChain.client;

import com.ist.DepChain.links.AuthenticatedPerfectLink;
import com.ist.DepChain.nodes.NodeState;

public class Client {
    private AuthenticatedPerfectLink apLink;
    NodeState nodeState;
    ClientCommandListener commandListener;

    public Client (AuthenticatedPerfectLink aplink, NodeState nodeState) {
        this.apLink = aplink;
        this.nodeState = nodeState;
        this.commandListener = new ClientCommandListener(nodeState, apLink);
        start();
    }

    private void start() {
        Thread commandListenerThread = new Thread(commandListener);
        commandListenerThread.start();
    }
}
