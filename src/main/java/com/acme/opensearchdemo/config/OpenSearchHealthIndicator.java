package com.acme.opensearchdemo.config;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("opensearch")
@RequiredArgsConstructor
public class OpenSearchHealthIndicator implements HealthIndicator {

	private final OpenSearchClient client;

	@Override
	public Health health() {
		try {
			HealthResponse response = client.cluster().health();
			HealthStatus status = response.status();

			Health.Builder builder = isClusterHealthy(status) ? Health.up() : Health.down();
			if (!isClusterHealthy(status)) {
				log.warn("OpenSearch cluster unhealthy: status={}, unassignedShards={}", status.jsonValue(),
						response.unassignedShards());
			}
			/* spotless:off */
			return builder.withDetail("clusterName", response.clusterName())
					.withDetail("status", status.jsonValue())
					.withDetail("numberOfNodes", response.numberOfNodes())
					.withDetail("activeShards", response.activeShards())
					.withDetail("unassignedShards", response.unassignedShards())
					.build();
			/* spotless:on */
		} catch (Exception ex) {
			log.error("OpenSearch health check failed", ex);
			return Health.down().withException(ex).build();
		}
	}

	private boolean isClusterHealthy(HealthStatus status) {
		return status == HealthStatus.Green || status == HealthStatus.Yellow;
	}
}
