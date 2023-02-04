package ru.yandex.intranet.d.rest

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.imports.*

/**
 * WebFluxConfigurationTest.
 *
 * @author Petr Surkov <petrsurkov@yandex-team.ru>
 */
@IntegrationTest
class WebFluxConfigurationTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    /**
     * Approximately 7.2 MB is sent to the Rest API, which is less than the max-in-memory-size
     */
    @Test
    fun maxInMemorySizeAtLeast7MbTest() {
        val quotasToImport = ImportDto(
            List(10000) {
                ImportFolderDto("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                    null,
                    listOf(
                        ImportResourceDto(
                            "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                            null,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            100L,
                            "gigabytes",
                            50L,
                            "gigabytes"
                        )
                    ),
                    listOf(
                        ImportAccountDto(
                            "1",
                            "test",
                            "Test",
                            false,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            listOf(
                                ImportAccountProvisionDto(
                                    "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                                    null,
                                    50L,
                                    "gigabytes",
                                    25L,
                                    "gigabytes"
                                )
                            ),
                            AccountSpaceIdentityDto(
                                "978bd75a-cf67-44ac-b944-e8ca949bdf7e",
                                null
                            ), false)
                    )
                )
            }
        )
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/import/_importQuotas")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(quotasToImport)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
    }

    /**
     * Approximately 14.4 MB is sent to the Rest API, which is more than the max-in-memory-size
     */
    @Test
    fun statusCodeWhenPayloadExceedsMaxInMemorySizeTest() {
        val quotasToImport = ImportDto(
            List(20000) {
                ImportFolderDto("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                    null,
                    listOf(
                        ImportResourceDto(
                            "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                            null,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            100L,
                            "gigabytes",
                            50L,
                            "gigabytes"
                        )
                    ),
                    listOf(
                        ImportAccountDto(
                            "1",
                            "test",
                            "Test",
                            false,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            listOf(
                                ImportAccountProvisionDto(
                                    "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                                    null,
                                    50L,
                                    "gigabytes",
                                    25L,
                                    "gigabytes"
                                )
                            ),
                            AccountSpaceIdentityDto(
                                "978bd75a-cf67-44ac-b944-e8ca949bdf7e",
                                null
                            ), false)
                    )
                )
            }
        )
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/import/_importQuotas")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(quotasToImport)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE)
            .expectBody(Map::class.java)
            .returnResult()
            .responseBody
        Assertions.assertEquals(
            mapOf("status" to 413,
                "error" to "Payload Too Large",
                "message" to "Payload content length greater than maximum allowed 10485760"),
            result)
    }
}
