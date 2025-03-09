package com.ist.DepChain.nodes;

import java.util.ArrayList;
import java.util.List;

import com.ist.DepChain.links.AuthenticatedPerfectLink;
import com.ist.DepChain.util.Pair;

public class BizantineConsensus {
    private NodeState nodestate;
    private AuthenticatedPerfectLink apLink;
    private static final int BASE_PORT = 5000;
    private List<List<Integer>> storedValts;
    private List<List<String>> storedVal;
    private List<List<List<Pair>>> storedConsensusPairs;

    public BizantineConsensus(NodeState nodeState, AuthenticatedPerfectLink apLink) {
        this.nodestate = nodeState;
        this.apLink = apLink;
        storedConsensusPairs = new ArrayList<>();
        storedValts = new ArrayList<>();
        storedVal = new ArrayList<>();
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
        String content = message.substring(1, message.length() - 1);

        // Split the string by commas, limit to 3 parts to handle the list of pairs correctly
        String[] parts = content.split(",", 3);

        // Extract valts, val, and the list of pairs
        String valts = parts[0].trim();
        String val = parts[1].trim();
        String pairsList = parts[2].trim();

        // Remove the square brackets and split the pairs
        pairsList = pairsList.substring(1, pairsList.length() - 1);
        String[] pairs = pairsList.split("\\}, \\{");

        // List to store the parsed pairs
        List<Pair> parsedPairs = new ArrayList<>();

        // Parse each pair
        for (String pair : pairs) {
            // Remove any remaining curly braces
            pair = pair.replace("{", "").replace("}", "").trim();
            String[] pairParts = pair.split(",");
            int ts = Integer.parseInt(pairParts[0].trim());
            String pairVal = pairParts[1].trim();
            parsedPairs.add(new Pair(ts, pairVal));
        }

        if (storedValts.get(consensusIndex) == null){
            storedValts.add(consensusIndex, new ArrayList<>());
        }
        storedValts.get(consensusIndex).add(Integer.parseInt(valts));

        if (storedVal.get(consensusIndex) == null){
            storedVal.add(consensusIndex, new ArrayList<>());
        }
        storedVal.get(consensusIndex).add(val);

        if (storedConsensusPairs.get(consensusIndex) == null){
            storedConsensusPairs.add(consensusIndex, new ArrayList<>());
        }
        storedConsensusPairs.get(consensusIndex).add(parsedPairs);

        if (storedValts.get(consensusIndex).size() == nodestate.numNodes - 1){
            //Bizantine consensus as been reached
        }
    }
}
