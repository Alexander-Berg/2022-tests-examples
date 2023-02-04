package ru.yandex.qe.dispenser.ws.logic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourceGroup;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiSegmentation;
import ru.yandex.qe.dispenser.api.v1.DiService;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.Body;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.CampaignResource;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.ResourceSegmentation;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.index.LongIndexBase;
import ru.yandex.qe.dispenser.standalone.MockTrackerManager;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;
import ru.yandex.startrek.client.model.CommentCreate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest.TEST_PROJECT_KEY;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.requestBodyBuilderWithDefaultFields;

public class ResourcePreOrderRequestWorkflowValidationTest extends BusinessLogicTestBase {

    private static final String NEW_RESOURCE_KEY = "UNIQUE-RESOURCE";

    @Autowired
    private BigOrderManager bigOrderManager;

    @Autowired
    private MockTrackerManager trackerManager;

    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private QuotaChangeRequestDao quotaChangeRequestDao;

    @Autowired
    private MockTrackerManager mockTrackerManager;

    @Autowired
    private ResourceDao resourceDao;

    @Autowired
    private ServiceDao serviceDao;

    private BigOrder bigOrder;

    @BeforeAll
    public void beforeClass() {
        final String date = LocalDate.of(2020, Month.JANUARY, 1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        bigOrderManager.clear();
        bigOrder = bigOrderManager.create(BigOrder.builder(LocalDate.of(2020, 1, 1)));
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        dispenser()
                .service(MDB)
                .resource(NEW_RESOURCE_KEY)
                .create()
                .withName(NEW_RESOURCE_KEY)
                .withType(DiResourceType.TRAFFIC)
                .performBy(AMOSOV_F);

        createProject("test1", YANDEX, BINARY_CAT.getLogin());
        createProject("test2", "test1", LOTREK.getLogin());

        campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setStartDate(LocalDate.of(2020, Month.JANUARY, 1))
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, Month.JANUARY, 1))))
                .build());
        prepareCampaignResources();
    }

    @Test
    public void resourceRequestCantBeConfirmedByAnyResponsibleAncestorService() {
        final DiQuotaChangeRequest request = dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey("test2")
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(MDB, NEW_RESOURCE_KEY, bigOrder.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.TBPS))
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F)
                .getFirst();

        assertThrowsWithMessage(() -> dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.CONFIRMED)
                .performBy(LOTREK), "Can't change status to CONFIRMED. Available statuses: CANCELLED, READY_FOR_REVIEW");

    }

    @Test
    public void confirmedIsFinalStatusForResourcePreOrderRequest() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey(project.getPublicKey())
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(MDB, NEW_RESOURCE_KEY, bigOrder.getId(), Collections.emptySet(), DiAmount.of(42, DiUnit.TBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);


        assertEquals(1, requests.size());

        final DiQuotaChangeRequest changeRequest = requests.getFirst();

        assertEquals(DiQuotaChangeRequest.Status.NEW, changeRequest.getStatus());

        quotaChangeRequestDao.update(
                quotaChangeRequestDao.read(changeRequest.getId()).copyBuilder().status(QuotaChangeRequest.Status.CONFIRMED).build());

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(changeRequest.getId())
                    .setStatus(DiQuotaChangeRequest.Status.APPLIED)
                    .performBy(AMOSOV_F);
        }, "Can't change status to APPLIED.");
    }

    @Test
    public void requestCantBeCreatedForTrashProject() {

        createProject(Project.TRASH_PROJECT_KEY, YANDEX);
        createProject("trash-test", Project.TRASH_PROJECT_KEY);

        assertThrowsWithMessage(() -> {

            final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                    .create(new Body.BodyBuilder()
                            .projectKey("trash-test")
                            .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                            .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                            .changes(MDB, NEW_RESOURCE_KEY, bigOrder.getId(), Collections.emptySet(), DiAmount.of(42, DiUnit.TBPS))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .build(), null)
                    .performBy(AMOSOV_F);
        }, "Request can't be created in closed, deleted or un-exportable project");
    }

    @Test
    public void onTrackerTriggerReopenRequest() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey(project.getPublicKey())
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(MDB, NEW_RESOURCE_KEY, bigOrder.getId(), Collections.emptySet(), DiAmount.of(42, DiUnit.TBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F)
                .getFirst();

        quotaChangeRequestDao.update(
                quotaChangeRequestDao.read(request.getId()).copyBuilder().status(QuotaChangeRequest.Status.CONFIRMED).build());

        updateHierarchy();

        final Map<String, String> triggerData = ImmutableMap.of("ticketKey", request.getTrackerIssueKey());

        final Response forbiddenRequest = createAuthorizedLocalClient(LOTREK)
                .path("/v1/trigger/resource-preorder-comment")
                .post(triggerData);

        assertEquals(403, forbiddenRequest.getStatus());

        final Response validRequest = createAuthorizedLocalClient(DiPerson.login("robot-dispenser"))
                .path("/v1/trigger/resource-preorder-comment")
                .post(triggerData);

        assertEquals(200, validRequest.getStatus());

        updateHierarchy();

        final DiQuotaChangeRequest refetchedRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.NEW, refetchedRequest.getStatus());

        final List<CommentCreate> issueComments = trackerManager.getIssueComments(refetchedRequest.getTrackerIssueKey());

        assertEquals(1, issueComments.size());
        assertEquals("кем:robot-dispenser заявка переведена в статус %%Черновик%%", issueComments.get(0).getComment().get());
        assertEquals(Sets.newHashSet(issueComments.get(0).getSummonees()), ImmutableSet.of(AMOSOV_F.getLogin()));
    }

    @Test
    public void commentShouldHaveCommentAboutResourceAndDescriptionChangesAtSameTime() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        final String trackerIssueKey = request.getTrackerIssueKey();
        List<CommentCreate> comments = trackerManager.getIssueComments(trackerIssueKey);

        assertEquals(comments.size(), 0);

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.COUNT))
                        .description("new description")
                        .build())
                .performBy(AMOSOV_F);

        comments = trackerManager.getIssueComments(trackerIssueKey);

        assertEquals(1, comments.size());
        final CommentCreate lastComment = comments.get(0);
        assertEquals("Ресурсы изменены кем:amosov-f\n" +
                "\n" +
                "* YT CPU: 100 units -> **!!(red)200 units!!**\n" +
                "\n" +
                "\n" +
                "Описание изменено кем:amosov-f", lastComment.getComment().get());

    }

    @Test
    public void requestShouldHavePermissionIfUserCanUpdateReviewPopup() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final long requestId = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst().getId();


        Set<DiQuotaChangeRequest.Permission> permissions = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + requestId)
                .get(DiQuotaChangeRequest.class)
                .getPermissions();

        assertFalse(permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP));

        permissions = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + requestId)
                .get(DiQuotaChangeRequest.class)
                .getPermissions();

        assertTrue(permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP));

    }

    @Test
    public void servicesRestrictionForBigOrdersShouldWork() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        BigOrder anotherBigOrder = bigOrderManager.create(BigOrder.builder(LocalDate.of(2020, 1, 1)));

        final Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setKey("restrict")
                .setName("Restrict")
                .setStartDate(LocalDate.of(2020, Month.JANUARY, 2))
                .setBigOrders(Arrays.asList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, Month.JANUARY, 1)),
                        new Campaign.BigOrder(anotherBigOrder.getId(), LocalDate.of(2020, Month.JANUARY, 1))
                ))
                .build());

        updateHierarchy();
        prepareCampaignResources();
        prepareCampaignResourceWithRestrictions(campaign, YT_CPU, Optional.empty(),
                campaign.getBigOrders().stream().filter(bigOrder -> bigOrder.getBigOrderId() == anotherBigOrder.getId()).map(LongIndexBase::getId).findFirst(),
                Collections.emptyMap()
        );

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, anotherBigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        assertThrowsWithMessage(() ->
                dispenser().quotaChangeRequests()
                        .create(requestBodyBuilderWithDefaultFields()
                                .projectKey(project.getPublicKey())
                                .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                                .build(), null)
                        .performBy(AMOSOV_F), "Big order " + bigOrder.getId() + " in resource change YT CPU for service Nirvana is not in segmented big orders for campaign");

        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey("test2")
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(MDB, NEW_RESOURCE_KEY, bigOrder.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.TBPS))
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
    }

    @Test
    public void servicesRestrictionForLocationsShouldWork() {
        BigOrder anotherBigOrder = bigOrderManager.create(BigOrder.builder(LocalDate.of(2020, 1, 1)));

        final Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setKey("restrict")
                .setName("Restrict")
                .setStartDate(LocalDate.of(2020, Month.JANUARY, 2))
                .setBigOrders(Arrays.asList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, Month.JANUARY, 1)),
                        new Campaign.BigOrder(anotherBigOrder.getId(), LocalDate.of(2020, Month.JANUARY, 1))
                ))
                .build());

        updateHierarchy();

        prepareCampaignResources();
        final Map<List<Long>, List<Long>> bigOrderSegmentations = new HashMap<>();
        bigOrderSegmentations.put(
                List.of(
                        Hierarchy.get().getSegmentReader().read(DC_SEGMENT_1).getId(),
                        Hierarchy.get().getSegmentReader().read(SEGMENT_SEGMENT_1).getId()
                ),
                Collections.singletonList(
                        campaign.getBigOrders().stream().filter(bo -> bo.getBigOrderId() == bigOrder.getId()).map(LongIndexBase::getId).findFirst().get()
                )
        );
        bigOrderSegmentations.put(
                List.of(
                        Hierarchy.get().getSegmentReader().read(DC_SEGMENT_2).getId(),
                        Hierarchy.get().getSegmentReader().read(SEGMENT_SEGMENT_1).getId()
                ),
                Collections.singletonList(
                        campaign.getBigOrders().stream().filter(bo -> bo.getBigOrderId() == anotherBigOrder.getId()).map(LongIndexBase::getId).findFirst().get()
                )
        );
        bigOrderSegmentations.put(
                List.of(
                        Hierarchy.get().getSegmentReader().read(DC_SEGMENT_3).getId(),
                        Hierarchy.get().getSegmentReader().read(SEGMENT_SEGMENT_1).getId()
                ),
                Collections.singletonList(
                        campaign.getBigOrders().stream().filter(bo -> bo.getBigOrderId() == anotherBigOrder.getId()).map(LongIndexBase::getId).findFirst().get()
                )
        );
        bigOrderSegmentations.put(
                List.of(
                        Hierarchy.get().getSegmentReader().read(DC_SEGMENT_3).getId(),
                        Hierarchy.get().getSegmentReader().read(SEGMENT_SEGMENT_2).getId()
                ),
                Collections.singletonList(
                        campaign.getBigOrders().stream().filter(bo -> bo.getBigOrderId() == bigOrder.getId()).map(LongIndexBase::getId).findFirst().get()
                )
        );
        prepareCampaignResourceWithRestrictions(campaign, SEGMENT_CPU, Optional.empty(), Optional.empty(), bigOrderSegmentations);

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(100L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        assertThrowsWithMessage(() ->
                dispenser().quotaChangeRequests()
                        .create(requestBodyBuilderWithDefaultFields()
                                .projectKey(project.getPublicKey())
                                .changes(YP, SEGMENT_CPU, bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_3, SEGMENT_SEGMENT_1), DiAmount.of(100L, DiUnit.CORES))
                                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                                .build(), null)
                        .performBy(AMOSOV_F), "Big order " + bigOrder.getId() + " not allowed for segment (LOCATION_3, Segment1) in resource Segment CPU for service YP");

        assertThrowsWithMessage(() ->
                dispenser().quotaChangeRequests()
                        .create(requestBodyBuilderWithDefaultFields()
                                .projectKey(project.getPublicKey())
                                .changes(YP, SEGMENT_CPU, anotherBigOrder.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(100L, DiUnit.CORES))
                                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                                .build(), null)
                        .performBy(AMOSOV_F), "Big order " + anotherBigOrder.getId() + " not allowed for segment (LOCATION_1, Segment1) in resource Segment CPU for service YP");

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, anotherBigOrder.getId(), ImmutableSet.of(DC_SEGMENT_3, SEGMENT_SEGMENT_1), DiAmount.of(100L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        // test big order allowed by segments pair
        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU,
                                anotherBigOrder.getId(), ImmutableSet.of(DC_SEGMENT_3, SEGMENT_SEGMENT_2),
                                DiAmount.of(100L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F),
                "Big order " + anotherBigOrder.getId() +
                        " not allowed for segment (LOCATION_3, Segment2) in resource Segment CPU for service YP"
        );
        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU,
                                bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_3, SEGMENT_SEGMENT_2),
                                DiAmount.of(100L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
    }

    @Test
    public void servicesLocationRestrictionForResourceGroupsShouldWork() {
        BigOrder anotherBigOrder = bigOrderManager.create(BigOrder.builder(LocalDate.of(2020, 1, 1)));

        final Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setKey("restrict")
                .setName("Restrict")
                .setStartDate(LocalDate.of(2020, Month.JANUARY, 2))
                .setBigOrders(Arrays.asList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, Month.JANUARY, 1)),
                        new Campaign.BigOrder(anotherBigOrder.getId(), LocalDate.of(2020, Month.JANUARY, 1))
                ))
                .build());

        final DiService mdb = DiService.withKey(MDB).withName(MDB).build();

        dispenser()
                .service(MDB)
                .resourceGroups()
                .create(DiResourceGroup.withKey("g1")
                        .inService(mdb)
                        .withName("g1")
                        .build())
                .performBy(AMOSOV_F);

        dispenser()
                .service(MDB)
                .resourceGroups()
                .create(DiResourceGroup.withKey("g2_etc")
                        .inService(mdb)
                        .withName("g2")
                        .build())
                .performBy(AMOSOV_F);


        updateHierarchy();


        dispenser()
                .service(MDB)
                .resource("cpu2")
                .create()
                .withName("cpu2")
                .inMode(DiQuotingMode.SYNCHRONIZATION)
                .withType(DiResourceType.PROCESSOR)
                .inGroup("g1")
                .performBy(AMOSOV_F);

        dispenser()
                .service(MDB)
                .resource("ram2")
                .create()
                .withName("ram2")
                .inMode(DiQuotingMode.SYNCHRONIZATION)
                .withType(DiResourceType.MEMORY)
                .inGroup("g2_etc")
                .performBy(AMOSOV_F);

        updateHierarchy();

        final List<DiSegmentation> segmentations = Collections.singletonList(new DiSegmentation.Builder(DC_SEGMENTATION).withDescription(DC_SEGMENTATION).withName(DC_SEGMENTATION).build());

        dispenser()
                .service(MDB)
                .resource("cpu2")
                .segmentations()
                .update(segmentations)
                .performBy(AMOSOV_F);

        dispenser()
                .service(MDB)
                .resource("ram2")
                .segmentations()
                .update(segmentations)
                .performBy(AMOSOV_F);

        updateHierarchy();

        prepareCampaignResources();

        prepareCampaignResourceWithRestrictions(campaign, "cpu2", Optional.of(DC_SEGMENT_1), Optional.empty(), Collections.emptyMap());
        prepareCampaignResourceWithRestrictions(campaign, "ram2", Optional.of(DC_SEGMENT_2), Optional.empty(), Collections.emptyMap());

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(MDB, "cpu2", bigOrder.getId(), Collections.singleton(DC_SEGMENT_1), DiAmount.of(100L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        assertThrowsWithMessage(() ->
                        dispenser().quotaChangeRequests()
                                .create(requestBodyBuilderWithDefaultFields()
                                        .projectKey(project.getPublicKey())
                                        .changes(MDB, "cpu2", bigOrder.getId(), Collections.singleton(DC_SEGMENT_2), DiAmount.of(100L, DiUnit.CORES))
                                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                                        .build(), null)
                                .performBy(AMOSOV_F)
                , "Segment LOCATION_2 not allowed in resource cpu2 for service MDB");

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(MDB, "ram2", bigOrder.getId(), Collections.singleton(DC_SEGMENT_2), DiAmount.of(100L, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        assertThrowsWithMessage(() ->
                        dispenser().quotaChangeRequests()
                                .create(requestBodyBuilderWithDefaultFields()
                                        .projectKey(project.getPublicKey())
                                        .changes(MDB, "ram2", bigOrder.getId(), Collections.singleton(DC_SEGMENT_3), DiAmount.of(100L, DiUnit.BYTE))
                                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                                        .build(), null)
                                .performBy(AMOSOV_F),
                "Segment LOCATION_3 not allowed in resource ram2 for service MDB");
    }

    private void prepareCampaignResourceWithRestrictions(
            final Campaign campaign,
            final String resourceKey,
            final Optional<String> segmentRestriction,
            final Optional<Long> bigOrderRestriction,
            final Map<List<Long>, List<Long>> segmentsBigOrderRestriction
    ) {
        TransactionWrapper.INSTANCE.execute(() -> {
            final List<Resource> resourceList = Hierarchy.get().getResourceReader().getAll().stream()
                    .filter(resource -> resource.getPublicKey().equals(resourceKey)).collect(Collectors.toList());
            assertEquals(1, resourceList.size());

            final Resource resource = resourceList.get(0);

            final List<Long> bigOrders = bigOrderRestriction.isPresent() ? Arrays.asList(bigOrderRestriction.get()) :
                    campaign.getBigOrders().stream()
                            .map(LongIndexBase::getId).collect(Collectors.toList());

            final List<Segmentation> segmentationList = Hierarchy.get()
                    .getResourceSegmentationReader().getResourceSegmentations(resource)
                    .stream().map(ResourceSegmentation::getSegmentation).collect(Collectors.toList());
            final List<CampaignResource.Segmentation> segmentationsSettings = new ArrayList<>();
            segmentationList.forEach(segmentation -> {
                final Set<Segment> segments = Hierarchy.get().getSegmentReader().get(segmentation);
                segmentationsSettings.add(new CampaignResource.Segmentation(segmentation.getId(),
                        segments.stream().filter(seg -> !segmentRestriction.isPresent() || seg.getKey().getPublicKey().equals(segmentRestriction.get()))
                                .map(LongIndexBase::getId).collect(Collectors.toList())));
            });
            final List<CampaignResource.SegmentsBigOrders> segmentBigOrders;
            if (segmentsBigOrderRestriction.isEmpty()) {
                segmentBigOrders = prepareSegmentBigOrders(segmentationList, bigOrders);
            } else {
                segmentBigOrders = new ArrayList<>();
                segmentsBigOrderRestriction.entrySet().stream().forEach(entry ->
                        segmentBigOrders.add(new CampaignResource.SegmentsBigOrders(entry.getKey(), entry.getValue())));
            }
            final CampaignResource campaignResource = new CampaignResource(campaign.getId(), resource.getId(),
                    false, false, null, new CampaignResource.Settings(segmentationsSettings,
                    bigOrders, segmentBigOrders));
            campaignResourceDao.getByResourceId(campaignResource.getResourceId()).stream()
                    .filter(r -> r.getCampaignId() == campaignResource.getCampaignId()).forEach(r -> campaignResourceDao.delete(r));
            campaignResourceDao.create(campaignResource);
        });
    }

    @Test
    public void quotaRequestShouldHavePermissionIfQuotaAllocationIsAvailable() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());
        Campaign campaign = campaignDao.getLastActive().orElseThrow();
        Campaign aggregatedCampaign = campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setKey("test-aggregated")
                .setName("test-aggregated")
                .setType(Campaign.Type.AGGREGATED).build());
        prepareCampaignResources();

        updateHierarchy();

        final DiQuotaChangeRequest gencfgReq = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(GENCFG, GENCFG_SEGMENT_CPU, bigOrder.getId(),
                                Set.of(DC_SEGMENT_1, GENCFG_SEGMENT_SEGMENT_1), DiAmount.of(100L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), aggregatedCampaign.getId())
                .performBy(AMOSOV_F).getFirst();

        assertFalse(gencfgReq.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        final QuotaChangeRequest origGencfgReq = quotaChangeRequestDao.read(gencfgReq.getId());
        quotaChangeRequestDao.update(origGencfgReq.copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());

        quotaChangeRequestDao.updateChanges(Collections.singletonList(origGencfgReq.getChanges().iterator().next().copyBuilder()
                .amountReady(50_000L)
                .build()));

        assertThrowsForbiddenWithMessage(() -> {
            createAuthorizedLocalClient(AMOSOV_F)
                    .path("/v1/resource-preorder/" + gencfgReq.getId() + "/request-allocation")
                    .post(null, DiResponse.class);
        }, "Manual allocation unavailable for service 'GenCfg'");

        Service rtmr = Service.withKey("rtmr")
                .withName("RTMR")
                .withPriority(42)
                .withAbcServiceId(SAAS_ABC_SERVICE_ID)
                .withSettings(Service.Settings.builder()
                        .accountActualValuesInQuotaDistribution(true)
                        .usesProjectHierarchy(true)
                        .manualQuotaAllocation(true)
                        .build())
                .build();
        serviceDao.create(rtmr);
        updateHierarchy();
        rtmr = serviceDao.read("rtmr");

        final Resource cpu = resourceDao.create(new Resource.Builder("cpu", rtmr)
                .priority(42)
                .noGroup()
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .name("CPU")
                .description("CPU")
                .build()
        );
        updateHierarchy();
        prepareCampaignResources();

        DiQuotaChangeRequest req = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes("rtmr", cpu.getPublicKey(), bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), aggregatedCampaign.getId())
                .performBy(AMOSOV_F).getFirst();

        assertFalse(req.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        final WebClient client = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/" + req.getId() + "/request-allocation");

        assertThrowsForbiddenWithMessage(() -> {
            client.post(null, DiResponse.class);
        }, "Only request with status 'CONFIRMED' can be allocated manually");

        quotaChangeRequestDao.update(quotaChangeRequestDao.read(req.getId()).copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());

        req = dispenser().quotaChangeRequests()
                .byId(req.getId())
                .get()
                .perform();

        assertFalse(req.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        final long mdbReqId = req.getId();
        req = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + req.getId())
                .get(DiQuotaChangeRequest.class);

        assertFalse(req.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        assertThrowsForbiddenWithMessage(() -> {
            client.post(null, DiResponse.class);
        }, "No resources available for allocation in request");

        final QuotaChangeRequest request = quotaChangeRequestDao.read(req.getId());
        quotaChangeRequestDao.updateChanges(ImmutableList.of(
                request.getChanges().get(0).copyBuilder()
                        .amountReady(1_000)
                        .build()
        ));

        assertThrowsForbiddenWithMessage(() -> {
            createAuthorizedLocalClient(BINARY_CAT)
                    .path("/v1/resource-preorder/" + mdbReqId + "/request-allocation")
                    .post(null, DiResponse.class);
        }, "Only request author or quota manager can allocate quota");


        req = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + req.getId())
                .get(DiQuotaChangeRequest.class);

        assertTrue(req.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        final Response response = client.post(null);
        assertEquals(200, response.getStatus());

        req = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + req.getId())
                .get(DiQuotaChangeRequest.class);

        assertFalse(req.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        assertThrowsForbiddenWithMessage(() -> {
            client.post(null, DiResponse.class);
        }, "Quota allocation already requested");
    }

    @Test
    public void requestCanBeCancelledInCompletedStatus() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey(project.getPublicKey())
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(MDB, NEW_RESOURCE_KEY, bigOrder.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.TBPS))
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F)
                .getFirst();

        quotaChangeRequestDao.update(quotaChangeRequestDao.read(request.getId()).copyBuilder()
                .status(QuotaChangeRequest.Status.COMPLETED)
                .build());

        final DiQuotaChangeRequest fetchetRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + request.getId())
                .get(DiQuotaChangeRequest.class);

        assertEquals(DiQuotaChangeRequest.Status.COMPLETED, fetchetRequest.getStatus());
        assertFalse(fetchetRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_CANCEL));

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .setStatus(DiQuotaChangeRequest.Status.CANCELLED)
                    .performBy(AMOSOV_F);
        }, "Can't change status to CANCELLED.");

    }

    @Test
    public void requestCanNotBeCancelledWhileQuotaDeliveryIsInProgress() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey(project.getPublicKey())
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(MDB, NEW_RESOURCE_KEY, bigOrder.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.TBPS))
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F)
                .getFirst();

        final DiQuotaChangeRequest requestNoDelivery = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + request.getId())
                .get(DiQuotaChangeRequest.class);

        assertEquals(DiQuotaChangeRequest.Status.NEW, requestNoDelivery.getStatus());
        assertTrue(requestNoDelivery.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_CANCEL));

        final BigOrder bigOrderLoaded = Objects.requireNonNull(bigOrderManager.getById(bigOrder.getId()));
        final Service service = Hierarchy.get().getServiceReader().read(MDB);
        final Resource resource = Hierarchy.get().getResourceReader().read(new Resource.Key(NEW_RESOURCE_KEY, service));
        final long changeId = quotaChangeRequestDao.read(request.getId()).getChanges().get(0).getId();
        quotaChangeRequestDao.setChangesReadyAmount(ImmutableMap.of(changeId, DiUnit.BPS.convert(5, DiUnit.TBPS)));
        quotaChangeRequestDao.setChangesAllocatedAmount(ImmutableMap.of(changeId, DiUnit.BPS.convert(5, DiUnit.TBPS)));
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(request.getId()).copyBuilder()
                .changes(ImmutableList.of(QuotaChangeRequest.Change.builder()
                        .resource(resource)
                        .segments(Collections.emptySet())
                        .order(new QuotaChangeRequest.BigOrder(bigOrderLoaded.getId(), bigOrderLoaded.getDate(), true))
                        .id(changeId)
                        .amount(DiUnit.BPS.convert(10, DiUnit.TBPS))
                        .amountReady(DiUnit.BPS.convert(5, DiUnit.TBPS))
                        .amountAllocated(DiUnit.BPS.convert(5, DiUnit.TBPS))
                        .amountAllocating(DiUnit.BPS.convert(5, DiUnit.TBPS))
                        .owningCost(BigDecimal.ZERO)
                        .build()))
                .build());

        final DiQuotaChangeRequest requestWithDelivery = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + request.getId())
                .get(DiQuotaChangeRequest.class);

        assertEquals(DiQuotaChangeRequest.Status.NEW, requestWithDelivery.getStatus());
        assertFalse(requestWithDelivery.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_CANCEL));

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .setStatus(DiQuotaChangeRequest.Status.CANCELLED)
                    .performBy(AMOSOV_F);
        }, "Can't change status to CANCELLED.");
    }

    @Test
    public void requestCanNotBeRejectedWhileQuotaDeliveryIsInProgress() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey(project.getPublicKey())
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(MDB, NEW_RESOURCE_KEY, bigOrder.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.TBPS))
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F)
                .getFirst();

        quotaChangeRequestDao.update(quotaChangeRequestDao.read(request.getId()).copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());

        final DiQuotaChangeRequest requestNoDelivery = createAuthorizedLocalClient(KEYD)
                .path("/v1/quota-requests/" + request.getId())
                .get(DiQuotaChangeRequest.class);

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestNoDelivery.getStatus());
        assertTrue(requestNoDelivery.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_REJECT));

        final BigOrder bigOrderLoaded = Objects.requireNonNull(bigOrderManager.getById(bigOrder.getId()));
        final Service service = Hierarchy.get().getServiceReader().read(MDB);
        final Resource resource = Hierarchy.get().getResourceReader().read(new Resource.Key(NEW_RESOURCE_KEY, service));
        final long changeId = quotaChangeRequestDao.read(request.getId()).getChanges().get(0).getId();
        quotaChangeRequestDao.setChangesReadyAmount(ImmutableMap.of(changeId, DiUnit.BPS.convert(5, DiUnit.TBPS)));
        quotaChangeRequestDao.setChangesAllocatedAmount(ImmutableMap.of(changeId, DiUnit.BPS.convert(5, DiUnit.TBPS)));
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(request.getId()).copyBuilder()
                .changes(ImmutableList.of(QuotaChangeRequest.Change.builder()
                        .resource(resource)
                        .segments(Collections.emptySet())
                        .order(new QuotaChangeRequest.BigOrder(bigOrderLoaded.getId(), bigOrderLoaded.getDate(), true))
                        .id(changeId)
                        .amount(DiUnit.BPS.convert(10, DiUnit.TBPS))
                        .amountReady(DiUnit.BPS.convert(5, DiUnit.TBPS))
                        .amountAllocated(DiUnit.BPS.convert(5, DiUnit.TBPS))
                        .amountAllocating(DiUnit.BPS.convert(5, DiUnit.TBPS))
                        .owningCost(BigDecimal.ZERO)
                        .build()))
                .build());

        final DiQuotaChangeRequest requestWithDelivery = createAuthorizedLocalClient(KEYD)
                .path("/v1/quota-requests/" + request.getId())
                .get(DiQuotaChangeRequest.class);

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestWithDelivery.getStatus());
        assertFalse(requestWithDelivery.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_REJECT));

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .setStatus(DiQuotaChangeRequest.Status.REJECTED)
                    .performBy(KEYD);
        }, "Can't change status to REJECTED.");
    }

    @Test
    public void quotaChangeRequestStatusCanBeSetToReadyForReview() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest nirvanaReq = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .chartLinksAbsenceExplanation("explanation")
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        final ImmutableMap<Long, String> growthAnswers = ImmutableMap.of(
                4L, "yes!",
                5L, "idk!",
                6L, "too!"
        );

        dispenser().quotaChangeRequests()
                .byId(nirvanaReq.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(growthAnswers).build())
                .performBy(AMOSOV_F);


        dispenser().quotaChangeRequests()
                .byId(nirvanaReq.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(AMOSOV_F);
    }

    @Test
    public void quotaChangeRequestStatusCanBeSetToReadyForReviewWithoutAnswers() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        campaignDao.update(campaignDao.getAllSorted(Collections.singleton(Campaign.Status.ACTIVE)).get(0).copyBuilder()
                .setSingleProviderRequestModeEnabled(true)
        .build());
        final DiQuotaChangeRequest nirvanaReq = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        dispenser().quotaChangeRequests()
                .byId(nirvanaReq.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(AMOSOV_F);
    }

    @Test
    public void responsibleShouldReceiveCommentWhenQuotaChangeRequestIsReadyForReview() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .chartLinksAbsenceExplanation("explanation")
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        final ImmutableMap<Long, String> growthAnswers = ImmutableMap.of(
                4L, "yes!",
                5L, "idk!",
                6L, "too!"
        );

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(growthAnswers).build())
                .performBy(AMOSOV_F);

        List<CommentCreate> comments = mockTrackerManager.getIssueComments(request.getTrackerIssueKey());
        assertEquals(0, comments.size());

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(AMOSOV_F);

        comments = mockTrackerManager.getIssueComments(request.getTrackerIssueKey());
        assertEquals(1, comments.size());
        final String comment = comments.get(0).getComment().get();
        assertEquals("Заявка переведена в статус %%Готова к защите%%.\n" +
                "кем:amosov-f была завершена подготовка к защите заявки, теперь её можно подтвердить.\n" +
                "\n" +
                "**Причина заказа ресурсов**\n" +
                "естественный рост\n" +
                "\n" +
                "**Расчёты**\n" +
                "Description for how we calculated required amounts\n" +
                "\n" +
                "**Причина отсутствия графиков**\n" +
                "explanation\n" +
                "\n" +
                "**Какие причины роста?**\n" +
                "yes!\n" +
                "\n" +
                "**Как модерируются рост и потребление ресурсов?**\n" +
                "idk!\n" +
                "\n" +
                "**Что произойдёт с сервисом, если ничего не выделить на указанный рост? Если выделить 50%?**\n" +
                "too!\n", comment);
    }

    @Test
    public void commentPostedOnEditInReadyState() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .chartLinksAbsenceExplanation("explanation")
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(ImmutableMap.of(
                4L, "none",
                5L, "none",
                6L, "none"
        )).build())
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(AMOSOV_F);

        List<CommentCreate> comments = mockTrackerManager.getIssueComments(request.getTrackerIssueKey());
        assertEquals(1, comments.size());
        final CommentCreate commentCreate = comments.get(0);
        String comment = commentCreate.getComment().get();
        assertEquals("Заявка переведена в статус %%Готова к защите%%.\n" +
                "кем:amosov-f была завершена подготовка к защите заявки, теперь её можно подтвердить.\n" +
                "\n" +
                "**Причина заказа ресурсов**\n" +
                "естественный рост\n" +
                "\n" +
                "**Расчёты**\n" +
                "Description for how we calculated required amounts\n" +
                "\n" +
                "**Причина отсутствия графиков**\n" +
                "explanation\n"+
                "\n" +
                "**Какие причины роста?**\n" +
                "none\n" +
                "\n" +
                "**Как модерируются рост и потребление ресурсов?**\n" +
                "none\n" +
                "\n" +
                "**Что произойдёт с сервисом, если ничего не выделить на указанный рост? Если выделить 50%?**\n" +
                "none\n", comment
                );

        final ListF<String> summonees = commentCreate.getSummonees();
        assertEquals(1, summonees.size());
        assertEquals("whistler", summonees.get(0));

        final ImmutableMap<Long, String> growthAnswers = ImmutableMap.of(
                4L, "yes!",
                5L, "idk!",
                6L, "too!"
        );

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(growthAnswers).build())
                .performBy(AMOSOV_F);

        comments = mockTrackerManager.getIssueComments(request.getTrackerIssueKey());
        assertEquals(3, comments.size());
        comment = comments.get(1).getComment().get();
        assertEquals("Ответы на вопросы были изменены кем:amosov-f.\n" +
                "\n" +
                "**Причина заказа ресурсов**\n" +
                "естественный рост\n" +
                "\n" +
                "**Расчёты**\n" +
                "Description for how we calculated required amounts\n" +
                "\n" +
                "**Причина отсутствия графиков**\n" +
                "explanation\n" +
                "\n" +
                "**Какие причины роста?**\n" +
                "yes!\n" +
                "\n" +
                "**Как модерируются рост и потребление ресурсов?**\n" +
                "idk!\n" +
                "\n" +
                "**Что произойдёт с сервисом, если ничего не выделить на указанный рост? Если выделить 50%?**\n" +
                "too!\n", comment);

        comment = comments.get(2).getComment().get();
        assertEquals("Описание изменено кем:amosov-f\n" +
                "\n" +
                "Измененная кем:amosov-f заявка ожидает рассмотрения.", comment);
    }

    @Test
    public void commentNotPostedOnEditInNotReadyState() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        final ImmutableMap<Long, String> growthAnswers = ImmutableMap.of(
                4L, "yes!",
                5L, "idk!",
                6L, "too!"
        );

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(growthAnswers).build())
                .performBy(AMOSOV_F);

        final List<CommentCreate> comments = mockTrackerManager.getIssueComments(request.getTrackerIssueKey());
        assertEquals(0, comments.size());
    }

    @Test
    public void inOldModeCampaignCanBeExistsOnlySingleProviderRequests() {
        Campaign campaign = campaignDao.getAllSorted(Collections.singleton(Campaign.Status.ACTIVE)).iterator().next();
        campaignDao.delete(campaign);
        BigOrder.Builder anotherBigOrderBuilder = BigOrder.builder(LocalDate.of(2020, 4, 1));
        BigOrder anotherBigOrder = bigOrderManager.create(anotherBigOrderBuilder);
        campaign = campaignDao.create(campaign.copyBuilder()
                .setBigOrders(Arrays.asList(
                        new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 1, 1)),
                        new Campaign.BigOrder(anotherBigOrder)
                ))
                .build());
        prepareCampaignResources();

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey(project.getPublicKey())
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(MDB, NEW_RESOURCE_KEY, bigOrder.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.TBPS))
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(12, DiUnit.KILO))
                        .changes(NIRVANA, YT_CPU, anotherBigOrder.getId(), Collections.emptySet(), DiAmount.of(12, DiUnit.KILO))
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(WHISTLER);

        campaignDao.update(campaign.copyBuilder()
                .setSingleProviderRequestModeEnabled(true)
                .build());

        assertThrowsWithMessage(() -> {

            dispenser()
                    .quotaChangeRequests()
                    .create(new Body.BodyBuilder()
                            .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                            .projectKey(project.getPublicKey())
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                            .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                            .changes(MDB, NEW_RESOURCE_KEY, bigOrder.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.TBPS))
                            .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(12, DiUnit.KILO))
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                            .build(), null)
                    .performBy(WHISTLER);
        }, "Requests with multiple services can't be created in campaign 'test-campaign'");

        assertThrowsWithMessage(() -> {

            dispenser()
                    .quotaChangeRequests()
                    .create(new Body.BodyBuilder()
                            .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                            .projectKey(project.getPublicKey())
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                            .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                            .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(12, DiUnit.KILO))
                            .changes(NIRVANA, YT_CPU, anotherBigOrder.getId(), Collections.emptySet(), DiAmount.of(12, DiUnit.KILO))
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                            .build(), null)
                    .performBy(WHISTLER);
        }, "Requests with multiple big orders can't be created in campaign 'test-campaign'");

        final long singleProviderRequestId = dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey(project.getPublicKey())
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(12, DiUnit.KILO))
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(WHISTLER)
                .getFirst().getId();

        assertThrowsWithMessage(() -> {

            dispenser().quotaChangeRequests()
                    .byId(singleProviderRequestId)
                    .update(new BodyUpdate.BodyUpdateBuilder()
                            .changes(MDB, NEW_RESOURCE_KEY, bigOrder.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.TBPS))
                            .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(12, DiUnit.KILO))
                            .build())
                    .performBy(WHISTLER);

        }, "Requests with multiple services can't be created in campaign 'test-campaign'");

        assertThrowsWithMessage(() -> {

            dispenser().quotaChangeRequests()
                    .byId(singleProviderRequestId)
                    .update(new BodyUpdate.BodyUpdateBuilder()
                            .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(12, DiUnit.KILO))
                            .changes(NIRVANA, YT_CPU, anotherBigOrder.getId(), Collections.emptySet(), DiAmount.of(12, DiUnit.KILO))
                            .build())
                    .performBy(WHISTLER);

        }, "Requests with multiple big orders can't be created in campaign 'test-campaign'");

        dispenser().quotaChangeRequests()
                .byId(singleProviderRequestId)
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(MDB, NEW_RESOURCE_KEY, bigOrder.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.TBPS))
                        .build())
                .performBy(WHISTLER);
    }


    @Test
    public void projectMemberCanUpdateReviewPopupFields() {
        createProject(TEST_PROJECT_KEY, YANDEX, SLONNN.getLogin());

        dispenser().project(TEST_PROJECT_KEY)
                .members()
                .attach(LOTREK.getLogin())
                .attach(TERRY.getLogin())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(LOTREK).getFirst();

        Set<DiQuotaChangeRequest.Permission> permissions = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + request.getId())
                .get(DiQuotaChangeRequest.class)
                .getPermissions();

        assertTrue(permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT));
        assertTrue(permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP));

        permissions = createAuthorizedLocalClient(TERRY)
                .path("/v1/quota-requests/" + request.getId())
                .get(DiQuotaChangeRequest.class)
                .getPermissions();

        assertTrue(permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT));
        assertTrue(permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP));

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .calculations("3/2")
                        .build())
                .performBy(TERRY);

        final String calculations = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform()
                .getCalculations();

        assertEquals("3/2", calculations);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .calculations("4/3")
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.COUNT))
                        .build())
                .performBy(TERRY);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .calculations("4/3")
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.COUNT))
                        .build())
                .performBy(LOTREK);

        Campaign campaign = campaignDao.getAllSorted(Collections.singleton(Campaign.Status.ACTIVE)).iterator().next();
        campaignDao.update(campaign.copyBuilder()
                .setRequestModificationDisabled(true)
                .build());

        permissions = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + request.getId())
                .get(DiQuotaChangeRequest.class)
                .getPermissions();

        assertFalse(permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT));
        assertFalse(permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP));


        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .update(new BodyUpdate.BodyUpdateBuilder()
                            .calculations("5/4")
                            .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(300L, DiUnit.COUNT))
                            .build())
                    .performBy(LOTREK);

        }, "Request can't be updated at this campaign stage");

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .update(new BodyUpdate.BodyUpdateBuilder()
                            .calculations("6/5")
                            .build())
                    .performBy(LOTREK);
        }, "Request can't be updated at this campaign stage");

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .update(new BodyUpdate.BodyUpdateBuilder()
                            .calculations("7/6")
                            .build())
                    .performBy(TERRY);
        }, "Request can't be updated at this campaign stage");
    }

    @Test
    public void reviewPopupFieldsShouldBeRequiredInReadyForReviewStatus() {
        createProject(TEST_PROJECT_KEY, YANDEX, SLONNN.getLogin());

        dispenser().project(TEST_PROJECT_KEY)
                .members()
                .attach(LOTREK.getLogin())
                .attach(TERRY.getLogin())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .calculations("7/0")
                        .build(), null)
                .performBy(LOTREK).getFirst();

        final ImmutableList<Pair<BodyUpdate, BodyUpdate>> fields = ImmutableList.of(
                Pair.of(
                        new BodyUpdate.BodyUpdateBuilder()
                                .calculations("")
                                .build(),
                        new BodyUpdate.BodyUpdateBuilder()
                                .calculations("778")
                                .build()
                ),
                Pair.of(
                        new BodyUpdate.BodyUpdateBuilder()
                                .requestGoalAnswers(Collections.singletonMap(5L, "ok"))
                                .build(),
                        new BodyUpdate.BodyUpdateBuilder()
                                .requestGoalAnswers(ImmutableMap.of(4L, "4", 5L, "5", 6L, "6"))
                                .build()
                ),
                Pair.of(
                        new BodyUpdate.BodyUpdateBuilder()
                                .chartLink(Collections.emptyList())
                                .chartLinksAbsenceExplanation("")
                                .build(),
                        new BodyUpdate.BodyUpdateBuilder()
                                .chartLink(Collections.emptyList())
                                .chartLinksAbsenceExplanation("idk")
                                .build()
                ),
                Pair.of(
                        new BodyUpdate.BodyUpdateBuilder()
                                .chartLink(Collections.emptyList())
                                .chartLinksAbsenceExplanation("")
                                .build(),
                        new BodyUpdate.BodyUpdateBuilder()
                                .chartLink(Collections.singletonList("https://dispenser.yandex-team.ru"))
                                .chartLinksAbsenceExplanation("")
                                .build()
                )
        );

        for (final Pair<BodyUpdate, BodyUpdate> field : fields) {
            quotaChangeRequestDao.update(quotaChangeRequestDao.read(request.getId()).copyBuilder()
                    .status(QuotaChangeRequest.Status.NEW)
                    .chartLinks(Collections.emptyList())
                    .chartLinksAbsenceExplanation(null)
                    .build());

            dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .update(field.getLeft())
                    .performBy(AMOSOV_F);

            quotaChangeRequestDao.update(quotaChangeRequestDao.read(request.getId()).copyBuilder()
                    .status(QuotaChangeRequest.Status.READY_FOR_REVIEW)
                    .build());

            dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .update(field.getRight())
                    .performBy(AMOSOV_F);
        }
    }

    @Test
    public void requestWithoutPopupFieldsCanBeMarkedAsReadyForReview() {
        createProject(TEST_PROJECT_KEY, YANDEX, SLONNN.getLogin());

        dispenser().project(TEST_PROJECT_KEY)
                .members()
                .attach(LOTREK.getLogin())
                .attach(TERRY.getLogin())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .calculations("")
                        .comment("")
                        .chartLink(Collections.emptyList())
                        .chartLinksAbsenceExplanation("")
                        .build(), null)
                .performBy(LOTREK).getFirst();

        final Set<DiQuotaChangeRequest.Permission> permissions = createAuthorizedLocalClient(TERRY)
                .path("/v1/quota-requests/" + request.getId())
                .get(DiQuotaChangeRequest.class)
                .getPermissions();

        assertTrue(permissions.contains(DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW));

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(Collections.emptyMap())
                        .build())
                .performBy(TERRY);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(ImmutableMap.of(4L, "6", 5L, "5", 6L, "6"))
                        .build())
                .performBy(TERRY);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .calculations("easy")
                        .build())
                .performBy(TERRY);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .chartLinksAbsenceExplanation("no")
                        .build())
                .performBy(TERRY);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(TERRY);
    }

}
