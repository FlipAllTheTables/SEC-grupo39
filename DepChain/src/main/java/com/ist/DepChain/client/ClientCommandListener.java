package com.ist.DepChain.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import com.ist.DepChain.links.AuthenticatedPerfectLink;
import com.ist.DepChain.nodes.NodeState;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ClientCommandListener implements Runnable {

    private NodeState nodestate;
    private AuthenticatedPerfectLink apLink;
    private static final int BASE_PORT = 5000;
    List<String> accounts;
    HashMap<String, String> methodIds;
    private final String signAlgo = "SHA256withRSA";
    private List<String> ownedAccounts;

    public ClientCommandListener (NodeState nodeState, AuthenticatedPerfectLink apLink, HashMap<String, String> methodIds, List<String> ownedAccounts) {
        this.ownedAccounts = ownedAccounts;
        this.methodIds = methodIds;
        this.nodestate = nodeState;
        this.apLink = apLink;
        accounts = new ArrayList<>();
        accounts.add("0000000000000000000000000000000000000001");
        accounts.add("0000000000000000000000000000000000000002");
        accounts.add("0000000000000000000000000000000000000003");
        accounts.add("0000000000000000000000000000000000000004");
    }

    @Override
    public void run() {
        System.out.println("Started listening for commands (type 'exit' to quit)");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            try {
                System.out.print("> "); // Command prompt
                String command = scanner.nextLine().trim();

                if (command.equalsIgnoreCase("exit")) {
                    System.out.println("Shutting down command listener...");
                    break;
                }

                commandHandler(command);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        scanner.close();
    }

    private synchronized void commandHandler(String command) throws Exception{
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0];
        String arg = (parts.length > 1) ? parts[1] : "";
        String sender, receiver, value, transaction;
        String[] args;
        Scanner scanner = new Scanner(System.in);
    
        int i = 0;

        switch (cmd.toUpperCase()) {
            case "APPEND":
                for (i = 1; i < nodestate.numNodes-nodestate.bizantineProcesses; i++) {
                    String innit = "INNIT|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + arg;
                    int sendPort = BASE_PORT + i;
                    new Thread(() -> {
                        try {
                            apLink.send(innit, sendPort);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                } 
                break;

            case "TX":
                String encodedTx;
                if(!arg.isEmpty()) {
                    System.out.println("Wrong format for ISTTX command. Use: ISTTX with no arguments");
                    break;
                }

                while (true) {
                    try {
                        System.out.print("Enter sender: ");
                        sender = scanner.nextLine().trim();
                        if (sender.isEmpty() || !ownedAccounts.contains(sender)) {
                            System.out.println("You do not own the account or the account is empty. Please try again.");
                            continue;
                        }

                        System.out.print("Enter receiver: ");
                        receiver = scanner.nextLine().trim();
                        if (receiver.isEmpty()) {
                            System.out.println("Receiver cannot be empty. Please try again.");
                            continue;
                        }
                        System.out.print("Enter value: ");
                        value = scanner.nextLine().trim();
                        if (value.isEmpty() || Integer.parseInt(value) <= 0) {
                            System.out.println("Value cannot be empty. Please try again.");
                            continue;
                        }
                        System.out.print("Enter transfer type: ");
                        value = scanner.nextLine().trim();
                        if (value.isEmpty() || Integer.parseInt(value) <= 0 || methodIds.get(value) == null) {
                            System.out.println("Value cannot be empty. Please try again.");
                            continue;
                        }
                        String callData = "";
                        String from;
                        String to;
                        int ammount;

                        switch(value) {
                            case "transfer":
                                System.out.print("Enter receiver: ");
                                to = scanner.nextLine().trim();
                                if (receiver.isEmpty()) {
                                    System.out.println("Receiver cannot be empty. Please try again.");
                                    continue;
                                }
                                System.out.print("Enter ammount: ");
                                ammount = Integer.parseInt(scanner.nextLine().trim());
                                if (ammount < 0) {
                                    System.out.println("Value cannot be empty nor negative. Please try again.");
                                    continue;
                                }
                                callData = methodIds.get(value) + padHexStringTo256Bit(to) + convertIntegerToHex256Bit(ammount);
                                break;
                            case "transferFrom":
                                System.out.print("Enter sender: ");
                                from = scanner.nextLine().trim();
                                if (from.isEmpty()) {
                                    System.out.println("Sender cannot be empty. Please try again.");
                                    continue;
                                }
                                System.out.print("Enter receiver: ");
                                to = scanner.nextLine().trim();
                                if (to.isEmpty()) {
                                    System.out.println("Receiver cannot be empty. Please try again.");
                                    continue;
                                }
                                System.out.print("Enter ammount: ");
                                ammount = Integer.parseInt(scanner.nextLine().trim());
                                if (ammount < 0) {
                                    System.out.println("Value cannot be empty nor negative. Please try again.");
                                    continue;
                                }
                                callData = methodIds.get(value) + padHexStringTo256Bit(from) + padHexStringTo256Bit(to) + convertIntegerToHex256Bit(ammount);
                                break;
                            case "approve":
                                System.out.print("Enter spender: ");
                                to = scanner.nextLine().trim();
                                if (to.isEmpty()) {
                                    System.out.println("Spender cannot be empty. Please try again.");
                                    continue;
                                }
                                System.out.print("Enter ammount: ");
                                ammount = Integer.parseInt(scanner.nextLine().trim());
                                if (ammount < 0) {
                                    System.out.println("Value cannot be empty nor negative. Please try again.");
                                    continue;
                                }
                                callData = methodIds.get(value) + padHexStringTo256Bit(to) + convertIntegerToHex256Bit(ammount);
                                break;
                            case "addToBlackList":
                                System.out.print("Enter account: ");
                                to = scanner.nextLine().trim();
                                if (to.isEmpty()) {
                                    System.out.println("Account cannot be empty. Please try again.");
                                    continue;
                                }
                                callData = methodIds.get(value) + padHexStringTo256Bit(to);
                                break;
                            case "removeFromBlackList":
                                System.out.print("Enter account: ");
                                to = scanner.nextLine().trim();
                                if (to.isEmpty()) {
                                    System.out.println("Account cannot be empty. Please try again.");
                                    continue;
                                }
                                callData = methodIds.get(value) + padHexStringTo256Bit(to);
                                break;
                            case "isBlackListed":
                                System.out.print("Enter account: ");
                                to = scanner.nextLine().trim();
                                if (to.isEmpty()) {
                                    System.out.println("Account cannot be empty. Please try again.");
                                    continue;
                                }
                                callData = methodIds.get(value) + padHexStringTo256Bit(to);
                                break;
                            default:
                                System.out.println("Unknown transaction type. Please try again.");
                                break;
                                
                        }
                        encodedTx = formatTx(sender, receiver, Integer.parseInt(value), callData);
                    }
                    catch (Exception e) {
                        System.out.println("Error parsing input. Please try again.");
                        continue;
                    }
                    break;
                }
                String nounce = Integer.toString(nodestate.myId) + Integer.toString(nodestate.seqNum);
                encodedTx += "%" + Base64.getEncoder().encodeToString(nounce.getBytes());
                for (i = 1; i < nodestate.numNodes-nodestate.bizantineProcesses + 1; i++) {
                    String isttx = "DEPTX|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + encodedTx;
                    int sendPort = BASE_PORT + i;
                    new Thread(() -> {
                        try {
                            apLink.send(isttx, sendPort);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                break;

            /*case "DEPTX":
                String encodedTx;
                if(!arg.isEmpty()) {
                    System.out.println("Wrong format for ISTTX command. Use: ISTTX with no arguments");
                    break;
                }

                while (true) {
                    try {
                        System.out.print("Enter sender: ");
                        sender = scanner.nextLine().trim();
                        if (sender.isEmpty() ) {
                            System.out.println("Sender cannot be empty. Please try again.");
                            continue;
                        }

                        System.out.print("Enter receiver: ");
                        receiver = scanner.nextLine().trim();
                        if (receiver.isEmpty()) {
                            System.out.println("Receiver cannot be empty. Please try again.");
                            continue;
                        }
                        System.out.print("Enter value: ");
                        value = scanner.nextLine().trim();
                        if (value.isEmpty() || Integer.parseInt(value) <= 0) {
                            System.out.println("Value cannot be empty. Please try again.");
                            continue;
                        }
                        encodedTx = formatDepTx(sender, receiver, Integer.parseInt(value));
                    }
                    catch (Exception e) {
                        System.out.println("Error parsing input. Please try again.");
                        continue;
                    }
                    break;
                }
                String nounce = Integer.toString(nodestate.myId) + Integer.toString(nodestate.seqNum);
                encodedTx += "%" + Base64.getEncoder().encodeToString(nounce.getBytes());
                for (i = 1; i < nodestate.numNodes-nodestate.bizantineProcesses + 1; i++) {
                    String isttx = "DEPTX|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + encodedTx;
                    int sendPort = BASE_PORT + i;
                    new Thread(() -> {
                        try {
                            apLink.send(isttx, sendPort);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                break;
            
            case "ISTTX":
                String encodedIstTx;
                if(!arg.isEmpty()) {
                    System.out.println("Wrong format for ISTTX command. Use: ISTTX with no arguments");
                    break;
                }

                while (true) {
                    try {
                        System.out.print("Enter transaction: ");
                        transaction = scanner.nextLine().trim();
                        if (transaction.isEmpty() || !transaction.equals("transfer") || !transaction.equals("transferFrom") || !transaction.equals("approve") || !transaction.equals("addToBlackList") || !transaction.equals("removeFromBlackList") || !transaction.equals("isBlackListed")) {
                            System.out.println("Not a valid transaction. Valid transactions are: transfer, transferFrom, approve, addToBlackList, removeFromBlackList, isBlackListed.");
                            continue;
                        }

                        System.out.print("Enter args: ");
                        String argString = scanner.nextLine().trim();
                        if (argString.isEmpty()) {
                            System.out.println("Please enter args.");
                            continue;
                        }
                        args = argString.split("\\s+");
                        encodedIstTx = formatIstTx(transaction, args);
                        String nounce1 = Integer.toString(nodestate.myId) + Integer.toString(nodestate.seqNum);
                        encodedIstTx += "%" + Base64.getEncoder().encodeToString(nounce1.getBytes());
                    }
                    catch (Exception e) {
                        System.out.println("Error parsing input. Please try again.");
                        continue;
                    }
                    break;
                }

                for (i = 0; i < nodestate.numNodes; i++) {
                    String isttx = "ISTTX|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + encodedIstTx;
                    int sendPort = BASE_PORT + i;
                    new Thread(() -> {
                        try {
                            apLink.send(isttx, sendPort);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                break;*/

            case "LOOP":
                Random random = new Random();
                for(int index=0; index<10;index++){
                    if(random.nextBoolean()){
                        String encodedDepTx;
                        int randomIndex = random.nextInt(accounts.size());
                        String randomAccount = accounts.get(randomIndex);
                        String randomAccount2 = accounts.get(random.nextInt(accounts.size()));
        
                        encodedDepTx = formatDepTx(randomAccount, randomAccount2, random.nextInt(100));

                        String nounce1 = Integer.toString(nodestate.myId) + Integer.toString(nodestate.seqNum);
                        encodedDepTx += "%" + Base64.getEncoder().encodeToString(nounce1.getBytes());

                        for (i = 0; i < nodestate.numNodes; i++) {
                            String isttx = "DEPTX|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + encodedDepTx;
                            int sendPort = BASE_PORT + i;
                            new Thread(() -> {
                                try {
                                    apLink.send(isttx, sendPort);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                    }
                    else{
                        String encodedIstTx1;
                        int randomIndex = random.nextInt(accounts.size());
                        String randomAccount = accounts.get(randomIndex);
                        String randomAccount2 = accounts.get(random.nextInt(accounts.size()));
        
                        encodedIstTx1 = formatIstTx("transfer", new String[]{randomAccount, randomAccount2, Integer.toString(random.nextInt(100))});
                        String nounce1 = Integer.toString(nodestate.myId) + Integer.toString(nodestate.seqNum);
                        encodedIstTx1 += "%" + Base64.getEncoder().encodeToString(nounce1.getBytes());

                        for (i = 0; i < nodestate.numNodes; i++) {
                            String isttx = "ISTTX|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + encodedIstTx1;
                            int sendPort = BASE_PORT + i;
                            new Thread(() -> {
                                try {
                                    apLink.send(isttx, sendPort);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                    }

                }

            
            case "READALL":
                for (i = 0; i < nodestate.numNodes; i++) {
                    String read = "READALL|" + nodestate.myId + "|" + nodestate.seqNum++ + "|";
                    int sendPort = BASE_PORT + i;
                    new Thread(() -> {
                        try {
                            apLink.send(read, sendPort);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                break;
            default:
                System.out.println("Unknown command: " + cmd);
                break;
        }
    }

    private String formatDepTx(String sender, String receiver, int value) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("sender", sender);
        jsonObject.addProperty("receiver", receiver);
        jsonObject.addProperty("value", value);

        return java.util.Base64.getEncoder().encodeToString(jsonObject.toString().getBytes());
    }

    private String formatIstTx (String transaction, String[] args) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("transaction", transaction);
        JsonArray jsonArray = new JsonArray();
        for (String arg : args) {
            jsonArray.add(arg);
        }
        jsonObject.add("args", jsonArray);

        return java.util.Base64.getEncoder().encodeToString(jsonObject.toString().getBytes());
    }
    
    private String authenticate(String m, String account) throws Exception {
        int accountId = (int)account.charAt(account.length() - 1);
        PrivateKey privKey = null;
        if (accountId == 1){
            privKey = (PrivateKey) readRSA("src/main/java/com/ist/DepChain/keys/Owner_priv.key", "priv");
        }
        else{
            privKey = (PrivateKey) readRSA("src/main/java/com/ist/DepChain/keys/Client_" + (accountId+1) + "_priv.key", "priv");
        }
        Signature signMaker = Signature.getInstance(signAlgo);
        signMaker.initSign(privKey);
        signMaker.update(m.getBytes());
        byte[] signature = signMaker.sign();
        return new String(Base64.getEncoder().encode(signature));
    }

    private String formatTx (String sender, String receiver, int value, String data) throws Exception{
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("sender", sender);
        jsonObject.addProperty("receiver", receiver);
        jsonObject.addProperty("value", value);
        jsonObject.addProperty("data", data);

        String sign = authenticate(jsonObject.toString(), sender);
        jsonObject.addProperty("signature", sign);

        return java.util.Base64.getEncoder().encodeToString(jsonObject.toString().getBytes());
    }

    public static Key readRSA(String keyPath, String type) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encoded;
        try (FileInputStream fis = new FileInputStream(keyPath)) {
            encoded = new byte[fis.available()];
            fis.read(encoded);
        }
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        if (type.equals("pub") ){
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            return keyFactory.generatePublic(keySpec);
        }

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return keyFactory.generatePrivate(keySpec);
    }

    public static String convertIntegerToHex256Bit(int number) {
        BigInteger bigInt = BigInteger.valueOf(number);

        return String.format("%064x", bigInt);
    }

    public static String padHexStringTo256Bit(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }

        int length = hexString.length();
        int targetLength = 64;

        if (length >= targetLength) {
            return hexString.substring(0, targetLength);
        }

        return "0".repeat(targetLength - length) +
                hexString;
    }
}
