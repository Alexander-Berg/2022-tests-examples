package ru.yandex.qe.dispenser.ws.logic

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.qe.dispenser.api.v1.DiAmount
import ru.yandex.qe.dispenser.api.v1.DiCampaign
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType
import ru.yandex.qe.dispenser.api.v1.DiResourceType
import ru.yandex.qe.dispenser.api.v1.DiUnit
import ru.yandex.qe.dispenser.api.v1.field.DiProjectFields
import ru.yandex.qe.dispenser.api.v1.project.DiExtendedProject
import ru.yandex.qe.dispenser.api.v1.request.quota.Body
import ru.yandex.qe.dispenser.api.v1.request.quota.ChangeBody
import ru.yandex.qe.dispenser.client.v1.DiPerson
import ru.yandex.qe.dispenser.domain.BotCampaignGroup
import ru.yandex.qe.dispenser.domain.Campaign
import ru.yandex.qe.dispenser.domain.Person
import ru.yandex.qe.dispenser.domain.PersonAffiliation
import ru.yandex.qe.dispenser.domain.Project
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
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao
import ru.yandex.qe.dispenser.domain.hierarchy.Role
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager
import java.time.LocalDate
import java.time.Month
import java.util.*
import javax.ws.rs.HttpMethod
import javax.ws.rs.core.GenericType

