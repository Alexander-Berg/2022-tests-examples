package ru.yandex.intranet.d.web.front.transfer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.UnitIds
import ru.yandex.intranet.d.UnitsEnsembleIds
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.folders.FolderModel
import ru.yandex.intranet.d.model.providers.ProviderModel
import ru.yandex.intranet.d.model.providers.RelatedCoefficient
import ru.yandex.intranet.d.model.providers.RelatedResourceMapping
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.util.JsonUtil
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsHelper.Companion.provisionTransfer
import ru.yandex.intranet.d.web.model.AmountDto
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateProvisionTransferDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaResourceTransferDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestParametersDto
import ru.yandex.intranet.d.web.model.transfers.front.dryrun.FrontDryRunCreateTransferRequestDto
import ru.yandex.intranet.d.web.model.transfers.front.dryrun.FrontDryRunSingleTransferResultDto
import java.util.stream.Stream

@IntegrationTest
class FrontDryRunTransferProvisionRequestsControllerTest {

    @Autowired
    lateinit var helper: FrontTransferRequestsHelper

    companion object {
        fun provisionTransferDryRun(
            fromAccount: AccountModel,
            fromFolder: FolderModel,
            toAccount: AccountModel,
            toFolder: FolderModel,
            resource: ResourceModel,
            provisionDelta: Long,
            prepareResponsible: Boolean = true,
            preparePermissions: Boolean = true,
        ) = FrontDryRunCreateTransferRequestDto(
            TransferRequestTypeDto.PROVISION_TRANSFER,
            FrontCreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(FrontCreateProvisionTransferDto(
                    sourceAccountId = fromAccount.id,
                    sourceFolderId = fromFolder.id,
                    sourceServiceId = fromFolder.serviceId.toString(),
                    destinationAccountId = toAccount.id,
                    destinationFolderId = toFolder.id,
                    destinationServiceId = toFolder.serviceId.toString(),
                    sourceAccountTransfers = listOf(FrontCreateQuotaResourceTransferDto(
                            resource.id, (-provisionDelta).toString(), resource.baseUnitId
                        )),
                    destinationAccountTransfers = listOf(FrontCreateQuotaResourceTransferDto(
                        resource.id, provisionDelta.toString(), resource.baseUnitId
                    )),
                ))
                .build(),
            prepareResponsible,
            preparePermissions,
            null
        )

        fun provisionTransferDryRun(
            provisionTransfers: List<FrontCreateProvisionTransferDto>,
            prepareResponsible: Boolean = true,
            preparePermissions: Boolean = true,
        ) = FrontDryRunCreateTransferRequestDto(
            TransferRequestTypeDto.PROVISION_TRANSFER,
            FrontCreateTransferRequestParametersDto.builder()
                .addProvisionTransfers(provisionTransfers)
                .build(),
            prepareResponsible,
            preparePermissions,
            null
        )

        @JvmStatic
        fun providerSettings(): Stream<Arguments> = Stream.of(
            Arguments.of(true, true, "Transfer all not allocated quota.", "100"),
            Arguments.of(false, true, "Transfer all not allocated quota.", "100"),
            Arguments.of(false, false, "Transfer all provided quota.", "150"),
        )

        @JvmStatic
        fun dryRunInput(): Stream<Arguments> = Stream.of(
            Arguments.of(
                150L, //delta
                500L, 0L, //source quota, balance,
                200L, 50L, 50L, //source provided, allocated, frozen
                450L, 50L, //destination quota, balance
                100L, 100L, 0L, //destination provided, allocated, frozen
                350L, 0L, 50L, //expected source quota, balance, provided
                600L, 50L, 250L //expected destination quota, balance, provided
            ),
            Arguments.of(
                100L, //delta
                500L, 0L, //source quota, balance
                200L, 50L, 50L, //source provided, allocated, frozen
                450L, 50L, //destination quota, balance,
                100L, 0L, 0L, //destination provided, allocated, frozen
                400L, 0L, 100L, //expected source quota, balance, provided
                550L, 50L, 200L //expected destination quota, balance, provided
            ),
            Arguments.of(
                75L, //delta
                50L, -150L, //source quota, balance
                200L, 50L, 50L, //source provided, allocated, frozen
                50L, -50L, //destination quota, balance
                100L, 0L, 0L, //destination provided, allocated, frozen
                0L, -125L, 125L, //expected source quota, balance, provided
                100L, -75L, 175L //expected destination quota, balance, provided
            )
        )
    }

