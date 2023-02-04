package ru.yandex.intranet.d.web.front.transfer

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.*
import ru.yandex.intranet.d.backend.service.provider_proto.*
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsDao
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.folders.FolderDao
import ru.yandex.intranet.d.dao.loans.*
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountReserveType
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.model.folders.FolderModel
import ru.yandex.intranet.d.model.folders.FolderType
import ru.yandex.intranet.d.model.loans.*
import ru.yandex.intranet.d.model.providers.*
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestSubtypeDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto
import ru.yandex.intranet.d.web.model.transfers.front.*
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Transfer requests front API loans test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class FrontTransferLoansTest(@Autowired private val webClient: WebTestClient,
                             @Autowired private val stubProviderService: StubProviderService,
                             @Autowired private val tableClient: YdbTableClient,
                             @Autowired private val providersDao: ProvidersDao,
                             @Autowired private val resourceTypesDao: ResourceTypesDao,
                             @Autowired private val resourcesDao: ResourcesDao,
                             @Autowired private val quotasDao: QuotasDao,
                             @Autowired private val folderDao: FolderDao,
                             @Autowired private val accountsDao: AccountsDao,
                             @Autowired private val accountsQuotasDao: AccountsQuotasDao,
                             @Autowired private val loansDao: LoansDao,
                             @Autowired private val pendingLoansDao: PendingLoansDao,
                             @Autowired private val serviceLoansInDao: ServiceLoansInDao,
                             @Autowired private val serviceLoansOutDao: ServiceLoansOutDao,
                             @Autowired private val serviceLoansBalanceDao: ServiceLoansBalanceDao,
                             @Autowired private val loansHistoryDao: LoansHistoryDao) {

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
        val createTransferRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(FrontCreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(FrontCreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    sourceServiceId = TestServices.TEST_SERVICE_ID_DISPENSER.toString(),
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    destinationServiceId = TestServices.TEST_SERVICE_ID_D.toString(),
                    sourceAccountTransfers = listOf(FrontCreateQuotaResourceTransferDto.builder()
                        .delta("-5")
                        .deltaUnitId(UnitIds.CORES)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(FrontCreateQuotaResourceTransferDto.builder()
                        .delta("5")
                        .deltaUnitId(UnitIds.CORES)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(FrontTransferLoanParametersDto(
                borrowParameters = FrontTransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/front/transfers/{id}", transferRequest.transfer.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.transfer.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER,
            transferRequestResult.transfer.requestSubtype)
        Assertions.assertTrue(transferRequestResult.transfer.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.transfer.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.transfer.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.transfer.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.transfer.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.transfer.loanMeta.get().borrowMeta!!.loanIds.first()
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
        Assertions.assertEquals(transferRequest.transfer.id, loan.borrowTransferRequestId)
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
        val createTransferRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(FrontCreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(FrontCreateProvisionTransferDto(
                    sourceAccountId = accountOne.id,
                    sourceFolderId = folderOne.id,
                    sourceServiceId = TestServices.TEST_SERVICE_ID_DISPENSER.toString(),
                    destinationAccountId = accountTwo.id,
                    destinationFolderId = folderTwo.id,
                    destinationServiceId = TestServices.TEST_SERVICE_ID_D.toString(),
                    sourceAccountTransfers = listOf(FrontCreateQuotaResourceTransferDto.builder()
                        .delta("-5")
                        .deltaUnitId(UnitIds.CORES)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(FrontCreateQuotaResourceTransferDto.builder()
                        .delta("5")
                        .deltaUnitId(UnitIds.CORES)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(FrontTransferLoanParametersDto(
                borrowParameters = FrontTransferLoanBorrowParametersDto(
                    dueDate = dueDate
                ),
                payOffParameters = null
            ))
            .build()
        val transferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val transferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/front/transfers/{id}", transferRequest.transfer.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, transferRequestResult.transfer.status)
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER,
            transferRequestResult.transfer.requestSubtype)
        Assertions.assertTrue(transferRequestResult.transfer.loanMeta.isPresent)
        Assertions.assertNotNull(transferRequestResult.transfer.loanMeta.get().borrowMeta)
        Assertions.assertNull(transferRequestResult.transfer.loanMeta.get().payOffMeta)
        Assertions.assertEquals(dueDate, transferRequest.transfer.loanMeta.get().borrowMeta!!.dueDate)
        Assertions.assertEquals(1, transferRequestResult.transfer.loanMeta.get().borrowMeta!!.loanIds.size)
        val loanId = transferRequestResult.transfer.loanMeta.get().borrowMeta!!.loanIds.first()
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
        Assertions.assertEquals(transferRequest.transfer.id, loan.borrowTransferRequestId)
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
        val createPayOffTransferRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .description("Test")
            .parameters(FrontCreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(FrontCreateProvisionTransferDto(
                    sourceAccountId = accountTwo.id,
                    sourceFolderId = folderTwo.id,
                    sourceServiceId = TestServices.TEST_SERVICE_ID_D.toString(),
                    destinationAccountId = accountOne.id,
                    destinationFolderId = folderOne.id,
                    destinationServiceId = TestServices.TEST_SERVICE_ID_DISPENSER.toString(),
                    sourceAccountTransfers = listOf(FrontCreateQuotaResourceTransferDto.builder()
                        .delta("-5")
                        .deltaUnitId(UnitIds.CORES)
                        .resourceId(resource.id)
                        .build()),
                    destinationAccountTransfers = listOf(FrontCreateQuotaResourceTransferDto.builder()
                        .delta("5")
                        .deltaUnitId(UnitIds.CORES)
                        .resourceId(resource.id)
                        .build())
                ))
                .build())
            .loanParameters(FrontTransferLoanParametersDto(
                borrowParameters = null,
                payOffParameters = FrontTransferLoanPayOffParametersDto(
                    loanId = loan.id
                )
            ))
            .build()
        val payOffTransferRequest = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createPayOffTransferRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffTransferRequestResult = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .get()
            .uri("/front/transfers/{id}", payOffTransferRequest.transfer.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
        val payOffLoanId = payOffTransferRequestResult.transfer.loanMeta.get().payOffMeta!!.loanId
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
        Assertions.assertEquals(TransferRequestSubtypeDto.LOAN_PROVISION_TRANSFER,
            payOffTransferRequestResult.transfer.requestSubtype)
        Assertions.assertEquals(LoanStatus.SETTLED, payOffLoan.status)
        Assertions.assertEquals(LoanType.PEER_TO_PEER, payOffLoan.type)
        Assertions.assertEquals(dueDate, payOffLoan.dueAt.localDate)
        Assertions.assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, payOffLoan.requestedBy.user)
        Assertions.assertTrue(payOffLoan.requestApprovedBy.subjects.isNotEmpty())
        Assertions.assertEquals(transferRequest.transfer.id, payOffLoan.borrowTransferRequestId)
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
