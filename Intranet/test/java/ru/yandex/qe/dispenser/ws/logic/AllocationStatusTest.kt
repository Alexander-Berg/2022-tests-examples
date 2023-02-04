package ru.yandex.qe.dispenser.ws.logic

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.qe.dispenser.api.v1.DiAmount
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType
import ru.yandex.qe.dispenser.api.v1.DiResourceType
import ru.yandex.qe.dispenser.api.v1.DiSetAmountResult
import ru.yandex.qe.dispenser.api.v1.DiUnit
import ru.yandex.qe.dispenser.api.v1.request.quota.Body
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate.BodyUpdateBuilder
import ru.yandex.qe.dispenser.api.v1.request.quota.ChangeBody
import ru.yandex.qe.dispenser.domain.BotCampaignGroup
import ru.yandex.qe.dispenser.domain.Campaign
import ru.yandex.qe.dispenser.domain.Project
import ru.yandex.qe.dispenser.domain.Resource
import ru.yandex.qe.dispenser.domain.ResourceSegmentation
import ru.yandex.qe.dispenser.domain.Segment
import ru.yandex.qe.dispenser.domain.Segmentation
import ru.yandex.qe.dispenser.domain.Service
import ru.yandex.qe.dispenser.domain.bot.BigOrder
import ru.yandex.qe.dispenser.domain.d.DeliverableFolderOperationDto
import ru.yandex.qe.dispenser.domain.d.DeliverableMetaRequestDto
import ru.yandex.qe.dispenser.domain.d.DeliveryOperationStatusDto
import ru.yandex.qe.dispenser.domain.d.DeliveryStatusDto
import ru.yandex.qe.dispenser.domain.d.DeliveryStatusOperationDto
import ru.yandex.qe.dispenser.domain.d.DeliveryStatusResponseDto
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao
import ru.yandex.qe.dispenser.domain.dao.delivery.DeliveryDao
import ru.yandex.qe.dispenser.domain.dao.goal.GoalDao
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao
import ru.yandex.qe.dispenser.domain.dao.project.ProjectDao
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao
import ru.yandex.qe.dispenser.domain.resources_model.DeliveryResult
import ru.yandex.qe.dispenser.domain.resources_model.ExternalResource
import ru.yandex.qe.dispenser.domain.resources_model.InternalResource
import ru.yandex.qe.dispenser.domain.resources_model.QuotaRequestDelivery
import ru.yandex.qe.dispenser.domain.resources_model.QuotaRequestDeliveryResolveStatus
import ru.yandex.qe.dispenser.standalone.MockDApi
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager
import ru.yandex.qe.dispenser.ws.quota.request.SetResourceAmountBody
import ru.yandex.qe.dispenser.ws.reqbody.DeliveryStatusRequest
import ru.yandex.qe.dispenser.ws.reqbody.DeliveryStatusResponse
import ru.yandex.qe.dispenser.ws.reqbody.QuotaRequestDeliveryOperationStatus
import ru.yandex.qe.dispenser.ws.reqbody.QuotaRequestPendingDeliveryResponse
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.util.*
import javax.ws.rs.HttpMethod

