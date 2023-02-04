package ru.yandex.qe.dispenser.ws.logic;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.GenericType;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import ru.yandex.inside.goals.model.Goal.Importance;
import ru.yandex.inside.goals.model.Goal.Status;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiOrder;
import ru.yandex.qe.dispenser.api.v1.DiPerformer;
import ru.yandex.qe.dispenser.api.v1.DiPersonGroup;
import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiQuotaKey;
import ru.yandex.qe.dispenser.api.v1.DiQuotaRequestHistoryEventType;
import ru.yandex.qe.dispenser.api.v1.DiQuotaSpec;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiSegmentation;
import ru.yandex.qe.dispenser.api.v1.DiSetAmountResult;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.project.DiExtendedProject;
import ru.yandex.qe.dispenser.api.v1.request.quota.Body;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.api.v1.request.quota.ChangeBody;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.client.v1.impl.SpyWebClient;
import ru.yandex.qe.dispenser.domain.BotCampaignGroup;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.CampaignForBot;
import ru.yandex.qe.dispenser.domain.CampaignUpdate;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequestHistoryEvent;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.ResourceGroup;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.abc.AbcService;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.bot.MappedPreOrder;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.BotPreOrderDao;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.MappedPreOrderDao;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.change.BotPreOrderChangeDao;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.dispenser_admins.DispenserAdminsDao;
import ru.yandex.qe.dispenser.domain.dao.goal.Goal;
import ru.yandex.qe.dispenser.domain.dao.goal.OkrAncestors;
import ru.yandex.qe.dispenser.domain.dao.history.request.QuotaChangeRequestHistoryDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.group.ResourceGroupDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.hierarchy.HierarchySupplier;
import ru.yandex.qe.dispenser.domain.hierarchy.Role;
import ru.yandex.qe.dispenser.domain.util.RelativePage;
import ru.yandex.qe.dispenser.domain.util.RelativePageInfo;
import ru.yandex.qe.dispenser.solomon.SolomonHolder;
import ru.yandex.qe.dispenser.standalone.MockTrackerManager;
import ru.yandex.qe.dispenser.ws.admin.RequestAdminService;
import ru.yandex.qe.dispenser.ws.api.BotCampaignGroupServiceApiTest;
import ru.yandex.qe.dispenser.ws.history.QuotaChangeRequestHistoryFilterImpl;
import ru.yandex.qe.dispenser.ws.quota.request.QuotaChangeRequestManager;
import ru.yandex.qe.dispenser.ws.quota.request.ReCreateRequestTicketsTask;
import ru.yandex.qe.dispenser.ws.quota.request.SetResourceAmountBody;
import ru.yandex.qe.dispenser.ws.quota.request.ticket.QuotaChangeRequestTicketManager;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.ServiceRef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.api.v1.project.DiExtendedProject.Permission.CAN_CREATE_QUOTA_REQUEST;


public class QuotaChangeRequestValidationTest extends BaseQuotaRequestTest {
    public static final String TEST_DESCRIPTION = "For needs.";
    public static final String TEST_COMMENT = "Some optional info.";
    public static final String TEST_CALCULATIONS = "Description for how we calculated required amounts";
    public static final String TEST_SUMMARY = "test";
    public static final int TEST_YANDEX_ABC_SERVICE_ID = 987;
    public static final int TEST_SEARCH_ABC_SERVICE_ID = 789;
    public static final long TEST_SECOND_GOAL_ID = 2;
    public static final long TEST_INVALID_GOAL_ID = 999;
    public static final long TEST_PRIVATE_GOAL_ID = 1000;
    public static final long TEST_CANCELLED_GOAL_ID = 1001;
    public static final long TEST_GOAL_BATCH_ONE_ID = 2002;
    public static final long TEST_GOAL_BATCH_TWO_ID = 3003;
    private static final LocalDate TEST_BIG_ORDER_INVALID_DATE = LocalDate.of(2019, Month.JANUARY, 1);
    private static final String TEST_CAMPAIGN_KEY = "test-campaign";
    @Value("#{${tracker.components}}")
    Map<String, String> trackerComponents;

    private static final String YT = "yt";
    private static final String CPU_YT = "cpu_yt";
    private static final String DBAAS = "dbaas";
    private static final String MDB_HDD_CH = "hdd_dbaas_clickhouse";
    private static final String RESOURCE_GROUP_MDB_CLICKHOUSE = "dbaas_clickhouse";
    private static final String DEFAULT_GROUP_KEY = "default";

    public static final ImmutableMap<Long, String> GOAL_ANSWER = ImmutableMap.of(
            0L, "fine!",
            1L, "good!",
            2L, "done!"
    );

    public static final ImmutableMap<Long, String> GROWTH_ANSWER = ImmutableMap.of(
            4L, "fine!",
            5L, "good!",
            6L, "done!"
    );

    private static Long BIG_ORDER_ONE_ID;

    @Autowired
    private MockTrackerManager trackerManager;

    @Autowired
    private QuotaChangeRequestTicketManager quotaChangeRequestTicketManager;

    @Autowired
    private QuotaChangeRequestDao quotaChangeRequestDao;

    @Autowired
    private BotPreOrderChangeDao botPreOrderChangeDao;

    @Autowired
    private MappedPreOrderDao mappedPreOrderDao;
    @Autowired
    private BotPreOrderDao botPreOrderDao;

    @Autowired
    private ResourceGroupDao resourceGroupDao;

    @Autowired
    private QuotaChangeRequestManager quotaChangeRequestManager;

    @Autowired
    private SolomonHolder solomonHolder;

    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;

    @Autowired
    private QuotaChangeRequestHistoryDao quotaChangeRequestHistoryDao;

    @Autowired
    private DispenserAdminsDao dispenserAdminsDao;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private ResourceDao resourceDao;

    @Autowired
    private ServiceDao serviceDao;

    @Autowired
    private HierarchySupplier hierarchySupplier;
    private ReCreateRequestTicketsTask reCreateRequestTicketsTask;
    private Campaign campaign;
    private Project project;
    private BotCampaignGroup botCampaignGroup;
    private BigOrder invalidBigOrder;
    private BigOrder secondBigOrder;
    private BigOrder thirdBigOrder;

