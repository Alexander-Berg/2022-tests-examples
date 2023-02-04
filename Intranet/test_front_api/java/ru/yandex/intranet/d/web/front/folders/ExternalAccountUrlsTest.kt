package ru.yandex.intranet.d.web.front.folders

import com.yandex.ydb.table.transaction.TransactionMode.SERIALIZABLE_READ_WRITE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1
import ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_10_ID
import ru.yandex.intranet.d.TestAccounts.TEST_YT_POOL_ID
import ru.yandex.intranet.d.TestProviders
import ru.yandex.intranet.d.TestProviders.YP_ID
import ru.yandex.intranet.d.TestProviders.YT_ID
import ru.yandex.intranet.d.TestResources.YDB_RAM_SAS
import ru.yandex.intranet.d.TestResources.YP_HDD_MAN
import ru.yandex.intranet.d.TestSegmentations.YP_LOCATION_MAN
import ru.yandex.intranet.d.TestSegmentations.YP_SEGMENT_DEFAULT
import ru.yandex.intranet.d.TestServices
import ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_DISPENSER
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.datasource.model.YdbSession
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.datasource.model.YdbTxSession
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.providers.ExternalAccountUrlTemplate
import ru.yandex.intranet.d.services.quotas.ExternalAccountUrlFactory
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.folders.FrontFolderWithQuotesDto

