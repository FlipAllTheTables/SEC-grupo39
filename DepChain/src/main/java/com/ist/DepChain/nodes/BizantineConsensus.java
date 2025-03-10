package com.ist.DepChain.nodes;

import java.util.ArrayList;
import java.util.List;

import com.ist.DepChain.links.AuthenticatedPerfectLink;
import com.ist.DepChain.util.Pair;

public class BizantineConsensus {
    private NodeState nodestate;
    private AuthenticatedPerfectLink apLink;
    private static final int BASE_PORT = 5000;
    private List<List<String>> storedMessages;

    public BizantineConsensus(NodeState nodeState, AuthenticatedPerfectLink apLink) {
        this.nodestate = nodeState;
        this.apLink = apLink;
        storedMessages = new ArrayList<>();
    }

    public void read() {
        String query = "READ|" + nodestate.myId + "|" + nodestate.seqNum + "|";
        for (int i = 1; i < nodestate.numNodes; i++) {
            final int port = BASE_PORT + i;
            new Thread(() -> {
                try {
                    apLink.send(query, port);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public void state(int senderId){
        String state = "STATE|" + nodestate.myId + "|" + nodestate.seqNum + "|<" + 
        nodestate.valts + "," + nodestate.val + "," + nodestate.consensusPairs + ">";

        try {
            apLink.send(state, BASE_PORT + senderId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void analyseState(String message, int consensusIndex){
        if (storedMessages.get(consensusIndex) == null) {
            storedMessages.set(consensusIndex, new ArrayList<>());
        }
        storedMessages.get(consensusIndex).add(message);

        if (storedMessages.get(consensusIndex).size() == nodestate.quorumSize) {
            broadcastCollected(storedMessages.get(consensusIndex));
        }
    }

    private void broadcastCollected(List<String> messages){
        StringBuilder contentBuilder = new StringBuilder("COLLECTED|" + nodestate.myId + "|" + nodestate.seqNum + "|");
        for (int i = 0; i < messages.size(); i++) {
            contentBuilder.append("<").append(messages.get(i)).append(">");
        }
        final String content = contentBuilder.toString(); // Make content final

        for (int i = 1; i < nodestate.numNodes; i++) {
            final int port = BASE_PORT + i;
            new Thread(() -> {
                try {
                    apLink.send(content, port);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

    }
}
