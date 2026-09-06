CREATE TABLE carts (
id BIGINT AUTO_INCREMENT PRIMARY KEY,
user_id BIGINT NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
created_at TIMESTAMP(6) NOT NULL,
updated_at TIMESTAMP(6) NOT NULL,
CONSTRAINT uk_carts_active_user UNIQUE (user_id, status)
);

CREATE INDEX idx_carts_user_id ON carts(user_id);

CREATE INDEX idx_carts_status ON carts(status);

CREATE TABLE cart_items (
id BIGINT AUTO_INCREMENT PRIMARY KEY,
cart_id BIGINT NOT NULL,
product_id BIGINT NOT NULL,
quantity INT NOT NULL,
unit_price DECIMAL(12,2) NOT NULL,
created_at TIMESTAMP(6) NOT NULL,
updated_at TIMESTAMP(6) NOT NULL,
CONSTRAINT fk_cart_items_cart
FOREIGN KEY (cart_id)
REFERENCES carts(id)
ON DELETE CASCADE,
CONSTRAINT uk_cart_items_product
UNIQUE (cart_id, product_id),
CONSTRAINT chk_cart_items_quantity
CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);

CREATE INDEX idx_cart_items_product_id ON cart_items(product_id);
