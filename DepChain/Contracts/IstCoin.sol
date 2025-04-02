// SPDX-License-Identifier: MIT
// Compatible with OpenZeppelin Contracts ^5.0.0
pragma solidity ^0.8.22;

import {ERC20} from "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import {ERC20Permit} from "@openzeppelin/contracts/token/ERC20/extensions/ERC20Permit.sol";
import {Blacklist} from "Blacklist.sol";

contract ISTCoin is ERC20, ERC20Permit, Blacklist {

    constructor(address owner) 
        ERC20("IST Coin", "IST") 
        ERC20Permit("IST Coin")
        Blacklist(owner) 
    {
        _mint(owner, 100000000 * 10 ** decimals());
    }

    function decimals() public pure override returns (uint8) {
        return 2; // Explicitly define decimals
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!isBlacklisted(msg.sender), "Sender is blacklisted");

        return super.transfer(to, amount);
    }

    function transferFrom(address from, address to, uint256 amount) public override returns (bool) {
        require(!isBlacklisted(from), "Sender is blacklisted");

        return super.transferFrom(from, to, amount);
    }
}