class AllocationStatusTest(
    @Autowired private val bigOrderManager: BigOrderManager,
    @Autowired private val goalDaoImpl: GoalDao,
    @Autowired private val botCampaignGroupDaoImpl: BotCampaignGroupDao,
    @Autowired private val serviceDaoImpl: ServiceDao,
    @Autowired private val campaignDaoImpl: CampaignDao,
    @Autowired private val segmentationDaoImpl: SegmentationDao,
    @Autowired private val segmentDaoImpl: SegmentDao,
    @Autowired private val resourceDaoImpl: ResourceDao,
    @Autowired private val resourceSegmentationDaoImpl: ResourceSegmentationDao,
    @Autowired private val projectDaoImpl: ProjectDao,
    @Autowired private val deliveryDaoImpl: DeliveryDao,
    @Autowired private val personDaoImpl: PersonDao,
    @Autowired private val mockDApi: MockDApi
): AcceptanceTestBase() {

    @BeforeEach
    fun beforeEachTest() {
        bigOrderManager.clear()
        goalDaoImpl.clear()
        deliveryDaoImpl.clear()
        mockDApi.clearDeliveryIds()
    }

    @Test
    fun testGetStatusSuccess() {
        val bigOrders = prepareBigOrders()
        val campaign = prepareCampaign(bigOrders)
        val campaignGroup = prepareCampaignGroup(campaign, bigOrders)
        val providerOne = prepareProvider("logfeller", 1)
        val providerTwo = prepareProvider("distbuild", 2)
        val providerThree = prepareProvider("strm", 3)
        val providerTwoSegmentationOne = prepareSegmentation("segmentationOne")
        val providerTwoSegmentationTwo = prepareSegmentation("segmentationTwo")
        val providerTwoSegmentationOneSegmentOne = prepareSegment(providerTwoSegmentationOne, "segmentOne")
        val providerTwoSegmentationOneSegmentTwo = prepareSegment(providerTwoSegmentationOne, "segmentTwo")
        val providerTwoSegmentationTwoSegmentOne = prepareSegment(providerTwoSegmentationTwo, "segmentThree")
        val providerTwoSegmentationTwoSegmentTwo = prepareSegment(providerTwoSegmentationTwo, "segmentFour")
        val providerOneResourceOne = prepareResource(providerOne, "resourceOne", DiResourceType.PROCESSOR)
        val providerOneResourceTwo = prepareResource(providerOne, "resourceTwo", DiResourceType.PROCESSOR)
        val providerTwoResourceOne = prepareResource(providerTwo, "resourceThree", DiResourceType.PROCESSOR,
            providerTwoSegmentationOne, providerTwoSegmentationTwo)
        val providerTwoResourceTwo = prepareResource(providerTwo, "resourceFour", DiResourceType.PROCESSOR,
            providerTwoSegmentationOne, providerTwoSegmentationTwo)
        val providerThreeResourceOne = prepareResource(providerThree, "resourceFive", DiResourceType.PROCESSOR)
        val providerThreeResourceTwo = prepareResource(providerThree, "resourceSix", DiResourceType.PROCESSOR)
        val projectOne = prepareProject("projectOne", 4)
        updateHierarchy()
        prepareCampaignResources()
        val requestOne = prepareRequestOne(bigOrders, providerOne, providerTwo, providerThree,
            providerTwoSegmentationOneSegmentOne, providerTwoSegmentationOneSegmentTwo,
            providerTwoSegmentationTwoSegmentOne, providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne,
            providerOneResourceTwo, providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne,
            providerThreeResourceTwo, projectOne, campaign)
        val requestTwo = prepareRequestTwo(bigOrders, providerOne, providerTwo, providerThree,
            providerTwoSegmentationOneSegmentOne, providerTwoSegmentationOneSegmentTwo,
            providerTwoSegmentationTwoSegmentOne, providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne,
            providerOneResourceTwo, providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne,
            providerThreeResourceTwo, projectOne, campaign)
        val confirmedRequestOne = confirmRequest(requestOne)
        val confirmedRequestTwo = confirmRequest(requestTwo)
        setReadyRequestOne(bigOrders, providerOne, providerTwo, providerThree, providerTwoSegmentationOneSegmentOne,
            providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentOne,
            providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne, providerOneResourceTwo,
            providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne, providerThreeResourceTwo,
            confirmedRequestOne)
        setReadyRequestTwo(bigOrders, providerOne, providerTwo, providerThree, providerTwoSegmentationOneSegmentOne,
            providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentOne,
            providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne, providerOneResourceTwo,
            providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne, providerThreeResourceTwo,
            confirmedRequestTwo)
        val folderId = UUID.randomUUID()
        val operationIdOne = UUID.randomUUID()
        val operationIdTwo = UUID.randomUUID()
        val operationIdThree = UUID.randomUUID()
        val operationIdFour = UUID.randomUUID()
        val operationIdFive = UUID.randomUUID()
        val operationIdSix = UUID.randomUUID()
        val externalResourceOne = UUID.randomUUID()
        val externalResourceTwo = UUID.randomUUID()
        val externalResourceThree = UUID.randomUUID()
        val externalResourceFour = UUID.randomUUID()
        val externalResourceFive = UUID.randomUUID()
        val externalResourceSix = UUID.randomUUID()
        val externalResourceSeven = UUID.randomUUID()
        val externalResourceEight = UUID.randomUUID()
        val deliveryOne = prepareDelivery(requestOne, providerOne, true,
            listOf(ExternalResource(externalResourceOne, bigOrders[0].id, 21, "core", null, null, null),
                ExternalResource(externalResourceOne, bigOrders[1].id, 34, "core", null, null, null),
                ExternalResource(externalResourceTwo, bigOrders[0].id, 22, "core", null, null, null),
                ExternalResource(externalResourceTwo, bigOrders[1].id, 35, "core", null, null, null)),
            listOf(InternalResource(providerOneResourceOne.id, setOf(), bigOrders[0].id, 21000),
                InternalResource(providerOneResourceOne.id, setOf(), bigOrders[1].id, 34000),
                InternalResource(providerOneResourceTwo.id, setOf(), bigOrders[0].id, 22000),
                InternalResource(providerOneResourceTwo.id, setOf(), bigOrders[1].id, 35000)),
            listOf(DeliveryResult(folderId, bigOrders[0].id, externalResourceOne, operationIdOne, Instant.now(), null,
                null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceOne, operationIdOne, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[0].id, externalResourceTwo, operationIdOne, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceTwo, operationIdOne, Instant.now(), null,
                    null)))
        val deliveryTwo = prepareDelivery(requestOne, providerTwo, true,
            listOf(ExternalResource(externalResourceThree, bigOrders[0].id, 23, "core", null, null, null),
                ExternalResource(externalResourceThree, bigOrders[1].id, 36, "core", null, null, null),
                ExternalResource(externalResourceFour, bigOrders[0].id, 24, "core", null, null, null),
                ExternalResource(externalResourceFour, bigOrders[1].id, 37, "core", null, null, null),
                ExternalResource(externalResourceFive, bigOrders[0].id, 25, "core", null, null, null),
                ExternalResource(externalResourceFive, bigOrders[1].id, 38, "core", null, null, null),
                ExternalResource(externalResourceSix, bigOrders[0].id, 26, "core", null, null, null),
                ExternalResource(externalResourceSix, bigOrders[1].id, 39, "core", null, null, null)),
            listOf(InternalResource(providerTwoResourceOne.id, setOf(providerTwoSegmentationOneSegmentOne.id,
                providerTwoSegmentationTwoSegmentOne.id), bigOrders[0].id, 23000),
                InternalResource(providerTwoResourceOne.id, setOf(providerTwoSegmentationOneSegmentOne.id,
                    providerTwoSegmentationTwoSegmentOne.id), bigOrders[1].id, 36000),
                InternalResource(providerTwoResourceOne.id, setOf(providerTwoSegmentationOneSegmentTwo.id,
                    providerTwoSegmentationTwoSegmentTwo.id), bigOrders[0].id, 24000),
                InternalResource(providerTwoResourceOne.id, setOf(providerTwoSegmentationOneSegmentTwo.id,
                    providerTwoSegmentationTwoSegmentTwo.id), bigOrders[1].id, 37000),
                InternalResource(providerTwoResourceTwo.id, setOf(providerTwoSegmentationOneSegmentOne.id,
                    providerTwoSegmentationTwoSegmentOne.id), bigOrders[0].id, 25000),
                InternalResource(providerTwoResourceTwo.id, setOf(providerTwoSegmentationOneSegmentOne.id,
                    providerTwoSegmentationTwoSegmentOne.id), bigOrders[1].id, 38000),
                InternalResource(providerTwoResourceTwo.id, setOf(providerTwoSegmentationOneSegmentTwo.id,
                    providerTwoSegmentationTwoSegmentTwo.id), bigOrders[0].id, 26000),
                InternalResource(providerTwoResourceTwo.id, setOf(providerTwoSegmentationOneSegmentTwo.id,
                    providerTwoSegmentationTwoSegmentTwo.id), bigOrders[1].id, 39000)),
            listOf(DeliveryResult(folderId, bigOrders[0].id, externalResourceThree, operationIdTwo, Instant.now(), null,
                null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceThree, operationIdTwo, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[0].id, externalResourceFour, operationIdTwo, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceFour, operationIdTwo, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[0].id, externalResourceFive, operationIdTwo, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceFive, operationIdTwo, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[0].id, externalResourceSix, operationIdTwo, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceSix, operationIdTwo, Instant.now(), null,
                    null)))
        val deliveryThree = prepareDelivery(requestOne, providerThree, true,
            listOf(ExternalResource(externalResourceSeven, bigOrders[0].id, 27, "core", null, null, null),
                ExternalResource(externalResourceSeven, bigOrders[1].id, 40, "core", null, null, null),
                ExternalResource(externalResourceEight, bigOrders[0].id, 28, "core", null, null, null),
                ExternalResource(externalResourceEight, bigOrders[1].id, 41, "core", null, null, null)),
            listOf(InternalResource(providerThreeResourceOne.id, setOf(), bigOrders[0].id, 27000),
                InternalResource(providerThreeResourceOne.id, setOf(), bigOrders[1].id, 40000),
                InternalResource(providerThreeResourceTwo.id, setOf(), bigOrders[0].id, 28000),
                InternalResource(providerThreeResourceTwo.id, setOf(), bigOrders[1].id, 41000)),
            listOf(DeliveryResult(folderId, bigOrders[0].id, externalResourceSeven, operationIdThree, Instant.now(),
                null, null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceSeven, operationIdThree, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[0].id, externalResourceEight, operationIdThree, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceEight, operationIdThree, Instant.now(), null,
                    null)))
        val deliveryFour = prepareDelivery(requestTwo, providerOne, false,
            listOf(ExternalResource(externalResourceOne, bigOrders[0].id, 210, "core", null, null, null),
                ExternalResource(externalResourceOne, bigOrders[1].id, 340, "core", null, null, null),
                ExternalResource(externalResourceTwo, bigOrders[0].id, 220, "core", null, null, null),
                ExternalResource(externalResourceTwo, bigOrders[1].id, 350, "core", null, null, null)),
            listOf(InternalResource(providerOneResourceOne.id, setOf(), bigOrders[0].id, 210000),
                InternalResource(providerOneResourceOne.id, setOf(), bigOrders[1].id, 340000),
                InternalResource(providerOneResourceTwo.id, setOf(), bigOrders[0].id, 220000),
                InternalResource(providerOneResourceTwo.id, setOf(), bigOrders[1].id, 350000)),
            listOf(DeliveryResult(folderId, bigOrders[0].id, externalResourceOne, operationIdFour, Instant.now(), null,
                null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceOne, operationIdFour, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[0].id, externalResourceTwo, operationIdFour, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceTwo, operationIdFour, Instant.now(), null,
                    null)))
        val deliveryFive = prepareDelivery(requestTwo, providerTwo, false,
            listOf(ExternalResource(externalResourceThree, bigOrders[0].id, 230, "core", null, null, null),
                ExternalResource(externalResourceThree, bigOrders[1].id, 360, "core", null, null, null),
                ExternalResource(externalResourceFour, bigOrders[0].id, 240, "core", null, null, null),
                ExternalResource(externalResourceFour, bigOrders[1].id, 370, "core", null, null, null),
                ExternalResource(externalResourceFive, bigOrders[0].id, 250, "core", null, null, null),
                ExternalResource(externalResourceFive, bigOrders[1].id, 380, "core", null, null, null),
                ExternalResource(externalResourceSix, bigOrders[0].id, 260, "core", null, null, null),
                ExternalResource(externalResourceSix, bigOrders[1].id, 390, "core", null, null, null)),
            listOf(InternalResource(providerTwoResourceOne.id, setOf(providerTwoSegmentationOneSegmentOne.id,
                providerTwoSegmentationTwoSegmentOne.id), bigOrders[0].id, 230000),
                InternalResource(providerTwoResourceOne.id, setOf(providerTwoSegmentationOneSegmentOne.id,
                    providerTwoSegmentationTwoSegmentOne.id), bigOrders[1].id, 360000),
                InternalResource(providerTwoResourceOne.id, setOf(providerTwoSegmentationOneSegmentTwo.id,
                    providerTwoSegmentationTwoSegmentTwo.id), bigOrders[0].id, 240000),
                InternalResource(providerTwoResourceOne.id, setOf(providerTwoSegmentationOneSegmentTwo.id,
                    providerTwoSegmentationTwoSegmentTwo.id), bigOrders[1].id, 370000),
                InternalResource(providerTwoResourceTwo.id, setOf(providerTwoSegmentationOneSegmentOne.id,
                    providerTwoSegmentationTwoSegmentOne.id), bigOrders[0].id, 250000),
                InternalResource(providerTwoResourceTwo.id, setOf(providerTwoSegmentationOneSegmentOne.id,
                    providerTwoSegmentationTwoSegmentOne.id), bigOrders[1].id, 380000),
                InternalResource(providerTwoResourceTwo.id, setOf(providerTwoSegmentationOneSegmentTwo.id,
                    providerTwoSegmentationTwoSegmentTwo.id), bigOrders[0].id, 260000),
                InternalResource(providerTwoResourceTwo.id, setOf(providerTwoSegmentationOneSegmentTwo.id,
                    providerTwoSegmentationTwoSegmentTwo.id), bigOrders[1].id, 390000)),
            listOf(DeliveryResult(folderId, bigOrders[0].id, externalResourceThree, operationIdFive, Instant.now(),
                null, null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceThree, operationIdFive, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[0].id, externalResourceFour, operationIdFive, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceFour, operationIdFive, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[0].id, externalResourceFive, operationIdFive, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceFive, operationIdFive, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[0].id, externalResourceSix, operationIdFive, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceSix, operationIdFive, Instant.now(), null,
                    null)))
        val deliverySix = prepareDelivery(requestTwo, providerThree, false,
            listOf(ExternalResource(externalResourceSeven, bigOrders[0].id, 270, "core", null, null, null),
                ExternalResource(externalResourceSeven, bigOrders[1].id, 400, "core", null, null, null),
                ExternalResource(externalResourceEight, bigOrders[0].id, 280, "core", null, null, null),
                ExternalResource(externalResourceEight, bigOrders[1].id, 410, "core", null, null, null)),
            listOf(InternalResource(providerThreeResourceOne.id, setOf(), bigOrders[0].id, 270000),
                InternalResource(providerThreeResourceOne.id, setOf(), bigOrders[1].id, 400000),
                InternalResource(providerThreeResourceTwo.id, setOf(), bigOrders[0].id, 280000),
                InternalResource(providerThreeResourceTwo.id, setOf(), bigOrders[1].id, 410000)),
            listOf(DeliveryResult(folderId, bigOrders[0].id, externalResourceSeven, operationIdSix, Instant.now(), null,
                null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceSeven, operationIdSix, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[0].id, externalResourceEight, operationIdSix, Instant.now(), null,
                    null),
                DeliveryResult(folderId, bigOrders[1].id, externalResourceEight, operationIdSix, Instant.now(), null,
                    null)))
        mockDApi.setStatus(DeliveryStatusResponseDto(listOf(
            DeliveryStatusDto(deliveryOne.id.toString(), listOf(
                DeliveryStatusOperationDto(operationIdOne.toString(), UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(), listOf(), Instant.now(), null, DeliveryOperationStatusDto.SUCCESS,
                    null, null, DeliverableMetaRequestDto(1L, 2L, 3L), DeliverableFolderOperationDto("", "",
                        java.time.Instant.now()))),
                listOf(), listOf(), listOf()),
            DeliveryStatusDto(deliveryTwo.id.toString(), listOf(
                DeliveryStatusOperationDto(operationIdTwo.toString(), UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(), listOf(), Instant.now(), null, DeliveryOperationStatusDto.SUCCESS,
                    null, null, DeliverableMetaRequestDto(1L, 2L, 3L), DeliverableFolderOperationDto("", "",
                        java.time.Instant.now()))),
                listOf(), listOf(), listOf()),
            DeliveryStatusDto(deliveryThree.id.toString(), listOf(
                DeliveryStatusOperationDto(operationIdThree.toString(), UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(), listOf(), Instant.now(), null, DeliveryOperationStatusDto.SUCCESS,
                    null, null, DeliverableMetaRequestDto(1L, 2L, 3L), DeliverableFolderOperationDto("", "",
                        java.time.Instant.now()))),
                listOf(), listOf(), listOf()),
            DeliveryStatusDto(deliveryFour.id.toString(), listOf(
                DeliveryStatusOperationDto(operationIdFour.toString(), UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(), listOf(), Instant.now(), null, DeliveryOperationStatusDto.IN_PROGRESS,
                    null, null, DeliverableMetaRequestDto(1L, 2L, 3L), DeliverableFolderOperationDto("", "",
                        java.time.Instant.now()))),
                listOf(), listOf(), listOf()),
            DeliveryStatusDto(deliveryFive.id.toString(), listOf(
                DeliveryStatusOperationDto(operationIdFive.toString(), UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(), listOf(), Instant.now(), null, DeliveryOperationStatusDto.IN_PROGRESS,
                    null, null, DeliverableMetaRequestDto(1L, 2L, 3L), DeliverableFolderOperationDto("", "",
                        java.time.Instant.now()))),
                listOf(), listOf(), listOf()),
            DeliveryStatusDto(deliverySix.id.toString(), listOf(
                DeliveryStatusOperationDto(operationIdSix.toString(), UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(), listOf(), Instant.now(), null, DeliveryOperationStatusDto.IN_PROGRESS,
                    null, null, DeliverableMetaRequestDto(1L, 2L, 3L), DeliverableFolderOperationDto("", "",
                        java.time.Instant.now()))),
                listOf(), listOf(), listOf())
        )))
        val deliveriesStatusByRequests = findDeliveryStatusByRequests(listOf(requestOne, requestTwo))
        Assertions.assertEquals(setOf(deliveryOne.id.toString(), deliveryTwo.id.toString(),
            deliveryThree.id.toString(), deliveryFour.id.toString(), deliveryFive.id.toString(),
            deliverySix.id.toString()), mockDApi.deliveryIds[0].toSet())
        val allPendingDeliveries = findPendingDeliveries()
        val pendingDeliveriesFirstPage = findPendingDeliveries(1, null)
        val pendingDeliveriesNextPage = findPendingDeliveries(2, pendingDeliveriesFirstPage.nextFrom!!)
        Assertions.assertEquals(2, deliveriesStatusByRequests.quotaRequests.size)
        val deliveriesStatusByRequestId = deliveriesStatusByRequests.quotaRequests.associateBy { it.requestId }
        Assertions.assertEquals(3, deliveriesStatusByRequestId[requestOne.id]!!.deliveries.size)
        Assertions.assertEquals(3, deliveriesStatusByRequestId[requestTwo.id]!!.deliveries.size)
        val deliveriesByIdOne = deliveriesStatusByRequestId[requestOne.id]!!.deliveries.associateBy { it.id }
        val deliveriesByIdTwo = deliveriesStatusByRequestId[requestTwo.id]!!.deliveries.associateBy { it.id }
        Assertions.assertFalse(deliveriesByIdOne[deliveryOne.id.toString()]!!.isPending())
        Assertions.assertFalse(deliveriesByIdOne[deliveryTwo.id.toString()]!!.isPending())
        Assertions.assertFalse(deliveriesByIdOne[deliveryThree.id.toString()]!!.isPending())
        Assertions.assertTrue(deliveriesByIdTwo[deliveryFour.id.toString()]!!.isPending())
        Assertions.assertTrue(deliveriesByIdTwo[deliveryFive.id.toString()]!!.isPending())
        Assertions.assertTrue(deliveriesByIdTwo[deliverySix.id.toString()]!!.isPending())
        Assertions.assertTrue(deliveriesByIdOne[deliveryOne.id.toString()]!!
            .operations.all { it.status == QuotaRequestDeliveryOperationStatus.SUCCESS })
        Assertions.assertTrue(deliveriesByIdOne[deliveryTwo.id.toString()]!!
            .operations.all { it.status == QuotaRequestDeliveryOperationStatus.SUCCESS })
        Assertions.assertTrue(deliveriesByIdOne[deliveryThree.id.toString()]!!
            .operations.all { it.status == QuotaRequestDeliveryOperationStatus.SUCCESS })
        Assertions.assertTrue(deliveriesByIdTwo[deliveryFour.id.toString()]!!
            .operations.all { it.status == QuotaRequestDeliveryOperationStatus.IN_PROGRESS })
        Assertions.assertTrue(deliveriesByIdTwo[deliveryFive.id.toString()]!!
            .operations.all { it.status == QuotaRequestDeliveryOperationStatus.IN_PROGRESS })
        Assertions.assertTrue(deliveriesByIdTwo[deliverySix.id.toString()]!!
            .operations.all { it.status == QuotaRequestDeliveryOperationStatus.IN_PROGRESS })
        Assertions.assertEquals(1, deliveriesByIdOne[deliveryOne.id.toString()]!!.operations.size)
        Assertions.assertEquals(1, deliveriesByIdOne[deliveryTwo.id.toString()]!!.operations.size)
        Assertions.assertEquals(1, deliveriesByIdOne[deliveryThree.id.toString()]!!.operations.size)
        Assertions.assertEquals(1, deliveriesByIdTwo[deliveryFour.id.toString()]!!.operations.size)
        Assertions.assertEquals(1, deliveriesByIdTwo[deliveryFive.id.toString()]!!.operations.size)
        Assertions.assertEquals(1, deliveriesByIdTwo[deliverySix.id.toString()]!!.operations.size)
        Assertions.assertEquals(4, deliveriesByIdOne[deliveryOne.id.toString()]!!.amounts.size)
        Assertions.assertEquals(8, deliveriesByIdOne[deliveryTwo.id.toString()]!!.amounts.size)
        Assertions.assertEquals(4, deliveriesByIdOne[deliveryThree.id.toString()]!!.amounts.size)
        Assertions.assertEquals(4, deliveriesByIdTwo[deliveryFour.id.toString()]!!.amounts.size)
        Assertions.assertEquals(8, deliveriesByIdTwo[deliveryFive.id.toString()]!!.amounts.size)
        Assertions.assertEquals(4, deliveriesByIdTwo[deliverySix.id.toString()]!!.amounts.size)
        Assertions.assertEquals(3, allPendingDeliveries.deliveries.size)
        Assertions.assertEquals(setOf(deliveryFour.id.toString(), deliveryFive.id.toString(),
            deliverySix.id.toString()), allPendingDeliveries.deliveries.map { it.id }.toSet())
        Assertions.assertEquals(1, pendingDeliveriesFirstPage.deliveries.size)
        Assertions.assertEquals(2, pendingDeliveriesNextPage.deliveries.size)
        Assertions.assertNull(pendingDeliveriesNextPage.nextFrom)
    }

    @Test
    fun testGetStatusSuccessEmptyResult() {
        val bigOrders = prepareBigOrders()
        val campaign = prepareCampaign(bigOrders)
        val campaignGroup = prepareCampaignGroup(campaign, bigOrders)
        val providerOne = prepareProvider("logfeller", 1)
        val providerTwo = prepareProvider("distbuild", 2)
        val providerThree = prepareProvider("strm", 3)
        val providerTwoSegmentationOne = prepareSegmentation("segmentationOne")
        val providerTwoSegmentationTwo = prepareSegmentation("segmentationTwo")
        val providerTwoSegmentationOneSegmentOne = prepareSegment(providerTwoSegmentationOne, "segmentOne")
        val providerTwoSegmentationOneSegmentTwo = prepareSegment(providerTwoSegmentationOne, "segmentTwo")
        val providerTwoSegmentationTwoSegmentOne = prepareSegment(providerTwoSegmentationTwo, "segmentThree")
        val providerTwoSegmentationTwoSegmentTwo = prepareSegment(providerTwoSegmentationTwo, "segmentFour")
        val providerOneResourceOne = prepareResource(providerOne, "resourceOne", DiResourceType.PROCESSOR)
        val providerOneResourceTwo = prepareResource(providerOne, "resourceTwo", DiResourceType.PROCESSOR)
        val providerTwoResourceOne = prepareResource(providerTwo, "resourceThree", DiResourceType.PROCESSOR,
            providerTwoSegmentationOne, providerTwoSegmentationTwo)
        val providerTwoResourceTwo = prepareResource(providerTwo, "resourceFour", DiResourceType.PROCESSOR,
            providerTwoSegmentationOne, providerTwoSegmentationTwo)
        val providerThreeResourceOne = prepareResource(providerThree, "resourceFive", DiResourceType.PROCESSOR)
        val providerThreeResourceTwo = prepareResource(providerThree, "resourceSix", DiResourceType.PROCESSOR)
        val projectOne = prepareProject("projectOne", 4)
        updateHierarchy()
        prepareCampaignResources()
        val requestOne = prepareRequestOne(bigOrders, providerOne, providerTwo, providerThree,
            providerTwoSegmentationOneSegmentOne, providerTwoSegmentationOneSegmentTwo,
            providerTwoSegmentationTwoSegmentOne, providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne,
            providerOneResourceTwo, providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne,
            providerThreeResourceTwo, projectOne, campaign)
        val requestTwo = prepareRequestTwo(bigOrders, providerOne, providerTwo, providerThree,
            providerTwoSegmentationOneSegmentOne, providerTwoSegmentationOneSegmentTwo,
            providerTwoSegmentationTwoSegmentOne, providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne,
            providerOneResourceTwo, providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne,
            providerThreeResourceTwo, projectOne, campaign)
        mockDApi.setStatus(DeliveryStatusResponseDto(listOf()))
        val deliveriesStatusByRequests = findDeliveryStatusByRequests(listOf(requestOne, requestTwo))
        Assertions.assertTrue(mockDApi.deliveryIds.isEmpty())
        val allPendingDeliveries = findPendingDeliveries()
        val pendingDeliveriesFirstPage = findPendingDeliveries(1, null)
        Assertions.assertEquals(2, deliveriesStatusByRequests.quotaRequests.size)
        Assertions.assertTrue(deliveriesStatusByRequests.quotaRequests.all { it.deliveries.isEmpty() })
        Assertions.assertTrue(allPendingDeliveries.deliveries.isEmpty())
        Assertions.assertTrue(pendingDeliveriesFirstPage.deliveries.isEmpty())
        Assertions.assertNull(pendingDeliveriesFirstPage.nextFrom)
    }

    private fun prepareRequestOne(
        bigOrders: List<BigOrder>, providerOne: Service, providerTwo: Service,
        providerThree: Service, providerTwoSegmentationOneSegmentOne: Segment,
        providerTwoSegmentationOneSegmentTwo: Segment, providerTwoSegmentationTwoSegmentOne: Segment,
        providerTwoSegmentationTwoSegmentTwo: Segment, providerOneResourceOne: Resource,
        providerOneResourceTwo: Resource, providerTwoResourceOne: Resource, providerTwoResourceTwo: Resource,
        providerThreeResourceOne: Resource, providerThreeResourceTwo: Resource, projectOne: Project,
        campaign: Campaign
    ): DiQuotaChangeRequest {
        return prepareQuotaRequest(
            projectOne.publicKey,
            campaign,
            ChangeBody(providerOne.key, providerOneResourceOne.publicKey, bigOrders[0].id,
                setOf(), DiAmount.of(42, DiUnit.CORES)),
            ChangeBody(providerOne.key, providerOneResourceOne.publicKey, bigOrders[1].id,
                setOf(), DiAmount.of(69, DiUnit.CORES)),
            ChangeBody(providerOne.key, providerOneResourceTwo.publicKey, bigOrders[0].id,
                setOf(), DiAmount.of(43, DiUnit.CORES)),
            ChangeBody(providerOne.key, providerOneResourceTwo.publicKey, bigOrders[1].id,
                setOf(), DiAmount.of(70, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceOne.publicKey, bigOrders[0].id,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey, providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(44, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceOne.publicKey, bigOrders[1].id,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey, providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(71, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceOne.publicKey, bigOrders[0].id,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey, providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(45, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceOne.publicKey, bigOrders[1].id,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey, providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(72, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceTwo.publicKey, bigOrders[0].id,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey, providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(46, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceTwo.publicKey, bigOrders[1].id,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey, providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(73, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceTwo.publicKey, bigOrders[0].id,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey, providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(47, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceTwo.publicKey, bigOrders[1].id,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey, providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(74, DiUnit.CORES)),
            ChangeBody(providerThree.key, providerThreeResourceOne.publicKey,
                bigOrders[0].id, setOf(), DiAmount.of(48, DiUnit.CORES)),
            ChangeBody(providerThree.key, providerThreeResourceOne.publicKey,
                bigOrders[1].id, setOf(), DiAmount.of(75, DiUnit.CORES)),
            ChangeBody(providerThree.key, providerThreeResourceTwo.publicKey,
                bigOrders[0].id, setOf(), DiAmount.of(49, DiUnit.CORES)),
            ChangeBody(providerThree.key, providerThreeResourceTwo.publicKey,
                bigOrders[1].id, setOf(), DiAmount.of(76, DiUnit.CORES))
        )
    }

    private fun prepareRequestTwo(
        bigOrders: List<BigOrder>, providerOne: Service, providerTwo: Service,
        providerThree: Service, providerTwoSegmentationOneSegmentOne: Segment,
        providerTwoSegmentationOneSegmentTwo: Segment, providerTwoSegmentationTwoSegmentOne: Segment,
        providerTwoSegmentationTwoSegmentTwo: Segment, providerOneResourceOne: Resource,
        providerOneResourceTwo: Resource, providerTwoResourceOne: Resource, providerTwoResourceTwo: Resource,
        providerThreeResourceOne: Resource, providerThreeResourceTwo: Resource, projectOne: Project,
        campaign: Campaign
    ): DiQuotaChangeRequest {
        return prepareQuotaRequest(
            projectOne.publicKey,
            campaign,
            ChangeBody(providerOne.key, providerOneResourceOne.publicKey, bigOrders[0].id,
                setOf(), DiAmount.of(420, DiUnit.CORES)),
            ChangeBody(providerOne.key, providerOneResourceOne.publicKey, bigOrders[1].id,
                setOf(), DiAmount.of(690, DiUnit.CORES)),
            ChangeBody(providerOne.key, providerOneResourceTwo.publicKey, bigOrders[0].id,
                setOf(), DiAmount.of(430, DiUnit.CORES)),
            ChangeBody(providerOne.key, providerOneResourceTwo.publicKey, bigOrders[1].id,
                setOf(), DiAmount.of(700, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceOne.publicKey, bigOrders[0].id,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey, providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(440, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceOne.publicKey, bigOrders[1].id,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey, providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(710, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceOne.publicKey, bigOrders[0].id,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey, providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(450, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceOne.publicKey, bigOrders[1].id,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey, providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(720, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceTwo.publicKey, bigOrders[0].id,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey, providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(460, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceTwo.publicKey, bigOrders[1].id,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey, providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(730, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceTwo.publicKey, bigOrders[0].id,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey, providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(470, DiUnit.CORES)),
            ChangeBody(providerTwo.key, providerTwoResourceTwo.publicKey, bigOrders[1].id,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey, providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(740, DiUnit.CORES)),
            ChangeBody(providerThree.key, providerThreeResourceOne.publicKey,
                bigOrders[0].id, setOf(), DiAmount.of(480, DiUnit.CORES)),
            ChangeBody(providerThree.key, providerThreeResourceOne.publicKey,
                bigOrders[1].id, setOf(), DiAmount.of(750, DiUnit.CORES)),
            ChangeBody(providerThree.key, providerThreeResourceTwo.publicKey,
                bigOrders[0].id, setOf(), DiAmount.of(490, DiUnit.CORES)),
            ChangeBody(providerThree.key, providerThreeResourceTwo.publicKey,
                bigOrders[1].id, setOf(), DiAmount.of(760, DiUnit.CORES))
        )
    }

    private fun setReadyRequestOne(
        bigOrders: List<BigOrder>, providerOne: Service, providerTwo: Service,
        providerThree: Service, providerTwoSegmentationOneSegmentOne: Segment,
        providerTwoSegmentationOneSegmentTwo: Segment, providerTwoSegmentationTwoSegmentOne: Segment,
        providerTwoSegmentationTwoSegmentTwo: Segment, providerOneResourceOne: Resource,
        providerOneResourceTwo: Resource, providerTwoResourceOne: Resource, providerTwoResourceTwo: Resource,
        providerThreeResourceOne: Resource, providerThreeResourceTwo: Resource,
        confirmedRequestOne: DiQuotaChangeRequest) {
        Assertions.assertEquals(DiSetAmountResult.SUCCESS, setQuotaRequestReady(confirmedRequestOne,
            SetResourceAmountBody.ChangeBody(providerOne.key, bigOrders[0].id,
                providerOneResourceOne.publicKey, setOf(), DiAmount.of(42, DiUnit.CORES),
                DiAmount.of(21, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerOne.key, bigOrders[1].id,
                providerOneResourceOne.publicKey, setOf(), DiAmount.of(69, DiUnit.CORES),
                DiAmount.of(34, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerOne.key, bigOrders[0].id,
                providerOneResourceTwo.publicKey, setOf(), DiAmount.of(43, DiUnit.CORES),
                DiAmount.of(22, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerOne.key, bigOrders[1].id,
                providerOneResourceTwo.publicKey, setOf(), DiAmount.of(70, DiUnit.CORES),
                DiAmount.of(35, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[0].id, providerTwoResourceOne.publicKey,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey,
                    providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(44, DiUnit.CORES), DiAmount.of(23, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[1].id, providerTwoResourceOne.publicKey,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey,
                    providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(71, DiUnit.CORES), DiAmount.of(36, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[0].id, providerTwoResourceOne.publicKey,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey,
                    providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(45, DiUnit.CORES), DiAmount.of(24, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[1].id, providerTwoResourceOne.publicKey,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey,
                    providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(72, DiUnit.CORES), DiAmount.of(37, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[0].id, providerTwoResourceTwo.publicKey,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey,
                    providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(46, DiUnit.CORES), DiAmount.of(25, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[1].id, providerTwoResourceTwo.publicKey,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey,
                    providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(73, DiUnit.CORES), DiAmount.of(38, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[0].id, providerTwoResourceTwo.publicKey,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey,
                    providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(47, DiUnit.CORES), DiAmount.of(26, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[1].id, providerTwoResourceTwo.publicKey,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey,
                    providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(74, DiUnit.CORES), DiAmount.of(39, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerThree.key, bigOrders[0].id,
                providerThreeResourceOne.publicKey, setOf(), DiAmount.of(48, DiUnit.CORES),
                DiAmount.of(27, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerThree.key, bigOrders[1].id,
                providerThreeResourceOne.publicKey, setOf(), DiAmount.of(75, DiUnit.CORES),
                DiAmount.of(40, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerThree.key, bigOrders[0].id,
                providerThreeResourceTwo.publicKey, setOf(), DiAmount.of(49, DiUnit.CORES),
                DiAmount.of(28, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerThree.key, bigOrders[1].id,
                providerThreeResourceTwo.publicKey, setOf(), DiAmount.of(76, DiUnit.CORES),
                DiAmount.of(41, DiUnit.CORES))
        )
        )
    }

    private fun setReadyRequestTwo(
        bigOrders: List<BigOrder>, providerOne: Service, providerTwo: Service,
        providerThree: Service, providerTwoSegmentationOneSegmentOne: Segment,
        providerTwoSegmentationOneSegmentTwo: Segment, providerTwoSegmentationTwoSegmentOne: Segment,
        providerTwoSegmentationTwoSegmentTwo: Segment, providerOneResourceOne: Resource,
        providerOneResourceTwo: Resource, providerTwoResourceOne: Resource, providerTwoResourceTwo: Resource,
        providerThreeResourceOne: Resource, providerThreeResourceTwo: Resource,
        confirmedRequestTwo: DiQuotaChangeRequest
    ) {
        Assertions.assertEquals(DiSetAmountResult.SUCCESS, setQuotaRequestReady(confirmedRequestTwo,
            SetResourceAmountBody.ChangeBody(providerOne.key, bigOrders[0].id,
                providerOneResourceOne.publicKey, setOf(), DiAmount.of(420, DiUnit.CORES),
                DiAmount.of(210, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerOne.key, bigOrders[1].id,
                providerOneResourceOne.publicKey, setOf(), DiAmount.of(690, DiUnit.CORES),
                DiAmount.of(340, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerOne.key, bigOrders[0].id,
                providerOneResourceTwo.publicKey, setOf(), DiAmount.of(430, DiUnit.CORES),
                DiAmount.of(220, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerOne.key, bigOrders[1].id,
                providerOneResourceTwo.publicKey, setOf(), DiAmount.of(700, DiUnit.CORES),
                DiAmount.of(350, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[0].id, providerTwoResourceOne.publicKey,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey,
                    providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(440, DiUnit.CORES), DiAmount.of(230, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[1].id, providerTwoResourceOne.publicKey,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey,
                    providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(710, DiUnit.CORES), DiAmount.of(360, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[0].id, providerTwoResourceOne.publicKey,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey,
                    providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(450, DiUnit.CORES), DiAmount.of(240, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[1].id, providerTwoResourceOne.publicKey,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey,
                    providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(720, DiUnit.CORES), DiAmount.of(370, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[0].id, providerTwoResourceTwo.publicKey,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey,
                    providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(460, DiUnit.CORES), DiAmount.of(250, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[1].id, providerTwoResourceTwo.publicKey,
                setOf(providerTwoSegmentationOneSegmentOne.publicKey,
                    providerTwoSegmentationTwoSegmentOne.publicKey),
                DiAmount.of(730, DiUnit.CORES), DiAmount.of(380, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[0].id, providerTwoResourceTwo.publicKey,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey,
                    providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(470, DiUnit.CORES), DiAmount.of(260, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerTwo.key, bigOrders[1].id, providerTwoResourceTwo.publicKey,
                setOf(providerTwoSegmentationOneSegmentTwo.publicKey,
                    providerTwoSegmentationTwoSegmentTwo.publicKey),
                DiAmount.of(740, DiUnit.CORES), DiAmount.of(390, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerThree.key, bigOrders[0].id,
                providerThreeResourceOne.publicKey, setOf(), DiAmount.of(480, DiUnit.CORES),
                DiAmount.of(270, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerThree.key, bigOrders[1].id,
                providerThreeResourceOne.publicKey, setOf(), DiAmount.of(750, DiUnit.CORES),
                DiAmount.of(400, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerThree.key, bigOrders[0].id,
                providerThreeResourceTwo.publicKey, setOf(), DiAmount.of(490, DiUnit.CORES),
                DiAmount.of(280, DiUnit.CORES)),
            SetResourceAmountBody.ChangeBody(providerThree.key, bigOrders[1].id,
                providerThreeResourceTwo.publicKey, setOf(), DiAmount.of(760, DiUnit.CORES),
                DiAmount.of(410, DiUnit.CORES))
        )
        )
    }

    private fun prepareBigOrders(): List<BigOrder> {
        val result = listOf(
            bigOrderManager.create(BigOrder.builder(LocalDate.of(2022, 6, 1))),
            bigOrderManager.create(BigOrder.builder(LocalDate.of(2022, 9, 1))),
            bigOrderManager.create(BigOrder.builder(LocalDate.of(2022, 12, 1)))
        )
        updateHierarchy()
        return result
    }

    private fun prepareCampaign(bigOrders: List<BigOrder>): Campaign {
        val result = campaignDaoImpl.create(
            Campaign.builder()
                .setKey("test")
                .setName("Test")
                .setStatus(Campaign.Status.ACTIVE)
                .setStartDate(LocalDate.of(2021, Month.AUGUST, 1))
                .setBigOrders(bigOrders.map { Campaign.BigOrder(it.id, it.date) })
                .setRequestCreationDisabled(false)
                .setRequestModificationDisabledForNonManagers(false)
                .setAllowedRequestModificationWhenClosed(false)
                .setAllowedModificationOnMissingAdditionalFields(false)
                .setForcedAllowingRequestStatusChange(false)
                .setAllowedRequestCreationForProviderAdmin(false)
                .setSingleProviderRequestModeEnabled(false)
                .setAllowedRequestCreationForCapacityPlanner(false)
                .setType(Campaign.Type.AGGREGATED)
                .setRequestModificationDisabled(false)
                .build())
        updateHierarchy()
        return result
    }

    private fun prepareCampaignGroup(campaign: Campaign, bigOrders: List<BigOrder>): BotCampaignGroup {
        val builder = BotCampaignGroup.builder()
            .setKey("test_campaign_group")
            .setName("Test Campaign Group")
            .setActive(true)
            .setBotPreOrderIssueKey("DISPENSERREQ-1")
            .addCampaign(campaignDaoImpl.readForBot(setOf(campaign.id))[campaign.id])
        bigOrders.forEach { builder.addBigOrder(it) }
        val result = botCampaignGroupDaoImpl.create(builder.build())
        updateHierarchy()
        return result
    }

    private fun prepareProvider(key: String, abcServiceId: Int): Service {
        val result = serviceDaoImpl.create(Service
            .withKey(key)
            .withName(key)
            .withAbcServiceId(abcServiceId)
            .withSettings(Service.Settings.builder().build())
            .build())
        updateHierarchy()
        return result
    }

    private fun prepareSegmentation(key: String): Segmentation {
        val result = segmentationDaoImpl.create(Segmentation.Builder(key)
            .name(key)
            .description(key)
            .build())
        updateHierarchy()
        return result
    }

    private fun prepareSegment(segmentation: Segmentation, key: String): Segment {
        val segment = segmentDaoImpl.create(Segment.Builder(key, segmentation)
            .name(key)
            .description(key)
            .priority(0.toShort())
            .build())
        updateHierarchy()
        return segment
    }

    private fun prepareResource(provider: Service, key: String, type: DiResourceType,
                                vararg segmentations: Segmentation
    ): Resource {
        val resource = resourceDaoImpl.create(Resource.Builder(key, provider)
            .name(key)
            .type(type)
            .mode(DiQuotingMode.DEFAULT)
            .build())
        updateHierarchy()
        for (segmentation in segmentations) {
            resourceSegmentationDaoImpl.create(ResourceSegmentation.Builder(resource, segmentation).build())
        }
        updateHierarchy()
        return resource
    }

    private fun prepareProject(key: String, abcServiceId: Int): Project {
        val parent = projectDaoImpl.read(YANDEX)
        val result = projectDaoImpl.create(Project
            .withKey(key)
            .name(key)
            .parent(parent)
            .abcServiceId(abcServiceId)
            .build())
        updateHierarchy()
        return result
    }

    private fun prepareQuotaRequest(abcServiceKey: String, campaign: Campaign, vararg changes: ChangeBody): DiQuotaChangeRequest {
        val builder = Body.BodyBuilder()
            .summary("test")
            .description("test")
            .calculations("test")
            .projectKey(abcServiceKey)
            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
        for (change in changes) {
            builder.changes(change)
        }
        val quotaRequests = dispenser().quotaChangeRequests()
            .create(builder.build(), campaign.id)
            .performBy(AMOSOV_F)
        return quotaRequests.first
    }

    private fun confirmRequest(request: DiQuotaChangeRequest): DiQuotaChangeRequest {
        val answered = dispenser()
            .quotaChangeRequests()
            .byId(request.id)
            .update(BodyUpdateBuilder()
                .chartLinksAbsenceExplanation("test")
                .requestGoalAnswers(QuotaChangeRequestValidationTest.GROWTH_ANSWER)
                .build())
            .performBy(AMOSOV_F)
        val readyForReview = dispenser()
            .quotaChangeRequests()
            .byId(answered.id)
            .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
            .performBy(AMOSOV_F)
        val approved = dispenser()
            .quotaChangeRequests()
            .byId(readyForReview.id)
            .setStatus(DiQuotaChangeRequest.Status.APPROVED)
            .performBy(AMOSOV_F)
        return dispenser()
            .quotaChangeRequests()
            .byId(approved.id)
            .setStatus(DiQuotaChangeRequest.Status.CONFIRMED)
            .performBy(AMOSOV_F)
    }

    private fun setQuotaRequestReady(request: DiQuotaChangeRequest,
                                     vararg changes: SetResourceAmountBody.ChangeBody
    ): DiSetAmountResult {
        val body = SetResourceAmountBody(listOf(SetResourceAmountBody.Item(request.id, null,
            listOf(*changes), "Test")))
        val response = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/resource-preorder/quotaState")
            .invoke(HttpMethod.PATCH, body)
        return response.readEntity(DiSetAmountResult::class.java)
    }

    private fun prepareDelivery(
        quotaRequest: DiQuotaChangeRequest,
        provider: Service, resolved: Boolean, externalResources: List<ExternalResource>,
        internalResources: List<InternalResource>, deliveryResults: List<DeliveryResult>): QuotaRequestDelivery {
        val author = personDaoImpl.readPersonByLogin(AMOSOV_F.login)
        val delivery = QuotaRequestDelivery.builder()
            .id(UUID.randomUUID())
            .authorId(author.id)
            .authorUid(author.uid)
            .abcServiceId(quotaRequest.project.abcServiceId!!.toLong())
            .quotaRequestId(quotaRequest.id)
            .campaignId(quotaRequest.campaign!!.id)
            .providerId(provider.id)
            .createdAt(Instant.now())
            .resolvedAt(if (resolved) {Instant.now()} else {null})
            .addExternalResources(externalResources)
            .addInternalResources(internalResources)
            .addDeliveryResults(deliveryResults)
            .resolved(resolved)
            .resolveStatus(if (resolved) {QuotaRequestDeliveryResolveStatus.RESOLVED} else {QuotaRequestDeliveryResolveStatus.IN_ALLOCATING_PROCESS})
            .build()
        deliveryDaoImpl.create(delivery);
        return delivery;
    }

    private fun findDeliveryStatusByRequests(
        requests: List<DiQuotaChangeRequest>
    ): DeliveryStatusResponse {
        val body = DeliveryStatusRequest(requests.map { it.id })
        val response = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/resource-preorder/find-delivery-status")
            .invoke(HttpMethod.POST, body)
        return response.readEntity(DeliveryStatusResponse::class.java)
    }

    private fun findPendingDeliveries(limit: Long? = null, from: String? = null): QuotaRequestPendingDeliveryResponse {
        val webClient = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/resource-preorder/find-pending-deliveries")
        if (limit != null) {
            webClient.replaceQueryParam("limit", limit)
        }
        if (from != null) {
            webClient.replaceQueryParam("from", from)
        }
        val response = webClient.invoke(HttpMethod.POST, null)
        return response.readEntity(QuotaRequestPendingDeliveryResponse::class.java)
    }

}
