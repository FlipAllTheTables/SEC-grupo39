// SPDX-License-Identifier: MIT
// Compatible with OpenZeppelin Contracts ^5.0.0
pragma solidity ^0.8.22;

import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";

contract Blacklist is Ownable{
    mapping(address => bool) public blacklistedAddresses;

    constructor(address owner) Ownable(owner) {}

    function addToBlackList(address addr) public returns (bool) {
        if (blacklistedAddresses[addr]) {
            return false; // Address is already blacklisted
        }
        blacklistedAddresses[addr] = true;
        return true;
    }

    function removeFromBlacklist(address addr) public returns (bool) {
        if (!blacklistedAddresses[addr]) {
            return false; // Address is not in blacklist
        }
        blacklistedAddresses[addr] = false;
        return true;
    }

    function isBlacklisted(address addr) view  public returns (bool) {
        return blacklistedAddresses[addr];
    }

}