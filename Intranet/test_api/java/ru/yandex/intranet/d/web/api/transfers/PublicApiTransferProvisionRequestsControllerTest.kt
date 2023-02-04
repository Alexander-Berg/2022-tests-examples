package ru.yandex.intranet.d.web.api.transfers

import com.yandex.ydb.table.transaction.TransactionMode
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel
import ru.yandex.intranet.d.model.accounts.OperationErrorKind
import ru.yandex.intranet.d.model.folders.FolderModel
import ru.yandex.intranet.d.model.folders.OperationPhase
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.transfers.OperationStatus
import ru.yandex.intranet.d.services.tracker.TrackerClientStub
import ru.yandex.intranet.d.util.Details
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsHelper.Companion.provisionRequest
import ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsHelper.Companion.provisionTransfer
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.SortOrderDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestSubtypeDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestVoteTypeDto
import ru.yandex.intranet.d.web.model.transfers.api.*
import java.util.stream.Stream

@IntegrationTest
class PublicApiTransferProvisionRequestsControllerTest @Autowired constructor(
    private val helper: PublicApiTransferRequestsHelper,
    private val tableClient: YdbTableClient,
    private val resourcesDao: ResourcesDao,
    private val accountsQuotasDao: AccountsQuotasDao
) {

    companion object {
        @JvmStatic
        fun resources(): Stream<Arguments> = Stream.of(
            Arguments.of(true, true, false, "Resource is virtual."),
            Arguments.of(false, false, false, "Resource is not managed."),
            Arguments.of(false, true, true, "Resource not found."),
        )

        @JvmStatic
        fun deltas(): Stream<Arguments> = Stream.of(
            Arguments.of(-200, 100),
            Arguments.of(-100, 200),
        )

        @JvmStatic
        fun deltasAndQuotasWithExpected(): Stream<Arguments> = Stream.of(
            Arguments.of(
                100,            // delta
                500, 0, 200,    // source quota, balance and provided
                450, 50, 100,   // destination quota, balance and provided
                400, 0,         // source expected quota and balance
                550, 50         // destination expected quota and balance
            ),
            Arguments.of(
                75,             // delta
                50, -150, 200,  // source old quota, balance and provided
                50, -50, 100,   // destination quota, balance and provided
                0, -125,        // source expected quota and balance
                100, -75        // destination expected quota and balance
            ),
        )

        @JvmStatic
        fun deltasAndQuotasWithMissingWithExpected(): Stream<Arguments> = Stream.of(
            Arguments.of(
                false,          // 'false' means destination quota is missing
                100,            // delta
                500, 0, 200,    // source quota, balance and provided
                400, 0,         // source expected quota and balance
                100, 0          // destination expected quota and balance
            ),
            Arguments.of(
                false,          // 'false' means destination quota is missing
                75,             // delta
                50, -150, 200,  // source old quota, balance and provided
                0, -125,        // source expected quota and balance
                50, -25         // destination expected quota and balance
            )
        )
    }

    private fun checkResponsible(
        result: TransferRequestDto,
        folderOne: FolderModel,
        folderTwo: FolderModel
    ) {
        assertEquals(2, result.transferResponsible.grouped.size)
        assertTrue(result.transferResponsible.grouped.stream()
            .anyMatch { g -> g.folderIds.contains(folderOne.id) })
        assertTrue(result.transferResponsible.grouped.stream()
            .anyMatch { g -> g.folderIds.contains(folderTwo.id) })
        assertTrue(result.transferResponsible.grouped.stream()
            .anyMatch { g ->
                g.folderIds.contains(folderOne.id) && g.responsibleSet.stream()
                    .anyMatch { it.responsible == TestUsers.SERVICE_1_QUOTA_MANAGER_UID }
            })
        assertTrue(result.transferResponsible.grouped.stream()
            .anyMatch { g ->
                g.folderIds.contains(folderOne.id) && g.responsibleSet.stream()
                    .anyMatch { it.responsible == TestUsers.SERVICE_1_QUOTA_MANAGER_2_UID }
            })
        assertTrue(result.transferResponsible.grouped.stream()
            .anyMatch { g ->
                g.folderIds.contains(folderTwo.id) && g.responsibleSet.stream()
                    .anyMatch { it.responsible == TestUsers.SERVICE_1_QUOTA_MANAGER_UID }
            })
        assertTrue(result.transferResponsible.grouped.stream()
            .anyMatch { g ->
                g.folderIds.contains(folderTwo.id) && g.responsibleSet.stream()
                    .anyMatch { it.responsible == TestUsers.SERVICE_1_QUOTA_MANAGER_2_UID }
            })
        assertTrue(result.transferResponsible.grouped.stream()
            .anyMatch { g ->
                g.folderIds.contains(folderOne.id) && g.responsibleSet.stream()
                    .anyMatch {
                        it.responsible == TestUsers.SERVICE_1_QUOTA_MANAGER_UID
                            && it.serviceIds.contains(1L)
                    }
            })
        assertTrue(result.transferResponsible.grouped.stream()
            .anyMatch { g ->
                g.folderIds.contains(folderOne.id) && g.responsibleSet.stream()
                    .anyMatch {
                        it.responsible == TestUsers.SERVICE_1_QUOTA_MANAGER_2_UID
                            && it.serviceIds.contains(1L)
                    }
            })
        assertTrue(result.transferResponsible.grouped.stream()
            .anyMatch { g ->
                g.folderIds.contains(folderTwo.id) && g.responsibleSet.stream()
                    .anyMatch {
                        it.responsible == TestUsers.SERVICE_1_QUOTA_MANAGER_UID
                            && it.serviceIds.contains(2L)
                    }
            })
        assertTrue(result.transferResponsible.grouped.stream()
            .anyMatch { g ->
                g.folderIds.contains(folderTwo.id) && g.responsibleSet.stream()
                    .anyMatch {
                        it.responsible == TestUsers.SERVICE_1_QUOTA_MANAGER_2_UID
                            && it.serviceIds.contains(2L)
                    }
            })
    }

    @Test
    fun getTransferRequestTest() {
        val data = helper.prepareData()
        val deltaUnitKey = "bytes"
        val counter = helper.mailSender.counter
        val result = helper.createRequest(
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, data.unitsEnsemble, 100
            ),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        assertNotNull(result)
        val getResult = helper.getRequest(result.id, MockUser.uid(TestUsers.USER_1_UID))
        assertNotNull(getResult)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, getResult.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, getResult.requestSubtype)
        assertEquals(TransferRequestStatusDto.PENDING, getResult.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER_UID, getResult.createdBy)
        assertTrue(getResult.votes.voters.isEmpty())
        checkResponsible(getResult, data.folderOne, data.folderTwo)
        assertEquals(1, getResult.parameters.provisionTransfers.size)
        val provisionTransferDto = getResult.parameters.provisionTransfers[0]
        assertEquals(data.accountOne.id, provisionTransferDto.sourceAccountId)
        assertEquals(data.folderOne.id, provisionTransferDto.sourceFolderId)
        assertEquals("1", provisionTransferDto.sourceServiceId)
        assertEquals(data.accountTwo.id, provisionTransferDto.destinationAccountId)
        assertEquals(data.folderTwo.id, provisionTransferDto.destinationFolderId)
        assertEquals("2", provisionTransferDto.destinationServiceId)
        assertNotNull(provisionTransferDto.sourceAccountTransfers)
        assertNotNull(provisionTransferDto.destinationAccountTransfers)
        assertTrue(provisionTransferDto.sourceAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == -100L && it.deltaUnitKey == deltaUnitKey
        })
        assertTrue(provisionTransferDto.destinationAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == 100L && it.deltaUnitKey == deltaUnitKey
        })
        assertEquals(2, helper.mailSender.counter - counter)
    }

    @Test
    fun getTransferRequestEmptyFolderTransfersTest() {
        val data = helper.prepareData()
        val deltaUnitKey = "bytes"
        val counter = helper.mailSender.counter
        val result = helper.createRequest(
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, data.unitsEnsemble, 100
            ),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        assertNotNull(result)
        val getResult = helper.getRequest(result.id, MockUser.uid(TestUsers.USER_1_UID))
        assertNotNull(getResult)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, getResult.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, getResult.requestSubtype)
        assertEquals(TransferRequestStatusDto.PENDING, getResult.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER_UID, getResult.createdBy)
        assertTrue(getResult.votes.voters.isEmpty())
        checkResponsible(getResult, data.folderOne, data.folderTwo)
        assertEquals(1, getResult.parameters.provisionTransfers.size)
        val frontProvisionTransferDto = getResult.parameters.provisionTransfers[0]
        assertEquals(data.accountOne.id, frontProvisionTransferDto.sourceAccountId)
        assertEquals(data.folderOne.id, frontProvisionTransferDto.sourceFolderId)
        assertEquals("1", frontProvisionTransferDto.sourceServiceId)
        assertEquals(data.accountTwo.id, frontProvisionTransferDto.destinationAccountId)
        assertEquals(data.folderTwo.id, frontProvisionTransferDto.destinationFolderId)
        assertEquals("2", frontProvisionTransferDto.destinationServiceId)
        assertNotNull(frontProvisionTransferDto.sourceAccountTransfers)
        assertNotNull(frontProvisionTransferDto.destinationAccountTransfers)
        assertTrue(frontProvisionTransferDto.sourceAccountTransfers.all {
            it.resourceId == data.resource.id && it.delta == -100L && it.deltaUnitKey == deltaUnitKey
        })
        assertTrue(frontProvisionTransferDto.destinationAccountTransfers.all {
            it.resourceId == data.resource.id && it.delta == 100L && it.deltaUnitKey == deltaUnitKey
        })
        assertEquals(2, helper.mailSender.counter - counter)
    }

    @Test
    fun createTransferRequestWithConfirmationTest() {
        val data = helper.prepareData()
        val counter = helper.mailSender.counter
        val result = helper.createRequest(
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, data.unitsEnsemble, 100L,
                addConfirmation = true
            ), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        assertNotNull(result)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.requestSubtype)
        assertEquals(TransferRequestStatusDto.EXECUTING, result.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER_UID, result.createdBy)
        val votes = result.votes.voters
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.voter == TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(
            setOf(data.folderOne.serviceId, data.folderTwo.serviceId),
            vote.serviceIds.toSet()
        )
        checkResponsible(result, data.folderOne, data.folderTwo)
        assertEquals(1, result.parameters.provisionTransfers.size)
        assertEquals(0, helper.mailSender.counter - counter)
    }

    @MethodSource("resources")
    @ParameterizedTest
    fun createTransferRequestWithIncorrectResource(
        virtual: Boolean,
        managed: Boolean,
        deleted: Boolean,
        message: String
    ) {
        val data = helper.prepareData()
        helper.tableClient.usingSessionMonoRetryable {
            it.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                helper.resourcesDao.updateResourceRetryable(
                    txSession, ResourceModel.builder(data.resource)
                        .virtual(virtual)
                        .managed(managed)
                        .deleted(deleted)
                        .build()
                )
            }
        }.block()
        val result = helper.createRequestResponse(
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID),
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource,
                data.unitsEnsemble, 150
            )
        )
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf(message), result.fieldErrors.values.toSet().first())
    }

    @Test
    fun createTransferRequestWithIncorrectDelta() {
        val data = helper.prepareData()
        val result = helper.createRequestResponse(
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID),
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource,
                data.unitsEnsemble, 1000L
            )
        )
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        val provisionSuggestedAmount = "${data.resource.id}.suggestedProvisionAmount"
        assertEquals(1, result.details[provisionSuggestedAmount]?.size)
        val provisionDetails = result.details[provisionSuggestedAmount]?.first() as Map<*, *>
        assertEquals("100", provisionDetails["rawAmount"])
        assertEquals("B", provisionDetails["rawUnit"])
    }

    @Test
    fun createTransferRequestWithNegativeDelta() {
        val data = helper.prepareData()
        val result = helper.createRequestResponse(
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID),
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource,
                data.unitsEnsemble, -1000L
            )
        )
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals("Number must be positive.",
            result.fieldErrors["parameters.provisionTransfers.0.destinationAccountTransfers.0.delta"]?.first())
    }

    @MethodSource("deltas")
    @ParameterizedTest
    fun createTransferRequestWithUnbalancedDelta(
        fromAccountDiff: Long,
        toAccountDiff: Long,
    ) {
        val data = helper.prepareData()
        val deltaUnitKey = "bytes"
        val result = helper.createRequestResponse(
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID),
            CreateTransferRequestDto(
                TransferRequestTypeDto.PROVISION_TRANSFER, null, false,
                CreateTransferRequestParametersDto.builder()
                    .addProvisionTransfer(
                        CreateProvisionTransferDto(
                            sourceAccountId = data.accountOne.id,
                            sourceFolderId = data.folderOne.id,
                            destinationAccountId = data.accountTwo.id,
                            destinationFolderId = data.folderTwo.id,
                            sourceAccountTransfers = listOf(
                                CreateQuotaResourceTransferDto(
                                    data.provider.id, data.resource.id, fromAccountDiff,
                                    deltaUnitKey
                                )
                            ),
                            destinationAccountTransfers = listOf(
                                CreateQuotaResourceTransferDto(
                                    data.provider.id, data.resource.id, toAccountDiff,
                                    deltaUnitKey
                                )
                            )
                        )
                    )
                    .build(),
                null
            )
        )
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertTrue(
            "Transfer request has non-zero resource quotas sum." in result.errors,
            "Actual errors: ${result.errors}"
        )
    }

    @Test
    fun createAndGetTransferRequestWithMultipleAccountPairs() {
        val data = helper.prepareData()
        val (accountThree, _) = helper.createAccountWithQuota(
            data.resource, data.folderOne,
            100, 0, 0, data.accountSpaceModel
        )
        val (accountFour, _) = helper.createAccountWithQuota(
            data.resource, data.folderTwo,
            200, 0, 0, data.accountSpaceModel
        )
        val transferRequestDto = helper.createRequest(
            provisionRequest(
                listOf(
                    provisionTransfer(
                        data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource,
                        data.unitsEnsemble, 100L
                    ),
                    provisionTransfer(
                        accountThree, data.folderOne, accountFour, data.folderTwo, data.resource,
                        data.unitsEnsemble, 50L
                    ),
                )
            ),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        val getResult = helper.getRequest(
            transferRequestDto.id,
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        val provisionTransfers = getResult.parameters.provisionTransfers
        assertEquals(2, provisionTransfers.size)
        val transfersBySourceAccountId = provisionTransfers.associateBy { it.sourceAccountId }
        val transferOne = transfersBySourceAccountId[data.accountOne.id]!!
        assertEquals(data.accountTwo.id, transferOne.destinationAccountId)
        assertEquals(data.folderOne.id, transferOne.sourceFolderId)
        assertEquals(data.folderTwo.id, transferOne.destinationFolderId)
        assertEquals(1, transferOne.sourceAccountTransfers.size)
        assertEquals(-100L, transferOne.sourceAccountTransfers.first().delta)
        assertEquals(data.resource.id, transferOne.sourceAccountTransfers.first().resourceId)
        assertEquals(1, transferOne.destinationAccountTransfers.size)
        assertEquals(100L, transferOne.destinationAccountTransfers.first().delta)
        assertEquals(data.resource.id, transferOne.destinationAccountTransfers.first().resourceId)
        val transferTwo = transfersBySourceAccountId[accountThree.id]!!
        assertEquals(accountFour.id, transferTwo.destinationAccountId)
        assertEquals(data.folderOne.id, transferTwo.sourceFolderId)
        assertEquals(data.folderTwo.id, transferTwo.destinationFolderId)
        assertEquals(1, transferTwo.sourceAccountTransfers.size)
        assertEquals(-50L, transferTwo.sourceAccountTransfers.first().delta)
        assertEquals(data.resource.id, transferTwo.sourceAccountTransfers.first().resourceId)
        assertEquals(1, transferTwo.destinationAccountTransfers.size)
        assertEquals(50L, transferTwo.destinationAccountTransfers.first().delta)
        assertEquals(data.resource.id, transferTwo.destinationAccountTransfers.first().resourceId)
    }

    @Test
    fun createTransferRequestWithResourceDuplicatesTest() {
        val data = helper.prepareData()
        val deltaUnitKey = "bytes"
        val result = helper.createRequestResponse(
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID),
            provisionRequest(
                listOf(
                    CreateProvisionTransferDto(
                        sourceAccountId = data.accountOne.id,
                        sourceFolderId = data.folderOne.id,
                        destinationAccountId = data.accountTwo.id,
                        destinationFolderId = data.folderTwo.id,
                        sourceAccountTransfers = listOf(
                            CreateQuotaResourceTransferDto(data.provider.id, data.resource.id, -50L, deltaUnitKey),
                            CreateQuotaResourceTransferDto(data.provider.id, data.resource.id, -50L, deltaUnitKey),
                        ),
                        destinationAccountTransfers = listOf(
                            CreateQuotaResourceTransferDto(data.provider.id, data.resource.id, 50L, deltaUnitKey),
                            CreateQuotaResourceTransferDto(data.provider.id, data.resource.id, 50L, deltaUnitKey),
                        )
                    )
                )
            )
        )
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(
            setOf("Duplicate resource ids are not allowed."),
            result.fieldErrors["parameters.provisionTransfers.0.sourceAccountTransfers"]
        )
        assertEquals(
            setOf("Duplicate resource ids are not allowed."),
            result.fieldErrors["parameters.provisionTransfers.0.destinationAccountTransfers"]
        )
    }

    @Test
    fun createTransferRequestWithDifferentAccountProvidersTest() {
        val data = helper.prepareData()
        val anotherProvider = helper.createProvider()
        val (anotherResource, _) = helper.createResource(anotherProvider, "bar")
        val (accountThree, _) = helper.createAccountWithQuota(anotherResource, data.folderTwo, 100, 0, 0)
        val result = helper.createRequestResponse(
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID),
            provisionRequest(
                data.accountOne, data.folderOne, accountThree, data.folderTwo, anotherResource,
                data.unitsEnsemble, 100
            )
        )
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Accounts '${data.accountOne.displayName.get()}' (${data.accountOne.id}) and" +
            " '${accountThree.displayName.get()}' (${accountThree.id}) have different providers.",
            "Accounts '${data.accountOne.displayName.get()}' (${data.accountOne.id}) and" +
                " '${accountThree.displayName.get()}' (${accountThree.id}) have different accounts spaces."),
            result.fieldErrors["parameters.provisionTransfers.0"])
        assertEquals(setOf("Resource provider is different from accounts provider.",
            "Resource accounts space is different from accounts' space."),
            result.fieldErrors["parameters.provisionTransfers.0.sourceAccountTransfers.0.resourceId"])
        assertEquals(setOf("Resource provider is different from accounts provider.",
            "Resource accounts space is different from accounts' space."),
            result.fieldErrors["parameters.provisionTransfers.0.destinationAccountTransfers.0.resourceId"])
    }

    @Test
    fun createTransferRequestWithMoreThanTwoFoldersTest() {
        val data = helper.prepareData()
        val (anotherFolder, _) = helper.createFolderWithQuota(3L, data.resource, 200, 0)
        val (anotherAccount, _) = helper.createAccountWithQuota(data.resource, anotherFolder, 100, 0, 0)
        val result = helper.createRequestResponse(
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID),
            provisionRequest(
                listOf(
                    provisionTransfer(
                        data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource,
                        data.unitsEnsemble, 10
                    ),
                    provisionTransfer(
                        data.accountOne, data.folderOne, anotherAccount, anotherFolder, data.resource,
                        data.unitsEnsemble, 10
                    )
                )
            )
        )
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(
            setOf("Cannot set more than two folders for transfer."),
            result.fieldErrors["parameters.provisionTransfers"]
        )
    }

    @Test
    fun createAndGetTransferRequestWithMultipleTransfersForOneAccountTest() {
        val data = helper.prepareData()
        val (anotherAccount, _) = helper.createAccountWithQuota(
            data.resource, data.folderTwo, 100, 0, 0,
            data.accountSpaceModel
        )
        val (anotherResource, _) = helper.createResource(data.provider, "anotherResource", data.accountSpaceModel)
        helper.createAccountsQuotas(
            listOf(data.accountOne, data.accountTwo, anotherAccount),
            anotherResource, 100, 0, 0
        )
        helper.createFolderQuotas(listOf(data.folderOne, data.folderTwo), anotherResource, 1000, 900)
        val result = helper.createRequest(
            provisionRequest(
                listOf(
                    provisionTransfer(
                        data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource,
                        data.unitsEnsemble, 10
                    ),
                    provisionTransfer(
                        data.accountOne, data.folderOne, anotherAccount, data.folderTwo, anotherResource,
                        data.unitsEnsemble, 20
                    )
                )
            ), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        val transferRequestDto = helper.getRequest(
            result.id,
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        assertEquals(2, transferRequestDto.parameters.provisionTransfers.size)
        val transfersByDestinationAccount = transferRequestDto.parameters.provisionTransfers
            .associateBy { it.destinationAccountId }
        assertEquals(2, transfersByDestinationAccount.size)
        val transferOne = transfersByDestinationAccount[data.accountTwo.id]!!
        assertEquals(data.accountOne.id, transferOne.sourceAccountId)
        assertEquals(-10L, transferOne.sourceAccountTransfers.first().delta)
        assertEquals(data.resource.id, transferOne.sourceAccountTransfers.first().resourceId)
        val transferTwo = transfersByDestinationAccount[anotherAccount.id]!!
        assertEquals(data.accountOne.id, transferTwo.sourceAccountId)
        assertEquals(-20L, transferTwo.sourceAccountTransfers.first().delta)
        assertEquals(anotherResource.id, transferTwo.sourceAccountTransfers.first().resourceId)
    }

    @Test
    fun createTransferRequestWithDuplicatedAccountPairsTest() {
        val data = helper.prepareData()
        val result = helper.createRequestResponse(
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID),
            provisionRequest(
                listOf(
                    provisionTransfer(
                        data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource,
                        data.unitsEnsemble, 10
                    ),
                    provisionTransfer(
                        data.accountTwo, data.folderTwo, data.accountOne, data.folderOne, data.resource,
                        data.unitsEnsemble, 5
                    )
                )
            )
        )
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(
            setOf(
                "Duplicated provision transfers are not allowed.",
                "Cannot set more than two folders for transfer."
            ),
            result.fieldErrors["parameters.provisionTransfers"]
        )
    }

    @Test
    fun createTransferRequestWithSameAccountPairTest() {
        val data = helper.prepareData()
        val result = helper.createRequestResponse(
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID),
            provisionRequest(
                listOf(
                    provisionTransfer(
                        data.accountOne, data.folderOne, data.accountOne, data.folderOne, data.resource,
                        data.unitsEnsemble, 10
                    )
                )
            )
        )
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(
            setOf("Cannot move quotas between the same account."),
            result.fieldErrors["parameters.provisionTransfers.0"]
        )
    }

    @ParameterizedTest
    @MethodSource("deltasAndQuotasWithExpected")
    fun createAndApplyTransferRequestWithConfirmationTest(
        delta: Long,
        sourceQuota: Long, sourceBalance: Long, sourceProvided: Long,
        destQuota: Long, destBalance: Long, destProvided: Long,
        expectedSourceQuota: Long, expectedSourceBalance: Long,
        expectedDestQuota: Long, expectedDestBalance: Long
    ) {
        val data = helper.prepareData(sourceQuota, sourceBalance, sourceProvided, destQuota, destBalance, destProvided)
        helper.stubProviderService.reset()
        helper.setupGetAccountAnswers(
            data.accountOne, listOf(data.accountQuotaOne), data.accountTwo,
            listOf(data.accountQuotaTwo), data.resourceType, "bytes", "VLA", "location"
        )
        helper.setupMoveProvisionAnswers(
            data.accountOne, data.accountTwo,
            data.accountQuotaOne.providedQuota - delta, data.accountQuotaOne.allocatedQuota,
            data.accountQuotaTwo.providedQuota + delta, data.accountQuotaTwo.allocatedQuota,
            data.resourceType, "bytes", "VLA", "location", 42L
        )
        val counter = helper.mailSender.counter
        val result = helper.createRequest(
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, data.unitsEnsemble, delta,
                addConfirmation = true
            ), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        assertNotNull(result)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.requestSubtype)
        assertEquals(TransferRequestStatusDto.EXECUTING, result.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER_UID, result.createdBy)
        val votes = result.votes.voters
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.voter == TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(
            setOf(data.folderOne.serviceId, data.folderTwo.serviceId),
            vote.serviceIds.toSet()
        )
        checkResponsible(result, data.folderOne, data.folderTwo)
        assertEquals(1, result.parameters.provisionTransfers.size)
        assertEquals(0, helper.mailSender.counter - counter)
        val updatedQuotas = helper.rwTx {
            helper.accountQuotaDao.getAllByAccountIds(
                it, Tenants.DEFAULT_TENANT_ID,
                setOf(data.accountOne.id, data.accountTwo.id)
            )
        }!!
        val updatedQuotaByAccountId = updatedQuotas.associateBy { it.accountId }
        val updatedQuotaOne = updatedQuotaByAccountId[data.accountOne.id]!!
        assertEquals(sourceProvided - delta, updatedQuotaOne.providedQuota)
        assertEquals(data.accountQuotaOne.allocatedQuota, updatedQuotaOne.allocatedQuota)
        assertEquals(data.accountQuotaOne.frozenProvidedQuota, updatedQuotaOne.frozenProvidedQuota)
        assertEquals(42L, updatedQuotaOne.lastReceivedProvisionVersion.get())
        val updatedQuotaTwo = updatedQuotaByAccountId[data.accountTwo.id]!!
        assertEquals(destProvided + delta, updatedQuotaTwo.providedQuota)
        assertEquals(data.accountQuotaTwo.allocatedQuota, updatedQuotaTwo.allocatedQuota)
        assertEquals(data.accountQuotaTwo.frozenProvidedQuota, updatedQuotaTwo.frozenProvidedQuota)
        assertEquals(42L, updatedQuotaTwo.lastReceivedProvisionVersion.get())

        val updatedFolderQuotas = helper.rwTx {
            helper.quotasDao.getByFolders(it, listOf(data.folderOne.id, data.folderTwo.id), Tenants.DEFAULT_TENANT_ID)
        }!!
        val folderQuotasByFolderId = updatedFolderQuotas.associateBy { it.folderId }
        val sourceFolderQuota = folderQuotasByFolderId[data.folderOne.id]!!
        assertEquals(expectedSourceQuota, sourceFolderQuota.quota)
        assertEquals(expectedSourceBalance, sourceFolderQuota.balance)
        assertEquals(data.quotaOne.frozenQuota, sourceFolderQuota.frozenQuota)
        val destFolderQuota = folderQuotasByFolderId[data.folderTwo.id]!!
        assertEquals(expectedDestQuota, destFolderQuota.quota)
        assertEquals(expectedDestBalance, destFolderQuota.balance)
        assertEquals(data.quotaTwo.frozenQuota, destFolderQuota.frozenQuota)

        val request = helper.getRequest(result.id, MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.APPLIED, request.status)
        assertTrue(request.application.isPresent)
        val applicationDetailsDto = request.application.orElseThrow()
        val frontProvisionTransferDto = request.parameters.provisionTransfers[0]
        assertNotNull(frontProvisionTransferDto.operationId)
        assertEquals(
            TransferOperationStatusDto.COMPLETED,
            applicationDetailsDto.operationStatusById[frontProvisionTransferDto.operationId]
        )
        assertTrue(applicationDetailsDto.failuresByOperationId.isEmpty())
        val pendingRequest = helper.rwTx {
            helper.pendingTransferRequestsDao.getById(it, request.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(pendingRequest.isEmpty)
        val transferRequestModelO = helper.rwTx {
            helper.transferRequestDao.getById(it, request.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(transferRequestModelO.isPresent)
        val transferRequestModel = transferRequestModelO.get()
        assertTrue(transferRequestModel.applicationDetails.isPresent)
        val applicationDetails = transferRequestModel.applicationDetails.get()
        assertEquals(1, applicationDetails.operationIds.size)
        assertEquals(1, applicationDetails.operationStatusById.size)
        val operationId = applicationDetails.operationIds.first()
        val operationStatus = applicationDetails.operationStatusById[operationId]
        assertEquals(OperationStatus.COMPLETED, operationStatus)
        assertEquals(2, applicationDetails.folderOpLogIdsByFolderId.size)
        assertTrue(applicationDetails.folderOpLogIdsByFolderId.all { (_, opLogIds) -> opLogIds.size == 2 })
        val accountsQuotasOperation = helper.rwTx {
            helper.accountsQuotasOperationDao.getById(it, operationId, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        val requestedChanges = accountsQuotasOperation.requestedChanges
        assertEquals(data.accountOne.id, requestedChanges.accountId.get())
        assertEquals(data.accountTwo.id, requestedChanges.destinationAccountId.get())
        assertTrue(requestedChanges.frozenProvisions.isPresent)
        assertTrue(requestedChanges.frozenDestinationProvisions.isPresent)
        val frozenProvisions = requestedChanges.frozenProvisions.get()
        val frozenDestinationProvisions = requestedChanges.frozenDestinationProvisions.get()
        if (delta < 0) {
            assertTrue(frozenProvisions.isEmpty())
            assertTrue(frozenDestinationProvisions.isNotEmpty())
            val frozenDestinationProvision = frozenDestinationProvisions[0]
            assertEquals(-delta, frozenDestinationProvision.amount)
            assertEquals(data.resource.id, frozenDestinationProvision.resourceId)
        } else {
            assertTrue(frozenProvisions.isNotEmpty())
            assertTrue(frozenDestinationProvisions.isEmpty())
            val frozenProvision = requestedChanges.frozenProvisions.get()[0]
            assertEquals(delta, frozenProvision.amount)
            assertEquals(data.resource.id, frozenProvision.resourceId)
        }
        val sourceProvision = requestedChanges.updatedProvisions.get()[0]
        assertEquals(sourceProvided - delta, sourceProvision.amount)
        val destProvision = requestedChanges.updatedDestinationProvisions.get()[0]
        assertEquals(destProvided + delta, destProvision.amount)
        val ticket = helper.trackerClientStub.getTicket(request.trackerIssueKey.get())!!
        assertEquals(TrackerClientStub.TicketStatus.CLOSED, ticket.status)
    }

    @ParameterizedTest
    @MethodSource("deltasAndQuotasWithMissingWithExpected")
    fun createAndApplyTransferRequestWithMissingQuotaTest(
        isSourceMissing: Boolean,
        delta: Long,
        existingFolderQuota: Long, existingFolderBalance: Long, existingFolderProvided: Long,
        expectedSourceQuota: Long, expectedSourceBalance: Long,
        expectedDestQuota: Long, expectedDestBalance: Long
    ) {
        val sourceQuota = if (isSourceMissing) 0L else existingFolderQuota
        val sourceBalance = if (isSourceMissing) 0L else existingFolderBalance
        val sourceProvided = if (isSourceMissing) 0L else existingFolderProvided
        val destQuota = if (isSourceMissing) existingFolderQuota else 0L
        val destBalance = if (isSourceMissing) existingFolderBalance else 0L
        val destProvided = if (isSourceMissing) existingFolderProvided else 0L
        val data = helper.prepareData(
            sourceQuota,
            sourceBalance,
            sourceProvided,
            destQuota,
            destBalance,
            destProvided,
            !isSourceMissing,
            isSourceMissing
        )
        helper.stubProviderService.reset()
        helper.setupGetAccountAnswers(
            data.accountOne, listOf(data.accountQuotaOne), data.accountTwo,
            listOf(data.accountQuotaTwo), data.resourceType, "bytes", "VLA", "location"
        )
        helper.setupMoveProvisionAnswers(
            data.accountOne, data.accountTwo,
            data.accountQuotaOne.providedQuota - delta, data.accountQuotaOne.allocatedQuota,
            data.accountQuotaTwo.providedQuota + delta, data.accountQuotaTwo.allocatedQuota,
            data.resourceType, "bytes", "VLA", "location", 42L
        )
        val counter = helper.mailSender.counter
        val result = helper.createRequest(
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, data.unitsEnsemble, delta,
                addConfirmation = true
            ), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        assertNotNull(result)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.requestSubtype)
        assertEquals(TransferRequestStatusDto.EXECUTING, result.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER_UID, result.createdBy)
        val votes = result.votes.voters
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.voter == TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(
            setOf(data.folderOne.serviceId, data.folderTwo.serviceId),
            vote.serviceIds.toSet()
        )
        checkResponsible(result, data.folderOne, data.folderTwo)
        assertEquals(1, result.parameters.provisionTransfers.size)
        assertEquals(0, helper.mailSender.counter - counter)
        val updatedQuotas = helper.rwTx {
            helper.accountQuotaDao.getAllByAccountIds(
                it, Tenants.DEFAULT_TENANT_ID,
                setOf(data.accountOne.id, data.accountTwo.id)
            )
        }!!
        val updatedQuotaByAccountId = updatedQuotas.associateBy { it.accountId }
        val updatedQuotaOne = updatedQuotaByAccountId[data.accountOne.id]!!
        assertEquals(sourceProvided - delta, updatedQuotaOne.providedQuota)
        assertEquals(data.accountQuotaOne.allocatedQuota, updatedQuotaOne.allocatedQuota)
        assertEquals(data.accountQuotaOne.frozenProvidedQuota, updatedQuotaOne.frozenProvidedQuota)
        assertEquals(42L, updatedQuotaOne.lastReceivedProvisionVersion.get())
        val updatedQuotaTwo = updatedQuotaByAccountId[data.accountTwo.id]!!
        assertEquals(destProvided + delta, updatedQuotaTwo.providedQuota)
        assertEquals(data.accountQuotaTwo.allocatedQuota, updatedQuotaTwo.allocatedQuota)
        assertEquals(data.accountQuotaTwo.frozenProvidedQuota, updatedQuotaTwo.frozenProvidedQuota)
        assertEquals(42L, updatedQuotaTwo.lastReceivedProvisionVersion.get())

        val updatedFolderQuotas = helper.rwTx {
            helper.quotasDao.getByFolders(it, listOf(data.folderOne.id, data.folderTwo.id), Tenants.DEFAULT_TENANT_ID)
        }!!
        val folderQuotasByFolderId = updatedFolderQuotas.associateBy { it.folderId }
        val sourceFolderQuota = folderQuotasByFolderId[data.folderOne.id]!!
        assertEquals(expectedSourceQuota, sourceFolderQuota.quota)
        assertEquals(expectedSourceBalance, sourceFolderQuota.balance)
        assertEquals(data.quotaOne.frozenQuota, sourceFolderQuota.frozenQuota)
        val destFolderQuota = folderQuotasByFolderId[data.folderTwo.id]!!
        assertEquals(expectedDestQuota, destFolderQuota.quota)
        assertEquals(expectedDestBalance, destFolderQuota.balance)
        assertEquals(data.quotaTwo.frozenQuota, destFolderQuota.frozenQuota)

        val request = helper.getRequest(result.id, MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.APPLIED, request.status)
        assertTrue(request.application.isPresent)
        val applicationDetailsDto = request.application.orElseThrow()
        val frontProvisionTransferDto = request.parameters.provisionTransfers[0]
        assertNotNull(frontProvisionTransferDto.operationId)
        assertEquals(
            TransferOperationStatusDto.COMPLETED,
            applicationDetailsDto.operationStatusById[frontProvisionTransferDto.operationId]
        )
        assertTrue(applicationDetailsDto.failuresByOperationId.isEmpty())
        val pendingRequest = helper.rwTx {
            helper.pendingTransferRequestsDao.getById(it, request.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(pendingRequest.isEmpty)
        val transferRequestModelO = helper.rwTx {
            helper.transferRequestDao.getById(it, request.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(transferRequestModelO.isPresent)
        val transferRequestModel = transferRequestModelO.get()
        assertTrue(transferRequestModel.applicationDetails.isPresent)
        val applicationDetails = transferRequestModel.applicationDetails.get()
        assertEquals(1, applicationDetails.operationIds.size)
        assertEquals(1, applicationDetails.operationStatusById.size)
        val operationId = applicationDetails.operationIds.first()
        val operationStatus = applicationDetails.operationStatusById[operationId]
        assertEquals(OperationStatus.COMPLETED, operationStatus)
        assertEquals(2, applicationDetails.folderOpLogIdsByFolderId.size)
        assertTrue(applicationDetails.folderOpLogIdsByFolderId.all { (_, opLogIds) -> opLogIds.size == 2 })
        val accountsQuotasOperation = helper.rwTx {
            helper.accountsQuotasOperationDao.getById(it, operationId, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        val requestedChanges = accountsQuotasOperation.requestedChanges
        assertEquals(data.accountOne.id, requestedChanges.accountId.get())
        assertEquals(data.accountTwo.id, requestedChanges.destinationAccountId.get())
        assertTrue(requestedChanges.frozenProvisions.isPresent)
        assertTrue(requestedChanges.frozenDestinationProvisions.isPresent)
        val frozenProvisions = requestedChanges.frozenProvisions.get()
        val frozenDestinationProvisions = requestedChanges.frozenDestinationProvisions.get()
        if (delta < 0) {
            assertTrue(frozenProvisions.isEmpty())
            assertTrue(frozenDestinationProvisions.isNotEmpty())
            val frozenDestinationProvision = frozenDestinationProvisions[0]
            assertEquals(-delta, frozenDestinationProvision.amount)
            assertEquals(data.resource.id, frozenDestinationProvision.resourceId)
        } else {
            assertTrue(frozenProvisions.isNotEmpty())
            assertTrue(frozenDestinationProvisions.isEmpty())
            val frozenProvision = requestedChanges.frozenProvisions.get()[0]
            assertEquals(delta, frozenProvision.amount)
            assertEquals(data.resource.id, frozenProvision.resourceId)
        }
        val sourceProvision = requestedChanges.updatedProvisions.get()[0]
        assertEquals(sourceProvided - delta, sourceProvision.amount)
        val destProvision = requestedChanges.updatedDestinationProvisions.get()[0]
        assertEquals(destProvided + delta, destProvision.amount)
        val ticket = helper.trackerClientStub.getTicket(request.trackerIssueKey.get())!!
        assertEquals(TrackerClientStub.TicketStatus.CLOSED, ticket.status)
    }

    @Test
    fun createAndApplyTransferRequestWithProviderConflict() {
        val data = helper.prepareData()
        helper.stubProviderService.reset()
        val accountOneResponse = GrpcResponse.success(
            helper.toGrpcAccount(
                data.accountOne, listOf(data.accountQuotaOne), data.resourceType,
                "bytes", "VLA", "location"
            )
        )
        helper.stubProviderService.addGetAccountResponses(data.accountOne.id, listOf(
            accountOneResponse, accountOneResponse, accountOneResponse,
        ))
        val accountTwoResponse = GrpcResponse.success(
            helper.toGrpcAccount(
                data.accountTwo, listOf(data.accountQuotaTwo), data.resourceType,
                "bytes", "VLA", "location"
            )
        )
        helper.stubProviderService.addGetAccountResponses(data.accountOne.id, listOf(
            accountTwoResponse, accountTwoResponse, accountTwoResponse,
        ))
        helper.stubProviderService.setMoveProvisionResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION))
            )
        )
        val counter = helper.mailSender.counter
        val result = helper.createRequest(
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, data.unitsEnsemble, 100,
                addConfirmation = true
            ), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        assertNotNull(result)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.requestSubtype)
        assertEquals(TransferRequestStatusDto.EXECUTING, result.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER_UID, result.createdBy)
        val votes = result.votes.voters
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.voter == TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(
            setOf(data.folderOne.serviceId, data.folderTwo.serviceId),
            vote.serviceIds.toSet()
        )
        checkResponsible(result, data.folderOne, data.folderTwo)
        assertEquals(1, result.parameters.provisionTransfers.size)
        assertEquals(0, helper.mailSender.counter - counter)
        val updatedQuotas = helper.tableClient.usingSessionMonoRetryable {
            it.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { tx ->
                helper.accountQuotaDao.getAllByAccountIds(
                    tx, Tenants.DEFAULT_TENANT_ID,
                    setOf(data.accountOne.id, data.accountTwo.id)
                )
            }
        }.block()!!
        val updatedQuotaByAccountId = updatedQuotas.associateBy { it.accountId }
        val updatedQuotaOne = updatedQuotaByAccountId[data.accountOne.id]!!
        assertEquals(200, updatedQuotaOne.providedQuota)
        assertEquals(50, updatedQuotaOne.allocatedQuota)
        assertEquals(50, updatedQuotaOne.frozenProvidedQuota)
        assertEquals(data.accountQuotaOne.lastReceivedProvisionVersion, updatedQuotaOne.lastReceivedProvisionVersion)
        val updatedQuotaTwo = updatedQuotaByAccountId[data.accountTwo.id]!!
        assertEquals(100, updatedQuotaTwo.providedQuota)
        assertEquals(100, updatedQuotaTwo.allocatedQuota)
        assertEquals(0, updatedQuotaTwo.frozenProvidedQuota)
        assertEquals(data.accountQuotaTwo.lastReceivedProvisionVersion, updatedQuotaTwo.lastReceivedProvisionVersion)
        val request = helper.getRequest(result.id, MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.FAILED, request.status)
        val frontProvisionTransferDto = request.parameters.provisionTransfers[0]
        assertTrue(frontProvisionTransferDto.operationId != null)
        assertTrue(request.application.isPresent)
        val applicationDetailsDto = request.application.orElseThrow()
        assertTrue(frontProvisionTransferDto.operationId in applicationDetailsDto.failuresByOperationId)
        assertEquals(TransferOperationStatusDto.FAILED,
            applicationDetailsDto.operationStatusById[frontProvisionTransferDto.operationId])
        assertTrue(frontProvisionTransferDto.operationId in applicationDetailsDto.failuresByOperationId)
        val failures = applicationDetailsDto.failuresByOperationId[frontProvisionTransferDto.operationId]!!
        assertTrue(Details.ERROR_FROM_PROVIDER in failures.details)
        assertTrue(failures.details[Details.ERROR_FROM_PROVIDER]!!.isNotEmpty())
        val pendingRequest = helper.rwTx {
            helper.pendingTransferRequestsDao.getById(it, request.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(pendingRequest.isEmpty)
        val transferRequestModelO = helper.rwTx {
            helper.transferRequestDao.getById(it, request.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(transferRequestModelO.isPresent)
        val transferRequestModel = transferRequestModelO.get()
        assertTrue(transferRequestModel.applicationDetails.isPresent)

        val applicationDetails = transferRequestModel.applicationDetails.get()
        assertEquals(1, applicationDetails.operationIds.size)
        assertEquals(1, applicationDetails.operationStatusById.size)
        val operationId = applicationDetails.operationIds.first()
        val operationStatus = applicationDetails.operationStatusById[operationId]
        assertEquals(OperationStatus.FAILED, operationStatus)
        assertEquals(2, applicationDetails.folderOpLogIdsByFolderId.size)
        assertTrue(applicationDetails.folderOpLogIdsByFolderId.all { (_, opLogIds) -> opLogIds.size == 2 })
        sequenceOf(data.folderOne.id, data.folderTwo.id).forEach { folderId ->
            val opLogs = helper.rwTx {
                helper.folderOperationLogDao.getFirstPageByFolder(it, Tenants.DEFAULT_TENANT_ID, folderId,
                    SortOrderDto.ASC, 100)
            }!!
            val opLogByPhase = opLogs.associateBy { it.operationPhase.orElse(null) }
            assertTrue(opLogByPhase.containsKey(OperationPhase.SUBMIT))
            assertTrue(opLogByPhase.containsKey(OperationPhase.FAIL))
            assertFalse(opLogByPhase.containsKey(OperationPhase.CLOSE))
            val submitLog = opLogByPhase[OperationPhase.SUBMIT]!!
            val failLog = opLogByPhase[OperationPhase.FAIL]!!
            assertEquals(submitLog.oldBalance, failLog.oldBalance)
            assertEquals(submitLog.newBalance, failLog.newBalance)
            assertEquals(submitLog.oldProvisions, failLog.oldProvisions)
            assertEquals(submitLog.newProvisions, failLog.newProvisions)
            assertEquals(submitLog.oldQuotas, failLog.oldQuotas)
            assertEquals(submitLog.newQuotas, failLog.newQuotas)
            assertTrue(submitLog.order < failLog.order)
        }
        val accountsQuotasOperation = helper.rwTx {
            helper.accountsQuotasOperationDao.getById(it, operationId, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(OperationErrorKind.FAILED_PRECONDITION, accountsQuotasOperation.errorKind.get())
        assertEquals(AccountsQuotasOperationsModel.RequestStatus.ERROR, accountsQuotasOperation.requestStatus.get())
        val requestedChanges = accountsQuotasOperation.requestedChanges
        assertEquals(data.accountOne.id, requestedChanges.accountId.get())
        assertEquals(data.accountTwo.id, requestedChanges.destinationAccountId.get())
        val frozenProvision = requestedChanges.frozenProvisions.get()[0]
        assertEquals(100, frozenProvision.amount)
        assertEquals(data.resource.id, frozenProvision.resourceId)
        val sourceProvision = requestedChanges.updatedProvisions.get()[0]
        assertEquals(100, sourceProvision.amount)
        val destProvision = requestedChanges.updatedDestinationProvisions.get()[0]
        assertEquals(200, destProvision.amount)
        val ticket = helper.trackerClientStub.getTicket(request.trackerIssueKey.get())!!
        assertEquals(TrackerClientStub.TicketStatus.CLOSED, ticket.status)
    }

    @Test
    fun createAndApplyTransferRequestWithProviderNotFatalError() {
        val data = helper.prepareData()
        helper.stubProviderService.reset()
        helper.stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    helper.toGrpcAccount(
                        data.accountOne, listOf(data.accountQuotaOne), data.resourceType,
                        "bytes", "VLA", "location"
                    )
                ),
                GrpcResponse.success(
                    helper.toGrpcAccount(
                        data.accountTwo, listOf(data.accountQuotaTwo), data.resourceType,
                        "bytes", "VLA", "location"
                    )
                ),
                GrpcResponse.success(
                    helper.toGrpcAccount(
                        data.accountOne, listOf(data.accountQuotaOne), data.resourceType,
                        "bytes", "VLA", "location"
                    )
                ),
                GrpcResponse.success(
                    helper.toGrpcAccount(
                        data.accountTwo, listOf(data.accountQuotaTwo), data.resourceType,
                        "bytes", "VLA", "location"
                    )
                ),
            )
        )
        helper.stubProviderService.setMoveProvisionResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.INTERNAL))
            )
        )
        val counter = helper.mailSender.counter
        val result = helper.createRequest(
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, data.unitsEnsemble, 100,
                addConfirmation = true
            ), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        assertNotNull(result)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.requestSubtype)
        assertEquals(TransferRequestStatusDto.EXECUTING, result.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER_UID, result.createdBy)
        val votes = result.votes.voters
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.voter == TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(
            setOf(data.folderOne.serviceId, data.folderTwo.serviceId),
            vote.serviceIds.toSet()
        )
        checkResponsible(result, data.folderOne, data.folderTwo)
        assertEquals(1, result.parameters.provisionTransfers.size)
        assertEquals(0, helper.mailSender.counter - counter)
        val updatedQuotas = helper.tableClient.usingSessionMonoRetryable {
            it.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { tx ->
                helper.accountQuotaDao.getAllByAccountIds(
                    tx, Tenants.DEFAULT_TENANT_ID,
                    setOf(data.accountOne.id, data.accountTwo.id)
                )
            }
        }.block()!!
        val updatedQuotaByAccountId = updatedQuotas.associateBy { it.accountId }
        val updatedQuotaOne = updatedQuotaByAccountId[data.accountOne.id]!!
        assertEquals(200, updatedQuotaOne.providedQuota)
        assertEquals(50, updatedQuotaOne.allocatedQuota)
        assertEquals(150, updatedQuotaOne.frozenProvidedQuota)
        assertEquals(data.accountQuotaOne.lastReceivedProvisionVersion, updatedQuotaOne.lastReceivedProvisionVersion)
        val updatedQuotaTwo = updatedQuotaByAccountId[data.accountTwo.id]!!
        assertEquals(100, updatedQuotaTwo.providedQuota)
        assertEquals(100, updatedQuotaTwo.allocatedQuota)
        assertEquals(0, updatedQuotaTwo.frozenProvidedQuota)
        assertEquals(data.accountQuotaTwo.lastReceivedProvisionVersion, updatedQuotaTwo.lastReceivedProvisionVersion)
        val request = helper.getRequest(result.id, MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.EXECUTING, request.status)
        val frontProvisionTransferDto = request.parameters.provisionTransfers[0]
        assertTrue(frontProvisionTransferDto.operationId != null)
        val frontApplicationDetailsO = request.application
        assertTrue(frontApplicationDetailsO.isPresent)
        val applicationDetailsDto = frontApplicationDetailsO.orElseThrow()
        assertEquals(
            TransferOperationStatusDto.EXECUTING,
            applicationDetailsDto.operationStatusById[frontProvisionTransferDto.operationId]
        )
        assertTrue(applicationDetailsDto.failuresByOperationId.isEmpty())
        val pendingRequest = helper.rwTx {
            helper.pendingTransferRequestsDao.getById(it, request.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(pendingRequest.isEmpty)
        val transferRequestModelO = helper.rwTx {
            helper.transferRequestDao.getById(it, request.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(transferRequestModelO.isPresent)
        val transferRequestModel = transferRequestModelO.get()
        assertTrue(transferRequestModel.applicationDetails.isPresent)
        val applicationDetails = transferRequestModel.applicationDetails.get()
        assertEquals(1, applicationDetails.operationIds.size)
        assertEquals(1, applicationDetails.operationStatusById.size)
        val operationId = applicationDetails.operationIds.first()
        assertEquals(OperationStatus.EXECUTING, applicationDetails.operationStatusById[operationId])
        assertEquals(2, applicationDetails.folderOpLogIdsByFolderId.size)
        assertTrue(applicationDetails.folderOpLogIdsByFolderId.all { (_, opLogIds) -> opLogIds.size == 1 })
        assertTrue(applicationDetails.errorsEn.isEmpty)
        assertTrue(applicationDetails.errorsRu.isEmpty)
        val accountsQuotasOperation = helper.rwTx {
            helper.accountsQuotasOperationDao.getById(it, operationId, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(AccountsQuotasOperationsModel.RequestStatus.WAITING, accountsQuotasOperation.requestStatus.get())
        val requestedChanges = accountsQuotasOperation.requestedChanges
        assertEquals(data.accountOne.id, requestedChanges.accountId.get())
        assertEquals(data.accountTwo.id, requestedChanges.destinationAccountId.get())
        val frozenProvision = requestedChanges.frozenProvisions.get()[0]
        assertEquals(100, frozenProvision.amount)
        assertEquals(data.resource.id, frozenProvision.resourceId)
        val sourceProvision = requestedChanges.updatedProvisions.get()[0]
        assertEquals(100, sourceProvision.amount)
        val destProvision = requestedChanges.updatedDestinationProvisions.get()[0]
        assertEquals(200, destProvision.amount)
        val ticket = helper.trackerClientStub.getTicket(request.trackerIssueKey.get())!!
        assertEquals(TrackerClientStub.TicketStatus.OPENED, ticket.status)
    }

    @Test
    fun voteAndApplyTransferRequestTest() {
        val data = helper.prepareData()
        helper.stubProviderService.reset()
        helper.setupGetAccountAnswers(
            data.accountOne, listOf(data.accountQuotaOne), data.accountTwo,
            listOf(data.accountQuotaTwo), data.resourceType, "bytes", "VLA", "location"
        )
        helper.setupMoveProvisionAnswers(
            data.accountOne, data.accountTwo,
            data.accountQuotaOne.providedQuota - 100L, data.accountQuotaOne.allocatedQuota,
            data.accountQuotaTwo.providedQuota + 100L, data.accountQuotaTwo.allocatedQuota,
            data.resourceType, "bytes", "VLA", "location", 42L
        )
        var result = helper.createRequest(
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, data.unitsEnsemble, 100
            ),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        assertNotNull(result)
        result = helper.voteRequest(
            result,
            TransferRequestVotingDto(result.version, TransferRequestVoteTypeDto.CONFIRM),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.requestSubtype)
        assertEquals(TransferRequestStatusDto.EXECUTING, result.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER_UID, result.createdBy)
        val votes = result.votes.voters
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.voter == TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(
            setOf(data.folderOne.serviceId, data.folderTwo.serviceId),
            vote.serviceIds.toSet()
        )
        assertEquals(1, result.parameters.provisionTransfers.size)
        val frontProvisionTransferDto = result.parameters.provisionTransfers[0]
        assertTrue(frontProvisionTransferDto.operationId != null)
        assertTrue(result.application.isPresent)
        val applicationDetailsDto = result.application.orElseThrow()
        assertEquals(
            TransferOperationStatusDto.EXECUTING,
            applicationDetailsDto.operationStatusById[frontProvisionTransferDto.operationId]
        )
        val updatedQuotas = helper.tableClient.usingSessionMonoRetryable {
            it.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { tx ->
                helper.accountQuotaDao.getAllByAccountIds(
                    tx, Tenants.DEFAULT_TENANT_ID,
                    setOf(data.accountOne.id, data.accountTwo.id)
                )
            }
        }.block()!!
        val updatedQuotaByAccountId = updatedQuotas.associateBy { it.accountId }
        val updatedQuotaOne = updatedQuotaByAccountId[data.accountOne.id]!!
        assertEquals(100, updatedQuotaOne.providedQuota)
        assertEquals(50, updatedQuotaOne.allocatedQuota)
        assertEquals(50, updatedQuotaOne.frozenProvidedQuota)
        assertEquals(42L, updatedQuotaOne.lastReceivedProvisionVersion.get())
        val updatedQuotaTwo = updatedQuotaByAccountId[data.accountTwo.id]!!
        assertEquals(200, updatedQuotaTwo.providedQuota)
        assertEquals(100, updatedQuotaTwo.allocatedQuota)
        assertEquals(0, updatedQuotaTwo.frozenProvidedQuota)
        assertEquals(42L, updatedQuotaTwo.lastReceivedProvisionVersion.get())
        val updatedFolderQuotas = helper.rwTx {
            helper.quotasDao.getByFolders(it, listOf(data.folderOne.id, data.folderTwo.id), Tenants.DEFAULT_TENANT_ID)
        }!!
        val folderQuotasByFolderId = updatedFolderQuotas.associateBy { it.folderId }
        val sourceFolderQuota = folderQuotasByFolderId[data.folderOne.id]!!
        assertEquals(-100, sourceFolderQuota.quota - data.quotaOne.quota)
        assertEquals(data.quotaOne.balance, sourceFolderQuota.balance)
        assertEquals(data.quotaOne.frozenQuota, sourceFolderQuota.frozenQuota)
        val destFolderQuota = folderQuotasByFolderId[data.folderTwo.id]!!
        assertEquals(100, destFolderQuota.quota - data.quotaTwo.quota)
        assertEquals(data.quotaTwo.balance, destFolderQuota.balance)
        assertEquals(data.quotaTwo.frozenQuota, destFolderQuota.frozenQuota)
        val request = helper.getRequest(result.id, MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.APPLIED, request.status)
        val pendingRequest = helper.rwTx {
            helper.pendingTransferRequestsDao.getById(it, request.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(pendingRequest.isEmpty)
        val transferRequestModelO = helper.rwTx {
            helper.transferRequestDao.getById(it, request.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(transferRequestModelO.isPresent)
        val transferRequestModel = transferRequestModelO.get()
        assertTrue(transferRequestModel.applicationDetails.isPresent)
        val applicationDetails = transferRequestModel.applicationDetails.get()
        assertEquals(1, applicationDetails.operationIds.size)
        assertEquals(1, applicationDetails.operationStatusById.size)
        val operationId = applicationDetails.operationIds.first()
        val operationStatus = applicationDetails.operationStatusById[operationId]
        assertEquals(OperationStatus.COMPLETED, operationStatus)
        assertEquals(2, applicationDetails.folderOpLogIdsByFolderId.size)
        assertTrue(applicationDetails.folderOpLogIdsByFolderId.all { (_, opLogIds) -> opLogIds.size == 2 })
        val accountsQuotasOperation = helper.rwTx {
            helper.accountsQuotasOperationDao.getById(it, operationId, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        val requestedChanges = accountsQuotasOperation.requestedChanges
        assertEquals(data.accountOne.id, requestedChanges.accountId.get())
        assertEquals(data.accountTwo.id, requestedChanges.destinationAccountId.get())
        val frozenProvision = requestedChanges.frozenProvisions.get()[0]
        assertEquals(100, frozenProvision.amount)
        assertEquals(data.resource.id, frozenProvision.resourceId)
        val sourceProvision = requestedChanges.updatedProvisions.get()[0]
        assertEquals(100, sourceProvision.amount)
        val destProvision = requestedChanges.updatedDestinationProvisions.get()[0]
        assertEquals(200, destProvision.amount)
    }

    @Test
    //DISPENSER-5324
    fun createAndApplyTransferRequestWithDeletedResources() = runBlocking {
        val data = helper.prepareData()
        val resource = helper.createResource(data.provider, "del_test", data.accountSpaceModel)
        val deletedResource = ResourceModel.builder(resource.first).deleted(true).build()
        dbSessionRetryable(tableClient){
            rwTxRetryable { resourcesDao.updateResourceRetryable(txSession, deletedResource).awaitSingleOrNull() }
        }
        val accountsQuotas = helper.createAccountsQuotas(
            listOf(data.accountOne, data.accountTwo), deletedResource, 0L, 0L, 0L
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable { accountsQuotasDao.upsertAllRetryable(txSession, accountsQuotas).awaitSingleOrNull() }
        }
        val delta = 50L
        helper.stubProviderService.reset()
        helper.setupGetAccountAnswers(
            data.accountOne, listOf(data.accountQuotaOne), data.accountTwo,
            listOf(data.accountQuotaTwo), data.resourceType, "bytes", "VLA", "location"
        )
        helper.setupMoveProvisionAnswers(
            data.accountOne, data.accountTwo,
            data.accountQuotaOne.providedQuota - delta, data.accountQuotaOne.allocatedQuota,
            data.accountQuotaTwo.providedQuota + delta, data.accountQuotaTwo.allocatedQuota,
            data.resourceType, "bytes", "VLA", "location", 42L
        )
        val counter = helper.mailSender.counter
        val result = helper.createRequest(
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, data.unitsEnsemble, delta,
                addConfirmation = true
            ), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        assertNotNull(result)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.requestSubtype)
        assertEquals(TransferRequestStatusDto.EXECUTING, result.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER_UID, result.createdBy)
        val votes = result.votes.voters
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.voter == TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(
            setOf(data.folderOne.serviceId, data.folderTwo.serviceId),
            vote.serviceIds.toSet()
        )
        checkResponsible(result, data.folderOne, data.folderTwo)
        assertEquals(1, result.parameters.provisionTransfers.size)
        assertEquals(0, helper.mailSender.counter - counter)

        val request = helper.getRequest(result.id, MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.APPLIED, request.status)
        assertTrue(request.application.isPresent)
        val applicationDetailsDto = request.application.orElseThrow()
        val frontProvisionTransferDto = request.parameters.provisionTransfers[0]
        assertNotNull(frontProvisionTransferDto.operationId)
        assertEquals(
            TransferOperationStatusDto.COMPLETED,
            applicationDetailsDto.operationStatusById[frontProvisionTransferDto.operationId]
        )
        assertTrue(applicationDetailsDto.failuresByOperationId.isEmpty())
        val pendingRequest = helper.rwTx {
            helper.pendingTransferRequestsDao.getById(it, request.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(pendingRequest.isEmpty)
        val transferRequestModelO = helper.rwTx {
            helper.transferRequestDao.getById(it, request.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(transferRequestModelO.isPresent)
        val transferRequestModel = transferRequestModelO.get()
        assertTrue(transferRequestModel.applicationDetails.isPresent)
        val applicationDetails = transferRequestModel.applicationDetails.get()
        assertEquals(1, applicationDetails.operationIds.size)
        assertEquals(1, applicationDetails.operationStatusById.size)
        val operationId = applicationDetails.operationIds.first()
        val operationStatus = applicationDetails.operationStatusById[operationId]
        assertEquals(OperationStatus.COMPLETED, operationStatus)
        assertEquals(2, applicationDetails.folderOpLogIdsByFolderId.size)
        assertTrue(applicationDetails.folderOpLogIdsByFolderId.all { (_, opLogIds) -> opLogIds.size == 2 })
        val accountsQuotasOperation = helper.rwTx {
            helper.accountsQuotasOperationDao.getById(it, operationId, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        val requestedChanges = accountsQuotasOperation.requestedChanges
        assertEquals(data.accountOne.id, requestedChanges.accountId.get())
        assertEquals(data.accountTwo.id, requestedChanges.destinationAccountId.get())
        assertTrue(requestedChanges.frozenProvisions.isPresent)
        assertTrue(requestedChanges.frozenDestinationProvisions.isPresent)
        val frozenProvisions = requestedChanges.frozenProvisions.get()
        val frozenDestinationProvisions = requestedChanges.frozenDestinationProvisions.get()
        assertTrue(frozenProvisions.isNotEmpty())
            assertTrue(frozenDestinationProvisions.isEmpty())
            val frozenProvision = requestedChanges.frozenProvisions.get()[0]
            assertEquals(delta, frozenProvision.amount)
            assertEquals(data.resource.id, frozenProvision.resourceId)

    }

    @Test
    fun mailTest() = runBlocking {
        val data = helper.prepareData()
        val counter = helper.mailSender.counter
        val transfer = helper.createRequest(
            provisionRequest(
                data.accountOne, data.folderOne, data.accountTwo,
                data.folderTwo, data.resource, data.unitsEnsemble, 100
            ),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        assertEquals(2, helper.mailSender.counter - counter)
        val mail = helper.mailSender.mail
        val expectedHtmlBody =
            """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Новая заявка на перемещение квот</title>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                </head>
                <body>
                    <p>Перейдите по ссылке, чтобы подтвердить или отклонить заявку: <a href="https://abc.test.yandex-team.ru/folders/transfers/${transfer.id}?utm_source=email&amp;utm_medium=single_request_link&amp;utm_campaign=email_analytics">Перейти</a>.</p>
                    <p>Перейдите по ссылке, чтобы просмотреть все ожидающие заявки: <a href="https://abc.test.yandex-team.ru/approves/quota-transfer?utm_source=email&amp;utm_medium=single_request_link&amp;utm_campaign=email_analytics">Перейти</a>.</p>
                </body>
                </html>

            """.trimIndent()
        val expectedPlainTextBody =
            """
                Перейдите по ссылке, чтобы подтвердить или отклонить заявку: https://abc.test.yandex-team.ru/folders/transfers/${transfer.id}?utm_source=email&utm_medium=single_request_link&utm_campaign=email_analytics
                Перейдите по ссылке, чтобы просмотреть все ожидающие заявки: https://abc.test.yandex-team.ru/approves/quota-transfer?utm_source=email&utm_medium=single_request_link&utm_campaign=email_analytics

            """.trimIndent()
        assertEquals(expectedHtmlBody, mail[0].htmlBody)
        assertEquals(expectedHtmlBody, mail[1].htmlBody)
        assertEquals(expectedPlainTextBody, mail[0].plainTextBody)
        assertEquals(expectedPlainTextBody, mail[1].plainTextBody)
    }
}
