package ru.yandex.intranet.d.web.api.providers

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.i18n.Locales
import ru.yandex.intranet.d.services.sync.AccountsSyncService
import ru.yandex.intranet.d.services.sync.SyncTestHelper
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.providers.ProviderSyncErrorDto
import ru.yandex.intranet.d.web.model.providers.ProviderSyncStatusDto
import java.time.Clock
import java.time.Duration
import java.time.Instant
import ru.yandex.intranet.d.web.model.providers.ProviderSyncStatusDto.SyncStatuses.DONE_OK as DONE_OK_DTO

/**
 * SyncStatusTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 24-06-2021
 */
@IntegrationTest
class SyncStatusTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var syncTestHelper: SyncTestHelper

    @Autowired
    private lateinit var accountsSyncService: AccountsSyncService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    /**
     * @see ru.yandex.intranet.d.web.controllers.api.v1.providers.ApiV1ProvidersController.getSyncStatus
     */
    @Test
    internal fun getSyncStatusTest() {
        syncTestHelper.initProviderStub { provider, user ->
            accountsSyncService.syncOneProvider(provider, Locales.ENGLISH, Clock.systemUTC()).block()

            val syncStatus = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/" + provider.id + "/_syncStatus")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody(ProviderSyncStatusDto::class.java)
                .returnResult()
                .responseBody

            assertEquals(DONE_OK_DTO, syncStatus.lastSyncStatus)
            assertEquals(1, syncStatus.accountsCount)
            assertEquals(1, syncStatus.quotasCount)
        }
    }

    @Test
    fun providerSyncStatusDtoSerializationTest() {
        val s = objectMapper.writeValueAsString(
            ProviderSyncStatusDto(
                /* lastSyncStart = */ Instant.ofEpochSecond(1L),
                /* lastSyncFinish = */ Instant.ofEpochSecond(6L),
                /* lastSyncStatus = */ ProviderSyncStatusDto.SyncStatuses.DONE_OK,
                /* accountsCount = */ 3L,
                /* quotasCount = */ 4L,
                /* syncDuration = */ Duration.ofSeconds(5),
                /* providerSyncErrors = */ listOf(ProviderSyncErrorDto(
                    Instant.ofEpochSecond(5L),
                    "Error message",
                    HashMap()
                ))
            )
        )
        assertEquals(
          """{"lastSyncStart":"1970-01-01T00:00:01.000Z","lastSyncFinish":"1970-01-01T00:00:06.000Z",""" +
          """"lastSyncStatus":"DONE_OK","accountsCount":3,"quotasCount":4,"syncDuration":5.000000000,""" +
          """"providerSyncErrors":[{"requestTimestamp":"1970-01-01T00:00:05.000Z",""" +
          """"errorMessage":"Error message","errorDetails":{}}]}""", s
        )
    }
}
