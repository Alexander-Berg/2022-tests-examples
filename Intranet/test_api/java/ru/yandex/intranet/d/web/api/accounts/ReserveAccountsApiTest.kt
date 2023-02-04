package ru.yandex.intranet.d.web.api.accounts

import com.google.protobuf.Timestamp
import com.yandex.ydb.table.transaction.TransactionMode
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestAccounts
import ru.yandex.intranet.d.TestFolders
import ru.yandex.intranet.d.TestProviders
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
import ru.yandex.intranet.d.dao.accounts.AccountsSpacesDao
import ru.yandex.intranet.d.dao.accounts.ProviderReserveAccountsDao
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao
import ru.yandex.intranet.d.datasource.dbSession
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbSession
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.datasource.model.YdbTxSession
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountReserveType
import ru.yandex.intranet.d.model.accounts.AccountSpaceModel
import ru.yandex.intranet.d.model.accounts.ProviderReserveAccountKey
import ru.yandex.intranet.d.model.accounts.ProviderReserveAccountModel
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel
import ru.yandex.intranet.d.model.providers.AggregationSettings
import ru.yandex.intranet.d.model.providers.FreeProvisionAggregationMode
import ru.yandex.intranet.d.model.providers.ProviderModel
import ru.yandex.intranet.d.model.providers.UsageMode
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.SortOrderDto
import ru.yandex.intranet.d.web.model.accounts.AccountOperationDto
import ru.yandex.intranet.d.web.model.accounts.AccountOperationStatusDto
import ru.yandex.intranet.d.web.model.accounts.AccountReserveTypeDto
import ru.yandex.intranet.d.web.model.accounts.AccountReserveTypeInputDto
import ru.yandex.intranet.d.web.model.accounts.CreateAccountDto
import ru.yandex.intranet.d.web.model.accounts.PutAccountDto
import ru.yandex.intranet.d.web.model.operations.OperationDto
import ru.yandex.intranet.d.web.model.operations.OperationStatusDto
import ru.yandex.intranet.d.web.model.providers.ProviderReserveAccountsDto
import java.time.Instant
import java.util.*

