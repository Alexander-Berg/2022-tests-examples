package ru.yandex.intranet.d.web.front.accounts

import com.google.protobuf.Timestamp
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestAccounts
import ru.yandex.intranet.d.TestFolders
import ru.yandex.intranet.d.TestProviders
import ru.yandex.intranet.d.TestResources
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.backend.service.provider_proto.Account
import ru.yandex.intranet.d.backend.service.provider_proto.AccountsSpaceKey
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundAccountsSpaceKey
import ru.yandex.intranet.d.backend.service.provider_proto.CurrentVersion
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate
import ru.yandex.intranet.d.backend.service.provider_proto.PassportUID
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey
import ru.yandex.intranet.d.backend.service.provider_proto.StaffLogin
import ru.yandex.intranet.d.backend.service.provider_proto.UserID
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsDao
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.accounts.ProviderReserveAccountsDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountReserveType
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.model.accounts.ProviderReserveAccountKey
import ru.yandex.intranet.d.model.accounts.ProviderReserveAccountModel
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.AccountDto
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.accounts.AccountOperationStatusDto
import ru.yandex.intranet.d.web.model.accounts.AccountReserveTypeDto
import ru.yandex.intranet.d.web.model.accounts.AccountReserveTypeInputDto
import ru.yandex.intranet.d.web.model.folders.FrontAccountInputDto
import ru.yandex.intranet.d.web.model.folders.FrontAccountOperationDto
import ru.yandex.intranet.d.web.model.folders.FrontPutAccountDto
import ru.yandex.intranet.d.web.model.folders.FrontReserveAccountsDto
import ru.yandex.intranet.d.web.model.operations.OperationDto
import ru.yandex.intranet.d.web.model.operations.OperationStatusDto
import java.time.Instant
import java.util.*

