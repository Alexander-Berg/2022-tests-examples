package ru.yandex.qe.dispenser.ws.logic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.GenericType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiOrder;
import ru.yandex.qe.dispenser.api.v1.DiPersonGroup;
import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiQuotaRequestHistoryEvent;
import ru.yandex.qe.dispenser.api.v1.DiQuotaRequestHistoryEventType;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiSetAmountResult;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.api.v1.response.DiListRelativePageResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.history.request.QuotaChangeRequestHistoryDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.hierarchy.HierarchySupplier;
import ru.yandex.qe.dispenser.solomon.SolomonHolder;
import ru.yandex.qe.dispenser.standalone.MockTrackerManager;
import ru.yandex.qe.dispenser.ws.admin.RequestAdminService;
import ru.yandex.qe.dispenser.ws.quota.request.QuotaChangeRequestManager;
import ru.yandex.qe.dispenser.ws.quota.request.ReCreateRequestTicketsTask;
import ru.yandex.qe.dispenser.ws.quota.request.SetResourceAmountBody;
import ru.yandex.qe.dispenser.ws.quota.request.ticket.QuotaChangeRequestTicketManager;
import ru.yandex.qe.dispenser.ws.reqbody.SetResourceAmountBodyOptional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;
import static ru.yandex.qe.dispenser.ws.param.RelativePageParam.FROM_ID;
import static ru.yandex.qe.dispenser.ws.param.RelativePageParam.PAGE_ORDER;
import static ru.yandex.qe.dispenser.ws.param.RelativePageParam.PAGE_SIZE;

public class QuotaChangeRequestHistoryTest extends BaseQuotaRequestTest {

    public static final GenericType<DiListRelativePageResponse<DiQuotaRequestHistoryEvent>> RESPONSE_TYPE = new GenericType<DiListRelativePageResponse<DiQuotaRequestHistoryEvent>>() {
    };
    public static final String PROVIDER = "sqs";

    private Project project;

    private Campaign campaign;

    @Autowired
    private MockTrackerManager trackerManager;

    @Autowired
    private QuotaChangeRequestDao requestDao;

    @Autowired
    private QuotaChangeRequestTicketManager ticketManager;

    @Autowired
    private QuotaChangeRequestManager quotaChangeRequestManager;

    @Autowired
    private SolomonHolder solomonHolder;

    @Autowired
    private QuotaChangeRequestHistoryDao quotaChangeRequestHistoryDao;

    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;

