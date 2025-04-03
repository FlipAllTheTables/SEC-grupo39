package com.ist.DepChain.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ist.DepChain.links.AuthenticatedPerfectLink;
import com.ist.DepChain.nodes.NodeState;

public class Client {
    private AuthenticatedPerfectLink apLink;
    NodeState nodeState;
    ClientCommandListener commandListener;

    public Client (AuthenticatedPerfectLink aplink, NodeState nodeState, HashMap<String, String> methodIds) {
        this.apLink = aplink;
        this.nodeState = nodeState;
        List<String> ownedAccounts = new ArrayList<>();
        if(nodeState.myId == nodeState.numNodes) {
            ownedAccounts.add("0000000000000000000000000000000000000001");
        }
        else if (nodeState.myId == nodeState.numNodes + 1) {
            ownedAccounts.add("0000000000000000000000000000000000000002");
            ownedAccounts.add("0000000000000000000000000000000000000004");
        }
        else if (nodeState.myId == nodeState.numNodes + 2) {
            ownedAccounts.add("0000000000000000000000000000000000000003");
        }
        else if (nodeState.myId == nodeState.numNodes + 3) {
            ownedAccounts.add("0000000000000000000000000000000000000004");
            ownedAccounts.add("0000000000000000000000000000000000000003");
        }
        else {
        }
        this.commandListener = new ClientCommandListener(nodeState, apLink, methodIds, ownedAccounts);
        start(methodIds);
    }

    private void start(HashMap<String, String> methodIds) {
        Thread commandListenerThread = new Thread(commandListener);
        commandListenerThread.start();
    }
}
