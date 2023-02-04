package ru.yandex.qe.dispenser.ws.logic;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.inside.goals.model.Goal.Importance;
import ru.yandex.inside.goals.model.Goal.Status;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.Body;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.api.v1.request.quota.ChangeBody;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.goal.Goal;
import ru.yandex.qe.dispenser.domain.dao.goal.OkrAncestors;
import ru.yandex.qe.dispenser.domain.index.LongIndexBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class MultiServicePreOrderRequestTest extends BaseQuotaRequestTest {

    public static final Comparator<DiQuotaChangeRequest.Change> CHANGE_COMPARATOR = Comparator.comparing((DiQuotaChangeRequest.Change c) -> c.getService().getKey())
            .thenComparing(c -> c.getResource().getKey())
            .thenComparing(c -> c.getOrder() == null ? null : c.getOrder().getId())
            .thenComparing(c -> c.getSegmentKeys().stream().sorted().collect(Collectors.joining(",")));
    private @NotNull Campaign campaign;


    @Override
    @BeforeAll
    public void beforeClass() {
        bigOrderManager.clear();
        bigOrderOne = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE));
        bigOrderTwo = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE_2));
        bigOrderThree = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE_3));

        campaignDao.clear();
        goalDao.clear();
        newGoal = goalDao.create(new Goal(TEST_GOAL_ID, TEST_GOAL_NAME, Importance.DEPARTMENT, Status.NEW, OkrAncestors.EMPTY));
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setBigOrders(Lists.newArrayList(
                        new Campaign.BigOrder(bigOrderOne.getId(), LocalDate.now().plusDays(100)),
                        new Campaign.BigOrder(bigOrderTwo.getId(), LocalDate.now().plusDays(200))
                ))
                .build());

        campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey(TEST_CAMPAIGN_KEY + "_off")
                .setName(TEST_CAMPAIGN_NAME + "_off")
                .setStatus(Campaign.Status.CLOSED)
                .setStartDate(LocalDate.now().minusDays(10))
                .setBigOrders(Lists.newArrayList(
                        new Campaign.BigOrder(bigOrderThree.getId(), LocalDate.now().minusDays(300))
                ))
                .build());
    }

    @Test
    public void requestWithSeveralProvidersAndOrdersCanBeCreated() {
        final ImmutableList<ChangeBody> changeBodies = ImmutableList.of(
                new ChangeBody(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(300, DiUnit.CORES)),
                new ChangeBody(MDB, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(400, DiUnit.GIBIBYTE)),
                new ChangeBody(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.COUNT)),
                new ChangeBody(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(200, DiUnit.COUNT)),
                new ChangeBody(NIRVANA, YT_GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(300, DiUnit.COUNT)),
                new ChangeBody(YP, SEGMENT_CPU, bigOrderTwo.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(500, DiUnit.CORES)),
                new ChangeBody(YP, SEGMENT_CPU, bigOrderTwo.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_1), DiAmount.of(600, DiUnit.CORES))
        );

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final Body.BodyBuilder bodyBuilder = requestBodyBuilderWithDefaultFields()
                .projectKey(project.getPublicKey())
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH);

        for (final ChangeBody changeBody : changeBodies) {
            bodyBuilder.changes(changeBody);
        }

        prepareCampaignResources();
        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(bodyBuilder.build(), null)
                .performBy(AMOSOV_F).getFirst();

        final List<DiQuotaChangeRequest.Change> changes = request.getChanges();
        assertEquals(7, changes.size());

        final List<DiQuotaChangeRequest.Change> sortedChanges = changes.stream()
                .sorted(CHANGE_COMPARATOR)
                .collect(Collectors.toList());


        for (int i = 0; i < changes.size(); i++) {
            final DiQuotaChangeRequest.Change change = sortedChanges.get(i);
            final ChangeBody body = changeBodies.get(i);

            assertEquals(change.getService().getKey(), body.getServiceKey());
            assertEquals(change.getResource().getKey(), body.getResourceKey());
            assertEquals(change.getOrder().getId(), (long) body.getOrderId());
            assertEquals(change.getSegmentKeys(), body.getSegmentKeys());
            assertEquals(body.getAmount().getUnit().convert(change.getAmount()), body.getAmount().getValue());
            assertEquals(0L, change.getAmountReady().getValue());
            assertEquals(0L, change.getAmountAllocated().getValue());
        }

    }

    @Test
    public void invalidBigOrdersShouldValidated() {
        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .projectKey(YANDEX)
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                            .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(300, DiUnit.CORES))
                            .changes(MDB, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(300, DiUnit.CORES))
                            .changes(MDB, CPU, bigOrderThree.getId(), Collections.emptySet(), DiAmount.of(300, DiUnit.CORES))
                            .build(), null)
                    .performBy(AMOSOV_F);
        }, "Big order '" + bigOrderThree.getId() + "' is not present in the current campaign");
    }

    @Test
    public void requestChangesCanBeUpdated() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(300, DiUnit.CORES))
                        .changes(MDB, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(300, DiUnit.CORES))
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        assertEquals(2, request.getChanges().size());

        final DiQuotaChangeRequest updatedRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(300, DiUnit.CORES))
                        .changes(MDB, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(300, DiUnit.MEBIBYTE))
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);

        assertEquals(3, updatedRequest.getChanges().size());
    }

    @Test
    public void requestsFilteredByServiceShouldReturnAllChanges() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        final long mdbRequestId = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(300, DiUnit.CORES))
                        .changes(MDB, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(300, DiUnit.TEBIBYTE))
                        .build(), null)
                .performBy(AMOSOV_F).getFirst().getId();

        final long mdbNirvanaRequestId = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.COUNT))
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(200, DiUnit.COUNT))
                        .changes(MDB, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(400, DiUnit.GIBIBYTE))
                        .build(), null)
                .performBy(AMOSOV_F).getFirst().getId();

        final long ypNirvanaRequestId = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.COUNT))
                        .changes(YP, SEGMENT_CPU, bigOrderTwo.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(500, DiUnit.CORES))
                        .changes(YP, SEGMENT_CPU, bigOrderTwo.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_1), DiAmount.of(600, DiUnit.CORES))
                        .build(), null)
                .performBy(AMOSOV_F).getFirst().getId();


        final DiListResponse<DiQuotaChangeRequest> mdbFiltered = dispenser().quotaChangeRequests()
                .get()
                .query("service", MDB)
                .perform();

        assertEquals(2, mdbFiltered.size());

        Map<Long, DiQuotaChangeRequest> requestById = mdbFiltered.stream()
                .collect(Collectors.toMap(DiQuotaChangeRequest::getId, Function.identity()));

        assertEquals(2, requestById.get(mdbRequestId).getChanges().size());
        assertEquals(3, requestById.get(mdbNirvanaRequestId).getChanges().size());

        final DiListResponse<DiQuotaChangeRequest> ypFiltered = dispenser().quotaChangeRequests()
                .get()
                .query("service", YP)
                .perform();

        assertEquals(1, ypFiltered.size());

        requestById = ypFiltered.stream()
                .collect(Collectors.toMap(DiQuotaChangeRequest::getId, Function.identity()));

        assertEquals(3, requestById.get(ypNirvanaRequestId).getChanges().size());

        final DiListResponse<DiQuotaChangeRequest> order1Filtered = dispenser().quotaChangeRequests()
                .get()
                .query("order", String.valueOf(bigOrderOne.getId()))
                .perform();

        assertEquals(3, order1Filtered.size());

        requestById = order1Filtered.stream()
                .collect(Collectors.toMap(DiQuotaChangeRequest::getId, Function.identity()));

        assertEquals(2, requestById.get(mdbRequestId).getChanges().size());
        assertEquals(3, requestById.get(mdbNirvanaRequestId).getChanges().size());
        assertEquals(3, requestById.get(ypNirvanaRequestId).getChanges().size());

        final DiListResponse<DiQuotaChangeRequest> order2Filtered = dispenser().quotaChangeRequests()
                .get()
                .query("order", String.valueOf(bigOrderTwo.getId()))
                .perform();

        assertEquals(2, order2Filtered.size());

        requestById = order2Filtered.stream()
                .collect(Collectors.toMap(DiQuotaChangeRequest::getId, Function.identity()));

        assertEquals(3, requestById.get(mdbNirvanaRequestId).getChanges().size());
        assertEquals(3, requestById.get(ypNirvanaRequestId).getChanges().size());

        final DiListResponse<DiQuotaChangeRequest> order3Filtered = dispenser().quotaChangeRequests()
                .get()
                .query("order", String.valueOf(bigOrderThree.getId()))
                .perform();

        assertTrue(order3Filtered.isEmpty());

        final Map<Long, Long> campaignOrderIdByBigOrderId = campaign.getBigOrders().stream()
                .collect(Collectors.toMap(Campaign.BigOrder::getBigOrderId, LongIndexBase::getId));

        final DiListResponse<DiQuotaChangeRequest> campaignOrder1Filtered = dispenser().quotaChangeRequests()
                .get()
                .query("campaignOrder", String.valueOf(campaignOrderIdByBigOrderId.get(bigOrderOne.getId())))
                .perform();

        assertEquals(3, campaignOrder1Filtered.size());

        requestById = campaignOrder1Filtered.stream()
                .collect(Collectors.toMap(DiQuotaChangeRequest::getId, Function.identity()));

        assertEquals(2, requestById.get(mdbRequestId).getChanges().size());
        assertEquals(3, requestById.get(mdbNirvanaRequestId).getChanges().size());
        assertEquals(3, requestById.get(ypNirvanaRequestId).getChanges().size());

        final DiListResponse<DiQuotaChangeRequest> campaignOrder2Filtered = dispenser().quotaChangeRequests()
                .get()
                .query("campaignOrder", String.valueOf(campaignOrderIdByBigOrderId.get(bigOrderTwo.getId())))
                .perform();

        assertEquals(2, campaignOrder2Filtered.size());

        requestById = campaignOrder2Filtered.stream()
                .collect(Collectors.toMap(DiQuotaChangeRequest::getId, Function.identity()));

        assertEquals(3, requestById.get(mdbNirvanaRequestId).getChanges().size());
        assertEquals(3, requestById.get(ypNirvanaRequestId).getChanges().size());
    }

    @Test
    public void requestsFilteredByCampaignType() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());
        campaignDao.clear();
        campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setType(Campaign.Type.DRAFT)
                .setBigOrders(Lists.newArrayList(
                        new Campaign.BigOrder(bigOrderOne.getId(), LocalDate.now().plusDays(100)),
                        new Campaign.BigOrder(bigOrderTwo.getId(), LocalDate.now().plusDays(200))
                ))
                .build());
        updateHierarchy();
        prepareCampaignResources();
        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(300, DiUnit.CORES))
                        .changes(MDB, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(300, DiUnit.TEBIBYTE))
                        .build(), campaign.getId())
                .performBy(AMOSOV_F).getFirst().getId();
        campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("another-campaign")
                .setName("Another campaign")
                .setType(Campaign.Type.AGGREGATED)
                .setStatus(Campaign.Status.ACTIVE)
                .setBigOrders(Lists.newArrayList(
                        new Campaign.BigOrder(bigOrderOne.getId(), LocalDate.now().plusDays(100)),
                        new Campaign.BigOrder(bigOrderTwo.getId(), LocalDate.now().plusDays(200))
                ))
                .build());
        updateHierarchy();
        prepareCampaignResources();
        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.COUNT))
                        .changes(YP, SEGMENT_CPU, bigOrderTwo.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(500, DiUnit.CORES))
                        .changes(YP, SEGMENT_CPU, bigOrderTwo.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_1), DiAmount.of(600, DiUnit.CORES))
                        .build(), campaign.getId())
                .performBy(AMOSOV_F).getFirst().getId();
        DiListResponse<DiQuotaChangeRequest> draftFiltered = dispenser().quotaChangeRequests()
                .get()
                .query("campaignTypes", Campaign.Type.DRAFT.name())
                .perform();
        assertEquals(1, draftFiltered.size());
        DiListResponse<DiQuotaChangeRequest> aggregatedFiltered = dispenser().quotaChangeRequests()
                .get()
                .query("campaignTypes", Campaign.Type.AGGREGATED.name())
                .perform();
        assertEquals(1, aggregatedFiltered.size());
        DiListResponse<DiQuotaChangeRequest> bothFiltered = dispenser().quotaChangeRequests()
                .get()
                .query("campaignTypes", Campaign.Type.DRAFT.name(), Campaign.Type.AGGREGATED.name())
                .perform();
        assertEquals(2, bothFiltered.size());
        DiListResponse<DiQuotaChangeRequest> anyFiltered = dispenser().quotaChangeRequests()
                .get()
                .perform();
        assertEquals(2, anyFiltered.size());
    }

}