class CreateQuotaRequestPermissionsTest(
    @Autowired private val bigOrderManager: BigOrderManager,
    @Autowired private val botCampaignGroupDaoImpl: BotCampaignGroupDao,
    @Autowired private val serviceDaoImpl: ServiceDao,
    @Autowired private val campaignDaoImpl: CampaignDao,
    @Autowired private val resourceDaoImpl: ResourceDao,
    @Autowired private val resourceSegmentationDaoImpl: ResourceSegmentationDao,
    @Autowired private val projectDaoImpl: ProjectDao,
    @Autowired private val personDaoImpl: PersonDao,
    @Autowired private val dispenserAdminsDao: DispenserAdminsDao
): AcceptanceTestBase() {

    @BeforeEach
    fun beforeEachTest() {
        bigOrderManager.clear()
    }

    @Test
    fun testDraftCampaign() {
        val bigOrders = prepareBigOrders()
        val activeCampaign = prepareCampaign(bigOrders, Campaign.Type.DRAFT, Campaign.Status.ACTIVE, false)
        val inactiveCampaign = prepareCampaign(bigOrders, Campaign.Type.DRAFT, Campaign.Status.CLOSED, false)
        val noCreationCampaign = prepareCampaign(bigOrders, Campaign.Type.DRAFT, Campaign.Status.ACTIVE, true)
        prepareCampaignGroup(listOf(activeCampaign, inactiveCampaign, noCreationCampaign), bigOrders)
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
        Assertions.assertTrue(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personOne.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personOne.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personOne.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personOne.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personOne.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personOne.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personTwo.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personTwo.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personTwo.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personTwo.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personTwo.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personTwo.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personThree.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personThree.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personThree.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personThree.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personThree.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personThree.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personFour.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personFour.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personFour.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personFour.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personFour.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personFour.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personFive.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personFive.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personFive.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personFive.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personFive.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personFive.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personSix.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personSix.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personSix.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personSix.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personSix.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personSix.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personSeven.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personSeven.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personSeven.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personSeven.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personSeven.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personSeven.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personEight.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personEight.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personEight.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personEight.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personEight.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personEight.login), changeBody))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectOne,
            DiPerson.login(personOne.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectTwo,
            DiPerson.login(personOne.login)))
        Assertions.assertEquals(emptySet<Long>(), getAvailableCampaigns(projectOne,
            DiPerson.login(personTwo.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectTwo,
            DiPerson.login(personTwo.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectOne,
            DiPerson.login(personThree.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectTwo,
            DiPerson.login(personThree.login)))
        Assertions.assertEquals(emptySet<Long>(), getAvailableCampaigns(projectOne,
            DiPerson.login(personFour.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectTwo,
            DiPerson.login(personFour.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectOne,
            DiPerson.login(personFive.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectTwo,
            DiPerson.login(personFive.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectOne,
            DiPerson.login(personSix.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectTwo,
            DiPerson.login(personSix.login)))
        Assertions.assertEquals(emptySet<Long>(), getAvailableCampaigns(projectOne,
            DiPerson.login(personSeven.login)))
        Assertions.assertEquals(emptySet<Long>(), getAvailableCampaigns(projectTwo,
            DiPerson.login(personSeven.login)))
        Assertions.assertEquals(emptySet<Long>(), getAvailableCampaigns(projectOne,
            DiPerson.login(personEight.login)))
        Assertions.assertEquals(emptySet<Long>(), getAvailableCampaigns(projectTwo,
            DiPerson.login(personEight.login)))
        Assertions.assertTrue(checkCreatePermissions(projectOne, DiPerson.login(personOne.login)))
        Assertions.assertTrue(checkCreatePermissions(projectTwo, DiPerson.login(personOne.login)))
        Assertions.assertFalse(checkCreatePermissions(projectOne, DiPerson.login(personTwo.login)))
        Assertions.assertTrue(checkCreatePermissions(projectTwo, DiPerson.login(personTwo.login)))
        Assertions.assertTrue(checkCreatePermissions(projectOne, DiPerson.login(personThree.login)))
        Assertions.assertTrue(checkCreatePermissions(projectTwo, DiPerson.login(personThree.login)))
        Assertions.assertFalse(checkCreatePermissions(projectOne, DiPerson.login(personFour.login)))
        Assertions.assertTrue(checkCreatePermissions(projectTwo, DiPerson.login(personFour.login)))
        Assertions.assertTrue(checkCreatePermissions(projectOne, DiPerson.login(personFive.login)))
        Assertions.assertTrue(checkCreatePermissions(projectTwo, DiPerson.login(personFive.login)))
        Assertions.assertTrue(checkCreatePermissions(projectOne, DiPerson.login(personSix.login)))
        Assertions.assertTrue(checkCreatePermissions(projectTwo, DiPerson.login(personSix.login)))
        Assertions.assertFalse(checkCreatePermissions(projectOne, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(checkCreatePermissions(projectTwo, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(checkCreatePermissions(projectOne, DiPerson.login(personEight.login)))
        Assertions.assertFalse(checkCreatePermissions(projectTwo, DiPerson.login(personEight.login)))
    }

    @Test
    fun testAggregatedCampaign() {
        val bigOrders = prepareBigOrders()
        val activeCampaign = prepareCampaign(bigOrders, Campaign.Type.AGGREGATED, Campaign.Status.ACTIVE, false)
        val inactiveCampaign = prepareCampaign(bigOrders, Campaign.Type.AGGREGATED, Campaign.Status.CLOSED, false)
        val noCreationCampaign = prepareCampaign(bigOrders, Campaign.Type.AGGREGATED, Campaign.Status.ACTIVE, true)
        prepareCampaignGroup(listOf(activeCampaign, inactiveCampaign, noCreationCampaign), bigOrders)
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
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personOne.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personOne.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personOne.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personOne.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personOne.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personOne.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personTwo.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personTwo.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personTwo.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personTwo.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personTwo.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personTwo.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personThree.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personThree.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personThree.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personThree.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personThree.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personThree.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personFour.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personFour.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personFour.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personFour.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personFour.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personFour.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personFive.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personFive.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personFive.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personFive.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personFive.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personFive.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personSix.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personSix.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personSix.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personSix.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personSix.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personSix.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personSeven.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personSeven.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personSeven.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personSeven.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personSeven.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personSeven.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, activeCampaign.id,
            DiPerson.login(personEight.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personEight.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectOne.publicKey, noCreationCampaign.id,
            DiPerson.login(personEight.login), changeBody))
        Assertions.assertTrue(createQuotaRequest(projectTwo.publicKey, activeCampaign.id,
            DiPerson.login(personEight.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personEight.login), changeBody))
        Assertions.assertFalse(createQuotaRequest(projectTwo.publicKey, noCreationCampaign.id,
            DiPerson.login(personEight.login), changeBody))
        Assertions.assertEquals(emptySet<Long>(), getAvailableCampaigns(projectOne,
            DiPerson.login(personOne.login)))
        Assertions.assertEquals(emptySet<Long>(), getAvailableCampaigns(projectTwo,
            DiPerson.login(personOne.login)))
        Assertions.assertEquals(emptySet<Long>(), getAvailableCampaigns(projectOne,
            DiPerson.login(personTwo.login)))
        Assertions.assertEquals(emptySet<Long>(), getAvailableCampaigns(projectTwo,
            DiPerson.login(personTwo.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectOne,
            DiPerson.login(personThree.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectTwo,
            DiPerson.login(personThree.login)))
        Assertions.assertEquals(emptySet<Long>(), getAvailableCampaigns(projectOne,
            DiPerson.login(personFour.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectTwo,
            DiPerson.login(personFour.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectOne,
            DiPerson.login(personFive.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectTwo,
            DiPerson.login(personFive.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectOne,
            DiPerson.login(personSix.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectTwo,
            DiPerson.login(personSix.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectOne,
            DiPerson.login(personSeven.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectTwo,
            DiPerson.login(personSeven.login)))
        Assertions.assertEquals(emptySet<Long>(), getAvailableCampaigns(projectOne,
            DiPerson.login(personEight.login)))
        Assertions.assertEquals(setOf(activeCampaign.id), getAvailableCampaigns(projectTwo,
            DiPerson.login(personEight.login)))
        Assertions.assertFalse(checkCreatePermissions(projectOne, DiPerson.login(personOne.login)))
        Assertions.assertFalse(checkCreatePermissions(projectTwo, DiPerson.login(personOne.login)))
        Assertions.assertFalse(checkCreatePermissions(projectOne, DiPerson.login(personTwo.login)))
        Assertions.assertFalse(checkCreatePermissions(projectTwo, DiPerson.login(personTwo.login)))
        Assertions.assertTrue(checkCreatePermissions(projectOne, DiPerson.login(personThree.login)))
        Assertions.assertTrue(checkCreatePermissions(projectTwo, DiPerson.login(personThree.login)))
        Assertions.assertFalse(checkCreatePermissions(projectOne, DiPerson.login(personFour.login)))
        Assertions.assertTrue(checkCreatePermissions(projectTwo, DiPerson.login(personFour.login)))
        Assertions.assertTrue(checkCreatePermissions(projectOne, DiPerson.login(personFive.login)))
        Assertions.assertTrue(checkCreatePermissions(projectTwo, DiPerson.login(personFive.login)))
        Assertions.assertTrue(checkCreatePermissions(projectOne, DiPerson.login(personSix.login)))
        Assertions.assertTrue(checkCreatePermissions(projectTwo, DiPerson.login(personSix.login)))
        Assertions.assertTrue(checkCreatePermissions(projectOne, DiPerson.login(personSeven.login)))
        Assertions.assertTrue(checkCreatePermissions(projectTwo, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(checkCreatePermissions(projectOne, DiPerson.login(personEight.login)))
        Assertions.assertTrue(checkCreatePermissions(projectTwo, DiPerson.login(personEight.login)))
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

    private fun prepareCampaign(bigOrders: List<BigOrder>, type: Campaign.Type, status: Campaign.Status,
                                requestCreationDisabled: Boolean): Campaign {
        val result = campaignDaoImpl.create(
            Campaign.builder()
                .setKey(UUID.randomUUID().toString())
                .setName(UUID.randomUUID().toString())
                .setStatus(status)
                .setStartDate(LocalDate.of(2021, Month.AUGUST, 1))
                .setBigOrders(bigOrders.map { Campaign.BigOrder(it.id, it.date) })
                .setRequestCreationDisabled(requestCreationDisabled)
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

    private fun createQuotaRequest(abcServiceKey: String, campaignId: Long, author: DiPerson, vararg changes: ChangeBody): Boolean {
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
        try {
            dispenser().quotaChangeRequests()
                .create(builder.build(), campaignId)
                .performBy(author)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun getAvailableCampaigns(project: Project, author: DiPerson): Set<Long> {
        val response = createAuthorizedLocalClient(author)
            .path("/v1/projects/${project.publicKey}/availableCampaigns")
            .invoke(HttpMethod.GET, null)
        return response.readEntity(object : GenericType<List<DiCampaign>>() {}).map { it.id }.toSet()
    }

    private fun checkCreatePermissions(project: Project, author: DiPerson): Boolean {
        val response = createAuthorizedLocalClient(author)
            .path("/v1/projects/${project.publicKey}")
            .replaceQueryParam("field", DiProjectFields.PERMISSIONS.key)
            .invoke(HttpMethod.GET, null)
        return response.readEntity(DiExtendedProject::class.java).permissions
            .contains(DiExtendedProject.Permission.CAN_CREATE_QUOTA_REQUEST)
    }

}
