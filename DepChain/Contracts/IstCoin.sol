// SPDX-License-Identifier: MIT
// Compatible with OpenZeppelin Contracts ^5.0.0
pragma solidity ^0.8.22;

import {ERC20} from "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import {ERC20Permit} from "@openzeppelin/contracts/token/ERC20/extensions/ERC20Permit.sol";

interface IBlacklist {
    function isBlacklisted(address user) external view returns (bool);
}

contract ISTCoin is ERC20, ERC20Permit {
    IBlacklist private blacklist;

    constructor(address recipient, address blacklistAddr) 
        ERC20("IST Coin", "IST") 
        ERC20Permit("IST Coin") 
    {
        _mint(recipient, 100000000 * 10 ** decimals());
        blacklist = IBlacklist(blacklistAddr);
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        //Not like this. Have to call the blacklist function to check if it is trusted
        require(blacklist.isBlacklisted(msg.sender), "Recipient is blacklisted");

        return super.transfer(to, amount);
    }

    function transferFrom(address from, address to, uint256 amount) public override returns (bool) {
        //Not like this. Have to call the blacklist function to check if it is trusted
        require(blacklist.isBlacklisted(msg.sender), "Recipient is blacklisted");
        //Also check if the sender is blacklisted maybe

        return super.transferFrom(from, to, amount);
    }
}