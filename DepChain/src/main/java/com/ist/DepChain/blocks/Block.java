package com.ist.DepChain.blocks;

import com.ist.DepChain.besu.Account;
import com.ist.DepChain.besu.Contract;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Block {
    public String blockHash;
    public String prevBlockHash;
    public JsonArray transactions;
    public Map<String, Account> accounts;
    public Map<String, Contract> contracts;

    //Constructor for genesis block
    public Block(String blockHash, String prevBlockHash, JsonArray transactions, Map<String, Account> accounts, Map<String, Contract> contracts) {
        this.blockHash = blockHash;
        this.prevBlockHash = prevBlockHash;
        this.transactions = transactions;
        this.accounts = accounts;
        this.contracts = contracts;
    }

    //Constructor for regular blocks
    public Block(String blockHash, String prevBlockHash, JsonArray transactions) {
        this.blockHash = blockHash;
        this.prevBlockHash = prevBlockHash;
        this.transactions = transactions;
    }

}
