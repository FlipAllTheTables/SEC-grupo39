package com.ist.DepChain.besu;

import java.util.HashMap;


import com.google.gson.JsonObject;

public class Contract {
    String name;
    String address;
    String balance;
    String code;
    String owner;
    HashMap<String, String> methodIds;
    JsonObject storage;

    public Contract(String name, String address, String balance, String code, JsonObject storage, String owner, HashMap<String, String> methodIds) {
        this.name = name;
        this.address = address;
        this.balance = balance;
        this.code = code;
        this.storage = storage;
        this.owner = owner;
        this.methodIds = methodIds;
    }

    @Override
    public String toString() {
        return  "\nName: " + name + 
                "\nAddress: " + address +
                "\nBalance: " + balance +
                //"\nCode: " + code +
                "\nStorage: " + storage +
                "\nOwner: " + owner +
                "\nMethod IDs: " + methodIds +
                "\n---------------------";
    }
}
