package pl.allegro.tech.servicemesh.envoycontrol.snapshot

import com.google.protobuf.util.Durations
import org.junit.jupiter.api.Test
import pl.allegro.tech.servicemesh.envoycontrol.groups.DependencySettings
import pl.allegro.tech.servicemesh.envoycontrol.groups.Outgoing
import pl.allegro.tech.servicemesh.envoycontrol.groups.hasCustomIdleTimeout
import pl.allegro.tech.servicemesh.envoycontrol.groups.hasCustomRequestTimeout
import pl.allegro.tech.servicemesh.envoycontrol.groups.hasRequestHeaderToAdd
import pl.allegro.tech.servicemesh.envoycontrol.groups.hasNoRequestHeaderToAdd
import pl.allegro.tech.servicemesh.envoycontrol.groups.hasResponseHeaderToAdd

internal class EnvoyEgressRoutesFactoryTest {

    val clusters = listOf(
        RouteSpecification(
            clusterName = "srv1",
            routeDomain = "srv1",
            settings = DependencySettings(
                handleInternalRedirect = true,
                timeoutPolicy = Outgoing.TimeoutPolicy(
                    idleTimeout = Durations.fromSeconds(10L),
                    requestTimeout = Durations.fromSeconds(10L)
                )
            )
        )
    )

    @Test
    fun `should add client identity header if incoming permissions are enabled`() {
        // given
        val routesFactory = EnvoyEgressRoutesFactory(SnapshotProperties().apply {
            incomingPermissions.enabled = true
        })

        // when
        val routeConfig = routesFactory.createEgressRouteConfig("client1", clusters, false)

        // then
        routeConfig
            .hasRequestHeaderToAdd("x-service-name", "client1")

        routeConfig
            .virtualHostsList[0]
            .routesList[0]
            .route
            .hasCustomIdleTimeout(Durations.fromSeconds(10L))
            .hasCustomRequestTimeout(Durations.fromSeconds(10L))
    }

    @Test
    fun `should not add client identity header if incoming permissions are disabled`() {
        // given
        val routesFactory = EnvoyEgressRoutesFactory(SnapshotProperties().apply {
            incomingPermissions.enabled = false
        })

        // when
        val routeConfig = routesFactory.createEgressRouteConfig("client1", clusters, false)

        // then
        routeConfig
            .hasNoRequestHeaderToAdd("x-service-name")

        routeConfig
            .virtualHostsList[0]
            .routesList[0]
            .route
            .hasCustomIdleTimeout(Durations.fromSeconds(10L))
            .hasCustomRequestTimeout(Durations.fromSeconds(10L))
    }

    @Test
    fun `should add upstream remote address header if addUpstreamAddress is enabled`() {
        // given
        val routesFactory = EnvoyEgressRoutesFactory(SnapshotProperties())

        // when
        val routeConfig = routesFactory.createEgressRouteConfig("client1", clusters, true)

        // then
        routeConfig
            .hasResponseHeaderToAdd("x-envoy-upstream-remote-address", "%UPSTREAM_REMOTE_ADDRESS%")
    }

    @Test
    fun `should not add upstream remote address header if addUpstreamAddress is disabled`() {
        // given
        val routesFactory = EnvoyEgressRoutesFactory(SnapshotProperties())

        // when
        val routeConfig = routesFactory.createEgressRouteConfig("client1", clusters, false)

        // then
        routeConfig
            .hasNoRequestHeaderToAdd("x-envoy-upstream-remote-address")
    }
}
