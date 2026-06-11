package com.acme.opensearch.util;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
/**
 * Builds an {@link OpenSearchClient} from
 * {@link OpenSearchConnectionProperties}.
 * <p>
 * Framework-free so future Spring Boot starters can share the same client
 * construction logic.
 */
@Slf4j
@UtilityClass
public final class OpenSearchClientFactory {

	public static OpenSearchClient create(OpenSearchConnectionProperties properties) {
		URI parsedUri = URI.create(properties.uri());
		HttpHost host = new HttpHost(parsedUri.getScheme(), parsedUri.getHost(), resolvePort(parsedUri));

		BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(new AuthScope(host),
				new UsernamePasswordCredentials(properties.username(), properties.password().toCharArray()));

		ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(host)
				.setMapper(new JacksonJsonpMapper()).setHttpClientConfigCallback(httpClientBuilder -> {
					httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

					if (properties.trustSelfSigned()) {
						// HttpClient 5: TLS via PoolingAsyncClientConnectionManager + TlsStrategy
						// (HttpAsyncClientBuilder no longer exposes setSSLContext).
						// Local Docker demo only — do not use trust-all in production.
						SSLContext sslContext = createTrustAllSslContext();
						TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create().setSslContext(sslContext)
								.setHostnameVerifier(NoopHostnameVerifier.INSTANCE).buildAsync();
						PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder
								.create().setTlsStrategy(tlsStrategy).build();
						httpClientBuilder.setConnectionManager(connectionManager);
					}

					return httpClientBuilder;
				});

		OpenSearchTransport transport = builder.build();
		log.info("OpenSearch client configured for {}://{}:{} (trustSelfSigned={})", parsedUri.getScheme(),
				parsedUri.getHost(), host.getPort(), properties.trustSelfSigned());
		return new OpenSearchClient(transport);
	}

	// Default HTTPS/HTTP ports when the URI omits an explicit port (e.g.
	// https://localhost:443).
	private static int resolvePort(URI uri) {
		if (uri.getPort() != -1) {
			return uri.getPort();
		}

		if ("https".equalsIgnoreCase(uri.getScheme())) {
			return 443;
		}

		if ("http".equalsIgnoreCase(uri.getScheme())) {
			return 80;
		}

		throw new IllegalArgumentException("Unsupported OpenSearch URI scheme: " + uri.getScheme());
	}

	private static SSLContext createTrustAllSslContext() {
		try {
			TrustStrategy trustAll = (chain, authType) -> true;
			return SSLContexts.custom().loadTrustMaterial(null, trustAll).build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException ex) {
			throw new IllegalStateException("Failed to create OpenSearch SSL context", ex);
		}
	}
}
