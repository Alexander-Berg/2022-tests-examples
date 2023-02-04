package ru.yandex.qe.dispenser.ws.logic

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.inside.goals.model.Goal.Importance
import ru.yandex.inside.goals.model.Goal.Status
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
import ru.yandex.qe.dispenser.domain.dao.goal.Goal
import ru.yandex.qe.dispenser.domain.dao.goal.GoalDao
import ru.yandex.qe.dispenser.domain.dao.goal.OkrAncestors
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao
import ru.yandex.qe.dispenser.domain.dao.project.ProjectDao
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager
import java.time.LocalDate
import java.time.Month
import java.util.*

class QuotaRequestReviewFieldsTest(
    @Autowired private val bigOrderManager: BigOrderManager,
    @Autowired private val botCampaignGroupDaoImpl: BotCampaignGroupDao,
    @Autowired private val serviceDaoImpl: ServiceDao,
    @Autowired private val campaignDaoImpl: CampaignDao,
    @Autowired private val resourceDaoImpl: ResourceDao,
    @Autowired private val resourceSegmentationDaoImpl: ResourceSegmentationDao,
    @Autowired private val projectDaoImpl: ProjectDao,
    @Autowired private val personDaoImpl: PersonDao,
    @Autowired private val dispenserAdminsDao: DispenserAdminsDao,
    @Autowired private val goalDao: GoalDao
): AcceptanceTestBase() {

    @BeforeEach
    fun beforeEach() {
        bigOrderManager.clear()
        goalDao.clear()
    }

    @Test
    fun testCreate() {
        val bigOrders = prepareBigOrders()
        val activeCampaign = prepareCampaign(bigOrders, Campaign.Type.DRAFT, Campaign.Status.ACTIVE, false)
        prepareCampaignGroup(listOf(activeCampaign), bigOrders)
        val providerOne = prepareProvider("logfeller", 1)
        val providerOneResourceOne = prepareResource(providerOne, "resourceOne", DiResourceType.PROCESSOR)
        val root = projectDaoImpl.read(YANDEX)
        val projectOne = prepareProject("projectOne", root, 2)
        val personOne = preparePerson("personOne", 1)
        addAdmin(personOne)
        val goal = prepareGoal(4269L, "test", Importance.COMPANY, Status.PLANNED, OkrAncestors(emptyMap()))
        updateHierarchy()
        prepareCampaignResources()
        val body = Body.BodyBuilder()
            .summary("test")
            .description("test")
            .projectKey(projectOne.publicKey)
            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
            .changes(prepareChangeBody(providerOne, providerOneResourceOne, bigOrders[0]))
            .build()
        val created = createQuotaRequest(body, activeCampaign.id, DiPerson.login(personOne.login))
        Assertions.assertNotNull(created)
        val found = getQuotaRequest(created.id)
        Assertions.assertNotNull(found)
        val bodyWithReviewGrowth = Body.BodyBuilder()
            .summary("test")
            .description("test")
            .projectKey(projectOne.publicKey)
            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
            .changes(prepareChangeBody(providerOne, providerOneResourceOne, bigOrders[0]))
            .calculations("Test calculations")
            .chartLink("https://yandex.ru")
            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
            .build()
        val createdWithReviewGrowth = createQuotaRequest(bodyWithReviewGrowth, activeCampaign.id, DiPerson.login(personOne.login))
        Assertions.assertNotNull(createdWithReviewGrowth)
        val foundWithReviewGrowth = getQuotaRequest(createdWithReviewGrowth.id)
        Assertions.assertNotNull(foundWithReviewGrowth)
        val bodyWithReviewGoal = Body.BodyBuilder()
            .summary("test")
            .description("test")
            .projectKey(projectOne.publicKey)
            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
            .changes(prepareChangeBody(providerOne, providerOneResourceOne, bigOrders[0]))
            .calculations("Test calculations")
            .chartLinksAbsenceExplanation("Test explanation")
            .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
            .goalId(goal.id)
            .build()
        val createdWithReviewGoal = createQuotaRequest(bodyWithReviewGoal, activeCampaign.id, DiPerson.login(personOne.login))
        Assertions.assertNotNull(createdWithReviewGoal)
        val foundWithReviewGoal = getQuotaRequest(createdWithReviewGoal.id)
        Assertions.assertNotNull(foundWithReviewGoal)
    }

    @Test
    fun testUpdate() {
        val bigOrders = prepareBigOrders()
        val activeCampaign = prepareCampaign(bigOrders, Campaign.Type.DRAFT, Campaign.Status.ACTIVE, false)
        prepareCampaignGroup(listOf(activeCampaign), bigOrders)
        val providerOne = prepareProvider("logfeller", 1)
        val providerOneResourceOne = prepareResource(providerOne, "resourceOne", DiResourceType.PROCESSOR)
        val root = projectDaoImpl.read(YANDEX)
        val projectOne = prepareProject("projectOne", root, 2)
        val personOne = preparePerson("personOne", 1)
        addAdmin(personOne)
        val goal = prepareGoal(4269L, "test", Importance.COMPANY, Status.PLANNED, OkrAncestors(emptyMap()))
        updateHierarchy()
        prepareCampaignResources()
        val body = Body.BodyBuilder()
            .summary("test")
            .description("test")
            .projectKey(projectOne.publicKey)
            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
            .changes(prepareChangeBody(providerOne, providerOneResourceOne, bigOrders[0]))
            .build()
        val createdOne = createQuotaRequest(body, activeCampaign.id, DiPerson.login(personOne.login))
        Assertions.assertNotNull(createdOne)
        val foundOne = getQuotaRequest(createdOne.id)
        Assertions.assertNotNull(foundOne)
        val createdTwo = createQuotaRequest(body, activeCampaign.id, DiPerson.login(personOne.login))
        Assertions.assertNotNull(createdTwo)
        val foundTwo = getQuotaRequest(createdTwo.id)
        Assertions.assertNotNull(foundTwo)
        val updateWithGrowth = BodyUpdate.BodyUpdateBuilder()
            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
            .calculations("Test calculations")
            .chartLink("https://yandex.ru")
            .chartLinksAbsenceExplanation("")
            .requestGoalAnswers(mapOf(4L to "Answer one", 5L to "Answer two", 6L to "Answer three"))
            .build()
        val updatedWithGrowth = updateQuotaRequest(createdOne.id, updateWithGrowth, DiPerson.login(personOne.login))
        Assertions.assertNotNull(updatedWithGrowth)
        val foundWithGrowth = getQuotaRequest(updatedWithGrowth.id)
        Assertions.assertNotNull(foundWithGrowth)
        val updateWithGoal = BodyUpdate.BodyUpdateBuilder()
            .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
            .calculations("Test calculations")
            .chartLink(emptyList())
            .chartLinksAbsenceExplanation("Test explanation")
            .requestGoalAnswers(mapOf(0L to "Answer one", 1L to "Answer two", 2L to "Answer three"))
            .goalId(goal.id)
            .build()
        val updatedWithGoal = updateQuotaRequest(createdTwo.id, updateWithGoal, DiPerson.login(personOne.login))
        Assertions.assertNotNull(updatedWithGoal)
        val foundWithGoal = getQuotaRequest(updatedWithGoal.id)
        Assertions.assertNotNull(foundWithGoal)
        val updatedGrowthToGoal = updateQuotaRequest(updatedWithGrowth.id, updateWithGoal, DiPerson.login(personOne.login))
        Assertions.assertNotNull(updatedGrowthToGoal)
        val foundWithGrowthToGoal = getQuotaRequest(updatedGrowthToGoal.id)
        Assertions.assertNotNull(foundWithGrowthToGoal)
        val updatedGoalToGrowth = updateQuotaRequest(updatedWithGoal.id, updateWithGrowth, DiPerson.login(personOne.login))
        Assertions.assertNotNull(updatedGoalToGrowth)
        val foundWithGoalToGrowth = getQuotaRequest(updatedGoalToGrowth.id)
        Assertions.assertNotNull(foundWithGoalToGrowth)
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

    private fun preparePerson(login: String, uid: Long): Person {
        val result = personDaoImpl.create(Person(login, uid, false, false, false, PersonAffiliation.YANDEX))
        updateHierarchy()
        return result
    }

    private fun addAdmin(person: Person) {
        dispenserAdminsDao.setDispenserAdmins(dispenserAdminsDao.dispenserAdmins + person)
    }

    private fun prepareGoal(id: Long, name: String, importance: Importance, status: Status, okrAncestors: OkrAncestors): Goal {
        return goalDao.create(Goal(id, name, importance, status, okrAncestors))
    }

    private fun createQuotaRequest(request: Body, campaignId: Long, author: DiPerson): DiQuotaChangeRequest {
        return dispenser().quotaChangeRequests()
            .create(request, campaignId)
            .performBy(author).first
    }

    private fun getQuotaRequest(id: Long): DiQuotaChangeRequest {
        return dispenser().quotaChangeRequests().byId(id).get().perform()
    }

    private fun updateQuotaRequest(id: Long, update: BodyUpdate, author: DiPerson): DiQuotaChangeRequest {
        return dispenser().quotaChangeRequests().byId(id).update(update).performBy(author)
    }

}
