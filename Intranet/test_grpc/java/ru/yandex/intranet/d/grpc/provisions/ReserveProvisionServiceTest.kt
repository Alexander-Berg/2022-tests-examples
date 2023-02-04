package ru.yandex.intranet.d.grpc.provisions

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import net.devh.boot.grpc.client.inject.GrpcClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestAccounts
import ru.yandex.intranet.d.TestFolders
import ru.yandex.intranet.d.TestProviders
import ru.yandex.intranet.d.TestResources
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.backend.service.proto.AccountKey
import ru.yandex.intranet.d.backend.service.proto.AccountName
import ru.yandex.intranet.d.backend.service.proto.AccountReserveType
import ru.yandex.intranet.d.backend.service.proto.AccountsServiceGrpc
import ru.yandex.intranet.d.backend.service.proto.AccountsSpaceOfProviderReserveAccount
import ru.yandex.intranet.d.backend.service.proto.CreateAccountRequest
import ru.yandex.intranet.d.backend.service.proto.ProvideReserveAccountProvision
import ru.yandex.intranet.d.backend.service.proto.ProvideReserveAccountProvisionAmount
import ru.yandex.intranet.d.backend.service.proto.ProvideReserveAccountRequest
import ru.yandex.intranet.d.backend.service.proto.ProviderAccountsSpace
import ru.yandex.intranet.d.backend.service.proto.ProvidersServiceGrpc
import ru.yandex.intranet.d.backend.service.proto.ReserveProvisionOperationStatus
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
import ru.yandex.intranet.d.grpc.MockGrpcUser
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel
import ru.yandex.intranet.d.model.folders.FolderOperationType
import ru.yandex.intranet.d.model.folders.OperationPhase
import ru.yandex.intranet.d.web.model.SortOrderDto
import java.time.Instant
import java.util.*

/**
 * Reserve provision GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class ReserveProvisionServiceTest(
    @Autowired private val stubProviderService: StubProviderService,
    @Autowired private val quotasDao: QuotasDao,
    @Autowired private val accountsQuotasDao: AccountsQuotasDao,
    @Autowired private val accountsQuotasOperationsDao: AccountsQuotasOperationsDao,
    @Autowired private val folderOperationLogDao: FolderOperationLogDao,
    @Autowired private val tableClient: YdbTableClient
) {

    @GrpcClient("inProcess")
    private lateinit var accountsService: AccountsServiceGrpc.AccountsServiceFutureStub
    @GrpcClient("inProcess")
    private lateinit var providersService: ProvidersServiceGrpc.ProvidersServiceFutureStub

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
        val request = CreateAccountRequest.newBuilder()
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setProviderId(TestProviders.YP_ID)
            .setExternalKey(AccountKey.newBuilder().setValue("outerKey").build())
            .setDisplayName(AccountName.newBuilder().setValue("outerDisplayName").build())
            .setAccountsSpace(ProviderAccountsSpace.newBuilder().setId(TestAccounts.TEST_ACCOUNT_SPACE_3_ID).build())
            .setFreeTier(false)
            .setReserveType(AccountReserveType.RESERVE_TYPE_PROVIDER)
            .build()
        val createAccountResult = accountsService
            .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
            .createAccount(request)
            .await()
        Assertions.assertNotNull(createAccountResult)
        Assertions.assertTrue(createAccountResult.hasAccount())
        val quotasBefore = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val responseBody = providersService
            .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
            .provideReserveAccount(ProvideReserveAccountRequest.newBuilder()
                .setProviderId(TestProviders.YP_ID)
                .setAccountsSpaceId(AccountsSpaceOfProviderReserveAccount.newBuilder()
                    .setId(TestAccounts.TEST_ACCOUNT_SPACE_3_ID).build())
                .setDeltaValues(false)
                .addValues(ProvideReserveAccountProvision.newBuilder()
                    .setResourceId(TestResources.YP_HDD_MAN)
                    .setProvidedAmount(ProvideReserveAccountProvisionAmount.newBuilder()
                        .setValue(100L)
                        .setUnitKey("gigabytes")
                        .build())
                    .build())
                .addValues(ProvideReserveAccountProvision.newBuilder()
                    .setResourceId(TestResources.YP_SSD_MAN)
                    .setProvidedAmount(ProvideReserveAccountProvisionAmount.newBuilder()
                        .setValue(10L)
                        .setUnitKey("gigabytes")
                        .build())
                    .build())
                .build())
            .await()
        val next = stubProviderService.updateProvisionRequests.iterator().next()!!
        val operationId = next.t1.operationId
        Assertions.assertEquals(operationId, responseBody.operationId)
        Assertions.assertEquals(ReserveProvisionOperationStatus.RESERVE_PROVISION_SUCCESS, responseBody.operationStatus)
        Assertions.assertNotNull(responseBody.result)
        val quotasAfter = dbSessionRetryable(tableClient) {
            quotasDao.getByProviderFoldersResources(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID,
                setOf(TestFolders.TEST_FOLDER_1_ID), TestProviders.YP_ID,
                setOf(TestResources.YP_HDD_MAN, TestResources.YP_SSD_MAN)).awaitSingle()
        }!!.associateBy { it.resourceId }
        val accountId = createAccountResult.account.id
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
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK, operation.requestStatus.get())
        Assertions.assertEquals(AccountsQuotasOperationsModel.OperationType.PROVIDE_RESERVE, operation.operationType)
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

}