    @Autowired
    private HierarchySupplier hierarchySupplier;
    private Service nirvana;
    private Campaign aggregatedCampaign;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        quotaChangeRequestHistoryDao.clear();
        campaign = createDefaultCampaign();
        aggregatedCampaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("test-aggregated")
                .setName("test-aggregated")
                .setType(Campaign.Type.AGGREGATED).build());

        final String key = dispenser().projects()
                .create(DiProject.withKey("test")
                        .withParentProject(YANDEX)
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .withName("test")
                        .withDescription("test")
                        .withResponsibles(DiPersonGroup.builder().addPersons(LOTREK.getLogin()).build())
                        .withMembers(DiPersonGroup.builder().addPersons(LOTREK.getLogin(), QDEEE.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F).getKey();
        updateHierarchy();
        project = Hierarchy.get().getProjectReader().read(key);

        nirvana = serviceDao.read(NIRVANA);

        final Service provider = serviceDao.create(Service.withKey(PROVIDER)
                .withName("Sqs")
                .withAbcServiceId(nirvana.getAbcServiceId())
                .withSettings(Service.Settings.builder()
                        .accountActualValuesInQuotaDistribution(nirvana.getSettings().accountActualValuesInQuotaDistribution())
                        .usesProjectHierarchy(nirvana.getSettings().usesProjectHierarchy())
                        .manualQuotaAllocation(nirvana.getSettings().isManualQuotaAllocation())
                        .build())
                .build());

        updateHierarchy();

        final Resource storage = resourceDao.create(new Resource.Builder(STORAGE, provider)
                .name("Storage")
                .type(DiResourceType.STORAGE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        updateHierarchy();

    }

    private DiQuotaChangeRequest createResourcePreorderRequestWithGoal() {
        return dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .goalId(TEST_GOAL_ID)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                        .changes(PROVIDER, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F)
                .getFirst();
    }

    private DiQuotaChangeRequest createResourcePreorderRequest() {
        return createResourcePreorderRequest(campaign);
    }

    private DiQuotaChangeRequest createResourcePreorderRequest(Campaign campaign) {
        return createResourcePreorderRequest(AMOSOV_F, campaign);
    }

    private DiQuotaChangeRequest createResourcePreorderRequest(final DiPerson author) {
        return createResourcePreorderRequest(author, campaign);
    }

    private DiQuotaChangeRequest createResourcePreorderRequest(final DiPerson author, Campaign campaign) {
        return dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(PROVIDER, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .chartLinksAbsenceExplanation("explanation")
                        .build(), campaign.getId())
                .performBy(author)
                .getFirst();
    }

    private DiQuotaChangeRequest updateRequest(final long reqId, BodyUpdate bodyUpdate, final DiPerson person) {
        return createAuthorizedLocalClient(person)
                .path("/v1/quota-requests/" + reqId)
                .invoke(HttpMethod.PATCH, bodyUpdate, DiQuotaChangeRequest.class);
    }

    private void answerGrowthRequestGoalQuestions(final long id) {
        dispenser().quotaChangeRequests()
                .byId(id)
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(ImmutableMap.of(
                                4L, "fine",
                                5L, "too",
                                6L, "ok"
                        )).build())
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .byId(id)
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(AMOSOV_F);
    }

    private DiListRelativePageResponse<DiQuotaRequestHistoryEvent> historyByRequest(final long reqId) {
        return createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/history/quota-requests/" + reqId)
                .query(PAGE_ORDER, DiOrder.ASC)
                .get(RESPONSE_TYPE);
    }

    private DiListRelativePageResponse<DiQuotaRequestHistoryEvent> historyByRequest(final long reqId, final Map<String, Object> params) {
        final WebClient client = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/history/quota-requests/" + reqId);
        params.forEach(client::query);
        return client
                .get(RESPONSE_TYPE);
    }

    private DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history(final Map<String, Object> params) {
        final WebClient client = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/history/quota-requests/");
        params.forEach(client::query);
        return client
                .get(RESPONSE_TYPE);
    }


    private DiQuotaChangeRequest updateStatus(final long reqId, final DiQuotaChangeRequest.Status status, final DiPerson performer) {
        return createAuthorizedLocalClient(performer)
                .path("/v1/quota-requests/" + reqId + "/status/" + status)
                .put(null, DiQuotaChangeRequest.class);
    }

    private static List<DiQuotaRequestHistoryEvent> asList(final DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history) {
        return new ArrayList<>(history.getResults());
    }

    private static void assertHistoryEqualsByTypesAsc(final List<DiQuotaRequestHistoryEvent> history,
                                                      final List<DiQuotaRequestHistoryEventType> eventTypes) {
        Instant prev = Instant.ofEpochMilli(0);
        for (final DiQuotaRequestHistoryEvent event : history) {
            final Instant updated = Instant.parse(event.getUpdated());
            assertFalse(prev.isAfter(updated));
            prev = updated;
        }

        assertEquals(eventTypes, history.stream().map(DiQuotaRequestHistoryEvent::getType).collect(Collectors.toList()));
    }

    private static void assertThatOnlyOneFieldIsChanged(final DiQuotaRequestHistoryEvent event, final String field) {
        assertEquals(1, event.getOldData().size());
        assertTrue(event.getOldData().containsKey(field));

        assertEquals(1, event.getNewData().size());
        assertTrue(event.getNewData().containsKey(field));
    }

    private static void assertThatOnlyOneFieldIsChanged(final DiQuotaRequestHistoryEvent event, final String field, final Object was, final Object now) {
        assertEquals(1, event.getOldData().size());
        assertEquals(event.getOldData().get(field), was);

        assertEquals(1, event.getNewData().size());
        assertEquals(event.getNewData().get(field), now);
    }

    @Test
    public void resourcePreorderRequestCreationWillBeAddedToHistory() {
        prepareCampaignResources();
        final DiQuotaChangeRequest req = createResourcePreorderRequest();

        final DiListRelativePageResponse<DiQuotaRequestHistoryEvent> historyEvents = historyByRequest(req.getId());

        assertEquals(2, historyEvents.size());
        final List<DiQuotaRequestHistoryEvent> history = historyEvents.stream().collect(Collectors.toList());
        assertEquals(DiQuotaRequestHistoryEventType.CREATE, history.get(0).getType());
        final DiQuotaRequestHistoryEvent second = history.get(1);
        assertEquals(DiQuotaRequestHistoryEventType.ISSUE_KEY_UPDATE, second.getType());
        assertTrue(second.getOldData().containsKey(QuotaChangeRequest.Field.ISSUE_KEY.getFieldName()));
        assertTrue(second.getNewData().containsKey(QuotaChangeRequest.Field.ISSUE_KEY.getFieldName()));
    }

    @Test
    public void statusUpdateWillBeRecorded() {
        prepareCampaignResources();
        final DiQuotaChangeRequest req = createResourcePreorderRequest();
        updateStatus(req.getId(), DiQuotaChangeRequest.Status.CANCELLED, AMOSOV_F);
        final DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = historyByRequest(req.getId());
        assertEquals(3, history.size());
        final List<DiQuotaRequestHistoryEvent> eventList = asList(history);
        final DiQuotaRequestHistoryEvent third = eventList.get(2);
        assertEquals(DiQuotaRequestHistoryEventType.STATUS_UPDATE, third.getType());
        assertThatOnlyOneFieldIsChanged(third, "status", "NEW", "CANCELLED");
        assertEquals(third.getPerson(), AMOSOV_F.getLogin());
    }

    @Test
    public void requestUpdateWillBeRecorded() {
        prepareCampaignResources();
        final DiQuotaChangeRequest req = createResourcePreorderRequest();
        final String placeholder = "foo";
        final BodyUpdate update = new BodyUpdate.BodyUpdateBuilder()
                .calculations(placeholder)
                .comment(placeholder)
                .additionalProperties(ImmutableMap.of("account", placeholder))
                .changes(PROVIDER, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(9L, DiUnit.GIBIBYTE))
                .build();
        updateRequest(req.getId(), update, AMOSOV_F);
        final DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = historyByRequest(req.getId());
        assertEquals(4, history.size());
        final List<DiQuotaRequestHistoryEvent> eventList = asList(history);

        final DiQuotaRequestHistoryEvent changesUpdate = eventList.get(2);

        assertEquals(DiQuotaRequestHistoryEventType.CHANGES_UPDATE, changesUpdate.getType());
        assertThatOnlyOneFieldIsChanged(changesUpdate, "changes");

        final DiQuotaRequestHistoryEvent fieldsUpdateEvent = eventList.get(3);

        assertEquals(DiQuotaRequestHistoryEventType.FIELDS_UPDATE, fieldsUpdateEvent.getType());
        assertEquals(3, fieldsUpdateEvent.getOldData().size());
        assertEquals(3, fieldsUpdateEvent.getNewData().size());

        assertFalse(Instant.parse(fieldsUpdateEvent.getUpdated()).isBefore(Instant.parse(changesUpdate.getUpdated())));
    }

    @Test
    public void segmentChangesAdditionWillBeRecorded() {
        prepareCampaignResources();
        final DiQuotaChangeRequest req = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of("dc_1", "ss_1"), DiAmount.of(10L, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F)
                .getFirst();
        final BodyUpdate update = new BodyUpdate.BodyUpdateBuilder()
                .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of("dc_1", "ss_1"), DiAmount.of(10L, DiUnit.PERMILLE_CORES))
                .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of("dc_2", "ss_1"), DiAmount.of(20L, DiUnit.PERMILLE_CORES))
                .build();
        updateRequest(req.getId(), update, AMOSOV_F);
        final DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = historyByRequest(req.getId());
        assertEquals(3, history.size());
        final List<DiQuotaRequestHistoryEvent> eventList = asList(history);

        final DiQuotaRequestHistoryEvent changesUpdate = eventList.get(2);

        assertEquals(DiQuotaRequestHistoryEventType.CHANGES_UPDATE, changesUpdate.getType());
        assertThatOnlyOneFieldIsChanged(changesUpdate, "changes");
        assertEquals(1, ((List<?>) changesUpdate.getOldData().get("changes")).size());
        assertEquals(2, ((List<?>) changesUpdate.getNewData().get("changes")).size());
    }

    @Test
    public void segmentChangesRemovalWillBeRecorded() {
        prepareCampaignResources();
        final DiQuotaChangeRequest req = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of("dc_1", "ss_1"), DiAmount.of(10L, DiUnit.PERMILLE_CORES))
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of("dc_2", "ss_1"), DiAmount.of(20L, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F)
                .getFirst();
        final BodyUpdate update = new BodyUpdate.BodyUpdateBuilder()
                .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of("dc_1", "ss_1"), DiAmount.of(10L, DiUnit.PERMILLE_CORES))
                .build();
        updateRequest(req.getId(), update, AMOSOV_F);
        final DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = historyByRequest(req.getId());
        assertEquals(3, history.size());
        final List<DiQuotaRequestHistoryEvent> eventList = asList(history);

        final DiQuotaRequestHistoryEvent changesUpdate = eventList.get(2);

        assertEquals(DiQuotaRequestHistoryEventType.CHANGES_UPDATE, changesUpdate.getType());
        assertThatOnlyOneFieldIsChanged(changesUpdate, "changes");
        assertEquals(2, ((List<?>) changesUpdate.getOldData().get("changes")).size());
        assertEquals(1, ((List<?>) changesUpdate.getNewData().get("changes")).size());
    }

    @Test
    public void requestProjectChangeWillBeRecorded() {
        prepareCampaignResources();
        final DiQuotaChangeRequest req = createResourcePreorderRequest();

        final String key = dispenser().projects()
                .create(DiProject.withKey("test42")
                        .withParentProject(YANDEX)
                        .withAbcServiceId(1234)
                        .withName("test2")
                        .withDescription("test2")
                        .withResponsibles(DiPersonGroup.builder().addPersons(LOTREK.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F).getKey();
        updateHierarchy();

        final Project test2 = Hierarchy.get().getProjectReader().read(key);

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/admin/requests/_moveToProject/")
                .post(new RequestAdminService.MoveRequestsToProjectParams(project.getAbcServiceId(), null, test2.getAbcServiceId()));

        final DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = historyByRequest(req.getId());
        assertEquals(3, history.size());
        final DiQuotaRequestHistoryEvent projectUpdate = asList(history).get(2);
        assertEquals(DiQuotaRequestHistoryEventType.PROJECT_UPDATE, projectUpdate.getType());
        assertThatOnlyOneFieldIsChanged(projectUpdate, "project");
    }

    @Test
    public void batchStatusUpdateWillBeRecorded() {
        prepareCampaignResources();
        final DiQuotaChangeRequest firstRequest = createResourcePreorderRequest(aggregatedCampaign);
        answerGrowthRequestGoalQuestions(firstRequest.getId());
        updateStatus(firstRequest.getId(), DiQuotaChangeRequest.Status.APPROVED, WHISTLER);
        updateStatus(firstRequest.getId(), DiQuotaChangeRequest.Status.CONFIRMED, KEYD);

        final DiQuotaChangeRequest secondRequest = createResourcePreorderRequest(aggregatedCampaign);
        answerGrowthRequestGoalQuestions(secondRequest.getId());
        updateStatus(secondRequest.getId(), DiQuotaChangeRequest.Status.APPROVED, WHISTLER);
        updateStatus(secondRequest.getId(), DiQuotaChangeRequest.Status.CONFIRMED, KEYD);

        final SetResourceAmountBodyOptional setResourceAmountBody = new SetResourceAmountBodyOptional(
                Arrays.asList(
                        new SetResourceAmountBodyOptional.Item(firstRequest.getId(), null, null, ""
                        ),
                        new SetResourceAmountBodyOptional.Item(secondRequest.getId(), null, null, ""
                        )
                ),
                SetResourceAmountBodyOptional.UpdateFor.BOTH
        );
        final DiSetAmountResult result = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, setResourceAmountBody, DiSetAmountResult.class);

        DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = historyByRequest(firstRequest.getId());
        assertEquals(10, history.size());

        DiQuotaRequestHistoryEvent batch = asList(history).get(9);

        assertEquals(DiQuotaRequestHistoryEventType.BATCH_STATUS_UPDATE, batch.getType());
        assertEquals(batch.getPerson(), AMOSOV_F.getLogin());

        assertThatOnlyOneFieldIsChanged(batch, "status", "CONFIRMED", "COMPLETED");

        history = historyByRequest(secondRequest.getId());
        assertEquals(10, history.size());

        batch = asList(history).get(9);

        assertEquals(DiQuotaRequestHistoryEventType.BATCH_STATUS_UPDATE, batch.getType());

        assertThatOnlyOneFieldIsChanged(batch, "status", "CONFIRMED", "COMPLETED");
    }

    @Test
    public void changesUpdateForCancelledRequestWillBeRecordedWithStatusUpdate() {
        prepareCampaignResources();
        final DiQuotaChangeRequest request = createResourcePreorderRequest();

        updateStatus(request.getId(), DiQuotaChangeRequest.Status.CANCELLED, AMOSOV_F);

        final BodyUpdate update = new BodyUpdate.BodyUpdateBuilder()
                .changes(NIRVANA, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(9L, DiUnit.GIBIBYTE))
                .build();
        updateRequest(request.getId(), update, AMOSOV_F);

        final DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = historyByRequest(request.getId());

        assertEquals(5, history.size());
        final List<DiQuotaRequestHistoryEvent> historyEventList = asList(history);
        assertHistoryEqualsByTypesAsc(historyEventList, Arrays.asList(
                DiQuotaRequestHistoryEventType.CREATE,
                DiQuotaRequestHistoryEventType.ISSUE_KEY_UPDATE,
                DiQuotaRequestHistoryEventType.STATUS_UPDATE,
                DiQuotaRequestHistoryEventType.CHANGES_UPDATE,
                DiQuotaRequestHistoryEventType.STATUS_UPDATE
        ));
        final DiQuotaRequestHistoryEvent statusChange = historyEventList.get(4);

        assertThatOnlyOneFieldIsChanged(statusChange, "status", "CANCELLED", "NEW");
    }

    @Test
    public void issueCreationThroughApiWillBeRecorded() {
        prepareCampaignResources();
        trackerManager.setTrackerAvailable(false);
        final DiQuotaChangeRequest request;
        DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history;
        try {
            request = createResourcePreorderRequest();

            history = historyByRequest(request.getId());
            assertEquals(1, history.size());
        } finally {
            trackerManager.setTrackerAvailable(true);
        }
        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + request.getId() + "/create-ticket")
                .post(null, DiQuotaChangeRequest.class);

        history = historyByRequest(request.getId());
        assertEquals(2, history.size());

        final DiQuotaRequestHistoryEvent second = asList(history).get(1);

        assertThatOnlyOneFieldIsChanged(second, "trackerIssueKey");
    }

    @Test
    public void issueCreationThroughTaskWillBeRecorded() {
        prepareCampaignResources();
        trackerManager.setTrackerAvailable(false);
        final DiQuotaChangeRequest request;
        DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history;
        try {
            request = createResourcePreorderRequest();

            history = historyByRequest(request.getId());
            assertEquals(1, history.size());
        } finally {
            trackerManager.setTrackerAvailable(true);
        }
        final ReCreateRequestTicketsTask task = new ReCreateRequestTicketsTask(requestDao, ticketManager, solomonHolder, quotaChangeRequestManager, hierarchySupplier, ROBOT_DISPENSER);

        task.createTickets();

        history = historyByRequest(request.getId());
        assertEquals(2, history.size());

        final DiQuotaRequestHistoryEvent second = asList(history).get(1);

        assertThatOnlyOneFieldIsChanged(second, "trackerIssueKey");
    }

    @Test
    public void historyCanBeFetchedWithFilters() {
        prepareCampaignResources();
        final DiQuotaChangeRequest first = createResourcePreorderRequest(AMOSOV_F);
        final DiQuotaChangeRequest second = createResourcePreorderRequest(WHISTLER);

        DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = history(ImmutableMap.of(PAGE_ORDER, "ASC"));

        final List<DiQuotaRequestHistoryEvent> eventList = asList(history);
        assertHistoryEqualsByTypesAsc(eventList, Arrays.asList(
                DiQuotaRequestHistoryEventType.CREATE,
                DiQuotaRequestHistoryEventType.ISSUE_KEY_UPDATE,
                DiQuotaRequestHistoryEventType.CREATE,
                DiQuotaRequestHistoryEventType.ISSUE_KEY_UPDATE
        ));

        final DiQuotaRequestHistoryEvent secondEvent = eventList.get(1);
        final DiQuotaRequestHistoryEvent thirdEvent = eventList.get(2);

        final Map<String, Object> params = ImmutableMap.<String, Object>builder()
                .put(PAGE_ORDER, "ASC")
                .put("status", "NEW")
                .put("service", PROVIDER)
                .put("campaign", campaign.getId())
                .put("abcServiceId", project.getAbcServiceId())
                .put("order", bigOrderOne.getId())
                .build();
        history = history(params);

        assertEquals(4, history.size());

        history = history(
                ImmutableMap.<String, Object>builder()
                        .putAll(params)
                        .put("eventType", "CREATE")
                        .build()
        );

        assertHistoryEqualsByTypesAsc(asList(history), Arrays.asList(
                DiQuotaRequestHistoryEventType.CREATE,
                DiQuotaRequestHistoryEventType.CREATE
        ));

        history = history(
                ImmutableMap.<String, Object>builder()
                        .putAll(params)
                        .put("eventType", "ISSUE_KEY_UPDATE")
                        .build()
        );

        assertHistoryEqualsByTypesAsc(asList(history), Arrays.asList(
                DiQuotaRequestHistoryEventType.ISSUE_KEY_UPDATE,
                DiQuotaRequestHistoryEventType.ISSUE_KEY_UPDATE
        ));

        history = history(
                ImmutableMap.<String, Object>builder()
                        .putAll(params)
                        .put("performer", WHISTLER.getLogin())
                        .build()
        );

        history.forEach(e -> assertEquals(e.getPerson(), WHISTLER.getLogin()));

        history = history(
                ImmutableMap.<String, Object>builder()
                        .putAll(params)
                        .put("from", Instant.parse(thirdEvent.getUpdated()))
                        .build()
        );

        assertHistoryEqualsByTypesAsc(asList(history), Arrays.asList(
                DiQuotaRequestHistoryEventType.CREATE,
                DiQuotaRequestHistoryEventType.ISSUE_KEY_UPDATE
        ));

        history = history(
                ImmutableMap.<String, Object>builder()
                        .putAll(params)
                        .put("to", Instant.parse(secondEvent.getUpdated()))
                        .build()
        );

        assertHistoryEqualsByTypesAsc(asList(history), Arrays.asList(
                DiQuotaRequestHistoryEventType.CREATE,
                DiQuotaRequestHistoryEventType.ISSUE_KEY_UPDATE
        ));
    }

    @Test
    public void relativePaginationShouldWorkCorrectly() {
        prepareCampaignResources();
        createResourcePreorderRequest();
        createResourcePreorderRequest();

        DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = history(ImmutableMap.of(PAGE_ORDER, "ASC"));

        assertEquals(4, history.size());

        final DiQuotaRequestHistoryEvent second = asList(history).get(1);
        final DiQuotaRequestHistoryEvent third = asList(history).get(2);

        history = history(ImmutableMap.of(PAGE_ORDER, "ASC", FROM_ID, second.getId(), PAGE_SIZE, 3));

        assertEquals(3, history.size());
        assertNull(history.getNextPageUrl());
        assertEquals(history.getFirst().getId(), second.getId());

        history = history(ImmutableMap.of(PAGE_ORDER, "ASC", FROM_ID, second.getId(), PAGE_SIZE, 2));

        assertNotNull(history.getNextPageUrl());
        assertTrue(history.getNextPageUrl().toASCIIString().contains("/api/v1/history/quota-requests/"));
        assertTrue(history.getNextPageUrl().toString().contains("fromId=" + (third.getId() + 1)));

        assertEquals(2, history.size());

        history = history(ImmutableMap.of(PAGE_ORDER, "DESC", FROM_ID, third.getId(), PAGE_SIZE, 2));

        assertNotNull(history.getNextPageUrl());
        assertTrue(history.getNextPageUrl().toString().contains("fromId=" + (second.getId() + 1)));
        assertEquals(2, history.size());

        final List<DiQuotaRequestHistoryEvent> eventList = asList(history);

        assertEquals(eventList.get(0).getId(), third.getId());
        assertEquals(eventList.get(1).getId(), second.getId());
    }

    @Test
    public void eventCanBeFetchedById() {
        prepareCampaignResources();
        createResourcePreorderRequest();
        final DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = history(Collections.emptyMap());
        final DiQuotaRequestHistoryEvent first = history.getFirst();
        final DiQuotaRequestHistoryEvent fetched = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/history/quota-requests/events/" + first.getId())
                .get(DiQuotaRequestHistoryEvent.class);

        assertEquals(first.getId(), fetched.getId());
    }

    @Test
    public void historyByRequestCanBeFetchedWithPaginationAndFilterParams() {
        prepareCampaignResources();
        final DiQuotaChangeRequest req = createResourcePreorderRequest(AMOSOV_F);
        updateStatus(req.getId(), DiQuotaChangeRequest.Status.CANCELLED, AMOSOV_F);
        updateStatus(req.getId(), DiQuotaChangeRequest.Status.NEW, WHISTLER);
        DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = historyByRequest(req.getId());
        assertEquals(4, history.size());
        final List<DiQuotaRequestHistoryEvent> eventList = asList(history);
        final DiQuotaRequestHistoryEvent second = eventList.get(1);
        final DiQuotaRequestHistoryEvent third = eventList.get(2);

        final Map<String, Object> params = ImmutableMap.<String, Object>builder()
                .put("performer", AMOSOV_F.getLogin())
                .put(FROM_ID, second.getId())
                .put(PAGE_ORDER, "ASC")
                .put(PAGE_SIZE, 3)
                .put("to", Instant.now().plusSeconds(100500))
                .put("from", Instant.parse(third.getUpdated()))
                .build();

        history = historyByRequest(req.getId(), params);

        assertEquals(1, history.size());
        assertEquals(history.getFirst().getId(), third.getId());
    }

    @Test
    public void humanReadableErrorAppearsWhenIncorrectInstantParamIsProvided() {
        final Map<String, Object> params = ImmutableMap.<String, Object>builder()
                .put("from", "foo")
                .build();

        assertThrowsWithMessage(() -> history(params), 400, "Incorrect time format for value 'foo'. Expected format: yyyy-MM-dd'T'HH:mm:ss'Z'");
    }

    @Test
    public void humanReadableErrorAppearsWhenIncorrectPageSizeParamIsProvided() {
        final Map<String, Object> params = ImmutableMap.<String, Object>builder()
                .put("pageSize", 0)
                .build();

        assertThrowsWithMessage(() -> history(params), 400, "pageSize must be positive");
    }

    @Test
    public void fromIdDefaultValueDependsOnOrder() {
        prepareCampaignResources();
        final DiQuotaChangeRequest req = createResourcePreorderRequest();
        DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = history(ImmutableMap.of(PAGE_ORDER, "ASC", FROM_ID, 0));
        final List<DiQuotaRequestHistoryEvent> eventList = asList(history);
        final long firstId = eventList.get(0).getId();
        final long lastId = eventList.get(1).getId();

        history = historyByRequest(req.getId(), ImmutableMap.of(PAGE_ORDER, "ASC", PAGE_SIZE, 1));

        assertEquals(1, history.size());
        DiQuotaRequestHistoryEvent event = history.getFirst();
        assertEquals(event.getId(), firstId);

        history = historyByRequest(req.getId(), ImmutableMap.of(PAGE_ORDER, "DESC", PAGE_SIZE, 1));

        assertEquals(1, history.size());
        event = history.getFirst();
        assertEquals(event.getId(), lastId);

        history = history(ImmutableMap.of(PAGE_ORDER, "ASC", PAGE_SIZE, 1));

        assertEquals(1, history.size());
        event = history.getFirst();
        assertEquals(event.getId(), firstId);

        history = history(ImmutableMap.of(PAGE_ORDER, "DESC", PAGE_SIZE, 1));

        assertEquals(1, history.size());
        event = history.getFirst();
        assertEquals(event.getId(), lastId);
    }

    @Test
    public void historyShouldNotBeUpdatedWhenThereIsNoRealUpdate() {
        prepareCampaignResources();
        final DiQuotaChangeRequest req = createResourcePreorderRequest();
        final String foo = "foo";
        final BodyUpdate update = new BodyUpdate.BodyUpdateBuilder()
                .calculations(foo)
                .changes(PROVIDER, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.GIBIBYTE))
                .build();
        updateRequest(req.getId(), update, AMOSOV_F);

        DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = historyByRequest(req.getId());

        assertEquals(3, history.size());

        updateRequest(req.getId(), update, AMOSOV_F);

        history = historyByRequest(req.getId());

        assertEquals(3, history.size());
    }

    @Test
    public void readyAndAllocatedAmountUpdateWillBeRecorded() {
        prepareCampaignResources();
        final DiQuotaChangeRequest first = createResourcePreorderRequest(aggregatedCampaign);
        final DiQuotaChangeRequest second = createResourcePreorderRequest(aggregatedCampaign);
        answerGrowthRequestGoalQuestions(first.getId());
        answerGrowthRequestGoalQuestions(second.getId());

        updateStatus(first.getId(), DiQuotaChangeRequest.Status.APPROVED, WHISTLER);
        updateStatus(first.getId(), DiQuotaChangeRequest.Status.CONFIRMED, KEYD);
        updateStatus(second.getId(), DiQuotaChangeRequest.Status.APPROVED, WHISTLER);
        updateStatus(second.getId(), DiQuotaChangeRequest.Status.CONFIRMED, KEYD);
        final SetResourceAmountBody setResourceAmountBody = new SetResourceAmountBody(
                Arrays.asList(
                        new SetResourceAmountBody.Item(first.getId(), null, Arrays.asList(
                                new SetResourceAmountBody.ChangeBody(PROVIDER, bigOrderOne.getId(), STORAGE, Collections.emptySet(),
                                        DiAmount.of(1, DiUnit.GIBIBYTE), DiAmount.of(1, DiUnit.GIBIBYTE))), ""
                        ),
                        new SetResourceAmountBody.Item(second.getId(), null, Arrays.asList(
                                new SetResourceAmountBody.ChangeBody(PROVIDER, bigOrderOne.getId(), STORAGE, Collections.emptySet(),
                                        DiAmount.of(1, DiUnit.GIBIBYTE), DiAmount.of(1, DiUnit.GIBIBYTE))), ""
                        )
                )
        );
        final DiSetAmountResult result = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, setResourceAmountBody, DiSetAmountResult.class);
        assertEquals("SUCCESS", result.getStatus());
        final DiListRelativePageResponse<DiQuotaRequestHistoryEvent> firstHistory = historyByRequest(first.getId());
        final DiListRelativePageResponse<DiQuotaRequestHistoryEvent> secondHistory = historyByRequest(second.getId());
        final List<DiQuotaRequestHistoryEvent> firstEventList = asList(firstHistory);
        final List<DiQuotaRequestHistoryEvent> secondEventList = asList(secondHistory);
        final List<DiQuotaRequestHistoryEventType> expectedHistory = Arrays.asList(
                DiQuotaRequestHistoryEventType.CREATE,
                DiQuotaRequestHistoryEventType.ISSUE_KEY_UPDATE,
                DiQuotaRequestHistoryEventType.FIELDS_UPDATE,
                DiQuotaRequestHistoryEventType.STATUS_UPDATE,
                DiQuotaRequestHistoryEventType.STATUS_UPDATE,
                DiQuotaRequestHistoryEventType.STATUS_UPDATE,
                DiQuotaRequestHistoryEventType.CHANGES_READY_AMOUNT_UPDATE,
                DiQuotaRequestHistoryEventType.CHANGES_ALLOCATED_AMOUNT_UPDATE,
                DiQuotaRequestHistoryEventType.CHANGES_ALLOCATING_AMOUNT_UPDATE
        );
        assertHistoryEqualsByTypesAsc(firstEventList, expectedHistory);
        assertHistoryEqualsByTypesAsc(secondEventList, expectedHistory);
    }

    @Test
    public void readyForAllocationStateUpdateWillBeRecorded() {
        final Service service = serviceDao.read(PROVIDER);

        final Service updatedService = Service.withKey(service.getKey())
                .withName(service.getName())
                .withPriority(service.getPriority())
                .withAbcServiceId(service.getAbcServiceId())
                .withId(service.getId())
                .withSettings(Service.Settings.builder()
                        .accountActualValuesInQuotaDistribution(service.getSettings().accountActualValuesInQuotaDistribution())
                        .usesProjectHierarchy(service.getSettings().usesProjectHierarchy())
                        .manualQuotaAllocation(true)
                        .build())
                .build();

        serviceDao.update(updatedService);

        prepareCampaignResources();
        resourceDao.updateAll(
                resourceDao.getByService(updatedService).stream()
                        .map(resource -> new Resource.Builder(resource.getPublicKey(), updatedService)
                                .id(resource.getId())
                                .name(resource.getName())
                                .description(resource.getDescription())
                                .type(resource.getType())
                                .mode(resource.getMode())
                                .group(resource.getGroup())
                                .priority(resource.getPriority())
                                .build()
                        ).collect(Collectors.toList())
        );

        updateHierarchy();

        final DiQuotaChangeRequest request = createResourcePreorderRequest(aggregatedCampaign);
        answerGrowthRequestGoalQuestions(request.getId());
        updateStatus(request.getId(), DiQuotaChangeRequest.Status.APPROVED, WHISTLER);
        updateStatus(request.getId(), DiQuotaChangeRequest.Status.CONFIRMED, KEYD);

        SetResourceAmountBody setResourceAmountBody = new SetResourceAmountBody(
                Arrays.asList(
                        new SetResourceAmountBody.Item(request.getId(), null, Arrays.asList(
                                new SetResourceAmountBody.ChangeBody(PROVIDER, bigOrderOne.getId(), STORAGE, Collections.emptySet(),
                                        DiAmount.of(10, DiUnit.GIBIBYTE), null)), ""
                        )
                )
        );
        DiSetAmountResult quotaStateResult = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, setResourceAmountBody, DiSetAmountResult.class);
        assertEquals("SUCCESS", quotaStateResult.getStatus());

        final DiQuotaChangeRequest result = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/" + request.getId() + "/request-allocation")
                .post(null, DiQuotaChangeRequest.class);
        DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = historyByRequest(request.getId());

        List<DiQuotaRequestHistoryEvent> eventList = asList(history);

        assertHistoryEqualsByTypesAsc(eventList, Arrays.asList(
                DiQuotaRequestHistoryEventType.CREATE,
                DiQuotaRequestHistoryEventType.ISSUE_KEY_UPDATE,
                DiQuotaRequestHistoryEventType.FIELDS_UPDATE,
                DiQuotaRequestHistoryEventType.STATUS_UPDATE,
                DiQuotaRequestHistoryEventType.STATUS_UPDATE,
                DiQuotaRequestHistoryEventType.STATUS_UPDATE,
                DiQuotaRequestHistoryEventType.CHANGES_READY_AMOUNT_UPDATE
        ));

        setResourceAmountBody = new SetResourceAmountBody(
                Arrays.asList(
                        new SetResourceAmountBody.Item(request.getId(), null, Arrays.asList(
                                new SetResourceAmountBody.ChangeBody(PROVIDER, bigOrderOne.getId(), STORAGE, Collections.emptySet(),
                                        null, DiAmount.of(9, DiUnit.GIBIBYTE))), ""
                        )
                )
        );

        quotaStateResult = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, setResourceAmountBody, DiSetAmountResult.class);
        assertEquals("SUCCESS", quotaStateResult.getStatus());

        history = historyByRequest(request.getId());
        eventList = asList(history);

        assertHistoryEqualsByTypesAsc(eventList, Arrays.asList(
                DiQuotaRequestHistoryEventType.CREATE,
                DiQuotaRequestHistoryEventType.ISSUE_KEY_UPDATE,
                DiQuotaRequestHistoryEventType.FIELDS_UPDATE,
                DiQuotaRequestHistoryEventType.STATUS_UPDATE,
                DiQuotaRequestHistoryEventType.STATUS_UPDATE,
                DiQuotaRequestHistoryEventType.STATUS_UPDATE,
                DiQuotaRequestHistoryEventType.CHANGES_READY_AMOUNT_UPDATE,
                DiQuotaRequestHistoryEventType.CHANGES_ALLOCATED_AMOUNT_UPDATE,
                DiQuotaRequestHistoryEventType.CHANGES_ALLOCATING_AMOUNT_UPDATE
        ));
    }

    @Test
    public void requestWithEmptyProjectsFilterWillWorkCorrectly() {
        final DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = history(ImmutableMap.of("ancestorAbcServiceId", 1234567890L));
        assertTrue(history.isEmpty());
        assertNull(history.getNextPageUrl());
    }
}
