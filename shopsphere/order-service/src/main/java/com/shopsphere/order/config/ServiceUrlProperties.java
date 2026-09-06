package com.shopsphere.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Base URLs for the downstream services that Order Service calls
 * synchronously. Bound from {@code shopsphere.services.*.url} properties,
 * each overridable via an environment variable (see application.properties).
 * <p>
 * Only services actually called by Order Service are configured here
 * (Product, Inventory, Payment, Cart) - User Service and Notification
 * Service are intentionally NOT called synchronously at this stage.
 */
@ConfigurationProperties(prefix = "shopsphere.services")
public class ServiceUrlProperties {

    private final Endpoint product = new Endpoint();
    private final Endpoint inventory = new Endpoint();
    private final Endpoint payment = new Endpoint();
    private final Endpoint cart = new Endpoint();

    public Endpoint getProduct() {
        return product;
    }

    public Endpoint getInventory() {
        return inventory;
    }

    public Endpoint getPayment() {
        return payment;
    }

    public Endpoint getCart() {
        return cart;
    }

    public static class Endpoint {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
