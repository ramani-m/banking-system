-- Transaction Service Schema

CREATE TABLE IF NOT EXISTS transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key     VARCHAR(100) NOT NULL UNIQUE,
    from_account_id     UUID,
    to_account_id       UUID,
    initiated_by        UUID NOT NULL,
    amount              DECIMAL(19, 4) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'USD',
    type                VARCHAR(30) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description         VARCHAR(500),
    reference_number    VARCHAR(50),
    failure_reason      VARCHAR(500),
    metadata            JSONB,
    processed_at        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    CONSTRAINT chk_amount_positive CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS scheduled_payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    from_account_id     UUID NOT NULL,
    to_account_id       UUID NOT NULL,
    amount              DECIMAL(19, 4) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'USD',
    description         VARCHAR(200),
    frequency           VARCHAR(20) NOT NULL,
    next_run_at         TIMESTAMP NOT NULL,
    end_date            TIMESTAMP,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_executions    INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_txn_idempotency_key ON transactions(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_txn_from_account ON transactions(from_account_id);
CREATE INDEX IF NOT EXISTS idx_txn_to_account ON transactions(to_account_id);
CREATE INDEX IF NOT EXISTS idx_txn_initiated_by ON transactions(initiated_by);
CREATE INDEX IF NOT EXISTS idx_txn_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_txn_created_at ON transactions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_scheduled_next_run ON scheduled_payments(next_run_at) WHERE status = 'ACTIVE';
