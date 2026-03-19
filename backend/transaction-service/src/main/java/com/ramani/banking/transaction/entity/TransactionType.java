package com.ramani.banking.transaction.entity;

public enum TransactionType {
    P2P_TRANSFER,
    BANK_TO_WALLET,
    WALLET_TO_BANK,
    WALLET_TO_WALLET,
    TOP_UP,
    WITHDRAWAL,
    REVERSAL,
    SCHEDULED_PAYMENT,
    FEE
}
