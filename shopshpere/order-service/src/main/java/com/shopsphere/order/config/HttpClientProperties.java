package com.shopsphere.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection and read timeouts applied to all outbound RestClient calls.
 * Bound from {@code shopsphere.http.*} properties. Deliberately conservative
 * defaults so a slow/unreachable downstream service cannot hang a request
 * indefinitely.
 */
@ConfigurationProperties(prefix = "shopsphere.http")
public class HttpClientProperties {

    /** Time allowed to establish a TCP connection, in milliseconds. */
    private long connectTimeoutMs = 2000;

    /** Time allowed to read the response once connected, in milliseconds. */
    private long readTimeoutMs = 3000;

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
