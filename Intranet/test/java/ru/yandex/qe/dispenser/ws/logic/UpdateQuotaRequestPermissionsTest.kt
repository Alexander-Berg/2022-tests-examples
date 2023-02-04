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

class UpdateQuotaRequestPermissionsTest(
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
        val activeCampaign = prepareCampaign(bigOrders, Campaign.Type.DRAFT, false)
        val inactiveCampaign = prepareCampaign(bigOrders, Campaign.Type.DRAFT, true)
        prepareCampaignGroup(listOf(activeCampaign, inactiveCampaign), bigOrders)
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
        val createdRequestThree = createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personSix.login), changeBody)
        val createdRequestFour = createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personSix.login), changeBody)
        Assertions.assertTrue(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personOne.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personTwo.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personTwo.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personThree.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personThree.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personFour.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personFour.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personFive.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personFive.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personSix.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personSix.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personEight.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personEight.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personOne.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personTwo.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personTwo.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personThree.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personThree.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personFour.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personFour.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personFive.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personFive.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personSix.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personSix.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personEight.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personEight.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personTwo.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personTwo.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personThree.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personThree.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personFour.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personFour.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personFive.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personFive.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personSix.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personSix.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personEight.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personEight.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personTwo.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personTwo.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personThree.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personThree.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personFour.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personFour.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personFive.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personFive.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personSix.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personSix.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personEight.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personEight.login)))
    }

    @Test
    fun testAggregatedCampaign() {
        val bigOrders = prepareBigOrders()
        val activeCampaign = prepareCampaign(bigOrders, Campaign.Type.AGGREGATED, false)
        val inactiveCampaign = prepareCampaign(bigOrders, Campaign.Type.AGGREGATED, true)
        prepareCampaignGroup(listOf(activeCampaign, inactiveCampaign), bigOrders)
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
        val createdRequestThree = createQuotaRequest(projectOne.publicKey, inactiveCampaign.id,
            DiPerson.login(personSix.login), changeBody)
        val createdRequestFour = createQuotaRequest(projectTwo.publicKey, inactiveCampaign.id,
            DiPerson.login(personSix.login), changeBody)
        Assertions.assertFalse(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personTwo.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personTwo.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personThree.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personThree.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personFour.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personFour.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personFive.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personFive.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personSix.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personSix.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personSeven.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestOne.id, DiPerson.login(personEight.login)))
        Assertions.assertTrue(checkUpdatePermission(createdRequestTwo.id, DiPerson.login(personEight.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personTwo.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personTwo.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personThree.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personThree.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personFour.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personFour.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personFive.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personFive.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personSix.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personSix.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personSeven.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestOne.id, DiPerson.login(personEight.login)))
        Assertions.assertTrue(updateQuotaRequest(createdRequestTwo.id, DiPerson.login(personEight.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personTwo.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personTwo.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personThree.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personThree.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personFour.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personFour.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personFive.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personFive.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personSix.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personSix.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestThree.id, DiPerson.login(personEight.login)))
        Assertions.assertFalse(checkUpdatePermission(createdRequestFour.id, DiPerson.login(personEight.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personOne.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personTwo.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personTwo.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personThree.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personThree.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personFour.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personFour.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personFive.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personFive.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personSix.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personSix.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personSeven.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestThree.id, DiPerson.login(personEight.login)))
        Assertions.assertFalse(updateQuotaRequest(createdRequestFour.id, DiPerson.login(personEight.login)))

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

    private fun prepareCampaign(bigOrders: List<BigOrder>, type: Campaign.Type,
                                requestModificationDisabled: Boolean): Campaign {
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
                .setRequestModificationDisabled(requestModificationDisabled)
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
        for (change in changes) {
            builder.changes(change)
        }
        return dispenser().quotaChangeRequests()
            .create(builder.build(), campaignId)
            .performBy(author).first
    }

    private fun getQuotaRequest(id: Long, author: DiPerson): DiQuotaChangeRequest {
        return createAuthorizedLocalClient(author)
            .path("/v1/quota-requests/$id")
            .get(DiQuotaChangeRequest::class.java)
    }

    private fun checkUpdatePermission(id: Long, author: DiPerson): Boolean {
        return getQuotaRequest(id, author).permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT)
            && getQuotaRequest(id, author).permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP)
    }

    private fun updateQuotaRequest(id: Long, author: DiPerson): Boolean {
        return try {
            createAuthorizedLocalClient(author)
                .path("/v1/quota-requests/$id")
                .invoke("PATCH", BodyUpdate.BodyUpdateBuilder()
                    .description(UUID.randomUUID().toString())
                    .build(), DiQuotaChangeRequest::class.java)
            true
        } catch (e: Exception) {
            false
        }
    }

}