@IntegrationTest
class ExternalAccountUrlsTest(
    @Autowired
    val webClient: WebTestClient,
    @Autowired
    val resourcesDao: ResourcesDao,
    @Autowired
    val ydbTableClient: YdbTableClient
) {

    @Test
    fun testYpUrl() {
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/folders/?serviceId={serviceId}", TEST_SERVICE_ID_DISPENSER)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontFolderWithQuotesDto::class.java)
            .returnResult()
            .responseBody!!

        val accountWithResources = result.folders
            .flatMap { f -> f.providers }
            .filter { p -> p.providerId.equals(YP_ID) }
            .flatMap { p -> p.accounts }
            .filter { a -> a.resources.isNotEmpty() }[0]
        assertTrue(accountWithResources.urlsForSegments)
        assertEquals(
            mapOf(
                "$YP_SEGMENT_DEFAULT.$YP_LOCATION_MAN:Yandex Deploy"
                    to "https://deploy.yandex-team.ru/yp/man/pod-sets?accountId=abc:service:1&segments=default"
            ),
            accountWithResources.externalAccountUrls
        )

        // url for YP account without resources can not be generated. Must be null.
        val accountWithoutResources = result.folders
            .flatMap { f -> f.providers }
            .filter { p -> p.providerId.equals(YP_ID) }
            .flatMap { p -> p.accounts }
            .filter { a -> a.resources.isEmpty() }[0]

        assertNull(accountWithoutResources.externalAccountUrls)
    }

    @Test
    fun testYdbAndMdbUrl() {
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/folders/?serviceId={serviceId}", TestServices.TEST_SERVICE_ID_ABC)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontFolderWithQuotesDto::class.java)
            .returnResult()
            .responseBody!!

        val ydbAccount = result.folders
            .flatMap { f -> f.providers }
            .filter { p -> p.providerId.equals(TestProviders.YDB_ID) }
            .flatMap { p -> p.accounts }[0]

        assertFalse(ydbAccount.urlsForSegments)
        assertEquals(
            mapOf("Yandex Cloud" to "https://yc.yandex-team.ru/clouds/666666"),
            ydbAccount.externalAccountUrls
        )
    }

    @Test
    fun testSandboxUrl() {
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/folders/?serviceId={serviceId}", TestServices.TEST_SERVICE_WITH_UNMANAGED_PROVIDER)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontFolderWithQuotesDto::class.java)
            .returnResult()
            .responseBody!!

        val sandboxAccount = result.folders
            .flatMap { f -> f.providers }
            .filter { p -> p.providerId.equals(TestProviders.UNMANAGED_PROVIDER_ID) }
            .flatMap { p -> p.accounts }[0]

        assertFalse(sandboxAccount.urlsForSegments)
        assertEquals(
            mapOf("Sandbox" to "https://sandbox.yandex-team.ru/admin/groups/Account_of_unmanaged_provider"),
            sandboxAccount.externalAccountUrls
        )
    }

    @Test
    fun testYtAccountUrl() {
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/folders/?serviceId={serviceId}", TestServices.TEST_SERVICE_WITH_YT_RESOURCES)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontFolderWithQuotesDto::class.java)
            .returnResult()
            .responseBody!!

        val ytAccount = result.folders
            .flatMap { f -> f.providers }
            .filter { p -> p.providerId.equals(YT_ID) }
            .flatMap { p -> p.accounts }
            .first { a -> a.account.id == TEST_ACCOUNT_10_ID }
        assertFalse(ytAccount.urlsForSegments)
        assertEquals(
            mapOf("YT" to "https://yt.yandex-team.ru/arnold/accounts/general?account=d-prod"),
            ytAccount.externalAccountUrls
        )

        val ytPool = result.folders
            .flatMap { f -> f.providers }
            .filter { p -> p.providerId.equals(YT_ID) }
            .flatMap { p -> p.accounts }
            .first { a -> a.account.id == TEST_YT_POOL_ID }
        assertFalse(ytPool.urlsForSegments)
        assertEquals(
            mapOf("YT" to "https://yt.yandex-team.ru/freud/scheduling/overview?pool=test-yt-pool&tree=physical"),
            ytPool.externalAccountUrls
        )
    }

    @Test
    fun noUrlTemplateTest() {
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/folders/?serviceId={serviceId}", TestServices.TEST_SERVICE_ID_MULTIPLE_RESERVE_FOLDERS)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontFolderWithQuotesDto::class.java)
            .returnResult()
            .responseBody!!

        // Doesn't have any templates in provider's accounts settings
        val accountWithNullUrlTemplate = result.folders
            .flatMap { f -> f.providers }
            .filter { p -> p.providerId.equals(TestProviders.DEFAULT_QUOTAS_PROVIDER_ID) }
            .flatMap { p -> p.accounts }[0]

        assertNull(accountWithNullUrlTemplate.externalAccountUrls)
        assertNull(accountWithNullUrlTemplate.urlsForSegments)
    }

    @Test
    fun buildUrlWithoutDataWithSegmentationsTest() {
        val templates = setOf(
            ExternalAccountUrlTemplate(
                urlTemplates = mapOf(
                    "YP" to "https://deploy.yandex-team.ru/yp/{resource_segment_location}/{service_id}"
                ),
                urlsForSegments = true
            )
        )
        val urlFactory = ExternalAccountUrlFactory(templates)

        val resource = ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(SERIALIZABLE_READ_WRITE) { ts: YdbTxSession? ->
                resourcesDao.getById(ts, YP_HDD_MAN, DEFAULT_TENANT_ID)
            }
        }
            .block()!!
            .orElseThrow()

        val urls1 = urlFactory.generateUrl(TEST_ACCOUNT_1, listOf(resource))
        assertNull(urls1)

        val urls2 = urlFactory.generateUrl(TEST_ACCOUNT_1, listOf())
        assertNull(urls2)

        val urls3 = urlFactory.generateUrl(AccountModel.Builder().build(), listOf(resource))
        assertNull(urls3)

        val urls4 = urlFactory.generateUrl(AccountModel.Builder().build(), listOf())
        assertNull(urls4)
    }

    @Test
    fun buildUrlWithoutDataWithoutSegmentationsTest() {
        val templates = setOf(
            ExternalAccountUrlTemplate(
                urlTemplates = mapOf("YDB" to "https://yc.yandex-team.ru/clouds/{outer_account_id_in_provider}"),
                urlsForSegments = false
            )
        )
        val urlFactory = ExternalAccountUrlFactory(templates)

        val resource = ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(SERIALIZABLE_READ_WRITE) { ts: YdbTxSession? ->
                resourcesDao.getById(ts, YDB_RAM_SAS, DEFAULT_TENANT_ID)
            }
        }
            .block()!!
            .orElseThrow()

        val urls1 = urlFactory.generateUrl(AccountModel.Builder().build(), listOf(resource))
        assertNull(urls1)

        val urls2 = urlFactory.generateUrl(AccountModel.Builder().build(), listOf())
        assertNull(urls2)
    }
}
