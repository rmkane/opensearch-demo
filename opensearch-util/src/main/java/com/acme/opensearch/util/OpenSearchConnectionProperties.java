package com.acme.opensearch.util;

/**
 * Connection settings for {@link OpenSearchClientFactory}.
 *
 * @param uri
 *            cluster URL, e.g. {@code https://localhost:443}
 * @param username
 *            basic-auth username
 * @param password
 *            basic-auth password
 * @param trustSelfSigned
 *            when {@code true}, trust self-signed TLS certs (local Docker demo
 *            only)
 */
public record OpenSearchConnectionProperties(String uri, String username, String password, boolean trustSelfSigned) {
}
