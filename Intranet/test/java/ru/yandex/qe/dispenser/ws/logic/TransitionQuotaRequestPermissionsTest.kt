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
import ru.yandex.qe.dispenser.api.v1.DiUnit
import ru.yandex.qe.dispenser.api.v1.request.quota.Body
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate
import ru.yandex.qe.dispenser.api.v1.request.quota.ChangeBody
import ru.yandex.qe.dispenser.client.v1.DiPerson
import ru.yandex.qe.dispenser.domain.BotCampaignGroup
import ru.yandex.qe.dispenser.domain.Campaign
import ru.yandex.qe.dispenser.domain.Person
import ru.yandex.qe.dispenser.domain.PersonAffiliation
import ru.yandex.qe.dispenser.domain.Project
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest
import ru.yandex.qe.dispenser.domain.Resource
import ru.yandex.qe.dispenser.domain.ResourceSegmentation
import ru.yandex.qe.dispenser.domain.Segmentation
import ru.yandex.qe.dispenser.domain.Service
import ru.yandex.qe.dispenser.domain.bot.BigOrder
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao
import ru.yandex.qe.dispenser.domain.dao.dispenser_admins.DispenserAdminsDao
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao
import ru.yandex.qe.dispenser.domain.dao.project.ProjectDao
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao
import ru.yandex.qe.dispenser.domain.hierarchy.Role
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager
import java.time.LocalDate
import java.time.Month
import java.util.*

