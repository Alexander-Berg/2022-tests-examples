package ru.auto

import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.server.routing.*
import kotlin.test.*
import io.ktor.server.testing.*
import ru.auto.routing.configureMarketplaceRouting
import ru.auto.server.ApiConfig
import ru.auto.server.AppConfig
import ru.auto.server.DeploymentConfig
import ru.auto.server.config.AuthToken


class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            routing {
                configureMarketplaceRouting(
                    AppConfig(
                        deploymentConfig = DeploymentConfig(
                            isDev = true,
                            appVersion = "test",
                            serviceName = "test",
                            branch = null,
                            hostname = "test",
                            isCanary = false,
                            salt = "",
                            requiredTamper = false,
                            metricsPort = 100
                        ),
                        apiConfig = ApiConfig(
                            publicApiScheme = "scheme",
                            publicApiHost = "test",
                            publicApiPort = 10,
                            publicApiKey = AuthToken(""),
                            journalApiScheme = "scheme",
                            journalApiHost = "host",
                            journalApiPort = 10,
                            bunkerApi = "test",
                        )
                    )
                )
            }
        }
        client.get("/ping").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
