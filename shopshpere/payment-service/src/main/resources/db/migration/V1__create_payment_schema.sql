CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_reference VARCHAR(100) NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_payments_reference UNIQUE (payment_reference),
    CONSTRAINT chk_payments_amount_pos CHECK (amount > 0)
);

CREATE INDEX idx_payments_payment_reference ON payments(payment_reference);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
