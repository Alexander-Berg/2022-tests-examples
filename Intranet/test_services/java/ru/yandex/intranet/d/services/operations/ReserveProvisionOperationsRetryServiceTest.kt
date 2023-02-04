package ru.yandex.intranet.d.services.operations

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
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
import ru.yandex.intranet.d.backend.service.provider_proto.Amount
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundAccountsSpaceKey
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundResourceKey
import ru.yandex.intranet.d.backend.service.provider_proto.CurrentVersion
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate
import ru.yandex.intranet.d.backend.service.provider_proto.PassportUID
import ru.yandex.intranet.d.backend.service.provider_proto.Provision
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceKey
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey
import ru.yandex.intranet.d.backend.service.provider_proto.StaffLogin
import ru.yandex.intranet.d.backend.service.provider_proto.UserID
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao
import ru.yandex.intranet.d.dao.accounts.OperationsInProgressDao
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.i18n.Locales
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel
import ru.yandex.intranet.d.model.accounts.OperationChangesModel
import ru.yandex.intranet.d.model.accounts.OperationInProgressModel
import ru.yandex.intranet.d.model.accounts.OperationOrdersModel
import ru.yandex.intranet.d.model.accounts.OperationSource
import ru.yandex.intranet.d.model.folders.FolderOperationType
import ru.yandex.intranet.d.model.folders.OperationPhase
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.SortOrderDto
import ru.yandex.intranet.d.web.model.accounts.AccountOperationDto
import ru.yandex.intranet.d.web.model.accounts.AccountReserveTypeInputDto
import ru.yandex.intranet.d.web.model.accounts.CreateAccountDto
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Reserve provision operations retry test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class ReserveProvisionOperationsRetryServiceTest(
    @Autowired private val webClient: WebTestClient,
    @Autowired private val stubProviderService: StubProviderService,
    @Autowired private val quotasDao: QuotasDao,
    @Autowired private val accountsQuotasDao: AccountsQuotasDao,
    @Autowired private val accountsQuotasOperationsDao: AccountsQuotasOperationsDao,
    @Autowired private val folderOperationLogDao: FolderOperationLogDao,
    @Autowired private val operationsInProgressDao: OperationsInProgressDao,
    @Autowired private val operationsRetryService: OperationsRetryService,
    @Autowired private val tableClient: YdbTableClient
) {

    @Test
    fun testSuccessfulCompletion(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
        val externalAccountId = UUID.randomUUID().toString()
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(externalAccountId)
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setDisplayName("outerDisplayName")
            .setKey("outerKey")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                    .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                        .setResourceSegmentationKey("location")
                        .setResourceSegmentKey("man")
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
                .setTimestamp(Timestamp.newBuilder().setSeconds(now.epochSecond).build())
                .build()).build())))
        stubProviderService.setGetAccountResponses(listOf(GrpcResponse.success(Account
            .newBuilder()
            .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                    .addAllResourceSegmentKeys(listOf(
                        ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey("location")
                            .setResourceSegmentKey("man")
                            .build(),
                        ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey("segment")
                            .setResourceSegmentKey("default")
                            .build()
                    ))
                    .build())
                .build())
            .setAccountId(externalAccountId)
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setDisplayName("outerDisplayName")
            .setKey("outerKey")
            .setFreeTier(false)
            .addProvisions(Provision.newBuilder()
                .setResourceKey(ResourceKey.newBuilder()
                    .setCompoundKey(CompoundResourceKey.newBuilder()
                        .setResourceTypeKey("hdd")
                        .build())
                    .build())
                .setProvided(Amount.newBuilder()
                    .setValue(100)
                    .setUnitKey("gigabytes")
                    .build())
                .setAllocated(Amount.newBuilder()
                    .setValue(50)
                    .setUnitKey("gigabytes")
                    .build())
                .setLastUpdate(LastUpdate.newBuilder()
                    .setAuthor(UserID.newBuilder()
                        .setPassportUid(PassportUID.newBuilder()
                            .setPassportUid(TestUsers.USER_3_UID)
                            .build())
                        .setStaffLogin(StaffLogin.newBuilder()
                            .setStaffLogin(TestUsers.USER_3_LOGIN)
                            .build())
                        .build())
                    .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                    .build())
                .build())
            .addProvisions(Provision.newBuilder()
                .setResourceKey(ResourceKey.newBuilder()
                    .setCompoundKey(CompoundResourceKey.newBuilder()
                        .setResourceTypeKey("ssd")
                        .build())
                    .build())
                .setProvided(Amount.newBuilder()
                    .setValue(10)
                    .setUnitKey("gigabytes")
                    .build())
                .setAllocated(Amount.newBuilder()
                    .setValue(0)
                    .setUnitKey("gigabytes")
                    .build())
                .setLastUpdate(LastUpdate.newBuilder()
                    .setAuthor(UserID.newBuilder()
                        .setPassportUid(PassportUID.newBuilder()
                            .setPassportUid(TestUsers.USER_3_UID)
                            .build())
                        .setStaffLogin(StaffLogin.newBuilder()
                            .setStaffLogin(TestUsers.USER_3_LOGIN)
                            .build())
                        .build())
                    .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                    .build())
                .build())
            .addProvisions(Provision.newBuilder()
                .setResourceKey(ResourceKey.newBuilder()
                    .setCompoundKey(CompoundResourceKey.newBuilder()
                        .setResourceTypeKey("cpu")
                        .build())
                    .build())
                .setProvided(Amount.newBuilder()
                    .setValue(0)
                    .setUnitKey("millicores")
                    .build())
                .setAllocated(Amount.newBuilder()
                    .setValue(0)
                    .setUnitKey("millicores")
                    .build())
                .setLastUpdate(LastUpdate.newBuilder()
                    .setAuthor(UserID.newBuilder()
                        .setPassportUid(PassportUID.newBuilder()
                            .setPassportUid(TestUsers.USER_3_UID)
                            .build())
                        .setStaffLogin(StaffLogin.newBuilder()
                            .setStaffLogin(TestUsers.USER_3_LOGIN)
                            .build())
                        .build())
                    .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                    .build())
                .build())
            .build())))
        val createAccountRequest = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_3_ID)
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val createAccountResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createAccountRequest)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(createAccountResult)
        Assertions.assertTrue(createAccountResult.result.isPresent)
        val accountId = createAccountResult.result.get().id
        val operationChanges = OperationChangesModel.builder()
            .accountId(accountId)
            .updatedProvisions(listOf(OperationChangesModel.Provision(TestResources.YP_HDD_MAN, 100_000_000_000L),
                OperationChangesModel.Provision(TestResources.YP_SSD_MAN, 10_000_000_000L)))
            .frozenProvisions(listOf(OperationChangesModel.Provision(TestResources.YP_HDD_MAN, 100_000_000_000L),
                OperationChangesModel.Provision(TestResources.YP_SSD_MAN, 10_000_000_000L)))
            .build()
        val operation = AccountsQuotasOperationsModel.builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setOperationId(UUID.randomUUID().toString())
            .setLastRequestId(UUID.randomUUID().toString())
            .setCreateDateTime(now)
            .setOperationSource(OperationSource.USER)
            .setOperationType(AccountsQuotasOperationsModel.OperationType.PROVIDE_RESERVE)
            .setAuthorUserId(TestUsers.USER_1_ID)
            .setAuthorUserUid(TestUsers.USER_1_UID)
            .setProviderId(TestProviders.YP_ID)
            .setAccountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_3_ID)
            .setUpdateDateTime(null)
            .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.WAITING)
            .setErrorMessage(null)
            .setFullErrorMessage(null)
            .setRequestedChanges(operationChanges)
            .setOrders(OperationOrdersModel.builder()
                .submitOrder(0L)
                .build())
            .setErrorKind(null)
            .setLogs(emptyList())
            .build()
        val operationInProgress = OperationInProgressModel(Tenants.DEFAULT_TENANT_ID, operation.operationId,
            TestFolders.TEST_FOLDER_1_ID, accountId, 0L)
        dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.upsertOneRetryable(rwSingleRetryableCommit(), operation).awaitSingle()
            operationsInProgressDao.upsertOneRetryable(rwSingleRetryableCommit(), operationInProgress).awaitSingle()
        }
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val quotaBeforeOne = quotasBefore[TestResources.YP_HDD_MAN]!!
        val quotaBeforeTwo = quotasBefore[TestResources.YP_SSD_MAN]!!
        val updatedQuotaOne = QuotaModel.builder(quotaBeforeOne)
            .quota(quotaBeforeOne.quota + 100_000_000_000L)
            .frozenQuota(quotaBeforeOne.frozenQuota + 100_000_000_000L)
            .build()
        val updatedQuotaTwo = QuotaModel.builder(quotaBeforeTwo)
            .quota(quotaBeforeTwo.quota + 10_000_000_000L)
            .frozenQuota(quotaBeforeTwo.frozenQuota + 10_000_000_000L)
            .build()
        dbSessionRetryable(tableClient) {
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(updatedQuotaOne, updatedQuotaTwo))
                .awaitSingleOrNull()
        }
        operationsRetryService.retryOperations(Clock.fixed(now.plus(1, ChronoUnit.HOURS), ZoneOffset.UTC),
            Locales.ENGLISH).awaitSingleOrNull()
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val provisionsAfter = dbSessionRetryable(tableClient) {
            accountsQuotasDao.getAllByAccountIds(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(accountId)).awaitSingle()
        }!!.groupBy { it.accountId }.mapValues { e -> e.value.associateBy { it.resourceId } }
        Assertions.assertEquals(100_000_000_000L,
            quotasAfter[TestResources.YP_HDD_MAN]!!.quota - quotasBefore[TestResources.YP_HDD_MAN]!!.quota)
        Assertions.assertEquals(10_000_000_000L,
            quotasAfter[TestResources.YP_SSD_MAN]!!.quota - quotasBefore[TestResources.YP_SSD_MAN]!!.quota)
        Assertions.assertEquals(0L, quotasAfter[TestResources.YP_HDD_MAN]!!.frozenQuota)
        Assertions.assertEquals(0L, quotasAfter[TestResources.YP_SSD_MAN]!!.frozenQuota)
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_HDD_MAN]!!.balance - quotasBefore[TestResources.YP_HDD_MAN]!!.balance)
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_SSD_MAN]!!.balance - quotasBefore[TestResources.YP_SSD_MAN]!!.balance)
        Assertions.assertEquals(100_000_000_000L, provisionsAfter[accountId]!![TestResources.YP_HDD_MAN]!!.providedQuota)
        Assertions.assertEquals(10_000_000_000L, provisionsAfter[accountId]!![TestResources.YP_SSD_MAN]!!.providedQuota)
        Assertions.assertEquals(50_000_000_000L, provisionsAfter[accountId]!![TestResources.YP_HDD_MAN]!!.allocatedQuota)
        Assertions.assertEquals(0L, provisionsAfter[accountId]!![TestResources.YP_SSD_MAN]!!.allocatedQuota)
        val updatedOperation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operation.operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.requestStatus.get())
        Assertions.assertEquals(AccountsQuotasOperationsModel.OperationType.PROVIDE_RESERVE, updatedOperation.operationType)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 1).awaitSingle()
        }!!
        Assertions.assertEquals(1, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertTrue(opLogs[0].oldQuotas.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].newQuotas.asMap().isEmpty())
        Assertions.assertEquals(0L, opLogs[0].oldProvisions[accountId]!![TestResources.YP_HDD_MAN]!!.provision)
        Assertions.assertEquals(0L, opLogs[0].oldProvisions[accountId]!![TestResources.YP_SSD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YP_HDD_MAN]!!.providedQuota,
            opLogs[0].newProvisions[accountId]!![TestResources.YP_HDD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YP_SSD_MAN]!!.providedQuota,
            opLogs[0].newProvisions[accountId]!![TestResources.YP_SSD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YP_HDD_MAN]!!.providedQuota,
            opLogs[0].actuallyAppliedProvisions.get()[accountId]!![TestResources.YP_HDD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YP_SSD_MAN]!!.providedQuota,
            opLogs[0].actuallyAppliedProvisions.get()[accountId]!![TestResources.YP_SSD_MAN]!!.provision)
    }

    @Test
    fun testRollback(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
        val externalAccountId = UUID.randomUUID().toString()
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(externalAccountId)
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setDisplayName("outerDisplayName")
            .setKey("outerKey")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                    .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                        .setResourceSegmentationKey("location")
                        .setResourceSegmentKey("man")
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
                .setTimestamp(Timestamp.newBuilder().setSeconds(now.epochSecond).build())
                .build()).build())))
        stubProviderService.setGetAccountResponses(listOf(GrpcResponse.success(Account
            .newBuilder()
            .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                    .addAllResourceSegmentKeys(listOf(
                        ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey("location")
                            .setResourceSegmentKey("man")
                            .build(),
                        ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey("segment")
                            .setResourceSegmentKey("default")
                            .build()
                    ))
                    .build())
                .build())
            .setAccountId(externalAccountId)
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setDisplayName("outerDisplayName")
            .setKey("outerKey")
            .setFreeTier(false)
            .build())))
        val createAccountRequest = CreateAccountDto.builder()
            .providerId(TestProviders.YP_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .accountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_3_ID)
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val createAccountResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createAccountRequest)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(AccountOperationDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertNotNull(createAccountResult)
        Assertions.assertTrue(createAccountResult.result.isPresent)
        val accountId = createAccountResult.result.get().id
        val operationChanges = OperationChangesModel.builder()
            .accountId(accountId)
            .updatedProvisions(listOf(OperationChangesModel.Provision(TestResources.YP_HDD_MAN, 100_000_000_000L),
                OperationChangesModel.Provision(TestResources.YP_SSD_MAN, 10_000_000_000L)))
            .frozenProvisions(listOf(OperationChangesModel.Provision(TestResources.YP_HDD_MAN, 100_000_000_000L),
                OperationChangesModel.Provision(TestResources.YP_SSD_MAN, 10_000_000_000L)))
            .build()
        val operation = AccountsQuotasOperationsModel.builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setOperationId(UUID.randomUUID().toString())
            .setLastRequestId(UUID.randomUUID().toString())
            .setCreateDateTime(now)
            .setOperationSource(OperationSource.USER)
            .setOperationType(AccountsQuotasOperationsModel.OperationType.PROVIDE_RESERVE)
            .setAuthorUserId(TestUsers.USER_1_ID)
            .setAuthorUserUid(TestUsers.USER_1_UID)
            .setProviderId(TestProviders.YP_ID)
            .setAccountsSpaceId(TestAccounts.TEST_ACCOUNT_SPACE_3_ID)
            .setUpdateDateTime(null)
            .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.WAITING)
            .setErrorMessage(null)
            .setFullErrorMessage(null)
            .setRequestedChanges(operationChanges)
            .setOrders(OperationOrdersModel.builder()
                .submitOrder(0L)
                .build())
            .setErrorKind(null)
            .setLogs(emptyList())
            .build()
        val operationInProgress = OperationInProgressModel(Tenants.DEFAULT_TENANT_ID, operation.operationId,
            TestFolders.TEST_FOLDER_1_ID, accountId, 0L)
        dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.upsertOneRetryable(rwSingleRetryableCommit(), operation).awaitSingle()
            operationsInProgressDao.upsertOneRetryable(rwSingleRetryableCommit(), operationInProgress).awaitSingle()
        }
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val quotaBeforeOne = quotasBefore[TestResources.YP_HDD_MAN]!!
        val quotaBeforeTwo = quotasBefore[TestResources.YP_SSD_MAN]!!
        val updatedQuotaOne = QuotaModel.builder(quotaBeforeOne)
            .quota(quotaBeforeOne.quota + 100_000_000_000L)
            .frozenQuota(quotaBeforeOne.frozenQuota + 100_000_000_000L)
            .build()
        val updatedQuotaTwo = QuotaModel.builder(quotaBeforeTwo)
            .quota(quotaBeforeTwo.quota + 10_000_000_000L)
            .frozenQuota(quotaBeforeTwo.frozenQuota + 10_000_000_000L)
            .build()
        dbSessionRetryable(tableClient) {
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(updatedQuotaOne, updatedQuotaTwo))
                .awaitSingleOrNull()
        }
        operationsRetryService.retryOperations(Clock.fixed(now.plus(1, ChronoUnit.HOURS), ZoneOffset.UTC),
            Locales.ENGLISH).awaitSingleOrNull()
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val provisionsAfter = dbSessionRetryable(tableClient) {
            accountsQuotasDao.getAllByAccountIds(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(accountId)).awaitSingle()
        }!!.groupBy { it.accountId }.mapValues { e -> e.value.associateBy { it.resourceId } }
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_HDD_MAN]!!.quota - quotasBefore[TestResources.YP_HDD_MAN]!!.quota)
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_SSD_MAN]!!.quota - quotasBefore[TestResources.YP_SSD_MAN]!!.quota)
        Assertions.assertEquals(0L, quotasAfter[TestResources.YP_HDD_MAN]!!.frozenQuota)
        Assertions.assertEquals(0L, quotasAfter[TestResources.YP_SSD_MAN]!!.frozenQuota)
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_HDD_MAN]!!.balance - quotasBefore[TestResources.YP_HDD_MAN]!!.balance)
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_SSD_MAN]!!.balance - quotasBefore[TestResources.YP_SSD_MAN]!!.balance)
        Assertions.assertEquals(0L,
            (provisionsAfter[accountId] ?: emptyMap())[TestResources.YP_HDD_MAN]?.providedQuota ?: 0L)
        Assertions.assertEquals(0L,
            (provisionsAfter[accountId] ?: emptyMap())[TestResources.YP_SSD_MAN]?.providedQuota ?: 0L)
        Assertions.assertEquals(0L,
            (provisionsAfter[accountId] ?: emptyMap())[TestResources.YP_HDD_MAN]?.allocatedQuota ?: 0L)
        Assertions.assertEquals(0L,
            (provisionsAfter[accountId] ?: emptyMap())[TestResources.YP_SSD_MAN]?.allocatedQuota ?: 0L)
        val updatedOperation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operation.operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.ERROR, updatedOperation.requestStatus.get())
        Assertions.assertEquals(AccountsQuotasOperationsModel.OperationType.PROVIDE_RESERVE, updatedOperation.operationType)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 1).awaitSingle()
        }!!
        Assertions.assertEquals(1, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota + 100_000_000_000L,
            opLogs[0].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota + 10_000_000_000L,
            opLogs[0].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[0].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[0].newQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertTrue(opLogs[0].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].actuallyAppliedProvisions.isEmpty)
    }

}