/**
 * Front reserve accounts API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class FrontReserveAccountsApiTest(@Autowired private val webClient: WebTestClient,
                                  @Autowired private val stubProviderService: StubProviderService,
                                  @Autowired private val accountsQuotasDao: AccountsQuotasDao,
                                  @Autowired private val quotasDao: QuotasDao,
                                  @Autowired private val tableClient: YdbTableClient,
                                  @Autowired private val accountsDao: AccountsDao,
                                  @Autowired private val providerReserveAccountsDao: ProviderReserveAccountsDao) {

    @Test
    fun testCreateProviderReserveAccountSuccess(): Unit = runBlocking {
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setDisplayName("outerDisplayName")
            .setKey("outerKey")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                    .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                        .setResourceSegmentationKey("location")
                        .setResourceSegmentKey("vla")
                        .build())
                    .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                        .setResourceSegmentationKey("segment")
                        .setResourceSegmentKey("default")
                        .build())
                    .build())
                .build())
            .setLastUpdate(LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                    .setPassportUid(PassportUID.newBuilder()
                        .setPassportUid(TestUsers.USER_1_UID).build())
                    .setStaffLogin(StaffLogin.newBuilder()
                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                    .build())
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                .build()).build())))
        val request = FrontAccountInputDto(
            TestFolders.TEST_FOLDER_1_ID,
            TestProviders.YP_ID,
            "outerDisplayName",
            "outerKey",
            TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            false,
            AccountReserveTypeInputDto.PROVIDER)
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/accounts")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(AccountDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.folderId)
        Assertions.assertEquals(TestProviders.YP_ID, result.providerId)
        Assertions.assertEquals("outerDisplayName", result.displayName)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, result.accountsSpacesId)
        Assertions.assertNotEquals("", result.id)
        Assertions.assertFalse(result.isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, result.reserveType.orElseThrow())
        dbSessionRetryable(tableClient) {
            accountsQuotasDao.upsertOneRetryable(rwSingleRetryableCommit(), AccountsQuotasModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setProviderId(TestProviders.YP_ID)
                .setAccountId(result.id)
                .setResourceId(TestResources.YP_SSD_VLA)
                .setProvidedQuota(10_000_000_000L)
                .setAllocatedQuota(5_000_000_000L)
                .setFrozenProvidedQuota(0L)
                .setLastProvisionUpdate(Instant.now())
                .build()).awaitSingle()
            quotasDao.upsertOneRetryable(rwSingleRetryableCommit(), QuotaModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .providerId(TestProviders.YP_ID)
                .folderId(TestFolders.TEST_FOLDER_1_ID)
                .resourceId(TestResources.YP_SSD_VLA)
                .quota(15_000_000_000L)
                .balance(5_000_000_000L)
                .frozenQuota(0L)
                .build()).awaitSingle()
        }
        val reserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/accounts/_reserve")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1,
            reserveAccountsResult.providerReserveAccounts[TestProviders.YP_ID]!!.accounts.size)
        Assertions.assertEquals(1, reserveAccountsResult.accountSpaces.size)
        Assertions.assertEquals(1, reserveAccountsResult.resourceTypes.size)
        Assertions.assertEquals(1, reserveAccountsResult.resources.size)
        Assertions.assertEquals(1, reserveAccountsResult.providers.size)
        Assertions.assertEquals(1,
            reserveAccountsResult.providerReserveAccounts[TestProviders.YP_ID]!!.accounts[0].resources.size)
        Assertions.assertEquals(1, reserveAccountsResult.folders.size)
        Assertions.assertEquals(1, reserveAccountsResult.services.size)
        val providerReserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri { b -> b.path("/front/accounts/_reserve")
                .queryParam("providerId", TestProviders.YP_ID).build() }
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1,
            providerReserveAccountsResult.providerReserveAccounts[TestProviders.YP_ID]!!.accounts.size)
        Assertions.assertEquals(1, providerReserveAccountsResult.accountSpaces.size)
        Assertions.assertEquals(1, providerReserveAccountsResult.resourceTypes.size)
        Assertions.assertEquals(1, providerReserveAccountsResult.resources.size)
        Assertions.assertEquals(1, providerReserveAccountsResult.providers.size)
        Assertions.assertEquals(1,
            providerReserveAccountsResult.providerReserveAccounts[TestProviders.YP_ID]!!.accounts[0].resources.size)
        Assertions.assertEquals(1, providerReserveAccountsResult.folders.size)
        Assertions.assertEquals(1, providerReserveAccountsResult.services.size)
    }

    @Test
    fun testCreateProviderReserveAccountInDifferentFolder(): Unit = runBlocking {
        val firstAccount = AccountModel.Builder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setProviderId(TestProviders.YP_ID)
            .setVersion(0L)
            .setDeleted(false)
            .setDisplayName("something")
            .setAccountsSpacesId(TestAccounts.TEST_ACCOUNT_SPACE_2_ID)
            .setOuterAccountIdInProvider(UUID.randomUUID().toString())
            .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
            .setFolderId(TestFolders.TEST_FOLDER_2_ID)
            .setLastAccountUpdate(Instant.now())
            .setReserveType(AccountReserveType.PROVIDER)
            .build()
        dbSessionRetryable(tableClient) {
            rwTxRetryable { accountsDao.upsertOneRetryable(txSession, firstAccount).awaitSingleOrNull() }
        }
        val reserveAccountModel = ProviderReserveAccountModel(
            ProviderReserveAccountKey(
                Tenants.DEFAULT_TENANT_ID, TestProviders.YP_ID, TestAccounts.TEST_ACCOUNT_SPACE_2_ID, firstAccount.id
            )
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable { providerReserveAccountsDao.upsertOneRetryable(txSession, reserveAccountModel) }
        }
        val request = FrontAccountInputDto(
            TestFolders.TEST_FOLDER_1_ID,
            TestProviders.YP_ID,
            "outerDisplayName",
            "outerKey",
            TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            false,
            AccountReserveTypeInputDto.PROVIDER)
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/accounts")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!

        Assertions.assertEquals(1, result.errors.size)
        Assertions.assertTrue(result.errors.contains("All provider reserve accounts must be in the same folder."))
    }

    @Test
    fun testCreateProviderReserveAccountInDifferentService(): Unit = runBlocking {
        val firstAccount = AccountModel.Builder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setProviderId(TestProviders.YP_ID)
            .setVersion(0L)
            .setDeleted(false)
            .setDisplayName("something")
            .setAccountsSpacesId(TestAccounts.TEST_ACCOUNT_SPACE_2_ID)
            .setOuterAccountIdInProvider(UUID.randomUUID().toString())
            .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
            .setFolderId(TestFolders.TEST_FOLDER_10_ID)
            .setLastAccountUpdate(Instant.now())
            .setReserveType(AccountReserveType.PROVIDER)
            .build()
        dbSessionRetryable(tableClient) {
            rwTxRetryable { accountsDao.upsertOneRetryable(txSession, firstAccount).awaitSingleOrNull() }
        }
        val reserveAccountModel = ProviderReserveAccountModel(
            ProviderReserveAccountKey(
                Tenants.DEFAULT_TENANT_ID, TestProviders.YP_ID, TestAccounts.TEST_ACCOUNT_SPACE_2_ID, firstAccount.id
            )
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable { providerReserveAccountsDao.upsertOneRetryable(txSession, reserveAccountModel) }
        }
        val request = FrontAccountInputDto(
            TestFolders.TEST_FOLDER_1_ID,
            TestProviders.YP_ID,
            "outerDisplayName",
            "outerKey",
            TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            false,
            AccountReserveTypeInputDto.PROVIDER)
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/accounts")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!

        Assertions.assertEquals(1, result.errors.size)
        Assertions.assertTrue(result.errors.contains("All provider reserve accounts must be in the same service."))
    }

    @Test
    fun testPutProviderReserveAccountInDifferentService(): Unit = runBlocking {
        val firstAccount = AccountModel.Builder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setProviderId(TestProviders.YP_ID)
            .setVersion(0L)
            .setDeleted(false)
            .setDisplayName("something1")
            .setAccountsSpacesId(TestAccounts.TEST_ACCOUNT_SPACE_1_ID)
            .setOuterAccountIdInProvider(UUID.randomUUID().toString())
            .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setLastAccountUpdate(Instant.now())
            .setReserveType(AccountReserveType.PROVIDER)
            .build()
        val secondAccount = AccountModel.Builder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setProviderId(TestProviders.YP_ID)
            .setVersion(0L)
            .setDeleted(false)
            .setDisplayName("something2")
            .setAccountsSpacesId(TestAccounts.TEST_ACCOUNT_SPACE_2_ID)
            .setOuterAccountIdInProvider(UUID.randomUUID().toString())
            .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
            .setFolderId(TestFolders.TEST_FOLDER_10_ID)
            .setLastAccountUpdate(Instant.now())
            .setReserveType(null)
            .build()
        dbSessionRetryable(tableClient) {
            rwTxRetryable { accountsDao.upsertAllRetryable(txSession, listOf(firstAccount, secondAccount))
                .awaitSingleOrNull() }
        }
        val firstReserveAccountModel = ProviderReserveAccountModel(
            ProviderReserveAccountKey(
                Tenants.DEFAULT_TENANT_ID, TestProviders.YP_ID, TestAccounts.TEST_ACCOUNT_SPACE_1_ID, firstAccount.id
            )
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable { providerReserveAccountsDao.upsertOneRetryable(txSession, firstReserveAccountModel) }
        }
        val request = FrontPutAccountDto(AccountReserveTypeInputDto.PROVIDER)
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/front/accounts/{accountId}", secondAccount.id)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!

        Assertions.assertEquals(1, result.errors.size)
        Assertions.assertTrue(result.errors.contains("All provider reserve accounts must be in the same service."))
    }

    @Test
    fun testPutProviderReserveAccountInDifferentFolder(): Unit = runBlocking {
        val firstAccount = AccountModel.Builder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setProviderId(TestProviders.YP_ID)
            .setVersion(0L)
            .setDeleted(false)
            .setDisplayName("something1")
            .setAccountsSpacesId(TestAccounts.TEST_ACCOUNT_SPACE_1_ID)
            .setOuterAccountIdInProvider(UUID.randomUUID().toString())
            .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setLastAccountUpdate(Instant.now())
            .setReserveType(AccountReserveType.PROVIDER)
            .build()
        val secondAccount = AccountModel.Builder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setProviderId(TestProviders.YP_ID)
            .setVersion(0L)
            .setDeleted(false)
            .setDisplayName("something2")
            .setAccountsSpacesId(TestAccounts.TEST_ACCOUNT_SPACE_2_ID)
            .setOuterAccountIdInProvider(UUID.randomUUID().toString())
            .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
            .setFolderId(TestFolders.TEST_FOLDER_2_ID)
            .setLastAccountUpdate(Instant.now())
            .setReserveType(null)
            .build()
        dbSessionRetryable(tableClient) {
            rwTxRetryable { accountsDao.upsertAllRetryable(txSession, listOf(firstAccount, secondAccount))
                .awaitSingleOrNull() }
        }
        val firstReserveAccountModel = ProviderReserveAccountModel(
            ProviderReserveAccountKey(
                Tenants.DEFAULT_TENANT_ID, TestProviders.YP_ID, TestAccounts.TEST_ACCOUNT_SPACE_1_ID, firstAccount.id
            )
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable { providerReserveAccountsDao.upsertOneRetryable(txSession, firstReserveAccountModel) }
        }
        val request = FrontPutAccountDto(AccountReserveTypeInputDto.PROVIDER)
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/front/accounts/{accountId}", secondAccount.id)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!

        Assertions.assertEquals(1, result.errors.size)
        Assertions.assertTrue(result.errors.contains("All provider reserve accounts must be in the same folder."))
    }

    @Test
    fun testPutProviderReserveAccountSetSuccess(): Unit = runBlocking {
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setDisplayName("outerDisplayName")
            .setKey("outerKey")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                    .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                        .setResourceSegmentationKey("location")
                        .setResourceSegmentKey("vla")
                        .build())
                    .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                        .setResourceSegmentationKey("segment")
                        .setResourceSegmentKey("default")
                        .build())
                    .build())
                .build())
            .setLastUpdate(LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                    .setPassportUid(PassportUID.newBuilder()
                        .setPassportUid(TestUsers.USER_1_UID).build())
                    .setStaffLogin(StaffLogin.newBuilder()
                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                    .build())
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                .build()).build())))
        val request = FrontAccountInputDto(
            TestFolders.TEST_FOLDER_1_ID,
            TestProviders.YP_ID,
            "outerDisplayName",
            "outerKey",
            TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            false,
            null)
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/accounts")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(AccountDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.folderId)
        Assertions.assertEquals(TestProviders.YP_ID, result.providerId)
        Assertions.assertEquals("outerDisplayName", result.displayName)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, result.accountsSpacesId)
        Assertions.assertNotEquals("", result.id)
        Assertions.assertFalse(result.isFreeTier)
        val putRequest = FrontPutAccountDto(AccountReserveTypeInputDto.PROVIDER)
        val putResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/front/accounts/{accountId}", result.id)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontAccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(putResult)
        Assertions.assertNotNull(putResult.result)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, putResult.result!!.folderId)
        Assertions.assertEquals(TestProviders.YP_ID, putResult.result!!.providerId)
        Assertions.assertEquals("outerDisplayName", putResult.result!!.displayName)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, putResult.result!!.accountsSpacesId)
        Assertions.assertNotEquals("", putResult.result!!.id)
        Assertions.assertNotEquals("", putResult.operationId)
        Assertions.assertFalse(putResult.result!!.isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, putResult.result!!.reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, putResult.operationStatus)
        val putOperationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", putResult.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(putOperationResult)
        Assertions.assertEquals(putResult.operationId, putOperationResult.id)
        Assertions.assertEquals(TestProviders.YP_ID, putOperationResult.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, putOperationResult.accountsSpaceId.orElseThrow())
        Assertions.assertEquals(OperationStatusDto.SUCCESS, putOperationResult.status)
        Assertions.assertFalse(putOperationResult.failure.isPresent)
        val reserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/accounts/_reserve")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1, reserveAccountsResult.providerReserveAccounts[TestProviders.YP_ID]!!.accounts.size)
    }

    @Test
    fun testGetNoReserves(): Unit = runBlocking {
        val providerReserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri { b -> b.path("/front/accounts/_reserve")
                .queryParam("providerId", TestProviders.YDB_ID).build() }
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertTrue(providerReserveAccountsResult.providerReserveAccounts.isEmpty())
    }

}