    private fun dryRun(
        body: FrontDryRunCreateTransferRequestDto,
        mockUser: MockUser
    ) = dryRunRequest(body, mockUser)
            .expectStatus()
            .isOk
            .expectBody(FrontDryRunSingleTransferResultDto::class.java)
            .returnResult()
            .responseBody!!

    private fun dryRunRequest(
        body: FrontDryRunCreateTransferRequestDto,
        mockUser: MockUser
    ) = helper.webClient
            .mutateWith(mockUser)
            .post()
            .uri("/front/transfers/_dryRun")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()

    private fun setupRelatedResources(
        provider: ProviderModel,
        relatedResources: Map<String, RelatedResourceMapping>
    ): ProviderModel {
        val updatedProvider = ProviderModel.builder(provider)
            .relatedResourcesByResourceId(relatedResources)
            .build()
        helper.rwTx {
            helper.providersDao.upsertProviderRetryable(it, updatedProvider)
        }
        return updatedProvider
    }

    @ParameterizedTest
    @MethodSource("dryRunInput")
    fun dryRunProvisionTransfer(
        delta: Long,
        sourceQuota: Long, sourceBalance: Long,
        sourceProvided: Long, sourceAllocated: Long, sourceFrozen: Long,
        destQuota: Long, destBalance: Long,
        destProvided: Long, destAllocated: Long, destFrozen: Long,
        expectedSourceQuota: Long, expectedSourceBalance: Long, expectedSourceProvided: Long,
        expectedDestQuota: Long, expectedDestBalance: Long, expectedDestProvided: Long,
    ) {
        val data = helper.prepareData(
            sourceQuota = sourceQuota, sourceBalance = sourceBalance,
            sourceProvided = sourceProvided, sourceAllocated = sourceAllocated, sourceFrozen = sourceFrozen,
            destQuota = destQuota, destBalance = destBalance,
            destProvided = destProvided, destAllocated = destAllocated, destFrozen = destFrozen
        )

        val result = dryRun(provisionTransferDryRun(data.accountOne, data.folderOne, data.accountTwo, data.folderTwo,
            data.resource, delta),
            MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))

        val frozen = if (delta > 0) sourceFrozen else destFrozen
        val allocated = if (delta > 0) sourceAllocated else destAllocated
        val provided = if (delta > 0) sourceProvided else destProvided
        val available = provided - frozen - allocated
        if (delta > available) {
            assertEquals(1, result.warnings.perAccount.size)
            val accountId = if (delta > 0) data.accountOne.id else data.accountTwo.id
            val accountWarningsDto = result.warnings.perAccount[accountId]!!
            assertEquals(1, accountWarningsDto.messages.size)

            var accountMessage = "Not enough quota for provision transfer.\n$provided B is provided to the provider."
            if (allocated > 0) {
                accountMessage += "\n$allocated B is allocated in the provider."
            }
            if (frozen > 0) {
                accountMessage += "\n$frozen B is in the process of transfer in another transfer request."
            }
            if (available > 0) {
                accountMessage += "\nThe available amount is $available B."
            }
            assertTrue(result.warnings.general.isEmpty())
            assertEquals(accountMessage, accountWarningsDto.messages.first())
            assertEquals(1, accountWarningsDto.perResource.size)
            assertEquals(accountMessage, accountWarningsDto.perResource[data.resource.id]?.first())
            assertEquals(accountMessage, result.warnings.perResource[data.resource.id]?.first())
            val details = accountWarningsDto.detailsPerResource[data.resource.id]?.get("suggestedProvisionAmount") as Map<*, *>
            assertEquals(available.toString(), details["rawAmount"])
            assertEquals("B", details["rawUnit"])
        }

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
        val oldQuotasByAccountId = result.transfer.accountQuotasOld.associateBy { it.accountId }
        assertEquals(sourceProvided.toString(), oldQuotasByAccountId[data.accountOne.id]?.quotas?.first()?.providedQuota)
        assertEquals(sourceAllocated.toString(), oldQuotasByAccountId[data.accountOne.id]?.quotas?.first()?.allocatedQuota)
        assertEquals(sourceFrozen.toString(), oldQuotasByAccountId[data.accountOne.id]?.quotas?.first()?.frozenProvidedQuota)
        assertEquals(destProvided.toString(), oldQuotasByAccountId[data.accountTwo.id]?.quotas?.first()?.providedQuota)
        assertEquals(destAllocated.toString(), oldQuotasByAccountId[data.accountTwo.id]?.quotas?.first()?.allocatedQuota)
        assertEquals(destFrozen.toString(), oldQuotasByAccountId[data.accountTwo.id]?.quotas?.first()?.frozenProvidedQuota)

