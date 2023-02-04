package ru.yandex.intranet.d.web.front.transfer

import com.yandex.ydb.table.transaction.TransactionMode
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import ru.yandex.direct.utils.mapToSet
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.LogCollectingFilter
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.UnitIds.BYTES
import ru.yandex.intranet.d.backend.service.provider_proto.Account
import ru.yandex.intranet.d.backend.service.provider_proto.MoveProvisionRequest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.kotlin.mono
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountReserveType
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel
import ru.yandex.intranet.d.model.accounts.OperationErrorKind
import ru.yandex.intranet.d.model.folders.FolderModel
import ru.yandex.intranet.d.model.folders.FolderOperationType
import ru.yandex.intranet.d.model.folders.OperationPhase
import ru.yandex.intranet.d.model.folders.TransferMetaHistoryModel.RoleInTransfer
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.transfers.OperationStatus
import ru.yandex.intranet.d.model.transfers.TransferRequestModel
import ru.yandex.intranet.d.model.transfers.TransferRequestStatus
import ru.yandex.intranet.d.services.integration.providers.rest.model.ResourceComplexKey
import ru.yandex.intranet.d.services.tracker.TrackerClientStub
import ru.yandex.intranet.d.services.transfer.ticket.TrackerTransferType
import ru.yandex.intranet.d.util.Details
import ru.yandex.intranet.d.util.MdcKey
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsHelper.Companion.providerModel
import ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsHelper.Companion.provisionRequest
import ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsHelper.Companion.provisionRequestPut
import ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsHelper.Companion.provisionTransfer
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.SortOrderDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestSubtypeDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestVoteTypeDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateProvisionTransferDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaResourceTransferDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestParametersDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontSingleTransferRequestDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferOperationStatusDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestProviderResponsibleDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestResponsibleGroupDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestVotingDto
import java.util.*

