CREATE TABLE inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    available_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    reorder_level INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_inventory_product_id UNIQUE (product_id),
    CONSTRAINT chk_inventory_avail_non_neg CHECK (available_quantity >= 0),
    CONSTRAINT chk_inventory_res_non_neg CHECK (reserved_quantity >= 0)
);

CREATE INDEX idx_inventory_product_id ON inventory(product_id);
CREATE INDEX idx_inventory_sku ON inventory(sku);

CREATE TABLE inventory_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RESERVED',
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_reservations_inventory FOREIGN KEY (inventory_id) REFERENCES inventory(id) ON DELETE CASCADE,
    CONSTRAINT chk_reservations_qty_pos CHECK (quantity > 0)
);

CREATE INDEX idx_reservations_inventory_id ON inventory_reservations(inventory_id);
CREATE INDEX idx_reservations_order_id ON inventory_reservations(order_id);
CREATE INDEX idx_reservations_status ON inventory_reservations(status);
