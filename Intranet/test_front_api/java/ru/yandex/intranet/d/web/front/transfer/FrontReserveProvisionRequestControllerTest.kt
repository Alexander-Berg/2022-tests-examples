package ru.yandex.intranet.d.web.front.transfer

import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.LogCollectingFilter
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.UnitIds
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountReserveType
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.model.accounts.OperationInProgressModel
import ru.yandex.intranet.d.model.loans.LoanStatus
import ru.yandex.intranet.d.model.loans.LoanSubjectType
import ru.yandex.intranet.d.model.loans.LoanType
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsHelper.Companion.prepareLoan
import ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsHelper.Companion.provisionRequest
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestVoteTypeDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateProvisionTransferDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaResourceTransferDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferLoanBorrowParametersDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferLoanPayOffParametersDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestProviderResponsibleDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestVotingDto
import java.time.LocalDate

private typealias GrpcConfigurer =
    FrontTransferRequestsHelper.(FrontReserveProvisionRequestControllerTest.OperationContext) -> Unit

@IntegrationTest
class FrontReserveProvisionRequestControllerTest @Autowired constructor(
    private val helper: FrontTransferRequestsHelper
) {
    companion object {
        @JvmStatic
        fun loanData() = listOf(
            Arguments.of(99_000L, 99_000),
            Arguments.of(99_000L, 98_000),
        )

        @JvmStatic
        fun grpcFailureResponses(): List<Arguments> {
            val refreshAccountFailure: GrpcConfigurer = { ctx ->
                stubProviderService.setGetAccountResponses(listOf(
                    GrpcResponse.failure(StatusRuntimeException(Status.INTERNAL))
                ))
            }
            val updateProvisionFailure: GrpcConfigurer = { ctx ->
                stubProviderService.setGetAccountResponses(listOf(
                    GrpcResponse.success(toGrpcAccount(ctx.currentAccount, listOf(ctx.currentQuota), ctx.resourceType,
                            "bytes", "VLA", "location"))
                ))
                stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse.failure(StatusRuntimeException(
                        Status.INTERNAL))))
            }
            val refreshAccountConflict: GrpcConfigurer = { ctx ->
                stubProviderService.setGetAccountResponses(listOf(
                    GrpcResponse.success(toGrpcAccountBuilder(ctx.currentAccount, listOf(), ctx.resourceType,
                        "bytes", "VLA", "location")
                        .addProvisions(toGrpcProvision(ctx.currentQuota.providedQuota + 100,
                            ctx.currentQuota.allocatedQuota + 100, ctx.resourceType, "bytes", 42))
                        .build())
                ))
            }
            val updateProvisionConflict: GrpcConfigurer = { ctx ->
                stubProviderService.setGetAccountResponses(listOf(
                    GrpcResponse.success(toGrpcAccount(ctx.currentAccount, listOf(ctx.currentQuota), ctx.resourceType,
                        "bytes", "VLA", "location"))
                ))
                stubProviderService.setUpdateProvisionResponses(listOf(GrpcResponse.failure(StatusRuntimeException(
                    Status.FAILED_PRECONDITION))))
            }
            return listOf(
                Arguments.of("with account refresh failure", refreshAccountFailure,
                    TransferRequestStatusDto.EXECUTING),
                Arguments.of("with provision update failure", updateProvisionFailure,
                    TransferRequestStatusDto.EXECUTING),
                Arguments.of("with account refresh conflict", refreshAccountConflict,
                    TransferRequestStatusDto.FAILED),
                Arguments.of("with provision update conflict", updateProvisionConflict,
                    TransferRequestStatusDto.FAILED),
            )
        }
    }

    @BeforeEach
    fun beforeEach() {
        LogCollectingFilter.clear()
        helper.stubProviderService.reset()
    }

    @Test
    fun createAndApplyOverCommitProvisionTransferRequestTest() = runBlocking {
        val data = helper.prepareData(destQuota = 100, destBalance = 0, destProvided = 100)
        val (anotherResource, _) = helper.createResource(data.provider, "another_resource_2")
        helper.createAccountsQuotas(listOf(data.accountTwo), anotherResource, 1000, 1000, 0)
        helper.attachProviderResponsible(data.provider, TestUsers.USER_1_STAFF_ID)
        helper.addGetAccountAnswers(data.accountTwo, listOf(data.accountQuotaTwo), data.resourceType,
            "bytes", "VLA", "location")
        helper.setupUpdateProvisionAnswers(data.accountTwo, data.accountQuotaTwo.providedQuota + 100500,
            data.accountQuotaTwo.allocatedQuota, data.resourceType, "bytes", "VLA", "location", 42L)
        val deltaUnit = UnitIds.BYTES
        val counter = helper.mailSender.counter
        val (reserveAccount, reserveAccountQuota) = helper.createAccountWithQuota(
            data.resource, data.folderOne, 150, 0, 0,
            data.accountSpaceModel, reserveType = AccountReserveType.PROVIDER
        )
        helper.setReserveAccount(data.provider, reserveAccount)
        val dueDate = LocalDate.of(2077, 1, 1)
        val result = helper.createRequest(
            provisionRequest(listOf(FrontCreateProvisionTransferDto(
                reserveAccount.id, data.folderOne.id, data.folderOne.serviceId.toString(),
                data.accountTwo.id, data.folderTwo.id, data.folderTwo.serviceId.toString(),
                sourceAccountTransfers = listOf(),
                destinationAccountTransfers = listOf(FrontCreateQuotaResourceTransferDto(
                    data.resource.id, "100500", data.resource.baseUnitId
                ))
            )),
                provideOverCommitReserve = true,
                borrowParameters = FrontTransferLoanBorrowParametersDto(dueDate)
            ),
            MockUser.uid(TestUsers.USER_1_UID))
        assertNotNull(result)
        val getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_1_UID))
        assertNotNull(getResult)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, getResult.transfer.requestType)
        assertEquals(TransferRequestStatusDto.APPLIED, getResult.transfer.status)
        assertEquals(TestUsers.USER_1_ID, getResult.transfer.createdBy)
        assertTrue(getResult.transfer.transferVotes.votes.isNotEmpty())
        assertEquals(1, getResult.transfer.parameters.provisionTransfers.size)
        val frontProvisionTransferDto = getResult.transfer.parameters.provisionTransfers[0]
        assertEquals(reserveAccount.id, frontProvisionTransferDto.sourceAccountId)
        assertEquals(data.folderOne.id, frontProvisionTransferDto.sourceFolderId)
        assertEquals("1", frontProvisionTransferDto.sourceServiceId)
        assertEquals(data.accountTwo.id, frontProvisionTransferDto.destinationAccountId)
        assertEquals(data.folderTwo.id, frontProvisionTransferDto.destinationFolderId)
        assertEquals("2", frontProvisionTransferDto.destinationServiceId)
        assertNotNull(frontProvisionTransferDto.sourceAccountTransfers)
        assertNotNull(frontProvisionTransferDto.destinationAccountTransfers)
        assertTrue(frontProvisionTransferDto.sourceAccountTransfers.isEmpty())
        assertTrue(frontProvisionTransferDto.destinationAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "100500" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertEquals(0, helper.mailSender.counter - counter)
        assertTrue(result.accounts.contains(reserveAccount.id))
        assertEquals(reserveAccount.displayName.get(), result.accounts[reserveAccount.id]?.name)
        assertEquals(reserveAccount.folderId, result.accounts[reserveAccount.id]?.folderId)
        assertTrue(result.accounts.contains(data.accountTwo.id))
        assertEquals(data.accountTwo.displayName.get(), result.accounts[data.accountTwo.id]?.name)
        assertEquals(data.accountTwo.folderId, result.accounts[data.accountTwo.id]?.folderId)

        //accounts quota check
        val updatedQuotas = helper.rwTx {
            helper.accountQuotaDao.getAllByAccountIds(it, Tenants.DEFAULT_TENANT_ID,
                setOf(reserveAccount.id, data.accountTwo.id))
        }!!
        val updatedQuotaByAccountId = updatedQuotas
            .filter { it.resourceId == data.resource.id }
            .associateBy { it.accountId }
        val updatedReserveQuota = updatedQuotaByAccountId[reserveAccount.id]!!
        assertEquals(reserveAccountQuota.providedQuota, updatedReserveQuota.providedQuota)
        assertEquals(reserveAccountQuota.allocatedQuota, updatedReserveQuota.allocatedQuota)
        assertEquals(reserveAccountQuota.frozenProvidedQuota, updatedReserveQuota.frozenProvidedQuota)
        assertEquals(reserveAccountQuota.lastReceivedProvisionVersion, updatedReserveQuota.lastReceivedProvisionVersion)
        val updatedQuotaTwo = updatedQuotaByAccountId[data.accountTwo.id]!!
        assertEquals(100600, updatedQuotaTwo.providedQuota)
        assertEquals(100, updatedQuotaTwo.allocatedQuota)
        assertEquals(0, updatedQuotaTwo.frozenProvidedQuota)
        assertEquals(42L, updatedQuotaTwo.lastReceivedProvisionVersion.get())

        //folders quota check
        val updatedFolderQuotas = helper.rwTx {
            helper.quotasDao.getByFolders(it, listOf(data.folderOne.id, data.folderTwo.id), Tenants.DEFAULT_TENANT_ID)
        }!!
        val folderQuotasByFolderId = updatedFolderQuotas
            .filter { it.resourceId == data.resource.id }
            .associateBy { it.folderId }
        val sourceFolderQuota = folderQuotasByFolderId[data.folderOne.id]!!
        assertEquals(data.quotaOne.quota, sourceFolderQuota.quota)
        assertEquals(data.quotaOne.balance, sourceFolderQuota.balance)
        assertEquals(data.quotaOne.frozenQuota, sourceFolderQuota.frozenQuota)
        val destFolderQuota = folderQuotasByFolderId[data.folderTwo.id]!!
        assertEquals(100500, destFolderQuota.quota - data.quotaTwo.quota)
        assertEquals(data.quotaTwo.balance, destFolderQuota.balance)
        assertEquals(data.quotaTwo.frozenQuota, destFolderQuota.frozenQuota)

        //loan check
        assertTrue(getResult.transfer.loanMeta.isPresent)
        val loanMetaDto = getResult.transfer.loanMeta.orElseThrow()
        assertEquals(dueDate, loanMetaDto.borrowMeta?.dueDate)
        assertTrue(loanMetaDto.borrowMeta?.loanIds?.isNotEmpty() ?: false)
        assertTrue(loanMetaDto.provideOverCommitReserve ?: false)
        val loanId = loanMetaDto.borrowMeta!!.loanIds.first()
        val loan = helper.rwTxSuspend {
            helper.loansDao.getById(it, loanId, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertEquals(reserveAccount.id, loan.borrowedFrom.account)
        assertEquals(data.accountTwo.id, loan.borrowedTo.account)
        assertEquals(LoanType.PROVIDER_RESERVE_OVER_COMMIT, loan.type)
        assertEquals(LoanStatus.PENDING, loan.status)
        assertEquals(TestUsers.USER_1_ID, loan.requestedBy.user)
    }

    @Test
    fun createAndPutOverCommitProvisionTransferRequestTest() = runBlocking {
        val data = helper.prepareData(destQuota = 100, destBalance = 0, destProvided = 100)
        helper.addGetAccountAnswers(data.accountTwo, listOf(data.accountQuotaTwo), data.resourceType,
            "bytes", "VLA", "location")
        helper.setupUpdateProvisionAnswers(data.accountTwo, data.accountQuotaTwo.providedQuota + 100500,
            data.accountQuotaTwo.allocatedQuota, data.resourceType, "bytes", "VLA", "location", 42L)
        val deltaUnit = UnitIds.BYTES
        val (reserveAccount, reserveAccountQuota) = helper.createAccountWithQuota(
            data.resource, data.folderOne, 150, 0, 0,
            data.accountSpaceModel, reserveType = AccountReserveType.PROVIDER
        )
        helper.setReserveAccount(data.provider, reserveAccount)
        helper.attachProviderResponsible(data.provider, TestUsers.USER_2_STAFF_ID)
        val dueDate = LocalDate.of(2077, 1, 1)
        val result = helper.createRequest(
            provisionRequest(
                reserveAccount, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, 150,
                borrowParameters = FrontTransferLoanBorrowParametersDto(dueDate)
            ),
            MockUser.uid(TestUsers.USER_1_UID))
        assertNotNull(result)
        var getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_2_UID))
        assertNotNull(getResult)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, getResult.transfer.requestType)
        assertEquals(TransferRequestStatusDto.PENDING, getResult.transfer.status)
        assertEquals(TestUsers.USER_1_ID, getResult.transfer.createdBy)

        helper.putRequest(
            result, FrontTransferRequestsHelper.provisionRequestPut(
                provisionDelta = 100500,
                fromAccount = reserveAccount,
                toAccount = data.accountTwo,
                fromFolder = data.folderOne,
                toFolder = data.folderTwo,
                resource = data.resource,
                provideOverCommitReserve = true,
                borrowParameters = FrontTransferLoanBorrowParametersDto(dueDate),
                addConfirmation = false,
            ), MockUser.uid(TestUsers.USER_2_UID)
        )

        getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_2_UID))
        assertEquals(TransferRequestStatusDto.PENDING, getResult.transfer.status)

        assertTrue(getResult.transfer.transferVotes.votes.isEmpty())
        assertEquals(1, getResult.transfer.parameters.provisionTransfers.size)
        val frontProvisionTransferDto = getResult.transfer.parameters.provisionTransfers[0]
        assertEquals(reserveAccount.id, frontProvisionTransferDto.sourceAccountId)
        assertEquals(data.folderOne.id, frontProvisionTransferDto.sourceFolderId)
        assertEquals("1", frontProvisionTransferDto.sourceServiceId)
        assertEquals(data.accountTwo.id, frontProvisionTransferDto.destinationAccountId)
        assertEquals(data.folderTwo.id, frontProvisionTransferDto.destinationFolderId)
        assertEquals("2", frontProvisionTransferDto.destinationServiceId)
        assertNotNull(frontProvisionTransferDto.sourceAccountTransfers)
        assertNotNull(frontProvisionTransferDto.destinationAccountTransfers)
        assertTrue(frontProvisionTransferDto.sourceAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "-100500" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertTrue(frontProvisionTransferDto.destinationAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "100500" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertTrue(result.accounts.contains(reserveAccount.id))
        assertEquals(reserveAccount.displayName.get(), result.accounts[reserveAccount.id]?.name)
        assertEquals(reserveAccount.folderId, result.accounts[reserveAccount.id]?.folderId)
        assertTrue(result.accounts.contains(data.accountTwo.id))
        assertEquals(data.accountTwo.displayName.get(), result.accounts[data.accountTwo.id]?.name)
        assertEquals(data.accountTwo.folderId, result.accounts[data.accountTwo.id]?.folderId)

        //accounts quota check
        val updatedQuotas = helper.rwTx {
            helper.accountQuotaDao.getAllByAccountIds(it, Tenants.DEFAULT_TENANT_ID,
                setOf(reserveAccount.id, data.accountTwo.id))
        }!!
        val updatedQuotaByAccountId = updatedQuotas
            .filter { it.resourceId == data.resource.id }
            .associateBy { it.accountId }
        val updatedReserveQuota = updatedQuotaByAccountId[reserveAccount.id]!!
        assertEquals(reserveAccountQuota.providedQuota, updatedReserveQuota.providedQuota)
        assertEquals(reserveAccountQuota.allocatedQuota, updatedReserveQuota.allocatedQuota)
        assertEquals(reserveAccountQuota.frozenProvidedQuota, updatedReserveQuota.frozenProvidedQuota)
        assertEquals(reserveAccountQuota.lastReceivedProvisionVersion, updatedReserveQuota.lastReceivedProvisionVersion)
        val updatedQuotaTwo = updatedQuotaByAccountId[data.accountTwo.id]!!
        assertEquals(100, updatedQuotaTwo.providedQuota)
        assertEquals(100, updatedQuotaTwo.allocatedQuota)
        assertEquals(0, updatedQuotaTwo.frozenProvidedQuota)
        assertTrue(updatedQuotaTwo.lastReceivedProvisionVersion.isEmpty)

        //folders quota check
        val updatedFolderQuotas = helper.rwTx {
            helper.quotasDao.getByFolders(it, listOf(data.folderOne.id, data.folderTwo.id), Tenants.DEFAULT_TENANT_ID)
        }!!
        val folderQuotasByFolderId = updatedFolderQuotas
            .filter { it.resourceId == data.resource.id }
            .associateBy { it.folderId }
        val sourceFolderQuota = folderQuotasByFolderId[data.folderOne.id]!!
        assertEquals(data.quotaOne.quota, sourceFolderQuota.quota)
        assertEquals(data.quotaOne.balance, sourceFolderQuota.balance)
        assertEquals(data.quotaOne.frozenQuota, sourceFolderQuota.frozenQuota)
        val destFolderQuota = folderQuotasByFolderId[data.folderTwo.id]!!
        assertEquals(data.quotaTwo.quota, destFolderQuota.quota)
        assertEquals(data.quotaTwo.balance, destFolderQuota.balance)
        assertEquals(data.quotaTwo.frozenQuota, destFolderQuota.frozenQuota)

        //loan check
        assertTrue(getResult.transfer.loanMeta.isPresent)
        val loanMetaDto = getResult.transfer.loanMeta.orElseThrow()
        assertEquals(dueDate, loanMetaDto.borrowMeta?.dueDate)
        assertTrue(loanMetaDto.borrowMeta?.loanIds?.isEmpty() ?: false)
        assertTrue(loanMetaDto.provideOverCommitReserve ?: false)
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("grpcFailureResponses")
    fun createAndApplyOverCommitProvisionTransferRequestWithProviderErrorTest(
        testName: String,
        grpcConfigurer: GrpcConfigurer,
        expectedStatus: TransferRequestStatusDto
    ) = runBlocking {
        val failed = expectedStatus == TransferRequestStatusDto.FAILED
        val data = helper.prepareData(destQuota = 100, destBalance = 0, destProvided = 100)
        helper.stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.failure(StatusRuntimeException(Status.INTERNAL))
        ))
        helper.grpcConfigurer(OperationContext(data.accountTwo, data.resourceType, data.accountQuotaTwo))
        val deltaUnit = UnitIds.BYTES
        val (reserveAccount, reserveAccountQuota) = helper.createAccountWithQuota(
            data.resource, data.folderOne, 150, 0, 0,
            data.accountSpaceModel, reserveType = AccountReserveType.PROVIDER
        )
        helper.setReserveAccount(data.provider, reserveAccount)
        val dueDate = LocalDate.of(2077, 1, 1)
        helper.attachProviderResponsible(data.provider, TestUsers.USER_1_STAFF_ID)
        val result = helper.createRequest(
            provisionRequest(
                reserveAccount, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, 100500,
                borrowParameters = FrontTransferLoanBorrowParametersDto(dueDate),
                provideOverCommitReserve = true
            ),
            MockUser.uid(TestUsers.USER_1_UID))
        assertNotNull(result)
        val getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_1_UID))
        assertNotNull(getResult)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, getResult.transfer.requestType)
        assertEquals(expectedStatus, getResult.transfer.status)
        assertEquals(TestUsers.USER_1_ID, getResult.transfer.createdBy)

        //vote check
        assertTrue(getResult.transfer.transferVotes.votes.isNotEmpty())
        val vote = getResult.transfer.transferVotes.votes.first()
        assertEquals(TestUsers.USER_1_ID, vote.userId)
        assertEquals(TransferRequestVoteTypeDto.CONFIRM, vote.voteType)

        //provisions body check
        assertEquals(1, getResult.transfer.parameters.provisionTransfers.size)
        val frontProvisionTransferDto = getResult.transfer.parameters.provisionTransfers[0]
        assertEquals(reserveAccount.id, frontProvisionTransferDto.sourceAccountId)
        assertEquals(data.folderOne.id, frontProvisionTransferDto.sourceFolderId)
        assertEquals("1", frontProvisionTransferDto.sourceServiceId)
        assertEquals(data.accountTwo.id, frontProvisionTransferDto.destinationAccountId)
        assertEquals(data.folderTwo.id, frontProvisionTransferDto.destinationFolderId)
        assertEquals("2", frontProvisionTransferDto.destinationServiceId)
        assertNotNull(frontProvisionTransferDto.sourceAccountTransfers)
        assertNotNull(frontProvisionTransferDto.destinationAccountTransfers)
        assertTrue(frontProvisionTransferDto.sourceAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "-100500" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertTrue(frontProvisionTransferDto.destinationAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "100500" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertTrue(result.accounts.contains(reserveAccount.id))
        assertEquals(reserveAccount.displayName.get(), result.accounts[reserveAccount.id]?.name)
        assertEquals(reserveAccount.folderId, result.accounts[reserveAccount.id]?.folderId)
        assertTrue(result.accounts.contains(data.accountTwo.id))
        assertEquals(data.accountTwo.displayName.get(), result.accounts[data.accountTwo.id]?.name)
        assertEquals(data.accountTwo.folderId, result.accounts[data.accountTwo.id]?.folderId)

        //accounts quota check
        val updatedQuotas = helper.rwTx {
            helper.accountQuotaDao.getAllByAccountIds(it, Tenants.DEFAULT_TENANT_ID,
                setOf(reserveAccount.id, data.accountTwo.id))
        }!!
        val updatedQuotaByAccountId = updatedQuotas
            .filter { it.resourceId == data.resource.id }
            .associateBy { it.accountId }
        val updatedReserveQuota = updatedQuotaByAccountId[reserveAccount.id]!!
        assertEquals(reserveAccountQuota.providedQuota, updatedReserveQuota.providedQuota)
        assertEquals(reserveAccountQuota.allocatedQuota, updatedReserveQuota.allocatedQuota)
        assertEquals(reserveAccountQuota.frozenProvidedQuota, updatedReserveQuota.frozenProvidedQuota)
        assertEquals(reserveAccountQuota.lastReceivedProvisionVersion, updatedReserveQuota.lastReceivedProvisionVersion)
        val updatedQuotaTwo = updatedQuotaByAccountId[data.accountTwo.id]!!
        assertEquals(100, updatedQuotaTwo.providedQuota)
        assertEquals(100, updatedQuotaTwo.allocatedQuota)
        assertEquals(0, updatedQuotaTwo.frozenProvidedQuota)
        assertTrue(updatedQuotaTwo.lastReceivedProvisionVersion.isEmpty)

        //folders quota check
        val updatedFolderQuotas = helper.rwTx {
            helper.quotasDao.getByFolders(it, listOf(data.folderOne.id, data.folderTwo.id), Tenants.DEFAULT_TENANT_ID)
        }!!
        val folderQuotasByFolderId = updatedFolderQuotas
            .filter { it.resourceId == data.resource.id }
            .associateBy { it.folderId }
        val sourceFolderQuota = folderQuotasByFolderId[data.folderOne.id]!!
        assertEquals(data.quotaOne.quota, sourceFolderQuota.quota)
        assertEquals(data.quotaOne.balance, sourceFolderQuota.balance)
        assertEquals(data.quotaOne.frozenQuota, sourceFolderQuota.frozenQuota)
        val destFolderQuota = folderQuotasByFolderId[data.folderTwo.id]!!

        val operationId = frontProvisionTransferDto.operationId
        assertNotNull(operationId)

        if (failed) {
            assertEquals(data.quotaTwo.quota, destFolderQuota.quota)
            assertEquals(data.quotaTwo.balance, destFolderQuota.balance)
            assertEquals(data.quotaTwo.frozenQuota, destFolderQuota.frozenQuota)
        } else {
            assertEquals(100500, destFolderQuota.quota - data.quotaTwo.quota)
            assertEquals(data.quotaTwo.balance, destFolderQuota.balance)
            assertEquals(100500, destFolderQuota.frozenQuota - data.quotaTwo.frozenQuota)
            val operationInProgress = helper.rwTxSuspend {
                helper.operationsInProgressDao.getById(
                    it, OperationInProgressModel.Key(operationId!!, data.folderTwo.id),
                    Tenants.DEFAULT_TENANT_ID
                ).awaitSingle()
            }!!
            assertTrue(operationInProgress.isPresent)
            assertEquals(1, operationInProgress.get().retryCounter)
        }

        //loan check
        assertTrue(getResult.transfer.loanMeta.isPresent)
        val loanMetaDto = getResult.transfer.loanMeta.orElseThrow()
        assertEquals(dueDate, loanMetaDto.borrowMeta?.dueDate)
        assertTrue(loanMetaDto.borrowMeta?.loanIds?.isEmpty() ?: false)
        assertTrue(loanMetaDto.provideOverCommitReserve ?: false)
    }

    @Test
    fun simpleUserCannotProvideReserveTest() = runBlocking {
        val data = helper.prepareData(destQuota = 100, destBalance = 0, destProvided = 100)
        val (reserveAccount, reserveAccountQuota) = helper.createAccountWithQuota(
            data.resource, data.folderOne, 150, 0, 0,
            data.accountSpaceModel, reserveType = AccountReserveType.PROVIDER
        )
        helper.setReserveAccount(data.provider, reserveAccount)
        val dueDate = LocalDate.of(2077, 1, 1)
        val result = helper.createRequestResponse(
            provisionRequest(
                reserveAccount, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, 100500,
                provideOverCommitReserve = true,
                borrowParameters = FrontTransferLoanBorrowParametersDto(dueDate)
            ),
            MockUser.uid(TestUsers.USER_1_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Only provider responsible can provide over commit reserve."),
            result.fieldErrors["parameters.provideOverCommitReserve"])
    }

    @Test
    fun simpleUserCannotPutProvideReserveFlagTest() = runBlocking {
        val data = helper.prepareData(destQuota = 100, destBalance = 0, destProvided = 100)
        val (reserveAccount, reserveAccountQuota) = helper.createAccountWithQuota(
            data.resource, data.folderOne, 150, 0, 0,
            data.accountSpaceModel, reserveType = AccountReserveType.PROVIDER
        )
        helper.setReserveAccount(data.provider, reserveAccount)
        val dueDate = LocalDate.of(2077, 1, 1)
        val result = helper.createRequest(
            provisionRequest(
                reserveAccount, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, 100,
                borrowParameters = FrontTransferLoanBorrowParametersDto(dueDate)
            ),
            MockUser.uid(TestUsers.USER_1_UID))
        val errors = helper.putRequestResponse(
            result, FrontTransferRequestsHelper.provisionRequestPut(
                provisionDelta = 100500,
                fromAccount = reserveAccount,
                toAccount = data.accountTwo,
                fromFolder = data.folderOne,
                toFolder = data.folderTwo,
                resource = data.resource,
                provideOverCommitReserve = true,
                borrowParameters = FrontTransferLoanBorrowParametersDto(dueDate)
            ), MockUser.uid(TestUsers.USER_1_UID)
        )
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Only provider responsible can provide over commit reserve."),
            errors.fieldErrors["parameters.provideOverCommitReserve"])
    }

    @Test
    fun providerResponsibleCanCancelReserveProvisionRequestTest() = runBlocking {
        val data = helper.prepareData(destQuota = 100, destBalance = 0, destProvided = 100)
        val (reserveAccount, reserveAccountQuota) = helper.createAccountWithQuota(
            data.resource, data.folderOne, 150, 0, 0,
            data.accountSpaceModel, reserveType = AccountReserveType.PROVIDER
        )
        helper.setReserveAccount(data.provider, reserveAccount)
        helper.attachProviderResponsible(data.provider, TestUsers.USER_2_STAFF_ID)
        val dueDate = LocalDate.of(2077, 1, 1)
        val result = helper.createRequest(
            provisionRequest(
                reserveAccount, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, 100,
                borrowParameters = FrontTransferLoanBorrowParametersDto(dueDate)
            ),
            MockUser.uid(TestUsers.USER_1_UID))
        val getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_2_UID))
        assertTrue(getResult.transfer.isCanCancel)

        val cancelResult = helper.cancelRequest(result, MockUser.uid(TestUsers.USER_2_UID))
        assertEquals(TransferRequestStatusDto.CANCELLED, cancelResult.transfer.status)
    }

    @Test
    fun providerResponsibleIsOnlyResponsibleForReserveProvisionRequestTest() = runBlocking {
        val data = helper.prepareData(destQuota = 100, destBalance = 0, destProvided = 100)
        val (reserveAccount, reserveAccountQuota) = helper.createAccountWithQuota(
            data.resource, data.folderOne, 150, 0, 0,
            data.accountSpaceModel, reserveType = AccountReserveType.PROVIDER
        )
        helper.setReserveAccount(data.provider, reserveAccount)
        helper.attachProviderResponsible(data.provider, TestUsers.USER_2_STAFF_ID)
        val dueDate = LocalDate.of(2077, 1, 1)
        val result = helper.createRequest(
            provisionRequest(
                reserveAccount, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, 100,
                borrowParameters = FrontTransferLoanBorrowParametersDto(dueDate)
            ),
            MockUser.uid(TestUsers.USER_1_UID))
        val getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_2_UID))
        assertTrue(getResult.transfer.transferResponsible.responsibleGroups.isEmpty())
        assertTrue(getResult.transfer.transferResponsible.reserveResponsible.isEmpty)
        assertEquals(1, getResult.transfer.transferResponsible.providerResponsible.size)
        val providerResponsibleDto = getResult.transfer.transferResponsible.providerResponsible.first()
        assertEquals(FrontTransferRequestProviderResponsibleDto(TestUsers.USER_2_ID, listOf(data.provider.id)),
            providerResponsibleDto)
        assertTrue(getResult.transfer.isCanProvideOverCommitReserve)
        assertTrue(getResult.transfer.isCanUpdate)
        assertTrue(getResult.transfer.isCanCancel)
        assertTrue(getResult.transfer.isCanVote)
    }

    @Test
    fun providerResponsibleCanVoteForReserveProvisionRequestTest() = runBlocking {
        val data = helper.prepareData(destQuota = 100, destBalance = 0, destProvided = 100)
        val (reserveAccount, reserveAccountQuota) = helper.createAccountWithQuota(
            data.resource, data.folderOne, 150, 0, 0,
            data.accountSpaceModel, reserveType = AccountReserveType.PROVIDER
        )
        helper.setReserveAccount(data.provider, reserveAccount)
        helper.attachProviderResponsible(data.provider, TestUsers.USER_2_STAFF_ID)
        val dueDate = LocalDate.of(2077, 1, 1)
        val result = helper.createRequest(
            provisionRequest(
                reserveAccount, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, 100,
                borrowParameters = FrontTransferLoanBorrowParametersDto(dueDate)
            ),
            MockUser.uid(TestUsers.USER_1_UID))
        val getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_2_UID))
        assertTrue(getResult.transfer.isCanVote)

        val cancelResult = helper.voteRequest(result, FrontTransferRequestVotingDto(TransferRequestVoteTypeDto.CONFIRM),
            MockUser.uid(TestUsers.USER_2_UID))
        assertEquals(TransferRequestStatusDto.EXECUTING, cancelResult.transfer.status)
    }

    @Test
    fun simpleAccountCannotBeUsedForReserveProvisionTest() = runBlocking {
        val data = helper.prepareData(destQuota = 100, destBalance = 0, destProvided = 100)
        helper.attachProviderResponsible(data.provider, TestUsers.USER_1_STAFF_ID)
        val dueDate = LocalDate.of(2077, 1, 1)
        val result = helper.createRequestResponse(
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, 100500,
                provideOverCommitReserve = true,
                borrowParameters = FrontTransferLoanBorrowParametersDto(dueDate)
            ),
            MockUser.uid(TestUsers.USER_1_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Provider's reserve account required to provide over commit reserve."),
            result.fieldErrors["parameters.provisionTransfers.0.sourceAccount"])
    }

    @Test
    fun cannotProvideReserveWithoutBorrowDateTest() = runBlocking {
        val data = helper.prepareData(destQuota = 100, destBalance = 0, destProvided = 100)
        helper.attachProviderResponsible(data.provider, TestUsers.USER_1_STAFF_ID)
        val result = helper.createRequestResponse(
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, 100500,
                provideOverCommitReserve = true,
            ),
            MockUser.uid(TestUsers.USER_1_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Borrow parameters required for over commit reserve provision."),
            result.fieldErrors["parameters.loanParameters"])
    }

    @ParameterizedTest
    @MethodSource("loanData")
    fun payOffOverCommitQuotaTest(
        loanAmount: Long,
        payOffAmount: Long,
    ) = runBlocking {
        val data = helper.prepareData(destQuota = 100_000L, destBalance = 500, destProvided = 99_500)
        val (reserveAccount, reserveAccountQuota) = helper.createAccountWithQuota(
            data.resource, data.folderOne, 150, 0, 0,
            data.accountSpaceModel, reserveType = AccountReserveType.PROVIDER
        )
        helper.setReserveAccount(data.provider, reserveAccount)
        val loan = prepareLoan(
            TestUsers.USER_1_ID,
            LoanType.PROVIDER_RESERVE_OVER_COMMIT, reserveAccount, data.folderOne, data.accountTwo, data.folderTwo,
            LoanSubjectType.PROVIDER_RESERVE_OVER_COMMIT, data.resource, loanAmount
        )
        helper.rwTxSuspend {
            helper.loansDao.upsertOneRetryable(it, loan)
        }
        helper.addGetAccountAnswers(data.accountTwo, listOf(data.accountQuotaTwo), data.resourceType,
            "bytes", "VLA", "location")
        helper.setupUpdateProvisionAnswers(data.accountTwo, data.accountQuotaTwo.providedQuota - payOffAmount,
            data.accountQuotaTwo.allocatedQuota, data.resourceType, "bytes", "VLA", "location", 42L)
        helper.attachProviderResponsible(data.provider, TestUsers.USER_1_STAFF_ID)
        val result = helper.createRequest(
            provisionRequest(listOf(FrontCreateProvisionTransferDto(
                data.accountTwo.id, data.folderTwo.id, data.folderTwo.serviceId.toString(),
                reserveAccount.id, data.folderOne.id, data.folderOne.serviceId.toString(),
                sourceAccountTransfers = listOf(FrontCreateQuotaResourceTransferDto(
                    data.resource.id, (-payOffAmount).toString(), data.resource.baseUnitId
                )),
                destinationAccountTransfers = listOf()
            )),
                payOffParametersDto = FrontTransferLoanPayOffParametersDto(loan.id)
            ),
            MockUser.uid(TestUsers.USER_1_UID))

        val getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_1_UID))

        assertEquals(TransferRequestStatusDto.APPLIED, getResult.transfer.status)

        //accounts quota check
        val updatedQuotas = helper.rwTx {
            helper.accountQuotaDao.getAllByAccountIds(it, Tenants.DEFAULT_TENANT_ID,
                setOf(reserveAccount.id, data.accountTwo.id))
        }!!
        val updatedQuotaByAccountId = updatedQuotas
            .filter { it.resourceId == data.resource.id }
            .associateBy { it.accountId }
        val updatedReserveQuota = updatedQuotaByAccountId[reserveAccount.id]!!
        assertEquals(reserveAccountQuota.providedQuota, updatedReserveQuota.providedQuota)
        assertEquals(reserveAccountQuota.allocatedQuota, updatedReserveQuota.allocatedQuota)
        assertEquals(reserveAccountQuota.frozenProvidedQuota, updatedReserveQuota.frozenProvidedQuota)
        assertEquals(reserveAccountQuota.lastReceivedProvisionVersion, updatedReserveQuota.lastReceivedProvisionVersion)
        val updatedQuotaTwo = updatedQuotaByAccountId[data.accountTwo.id]!!
        assertEquals(data.accountQuotaTwo.providedQuota - payOffAmount, updatedQuotaTwo.providedQuota)
        assertEquals(100, updatedQuotaTwo.allocatedQuota)
        assertEquals(0, updatedQuotaTwo.frozenProvidedQuota)
        assertEquals(42L, updatedQuotaTwo.lastReceivedProvisionVersion.get())

        //folders quota check
        val updatedFolderQuotas = helper.rwTx {
            helper.quotasDao.getByFolders(it, listOf(data.folderOne.id, data.folderTwo.id), Tenants.DEFAULT_TENANT_ID)
        }!!
        val folderQuotasByFolderId = updatedFolderQuotas
            .filter { it.resourceId == data.resource.id }
            .associateBy { it.folderId }
        val sourceFolderQuota = folderQuotasByFolderId[data.accountTwo.folderId]!!
        assertEquals(data.quotaTwo.quota - payOffAmount, sourceFolderQuota.quota)
        assertEquals(data.quotaTwo.balance, sourceFolderQuota.balance)
        assertEquals(data.quotaTwo.frozenQuota, sourceFolderQuota.frozenQuota)
        val destFolderQuota = folderQuotasByFolderId[reserveAccount.folderId]!!
        assertEquals(data.quotaOne.quota, destFolderQuota.quota)
        assertEquals(data.quotaOne.balance, destFolderQuota.balance)
        assertEquals(data.quotaOne.frozenQuota, destFolderQuota.frozenQuota)

        val updatedLoan = helper.rwTxSuspend {
            helper.loansDao.getById(it, loan.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        val settled = payOffAmount == loanAmount
        if (settled) {
            assertEquals(LoanStatus.SETTLED, updatedLoan.status)
            assertNotNull(updatedLoan.settledAt)
            assertTrue(updatedLoan.dueAmounts.amounts.isEmpty())
        } else {
            assertEquals(LoanStatus.PENDING, updatedLoan.status)
            assertEquals(loanAmount - payOffAmount, updatedLoan.dueAmounts.amounts.first().amount.toLong())
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("grpcFailureResponses")
    fun payOffOverCommitQuotaWithProviderErrorTest(
        testName: String,
        grpcConfigurer: GrpcConfigurer,
        expectedStatus: TransferRequestStatusDto
    ) = runBlocking {
        val data = helper.prepareData(destQuota = 100_000L, destBalance = 500, destProvided = 99_500)
        val (reserveAccount, reserveAccountQuota) = helper.createAccountWithQuota(
            data.resource, data.folderOne, 150, 0, 0,
            data.accountSpaceModel, reserveType = AccountReserveType.PROVIDER
        )
        helper.setReserveAccount(data.provider, reserveAccount)
        val loan = prepareLoan(
            TestUsers.USER_1_ID,
            LoanType.PROVIDER_RESERVE_OVER_COMMIT, reserveAccount, data.folderOne, data.accountTwo, data.folderTwo,
            LoanSubjectType.PROVIDER_RESERVE_OVER_COMMIT, data.resource, 99000
        )
        helper.rwTxSuspend {
            helper.loansDao.upsertOneRetryable(it, loan)
        }
        helper.grpcConfigurer(OperationContext(data.accountTwo, data.resourceType, data.accountQuotaTwo))
        helper.attachProviderResponsible(data.provider, TestUsers.USER_1_STAFF_ID)
        val result = helper.createRequest(
            provisionRequest(listOf(FrontCreateProvisionTransferDto(
                data.accountTwo.id, data.folderTwo.id, data.folderTwo.serviceId.toString(),
                reserveAccount.id, data.folderOne.id, data.folderOne.serviceId.toString(),
                sourceAccountTransfers = listOf(FrontCreateQuotaResourceTransferDto(
                    data.resource.id, "-99000", data.resource.baseUnitId
                )),
                destinationAccountTransfers = listOf()
            )),
                payOffParametersDto = FrontTransferLoanPayOffParametersDto(loan.id)
            ),
            MockUser.uid(TestUsers.USER_1_UID))

        val getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_1_UID))

        assertEquals(expectedStatus, getResult.transfer.status)

        //accounts quota check
        val updatedQuotas = helper.rwTx {
            helper.accountQuotaDao.getAllByAccountIds(it, Tenants.DEFAULT_TENANT_ID,
                setOf(reserveAccount.id, data.accountTwo.id))
        }!!
        val updatedQuotaByAccountId = updatedQuotas
            .filter { it.resourceId == data.resource.id }
            .associateBy { it.accountId }
        val updatedReserveQuota = updatedQuotaByAccountId[reserveAccount.id]!!
        assertEquals(reserveAccountQuota.providedQuota, updatedReserveQuota.providedQuota)
        assertEquals(reserveAccountQuota.allocatedQuota, updatedReserveQuota.allocatedQuota)
        assertEquals(reserveAccountQuota.frozenProvidedQuota, updatedReserveQuota.frozenProvidedQuota)
        assertEquals(reserveAccountQuota.lastReceivedProvisionVersion, updatedReserveQuota.lastReceivedProvisionVersion)
        val updatedQuotaTwo = updatedQuotaByAccountId[data.accountTwo.id]!!
        assertEquals(data.accountQuotaTwo.providedQuota, updatedQuotaTwo.providedQuota)
        assertEquals(data.accountQuotaTwo.allocatedQuota, updatedQuotaTwo.allocatedQuota)
        assertEquals(data.accountQuotaTwo.frozenProvidedQuota, updatedQuotaTwo.frozenProvidedQuota)

        //folders quota check
        val updatedFolderQuotas = helper.rwTx {
            helper.quotasDao.getByFolders(it, listOf(data.folderOne.id, data.folderTwo.id), Tenants.DEFAULT_TENANT_ID)
        }!!
        val folderQuotasByFolderId = updatedFolderQuotas
            .filter { it.resourceId == data.resource.id }
            .associateBy { it.folderId }
        val sourceFolderQuota = folderQuotasByFolderId[data.accountTwo.folderId]!!
        assertEquals(data.quotaTwo.quota, sourceFolderQuota.quota)
        assertEquals(data.quotaTwo.balance, sourceFolderQuota.balance)
        assertEquals(data.quotaTwo.frozenQuota, sourceFolderQuota.frozenQuota)
        //todo проверить замороженную квоту в зависимости от операции
        val destFolderQuota = folderQuotasByFolderId[reserveAccount.folderId]!!
        assertEquals(data.quotaOne.quota, destFolderQuota.quota)
        assertEquals(data.quotaOne.balance, destFolderQuota.balance)
        assertEquals(data.quotaOne.frozenQuota, destFolderQuota.frozenQuota)

        val updatedLoan = helper.rwTxSuspend {
            helper.loansDao.getById(it, loan.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertEquals(LoanStatus.PENDING, updatedLoan.status)
        assertEquals(99000L, updatedLoan.dueAmounts.amounts.first().amount.toLong())

        val operationId = getResult.transfer.parameters.provisionTransfers.first().operationId
        assertNotNull(operationId)

        val operationInProgress = helper.rwTxSuspend {
            helper.operationsInProgressDao.getById(
                it, OperationInProgressModel.Key(operationId!!, data.folderTwo.id),
                Tenants.DEFAULT_TENANT_ID
            ).awaitSingle()
        }!!
        if (expectedStatus == TransferRequestStatusDto.FAILED) {
            assertTrue(operationInProgress.isEmpty)
        } else {
            assertTrue(operationInProgress.isPresent)
            assertEquals(1, operationInProgress.get().retryCounter)
        }
    }

    @Test
    fun cannotPayOffLoanToNotReserveAccountTest() = runBlocking {
        val data = helper.prepareData(destQuota = 100_000L, destBalance = 500, destProvided = 99_500)
        val (reserveAccount, reserveAccountQuota) = helper.createAccountWithQuota(
            data.resource, data.folderOne, 150, 0, 0,
            data.accountSpaceModel, reserveType = AccountReserveType.PROVIDER
        )
        helper.setReserveAccount(data.provider, reserveAccount)
        val loan = prepareLoan(
            TestUsers.USER_1_ID,
            LoanType.PROVIDER_RESERVE_OVER_COMMIT, reserveAccount, data.folderOne, data.accountTwo, data.folderTwo,
            LoanSubjectType.PROVIDER_RESERVE_OVER_COMMIT, data.resource, 99000
        )
        helper.rwTxSuspend {
            helper.loansDao.upsertOneRetryable(it, loan)
        }
        helper.addGetAccountAnswers(data.accountTwo, listOf(data.accountQuotaTwo), data.resourceType,
            "bytes", "VLA", "location")
        helper.setupUpdateProvisionAnswers(data.accountTwo, data.accountQuotaTwo.providedQuota - 99000,
            data.accountQuotaTwo.allocatedQuota, data.resourceType, "bytes", "VLA", "location", 42L)
        helper.attachProviderResponsible(data.provider, TestUsers.USER_1_STAFF_ID)
        val result = helper.createRequestResponse(
            provisionRequest(
                data.accountTwo, data.folderTwo, data.accountOne,
                data.folderOne, data.resource, 99000,
                payOffParametersDto = FrontTransferLoanPayOffParametersDto(loan.id)
            ),
            MockUser.uid(TestUsers.USER_1_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Transfer request source or destination does not match the loan."), result.errors)
    }

    data class OperationContext(
        val currentAccount: AccountModel,
        val resourceType: ResourceTypeModel,
        val currentQuota: AccountsQuotasModel,
    )

}
