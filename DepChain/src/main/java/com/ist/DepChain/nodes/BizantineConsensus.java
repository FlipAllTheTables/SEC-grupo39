package com.ist.DepChain.nodes;

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
import java.util.Random;
import java.util.concurrent.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ist.DepChain.besu.ManageContracts;
import com.ist.DepChain.blocks.Block;
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
    private List<Integer> countedAborts;
    private List<Boolean> consenusReached;
    private List<Boolean> alreadyWritten;
    private Map<Integer, ScheduledFuture<?>> abortTimers = new HashMap<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    ManageContracts manageContracts;

    public BizantineConsensus(NodeState nodeState, AuthenticatedPerfectLink apLink, ManageContracts manageContracts) {
        this.nodestate = nodeState;
        this.apLink = apLink;
        storedMessages = new ArrayList<>();
        countedWrites = new ArrayList<>();
        countedAccepts = new ArrayList<>();
        consenusReached = new ArrayList<>();
        alreadyWritten = new ArrayList<>();
        countedAborts = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            storedMessages.add(new ArrayList<>());
            countedWrites.add(new HashMap<>());
            countedAccepts.add(new HashMap<>());
            countedAborts.add(0);
            consenusReached.add(false);
            alreadyWritten.add(false);
        }
        this.manageContracts = manageContracts;
    }

    public void read(int consensusIndex) {
        for (int i = 1; i < nodestate.numNodes; i++) {
            String query = "READ|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + consensusIndex + "|";
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
        String state;

        if (nodestate.isBizantine){
            state = bizantineState(consensusIndex);
        }
        else{
            state = "STATE|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + consensusIndex + "|<" + 
            nodestate.valts.get(Integer.parseInt(consensusIndex)) + "," + nodestate.val.get(Integer.parseInt(consensusIndex)) + "," + nodestate.consensusPairs + ">";

            try {
                apLink.send(state, BASE_PORT + senderId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void analyseState(String message, int consensusIndex){
        storedMessages.get(consensusIndex).add(message);

        if (storedMessages.get(consensusIndex).size() == nodestate.quorumSize) {
            broadcastCollected(storedMessages.get(consensusIndex));
        }
    }

    private void broadcastCollected(List<String> messages){
        for (int i = 1; i < nodestate.numNodes; i++) {
            String consensusIndex = messages.get(0).split("\\|", 6)[3];
            StringBuilder contentBuilder = new StringBuilder("COLLECTED|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + consensusIndex + "|");
            String state = "STATE$" + nodestate.myId + "$" + nodestate.seqNum + "$" + nodestate.consensusIndex + "$<" +
            nodestate.valts.get(Integer.parseInt(consensusIndex)) + "," + nodestate.val.get(Integer.parseInt(consensusIndex)) + "," + nodestate.consensusPairs + ">";
            try{
                state += "$" + authenticate(state);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            for (int j = 0; j < messages.size(); j++) {
                contentBuilder.append("<").append(messages.get(j).replaceAll("\\|", "\\$")).append(">");
            }
            //Append own state
            contentBuilder.append("<").append(state).append(">");
            final String content = contentBuilder.toString();

            final int port = BASE_PORT + i;
            new Thread(() -> {
                try {
                    apLink.send(content, port);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            if (i == nodestate.numNodes - 1) {
                try{
                    readCollected(content, Integer.parseInt(consensusIndex));
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void readCollected(String message, int consensusIndex) throws Exception {
        // Split the Message section using the '><' delimiter
        String content = message.split("\\|", 6)[4];
        content.replaceAll("\\$", "\\|");
        String[] messagesArray = content.split("><");

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
            if (verifyAuth(msg)) {
                messagesList.add(msg);
            }
        }

        Map<Integer, String> states = new HashMap<>();
        // Process the messages
        for (String msg : messagesList) {
            // Split the message using the '|' delimiter
            String[] msgArray = msg.split("\\$", 6);
            int senderId = Integer.parseInt(msgArray[1]);

            states.put(senderId, msgArray[4]);
        }
        decideWrite(states, consensusIndex);

    }

    private void decideWrite(Map<Integer, String> states, int consensusIndex) {
        int highestTimestamp = 0;
        String highestValue = "";
        int quorumCount = 0;
        boolean decided = false;

        // Process the states
        for (Map.Entry<Integer, String> entry : states.entrySet()) {
            String state = entry.getValue();
            String cleanState = state.replace("<", "").replace(">", "");

            // Split the state using the ',' delimiter
            String[] stateArray = cleanState.split(",", 3);
            int timestamp = Integer.parseInt(stateArray[0]);
            String value = stateArray[1];

            if (timestamp > highestTimestamp) {  
                highestTimestamp = timestamp;
                highestValue = value;
            }
        }

        for (Map.Entry<Integer, String> entry : states.entrySet()) {
            String state = entry.getValue();
            String cleanState = state.replace("<", "").replace(">", "");
            String[] stateArray = cleanState.split(",", 3);

            if (stateArray[2].contains(highestTimestamp + "," + highestValue)) {
                quorumCount++; 
                if (quorumCount == nodestate.bizantineProcesses + 1) {
                    decided = true;
                    writeValue(highestTimestamp, highestValue, consensusIndex);
                    break;
                }
            }
        }

        if (!decided) {
            String leaderState = states.get(0);
            String cleanLeaderState = leaderState.replace("<", "").replace(">", "");
            String[] leaderStateArray = cleanLeaderState.split(",", 3);
            writeValue(Integer.parseInt(leaderStateArray[0]), leaderStateArray[1], consensusIndex);
        }
    }

    private void writeValue(int timestamp, String value, int consensusIndex){
        for (int i = 0; i < nodestate.numNodes; i++) {
            String content;
            if (i == nodestate.myId){
                continue;
            }
            if (nodestate.isBizantine) {
                content = bizantineWrite(consensusIndex);
            }
            else {
                content = "WRITE|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + consensusIndex + "|" + timestamp + "," + value;
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

        if (countedWrites.get(consensusIndex).get(key) == nodestate.quorumSize && !alreadyWritten.get(consensusIndex)) {
            alreadyWritten.set(consensusIndex, true);
            sendAccept(writeValue, consensusIndex);
        }
    }

    public void sendAccept (String value, int consensusIndex) {
        for (int i = 0; i < nodestate.numNodes; i++) {
            String content;
            if (i == nodestate.myId){
                continue;
            }
            if (nodestate.isBizantine) {
                content = bizantineAccept(consensusIndex);
            }
            else {
                content = "ACCEPT|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + consensusIndex + "|" + value;
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

        // Increment the count for the accepted value
        countedAccepts.get(consensusIndex).put(value, countedAccepts.get(consensusIndex).getOrDefault(value, 0) + 1);

        // Check if consensus is reached
        if (countedAccepts.get(consensusIndex).get(value) == nodestate.quorumSize && !consenusReached.get(consensusIndex)) {
            consenusReached.set(consensusIndex, true);
            nodestate.val.set(consensusIndex, value);
            nodestate.valuesToAppend.remove(value);
            System.out.println("Decided on value: " + value);
            addToBlockChain(value);

            // Cancel the timer if consensus is reached
            if (abortTimers.containsKey(consensusIndex)) {
                abortTimers.get(consensusIndex).cancel(false);
                abortTimers.remove(consensusIndex);
            }
        } else {
            // Start a timer if not already started
            if (!abortTimers.containsKey(consensusIndex)) {
                ScheduledFuture<?> abortTask = scheduler.schedule(() -> {
                    if (!consenusReached.get(consensusIndex)) {
                        sendAbort(consensusIndex);
                    }
                }, 5, TimeUnit.SECONDS); // Set the timeout duration (e.g., 10 seconds)

                abortTimers.put(consensusIndex, abortTask);
            }
        }
    }

    private void sendAbort(int consensusIndex) {
        String abortMessage = "ABORT|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + consensusIndex;
        for (int i = 0; i < nodestate.numNodes; i++) {
            if (i == nodestate.myId) {
                continue;
            }
            final int port = BASE_PORT + i;
            new Thread(() -> {
                try {
                    apLink.send(abortMessage, port);
                    System.out.println("Sent abort message for consensusIndex: " + consensusIndex);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public void countAborts(String abort) {
        String[] abortArray = abort.split("\\|", 6);
        int consensusIndex = Integer.parseInt(abortArray[3]);

        countedAborts.set(consensusIndex, countedAborts.get(consensusIndex) + 1);
        if (countedAborts.get(consensusIndex) == nodestate.quorumSize && !consenusReached.get(consensusIndex)) {
            consenusReached.set(consensusIndex, true);
            System.out.println("Consensus aborted for index: " + consensusIndex);
            nodestate.val.set(consensusIndex, "ABORTED");
            nodestate.valuesToAppend.remove("ABORTED");
            addToBlockChain("ABORTED");
        }
    }

    private void addToBlockChain(String transactions){
        if(transactions.equals("ABORTED")){
            nodestate.blockChain.add(new Block(transactions));
            return;
        }

        String decodedTransaction = new String(Base64.getDecoder().decode(transactions));
        String[] transactionArray = decodedTransaction.split("&");
        List<JsonObject> jsonArray = new ArrayList<>();

        for (String transaction : transactionArray) {
            //Remove the nounce
            transaction = transaction.split("%")[0];
            String decodedText = new String(Base64.getDecoder().decode(transaction));
            System.out.println("Transaction " + decodedText);
            JsonObject jsonObject = JsonParser.parseString(decodedText).getAsJsonObject();
            jsonArray.add(jsonObject);           
        }

        nodestate.blockChain.add(new Block(jsonArray, nodestate.blockChain.get(nodestate.blockChain.size() - 1).blockHash));
        manageContracts.processTransactions(jsonArray);  
    }

    private boolean verifyAuth(String message) throws Exception{
        String[] splitMsg = message.split("\\$", 6);
        String sender = message.split("\\$", 6)[1];
        String signature = message.split("\\$", 6)[5];
        StringBuilder checkSig = new StringBuilder(splitMsg[0] + "$" + splitMsg[1] + "$" + splitMsg[2] + "$" + splitMsg[3] + "$" + splitMsg[4]);

        Signature signMaker = Signature.getInstance(signAlgo);
        PublicKey pubKey = readPublicKey("src/main/java/com/ist/DepChain/keys/" + sender + "_pub.key");
        signMaker.initVerify(pubKey);
        signMaker.update(checkSig.toString().getBytes());

        return signMaker.verify(Base64.getDecoder().decode(signature.getBytes()));
    }

    private String authenticate(String m) throws Exception {
        Signature signMaker = Signature.getInstance(signAlgo);
        signMaker.initSign(nodestate.privateKey);
        signMaker.update(m.getBytes());
        byte[] signature = signMaker.sign();
        return new String(Base64.getEncoder().encode(signature));
    }

    private PublicKey readPublicKey(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filename)); // Read the binary key file
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    private String bizantineState (String consensusIndex) {
        Random random = new Random();
        int length = 10; // Length of the random string
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        StringBuilder randomString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            randomString.append(characters.charAt(index));
        }

        Pair pair = new Pair(random.nextInt(100), randomString.toString());

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            randomString.append(characters.charAt(index));
        }

        String state = "STATE|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + consensusIndex + "|<" + 
        random.nextInt(100) + "," + randomString.toString() + "," + pair  + ">";

        return state;
    }

    private String bizantineWrite (Integer consensusIndex) {
        Random random = new Random();
        int timestamp = random.nextInt(100);
        int length = 10; // Length of the random string
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        StringBuilder randomString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            randomString.append(characters.charAt(index));
        }

        String content = "WRITE|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + consensusIndex + "|" + timestamp + "," + randomString.toString();
        return content;
    }

    private String bizantineAccept (Integer consensusIndex) {
        Random random = new Random();
        int length = 10; // Length of the random string
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        StringBuilder randomString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            randomString.append(characters.charAt(index));
        }

        String content = "ACCEPT|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + consensusIndex + "|" + randomString.toString();
        return content;
    }
}