class TransitionQuotaRequestPermissionsTest(
    @Autowired private val bigOrderManager: BigOrderManager,
    @Autowired private val botCampaignGroupDaoImpl: BotCampaignGroupDao,
    @Autowired private val serviceDaoImpl: ServiceDao,
    @Autowired private val campaignDaoImpl: CampaignDao,
    @Autowired private val resourceDaoImpl: ResourceDao,
    @Autowired private val resourceSegmentationDaoImpl: ResourceSegmentationDao,
    @Autowired private val projectDaoImpl: ProjectDao,
    @Autowired private val personDaoImpl: PersonDao,
    @Autowired private val dispenserAdminsDao: DispenserAdminsDao,
    @Autowired private val quotaChangeRequestDao: QuotaChangeRequestDao
): AcceptanceTestBase() {

    @BeforeEach
    fun beforeEachTest() {
        bigOrderManager.clear()
    }

    @Test
    fun testDraftCampaign() {
        val bigOrders = prepareBigOrders()
        val activeCampaign = prepareCampaign(bigOrders, Campaign.Type.DRAFT)
        prepareCampaignGroup(listOf(activeCampaign), bigOrders)
        val providerOne = prepareProvider("logfeller", 1)
        val providerOneResourceOne = prepareResource(providerOne, "resourceOne", DiResourceType.PROCESSOR)
        val root = projectDaoImpl.read(YANDEX)
        val projectOne = prepareProject("projectOne", root, 2)
        val projectTwo = prepareProject("projectTwo", projectOne, 3)
        val personOne = preparePerson("personOne", 1)
        val personTwo = preparePerson("personTwo", 2)
        val personThree = preparePerson("personThree", 3)
        val personFour = preparePerson("personFour", 4)
        val personFive = preparePerson("personFive", 5)
        val personSix = preparePerson("personSix", 6)
        val personSeven = preparePerson("personSeven", 7)
        val personEight = preparePerson("personEight", 8)
        addRoles(projectOne, Role.MEMBER, listOf(personOne))
        addRoles(projectTwo, Role.MEMBER, listOf(personTwo))
        addRoles(projectOne, Role.RESPONSIBLE, listOf(personThree))
        addRoles(projectTwo, Role.RESPONSIBLE, listOf(personFour))
        addRoles(root, Role.PROCESS_RESPONSIBLE, listOf(personFive))
        addAdmin(personSix)
        addRoles(projectOne, Role.RESOURCE_ORDER_MANAGER, listOf(personSeven))
        addRoles(projectTwo, Role.RESOURCE_ORDER_MANAGER, listOf(personEight))
        updateHierarchy()
        prepareCampaignResources()
        val changeBody = prepareChangeBody(providerOne, providerOneResourceOne, bigOrders[0])
        val createdRequestOne = createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personSix.login), changeBody)
        val createdRequestTwo = createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personSix.login), changeBody)
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO,
            DiQuotaChangeRequest.Permission.CAN_CONFIRM)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO,
                DiQuotaChangeRequest.Permission.CAN_CONFIRM)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO,
                DiQuotaChangeRequest.Permission.CAN_CONFIRM)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO,
                DiQuotaChangeRequest.Permission.CAN_CONFIRM)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO,
                DiQuotaChangeRequest.Status.CONFIRMED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO,
                DiQuotaChangeRequest.Status.CONFIRMED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO,
                DiQuotaChangeRequest.Status.CONFIRMED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO,
                DiQuotaChangeRequest.Status.CONFIRMED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
    }

    @Test
    fun testAggregatedCampaign() {
        val bigOrders = prepareBigOrders()
        val activeCampaign = prepareCampaign(bigOrders, Campaign.Type.AGGREGATED)
        prepareCampaignGroup(listOf(activeCampaign), bigOrders)
        val providerOne = prepareProvider("logfeller", 1)
        val providerOneResourceOne = prepareResource(providerOne, "resourceOne", DiResourceType.PROCESSOR)
        val root = projectDaoImpl.read(YANDEX)
        val projectOne = prepareProject("projectOne", root, 2)
        val projectTwo = prepareProject("projectTwo", projectOne, 3)
        val personOne = preparePerson("personOne", 1)
        val personTwo = preparePerson("personTwo", 2)
        val personThree = preparePerson("personThree", 3)
        val personFour = preparePerson("personFour", 4)
        val personFive = preparePerson("personFive", 5)
        val personSix = preparePerson("personSix", 6)
        val personSeven = preparePerson("personSeven", 7)
        val personEight = preparePerson("personEight", 8)
        addRoles(projectOne, Role.MEMBER, listOf(personOne))
        addRoles(projectTwo, Role.MEMBER, listOf(personTwo))
        addRoles(projectOne, Role.RESPONSIBLE, listOf(personThree))
        addRoles(projectTwo, Role.RESPONSIBLE, listOf(personFour))
        addRoles(root, Role.PROCESS_RESPONSIBLE, listOf(personFive))
        addAdmin(personSix)
        addRoles(projectOne, Role.RESOURCE_ORDER_MANAGER, listOf(personSeven))
        addRoles(projectTwo, Role.RESOURCE_ORDER_MANAGER, listOf(personEight))
        updateHierarchy()
        prepareCampaignResources()
        val changeBody = prepareChangeBody(providerOne, providerOneResourceOne, bigOrders[0])
        val createdRequestOne = createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personSix.login), changeBody)
        val createdRequestTwo = createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personSix.login), changeBody)
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO,
                DiQuotaChangeRequest.Permission.CAN_CONFIRM)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO,
                DiQuotaChangeRequest.Permission.CAN_CONFIRM)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO,
                DiQuotaChangeRequest.Permission.CAN_CONFIRM)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_APPROVE,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL, DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO,
                DiQuotaChangeRequest.Permission.CAN_CONFIRM)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_APPROVE, DiQuotaChangeRequest.Permission.CAN_REJECT)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitionPermission(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW, DiQuotaChangeRequest.Permission.CAN_CANCEL)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personOne.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personTwo.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personThree.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFour.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO,
                DiQuotaChangeRequest.Status.CONFIRMED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO,
                DiQuotaChangeRequest.Status.CONFIRMED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personFive.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO,
                DiQuotaChangeRequest.Status.CONFIRMED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.APPROVED,
                DiQuotaChangeRequest.Status.NEED_INFO)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.REJECTED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf(DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED, DiQuotaChangeRequest.Status.REJECTED, DiQuotaChangeRequest.Status.NEED_INFO,
                DiQuotaChangeRequest.Status.CONFIRMED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSix.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED,
                DiQuotaChangeRequest.Status.APPROVED, DiQuotaChangeRequest.Status.REJECTED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personSeven.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CANCELLED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.APPROVED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestOne.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEW,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.READY_FOR_REVIEW,
            setOf(DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CANCELLED,
            setOf(DiQuotaChangeRequest.Status.NEW)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.REJECTED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.CONFIRMED,
            setOf()))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.APPROVED,
            setOf(DiQuotaChangeRequest.Status.CANCELLED)))
        Assertions.assertTrue(checkTransitions(createdRequestTwo.id, DiPerson.login(personEight.login), QuotaChangeRequest.Status.NEED_INFO,
            setOf(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, DiQuotaChangeRequest.Status.CANCELLED)))
    }

    private fun prepareChangeBody(provider: Service, resource: Resource, bigOrder: BigOrder): ChangeBody {
        return ChangeBody(provider.key, resource.publicKey, bigOrder.id, setOf(), DiAmount.of(42, DiUnit.CORES))
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

    private fun prepareCampaign(bigOrders: List<BigOrder>, type: Campaign.Type): Campaign {
        val result = campaignDaoImpl.create(
            Campaign.builder()
                .setKey(UUID.randomUUID().toString())
                .setName(UUID.randomUUID().toString())
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
                .setType(type)
                .setRequestModificationDisabled(false)
                .build())
        updateHierarchy()
        return result
    }

    private fun prepareCampaignGroup(campaigns: List<Campaign>, bigOrders: List<BigOrder>): BotCampaignGroup {
        val builder = BotCampaignGroup.builder()
            .setKey("test_campaign_group")
            .setName("Test Campaign Group")
            .setActive(true)
            .setBotPreOrderIssueKey("DISPENSERREQ-1")
            .setCampaigns(campaignDaoImpl.readForBot(campaigns.map { it.id }.toSet()).values.toList())
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

    private fun prepareProject(key: String, parent: Project, abcServiceId: Int): Project {
        val result = projectDaoImpl.create(Project
            .withKey(key)
            .name(key)
            .parent(parent)
            .abcServiceId(abcServiceId)
            .build())
        updateHierarchy()
        return result
    }

    private fun addRoles(project: Project, role: Role, persons: List<Person>) {
        projectDaoImpl.attachAll(persons, emptyList(), project, role)
        updateHierarchy()
    }

    private fun preparePerson(login: String, uid: Long): Person {
        val result = personDaoImpl.create(Person(login, uid, false, false, false, PersonAffiliation.YANDEX))
        updateHierarchy()
        return result
    }

    private fun addAdmin(person: Person) {
        dispenserAdminsDao.setDispenserAdmins(dispenserAdminsDao.dispenserAdmins + person)
    }

    private fun createQuotaRequest(abcServiceKey: String, campaignId: Long, author: DiPerson, vararg changes: ChangeBody): DiQuotaChangeRequest {
        val builder = Body.BodyBuilder()
            .summary("test")
            .description("test")
            .calculations("test")
            .projectKey(abcServiceKey)
            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
            .calculations("Test")
            .chartLinksAbsenceExplanation("Test")
        for (change in changes) {
            builder.changes(change)
        }
        val request = dispenser().quotaChangeRequests()
            .create(builder.build(), campaignId)
            .performBy(author).first
        return createAuthorizedLocalClient(author)
            .path("/v1/quota-requests/${request.id}")
            .invoke("PATCH", BodyUpdate.BodyUpdateBuilder()
                .requestGoalAnswers(mapOf(4L to "Test", 5L to "Test", 6L to "Test"))
                .build(), DiQuotaChangeRequest::class.java)
    }

    private fun getQuotaRequest(id: Long, author: DiPerson): DiQuotaChangeRequest {
        return createAuthorizedLocalClient(author)
            .path("/v1/quota-requests/$id")
            .get(DiQuotaChangeRequest::class.java)
    }

    private fun checkTransitionPermission(id: Long, author: DiPerson, status: QuotaChangeRequest.Status,
                                          permissions: Set<DiQuotaChangeRequest.Permission>): Boolean {
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(listOf(id))[id]!!.copyBuilder().status(status).build())
        val actualPermissions = getQuotaRequest(id, author).permissions
        val expectedMissingPermissions = setOf(DiQuotaChangeRequest.Permission.CAN_REOPEN,
            DiQuotaChangeRequest.Permission.CAN_CANCEL,
            DiQuotaChangeRequest.Permission.CAN_APPLY,
            DiQuotaChangeRequest.Permission.CAN_CONFIRM,
            DiQuotaChangeRequest.Permission.CAN_REJECT,
            DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW,
            DiQuotaChangeRequest.Permission.CAN_COMPLETE,
            DiQuotaChangeRequest.Permission.CAN_MARK_AS_NEED_INFO,
            DiQuotaChangeRequest.Permission.CAN_APPROVE)
            .subtract(permissions)
        return permissions.all { actualPermissions.contains(it) }
            && expectedMissingPermissions.all { !actualPermissions.contains(it) }
    }

    private fun checkTransitions(id: Long, author: DiPerson, status: QuotaChangeRequest.Status,
                                 statuses: Set<DiQuotaChangeRequest.Status>): Boolean {
        val otherStatuses = setOf(DiQuotaChangeRequest.Status.NEW,
            DiQuotaChangeRequest.Status.CANCELLED,
            DiQuotaChangeRequest.Status.REJECTED,
            DiQuotaChangeRequest.Status.APPLIED,
            DiQuotaChangeRequest.Status.CONFIRMED,
            DiQuotaChangeRequest.Status.READY_FOR_REVIEW,
            DiQuotaChangeRequest.Status.COMPLETED,
            DiQuotaChangeRequest.Status.NEED_INFO,
            DiQuotaChangeRequest.Status.APPROVED)
            .subtract(statuses).subtract(setOf(status.toView()))
        return statuses.all {
            quotaChangeRequestDao.update(quotaChangeRequestDao.read(listOf(id))[id]!!.copyBuilder().status(status).build())
            transitionQuotaRequest(id, author, it)
        } && otherStatuses.all {
            quotaChangeRequestDao.update(quotaChangeRequestDao.read(listOf(id))[id]!!.copyBuilder().status(status).build())
            !transitionQuotaRequest(id, author, it)
        }
    }

    private fun transitionQuotaRequest(id: Long, author: DiPerson, status: DiQuotaChangeRequest.Status): Boolean {
        return try {
            createAuthorizedLocalClient(author)
                .path("/v1/quota-requests/$id/status/${status.name}")
                .invoke("PUT", null, DiQuotaChangeRequest::class.java)
            true
        } catch (e: Exception) {
            false
        }
    }

}
