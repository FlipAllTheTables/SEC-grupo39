package com.ist.DepChain.blocks;

import com.ist.DepChain.besu.Account;
import com.ist.DepChain.besu.Contract;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import java.security.MessageDigest;

public class Block {
    public String blockHash;
    public String prevBlockHash;
    public List<JsonObject> transactions;
    public Map<String, Account> accounts;
    public Map<String, Contract> contracts;
    private static final String SHA2_ALGORITHM = "SHA-256";

    //Constructor for genesis block
    public Block(Map<String, Account> accounts, Map<String, Contract> contracts) {
        this.prevBlockHash = "";
        this.accounts = accounts;
        this.contracts = contracts;
        this.transactions = null;
        try{
            this.blockHash = calculateHashGenesis();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    //Constructor for regular blocks
    public Block(List<JsonObject> transactions, String prevBlockHash) {
        this.transactions = transactions;
        this.prevBlockHash = prevBlockHash;
        try{
            this.blockHash = calculateHash();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Constructor for aborted blocks
    public Block(String aborted){
        this.prevBlockHash = "";
        this.transactions = new ArrayList<>();
        this.accounts = null;
        this.contracts = null;
        this.blockHash = aborted;
    }

    private String calculateHashGenesis() throws Exception {
        MessageDigest digest = MessageDigest.getInstance(SHA2_ALGORITHM);
        StringBuilder dataString = new StringBuilder();
        dataString.append(prevBlockHash);
        dataString.append(accounts);
        dataString.append(contracts);
        return digest.digest(dataString.toString().getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String calculateHash() throws Exception {
        MessageDigest digest = MessageDigest.getInstance(SHA2_ALGORITHM);
        StringBuilder dataString = new StringBuilder();
        dataString.append(prevBlockHash);
        dataString.append(transactions);
        return digest.digest(dataString.toString().getBytes(StandardCharsets.UTF_8)).toString();
    }

    @Override
    public String toString() {
        return "Block{" +
                "blockHash='" + blockHash + '\'' +
                ", prevBlockHash='" + prevBlockHash + '\'' +
                ", transactions=" + transactions +
                '}';
    }

}
