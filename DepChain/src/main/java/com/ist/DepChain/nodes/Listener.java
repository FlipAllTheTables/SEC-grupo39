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
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import com.ist.DepChain.links.AuthenticatedPerfectLink;

/**
     * Listener class that implements runnable to continuously wait for incoming messages
     */
    public class Listener implements Runnable {

        private AuthenticatedPerfectLink apLink;
        public NodeState nodestate;
        private static final int BASE_PORT = 5000;
        private BizantineConsensus bizantineConsensus;
        private final String signAlgo = "SHA256withRSA";

        public Listener(AuthenticatedPerfectLink apLink, NodeState nodeState, BizantineConsensus bizantineConsensus) {
            this.apLink = apLink;
            this.nodestate = nodeState;
            this.bizantineConsensus = bizantineConsensus;
        }

        public void run() {
            System.out.println("Started listening for messages");
            while (true) {
                try {
                    DatagramPacket dp = apLink.deliver();
                    if (dp == null){
                        System.out.println("Signature didnt match content");
                    }
                    else{
                        new Thread(() -> {
                            try {
                                messageHandler(dp);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void messageHandler(DatagramPacket dp) throws Exception {
            String message = new String(dp.getData(), 0, dp.getLength());
            
            String command = message.split("\\|",6)[0];
            String senderId = message.split("\\|",6)[1];
            String seqNum = message.split("\\|",6)[2];

            switch(command) {
                case "ACK": 
                    //System.out.println("Received ack from " + senderId +  " with sequence number: " + seqNum);

                    // Verify if acknowledge message received comes from a node to which there is an acknowledge waiting
                    if (nodestate.acks.containsKey(Integer.valueOf(senderId)) && nodestate.acks.get(Integer.valueOf(senderId)).contains(Integer.valueOf(seqNum)))
                        nodestate.acks.get(Integer.valueOf(senderId)).remove(Integer.valueOf(seqNum));
                    break;

                case "TEST":
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case "TX":
                    System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String value = message.split("\\|",6)[3];

                    if (nodestate.myId == 0){ //If im the leader
                        if(!(Integer.parseInt(senderId) >= nodestate.numNodes)) {
                            String nodeMessage = message.split("\\|",6)[3];
                            if(!verifyAuth(nodeMessage)){
                                System.out.println("Client message forged. Ignoring message");
                                return;
                            }
                        }
                        synchronized(nodestate.valuesToAppend){
                            if (!nodestate.valuesToAppend.contains(value)){
                                System.out.println("Value: " + value + " added to the list of values to append");
                                nodestate.valuesToAppend.add(value);
                                nodestate.currentBlockTransactions.add(value);
                            }
                            else{
                                System.out.println("Value: " + value + " already in the list of values to append");
                                return;
                            }
                            if(nodestate.currentBlockTransactions.size() == 10){
                                ArrayList<String> transactions = new ArrayList<>();
                                //Deep copy of the list
                                for (String transaction : nodestate.currentBlockTransactions) {
                                    transactions.add(new String(transaction));
                                }
                                nodestate.currentBlockTransactions.clear();
                                int consensusIndex = ++nodestate.consensusIndex;
                                String state = prepareState(transactions);
                                System.out.println("State: " + state);
                                nodestate.val.add(consensusIndex, state);
                                nodestate.valts.add(consensusIndex, 0);
                                bizantineConsensus.read(consensusIndex);
                            }
                        }
                    }                   
                    break;

                case "READ":
                    System.out.println("Received message from " + senderId + ": " + message);
                    if (Integer.valueOf(senderId) != 0) {
                        System.out.println("READ command comes from non-leader node, discarded");
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                        return;
                    }
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    bizantineConsensus.state(message);
                    break;

                case "STATE":
                    System.out.println("Received message from " + senderId + ": " + message);
                    int consensusRun = Integer.valueOf(message.split("\\|",6)[3]);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    bizantineConsensus.analyseState(message, consensusRun);
                    break;

                case "COLLECTED":
                    System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    int consensusIndex = Integer.valueOf(message.split("\\|",6)[3]);
                    bizantineConsensus.readCollected(message, consensusIndex); 
                    break;

                case "WRITE":
                    System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    bizantineConsensus.countWrites(message);
                    break;

                case "ACCEPT":
                    System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    bizantineConsensus.countAccepts(message);
                    break;

                case "ABORT":
                    System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    bizantineConsensus.countAborts(message);
                    break;
                
                case "READALL":
                    //System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String response = "INFO|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + nodestate.blockChain.toString();
                    new Thread(() -> {
                        try {
                            apLink.send(response, BASE_PORT+Integer.parseInt(senderId));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                    break;

                case "INFO":
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String content = message.split("\\|",6)[3];
                    System.out.println("Received message from " + senderId + ": " + content);
                    break;

                case "ESTABLISH":
                    String key = message.split("\\|",6)[3];
                    System.out.println("Received message from " + senderId + ": " + message);
                    nodestate.sharedKeys.put(Integer.parseInt(senderId), new SecretKeySpec(key.getBytes(), "AES"));
                    try {
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                        nodestate.acks.put(Integer.valueOf(senderId), new ArrayList<>());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                default:
                    System.out.println("No command found");
                    break;
            }
        }

        //Turns the list of transactions into a string separated by "&" and encodes it
        private String prepareState(List<String> encodedTransactions) {
            StringBuilder state = new StringBuilder();
            for (String transaction : encodedTransactions) {
                state.append(transaction).append("&");
            }
            return Base64.getEncoder().encodeToString(state.toString().getBytes());
        }

        private boolean verifyAuth(String message) throws Exception{
            String[] splitMsg = message.split("\\$", 6);
            String sender = message.split("\\$", 6)[1];
            String signature = message.split("\\$", 6)[4];
            System.out.println("Signature: " + signature);
            StringBuilder checkSig = new StringBuilder(splitMsg[0] + "|" + splitMsg[1] + "|" + splitMsg[2] + "|" + splitMsg[3]);
            System.out.println("CheckSig: " + checkSig);
    
            Signature signMaker = Signature.getInstance(signAlgo);
            PublicKey pubKey = readPublicKey("src/main/java/com/ist/DepChain/keys/" + sender + "_pub.key");
            signMaker.initVerify(pubKey);
            signMaker.update(checkSig.toString().getBytes());
    
            return signMaker.verify(Base64.getDecoder().decode(signature.getBytes()));
        }
        private PublicKey readPublicKey(String filename) throws Exception {
            byte[] keyBytes = Files.readAllBytes(Paths.get(filename)); // Read the binary key file
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        }

    }
