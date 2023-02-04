package ru.yandex.qe.dispenser.ws.logic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.PersonAffiliation;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Role;
import ru.yandex.qe.dispenser.standalone.MockTrackerManager;
import ru.yandex.qe.dispenser.ws.quota.request.QuotaChangeRequestManager;
import ru.yandex.qe.dispenser.ws.quota.request.ticket.QuotaChangeRequestTicketManager;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.GROWTH_ANSWER;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;
import static ru.yandex.qe.dispenser.ws.quota.request.ticket.QuotaChangeRequestTicketManager.CRITICAL_TAG;

public class QuotaChangeRequestTicketManagerTest extends BaseQuotaRequestTest {

    @Inject
    private MockTrackerManager trackerManager;

    @Inject
    private QuotaChangeRequestDao quotaChangeRequestDao;

    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ResourceSegmentationDao resourceSegmentationDao;

    @Autowired
    private SegmentationDao segmentationDao;

    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private PersonDao personDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
    }

    @Test
    public void testResourcesTableGeneration() {
        List<QuotaChangeRequest.Change> changes = new ArrayList<>();

        Set<Segment> segments = new HashSet<>();
        segments.add(new Segment.Builder("segmentKey1",
                new Segmentation.Builder("segmentation1").build())
                .name("Segment Key 1").build());
        segments.add(new Segment.Builder("segmentKey2",
                new Segmentation.Builder("segmentation2").build())
                .name("Segment Key 2").build());
        Set<Segment> segments2 = new HashSet<>();
        segments2.add(new Segment.Builder("segmentKey3",
                new Segmentation.Builder("segmentation3").build())
                .name("Segment Key 3").build());
        Service service = Service.withKey("test").withName("Test").build();
        Resource resource1 = new Resource.Builder("key1", service).name("test1").type(DiResourceType.STORAGE).build();
        Resource resource2 = new Resource.Builder("key2", service).name("test2").type(DiResourceType.STORAGE).build();
        Resource resource3 = new Resource.Builder("key3", service).name("test3").type(DiResourceType.STORAGE).build();
        changes.add(QuotaChangeRequest.Change.newChangeBuilder().resource(resource1).segments(segments).amount(100).build());
        changes.add(QuotaChangeRequest.Change.newChangeBuilder().resource(resource2).segments(segments).amount(200).build());
        changes.add(QuotaChangeRequest.Change.newChangeBuilder().resource(resource1).segments(segments).amount(200).build());
        changes.add(QuotaChangeRequest.Change.newChangeBuilder().resource(resource2).segments(segments2).amount(200).build());
        changes.add(QuotaChangeRequest.Change.newChangeBuilder().resource(resource3).segments(segments2).amount(200).build());

        QuotaChangeRequest quotaChangeRequest = new QuotaChangeRequest.Builder()
                .author(new Person("lotrek", 1120000000024781L, false, false, false, PersonAffiliation.YANDEX))
                .calculations("test")
                .chartLinks(new ArrayList<>())
                .chartLinksAbsenceExplanation("test")
                .comment("test")
                .description("test")
                .project(new Project("key", "name", null))
                .created(Instant.now().getEpochSecond())
                .updated(Instant.now().getEpochSecond())
                .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                .status(QuotaChangeRequest.Status.NEW)
                .changes(changes)
                .cost(0)
                .requestOwningCost(0L)
                .build();
        List<List<String>> resources = QuotaChangeRequestTicketManager.getResources(quotaChangeRequest);

        String[] topRaw = {"", "test1", "test2", "test3"};
        String[] raw0 = {"", "300 B", "400 B", "200 B"};
        String[] raw1 = {"Segment Key 1, Segment Key 2", "300 B", "200 B", "0 B"};
        String[] raw2 = {"Segment Key 3", "0 B", "200 B", "200 B"};
        assertArrayEquals(resources.get(0).toArray(), topRaw);
        assertArrayEquals(resources.get(1).toArray(), raw0);
        assertArrayEquals(resources.get(2).toArray(), raw1);
        assertArrayEquals(resources.get(3).toArray(), raw2);
    }

    @Test
    public void testMoveRequests() {

        final Service service = serviceDao.read(NIRVANA);
        final Resource resource = resourceDao.read(new Resource.Key(YT_CPU, service));
        final List<QuotaChangeRequest.Change> changes = Collections.singletonList(QuotaChangeRequest.Change.newChangeBuilder().resource(resource).segments(Collections.emptySet()).amount(100).build());

        final Project rootProject = projectDao.getRoot();
        final Project project = Project.withKey("key").name("name").abcServiceId(1001).parent(rootProject).build();
        final Project otherProject = Project.withKey("test2").name("Test-2").abcServiceId(1002).parent(rootProject).build();

        final Map<Project.Key, Project> projects = new HashMap<>();
        if (transactionManager != null) {
            new TransactionTemplate(transactionManager).execute(status -> {
                projects.putAll(projectDao.createAllIfAbsent(Arrays.asList(project, otherProject)));
                projectDao.attach(personDao.readPersonByLogin("lotrek"), projects.get(otherProject.getKey()), Role.RESPONSIBLE);
                return null;
            });
        } else {
            projects.putAll(projectDao.createAllIfAbsent(Arrays.asList(project, otherProject)));
            projectDao.attach(personDao.readPersonByLogin("lotrek"), projects.get(otherProject.getKey()), Role.RESPONSIBLE);
        }
        prepareCampaignResources();

        final String issueKey = trackerManager.createIssues(IssueCreate.builder()
                .assignee(AMOSOV_F.getLogin())
                .build());

        final Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        QuotaChangeRequest quotaChangeRequest = new QuotaChangeRequest.Builder()
                .summary("test")
                .author(personDao.read(LOTREK.getLogin()))
                .calculations("test")
                .chartLinks(new ArrayList<>())
                .chartLinksAbsenceExplanation("test")
                .comment("test")
                .description("test")
                .project(projects.get(project.getKey()))
                .created(Instant.now().getEpochSecond())
                .updated(Instant.now().getEpochSecond())
                .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                .status(QuotaChangeRequest.Status.NEW)
                .changes(changes)
                .trackerIssueKey(issueKey)
                .campaign(QuotaChangeRequest.Campaign.from(activeCampaign))
                .campaignType(activeCampaign.getType())
                .cost(0)
                .requestOwningCost(0L)
                .build();

        quotaChangeRequest = quotaChangeRequestDao.create(quotaChangeRequest);

        updateHierarchy();

        final Collection<? extends QuotaChangeRequestManager.MoveResult> results = createAuthorizedLocalClient(AMOSOV_F)
                .path("/admin/requests/_moveToProject")
                .postAndGetCollection("{\"fromProject\": 1001,\"toProject\": 1002}", QuotaChangeRequestManager.MoveResult.class);

        assertEquals(1, results.size());
        final QuotaChangeRequestManager.MoveResult result = new ArrayList<>(results).get(0);

        assertEquals(issueKey, result.getTicketId());
        assertEquals("OK", result.getResult());

        assertEquals(otherProject, quotaChangeRequestDao.read(quotaChangeRequest.getId()).getProject());

        assertTrue(trackerManager.getIssue(issueKey).getAssignee().isMatch(user -> user.getLogin().equals("lotrek")));

        final List<CommentCreate> comments = trackerManager.getIssueComments(issueKey);
        assertFalse(comments.isEmpty());

        final CommentCreate comment = comments.get(comments.size() - 1);
        assertFalse(comment.getSummonees().containsTs(LOTREK.getLogin()));
    }

    /**
     * 1) Создаем новую заявку
     * 2) В заявке меняем описание
     * 3) Проверяем, что изменения не проросли в тикет
     * 4) Вызываем тестируемую функцию {@link ru.yandex.qe.dispenser.ws.admin.RequestAdminService#refresh(String, boolean)}
     * 5) Проверяем, что теперь изменения проросли в тикет и появился комментарий с призывом ответственного
     */
    @Test
    public void testRefreshRequests() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(200, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        assertEquals(1, requests.size());
        final DiQuotaChangeRequest request = requests.getFirst();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(GROWTH_ANSWER)
                        .chartLinksAbsenceExplanation("explanation")
                        .build())
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(AMOSOV_F);

        final String trackerIssueKey = request.getTrackerIssueKey();
        assertNotNull(trackerIssueKey);

        final String fieldName = "description";

        final String issueDescription = (String) trackerManager.getIssueFields(trackerIssueKey).get(fieldName);
        final int issueCommentsSize = trackerManager.getIssueComments(trackerIssueKey).size();

        final String newDescription = "Test comment";

        final QuotaChangeRequest confirmedRequest = quotaChangeRequestDao.read(request.getId());
        final boolean quotaUpdate = quotaChangeRequestDao.update(confirmedRequest.copyBuilder()
                .description(newDescription)
                .build());

        assertTrue(quotaUpdate);

        final String issueOutdatedDescription = (String) trackerManager.getIssueFields(trackerIssueKey).get(fieldName);
        assertEquals(issueDescription, issueOutdatedDescription);

        final JsonNode results = createAuthorizedLocalClient(AMOSOV_F)
                .path("/admin/requests/_refresh/" + trackerIssueKey)
                .query("summonAssignee", Boolean.TRUE)
                .post(null, JsonNode.class);

        assertTrue(results.get("success").booleanValue());

        final String issueUpdatedDescription = (String) trackerManager.getIssueFields(trackerIssueKey).get(fieldName);
        assertNotEquals(issueDescription, issueUpdatedDescription);
        assertTrue(issueUpdatedDescription.contains(newDescription));

        final List<CommentCreate> issueCommentsUpdated = trackerManager.getIssueComments(trackerIssueKey);
        assertEquals(issueCommentsSize + 1, issueCommentsUpdated.size());
        assertFalse(issueCommentsUpdated.get(issueCommentsUpdated.size() - 1).getSummonees().isEmpty());
    }

    @Test
    public void quotaChangeRequestTicketDescriptionShouldContainAdditionalProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("segment", "default");

        prepareCampaignResources();

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .additionalProperties(properties)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdRequest = requests.getFirst();

        assertNotNull(createdRequest.getTrackerIssueKey());

        final Map<String, Object> fields = trackerManager.getIssueFields(createdRequest.getTrackerIssueKey());
        final String description = (String) fields.get("description");

        assertTrue(description.contains("|| **segment** | default ||"));

        properties.put("segment", "dev");

        final DiQuotaChangeRequest updatedRequest = dispenser().quotaChangeRequests()
                .byId(createdRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .additionalProperties(properties)
                        .build())
                .performBy(AMOSOV_F);

        assertNotNull(updatedRequest.getTrackerIssueKey());

        final Map<String, Object> updatedFields = trackerManager.getIssueFields(updatedRequest.getTrackerIssueKey());
        final String updatedDescription = (String) updatedFields.get("description");

        assertTrue(updatedDescription.contains("|| **segment** | dev ||"));
    }

    @Test
    public void quotaChangeRequestNewIssueNoSummon() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("segment", "default");

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .additionalProperties(properties)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdRequest = requests.getFirst();

        assertNotNull(createdRequest.getTrackerIssueKey());

        final List<CommentCreate> comments = trackerManager.getIssueComments(createdRequest.getTrackerIssueKey());
        assertTrue(comments.isEmpty());
    }

    @Test
    public void quotaChangeRequestBackToNewNoSummon() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("segment", "default");

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .additionalProperties(properties)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdRequest = requests.getFirst();

        assertNotNull(createdRequest.getTrackerIssueKey());

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .setStatus(DiQuotaChangeRequest.Status.CANCELLED).performBy(AMOSOV_F);

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .setStatus(DiQuotaChangeRequest.Status.NEW).performBy(AMOSOV_F);

        final List<CommentCreate> comments = trackerManager.getIssueComments(createdRequest.getTrackerIssueKey());
        assertTrue(comments.stream().allMatch(c -> c.getSummonees().stream().noneMatch(s -> s.equals(WHISTLER.getLogin()))));
    }

    @Test
    public void quotaChangeRequestUpdateCancelledNoSummon() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("segment", "default");

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .additionalProperties(properties)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdRequest = requests.getFirst();

        assertNotNull(createdRequest.getTrackerIssueKey());

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .setStatus(DiQuotaChangeRequest.Status.CANCELLED).performBy(AMOSOV_F);

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().description("https://yandex.ru").build())
                .performBy(AMOSOV_F);

        final List<CommentCreate> comments = trackerManager.getIssueComments(createdRequest.getTrackerIssueKey());
        assertTrue(comments.stream().allMatch(c -> c.getSummonees().stream().noneMatch(s -> s.equals(WHISTLER.getLogin()))));
    }

    @Test
    public void quotaChangeRequestUpdateRejectedNoSummon() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("segment", "default");

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .additionalProperties(properties)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdRequest = requests.getFirst();

        quotaChangeRequestDao.update(quotaChangeRequestDao.read(createdRequest.getId()).copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());

        assertNotNull(createdRequest.getTrackerIssueKey());

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .setStatus(DiQuotaChangeRequest.Status.REJECTED).performBy(KEYD);

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().description("https://yandex.ru").build())
                .performBy(AMOSOV_F);

        final List<CommentCreate> comments = trackerManager.getIssueComments(createdRequest.getTrackerIssueKey());
        assertTrue(comments.stream().allMatch(c -> c.getSummonees().stream().noneMatch(s -> s.equals(WHISTLER.getLogin()))));
    }

    @Test
    public void quotaChangeRequestUpdateConfirmedHasSummon() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("segment", "default");

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .additionalProperties(properties)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdRequest = requests.getFirst();

        assertNotNull(createdRequest.getTrackerIssueKey());

        quotaChangeRequestDao.update(
                quotaChangeRequestDao.read(createdRequest.getId()).copyBuilder().status(QuotaChangeRequest.Status.READY_FOR_REVIEW).build());

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW).performBy(WHISTLER);
        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED).performBy(WHISTLER);

        final Map<String, String> properties2 = new HashMap<>();
        properties2.put("segment", "dev");

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().additionalProperties(properties2).build())
                .performBy(AMOSOV_F);

        final List<CommentCreate> comments = trackerManager.getIssueComments(createdRequest.getTrackerIssueKey());
        assertTrue(comments.stream().anyMatch(c -> c.getSummonees().stream().anyMatch(s -> s.equals(WHISTLER.getLogin()))));
    }

    @Test
    public void quotaChangeRequestUpdateConfirmedHasSummonSuppressed() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("segment", "default");

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .additionalProperties(properties)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdRequest = requests.getFirst();

        assertNotNull(createdRequest.getTrackerIssueKey());

        quotaChangeRequestDao.update(
                quotaChangeRequestDao.read(createdRequest.getId()).copyBuilder().status(QuotaChangeRequest.Status.READY_FOR_REVIEW).build());

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, true).performBy(WHISTLER);
        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED, true).performBy(WHISTLER);

        final Map<String, String> properties2 = new HashMap<>();
        properties2.put("segment", "dev");

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().additionalProperties(properties2).build(), true)
                .performBy(AMOSOV_F);

        final List<CommentCreate> comments = trackerManager.getIssueComments(createdRequest.getTrackerIssueKey());
        assertTrue(comments.stream().allMatch(c -> c.getSummonees().isEmpty()));
    }

    @Test
    public void quotaChangeRequestUpdateNewNoSummon() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("segment", "default");

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .additionalProperties(properties)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdRequest = requests.getFirst();

        assertNotNull(createdRequest.getTrackerIssueKey());

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().chartLink("https://yandex.ru").build())
                .performBy(AMOSOV_F);

        final List<CommentCreate> comments = trackerManager.getIssueComments(createdRequest.getTrackerIssueKey());
        assertTrue(comments.stream().allMatch(c -> c.getSummonees().stream().noneMatch(s -> s.equals(WHISTLER.getLogin()))));
    }

    @Test
    public void quotaChangeRequestUpdateDefenceFieldsNotChangingStatus() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("segment", "default");

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .additionalProperties(properties)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdRequest = requests.getFirst();

        assertNotNull(createdRequest.getTrackerIssueKey());

        quotaChangeRequestDao.update(
                quotaChangeRequestDao.read(createdRequest.getId()).copyBuilder().status(QuotaChangeRequest.Status.READY_FOR_REVIEW).build());

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED).performBy(WHISTLER);

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                        .goalId(newGoal.getId())
                        .description("New Description for This task")
                        .calculations("256 * 1024")
                        .chartLinksAbsenceExplanation(" i don't know")
                        .requestGoalAnswers(ImmutableMap.of(0L, "True",
                                1L, "False",
                                2L, "Exactly"))
                        .comment("Really important comment")
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest quotaChangeRequest = dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.APPROVED, quotaChangeRequest.getStatus());
    }

    @Test
    public void refreshTicketActionWillMoveStatusIfIssueStatusIsNotCorrect() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdRequest = requests.getFirst();

        quotaChangeRequestDao.update(
                quotaChangeRequestDao.read(createdRequest.getId()).copyBuilder().status(QuotaChangeRequest.Status.READY_FOR_REVIEW).build());

        assertNotNull(createdRequest.getTrackerIssueKey());
        assertEquals("open", trackerManager.getIssue(createdRequest.getTrackerIssueKey()).getStatus().getKey());

        trackerManager.executeTransition(createdRequest.getTrackerIssueKey(), "closed", IssueUpdate.builder().build());
        assertEquals("closed", trackerManager.getIssue(createdRequest.getTrackerIssueKey()).getStatus().getKey());

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .description("foo 42")
                        .calculations("2 + 2 = 5")
                        .build())
                .performBy(WHISTLER);

        assertEquals("readyToReview", trackerManager.getIssue(createdRequest.getTrackerIssueKey()).getStatus().getKey());

        trackerManager.executeTransition(createdRequest.getTrackerIssueKey(), "closed", IssueUpdate.builder().build());
        assertEquals("closed", trackerManager.getIssue(createdRequest.getTrackerIssueKey()).getStatus().getKey());

        dispenser().quotaChangeRequests().byId(createdRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .description("foo 24")
                        .calculations("2 + 2 = 4")
                        .build())
                .performBy(WHISTLER);

        assertEquals("readyToReview", trackerManager.getIssue(createdRequest.getTrackerIssueKey()).getStatus().getKey());

        trackerManager.executeTransition(createdRequest.getTrackerIssueKey(), "closed", IssueUpdate.builder().build());
        assertEquals("closed", trackerManager.getIssue(createdRequest.getTrackerIssueKey()).getStatus().getKey());

        final Map<?, ?> result = createAuthorizedLocalClient(AMOSOV_F)
                .path("/admin/requests/_refresh/" + createdRequest.getTrackerIssueKey())
                .post(null, Map.class);

        assertEquals(ImmutableMap.of("success", true), result);
        assertEquals("readyToReview", trackerManager.getIssue(createdRequest.getTrackerIssueKey()).getStatus().getKey());
    }

    @Test
    public void quotaChangeRequestTicketTagsShouldContainImportantFlag() {
        prepareCampaignResources();

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdNotImportantRequest = requests.getFirst();

        assertNotNull(createdNotImportantRequest.getTrackerIssueKey());

        Map<String, Object> fields = trackerManager.getIssueFields(createdNotImportantRequest.getTrackerIssueKey());
        String[] tags = (String[]) fields.get("tags");

        assertFalse(Arrays.asList(tags).contains(CRITICAL_TAG));

        requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdImportantRequest = requests.getFirst();

        assertNotNull(createdImportantRequest.getTrackerIssueKey());

        fields = trackerManager.getIssueFields(createdImportantRequest.getTrackerIssueKey());
        tags = (String[]) fields.get("tags");

        assertTrue(Arrays.asList(tags).contains(CRITICAL_TAG));

        final DiQuotaChangeRequest updatedCreatedNotImportantRequest = dispenser().quotaChangeRequests()
                .byId(createdNotImportantRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .importantRequest(true)
                        .build())
                .performBy(AMOSOV_F);

        assertNotNull(updatedCreatedNotImportantRequest.getTrackerIssueKey());

        fields = trackerManager.getIssueFields(updatedCreatedNotImportantRequest.getTrackerIssueKey());
        Object[] tagsO = (Object[]) fields.get("tags");

        assertTrue(Arrays.asList(tagsO).contains(CRITICAL_TAG));

        final DiQuotaChangeRequest updatedCreatedImportantRequest = dispenser().quotaChangeRequests()
                .byId(createdImportantRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .importantRequest(false)
                        .build())
                .performBy(AMOSOV_F);

        assertNotNull(updatedCreatedImportantRequest.getTrackerIssueKey());

        fields = trackerManager.getIssueFields(updatedCreatedImportantRequest.getTrackerIssueKey());
        tagsO = (Object[]) fields.get("tags");

        assertFalse(Arrays.asList(tagsO).contains(CRITICAL_TAG));
    }

    @Test
    public void quotaChangeRequestTicketNameMustUpdatedTest() {
        prepareCampaignResources();

        String oldName = "[test-campaign] test (Test Project)";
        String newName = "some new name";
        String newSummaryExpected = "[test-campaign] " + newName + " (Test Project)";

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdNotImportantRequest = requests.getFirst();

        assertNotNull(createdNotImportantRequest.getTrackerIssueKey());

        Map<String, Object> fields = trackerManager.getIssueFields(createdNotImportantRequest.getTrackerIssueKey());

        String summary = (String) fields.get("summary");
        Assertions.assertEquals(oldName, summary);

        final DiQuotaChangeRequest updatedCreatedNotImportantRequest = dispenser().quotaChangeRequests()
                .byId(createdNotImportantRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .summary(newName)
                        .build())
                .performBy(AMOSOV_F);

        assertNotNull(updatedCreatedNotImportantRequest.getTrackerIssueKey());

        fields = trackerManager.getIssueFields(updatedCreatedNotImportantRequest.getTrackerIssueKey());
        String newSummary = (String) fields.get("summary");
        Assertions.assertEquals(newSummaryExpected, newSummary);
    }

    @Test
    public void quotaChangeRequestTicketNameMustUpdatedOnCriticalFlagUpdateTest() {
        prepareCampaignResources();

        String nonCritical = "[test-campaign] test (Test Project)";
        String critical = "[test-campaign][" + CRITICAL_TAG + "] test (Test Project)";

        Project yandex = projectDao.read(YANDEX);
        String test_project = "Test Project";
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name(test_project).parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdNotImportantRequest = requests.getFirst();

        assertNotNull(createdNotImportantRequest.getTrackerIssueKey());

        Map<String, Object> fields = trackerManager.getIssueFields(createdNotImportantRequest.getTrackerIssueKey());

        String summary = (String) fields.get("summary");
        Assertions.assertEquals(nonCritical, summary);

        final DiQuotaChangeRequest updatedCreatedNotImportantRequest = dispenser().quotaChangeRequests()
                .byId(createdNotImportantRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .importantRequest(true)
                        .build())
                .performBy(AMOSOV_F);

        assertNotNull(updatedCreatedNotImportantRequest.getTrackerIssueKey());

        fields = trackerManager.getIssueFields(updatedCreatedNotImportantRequest.getTrackerIssueKey());
        String newSummary = (String) fields.get("summary");
        Assertions.assertEquals(critical, newSummary);


        DiListResponse<DiQuotaChangeRequest> requests2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdImportantRequest = requests.getFirst();

        assertNotNull(createdImportantRequest.getTrackerIssueKey());

        fields = trackerManager.getIssueFields(createdImportantRequest.getTrackerIssueKey());

        summary = (String) fields.get("summary");
        Assertions.assertEquals(critical, summary);

        final DiQuotaChangeRequest updatedCreatedImportantRequest = dispenser().quotaChangeRequests()
                .byId(createdImportantRequest.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .importantRequest(false)
                        .build())
                .performBy(AMOSOV_F);

        assertNotNull(updatedCreatedImportantRequest.getTrackerIssueKey());

        fields = trackerManager.getIssueFields(updatedCreatedImportantRequest.getTrackerIssueKey());
        newSummary = (String) fields.get("summary");
        Assertions.assertEquals(nonCritical, newSummary);
    }
}
