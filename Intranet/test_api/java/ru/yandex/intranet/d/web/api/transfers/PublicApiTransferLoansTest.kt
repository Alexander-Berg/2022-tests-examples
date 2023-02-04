package ru.yandex.intranet.d.web.api.transfers

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.*
import ru.yandex.intranet.d.backend.service.provider_proto.*
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsDao
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.accounts.AccountsSpacesDao
import ru.yandex.intranet.d.dao.folders.FolderDao
import ru.yandex.intranet.d.dao.loans.*
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.dao.users.AbcServiceMemberDao
import ru.yandex.intranet.d.dao.users.UsersDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.datasource.model.YdbTxSession
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.kotlin.getOrNull
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountReserveType
import ru.yandex.intranet.d.model.accounts.AccountSpaceModel
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.model.folders.FolderModel
import ru.yandex.intranet.d.model.folders.FolderType
import ru.yandex.intranet.d.model.loans.*
import ru.yandex.intranet.d.model.providers.*
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.model.users.AbcServiceMemberModel
import ru.yandex.intranet.d.model.users.AbcServiceMemberState
import ru.yandex.intranet.d.model.users.UserServiceRoles
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestSubtypeDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto
import ru.yandex.intranet.d.web.model.transfers.api.*
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Transfer requests public API loans test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class PublicApiTransferLoansTest(@Autowired private val webClient: WebTestClient,
                                 @Autowired private val stubProviderService: StubProviderService,
                                 @Autowired private val tableClient: YdbTableClient,
                                 @Autowired private val providersDao: ProvidersDao,
                                 @Autowired private val resourceTypesDao: ResourceTypesDao,
                                 @Autowired private val resourceSegmentationsDao: ResourceSegmentationsDao,
                                 @Autowired private val resourceSegmentsDao: ResourceSegmentsDao,
                                 @Autowired private val resourcesDao: ResourcesDao,
                                 @Autowired private val quotasDao: QuotasDao,
                                 @Autowired private val folderDao: FolderDao,
                                 @Autowired private val accountsDao: AccountsDao,
                                 @Autowired private val accountsQuotasDao: AccountsQuotasDao,
                                 @Autowired private val accountsSpacesDao: AccountsSpacesDao,
                                 @Autowired private val loansDao: LoansDao,
                                 @Autowired private val pendingLoansDao: PendingLoansDao,
                                 @Autowired private val serviceLoansInDao: ServiceLoansInDao,
                                 @Autowired private val serviceLoansOutDao: ServiceLoansOutDao,
                                 @Autowired private val serviceLoansBalanceDao: ServiceLoansBalanceDao,
                                 @Autowired private val loansHistoryDao: LoansHistoryDao,
                                 @Autowired private val usersDao: UsersDao,
                                 @Autowired private val abcServiceMemberDao: AbcServiceMemberDao,
                                 @Value("\${abc.roles.responsibleOfProvider}")
                                 @Autowired val responsibleOfProvider: Long) {

    private suspend fun attachProviderAdmin(
        it: YdbTxSession,
        provider: ProviderModel,
        userStaffId: Long,
    ) {
        val maxId = abcServiceMemberDao.getMaxId(it).awaitSingle().getOrNull() ?: 0L
        abcServiceMemberDao.upsertManyRetryable(it, listOf(
            AbcServiceMemberModel.newBuilder()
            .id(maxId + 1)
            .serviceId(provider.serviceId)
            .roleId(responsibleOfProvider)
            .staffId(userStaffId)
            .state(AbcServiceMemberState.ACTIVE)
            .build())).awaitSingleOrNull()
        val user = usersDao.getByStaffId(it, userStaffId, Tenants.DEFAULT_TENANT_ID).awaitSingle().orElseThrow()
        val roles = user.roles.toMutableMap()
        val providerServices = roles[UserServiceRoles.RESPONSIBLE_OF_PROVIDER] ?: setOf()
        roles[UserServiceRoles.RESPONSIBLE_OF_PROVIDER] = providerServices union setOf(provider.serviceId)
        usersDao.upsertUserRetryable(it, user.copyBuilder().roles(roles).build()).awaitSingleOrNull()
    }

    @Test
    fun testPeerToPeerBorrowNoAccountsSpaces(): Unit = runBlocking {
        val folderOne = folderModel(TestServices.TEST_SERVICE_ID_DISPENSER)
        val folderTwo = folderModel(TestServices.TEST_SERVICE_ID_D)
        val provider = providerModel(false)
        val resourceType = resourceTypeModel(provider.id, "cpu", UnitsEnsembleIds.CPU_UNITS_ID)
        val resource = resourceModel(provider.id, "cpu", resourceType.id, emptyMap(), UnitsEnsembleIds.CPU_UNITS_ID,
            setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, null)
        val quotaOne = quotaModel(provider.id, resource.id, folderOne.id, 10000L, 0L)
        val quotaTwo = quotaModel(provider.id, resource.id, folderTwo.id, 1000L, 0L)
        val accountOne = accountModel(provider.id, null, folderOne.id, "testOne", null)
        val accountTwo = accountModel(provider.id, null, folderTwo.id, "testTwo", null)
        val provisionOne = accountQuotaModel(provider.id, resource.id, folderOne.id, accountOne.id, 10000L, 0L)
        val provisionTwo = accountQuotaModel(provider.id, resource.id, folderTwo.id, accountTwo.id, 1000L, 0L)
        dbSessionRetryable(tableClient) {
            folderDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(folderOne, folderTwo)).awaitSingleOrNull()
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
            resourceTypesDao.upsertResourceTypeRetryable(rwSingleRetryableCommit(), resourceType).awaitSingleOrNull()
            resourcesDao.upsertResourceRetryable(rwSingleRetryableCommit(), resource).awaitSingleOrNull()
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(quotaOne, quotaTwo)).awaitSingleOrNull()
            accountsDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(accountOne, accountTwo)).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(provisionOne, provisionTwo)).awaitSingleOrNull()
        }
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val dueDate = LocalDate.now().plusDays(2)
        val createTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = TransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", transferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER, transferRequestResult.requestSubtype)
        Assertions.assertTrue(transferRequestResult.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.first()
        val loan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), loanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val pendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                loan.dueAtTimestamp, loan.id))
        }!!
        val loanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val loanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val loanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 1).first()
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, loan.status)
        Assertions.assertEquals(LoanType.PEER_TO_PEER, loan.type)
        Assertions.assertEquals(dueDate, loan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, loan.requestedBy.user)
        Assertions.assertTrue(loan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, loan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, pendingLoan.dueAt.localDate)
        Assertions.assertEquals(loan.id, loanIn.loanId)
        Assertions.assertEquals(loan.id, loanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, loanHistory.eventType)
    }

    @Test
    fun testPeerToPeerBorrowWithAccountsSpaces(): Unit = runBlocking {
        val folderOne = folderModel(TestServices.TEST_SERVICE_ID_DISPENSER)
        val folderTwo = folderModel(TestServices.TEST_SERVICE_ID_D)
        val provider = providerModel(true)
        val segmentationOne = resourceSegmentationModel(provider.id, "cluster")
        val segmentationTwo = resourceSegmentationModel(provider.id, "scope")
        val segmentOne = resourceSegmentModel(segmentationOne.id, "sas")
        val segmentTwo = resourceSegmentModel(segmentationTwo.id, "compute")
        val accountsSpace = accountsSpaceModel(provider.id, "sas", mapOf(segmentationOne.id to segmentOne.id))
        val resourceType = resourceTypeModel(provider.id, "cpu", UnitsEnsembleIds.CPU_UNITS_ID)
        val resource = resourceModel(provider.id, "cpu", resourceType.id, mapOf(segmentationOne.id to segmentOne.id,
            segmentationTwo.id to segmentTwo.id), UnitsEnsembleIds.CPU_UNITS_ID,
            setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, accountsSpace.id)
        val quotaOne = quotaModel(provider.id, resource.id, folderOne.id, 10000L, 0L)
        val quotaTwo = quotaModel(provider.id, resource.id, folderTwo.id, 1000L, 0L)
        val accountOne = accountModel(provider.id, accountsSpace.id, folderOne.id, "testOne", null)
        val accountTwo = accountModel(provider.id, accountsSpace.id, folderTwo.id, "testTwo", null)
        val provisionOne = accountQuotaModel(provider.id, resource.id, folderOne.id, accountOne.id, 10000L, 0L)
        val provisionTwo = accountQuotaModel(provider.id, resource.id, folderTwo.id, accountTwo.id, 1000L, 0L)
        dbSessionRetryable(tableClient) {
            folderDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(folderOne, folderTwo)).awaitSingleOrNull()
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
            resourceSegmentationsDao.upsertResourceSegmentationsRetryable(rwSingleRetryableCommit(),
                listOf(segmentationOne, segmentationTwo)).awaitSingleOrNull()
            resourceSegmentsDao.upsertResourceSegmentsRetryable(rwSingleRetryableCommit(), listOf(segmentOne, segmentTwo))
                .awaitSingleOrNull()
            accountsSpacesDao.upsertOneRetryable(rwSingleRetryableCommit(), accountsSpace).awaitSingleOrNull()
            resourceTypesDao.upsertResourceTypeRetryable(rwSingleRetryableCommit(), resourceType).awaitSingleOrNull()
            resourcesDao.upsertResourceRetryable(rwSingleRetryableCommit(), resource).awaitSingleOrNull()
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(quotaOne, quotaTwo)).awaitSingleOrNull()
            accountsDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(accountOne, accountTwo)).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(provisionOne, provisionTwo)).awaitSingleOrNull()
        }
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val dueDate = LocalDate.now().plusDays(2)
        val createTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = TransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", transferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER, transferRequestResult.requestSubtype)
        Assertions.assertTrue(transferRequestResult.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.first()
        val loan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), loanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val pendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                loan.dueAtTimestamp, loan.id))
        }!!
        val loanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val loanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val loanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 1).first()
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, loan.status)
        Assertions.assertEquals(LoanType.PEER_TO_PEER, loan.type)
        Assertions.assertEquals(dueDate, loan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, loan.requestedBy.user)
        Assertions.assertTrue(loan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, loan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, pendingLoan.dueAt.localDate)
        Assertions.assertEquals(loan.id, loanIn.loanId)
        Assertions.assertEquals(loan.id, loanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, loanHistory.eventType)
    }

    @Test
    fun testProviderReserveBorrowNoAccountsSpaces(): Unit = runBlocking {
        val folderOne = folderModel(TestServices.TEST_SERVICE_ID_DISPENSER)
        val folderTwo = folderModel(TestServices.TEST_SERVICE_ID_D)
        val provider = providerModel(false)
        val resourceType = resourceTypeModel(provider.id, "cpu", UnitsEnsembleIds.CPU_UNITS_ID)
        val resource = resourceModel(provider.id, "cpu", resourceType.id, emptyMap(), UnitsEnsembleIds.CPU_UNITS_ID,
            setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, null)
        val quotaOne = quotaModel(provider.id, resource.id, folderOne.id, 10000L, 0L)
        val quotaTwo = quotaModel(provider.id, resource.id, folderTwo.id, 1000L, 0L)
        val accountOne = accountModel(provider.id, null, folderOne.id, "testOne",
            AccountReserveType.PROVIDER)
        val accountTwo = accountModel(provider.id, null, folderTwo.id, "testTwo", null)
        val provisionOne = accountQuotaModel(provider.id, resource.id, folderOne.id, accountOne.id, 10000L, 0L)
        val provisionTwo = accountQuotaModel(provider.id, resource.id, folderTwo.id, accountTwo.id, 1000L, 0L)
        dbSessionRetryable(tableClient) {
            folderDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(folderOne, folderTwo)).awaitSingleOrNull()
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
            resourceTypesDao.upsertResourceTypeRetryable(rwSingleRetryableCommit(), resourceType).awaitSingleOrNull()
            resourcesDao.upsertResourceRetryable(rwSingleRetryableCommit(), resource).awaitSingleOrNull()
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(quotaOne, quotaTwo)).awaitSingleOrNull()
            accountsDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(accountOne, accountTwo)).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(provisionOne, provisionTwo)).awaitSingleOrNull()
            attachProviderAdmin(rwSingleRetryableCommit(), provider, TestUsers.SERVICE_1_QUOTA_MANAGER_STAFF_ID)
        }
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val dueDate = LocalDate.now().plusDays(2)
        val createTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = TransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", transferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER, transferRequestResult.requestSubtype)
        Assertions.assertTrue(transferRequestResult.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.first()
        val loan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), loanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val pendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                loan.dueAtTimestamp, loan.id))
        }!!
        val loanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val loanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val loanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 1).first()
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, loan.status)
        Assertions.assertEquals(LoanType.PROVIDER_RESERVE, loan.type)
        Assertions.assertEquals(dueDate, loan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, loan.requestedBy.user)
        Assertions.assertTrue(loan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, loan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, pendingLoan.dueAt.localDate)
        Assertions.assertEquals(loan.id, loanIn.loanId)
        Assertions.assertEquals(loan.id, loanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, loanHistory.eventType)
    }

    @Test
    fun testProviderReserveBorrowWithAccountsSpaces(): Unit = runBlocking {
        val folderOne = folderModel(TestServices.TEST_SERVICE_ID_DISPENSER)
        val folderTwo = folderModel(TestServices.TEST_SERVICE_ID_D)
        val provider = providerModel(true)
        val segmentationOne = resourceSegmentationModel(provider.id, "cluster")
        val segmentationTwo = resourceSegmentationModel(provider.id, "scope")
        val segmentOne = resourceSegmentModel(segmentationOne.id, "sas")
        val segmentTwo = resourceSegmentModel(segmentationTwo.id, "compute")
        val accountsSpace = accountsSpaceModel(provider.id, "sas", mapOf(segmentationOne.id to segmentOne.id))
        val resourceType = resourceTypeModel(provider.id, "cpu", UnitsEnsembleIds.CPU_UNITS_ID)
        val resource = resourceModel(provider.id, "cpu", resourceType.id, mapOf(segmentationOne.id to segmentOne.id,
            segmentationTwo.id to segmentTwo.id), UnitsEnsembleIds.CPU_UNITS_ID,
            setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, accountsSpace.id)
        val quotaOne = quotaModel(provider.id, resource.id, folderOne.id, 10000L, 0L)
        val quotaTwo = quotaModel(provider.id, resource.id, folderTwo.id, 1000L, 0L)
        val accountOne = accountModel(provider.id, accountsSpace.id, folderOne.id, "testOne",
            AccountReserveType.PROVIDER)
        val accountTwo = accountModel(provider.id, accountsSpace.id, folderTwo.id, "testTwo", null)
        val provisionOne = accountQuotaModel(provider.id, resource.id, folderOne.id, accountOne.id, 10000L, 0L)
        val provisionTwo = accountQuotaModel(provider.id, resource.id, folderTwo.id, accountTwo.id, 1000L, 0L)
        dbSessionRetryable(tableClient) {
            folderDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(folderOne, folderTwo)).awaitSingleOrNull()
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
            resourceSegmentationsDao.upsertResourceSegmentationsRetryable(rwSingleRetryableCommit(),
                listOf(segmentationOne, segmentationTwo)).awaitSingleOrNull()
            resourceSegmentsDao.upsertResourceSegmentsRetryable(rwSingleRetryableCommit(), listOf(segmentOne, segmentTwo))
                .awaitSingleOrNull()
            accountsSpacesDao.upsertOneRetryable(rwSingleRetryableCommit(), accountsSpace).awaitSingleOrNull()
            resourceTypesDao.upsertResourceTypeRetryable(rwSingleRetryableCommit(), resourceType).awaitSingleOrNull()
            resourcesDao.upsertResourceRetryable(rwSingleRetryableCommit(), resource).awaitSingleOrNull()
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(quotaOne, quotaTwo)).awaitSingleOrNull()
            accountsDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(accountOne, accountTwo)).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(provisionOne, provisionTwo)).awaitSingleOrNull()
            attachProviderAdmin(rwSingleRetryableCommit(), provider, TestUsers.SERVICE_1_QUOTA_MANAGER_STAFF_ID)
        }
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val dueDate = LocalDate.now().plusDays(2)
        val createTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = TransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", transferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER, transferRequestResult.requestSubtype)
        Assertions.assertTrue(transferRequestResult.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.first()
        val loan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), loanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val pendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                loan.dueAtTimestamp, loan.id))
        }!!
        val loanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val loanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val loanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 1).first()
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, loan.status)
        Assertions.assertEquals(LoanType.PROVIDER_RESERVE, loan.type)
        Assertions.assertEquals(dueDate, loan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, loan.requestedBy.user)
        Assertions.assertTrue(loan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, loan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, pendingLoan.dueAt.localDate)
        Assertions.assertEquals(loan.id, loanIn.loanId)
        Assertions.assertEquals(loan.id, loanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, loanHistory.eventType)
    }

    @Test
    fun testPeerToPeerBorrowAndPayOffNoAccountsSpaces(): Unit = runBlocking {
        val folderOne = folderModel(TestServices.TEST_SERVICE_ID_DISPENSER)
        val folderTwo = folderModel(TestServices.TEST_SERVICE_ID_D)
        val provider = providerModel(false)
        val resourceType = resourceTypeModel(provider.id, "cpu", UnitsEnsembleIds.CPU_UNITS_ID)
        val resource = resourceModel(provider.id, "cpu", resourceType.id, emptyMap(), UnitsEnsembleIds.CPU_UNITS_ID,
            setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, null)
        val quotaOne = quotaModel(provider.id, resource.id, folderOne.id, 10000L, 0L)
        val quotaTwo = quotaModel(provider.id, resource.id, folderTwo.id, 1000L, 0L)
        val accountOne = accountModel(provider.id, null, folderOne.id, "testOne", null)
        val accountTwo = accountModel(provider.id, null, folderTwo.id, "testTwo", null)
        val provisionOne = accountQuotaModel(provider.id, resource.id, folderOne.id, accountOne.id, 10000L, 0L)
        val provisionTwo = accountQuotaModel(provider.id, resource.id, folderTwo.id, accountTwo.id, 1000L, 0L)
        dbSessionRetryable(tableClient) {
            folderDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(folderOne, folderTwo)).awaitSingleOrNull()
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
            resourceTypesDao.upsertResourceTypeRetryable(rwSingleRetryableCommit(), resourceType).awaitSingleOrNull()
            resourcesDao.upsertResourceRetryable(rwSingleRetryableCommit(), resource).awaitSingleOrNull()
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(quotaOne, quotaTwo)).awaitSingleOrNull()
            accountsDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(accountOne, accountTwo)).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(provisionOne, provisionTwo)).awaitSingleOrNull()
        }
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val dueDate = LocalDate.now().plusDays(2)
        val createTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = TransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", transferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER, transferRequestResult.requestSubtype)
        Assertions.assertTrue(transferRequestResult.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.first()
        val loan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), loanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val pendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                loan.dueAtTimestamp, loan.id))
        }!!
        val loanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val loanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val loanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 1).first()
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, loan.status)
        Assertions.assertEquals(LoanType.PEER_TO_PEER, loan.type)
        Assertions.assertEquals(dueDate, loan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, loan.requestedBy.user)
        Assertions.assertTrue(loan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, loan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, pendingLoan.dueAt.localDate)
        Assertions.assertEquals(loan.id, loanIn.loanId)
        Assertions.assertEquals(loan.id, loanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, loanHistory.eventType)
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val createPayOffTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountTwo.id,
                    sourceFolderId = folderTwo.id,
                    destinationAccountId = accountOne.id,
                    destinationFolderId = folderOne.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = null,
                payOffParameters = TransferLoanPayOffParametersDto(
                    loanId = loan.id
                )
            ))
            .build()
        val payOffTransferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createPayOffTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffTransferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", payOffTransferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffLoanId = payOffTransferRequestResult.loanMeta.get().payOffMeta!!.loanId
        val payOffLoan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), payOffLoanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val payOffPendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                payOffLoan.dueAtTimestamp, payOffLoan.id))
        }
        val payOffLoanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.SETTLED, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.SETTLED, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val payOffLoanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val payOffLoanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 2)
        }!!
        Assertions.assertEquals(LoanStatus.SETTLED, payOffLoan.status)
        Assertions.assertEquals(LoanType.PEER_TO_PEER, payOffLoan.type)
        Assertions.assertEquals(dueDate, payOffLoan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, payOffLoan.requestedBy.user)
        Assertions.assertTrue(payOffLoan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, payOffLoan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.payOffAmounts.amounts[0].amount)
        Assertions.assertTrue(payOffLoan.dueAmounts.amounts.isEmpty())
        Assertions.assertNull(payOffPendingLoan)
        Assertions.assertEquals(payOffLoan.id, payOffLoanIn.loanId)
        Assertions.assertEquals(payOffLoan.id, payOffLoanOut.loanId)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_SETTLED, payOffLoanHistory[0].eventType)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, payOffLoanHistory[1].eventType)
    }

    @Test
    fun testPeerToPeerBorrowAndPayOffWithAccountsSpaces(): Unit = runBlocking {
        val folderOne = folderModel(TestServices.TEST_SERVICE_ID_DISPENSER)
        val folderTwo = folderModel(TestServices.TEST_SERVICE_ID_D)
        val provider = providerModel(true)
        val segmentationOne = resourceSegmentationModel(provider.id, "cluster")
        val segmentationTwo = resourceSegmentationModel(provider.id, "scope")
        val segmentOne = resourceSegmentModel(segmentationOne.id, "sas")
        val segmentTwo = resourceSegmentModel(segmentationTwo.id, "compute")
        val accountsSpace = accountsSpaceModel(provider.id, "sas", mapOf(segmentationOne.id to segmentOne.id))
        val resourceType = resourceTypeModel(provider.id, "cpu", UnitsEnsembleIds.CPU_UNITS_ID)
        val resource = resourceModel(provider.id, "cpu", resourceType.id, mapOf(segmentationOne.id to segmentOne.id,
            segmentationTwo.id to segmentTwo.id), UnitsEnsembleIds.CPU_UNITS_ID,
            setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, accountsSpace.id)
        val quotaOne = quotaModel(provider.id, resource.id, folderOne.id, 10000L, 0L)
        val quotaTwo = quotaModel(provider.id, resource.id, folderTwo.id, 1000L, 0L)
        val accountOne = accountModel(provider.id, accountsSpace.id, folderOne.id, "testOne", null)
        val accountTwo = accountModel(provider.id, accountsSpace.id, folderTwo.id, "testTwo", null)
        val provisionOne = accountQuotaModel(provider.id, resource.id, folderOne.id, accountOne.id, 10000L, 0L)
        val provisionTwo = accountQuotaModel(provider.id, resource.id, folderTwo.id, accountTwo.id, 1000L, 0L)
        dbSessionRetryable(tableClient) {
            folderDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(folderOne, folderTwo)).awaitSingleOrNull()
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
            resourceSegmentationsDao.upsertResourceSegmentationsRetryable(rwSingleRetryableCommit(),
                listOf(segmentationOne, segmentationTwo)).awaitSingleOrNull()
            resourceSegmentsDao.upsertResourceSegmentsRetryable(rwSingleRetryableCommit(), listOf(segmentOne, segmentTwo))
                .awaitSingleOrNull()
            accountsSpacesDao.upsertOneRetryable(rwSingleRetryableCommit(), accountsSpace).awaitSingleOrNull()
            resourceTypesDao.upsertResourceTypeRetryable(rwSingleRetryableCommit(), resourceType).awaitSingleOrNull()
            resourcesDao.upsertResourceRetryable(rwSingleRetryableCommit(), resource).awaitSingleOrNull()
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(quotaOne, quotaTwo)).awaitSingleOrNull()
            accountsDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(accountOne, accountTwo)).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(provisionOne, provisionTwo)).awaitSingleOrNull()
        }
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val dueDate = LocalDate.now().plusDays(2)
        val createTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = TransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", transferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER, transferRequestResult.requestSubtype)
        Assertions.assertTrue(transferRequestResult.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.first()
        val loan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), loanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val pendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                loan.dueAtTimestamp, loan.id))
        }!!
        val loanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val loanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val loanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 1).first()
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, loan.status)
        Assertions.assertEquals(LoanType.PEER_TO_PEER, loan.type)
        Assertions.assertEquals(dueDate, loan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, loan.requestedBy.user)
        Assertions.assertTrue(loan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, loan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, pendingLoan.dueAt.localDate)
        Assertions.assertEquals(loan.id, loanIn.loanId)
        Assertions.assertEquals(loan.id, loanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, loanHistory.eventType)
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val createPayOffTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountTwo.id,
                    sourceFolderId = folderTwo.id,
                    destinationAccountId = accountOne.id,
                    destinationFolderId = folderOne.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = null,
                payOffParameters = TransferLoanPayOffParametersDto(
                    loanId = loan.id
                )
            ))
            .build()
        val payOffTransferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createPayOffTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffTransferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", payOffTransferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffLoanId = payOffTransferRequestResult.loanMeta.get().payOffMeta!!.loanId
        val payOffLoan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), payOffLoanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val payOffPendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                payOffLoan.dueAtTimestamp, payOffLoan.id))
        }
        val payOffLoanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.SETTLED, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.SETTLED, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val payOffLoanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val payOffLoanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 2)
        }!!
        Assertions.assertEquals(LoanStatus.SETTLED, payOffLoan.status)
        Assertions.assertEquals(LoanType.PEER_TO_PEER, payOffLoan.type)
        Assertions.assertEquals(dueDate, payOffLoan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, payOffLoan.requestedBy.user)
        Assertions.assertTrue(payOffLoan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, payOffLoan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.payOffAmounts.amounts[0].amount)
        Assertions.assertTrue(payOffLoan.dueAmounts.amounts.isEmpty())
        Assertions.assertNull(payOffPendingLoan)
        Assertions.assertEquals(payOffLoan.id, payOffLoanIn.loanId)
        Assertions.assertEquals(payOffLoan.id, payOffLoanOut.loanId)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_SETTLED, payOffLoanHistory[0].eventType)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, payOffLoanHistory[1].eventType)
    }

    @Test
    fun testProviderReserveBorrowAndPayOffNoAccountsSpaces(): Unit = runBlocking {
        val folderOne = folderModel(TestServices.TEST_SERVICE_ID_DISPENSER)
        val folderTwo = folderModel(TestServices.TEST_SERVICE_ID_D)
        val provider = providerModel(false)
        val resourceType = resourceTypeModel(provider.id, "cpu", UnitsEnsembleIds.CPU_UNITS_ID)
        val resource = resourceModel(provider.id, "cpu", resourceType.id, emptyMap(), UnitsEnsembleIds.CPU_UNITS_ID,
            setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, null)
        val quotaOne = quotaModel(provider.id, resource.id, folderOne.id, 10000L, 0L)
        val quotaTwo = quotaModel(provider.id, resource.id, folderTwo.id, 1000L, 0L)
        val accountOne = accountModel(provider.id, null, folderOne.id, "testOne",
            AccountReserveType.PROVIDER)
        val accountTwo = accountModel(provider.id, null, folderTwo.id, "testTwo", null)
        val provisionOne = accountQuotaModel(provider.id, resource.id, folderOne.id, accountOne.id, 10000L, 0L)
        val provisionTwo = accountQuotaModel(provider.id, resource.id, folderTwo.id, accountTwo.id, 1000L, 0L)
        dbSessionRetryable(tableClient) {
            folderDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(folderOne, folderTwo)).awaitSingleOrNull()
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
            resourceTypesDao.upsertResourceTypeRetryable(rwSingleRetryableCommit(), resourceType).awaitSingleOrNull()
            resourcesDao.upsertResourceRetryable(rwSingleRetryableCommit(), resource).awaitSingleOrNull()
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(quotaOne, quotaTwo)).awaitSingleOrNull()
            accountsDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(accountOne, accountTwo)).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(provisionOne, provisionTwo)).awaitSingleOrNull()
            attachProviderAdmin(rwSingleRetryableCommit(), provider, TestUsers.SERVICE_1_QUOTA_MANAGER_STAFF_ID)
        }
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val dueDate = LocalDate.now().plusDays(2)
        val createTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = TransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", transferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER, transferRequestResult.requestSubtype)
        Assertions.assertTrue(transferRequestResult.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.first()
        val loan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), loanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val pendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                loan.dueAtTimestamp, loan.id))
        }!!
        val loanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val loanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val loanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 1).first()
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, loan.status)
        Assertions.assertEquals(LoanType.PROVIDER_RESERVE, loan.type)
        Assertions.assertEquals(dueDate, loan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, loan.requestedBy.user)
        Assertions.assertTrue(loan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, loan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, pendingLoan.dueAt.localDate)
        Assertions.assertEquals(loan.id, loanIn.loanId)
        Assertions.assertEquals(loan.id, loanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, loanHistory.eventType)
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val createPayOffTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountTwo.id,
                    sourceFolderId = folderTwo.id,
                    destinationAccountId = accountOne.id,
                    destinationFolderId = folderOne.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = null,
                payOffParameters = TransferLoanPayOffParametersDto(
                    loanId = loan.id
                )
            ))
            .build()
        val payOffTransferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createPayOffTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffTransferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", payOffTransferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffLoanId = payOffTransferRequestResult.loanMeta.get().payOffMeta!!.loanId
        val payOffLoan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), payOffLoanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val payOffPendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                payOffLoan.dueAtTimestamp, payOffLoan.id))
        }
        val payOffLoanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.SETTLED, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.SETTLED, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val payOffLoanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val payOffLoanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 2)
        }!!
        Assertions.assertEquals(LoanStatus.SETTLED, payOffLoan.status)
        Assertions.assertEquals(LoanType.PROVIDER_RESERVE, payOffLoan.type)
        Assertions.assertEquals(dueDate, payOffLoan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, payOffLoan.requestedBy.user)
        Assertions.assertTrue(payOffLoan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, payOffLoan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.payOffAmounts.amounts[0].amount)
        Assertions.assertTrue(payOffLoan.dueAmounts.amounts.isEmpty())
        Assertions.assertNull(payOffPendingLoan)
        Assertions.assertEquals(payOffLoan.id, payOffLoanIn.loanId)
        Assertions.assertEquals(payOffLoan.id, payOffLoanOut.loanId)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_SETTLED, payOffLoanHistory[0].eventType)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, payOffLoanHistory[1].eventType)
    }

    @Test
    fun testProviderReserveBorrowAndPayOffWithAccountsSpaces(): Unit = runBlocking {
        val folderOne = folderModel(TestServices.TEST_SERVICE_ID_DISPENSER)
        val folderTwo = folderModel(TestServices.TEST_SERVICE_ID_D)
        val provider = providerModel(true)
        val segmentationOne = resourceSegmentationModel(provider.id, "cluster")
        val segmentationTwo = resourceSegmentationModel(provider.id, "scope")
        val segmentOne = resourceSegmentModel(segmentationOne.id, "sas")
        val segmentTwo = resourceSegmentModel(segmentationTwo.id, "compute")
        val accountsSpace = accountsSpaceModel(provider.id, "sas", mapOf(segmentationOne.id to segmentOne.id))
        val resourceType = resourceTypeModel(provider.id, "cpu", UnitsEnsembleIds.CPU_UNITS_ID)
        val resource = resourceModel(provider.id, "cpu", resourceType.id, mapOf(segmentationOne.id to segmentOne.id,
            segmentationTwo.id to segmentTwo.id), UnitsEnsembleIds.CPU_UNITS_ID,
            setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, accountsSpace.id)
        val quotaOne = quotaModel(provider.id, resource.id, folderOne.id, 10000L, 0L)
        val quotaTwo = quotaModel(provider.id, resource.id, folderTwo.id, 1000L, 0L)
        val accountOne = accountModel(provider.id, accountsSpace.id, folderOne.id, "testOne",
            AccountReserveType.PROVIDER)
        val accountTwo = accountModel(provider.id, accountsSpace.id, folderTwo.id, "testTwo", null)
        val provisionOne = accountQuotaModel(provider.id, resource.id, folderOne.id, accountOne.id, 10000L, 0L)
        val provisionTwo = accountQuotaModel(provider.id, resource.id, folderTwo.id, accountTwo.id, 1000L, 0L)
        dbSessionRetryable(tableClient) {
            folderDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(folderOne, folderTwo)).awaitSingleOrNull()
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
            resourceSegmentationsDao.upsertResourceSegmentationsRetryable(rwSingleRetryableCommit(),
                listOf(segmentationOne, segmentationTwo)).awaitSingleOrNull()
            resourceSegmentsDao.upsertResourceSegmentsRetryable(rwSingleRetryableCommit(), listOf(segmentOne, segmentTwo))
                .awaitSingleOrNull()
            accountsSpacesDao.upsertOneRetryable(rwSingleRetryableCommit(), accountsSpace).awaitSingleOrNull()
            resourceTypesDao.upsertResourceTypeRetryable(rwSingleRetryableCommit(), resourceType).awaitSingleOrNull()
            resourcesDao.upsertResourceRetryable(rwSingleRetryableCommit(), resource).awaitSingleOrNull()
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(quotaOne, quotaTwo)).awaitSingleOrNull()
            accountsDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(accountOne, accountTwo)).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(provisionOne, provisionTwo)).awaitSingleOrNull()
            attachProviderAdmin(rwSingleRetryableCommit(), provider, TestUsers.SERVICE_1_QUOTA_MANAGER_STAFF_ID)
        }
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val dueDate = LocalDate.now().plusDays(2)
        val createTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = TransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", transferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER, transferRequestResult.requestSubtype)
        Assertions.assertTrue(transferRequestResult.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.first()
        val loan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), loanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val pendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                loan.dueAtTimestamp, loan.id))
        }!!
        val loanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val loanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val loanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 1).first()
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, loan.status)
        Assertions.assertEquals(LoanType.PROVIDER_RESERVE, loan.type)
        Assertions.assertEquals(dueDate, loan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, loan.requestedBy.user)
        Assertions.assertTrue(loan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, loan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, pendingLoan.dueAt.localDate)
        Assertions.assertEquals(loan.id, loanIn.loanId)
        Assertions.assertEquals(loan.id, loanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, loanHistory.eventType)
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val createPayOffTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountTwo.id,
                    sourceFolderId = folderTwo.id,
                    destinationAccountId = accountOne.id,
                    destinationFolderId = folderOne.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = null,
                payOffParameters = TransferLoanPayOffParametersDto(
                    loanId = loan.id
                )
            ))
            .build()
        val payOffTransferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createPayOffTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffTransferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", payOffTransferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffLoanId = payOffTransferRequestResult.loanMeta.get().payOffMeta!!.loanId
        val payOffLoan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), payOffLoanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val payOffPendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                payOffLoan.dueAtTimestamp, payOffLoan.id))
        }
        val payOffLoanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.SETTLED, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.SETTLED, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val payOffLoanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val payOffLoanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 2)
        }!!
        Assertions.assertEquals(LoanStatus.SETTLED, payOffLoan.status)
        Assertions.assertEquals(LoanType.PROVIDER_RESERVE, payOffLoan.type)
        Assertions.assertEquals(dueDate, payOffLoan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, payOffLoan.requestedBy.user)
        Assertions.assertTrue(payOffLoan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, payOffLoan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.payOffAmounts.amounts[0].amount)
        Assertions.assertTrue(payOffLoan.dueAmounts.amounts.isEmpty())
        Assertions.assertNull(payOffPendingLoan)
        Assertions.assertEquals(payOffLoan.id, payOffLoanIn.loanId)
        Assertions.assertEquals(payOffLoan.id, payOffLoanOut.loanId)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_SETTLED, payOffLoanHistory[0].eventType)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, payOffLoanHistory[1].eventType)
    }

    @Test
    fun testPeerToPeerBorrowAndPartialPayOffNoAccountsSpaces(): Unit = runBlocking {
        val folderOne = folderModel(TestServices.TEST_SERVICE_ID_DISPENSER)
        val folderTwo = folderModel(TestServices.TEST_SERVICE_ID_D)
        val provider = providerModel(false)
        val resourceType = resourceTypeModel(provider.id, "cpu", UnitsEnsembleIds.CPU_UNITS_ID)
        val resource = resourceModel(provider.id, "cpu", resourceType.id, emptyMap(), UnitsEnsembleIds.CPU_UNITS_ID,
            setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, null)
        val quotaOne = quotaModel(provider.id, resource.id, folderOne.id, 10000L, 0L)
        val quotaTwo = quotaModel(provider.id, resource.id, folderTwo.id, 1000L, 0L)
        val accountOne = accountModel(provider.id, null, folderOne.id, "testOne", null)
        val accountTwo = accountModel(provider.id, null, folderTwo.id, "testTwo", null)
        val provisionOne = accountQuotaModel(provider.id, resource.id, folderOne.id, accountOne.id, 10000L, 0L)
        val provisionTwo = accountQuotaModel(provider.id, resource.id, folderTwo.id, accountTwo.id, 1000L, 0L)
        dbSessionRetryable(tableClient) {
            folderDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(folderOne, folderTwo)).awaitSingleOrNull()
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
            resourceTypesDao.upsertResourceTypeRetryable(rwSingleRetryableCommit(), resourceType).awaitSingleOrNull()
            resourcesDao.upsertResourceRetryable(rwSingleRetryableCommit(), resource).awaitSingleOrNull()
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(quotaOne, quotaTwo)).awaitSingleOrNull()
            accountsDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(accountOne, accountTwo)).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(provisionOne, provisionTwo)).awaitSingleOrNull()
        }
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val dueDate = LocalDate.now().plusDays(2)
        val createTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = TransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", transferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER, transferRequestResult.requestSubtype)
        Assertions.assertTrue(transferRequestResult.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.first()
        val loan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), loanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val pendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                loan.dueAtTimestamp, loan.id))
        }!!
        val loanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val loanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val loanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 1).first()
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, loan.status)
        Assertions.assertEquals(LoanType.PEER_TO_PEER, loan.type)
        Assertions.assertEquals(dueDate, loan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, loan.requestedBy.user)
        Assertions.assertTrue(loan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, loan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, pendingLoan.dueAt.localDate)
        Assertions.assertEquals(loan.id, loanIn.loanId)
        Assertions.assertEquals(loan.id, loanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, loanHistory.eventType)
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val createPayOffTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountTwo.id,
                    sourceFolderId = folderTwo.id,
                    destinationAccountId = accountOne.id,
                    destinationFolderId = folderOne.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-1)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(1)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = null,
                payOffParameters = TransferLoanPayOffParametersDto(
                    loanId = loan.id
                )
            ))
            .build()
        val payOffTransferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createPayOffTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffTransferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", payOffTransferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffLoanId = payOffTransferRequestResult.loanMeta.get().payOffMeta!!.loanId
        val payOffLoan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), payOffLoanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val payOffPendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val payOffLoanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val payOffLoanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 2)
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, payOffLoan.status)
        Assertions.assertEquals(LoanType.PEER_TO_PEER, payOffLoan.type)
        Assertions.assertEquals(dueDate, payOffLoan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, payOffLoan.requestedBy.user)
        Assertions.assertTrue(payOffLoan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, payOffLoan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(4000), payOffLoan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, payOffPendingLoan.dueAt.localDate)
        Assertions.assertEquals(payOffLoan.id, payOffLoanIn.loanId)
        Assertions.assertEquals(payOffLoan.id, payOffLoanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(4000), payOffLoanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(4000), payOffLoanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_PAY_OFF, payOffLoanHistory[0].eventType)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, payOffLoanHistory[1].eventType)
    }

    @Test
    fun testPeerToPeerBorrowAndPartialPayOffWithAccountsSpaces(): Unit = runBlocking {
        val folderOne = folderModel(TestServices.TEST_SERVICE_ID_DISPENSER)
        val folderTwo = folderModel(TestServices.TEST_SERVICE_ID_D)
        val provider = providerModel(true)
        val segmentationOne = resourceSegmentationModel(provider.id, "cluster")
        val segmentationTwo = resourceSegmentationModel(provider.id, "scope")
        val segmentOne = resourceSegmentModel(segmentationOne.id, "sas")
        val segmentTwo = resourceSegmentModel(segmentationTwo.id, "compute")
        val accountsSpace = accountsSpaceModel(provider.id, "sas", mapOf(segmentationOne.id to segmentOne.id))
        val resourceType = resourceTypeModel(provider.id, "cpu", UnitsEnsembleIds.CPU_UNITS_ID)
        val resource = resourceModel(provider.id, "cpu", resourceType.id, mapOf(segmentationOne.id to segmentOne.id,
            segmentationTwo.id to segmentTwo.id), UnitsEnsembleIds.CPU_UNITS_ID,
            setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, accountsSpace.id)
        val quotaOne = quotaModel(provider.id, resource.id, folderOne.id, 10000L, 0L)
        val quotaTwo = quotaModel(provider.id, resource.id, folderTwo.id, 1000L, 0L)
        val accountOne = accountModel(provider.id, accountsSpace.id, folderOne.id, "testOne", null)
        val accountTwo = accountModel(provider.id, accountsSpace.id, folderTwo.id, "testTwo", null)
        val provisionOne = accountQuotaModel(provider.id, resource.id, folderOne.id, accountOne.id, 10000L, 0L)
        val provisionTwo = accountQuotaModel(provider.id, resource.id, folderTwo.id, accountTwo.id, 1000L, 0L)
        dbSessionRetryable(tableClient) {
            folderDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(folderOne, folderTwo)).awaitSingleOrNull()
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
            resourceSegmentationsDao.upsertResourceSegmentationsRetryable(rwSingleRetryableCommit(),
                listOf(segmentationOne, segmentationTwo)).awaitSingleOrNull()
            resourceSegmentsDao.upsertResourceSegmentsRetryable(rwSingleRetryableCommit(), listOf(segmentOne, segmentTwo))
                .awaitSingleOrNull()
            accountsSpacesDao.upsertOneRetryable(rwSingleRetryableCommit(), accountsSpace).awaitSingleOrNull()
            resourceTypesDao.upsertResourceTypeRetryable(rwSingleRetryableCommit(), resourceType).awaitSingleOrNull()
            resourcesDao.upsertResourceRetryable(rwSingleRetryableCommit(), resource).awaitSingleOrNull()
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(quotaOne, quotaTwo)).awaitSingleOrNull()
            accountsDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(accountOne, accountTwo)).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(provisionOne, provisionTwo)).awaitSingleOrNull()
        }
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val dueDate = LocalDate.now().plusDays(2)
        val createTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = TransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", transferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER, transferRequestResult.requestSubtype)
        Assertions.assertTrue(transferRequestResult.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.first()
        val loan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), loanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val pendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                loan.dueAtTimestamp, loan.id))
        }!!
        val loanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val loanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val loanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 1).first()
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, loan.status)
        Assertions.assertEquals(LoanType.PEER_TO_PEER, loan.type)
        Assertions.assertEquals(dueDate, loan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, loan.requestedBy.user)
        Assertions.assertTrue(loan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, loan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, pendingLoan.dueAt.localDate)
        Assertions.assertEquals(loan.id, loanIn.loanId)
        Assertions.assertEquals(loan.id, loanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, loanHistory.eventType)
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val createPayOffTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountTwo.id,
                    sourceFolderId = folderTwo.id,
                    destinationAccountId = accountOne.id,
                    destinationFolderId = folderOne.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-1)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(1)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = null,
                payOffParameters = TransferLoanPayOffParametersDto(
                    loanId = loan.id
                )
            ))
            .build()
        val payOffTransferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createPayOffTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffTransferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", payOffTransferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffLoanId = payOffTransferRequestResult.loanMeta.get().payOffMeta!!.loanId
        val payOffLoan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), payOffLoanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val payOffPendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val payOffLoanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val payOffLoanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 2)
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, payOffLoan.status)
        Assertions.assertEquals(LoanType.PEER_TO_PEER, payOffLoan.type)
        Assertions.assertEquals(dueDate, payOffLoan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, payOffLoan.requestedBy.user)
        Assertions.assertTrue(payOffLoan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, payOffLoan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(4000), payOffLoan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, payOffPendingLoan.dueAt.localDate)
        Assertions.assertEquals(payOffLoan.id, payOffLoanIn.loanId)
        Assertions.assertEquals(payOffLoan.id, payOffLoanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(4000), payOffLoanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(4000), payOffLoanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_PAY_OFF, payOffLoanHistory[0].eventType)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, payOffLoanHistory[1].eventType)
    }

    @Test
    fun testProviderReserveBorrowAndPartialPayOffNoAccountsSpaces(): Unit = runBlocking {
        val folderOne = folderModel(TestServices.TEST_SERVICE_ID_DISPENSER)
        val folderTwo = folderModel(TestServices.TEST_SERVICE_ID_D)
        val provider = providerModel(false)
        val resourceType = resourceTypeModel(provider.id, "cpu", UnitsEnsembleIds.CPU_UNITS_ID)
        val resource = resourceModel(provider.id, "cpu", resourceType.id, emptyMap(), UnitsEnsembleIds.CPU_UNITS_ID,
            setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, null)
        val quotaOne = quotaModel(provider.id, resource.id, folderOne.id, 10000L, 0L)
        val quotaTwo = quotaModel(provider.id, resource.id, folderTwo.id, 1000L, 0L)
        val accountOne = accountModel(provider.id, null, folderOne.id, "testOne",
            AccountReserveType.PROVIDER)
        val accountTwo = accountModel(provider.id, null, folderTwo.id, "testTwo", null)
        val provisionOne = accountQuotaModel(provider.id, resource.id, folderOne.id, accountOne.id, 10000L, 0L)
        val provisionTwo = accountQuotaModel(provider.id, resource.id, folderTwo.id, accountTwo.id, 1000L, 0L)
        dbSessionRetryable(tableClient) {
            folderDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(folderOne, folderTwo)).awaitSingleOrNull()
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
            resourceTypesDao.upsertResourceTypeRetryable(rwSingleRetryableCommit(), resourceType).awaitSingleOrNull()
            resourcesDao.upsertResourceRetryable(rwSingleRetryableCommit(), resource).awaitSingleOrNull()
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(quotaOne, quotaTwo)).awaitSingleOrNull()
            accountsDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(accountOne, accountTwo)).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(provisionOne, provisionTwo)).awaitSingleOrNull()
            attachProviderAdmin(rwSingleRetryableCommit(), provider, TestUsers.SERVICE_1_QUOTA_MANAGER_STAFF_ID)
        }
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val dueDate = LocalDate.now().plusDays(2)
        val createTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = TransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", transferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER, transferRequestResult.requestSubtype)
        Assertions.assertTrue(transferRequestResult.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.first()
        val loan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), loanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val pendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                loan.dueAtTimestamp, loan.id))
        }!!
        val loanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val loanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val loanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 1).first()
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, loan.status)
        Assertions.assertEquals(LoanType.PROVIDER_RESERVE, loan.type)
        Assertions.assertEquals(dueDate, loan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, loan.requestedBy.user)
        Assertions.assertTrue(loan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, loan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, pendingLoan.dueAt.localDate)
        Assertions.assertEquals(loan.id, loanIn.loanId)
        Assertions.assertEquals(loan.id, loanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, loanHistory.eventType)
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val createPayOffTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountTwo.id,
                    sourceFolderId = folderTwo.id,
                    destinationAccountId = accountOne.id,
                    destinationFolderId = folderOne.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-1)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(1)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = null,
                payOffParameters = TransferLoanPayOffParametersDto(
                    loanId = loan.id
                )
            ))
            .build()
        val payOffTransferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createPayOffTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffTransferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", payOffTransferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffLoanId = payOffTransferRequestResult.loanMeta.get().payOffMeta!!.loanId
        val payOffLoan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), payOffLoanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val payOffPendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val payOffLoanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val payOffLoanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 2)
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, payOffLoan.status)
        Assertions.assertEquals(LoanType.PROVIDER_RESERVE, payOffLoan.type)
        Assertions.assertEquals(dueDate, payOffLoan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, payOffLoan.requestedBy.user)
        Assertions.assertTrue(payOffLoan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, payOffLoan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(4000), payOffLoan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, payOffPendingLoan.dueAt.localDate)
        Assertions.assertEquals(payOffLoan.id, payOffLoanIn.loanId)
        Assertions.assertEquals(payOffLoan.id, payOffLoanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(4000), payOffLoanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(4000), payOffLoanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_PAY_OFF, payOffLoanHistory[0].eventType)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, payOffLoanHistory[1].eventType)
    }

    @Test
    fun testProviderReserveBorrowAndPartialPayOffWithAccountsSpaces(): Unit = runBlocking {
        val folderOne = folderModel(TestServices.TEST_SERVICE_ID_DISPENSER)
        val folderTwo = folderModel(TestServices.TEST_SERVICE_ID_D)
        val provider = providerModel(true)
        val segmentationOne = resourceSegmentationModel(provider.id, "cluster")
        val segmentationTwo = resourceSegmentationModel(provider.id, "scope")
        val segmentOne = resourceSegmentModel(segmentationOne.id, "sas")
        val segmentTwo = resourceSegmentModel(segmentationTwo.id, "compute")
        val accountsSpace = accountsSpaceModel(provider.id, "sas", mapOf(segmentationOne.id to segmentOne.id))
        val resourceType = resourceTypeModel(provider.id, "cpu", UnitsEnsembleIds.CPU_UNITS_ID)
        val resource = resourceModel(provider.id, "cpu", resourceType.id, mapOf(segmentationOne.id to segmentOne.id,
            segmentationTwo.id to segmentTwo.id), UnitsEnsembleIds.CPU_UNITS_ID,
            setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, accountsSpace.id)
        val quotaOne = quotaModel(provider.id, resource.id, folderOne.id, 10000L, 0L)
        val quotaTwo = quotaModel(provider.id, resource.id, folderTwo.id, 1000L, 0L)
        val accountOne = accountModel(provider.id, accountsSpace.id, folderOne.id, "testOne",
            AccountReserveType.PROVIDER)
        val accountTwo = accountModel(provider.id, accountsSpace.id, folderTwo.id, "testTwo", null)
        val provisionOne = accountQuotaModel(provider.id, resource.id, folderOne.id, accountOne.id, 10000L, 0L)
        val provisionTwo = accountQuotaModel(provider.id, resource.id, folderTwo.id, accountTwo.id, 1000L, 0L)
        dbSessionRetryable(tableClient) {
            folderDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(folderOne, folderTwo)).awaitSingleOrNull()
            providersDao.upsertProviderRetryable(rwSingleRetryableCommit(), provider).awaitSingleOrNull()
            resourceSegmentationsDao.upsertResourceSegmentationsRetryable(rwSingleRetryableCommit(),
                listOf(segmentationOne, segmentationTwo)).awaitSingleOrNull()
            resourceSegmentsDao.upsertResourceSegmentsRetryable(rwSingleRetryableCommit(), listOf(segmentOne, segmentTwo))
                .awaitSingleOrNull()
            accountsSpacesDao.upsertOneRetryable(rwSingleRetryableCommit(), accountsSpace).awaitSingleOrNull()
            resourceTypesDao.upsertResourceTypeRetryable(rwSingleRetryableCommit(), resourceType).awaitSingleOrNull()
            resourcesDao.upsertResourceRetryable(rwSingleRetryableCommit(), resource).awaitSingleOrNull()
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(quotaOne, quotaTwo)).awaitSingleOrNull()
            accountsDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(accountOne, accountTwo)).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(provisionOne, provisionTwo)).awaitSingleOrNull()
            attachProviderAdmin(rwSingleRetryableCommit(), provider, TestUsers.SERVICE_1_QUOTA_MANAGER_STAFF_ID)
        }
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(10)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(1)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val dueDate = LocalDate.now().plusDays(2)
        val createTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(5)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = TransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", transferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER, transferRequestResult.requestSubtype)
        Assertions.assertTrue(transferRequestResult.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.loanMeta.get().borrowMeta!!.loanIds.first()
        val loan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), loanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val pendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                loan.dueAtTimestamp, loan.id))
        }!!
        val loanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, loan.dueAtTimestamp, loan.id))
        }!!
        val loanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val loanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val loanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 1).first()
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, loan.status)
        Assertions.assertEquals(LoanType.PROVIDER_RESERVE, loan.type)
        Assertions.assertEquals(dueDate, loan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, loan.requestedBy.user)
        Assertions.assertTrue(loan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, loan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, loan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, loan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), loan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, pendingLoan.dueAt.localDate)
        Assertions.assertEquals(loan.id, loanIn.loanId)
        Assertions.assertEquals(loan.id, loanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(5000), loanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, loanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, loanHistory.eventType)
        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountOne.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountOne.displayName.orElse(""))
                .setFolderId(accountOne.folderId)
                .setFreeTier(false)
                .setKey(accountOne.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()),
            GrpcResponse.success(Account.newBuilder()
                .setAccountId(accountTwo.outerAccountIdInProvider)
                .setDeleted(false)
                .setDisplayName(accountTwo.displayName.orElse(""))
                .setFolderId(accountTwo.folderId)
                .setFreeTier(false)
                .setKey(accountTwo.outerAccountKeyInProvider.orElse(""))
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build())
        ))
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                    .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                            .setResourceSegmentationKey(segmentationOne.key)
                            .setResourceSegmentKey(segmentOne.key)
                            .build())
                        .build())
                    .build())
                .addSourceProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(5)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .addDestinationProvisions(Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder()
                        .setCompoundKey(CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(resourceType.key)
                            .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(segmentationTwo.key)
                                .setResourceSegmentKey(segmentTwo.key)
                                .build())
                            .build())
                        .build())
                    .setProvided(Amount.newBuilder()
                        .setValue(6)
                        .setUnitKey("cores")
                        .build())
                    .setAllocated(Amount.newBuilder()
                        .setValue(0)
                        .setUnitKey("cores")
                        .build())
                    .build())
                .build()
            )))
        val createPayOffTransferRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(CreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(CreateProvisionTransferDto(
                    sourceAccountId = accountTwo.id,
                    sourceFolderId = folderTwo.id,
                    destinationAccountId = accountOne.id,
                    destinationFolderId = folderOne.id,
                    sourceAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(-1)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(CreateQuotaResourceTransferDto.builder()
                        .delta(1)
                        .deltaUnitKey("cores")
                        .providerId(provider.id)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(TransferLoanParametersDto(
                borrowParameters = null,
                payOffParameters = TransferLoanPayOffParametersDto(
                    loanId = loan.id
                )
            ))
            .build()
        val payOffTransferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createPayOffTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffTransferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/api/v1/transfers/{id}", payOffTransferRequest.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffLoanId = payOffTransferRequestResult.loanMeta.get().payOffMeta!!.loanId
        val payOffLoan = dbSessionRetryable(tableClient) {
            loansDao.getById(rwSingleRetryableCommit(), payOffLoanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        val payOffPendingLoan = dbSessionRetryable(tableClient) {
            pendingLoansDao.getById(rwSingleRetryableCommit(), PendingLoanKey(Tenants.DEFAULT_TENANT_ID,
                payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanIn = dbSessionRetryable(tableClient) {
            serviceLoansInDao.getById(rwSingleRetryableCommit(), ServiceLoanInModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, LoanStatus.PENDING, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanOut = dbSessionRetryable(tableClient) {
            serviceLoansOutDao.getById(rwSingleRetryableCommit(), ServiceLoanOutModel(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, LoanStatus.PENDING, payOffLoan.dueAtTimestamp, payOffLoan.id))
        }!!
        val payOffLoanBalanceIn = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_DISPENSER, provider.id, resource.id))
        }!!
        val payOffLoanBalanceOut = dbSessionRetryable(tableClient) {
            serviceLoansBalanceDao.getById(rwSingleRetryableCommit(), ServiceLoanBalanceKey(Tenants.DEFAULT_TENANT_ID,
                TestServices.TEST_SERVICE_ID_D, provider.id, resource.id))
        }!!
        val payOffLoanHistory = dbSessionRetryable(tableClient) {
            loansHistoryDao.getByLoanFirstPage(rwSingleRetryableCommit(), Tenants.DEFAULT_TENANT_ID, loan.id, 2)
        }!!
        Assertions.assertEquals(LoanStatus.PENDING, payOffLoan.status)
        Assertions.assertEquals(LoanType.PROVIDER_RESERVE, payOffLoan.type)
        Assertions.assertEquals(dueDate, payOffLoan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, payOffLoan.requestedBy.user)
        Assertions.assertTrue(payOffLoan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.id, payOffLoan.borrowTransferRequestId)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.borrowedFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.borrowedTo.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_D, payOffLoan.payOffFrom.service)
        Assertions.assertEquals(TestServices.TEST_SERVICE_ID_DISPENSER, payOffLoan.payOffTo.service)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.borrowedAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(5000), payOffLoan.payOffAmounts.amounts[0].amount)
        Assertions.assertEquals(BigInteger.valueOf(4000), payOffLoan.dueAmounts.amounts[0].amount)
        Assertions.assertEquals(dueDate, payOffPendingLoan.dueAt.localDate)
        Assertions.assertEquals(payOffLoan.id, payOffLoanIn.loanId)
        Assertions.assertEquals(payOffLoan.id, payOffLoanOut.loanId)
        Assertions.assertEquals(BigInteger.valueOf(4000), payOffLoanBalanceIn.amountIn)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceIn.amountOut)
        Assertions.assertEquals(BigInteger.valueOf(4000), payOffLoanBalanceOut.amountOut)
        Assertions.assertEquals(BigInteger.ZERO, payOffLoanBalanceOut.amountIn)
        Assertions.assertEquals(LoanEventType.LOAN_PAY_OFF, payOffLoanHistory[0].eventType)
        Assertions.assertEquals(LoanEventType.LOAN_CREATED, payOffLoanHistory[1].eventType)
    }

    private fun providerModel(accountsSpacesSupported: Boolean): ProviderModel {
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
            .multipleAccountsPerFolder(true)
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
                    .multipleReservesAllowed(false)
                    .moveProvisionSupported(true)
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

    private fun resourceSegmentationModel(providerId: String, key: String): ResourceSegmentationModel {
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

    private fun resourceSegmentModel(segmentationId: String, key: String): ResourceSegmentModel {
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

    private fun accountsSpaceModel(providerId: String, key: String, segments: Map<String, String>): AccountSpaceModel {
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

    private fun quotaModel(providerId: String, resourceId: String, folderId: String, quota: Long, balance: Long): QuotaModel {
        return QuotaModel.builder()
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .providerId(providerId)
            .resourceId(resourceId)
            .folderId(folderId)
            .quota(quota)
            .balance(balance)
            .frozenQuota(0L)
            .build()
    }

    private fun accountQuotaModel(
        providerId: String, resourceId: String, folderId: String,
        accountId: String, provided: Long, allocated: Long
    ): AccountsQuotasModel {
        return AccountsQuotasModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setProviderId(providerId)
            .setResourceId(resourceId)
            .setFolderId(folderId)
            .setAccountId(accountId)
            .setProvidedQuota(provided)
            .setAllocatedQuota(allocated)
            .setLastProvisionUpdate(Instant.now())
            .setLastReceivedProvisionVersion(null)
            .setLatestSuccessfulProvisionOperationId(null)
            .build()
    }

    private fun accountModel(providerId: String, accountsSpaceId: String?, folderId: String,
                             displayName: String, reserveType: AccountReserveType?): AccountModel {
        return AccountModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(UUID.randomUUID().toString())
            .setVersion(0L)
            .setProviderId(providerId)
            .setAccountsSpacesId(accountsSpaceId)
            .setOuterAccountIdInProvider(UUID.randomUUID().toString())
            .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
            .setFolderId(folderId)
            .setDisplayName(displayName)
            .setDeleted(false)
            .setLastAccountUpdate(Instant.now())
            .setLastReceivedVersion(null)
            .setLatestSuccessfulAccountOperationId(null)
            .setReserveType(reserveType)
            .build()
    }

    private fun folderModel(serviceId: Long): FolderModel {
        return FolderModel.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setServiceId(serviceId)
            .setVersion(0L)
            .setDisplayName("Test")
            .setDescription("Test")
            .setDeleted(false)
            .setFolderType(FolderType.COMMON)
            .setTags(emptySet())
            .setNextOpLogOrder(1L)
            .build()
    }

    private fun resourceTypeModel(providerId: String, key: String, unitsEnsembleId: String): ResourceTypeModel {
        return ResourceTypeModel.builder()
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
            .unitsEnsembleId(unitsEnsembleId)
            .build()
    }

    private fun resourceModel(
        providerId: String,
        key: String,
        resourceTypeId: String,
        segments: Map<String, String>,
        unitsEnsembleId: String,
        allowedUnitIds: Set<String>,
        defaultUnitId: String,
        baseUnitId: String,
        accountsSpaceId: String?
    ): ResourceModel {
        return ResourceModel.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .version(0L)
            .key(key)
            .nameEn("Test")
            .nameRu("Test")
            .descriptionEn("Test")
            .descriptionRu("Test")
            .deleted(false)
            .unitsEnsembleId(unitsEnsembleId)
            .providerId(providerId)
            .resourceTypeId(resourceTypeId)
            .segments(segments.map { (k ,v) -> ResourceSegmentSettingsModel(k, v) }.toSet())
            .resourceUnits(ResourceUnitsModel(allowedUnitIds, defaultUnitId, null))
            .managed(true)
            .orderable(true)
            .readOnly(false)
            .baseUnitId(baseUnitId)
            .accountsSpacesId(accountsSpaceId)
            .build()
    }

}