/**
 * Reserve accounts API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class ReserveAccountsApiTest(@Autowired private val webClient: WebTestClient,
                             @Autowired private val stubProviderService: StubProviderService,
                             @Autowired private val tableClient: YdbTableClient,
                             @Autowired private var folderOperationLogDao: FolderOperationLogDao,
                             @Autowired private val accountsDao: AccountsDao,
                             @Autowired private val providerReserveAccountsDao: ProviderReserveAccountsDao,
                             @Autowired private val providersDao: ProvidersDao,
                             @Autowired private val accountsSpacesDao: AccountsSpacesDao,
                             @Autowired private val resourceSegmentationsDao: ResourceSegmentationsDao,
                             @Autowired private val resourceSegmentsDao: ResourceSegmentsDao) {

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
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_1.id)
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            result.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, result.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, operationResult.accountsSpaceId.orElseThrow())
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val reserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/providers/{id}/_reserveAccounts", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1, reserveAccountsResult.reserveAccounts.size)
        Assertions.assertEquals(result.result.get().id, reserveAccountsResult.reserveAccounts[0].accountId)
        Assertions.assertEquals(result.result.get().accountsSpaceId.get(),
            reserveAccountsResult.reserveAccounts[0].accountsSpaceId)
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
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_1.id)
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
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
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_1.id)
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
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
    fun testCreateProviderReserveAccountDifferentAccountsSpacesSuccess(): Unit = runBlocking {
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
                .build()).build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setDeleted(false)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setDisplayName("outerDisplayName2")
                .setKey("outerKey2")
                .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey("location")
                            .setResourceSegmentKey("sas")
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
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_1.id)
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            result.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, result.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, operationResult.accountsSpaceId.orElseThrow())
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val anotherRequest = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey2")
            .displayName("outerDisplayName2")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_2.id)
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val anotherResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(anotherRequest)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(anotherResult)
        Assertions.assertTrue(anotherResult.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, anotherResult.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, anotherResult.result.get().providerId)
        Assertions.assertEquals("outerKey2", anotherResult.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName2", anotherResult.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_2.id,
            anotherResult.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", anotherResult.result.get().id)
        Assertions.assertNotEquals("", anotherResult.result.get().externalId)
        Assertions.assertNotEquals("", anotherResult.operationId)
        Assertions.assertFalse(anotherResult.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, anotherResult.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, anotherResult.operationStatus)
        val anotherOperationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", anotherResult.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(anotherOperationResult)
        Assertions.assertEquals(anotherResult.operationId, anotherOperationResult.id)
        Assertions.assertEquals(TestProviders.YP_ID, anotherOperationResult.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_2.id, anotherOperationResult.accountsSpaceId.orElseThrow())
        Assertions.assertEquals(OperationStatusDto.SUCCESS, anotherOperationResult.status)
        Assertions.assertFalse(anotherOperationResult.failure.isPresent)
        val reserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/providers/{id}/_reserveAccounts", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(2, reserveAccountsResult.reserveAccounts.size)
    }

    @Test
    fun testCreateProviderReserveAccountNoAccountsSpaceSuccess(): Unit = runBlocking {
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setKey("outerKey")
            .setDisplayName("outerDisplayName")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setLastUpdate(LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                    .setPassportUid(PassportUID.newBuilder()
                        .setPassportUid(TestUsers.USER_1_UID).build())
                    .setStaffLogin(StaffLogin.newBuilder()
                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                    .build())
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                .build()).build())))
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YDB_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YDB_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertTrue(result.result.get().accountsSpaceId.isEmpty)
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, result.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YDB_ID, operationResult.providerId)
        Assertions.assertTrue(operationResult.accountsSpaceId.isEmpty)
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val reserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/providers/{id}/_reserveAccounts", TestProviders.YDB_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1, reserveAccountsResult.reserveAccounts.size)
        Assertions.assertEquals(result.result.get().id, reserveAccountsResult.reserveAccounts[0].accountId)
    }

    @Test
    fun testCreateProviderReserveAccountConflict(): Unit = runBlocking {
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
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_1.id)
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            result.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, result.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, operationResult.accountsSpaceId.orElseThrow())
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val reserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/providers/{id}/_reserveAccounts", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1, reserveAccountsResult.reserveAccounts.size)
        Assertions.assertEquals(result.result.get().id, reserveAccountsResult.reserveAccounts[0].accountId)
        val conflictResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(conflictResult)
        Assertions.assertEquals("Provider reserve account already exists.", conflictResult.errors.first())
    }

    @Test
    fun testCreateProviderReserveAccountNoConflict(): Unit = runBlocking {
        val provider = providerModel(true, true, true)
        dbSession(tableClient) {
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
        }
        val segmentation = resourceSegmentationModel(provider.id, "cluster")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao
            .upsertResourceSegmentationsRetryable(txSession, listOf(segmentation)).awaitSingleOrNull() }}
        val segment = resourceSegmentModel(segmentation.id, "hahn")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segment)).awaitSingleOrNull() }}
        val accountSpace = accountsSpaceModel(provider.id, "hahn", mapOf(segmentation.id to segment.id))
        dbSessionRetryable(tableClient) { rwTxRetryable { accountsSpacesDao.upsertAllRetryable(txSession,
            listOf(accountSpace)).awaitSingleOrNull() }}
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
                        .setResourceSegmentationKey("cluster")
                        .setResourceSegmentKey("hahn")
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
                .build()).build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setDeleted(false)
                .setFolderId(TestFolders.TEST_FOLDER_2_ID)
                .setDisplayName("outerDisplayNameOne")
                .setKey("outerKeyOne")
                .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey("cluster")
                            .setResourceSegmentKey("hahn")
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
        val request = CreateAccountDto.builder()
            .providerId(provider.id)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(accountSpace.id)
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(provider.id, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertEquals(accountSpace.id, result.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, result.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(provider.id, operationResult.providerId)
        Assertions.assertEquals(accountSpace.id, operationResult.accountsSpaceId.orElseThrow())
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val reserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/providers/{id}/_reserveAccounts", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1, reserveAccountsResult.reserveAccounts.size)
        Assertions.assertEquals(result.result.get().id, reserveAccountsResult.reserveAccounts[0].accountId)
        val requestOne = CreateAccountDto.builder()
            .providerId(provider.id)
            .externalKey("outerKeyOne")
            .displayName("outerDisplayNameOne")
            .accountsSpaceId(accountSpace.id)
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val noConflictResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(requestOne)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(noConflictResult)
        Assertions.assertTrue(noConflictResult.result.isPresent)
    }

    @Test
    fun testCreateProviderReserveAccountNoAccountsSpaceNoConflict(): Unit = runBlocking {
        val provider = providerModel(false, true, true)
        dbSession(tableClient) {
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
        }
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setDisplayName("outerDisplayName")
            .setKey("outerKey")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setLastUpdate(LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                    .setPassportUid(PassportUID.newBuilder()
                        .setPassportUid(TestUsers.USER_1_UID).build())
                    .setStaffLogin(StaffLogin.newBuilder()
                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                    .build())
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                .build()).build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setDeleted(false)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setDisplayName("outerDisplayNameOne")
                .setKey("outerKeyOne")
                .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
                .setLastUpdate(LastUpdate.newBuilder()
                    .setAuthor(UserID.newBuilder()
                        .setPassportUid(PassportUID.newBuilder()
                            .setPassportUid(TestUsers.USER_1_UID).build())
                        .setStaffLogin(StaffLogin.newBuilder()
                            .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                        .build())
                    .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                    .build()).build())))
        val request = CreateAccountDto.builder()
            .providerId(provider.id)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(provider.id, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, result.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(provider.id, operationResult.providerId)
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val reserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/providers/{id}/_reserveAccounts", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1, reserveAccountsResult.reserveAccounts.size)
        Assertions.assertEquals(result.result.get().id, reserveAccountsResult.reserveAccounts[0].accountId)
        val requestOne = CreateAccountDto.builder()
            .providerId(provider.id)
            .externalKey("outerKeyOne")
            .displayName("outerDisplayNameOne")
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val noConflictResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(requestOne)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(noConflictResult)
        Assertions.assertTrue(noConflictResult.result.isPresent)
    }

    @Test
    fun testCreateProviderReserveAccountNoAccountsSpaceDifferentFoldersNoConflict(): Unit = runBlocking {
        val provider = providerModel(false, true, false)
        dbSession(tableClient) {
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
        }
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setDisplayName("outerDisplayName")
            .setKey("outerKey")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setLastUpdate(LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                    .setPassportUid(PassportUID.newBuilder()
                        .setPassportUid(TestUsers.USER_1_UID).build())
                    .setStaffLogin(StaffLogin.newBuilder()
                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                    .build())
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                .build()).build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setDeleted(false)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setDisplayName("outerDisplayNameOne")
                .setKey("outerKeyOne")
                .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
                .setLastUpdate(LastUpdate.newBuilder()
                    .setAuthor(UserID.newBuilder()
                        .setPassportUid(PassportUID.newBuilder()
                            .setPassportUid(TestUsers.USER_1_UID).build())
                        .setStaffLogin(StaffLogin.newBuilder()
                            .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                        .build())
                    .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                    .build()).build())))
        val request = CreateAccountDto.builder()
            .providerId(provider.id)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(provider.id, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, result.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(provider.id, operationResult.providerId)
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val reserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/providers/{id}/_reserveAccounts", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1, reserveAccountsResult.reserveAccounts.size)
        Assertions.assertEquals(result.result.get().id, reserveAccountsResult.reserveAccounts[0].accountId)
        val requestOne = CreateAccountDto.builder()
            .providerId(provider.id)
            .externalKey("outerKeyOne")
            .displayName("outerDisplayNameOne")
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val noConflictResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_2_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(requestOne)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(noConflictResult)
        Assertions.assertTrue(noConflictResult.result.isPresent)
    }

    @Test
    fun testCreateProviderReserveAccountNoAccountsSpaceConflict(): Unit = runBlocking {
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setKey("outerKey")
            .setDisplayName("outerDisplayName")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setLastUpdate(LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                    .setPassportUid(PassportUID.newBuilder()
                        .setPassportUid(TestUsers.USER_1_UID).build())
                    .setStaffLogin(StaffLogin.newBuilder()
                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                    .build())
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                .build()).build())))
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YDB_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YDB_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertTrue(result.result.get().accountsSpaceId.isEmpty)
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, result.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YDB_ID, operationResult.providerId)
        Assertions.assertTrue(operationResult.accountsSpaceId.isEmpty)
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val reserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/providers/{id}/_reserveAccounts", TestProviders.YDB_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1, reserveAccountsResult.reserveAccounts.size)
        Assertions.assertEquals(result.result.get().id, reserveAccountsResult.reserveAccounts[0].accountId)
        val conflictResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(conflictResult)
        Assertions.assertEquals("Provider reserve account already exists.", conflictResult.errors.first())
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
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_1.id)
            .freeTier(false)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            result.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, operationResult.accountsSpaceId.orElseThrow())
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val putRequest = PutAccountDto(AccountReserveTypeInputDto.PROVIDER, result.result.get().version)
        val putResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/api/v1/folders/{folderId}/accounts/{accountId}", TestFolders.TEST_FOLDER_1_ID,
                result.result.get().id)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(putResult)
        Assertions.assertTrue(putResult.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, putResult.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, putResult.result.get().providerId)
        Assertions.assertEquals("outerKey", putResult.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", putResult.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            putResult.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", putResult.result.get().id)
        Assertions.assertNotEquals("", putResult.result.get().externalId)
        Assertions.assertNotEquals("", putResult.operationId)
        Assertions.assertFalse(putResult.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, putResult.result.get().reserveType.get())
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
            .uri("/api/v1/providers/{id}/_reserveAccounts", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1, reserveAccountsResult.reserveAccounts.size)
        Assertions.assertEquals(result.result.get().id, reserveAccountsResult.reserveAccounts[0].accountId)
        Assertions.assertEquals(result.result.get().accountsSpaceId.get(),
            reserveAccountsResult.reserveAccounts[0].accountsSpaceId)
        val opLogs = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID,
                    TestFolders.TEST_FOLDER_1_ID, null, SortOrderDto.DESC, 100)
            }
        }.awaitSingle()!!
        Assertions.assertEquals(AccountReserveType.PROVIDER,
            opLogs[0].newAccounts.get().accounts[result.result.get().id]!!.reserveType.get())
    }

    @Test
    fun testPutProviderReserveAccountNoAccountsSpaceSetSuccess(): Unit = runBlocking {
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setKey("outerKey")
            .setDisplayName("outerDisplayName")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setLastUpdate(LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                    .setPassportUid(PassportUID.newBuilder()
                        .setPassportUid(TestUsers.USER_1_UID).build())
                    .setStaffLogin(StaffLogin.newBuilder()
                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                    .build())
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                .build()).build())))
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YDB_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .freeTier(false)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YDB_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertTrue(result.result.get().accountsSpaceId.isEmpty)
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YDB_ID, operationResult.providerId)
        Assertions.assertTrue(operationResult.accountsSpaceId.isEmpty)
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val putRequest = PutAccountDto(AccountReserveTypeInputDto.PROVIDER, result.result.get().version)
        val putResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/api/v1/folders/{folderId}/accounts/{accountId}", TestFolders.TEST_FOLDER_1_ID,
                result.result.get().id)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(putResult)
        Assertions.assertTrue(putResult.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, putResult.result.get().folderId)
        Assertions.assertEquals(TestProviders.YDB_ID, putResult.result.get().providerId)
        Assertions.assertEquals("outerKey", putResult.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", putResult.result.get().displayName.orElseThrow())
        Assertions.assertNotEquals("", putResult.result.get().id)
        Assertions.assertNotEquals("", putResult.result.get().externalId)
        Assertions.assertNotEquals("", putResult.operationId)
        Assertions.assertFalse(putResult.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, putResult.result.get().reserveType.get())
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
        Assertions.assertEquals(TestProviders.YDB_ID, putOperationResult.providerId)
        Assertions.assertEquals(OperationStatusDto.SUCCESS, putOperationResult.status)
        Assertions.assertFalse(putOperationResult.failure.isPresent)
        val reserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/providers/{id}/_reserveAccounts", TestProviders.YDB_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1, reserveAccountsResult.reserveAccounts.size)
        Assertions.assertEquals(result.result.get().id, reserveAccountsResult.reserveAccounts[0].accountId)
        val opLogs = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID,
                    TestFolders.TEST_FOLDER_1_ID, null, SortOrderDto.DESC, 100)
            }
        }.awaitSingle()!!
        Assertions.assertEquals(AccountReserveType.PROVIDER,
            opLogs[0].newAccounts.get().accounts[result.result.get().id]!!.reserveType.get())
    }

    @Test
    fun testPutProviderReserveAccountUnsetSuccess(): Unit = runBlocking {
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
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_1.id)
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            result.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, result.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, operationResult.accountsSpaceId.orElseThrow())
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val putRequest = PutAccountDto(null, result.result.get().version)
        val putResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/api/v1/folders/{folderId}/accounts/{accountId}", TestFolders.TEST_FOLDER_1_ID,
                result.result.get().id)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(putResult)
        Assertions.assertTrue(putResult.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, putResult.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, putResult.result.get().providerId)
        Assertions.assertEquals("outerKey", putResult.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", putResult.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            putResult.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", putResult.result.get().id)
        Assertions.assertNotEquals("", putResult.result.get().externalId)
        Assertions.assertNotEquals("", putResult.operationId)
        Assertions.assertFalse(putResult.result.get().isFreeTier)
        Assertions.assertTrue(putResult.result.get().reserveType.isEmpty)
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
            .uri("/api/v1/providers/{id}/_reserveAccounts", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(0, reserveAccountsResult.reserveAccounts.size)
        val opLogs = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID,
                    TestFolders.TEST_FOLDER_1_ID, null, SortOrderDto.DESC, 100)
            }
        }.awaitSingle()!!
        Assertions.assertEquals(AccountReserveType.PROVIDER,
            opLogs[0].oldAccounts.get().accounts[result.result.get().id]!!.reserveType.get())
        Assertions.assertTrue(opLogs[0].newAccounts.get().accounts[result.result.get().id]!!.reserveType.isEmpty)
    }

    @Test
    fun testPutProviderReserveAccountNoAccountsSpaceUnsetSuccess(): Unit = runBlocking {
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setKey("outerKey")
            .setDisplayName("outerDisplayName")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setLastUpdate(LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                    .setPassportUid(PassportUID.newBuilder()
                        .setPassportUid(TestUsers.USER_1_UID).build())
                    .setStaffLogin(StaffLogin.newBuilder()
                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                    .build())
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                .build()).build())))
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YDB_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YDB_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertTrue(result.result.get().accountsSpaceId.isEmpty)
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, result.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YDB_ID, operationResult.providerId)
        Assertions.assertTrue(operationResult.accountsSpaceId.isEmpty)
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val putRequest = PutAccountDto(null, result.result.get().version)
        val putResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/api/v1/folders/{folderId}/accounts/{accountId}", TestFolders.TEST_FOLDER_1_ID,
                result.result.get().id)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(putResult)
        Assertions.assertTrue(putResult.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, putResult.result.get().folderId)
        Assertions.assertEquals(TestProviders.YDB_ID, putResult.result.get().providerId)
        Assertions.assertEquals("outerKey", putResult.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", putResult.result.get().displayName.orElseThrow())
        Assertions.assertNotEquals("", putResult.result.get().id)
        Assertions.assertNotEquals("", putResult.result.get().externalId)
        Assertions.assertNotEquals("", putResult.operationId)
        Assertions.assertFalse(putResult.result.get().isFreeTier)
        Assertions.assertTrue(putResult.result.get().reserveType.isEmpty)
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
        Assertions.assertEquals(TestProviders.YDB_ID, putOperationResult.providerId)
        Assertions.assertEquals(OperationStatusDto.SUCCESS, putOperationResult.status)
        Assertions.assertFalse(putOperationResult.failure.isPresent)
        val reserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/providers/{id}/_reserveAccounts", TestProviders.YDB_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(0, reserveAccountsResult.reserveAccounts.size)
        val opLogs = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID,
                    TestFolders.TEST_FOLDER_1_ID, null, SortOrderDto.DESC, 100)
            }
        }.awaitSingle()!!
        Assertions.assertEquals(AccountReserveType.PROVIDER,
            opLogs[0].oldAccounts.get().accounts[result.result.get().id]!!.reserveType.get())
        Assertions.assertTrue(opLogs[0].newAccounts.get().accounts[result.result.get().id]!!.reserveType.isEmpty)
    }

    @Test
    fun testPutProviderReserveAccountSetConflict(): Unit = runBlocking {
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
                .build()).build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setDeleted(false)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setDisplayName("outerDisplayName2")
                .setKey("outerKey2")
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
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_1.id)
            .freeTier(false)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            result.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, operationResult.accountsSpaceId.orElseThrow())
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val anotherRequest = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey2")
            .displayName("outerDisplayName2")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_1.id)
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val anotherResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(anotherRequest)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(anotherResult)
        Assertions.assertTrue(anotherResult.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, anotherResult.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, anotherResult.result.get().providerId)
        Assertions.assertEquals("outerKey2", anotherResult.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName2", anotherResult.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            anotherResult.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", anotherResult.result.get().id)
        Assertions.assertNotEquals("", anotherResult.result.get().externalId)
        Assertions.assertNotEquals("", anotherResult.operationId)
        Assertions.assertFalse(anotherResult.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, anotherResult.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, anotherResult.operationStatus)
        val anotherOperationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", anotherResult.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(anotherOperationResult)
        Assertions.assertEquals(anotherResult.operationId, anotherOperationResult.id)
        Assertions.assertEquals(TestProviders.YP_ID, anotherOperationResult.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, anotherOperationResult.accountsSpaceId.orElseThrow())
        Assertions.assertEquals(OperationStatusDto.SUCCESS, anotherOperationResult.status)
        Assertions.assertFalse(anotherOperationResult.failure.isPresent)
        val putRequest = PutAccountDto(AccountReserveTypeInputDto.PROVIDER, result.result.get().version)
        val putResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/api/v1/folders/{folderId}/accounts/{accountId}", TestFolders.TEST_FOLDER_1_ID,
                result.result.get().id)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(putResult)
        Assertions.assertEquals("Provider reserve account already exists.", putResult.errors.first())
    }

    @Test
    fun testPutProviderReserveAccountNoAccountsSpaceSetConflict(): Unit = runBlocking {
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setKey("outerKey")
            .setDisplayName("outerDisplayName")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setLastUpdate(LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                    .setPassportUid(PassportUID.newBuilder()
                        .setPassportUid(TestUsers.USER_1_UID).build())
                    .setStaffLogin(StaffLogin.newBuilder()
                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                    .build())
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                .build()).build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setDeleted(false)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setKey("outerKey2")
                .setDisplayName("outerDisplayName2")
                .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
                .setLastUpdate(LastUpdate.newBuilder()
                    .setAuthor(UserID.newBuilder()
                        .setPassportUid(PassportUID.newBuilder()
                            .setPassportUid(TestUsers.USER_1_UID).build())
                        .setStaffLogin(StaffLogin.newBuilder()
                            .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                        .build())
                    .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                    .build()).build())))
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YDB_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .freeTier(false)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YDB_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertTrue(result.result.get().accountsSpaceId.isEmpty)
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YDB_ID, operationResult.providerId)
        Assertions.assertTrue(operationResult.accountsSpaceId.isEmpty)
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val anotherRequest = CreateAccountDto.builder()
            .providerId(TestProviders.YDB_ID)
            .externalKey("outerKey2")
            .displayName("outerDisplayName2")
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val anotherResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(anotherRequest)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(anotherResult)
        Assertions.assertTrue(anotherResult.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, anotherResult.result.get().folderId)
        Assertions.assertEquals(TestProviders.YDB_ID, anotherResult.result.get().providerId)
        Assertions.assertEquals("outerKey2", anotherResult.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName2", anotherResult.result.get().displayName.orElseThrow())
        Assertions.assertTrue(anotherResult.result.get().accountsSpaceId.isEmpty)
        Assertions.assertNotEquals("", anotherResult.result.get().id)
        Assertions.assertNotEquals("", anotherResult.result.get().externalId)
        Assertions.assertNotEquals("", anotherResult.operationId)
        Assertions.assertFalse(anotherResult.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, anotherResult.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, anotherResult.operationStatus)
        val anotherOperationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", anotherResult.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(anotherOperationResult)
        Assertions.assertEquals(anotherResult.operationId, anotherOperationResult.id)
        Assertions.assertEquals(TestProviders.YDB_ID, anotherOperationResult.providerId)
        Assertions.assertTrue(anotherOperationResult.accountsSpaceId.isEmpty)
        Assertions.assertEquals(OperationStatusDto.SUCCESS, anotherOperationResult.status)
        Assertions.assertFalse(anotherOperationResult.failure.isPresent)
        val putRequest = PutAccountDto(AccountReserveTypeInputDto.PROVIDER, result.result.get().version)
        val putResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/api/v1/folders/{folderId}/accounts/{accountId}", TestFolders.TEST_FOLDER_1_ID,
                result.result.get().id)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(putResult)
        Assertions.assertEquals("Provider reserve account already exists.", putResult.errors.first())
    }

    @Test
    fun testPutProviderReserveAccountUnchangedSuccess(): Unit = runBlocking {
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
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_1.id)
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            result.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, result.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, operationResult.accountsSpaceId.orElseThrow())
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val putRequest = PutAccountDto(AccountReserveTypeInputDto.PROVIDER, result.result.get().version)
        val putResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/api/v1/folders/{folderId}/accounts/{accountId}", TestFolders.TEST_FOLDER_1_ID,
                result.result.get().id)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(putResult)
        Assertions.assertTrue(putResult.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, putResult.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, putResult.result.get().providerId)
        Assertions.assertEquals("outerKey", putResult.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", putResult.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            putResult.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", putResult.result.get().id)
        Assertions.assertNotEquals("", putResult.result.get().externalId)
        Assertions.assertNotEquals("", putResult.operationId)
        Assertions.assertFalse(putResult.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, putResult.result.get().reserveType.get())
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
            .uri("/api/v1/providers/{id}/_reserveAccounts", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1, reserveAccountsResult.reserveAccounts.size)
        val opLogs = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID,
                    TestFolders.TEST_FOLDER_1_ID, null, SortOrderDto.DESC, 100)
            }
        }.awaitSingle()!!
        Assertions.assertEquals(AccountReserveType.PROVIDER,
            opLogs[0].oldAccounts.get().accounts[result.result.get().id]!!.reserveType.get())
        Assertions.assertEquals(AccountReserveType.PROVIDER,
            opLogs[0].newAccounts.get().accounts[result.result.get().id]!!.reserveType.get())
    }

    @Test
    fun testPutProviderReserveAccountNoAccountsSpaceUnchangedSuccess(): Unit = runBlocking {
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setKey("outerKey")
            .setDisplayName("outerDisplayName")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setLastUpdate(LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                    .setPassportUid(PassportUID.newBuilder()
                        .setPassportUid(TestUsers.USER_1_UID).build())
                    .setStaffLogin(StaffLogin.newBuilder()
                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                    .build())
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                .build()).build())))
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YDB_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YDB_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertTrue(result.result.get().accountsSpaceId.isEmpty)
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, result.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YDB_ID, operationResult.providerId)
        Assertions.assertTrue(operationResult.accountsSpaceId.isEmpty)
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val putRequest = PutAccountDto(AccountReserveTypeInputDto.PROVIDER, result.result.get().version)
        val putResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/api/v1/folders/{folderId}/accounts/{accountId}", TestFolders.TEST_FOLDER_1_ID,
                result.result.get().id)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(putResult)
        Assertions.assertTrue(putResult.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, putResult.result.get().folderId)
        Assertions.assertEquals(TestProviders.YDB_ID, putResult.result.get().providerId)
        Assertions.assertEquals("outerKey", putResult.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", putResult.result.get().displayName.orElseThrow())
        Assertions.assertNotEquals("", putResult.result.get().id)
        Assertions.assertNotEquals("", putResult.result.get().externalId)
        Assertions.assertNotEquals("", putResult.operationId)
        Assertions.assertFalse(putResult.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, putResult.result.get().reserveType.get())
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
        Assertions.assertEquals(TestProviders.YDB_ID, putOperationResult.providerId)
        Assertions.assertEquals(OperationStatusDto.SUCCESS, putOperationResult.status)
        Assertions.assertFalse(putOperationResult.failure.isPresent)
        val reserveAccountsResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/providers/{id}/_reserveAccounts", TestProviders.YDB_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1, reserveAccountsResult.reserveAccounts.size)
        val opLogs = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID,
                    TestFolders.TEST_FOLDER_1_ID, null, SortOrderDto.DESC, 100)
            }
        }.awaitSingle()!!
        Assertions.assertEquals(AccountReserveType.PROVIDER,
            opLogs[0].oldAccounts.get().accounts[result.result.get().id]!!.reserveType.get())
        Assertions.assertEquals(AccountReserveType.PROVIDER,
            opLogs[0].newAccounts.get().accounts[result.result.get().id]!!.reserveType.get())
    }

    @Test
    fun testPutProviderReserveAccountSetVersionMismatch(): Unit = runBlocking {
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
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_1.id)
            .freeTier(false)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            result.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, operationResult.accountsSpaceId.orElseThrow())
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val putRequest = PutAccountDto(AccountReserveTypeInputDto.PROVIDER, result.result.get().version + 1)
        val putResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/api/v1/folders/{folderId}/accounts/{accountId}", TestFolders.TEST_FOLDER_1_ID,
                result.result.get().id)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.PRECONDITION_FAILED)
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(putResult)
    }

    @Test
    fun testPutProviderReserveAccountSetIdempotencySuccess(): Unit = runBlocking {
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
        val request = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_1.id)
            .freeTier(false)
            .build()
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, result.result.get().providerId)
        Assertions.assertEquals("outerKey", result.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", result.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            result.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", result.result.get().id)
        Assertions.assertNotEquals("", result.result.get().externalId)
        Assertions.assertNotEquals("", result.operationId)
        Assertions.assertFalse(result.result.get().isFreeTier)
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.operationStatus)
        val operationResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/api/v1/operations/{id}", result.operationId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(operationResult)
        Assertions.assertEquals(result.operationId, operationResult.id)
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, operationResult.accountsSpaceId.orElseThrow())
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.status)
        Assertions.assertFalse(operationResult.failure.isPresent)
        val putRequest = PutAccountDto(AccountReserveTypeInputDto.PROVIDER, result.result.get().version)
        val idempotencyKey = UUID.randomUUID().toString()
        val putResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/api/v1/folders/{folderId}/accounts/{accountId}", TestFolders.TEST_FOLDER_1_ID,
                result.result.get().id)
            .accept(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", idempotencyKey)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(putResult)
        Assertions.assertTrue(putResult.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, putResult.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, putResult.result.get().providerId)
        Assertions.assertEquals("outerKey", putResult.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", putResult.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            putResult.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", putResult.result.get().id)
        Assertions.assertNotEquals("", putResult.result.get().externalId)
        Assertions.assertNotEquals("", putResult.operationId)
        Assertions.assertFalse(putResult.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, putResult.result.get().reserveType.get())
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
            .uri("/api/v1/providers/{id}/_reserveAccounts", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ProviderReserveAccountsDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(1, reserveAccountsResult.reserveAccounts.size)
        Assertions.assertEquals(result.result.get().id, reserveAccountsResult.reserveAccounts[0].accountId)
        Assertions.assertEquals(result.result.get().accountsSpaceId.get(),
            reserveAccountsResult.reserveAccounts[0].accountsSpaceId)
        val opLogs = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID,
                    TestFolders.TEST_FOLDER_1_ID, null, SortOrderDto.DESC, 100)
            }
        }.awaitSingle()!!
        Assertions.assertEquals(AccountReserveType.PROVIDER,
            opLogs[0].newAccounts.get().accounts[result.result.get().id]!!.reserveType.get())
        val putIdempotencyResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/api/v1/folders/{folderId}/accounts/{accountId}", TestFolders.TEST_FOLDER_1_ID,
                result.result.get().id)
            .accept(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", idempotencyKey)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(putIdempotencyResult)
        Assertions.assertTrue(putIdempotencyResult.result.isPresent)
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, putIdempotencyResult.result.get().folderId)
        Assertions.assertEquals(TestProviders.YP_ID, putIdempotencyResult.result.get().providerId)
        Assertions.assertEquals("outerKey", putIdempotencyResult.result.get().externalKey.orElseThrow())
        Assertions.assertEquals("outerDisplayName", putIdempotencyResult.result.get().displayName.orElseThrow())
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id,
            putIdempotencyResult.result.get().accountsSpaceId.orElseThrow())
        Assertions.assertNotEquals("", putIdempotencyResult.result.get().id)
        Assertions.assertNotEquals("", putIdempotencyResult.result.get().externalId)
        Assertions.assertNotEquals("", putIdempotencyResult.operationId)
        Assertions.assertFalse(putIdempotencyResult.result.get().isFreeTier)
        Assertions.assertEquals(AccountReserveTypeDto.PROVIDER, putIdempotencyResult.result.get().reserveType.get())
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, putIdempotencyResult.operationStatus)
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
            rwTxRetryable { providerReserveAccountsDao
                .upsertManyRetryable(txSession, listOf(firstReserveAccountModel))
            }
        }
        val request = PutAccountDto(AccountReserveTypeInputDto.PROVIDER, secondAccount.version)
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/api/v1/folders/{folderId}/accounts/{accountId}", TestFolders.TEST_FOLDER_10_ID, secondAccount.id)
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
            rwTxRetryable { providerReserveAccountsDao
                .upsertManyRetryable(txSession, listOf(firstReserveAccountModel))
            }
        }
        val request = PutAccountDto(AccountReserveTypeInputDto.PROVIDER, secondAccount.version)
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/api/v1/folders/{folderId}/accounts/{accountId}", TestFolders.TEST_FOLDER_2_ID, secondAccount.id)
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

    private fun providerModel(accountsSpacesSupported: Boolean, allowConflicts: Boolean,
                              multipleAccountsPerFolder: Boolean): ProviderModel {
        return ProviderModel.builder()
            .id(UUID.randomUUID().toString())
            .grpcApiUri("in-process:test")
            .restApiUri(null)
            .destinationTvmId(42L)
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .version(0L)
            .nameEn("Test")
            .nameRu("Test")
            .descriptionEn("Test")
            .descriptionRu("Test")
            .sourceTvmId(42L)
            .serviceId(69L)
            .deleted(false)
            .readOnly(false)
            .multipleAccountsPerFolder(multipleAccountsPerFolder)
            .accountTransferWithQuota(true)
            .managed(true)
            .key("test")
            .trackerComponentId(1L)
            .accountsSettings(
                AccountsSettingsModel.builder()
                    .displayNameSupported(true)
                    .keySupported(true)
                    .deleteSupported(true)
                    .softDeleteSupported(false)
                    .moveSupported(true)
                    .renameSupported(true)
                    .perAccountVersionSupported(true)
                    .perProvisionVersionSupported(true)
                    .perAccountLastUpdateSupported(true)
                    .perProvisionLastUpdateSupported(true)
                    .operationIdDeduplicationSupported(true)
                    .syncCoolDownDisabled(false)
                    .retryCoolDownDisabled(false)
                    .accountsSyncPageSize(1000L)
                    .multipleReservesAllowed(allowConflicts)
                    .build()
            )
            .importAllowed(true)
            .accountsSpacesSupported(accountsSpacesSupported)
            .syncEnabled(true)
            .grpcTlsOn(true)
            .aggregationSettings(AggregationSettings(FreeProvisionAggregationMode.NONE, UsageMode.UNDEFINED, null))
            .aggregationAlgorithm(null)
            .build()
    }

    fun resourceSegmentationModel(providerId: String, key: String): ResourceSegmentationModel {
        return ResourceSegmentationModel.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .providerId(providerId)
            .version(0L)
            .key(key)
            .nameEn("Test")
            .nameRu("Test")
            .descriptionEn("Test")
            .descriptionRu("Test")
            .deleted(false)
            .build()
    }

    fun resourceSegmentModel(segmentationId: String, key: String): ResourceSegmentModel {
        return ResourceSegmentModel.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .segmentationId(segmentationId)
            .version(0L)
            .key(key)
            .nameEn("Test")
            .nameRu("Test")
            .descriptionEn("Test")
            .descriptionRu("Test")
            .deleted(false)
            .build()
    }

    fun accountsSpaceModel(providerId: String, key: String, segments: Map<String, String>): AccountSpaceModel {
        return AccountSpaceModel.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setDeleted(false)
            .setNameEn("Test")
            .setNameRu("Test")
            .setDescriptionEn("Test")
            .setDescriptionRu("Test")
            .setProviderId(providerId)
            .setOuterKeyInProvider(key)
            .setSegments(segments.map { (k ,v) -> ResourceSegmentSettingsModel(k, v) }.toSet())
            .setVersion(0L)
            .setReadOnly(false)
            .build()
    }

}