        val newQuotasByAccountId = result.transfer.accountQuotasNew.associateBy { it.accountId }
        assertEquals(expectedSourceProvided.toString(), newQuotasByAccountId[data.accountOne.id]?.quotas?.first()?.providedQuota)
        assertEquals(sourceAllocated.toString(), newQuotasByAccountId[data.accountOne.id]?.quotas?.first()?.allocatedQuota)
        assertEquals(sourceFrozen.toString(), newQuotasByAccountId[data.accountOne.id]?.quotas?.first()?.frozenProvidedQuota)
        assertEquals(expectedDestProvided.toString(), newQuotasByAccountId[data.accountTwo.id]?.quotas?.first()?.providedQuota)
        assertEquals(destAllocated.toString(), newQuotasByAccountId[data.accountTwo.id]?.quotas?.first()?.allocatedQuota)
        assertEquals(destFrozen.toString(), newQuotasByAccountId[data.accountTwo.id]?.quotas?.first()?.frozenProvidedQuota)

        val oldQuotasByFolderId = result.transfer.quotasOld.associateBy { it.folderId }
        assertEquals(sourceQuota.toString(), oldQuotasByFolderId[data.folderOne.id]?.quotas?.first()?.quota)
        assertEquals(sourceBalance.toString(), oldQuotasByFolderId[data.folderOne.id]?.quotas?.first()?.balance)
        assertEquals("0", oldQuotasByFolderId[data.folderOne.id]?.quotas?.first()?.frozenQuota)
        assertEquals(destQuota.toString(), oldQuotasByFolderId[data.folderTwo.id]?.quotas?.first()?.quota)
        assertEquals(destBalance.toString(), oldQuotasByFolderId[data.folderTwo.id]?.quotas?.first()?.balance)
        assertEquals("0", oldQuotasByFolderId[data.folderTwo.id]?.quotas?.first()?.frozenQuota)