@IntegrationTest
class FrontTransferProvisionRequestsControllerTest @Autowired constructor(
    val helper: FrontTransferRequestsHelper,
    val env: Environment,
) {
    val provisionComponentId = env.getProperty(TrackerTransferType.PROVISION.componentEnvValue, Long::class.java)
    val reserveComponentId = env.getProperty(TrackerTransferType.RESERVE.componentEnvValue, Long::class.java)
    val exchangeComponentId = env.getProperty(TrackerTransferType.EXCHANGE.componentEnvValue, Long::class.java)


    companion object {
        @JvmStatic
        fun resources() = listOf(
            Arguments.of(true, true, false, "Resource is virtual."),
            Arguments.of(false, false, false, "Resource is not managed."),
            Arguments.of(false, true, true, "Resource not found."),
        )

        @JvmStatic
        fun deltas() = listOf(
            Arguments.of(-200, 100),
            Arguments.of(-100, 200),
        )

        @JvmStatic
        fun grpcResponses(): List<Arguments> {
            val moveProvisionFailure: FrontTransferRequestsHelper.(FrontTransferRequestsHelper.Data) -> Unit = { data ->
                stubProviderService.reset()
                stubProviderService.setGetAccountResponses(listOf(
                    GrpcResponse.success(toGrpcAccount(data.accountOne, listOf(data.accountQuotaOne), data.resourceType,
                        "bytes", "VLA", "location")),
                    GrpcResponse.success(toGrpcAccount(data.accountTwo, listOf(data.accountQuotaTwo), data.resourceType,
                        "bytes", "VLA", "location")),
                    GrpcResponse.success(toGrpcAccount(data.accountOne, listOf(data.accountQuotaOne), data.resourceType,
                        "bytes", "VLA", "location")),
                    GrpcResponse.success(toGrpcAccount(data.accountTwo, listOf(data.accountQuotaTwo), data.resourceType,
                        "bytes", "VLA", "location")),
                ))
                stubProviderService.setMoveProvisionResponses(listOf(
                    GrpcResponse.failure(StatusRuntimeException(Status.INTERNAL))
                ))
            }
            val accountValidationFailure: FrontTransferRequestsHelper.(FrontTransferRequestsHelper.Data) -> Unit = { data ->
                stubProviderService.reset()
                sequenceOf(data.accountOne to data.accountQuotaOne, data.accountTwo to data.accountQuotaTwo)
                    .forEach { (account, quota) ->
                        //for pre-create fetch
                        stubProviderService.addGetAccountResponse(account.id, GrpcResponse.success(
                            toGrpcAccount(account, listOf(quota), data.resourceType, "bytes", "VLA",
                                "location")
                        ))
                        val accountProto = toGrpcAccountBuilder(account, listOf(), data.resourceType,
                            "bytes", "VLA", "location"
                        ).addProvisions(
                            toGrpcProvisionBuilder(100, 100, data.resourceType, "bytes")
                                .apply {
                                    lastUpdateBuilder.authorBuilder.apply {
                                        passportUidBuilder.passportUid = "foobarbaz-42"
                                        staffLoginBuilder.staffLogin = "foobarbaz-42"
                                    }
                                }.build()
                        ).build()
                        stubProviderService.addGetAccountResponse(account.id, GrpcResponse.success(accountProto))
                    }
            }
            return listOf(
                Arguments.of("with move provision response failure", moveProvisionFailure),
                Arguments.of("with account response validation failure", accountValidationFailure),
            )
        }
    }

    private fun checkIndices(
        transferRequestId: String,
        expectedStatus: TransferRequestStatus
    ) {
        val req = helper.rwTx {
            helper.transferRequestDao.getById(it, transferRequestId, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        return checkIndices(req, expectedStatus)
    }

    @BeforeEach
    fun beforeEach() {
        LogCollectingFilter.clear()
        helper.stubProviderService.reset()
    }

    private fun checkIndices(
        req: TransferRequestModel,
        expectedStatus: TransferRequestStatus
    ) {
        val indices = helper.rwTx {
            helper.transferRequestIndicesService.loadTransferRequestIndices(it, req)
        }!!
        val folderIds = req.parameters.provisionTransfers
            .flatMap { listOf(it.sourceFolderId, it.destinationFolderId) }
            .toSet()
        val serviceIds = req.parameters.provisionTransfers
            .flatMap { listOf(it.sourceServiceId, it.destinationServiceId) }
            .toSet()
        val responsibleIds = mutableSetOf<String>()
        req.responsible.run {
            responsible.forEach { foldersResponsible ->
                foldersResponsible.responsible.forEach { serviceResponsible ->
                    responsibleIds += serviceResponsible.responsibleIds
                }
            }
            providerResponsible.forEach { resp ->
                responsibleIds += resp.responsibleId
            }
            reserveResponsibleModel.ifPresent { reserveResponsible ->
                responsibleIds += reserveResponsible.responsibleIds
            }
        }
        assertTrue(indices.folderIndices.isNotEmpty())
        assertTrue(indices.folderIndices.all {
            it.transferRequestId == req.id && it.status == expectedStatus && it.tenantId == req.tenantId
                && it.folderId in folderIds
        })
        val indicesFolderIds = indices.folderIndices.map { it.folderId }.toSet()
        val missingFolderIds = folderIds - indicesFolderIds
        assertTrue(missingFolderIds.isEmpty(), "Missing folder ids: $missingFolderIds")
        assertTrue(indices.serviceIndices.isNotEmpty())
        assertTrue(indices.serviceIndices.all {
            it.transferRequestId == req.id && it.status == expectedStatus && it.tenantId == req.tenantId
                && it.serviceId in serviceIds
        })
        val indicesServiceIds = indices.serviceIndices.map { it.serviceId }.toSet()
        val missingServiceIDs = serviceIds - indicesServiceIds
        assertTrue(missingServiceIDs.isEmpty(), "Missing service ids: $missingServiceIDs")
        assertTrue(indices.responsibleIndices.isNotEmpty())
        assertTrue(indices.responsibleIndices.all {
            it.transferRequestId == req.id && it.status == expectedStatus && it.tenantId == req.tenantId
                && it.responsibleId in responsibleIds
        })
        val indicesResponsibleIds = indices.responsibleIndices.map { it.responsibleId }.toSet()
        val missingResponsibleIds = indicesResponsibleIds - responsibleIds
        assertTrue(missingResponsibleIds.isEmpty(), "Missing responsible ids: $missingResponsibleIds")
    }

    private fun assertDescriptionContains(ticket: TrackerClientStub.Ticket, expectedText: String) {
        assertTrue(ticket.description.contains(expectedText),
            "Ticket description does not contain text: '$expectedText'. Ticket description: '${ticket.description}'")
    }

    private fun checkResponsible(result: FrontSingleTransferRequestDto,
                                 folderOne: FolderModel,
                                 folderTwo: FolderModel) {
        assertEquals(2, result.transfer.transferResponsible.responsibleGroups.size)
        assertTrue(result.transfer.transferResponsible.responsibleGroups.stream()
            .anyMatch { g: FrontTransferRequestResponsibleGroupDto -> g.folders.contains(folderOne.id) })
        assertTrue(result.transfer.transferResponsible.responsibleGroups.stream()
            .anyMatch { g: FrontTransferRequestResponsibleGroupDto -> g.folders.contains(folderTwo.id) })
        assertTrue(result.transfer.transferResponsible.responsibleGroups.stream()
            .anyMatch { g: FrontTransferRequestResponsibleGroupDto ->
                g.folders.contains(folderOne.id) && g.responsibles.stream()
                    .anyMatch { it.responsibleId == "193adb36-7db2-4542-875f-ef93cddbd52d" }
            })
        assertTrue(result.transfer.transferResponsible.responsibleGroups.stream()
            .anyMatch { g: FrontTransferRequestResponsibleGroupDto ->
                g.folders.contains(folderOne.id) && g.responsibles.stream()
                    .anyMatch { it.responsibleId == "d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd" }
            })
        assertTrue(result.transfer.transferResponsible.responsibleGroups.stream()
            .anyMatch { g: FrontTransferRequestResponsibleGroupDto ->
                g.folders.contains(folderTwo.id) && g.responsibles.stream()
                    .anyMatch { it.responsibleId == "193adb36-7db2-4542-875f-ef93cddbd52d" }
            })
        assertTrue(result.transfer.transferResponsible.responsibleGroups.stream()
            .anyMatch { g: FrontTransferRequestResponsibleGroupDto ->
                g.folders.contains(folderTwo.id) && g.responsibles.stream()
                    .anyMatch { it.responsibleId == "d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd" }
            })
        assertTrue(result.transfer.transferResponsible.responsibleGroups.stream()
            .anyMatch { g: FrontTransferRequestResponsibleGroupDto ->
                g.folders.contains(folderOne.id) && g.responsibles.stream()
                    .anyMatch { it.responsibleId == "193adb36-7db2-4542-875f-ef93cddbd52d"
                        && it.serviceIds.contains("1") }
            })
        assertTrue(result.transfer.transferResponsible.responsibleGroups.stream()
            .anyMatch { g: FrontTransferRequestResponsibleGroupDto ->
                g.folders.contains(folderOne.id) && g.responsibles.stream()
                    .anyMatch { it.responsibleId == "d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"
                        && it.serviceIds.contains("1") }
            })
        assertTrue(result.transfer.transferResponsible.responsibleGroups.stream()
            .anyMatch { g: FrontTransferRequestResponsibleGroupDto ->
                g.folders.contains(folderTwo.id) && g.responsibles.stream()
                    .anyMatch { it.responsibleId == "193adb36-7db2-4542-875f-ef93cddbd52d"
                        && it.serviceIds.contains("2") }
            })
        assertTrue(result.transfer.transferResponsible.responsibleGroups.stream()
            .anyMatch { g: FrontTransferRequestResponsibleGroupDto ->
                g.folders.contains(folderTwo.id) && g.responsibles.stream()
                    .anyMatch { it.responsibleId == "d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"
                        && it.serviceIds.contains("2") }
            })
    }

    @Test
    fun getTransferRequestTest() {
        val data = helper.prepareData()
        val deltaUnit = BYTES
        val counter = helper.mailSender.counter
        val result = helper.createRequest(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 100),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertNotNull(result)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.transfer.requestSubtype)
        val getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_1_UID))
        assertNotNull(getResult)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, getResult.transfer.requestSubtype)
        assertEquals(data.accountSpaceModel!!.nameEn, result.accountsSpaces[data.accountSpaceModel.id]!!.name)
        assertEquals(
            data.accountOne.accountsSpacesId.orElseThrow(),
            result.accounts[data.accountOne.id]!!.accountsSpacesId
        )
        assertEquals(
            data.accountTwo.accountsSpacesId.orElseThrow(),
            result.accounts[data.accountTwo.id]!!.accountsSpacesId
        )
        assertEquals(data.locationSegment.id,
            result.resources[data.resource.id]?.segments?.get(data.locationSegmentation.id))
        assertEquals(data.locationSegment.id,
            result.accountsSpaces[data.accountSpaceModel.id]?.segments?.get(data.locationSegmentation.id))
        assertEquals(data.locationSegment.nameEn, result.segments[data.locationSegment.id]?.name)
        assertEquals(data.locationSegmentation.nameEn, result.segmentations[data.locationSegmentation.id]?.name)
        assertEquals(data.locationSegmentation.groupingOrder,
            result.segmentations[data.locationSegmentation.id]!!.groupingOrder)
        assertEquals(data.resourceType.id, result.resources[data.resource.id]!!.resourceTypeId)
        assertEquals(data.resourceType.nameEn, result.resourceTypes[data.resourceType.id]!!.name)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, getResult.transfer.requestType)
        assertEquals(TransferRequestStatusDto.PENDING, getResult.transfer.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, getResult.transfer.createdBy)
        assertTrue(getResult.transfer.transferVotes.votes.isEmpty())
        checkResponsible(getResult, data.folderOne, data.folderTwo)
        assertEquals(1, getResult.transfer.parameters.provisionTransfers.size)
        val frontProvisionTransferDto = getResult.transfer.parameters.provisionTransfers[0]
        assertEquals(data.accountOne.id, frontProvisionTransferDto.sourceAccountId)
        assertEquals(data.folderOne.id, frontProvisionTransferDto.sourceFolderId)
        assertEquals("1", frontProvisionTransferDto.sourceServiceId)
        assertEquals(data.accountTwo.id, frontProvisionTransferDto.destinationAccountId)
        assertEquals(data.folderTwo.id, frontProvisionTransferDto.destinationFolderId)
        assertEquals("2", frontProvisionTransferDto.destinationServiceId)
        assertNotNull(frontProvisionTransferDto.sourceAccountTransfers)
        assertNotNull(frontProvisionTransferDto.destinationAccountTransfers)
        assertTrue(frontProvisionTransferDto.sourceAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "-100" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertTrue(frontProvisionTransferDto.destinationAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "100" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertEquals(2, helper.mailSender.counter - counter)
        assertTrue(result.accounts.contains(data.accountOne.id))
        assertEquals(data.accountOne.displayName.get(), result.accounts[data.accountOne.id]?.name)
        assertEquals(data.accountOne.folderId, result.accounts[data.accountOne.id]?.folderId)
        assertTrue(result.accounts.contains(data.accountTwo.id))
        assertEquals(data.accountTwo.displayName.get(), result.accounts[data.accountTwo.id]?.name)
        assertEquals(data.accountTwo.folderId, result.accounts[data.accountTwo.id]?.folderId)
        val ticket = helper.trackerClientStub.getTicket(getResult.transfer.trackerIssueKey.get())!!
        assertEquals(setOf("dispenser", "d"), ticket.tags.toSet())
        assertTrue(ticket.components.contains(provisionComponentId))
        assertFalse(ticket.components.contains(reserveComponentId))
    }

    @Test
    fun createTransferRequestWithReserveAccount() {
        val data = helper.prepareData()
        val deltaUnit = BYTES
        val counter = helper.mailSender.counter
        val (reserveAccount, reserveAccountQuota) = helper.createAccountWithQuota(
            data.resource, data.folderOne, 150, 0, 0,
            data.accountSpaceModel, reserveType = AccountReserveType.PROVIDER
        )
        helper.attachProviderResponsible(data.provider, TestUsers.USER_1_STAFF_ID)
        val result = helper.createRequest(provisionRequest(reserveAccount, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 150),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertNotNull(result)
        assertEquals(TransferRequestSubtypeDto.RESERVE_PROVISION_TRANSFER, result.transfer.requestSubtype)
        val getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_1_UID))
        assertNotNull(getResult)
        assertEquals(TransferRequestSubtypeDto.RESERVE_PROVISION_TRANSFER, getResult.transfer.requestSubtype)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, getResult.transfer.requestType)
        assertEquals(TransferRequestStatusDto.PENDING, getResult.transfer.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, getResult.transfer.createdBy)
        assertTrue(getResult.transfer.transferVotes.votes.isEmpty())
        assertTrue(getResult.transfer.transferResponsible.responsibleGroups.isEmpty())
        assertTrue(getResult.transfer.transferResponsible.reserveResponsible.isEmpty)
        assertEquals(1, getResult.transfer.transferResponsible.providerResponsible.size)
        val providerResponsibleDto = getResult.transfer.transferResponsible.providerResponsible.first()
        assertEquals(FrontTransferRequestProviderResponsibleDto(TestUsers.USER_1_ID, listOf(data.provider.id)),
            providerResponsibleDto)
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
            it.resourceId == data.resource.id && it.delta == "-150" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertTrue(frontProvisionTransferDto.destinationAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "150" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertEquals(1, helper.mailSender.counter - counter)
        assertTrue(result.accounts.contains(reserveAccount.id))
        assertEquals(reserveAccount.displayName.get(), result.accounts[reserveAccount.id]?.name)
        assertEquals(reserveAccount.folderId, result.accounts[reserveAccount.id]?.folderId)
        assertTrue(result.accounts.contains(data.accountTwo.id))
        assertEquals(data.accountTwo.displayName.get(), result.accounts[data.accountTwo.id]?.name)
        assertEquals(data.accountTwo.folderId, result.accounts[data.accountTwo.id]?.folderId)
        val ticket = helper.trackerClientStub.getTicket(getResult.transfer.trackerIssueKey.get())!!
        assertEquals(setOf("dispenser", "d"), ticket.tags.toSet())
        assertTrue(ticket.components.contains(reserveComponentId))
        assertFalse(ticket.components.contains(provisionComponentId))
    }

    @Test
    fun getTransferRequestEmptyFolderTransfersTest() {
        val data = helper.prepareData()
        val deltaUnit = BYTES
        val counter = helper.mailSender.counter
        val result = helper.createRequest(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 100),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertNotNull(result)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.transfer.requestSubtype)
        val getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_1_UID))
        assertNotNull(getResult)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, getResult.transfer.requestSubtype)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, getResult.transfer.requestType)
        assertEquals(TransferRequestStatusDto.PENDING, getResult.transfer.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, getResult.transfer.createdBy)
        assertTrue(getResult.transfer.transferVotes.votes.isEmpty())
        checkResponsible(getResult, data.folderOne, data.folderTwo)
        assertEquals(1, getResult.transfer.parameters.provisionTransfers.size)
        val frontProvisionTransferDto = getResult.transfer.parameters.provisionTransfers[0]
        assertEquals(data.accountOne.id, frontProvisionTransferDto.sourceAccountId)
        assertEquals(data.folderOne.id, frontProvisionTransferDto.sourceFolderId)
        assertEquals("1", frontProvisionTransferDto.sourceServiceId)
        assertEquals(data.accountTwo.id, frontProvisionTransferDto.destinationAccountId)
        assertEquals(data.folderTwo.id, frontProvisionTransferDto.destinationFolderId)
        assertEquals("2", frontProvisionTransferDto.destinationServiceId)
        assertNotNull(frontProvisionTransferDto.sourceAccountTransfers)
        assertNotNull(frontProvisionTransferDto.destinationAccountTransfers)
        assertTrue(frontProvisionTransferDto.sourceAccountTransfers.all {
            it.resourceId == data.resource.id && it.delta == "-100" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertTrue(frontProvisionTransferDto.destinationAccountTransfers.all {
            it.resourceId == data.resource.id && it.delta == "100" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertEquals(2, helper.mailSender.counter - counter)
    }

    @Test
    fun createAndApplyTransferRequestWithConfirmationTest() {
        val data = helper.prepareData()
        val (ballastResource, _) = helper.createResource(data.provider, "ballastResource", data.accountSpaceModel)
        helper.createAccountsQuotas(listOf(data.accountOne),
            ballastResource, 100, 100, 0)
        helper.createFolderQuotas(listOf(data.folderOne), ballastResource, 100, 0)
        val (ballastResourceWithZeroProvision, _) = helper.createResource(
            data.provider, "ballastResourceWithZeroProvision", data.accountSpaceModel
        )
        helper.createAccountsQuotas(listOf(data.accountOne),
            ballastResourceWithZeroProvision, 100, 100, 0)
        helper.createFolderQuotas(listOf(data.folderOne), ballastResourceWithZeroProvision, 100, 0)
        helper.createAccountsQuotas(listOf(data.accountTwo),
            ballastResourceWithZeroProvision, 0, 0, 0)
        helper.stubProviderService.reset()
        val failureResponse = GrpcResponse.failure<Account>(
            //some unretryable status
            StatusRuntimeException(Status.PERMISSION_DENIED)
        )
        //to prevent pre-create account fetch
        helper.stubProviderService.addGetAccountResponses(data.accountOne.id, listOf(failureResponse))
        helper.stubProviderService.addGetAccountResponses(data.accountTwo.id, listOf(failureResponse))
        helper.addGetAccountAnswers(data.accountOne, listOf(data.accountQuotaOne), data.accountTwo,
            listOf(data.accountQuotaTwo), data.resourceType, "bytes", "VLA", "location")
        helper.setupMoveProvisionAnswers(data.accountOne, data.accountTwo,
            data.accountQuotaOne.providedQuota - 100L, data.accountQuotaOne.allocatedQuota,
            data.accountQuotaTwo.providedQuota + 100L, data.accountQuotaTwo.allocatedQuota,
            data.resourceType, "bytes", "VLA", "location", 42L)
        val counter = helper.mailSender.counter
        val result = helper.createRequest(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 100,
            addConfirmation = true), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertNotNull(result)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.transfer.requestSubtype)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.transfer.requestType)
        assertEquals(TransferRequestStatusDto.EXECUTING, result.transfer.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, result.transfer.createdBy)
        val votes = result.transfer.transferVotes.votes
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.userId == TestUsers.SERVICE_1_QUOTA_MANAGER)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(setOf(data.folderOne.serviceId.toString(), data.folderTwo.serviceId.toString()),
            vote.serviceIds.toSet())
        checkResponsible(result, data.folderOne, data.folderTwo)
        assertEquals(1, result.transfer.parameters.provisionTransfers.size)
        assertEquals(0, helper.mailSender.counter - counter)
        val updatedQuotas = helper.rwTx {
            helper.accountQuotaDao.getAllByAccountIds(it, Tenants.DEFAULT_TENANT_ID,
                setOf(data.accountOne.id, data.accountTwo.id))
        }!!
        val updatedQuotaByAccountId = updatedQuotas
            .filter { it.resourceId == data.resource.id }
            .associateBy { it.accountId }
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
        val folderQuotasByFolderId = updatedFolderQuotas
            .filter { it.resourceId == data.resource.id }
            .associateBy { it.folderId }
        val sourceFolderQuota = folderQuotasByFolderId[data.folderOne.id]!!
        assertEquals(-100, sourceFolderQuota.quota - data.quotaOne.quota)
        assertEquals(data.quotaOne.balance, sourceFolderQuota.balance)
        assertEquals(data.quotaOne.frozenQuota, sourceFolderQuota.frozenQuota)
        val destFolderQuota = folderQuotasByFolderId[data.folderTwo.id]!!
        assertEquals(100, destFolderQuota.quota - data.quotaTwo.quota)
        assertEquals(data.quotaTwo.balance, destFolderQuota.balance)
        assertEquals(data.quotaTwo.frozenQuota, destFolderQuota.frozenQuota)

        val request = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.APPLIED, request.transfer.status)
        assertTrue(request.transfer.applicationDetails.isPresent)
        val applicationDetailsDto = request.transfer.applicationDetails.orElseThrow()
        val frontProvisionTransferDto = request.transfer.parameters.provisionTransfers[0]
        assertNotNull(frontProvisionTransferDto.operationId)
        assertEquals(FrontTransferOperationStatusDto.COMPLETED,
            applicationDetailsDto.operationStatusById[frontProvisionTransferDto.operationId])
        assertTrue(applicationDetailsDto.transferErrorsByOperationId.isEmpty())
        val pendingRequest = helper.rwTx {
            helper.pendingTransferRequestsDao.getById(it, request.transfer.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(pendingRequest.isEmpty)
        val transferRequestModelO = helper.rwTx {
            helper.transferRequestDao.getById(it, request.transfer.id, Tenants.DEFAULT_TENANT_ID)
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
        val ticket = helper.trackerClientStub.getTicket(request.transfer.trackerIssueKey.get())!!
        assertEquals(TrackerClientStub.TicketStatus.CLOSED, ticket.status)
        checkIndices(transferRequestModel, TransferRequestStatus.APPLIED)

        val folderOneOperationLog = helper.rwTx {
            helper.folderOperationLogDao.getFirstPageByFolder(
                it, Tenants.DEFAULT_TENANT_ID, data.folderOne.id, SortOrderDto.DESC, 1
            )
        }!![0]
        assertEquals(FolderOperationType.PROVISION_TRANSFER, folderOneOperationLog.operationType)
        assertEquals(OperationPhase.CLOSE, folderOneOperationLog.operationPhase.get())
        assertEquals(100, folderOneOperationLog.newProvisions.get(data.accountOne.id).get(data.resource.id).provision)
        val folderOneTransferMeta = folderOneOperationLog.transferMeta.get()
        assertEquals(result.transfer.id, folderOneTransferMeta.transferRequestId)
        assertEquals(RoleInTransfer.SOURCE, folderOneTransferMeta.roleInTransfer)
        val folderOneAnother = folderOneTransferMeta.anotherParticipants.first()
        assertEquals(data.folderTwo.serviceId, folderOneAnother.serviceId)
        assertEquals(data.folderTwo.id, folderOneAnother.folderId)
        assertEquals(data.accountTwo.id, folderOneAnother.accountId)

        val folderTwoOperationLog = helper.rwTx {
            helper.folderOperationLogDao.getFirstPageByFolder(
                it, Tenants.DEFAULT_TENANT_ID, data.folderTwo.id, SortOrderDto.DESC, 1
            )
        }!![0]
        assertEquals(FolderOperationType.PROVISION_TRANSFER, folderTwoOperationLog.operationType)
        assertEquals(OperationPhase.CLOSE, folderTwoOperationLog.operationPhase.get())
        assertEquals(200, folderTwoOperationLog.newProvisions.get(data.accountTwo.id).get(data.resource.id).provision)
        val folderTwoTransferMeta = folderTwoOperationLog.transferMeta.get()
        assertEquals(result.transfer.id, folderTwoTransferMeta.transferRequestId)
        assertEquals(RoleInTransfer.DESTINATION, folderTwoTransferMeta.roleInTransfer)
        val folderTwoAnother = folderTwoTransferMeta.anotherParticipants.first()
        assertEquals(data.folderOne.serviceId, folderTwoAnother.serviceId)
        assertEquals(data.folderOne.id, folderTwoAnother.folderId)
        assertEquals(data.accountOne.id, folderTwoAnother.accountId)

        val folderHistory = helper.folderHistoryRequest(data.folderTwo.id)
        val transferMeta = folderHistory.page.items[0].transferMeta.get()
        assertEquals(result.transfer.id, transferMeta.transferRequestId)
        assertEquals(RoleInTransfer.DESTINATION, transferMeta.roleInTransfer)
        val anotherParticipant = transferMeta.anotherParticipants.first()
        assertEquals(data.folderOne.serviceId, anotherParticipant.serviceId)
        assertEquals(data.folderOne.id, anotherParticipant.folderId)
        assertEquals(data.accountOne.id, anotherParticipant.accountId)
        assertEquals("Dispenser", folderHistory.servicesById[data.folderOne.serviceId]!!.name)
        assertEquals("Test", folderHistory.accountsById[data.accountOne.id]!!.displayName)
        val logs = LogCollectingFilter.events()
        val moveProvisionLogs = logs.filter { it.loggerName.endsWith("ProvidersIntegrationService") }
        assertTrue(moveProvisionLogs.isNotEmpty())
        assertTrue(moveProvisionLogs.any { log ->
            val mdc = log.contextData.toMap()
            request.transfer.id == mdc[MdcKey.COMMON_TRANSFER_REQUEST_ID]
                && operationId == mdc[MdcKey.COMMON_OPERATION_ID]
        })

        val moveProvisionRequest: MoveProvisionRequest = helper.stubProviderService.moveProvisionRequests.first.t1
        val sourceKnownResources = moveProvisionRequest.knownSourceProvisionsList[0].knownProvisionsList
            .map { ResourceComplexKey(it.resourceKey) }.toSet()
        val destinationKnownResources = moveProvisionRequest.knownDestinationProvisionsList[0].knownProvisionsList
            .map { ResourceComplexKey(it.resourceKey) }.toSet()
        assertEquals(sourceKnownResources, destinationKnownResources)
        val sourceUpdatedResources = moveProvisionRequest.updatedSourceProvisionsList
            .map { ResourceComplexKey(it.resourceKey) }.toSet()
        assertEquals(sourceKnownResources, sourceUpdatedResources)
        val destinationUpdatedResources = moveProvisionRequest.updatedDestinationProvisionsList
            .map { ResourceComplexKey(it.resourceKey) }.toSet()
        assertEquals(
            sourceKnownResources.mapToSet { it.resourceTypeKey },
            destinationUpdatedResources.mapToSet { it.resourceTypeKey })
        assertEquals(sourceKnownResources, destinationUpdatedResources)
    }

    @Test
    fun putTransferRequestWithConfirmationTest() {
        val data = helper.prepareData()
        helper.stubProviderService.reset()
        helper.addGetAccountAnswers(data.accountOne, listOf(data.accountQuotaOne), data.accountTwo,
            listOf(data.accountQuotaTwo), data.resourceType, "bytes", "VLA", "location")
        helper.setupMoveProvisionAnswers(data.accountOne, data.accountTwo,
            data.accountQuotaOne.providedQuota - 100L, data.accountQuotaOne.allocatedQuota,
            data.accountQuotaTwo.providedQuota + 100L, data.accountQuotaTwo.allocatedQuota,
            data.resourceType, "bytes", "VLA", "location", 42L)
        val deltaUnit = BYTES
        val counter = helper.mailSender.counter
        var result = helper.createRequest(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 10),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertNotNull(result)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.transfer.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.transfer.requestSubtype)
        assertEquals(TransferRequestStatusDto.PENDING, result.transfer.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, result.transfer.createdBy)
        assertTrue(result.transfer.transferVotes.votes.isEmpty())
        checkResponsible(result, data.folderOne, data.folderTwo)
        assertEquals(1, result.transfer.parameters.provisionTransfers.size)
        assertEquals(2, helper.mailSender.counter - counter)
        checkIndices(result.transfer.id, TransferRequestStatus.PENDING)
        result = helper.putRequest(result, provisionRequestPut(100, description = "foobar",
            addConfirmation = true, fromAccount = data.accountOne, fromFolder = data.folderOne,
            toAccount = data.accountTwo, toFolder = data.folderTwo, resource = data.resource),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.EXECUTING, result.transfer.status)
        val votes = result.transfer.transferVotes.votes
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.userId == TestUsers.SERVICE_1_QUOTA_MANAGER)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(setOf(data.folderOne.serviceId.toString(), data.folderTwo.serviceId.toString()),
            vote.serviceIds.toSet())
        val frontProvisionTransferDto = result.transfer.parameters.provisionTransfers[0]
        assertTrue(frontProvisionTransferDto.sourceAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "-100" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertTrue(frontProvisionTransferDto.destinationAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "100" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        val updatedQuotas = helper.tableClient.usingSessionMonoRetryable {
            it.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { tx ->
                helper.accountQuotaDao.getAllByAccountIds(tx, Tenants.DEFAULT_TENANT_ID,
                    setOf(data.accountOne.id, data.accountTwo.id))
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

        val request = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.APPLIED, request.transfer.status)
        val pendingRequest = helper.rwTx {
            helper.pendingTransferRequestsDao.getById(it, request.transfer.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(pendingRequest.isEmpty)
        val transferRequestModelO = helper.rwTx {
            helper.transferRequestDao.getById(it, request.transfer.id, Tenants.DEFAULT_TENANT_ID)
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
        val ticket = helper.trackerClientStub.getTicket(request.transfer.trackerIssueKey.get())!!
        assertEquals(TrackerClientStub.TicketStatus.CLOSED, ticket.status)
        checkIndices(transferRequestModel, TransferRequestStatus.APPLIED)
    }

    @Test
    fun putTransferRequestTest() {
        val data = helper.prepareData()
        helper.stubProviderService.reset()
        helper.addGetAccountAnswers(data.accountOne, listOf(data.accountQuotaOne), data.accountTwo,
            listOf(data.accountQuotaTwo), data.resourceType, "bytes", "VLA", "location")
        helper.setupMoveProvisionAnswers(data.accountOne, data.accountTwo,
            data.accountQuotaOne.providedQuota - 100L, data.accountQuotaOne.allocatedQuota,
            data.accountQuotaTwo.providedQuota + 100L, data.accountQuotaTwo.allocatedQuota,
            data.resourceType, "bytes", "VLA", "location", 42L)
        val deltaUnit = BYTES
        val counter = helper.mailSender.counter
        var result = helper.createRequest(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 10),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertNotNull(result)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.transfer.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.transfer.requestSubtype)
        assertEquals(TransferRequestStatusDto.PENDING, result.transfer.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, result.transfer.createdBy)
        assertTrue(result.transfer.transferVotes.votes.isEmpty())
        checkResponsible(result, data.folderOne, data.folderTwo)
        assertEquals(1, result.transfer.parameters.provisionTransfers.size)
        assertEquals(2, helper.mailSender.counter - counter)
        checkIndices(result.transfer.id, TransferRequestStatus.PENDING)
        result = helper.putRequest(result, provisionRequestPut(100, description = "foobar",
            addConfirmation = false, fromAccount = data.accountOne, fromFolder = data.folderOne,
            toAccount = data.accountTwo, toFolder = data.folderTwo, resource = data.resource),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_2_UID))
        assertEquals(TransferRequestStatusDto.PENDING, result.transfer.status)
        assertEquals(data.accountSpaceModel!!.nameEn, result.accountsSpaces[data.accountSpaceModel.id]!!.name)
        assertEquals(
            data.accountOne.accountsSpacesId.orElseThrow(),
            result.accounts[data.accountOne.id]!!.accountsSpacesId
        )
        assertEquals(
            data.accountTwo.accountsSpacesId.orElseThrow(),
            result.accounts[data.accountTwo.id]!!.accountsSpacesId
        )
        assertEquals(data.locationSegment.id,
            result.resources[data.resource.id]?.segments?.get(data.locationSegmentation.id))
        assertEquals(data.locationSegment.id,
            result.accountsSpaces[data.accountSpaceModel.id]?.segments?.get(data.locationSegmentation.id))
        assertEquals(data.locationSegment.nameEn, result.segments[data.locationSegment.id]?.name)
        assertEquals(data.locationSegmentation.nameEn, result.segmentations[data.locationSegmentation.id]?.name)
        assertEquals(data.locationSegmentation.groupingOrder,
            result.segmentations[data.locationSegmentation.id]!!.groupingOrder)
        assertEquals(data.resourceType.id, result.resources[data.resource.id]!!.resourceTypeId)
        assertEquals(data.resourceType.nameEn, result.resourceTypes[data.resourceType.id]!!.name)
        val frontProvisionTransferDto = result.transfer.parameters.provisionTransfers[0]
        assertTrue(frontProvisionTransferDto.sourceAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "-100" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertTrue(frontProvisionTransferDto.destinationAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "100" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        checkIndices(result.transfer.id, TransferRequestStatus.PENDING)
    }

    @Test
    fun voteTransferRequestTest() {
        val data = helper.prepareData()
        val counter = helper.mailSender.counter
        var result = helper.createRequest(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 100),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertNotNull(result)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.transfer.requestSubtype)
        assertEquals(data.accountSpaceModel!!.nameEn, result.accountsSpaces[data.accountSpaceModel.id]!!.name)
        assertEquals(
            data.accountOne.accountsSpacesId.orElseThrow(),
            result.accounts[data.accountOne.id]!!.accountsSpacesId
        )
        assertEquals(
            data.accountTwo.accountsSpacesId.orElseThrow(),
            result.accounts[data.accountTwo.id]!!.accountsSpacesId
        )
        assertEquals(data.locationSegment.id,
            result.resources[data.resource.id]?.segments?.get(data.locationSegmentation.id))
        assertEquals(data.locationSegment.id,
            result.accountsSpaces[data.accountSpaceModel.id]?.segments?.get(data.locationSegmentation.id))
        assertEquals(data.locationSegment.nameEn, result.segments[data.locationSegment.id]?.name)
        assertEquals(data.locationSegmentation.nameEn, result.segmentations[data.locationSegmentation.id]?.name)
        assertEquals(data.locationSegmentation.groupingOrder,
            result.segmentations[data.locationSegmentation.id]!!.groupingOrder)
        assertEquals(data.resourceType.id, result.resources[data.resource.id]!!.resourceTypeId)
        assertEquals(data.resourceType.nameEn, result.resourceTypes[data.resourceType.id]!!.name)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.transfer.requestType)
        assertEquals(TransferRequestStatusDto.PENDING, result.transfer.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, result.transfer.createdBy)
        assertTrue(result.transfer.transferVotes.votes.isEmpty())
        checkResponsible(result, data.folderOne, data.folderTwo)
        assertEquals(1, result.transfer.parameters.provisionTransfers.size)
        assertEquals(2, helper.mailSender.counter - counter)
        result = helper.voteRequest(result, FrontTransferRequestVotingDto(TransferRequestVoteTypeDto.CONFIRM),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.transfer.requestType)
        assertEquals(TransferRequestStatusDto.EXECUTING, result.transfer.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, result.transfer.createdBy)
        val votes = result.transfer.transferVotes.votes
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.userId == TestUsers.SERVICE_1_QUOTA_MANAGER)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(setOf(data.folderOne.serviceId.toString(), data.folderTwo.serviceId.toString()),
            vote.serviceIds.toSet())
        assertEquals(1, result.transfer.parameters.provisionTransfers.size)
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
                helper.resourcesDao.updateResourceRetryable(txSession, ResourceModel.builder(data.resource)
                    .virtual(virtual)
                    .managed(managed)
                    .deleted(deleted)
                    .build())
            }
        }.block()
        val result = helper.createRequestResponse(
            provisionRequest(data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource, 150),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
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
            provisionRequest(data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource, 1000),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
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
            provisionRequest(data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource, -1000),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
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
        val result = helper.createRequestResponse(
            FrontCreateTransferRequestDto(null, TransferRequestTypeDto.PROVISION_TRANSFER,
                FrontCreateTransferRequestParametersDto.builder()
                    .addProvisionTransfer(FrontCreateProvisionTransferDto(
                        sourceAccountId = data.accountOne.id,
                        sourceFolderId = data.folderOne.id,
                        sourceServiceId = data.folderOne.serviceId.toString(),
                        destinationAccountId = data.accountTwo.id,
                        destinationFolderId = data.folderTwo.id,
                        destinationServiceId = data.folderTwo.serviceId.toString(),
                        sourceAccountTransfers = listOf(
                            FrontCreateQuotaResourceTransferDto(data.resource.id, fromAccountDiff.toString(),
                                data.resource.baseUnitId)
                        ),
                        destinationAccountTransfers = listOf(
                            FrontCreateQuotaResourceTransferDto(data.resource.id, toAccountDiff.toString(),
                                data.resource.baseUnitId)
                        )
                    ))
                    .build(),
                false, null), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertTrue("Transfer request has non-zero resource quotas sum." in result.errors,
            "Actual errors: ${result.errors}")
    }

    @Test
    fun createAndGetTransferRequestWithMultipleAccountPairs() {
        val data = helper.prepareData()
        val (accountThree, _) = helper.createAccountWithQuota(data.resource, data.folderOne,
            100, 0, 0, data.accountSpaceModel, "Account3")
        val (accountFour, _) = helper.createAccountWithQuota(data.resource, data.folderTwo,
            200, 0, 0, data.accountSpaceModel, "Account4")
        val transferRequestDto = helper.createRequest(
            provisionRequest(listOf(
                provisionTransfer(data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource, 100),
                provisionTransfer(accountThree, data.folderOne, accountFour, data.folderTwo, data.resource, 50),
            )),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID)
        )
        val getResult = helper.getRequest(transferRequestDto.transfer.id,
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        val provisionTransfers = getResult.transfer.parameters.provisionTransfers
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, getResult.transfer.requestSubtype)
        assertEquals(2, provisionTransfers.size)
        assertEquals(data.accountSpaceModel!!.nameEn, getResult.accountsSpaces[data.accountSpaceModel.id]!!.name)
        assertEquals(
            data.accountOne.accountsSpacesId.orElseThrow(),
            getResult.accounts[data.accountOne.id]!!.accountsSpacesId
        )
        assertEquals(
            data.accountTwo.accountsSpacesId.orElseThrow(),
            getResult.accounts[data.accountTwo.id]!!.accountsSpacesId
        )
        assertEquals(data.locationSegment.id,
            getResult.resources[data.resource.id]?.segments?.get(data.locationSegmentation.id))
        assertEquals(data.locationSegment.id,
            getResult.accountsSpaces[data.accountSpaceModel.id]?.segments?.get(data.locationSegmentation.id))
        assertEquals(data.locationSegment.nameEn, getResult.segments[data.locationSegment.id]?.name)
        assertEquals(data.locationSegmentation.nameEn, getResult.segmentations[data.locationSegmentation.id]?.name)
        assertEquals(data.locationSegmentation.groupingOrder,
            getResult.segmentations[data.locationSegmentation.id]!!.groupingOrder)
        assertEquals(data.resourceType.id, getResult.resources[data.resource.id]!!.resourceTypeId)
        assertEquals(data.resourceType.nameEn, getResult.resourceTypes[data.resourceType.id]!!.name)
        val transfersBySourceAccountId = provisionTransfers.associateBy { it.sourceAccountId }
        val transferOne = transfersBySourceAccountId[data.accountOne.id]!!
        assertEquals(data.accountTwo.id, transferOne.destinationAccountId)
        assertEquals(data.folderOne.id, transferOne.sourceFolderId)
        assertEquals(data.folderTwo.id, transferOne.destinationFolderId)
        assertEquals(1, transferOne.sourceAccountTransfers.size)
        assertEquals("-100", transferOne.sourceAccountTransfers.first().delta)
        assertEquals(data.resource.id, transferOne.sourceAccountTransfers.first().resourceId)
        assertEquals(1, transferOne.destinationAccountTransfers.size)
        assertEquals("100", transferOne.destinationAccountTransfers.first().delta)
        assertEquals(data.resource.id, transferOne.destinationAccountTransfers.first().resourceId)
        val transferTwo = transfersBySourceAccountId[accountThree.id]!!
        assertEquals(accountFour.id, transferTwo.destinationAccountId)
        assertEquals(data.folderOne.id, transferTwo.sourceFolderId)
        assertEquals(data.folderTwo.id, transferTwo.destinationFolderId)
        assertEquals(1, transferTwo.sourceAccountTransfers.size)
        assertEquals("-50", transferTwo.sourceAccountTransfers.first().delta)
        assertEquals(data.resource.id, transferTwo.sourceAccountTransfers.first().resourceId)
        assertEquals(1, transferTwo.destinationAccountTransfers.size)
        assertEquals("50", transferTwo.destinationAccountTransfers.first().delta)
        assertEquals(data.resource.id, transferTwo.destinationAccountTransfers.first().resourceId)
        assertTrue(getResult.transfer.trackerIssueKey.isPresent)
        val ticket = helper.trackerClientStub.getTicket(getResult.transfer.trackerIssueKey.get())!!
        assertDescriptionContains(ticket, """
            dispenser:Test:Test -> d:Test:Test
            ${data.resource.nameRu}: 100 B

        """.trimIndent())
        assertDescriptionContains(ticket, """
            dispenser:Test:Account3 -> d:Test:Account4
            ${data.resource.nameRu}: 50 B

        """.trimIndent())
    }

    @Test
    fun createTransferRequestWithResourceDuplicatesTest() {
        val data = helper.prepareData()
        val deltaUnit = BYTES
        val result = helper.createRequestResponse(
            provisionRequest(listOf(
                FrontCreateProvisionTransferDto(
                    sourceAccountId = data.accountOne.id,
                    sourceFolderId = data.folderOne.id,
                    sourceServiceId = data.folderOne.serviceId.toString(),
                    destinationAccountId = data.accountTwo.id,
                    destinationFolderId = data.folderTwo.id,
                    destinationServiceId = data.folderTwo.serviceId.toString(),
                    sourceAccountTransfers = listOf(
                        FrontCreateQuotaResourceTransferDto(data.resource.id, "-50", deltaUnit),
                        FrontCreateQuotaResourceTransferDto(data.resource.id, "-50", deltaUnit),
                    ),
                    destinationAccountTransfers = listOf(
                        FrontCreateQuotaResourceTransferDto(data.resource.id, "50", deltaUnit),
                        FrontCreateQuotaResourceTransferDto(data.resource.id, "50", deltaUnit),
                    )
                ))
            ), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Duplicate resource ids are not allowed."),
            result.fieldErrors["parameters.provisionTransfers.0.sourceAccountTransfers"])
        assertEquals(setOf("Duplicate resource ids are not allowed."),
            result.fieldErrors["parameters.provisionTransfers.0.destinationAccountTransfers"])
    }

    @Test
    fun createTransferRequestWithDifferentAccountProvidersTest() {
        val data = helper.prepareData()
        val anotherProvider = helper.createProvider()
        val (anotherResource, _) = helper.createResource(anotherProvider, "bar")
        val (accountThree, _) = helper.createAccountWithQuota(anotherResource, data.folderTwo, 100, 0, 0)
        val result = helper.createRequestResponse(
            provisionRequest(data.accountOne, data.folderOne, accountThree, data.folderTwo, anotherResource, 100),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
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
    fun createTransferRequestWithProviderWithoutMoveProvisionSupportTest() {
        val data = helper.prepareData()
        val anotherProvider = helper.createProvider(providerModel(moveProvisionSupported = false))
        val (anotherResource, _) = helper.createResource(anotherProvider, "bar")
        val (accountThree, _) = helper.createAccountWithQuota(anotherResource, data.folderOne, 100, 0, 0)
        val (accountFour, _) = helper.createAccountWithQuota(anotherResource, data.folderTwo, 100, 0, 0)
        val result = helper.createRequestResponse(
            provisionRequest(accountThree, data.folderOne, accountFour, data.folderTwo, anotherResource, 100),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Provision move is not supported for provider '${anotherProvider.nameEn}'."),
            result.fieldErrors["parameters.provisionTransfers.0"])
    }

    @Test
    fun createTransferRequestWithMoreThanTwoFoldersTest() {
        val data = helper.prepareData()
        val (anotherFolder, _) = helper.createFolderWithQuota(3L, data.resource, 200, 0)
        val (anotherAccount, _) = helper.createAccountWithQuota(data.resource, anotherFolder, 100, 0, 0)
        val result = helper.createRequestResponse(
            provisionRequest(listOf(
                provisionTransfer(data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource, 10),
                provisionTransfer(data.accountOne, data.folderOne, anotherAccount, anotherFolder, data.resource, 10)
            )), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Cannot set more than two folders for transfer."),
            result.fieldErrors["parameters.provisionTransfers"])
    }

    @Test
    fun createAndGetTransferRequestWithMultipleTransfersForOneAccountTest() {
        val data = helper.prepareData()
        val (anotherAccount, _) = helper.createAccountWithQuota(data.resource, data.folderTwo, 100, 0, 0,
            data.accountSpaceModel, "AnotherAccount")
        val (anotherResource, _) = helper.createResource(data.provider, "anotherResource", data.accountSpaceModel)
        helper.createAccountsQuotas(listOf(data.accountOne, data.accountTwo, anotherAccount),
            anotherResource, 100, 0, 0)
        helper.createFolderQuotas(listOf(data.folderOne, data.folderTwo), anotherResource, 1000, 900)
        val result = helper.createRequest(provisionRequest(listOf(
            provisionTransfer(data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource, 10),
            provisionTransfer(data.accountOne, data.folderOne, anotherAccount, data.folderTwo, anotherResource, 20))
        ), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        val transferRequestDto = helper.getRequest(result.transfer.id,
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.transfer.requestSubtype)
        assertEquals(2, transferRequestDto.transfer.parameters.provisionTransfers.size)
        val transfersByDestinationAccount = transferRequestDto.transfer.parameters.provisionTransfers
            .associateBy { it.destinationAccountId }
        assertEquals(2, transfersByDestinationAccount.size)
        val transferOne = transfersByDestinationAccount[data.accountTwo.id]!!
        assertEquals(data.accountOne.id, transferOne.sourceAccountId)
        assertEquals("-10", transferOne.sourceAccountTransfers.first().delta)
        assertEquals(data.resource.id, transferOne.sourceAccountTransfers.first().resourceId)
        val transferTwo = transfersByDestinationAccount[anotherAccount.id]!!
        assertEquals(data.accountOne.id, transferTwo.sourceAccountId)
        assertEquals("-20", transferTwo.sourceAccountTransfers.first().delta)
        assertEquals(anotherResource.id, transferTwo.sourceAccountTransfers.first().resourceId)
        assertTrue(transferRequestDto.transfer.trackerIssueKey.isPresent)
        val ticket = helper.trackerClientStub.getTicket(transferRequestDto.transfer.trackerIssueKey.get())!!
        assertDescriptionContains(ticket, """
            dispenser:Test:Test -> d:Test:Test
            ${data.resource.nameRu}: 10 B

        """.trimIndent())
        assertDescriptionContains(ticket, """
            dispenser:Test:Test -> d:Test:AnotherAccount
            ${anotherResource.nameRu}: 20 B

        """.trimIndent())
    }

    @Test
    fun createTransferRequestWithMultipleTransfersForOneAccountResourceTest() {
        val data = helper.prepareData()
        val (anotherAccount, _) = helper.createAccountWithQuota(data.resource, data.folderTwo, 100, 0, 0,
            data.accountSpaceModel, "AnotherAccount")
        val result = helper.createRequestResponse(provisionRequest(listOf(
            provisionTransfer(data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource, 10),
            provisionTransfer(data.accountOne, data.folderOne, anotherAccount, data.folderTwo, data.resource, 20))
        ), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Resource for account '${data.accountOne.displayName.get()}' (${data.accountOne.id})" +
            " already used in another transfer."),
            result.fieldErrors["parameters.provisionTransfers.1.sourceAccountTransfers.0.resourceId"])
    }

    @Test
    fun createTransferRequestWithDuplicatedAccountPairsTest() {
        val data = helper.prepareData()
        val result = helper.createRequestResponse(
            provisionRequest(listOf(
                provisionTransfer(data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource, 10),
                provisionTransfer(data.accountTwo, data.folderTwo, data.accountOne, data.folderOne, data.resource, 5)
            )), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Duplicated provision transfers are not allowed.",
            "Cannot set more than two folders for transfer."), result.fieldErrors["parameters.provisionTransfers"])
    }

    @Test
    fun createTransferRequestWithSameAccountPairTest() {
        val data = helper.prepareData()
        val result = helper.createRequestResponse(
            provisionRequest(listOf(
                provisionTransfer(data.accountOne, data.folderOne, data.accountOne, data.folderOne, data.resource, 10),
            )), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Cannot move quotas between the same account."),
            result.fieldErrors["parameters.provisionTransfers.0"])
    }

    @Test
    fun createAndGetTransferRequestWithOneFolderTest() {
        val data = helper.prepareData()
        val (anotherAccount, _) = helper.createAccountWithQuota(data.resource, data.folderOne, 100, 0, 0,
            data.accountSpaceModel, "AnotherAccount")
        val result = helper.createRequest(provisionRequest(listOf(
            provisionTransfer(data.accountOne, data.folderOne, anotherAccount, data.folderOne, data.resource, 10))
        ), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        val transferRequestDto = helper.getRequest(result.transfer.id,
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(1, transferRequestDto.transfer.parameters.provisionTransfers.size)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.transfer.requestSubtype)
        val transferOne = transferRequestDto.transfer.parameters.provisionTransfers[0]
        assertEquals(data.accountOne.id, transferOne.sourceAccountId)
        assertEquals(anotherAccount.id, transferOne.destinationAccountId)
        assertEquals(transferOne.destinationFolderId, transferOne.sourceFolderId)
        assertEquals("-10", transferOne.sourceAccountTransfers.first().delta)
        assertEquals(data.resource.id, transferOne.sourceAccountTransfers.first().resourceId)

        assertTrue(transferRequestDto.transfer.trackerIssueKey.isPresent)
        val ticket = helper.trackerClientStub.getTicket(transferRequestDto.transfer.trackerIssueKey.get())!!
        assertDescriptionContains(ticket, """
            Фолдер: Test (${data.folderOne.id})
            Аккаунты:
            * AnotherAccount
            * Test


            dispenser:Test:Test -> dispenser:Test:AnotherAccount
            ${data.resource.nameRu}: 10 B

        """.trimIndent())
    }

    @Test
    fun createAndGetTransferRequestWithSameServiceTest() {
        val data = helper.prepareData()
        val (anotherFolder, _) = helper.createFolderWithQuota(1L, data.resource, 1000, 0,
            folderName = "AnotherFolder")
        val (anotherAccount, _) = helper.createAccountWithQuota(data.resource, anotherFolder, 100, 0, 0,
            data.accountSpaceModel, accountName = "AnotherAccount")
        val result = helper.createRequest(provisionRequest(listOf(
            provisionTransfer(data.accountOne, data.folderOne, anotherAccount, anotherFolder, data.resource, 10))
        ), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        val transferRequestDto = helper.getRequest(result.transfer.id,
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.transfer.requestSubtype)
        assertEquals(1, transferRequestDto.transfer.parameters.provisionTransfers.size)
        val transferOne = transferRequestDto.transfer.parameters.provisionTransfers[0]
        assertEquals(data.accountOne.id, transferOne.sourceAccountId)
        assertEquals(anotherAccount.id, transferOne.destinationAccountId)
        assertEquals(data.folderOne.id, transferOne.sourceFolderId)
        assertEquals(anotherFolder.id, transferOne.destinationFolderId)
        assertEquals("-10", transferOne.sourceAccountTransfers.first().delta)
        assertEquals(data.resource.id, transferOne.sourceAccountTransfers.first().resourceId)

        assertTrue(transferRequestDto.transfer.trackerIssueKey.isPresent)
        val ticket = helper.trackerClientStub.getTicket(transferRequestDto.transfer.trackerIssueKey.get())!!
        assertDescriptionContains(ticket, """
            Фолдер: AnotherFolder (${anotherFolder.id})
            Аккаунт:
            * AnotherAccount

            Фолдер: Test (${data.folderOne.id})
            Аккаунт:
            * Test


            dispenser:Test:Test -> dispenser:AnotherFolder:AnotherAccount
            ${data.resource.nameRu}: 10 B

        """.trimIndent())
    }

    @Test
    fun createAndApplyTransferRequestWithProviderConflict() {
        val data = helper.prepareData()
        helper.stubProviderService.reset()
        helper.stubProviderService.addGetAccountResponses(data.accountOne.id, listOf(
            GrpcResponse.success(helper.toGrpcAccount(data.accountOne, listOf(data.accountQuotaOne), data.resourceType,
                "bytes", "VLA", "location")),
            GrpcResponse.success(helper.toGrpcAccount(data.accountOne, listOf(data.accountQuotaOne), data.resourceType,
                "bytes", "VLA", "location")),
            GrpcResponse.success(helper.toGrpcAccount(data.accountOne, listOf(data.accountQuotaOne), data.resourceType,
                "bytes", "VLA", "location")),
        ))
        helper.stubProviderService.addGetAccountResponses(data.accountTwo.id, listOf(
            GrpcResponse.success(helper.toGrpcAccount(data.accountTwo, listOf(data.accountQuotaTwo), data.resourceType,
                "bytes", "VLA", "location")),
            GrpcResponse.success(helper.toGrpcAccount(data.accountTwo, listOf(data.accountQuotaTwo), data.resourceType,
                "bytes", "VLA", "location")),
            GrpcResponse.success(helper.toGrpcAccount(data.accountTwo, listOf(data.accountQuotaTwo), data.resourceType,
                "bytes", "VLA", "location")),
        ))
        helper.stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION))))
        val counter = helper.mailSender.counter
        val result = helper.createRequest(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 100,
            addConfirmation = true), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertNotNull(result)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.transfer.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.transfer.requestSubtype)
        assertEquals(TransferRequestStatusDto.EXECUTING, result.transfer.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, result.transfer.createdBy)
        val votes = result.transfer.transferVotes.votes
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.userId == TestUsers.SERVICE_1_QUOTA_MANAGER)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(setOf(data.folderOne.serviceId.toString(), data.folderTwo.serviceId.toString()),
            vote.serviceIds.toSet())
        checkResponsible(result, data.folderOne, data.folderTwo)
        assertEquals(1, result.transfer.parameters.provisionTransfers.size)
        assertEquals(0, helper.mailSender.counter - counter)
        val updatedQuotas = helper.tableClient.usingSessionMonoRetryable {
            it.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { tx ->
                helper.accountQuotaDao.getAllByAccountIds(tx, Tenants.DEFAULT_TENANT_ID,
                    setOf(data.accountOne.id, data.accountTwo.id))
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
        val request = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.FAILED, request.transfer.status)
        val frontProvisionTransferDto = request.transfer.parameters.provisionTransfers[0]
        assertTrue(frontProvisionTransferDto.operationId != null)
        assertTrue(request.transfer.applicationDetails.isPresent)
        val applicationDetailsDto = request.transfer.applicationDetails.orElseThrow()
        assertEquals(FrontTransferOperationStatusDto.FAILED,
            applicationDetailsDto.operationStatusById[frontProvisionTransferDto.operationId])
        assertTrue(frontProvisionTransferDto.operationId in applicationDetailsDto.transferErrorsByOperationId)
        val operationErrors = applicationDetailsDto.transferErrorsByOperationId[frontProvisionTransferDto.operationId]!!
        assertTrue(Details.ERROR_FROM_PROVIDER in operationErrors.details)
        assertTrue(operationErrors.details[Details.ERROR_FROM_PROVIDER]!!.isNotEmpty())
        val pendingRequest = helper.rwTx {
            helper.pendingTransferRequestsDao.getById(it, request.transfer.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(pendingRequest.isEmpty)
        val transferRequestModelO = helper.rwTx {
            helper.transferRequestDao.getById(it, request.transfer.id, Tenants.DEFAULT_TENANT_ID)
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
        val ticket = helper.trackerClientStub.getTicket(request.transfer.trackerIssueKey.get())!!
        assertEquals(TrackerClientStub.TicketStatus.CLOSED, ticket.status)
        checkIndices(transferRequestModel, TransferRequestStatus.FAILED)
        val logs = LogCollectingFilter.events()
        val moveProvisionLogs = logs.filter { it.loggerName.endsWith("MoveProvisionOperationsRetryService")
            || it.loggerName.endsWith("ProvidersIntegrationService") }
        assertTrue(moveProvisionLogs.isNotEmpty())
        assertTrue(moveProvisionLogs.any { log ->
            val mdc = log.contextData.toMap()
            request.transfer.id == mdc[MdcKey.COMMON_TRANSFER_REQUEST_ID]
                && operationId == mdc[MdcKey.COMMON_OPERATION_ID]
        })
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("grpcResponses")
    fun createAndApplyTransferRequestWithProviderNotFatalError(
        testName: String,
        grpcResponseConfigurer: FrontTransferRequestsHelper.(FrontTransferRequestsHelper.Data) -> Unit
    ) {
        val data = helper.prepareData()
        helper.grpcResponseConfigurer(data)
        val counter = helper.mailSender.counter
        val result = helper.createRequest(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 100,
            addConfirmation = true), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertNotNull(result)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.transfer.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.transfer.requestSubtype)
        assertEquals(TransferRequestStatusDto.EXECUTING, result.transfer.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, result.transfer.createdBy)
        val votes = result.transfer.transferVotes.votes
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.userId == TestUsers.SERVICE_1_QUOTA_MANAGER)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(setOf(data.folderOne.serviceId.toString(), data.folderTwo.serviceId.toString()),
            vote.serviceIds.toSet())
        checkResponsible(result, data.folderOne, data.folderTwo)
        assertEquals(1, result.transfer.parameters.provisionTransfers.size)
        assertEquals(0, helper.mailSender.counter - counter)
        val updatedQuotas = helper.tableClient.usingSessionMonoRetryable {
            it.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { tx ->
                helper.accountQuotaDao.getAllByAccountIds(tx, Tenants.DEFAULT_TENANT_ID,
                    setOf(data.accountOne.id, data.accountTwo.id))
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
        val request = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.EXECUTING, request.transfer.status)
        val frontProvisionTransferDto = request.transfer.parameters.provisionTransfers[0]
        assertTrue(frontProvisionTransferDto.operationId != null)
        val frontApplicationDetailsO = request.transfer.applicationDetails
        assertTrue(frontApplicationDetailsO.isPresent)
        val applicationDetailsDto = frontApplicationDetailsO.orElseThrow()
        assertEquals(FrontTransferOperationStatusDto.EXECUTING,
            applicationDetailsDto.operationStatusById[frontProvisionTransferDto.operationId])
        assertTrue(applicationDetailsDto.transferErrorsByOperationId.isEmpty())
        val pendingRequest = helper.rwTx {
            helper.pendingTransferRequestsDao.getById(it, request.transfer.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(pendingRequest.isEmpty)
        val transferRequestModelO = helper.rwTx {
            helper.transferRequestDao.getById(it, request.transfer.id, Tenants.DEFAULT_TENANT_ID)
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
        val operationsInProgress = helper.rwTx {
            helper.operationsInProgressDao.getAllByTenantAccounts(it, Tenants.DEFAULT_TENANT_ID,
                setOf(data.accountOne.id, data.accountTwo.id))
        }!!
        operationsInProgress.forEach {
            assertEquals(operationId, it.operationId)
            assertEquals(1, it.retryCounter)
        }
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
        val ticket = helper.trackerClientStub.getTicket(request.transfer.trackerIssueKey.get())!!
        assertEquals(TrackerClientStub.TicketStatus.OPENED, ticket.status)
        checkIndices(transferRequestModel, TransferRequestStatus.EXECUTING)
        val logs = LogCollectingFilter.events()
        val moveProvisionLogs = logs.filter { it.loggerName.endsWith("MoveProvisionOperationsRetryService")
            || it.loggerName.endsWith("ProvidersIntegrationService") }
        assertTrue(moveProvisionLogs.isNotEmpty())
        assertTrue(moveProvisionLogs.any { log ->
            val mdc = log.contextData.toMap()
            request.transfer.id == mdc[MdcKey.COMMON_TRANSFER_REQUEST_ID]
                && operationId == mdc[MdcKey.COMMON_OPERATION_ID]
        })
    }

    @Test
    fun voteAndApplyTransferRequestTest() {
        val data = helper.prepareData()
        helper.stubProviderService.reset()
        helper.addGetAccountAnswers(data.accountOne, listOf(data.accountQuotaOne), data.accountTwo,
            listOf(data.accountQuotaTwo), data.resourceType, "bytes", "VLA", "location")
        helper.setupMoveProvisionAnswers(data.accountOne, data.accountTwo,
            data.accountQuotaOne.providedQuota - 100L, data.accountQuotaOne.allocatedQuota,
            data.accountQuotaTwo.providedQuota + 100L, data.accountQuotaTwo.allocatedQuota,
            data.resourceType, "bytes", "VLA", "location", 42L)
        var result = helper.createRequest(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 100),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertNotNull(result)
        result = helper.voteRequest(result, FrontTransferRequestVotingDto(TransferRequestVoteTypeDto.CONFIRM),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.transfer.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.transfer.requestSubtype)
        assertEquals(TransferRequestStatusDto.EXECUTING, result.transfer.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, result.transfer.createdBy)
        val votes = result.transfer.transferVotes.votes
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.userId == TestUsers.SERVICE_1_QUOTA_MANAGER)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(setOf(data.folderOne.serviceId.toString(), data.folderTwo.serviceId.toString()),
            vote.serviceIds.toSet())
        assertEquals(1, result.transfer.parameters.provisionTransfers.size)
        val frontProvisionTransferDto = result.transfer.parameters.provisionTransfers[0]
        assertTrue(frontProvisionTransferDto.operationId != null)
        assertTrue(result.transfer.applicationDetails.isPresent)
        val applicationDetailsDto = result.transfer.applicationDetails.orElseThrow()
        assertEquals(FrontTransferOperationStatusDto.EXECUTING,
            applicationDetailsDto.operationStatusById[frontProvisionTransferDto.operationId])
        val updatedQuotas = helper.tableClient.usingSessionMonoRetryable {
            it.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { tx ->
                helper.accountQuotaDao.getAllByAccountIds(tx, Tenants.DEFAULT_TENANT_ID,
                    setOf(data.accountOne.id, data.accountTwo.id))
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
        val request = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.APPLIED, request.transfer.status)
        val pendingRequest = helper.rwTx {
            helper.pendingTransferRequestsDao.getById(it, request.transfer.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(pendingRequest.isEmpty)
        val transferRequestModelO = helper.rwTx {
            helper.transferRequestDao.getById(it, request.transfer.id, Tenants.DEFAULT_TENANT_ID)
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
        val logs = LogCollectingFilter.events()
        val moveProvisionLogs = logs.filter { it.loggerName.endsWith("ProvidersIntegrationService") }
        assertTrue(moveProvisionLogs.isNotEmpty())
        assertTrue(moveProvisionLogs.any { log ->
            val mdc = log.contextData.toMap()
            request.transfer.id == mdc[MdcKey.COMMON_TRANSFER_REQUEST_ID]
                && operationId == mdc[MdcKey.COMMON_OPERATION_ID]
        })
    }

    @Test
    fun getTransferRequestPartlyApplied() {
        val data = helper.prepareData()
        val result = helper.createRequest(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 100,
            addConfirmation = true), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        helper.rwTx {
            mono {
                val transferRequest = helper.transferRequestDao.getById(it, result.transfer.id,
                    Tenants.DEFAULT_TENANT_ID).awaitSingle().get()
                helper.transferRequestDao.upsertOneRetryable(it, transferRequest.copyBuilder()
                    .status(TransferRequestStatus.PARTLY_APPLIED).build()).awaitSingleOrNull()
            }
        }
        val request = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.PARTLY_APPLIED, request.transfer.status)
    }

    @Test
    fun createTransferRequestWithWrongIdsTest() {
        val id = UUID.randomUUID().toString()
        val result = helper.createRequestResponse(
            provisionRequest(listOf(
                FrontCreateProvisionTransferDto(id, id, "4242", id, id, "4242",
                    listOf(FrontCreateQuotaResourceTransferDto(id, "-100", id)),
                    listOf(FrontCreateQuotaResourceTransferDto(id, "100", id))),
            )), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Account not found."),
            result.fieldErrors["parameters.provisionTransfers.0.sourceAccountId"])
        assertEquals(setOf("Account not found."),
            result.fieldErrors["parameters.provisionTransfers.0.destinationAccountId"])
        assertEquals(setOf("Folder not found."),
            result.fieldErrors["parameters.provisionTransfers.0.sourceFolderId"])
        assertEquals(setOf("Folder not found."),
            result.fieldErrors["parameters.provisionTransfers.0.destinationFolderId"])
        assertEquals(setOf("Service not found."),
            result.fieldErrors["parameters.provisionTransfers.0.sourceServiceId"])
        assertEquals(setOf("Service not found."),
            result.fieldErrors["parameters.provisionTransfers.0.destinationServiceId"])
    }

    @Test
    fun createTransferRequestWithWrongResourceIdsTest() {
        val data = helper.prepareData()
        val id = UUID.randomUUID().toString()
        val result = helper.createRequestResponse(
            provisionRequest(listOf(
                FrontCreateProvisionTransferDto(data.accountOne.id, data.folderOne.id,
                    data.folderOne.serviceId.toString(), data.accountTwo.id, data.folderTwo.id,
                    data.folderTwo.serviceId.toString(),
                    listOf(FrontCreateQuotaResourceTransferDto(id, "-100", id)),
                    listOf(FrontCreateQuotaResourceTransferDto(id, "100", id))),
            )), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        println(result)
        assertEquals(setOf("Resource not found."),
            result.fieldErrors["parameters.provisionTransfers.0.sourceAccountTransfers.0.resourceId"])
        assertEquals(setOf("Resource not found."),
            result.fieldErrors["parameters.provisionTransfers.0.destinationAccountTransfers.0.resourceId"])
    }

    @Test
    fun createAndApplyTransferRequestWithoutAccountSpacesTest() {
        val data = helper.prepareData(accountsSpacesSupported = false)
        helper.stubProviderService.reset()
        helper.addGetAccountAnswers(data.accountOne, listOf(data.accountQuotaOne), data.accountTwo,
            listOf(data.accountQuotaTwo), data.resourceType, "bytes", null, null)
        helper.setupMoveProvisionAnswers(data.accountOne, data.accountTwo,
            data.accountQuotaOne.providedQuota - 100L, data.accountQuotaOne.allocatedQuota,
            data.accountQuotaTwo.providedQuota + 100L, data.accountQuotaTwo.allocatedQuota,
            data.resourceType, "bytes", null, null, 42L)
        val counter = helper.mailSender.counter
        val result = helper.createRequest(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 100,
            addConfirmation = true), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertNotNull(result)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.transfer.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, result.transfer.requestSubtype)
        assertEquals(TransferRequestStatusDto.EXECUTING, result.transfer.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, result.transfer.createdBy)
        val votes = result.transfer.transferVotes.votes
        assertEquals(1, votes.size)
        val vote = votes[0]
        assertTrue(vote.userId == TestUsers.SERVICE_1_QUOTA_MANAGER)
        assertEquals(setOf(data.folderOne.id, data.folderTwo.id), vote.folderIds.toSet())
        assertEquals(setOf(data.folderOne.serviceId.toString(), data.folderTwo.serviceId.toString()),
            vote.serviceIds.toSet())
        checkResponsible(result, data.folderOne, data.folderTwo)
        assertEquals(1, result.transfer.parameters.provisionTransfers.size)
        assertEquals(0, helper.mailSender.counter - counter)
        val updatedQuotas = helper.rwTx {
            helper.accountQuotaDao.getAllByAccountIds(it, Tenants.DEFAULT_TENANT_ID,
                setOf(data.accountOne.id, data.accountTwo.id))
        }!!
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

        val request = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertEquals(TransferRequestStatusDto.APPLIED, request.transfer.status)
        assertTrue(request.transfer.applicationDetails.isPresent)
        val applicationDetailsDto = request.transfer.applicationDetails.orElseThrow()
        val frontProvisionTransferDto = request.transfer.parameters.provisionTransfers[0]
        assertNotNull(frontProvisionTransferDto.operationId)
        assertEquals(FrontTransferOperationStatusDto.COMPLETED,
            applicationDetailsDto.operationStatusById[frontProvisionTransferDto.operationId])
        assertTrue(applicationDetailsDto.transferErrorsByOperationId.isEmpty())
        val pendingRequest = helper.rwTx {
            helper.pendingTransferRequestsDao.getById(it, request.transfer.id, Tenants.DEFAULT_TENANT_ID)
        }!!
        assertTrue(pendingRequest.isEmpty)
        val transferRequestModelO = helper.rwTx {
            helper.transferRequestDao.getById(it, request.transfer.id, Tenants.DEFAULT_TENANT_ID)
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
        val ticket = helper.trackerClientStub.getTicket(request.transfer.trackerIssueKey.get())!!
        assertEquals(TrackerClientStub.TicketStatus.CLOSED, ticket.status)
        val logs = LogCollectingFilter.events()
        val moveProvisionLogs = logs.filter { it.loggerName.endsWith("ProvidersIntegrationService") }
        assertTrue(moveProvisionLogs.isNotEmpty())
        assertTrue(moveProvisionLogs.any { log ->
            val mdc = log.contextData.toMap()
            request.transfer.id == mdc[MdcKey.COMMON_TRANSFER_REQUEST_ID]
                && operationId == mdc[MdcKey.COMMON_OPERATION_ID]
        })
    }

    @Test
    fun getTransferRequestWithDeletedAccountTest() {
        val data = helper.prepareData()

        val deltaUnit = BYTES
        val counter = helper.mailSender.counter
        val result = helper.createRequest(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 100),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
        assertNotNull(result)

        val deletedAccount = AccountModel.Builder(data.accountTwo).setDeleted(true).build()
        helper.rwTx {
            helper.accountsDao.upsertOneRetryable(it, deletedAccount)
        }

        val getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_1_UID))
        assertNotNull(getResult)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, getResult.transfer.requestType)
        assertEquals(TransferRequestSubtypeDto.DEFAULT_PROVISION_TRANSFER, getResult.transfer.requestSubtype)
        assertEquals(TransferRequestStatusDto.PENDING, getResult.transfer.status)
        assertEquals(TestUsers.SERVICE_1_QUOTA_MANAGER, getResult.transfer.createdBy)
        assertTrue(getResult.transfer.transferVotes.votes.isEmpty())
        checkResponsible(getResult, data.folderOne, data.folderTwo)
        assertEquals(1, getResult.transfer.parameters.provisionTransfers.size)
        val frontProvisionTransferDto = getResult.transfer.parameters.provisionTransfers[0]
        assertEquals(data.accountOne.id, frontProvisionTransferDto.sourceAccountId)
        assertEquals(data.folderOne.id, frontProvisionTransferDto.sourceFolderId)
        assertEquals("1", frontProvisionTransferDto.sourceServiceId)
        assertEquals(deletedAccount.id, frontProvisionTransferDto.destinationAccountId)
        assertEquals(data.folderTwo.id, frontProvisionTransferDto.destinationFolderId)
        assertEquals("2", frontProvisionTransferDto.destinationServiceId)
        assertNotNull(frontProvisionTransferDto.sourceAccountTransfers)
        assertNotNull(frontProvisionTransferDto.destinationAccountTransfers)
        assertTrue(frontProvisionTransferDto.sourceAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "-100" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertTrue(frontProvisionTransferDto.destinationAccountTransfers.any {
            it.resourceId == data.resource.id && it.delta == "100" && it.deltaUnit == "B"
                && it.deltaUnitId == deltaUnit
        })
        assertEquals(2, helper.mailSender.counter - counter)
        assertTrue(getResult.accounts.contains(data.accountOne.id))
        assertEquals(data.accountOne.displayName.get(), getResult.accounts[data.accountOne.id]?.name)
        assertEquals(data.accountOne.folderId, getResult.accounts[data.accountOne.id]?.folderId)
        assertTrue(getResult.accounts.contains(deletedAccount.id))
        assertEquals(deletedAccount.displayName.get(), getResult.accounts[deletedAccount.id]?.name)
        assertEquals(deletedAccount.folderId, getResult.accounts[deletedAccount.id]?.folderId)
        assertFalse(getResult.accounts[data.accountOne.id]!!.deleted)
        assertTrue(getResult.accounts[deletedAccount.id]!!.deleted)
    }

    @Test
    fun createTransferRequestWithDeletedAccountFailTest() {
        val data = helper.prepareData()
        val deletedAccount = AccountModel.Builder(data.accountTwo).setDeleted(true).build()
        helper.rwTx {
            helper.accountsDao.upsertOneRetryable(it, deletedAccount)
        }
        val result = helper.createRequestResponse(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 150),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertTrue(
            result.fieldErrors["parameters.provisionTransfers.0.destinationAccountId"]
                ?.contains("Account not found.")
                ?: false
        )
    }

    @Test
    fun transferRequestExchangeSubtype() {
        val data = helper.prepareData()
        val (reserveAccount, reserveAccountQuota) = helper.createAccountWithQuota(
            data.resource, data.folderOne, 150, 0, 0,
            data.accountSpaceModel, reserveType = AccountReserveType.PROVIDER
        )
        val (anotherResource, _) = helper.createResource(data.provider, "anotherResource", data.accountSpaceModel)
        helper.createAccountsQuotas(listOf(data.accountTwo),
            anotherResource, 100, 0, 0)
        val result = helper.createRequest(FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(false)
            .parameters(FrontCreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(FrontCreateProvisionTransferDto(
                    sourceAccountId = reserveAccount.id,
                    sourceFolderId = data.folderOne.id,
                    sourceServiceId = data.folderOne.serviceId.toString(),
                    destinationAccountId = data.accountTwo.id,
                    destinationFolderId = data.folderTwo.id,
                    destinationServiceId = data.folderTwo.serviceId.toString(),
                    sourceAccountTransfers = listOf(
                        FrontCreateQuotaResourceTransferDto(data.resource.id, (-150).toString(),
                            data.resource.baseUnitId)
                    ),
                    destinationAccountTransfers = listOf(
                        FrontCreateQuotaResourceTransferDto(data.resource.id, 150.toString(),
                            data.resource.baseUnitId)
                    )
                ))
                .addProvisionTransfer(FrontCreateProvisionTransferDto(
                    sourceAccountId = data.accountTwo.id,
                    sourceFolderId = data.folderTwo.id,
                    sourceServiceId = data.folderTwo.serviceId.toString(),
                    destinationAccountId = reserveAccount.id,
                    destinationFolderId = data.folderOne.id,
                    destinationServiceId = data.folderOne.serviceId.toString(),
                    sourceAccountTransfers = listOf(
                        FrontCreateQuotaResourceTransferDto(anotherResource.id, (-100).toString(),
                            anotherResource.baseUnitId)
                    ),
                    destinationAccountTransfers = listOf(
                        FrontCreateQuotaResourceTransferDto(anotherResource.id, 100.toString(),
                            anotherResource.baseUnitId)
                    )
                ))
                .build())
            .build(),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))

        assertNotNull(result)
        assertEquals(TransferRequestSubtypeDto.EXCHANGE_PROVISION_TRANSFER, result.transfer.requestSubtype)
        val getResult = helper.getRequest(result.transfer.id, MockUser.uid(TestUsers.USER_1_UID))
        assertNotNull(getResult)
        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, getResult.transfer.requestType)
        assertEquals(TransferRequestSubtypeDto.EXCHANGE_PROVISION_TRANSFER, getResult.transfer.requestSubtype)

        val ticket = helper.trackerClientStub.getTicket(getResult.transfer.trackerIssueKey.get())!!
        assertEquals(setOf("dispenser", "d"), ticket.tags.toSet())
        assertTrue(ticket.components.contains(exchangeComponentId))
        assertFalse(ticket.components.contains(provisionComponentId))
    }

    @Test
    fun createTransferRequestWithOutOfSyncAccountTest() {
        val data = helper.prepareData(sourceProvided = 200, sourceAllocated = 50, sourceFrozen = 50)
        helper.stubProviderService.reset()
        helper.stubProviderService.addGetAccountResponse(data.accountOne.id, GrpcResponse.success(
            helper.toGrpcAccountBuilder(data.accountOne, listOf(), data.resourceType, "bytes", "VLA", "location")
                .addProvisions(helper.toGrpcProvision(180, 100, data.resourceType, "bytes", 42L))
                .build()
        ))
        helper.stubProviderService.addGetAccountResponse(data.accountTwo.id, GrpcResponse.success(helper.toGrpcAccount(
            data.accountTwo, listOf(data.accountQuotaTwo), data.resourceType, "bytes", "VLA", "location")))
        val result = helper.createRequestResponse(provisionRequest(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 100),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Not enough quota for provision transfer.\n" +
            "180 B is provided to the provider.\n" +
            "100 B is allocated in the provider.\n" +
            "50 B is in the process of transfer in another transfer request.\n" +
            "The available amount is 30 B."),
            result.fieldErrors["parameters.provisionTransfers.0.sourceAccountTransfers.0.delta"])
        val provisionSuggestedAmount = "${data.resource.id}.suggestedProvisionAmount"
        assertEquals(1, result.details[provisionSuggestedAmount]?.size)
        val provisionDetails = result.details[provisionSuggestedAmount]?.first() as Map<*, *>
        assertEquals("30", provisionDetails["rawAmount"])
        assertEquals("B", provisionDetails["rawUnit"])
        assertEquals(setOf("Transfer all not allocated quota."), result.details["suggestedProvisionAmountPrompt"])
    }
}
