package com.ist.DepChain.besu;

import com.ist.DepChain.blocks.Block;
import com.ist.DepChain.nodes.NodeState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.tuweni.bytes.Bytes;

import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.*;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageContracts {
    private static Address istCoinAddress;
    public static MutableAccount contractAccount;
    public static MutableAccount blackListAccount;
    public static EVMExecutor istCoinExecutor;
    public HashMap<String, Contract> contracts;
    public HashMap<String, Account> accounts;
    ByteArrayOutputStream byteArrayOutputStreamIst;
    ByteArrayOutputStream byteArrayOutputStreamBL;
    private final String signAlgo = "SHA256withRSA";

    public ManageContracts() {
        contracts = new HashMap<>();
        accounts = new HashMap<>();
    }

    public void createContracts (Contract istCoin, HashMap<String, Account> accounts) {
        istCoinAddress = Address.fromHexString(istCoin.address);

        SimpleWorld simpleWorld = new SimpleWorld();

        simpleWorld.createAccount(istCoinAddress,0, Wei.fromEth(0));
        contractAccount = (MutableAccount) simpleWorld.get(istCoinAddress);
        System.out.println("istCoin Contract Account");
        System.out.println("  Address: "+contractAccount.getAddress());
        System.out.println("  Balance: "+contractAccount.getBalance());
        System.out.println("  Nonce: "+contractAccount.getNonce());
        System.out.println("  Storage:");
        System.out.println("    Slot 0: "+simpleWorld.get(istCoinAddress).getStorageValue(UInt256.valueOf(0)));

        byteArrayOutputStreamIst = new ByteArrayOutputStream();
        PrintStream printStreamIst = new PrintStream(byteArrayOutputStreamIst);
        StandardJsonTracer tracerIst = new StandardJsonTracer(printStreamIst, true, true, true, true);

        istCoinExecutor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        istCoinExecutor.tracer(tracerIst);
        istCoinExecutor.code(Bytes.fromHexString(istCoin.code));
        istCoinExecutor.receiver(istCoinAddress);
        istCoinExecutor.worldUpdater(simpleWorld.updater());
        istCoinExecutor.commitWorldState();
        istCoinExecutor.execute();

        String runtimeByteCode = extractFromReturnData(byteArrayOutputStreamIst);
        istCoinExecutor.code(Bytes.fromHexString(runtimeByteCode));
        istCoinExecutor.commitWorldState();

        initializeAccounts(accounts, simpleWorld);
    }

    public void initializeAccounts(HashMap<String, Account> accounts, SimpleWorld simpleWorld) {
        for (Map.Entry<String, Account> entry : accounts.entrySet()) {
            String address = entry.getKey();
            String balance = entry.getValue().balance;

            Address accountAddress = Address.fromHexString(address);
            Wei weiBalance = Wei.fromEth(Long.parseLong(balance));

            simpleWorld.createAccount(accountAddress, 0, weiBalance);
            entry.getValue().account = (MutableAccount) simpleWorld.get(accountAddress);
        }

    }

    public HashMap<String,String> parseGenesisBlock(String filePath, NodeState nodeState) {
        try (FileReader reader = new FileReader(filePath)) {
            // Parse JSON file
            JsonObject genesisBlock = JsonParser.parseReader(reader).getAsJsonObject();

            // Extract block hash
            String blockHash = genesisBlock.get("block_hash").getAsString();
            System.out.println("Block Hash: " + blockHash);

            // Extract previous block hash
            String prevBlockHash = genesisBlock.get("previous_block_hash").isJsonNull() ? "None" : genesisBlock.get("previous_block_hash").getAsString();
            System.out.println("Previous Block Hash: " + prevBlockHash);

            // Extract transactions
            JsonArray transactions = genesisBlock.getAsJsonArray("transactions");
            System.out.println("Number of Transactions: " + transactions.size());

            // Extract state (accounts and contracts)
            JsonObject state = genesisBlock.getAsJsonObject("state");

            for (Map.Entry<String, JsonElement> entry : state.entrySet()) {
                JsonObject accountData = entry.getValue().getAsJsonObject();
                String address = accountData.get("address").getAsString();

                // Check if it's an EOA or contract
                boolean isContract = accountData.has("code");

                if(isContract){
                    HashMap<String, String> methodIds = new HashMap<>();
                    System.out.println("IstCoin Contract");
                    methodIds.put("transfer", accountData.get("transfer").getAsString());
                    methodIds.put("transferFrom", accountData.get("transferFrom").getAsString());
                    methodIds.put("addToBlackList", accountData.get("addToBlackList").getAsString());
                    methodIds.put("removeFromBlackList", accountData.get("removeFromBlackList").getAsString());
                    methodIds.put("isBlacklisted", accountData.get("isBlackListed").getAsString());
                    methodIds.put("approve", accountData.get("approve").getAsString());
                    Contract contract = new Contract(
                            entry.getKey(),
                            address,
                            accountData.get("balance").getAsString(),
                            accountData.get("code").getAsString(),
                            accountData.get("storage").getAsJsonObject(),
                            accountData.get("owner").getAsString(),
                            methodIds
                    );
                    contracts.put(entry.getKey(), contract);
                    System.out.println(contract);
                }
                else{
                    String pubKey = accountData.get("Public Key").getAsString();
                    PublicKey publicKey = null;
                    try{
                        publicKey = decodePublicKey(pubKey);
                    }
                    catch (Exception e){
                        System.out.println("Error decoding public key: " + e.getMessage());
                    }
                    Account account = new Account(
                            address,
                            accountData.get("balance").getAsString(),
                            publicKey
                    );
                    System.out.println(account);
                    accounts.put(address, account);
                }
            }

            // Print account details
            for (Account acc : accounts.values()) {
                System.out.println(acc);
            }
            for(Contract contract : contracts.values()) {
                System.out.println(contract);
            }
            nodeState.blockChain.add(new Block(accounts, contracts));
            createContracts(contracts.get("ISTCoin"), accounts);

        } catch (IOException e) {
            System.err.println("Error reading genesis block file: " + e.getMessage());
        }
        return contracts.get("ISTCoin").methodIds;
    }

    public PublicKey decodePublicKey(String encodedKey) throws Exception {
        // Decode the Base64 string into a byte array
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);

        // Reconstruct the public key using a KeyFactory
        KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Replace "RSA" with your algorithm
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
        return keyFactory.generatePublic(keySpec);
    }

    public void processTransactions(List<JsonObject> transactions){
        for (JsonObject transaction: transactions){
            String sender = transaction.get("sender").getAsString();
            String receiver = transaction.get("receiver").getAsString();
            int value = transaction.get("value").getAsInt();
            String data = transaction.get("data").getAsString();
            String sign = transaction.get("signature").getAsString();

            JsonObject unsignedMessage = new JsonObject();
            unsignedMessage.addProperty("sender", sender);
            unsignedMessage.addProperty("receiver", receiver);
            unsignedMessage.addProperty("value", value);
            unsignedMessage.addProperty("data", data);

            try{
                if(verifySign(unsignedMessage, sender, sign)){
                    System.out.println("Signature verified successfully.");
                }
                else{
                    System.out.println("Signature verification failed. Transaction Forged.");
                    continue;                
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            if (value > 0){
                depCoinExchange(accounts.get(sender), accounts.get(receiver), value);
            }
            if(data != null && !data.isEmpty()){
                executeTx(data, sender);
            }
        }   
    }

    public void executeTx(String callData, String sender){
        istCoinExecutor.sender(Address.fromHexString(sender));
        istCoinExecutor.callData(Bytes.fromHexString(callData));
        istCoinExecutor.execute();
        String count = extractBooleanReturnData(byteArrayOutputStreamIst);
        System.out.println("Output of 'executeTx():' " + count + "\n");
    }

    private boolean verifySign(JsonObject unsignedMessage, String account, String sign) throws Exception {
        char lastChar = account.charAt(account.length() - 1);

        // Convert the character to an integer
        int accountId = Character.getNumericValue(lastChar);
        PublicKey pubKey = null;
        System.out.println("Account ID: " + accountId);
        if (accountId == 1){
            System.out.println("Reading key in file: " + "src/main/java/com/ist/DepChain/keys/Owner_pub.key");
            pubKey = (PublicKey) readRSA("src/main/java/com/ist/DepChain/keys/Owner_pub.key", "pub");
        }
        else{
            System.out.println("Reading key in file: " + "src/main/java/com/ist/DepChain/keys/Client_" + (accountId+1) + "_pub.key");
            pubKey = (PublicKey) readRSA("src/main/java/com/ist/DepChain/keys/Client_" + (accountId+1) + "_pub.key", "pub");
        }
        byte[] decodedSign = Base64.getDecoder().decode(sign);
        System.out.println("Decoded Sign: " + decodedSign);
        Signature signMaker = Signature.getInstance(signAlgo);
        signMaker.initVerify(pubKey);
        signMaker.update(unsignedMessage.toString().getBytes());
        return signMaker.verify(decodedSign);
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

    public void depCoinExchange(Account sender, Account receiver, int amount){
        if (amount < 0) {
            System.out.println("Invalid amount. Must be greater than or equal 0.");
            return;
        }
        if(sender.account.getBalance().compareTo(Wei.fromEth(amount)) < 0) {
            System.out.println("Insufficient balance for transaction.");
            return;
        }
        sender.account.decrementBalance(Wei.fromEth(amount));
        receiver.account.incrementBalance(Wei.fromEth(amount));
        System.out.println("Transaction successful. New balances:");
        BigInteger senderBalanceInWei = sender.account.getBalance().toBigInteger(); // Get the balance in Wei
        int balanceInEther = senderBalanceInWei.divide(BigInteger.TEN.pow(18)).intValue(); // Convert to Ether as an integer
        System.out.println("Sender Balance in DepCoin: " + balanceInEther);
        BigInteger reciverBalanceInWei = receiver.account.getBalance().toBigInteger(); // Get the balance in Wei
        int recieverBalanceInEther = reciverBalanceInWei.divide(BigInteger.TEN.pow(18)).intValue(); // Convert to Ether as an integer
        System.out.println("Sender Balance in DepCoin: " + recieverBalanceInEther + "\n");
    }

    public void executeTransfer(String sender, String to, int amount){
        String paddedTo = padHexStringTo256Bit(to);
        String paddedAmount = convertIntegerToHex256Bit(amount);

        String transferCodeWithArgs = contracts.get("ISTCoin").methodIds.get("transfer") + paddedTo + paddedAmount;
        istCoinExecutor.sender(Address.fromHexString(sender));
        istCoinExecutor.callData(Bytes.fromHexString(transferCodeWithArgs));
        istCoinExecutor.execute();

        String count = extractBooleanReturnData(byteArrayOutputStreamIst);
        System.out.println("Output of 'transfer():' " + count + "\n");
    }

    public void executeTransferFrom(String to, String From, int amount){
        String paddedTo = padHexStringTo256Bit(to);
        String paddedFrom = padHexStringTo256Bit(From);
        String paddedAmount = convertIntegerToHex256Bit(amount);

        String transferFromCodeWithArgs = contracts.get("ISTCoin").methodIds.get("transferFrom") + paddedFrom + paddedTo + paddedAmount;
        System.out.println("TransferFrom Code with Args: " + transferFromCodeWithArgs);
        istCoinExecutor.sender(Address.fromHexString(to));
        istCoinExecutor.callData(Bytes.fromHexString(transferFromCodeWithArgs));
        istCoinExecutor.execute();

        String count = extractBooleanReturnData(byteArrayOutputStreamIst);
        System.out.println("Output of 'transferFrom():' " + count + "\n");
    }

    public void executeAddToBlackList(String address){
        String paddedAddress = padHexStringTo256Bit(address);

        String addToBlackListCodeWithArgs = contracts.get("ISTCoin").methodIds.get("addToBlackList") + paddedAddress;
        istCoinExecutor.sender(Address.fromHexString(address));
        istCoinExecutor.callData(Bytes.fromHexString(addToBlackListCodeWithArgs));
        istCoinExecutor.execute();

        String count = extractBooleanReturnData(byteArrayOutputStreamIst);
        System.out.println("AddtoBlackList():' " + count + "\n");
    }

    public void executeRemoveFromBlackList(String address){
        String paddedAddress = padHexStringTo256Bit(address);

        String removeFromBlackListCodeWithArgs = contracts.get("BlackList").methodIds.get("removeFromBlackList") + paddedAddress;
        istCoinExecutor.sender(Address.fromHexString(address));
        istCoinExecutor.callData(Bytes.fromHexString(removeFromBlackListCodeWithArgs));
        istCoinExecutor.execute();

        String count = extractBooleanReturnData(byteArrayOutputStreamIst);
        System.out.println("RemoveFromBlackList():' " + count + "\n");
    }

    public void executeIsBlackListed(String address){
        String paddedAddress = padHexStringTo256Bit(address);

        String isBlackListedCodeWithArgs = contracts.get("BlackList").methodIds.get("isBlackListed") + paddedAddress;
        istCoinExecutor.sender(Address.fromHexString(address));
        istCoinExecutor.callData(Bytes.fromHexString(isBlackListedCodeWithArgs));
        istCoinExecutor.execute();

        String count = extractBooleanReturnData(byteArrayOutputStreamIst);
        System.out.println("isBlackListed():' " + count + "\n");
    }

    public void executeApprove(String sender, String spender, int amount){
        String paddedSpender = padHexStringTo256Bit(spender);
        String paddedAmount = convertIntegerToHex256Bit(amount);

        String approveCodeWithArgs = contracts.get("ISTCoin").methodIds.get("approve") + paddedSpender + paddedAmount;
        System.out.println("Approve Code with Args: " + approveCodeWithArgs);
        istCoinExecutor.sender(Address.fromHexString(sender));
        istCoinExecutor.callData(Bytes.fromHexString(approveCodeWithArgs));
        istCoinExecutor.execute();

        String count = extractBooleanReturnData(byteArrayOutputStreamIst);
        System.out.println("Output of 'approve():' " + count + "\n");

    }

    public static String extractFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        System.out.println(stack.get(stack.size() - 2).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        return memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
    }

    public static String extractBooleanReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        if(returnData.endsWith("0")) {
            return "false";
        }
        else{
            return "true";
        }
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