        val newQuotasByFolderId = result.transfer.quotasNew.associateBy { it.folderId }
        assertEquals(expectedSourceQuota.toString(), newQuotasByFolderId[data.folderOne.id]?.quotas?.first()?.quota)
        assertEquals(expectedSourceBalance.toString(), newQuotasByFolderId[data.folderOne.id]?.quotas?.first()?.balance)
        assertEquals("0", newQuotasByFolderId[data.folderOne.id]?.quotas?.first()?.frozenQuota)
        assertEquals(expectedDestQuota.toString(), newQuotasByFolderId[data.folderTwo.id]?.quotas?.first()?.quota)
        assertEquals(expectedDestBalance.toString(), newQuotasByFolderId[data.folderTwo.id]?.quotas?.first()?.balance)
        assertEquals("0", newQuotasByFolderId[data.folderTwo.id]?.quotas?.first()?.frozenQuota)
    }

    @MethodSource("providerSettings")
    @ParameterizedTest
    fun dryRunProvisionTransferWithDeltaBiggerThanProvided(
        isAllocatedSupported: Boolean,
        isAllocatedSupportedResource: Boolean,
        expectedSuggestedPrompt: String,
        expectedAvailable: String
    ) {
        val data = helper.prepareData(allocatedSupported = isAllocatedSupported,
            allocatedSupportedResource = isAllocatedSupportedResource)
        val result = dryRunRequest(provisionTransferDryRun(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, 300), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .expectStatus()
            .isOk
            .expectBody(FrontDryRunSingleTransferResultDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Not enough quota for provision transfer.\n" +
            "200 B is provided to the provider.\n" +
            (if (isAllocatedSupported || isAllocatedSupportedResource) "50 B is allocated in the provider.\n" else "") +
            "50 B is in the process of transfer in another transfer request.\n" +
            "The available amount is $expectedAvailable B."),
            result.errors.fieldErrors["parameters.provisionTransfers.0.sourceAccountTransfers.0.delta"])
        assertEquals(setOf(expectedSuggestedPrompt), result.errors.details["suggestedProvisionAmountPrompt"])
        val unitId = data.resource.baseUnitId
        val amount = AmountDto(expectedAvailable, "B", expectedAvailable, "B", expectedAvailable,
            unitId, expectedAvailable, unitId)
        val amountMap = JsonUtil.convertToMap(amount)
        assertEquals(setOf(amountMap), result.errors.details["${data.resource.id}.suggestedProvisionAmount"])
    }

    @Test
    fun dryRunProvisionTransferWithNegativeDelta() {
        val data = helper.prepareData()
        val result = dryRunRequest(provisionTransferDryRun(data.accountOne, data.folderOne, data.accountTwo,
            data.folderTwo, data.resource, -300), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(setOf("Number must be positive."),
            result.fieldErrors["parameters.provisionTransfers.0.destinationAccountTransfers.0.delta"])
    }

    @Test
    fun dryRunProvisionTransferWithMultipleProvisionTransfersAndRelatedResources() {
        val data = helper.prepareData()
        val (accountThree, _) = helper.createAccountWithQuota(data.resource, data.folderTwo, 100, 0, 0,
            data.accountSpaceModel)
        val (anotherResource, _) = helper.createResource(data.provider, "anotherResource", data.accountSpaceModel)
        val (relatedResource, _) = helper.createResource(data.provider, "relatedResource", data.accountSpaceModel,
            unitsEnsembleId = UnitsEnsembleIds.CPU_UNITS_ID)
        val (relatedResource2, _) = helper.createResource(data.provider, "relatedResource2", data.accountSpaceModel,
            unitsEnsembleId = UnitsEnsembleIds.CPU_UNITS_ID)
        helper.createAccountsQuotas(listOf(data.accountOne, data.accountTwo, accountThree),
            anotherResource, 100, 0, 0)
        helper.createAccountsQuotas(listOf(data.accountOne, data.accountTwo, accountThree),
            relatedResource, 150, 0, 0)
        helper.createAccountsQuotas(listOf(data.accountOne, data.accountTwo, accountThree),
            relatedResource2, 1000, 500, 0)
        helper.createFolderQuotas(listOf(data.folderOne, data.folderTwo), anotherResource, 700, 600)
        setupRelatedResources(data.provider, mapOf(data.resource.id to RelatedResourceMapping(
            mapOf(relatedResource.id to RelatedCoefficient(989L, 1000L))
        ), anotherResource.id to RelatedResourceMapping(
            mapOf(relatedResource2.id to RelatedCoefficient(10L, 1L))
        )))
        val result = dryRun(provisionTransferDryRun(listOf(
            provisionTransfer(data.accountOne, data.folderOne, data.accountTwo, data.folderTwo, data.resource, 150),
            provisionTransfer(data.accountOne, data.folderOne, accountThree, data.folderTwo, anotherResource, 100),
        )), MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))

        assertTrue(relatedResource.baseUnitId in result.units)
        assertTrue(relatedResource2.baseUnitId in result.units)
        assertTrue(anotherResource.baseUnitId in result.units)
        assertTrue(data.resource.baseUnitId in result.units)
        val dryRunParameters = result.transfer.dryRunParameters
        assertNotNull(dryRunParameters)
        assertTrue(dryRunParameters.relatedProvisionTransfers.isNotEmpty())
        val relatedTransfersByAccounts = dryRunParameters.relatedProvisionTransfers
            .associateBy { it.sourceAccountId to it.destinationAccountId }
        val relatedTransferOne = relatedTransfersByAccounts[data.accountOne.id to data.accountTwo.id]!!
        assertEquals(1, relatedTransferOne.sourceAccountTransfers.size)
        assertEquals(1, relatedTransferOne.destinationAccountTransfers.size)
        relatedTransferOne.sourceAccountTransfers.forEach {
            assertEquals("-149", it.delta)
            assertEquals(relatedResource.id, it.resourceId)
            assertEquals(UnitIds.MILLICORES, it.deltaUnitId)
        }
        relatedTransferOne.destinationAccountTransfers.forEach {
            assertEquals("149", it.delta)
            assertEquals(relatedResource.id, it.resourceId)
            assertEquals(UnitIds.MILLICORES, it.deltaUnitId)
        }
        val relatedTransferTwo = relatedTransfersByAccounts[data.accountOne.id to accountThree.id]!!
        assertEquals(1, relatedTransferTwo.sourceAccountTransfers.size)
        assertEquals(1, relatedTransferTwo.destinationAccountTransfers.size)
        relatedTransferTwo.sourceAccountTransfers.forEach {
            assertEquals("-500", it.delta)
            assertEquals(relatedResource2.id, it.resourceId)
            assertEquals(UnitIds.MILLICORES, it.deltaUnitId)
        }
        relatedTransferTwo.destinationAccountTransfers.forEach {
            assertEquals("500", it.delta)
            assertEquals(relatedResource2.id, it.resourceId)
            assertEquals(UnitIds.MILLICORES, it.deltaUnitId)
        }

        assertEquals(1, result.warnings.perAccount.size)
        val accountWarningsDto = result.warnings.perAccount[data.accountOne.id]!!
        assertEquals(1, accountWarningsDto.messages.size)
        val accountMessage = "Not enough quota for provision transfer.\n" +
            "200 B is provided to the provider.\n" +
            "50 B is allocated in the provider.\n" +
            "50 B is in the process of transfer in another transfer request.\n" +
            "The available amount is 100 B."
        assertTrue(result.warnings.general.isEmpty())
        assertEquals(accountMessage, accountWarningsDto.messages.first())
        assertEquals(1, accountWarningsDto.perResource.size)
        assertEquals(accountMessage, accountWarningsDto.perResource[data.resource.id]?.first())
        assertEquals(accountMessage, result.warnings.perResource[data.resource.id]?.first())
        val details = accountWarningsDto.detailsPerResource[data.resource.id]?.get("suggestedProvisionAmount")
            as Map<*, *>
        assertEquals("100", details["rawAmount"])
        assertEquals("B", details["rawUnit"])

        assertEquals(TransferRequestTypeDto.PROVISION_TRANSFER, result.transfer.requestType)
        val oldQuotasByAccountId = result.transfer.accountQuotasOld
            .associateBy { it.accountId }
            .mapValues { (_, vs) -> vs.quotas.associateBy { it.resourceId } }
        assertEquals("200", oldQuotasByAccountId[data.accountOne.id]?.get(data.resource.id)?.providedQuota)
        assertEquals("50", oldQuotasByAccountId[data.accountOne.id]?.get(data.resource.id)?.allocatedQuota)
        assertEquals("50", oldQuotasByAccountId[data.accountOne.id]?.get(data.resource.id)?.frozenProvidedQuota)
        assertEquals("100", oldQuotasByAccountId[data.accountOne.id]?.get(anotherResource.id)?.providedQuota)
        assertEquals("0", oldQuotasByAccountId[data.accountOne.id]?.get(anotherResource.id)?.allocatedQuota)
        assertEquals("0", oldQuotasByAccountId[data.accountOne.id]?.get(anotherResource.id)?.frozenProvidedQuota)
        assertEquals("100", oldQuotasByAccountId[data.accountTwo.id]?.get(data.resource.id)?.providedQuota)
        assertEquals("100", oldQuotasByAccountId[data.accountTwo.id]?.get(data.resource.id)?.allocatedQuota)
        assertEquals("0", oldQuotasByAccountId[data.accountTwo.id]?.get(data.resource.id)?.frozenProvidedQuota)
        assertEquals("100", oldQuotasByAccountId[accountThree.id]?.get(anotherResource.id)?.providedQuota)
        assertEquals("0", oldQuotasByAccountId[accountThree.id]?.get(anotherResource.id)?.allocatedQuota)
        assertEquals("0", oldQuotasByAccountId[accountThree.id]?.get(anotherResource.id)?.frozenProvidedQuota)

        val newQuotasByAccountId = result.transfer.accountQuotasNew
            .associateBy { it.accountId }
            .mapValues { (_, vs) -> vs.quotas.associateBy { it.resourceId } }

        assertEquals("50", newQuotasByAccountId[data.accountOne.id]?.get(data.resource.id)?.providedQuota)
        assertEquals("50", newQuotasByAccountId[data.accountOne.id]?.get(data.resource.id)?.allocatedQuota)
        assertEquals("50", newQuotasByAccountId[data.accountOne.id]?.get(data.resource.id)?.frozenProvidedQuota)
        assertEquals("0", newQuotasByAccountId[data.accountOne.id]?.get(anotherResource.id)?.providedQuota)
        assertEquals("0", newQuotasByAccountId[data.accountOne.id]?.get(anotherResource.id)?.allocatedQuota)
        assertEquals("0", newQuotasByAccountId[data.accountOne.id]?.get(anotherResource.id)?.frozenProvidedQuota)
        assertEquals("250", newQuotasByAccountId[data.accountTwo.id]?.get(data.resource.id)?.providedQuota)
        assertEquals("100", newQuotasByAccountId[data.accountTwo.id]?.get(data.resource.id)?.allocatedQuota)
        assertEquals("0", newQuotasByAccountId[data.accountTwo.id]?.get(data.resource.id)?.frozenProvidedQuota)
        assertEquals("200", newQuotasByAccountId[accountThree.id]?.get(anotherResource.id)?.providedQuota)
        assertEquals("0", newQuotasByAccountId[accountThree.id]?.get(anotherResource.id)?.allocatedQuota)
        assertEquals("0", newQuotasByAccountId[accountThree.id]?.get(anotherResource.id)?.frozenProvidedQuota)

        val oldQuotasByFolderId = result.transfer.quotasOld
            .associateBy { it.folderId }
            .mapValues { (_, vs) -> vs.quotas.associateBy { it.resourceId } }
        assertEquals("500", oldQuotasByFolderId[data.folderOne.id]?.get(data.resource.id)?.quota)
        assertEquals("0", oldQuotasByFolderId[data.folderOne.id]?.get(data.resource.id)?.balance)
        assertEquals("0", oldQuotasByFolderId[data.folderOne.id]?.get(data.resource.id)?.frozenQuota)
        assertEquals("450", oldQuotasByFolderId[data.folderTwo.id]?.get(data.resource.id)?.quota)
        assertEquals("50", oldQuotasByFolderId[data.folderTwo.id]?.get(data.resource.id)?.balance)
        assertEquals("0", oldQuotasByFolderId[data.folderTwo.id]?.get(data.resource.id)?.frozenQuota)
        assertEquals("700", oldQuotasByFolderId[data.folderOne.id]?.get(anotherResource.id)?.quota)
        assertEquals("600", oldQuotasByFolderId[data.folderOne.id]?.get(anotherResource.id)?.balance)
        assertEquals("0", oldQuotasByFolderId[data.folderOne.id]?.get(anotherResource.id)?.frozenQuota)
        assertEquals("700", oldQuotasByFolderId[data.folderTwo.id]?.get(anotherResource.id)?.quota)
        assertEquals("600", oldQuotasByFolderId[data.folderTwo.id]?.get(anotherResource.id)?.balance)
        assertEquals("0", oldQuotasByFolderId[data.folderTwo.id]?.get(anotherResource.id)?.frozenQuota)

        val newQuotasByFolderId = result.transfer.quotasNew
            .associateBy { it.folderId }
            .mapValues { (_, vs) -> vs.quotas.associateBy { it.resourceId } }
        assertEquals("350", newQuotasByFolderId[data.folderOne.id]?.get(data.resource.id)?.quota)
        assertEquals("0", newQuotasByFolderId[data.folderOne.id]?.get(data.resource.id)?.balance)
        assertEquals("0", newQuotasByFolderId[data.folderOne.id]?.get(data.resource.id)?.frozenQuota)
        assertEquals("600", newQuotasByFolderId[data.folderTwo.id]?.get(data.resource.id)?.quota)
        assertEquals("50", newQuotasByFolderId[data.folderTwo.id]?.get(data.resource.id)?.balance)
        assertEquals("0", newQuotasByFolderId[data.folderTwo.id]?.get(data.resource.id)?.frozenQuota)
        assertEquals("600", newQuotasByFolderId[data.folderOne.id]?.get(anotherResource.id)?.quota)
        assertEquals("600", newQuotasByFolderId[data.folderOne.id]?.get(anotherResource.id)?.balance)
        assertEquals("0", newQuotasByFolderId[data.folderOne.id]?.get(anotherResource.id)?.frozenQuota)
        assertEquals("800", newQuotasByFolderId[data.folderTwo.id]?.get(anotherResource.id)?.quota)
        assertEquals("600", newQuotasByFolderId[data.folderTwo.id]?.get(anotherResource.id)?.balance)
        assertEquals("0", newQuotasByFolderId[data.folderTwo.id]?.get(anotherResource.id)?.frozenQuota)
    }

    @Test
    fun dryRunProvisionTransferErrorsWithWarningsTest() {
        val data = helper.prepareData(sourceProvided = 100, sourceAllocated = 90, sourceFrozen = 0)
        val (accountThree, _) = helper.createAccountWithQuota(
            data.resource, data.folderOne, provided = 100, allocated = 90, frozen = 0, data.accountSpaceModel
        )
        val (accountFour, _) = helper.createAccountWithQuota(
            data.resource, data.folderTwo, provided = 100, allocated = 90, frozen = 0, data.accountSpaceModel
        )
        val deltaOne = 20L
        val deltaTwo = 110L
        val request = FrontDryRunCreateTransferRequestDto(
            TransferRequestTypeDto.PROVISION_TRANSFER,
            FrontCreateTransferRequestParametersDto.builder()
                .addProvisionTransfer(
                    FrontCreateProvisionTransferDto(
                        sourceAccountId = data.accountOne.id,
                        sourceFolderId = data.folderOne.id,
                        sourceServiceId = data.folderOne.serviceId.toString(),
                        destinationAccountId = data.accountTwo.id,
                        destinationFolderId = data.folderTwo.id,
                        destinationServiceId = data.folderTwo.serviceId.toString(),
                        sourceAccountTransfers = listOf(
                            FrontCreateQuotaResourceTransferDto(
                                data.resource.id, (-deltaOne).toString(), data.resource.baseUnitId
                            )
                        ),
                        destinationAccountTransfers = listOf(
                            FrontCreateQuotaResourceTransferDto(
                                data.resource.id, deltaOne.toString(), data.resource.baseUnitId
                            )
                        )
                    )
                )
                .addProvisionTransfer(
                    FrontCreateProvisionTransferDto(
                        sourceAccountId = accountThree.id,
                        sourceFolderId = data.folderOne.id,
                        sourceServiceId = data.folderOne.serviceId.toString(),
                        destinationAccountId = accountFour.id,
                        destinationFolderId = data.folderTwo.id,
                        destinationServiceId = data.folderTwo.serviceId.toString(),
                        sourceAccountTransfers = listOf(
                            FrontCreateQuotaResourceTransferDto(
                                data.resource.id, (-deltaTwo).toString(), data.resource.baseUnitId
                            )
                        ),
                        destinationAccountTransfers = listOf(
                            FrontCreateQuotaResourceTransferDto(
                                data.resource.id, deltaTwo.toString(), data.resource.baseUnitId
                            )
                        )
                    )
                )
                .build(),
            true,
            true,
            null
        )
        val result = helper.webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers/_dryRun")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontDryRunSingleTransferResultDto::class.java)
            .returnResult()
            .responseBody!!

        assertNotNull(result.warnings)
        assertTrue(result.warnings!!.perAccount.isNotEmpty())
        assertTrue(result.warnings!!.perAccount.containsKey(data.accountOne.id))
        assertTrue(
            result.warnings!!.perAccount[data.accountOne.id]!!.messages.contains(
                "Not enough quota for provision transfer.\n" +
                    "100 B is provided to the provider.\n" +
                    "90 B is allocated in the provider.\n" +
                    "The available amount is 10 B."
            )
        )
        assertFalse(result.warnings!!.perAccount.containsKey(accountThree.id))
        assertNotNull(result.errors.fieldErrors)
        assertTrue(result.errors.fieldErrors!!.containsKey("parameters.provisionTransfers.1.sourceAccountTransfers.0.delta"))
        assertTrue(
            result.errors.fieldErrors!!["parameters.provisionTransfers.1.sourceAccountTransfers.0.delta"]!!.contains(
                "Not enough quota for provision transfer.\n" +
                    "100 B is provided to the provider.\n" +
                    "90 B is allocated in the provider.\n" +
                    "The available amount is 10 B."
            )
        )
    }
}