    public static Campaign.Builder defaultCampaignBuilder(BigOrder bigOrder) {
        return Campaign.builder()
                .setKey(TEST_CAMPAIGN_KEY)
                .setName("Test")
                .setStatus(Campaign.Status.ACTIVE)
                .setStartDate(TEST_BIG_ORDER_DATE)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), TEST_BIG_ORDER_DATE)))
                .setRequestCreationDisabled(false)
                .setRequestModificationDisabledForNonManagers(false)
                .setAllowedRequestModificationWhenClosed(false)
                .setAllowedModificationOnMissingAdditionalFields(false)
                .setForcedAllowingRequestStatusChange(false)
                .setAllowedRequestCreationForProviderAdmin(false)
                .setSingleProviderRequestModeEnabled(false)
                .setAllowedRequestCreationForCapacityPlanner(false)
                .setType(Campaign.Type.DRAFT)
                .setRequestModificationDisabled(false);
    }

    public static BotCampaignGroup.Builder defaultCampaignGroupBuilder() {
        return BotCampaignGroup.builder()
                .setKey("test_campaign_group")
                .setName("Test Campaign Group")
                .setActive(true)
                .setBotPreOrderIssueKey("DISPENSERREQ-1");
    }

    @NotNull
    public static Body.BodyBuilder requestBodyBuilderWithDefaultFields() {
        return new Body.BodyBuilder()
                .summary(TEST_SUMMARY)
                .description(TEST_DESCRIPTION)
                .calculations(TEST_CALCULATIONS);
    }

    public static Object[][] quotaKeysBuilders() {
        return new Object[][]{{
                new DiQuotaKey.Builder()
                        .serviceKey(MDB)
                        .resourceKey(CPU)
                        .quotaSpecKey(CPU)
        }, {
                new DiQuotaKey.Builder()
                        .serviceKey(YP)
                        .resourceKey(SEGMENT_CPU)
                        .quotaSpecKey(SEGMENT_CPU)
                        .segmentKeys(ImmutableSet.of(SEGMENT_SEGMENT_2, DC_SEGMENT_3))
        }
        };
    }

    @Override
    @BeforeAll
    public void beforeClass() {
        super.beforeClass();
        BIG_ORDER_ONE_ID = bigOrderOne.getId();
        invalidBigOrder = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_INVALID_DATE));
        secondBigOrder = bigOrderManager.create(BigOrder.builder(LocalDate.of(2119, 1, 1)));
        thirdBigOrder = bigOrderManager.create(BigOrder.builder(LocalDate.of(2119, 2, 1)));

        goalDao.create(new Goal(TEST_SECOND_GOAL_ID, "Normal Goal", ru.yandex.inside.goals.model.Goal.Importance.DEPARTMENT,
                ru.yandex.inside.goals.model.Goal.Status.NEW, OkrAncestors.EMPTY));
        goalDao.create(new Goal(TEST_PRIVATE_GOAL_ID, "Private Goal", ru.yandex.inside.goals.model.Goal.Importance.PRIVATE,
                ru.yandex.inside.goals.model.Goal.Status.NEW, OkrAncestors.EMPTY));
        goalDao.create(new Goal(TEST_CANCELLED_GOAL_ID, "Cancelled Goal", ru.yandex.inside.goals.model.Goal.Importance.DEPARTMENT,
                ru.yandex.inside.goals.model.Goal.Status.CANCELLED, OkrAncestors.EMPTY));

        goalDao.create(new Goal(TEST_GOAL_BATCH_ONE_ID, "Normal Goal one", Importance.DEPARTMENT, Status.NEW, OkrAncestors.EMPTY));
        goalDao.create(new Goal(TEST_GOAL_BATCH_TWO_ID, "Normal Goal two", Importance.COMPANY, Status.NEW, OkrAncestors.EMPTY));
    }

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
        botCampaignGroup = botCampaignGroupDao.create(
                defaultCampaignGroupBuilder()
                        .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrderOne.getId())))
                        .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values().iterator().next())
                        .build()
        );

        newCampaignId = campaign.getId();

        reCreateRequestTicketsTask = new ReCreateRequestTicketsTask(quotaChangeRequestDao, quotaChangeRequestTicketManager, solomonHolder,
                quotaChangeRequestManager, hierarchySupplier, ROBOT_DISPENSER);
        prepareCampaignResources();
    }


    @MethodSource("quotaKeysBuilders")
    @ParameterizedTest(name = "[{index}] {displayName}")
    public void quotaRequestCanBeCreated(final DiQuotaKey.Builder quotaKeyBuilder) {

        final DiAmount requestAmount = DiAmount.of(100L, DiUnit.PERCENT_CORES);

        final Instant beforeRequest = Instant.now();

        final DiPerson responsible = BINARY_CAT;

        createProject(TEST_PROJECT_KEY, YANDEX, responsible.getLogin());

        final DiQuotaKey quotaKey = quotaKeyBuilder.projectKey(TEST_PROJECT_KEY).build();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .changes(quotaKey, bigOrderOne.getId(), requestAmount)
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(responsible);

        assertEquals(1, requests.size());
        final DiQuotaChangeRequest request = requests.getFirst();

        final Instant afterRequest = Instant.now();

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertEquals(TEST_DESCRIPTION, fetchedRequest.getDescription());
        assertEquals(TEST_CALCULATIONS, fetchedRequest.getCalculations());

        assertNull(fetchedRequest.getComment());

        final List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        assertEquals(1, changes.size());

        final DiQuotaChangeRequest.Change change = changes.iterator().next();


        final long convertedAmount = DiUnit.PERCENT_CORES.convert(change.getAmount());
        assertEquals(100L, convertedAmount);

        assertEquals(fetchedRequest.getAuthor(), responsible.getLogin());
        assertEquals(fetchedRequest.getResponsible(), BINARY_CAT.getLogin());
        assertEquals(DiQuotaChangeRequest.Status.NEW, fetchedRequest.getStatus());
        assertEquals(fetchedRequest.getProject().getKey(), quotaKey.getProjectKey());
        assertEquals(DiQuotaChangeRequest.Type.RESOURCE_PREORDER, fetchedRequest.getType());
        assertEquals(TEST_SUMMARY, fetchedRequest.getSummary());
        assertNull(fetchedRequest.getSourceProject());
        assertEquals(change.getResource().getKey(), quotaKey.getResourceKey());
        assertEquals(change.getSegmentKeys(), quotaKey.getSegmentKeys());
        assertEquals(change.getService().getKey(), quotaKey.getServiceKey());

        final long created = fetchedRequest.getCreated();
        assertTrue(created > beforeRequest.toEpochMilli() && created < afterRequest.toEpochMilli());
        final long updated = fetchedRequest.getUpdated();
        assertTrue(updated > beforeRequest.toEpochMilli() && updated <= afterRequest.toEpochMilli());

        assertNotNull(fetchedRequest.getTrackerIssueKey());
    }


    @Test
    public void quotaRequestCanBeCancelledByOwner() {

        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiQuotaKey quotaKey = new DiQuotaKey.Builder()
                .serviceKey(NIRVANA)
                .resourceKey(YT_CPU)
                .quotaSpecKey(YT_CPU)
                .projectKey(TEST_PROJECT_KEY)
                .build();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .changes(quotaKey, bigOrderOne.getId(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .setStatus(DiQuotaChangeRequest.Status.CANCELLED)
                .performBy(BINARY_CAT);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.CANCELLED, fetchedRequest.getStatus());
    }

    @Test
    public void resourceRequestCanBeCreatedByProcessResponsible() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final Person person = Hierarchy.get().getPersonReader().readPersonByLogin(SLONNN.getLogin());
        projectDao.attach(person, Hierarchy.get().getProjectReader().getRoot(), Role.PROCESS_RESPONSIBLE);

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(SLONNN);

        Assertions.assertEquals(1, quotaRequests.size());
    }

    @Test
    public void resourceRequestCanBeCancelledByProcessResponsible() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        final Person person = Hierarchy.get().getPersonReader().readPersonByLogin(SLONNN.getLogin());
        projectDao.attach(person, Hierarchy.get().getProjectReader().getRoot(), Role.PROCESS_RESPONSIBLE);

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .setStatus(DiQuotaChangeRequest.Status.CANCELLED)
                .performBy(SLONNN);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.CANCELLED, fetchedRequest.getStatus());
    }

    @Test
    public void quotaRequestWithTypeResourcePreorder() {
        final Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        final DiQuotaChangeRequest requestByCreator = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertEquals(requestByCreator.getResponsible(), BINARY_CAT.getLogin());
        assertEquals(Sets.immutableEnumSet(
                        DiQuotaChangeRequest.Permission.CAN_CANCEL,
                        DiQuotaChangeRequest.Permission.CAN_EDIT,
                        DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW,
                        DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP,
                        DiQuotaChangeRequest.Permission.CAN_CLOSE_ALLOCATION_NOTE
                ),
                requestByCreator.getPermissions());

        final DiQuotaChangeRequest requestByResponsible = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        final Set<DiQuotaChangeRequest.Permission> permissionsForWhistler = Sets.immutableEnumSet(
                DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_EDIT,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW,
                DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP);
        assertEquals(permissionsForWhistler, requestByResponsible.getPermissions());
        assertEquals(requestByResponsible.getCampaign().getId(), Long.valueOf(activeCampaign.getId()));
        assertEquals(requestByResponsible.getCampaign().getStatus(), activeCampaign.getStatus().name());
        assertEquals(requestByResponsible.getCampaign().getKey(), activeCampaign.getKey());
        assertEquals(requestByResponsible.getCampaign().getName(), activeCampaign.getName());
        final DiQuotaChangeRequest.Change firstChange = requestByResponsible.getChanges().iterator().next();
        assertEquals(bigOrderOne.getId(), firstChange.getOrder().getId());
        assertEquals(firstChange.getOrder().getOrderDate(), activeCampaign.getBigOrders().stream()
                .filter(o -> o.getBigOrderId() == bigOrderOne.getId()).findFirst().get().getDate());

        final DiQuotaChangeRequest requestByOther = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        assertTrue(requestByOther.getPermissions().isEmpty());
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderWithoutDescription() {
        final Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(TEST_SUMMARY)
                        .calculations(TEST_CALCULATIONS)
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        final DiQuotaChangeRequest requestByCreator = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertEquals(Sets.immutableEnumSet(
                        DiQuotaChangeRequest.Permission.CAN_CANCEL,
                        DiQuotaChangeRequest.Permission.CAN_EDIT,
                        DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW,
                        DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP,
                        DiQuotaChangeRequest.Permission.CAN_CLOSE_ALLOCATION_NOTE
                ),
                requestByCreator.getPermissions());

        final DiQuotaChangeRequest requestByResponsible = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        final Set<DiQuotaChangeRequest.Permission> permissionsForWhistler = Sets.immutableEnumSet(
                DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_EDIT,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW,
                DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP);
        assertEquals(permissionsForWhistler, requestByResponsible.getPermissions());
        assertEquals(requestByResponsible.getCampaign().getId(), Long.valueOf(activeCampaign.getId()));
        assertEquals(requestByResponsible.getCampaign().getStatus(), activeCampaign.getStatus().name());
        assertEquals(requestByResponsible.getCampaign().getKey(), activeCampaign.getKey());
        assertEquals(requestByResponsible.getCampaign().getName(), activeCampaign.getName());
        final DiQuotaChangeRequest.Change firstChange = requestByResponsible.getChanges().iterator().next();
        assertEquals(bigOrderOne.getId(), firstChange.getOrder().getId());
        assertEquals(firstChange.getOrder().getOrderDate(), activeCampaign.getBigOrders().stream()
                .filter(o -> o.getBigOrderId() == bigOrderOne.getId()).findFirst().get().getDate());
        assertNull(requestByResponsible.getDescription());
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderEmptyDescription() {
        final Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(TEST_SUMMARY)
                        .description("")
                        .calculations(TEST_CALCULATIONS)
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        final DiQuotaChangeRequest requestByCreator = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertEquals(Sets.immutableEnumSet(
                        DiQuotaChangeRequest.Permission.CAN_CANCEL,
                        DiQuotaChangeRequest.Permission.CAN_EDIT,
                        DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW,
                        DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP,
                        DiQuotaChangeRequest.Permission.CAN_CLOSE_ALLOCATION_NOTE
                ),
                requestByCreator.getPermissions());

        final DiQuotaChangeRequest requestByResponsible = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        final Set<DiQuotaChangeRequest.Permission> permissionsForWhistler = Sets.immutableEnumSet(
                DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_EDIT,
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW,
                DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP);
        assertEquals(permissionsForWhistler, requestByResponsible.getPermissions());
        assertEquals(requestByResponsible.getCampaign().getId(), Long.valueOf(activeCampaign.getId()));
        assertEquals(requestByResponsible.getCampaign().getStatus(), activeCampaign.getStatus().name());
        assertEquals(requestByResponsible.getCampaign().getKey(), activeCampaign.getKey());
        assertEquals(requestByResponsible.getCampaign().getName(), activeCampaign.getName());
        final DiQuotaChangeRequest.Change firstChange = requestByResponsible.getChanges().iterator().next();
        assertEquals(bigOrderOne.getId(), firstChange.getOrder().getId());
        assertEquals(firstChange.getOrder().getOrderDate(), activeCampaign.getBigOrders().stream()
                .filter(o -> o.getBigOrderId() == bigOrderOne.getId()).findFirst().get().getDate());
        assertEquals("", requestByResponsible.getDescription());
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderMissingSummaryForbidden() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .create(new Body.BodyBuilder()
                            .calculations(TEST_CALCULATIONS)
                            .projectKey(TEST_PROJECT_KEY)
                            .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                            .build(), null)
                    .performBy(BINARY_CAT);
        }, "Summary is required");
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderEmptySummaryForbidden() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .create(new Body.BodyBuilder()
                            .summary("")
                            .calculations(TEST_CALCULATIONS)
                            .projectKey(TEST_PROJECT_KEY)
                            .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                            .build(), null)
                    .performBy(BINARY_CAT);
        }, "Non-blank summary is required.");
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderTooLongSummaryForbidden() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .create(new Body.BodyBuilder()
                            .summary(StringUtils.repeat("a", 1001))
                            .calculations(TEST_CALCULATIONS)
                            .projectKey(TEST_PROJECT_KEY)
                            .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                            .build(), null)
                    .performBy(BINARY_CAT);
        }, "Summary must be shorter than 1000 symbols.");
    }

    @Test
    public void quotaRequestShouldBeCreatedForMyProjectsWithoutResources() {
        createProject("L1", YANDEX, "lotrek");
        createProject("L2", "L1", BINARY_CAT.getLogin());
        createProject("L3", "L2", BINARY_CAT.getLogin());

        final DiQuotaKey quotaKey = new DiQuotaKey.Builder()
                .serviceKey(NIRVANA)
                .resourceKey(YT_CPU)
                .quotaSpecKey(YT_CPU)
                .projectKey("L3")
                .build();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .changes(quotaKey, bigOrderOne.getId(), DiAmount.of(10_000L, DiUnit.PERMILLE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(BINARY_CAT);

        assertEquals(1, requests.size());

        assertTrue(requests.stream()
                .allMatch(req -> req.getStatus() == DiQuotaChangeRequest.Status.NEW));
    }

    @Test
    public void quotaRequestShouldBeFilterableByAuthorLoginOrStatus() {
        final String[] authors = {"lotrek", AMOSOV_F.getLogin(), "dm-tim"};
        createProject("L1", YANDEX, BINARY_CAT.getLogin());
        createProject("L2", "L1", authors);

        final DiQuotaKey quotaKey = new DiQuotaKey.Builder()
                .serviceKey(NIRVANA)
                .resourceKey(YT_CPU)
                .quotaSpecKey(YT_CPU)
                .projectKey("L2")
                .build();

        for (final String author : authors) {
            dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .changes(quotaKey, bigOrderOne.getId(), DiAmount.of(10_000, DiUnit.PERMILLE))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .build(), null)
                    .performBy(DiPerson.login(author));
        }

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requestsFolLeafProject = dispenser()
                .quotaChangeRequests()
                .get()
                .query("project", "L2")
                .perform();

        assertTrue(requestsFolLeafProject.isEmpty());

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser()
                .quotaChangeRequests()
                .get()
                .query("project", "L1")
                .perform();

        assertEquals(3, requests.size());

        final DiListResponse<DiQuotaChangeRequest> filteredByAuthorRequests = dispenser()
                .quotaChangeRequests()
                .get()
                .query("project", "L1")
                .query("author", "lotrek")
                .perform();

        assertEquals(1, filteredByAuthorRequests.size());

        final DiListResponse<DiQuotaChangeRequest> filteredByStatusRequests = dispenser()
                .quotaChangeRequests()
                .get()
                .query("project", "L1")
                .query("status", DiQuotaChangeRequest.Status.NEW.name())
                .perform();

        assertEquals(3, filteredByStatusRequests.size());

        final DiListResponse<DiQuotaChangeRequest> filterdByProjectAndStatusMultiple = dispenser()
                .quotaChangeRequests()
                .get()
                .query("project", "L1")
                .query("project", YANDEX)
                .query("status", DiQuotaChangeRequest.Status.NEW.name())
                .perform();

        assertEquals(3, filterdByProjectAndStatusMultiple.size());
    }

    @Test
    public void quotaRequestCanBeCreatedWithoutTicket() {
        createProject("L1", YANDEX, BINARY_CAT.getLogin());

        final DiQuotaKey quotaKey = new DiQuotaKey.Builder()
                .serviceKey(NIRVANA)
                .resourceKey(YT_CPU)
                .quotaSpecKey(YT_CPU)
                .projectKey("L1")
                .build();

        trackerManager.setTrackerAvailable(false);
        final DiListResponse<DiQuotaChangeRequest> requests;
        try {
            requests = dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .changes(quotaKey, bigOrderOne.getId(), DiAmount.of(10_000, DiUnit.PERMILLE))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .build(), null)
                    .performBy(BINARY_CAT);
        } finally {
            trackerManager.setTrackerAvailable(true);
        }

        final DiQuotaChangeRequest request = requests.getFirst();

        assertNull(request.getTrackerIssueKey());
        assertEquals(DiQuotaChangeRequest.Status.NEW, request.getStatus());

        updateHierarchy();

        final DiQuotaChangeRequest updatedRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.CANCELLED)
                .performBy(AMOSOV_F);

        assertEquals(DiQuotaChangeRequest.Status.CANCELLED, updatedRequest.getStatus());
    }


    @Test
    public void ticketCanBeCreatedForRequestWithoutTicket() {
        createProject("L1", YANDEX, BINARY_CAT.getLogin());

        final DiQuotaKey quotaKey = new DiQuotaKey.Builder()
                .serviceKey(NIRVANA)
                .resourceKey(YT_CPU)
                .quotaSpecKey(YT_CPU)
                .projectKey("L1")
                .build();

        trackerManager.setTrackerAvailable(false);
        final DiListResponse<DiQuotaChangeRequest> requests;
        try {
            requests = dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .changes(quotaKey, bigOrderOne.getId(), DiAmount.of(10_000, DiUnit.PERMILLE))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .build(), null)
                    .performBy(BINARY_CAT);
        } finally {
            trackerManager.setTrackerAvailable(true);
        }

        final DiQuotaChangeRequest request = requests.getFirst();

        assertNull(request.getTrackerIssueKey());

        updateHierarchy();

        final DiQuotaChangeRequest updatedRequest = dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .createTicket()
                .performBy(BINARY_CAT);

        assertNotNull(updatedRequest.getTrackerIssueKey());
    }

    @Test
    public void ticketCanBeCreatedInTaskForRequestWithoutTicket() {
        createProject("L1", YANDEX, BINARY_CAT.getLogin());

        final DiQuotaKey quotaKey = new DiQuotaKey.Builder()
                .serviceKey(NIRVANA)
                .resourceKey(YT_CPU)
                .quotaSpecKey(YT_CPU)
                .projectKey("L1")
                .build();

        final DiQuotaChangeRequest normalRequest = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .changes(quotaKey, bigOrderOne.getId(), DiAmount.of(10_000, DiUnit.PERMILLE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(BINARY_CAT).getFirst();

        final long normalRequestUpdated = normalRequest.getUpdated();

        trackerManager.setTrackerAvailable(false);
        final DiListResponse<DiQuotaChangeRequest> requests;
        try {
            requests = dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .changes(quotaKey, bigOrderOne.getId(), DiAmount.of(10_000, DiUnit.PERMILLE))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .build(), null)
                    .performBy(BINARY_CAT);
        } finally {
            trackerManager.setTrackerAvailable(true);
        }

        final DiQuotaChangeRequest request = requests.getFirst();

        reCreateRequestTicketsTask.createTickets();

        final DiQuotaChangeRequest updatedRequest = dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertNotNull(updatedRequest.getTrackerIssueKey());
        assertNotEquals(updatedRequest.getUpdated(), updatedRequest.getCreated());

        quotaChangeRequestDao.update(quotaChangeRequestDao.read(updatedRequest.getId()).copyBuilder()
                .trackerIssueKey(null)
                .build()
        );

        reCreateRequestTicketsTask.createTickets();

        final DiQuotaChangeRequest updatedRequest2 = dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertEquals(updatedRequest.getTrackerIssueKey(), updatedRequest2.getTrackerIssueKey());

        final DiQuotaChangeRequest nonUpdatedNormalRequest = dispenser()
                .quotaChangeRequests()
                .byId(normalRequest.getId())
                .get()
                .perform();

        assertEquals(nonUpdatedNormalRequest.getUpdated(), normalRequestUpdated);
    }

    @Test
    public void ticketCanBeCreatedInTaskForRequestWithNothingReasonType() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        trackerManager.setTrackerAvailable(false);
        final DiListResponse<DiQuotaChangeRequest> requests;
        try {
            requests = dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithTypeResourcePreorder(bigOrderOne)
                            .build(), null)
                    .performBy(BINARY_CAT);
        } finally {
            trackerManager.setTrackerAvailable(true);
        }

        final long id = requests.getFirst().getId();
        final QuotaChangeRequest quotaChangeRequest = quotaChangeRequestDao.read(id);

        assertNull(quotaChangeRequest.getTrackerIssueKey());

        quotaChangeRequestDao.update(quotaChangeRequest
                .copyBuilder()
                .resourcePreorderReasonType(DiResourcePreorderReasonType.NOTHING)
                .build());

        updateHierarchy();

        reCreateRequestTicketsTask.createTickets();

        final DiQuotaChangeRequest updatedRequest = dispenser()
                .quotaChangeRequests()
                .byId(id)
                .get()
                .perform();

        assertNotNull(updatedRequest.getTrackerIssueKey());
    }

    @Test
    public void quotaChangeRequestCanBeCreatedWithSeveralResources() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiQuotaKey firstQuotaKey = new DiQuotaKey.Builder()
                .serviceKey(MDB)
                .resourceKey(CPU)
                .quotaSpecKey(CPU)
                .projectKey(TEST_PROJECT_KEY)
                .build();

        final DiQuotaKey secondQuotaKey = new DiQuotaKey.Builder()
                .serviceKey(MDB)
                .resourceKey(RAM)
                .quotaSpecKey(RAM)
                .projectKey(TEST_PROJECT_KEY)
                .build();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .changes(firstQuotaKey, bigOrderOne.getId(), DiAmount.of(32L, DiUnit.CORES))
                        .changes(secondQuotaKey, bigOrderOne.getId(), DiAmount.of(46L, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(BINARY_CAT);


        assertEquals(1, requests.size());

        final DiQuotaChangeRequest request = requests.getFirst();

        assertEquals(2, request.getChanges().size());

        final Map<String, DiQuotaChangeRequest.Change> changeByResourceKey = request.getChanges().stream()
                .collect(Collectors.toMap(c -> c.getResource().getKey(), Function.identity()));

        final DiQuotaChangeRequest.Change firstChange = changeByResourceKey.get(CPU);
        assertEquals(firstChange.getResource().getKey(), firstQuotaKey.getResourceKey());
        assertEquals(32, DiUnit.CORES.convert(firstChange.getAmount()));

        final DiQuotaChangeRequest.Change secondChange = changeByResourceKey.get(RAM);
        assertEquals(secondChange.getResource().getKey(), secondQuotaKey.getResourceKey());
        assertEquals(46, DiUnit.BYTE.convert(secondChange.getAmount()));
    }

    @Test
    public void quotaRequestWithReasonTypeNothing() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final Person lotrek = personDao.readPersonByLogin(LOTREK.getLogin());
        final Project project = Hierarchy.get().getProjectReader().read(TEST_PROJECT_KEY);
        projectDao.attach(lotrek, project, Role.MEMBER);

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requestsWithGrowth = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithTypeResourcePreorder(bigOrderOne)
                        .build(), null)
                .performBy(LOTREK);

        final DiListResponse<DiQuotaChangeRequest> requestsWithGoal = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(LOTREK);

        final DiListResponse<DiQuotaChangeRequest> requestsWithNothing = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithTypeResourcePreorder(bigOrderOne)
                        .build(), null)
                .performBy(LOTREK);

        final long id = requestsWithNothing.getFirst().getId();
        final QuotaChangeRequest quotaChangeRequest = quotaChangeRequestDao.read(id);

        quotaChangeRequestDao.update(quotaChangeRequest
                .copyBuilder()
                .resourcePreorderReasonType(DiResourcePreorderReasonType.NOTHING)
                .build());

        updateHierarchy();

        DiQuotaChangeRequest requestByCreator = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + requestsWithGrowth.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Set<DiQuotaChangeRequest.Permission> permissions = requestByCreator.getPermissions();

        final ImmutableSet<DiQuotaChangeRequest.Permission> permissions1 = Sets.immutableEnumSet(
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW,
                DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_EDIT,
                DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP,
                DiQuotaChangeRequest.Permission.CAN_CLOSE_ALLOCATION_NOTE
        );
        assertEquals(permissions, permissions1);

        requestByCreator = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + requestsWithGoal.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        permissions = requestByCreator.getPermissions();
        assertEquals(permissions, permissions1);

        requestByCreator = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + requestsWithNothing.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        permissions = requestByCreator.getPermissions();
        assertEquals(Sets.immutableEnumSet(
                DiQuotaChangeRequest.Permission.CAN_MARK_AS_READY_FOR_REVIEW,
                DiQuotaChangeRequest.Permission.CAN_CANCEL,
                DiQuotaChangeRequest.Permission.CAN_EDIT,
                DiQuotaChangeRequest.Permission.CAN_CLOSE_ALLOCATION_NOTE), permissions);

        campaignDao.getAll().stream().filter(e -> ru.yandex.qe.dispenser.domain.Campaign.Status.ACTIVE == e.getStatus()).forEach(e ->
                campaignDao.partialUpdate(e, CampaignUpdate.builder().setStatus(Campaign.Status.CLOSED).build())
        );

        requestByCreator = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + requestsWithGrowth.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        permissions = requestByCreator.getPermissions();

        assertEquals(Set.of(DiQuotaChangeRequest.Permission.CAN_CLOSE_ALLOCATION_NOTE), permissions);

        requestByCreator = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + requestsWithGoal.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        permissions = requestByCreator.getPermissions();
        assertEquals(Set.of(DiQuotaChangeRequest.Permission.CAN_CLOSE_ALLOCATION_NOTE), permissions);

        requestByCreator = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + requestsWithNothing.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        permissions = requestByCreator.getPermissions();
        assertEquals(Set.of(DiQuotaChangeRequest.Permission.CAN_CLOSE_ALLOCATION_NOTE), permissions);
    }

    @Test
    public void quotaChangeRequestCanBeCreatedForResourceWithoutQuotas() {
        final Project yandex = projectDao.read(YANDEX);
        projectDao.update(Project.copyOf(yandex)
                .abcServiceId(TEST_YANDEX_ABC_SERVICE_ID)
                .build());

        final Project search = projectDao.read(SEARCH);
        projectDao.update(Project.copyOf(search)
                .abcServiceId(TEST_SEARCH_ABC_SERVICE_ID)
                .build());

        final String newResourceKey = "UNIQUE-RESOURCE";
        dispenser()
                .service(YP)
                .resource(newResourceKey)
                .create()
                .withName(newResourceKey)
                .withType(DiResourceType.TRAFFIC)
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .service(YP)
                .resource(newResourceKey)
                .segmentations()
                .update(ImmutableList.of(
                        new DiSegmentation.Builder(DC_SEGMENTATION)
                                .withName("DC")
                                .withDescription("DC")
                                .build(),
                        new DiSegmentation.Builder(SEGMENT_SEGMENTATION)
                                .withName("segment")
                                .withDescription("segment")
                                .build()
                )).performBy(AMOSOV_F);

        updateHierarchy();

        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(SEARCH)
                        .changes(YP, newResourceKey, bigOrderOne.getId(), Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(42, DiUnit.TBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .chartLink("https://st.yandex.ru")
                        .chartLinksAbsenceExplanation("Test explanation")
                        .additionalProperties(Collections.singletonMap("segment", "default"))
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);


        assertEquals(1, requests.size());

        final DiQuotaChangeRequest changeRequest = requests.getFirst();

        assertEquals(DiQuotaChangeRequest.Status.NEW, changeRequest.getStatus());
        assertEquals(DiQuotaChangeRequest.Type.RESOURCE_PREORDER, changeRequest.getType());

        DiQuotaChangeRequest.Order firstChangeOrder = changeRequest.getChanges().iterator().next().getOrder();

        assertNotNull(firstChangeOrder);
        assertEquals(bigOrderOne.getId(), firstChangeOrder.getId());

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = dispenser().quotaChangeRequests()
                .byId(changeRequest.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.NEW, fetchedRequest.getStatus());
        assertEquals(DiQuotaChangeRequest.Type.RESOURCE_PREORDER, fetchedRequest.getType());

        final DiQuotaChangeRequest.Change firstChange = fetchedRequest.getChanges().iterator().next();
        firstChangeOrder = firstChange.getOrder();

        assertNotNull(firstChangeOrder);
        assertEquals(bigOrderOne.getId(), firstChangeOrder.getId());

        Map<String, Object> issue = trackerManager.getIssueFields(changeRequest.getTrackerIssueKey());
        String description = (String) issue.get("description");

        assertNull(issue.get("assignee"));
        assertEquals(issue.get("author"), AMOSOV_F.getLogin());
        assertEquals("[test-campaign] test (Search)", issue.get("summary"));

        final Set<String> tags = Sets.newHashSet(Arrays.asList((String[]) issue.get("tags")));
        assertEquals(Sets.newHashSet(TEST_BIG_ORDER_DATE.format(DateTimeFormatter.ISO_LOCAL_DATE), TEST_CAMPAIGN_KEY,
                Campaign.Type.DRAFT.name().toLowerCase(Locale.ENGLISH)), tags);

        assertEquals(description, "**Тикет создан только для согласования. Для подтверждения или изменения перейдите к ((https://abc.test.yandex-team.ru/hardware/" + requests.getFirst().getId() + " заявке))**\n" +
                "===Подтверждение заявки\n" +
                "Ожидается подтверждение от ответственного за Capacity Planning сервиса ((https://abc.test.yandex-team.ru/services/789 Search)). Когда заявка будет готова к защите, данные, нужные для защиты, появятся в этом тикете, а ответственный получит призыв.\n" +
                "===Данные заявки\n" +
                "#|\n" +
                "|| **Сервис** | ((https://abc.test.yandex-team.ru/services/search/hardware/?view=consuming Search)) ||\n" +
                "|| **Автор заявки** | staff:amosov-f ||\n" +
                "|| **segment** | default ||\n" +
                "|| **Провайдеры** | YP ||\n" +
                "|| **Кампания** | Test ||\n" +
                "|| **Тип кампании** | Черновая ||\n" +
                "|| **Даты поставки** | Январь 2020 ||\n" +
                "|| **Статус** | Черновик ||\n" +
                "|#\n" +
                "===Ресурсы YP\n" +
                "#|\n" +
                "||  | UNIQUE-RESOURCE ||\n" +
                "|| Январь 2020: LOCATION_1, Segment1 | 42 TBps ||\n" +
                "|#\n" +
                "Комментарий:\n" +
                "For needs.\n"
        );

        final Set<Long> expectedComponents = Arrays.stream((long[]) issue.get("components")).boxed().collect(Collectors.toSet());
        assertEquals(expectedComponents, Sets.newHashSet(Long.valueOf(trackerComponents.get("resourcePreorder")),
                Long.valueOf(trackerComponents.get("yp"))));

        final Set<Long> serviceRefs = Arrays.stream((ServiceRef[]) issue.get("abcService"))
                .map(ServiceRef::getId)
                .collect(Collectors.toSet());

        assertEquals(serviceRefs, Sets.newHashSet(Long.valueOf(projectDao.read(YANDEX).getAbcServiceId()),
                Long.valueOf(projectDao.read(SEARCH).getAbcServiceId())));

        dispenser().quotaChangeRequests()
                .byId(requests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(GROWTH_ANSWER)
                        .build())
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .byId(requests.getFirst().getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(AMOSOV_F);

        issue = trackerManager.getIssueFields(changeRequest.getTrackerIssueKey());
        description = (String) issue.get("description");

        assertNotNull(issue.get("assignee"));
        assertEquals(description, "**Тикет создан только для согласования. Для подтверждения или изменения перейдите к ((https://abc.test.yandex-team.ru/hardware/" + requests.getFirst().getId() + " заявке))**\n" +
                "===Подтверждение заявки\n" +
                "Ожидается подтверждение от ответственного за Capacity Planning сервиса ((https://abc.test.yandex-team.ru/services/789 Search)). Когда заявка будет готова к защите, данные, нужные для защиты, появятся в этом тикете, а ответственный получит призыв.\n" +
                "===Данные заявки\n" +
                "#|\n" +
                "|| **Сервис** | ((https://abc.test.yandex-team.ru/services/search/hardware/?view=consuming Search)) ||\n" +
                "|| **Автор заявки** | staff:amosov-f ||\n" +
                "|| **segment** | default ||\n" +
                "|| **Провайдеры** | YP ||\n" +
                "|| **Кампания** | Test ||\n" +
                "|| **Тип кампании** | Черновая ||\n" +
                "|| **Даты поставки** | Январь 2020 ||\n" +
                "|| **Статус** | Готова к защите ||\n" +
                "|#\n" +
                "===Ресурсы YP\n" +
                "#|\n" +
                "||  | UNIQUE-RESOURCE ||\n" +
                "|| Январь 2020: LOCATION_1, Segment1 | 42 TBps ||\n" +
                "|#\n" +
                "Комментарий:\n" +
                "For needs.\n" +
                "===Защита\n" +
                "#|\n" +
                "|| **Причина заказа ресурсов** | естественный рост ||\n" +
                "|| **Расчёты** | Description for how we calculated required amounts ||\n" +
                "|| **Графики утилизации** | ((https://st.yandex.ru)) ||\n" +
                "|| **Причина отсутствия графиков** | Test explanation ||\n" +
                "|| **Какие причины роста?** | fine! ||\n" +
                "|| **Как модерируются рост и потребление ресурсов?** | good! ||\n" +
                "|| **Что произойдёт с сервисом, если ничего не выделить на указанный рост? Если выделить 50%?** | done! ||\n" +
                "|#\n");
    }

    @Test
    public void createTicketFromQuotaChangeRequestWithGoal() {
        final Project search = projectDao.read(SEARCH);
        projectDao.update(Project.copyOf(search)
                .abcServiceId(TEST_SEARCH_ABC_SERVICE_ID)
                .build());
        updateHierarchy();

        campaignDao.clear();
        final LocalDate localDate = LocalDate.of(2020, Month.MARCH, 1);
        final String date = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        BigOrder bigOrderA = bigOrderManager.create(BigOrder.builder(localDate));
        final LocalDate localDate1 = localDate.plusMonths(1);
        final String date1 = localDate1.format(DateTimeFormatter.ISO_LOCAL_DATE);
        BigOrder bigOrderB = bigOrderManager.create(BigOrder.builder(localDate1));
        campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setBigOrders(Arrays.asList(new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(bigOrderA.getId(), localDate),
                        new Campaign.BigOrder(bigOrderB.getId(), localDate1)))
                .build());

        prepareCampaignResources();

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(SEARCH)
                        .changes(YP, SEGMENT_CPU, bigOrderA.getId(), ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(100L, DiUnit.PERMILLE_CORES))
                        .changes(YP, SEGMENT_HDD, bigOrderA.getId(), ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_3), DiAmount.of(1, DiUnit.BYTE))
                        .changes(YP, SEGMENT_CPU, bigOrderB.getId(), ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(110L, DiUnit.PERMILLE_CORES))
                        .changes(YP, SEGMENT_HDD, bigOrderB.getId(), ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_3), DiAmount.of(7, DiUnit.BYTE))
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(190L, DiUnit.PERMILLE_CORES))
                        .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_3), DiAmount.of(155, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .chartLink("https://st.yandex.ru")
                        .chartLinksAbsenceExplanation("Test explanation")
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                        .goalId(TEST_GOAL_ID)
                        .build(), null)
                .performBy(AMOSOV_F);

        final DiQuotaChangeRequest changeRequest = requests.getFirst();
        final DiQuotaChangeRequest.Change firstChange = changeRequest.getChanges().iterator().next();

        Map<String, Object> issue = trackerManager.getIssueFields(changeRequest.getTrackerIssueKey());
        String description = (String) issue.get("description");

        assertNull(issue.get("assignee"));
        assertEquals(issue.get("author"), AMOSOV_F.getLogin());
        assertEquals("[test-campaign] test (Search)", issue.get("summary"));

        final Set<String> tags = Sets.newHashSet(Arrays.asList((String[]) issue.get("tags")));
        assertEquals(tags, Sets.newHashSet(TEST_BIG_ORDER_DATE.format(DateTimeFormatter.ISO_LOCAL_DATE), TEST_CAMPAIGN_KEY,
                date, date1, Campaign.Type.DRAFT.name().toLowerCase(Locale.ENGLISH)));

        NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
        assertEquals(description, "**Тикет создан только для согласования. Для подтверждения или изменения перейдите к ((https://abc.test.yandex-team.ru/hardware/" + requests.getFirst().getId() + " заявке))**\n" +
                "===Подтверждение заявки\n" +
                "Ожидается подтверждение от ответственного за Capacity Planning сервиса ((https://abc.test.yandex-team.ru/services/789 Search)). Когда заявка будет готова к защите, данные, нужные для защиты, появятся в этом тикете, а ответственный получит призыв.\n" +
                "===Данные заявки\n" +
                "#|\n" +
                "|| **Сервис** | ((https://abc.test.yandex-team.ru/services/search/hardware/?view=consuming Search)) ||\n" +
                "|| **Автор заявки** | staff:amosov-f ||\n" +
                "|| **Провайдеры** | YP ||\n" +
                "|| **Кампания** | Test ||\n" +
                "|| **Тип кампании** | Черновая ||\n" +
                "|| **Даты поставки** | Январь, Март, Апрель 2020 ||\n" +
                "|| **Статус** | Черновик ||\n" +
                "|#\n" +
                "===Ресурсы YP\n" +
                "#|\n" +
                "||  | Segment CPU | Segment Storage ||\n" +
                "||  | **" + format.format(0.4d) + " cores** | **163 B** ||\n" +
                "|| Январь 2020: LOCATION_1, Segment1 | " + format.format(0.19d) + " cores | 0 B ||\n" +
                "|| Январь 2020: LOCATION_3, Segment1 | 0 cores | 155 B ||\n" +
                "|| Март 2020: LOCATION_1, Segment1 | " + format.format(0.1d) + " cores | 0 B ||\n" +
                "|| Март 2020: LOCATION_3, Segment1 | 0 cores | 1 B ||\n" +
                "|| Апрель 2020: LOCATION_1, Segment1 | " + format.format(0.11d) + " cores | 0 B ||\n" +
                "|| Апрель 2020: LOCATION_3, Segment1 | 0 cores | 7 B ||\n" +
                "|#\n" +
                "Комментарий:\n" +
                "For needs.\n"
        );

        final long[] components = (long[]) issue.get("components");
        final ArrayList<Long> expectedComponents = new ArrayList<>();
        expectedComponents.add(Long.parseLong(trackerComponents.get("yp")));
        expectedComponents.add(Long.parseLong(trackerComponents.get("resourcePreorder")));

        assertEquals(2, components.length);
        assertTrue(expectedComponents.contains(components[0]) && expectedComponents.contains(components[1]));

        dispenser().quotaChangeRequests()
                .byId(requests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(GOAL_ANSWER)
                        .build())
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .byId(requests.getFirst().getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(AMOSOV_F);

        issue = trackerManager.getIssueFields(changeRequest.getTrackerIssueKey());
        description = (String) issue.get("description");

        assertNotNull(issue.get("assignee"));
        assertEquals(description, "**Тикет создан только для согласования. Для подтверждения или изменения перейдите к ((https://abc.test.yandex-team.ru/hardware/" + requests.getFirst().getId() + " заявке))**\n" +
                "===Подтверждение заявки\n" +
                "Ожидается подтверждение от ответственного за Capacity Planning сервиса ((https://abc.test.yandex-team.ru/services/789 Search)). Когда заявка будет готова к защите, данные, нужные для защиты, появятся в этом тикете, а ответственный получит призыв.\n" +
                "===Данные заявки\n" +
                "#|\n" +
                "|| **Сервис** | ((https://abc.test.yandex-team.ru/services/search/hardware/?view=consuming Search)) ||\n" +
                "|| **Автор заявки** | staff:amosov-f ||\n" +
                "|| **Провайдеры** | YP ||\n" +
                "|| **Кампания** | Test ||\n" +
                "|| **Тип кампании** | Черновая ||\n" +
                "|| **Даты поставки** | Январь, Март, Апрель 2020 ||\n" +
                "|| **Статус** | Готова к защите ||\n" +
                "|#\n" +
                "===Ресурсы YP\n" +
                "#|\n" +
                "||  | Segment CPU | Segment Storage ||\n" +
                "||  | **" + format.format(0.4d) + " cores** | **163 B** ||\n" +
                "|| Январь 2020: LOCATION_1, Segment1 | " + format.format(0.19) + " cores | 0 B ||\n" +
                "|| Январь 2020: LOCATION_3, Segment1 | 0 cores | 155 B ||\n" +
                "|| Март 2020: LOCATION_1, Segment1 | " + format.format(0.1) + " cores | 0 B ||\n" +
                "|| Март 2020: LOCATION_3, Segment1 | 0 cores | 1 B ||\n" +
                "|| Апрель 2020: LOCATION_1, Segment1 | " + format.format(0.11) + " cores | 0 B ||\n" +
                "|| Апрель 2020: LOCATION_3, Segment1 | 0 cores | 7 B ||\n" +
                "|#\n" +
                "Комментарий:\n" +
                "For needs.\n" +
                "===Защита\n" +
                "#|\n" +
                "|| **Причина заказа ресурсов** | ((https://goals.yandex-team.ru/filter?goal=1 Test Super Goal)) ||\n" +
                "|| **Расчёты** | Description for how we calculated required amounts ||\n" +
                "|| **Графики утилизации** | ((https://st.yandex.ru)) ||\n" +
                "|| **Причина отсутствия графиков** | Test explanation ||\n" +
                "|| **Расходы на эту цель будут единоразовыми или ежегодными?** | fine! ||\n" +
                "|| **Что будет, если мы не будем делать эту цель?** | good! ||\n" +
                "|| **Что будет, если выделим на цель 50% ресурсов от желаемого количества?** | done! ||\n" +
                "|#\n");

        final Map<Long, String> requestGoalAnswers = new HashMap<>();
        requestGoalAnswers.put(0L, "First answer");
        requestGoalAnswers.put(1L, "Second answer");
        requestGoalAnswers.put(2L, "Third answer");

        dispenser().quotaChangeRequests()
                .byId(requests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(requestGoalAnswers)
                        .build())
                .performBy(AMOSOV_F);

        issue = trackerManager.getIssueFields(changeRequest.getTrackerIssueKey());
        description = (String) issue.get("description");

        assertEquals(description, "**Тикет создан только для согласования. Для подтверждения или изменения перейдите к ((https://abc.test.yandex-team.ru/hardware/" + requests.getFirst().getId() + " заявке))**\n" +
                "===Подтверждение заявки\n" +
                "Ожидается подтверждение от ответственного за Capacity Planning сервиса ((https://abc.test.yandex-team.ru/services/789 Search)). Когда заявка будет готова к защите, данные, нужные для защиты, появятся в этом тикете, а ответственный получит призыв.\n" +
                "===Данные заявки\n" +
                "#|\n" +
                "|| **Сервис** | ((https://abc.test.yandex-team.ru/services/search/hardware/?view=consuming Search)) ||\n" +
                "|| **Автор заявки** | staff:amosov-f ||\n" +
                "|| **Провайдеры** | YP ||\n" +
                "|| **Кампания** | Test ||\n" +
                "|| **Тип кампании** | Черновая ||\n" +
                "|| **Даты поставки** | Январь, Март, Апрель 2020 ||\n" +
                "|| **Статус** | Готова к защите ||\n" +
                "|#\n" +
                "===Ресурсы YP\n" +
                "#|\n" +
                "||  | Segment CPU | Segment Storage ||\n" +
                "||  | **" + format.format(0.4d) + " cores** | **163 B** ||\n" +
                "|| Январь 2020: LOCATION_1, Segment1 | " + format.format(0.19d) + " cores | 0 B ||\n" +
                "|| Январь 2020: LOCATION_3, Segment1 | 0 cores | 155 B ||\n" +
                "|| Март 2020: LOCATION_1, Segment1 | " + format.format(0.1d) + " cores | 0 B ||\n" +
                "|| Март 2020: LOCATION_3, Segment1 | 0 cores | 1 B ||\n" +
                "|| Апрель 2020: LOCATION_1, Segment1 | " + format.format(0.11d) + " cores | 0 B ||\n" +
                "|| Апрель 2020: LOCATION_3, Segment1 | 0 cores | 7 B ||\n" +
                "|#\n" +
                "Комментарий:\n" +
                "For needs.\n" +
                "===Защита\n" +
                "#|\n" +
                "|| **Причина заказа ресурсов** | ((https://goals.yandex-team.ru/filter?goal=1 Test Super Goal)) ||\n" +
                "|| **Расчёты** | Description for how we calculated required amounts ||\n" +
                "|| **Графики утилизации** | ((https://st.yandex.ru)) ||\n" +
                "|| **Причина отсутствия графиков** | Test explanation ||\n" +
                "|| **Расходы на эту цель будут единоразовыми или ежегодными?** | First answer ||\n" +
                "|| **Что будет, если мы не будем делать эту цель?** | Second answer ||\n" +
                "|| **Что будет, если выделим на цель 50% ресурсов от желаемого количества?** | Third answer ||\n" +
                "|#\n");
    }

    @Test
    public void ticketWillUpdateAssigneeOnProjectChangeOnlyIfAssigneeWasPresent() {
        final Project search = projectDao.read(SEARCH);
        projectDao.update(Project.copyOf(search)
                .abcServiceId(TEST_SEARCH_ABC_SERVICE_ID)
                .build());
        final Project yandex = projectDao.read(YANDEX);
        projectDao.update(Project.copyOf(yandex)
                .abcServiceId(TEST_YANDEX_ABC_SERVICE_ID)
                .build());
        final Person keyd = personDao.readPersonByLogin(KEYD.getLogin());
        final Person lotrek = personDao.readPersonByLogin(LOTREK.getLogin());
        projectDao.attach(keyd, search, Role.RESPONSIBLE);
        projectDao.attach(lotrek, search, Role.RESPONSIBLE);
        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(SEARCH)
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(100L, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .chartLink("https://st.yandex.ru")
                        .chartLinksAbsenceExplanation("Test explanation")
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                        .goalId(TEST_GOAL_ID)
                        .build(), null)
                .performBy(AMOSOV_F);

        DiQuotaChangeRequest changeRequest = requests.getFirst();

        Map<String, Object> issue = trackerManager.getIssueFields(changeRequest.getTrackerIssueKey());

        assertNull(issue.get("assignee"));
        assertNull(issue.get("followers"));

        dispenser().quotaChangeRequests()
                .byId(requests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(GOAL_ANSWER)
                        .build())
                .performBy(AMOSOV_F);

        issue = trackerManager.getIssueFields(changeRequest.getTrackerIssueKey());
        assertNull(issue.get("assignee"));
        assertNull(issue.get("followers"));

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/admin/requests/_moveToProject")
                .post(new RequestAdminService.MoveRequestsToProjectParams(null, changeRequest.getTrackerIssueKey(), TEST_YANDEX_ABC_SERVICE_ID),
                        new GenericType<List<QuotaChangeRequestManager.MoveResult>>() {});

        changeRequest = dispenser().quotaChangeRequests()
                .byId(changeRequest.getId())
                .get()
                .perform();
        issue = trackerManager.getIssueFields(changeRequest.getTrackerIssueKey());
        assertNull(issue.get("assignee"));
        assertNull(issue.get("followers"));
        assertEquals(YANDEX, changeRequest.getProject().getKey());

        dispenser().quotaChangeRequests()
                .byId(requests.getFirst().getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(AMOSOV_F);

        issue = trackerManager.getIssueFields(changeRequest.getTrackerIssueKey());

        assertEquals(WHISTLER.getLogin(), issue.get("assignee"));
        assertIterableEquals(ImmutableList.of(WHISTLER.getLogin()), (Collection<String>)issue.get("followers"));

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/admin/requests/_moveToProject")
                .post(new RequestAdminService.MoveRequestsToProjectParams(null, changeRequest.getTrackerIssueKey(), TEST_SEARCH_ABC_SERVICE_ID),
                        new GenericType<List<QuotaChangeRequestManager.MoveResult>>() {});

        changeRequest = dispenser().quotaChangeRequests()
                .byId(changeRequest.getId())
                .get()
                .perform();
        issue = trackerManager.getIssueFields(changeRequest.getTrackerIssueKey());
        assertEquals(KEYD.getLogin(), issue.get("assignee"));
        assertEquals(SEARCH, changeRequest.getProject().getKey());
        assertIterableEquals(ImmutableList.of(WHISTLER.getLogin(), KEYD.getLogin(), LOTREK.getLogin()), (Collection<String>) issue.get("followers"));
    }

    private DiQuotaSpec createQuotaSpeq(final String serviceKey, final String resourceKey) {
        return createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-specifications/" + serviceKey + "/" + resourceKey + "/" + resourceKey + "-quota")
                .put("{\"description\":\"description\"}", DiQuotaSpec.class);
    }

    @Test
    public void checkEveryEnumInToReasonString() {
        createProject("Test", YANDEX, BINARY_CAT.getLogin());

        final long reqId = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT).getFirst().getId();
        ;

        final QuotaChangeRequest request = quotaChangeRequestDao.read(reqId);

        Arrays.stream(DiResourcePreorderReasonType.values())
                .filter(e -> e != DiResourcePreorderReasonType.NOTHING)
                .forEach(e -> quotaChangeRequestTicketManager.toReasonString(request.copyBuilder().resourcePreorderReasonType(e).build()));
    }

    @MethodSource("quotaKeysBuilders")
    @ParameterizedTest(name = "[{index}] {displayName}")
    public void quotaChangesWithSameChangeKeyShouldBeAggregated(final DiQuotaKey.Builder quotaKeyBuilder) {
        final Project yandex = projectDao.read(YANDEX);
        projectDao.update(Project.copyOf(yandex)
                .abcServiceId(TEST_YANDEX_ABC_SERVICE_ID)
                .build());
        final DiQuotaKey quotaKey = quotaKeyBuilder.projectKey(YANDEX).build();
        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(quotaKey.getProjectKey())
                        .changes(quotaKey.getServiceKey(), quotaKey.getResourceKey(), bigOrderOne.getId(), quotaKey.getSegmentKeys(), DiAmount.of(256, DiUnit.PERMILLE_CORES))
                        .changes(quotaKey.getServiceKey(), quotaKey.getResourceKey(), bigOrderOne.getId(), quotaKey.getSegmentKeys(), DiAmount.of(256, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);

        final List<DiQuotaChangeRequest.Change> changes = requests.getFirst().getChanges();
        assertEquals(1, changes.size());
        final DiQuotaChangeRequest.Change change = changes.iterator().next();

        assertEquals(quotaKey.getResourceKey(), change.getResource().getKey());
        assertEquals(512, change.getAmount().getValue());
        assertEquals(quotaKey.getSegmentKeys(), change.getSegmentKeys());
    }

    @Test
    public void requestsCanBeFilteredByAbcServiceIdParams() {
        final String newResourceKey = "UNIQUE-RESOURCE";
        dispenser()
                .service(MDB)
                .resource(newResourceKey)
                .create()
                .withName(newResourceKey)
                .withType(DiResourceType.ENUMERABLE)
                .performBy(AMOSOV_F);

        final int ancestorAbcServiceId = 10;
        final int parentAbcServiceId = 20;
        final int parentSiblingAbcServiceId = 25;
        final int abcServiceId = 30;
        final int cousinAbcServiceId = 35;

        final AbcService ancestorService = abcApi.addService(ancestorAbcServiceId, Collections.emptyList());
        final AbcService parentAbcService = abcApi.addService(parentAbcServiceId, Collections.singletonList(ancestorService));
        final AbcService uncleAbcService = abcApi.addService(parentSiblingAbcServiceId, Collections.singletonList(ancestorService));
        abcApi.addService(abcServiceId, ImmutableList.of(ancestorService, parentAbcService));
        abcApi.addService(cousinAbcServiceId, ImmutableList.of(ancestorService, uncleAbcService));

        updateHierarchy();

        final String ancestorProjectKey = "test-prj-003-ancestor";
        dispenser().projects()
                .create(DiProject.withKey(ancestorProjectKey)
                        .withName(ancestorProjectKey)
                        .withDescription(ancestorProjectKey)
                        .withAbcServiceId(ancestorAbcServiceId)
                        .withParentProject(YANDEX)
                        .withResponsibles(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        prepareCampaignResources();
        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(ancestorProjectKey)
                        .changes(MDB, newResourceKey, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.PERMILLE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);

        final String parentProjectKey = "test-prj-003-parent";
        dispenser().projects()
                .create(DiProject.withKey(parentProjectKey)
                        .withName(parentProjectKey)
                        .withDescription(parentProjectKey)
                        .withAbcServiceId(parentAbcServiceId)
                        .withParentProject(ancestorProjectKey)
                        .withResponsibles(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F);

        final String parentSiblingProjectKey = "test-prj-003-parent-sibling";
        dispenser().projects()
                .create(DiProject.withKey(parentSiblingProjectKey)
                        .withName(parentSiblingProjectKey)
                        .withDescription(parentSiblingProjectKey)
                        .withAbcServiceId(parentSiblingAbcServiceId)
                        .withParentProject(ancestorProjectKey)
                        .withResponsibles(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final String cousinProjectKey = "test-prj-003-cousin";
        dispenser().projects()
                .create(DiProject.withKey(cousinProjectKey)
                        .withName(cousinProjectKey)
                        .withDescription(cousinProjectKey)
                        .withAbcServiceId(cousinAbcServiceId)
                        .withParentProject(parentSiblingProjectKey)
                        .withResponsibles(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(parentProjectKey)
                        .changes(MDB, newResourceKey, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.PERMILLE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(cousinProjectKey)
                        .changes(MDB, newResourceKey, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.PERMILLE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);

        final String projectKey = "test-prj-003";
        dispenser().projects()
                .create(DiProject.withKey(projectKey)
                        .withName(projectKey)
                        .withDescription(projectKey)
                        .withAbcServiceId(abcServiceId)
                        .withParentProject(parentProjectKey)
                        .withResponsibles(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(projectKey)
                        .changes(MDB, newResourceKey, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.PERMILLE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> allRequests = dispenser().quotaChangeRequests()
                .get()
                .perform();

        assertEquals(4, allRequests.size());

        final DiListResponse<DiQuotaChangeRequest> filteredRequests = dispenser().quotaChangeRequests()
                .get()
                .query("abcServiceId", String.valueOf(abcServiceId))
                .perform();

        assertEquals(1, filteredRequests.size());

        assertEquals(projectKey, filteredRequests.getFirst().getProject().getKey());

        final DiListResponse<DiQuotaChangeRequest> childRequest = dispenser().quotaChangeRequests()
                .get()
                .query("ancestorAbcServiceId", String.valueOf(parentAbcServiceId))
                .perform();

        assertEquals(1, childRequest.size());

        final DiListResponse<DiQuotaChangeRequest> childProjects2 = dispenser().quotaChangeRequests()
                .get()
                .query("ancestorAbcServiceId", String.valueOf(ancestorAbcServiceId))
                .perform();

        assertEquals(3, childProjects2.size());

        final DiListResponse<DiQuotaChangeRequest> childProjects3 = dispenser().quotaChangeRequests()
                .get()
                .query("ancestorAbcServiceId", String.valueOf(parentAbcServiceId))
                .query("abcServiceId", String.valueOf(parentAbcServiceId))
                .perform();

        assertEquals(2, childProjects3.size());

        final DiListResponse<DiQuotaChangeRequest> childProjects4 = dispenser().quotaChangeRequests()
                .get()
                .query("ancestorAbcServiceId", String.valueOf(ancestorAbcServiceId))
                .query("abcServiceId", String.valueOf(ancestorAbcServiceId))
                .perform();

        assertEquals(4, childProjects4.size());

        final DiListResponse<DiQuotaChangeRequest> childProjects5 = dispenser().quotaChangeRequests()
                .get()
                .query("ancestorAbcServiceId", String.valueOf(parentAbcServiceId), String.valueOf(parentSiblingAbcServiceId))
                .perform();

        assertEquals(2, childProjects5.size());

        abcApi.reset();
    }

    public static Object[][] quotaChangeUpdation() {
        return new Object[][]{{
                new Body.BodyBuilder()
                        .projectKey("Test")
                        .changes(NIRVANA, YT_CPU, BIG_ORDER_ONE_ID, Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .description("v1")
                        .calculations(TEST_CALCULATIONS)
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .summary("Test")
                        .build(),

                ImmutableList.of(
                        new BodyUpdate.BodyUpdateBuilder()
                                .description("v2")
                                .build(),

                        new BodyUpdate.BodyUpdateBuilder()
                                .changes(NIRVANA, YT_GPU, BIG_ORDER_ONE_ID, Collections.emptySet(), DiAmount.of(200L, DiUnit.PERCENT))
                                .build(),

                        new BodyUpdate.BodyUpdateBuilder()
                                .changes(NIRVANA, YT_GPU, BIG_ORDER_ONE_ID, Collections.emptySet(), DiAmount.of(200L, DiUnit.PERCENT))
                                .changes(NIRVANA, STORAGE, BIG_ORDER_ONE_ID, Collections.emptySet(), DiAmount.of(100, DiUnit.BYTE))
                                .description("v1")
                                .build(),

                        new BodyUpdate.BodyUpdateBuilder()
                                .changes(NIRVANA, STORAGE, BIG_ORDER_ONE_ID, Collections.emptySet(), DiAmount.of(20, DiUnit.BYTE))
                                .build(),

                        new BodyUpdate.BodyUpdateBuilder()
                                .comment("Updated comment")
                                .build(),

                        new BodyUpdate.BodyUpdateBuilder()
                                .calculations("Updated calculations")
                                .build()
                )
        }, {
                new Body.BodyBuilder()
                        .projectKey("Test")
                        .changes(YP, SEGMENT_CPU, BIG_ORDER_ONE_ID, ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(100L, DiUnit.PERMILLE_CORES))
                        .description("v1")
                        .calculations(TEST_CALCULATIONS)
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .summary("Test")
                        .build(),
                ImmutableList.of(
                        new BodyUpdate.BodyUpdateBuilder()
                                .changes(YP, SEGMENT_CPU, BIG_ORDER_ONE_ID, ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_2), DiAmount.of(200L, DiUnit.PERMILLE_CORES))
                                .changes(YP, SEGMENT_CPU, BIG_ORDER_ONE_ID, ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_3), DiAmount.of(300L, DiUnit.PERMILLE_CORES))
                                .description("v3")
                                .build(),

                        new BodyUpdate.BodyUpdateBuilder()
                                .changes(YP, SEGMENT_HDD, BIG_ORDER_ONE_ID, ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_3), DiAmount.of(1, DiUnit.BYTE))
                                .description("v4")
                                .build()
                )
        }
        };
    }

    @MethodSource("quotaChangeUpdation")
    @ParameterizedTest(name = "[{index}] {displayName}")
    public void quotaChangeRequestCanBeUpdated(final Body createBody,
                                               final List<BodyUpdate> updates) {
        createProject(createBody.getProjectKey(), YANDEX, BINARY_CAT.getLogin());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(createBody, null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        final DiQuotaChangeRequest request = quotaRequests.getFirst();

        for (final BodyUpdate update : updates) {

            DiQuotaChangeRequest updatedRequest = dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .update(update)
                    .performBy(BINARY_CAT);

            assertRequestAffectedByUpdate(update, updatedRequest);

            updateHierarchy();

            updatedRequest = dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .get()
                    .perform();

            assertRequestAffectedByUpdate(update, updatedRequest);
        }
    }

    public void assertRequestAffectedByUpdate(final BodyUpdate update, final DiQuotaChangeRequest updatedRequest) {

        if (update.getDescription() != null) {
            assertEquals(updatedRequest.getDescription(), update.getDescription());
        }
        if (update.getComment() != null) {
            assertEquals(updatedRequest.getComment(), update.getComment());
        }
        if (update.getCalculations() != null) {
            assertEquals(updatedRequest.getCalculations(), update.getCalculations());
        }

        if (CollectionUtils.isNotEmpty(update.getChanges())) {

            assertEquals(updatedRequest.getChanges().size(), update.getChanges().size());

            final HashBasedTable<String, Set<String>, ChangeBody> changeBodyByResourceKeyAndSegments = update.getChanges().stream()
                    .collect(Collector.of(HashBasedTable::create, (m, body) -> m.put(body.getResourceKey(), body.getSegmentKeys(), body), (m1, m2) -> {
                        m1.putAll(m2);
                        return m2;
                    }));

            updatedRequest.getChanges().forEach(change -> {
                final ChangeBody changeBody = changeBodyByResourceKeyAndSegments.get(change.getResource().getKey(), change.getSegmentKeys());
                assertEquals(changeBody.getAmount().getUnit().convert(change.getAmount()), changeBody.getAmount().getValue());
                assertEquals(changeBody.getSegmentKeys(), change.getSegmentKeys());
            });

        }
    }

    public static Object[][] quotaChangeUpdateReopenSource() {
        return new Object[][]{{
                new Body.BodyBuilder()
                        .summary("test")
                        .projectKey("Test")
                        .changes(NIRVANA, YT_CPU, BIG_ORDER_ONE_ID, Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .description("v1")
                        .calculations(TEST_CALCULATIONS)
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(),

                ImmutableList.of(
                        new BodyUpdate.BodyUpdateBuilder()
                                .changes(NIRVANA, YT_GPU, BIG_ORDER_ONE_ID, Collections.emptySet(), DiAmount.of(200L, DiUnit.PERCENT))
                                .build(),

                        new BodyUpdate.BodyUpdateBuilder()
                                .changes(NIRVANA, YT_GPU, BIG_ORDER_ONE_ID, Collections.emptySet(), DiAmount.of(200L, DiUnit.PERCENT))
                                .changes(NIRVANA, STORAGE, BIG_ORDER_ONE_ID, Collections.emptySet(), DiAmount.of(100, DiUnit.BYTE))
                                .description("v1")
                                .build(),

                        new BodyUpdate.BodyUpdateBuilder()
                                .changes(NIRVANA, STORAGE, BIG_ORDER_ONE_ID, Collections.emptySet(), DiAmount.of(20, DiUnit.BYTE))
                                .build()
                )
        }, {
                new Body.BodyBuilder()
                        .summary("test")
                        .projectKey("Test")
                        .changes(YP, SEGMENT_CPU, BIG_ORDER_ONE_ID, ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(100L, DiUnit.PERMILLE_CORES))
                        .description("v1")
                        .calculations(TEST_CALCULATIONS)
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(),
                ImmutableList.of(
                        new BodyUpdate.BodyUpdateBuilder()
                                .changes(YP, SEGMENT_CPU, BIG_ORDER_ONE_ID, ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_2), DiAmount.of(200L, DiUnit.PERMILLE_CORES))
                                .changes(YP, SEGMENT_CPU, BIG_ORDER_ONE_ID, ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_3), DiAmount.of(300L, DiUnit.PERMILLE_CORES))
                                .description("v3")
                                .build(),

                        new BodyUpdate.BodyUpdateBuilder()
                                .changes(YP, SEGMENT_HDD, BIG_ORDER_ONE_ID, ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_3), DiAmount.of(1, DiUnit.BYTE))
                                .description("v4")
                                .build()
                )
        }
        };
    }

    @MethodSource("quotaChangeUpdateReopenSource")
    @ParameterizedTest(name = "[{index}] {displayName}")
    public void quotaChangeRequestCanBeUpdatedWithReopen(final Body createBody,
                                                         final List<BodyUpdate> updates) {
        createProject(createBody.getProjectKey(), YANDEX, BINARY_CAT.getLogin());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(createBody, null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        final DiQuotaChangeRequest createdRequest = quotaRequests.getFirst();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .byId(createdRequest.getId())
                .setStatus(DiQuotaChangeRequest.Status.CANCELLED)
                .performBy(BINARY_CAT);

        updateHierarchy();

        for (final BodyUpdate update : updates) {

            DiQuotaChangeRequest updatedRequest = dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .update(update)
                    .performBy(BINARY_CAT);

            assertRequestAffectedByUpdate(update, updatedRequest);
            assertEquals(DiQuotaChangeRequest.Status.NEW, updatedRequest.getStatus());

            updateHierarchy();

            updatedRequest = dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .get()
                    .perform();

            assertRequestAffectedByUpdate(update, updatedRequest);
            assertEquals(DiQuotaChangeRequest.Status.NEW, updatedRequest.getStatus());
        }
    }

    @Test
    public void requestsCanBeFilteredByType() {
        final Project yandex = projectDao.read(YANDEX);
        projectDao.update(Project.copyOf(yandex)
                .abcServiceId(TEST_YANDEX_ABC_SERVICE_ID)
                .build());
        updateHierarchy();
        dispenser()
                .quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(YANDEX)
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(13, DiUnit.PERMILLE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null
                )
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser()
                .quotaChangeRequests()
                .get()
                .perform();

        assertEquals(1, requests.size());

        final DiListResponse<DiQuotaChangeRequest> requestsIncrease = dispenser()
                .quotaChangeRequests()
                .get()
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name())
                .perform();

        assertEquals(1, requestsIncrease.size());
    }

    private Body.BodyBuilder requestBodyBuilderWithConfiguredRequiredFields() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        return new Body.BodyBuilder()
                .summary(TEST_SUMMARY)
                .projectKey(project.getPublicKey())
                .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.CORES))
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .description(TEST_DESCRIPTION)
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                .calculations(TEST_CALCULATIONS);
    }

    @Test
    public void requestCanBeCreatedWithComment() {
        dispenser()
                .quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .comment(TEST_COMMENT)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests().get().perform().getFirst();
        assertEquals(TEST_COMMENT, request.getComment());
    }

    @Test
    public void resourcePreOrderRequestCanBeCreatedByProjectMember() {
        createProject(TEST_PROJECT_KEY, YANDEX, SLONNN.getLogin());

        dispenser().project(TEST_PROJECT_KEY)
                .members()
                .attach(LOTREK.getLogin())
                .attach(TERRY.getLogin())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final Body requestBody = requestBodyBuilderWithDefaultFields()
                .projectKey(TEST_PROJECT_KEY)
                .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.PERMILLE_CORES))
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                .build();

        assertThrows(Throwable.class, () -> {
            dispenser().quotaChangeRequests()
                    .create(requestBody, null)
                    .performBy(BINARY_CAT);
        });

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBody, null)
                .performBy(LOTREK).getFirst();

        updateHierarchy();

        final BodyUpdate bodyUpdate = new BodyUpdate.BodyUpdateBuilder()
                .comment("new changes")
                .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(512, DiUnit.PERMILLE_CORES))
                .build();

        assertThrows(Throwable.class, () -> {
            dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .update(bodyUpdate)
                    .performBy(BINARY_CAT);
        });

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(bodyUpdate)
                .performBy(TERRY);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(bodyUpdate)
                .performBy(LOTREK);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(bodyUpdate)
                .performBy(SLONNN);
    }

    @Test
    public void requestsCanBeFilteredByService() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(MDB, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(256, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        assertEquals(1, dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", YP)
                .perform().size());

        assertEquals(2, dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", MDB)
                .perform().size());

        assertEquals(3, dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("service", MDB)
                .perform().size());
    }

    @Test
    public void chartLinksCanBeAddedWhenCreateRequest() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary("test")
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(23, DiUnit.PERMILLE))
                        .description("required")
                        .sourceProjectKey(INFRA)
                        .calculations("calc")
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .chartLink("https://wiki.yandex-team.ru/dispenser/dev/")
                        .chartLinksAbsenceExplanation("no_chart_link")
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null
                )
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests().get().perform().getFirst();

        assertEquals(1, Objects.requireNonNull(request.getChartLinks()).size());
        assertEquals("https://wiki.yandex-team.ru/dispenser/dev/", Objects.requireNonNull(request.getChartLinks()).get(0));
        assertEquals("no_chart_link", Objects.requireNonNull(request.getChartLinksAbsenceExplanation()));
    }

    @Test
    public void requestCantBeCreatedIfChartLinkIsNotValid() {
        assertThrows(BadRequestException.class, () -> {
            dispenser()
                    .quotaChangeRequests()
                    .create(new Body.BodyBuilder()
                            .projectKey(DEFAULT)
                            .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(23, DiUnit.PERMILLE))
                            .description("required")
                            .sourceProjectKey(INFRA)
                            .calculations("calc")
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .chartLink("Invalid.link")
                            .chartLinksAbsenceExplanation("no_chart_link")
                            .build(), null
                    )
                    .performBy(AMOSOV_F);
        });
    }

    public static Object[][] getSolomonUrls() {
        return new Object[][]{
                {"https://solomon.yandex-team.ru/?project=dispenser_common_prod&cluster=dispenser_qloud_env&service=dispenser_prod&graph=auto&l.sensor=yandex:%20nirvana/yt-disk/yt-disk-quota&l.attribute=actual|max&b=31d&stack=0"},
                {"https://solomon.yandex-team.ru/?project=dispenser_common_prod&cluster=dispenser_qloud_env&service=dispenser_db_prod&l.sensor=-wiki-%3A+mdb%2Fio%2Fio-quota&graph=auto"}
        };
    }

    @MethodSource("getSolomonUrls")
    @ParameterizedTest
    public void requestChartLinkValidationShouldAcceptSolomonUrlsAsChartLinks(final String url) {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final long requestId = dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(TEST_SUMMARY)
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(23, DiUnit.PERMILLE))
                        .description("Required")
                        .sourceProjectKey(INFRA)
                        .calculations("Calculations")
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .chartLink(url)
                        .build(), null)
                .performBy(AMOSOV_F)
                .getFirst()
                .getId();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests().byId(requestId).get().perform();
        assertEquals(request.getChartLinks().get(0), url);
    }


    @Test
    public void requestCanBeCreatedWithoutChartLinks() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(TEST_SUMMARY)
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(23, DiUnit.PERMILLE))
                        .description("required")
                        .sourceProjectKey(INFRA)
                        .calculations("calc")
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null
                )
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests().get().perform().getFirst();

        assertEquals(0, request.getChartLinks().size());
        assertNull(request.getChartLinksAbsenceExplanation());
    }

    @Test
    public void requestCantBeUpdatedIfChartLinkIsInvalid() {
        assertThrows(BadRequestException.class, () -> {
            dispenser()
                    .quotaChangeRequests()
                    .create(new Body.BodyBuilder()
                            .projectKey(DEFAULT)
                            .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(23, DiUnit.PERMILLE))
                            .description("required")
                            .sourceProjectKey(INFRA)
                            .calculations("calc")
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                            .build(), null
                    )
                    .performBy(AMOSOV_F);

            updateHierarchy();

            final DiQuotaChangeRequest request = dispenser().quotaChangeRequests().get().perform().getFirst();
            final List<String> updatedChartLinks = new ArrayList<>();
            updatedChartLinks.add("Invalid.link");
            dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .update(new BodyUpdate(null, null, null, null,
                            updatedChartLinks, null, Collections.emptyMap(), null, null, null, null, null, null))
                    .performBy(AMOSOV_F);
        });
    }

    @Test
    public void noCancelWithoutPermissions() {
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
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.PERMILLE))
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F)
                .getFirst();
        assertFalse(request.getPermissions().isEmpty());
        dispenser()
                .properties()
                .setProperty("resource_preorder", "active", false)
                .performBy(AMOSOV_F);

        serviceDao.detachAdmin(Hierarchy.get().getServiceReader().read(YP), Hierarchy.get().getPersonReader().readPersonByLogin(ZOMB_MOBSEARCH));
        projectDao.detach(Hierarchy.get().getPersonReader().readPersonByLogin(ZOMB_MOBSEARCH), Hierarchy.get().getProjectReader().read("seehardwaremoney"), Role.MEMBER);
        updateHierarchy();

        final DiQuotaChangeRequest requestFromApi = dispenser().quotaChangeRequests().byId(request.getId()).get().perform();
        assertTrue(requestFromApi.getPermissions().isEmpty());

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(request.getId())
                    .setStatus(DiQuotaChangeRequest.Status.CANCELLED)
                    .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        }, "Can't change status to");
    }

    @Test
    public void requestCantBeCreatedIfNoActiveCampaign() {
        final Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        campaignDao.partialUpdate(activeCampaign, CampaignUpdate.builder().setStatus(Campaign.Status.CLOSED).build());
        dispenser()
                .properties()
                .setProperty("resource_preorder", "active", false)
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser().projects()
                .create(DiProject.withKey(TEST_PROJECT_KEY)
                        .withName("Test")
                        .withDescription("Test")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .withParentProject(YANDEX)
                        .withResponsibles(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .withMembers(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiExtendedProject projectByMember = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/projects/" + TEST_PROJECT_KEY)
                .query("field", "permissions")
                .get(DiExtendedProject.class);

        assertFalse(projectByMember.getPermissions().contains(CAN_CREATE_QUOTA_REQUEST));

        assertThrowsWithMessage(() -> {
            createRequests();
        }, "No campaign is active at this moment");
    }

    @Test
    public void requestCantBeCreatedIfNoCreationFlagIsSet() {
        final Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        campaignDao.partialUpdate(activeCampaign, CampaignUpdate.builder().setRequestCreationDisabled(true).build());
        dispenser()
                .properties()
                .setProperty("resource_preorder", "active", false)
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser().projects()
                .create(DiProject.withKey(TEST_PROJECT_KEY)
                        .withName("Test")
                        .withDescription("Test")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .withParentProject(YANDEX)
                        .withResponsibles(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .withMembers(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiExtendedProject projectByMember = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/projects/" + TEST_PROJECT_KEY)
                .query("field", "permissions")
                .get(DiExtendedProject.class);

        assertFalse(projectByMember.getPermissions().contains(CAN_CREATE_QUOTA_REQUEST));

        assertThrowsWithMessage(() -> {
            createRequests();
        }, "Request creation is disabled at this moment");
    }

    @Test
    public void requestCantBeCreatedWithInvalidBigOrder() {
        dispenser()
                .properties()
                .setProperty("resource_preorder", "active", false)
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser().projects()
                .create(DiProject.withKey(TEST_PROJECT_KEY)
                        .withName("Test")
                        .withDescription("Test")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .withParentProject(YANDEX)
                        .withResponsibles(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .withMembers(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiExtendedProject projectByMember = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/projects/" + TEST_PROJECT_KEY)
                .query("field", "permissions")
                .get(DiExtendedProject.class);

        assertTrue(projectByMember.getPermissions().contains(CAN_CREATE_QUOTA_REQUEST));

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .projectKey(TEST_PROJECT_KEY)
                            .changes(NIRVANA, YT_CPU, invalidBigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .build(), null)
                    .performBy(BINARY_CAT);
        }, "Big order '" + invalidBigOrder.getId() + "' is not present in the current campaign");
    }

    @Test
    public void creatingRequestShouldWorkCorrectly() {
        dispenser()
                .properties()
                .setProperty("resource_preorder", "active", true)
                .performBy(AMOSOV_F);

        updateHierarchy();

        createProject("Test", YANDEX, BINARY_CAT.getLogin());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = createRequests();

        final DiQuotaChangeRequest req = quotaRequests.getFirst();

        assertFalse(req.getPermissions().isEmpty());
    }

    @Test
    public void multipleRequestsCanBeCreated() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .batchCreate(ImmutableList.of(
                        requestBodyBuilderWithDefaultFields()
                                .projectKey(project.getPublicKey())
                                .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                                .build(),
                        requestBodyBuilderWithDefaultFields()
                                .projectKey(project.getPublicKey())
                                .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                                .build()
                ), null)
                .performBy(AMOSOV_F);

        assertEquals(2, requests.size());
    }

    @Test
    public void multipleRequestsCanBeCreatedWithDifferentGoals() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .batchCreate(ImmutableList.of(
                        requestBodyBuilderWithDefaultFields()
                                .projectKey(project.getPublicKey())
                                .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                                .goalId(TEST_GOAL_BATCH_ONE_ID)
                                .build(),
                        requestBodyBuilderWithDefaultFields()
                                .projectKey(project.getPublicKey())
                                .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                                .goalId(TEST_GOAL_BATCH_TWO_ID)
                                .build()
                ), null)
                .performBy(AMOSOV_F);

        assertEquals(2, requests.size());
    }

    @Test
    public void multipleRequestsCanBeCreatedWithSameGoals() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .batchCreate(ImmutableList.of(
                        requestBodyBuilderWithDefaultFields()
                                .projectKey(project.getPublicKey())
                                .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                                .goalId(TEST_GOAL_BATCH_ONE_ID)
                                .build(),
                        requestBodyBuilderWithDefaultFields()
                                .projectKey(project.getPublicKey())
                                .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                                .goalId(TEST_GOAL_BATCH_ONE_ID)
                                .build()
                ), null)
                .performBy(AMOSOV_F);

        assertEquals(2, requests.size());
    }

    @Test
    public void quotaChangeRequestCantBeCreatedForInvalidBigOrder() {
        assertThrows(BadRequestException.class, () -> {
            final String newResourceKey = "UNIQUE-RESOURCE";
            dispenser()
                    .service(YP)
                    .resource(newResourceKey)
                    .create()
                    .withName(newResourceKey)
                    .withType(DiResourceType.TRAFFIC)
                    .performBy(AMOSOV_F);

            updateHierarchy();

            dispenser()
                    .service(YP)
                    .resource(newResourceKey)
                    .segmentations()
                    .update(ImmutableList.of(
                            new DiSegmentation.Builder(DC_SEGMENTATION)
                                    .withName("DC")
                                    .withDescription("DC")
                                    .build(),
                            new DiSegmentation.Builder(SEGMENT_SEGMENTATION)
                                    .withName("segment")
                                    .withDescription("segment")
                                    .build()
                    )).performBy(AMOSOV_F);

            updateHierarchy();

            final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .projectKey(YANDEX)
                            .changes(YP, newResourceKey, invalidBigOrder.getId(), Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(42, DiUnit.TBPS))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .chartLink("https://st.yandex.ru")
                            .chartLinksAbsenceExplanation("Test explanation")
                            .build(), null)
                    .performBy(AMOSOV_F);


            assertEquals(0, requests.size());
        });
    }

    @Test
    public void updatedRequestTicketShouldHaveUpdateCommentOnStatusChange() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        updateHierarchy();

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.CANCELLED)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final String trackerIssueKey = request.getTrackerIssueKey();

        List<CommentCreate> comments = trackerManager.getIssueComments(trackerIssueKey);
        assertEquals(1, comments.size());
        assertEquals("кем:amosov-f заявка переведена в статус %%Отменена%%", comments.get(0).getComment().get());
        assertTrue(comments.get(0).getSummonees().isEmpty());

        Map<String, Object> issue = trackerManager.getIssueFields(trackerIssueKey);
        assertEquals("closed", issue.get("status"));

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.NEW)
                .performBy(AMOSOV_F);

        comments = trackerManager.getIssueComments(trackerIssueKey);
        assertEquals(2, comments.size());
        assertEquals("кем:amosov-f заявка переведена в статус %%Черновик%%", comments.get(1).getComment().get());
        assertEquals(Sets.newHashSet(comments.get(1).getSummonees()), ImmutableSet.of(AMOSOV_F.getLogin()));

        issue = trackerManager.getIssueFields(trackerIssueKey);
        assertEquals("open", issue.get("status"));

        updateHierarchy();

        quotaChangeRequestDao.update(quotaChangeRequestDao.read(request.getId()).copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.REJECTED)
                .performBy(KEYD);

        comments = trackerManager.getIssueComments(trackerIssueKey);
        assertEquals(3, comments.size());

        issue = trackerManager.getIssueFields(trackerIssueKey);
        assertEquals("closed", issue.get("status"));

        updateHierarchy();

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.COUNT))
                        .comment("updated!!!")
                        .build())
                .performBy(AMOSOV_F);

        comments = trackerManager.getIssueComments(trackerIssueKey);
        assertEquals(4, comments.size());
        assertEquals("кем:amosov-f заявка переведена в статус %%Черновик%%\n" +
                "\n" +
                "Ресурсы изменены кем:amosov-f\n" +
                "\n" +
                "* YT CPU: 100 units -> **!!(red)200 units!!**\n" +
                "\n" +
                "\n" +
                "Описание изменено кем:amosov-f", comments.get(3).getComment().get());
        assertEquals(Sets.newHashSet(comments.get(3).getSummonees()), ImmutableSet.of(AMOSOV_F.getLogin()));

        issue = trackerManager.getIssueFields(trackerIssueKey);
        assertEquals("open", issue.get("status"));
    }

    @Test
    public void updatedRequestTicketShouldHaveUpdateCommentOnStatusChangeSummonSuppressed() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        updateHierarchy();

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.CANCELLED, true)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final String trackerIssueKey = request.getTrackerIssueKey();

        List<CommentCreate> comments = trackerManager.getIssueComments(trackerIssueKey);
        assertEquals(1, comments.size());
        assertEquals("кем:amosov-f заявка переведена в статус %%Отменена%%", comments.get(0).getComment().get());
        assertTrue(comments.get(0).getSummonees().isEmpty());

        Map<String, Object> issue = trackerManager.getIssueFields(trackerIssueKey);
        assertEquals("closed", issue.get("status"));

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.NEW, true)
                .performBy(AMOSOV_F);

        comments = trackerManager.getIssueComments(trackerIssueKey);
        assertEquals(2, comments.size());
        assertEquals("кем:amosov-f заявка переведена в статус %%Черновик%%", comments.get(1).getComment().get());
        assertTrue(comments.get(1).getSummonees().isEmpty());

        issue = trackerManager.getIssueFields(trackerIssueKey);
        assertEquals("open", issue.get("status"));

        updateHierarchy();

        quotaChangeRequestDao.update(quotaChangeRequestDao.read(request.getId()).copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.REJECTED, true)
                .performBy(KEYD);

        comments = trackerManager.getIssueComments(trackerIssueKey);
        assertEquals(3, comments.size());

        issue = trackerManager.getIssueFields(trackerIssueKey);
        assertEquals("closed", issue.get("status"));

        updateHierarchy();

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.COUNT))
                        .comment("updated!!!")
                        .build(), true)
                .performBy(AMOSOV_F);

        comments = trackerManager.getIssueComments(trackerIssueKey);
        assertEquals(4, comments.size());
        assertEquals("кем:amosov-f заявка переведена в статус %%Черновик%%\n" +
                "\n" +
                "Ресурсы изменены кем:amosov-f\n" +
                "\n" +
                "* YT CPU: 100 units -> **!!(red)200 units!!**\n" +
                "\n" +
                "\n" +
                "Описание изменено кем:amosov-f", comments.get(3).getComment().get());
        assertTrue(comments.get(3).getSummonees().isEmpty());

        issue = trackerManager.getIssueFields(trackerIssueKey);
        assertEquals("open", issue.get("status"));
    }

    @Test
    public void updatedRequestTicketShouldHaveUpdateCommentOnChanges() {
        serviceDao.create(Service.withKey(QLOUD)
                .withAbcServiceId(QLOUD_ABC_SERVICE_ID)
                .withName("QLOUD")
                .build());

        updateHierarchy();

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        dispenser()
                .service(QLOUD)
                .resource(CPU)
                .create()
                .withName("CPU")
                .withDescription("CPU")
                .withType(DiResourceType.PROCESSOR)
                .inMode(DiQuotingMode.SYNCHRONIZATION)
                .performBy(AMOSOV_F);

        updateHierarchy();

        prepareCampaignResources();

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(QLOUD, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        updateHierarchy();

        final Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put("segment", "cocs");
        additionalProperties.put("installation", "qloud");
        additionalProperties.put("project", "super project");

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .additionalProperties(additionalProperties)
                        .changes(QLOUD, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final String trackerIssueKey = request.getTrackerIssueKey();

        List<CommentCreate> comments = trackerManager.getIssueComments(trackerIssueKey);
        assertEquals(1, comments.size());
        assertEquals("Описание изменено кем:amosov-f", comments.get(0).getComment().get());
        assertEquals(Collections.emptyList(), comments.get(0).getSummonees());

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(QLOUD, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.CORES))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        comments = trackerManager.getIssueComments(trackerIssueKey);
        assertEquals(2, comments.size());
        assertEquals("Ресурсы изменены кем:amosov-f\n" +
                "\n" +
                "* CPU: 100 cores -> **!!(red)200 cores!!**\n", comments.get(1).getComment().get());
        assertEquals(Collections.emptyList(), comments.get(1).getSummonees());
    }

    @Test
    public void updatedRequestTicketShouldHaveCorrectCommentsOnChangesUpdate() {
        final Service qloud = serviceDao.create(Service.withKey(QLOUD)
                .withAbcServiceId(QLOUD_ABC_SERVICE_ID)
                .withName("QLOUD")
                .build());

        updateHierarchy();

        final ResourceGroup group1 = resourceGroupDao.create(new ResourceGroup.Builder("test_group_1", qloud).name("Test Group 1").priority(0).build());
        final ResourceGroup group2 = resourceGroupDao.create(new ResourceGroup.Builder("test_group_2", qloud).name("Test Group 2").priority(1).build());

        updateHierarchy();

        resourceDao.createAll(Arrays.asList(
                new Resource.Builder(CPU, qloud).name("CPU").description("CPU").type(DiResourceType.PROCESSOR).mode(DiQuotingMode.SYNCHRONIZATION).group(group1).priority(0).build(),
                new Resource.Builder(RAM, qloud).name("RAM").description("RAM").type(DiResourceType.MEMORY).mode(DiQuotingMode.SYNCHRONIZATION).group(group1).priority(1).build(),
                new Resource.Builder(SSD, qloud).name("SSD").description("SSD").type(DiResourceType.STORAGE).mode(DiQuotingMode.SYNCHRONIZATION).group(group2).priority(0).build(),
                new Resource.Builder(HDD, qloud).name("HDD").description("HDD").type(DiResourceType.STORAGE).mode(DiQuotingMode.SYNCHRONIZATION).group(group2).priority(1).build(),
                new Resource.Builder("io_ssd", qloud).name("IO SSD").description("IO SSD").type(DiResourceType.BINARY_TRAFFIC).mode(DiQuotingMode.SYNCHRONIZATION).noGroup().priority(0).build(),
                new Resource.Builder("io_hdd", qloud).name("IO HDD").description("IO HDD").type(DiResourceType.BINARY_TRAFFIC).mode(DiQuotingMode.SYNCHRONIZATION).noGroup().priority(1).build()
        ));

        updateHierarchy();

        prepareCampaignResources();

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(QLOUD, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(QLOUD, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(QLOUD, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(QLOUD, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(QLOUD, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(QLOUD, "io_ssd", bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(QLOUD, "io_hdd", bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.MIBPS))
                        .build())
                .performBy(AMOSOV_F);

        final String trackerIssueKey = request.getTrackerIssueKey();
        final List<CommentCreate> issueComments = trackerManager.getIssueComments(trackerIssueKey);
        assertEquals(1, issueComments.size());
        assertEquals("Ресурсы изменены кем:amosov-f\n" +
                "\n* IO HDD: 0 Bps -> **!!(red)100 MiBps!!**\n" +
                "* IO SSD: 0 Bps -> **!!(red)100 MiBps!!**\n" +
                "* Test Group 1\n" +
                "  * CPU: 100 cores\n" +
                "  * RAM: 0 B -> **!!(red)100 GiB!!**\n" +
                "* Test Group 2\n" +
                "  * HDD: 0 B -> **!!(red)100 GiB!!**\n" +
                "  * SSD: 0 B -> **!!(red)100 GiB!!**\n", issueComments.get(0).getComment().get());

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(QLOUD, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.KILO_CORES))
                        .changes(QLOUD, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.MEBIBYTE))
                        .changes(QLOUD, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(QLOUD, "io_ssd", bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.MIBPS))
                        .changes(QLOUD, "io_hdd", bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(0L, DiUnit.MIBPS))
                        .build())
                .performBy(AMOSOV_F);

        assertEquals("Ресурсы изменены кем:amosov-f\n\n" +
                "* IO HDD: 100 MiBps -> **!!(green)0 Bps!!**\n" +
                "* IO SSD: 100 MiBps -> **!!(red)200 MiBps!!**\n" +
                "* Test Group 1\n" +
                "  * CPU: 100 cores -> **!!(red)200 K cores!!**\n" +
                "  * RAM: 100 GiB -> **!!(green)100 MiB!!**\n" +
                "* Test Group 2\n" +
                "  * --HDD: 100 GiB--\n" +
                "  * SSD: 100 GiB\n", issueComments.get(1).getComment().get());
    }

    public DiListResponse<DiQuotaChangeRequest> createRequests() {
        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithTypeResourcePreorder(bigOrderOne)
                        .build(), null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        return requests;
    }


    @Test
    void requestsCanBeFilteredByOrderId() {
        final Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        campaignDao.partialUpdate(activeCampaign, CampaignUpdate.builder().setBigOrders(
                        ImmutableList.of(new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                                new Campaign.BigOrder(secondBigOrder.getId(), LocalDate.of(2119, 1, 1)), new Campaign.BigOrder(thirdBigOrder.getId(), LocalDate.of(2119, 1, 1))))
                .build());

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(MDB, RAM, secondBigOrder.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, secondBigOrder.getId(), Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(256, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, STORAGE, thirdBigOrder.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        assertEquals(1, dispenser()
                .quotaChangeRequests()
                .get()
                .query("order", String.valueOf(bigOrderOne.getId()))
                .perform().size());

        assertEquals(2, dispenser()
                .quotaChangeRequests()
                .get()
                .query("order", String.valueOf(secondBigOrder.getId()))
                .perform().size());

        assertEquals(3, dispenser()
                .quotaChangeRequests()
                .get()
                .query("order", String.valueOf(thirdBigOrder.getId()))
                .query("order", String.valueOf(secondBigOrder.getId()))
                .perform().size());

    }

    @Test
    void requestsCanBeFilteredBySummary() {
        final Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        campaignDao.partialUpdate(activeCampaign, CampaignUpdate.builder().setBigOrders(ImmutableList.of(new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(secondBigOrder.getId(), LocalDate.of(2119, 1, 1)), new Campaign.BigOrder(thirdBigOrder.getId(), LocalDate.of(2119, 1, 1))))
                .build());

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .summary("тест")
                        .projectKey(project.getPublicKey())
                        .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .summary("summary")
                        .projectKey(project.getPublicKey())
                        .changes(MDB, RAM, secondBigOrder.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .summary("two words")
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, secondBigOrder.getId(), Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(256, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .summary("two words text")
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, STORAGE, thirdBigOrder.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        assertEquals(1, dispenser()
                .quotaChangeRequests()
                .get()
                .query("summary", "тест")
                .perform().size());

        assertEquals(1, dispenser()
                .quotaChangeRequests()
                .get()
                .query("summary", "summary")
                .perform().size());

        assertEquals(0, dispenser()
                .quotaChangeRequests()
                .get()
                .query("summary", "not found")
                .perform().size());

        assertEquals(4, dispenser()
                .quotaChangeRequests()
                .get()
                .query("summary", "")
                .perform().size());

        assertEquals(2, dispenser()
                .quotaChangeRequests()
                .get()
                .query("summary", "two words")
                .perform().size());

        assertEquals(1, dispenser()
                .quotaChangeRequests()
                .get()
                .query("summary", "two words text")
                .perform().size());

    }

    @Test
    void requestsCanBeFilteredByCampaignId() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(MDB, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(256, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        assertEquals(4, dispenser()
                .quotaChangeRequests()
                .get()
                .query("campaign", String.valueOf(activeCampaign.getId()))
                .perform().size());

    }

    @Test
    void requestsCanBeFilteredByCampaignOrderId() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        campaignDao.partialUpdate(activeCampaign, CampaignUpdate.builder().setBigOrders(ImmutableList.of(new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(secondBigOrder.getId(), LocalDate.of(2119, 1, 1)), new Campaign.BigOrder(thirdBigOrder.getId(), LocalDate.of(2119, 1, 1))))
                .build());
        activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();

        prepareCampaignResources();
        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(MDB, RAM, secondBigOrder.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, secondBigOrder.getId(), Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(256, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, STORAGE, thirdBigOrder.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        assertEquals(1, dispenser()
                .quotaChangeRequests()
                .get()
                .query("campaignOrder", String.valueOf(activeCampaign.getBigOrders().get(0).getId()))
                .perform().size());

        assertEquals(2, dispenser()
                .quotaChangeRequests()
                .get()
                .query("campaignOrder", String.valueOf(activeCampaign.getBigOrders().get(1).getId()))
                .perform().size());

        assertEquals(3, dispenser()
                .quotaChangeRequests()
                .get()
                .query("campaignOrder", String.valueOf(activeCampaign.getBigOrders().get(2).getId()))
                .query("campaignOrder", String.valueOf(activeCampaign.getBigOrders().get(1).getId()))
                .perform().size());

    }

    @Test
    public void requestCreationIsAllowed() {
        final Set<DiExtendedProject.Permission> permissions = getProjectPermissionsFor(WHISTLER.chooses(INFRA));
        assertTrue(permissions.contains(CAN_CREATE_QUOTA_REQUEST));
    }

    @Test
    public void requestCreationIsForbiddenIfNoActiveCampaign() {
        final Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        campaignDao.partialUpdate(activeCampaign, CampaignUpdate.builder().setStatus(Campaign.Status.CLOSED).build());

        final Set<DiExtendedProject.Permission> permissions = getProjectPermissionsFor(WHISTLER.chooses(INFRA));
        assertFalse(permissions.contains(CAN_CREATE_QUOTA_REQUEST));

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .projectKey(INFRA)
                            .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .build(), null)
                    .performBy(WHISTLER);
        }, "No campaign is active at this moment");
    }

    @Test
    public void requestCreationIsForbiddenIfNoCreationFlagIsSet() {
        final Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        campaignDao.partialUpdate(activeCampaign, CampaignUpdate.builder().setRequestCreationDisabled(true).build());

        final Set<DiExtendedProject.Permission> permissions = getProjectPermissionsFor(WHISTLER.chooses(INFRA));
        assertFalse(permissions.contains(CAN_CREATE_QUOTA_REQUEST));

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .projectKey(INFRA)
                            .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .build(), null)
                    .performBy(WHISTLER);
        }, "Request creation is disabled at this moment");
    }

    @Test
    public void preorderIdsInRequest() {
        createProject("Test", YANDEX, BINARY_CAT.getLogin());

        final Project project = projectDao.read("Test");
        Service service = Service.withKey("test")
                .withAbcServiceId(TEST_ABC_SERVICE_ID)
                .withName("Test")
                .build();
        service = serviceDao.create(service);

        final long reqId = createRequests().getFirst().getId();
        final long changeId = quotaChangeRequestDao.read(reqId).getChanges().get(0).getId();

        mappedPreOrderDao.clear();
        botPreOrderDao.clear();
        botPreOrderChangeDao.clear();

        final CampaignForBot activeCampaign = campaignDao.getActiveForBotIntegration().get();
        createPreOrders(Arrays.asList(
                new MappedPreOrder.Builder().id(1L).project(project).service(service).bigOrderId(bigOrderOne.getId()).campaignGroupId(botCampaignGroup.getId())
                        .groupKey(DEFAULT_GROUP_KEY).bigOrderConfigId(1L).name("test name").build(),
                new MappedPreOrder.Builder().id(2L).project(project).service(service).bigOrderId(bigOrderOne.getId()).campaignGroupId(botCampaignGroup.getId())
                        .groupKey(DEFAULT_GROUP_KEY).bigOrderConfigId(2L).name("test name").build()
        ));

        botPreOrderChangeDao.setOrderChanges(1L, Collections.singletonMap(changeId, 1.0));
        botPreOrderChangeDao.setOrderChanges(2L, Collections.singletonMap(changeId, 1.0));

        final DiQuotaChangeRequest request = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + reqId)
                .get(DiQuotaChangeRequest.class);
        final List<Long> preOrderIds = request.getBotPreOrderIds();
        final String preOrdersUrl = request.getBotPreOrdersUrl();

        assertTrue(preOrderIds.contains(1L));
        assertTrue(preOrderIds.contains(2L));
        assertEquals("https://test.bot.yandex-team.ru/hwr/preorders/filter=id:1,2", preOrdersUrl);

        final DiQuotaChangeRequest requestNotResponsible = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + reqId)
                .get(DiQuotaChangeRequest.class);
        final List<Long> preOrderIdsNotResponsible = requestNotResponsible.getBotPreOrderIds();
        final String preOrdersUrlNotResponsible = requestNotResponsible.getBotPreOrdersUrl();

        assertFalse(preOrderIdsNotResponsible.isEmpty());
        assertNotNull(preOrdersUrlNotResponsible);
    }

    @Test
    public void createCommentOnNotification() {
        createProject("Test", YANDEX, BINARY_CAT.getLogin());

        final long reqId = createRequests().getFirst().getId();
        final QuotaChangeRequest request = quotaChangeRequestDao.read(reqId);

        quotaChangeRequestTicketManager.sendDescriptionChangedComment(request, "zamysh", "denblo", false);

        assertNotNull(request.getTrackerIssueKey());
        final List<CommentCreate> comments = trackerManager.getIssueComments(request.getTrackerIssueKey());
        assertFalse(comments.isEmpty());

        final CommentCreate comment = comments.get(0);
        assertTrue(comment.getSummonees().containsTs("denblo"));
        assertTrue(comment.getComment().isMatch(text -> text.contains("кем:zamysh")
                && text.contains(
                "ru/services/Test/hardware/?view=consuming&edit=" + reqId)));
    }

    @Test
    public void createCommentOnNotificationSuppressSummon() {
        createProject("Test", YANDEX, BINARY_CAT.getLogin());

        final long reqId = createRequests().getFirst().getId();
        final QuotaChangeRequest request = quotaChangeRequestDao.read(reqId);

        quotaChangeRequestTicketManager.sendDescriptionChangedComment(request, "zamysh", "denblo", true);

        assertNotNull(request.getTrackerIssueKey());
        final List<CommentCreate> comments = trackerManager.getIssueComments(request.getTrackerIssueKey());
        assertFalse(comments.isEmpty());

        final CommentCreate comment = comments.get(0);
        assertTrue(comment.getSummonees().isEmpty());
        assertTrue(comment.getComment().isMatch(text -> text.contains("кем:zamysh")
                && text.contains(
                "ru/services/Test/hardware/?view=consuming&edit=" + reqId)));
    }

    private Set<DiExtendedProject.Permission> getProjectPermissionsFor(final DiPerformer performer) {
        final DiExtendedProject project = createAuthorizedLocalClient(performer.getLogin())
                .path("/v1/projects/" + performer.getProjectKey())
                .query("field", "permissions")
                .get(DiExtendedProject.class);
        return project.getPermissions();
    }

    @Test
    public void requestUpdateIsForbiddenIfNoActiveCampaign() {
        dispenser().projects()
                .create(DiProject.withKey("Test")
                        .withName("Test")
                        .withDescription("Test")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .withParentProject(YANDEX)
                        .withResponsibles(DiPersonGroup.builder().addPersons(AMOSOV_F.getLogin()).build())
                        .withMembers(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final long requestId = createRequests().getFirst().getId();

        updateHierarchy();

        final Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        campaignDao.partialUpdate(activeCampaign, CampaignUpdate.builder().setStatus(Campaign.Status.CLOSED).build());

        Set<DiQuotaChangeRequest.Permission> permissions;

        permissions = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/quota-requests/" + requestId)
                .get(DiQuotaChangeRequest.class)
                .getPermissions();

        assertFalse(permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT));

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(requestId)
                    .update(new BodyUpdate.BodyUpdateBuilder()
                            .description("new")
                            .build())
                    .performBy(WHISTLER);

        }, "Request can't be updated at this campaign stage");

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(requestId)
                    .setStatus(DiQuotaChangeRequest.Status.CANCELLED)
                    .performBy(BINARY_CAT);

        }, "Can't change status to CANCELLED. No other statuses available for request");

        campaignDao.partialUpdate(activeCampaign, CampaignUpdate.builder().setStatus(Campaign.Status.ACTIVE).build());

        updateHierarchy();

        permissions = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/quota-requests/" + requestId)
                .get(DiQuotaChangeRequest.class)
                .getPermissions();

        assertTrue(permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT));

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .description("new")
                        .build())
                .performBy(WHISTLER);

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .description("new")
                        .build())
                .performBy(BINARY_CAT);

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.CANCELLED)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.NEW)
                .performBy(AMOSOV_F);

        quotaChangeRequestDao.update(quotaChangeRequestDao.read(requestId).copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.REJECTED)
                .performBy(KEYD);
    }

    @Test
    public void requestUpdateIsForbiddenIfUpdateDisabled() {
        dispenser().projects()
                .create(DiProject.withKey(TEST_PROJECT_KEY)
                        .withName("Test")
                        .withDescription("Test")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .withParentProject(YANDEX)
                        .withResponsibles(DiPersonGroup.builder().addPersons(AMOSOV_F.getLogin()).build())
                        .withMembers(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final long requestId = createRequests().getFirst().getId();

        updateHierarchy();

        final Campaign activeCampaign = campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        campaignDao.partialUpdate(activeCampaign, CampaignUpdate.builder().setRequestModificationDisabled(true).build());

        Set<DiQuotaChangeRequest.Permission> permissions;

        permissions = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + requestId)
                .get(DiQuotaChangeRequest.class)
                .getPermissions();

        assertFalse(permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT));

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(requestId)
                    .update(new BodyUpdate.BodyUpdateBuilder()
                            .description("new")
                            .build())
                    .performBy(BINARY_CAT);

        }, "Request can't be updated at this campaign stage");

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(requestId)
                    .setStatus(DiQuotaChangeRequest.Status.CANCELLED)
                    .performBy(BINARY_CAT);

        }, "Can't change status to CANCELLED. No other statuses available for request");

        campaignDao.partialUpdate(activeCampaign, CampaignUpdate.builder().setRequestModificationDisabled(false).build());

        updateHierarchy();

        permissions = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/quota-requests/" + requestId)
                .get(DiQuotaChangeRequest.class)
                .getPermissions();

        assertTrue(permissions.contains(DiQuotaChangeRequest.Permission.CAN_EDIT));

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .description("new")
                        .build())
                .performBy(BINARY_CAT);

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.CANCELLED)
                .performBy(BINARY_CAT);

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.NEW)
                .performBy(BINARY_CAT);

        quotaChangeRequestDao.update(quotaChangeRequestDao.read(requestId).copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.REJECTED)
                .performBy(KEYD);
    }

    @Test
    public void quotaRequestChangesOrderShouldBeStable() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_2), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_1), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_1), DiAmount.of(50L, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_2), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_2), DiAmount.of(50L, DiUnit.GIBIBYTE))
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(SEGMENT_SEGMENT_2, DC_SEGMENT_1), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests().get().query("project", YANDEX).perform();
        assertEquals(2, requests.size());

        final List<List<DiQuotaChangeRequest.Change>> changes = requests.stream().map(DiQuotaChangeRequest::getChanges).collect(Collectors.toList());

        final Map<String, List<Set<String>>> segmentsByResources1 = changes.get(0).stream().collect(Collectors.groupingBy(c -> c.getResource().getKey(), Collectors.mapping(DiQuotaChangeRequest.Change::getSegmentKeys, Collectors.toList())));
        final Map<String, List<Set<String>>> segmentsByResources2 = changes.get(1).stream().collect(Collectors.groupingBy(c -> c.getResource().getKey(), Collectors.mapping(DiQuotaChangeRequest.Change::getSegmentKeys, Collectors.toList())));
        assertEquals(segmentsByResources1.keySet(), segmentsByResources2.keySet());
        for (final String resourceKey : segmentsByResources1.keySet()) {
            assertEquals(segmentsByResources1.get(resourceKey), segmentsByResources2.get(resourceKey));
        }

        final Map<Set<String>, List<String>> resourcesBySegments1 = changes.get(0).stream().collect(Collectors.groupingBy(DiQuotaChangeRequest.Change::getSegmentKeys, Collectors.mapping(c -> c.getResource().getKey(), Collectors.toList())));
        final Map<Set<String>, List<String>> resourcesBySegments2 = changes.get(1).stream().collect(Collectors.groupingBy(DiQuotaChangeRequest.Change::getSegmentKeys, Collectors.mapping(c -> c.getResource().getKey(), Collectors.toList())));
        assertEquals(resourcesBySegments1.keySet(), resourcesBySegments2.keySet());
        for (final Set<String> segmentKeys : resourcesBySegments1.keySet()) {
            assertEquals(resourcesBySegments1.get(segmentKeys), resourcesBySegments2.get(segmentKeys));
        }
    }

    @Test
    public void resourceOrderForQuotaRequestWithDifferentSegmentsShouldBeTheSame() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(50L, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(500L, DiUnit.GIBIBYTE))
                        .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(5000L, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests().get().query("project", YANDEX).perform();
        assertEquals(2, requests.size());

        final List<List<DiQuotaChangeRequest.Change>> changes = requests.stream().map(DiQuotaChangeRequest::getChanges).collect(Collectors.toList());

        final Map<Set<String>, List<String>> resourcesBySegments1 = changes.get(0).stream().collect(Collectors.groupingBy(DiQuotaChangeRequest.Change::getSegmentKeys, Collectors.mapping(c -> c.getResource().getKey(), Collectors.toList())));
        final Map<Set<String>, List<String>> resourcesBySegments2 = changes.get(1).stream().collect(Collectors.groupingBy(DiQuotaChangeRequest.Change::getSegmentKeys, Collectors.mapping(c -> c.getResource().getKey(), Collectors.toList())));
        assertEquals(1, resourcesBySegments1.size());
        assertEquals(1, resourcesBySegments2.size());

        final List<String> resources1 = resourcesBySegments1.values().iterator().next();
        final List<String> resources2 = resourcesBySegments2.values().iterator().next();
        assertEquals(resources1, resources2);
    }

    @Test
    public void quotaRequestOnResourcePreorderCanBeEditedInApprovedStatus() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        quotaChangeRequestDao.update(
                quotaChangeRequestDao.read(request.getId()).copyBuilder().status(QuotaChangeRequest.Status.APPROVED).build());

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.COUNT))
                        .build())
                .performBy(WHISTLER);

        assertEquals(200, SpyWebClient.lastResponseStatus());
    }

    @Test
    public void quotaRequestOnResourcePreorderCanBeUpdatedByProcessResponsible() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        final Person person = Hierarchy.get().getPersonReader().readPersonByLogin(SLONNN.getLogin());
        projectDao.attach(person, Hierarchy.get().getProjectReader().getRoot(), Role.PROCESS_RESPONSIBLE);

        updateHierarchy();

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.COUNT))
                        .build())
                .performBy(SLONNN);

        assertEquals(200, SpyWebClient.lastResponseStatus());
    }

    @Test
    public void quotaRequestOnResourcePreorderCanBeEditedInConfirmedStatus() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        quotaChangeRequestDao.update(
                quotaChangeRequestDao.read(request.getId()).copyBuilder().requestGoalAnswers(GROWTH_ANSWER).status(QuotaChangeRequest.Status.CONFIRMED).build());

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(99L, DiUnit.COUNT))
                        .build())
                .performBy(WHISTLER);

        assertEquals(200, SpyWebClient.lastResponseStatus());

        request = dispenser().
                quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, request.getStatus());

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(99L, DiUnit.COUNT))
                        .build())
                .performBy(WHISTLER);

        assertEquals(200, SpyWebClient.lastResponseStatus());

        request = dispenser().
                quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, request.getStatus());

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .build())
                .performBy(WHISTLER);

        assertEquals(200, SpyWebClient.lastResponseStatus());

        request = dispenser().
                quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, request.getStatus());
    }

    @Test
    public void quotaRequestOnResourcePreorderCanBeEditedInConfirmedStatusByProcessResponsible() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        final Person person = Hierarchy.get().getPersonReader().readPersonByLogin(SLONNN.getLogin());
        projectDao.attach(person, Hierarchy.get().getProjectReader().getRoot(), Role.PROCESS_RESPONSIBLE);

        updateHierarchy();

        quotaChangeRequestDao.update(
                quotaChangeRequestDao.read(request.getId()).copyBuilder().requestGoalAnswers(GROWTH_ANSWER).status(QuotaChangeRequest.Status.CONFIRMED).build());

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(99L, DiUnit.COUNT))
                        .build())
                .performBy(SLONNN);

        assertEquals(200, SpyWebClient.lastResponseStatus());

        request = dispenser().
                quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, request.getStatus());

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(99L, DiUnit.COUNT))
                        .build())
                .performBy(SLONNN);

        assertEquals(200, SpyWebClient.lastResponseStatus());

        request = dispenser().
                quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, request.getStatus());

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .build())
                .performBy(SLONNN);

        assertEquals(200, SpyWebClient.lastResponseStatus());

        request = dispenser().
                quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, request.getStatus());
    }

    @Test
    public void confirmedRequestCanBeUpdatedWithDefenceFieldsAndSummary() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        quotaChangeRequestDao.update(
                quotaChangeRequestDao.read(request.getId()).copyBuilder().requestGoalAnswers(GROWTH_ANSWER).status(QuotaChangeRequest.Status.CONFIRMED).build());

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(Maps.transformValues(GROWTH_ANSWER, v -> v + "foo"))
                        .comment("foo42")
                        .chartLinksAbsenceExplanation("tratata")
                        .comment("foo42")
                        .calculations("2 + 2 = 5")
                        .summary("123456")
                        .build())
                .performBy(WHISTLER);

        assertEquals(200, SpyWebClient.lastResponseStatus());

        request = dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, request.getStatus());
    }

    @Test
    public void confirmedRequestCanBeUpdatedWithDefenceFieldsAndSummaryByProcessResponsible() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        quotaChangeRequestDao.update(
                quotaChangeRequestDao.read(request.getId()).copyBuilder().requestGoalAnswers(GROWTH_ANSWER).status(QuotaChangeRequest.Status.CONFIRMED).build());

        final Person person = Hierarchy.get().getPersonReader().readPersonByLogin(SLONNN.getLogin());
        projectDao.attach(person, Hierarchy.get().getProjectReader().getRoot(), Role.PROCESS_RESPONSIBLE);

        updateHierarchy();

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(Maps.transformValues(GROWTH_ANSWER, v -> v + "foo"))
                        .comment("foo42")
                        .chartLinksAbsenceExplanation("tratata")
                        .comment("foo42")
                        .calculations("2 + 2 = 5")
                        .summary("123456")
                        .build())
                .performBy(SLONNN);

        assertEquals(200, SpyWebClient.lastResponseStatus());

        request = dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, request.getStatus());
    }

    @Test
    public void confirmedRequestCanBeUpdatedWithImportanceFlagTest() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        quotaChangeRequestDao.update(
                quotaChangeRequestDao.read(request.getId()).copyBuilder().requestGoalAnswers(GROWTH_ANSWER).status(QuotaChangeRequest.Status.CONFIRMED).build());

        dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .importantRequest(true)
                        .build())
                .performBy(WHISTLER);

        assertEquals(200, SpyWebClient.lastResponseStatus());

        request = dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, request.getStatus());
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderCantBeCreatedWithTypeGrowthAndGoalId() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithTypeResourcePreorder(bigOrderOne)
                            .goalId(TEST_GOAL_ID)
                            .build(), null)
                    .performBy(BINARY_CAT);
        }, "Goal must be empty for request with reason type \\\"Growth\\\"");
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderCantBeCreatedWithTypeGoalAndWithNoGoalId() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithTypeResourcePreorder(bigOrderOne)
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                            .build(), null)
                    .performBy(BINARY_CAT);
        }, "Goal is required for request with reason type \\\"Goal\\\"");
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderCanBeCreatedWithTypeGrowth() {
        final DiProject diProject = createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final Project testProject = projectDao.read(diProject.getKey());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithTypeResourcePreorder(bigOrderOne)
                        .build(), null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        final DiQuotaChangeRequest requestByCreator = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertEquals(DiResourcePreorderReasonType.GROWTH, requestByCreator.getResourcePreorderReasonType());
        assertNull(requestByCreator.getGoal());
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderCanBeCreatedWithTypeGoal() {
        final DiProject diProject = createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final Project testProject = projectDao.read(diProject.getKey());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        final DiQuotaChangeRequest requestByCreator = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertEquals(DiResourcePreorderReasonType.GOAL, requestByCreator.getResourcePreorderReasonType());
        assertEquals(TEST_GOAL_ID, Long.valueOf(requestByCreator.getGoal().getId()));
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderCanBeCreatedWithoutPreorderType() {
        final DiProject diProject = createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final Project testProject = projectDao.read(diProject.getKey());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .build(), null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        final DiQuotaChangeRequest requestByCreator = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertEquals(DiResourcePreorderReasonType.GROWTH, requestByCreator.getResourcePreorderReasonType());
    }

    @Test
    public void quotaRequestUpdateGoalId() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final long thirdGoal = 369L;
        final Goal goal = goalDao.create(new Goal(thirdGoal, "Normal Goal", Importance.DEPARTMENT, Status.NEW, OkrAncestors.EMPTY));

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, null, Collections.emptyMap(), null, goal.getId(), null, null, null, null))
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        final long goalId = Objects.requireNonNull(diQuotaChangeRequest.getGoal()).getId();
        assertEquals(thirdGoal, goalId);
    }

    @Test
    public void quotaRequestUpdateSummary() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, null, Collections.emptyMap(), null, null, null, "updated", null, null))
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();
        assertEquals("updated", diQuotaChangeRequest.getSummary());
    }

    @Test
    public void quotaRequestUpdateProject() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final DiProject anotherProject = createProject("another_project", YANDEX, BINARY_CAT.getLogin());

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        List<CommentCreate> issueComments = trackerManager.getIssueComments(request.getTrackerIssueKey());

        assertTrue(issueComments.isEmpty());

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, null, Collections.emptyMap(), null, null, null, null, anotherProject.getKey(), null))
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();
        assertEquals(anotherProject, diQuotaChangeRequest.getProject());

        final QuotaChangeRequestHistoryFilterImpl historyFilter = new QuotaChangeRequestHistoryFilterImpl(Instant.MIN,
                Instant.MAX, Set.of(), Set.of(), Set.of(DiQuotaRequestHistoryEventType.PROJECT_UPDATE));
        final RelativePageInfo pageInfo = new RelativePageInfo(-1, 100);
        final RelativePage<QuotaChangeRequestHistoryEvent> history = quotaChangeRequestHistoryDao.readHistoryByRequest(request.getId(), historyFilter, pageInfo, DiOrder.ASC);
        assertEquals(1, history.getItems().size());

        issueComments = trackerManager.getIssueComments(request.getTrackerIssueKey());

        assertEquals(1, issueComments.size());
        assertTrue(issueComments.get(0).getComment().isMatch(comment -> comment.contains("Заявка перемещена кем:" + AMOSOV_F.getLogin() + " в сервис " + anotherProject.getKey())));
    }

    @Test
    public void quotaRequestUpdateProjectWillChangeResponsibleAndStatus() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final DiProject anotherProject = createProject("another_project", YANDEX, WHISTLER.getLogin());

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, "foo", Collections.emptyMap(), null, null, GOAL_ANSWER, null, null, null))
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(BINARY_CAT);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(BINARY_CAT);

        Issue issue = trackerManager.getIssue(request.getTrackerIssueKey());
        assertTrue(issue.getAssignee().isPresent());
        assertEquals(BINARY_CAT.getLogin(), issue.getAssignee().get().getLogin());

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, null, Collections.emptyMap(), null, null, null, null, anotherProject.getKey(), null))
                .performBy(AMOSOV_F);

        issue = trackerManager.getIssue(request.getTrackerIssueKey());
        assertTrue(issue.getAssignee().isPresent());
        assertEquals(WHISTLER.getLogin(), issue.getAssignee().get().getLogin());

        final DiQuotaChangeRequest diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();
        assertEquals(anotherProject, diQuotaChangeRequest.getProject());
    }

    @Test
    public void cannotUpdateQuotaRequestWithNotExistingProject() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        String fakeProject = "foo-42";

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, null, Collections.emptyMap(), null, null, null, null, fakeProject, null))
                .performBy(AMOSOV_F), 400);

        updateHierarchy();

        final DiQuotaChangeRequest diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();
        assertEquals(TEST_PROJECT_KEY, diQuotaChangeRequest.getProject().getKey());
    }

    @Test
    public void cannotUpdateQuotaRequestWithTrashProject() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final DiProject trash = createProject(Project.TRASH_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final DiProject garbage = createProject("garbage", trash.getKey(), BINARY_CAT.getLogin());

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, null, Collections.emptyMap(), null, null, null, null, garbage.getKey(), null))
                .performBy(AMOSOV_F), 400, "Cannot set abc service if it's closed, deleted or un-exportable");

        updateHierarchy();

        final DiQuotaChangeRequest diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();
        assertEquals(TEST_PROJECT_KEY, diQuotaChangeRequest.getProject().getKey());
    }

    @Test
    public void cannotUpdateQuotaRequestWithProjectIfRequestHasMdsResources() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final DiProject anotherProject = createProject("another_project", YANDEX, BINARY_CAT.getLogin());

        final Service nirvana = Hierarchy.get().getServiceReader().read(NIRVANA);
        final Resource s3Storage = resourceDao.create(new Resource.Builder("s3-storage", nirvana)
                .description("x")
                .name("x")
                .type(DiResourceType.STORAGE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .noGroup()
                .priority(42)
                .build()
        );
        updateHierarchy();
        prepareCampaignResources();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .changes(MDS, HDD, bigOrderOne.getId(), Set.of(), DiAmount.of(1, DiUnit.BYTE))
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        final DiQuotaChangeRequest request2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .changes(NIRVANA, s3Storage.getPublicKey(), bigOrderOne.getId(), Set.of(), DiAmount.of(1, DiUnit.BYTE))
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        for (DiQuotaChangeRequest req : List.of(request, request2)) {
            assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                    .byId(req.getId())
                    .update(new BodyUpdate(null, null, null, null,
                            null, null, Collections.emptyMap(), null, null, null, null, anotherProject.getKey(), null))
                    .performBy(AMOSOV_F), 400, "Cannot update abc service if request has MDS resources");

            final DiQuotaChangeRequest diQuotaChangeRequest = dispenser().quotaChangeRequests()
                    .byId(req.getId())
                    .get()
                    .perform();
            assertEquals(TEST_PROJECT_KEY, diQuotaChangeRequest.getProject().getKey());
        }
    }

    @Test
    public void quotaRequestUpdateSummaryBlankForbidden() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, null, Collections.emptyMap(), null, null, null, "", null, null))
                .performBy(AMOSOV_F), "Non-blank summary is required.");
    }

    @Test
    public void quotaRequestUpdateSummaryTooLongForbidden() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, null, Collections.emptyMap(), null, null, null, StringUtils.repeat("a", 1001), null, null))
                .performBy(AMOSOV_F), "Summary must be shorter than 1000 symbols.");
    }

    @Test
    public void quotaRequestCheckGoalIdUpdateNotChangingStatus() {
        final DiProject diProject = createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final Project testProject = projectDao.read(diProject.getKey());

        final long secondGoalId = 228L;
        final Goal goal = goalDao.create(new Goal(secondGoalId, "Normal Goal", Importance.DEPARTMENT, Status.NEW, OkrAncestors.EMPTY));

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(ImmutableMap.of(0L, "none", 1L, "none", 2L, "none"))
                        .chartLinksAbsenceExplanation("explanation")
                        .build())
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(BINARY_CAT);
        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(BINARY_CAT);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, null, Collections.emptyMap(), null, goal.getId(), null, null, null, null))
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        final long goalId = Objects.requireNonNull(diQuotaChangeRequest.getGoal()).getId();
        assertEquals(secondGoalId, goalId);
        assertEquals(DiQuotaChangeRequest.Status.APPROVED, diQuotaChangeRequest.getStatus());
    }

    @Test
    public void quotaRequestCanBeUpdatedToSameGoalId() {
        final DiProject diProject = createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final Project testProject = projectDao.read(diProject.getKey());

        final long firstGoalId = 822L;
        final Goal firstGoal = goalDao.create(new Goal(firstGoalId, "Normal Goal", Importance.DEPARTMENT, Status.NEW, OkrAncestors.EMPTY));

        final long secondGoalId = 909L;
        final Goal secondGoal = goalDao.create(new Goal(secondGoalId, "Normal Goal Too", Importance.DEPARTMENT, Status.NEW, OkrAncestors.EMPTY));

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .goalId(firstGoalId)
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        DiQuotaChangeRequest diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertNull(diQuotaChangeRequest.getGoal());

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                        .goalId(firstGoal.getId())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        long goalId = Objects.requireNonNull(diQuotaChangeRequest.getGoal()).getId();
        assertEquals(goalId, firstGoal.getId());

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                        .goalId(firstGoal.getId())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        goalId = Objects.requireNonNull(diQuotaChangeRequest.getGoal()).getId();
        assertEquals(goalId, firstGoal.getId());

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                        .goalId(secondGoal.getId())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        goalId = Objects.requireNonNull(diQuotaChangeRequest.getGoal()).getId();
        assertEquals(goalId, secondGoal.getId());

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .goalId(firstGoal.getId())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        goalId = Objects.requireNonNull(diQuotaChangeRequest.getGoal()).getId();
        assertEquals(goalId, firstGoal.getId());

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .goalId(firstGoal.getId())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        goalId = Objects.requireNonNull(diQuotaChangeRequest.getGoal()).getId();
        assertEquals(goalId, firstGoal.getId());
    }

    @Test
    public void quotaRequestCheckAnswersNotChangingStatus() {
        final DiProject diProject = createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final Project testProject = projectDao.read(diProject.getKey());

        final long secondGoalId = 229L;
        final Goal goal = goalDao.create(new Goal(secondGoalId, "Normal Goal", Importance.DEPARTMENT, Status.NEW, OkrAncestors.EMPTY));

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(ImmutableMap.of(0L, "none", 1L, "none", 2L, "none"))
                        .chartLinksAbsenceExplanation("explanation")
                        .build())
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(BINARY_CAT);
        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(BINARY_CAT);

        final ImmutableMap<Long, String> goalAnswers = ImmutableMap.of(
                0L, "fine!",
                1L, "good!",
                2L, "done!"
        );
        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, null, Collections.emptyMap(), null, null, goalAnswers, null, null, null))
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest diQuotaChangeRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + request.getId())
                .get(DiQuotaChangeRequest.class);

        assertEquals(diQuotaChangeRequest.getRequestGoalAnswers(), goalAnswers);
        assertEquals(DiQuotaChangeRequest.Status.APPROVED, diQuotaChangeRequest.getStatus());
    }

    @Test
    public void quotaRequestCheckReasonTypeUpdateNotChangingStatus() {
        final DiProject diProject = createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final Project testProject = projectDao.read(diProject.getKey());

        final long secondGoalId = 230L;
        final Goal goal = goalDao.create(new Goal(secondGoalId, "Normal Goal", Importance.DEPARTMENT, Status.NEW, OkrAncestors.EMPTY));

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(GOAL_ANSWER)
                        .chartLinksAbsenceExplanation("explanation")
                        .build())
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(BINARY_CAT);
        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(BINARY_CAT);

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .requestGoalAnswers(GROWTH_ANSWER)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertNull(diQuotaChangeRequest.getGoal());
        assertEquals(DiResourcePreorderReasonType.GROWTH, diQuotaChangeRequest.getResourcePreorderReasonType());
        assertEquals(DiQuotaChangeRequest.Status.APPROVED, diQuotaChangeRequest.getStatus());
    }

    @Test
    public void quotaRequestCantUpdateGoalIdForReasonTypeGrowth() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                        .byId(request.getId())
                        .update(new BodyUpdate(null, null, null, null,
                                null, null, Collections.emptyMap(), null, newGoal.getId(), null, null, null, null))
                        .performBy(AMOSOV_F),
                "Goal must be empty for request with reason type \\\"Growth\\\"");
    }

    @Test
    public void quotaRequestResponsibleCanChangeStatusAroundNeedInfoAndApprovedStatuses() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .chartLinksAbsenceExplanation("foo")
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();
        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GROWTH_ANSWER).build())
                .performBy(BINARY_CAT);

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(BINARY_CAT);

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(BINARY_CAT);

        assertEquals(DiQuotaChangeRequest.Status.NEED_INFO, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(BINARY_CAT);

        assertEquals(DiQuotaChangeRequest.Status.APPROVED, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(BINARY_CAT);

        assertEquals(DiQuotaChangeRequest.Status.NEED_INFO, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(BINARY_CAT);

        assertEquals(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(BINARY_CAT);

        assertEquals(DiQuotaChangeRequest.Status.APPROVED, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.CONFIRMED)
                .performBy(KEYD);

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, request.getStatus());
    }

    @Test
    public void quotaRequestResponsibleCanChangeStatusAroundNeedInfoAndApprovedStatusesByProcessResponsible() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .chartLinksAbsenceExplanation("foo")
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();
        updateHierarchy();

        final Person person = Hierarchy.get().getPersonReader().readPersonByLogin(SLONNN.getLogin());
        projectDao.attach(person, Hierarchy.get().getProjectReader().getRoot(), Role.PROCESS_RESPONSIBLE);

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GROWTH_ANSWER).build())
                .performBy(SLONNN);

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(SLONNN);

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.NEED_INFO, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.APPROVED, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.NEED_INFO, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.APPROVED, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.CONFIRMED)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, request.getStatus());
    }

    @Test
    public void quotaRequestResponsibleCanChangeStatusAroundNeedInfoAndApprovedStatusesByDispenserAdmin() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .chartLinksAbsenceExplanation("foo")
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();
        updateHierarchy();

        final Person person = Hierarchy.get().getPersonReader().readPersonByLogin(SLONNN.getLogin());
        dispenserAdminsDao.setDispenserAdmins(Sets.union(new HashSet<>(dispenserAdminsDao.getDispenserAdmins()), Set.of(person)));

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GROWTH_ANSWER).build())
                .performBy(SLONNN);

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(SLONNN);

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.NEED_INFO, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.APPROVED, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.NEED_INFO, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.APPROVED, request.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.CONFIRMED)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, request.getStatus());
    }

    @Test
    public void onlyProcessResponsibleCanMoveToConfirmed() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .chartLinksAbsenceExplanation("foo")
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();
        updateHierarchy();

        final long requestId = request.getId();
        dispenser().quotaChangeRequests()
                .byId(requestId)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GROWTH_ANSWER).build())
                .performBy(BINARY_CAT);

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(BINARY_CAT);
        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(BINARY_CAT);

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.CONFIRMED)
                .performBy(BINARY_CAT), "Available statuses: CANCELLED, NEED_INFO, REJECTED\"");

        final Person person = Hierarchy.get().getPersonReader().readPersonByLogin(BINARY_CAT.getLogin());
        projectDao.attach(person, Hierarchy.get().getProjectReader().getRoot(), Role.PROCESS_RESPONSIBLE);

        updateHierarchy();

        request = dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.CONFIRMED)
                .performBy(BINARY_CAT);

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, request.getStatus());
        assertEquals(EnumSet.of(DiQuotaChangeRequest.Permission.CAN_EDIT, DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP,
                        DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_CLOSE_ALLOCATION_NOTE),
                request.getPermissions());
    }

    @Test
    public void dispenserAdminCanMoveToConfirmed() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .chartLinksAbsenceExplanation("foo")
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();
        updateHierarchy();

        final long requestId = request.getId();
        dispenser().quotaChangeRequests()
                .byId(requestId)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GROWTH_ANSWER).build())
                .performBy(BINARY_CAT);

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(BINARY_CAT);
        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(BINARY_CAT);

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.CONFIRMED)
                .performBy(BINARY_CAT), "Available statuses: CANCELLED, NEED_INFO, REJECTED\"");

        final Person person = Hierarchy.get().getPersonReader().readPersonByLogin(SLONNN.getLogin());
        dispenserAdminsDao.setDispenserAdmins(Sets.union(new HashSet<>(dispenserAdminsDao.getDispenserAdmins()), Set.of(person)));

        updateHierarchy();

        request = dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.CONFIRMED)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, request.getStatus());
        assertEquals(EnumSet.of(DiQuotaChangeRequest.Permission.CAN_EDIT, DiQuotaChangeRequest.Permission.CAN_EDIT_REVIEW_POPUP,
                        DiQuotaChangeRequest.Permission.CAN_REJECT, DiQuotaChangeRequest.Permission.CAN_CLOSE_ALLOCATION_NOTE),
                request.getPermissions());
    }

    @Test
    public void quotaRequestAuthorCanResetNeedInfo() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final Person lotrek = Hierarchy.get().getPersonReader().readPersonByLogin(LOTREK.getLogin());
        final Project testProject = Hierarchy.get().getProjectReader().read(TEST_PROJECT_KEY);
        projectDao.attach(lotrek, testProject, Role.MEMBER);

        updateHierarchy();

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .chartLinksAbsenceExplanation("foo")
                        .build(), null)
                .performBy(LOTREK)
                .getFirst();

        final long requestId = request.getId();
        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GROWTH_ANSWER).build())
                .performBy(BINARY_CAT);

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(BINARY_CAT);

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(LOTREK), "Can't change status to NEED_INFO. Available statuses: CANCELLED\"");

        request = dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(BINARY_CAT);

        assertEquals(DiQuotaChangeRequest.Status.NEED_INFO, request.getStatus());

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(LOTREK), "Can't change status to APPROVED. Available statuses: CANCELLED, READY_FOR_REVIEW\"");
    }

    @Test
    public void quotaRequestProjectQuotaManagerCanManageRequest() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final Person lotrek = Hierarchy.get().getPersonReader().readPersonByLogin(LOTREK.getLogin());
        final Person aqru = Hierarchy.get().getPersonReader().readPersonByLogin(AQRU.getLogin());

        final Project testProject = Hierarchy.get().getProjectReader().read(TEST_PROJECT_KEY);
        projectDao.attach(lotrek, testProject, Role.MEMBER);
        projectDao.attach(aqru, testProject, Role.QUOTA_MANAGER);
        projectDao.attach(aqru, testProject, Role.MEMBER);

        updateHierarchy();

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .chartLinksAbsenceExplanation("foo")
                        .build(), null)
                .performBy(LOTREK)
                .getFirst();

        final long requestId = request.getId();
        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GROWTH_ANSWER).build())
                .performBy(AQRU);

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(AQRU);

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(AQRU), "Can't change status to NEED_INFO. Available statuses: CANCELLED\"");

        request = dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(BINARY_CAT);

        assertEquals(DiQuotaChangeRequest.Status.NEED_INFO, request.getStatus());

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(AQRU), "Can't change status to APPROVED. Available statuses: CANCELLED, READY_FOR_REVIEW\"");
    }

    @Test
    public void processResponsibleCanManageRequest() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final Person person = Hierarchy.get().getPersonReader().readPersonByLogin(SLONNN.getLogin());
        projectDao.attach(person, Hierarchy.get().getProjectReader().getRoot(), Role.PROCESS_RESPONSIBLE);

        updateHierarchy();

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .chartLinksAbsenceExplanation("foo")
                        .build(), null)
                .performBy(SLONNN)
                .getFirst();

        final long requestId = request.getId();
        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GROWTH_ANSWER).build())
                .performBy(SLONNN);

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(SLONNN);

        request = dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.NEED_INFO, request.getStatus());
    }

    @Test
    public void dispenserAdminCanManageRequest() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final Person person = Hierarchy.get().getPersonReader().readPersonByLogin(SLONNN.getLogin());
        dispenserAdminsDao.setDispenserAdmins(Sets.union(new HashSet<>(dispenserAdminsDao.getDispenserAdmins()), Set.of(person)));

        updateHierarchy();

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .chartLinksAbsenceExplanation("foo")
                        .build(), null)
                .performBy(SLONNN)
                .getFirst();

        final long requestId = request.getId();
        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GROWTH_ANSWER).build())
                .performBy(SLONNN);

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(SLONNN);

        request = dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(SLONNN);

        assertEquals(DiQuotaChangeRequest.Status.NEED_INFO, request.getStatus());
    }

    @Test
    public void quotaRequestProjectVSLeaderCanManageRequest() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final Person lotrek = Hierarchy.get().getPersonReader().readPersonByLogin(LOTREK.getLogin());
        final Person aqru = Hierarchy.get().getPersonReader().readPersonByLogin(AQRU.getLogin());

        final Project testProject = Hierarchy.get().getProjectReader().read(TEST_PROJECT_KEY);
        projectDao.attach(lotrek, testProject, Role.MEMBER);
        projectDao.attach(aqru, testProject, Role.VS_LEADER);
        projectDao.attach(aqru, testProject, Role.MEMBER);

        updateHierarchy();

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .chartLinksAbsenceExplanation("foo")
                        .build(), null)
                .performBy(LOTREK)
                .getFirst();

        final long requestId = request.getId();
        updateHierarchy();

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GROWTH_ANSWER).build())
                .performBy(AQRU);

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(AQRU);

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(AQRU), "Can't change status to NEED_INFO. Available statuses: CANCELLED\"");

        request = dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.NEED_INFO)
                .performBy(BINARY_CAT);

        assertEquals(DiQuotaChangeRequest.Status.NEED_INFO, request.getStatus());

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(requestId)
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(AQRU), "Can't change status to APPROVED. Available statuses: CANCELLED, READY_FOR_REVIEW\"");
    }

    @Test
    public void quotaRequestCanUpdateReasonTypeFromGrowthToGoal() {
        final DiProject diProject = createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final Project testProject = projectDao.read(diProject.getKey());

        quotaChangeRequestDao.clear();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, null, Collections.emptyMap(),
                        DiResourcePreorderReasonType.GOAL, newGoal.getId(), null, null, null, null))
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest diQuotaChangeRequest = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .get()
                .perform();

        assertEquals(DiResourcePreorderReasonType.GOAL, diQuotaChangeRequest.getResourcePreorderReasonType());
        assertEquals(diQuotaChangeRequest.getGoal(), newGoal.toView());
    }

    @Test
    public void quotaRequestCanUpdateReasonTypeFromGoalToGrowth() {
        final DiProject diProject = createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        final Project testProject = projectDao.read(diProject.getKey());

        quotaChangeRequestDao.clear();
        updateHierarchy();

        final DiQuotaChangeRequest request2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        dispenser().quotaChangeRequests()
                .byId(request2.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, null, Collections.emptyMap(),
                        DiResourcePreorderReasonType.GROWTH, null, null, null, null, null))
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest diQuotaChangeRequest2 = dispenser().quotaChangeRequests()
                .byId(request2.getId())
                .get()
                .perform();

        assertEquals(DiResourcePreorderReasonType.GROWTH, diQuotaChangeRequest2.getResourcePreorderReasonType());
        assertNull(diQuotaChangeRequest2.getGoal());
    }

    @Test
    public void quotaRequestCantUpdateReasonTypeFromGoalToNothing() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        quotaChangeRequestDao.clear();
        updateHierarchy();

        final DiQuotaChangeRequest request3 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                        .byId(request3.getId())
                        .update(new BodyUpdate(null, null, null, null,
                                null, null, Collections.emptyMap(),
                                DiResourcePreorderReasonType.NOTHING, null, null, null, null, null))
                        .performBy(AMOSOV_F),
                "diResourcePreorderReasonType must be GOAL or GROWTH for resource preorder request type");
    }

    @Test
    public void quotaRequestCreateThrowOnFakeGoalId() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .goalId(TEST_INVALID_GOAL_ID)
                        .build(), null)
                .performBy(BINARY_CAT), "status\":400,\"title\":\"Bad Request - Invalid argument");
    }

    @Test
    public void quotaRequestCreateThrowOnPrivateGoal() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .goalId(TEST_PRIVATE_GOAL_ID)
                        .build(), null)
                .performBy(BINARY_CAT), "The goal with id '1000' has invalid importance PRIVATE!");
    }

    @Test
    public void quotaRequestCreateThrowOnCancelledGoal() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .goalId(TEST_CANCELLED_GOAL_ID)
                        .build(), null)
                .performBy(BINARY_CAT), "The goal with id '1001' has invalid status CANCELLED!");
    }

    @Test
    public void quotaRequestUpdateThrowOnInvalidGoalId() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate(null, null, null, null,
                        null, null, Collections.emptyMap(), null,
                        TEST_INVALID_GOAL_ID, null, null, null, null))
                .performBy(AMOSOV_F), "status\":400,\"title\":\"Bad Request - Invalid argument");
    }

    @Test
    public void requestsCanBeFilteredByReasonTypeAndGoalId() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .batchCreate(ImmutableList.of(
                        requestBodyBuilderWithDefaultFields()
                                .projectKey(project.getPublicKey())
                                .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                                .goalId(TEST_GOAL_BATCH_ONE_ID)
                                .build(),
                        requestBodyBuilderWithDefaultFields()
                                .projectKey(project.getPublicKey())
                                .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                                .goalId(TEST_GOAL_BATCH_TWO_ID)
                                .build(),
                        requestBodyBuilderWithDefaultFields()
                                .projectKey(project.getPublicKey())
                                .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                .build()
                ), null)
                .performBy(AMOSOV_F);

        assertEquals(3, dispenser().quotaChangeRequests()
                .get()
                .perform().size());

        assertEquals(2, dispenser().quotaChangeRequests()
                .get()
                .query("resourcePreorderReasonType", "GOAL")
                .perform().size());

        assertEquals(1, dispenser().quotaChangeRequests()
                .get()
                .query("resourcePreorderReasonType", "GROWTH")
                .perform().size());

        assertEquals(1, dispenser().quotaChangeRequests()
                .get()
                .query("goal", String.valueOf(TEST_GOAL_BATCH_TWO_ID))
                .perform().size());

    }

    private void prepareYT() {
        Service yt;
        try {
            yt = serviceDao.read(YT);
        } catch (EmptyResultDataAccessException e) {
            yt = serviceDao.create(Service.withKey(YT)
                    .withName("YT")
                    .withAbcServiceId(TEST_ABC_SERVICE_ID)
                    .build());

            updateHierarchy();
        }

        resourceDao.create(new Resource.Builder(CPU_YT, yt)
                .name("CPU YT")
                .type(DiResourceType.PROCESSOR)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        updateHierarchy();
    }

    private @NotNull
    DiListResponse<DiQuotaChangeRequest> createYtRequestWithProps(final Map<String, String> properties) {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        return dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YT, CPU_YT, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.PERMILLE_CORES))
                        .additionalProperties(properties)
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                        .goalId(TEST_GOAL_BATCH_ONE_ID)
                        .build(), null
                ).performBy(AMOSOV_F);
    }

    @Test
    public void YtCanBeCreated() {
        prepareYT();

        prepareCampaignResources();
        createYtRequestWithProps(new HashMap<String, String>() {{
            put("pools", "some1, some2");
        }});

        createYtRequestWithProps(new HashMap<String, String>() {{
            put("accounts", "some1");
        }});

        createYtRequestWithProps(new HashMap<String, String>() {{
            put("tabletCellBundles", "some1, something_2, and_3");
        }});

        createYtRequestWithProps(new HashMap<String, String>() {{
            put("pools", "some1, some2");
            put("accounts", "some1");
            put("tabletCellBundles", "some1, something_2, and_3");
        }});

        // mix with ', ' ','

        createYtRequestWithProps(new HashMap<String, String>() {{
            put("pools", "some1,some2");
            put("accounts", "some1");
            put("tabletCellBundles", "some1,something_2, and_3");
        }});

        // with '-'

        createYtRequestWithProps(new HashMap<String, String>() {{
            put("pools", "som--e1,so-me-2");
            put("accounts", "s_ome-1");
            put("tabletCellBundles", "some1,something_2, and_3");
        }});

        // with WRONG pools TODO remove me when UI ready

        createYtRequestWithProps(new HashMap<String, String>() {{
            put("pulls", "som--e1,so-me-2");
            put("accounts", "s_ome-1");
            put("tabletCellBundles", "some1,something_2, and_3");
        }});
    }

    @Test
    public void YtCantBeCreatedWithWrongPropValueFormat() {
        prepareYT();

        prepareCampaignResources();
        assertThrowsWithMessage(() -> {
            createYtRequestWithProps(new HashMap<String, String>() {{
                put("pools", "some1, ");
            }});
        }, "Yt request has wrong value for properties 'pools'. Valid format is 'value1, value2, value3'");

        assertThrowsWithMessage(() -> {
            createYtRequestWithProps(new HashMap<String, String>() {{
                put("pools", "some1");
                put("tabletCellBundles", "some1, something_2, and_3");
                put("accounts", ",");
            }});
        }, "Yt request has wrong value for properties 'accounts'. Valid format is 'value1, value2, value3'");

        assertThrowsWithMessage(() -> {
            createYtRequestWithProps(new HashMap<String, String>() {{
                put("accounts", "some1");
                put("tabletCellBundles", "");
            }});
        }, "yt request should contain not empty additional properties 'tabletCellBundles'");

        assertThrowsWithMessage(() -> {
            createYtRequestWithProps(new HashMap<String, String>() {{
                put("tabletCellBundles", "some1, something_2, and_3");
                put("accounts", "some1");
                put("pools", "some1 something_2 and_3");
            }});
        }, "Yt request has wrong value for properties 'pools'. Valid format is 'value1, value2, value3'");

        assertThrowsWithMessage(() -> {
            createYtRequestWithProps(new HashMap<String, String>() {{
                put("tabletCellBundles", "some1, something_2 and_3");
                put("accounts", "some1,");
                put("pools", "some1 something_2 and_3");
            }});
        }, "Yt request has wrong value for properties 'tabletCellBundles, pools, accounts'. Valid format is 'value1, value2, value3'");
    }

    private void prepareMDB() {
        Service dbaas;
        try {
            dbaas = serviceDao.read(DBAAS);
        } catch (EmptyResultDataAccessException e) {
            dbaas = serviceDao.create(Service.withKey(DBAAS)
                    .withName("MDB dbaas")
                    .withAbcServiceId(MDB_ABC_SERVICE_ID)
                    .build());

            updateHierarchy();
        }

        resourceGroupDao.create(new ResourceGroup.Builder(RESOURCE_GROUP_MDB_CLICKHOUSE, dbaas)
                .name("mdb ClickHouse")
                .build());

        updateHierarchy();

        resourceDao.create(new Resource.Builder(MDB_HDD_CH, dbaas)
                .name("HDD ClickHouse")
                .type(DiResourceType.STORAGE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .group(RESOURCE_GROUP_MDB_CLICKHOUSE)
                .build());

        updateHierarchy();
    }

    @Test
    public void mdbHddDbaasClickhouseCanBeCreated() {
        prepareMDB();

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(DBAAS, MDB_HDD_CH, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(12800L, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                        .goalId(TEST_GOAL_BATCH_ONE_ID)
                        .build(), null
                ).performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(DBAAS, MDB_HDD_CH, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(12800L * 2, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                        .goalId(TEST_GOAL_BATCH_ONE_ID)
                        .build(), null
                ).performBy(AMOSOV_F);
    }

    @Test
    public void mdbHddDbaasClickhouseValidationTestThrowsOnWrongValue() {
        prepareMDB();

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .projectKey(YANDEX)
                            .changes(DBAAS, MDB_HDD_CH, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(12800L * 2 - 1, DiUnit.GIBIBYTE))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                            .goalId(TEST_GOAL_BATCH_ONE_ID)
                            .build(), null
                    ).performBy(AMOSOV_F);
        }, "HDD Clickhouse should be a multiple of 12800 GiB");
    }

    @Test
    public void requestCantBeCreatedWithZeroChangesValue() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareMDB();

        prepareCampaignResources();
        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .projectKey(project.getPublicKey())
                            .changes(DBAAS, MDB_HDD_CH, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(0, DiUnit.GIBIBYTE))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                            .goalId(TEST_GOAL_BATCH_ONE_ID)
                            .build(), null
                    ).performBy(AMOSOV_F);
        }, "At least one resource value must be greater than zero");

        final Service dbaas = serviceDao.read(DBAAS);
        resourceGroupDao.create(new ResourceGroup.Builder("test_resource_group_1", dbaas)
                .name("mdb Test 1")
                .build());

        resourceGroupDao.create(new ResourceGroup.Builder("test_resource_group_2", dbaas)
                .name("mdb Test 2")
                .build());

        updateHierarchy();

        resourceDao.create(new Resource.Builder("test_resource_1", dbaas)
                .name("SSD test 1")
                .type(DiResourceType.STORAGE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .group("test_resource_group_1")
                .build());

        resourceDao.create(new Resource.Builder("test_resource_2", dbaas)
                .name("SSD test 2")
                .type(DiResourceType.STORAGE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .group("test_resource_group_2")
                .build());

        updateHierarchy();

        prepareCampaignResources();
        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(DBAAS, MDB_HDD_CH, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(0, DiUnit.GIBIBYTE))
                        .changes(DBAAS, "test_resource_1", bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(0, DiUnit.GIBIBYTE))
                        .changes(DBAAS, "test_resource_2", bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                        .goalId(TEST_GOAL_BATCH_ONE_ID)
                        .build(), null
                ).performBy(AMOSOV_F);
    }

    @Test
    public void getForNotExistingRequestMustReturn404Status() {
        quotaChangeRequestDao.clear();

        assertThrowsWithMessage(() -> createAuthorizedLocalClient(BINARY_CAT)
                        .path("/v1/quota-requests/19922020")
                        .get(DiQuotaChangeRequest.class), 404,
                "with id 19922020");
    }

    private void setupProjectForAnswersTest() {
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
        project = projectDao.read(key);
    }

    @Test
    public void quotaChangeRequestAnswersCanBeViewed() {

        setupProjectForAnswersTest();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(AMOSOV_F)
                .getFirst();

        final long id = request.getId();

        dispenser().quotaChangeRequests()
                .byId(id)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GOAL_ANSWER).build())
                .performBy(LOTREK);

        final DiQuotaChangeRequest request1 = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + id)
                .get(DiQuotaChangeRequest.class);

        assertEquals(GOAL_ANSWER, request1.getRequestGoalAnswers());
    }

    @Test
    public void quotaChangeRequestAnswersCanBeUpdated() {

        setupProjectForAnswersTest();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(AMOSOV_F)
                .getFirst();

        final long id = request.getId();

        dispenser().quotaChangeRequests()
                .byId(id)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GOAL_ANSWER).build())
                .performBy(LOTREK);

        final DiQuotaChangeRequest request1 = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + id)
                .get(DiQuotaChangeRequest.class);

        assertEquals(GOAL_ANSWER, request1.getRequestGoalAnswers());

        final ImmutableMap<Long, String> newGoalAnswers = ImmutableMap.of(
                0L, "fine!",
                1L, "good!",
                2L, "done!"
        );

        dispenser().quotaChangeRequests()
                .byId(id)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(newGoalAnswers).build())
                .performBy(LOTREK);

        final DiQuotaChangeRequest request2 = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + id)
                .get(DiQuotaChangeRequest.class);

        assertEquals(request2.getRequestGoalAnswers(), newGoalAnswers);
    }

    @Test
    public void quotaChangeRequestAnswersCanBeUpdatedByProcessResponsible() {

        setupProjectForAnswersTest();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(AMOSOV_F)
                .getFirst();

        final Person person = Hierarchy.get().getPersonReader().readPersonByLogin(SLONNN.getLogin());
        projectDao.attach(person, Hierarchy.get().getProjectReader().getRoot(), Role.PROCESS_RESPONSIBLE);

        updateHierarchy();

        final long id = request.getId();

        dispenser().quotaChangeRequests()
                .byId(id)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GOAL_ANSWER).build())
                .performBy(SLONNN);

        final DiQuotaChangeRequest request1 = createAuthorizedLocalClient(SLONNN)
                .path("/v1/quota-requests/" + id)
                .get(DiQuotaChangeRequest.class);

        assertEquals(GOAL_ANSWER, request1.getRequestGoalAnswers());

        final ImmutableMap<Long, String> newGoalAnswers = ImmutableMap.of(
                0L, "fine!",
                1L, "good!",
                2L, "done!"
        );

        dispenser().quotaChangeRequests()
                .byId(id)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(newGoalAnswers).build())
                .performBy(SLONNN);

        final DiQuotaChangeRequest request2 = createAuthorizedLocalClient(SLONNN)
                .path("/v1/quota-requests/" + id)
                .get(DiQuotaChangeRequest.class);

        assertEquals(request2.getRequestGoalAnswers(), newGoalAnswers);
    }

    @Test
    public void onlyProjectMembersCanUpdateQuotaChangeRequestAnswers() {
        setupProjectForAnswersTest();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(AMOSOV_F)
                .getFirst();

        final long id = request.getId();

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(id)
                    .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GOAL_ANSWER).build())
                    .performBy(SLONNN);
        }, "Not enough permissions to modify quota request in campaign.");
    }

    @Test
    public void onlyActiveCampaignQuotaChangeRequestAnswersCanBeUpdated() {
        setupProjectForAnswersTest();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(AMOSOV_F)
                .getFirst();

        final long id = request.getId();

        campaignDao.update(campaign.withStatus(Campaign.Status.CLOSED));

        assertThrowsWithMessage(() -> {
            dispenser().quotaChangeRequests()
                    .byId(id)
                    .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(GOAL_ANSWER).build())
                    .performBy(LOTREK);
        }, "Request can't be updated at this campaign stage");
    }

    @Test
    public void requestCostShouldBeDisplayedForProcessResponsiblesServicesAdminsAndSimpleResponsibles() {
        setupProjectForAnswersTest();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        createProject("test_project_42", YANDEX, AQRU.getLogin());

        updateHierarchy();

        final long id = request.getId();
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(id).copyBuilder()
                .cost(9_990)
                .build());


        final DiQuotaChangeRequest requestByCreator = createAuthorizedLocalClient(QDEEE)
                .path("/v1/quota-requests/" + id)
                .get(DiQuotaChangeRequest.class);

        assertNull(requestByCreator.getCost());

        final DiQuotaChangeRequest requestByResponsibles = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + id)
                .get(DiQuotaChangeRequest.class);

        assertNull(requestByResponsibles.getCost());

        final DiQuotaChangeRequest requestByAnotherResponsible = createAuthorizedLocalClient(AQRU)
                .path("/v1/quota-requests/" + id)
                .get(DiQuotaChangeRequest.class);

        assertNull(requestByAnotherResponsible.getCost());

        final DiQuotaChangeRequest requestByServiceAdmin = createAuthorizedLocalClient(SANCHO)
                .path("/v1/quota-requests/" + id)
                .get(DiQuotaChangeRequest.class);

        assertNull(requestByServiceAdmin.getCost());

        final DiQuotaChangeRequest requestByProcessResponsible = createAuthorizedLocalClient(KEYD)
                .path("/v1/quota-requests/" + id)
                .get(DiQuotaChangeRequest.class);

        assertEquals(9990.0d, requestByProcessResponsible.getCost());

    }

    @Test
    public void requestOwningCostShouldNotBeDisplayedForProcessResponsiblesServicesAdminsAndSimpleResponsibles() {
        campaignDao.clear();
        LocalDate date = LocalDate.of(2021, Month.AUGUST, 1);
        campaign = campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2021")
                .setName("aug2021")
                .setId(143L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(), date)))
                .build());

        botCampaignGroupDao.clear();
        botCampaignGroup = botCampaignGroupDao.create(
                defaultCampaignGroupBuilder()
                        .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrderOne.getId())))
                        .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values().iterator().next())
                        .build()
        );
        newCampaignId = campaign.getId();

        setupProjectForAnswersTest();

        String SSD = "ssd_segmented";
        String HDD = "hdd_segmented";
        String CPU = "cpu_segmented";
        String RAM = "ram_segmented";
        String GPU = "gpu_segmented";
        String IO_HDD = "io_hdd";
        String IO_SSD = "io_ssd";

        Service service = Hierarchy.get().getServiceReader().read(YP);

        resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        resourceDao.create(new Resource.Builder(RAM, service).name(RAM).type(DiResourceType.MEMORY).build());
        resourceDao.create(new Resource.Builder(GPU, service).name(GPU).type(DiResourceType.ENUMERABLE).build());
        resourceDao.create(new Resource.Builder(IO_HDD, service).name(IO_HDD).type(DiResourceType.BINARY_TRAFFIC).build());
        resourceDao.create(new Resource.Builder(IO_SSD, service).name(IO_SSD).type(DiResourceType.BINARY_TRAFFIC).build());

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(QDEEE);

        createProject("test_project_42", YANDEX, AQRU.getLogin());

        updateHierarchy();

        final DiQuotaChangeRequest requestByCreator = createAuthorizedLocalClient(QDEEE)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertNull(requestByCreator.getRequestOwningCost());

        final DiQuotaChangeRequest requestByResponsibles = createAuthorizedLocalClient(LOTREK)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        String sumOfOwningCosts = requestByResponsibles.getChanges().stream()
                .filter(c -> c.getOwningCost() != null)
                .map(change -> new BigInteger(change.getOwningCost()))
                .reduce(BigInteger::add)
                .map(Objects::toString)
                .orElse(null);

        assertNull(sumOfOwningCosts);
        assertNull(requestByResponsibles.getRequestOwningCost());

        final DiQuotaChangeRequest requestByAnotherResponsible = createAuthorizedLocalClient(AQRU)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertNull(requestByAnotherResponsible.getRequestOwningCost());

        final DiQuotaChangeRequest requestByServiceAdmin = createAuthorizedLocalClient(SANCHO)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertNull(requestByServiceAdmin.getRequestOwningCost());

        final DiQuotaChangeRequest requestByProcessResponsible = createAuthorizedLocalClient(KEYD)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertEquals("178354", requestByProcessResponsible.getRequestOwningCost());

    }

    @Test
    public void quotaRequestShouldBeFilterableByResponsibleLogin() {
        final String[] authors = {"lotrek", AMOSOV_F.getLogin(), "dm-tim"};
        createProject("L1", YANDEX, BINARY_CAT.getLogin());
        createProject("L2", "L1", authors);
        createProject("L3", YANDEX, BINARY_CAT.getLogin());
        createProject("L4", "L3", "aqru");

        for (final String author : authors) {
            dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .projectKey("L2")
                            .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10_000, DiUnit.PERMILLE))
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .build(), null)
                    .performBy(DiPerson.login(author));
        }


        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey("L4")
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10_000, DiUnit.PERMILLE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(DiPerson.login("aqru"));

        updateHierarchy();

        final ImmutableList<String> logins = ImmutableList.of("aqru", LOTREK.getLogin(), BINARY_CAT.getLogin(), AMOSOV_F.getLogin(), DM_TIM.getLogin());
        final List<String[]> projectKeys = Arrays.asList(
                ArrayUtils.EMPTY_STRING_ARRAY,
                new String[]{"L1"},
                new String[]{"L2"},
                new String[]{"L3"},
                new String[]{"L1", "L3"}
        );
        for (final String[] keys : projectKeys) {

            DiListResponse<DiQuotaChangeRequest> filteredByAuthorRequests = dispenser()
                    .quotaChangeRequests()
                    .get()
                    .query("project", keys)
                    .perform();


            final Map<String, Integer> countByResponsible = new HashMap<>();
            for (final DiQuotaChangeRequest request : filteredByAuthorRequests) {
                final String responsible = request.getResponsible();
                countByResponsible.put(responsible, countByResponsible.getOrDefault(responsible, 0) + 1);
            }

            for (final String login : logins) {

                filteredByAuthorRequests = dispenser()
                        .quotaChangeRequests()
                        .get()
                        .query("responsible", login)
                        .query("project", keys)
                        .perform();

                assertEquals(filteredByAuthorRequests.size(), (int) countByResponsible.getOrDefault(login, 0), login + ": incorect");

            }
        }
    }


    @Test
    public void quotaRequestShouldBeSortedByUpdatedOrCreated() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        updateHierarchy();

        final DiQuotaChangeRequest requestFirst = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        final long idFirst = requestFirst.getId();

        final DiQuotaChangeRequest requestSecond = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        final long idSecond = requestSecond.getId();

        final DiQuotaChangeRequest requestThird = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(300L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        final long idThird = requestThird.getId();

        final BodyUpdate bodyUpdate = new BodyUpdate.BodyUpdateBuilder()
                .comment("new changes")
                .build();

        dispenser().quotaChangeRequests()
                .byId(idSecond)
                .update(bodyUpdate)
                .performBy(BINARY_CAT);

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> sortedByUpdateDesc = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("sortBy", "UPDATED_AT")
                .query("sortOrder", "DESC")
                .perform();

        assertEquals(3, sortedByUpdateDesc.size());
        assertEquals(sortedByUpdateDesc.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toList()),
                Arrays.asList(idSecond, idThird, idFirst));

        final DiListResponse<DiQuotaChangeRequest> sortedByCreatedAsc = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("sortBy", "CREATED_AT")
                .query("sortOrder", "ASC")
                .perform();

        assertEquals(3, sortedByCreatedAsc.size());
        assertEquals(sortedByCreatedAsc.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toList()),
                Arrays.asList(idFirst, idSecond, idThird));
    }

    @Test
    public void quotaRequestShouldBeSortedByCost() {
        setupProjectForAnswersTest();

        final DiQuotaChangeRequest request1 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        final long id1 = request1.getId();
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(id1).copyBuilder()
                .cost(9_990)
                .build());

        final DiQuotaChangeRequest request2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        final long id2 = request2.getId();
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(id2).copyBuilder()
                .cost(123)
                .build());

        final DiQuotaChangeRequest request3 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        final long id3 = request3.getId();
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(id3).copyBuilder()
                .cost(143_000)
                .build());

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> sortedByUpdateDesc = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("sortBy", "COST")
                .query("sortOrder", "DESC")
                .perform();

        assertEquals(3, sortedByUpdateDesc.size());
        assertEquals(sortedByUpdateDesc.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toList()),
                Arrays.asList(id3, id1, id2));

        final DiListResponse<DiQuotaChangeRequest> sortedByCreatedAsc = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("sortBy", "COST")
                .query("sortOrder", "ASC")
                .perform();

        assertEquals(3, sortedByCreatedAsc.size());
        assertEquals(sortedByCreatedAsc.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toList()),
                Arrays.asList(id2, id1, id3));
    }

    @Test
    public void quotaRequestShouldBeSortedByOwningCost() {
        setupProjectForAnswersTest();

        final DiQuotaChangeRequest request1 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        final long id1 = request1.getId();
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(id1).copyBuilder()
                .requestOwningCost(9_990L)
                .build());

        final DiQuotaChangeRequest request2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        final long id2 = request2.getId();
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(id2).copyBuilder()
                .requestOwningCost(123L)
                .build());

        final DiQuotaChangeRequest request3 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        final long id3 = request3.getId();
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(id3).copyBuilder()
                .requestOwningCost(143_000L)
                .build());

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> sortedByUpdateDesc = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("sortBy", "REQUEST_OWNING_COST")
                .query("sortOrder", "DESC")
                .perform();

        assertEquals(3, sortedByUpdateDesc.size());
        assertEquals(sortedByUpdateDesc.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toList()),
                Arrays.asList(id3, id1, id2));

        final DiListResponse<DiQuotaChangeRequest> sortedByCreatedAsc = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("sortBy", "REQUEST_OWNING_COST")
                .query("sortOrder", "ASC")
                .perform();

        assertEquals(3, sortedByCreatedAsc.size());
        assertEquals(sortedByCreatedAsc.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toList()),
                Arrays.asList(id2, id1, id3));
    }

    @Test
    public void quotaRequestShouldBeFilteredByOwningCost() {
        setupProjectForAnswersTest();

        final DiQuotaChangeRequest request1 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        final long id1 = request1.getId();
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(id1).copyBuilder()
                .requestOwningCost(9_990L)
                .build());

        final DiQuotaChangeRequest request2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        final long id2 = request2.getId();
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(id2).copyBuilder()
                .requestOwningCost(123L)
                .build());

        final DiQuotaChangeRequest request3 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        final long id3 = request3.getId();
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(id3).copyBuilder()
                .requestOwningCost(143_000L)
                .build());

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> sortedByUpdateDesc = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("sortBy", "REQUEST_OWNING_COST")
                .query("owningCostLessOrEquals", "123")
                .query("sortOrder", "ASC")
                .perform();

        assertEquals(1, sortedByUpdateDesc.size());
        assertEquals(sortedByUpdateDesc.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toList()),
                List.of(id2));

        final DiListResponse<DiQuotaChangeRequest> sortedByCreatedAsc = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("owningCostGreaterOrEquals", "124")
                .query("sortBy", "REQUEST_OWNING_COST")
                .query("sortOrder", "ASC")
                .perform();

        assertEquals(2, sortedByCreatedAsc.size());
        assertEquals(sortedByCreatedAsc.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toList()),
                Arrays.asList(id1, id3));

        final DiListResponse<DiQuotaChangeRequest> sortedByCreatedAscLimitedByBothSide = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("owningCostGreaterOrEquals", "124")
                .query("owningCostLessOrEquals", "142999")
                .query("sortBy", "REQUEST_OWNING_COST")
                .query("sortOrder", "ASC")
                .perform();

        assertEquals(1, sortedByCreatedAscLimitedByBothSide.size());
        assertEquals(sortedByCreatedAscLimitedByBothSide.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toList()),
                List.of(id1));
    }

    @Test
    public void quotaRequestShouldBeFilteredByImportance() {
        setupProjectForAnswersTest();

        final DiQuotaChangeRequest request1 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .importantRequest(false)
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        final long id1 = request1.getId();

        final DiQuotaChangeRequest request2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .importantRequest(true)
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        final long id2 = request2.getId();

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> importantRequests = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("importantFilter", "IMPORTANT")
                .perform();

        assertEquals(1, importantRequests.size());
        assertEquals(importantRequests.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toList()),
                List.of(id2));

        final DiListResponse<DiQuotaChangeRequest> notImportantRequests = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("importantFilter", "NOT_IMPORTANT")
                .perform();

        assertEquals(1, notImportantRequests.size());
        assertEquals(notImportantRequests.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toList()),
                List.of(id1));

        final DiListResponse<DiQuotaChangeRequest> allImportantRequest = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("importantFilter", "BOTH")
                .perform();

        assertEquals(2, allImportantRequest.size());
        assertTrue(allImportantRequest.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toSet())
                .containsAll(Set.of(id1, id2)));

        final DiListResponse<DiQuotaChangeRequest> allRequest = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .perform();

        assertEquals(2, allRequest.size());
        assertTrue(allRequest.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toSet())
                .containsAll(Set.of(id1, id2)));
    }

    @Test
    public void quotaRequestShouldBeFilteredByUnbalanced() {
        setupProjectForAnswersTest();

        final DiQuotaChangeRequest request1 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        final long id1 = request1.getId();

        final DiQuotaChangeRequest request2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(QDEEE)
                .getFirst();

        final long id2 = request2.getId();

        updateHierarchy();

        quotaChangeRequestDao.update(quotaChangeRequestDao.read(id2).copyBuilder()
                .unbalanced(true)
                .build());

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> importantRequests = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("unbalancedFilter", "UNBALANCED")
                .perform();

        assertEquals(1, importantRequests.size());
        assertEquals(importantRequests.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toList()),
                List.of(id2));

        final DiListResponse<DiQuotaChangeRequest> notImportantRequests = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("unbalancedFilter", "BALANCED")
                .perform();

        assertEquals(1, notImportantRequests.size());
        assertEquals(notImportantRequests.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toList()),
                List.of(id1));

        final DiListResponse<DiQuotaChangeRequest> allImportantRequest = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .query("unbalancedFilter", "BOTH")
                .perform();

        assertEquals(2, allImportantRequest.size());
        assertTrue(allImportantRequest.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toSet())
                .containsAll(Set.of(id1, id2)));

        final DiListResponse<DiQuotaChangeRequest> allRequest = dispenser()
                .quotaChangeRequests()
                .get()
                .query("service", NIRVANA)
                .perform();

        assertEquals(2, allRequest.size());
        assertTrue(allRequest.stream().map(DiQuotaChangeRequest::getId).collect(Collectors.toSet())
                .containsAll(Set.of(id1, id2)));
    }

    @Test
    public void quotaChangeRequestWillChangeUpdatedTimeWithChangesUpdate() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        updateHierarchy();

        final DiQuotaChangeRequest created = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithTypeResourcePreorder(bigOrderOne).build(), null)
                .performBy(AMOSOV_F)
                .getResults().iterator().next();

        updateHierarchy();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .byId(created.getId())
                .get()
                .perform();

        final long updated = request.getUpdated();

        final DiQuotaChangeRequest updatedRequest = dispenser().quotaChangeRequests().byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(new ChangeBody(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(42L, DiUnit.COUNT)))
                        .build()
                )
                .performBy(AMOSOV_F);

        assertTrue(updatedRequest.getUpdated() > updated);

        final DiQuotaChangeRequest updatedRequest2 = dispenser().quotaChangeRequests().byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(new ChangeBody(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(42L, DiUnit.COUNT)))
                        .build()
                )
                .performBy(AMOSOV_F);

        assertEquals(updatedRequest2.getUpdated(), updatedRequest.getUpdated());
    }

    @Test
    public void quotaChangeRequestChangesUpdateWithDecreaseWontUpdateStatus() {
        final DiProject diProject = createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary("foo")
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .chartLinksAbsenceExplanation("foo")
                        .calculations("foo")
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .description("foo")
                        .projectKey(diProject.getKey())
                        .changes(new ChangeBody(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.COUNT)))
                        .changes(new ChangeBody(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.COUNT)))
                        .build(), null)
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

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
                .performBy(BINARY_CAT);
        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(BINARY_CAT);

        DiQuotaChangeRequest updated = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(90, DiUnit.COUNT))
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.COUNT))
                        .build())
                .performBy(BINARY_CAT);

        assertEquals(DiQuotaChangeRequest.Status.APPROVED, updated.getStatus());
        final Map<String, DiQuotaChangeRequest.Change> changesByResourceKey = updated.getChanges().stream()
                .collect(Collectors.toMap(c -> c.getResource().getKey(), Function.identity()));

        assertEquals(DiAmount.of(100_000, DiUnit.PERMILLE), changesByResourceKey.get(YT_GPU).getAmount());
        assertEquals(DiAmount.of(90_000, DiUnit.PERMILLE), changesByResourceKey.get(YT_CPU).getAmount());

        updated = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(90, DiUnit.COUNT))
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(101, DiUnit.COUNT))
                        .build())
                .performBy(BINARY_CAT);

        assertEquals(DiQuotaChangeRequest.Status.READY_FOR_REVIEW, updated.getStatus());

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(BINARY_CAT);

        updated = dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(90, DiUnit.COUNT))
                        .build())
                .performBy(BINARY_CAT);

        assertEquals(DiQuotaChangeRequest.Status.APPROVED, updated.getStatus());
        assertEquals(1, updated.getChanges().size());
    }

    @Test
    public void quotaChangeRequestChangesUpdateCannotSetAmountLowerThanReadyOrAllocated() {
        Campaign aggregatedCampaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("test-aggregated")
                .setName("test-aggregated")
                .setType(Campaign.Type.AGGREGATED).build());
        prepareCampaignResources();
        final DiProject diProject = createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary("foo")
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .chartLinksAbsenceExplanation("foo")
                        .calculations("foo")
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .description("foo")
                        .projectKey(diProject.getKey())
                        .changes(new ChangeBody(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.COUNT)))
                        .build(), aggregatedCampaign.getId())
                .performBy(BINARY_CAT)
                .getFirst();

        updateHierarchy();

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
                .performBy(BINARY_CAT);
        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(BINARY_CAT);

        DiSetAmountResult diSetAmountResult = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, new SetResourceAmountBody(
                        Collections.singletonList(new SetResourceAmountBody.Item(request.getId(), null,
                                Collections.singletonList(new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrderOne.getId(), YT_CPU, Collections.emptySet(), DiAmount.of(50, DiUnit.COUNT), null)), null)))
                )
                .readEntity(DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, diSetAmountResult);

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                        .byId(request.getId())
                        .update(new BodyUpdate.BodyUpdateBuilder()
                                .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(40, DiUnit.COUNT))
                                .build())
                        .performBy(BINARY_CAT),
                "Can't set amount for resource YT CPU (Nirvana) less than amount ready"
        );

        dispenser().quotaChangeRequests()
                .byId(request.getId())
                .setStatus(DiQuotaChangeRequest.Status.CONFIRMED)
                .performBy(KEYD);

        diSetAmountResult = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, new SetResourceAmountBody(
                        Collections.singletonList(new SetResourceAmountBody.Item(request.getId(), null,
                                Collections.singletonList(new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrderOne.getId(), YT_CPU, Collections.emptySet(),
                                        DiAmount.of(40, DiUnit.COUNT), DiAmount.of(50, DiUnit.COUNT))), null)))
                )
                .readEntity(DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, diSetAmountResult);

        quotaChangeRequestDao.update(quotaChangeRequestDao.read(request.getId()).copyBuilder().status(QuotaChangeRequest.Status.APPROVED).build());

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                        .byId(request.getId())
                        .update(new BodyUpdate.BodyUpdateBuilder()
                                .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(40, DiUnit.COUNT))
                                .build())
                        .performBy(BINARY_CAT),
                "Can't set amount for resource YT CPU (Nirvana) less than amount allocated"
        );

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                        .byId(request.getId())
                        .update(new BodyUpdate.BodyUpdateBuilder()
                                .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.COUNT))
                                .build())
                        .performBy(BINARY_CAT),
                "Can't set amount for resource YT CPU (Nirvana) less than amount allocated",
                "Can't set amount for resource YT CPU (Nirvana) less than amount ready"
        );
    }

    private void createPreOrders(final Collection<MappedPreOrder> preOrders) {
        preOrders.stream()
                .map(BotCampaignGroupServiceApiTest::toSyncedPreOrder)
                .forEach(botPreOrderDao::create);
        mappedPreOrderDao.createAll(preOrders);
    }

    @Test
    public void adminCannotChangeStatusWithoutWorkflowValidationWithInvalidInput() {
        DiQuotaChangeRequest qr1 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields().build(), null)
                .performBy(AMOSOV_F)
                .getFirst();

        for (final String path : Arrays.asList("ids", "tickets")) {
            createAuthorizedLocalClient(LOTREK)
                    .path("/admin/requests/_changeStatus/" + path)
                    .post(null);

            assertEquals(SpyWebClient.lastResponseStatus(), 403);
            assertTrue(SpyWebClient.lastResponse().contains("isn't Dispenser admin"));


            createAuthorizedLocalClient(AMOSOV_F)
                    .path("/admin/requests/_changeStatus/" + path)
                    .query("toStatus", "CONFIRMED")
                    .post(null);

            assertEquals(SpyWebClient.lastResponseStatus(), 400);

            createAuthorizedLocalClient(AMOSOV_F)
                    .path("/admin/requests/_changeStatus/" + path)
                    .query("toStatus", "CONFIRMED")
                    .post(Collections.emptyList());

            assertEquals(SpyWebClient.lastResponseStatus(), 400);

            createAuthorizedLocalClient(AMOSOV_F)
                    .path("/admin/requests/_changeStatus/" + path)
                    .query("toStatus", "tratata")
                    .post(Collections.emptyList());

            assertEquals(SpyWebClient.lastResponseStatus(), 404);
        }

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/admin/requests/_changeStatus/ids")
                .post(Collections.singleton(qr1.getId()));

        assertEquals(SpyWebClient.lastResponseStatus(), 400);

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/admin/requests/_changeStatus/tickets")
                .post(Collections.singleton(qr1.getTrackerIssueKey()));

        assertEquals(SpyWebClient.lastResponseStatus(), 400);
    }

    @Test
    public void adminCanChangeStatusWithoutWorkflowValidationWithRequestIds() {
        DiQuotaChangeRequest qr1 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields().build(), null)
                .performBy(AMOSOV_F)
                .getFirst();
        DiQuotaChangeRequest qr2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields().build(), null)
                .performBy(AMOSOV_F)
                .getFirst();

        Map<String, Set<Long>> resp = createAuthorizedLocalClient(AMOSOV_F)
                .path("/admin/requests/_changeStatus/ids")
                .query("toStatus", QuotaChangeRequest.Status.CONFIRMED.toString())
                .query("comment", "42")
                .post(Arrays.asList(qr1.getId(), -1), new GenericType<Map<String, Set<Long>>>() {
                });

        assertEquals(SpyWebClient.lastResponseStatus(), 200);

        qr1 = dispenser().quotaChangeRequests()
                .byId(qr1.getId())
                .get()
                .perform();
        qr2 = dispenser().quotaChangeRequests()
                .byId(qr2.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, qr1.getStatus());
        assertEquals(DiQuotaChangeRequest.Status.NEW, qr2.getStatus());
        assertTrue(resp.get("skipped").isEmpty());
        assertEquals(ImmutableSet.of(-1L), resp.get("missed"));

        resp = createAuthorizedLocalClient(AMOSOV_F)
                .path("/admin/requests/_changeStatus/ids")
                .query("toStatus", QuotaChangeRequest.Status.NEW.toString())
                .query("comment", "42")
                .post(Collections.singletonList(qr2.getId()), new GenericType<Map<String, Set<Long>>>() {
                });

        qr2 = dispenser().quotaChangeRequests()
                .byId(qr2.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.NEW, qr2.getStatus());
        assertTrue(resp.get("missed").isEmpty());
        assertEquals(ImmutableSet.of(qr2.getId()), resp.get("skipped"));
    }

    @Test
    public void adminCanChangeStatusWithoutWorkflowValidationWithTicketKeys() {
        DiQuotaChangeRequest qr1 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields().build(), null)
                .performBy(AMOSOV_F)
                .getFirst();
        DiQuotaChangeRequest qr2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithConfiguredRequiredFields().build(), null)
                .performBy(AMOSOV_F)
                .getFirst();

        Map<String, Set<String>> resp = createAuthorizedLocalClient(AMOSOV_F)
                .path("/admin/requests/_changeStatus/tickets")
                .query("toStatus", QuotaChangeRequest.Status.CONFIRMED.toString())
                .query("comment", "42")
                .post(Arrays.asList(qr1.getTrackerIssueKey(), "FOO-42"), new GenericType<Map<String, Set<String>>>() {
                });

        assertEquals(SpyWebClient.lastResponseStatus(), 200);

        qr1 = dispenser().quotaChangeRequests()
                .byId(qr1.getId())
                .get()
                .perform();
        qr2 = dispenser().quotaChangeRequests()
                .byId(qr2.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, qr1.getStatus());
        assertEquals(DiQuotaChangeRequest.Status.NEW, qr2.getStatus());
        assertTrue(resp.get("skipped").isEmpty());
        assertEquals(ImmutableSet.of("FOO-42"), resp.get("missed"));

        resp = createAuthorizedLocalClient(AMOSOV_F)
                .path("/admin/requests/_changeStatus/tickets")
                .query("toStatus", QuotaChangeRequest.Status.NEW.toString())
                .query("comment", "42")
                .post(Collections.singletonList(qr2.getTrackerIssueKey()), new GenericType<Map<String, Set<String>>>() {
                });

        qr2 = dispenser().quotaChangeRequests()
                .byId(qr2.getId())
                .get()
                .perform();

        assertEquals(DiQuotaChangeRequest.Status.NEW, qr2.getStatus());
        assertTrue(resp.get("missed").isEmpty());
        assertEquals(ImmutableSet.of(qr2.getTrackerIssueKey()), resp.get("skipped"));
    }

    @Test
    public void quotaChangeRequestWontCreateNewCommentsOnEmptyUpdateWithGoal() {

        setupProjectForAnswersTest();

        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithReasonTypeGoalAndGoal()
                        .projectKey(project.getKey().getPublicKey())
                        .build(), null
                ).performBy(AMOSOV_F)
                .getFirst();

        final long id = request.getId();

        dispenser().quotaChangeRequests()
                .byId(id)
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .requestGoalAnswers(GOAL_ANSWER)
                        .chartLinksAbsenceExplanation("foo")
                        .build())
                .performBy(LOTREK);

        dispenser().quotaChangeRequests()
                .byId(id)
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(AMOSOV_F);

        final int expectedCommentsSize = trackerManager.getIssueComments(request.getTrackerIssueKey()).size();

        dispenser().quotaChangeRequests()
                .byId(id)
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .summary(request.getSummary())
                        .resourcePreorderReasonType(request.getResourcePreorderReasonType())
                        .goalId(request.getGoal().getId())
                        .build())
                .performBy(AMOSOV_F);

        assertEquals(expectedCommentsSize, trackerManager.getIssueComments(request.getTrackerIssueKey()).size());
    }

    @Test
    public void quotaRequestCanBeCreatedWithSpecifiedCampaign() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        Campaign campaign2 = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("campaign2")
                .setName("campaign 2")
                .setStartDate(campaign.getStartDate().minusYears(12))
                .setStatus(Campaign.Status.CLOSED)
                .setBigOrders(Arrays.asList(new Campaign.BigOrder(secondBigOrder.getId(), LocalDate.of(2119, 1, 1))))
                .build());

        BotCampaignGroup botCampaignGroup2 = botCampaignGroupDao.create(
                defaultCampaignGroupBuilder()
                        .setKey("bcg2")
                        .setName("BCG 2")
                        .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(secondBigOrder.getId())))
                        .addCampaign(campaignDao.readForBot(Collections.singleton(campaign2.getId())).values().iterator().next())
                        .build()
        );

        prepareCampaignResources();

        campaignDao.partialUpdate(campaign2, new CampaignUpdate.Builder().setStatus(Campaign.Status.ACTIVE).build());

        final DiQuotaChangeRequest request = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests")
                .post(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), new GenericType<DiListResponse<DiQuotaChangeRequest>>() {})
                .getFirst();

        assertEquals(campaign.getId(), request.getCampaign().getId());

        final DiQuotaChangeRequest request2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests")
                .query("campaign", campaign2.getId())
                .post(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(MDB, CPU, secondBigOrder.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), new GenericType<DiListResponse<DiQuotaChangeRequest>>() {})
                .getFirst();

        assertEquals(campaign2.getId(), request2.getCampaign().getId());
    }

    @Test
    public void quotaRequestShowOwningCostOnlyToResponsibleTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AQRU)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();

        assertEquals(1, changes.size());

        DiQuotaChangeRequest.Change change = changes.get(0);

        assertNull(change.getOwningCost());

        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(KEYD)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();

        assertEquals(1, changes2.size());

        DiQuotaChangeRequest.Change change2 = changes2.get(0);

        assertEquals("0", change2.getOwningCost());
    }

    @Test
    public void quotaRequestCanBeCreatedWithImportantFlag() {
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiListResponse<DiQuotaChangeRequest> quotaRequestWithImportant = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(BINARY_CAT);

        final DiListResponse<DiQuotaChangeRequest> quotaRequestWithoutImportant = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(false)
                        .build(), null)
                .performBy(BINARY_CAT);

        final DiListResponse<DiQuotaChangeRequest> quotaRequestWithoutImportantInBody = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);

        updateHierarchy();

        DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(KEYD)
                .path("/v1/quota-requests/" + quotaRequestWithImportant.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertTrue(fetchedRequest.isImportantRequest());

        fetchedRequest = createAuthorizedLocalClient(KEYD)
                .path("/v1/quota-requests/" + quotaRequestWithoutImportant.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertFalse(fetchedRequest.isImportantRequest());

        fetchedRequest = createAuthorizedLocalClient(KEYD)
                .path("/v1/quota-requests/" + quotaRequestWithoutImportantInBody.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertFalse(fetchedRequest.isImportantRequest());

        DiQuotaChangeRequest updatedQuotaRequestWithImportant = dispenser().quotaChangeRequests()
                .byId(quotaRequestWithImportant.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .importantRequest(false)
                        .build())
                .performBy(KEYD);
        Assertions.assertFalse(updatedQuotaRequestWithImportant.isImportantRequest());

        DiQuotaChangeRequest updatedQuotaRequestWithoutImportant = dispenser().quotaChangeRequests()
                .byId(quotaRequestWithoutImportant.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .importantRequest(true)
                        .build())
                .performBy(KEYD);
        Assertions.assertTrue(updatedQuotaRequestWithoutImportant.isImportantRequest());

        final DiQuotaChangeRequest updatedQuotaRequestWithoutImportantInBody = dispenser().quotaChangeRequests()
                .byId(quotaRequestWithoutImportantInBody.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .calculations("Some new calc.")
                        .build())
                .performBy(KEYD);
        Assertions.assertFalse(updatedQuotaRequestWithoutImportantInBody.isImportantRequest());

        updatedQuotaRequestWithImportant = dispenser().quotaChangeRequests()
                .byId(quotaRequestWithImportant.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .calculations("Some new calc.")
                        .build())
                .performBy(KEYD);
        Assertions.assertFalse(updatedQuotaRequestWithImportant.isImportantRequest());

        updatedQuotaRequestWithoutImportant = dispenser().quotaChangeRequests()
                .byId(quotaRequestWithoutImportant.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .calculations("Some new calc.")
                        .build())
                .performBy(KEYD);
        Assertions.assertTrue(updatedQuotaRequestWithoutImportant.isImportantRequest());
    }
}
