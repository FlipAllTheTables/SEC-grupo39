package com.ist.DepChain.nodes;

import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.ist.DepChain.links.AuthenticatedPerfectLink;

/**
     * Listener class that implements runnable to continuously wait for incoming messages
     */
    public class Listener implements Runnable {

        private AuthenticatedPerfectLink apLink;
        public NodeState nodestate;
        private static final int BASE_PORT = 5000;
        private BizantineConsensus bizantineConsensus;

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

                case "ISTTX":
                    System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String value = message.split("\\|",6)[3];

                    if (nodestate.myId == 0){ //If im the leader
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
                    else{
                        String init = "ISTTX|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + value;
                        new Thread(() -> {
                            try {
                                apLink.send(init, BASE_PORT);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }                    
                    break;

                case "DEPTX":
                    System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String value1 = message.split("\\|",6)[3];

                    if (nodestate.myId == 0){ //If im the leader
                        synchronized(nodestate.valuesToAppend){
                            if (!nodestate.valuesToAppend.contains(value1)){
                                System.out.println("Value: " + value1 + " added to the list of values to append");
                                nodestate.valuesToAppend.add(value1);
                                nodestate.currentBlockTransactions.add(value1);
                            }
                            else{
                                System.out.println("Value: " + value1 + " already in the list of values to append");
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
                    else{
                        String init = "DEPTX|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + value1;
                        new Thread(() -> {
                            try {
                                apLink.send(init, BASE_PORT);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
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
                    System.out.println("Received message from " + senderId + ": " + message);
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
            System.out.println("State: " + state.toString());
            return Base64.getEncoder().encodeToString(state.toString().getBytes());
        }

    }
