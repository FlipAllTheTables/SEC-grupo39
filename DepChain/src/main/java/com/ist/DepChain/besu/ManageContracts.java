package com.ist.DepChain.besu;

import com.ist.DepChain.*;

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
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageContracts {
    private static Address istCoinAddress;
    private static Address blackListAddress;
    public static MutableAccount contractAccount;
    public static MutableAccount blackListAccount;
    public static EVMExecutor istCoinExecutor;
    public static EVMExecutor blackListExecutor;
    public HashMap<String, Contract> contracts;
    public HashMap<String, Account> accounts;
    ByteArrayOutputStream byteArrayOutputStreamIst;
    ByteArrayOutputStream byteArrayOutputStreamBL;

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

        executeAddToBlackList("0000000000000000000000000000000000000002");
        executeTransfer("0000000000000000000000000000000000000002", "0000000000000000000000000000000000000003", 0);
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

    public void parseGenesisBlock(String filePath) {
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
                    Account account = new Account(
                            address,
                            accountData.get("balance").getAsString()
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
            createContracts(contracts.get("ISTCoin"), accounts);

        } catch (IOException e) {
            System.err.println("Error reading genesis block file: " + e.getMessage());
        }
    }

    public void executeTransfer(String sender, String to, int amount){
        String paddedTo = padHexStringTo256Bit(to);
        String paddedSender = padHexStringTo256Bit(sender);
        String paddedAmount = convertIntegerToHex256Bit(amount);

        String transferCodeWithArgs = contracts.get("ISTCoin").methodIds.get("transfer") + paddedTo + paddedAmount;
        System.out.println("Transfer Code with Args: " + transferCodeWithArgs);
        istCoinExecutor.sender(Address.fromHexString(sender));
        istCoinExecutor.callData(Bytes.fromHexString(transferCodeWithArgs));
        istCoinExecutor.execute();

        String count = extractBooleanReturnData(byteArrayOutputStreamIst);
        System.out.println("Output of 'transfer():' " + count);
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
        System.out.println("Output of 'transferFrom():' " + count);
    }

    public void executeAddToBlackList(String address){
        String paddedAddress = padHexStringTo256Bit(address);

        String addToBlackListCodeWithArgs = contracts.get("ISTCoin").methodIds.get("addToBlackList") + paddedAddress;
        istCoinExecutor.sender(Address.fromHexString(address));
        istCoinExecutor.callData(Bytes.fromHexString(addToBlackListCodeWithArgs));
        istCoinExecutor.execute();

        String count = extractBooleanReturnData(byteArrayOutputStreamIst);
        System.out.println("AddtoBlackList():' " + count);
    }

    public void executeRemoveFromBlackList(String address){
        String paddedAddress = padHexStringTo256Bit(address);

        String removeFromBlackListCodeWithArgs = contracts.get("BlackList").methodIds.get("removeFromBlackList") + paddedAddress;
        blackListExecutor.sender(Address.fromHexString(address));
        blackListExecutor.callData(Bytes.fromHexString(removeFromBlackListCodeWithArgs));
        blackListExecutor.execute();

        String count = extractBooleanReturnData(byteArrayOutputStreamIst);
        System.out.println("RemoveFromBlackList():' " + count);
    }

    public void executeIsBlackListed(String address){
        String paddedAddress = padHexStringTo256Bit(address);

        String isBlackListedCodeWithArgs = contracts.get("BlackList").methodIds.get("isBlackListed") + paddedAddress;
        blackListExecutor.sender(Address.fromHexString(address));
        blackListExecutor.callData(Bytes.fromHexString(isBlackListedCodeWithArgs));
        blackListExecutor.execute();

        String count = extractBooleanReturnData(byteArrayOutputStreamIst);
        System.out.println("isBlackListed():' " + count);
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

