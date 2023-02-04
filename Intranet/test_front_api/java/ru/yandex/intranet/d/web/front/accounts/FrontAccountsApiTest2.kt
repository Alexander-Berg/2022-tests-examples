package ru.yandex.intranet.d.web.front.accounts

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.util.function.Tuples
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestAccounts.*
import ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID
import ru.yandex.intranet.d.TestProviders.YP_ID
import ru.yandex.intranet.d.TestSegmentations.*
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.accounts.AccountsWithQuotaDto
import java.util.*
import java.util.stream.Collectors

/**
 * FrontAccountsController test.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 * @since 23-11-2021
 */
@IntegrationTest
class FrontAccountsApiTest2 {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    fun getAccountsForUnknownFolder404Test() {
        val folderId = "fake"
        val providerId = "fake"
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/accounts/${folderId}/${providerId}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is4xxClientError
    }

    @Test
    fun getAccountsTest() {
        val folderId = UUID.randomUUID()
        val providerId = UUID.randomUUID()
        val returnResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/accounts/${folderId}/${providerId}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(AccountsWithQuotaDto::class.java)
            .returnResult()

        val responseBody = returnResult.responseBody
        Assertions.assertEquals(0, responseBody.accountWithQuotaDtoList.size)

        val returnResult2 = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/accounts/${TEST_FOLDER_1_ID}/${YP_ID}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(AccountsWithQuotaDto::class.java)
            .returnResult()

        val responseBody2 = returnResult2.responseBody
        Assertions.assertEquals(1, responseBody2.accountWithQuotaDtoList.size)
        val accountWithQuotaDto = responseBody2.accountWithQuotaDtoList.get(0)
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_3_ID, accountWithQuotaDto.accountsSpaceId)
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, accountWithQuotaDto.id)
        Assertions.assertEquals(TEST_ACCOUNT_1.displayName.orElseThrow(), accountWithQuotaDto.displayName)
        Assertions.assertEquals(setOf(Tuples.of(YP_LOCATION_KEY, YP_LOCATION_MAN_KEY),
            Tuples.of(YP_SEGMENT_KEY, YP_SEGMENT_DEFAULT_KEY)),
            accountWithQuotaDto.accountsSpaceKey.segmentation.orElseThrow().stream()
                .map { s -> Tuples.of(s.segmentationKey.orElseThrow(), s.segmentKey.orElseThrow()) }
                .collect(Collectors.toSet()))

        Assertions.assertEquals(setOf(
            Tuples.of("ef333da9-b076-42f5-b7f5-84cd04ab7fcc", "200000000000", "100000000000"),
            Tuples.of("14e2705c-ff49-43a4-8048-622e373f5891", "80000000000", "0"),
        ),
            accountWithQuotaDto.provisions.stream()
                .map { p -> Tuples.of(p.resourceId, p.provided.rawAmount, p.allocated.rawAmount) }
                .collect(Collectors.toSet()))
    }

}
