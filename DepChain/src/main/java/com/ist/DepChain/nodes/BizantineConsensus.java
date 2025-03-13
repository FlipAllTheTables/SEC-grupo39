package com.ist.DepChain.nodes;

import java.net.DatagramPacket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ist.DepChain.links.AuthenticatedPerfectLink;
import com.ist.DepChain.util.Pair;

public class BizantineConsensus {
    private NodeState nodestate;
    private AuthenticatedPerfectLink apLink;
    private static final int BASE_PORT = 5000;
    private List<List<String>> storedMessages;
    private final String signAlgo = "SHA256withRSA";
    private List<Map<String, Integer>> countedWrites;
    private List<Map<String, Integer>> countedAccepts;

    public BizantineConsensus(NodeState nodeState, AuthenticatedPerfectLink apLink) {
        this.nodestate = nodeState;
        this.apLink = apLink;
        storedMessages = new ArrayList<>();
        countedWrites = new ArrayList<>();
        countedAccepts = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            storedMessages.add(new ArrayList<>());
            countedWrites.add(new HashMap<>());
            countedAccepts.add(new HashMap<>());
        }
    }

    public void read() {
        String query = "READ|" + nodestate.myId + "|" + nodestate.seqNum + "|" + nodestate.consensusIndex + "|";
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

    public void state(String message){
        String consensusIndex = message.split("\\|", 6)[3];
        int senderId = Integer.parseInt(message.split("\\|", 6)[1]);
        String state = "STATE|" + nodestate.myId + "|" + nodestate.seqNum + "|" + consensusIndex + "|<" + 
        nodestate.valts + "," + nodestate.val + "," + nodestate.consensusPairs + ">";

        try {
            apLink.send(state, BASE_PORT + senderId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void analyseState(String message, int consensusIndex){
        storedMessages.get(consensusIndex).add(message);

        if (storedMessages.get(consensusIndex).size() == nodestate.quorumSize) {
            broadcastCollected(storedMessages.get(consensusIndex));
        }
    }

    private void broadcastCollected(List<String> messages){
        String consensusIndex = messages.get(0).split("\\|", 6)[3];
        StringBuilder contentBuilder = new StringBuilder("COLLECTED|" + nodestate.myId + "|" + nodestate.seqNum + "|" + consensusIndex + "|");
        String state = "STATE$" + nodestate.myId + "$" + nodestate.seqNum + "$" + nodestate.consensusIndex + "$<" +
        nodestate.valts + "," + nodestate.val + "," + nodestate.consensusPairs;
        for (int i = 0; i < messages.size(); i++) {
            contentBuilder.append("<").append(messages.get(i).replaceAll("\\|", "\\$")).append(">");
        }
        //Append own state
        contentBuilder.append("<").append(state).append(">");
        final String content = contentBuilder.toString();

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

    public void readCollected(String message, int consensusIndex) throws Exception {
        // Split the Message section using the '><' delimiter
        message.replaceAll("\\$", "\\|");
        String[] messagesArray = message.split("><");

        // List to store the individual messages
        List<String> messagesList = new ArrayList<>();

        // Process each message
        for (int i = 0; i < messagesArray.length; i++) {
            String msg = messagesArray[i];
            // Remove leading '<' from the first message
            if (i == 0) {
                msg = msg.substring(1);
            }
            // Remove trailing '>' from the last message
            if (i == messagesArray.length - 1) {
                msg = msg.substring(0, msg.length() - 1);
            }
            // Add the cleaned message to the list
            if (verifyAuth(message)) {
                messagesList.add(msg);
            }
        }

        List<String> states = new ArrayList<>();
        // Process the messages
        for (String msg : messagesList) {
            // Split the message using the '|' delimiter
            String[] msgArray = msg.split("\\|", 6);
            int senderId = Integer.parseInt(msgArray[1]);

            states.add(senderId, msgArray[3]);
        }
        decideWrite(states, consensusIndex);

    }

    private void decideWrite(List<String> states, int consensusIndex) {
        int highestTimestamp = 0;
        String highestValue = "";
        int quorumCount = 0;
        boolean decided = false;

        // Process the states
        for (String state : states) {
            // Split the state using the ',' delimiter
            String[] stateArray = state.split(",", 3);
            int timestamp = Integer.parseInt(stateArray[0]);
            String value = stateArray[1];

            if (timestamp > highestTimestamp) {  
                highestTimestamp = timestamp;
                highestValue = value;
            }
        }

        for (String state: states){
            String[] stateArray = state.split(",", 3);

            if(stateArray[2].contains(highestTimestamp + "," + highestValue)){
                quorumCount++; 
                if (quorumCount == nodestate.bizantineProcesses + 1){
                    decided = true;
                    writeValue(highestTimestamp, highestValue, consensusIndex);
                    break;
                }
            }
        }

        if (!decided){
            String leaderState = states.get(0);
            String[] leaderStateArray = leaderState.split(",", 3);
            writeValue(Integer.parseInt(leaderStateArray[0]), leaderStateArray[1], consensusIndex);
        }

    }

    private void writeValue(int timestamp, String value, int consensusIndex){
        String content = "WRITE|" + nodestate.myId + "|" + nodestate.seqNum + "|" + consensusIndex + "|" + timestamp + "," + value;

        for (int i = 0; i < nodestate.numNodes; i++) {
            if (i == nodestate.myId){
                continue;
            }
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

    public void countWrites(String message){
        String[] messageArray = message.split("\\|", 6);
        String[] value = messageArray[4].split(",", 3);
        int timestamp = Integer.parseInt(value[0]);
        String writeValue = value[1];
        int consensusIndex = Integer.parseInt(messageArray[3]);

        String key = timestamp + "," + writeValue;

        if (countedWrites.get(consensusIndex) == null) {
            countedWrites.set(consensusIndex, new HashMap<>());
        }
        countedWrites.get(consensusIndex).put(key, countedWrites.get(consensusIndex).getOrDefault(key, 0) + 1);

        if (countedWrites.get(consensusIndex).get(key) == nodestate.quorumSize) {
            sendAccept(writeValue, consensusIndex);
        }
    }

    public void sendAccept (String value, int consensusIndex) {
        String content = "ACCEPT|" + nodestate.myId + "|" + nodestate.seqNum + "|" + consensusIndex + "|" + value;

        for (int i = 0; i < nodestate.numNodes; i++) {
            if (i == nodestate.myId){
                continue;
            }
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

    public void countAccepts(String accept) {
        String[] acceptArray = accept.split("\\|", 6);
        String value = acceptArray[4];
        int consensusIndex = Integer.parseInt(acceptArray[3]);

        if (countedAccepts.get(consensusIndex) == null) {
            countedAccepts.set(consensusIndex, new HashMap<>());
        }
        countedAccepts.get(consensusIndex).put(value, countedAccepts.get(consensusIndex).getOrDefault(value, 0) + 1);

        if (countedAccepts.get(consensusIndex).get(value) == nodestate.quorumSize) {
            nodestate.val.set(consensusIndex, value);
            nodestate.consensusIndex++;
            System.out.println("Decided on value: " + value);          
        }

    }

    private boolean verifyAuth(String message) throws Exception{
        String sender = message.split("\\|", 5)[1];
        System.out.println("Sender: " + sender);
        String content = message.split("\\|", 5)[4];
        String signature = message.split("\\|", 5)[5];

        Signature signMaker = Signature.getInstance(signAlgo);
        PublicKey pubKey = readPublicKey("src/main/java/com/ist/DepChain/keys/" + sender + "_pub.key");
        signMaker.initVerify(pubKey);
        signMaker.update(content.getBytes());

        return signMaker.verify(Base64.getDecoder().decode(signature.getBytes()));
    }

    private PublicKey readPublicKey(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filename)); // Read the binary key file
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    }
}

