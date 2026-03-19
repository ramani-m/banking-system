-- Account Service Schema

CREATE TABLE IF NOT EXISTS accounts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL,
    account_number          VARCHAR(20) NOT NULL UNIQUE,
    type                    VARCHAR(20) NOT NULL,
    currency                VARCHAR(3) NOT NULL DEFAULT 'USD',
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    available_balance       DECIMAL(19, 4) NOT NULL DEFAULT 0,
    pending_balance         DECIMAL(19, 4) NOT NULL DEFAULT 0,
    daily_transfer_limit    DECIMAL(19, 4) NOT NULL DEFAULT 10000.00,
    daily_transferred_today DECIMAL(19, 4) NOT NULL DEFAULT 0,
    daily_limit_reset_at    TIMESTAMP,
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    CONSTRAINT chk_available_balance_non_negative CHECK (available_balance >= 0),
    CONSTRAINT chk_pending_balance_non_negative CHECK (pending_balance >= 0)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_accounts_account_number ON accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts(status);
