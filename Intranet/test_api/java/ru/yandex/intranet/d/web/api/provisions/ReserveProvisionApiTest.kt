package ru.yandex.intranet.d.web.api.provisions

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.reactor.awaitSingle
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
import ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse
import ru.yandex.intranet.d.backend.service.provider_proto.UserID
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel.OperationType
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel.RequestStatus
import ru.yandex.intranet.d.model.folders.FolderOperationType
import ru.yandex.intranet.d.model.folders.OperationPhase
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.SortOrderDto
import ru.yandex.intranet.d.web.model.accounts.AccountOperationDto
import ru.yandex.intranet.d.web.model.accounts.AccountReserveTypeInputDto
import ru.yandex.intranet.d.web.model.accounts.CreateAccountDto
import ru.yandex.intranet.d.web.model.provisions.ProviderReserveProvisionRequestValueDto
import ru.yandex.intranet.d.web.model.provisions.UpdateProviderReserveProvisionsRequestDto
import ru.yandex.intranet.d.web.model.provisions.UpdateProviderReserveProvisionsResponseDto
import ru.yandex.intranet.d.web.model.provisions.UpdateProviderReserveProvisionsStatusDto
import java.time.Instant
import java.util.*

/**
 * Reserve provision API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class ReserveProvisionApiTest(@Autowired private val webClient: WebTestClient,
                              @Autowired private val stubProviderService: StubProviderService,
                              @Autowired private val quotasDao: QuotasDao,
                              @Autowired private val accountsQuotasDao: AccountsQuotasDao,
                              @Autowired private val accountsQuotasOperationsDao: AccountsQuotasOperationsDao,
                              @Autowired private val folderOperationLogDao: FolderOperationLogDao,
                              @Autowired private val tableClient: YdbTableClient) {

    @Test
    fun testReserveProvisionSuccess(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
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
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .success(UpdateProvisionResponse.newBuilder()
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                        .build())
                    .build())
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = 100L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = 10L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = false
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val next = stubProviderService.updateProvisionRequests.iterator().next()!!
        val operationId = next.t1.operationId
        Assertions.assertEquals(operationId, responseBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseBody.operationStatus)
        Assertions.assertNotNull(responseBody.result)
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val accountId = createAccountResult.result.get().id
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
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].newQuotas[TestResources.YP_SSD_MAN])
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
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)
    }

    @Test
    fun testReserveProvisionNoAccountsSpaceSuccess(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_2_ID)
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
                .setTimestamp(Timestamp.newBuilder().setSeconds(now.epochSecond).build())
                .build()).build())))
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .success(UpdateProvisionResponse.newBuilder()
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey("ram")
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
                    .setProvided(Amount.newBuilder()
                        .setValue(100)
                        .setUnitKey("gibibytes")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(50)
                        .setUnitKey("gibibytes")
                        .build())
                    .setLastUpdate(LastUpdate.newBuilder()
                        .setAuthor(UserID.newBuilder()
                            .setPassportUid(PassportUID.newBuilder()
                                .setPassportUid(TestUsers.USER_1_UID)
                                .build())
                            .setStaffLogin(StaffLogin.newBuilder()
                                .setStaffLogin(TestUsers.USER_1_LOGIN)
                                .build())
                            .build())
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                        .build())
                    .build())
                .build())))
        val createAccountRequest = CreateAccountDto.builder()
            .providerId(TestProviders.YDB_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val createAccountResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_2_ID)
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_2_ID), TestProviders.YDB_ID,
                setOf(TestResources.YDB_RAM_SAS)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YDB_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = null,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YDB_RAM_SAS,
                        provided = 100L,
                        providedUnitKey = "gibibytes"
                    )
                ),
                deltaValues = false
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val next = stubProviderService.updateProvisionRequests.iterator().next()!!
        val operationId = next.t1.operationId
        Assertions.assertEquals(operationId, responseBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseBody.operationStatus)
        Assertions.assertNotNull(responseBody.result)
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_2_ID), TestProviders.YDB_ID,
                setOf(TestResources.YDB_RAM_SAS)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val accountId = createAccountResult.result.get().id
        val provisionsAfter = dbSessionRetryable(tableClient) {
            accountsQuotasDao.getAllByAccountIds(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(accountId)).awaitSingle()
        }!!.groupBy { it.accountId }.mapValues { e -> e.value.associateBy { it.resourceId } }
        Assertions.assertEquals(100L * 1024L * 1024L * 1024L,
            (quotasAfter[TestResources.YDB_RAM_SAS]?.quota ?: 0L) - (quotasBefore[TestResources.YDB_RAM_SAS]?.quota ?: 0L))
        Assertions.assertEquals(0L, quotasAfter[TestResources.YDB_RAM_SAS]!!.frozenQuota)
        Assertions.assertEquals(0L,
            (quotasAfter[TestResources.YDB_RAM_SAS]?.balance ?: 0L) - (quotasBefore[TestResources.YDB_RAM_SAS]?.balance ?: 0L))
        Assertions.assertEquals(100L * 1024L * 1024L * 1024L, provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota)
        Assertions.assertEquals(50L * 1024L * 1024L * 1024L, provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.allocatedQuota)
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_2_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YDB_RAM_SAS]?.quota ?: 0L, opLogs[1].oldQuotas[TestResources.YDB_RAM_SAS])
        Assertions.assertEquals(quotasAfter[TestResources.YDB_RAM_SAS]?.quota ?: 0L, opLogs[1].newQuotas[TestResources.YDB_RAM_SAS])
        Assertions.assertTrue(opLogs[0].oldQuotas.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].newQuotas.asMap().isEmpty())
        Assertions.assertEquals(0L, opLogs[0].oldProvisions[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogs[0].newProvisions[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogs[0].actuallyAppliedProvisions.get()[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)
    }

    @Test
    fun testReserveProvisionDeltaSuccess(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
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
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .success(UpdateProvisionResponse.newBuilder()
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                        .build())
                    .build())
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = 100L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = 10L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = true
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val next = stubProviderService.updateProvisionRequests.iterator().next()!!
        val operationId = next.t1.operationId
        Assertions.assertEquals(operationId, responseBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseBody.operationStatus)
        Assertions.assertNotNull(responseBody.result)
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val accountId = createAccountResult.result.get().id
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
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].newQuotas[TestResources.YP_SSD_MAN])
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
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)
    }

    @Test
    fun testReserveProvisionNoAccountsSpaceDeltaSuccess(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_2_ID)
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
                .setTimestamp(Timestamp.newBuilder().setSeconds(now.epochSecond).build())
                .build()).build())))
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .success(UpdateProvisionResponse.newBuilder()
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey("ram")
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
                    .setProvided(Amount.newBuilder()
                        .setValue(100)
                        .setUnitKey("gibibytes")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(50)
                        .setUnitKey("gibibytes")
                        .build())
                    .setLastUpdate(LastUpdate.newBuilder()
                        .setAuthor(UserID.newBuilder()
                            .setPassportUid(PassportUID.newBuilder()
                                .setPassportUid(TestUsers.USER_1_UID)
                                .build())
                            .setStaffLogin(StaffLogin.newBuilder()
                                .setStaffLogin(TestUsers.USER_1_LOGIN)
                                .build())
                            .build())
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                        .build())
                    .build())
                .build())))
        val createAccountRequest = CreateAccountDto.builder()
            .providerId(TestProviders.YDB_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val createAccountResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_2_ID)
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_2_ID), TestProviders.YDB_ID,
                setOf(TestResources.YDB_RAM_SAS)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YDB_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = null,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YDB_RAM_SAS,
                        provided = 100L,
                        providedUnitKey = "gibibytes"
                    )
                ),
                deltaValues = true
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val next = stubProviderService.updateProvisionRequests.iterator().next()!!
        val operationId = next.t1.operationId
        Assertions.assertEquals(operationId, responseBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseBody.operationStatus)
        Assertions.assertNotNull(responseBody.result)
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_2_ID), TestProviders.YDB_ID,
                setOf(TestResources.YDB_RAM_SAS)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val accountId = createAccountResult.result.get().id
        val provisionsAfter = dbSessionRetryable(tableClient) {
            accountsQuotasDao.getAllByAccountIds(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(accountId)).awaitSingle()
        }!!.groupBy { it.accountId }.mapValues { e -> e.value.associateBy { it.resourceId } }
        Assertions.assertEquals(100L * 1024L * 1024L * 1024L,
            (quotasAfter[TestResources.YDB_RAM_SAS]?.quota ?: 0L) - (quotasBefore[TestResources.YDB_RAM_SAS]?.quota ?: 0L))
        Assertions.assertEquals(0L, quotasAfter[TestResources.YDB_RAM_SAS]!!.frozenQuota)
        Assertions.assertEquals(0L,
            (quotasAfter[TestResources.YDB_RAM_SAS]?.balance ?: 0L) - (quotasBefore[TestResources.YDB_RAM_SAS]?.balance ?: 0L))
        Assertions.assertEquals(100L * 1024L * 1024L * 1024L, provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota)
        Assertions.assertEquals(50L * 1024L * 1024L * 1024L, provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.allocatedQuota)
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_2_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YDB_RAM_SAS]?.quota ?: 0L, opLogs[1].oldQuotas[TestResources.YDB_RAM_SAS])
        Assertions.assertEquals(quotasAfter[TestResources.YDB_RAM_SAS]?.quota ?: 0L, opLogs[1].newQuotas[TestResources.YDB_RAM_SAS])
        Assertions.assertTrue(opLogs[0].oldQuotas.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].newQuotas.asMap().isEmpty())
        Assertions.assertEquals(0L, opLogs[0].oldProvisions[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogs[0].newProvisions[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogs[0].actuallyAppliedProvisions.get()[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)
    }

    @Test
    fun testReserveProvisionIncrementDecrementSuccess(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
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
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .success(UpdateProvisionResponse.newBuilder()
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                        .build())
                    .build())
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
                .build()),
            GrpcResponse.success(UpdateProvisionResponse.newBuilder()
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey("hdd")
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(50)
                        .setUnitKey("gigabytes")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(25)
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setValue(5)
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                        .build())
                    .build())
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = 100L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = 10L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = false
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val next = stubProviderService.updateProvisionRequests.iterator().next()!!
        val operationId = next.t1.operationId
        Assertions.assertEquals(operationId, responseBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseBody.operationStatus)
        Assertions.assertNotNull(responseBody.result)
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val accountId = createAccountResult.result.get().id
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
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].newQuotas[TestResources.YP_SSD_MAN])
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
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)
        val responseDecrementBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = 50L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = 5L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = false
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val nextDecrement = stubProviderService.updateProvisionRequests.first!!
        val operationIdDecrement = nextDecrement.t1.operationId
        Assertions.assertEquals(operationIdDecrement, responseBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseDecrementBody.operationStatus)
        Assertions.assertNotNull(responseDecrementBody.result)
        val quotasAfterDecrement = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val provisionsAfterDecrement = dbSessionRetryable(tableClient) {
            accountsQuotasDao.getAllByAccountIds(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(accountId)).awaitSingle()
        }!!.groupBy { it.accountId }.mapValues { e -> e.value.associateBy { it.resourceId } }
        Assertions.assertEquals(50_000_000_000L,
            quotasAfterDecrement[TestResources.YP_HDD_MAN]!!.quota - quotasBefore[TestResources.YP_HDD_MAN]!!.quota)
        Assertions.assertEquals(5_000_000_000L,
            quotasAfterDecrement[TestResources.YP_SSD_MAN]!!.quota - quotasBefore[TestResources.YP_SSD_MAN]!!.quota)
        Assertions.assertEquals(0L, quotasAfterDecrement[TestResources.YP_HDD_MAN]!!.frozenQuota)
        Assertions.assertEquals(0L, quotasAfterDecrement[TestResources.YP_SSD_MAN]!!.frozenQuota)
        Assertions.assertEquals(0L,
            quotasAfterDecrement[TestResources.YP_HDD_MAN]!!.balance - quotasBefore[TestResources.YP_HDD_MAN]!!.balance)
        Assertions.assertEquals(0L,
            quotasAfterDecrement[TestResources.YP_SSD_MAN]!!.balance - quotasBefore[TestResources.YP_SSD_MAN]!!.balance)
        Assertions.assertEquals(50_000_000_000L, provisionsAfterDecrement[accountId]!![TestResources.YP_HDD_MAN]!!.providedQuota)
        Assertions.assertEquals(5_000_000_000L, provisionsAfterDecrement[accountId]!![TestResources.YP_SSD_MAN]!!.providedQuota)
        Assertions.assertEquals(25_000_000_000L, provisionsAfterDecrement[accountId]!![TestResources.YP_HDD_MAN]!!.allocatedQuota)
        Assertions.assertEquals(0L, provisionsAfterDecrement[accountId]!![TestResources.YP_SSD_MAN]!!.allocatedQuota)
        val operationDecrement = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operationDecrement.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operationDecrement.operationType)
        val opLogsDecrement = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogsDecrement.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogsDecrement[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogsDecrement[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogsDecrement[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogsDecrement[1].operationPhase.get())
        Assertions.assertEquals(quotasAfter[TestResources.YP_HDD_MAN]!!.quota, opLogsDecrement[0].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_SSD_MAN]!!.quota, opLogsDecrement[0].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasAfterDecrement[TestResources.YP_HDD_MAN]!!.quota, opLogsDecrement[0].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasAfterDecrement[TestResources.YP_SSD_MAN]!!.quota, opLogsDecrement[0].newQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertTrue(opLogsDecrement[1].oldQuotas.asMap().isEmpty())
        Assertions.assertTrue(opLogsDecrement[1].newQuotas.asMap().isEmpty())
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YP_HDD_MAN]!!.providedQuota,
            opLogsDecrement[0].oldProvisions[accountId]!![TestResources.YP_HDD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YP_SSD_MAN]!!.providedQuota,
            opLogsDecrement[0].oldProvisions[accountId]!![TestResources.YP_SSD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfterDecrement[accountId]!![TestResources.YP_HDD_MAN]!!.providedQuota,
            opLogsDecrement[0].newProvisions[accountId]!![TestResources.YP_HDD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfterDecrement[accountId]!![TestResources.YP_SSD_MAN]!!.providedQuota,
            opLogsDecrement[0].newProvisions[accountId]!![TestResources.YP_SSD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfterDecrement[accountId]!![TestResources.YP_HDD_MAN]!!.providedQuota,
            opLogsDecrement[0].actuallyAppliedProvisions.get()[accountId]!![TestResources.YP_HDD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfterDecrement[accountId]!![TestResources.YP_SSD_MAN]!!.providedQuota,
            opLogsDecrement[0].actuallyAppliedProvisions.get()[accountId]!![TestResources.YP_SSD_MAN]!!.provision)
        Assertions.assertTrue(opLogsDecrement[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogsDecrement[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogsDecrement[1].actuallyAppliedProvisions.isEmpty)
    }

    @Test
    fun testReserveProvisionNoAccountsSpaceIncrementDecrementSuccess(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_2_ID)
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
                .setTimestamp(Timestamp.newBuilder().setSeconds(now.epochSecond).build())
                .build()).build())))
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .success(UpdateProvisionResponse.newBuilder()
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey("ram")
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
                    .setProvided(Amount.newBuilder()
                        .setValue(100)
                        .setUnitKey("gibibytes")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(50)
                        .setUnitKey("gibibytes")
                        .build())
                    .setLastUpdate(LastUpdate.newBuilder()
                        .setAuthor(UserID.newBuilder()
                            .setPassportUid(PassportUID.newBuilder()
                                .setPassportUid(TestUsers.USER_1_UID)
                                .build())
                            .setStaffLogin(StaffLogin.newBuilder()
                                .setStaffLogin(TestUsers.USER_1_LOGIN)
                                .build())
                            .build())
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(UpdateProvisionResponse.newBuilder()
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey("ram")
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
                    .setProvided(Amount.newBuilder()
                        .setValue(50)
                        .setUnitKey("gibibytes")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(25)
                        .setUnitKey("gibibytes")
                        .build())
                    .setLastUpdate(LastUpdate.newBuilder()
                        .setAuthor(UserID.newBuilder()
                            .setPassportUid(PassportUID.newBuilder()
                                .setPassportUid(TestUsers.USER_1_UID)
                                .build())
                            .setStaffLogin(StaffLogin.newBuilder()
                                .setStaffLogin(TestUsers.USER_1_LOGIN)
                                .build())
                            .build())
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                        .build())
                    .build())
                .build())))
        val createAccountRequest = CreateAccountDto.builder()
            .providerId(TestProviders.YDB_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val createAccountResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_2_ID)
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_2_ID), TestProviders.YDB_ID,
                setOf(TestResources.YDB_RAM_SAS)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YDB_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = null,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YDB_RAM_SAS,
                        provided = 100L,
                        providedUnitKey = "gibibytes"
                    )
                ),
                deltaValues = false
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val next = stubProviderService.updateProvisionRequests.iterator().next()!!
        val operationId = next.t1.operationId
        Assertions.assertEquals(operationId, responseBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseBody.operationStatus)
        Assertions.assertNotNull(responseBody.result)
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_2_ID), TestProviders.YDB_ID,
                setOf(TestResources.YDB_RAM_SAS)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val accountId = createAccountResult.result.get().id
        val provisionsAfter = dbSessionRetryable(tableClient) {
            accountsQuotasDao.getAllByAccountIds(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(accountId)).awaitSingle()
        }!!.groupBy { it.accountId }.mapValues { e -> e.value.associateBy { it.resourceId } }
        Assertions.assertEquals(100L * 1024L * 1024L * 1024L,
            (quotasAfter[TestResources.YDB_RAM_SAS]?.quota ?: 0L) - (quotasBefore[TestResources.YDB_RAM_SAS]?.quota ?: 0L))
        Assertions.assertEquals(0L, quotasAfter[TestResources.YDB_RAM_SAS]!!.frozenQuota)
        Assertions.assertEquals(0L,
            (quotasAfter[TestResources.YDB_RAM_SAS]?.balance ?: 0L) - (quotasBefore[TestResources.YDB_RAM_SAS]?.balance ?: 0L))
        Assertions.assertEquals(100L * 1024L * 1024L * 1024L, provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota)
        Assertions.assertEquals(50L * 1024L * 1024L * 1024L, provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.allocatedQuota)
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_2_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YDB_RAM_SAS]?.quota ?: 0L, opLogs[1].oldQuotas[TestResources.YDB_RAM_SAS])
        Assertions.assertEquals(quotasAfter[TestResources.YDB_RAM_SAS]?.quota ?: 0L, opLogs[1].newQuotas[TestResources.YDB_RAM_SAS])
        Assertions.assertTrue(opLogs[0].oldQuotas.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].newQuotas.asMap().isEmpty())
        Assertions.assertEquals(0L, opLogs[0].oldProvisions[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogs[0].newProvisions[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogs[0].actuallyAppliedProvisions.get()[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)
        val responseDecrementBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YDB_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = null,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YDB_RAM_SAS,
                        provided = 50L,
                        providedUnitKey = "gibibytes"
                    )
                ),
                deltaValues = false
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val nextDecrement = stubProviderService.updateProvisionRequests.last!!
        val operationIdDecrement = nextDecrement.t1.operationId
        Assertions.assertEquals(operationIdDecrement, responseDecrementBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseDecrementBody.operationStatus)
        Assertions.assertNotNull(responseDecrementBody.result)
        val quotasAfterDecrement = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_2_ID), TestProviders.YDB_ID,
                setOf(TestResources.YDB_RAM_SAS)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val provisionsAfterDecrement = dbSessionRetryable(tableClient) {
            accountsQuotasDao.getAllByAccountIds(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(accountId)).awaitSingle()
        }!!.groupBy { it.accountId }.mapValues { e -> e.value.associateBy { it.resourceId } }
        Assertions.assertEquals(50L * 1024L * 1024L * 1024L,
            (quotasAfterDecrement[TestResources.YDB_RAM_SAS]?.quota ?: 0L) - (quotasBefore[TestResources.YDB_RAM_SAS]?.quota ?: 0L))
        Assertions.assertEquals(0L, quotasAfterDecrement[TestResources.YDB_RAM_SAS]!!.frozenQuota)
        Assertions.assertEquals(0L,
            (quotasAfterDecrement[TestResources.YDB_RAM_SAS]?.balance ?: 0L) - (quotasBefore[TestResources.YDB_RAM_SAS]?.balance ?: 0L))
        Assertions.assertEquals(50L * 1024L * 1024L * 1024L, provisionsAfterDecrement[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota)
        Assertions.assertEquals(25L * 1024L * 1024L * 1024L, provisionsAfterDecrement[accountId]!![TestResources.YDB_RAM_SAS]!!.allocatedQuota)
        val operationDecrement = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operationDecrement.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operationDecrement.operationType)
        val opLogsDecrement = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_2_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogsDecrement.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogsDecrement[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogsDecrement[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogsDecrement[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogsDecrement[1].operationPhase.get())
        Assertions.assertEquals(quotasAfter[TestResources.YDB_RAM_SAS]?.quota ?: 0L,
            opLogsDecrement[0].oldQuotas[TestResources.YDB_RAM_SAS])
        Assertions.assertEquals(quotasAfterDecrement[TestResources.YDB_RAM_SAS]?.quota ?: 0L,
            opLogsDecrement[0].newQuotas[TestResources.YDB_RAM_SAS])
        Assertions.assertTrue(opLogsDecrement[1].oldQuotas.asMap().isEmpty())
        Assertions.assertTrue(opLogsDecrement[1].newQuotas.asMap().isEmpty())
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogsDecrement[0].oldProvisions[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertEquals(provisionsAfterDecrement[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogsDecrement[0].newProvisions[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertEquals(provisionsAfterDecrement[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogsDecrement[0].actuallyAppliedProvisions.get()[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertTrue(opLogsDecrement[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogsDecrement[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogsDecrement[1].actuallyAppliedProvisions.isEmpty)
    }

    @Test
    fun testReserveProvisionIncrementDecrementDeltaSuccess(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
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
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .success(UpdateProvisionResponse.newBuilder()
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                        .build())
                    .build())
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
                .build()),
            GrpcResponse.success(UpdateProvisionResponse.newBuilder()
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey("hdd")
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(50)
                        .setUnitKey("gigabytes")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(25)
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setValue(5)
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                        .build())
                    .build())
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = 100L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = 10L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = true
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val next = stubProviderService.updateProvisionRequests.iterator().next()!!
        val operationId = next.t1.operationId
        Assertions.assertEquals(operationId, responseBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseBody.operationStatus)
        Assertions.assertNotNull(responseBody.result)
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val accountId = createAccountResult.result.get().id
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
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].newQuotas[TestResources.YP_SSD_MAN])
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
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)
        val responseDecrementBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = -50L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = -5L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = true
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val nextDecrement = stubProviderService.updateProvisionRequests.first!!
        val operationIdDecrement = nextDecrement.t1.operationId
        Assertions.assertEquals(operationIdDecrement, responseBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseDecrementBody.operationStatus)
        Assertions.assertNotNull(responseDecrementBody.result)
        val quotasAfterDecrement = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val provisionsAfterDecrement = dbSessionRetryable(tableClient) {
            accountsQuotasDao.getAllByAccountIds(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(accountId)).awaitSingle()
        }!!.groupBy { it.accountId }.mapValues { e -> e.value.associateBy { it.resourceId } }
        Assertions.assertEquals(50_000_000_000L,
            quotasAfterDecrement[TestResources.YP_HDD_MAN]!!.quota - quotasBefore[TestResources.YP_HDD_MAN]!!.quota)
        Assertions.assertEquals(5_000_000_000L,
            quotasAfterDecrement[TestResources.YP_SSD_MAN]!!.quota - quotasBefore[TestResources.YP_SSD_MAN]!!.quota)
        Assertions.assertEquals(0L, quotasAfterDecrement[TestResources.YP_HDD_MAN]!!.frozenQuota)
        Assertions.assertEquals(0L, quotasAfterDecrement[TestResources.YP_SSD_MAN]!!.frozenQuota)
        Assertions.assertEquals(0L,
            quotasAfterDecrement[TestResources.YP_HDD_MAN]!!.balance - quotasBefore[TestResources.YP_HDD_MAN]!!.balance)
        Assertions.assertEquals(0L,
            quotasAfterDecrement[TestResources.YP_SSD_MAN]!!.balance - quotasBefore[TestResources.YP_SSD_MAN]!!.balance)
        Assertions.assertEquals(50_000_000_000L, provisionsAfterDecrement[accountId]!![TestResources.YP_HDD_MAN]!!.providedQuota)
        Assertions.assertEquals(5_000_000_000L, provisionsAfterDecrement[accountId]!![TestResources.YP_SSD_MAN]!!.providedQuota)
        Assertions.assertEquals(25_000_000_000L, provisionsAfterDecrement[accountId]!![TestResources.YP_HDD_MAN]!!.allocatedQuota)
        Assertions.assertEquals(0L, provisionsAfterDecrement[accountId]!![TestResources.YP_SSD_MAN]!!.allocatedQuota)
        val operationDecrement = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operationDecrement.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operationDecrement.operationType)
        val opLogsDecrement = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogsDecrement.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogsDecrement[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogsDecrement[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogsDecrement[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogsDecrement[1].operationPhase.get())
        Assertions.assertEquals(quotasAfter[TestResources.YP_HDD_MAN]!!.quota, opLogsDecrement[0].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_SSD_MAN]!!.quota, opLogsDecrement[0].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasAfterDecrement[TestResources.YP_HDD_MAN]!!.quota, opLogsDecrement[0].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasAfterDecrement[TestResources.YP_SSD_MAN]!!.quota, opLogsDecrement[0].newQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertTrue(opLogsDecrement[1].oldQuotas.asMap().isEmpty())
        Assertions.assertTrue(opLogsDecrement[1].newQuotas.asMap().isEmpty())
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YP_HDD_MAN]!!.providedQuota,
            opLogsDecrement[0].oldProvisions[accountId]!![TestResources.YP_HDD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YP_SSD_MAN]!!.providedQuota,
            opLogsDecrement[0].oldProvisions[accountId]!![TestResources.YP_SSD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfterDecrement[accountId]!![TestResources.YP_HDD_MAN]!!.providedQuota,
            opLogsDecrement[0].newProvisions[accountId]!![TestResources.YP_HDD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfterDecrement[accountId]!![TestResources.YP_SSD_MAN]!!.providedQuota,
            opLogsDecrement[0].newProvisions[accountId]!![TestResources.YP_SSD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfterDecrement[accountId]!![TestResources.YP_HDD_MAN]!!.providedQuota,
            opLogsDecrement[0].actuallyAppliedProvisions.get()[accountId]!![TestResources.YP_HDD_MAN]!!.provision)
        Assertions.assertEquals(provisionsAfterDecrement[accountId]!![TestResources.YP_SSD_MAN]!!.providedQuota,
            opLogsDecrement[0].actuallyAppliedProvisions.get()[accountId]!![TestResources.YP_SSD_MAN]!!.provision)
        Assertions.assertTrue(opLogsDecrement[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogsDecrement[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogsDecrement[1].actuallyAppliedProvisions.isEmpty)
    }

    @Test
    fun testReserveProvisionNoAccountsSpaceIncrementDecrementDeltaSuccess(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_2_ID)
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
                .setTimestamp(Timestamp.newBuilder().setSeconds(now.epochSecond).build())
                .build()).build())))
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .success(UpdateProvisionResponse.newBuilder()
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey("ram")
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
                    .setProvided(Amount.newBuilder()
                        .setValue(100)
                        .setUnitKey("gibibytes")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(50)
                        .setUnitKey("gibibytes")
                        .build())
                    .setLastUpdate(LastUpdate.newBuilder()
                        .setAuthor(UserID.newBuilder()
                            .setPassportUid(PassportUID.newBuilder()
                                .setPassportUid(TestUsers.USER_1_UID)
                                .build())
                            .setStaffLogin(StaffLogin.newBuilder()
                                .setStaffLogin(TestUsers.USER_1_LOGIN)
                                .build())
                            .build())
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(UpdateProvisionResponse.newBuilder()
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey("ram")
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
                    .setProvided(Amount.newBuilder()
                        .setValue(50)
                        .setUnitKey("gibibytes")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(25)
                        .setUnitKey("gibibytes")
                        .build())
                    .setLastUpdate(LastUpdate.newBuilder()
                        .setAuthor(UserID.newBuilder()
                            .setPassportUid(PassportUID.newBuilder()
                                .setPassportUid(TestUsers.USER_1_UID)
                                .build())
                            .setStaffLogin(StaffLogin.newBuilder()
                                .setStaffLogin(TestUsers.USER_1_LOGIN)
                                .build())
                            .build())
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                        .build())
                    .build())
                .build())))
        val createAccountRequest = CreateAccountDto.builder()
            .providerId(TestProviders.YDB_ID)
            .externalKey("outerKey")
            .displayName("outerDisplayName")
            .freeTier(false)
            .reserveType(AccountReserveTypeInputDto.PROVIDER)
            .build()
        val createAccountResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_2_ID)
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_2_ID), TestProviders.YDB_ID,
                setOf(TestResources.YDB_RAM_SAS)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YDB_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = null,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YDB_RAM_SAS,
                        provided = 100L,
                        providedUnitKey = "gibibytes"
                    )
                ),
                deltaValues = true
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val next = stubProviderService.updateProvisionRequests.iterator().next()!!
        val operationId = next.t1.operationId
        Assertions.assertEquals(operationId, responseBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseBody.operationStatus)
        Assertions.assertNotNull(responseBody.result)
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_2_ID), TestProviders.YDB_ID,
                setOf(TestResources.YDB_RAM_SAS)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val accountId = createAccountResult.result.get().id
        val provisionsAfter = dbSessionRetryable(tableClient) {
            accountsQuotasDao.getAllByAccountIds(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(accountId)).awaitSingle()
        }!!.groupBy { it.accountId }.mapValues { e -> e.value.associateBy { it.resourceId } }
        Assertions.assertEquals(100L * 1024L * 1024L * 1024L,
            (quotasAfter[TestResources.YDB_RAM_SAS]?.quota ?: 0L) - (quotasBefore[TestResources.YDB_RAM_SAS]?.quota ?: 0L))
        Assertions.assertEquals(0L, quotasAfter[TestResources.YDB_RAM_SAS]!!.frozenQuota)
        Assertions.assertEquals(0L,
            (quotasAfter[TestResources.YDB_RAM_SAS]?.balance ?: 0L) - (quotasBefore[TestResources.YDB_RAM_SAS]?.balance ?: 0L))
        Assertions.assertEquals(100L * 1024L * 1024L * 1024L, provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota)
        Assertions.assertEquals(50L * 1024L * 1024L * 1024L, provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.allocatedQuota)
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_2_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YDB_RAM_SAS]?.quota ?: 0L, opLogs[1].oldQuotas[TestResources.YDB_RAM_SAS])
        Assertions.assertEquals(quotasAfter[TestResources.YDB_RAM_SAS]?.quota ?: 0L, opLogs[1].newQuotas[TestResources.YDB_RAM_SAS])
        Assertions.assertTrue(opLogs[0].oldQuotas.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].newQuotas.asMap().isEmpty())
        Assertions.assertEquals(0L, opLogs[0].oldProvisions[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogs[0].newProvisions[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogs[0].actuallyAppliedProvisions.get()[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)
        val responseDecrementBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YDB_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = null,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YDB_RAM_SAS,
                        provided = -50L,
                        providedUnitKey = "gibibytes"
                    )
                ),
                deltaValues = true
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val nextDecrement = stubProviderService.updateProvisionRequests.last!!
        val operationIdDecrement = nextDecrement.t1.operationId
        Assertions.assertEquals(operationIdDecrement, responseDecrementBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseDecrementBody.operationStatus)
        Assertions.assertNotNull(responseDecrementBody.result)
        val quotasAfterDecrement = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_2_ID), TestProviders.YDB_ID,
                setOf(TestResources.YDB_RAM_SAS)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val provisionsAfterDecrement = dbSessionRetryable(tableClient) {
            accountsQuotasDao.getAllByAccountIds(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(accountId)).awaitSingle()
        }!!.groupBy { it.accountId }.mapValues { e -> e.value.associateBy { it.resourceId } }
        Assertions.assertEquals(50L * 1024L * 1024L * 1024L,
            (quotasAfterDecrement[TestResources.YDB_RAM_SAS]?.quota ?: 0L) - (quotasBefore[TestResources.YDB_RAM_SAS]?.quota ?: 0L))
        Assertions.assertEquals(0L, quotasAfterDecrement[TestResources.YDB_RAM_SAS]!!.frozenQuota)
        Assertions.assertEquals(0L,
            (quotasAfterDecrement[TestResources.YDB_RAM_SAS]?.balance ?: 0L) - (quotasBefore[TestResources.YDB_RAM_SAS]?.balance ?: 0L))
        Assertions.assertEquals(50L * 1024L * 1024L * 1024L, provisionsAfterDecrement[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota)
        Assertions.assertEquals(25L * 1024L * 1024L * 1024L, provisionsAfterDecrement[accountId]!![TestResources.YDB_RAM_SAS]!!.allocatedQuota)
        val operationDecrement = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operationDecrement.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operationDecrement.operationType)
        val opLogsDecrement = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_2_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogsDecrement.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogsDecrement[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogsDecrement[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogsDecrement[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogsDecrement[1].operationPhase.get())
        Assertions.assertEquals(quotasAfter[TestResources.YDB_RAM_SAS]?.quota ?: 0L,
            opLogsDecrement[0].oldQuotas[TestResources.YDB_RAM_SAS])
        Assertions.assertEquals(quotasAfterDecrement[TestResources.YDB_RAM_SAS]?.quota ?: 0L,
            opLogsDecrement[0].newQuotas[TestResources.YDB_RAM_SAS])
        Assertions.assertTrue(opLogsDecrement[1].oldQuotas.asMap().isEmpty())
        Assertions.assertTrue(opLogsDecrement[1].newQuotas.asMap().isEmpty())
        Assertions.assertEquals(provisionsAfter[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogsDecrement[0].oldProvisions[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertEquals(provisionsAfterDecrement[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogsDecrement[0].newProvisions[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertEquals(provisionsAfterDecrement[accountId]!![TestResources.YDB_RAM_SAS]!!.providedQuota,
            opLogsDecrement[0].actuallyAppliedProvisions.get()[accountId]!![TestResources.YDB_RAM_SAS]!!.provision)
        Assertions.assertTrue(opLogsDecrement[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogsDecrement[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogsDecrement[1].actuallyAppliedProvisions.isEmpty)
    }

    @Test
    fun testReserveProvisionSuccessIdempotencyKey(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
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
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .success(UpdateProvisionResponse.newBuilder()
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setOperationId(UUID.randomUUID().toString())
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
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(now.epochSecond))
                        .build())
                    .build())
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val idempotencyKey = UUID.randomUUID().toString()
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .header("Idempotency-Key", idempotencyKey)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = 100L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = 10L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = false
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val next = stubProviderService.updateProvisionRequests.iterator().next()!!
        val operationId = next.t1.operationId
        Assertions.assertEquals(operationId, responseBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseBody.operationStatus)
        Assertions.assertNotNull(responseBody.result)
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val accountId = createAccountResult.result.get().id
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
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].newQuotas[TestResources.YP_SSD_MAN])
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
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)

        val responseBodyRepeated = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .header("Idempotency-Key", idempotencyKey)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = 100L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = 10L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = false
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(operationId, responseBodyRepeated.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseBodyRepeated.operationStatus)
        Assertions.assertNotNull(responseBodyRepeated.result)
    }

    @Test
    fun testReserveProvisionInvalidAmount(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
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
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = -100L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = -10L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = true
            ))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertFalse(responseBody.fieldErrors.isEmpty())
    }

    @Test
    fun testReserveProvisionBadRequest(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
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
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .failure(StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Invalid argument")))))
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = 100L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = 10L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = false
            ))
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertFalse(responseBody.errors.isEmpty())
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_HDD_MAN]!!.quota - quotasBefore[TestResources.YP_HDD_MAN]!!.quota)
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_SSD_MAN]!!.quota - quotasBefore[TestResources.YP_SSD_MAN]!!.quota)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota + 100_000_000_000L,
            opLogs[1].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota + 10_000_000_000L,
            opLogs[1].newQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota + 100_000_000_000L,
            opLogs[0].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota + 10_000_000_000L,
            opLogs[0].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[0].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[0].newQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertTrue(opLogs[0].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), opLogs[0].accountsQuotasOperationsId.get(),
                Tenants.DEFAULT_TENANT_ID).awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.ERROR, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
    }

    @Test
    fun testReserveProvisionFatalError(): Unit = runBlocking {
        stubProviderService.reset()
        val now = Instant.now()
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
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .failure(StatusRuntimeException(Status.INTERNAL.withDescription("Internal error")))))
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = 100L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = 10L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = false
            ))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertFalse(responseBody.errors.isEmpty())
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_HDD_MAN]!!.quota - quotasBefore[TestResources.YP_HDD_MAN]!!.quota)
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_SSD_MAN]!!.quota - quotasBefore[TestResources.YP_SSD_MAN]!!.quota)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota + 100_000_000_000L,
            opLogs[1].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota + 10_000_000_000L,
            opLogs[1].newQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota + 100_000_000_000L,
            opLogs[0].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota + 10_000_000_000L,
            opLogs[0].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[0].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[0].newQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertTrue(opLogs[0].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), opLogs[0].accountsQuotasOperationsId.get(),
                Tenants.DEFAULT_TENANT_ID).awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.ERROR, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
    }

    @Test
    fun testReserveProvisionConflictUnresolved(): Unit = runBlocking {
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
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .failure(StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Failed precondition")))))
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
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setDisplayName("outerDisplayName")
            .setKey("outerKey")
            .setFreeTier(false)
            .addAllProvisions(emptyList())
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = 100L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = 10L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = false
            ))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.PRECONDITION_FAILED)
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertFalse(responseBody.errors.isEmpty())
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_HDD_MAN]!!.quota - quotasBefore[TestResources.YP_HDD_MAN]!!.quota)
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_SSD_MAN]!!.quota - quotasBefore[TestResources.YP_SSD_MAN]!!.quota)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota + 100_000_000_000L,
            opLogs[1].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota + 10_000_000_000L,
            opLogs[1].newQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota + 100_000_000_000L,
            opLogs[0].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota + 10_000_000_000L,
            opLogs[0].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[0].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[0].newQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertTrue(opLogs[0].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), opLogs[0].accountsQuotasOperationsId.get(),
                Tenants.DEFAULT_TENANT_ID).awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.ERROR, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
    }

    @Test
    fun testReserveProvisionConflictResolved(): Unit = runBlocking {
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
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .failure(StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Failed precondition")))))
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = 100L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = 10L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = false
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProviderReserveProvisionsResponseDto::class.java)
            .returnResult()
            .responseBody!!
        val next = stubProviderService.updateProvisionRequests.iterator().next()!!
        val operationId = next.t1.operationId
        Assertions.assertEquals(operationId, responseBody.operationId)
        Assertions.assertEquals(UpdateProviderReserveProvisionsStatusDto.SUCCESS, responseBody.operationStatus)
        Assertions.assertNotNull(responseBody.result)
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val accountId = createAccountResult.result.get().id
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
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), operationId, Tenants.DEFAULT_TENANT_ID)
                .awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.OK, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasAfter[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].newQuotas[TestResources.YP_SSD_MAN])
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
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)
    }

    @Test
    fun testReserveProvisionConflictUnresolvedResolutionError(): Unit = runBlocking {
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
        stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse
            .failure(StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Failed precondition")))))
        stubProviderService.setGetAccountResponses(listOf(GrpcResponse
            .failure(StatusRuntimeException(Status.NOT_FOUND.withDescription("Not found")))))
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
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
            .post()
            .uri("/api/v1/providers/{id}/_provideReserve", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(UpdateProviderReserveProvisionsRequestDto(
                accountsSpaceId = TestAccounts.TEST_ACCOUNT_SPACE_3_ID,
                values = listOf(
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_HDD_MAN,
                        provided = 100L,
                        providedUnitKey = "gigabytes"
                    ),
                    ProviderReserveProvisionRequestValueDto(
                        resourceId = TestResources.YP_SSD_MAN,
                        provided = 10L,
                        providedUnitKey = "gigabytes"
                    )
                ),
                deltaValues = false
            ))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.PRECONDITION_FAILED)
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertFalse(responseBody.errors.isEmpty())
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_HDD_MAN]!!.quota - quotasBefore[TestResources.YP_HDD_MAN]!!.quota)
        Assertions.assertEquals(0L,
            quotasAfter[TestResources.YP_SSD_MAN]!!.quota - quotasBefore[TestResources.YP_SSD_MAN]!!.quota)
        val opLogs = dbSessionRetryable(tableClient) {
            folderOperationLogDao.getFirstPageByFolder(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.DESC, 2).awaitSingle()
        }!!
        Assertions.assertEquals(2, opLogs.size)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[0].operationType)
        Assertions.assertEquals(FolderOperationType.PROVIDE_RESERVE, opLogs[1].operationType)
        Assertions.assertEquals(OperationPhase.CLOSE, opLogs[0].operationPhase.get())
        Assertions.assertEquals(OperationPhase.SUBMIT, opLogs[1].operationPhase.get())
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[1].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota + 100_000_000_000L,
            opLogs[1].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota + 10_000_000_000L,
            opLogs[1].newQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota + 100_000_000_000L,
            opLogs[0].oldQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota + 10_000_000_000L,
            opLogs[0].oldQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_HDD_MAN]!!.quota, opLogs[0].newQuotas[TestResources.YP_HDD_MAN])
        Assertions.assertEquals(quotasBefore[TestResources.YP_SSD_MAN]!!.quota, opLogs[0].newQuotas[TestResources.YP_SSD_MAN])
        Assertions.assertTrue(opLogs[0].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[0].actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(opLogs[1].oldProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].newProvisions.asMap().isEmpty())
        Assertions.assertTrue(opLogs[1].actuallyAppliedProvisions.isEmpty)
        val operation = dbSessionRetryable(tableClient) {
            accountsQuotasOperationsDao.getById(rwSingleRetryableCommit(), opLogs[0].accountsQuotasOperationsId.get(),
                Tenants.DEFAULT_TENANT_ID).awaitSingle().get()
        }!!
        Assertions.assertEquals(RequestStatus.ERROR, operation.requestStatus.get())
        Assertions.assertEquals(OperationType.PROVIDE_RESERVE, operation.operationType)
    }

}
