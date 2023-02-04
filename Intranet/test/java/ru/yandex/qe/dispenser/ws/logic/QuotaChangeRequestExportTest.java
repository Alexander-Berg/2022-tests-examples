package ru.yandex.qe.dispenser.ws.logic;

import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiPersonGroup;
import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourceGroup;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiService;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.Body;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.BotCampaignGroup;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.CampaignForBot;
import ru.yandex.qe.dispenser.domain.GoalQuestionHelper;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.PersonAffiliation;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.goal.Goal;
import ru.yandex.qe.dispenser.domain.dao.goal.GoalDao;
import ru.yandex.qe.dispenser.domain.dao.goal.OkrAncestors;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.hierarchy.Role;
import ru.yandex.qe.dispenser.ws.ServiceBase;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.bot.Provider;
import ru.yandex.qe.dispenser.ws.quota.request.SetResourceAmountBody;
import ru.yandex.qe.dispenser.ws.quota.request.owning_cost.campaignOwningCost.CampaignOwningCostRefreshManager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest.TEST_BIG_ORDER_DATE;
import static ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest.TEST_BIG_ORDER_DATE_2;
import static ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest.TEST_BIG_ORDER_DATE_3;
import static ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest.TEST_PROJECT_KEY;
import static ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignGroupBuilder;
import static ru.yandex.qe.dispenser.ws.quota.request.owning_cost.campaignOwningCost.CampaignOwningCostRefreshTransactionWrapper.VALID_STATUSES;

public class QuotaChangeRequestExportTest extends BusinessLogicTestBase {

    private static final int HEADERS_SIZE = 23;

    @Autowired
    private BigOrderManager bigOrderManager;

    @Autowired
    private QuotaChangeRequestDao quotaChangeRequestDao;

    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private GoalQuestionHelper goalQuestionHelper;

    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;

    @Autowired
    private GoalDao goalDao;

    @Autowired
    private CampaignOwningCostRefreshManager campaignOwningCostRefreshManager;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private ResourceDao resourceDao;

    private BigOrder bigOrder;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        bigOrderManager.clear();
        bigOrder = bigOrderManager.create(BigOrder.builder(LocalDate.of(2020, 12, 31)));
        campaignDao.clear();
    }


    @Test
    public void exportToCsvShouldWorkCorrectly() throws IOException {
        final DiPerson responsible = BINARY_CAT;

        createProject("Test", INFRA_SPECIAL, responsible.getLogin());

        dispenser().service(MDB)
                .resourceGroups()
                .create(DiResourceGroup.withKey("mdb-pg")
                        .withName("pg")
                        .inService(DiService
                                .withKey(MDB)
                                .withName(MDB)
                                .build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .service(MDB)
                .resource("ALL")
                .create()
                .inGroup("mdb-pg")
                .inMode(DiQuotingMode.SYNCHRONIZATION)
                .withDescription("ALL")
                .withType(DiResourceType.ENUMERABLE)
                .withName("ALL")
                .performBy(AMOSOV_F);

        updateHierarchy();

        final Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .build());

        final BotCampaignGroup campaignGroup = botCampaignGroupDao.create(defaultCampaignGroupBuilder()
                .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrder.getId())))
                .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values().iterator().next())
                .build());

        Body body1 = BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                .projectKey("Test")
                .changes(YP, SEGMENT_CPU, bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(100, DiUnit.CORES))
                .changes(YP, SEGMENT_HDD, bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(200, DiUnit.GIBIBYTE))
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .additionalProperties(Collections.singletonMap("segment", "default"))
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                .build();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> requests1 = dispenser().quotaChangeRequests()
                .create(body1, null)
                .performBy(responsible);

        Body body2 = BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                .projectKey("Test")
                .changes(MDB, RAM, bigOrder.getId(), Collections.emptySet(), DiAmount.of(204L, DiUnit.MEBIBYTE))
                .changes(MDB, "ALL", bigOrder.getId(), Collections.emptySet(), DiAmount.of(42L, DiUnit.COUNT))
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                .build();
        final DiListResponse<DiQuotaChangeRequest> requests2 = dispenser().quotaChangeRequests()
                .create(body2, null)
                .performBy(responsible);

        updateHierarchy();

        final DiQuotaChangeRequest request1 = requests1.getFirst();
        final DiQuotaChangeRequest request2 = requests2.getFirst();
        final String subticket1 = dispenser().quotaChangeRequests()
                .byId(request1.getId())
                .get()
                .perform().getTrackerIssueKey();
        final String subticket2 = dispenser().quotaChangeRequests()
                .byId(request2.getId())
                .get()
                .perform().getTrackerIssueKey();

        final CampaignForBot activeCampaign = campaignDao.getActiveForBotIntegration().get();

        final Response response = createAuthorizedLocalClient(AMOSOV_F, ServiceBase.TEXT_CSV_UTF_8).path("/v1/quota-requests/export/csv").get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.valueOf("text/csv;charset=utf-8"), response.getMediaType());
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));

        final String entity = response.readEntity(String.class);
        final CSVReader reader = new CSVReader(new StringReader(entity));
        final List<String[]> rows = reader.readAll();

        final String[] CURRENT_COLUMN_ORDER = {"ABC_SERVICE", "BIG_ORDER", "RESPONSIBLES",
                "HEAD_1", "HEAD_DEPARTMENT_1", "HEAD_2", "HEAD_DEPARTMENT_2",
                "HEAD_3", "HEAD_DEPARTMENT_3", "PROVIDER", "STATUS", "IMPORTANCE", "UNBALANCED", "PERCENTAGE_OF_CAMPAIGN_OWNING_COST",
                "SUBTICKET", "DC", "BOT_PREORDER", "CAMPAIGN", "JUSTIFICATION", "GOAL_URL", "GOAL_NAME", "SUMMARY", "ABC_SERVICE_SLUG",
                "MDB ALL-pg (units)", "MDB RAM (GiB)", "YP Segment CPU (cores)", "YP Segment Storage (GiB)", "MDB ALL-pg OWNING_COST",
                "MDB RAM OWNING_COST", "YP Segment CPU OWNING_COST", "YP Segment Storage OWNING_COST"
        };
        assertArrayEquals(CURRENT_COLUMN_ORDER, rows.get(0));

        final String[] CURRENT_ROW1_ORDER = {"Test", "2020-12-31", "binarycat", "", "Search", "", "Special infra projects", "binarycat",
                "Test", "MDB", "NEW", "false", "false", "", subticket2, "", "", activeCampaign.getKey(), "GROWTH", "", "", "test", "Test",
                "42", new DecimalFormat("#0.##").format(0.2), "", "", "0", "0", "", ""
        };
        assertArrayEquals(CURRENT_ROW1_ORDER, rows.get(1));

        final String[] CURRENT_ROW2_ORDER = {"Test", "2020-12-31", "binarycat", "", "Search", "", "Special infra projects", "binarycat", "Test",
                "YP", "NEW", "false", "false", "", subticket1, "LOCATION_2, Segment2", "", activeCampaign.getKey(), "GROWTH", "", "", "test", "Test", "", "", "", "200", "", "", "", "0"
        };

        final String[] CURRENT_ROW3_ORDER = {"Test", "2020-12-31", "binarycat", "", "Search", "", "Special infra projects", "binarycat", "Test",
                "YP", "NEW", "false", "false", "", subticket1, "LOCATION_1, Segment1", "", activeCampaign.getKey(), "GROWTH", "", "", "test", "Test", "", "", "100", "", "", "", "0", ""
        };

        final boolean rowTwoIsSecond = Arrays.equals(rows.get(2), CURRENT_ROW2_ORDER);
        final boolean rowTwoIsThird = Arrays.equals(rows.get(2), CURRENT_ROW3_ORDER);
        final boolean rowThreeIsThird = Arrays.equals(rows.get(3), CURRENT_ROW3_ORDER);
        final boolean rowThreeIsSecond = Arrays.equals(rows.get(3), CURRENT_ROW2_ORDER);
        assertTrue((rowTwoIsSecond && rowThreeIsThird) || (rowTwoIsThird && rowThreeIsSecond));
    }

    @Test
    public void exportToCsvShouldWorkCorrectlyIfProjectIsRoot() throws IOException {
        final Project yandex = projectDao.read(YANDEX);
        projectDao.update(Project.copyOf(yandex)
                .abcServiceId(987)
                .build());
        campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .build());
        prepareCampaignResources();
        updateHierarchy();

        dispenser().quotaChangeRequests()
                .create(BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                        .projectKey(YANDEX)
                        .changes(MDB, CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.PERCENT_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(WHISTLER);

        updateHierarchy();

        final Response response = createAuthorizedLocalClient(AMOSOV_F, ServiceBase.TEXT_CSV_UTF_8).path("/v1/quota-requests/export/csv").get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.valueOf("text/csv;charset=utf-8"), response.getMediaType());
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));

        final String entity = response.readEntity(String.class);
        final CSVReader reader = new CSVReader(new StringReader(entity));
        final List<String[]> rows = reader.readAll();

        assertTrue(rows.size() > 0);

    }

    @Test
    public void topLevelProjectShouldHaveHead1EqualsResponsible() throws IOException {
        campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .build());

        Project def = projectDao.read(DEFAULT);
        projectDao.update(Project.copyOf(def).abcServiceId(144).build());

        updateHierarchy();

        prepareCampaignResources();
        dispenser().quotaChangeRequests()
                .create(BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                        .projectKey(DEFAULT)
                        .changes(MDB, CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.PERCENT_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(WHISTLER);

        projectDao.attach(personDao.createIfAbsent(new Person(BINARY_CAT.getLogin(), 1120000000011199L, false, false, false, PersonAffiliation.YANDEX)), projectDao.read(DEFAULT), Role.RESPONSIBLE);

        updateHierarchy();

        final Response response = createAuthorizedLocalClient(AMOSOV_F, ServiceBase.TEXT_CSV_UTF_8).path("/v1/quota-requests/export/csv")
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name()).get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.valueOf("text/csv;charset=utf-8"), response.getMediaType());
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));

        final String entity = response.readEntity(String.class);
        final CSVReader reader = new CSVReader(new StringReader(entity));
        final List<String[]> rows = reader.readAll();

        assertEquals(2, rows.size());

        final String[] values = rows.get(1);

        assertEquals(values[0], values[4]);
        assertEquals(values[2], values[3]);
    }

    @Test
    public void exportToCsvSmallNumbers() throws IOException {
        final DiPerson responsible = BINARY_CAT;

        createProject("Test", INFRA_SPECIAL, responsible.getLogin());

        updateHierarchy();


        final Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .build());
        final BotCampaignGroup campaignGroup = botCampaignGroupDao.create(defaultCampaignGroupBuilder()
                .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrder.getId())))
                .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values().iterator().next())
                .build());


        prepareCampaignResources();
        Body body1 = BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                .projectKey("Test")
                .changes(YP, SEGMENT_HDD, bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(512, DiUnit.KIBIBYTE))
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .additionalProperties(Collections.singletonMap("segment", "default"))
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                .build();
        final DiListResponse<DiQuotaChangeRequest> requests1 = dispenser().quotaChangeRequests()
                .create(body1, null)
                .performBy(responsible);

        updateHierarchy();

        final DiQuotaChangeRequest request1 = requests1.getFirst();

        final Response response = createAuthorizedLocalClient(AMOSOV_F, ServiceBase.TEXT_CSV_UTF_8).path("/v1/quota-requests/export/csv").get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.valueOf("text/csv;charset=utf-8"), response.getMediaType());
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));

        final String entity = response.readEntity(String.class);
        final CSVReader reader = new CSVReader(new StringReader(entity));
        final List<String[]> rows = reader.readAll();

        assertEquals(new DecimalFormat("#0.#####").format(0.00049d), rows.get(1)[HEADERS_SIZE]);
    }

    @Test
    public void exportToCsvShouldContainAllProvidersResourceIfFlagIsRaised() throws IOException {
        campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .build());

        List<String[]> table = table();

        assertEquals(1, table.size());
        assertEquals(HEADERS_SIZE, table.get(0).length);

        table = table(ImmutableMap.of("allResources", true, "allResourcesCampaignId", -1));

        final long availableResourcesByProvidersCount = Stream.of(Provider.values())
                .map(Provider::getService)
                .filter(Objects::nonNull)
                .map(Hierarchy.get().getResourceReader()::getByService)
                .mapToLong(Set::size)
                .sum();

        assertEquals(1, table.size());
        assertEquals(table.get(0).length, HEADERS_SIZE + availableResourcesByProvidersCount * 2);

        final Service yp = Hierarchy.get().getServiceReader().read(YP);
        final Resource resource = Hierarchy.get().getResourceReader().read(new Resource.Key(SEGMENT_CPU, yp));

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final Body body = BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                .projectKey(project.getPublicKey())
                .changes(yp.getKey(), resource.getPublicKey(), bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(512, DiUnit.CORES))
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .additionalProperties(Collections.singletonMap("segment", "default"))
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                .build();
        prepareCampaignResources();
        dispenser().quotaChangeRequests()
                .create(body, null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        table = table(ImmutableMap.of("allResources", true));

        assertEquals(2, table.size());

        int resourceIndex = -1;

        final String[] headers = table.get(0);
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].contains(String.format("%s %s", yp.getName(), resource.getName()))) {
                resourceIndex = i;
                break;
            }
        }
        assertNotEquals(-1, resourceIndex);
        assertFalse(table.get(1)[resourceIndex].isEmpty());
    }


    @Test
    public void exportToCsvShouldContainAllActiveCampaignResourceIfFlagIsRaised() throws IOException {
        campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .build());
        prepareCampaignResources();

        List<String[]> table = table();

        assertEquals(1, table.size());
        assertEquals(HEADERS_SIZE, table.get(0).length);

        table = table(ImmutableMap.of("allResources", true));

        final long availableResourcesByProvidersCount = Hierarchy.get().getServiceReader().getAll().stream()
                .map(Hierarchy.get().getResourceReader()::getByService)
                .mapToLong(Set::size)
                .sum();

        assertEquals(1, table.size());
        assertEquals(HEADERS_SIZE + availableResourcesByProvidersCount * 2, table.get(0).length);

        final Service yp = Hierarchy.get().getServiceReader().read(YP);
        final Resource resource = Hierarchy.get().getResourceReader().read(new Resource.Key(SEGMENT_CPU, yp));

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final Body body = BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                .projectKey(project.getPublicKey())
                .changes(yp.getKey(), resource.getPublicKey(), bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(512, DiUnit.CORES))
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .additionalProperties(Collections.singletonMap("segment", "default"))
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                .build();
        prepareCampaignResources();
        dispenser().quotaChangeRequests()
                .create(body, null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        table = table(ImmutableMap.of("allResources", true));

        assertEquals(2, table.size());

        int resourceIndex = -1;

        final String[] headers = table.get(0);
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].contains(String.format("%s %s", yp.getName(), resource.getName()))) {
                resourceIndex = i;
                break;
            }
        }
        assertNotEquals(-1, resourceIndex);
        assertFalse(table.get(1)[resourceIndex].isEmpty());
    }

    @Test
    public void exportToCsvShouldContainAllCampaignResourceIfFlagIsRaisedAndCampaignIdProvided() throws IOException {
        final Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .build());
        prepareCampaignResources();

        List<String[]> table = table();

        assertEquals(1, table.size());
        assertEquals(HEADERS_SIZE, table.get(0).length);

        table = table(ImmutableMap.of("allResources", true));

        final long availableResourcesByProvidersCount = Hierarchy.get().getServiceReader().getAll().stream()
                .map(Hierarchy.get().getResourceReader()::getByService)
                .mapToLong(Set::size)
                .sum();

        assertEquals(1, table.size());
        assertEquals(table.get(0).length, HEADERS_SIZE + availableResourcesByProvidersCount * 2);

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final Service yp = Hierarchy.get().getServiceReader().read(YP);
        final Resource resource = Hierarchy.get().getResourceReader().read(new Resource.Key(SEGMENT_CPU, yp));

        final Body body = BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                .projectKey(project.getPublicKey())
                .changes(yp.getKey(), resource.getPublicKey(), bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(512, DiUnit.CORES))
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .additionalProperties(Collections.singletonMap("segment", "default"))
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                .build();
        prepareCampaignResources();
        dispenser().quotaChangeRequests()
                .create(body, null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        table = table(ImmutableMap.of("allResources", true, "allResourcesCampaignId", campaign.getId()));

        assertEquals(2, table.size());

        int resourceIndex = -1;

        final String[] headers = table.get(0);
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].contains(String.format("%s %s", yp.getName(), resource.getName()))) {
                resourceIndex = i;
                break;
            }
        }
        assertNotEquals(-1, resourceIndex);
        assertFalse(table.get(1)[resourceIndex].isEmpty());
    }

    private List<String[]> table() throws IOException {
        return table(Collections.emptyMap());
    }

    private List<String[]> table(final Map<String, Object> params) throws IOException {
        final WebClient client = createAuthorizedLocalClient(AMOSOV_F, ServiceBase.TEXT_CSV_UTF_8).path("/v1/quota-requests/export/csv");
        params.forEach(client::query);
        return tableFromResponse(client.get());
    }

    private static List<String[]> tableFromResponse(final Response response) throws IOException {
        final CSVReader reader = new CSVReader(new StringReader(response.readEntity(String.class)));
        return reader.readAll();
    }

    @Test
    public void exportToCsvShouldContainGoalAnswersIfFlagIsPresent() throws IOException {
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

        List<String[]> table;
        final Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .build());

        goalDao.clear();
        goalDao.create(new Goal(77L, "Do it!", ru.yandex.inside.goals.model.Goal.Importance.COMPANY, ru.yandex.inside.goals.model.Goal.Status.PLANNED, OkrAncestors.EMPTY));

        prepareCampaignResources();
        final long growthId = dispenser().quotaChangeRequests()
                .create(BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                        .projectKey(key)
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(512, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(WHISTLER).getFirst().getId();

        final long goalId = dispenser().quotaChangeRequests()
                .create(BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                        .projectKey(key)
                        .changes(MDB, CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(512, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                        .goalId(77L)
                        .build(), null)
                .performBy(WHISTLER).getFirst().getId();

        final ImmutableMap<Long, String> growthAnswers = ImmutableMap.of(
                4L, "yes!",
                5L, "idk!",
                6L, "too!"
        );

        dispenser().quotaChangeRequests()
                .byId(growthId)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(growthAnswers).build())
                .performBy(WHISTLER);


        final ImmutableMap<Long, String> goalAnswers = ImmutableMap.of(
                0L, "fine!",
                1L, "good!",
                2L, "done!"
        );

        dispenser().quotaChangeRequests()
                .byId(goalId)
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(goalAnswers).build())
                .performBy(WHISTLER);

        table = table(ImmutableMap.of("goalQuestions", true, "service", "nirvana"));

        assertEquals(2, table.size());
        String[] headers = table.get(0);
        String[] values = table.get(1);

        final List<GoalQuestionHelper.RequestGoalQuestionId> questionIds = goalQuestionHelper.getRequestGoalQuestionIds();
        final int shift = values.length - questionIds.size();

        for (int i = 0; i < questionIds.size(); i++) {
            final GoalQuestionHelper.RequestGoalQuestionId questionId = questionIds.get(i);
            assertEquals(headers[i + shift], goalQuestionHelper.getRequestGoalQuestion(questionId));
            if (questionId.getReasonType() == DiResourcePreorderReasonType.GROWTH) {
                assertEquals(values[i + shift], growthAnswers.get(questionId.getId()));
            }
        }

        table = table(ImmutableMap.of("goalQuestions", true, "service", "mdb"));

        assertEquals(2, table.size());
        headers = table.get(0);
        values = table.get(1);

        for (int i = 0; i < questionIds.size(); i++) {
            final GoalQuestionHelper.RequestGoalQuestionId questionId = questionIds.get(i);
            assertEquals(headers[i + shift], goalQuestionHelper.getRequestGoalQuestion(questionId));
            if (questionId.getReasonType() == DiResourcePreorderReasonType.GOAL) {
                assertEquals(values[i + shift], goalAnswers.get(questionId.getId()));
            }
        }

    }

    @Test
    public void exportMultiServiceRequestShouldWork() throws IOException {
        campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .build());

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        final String req1TicketKey = dispenser().quotaChangeRequests()
                .create(BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(512, DiUnit.COUNT))
                        .changes(MDB, CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(128, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(WHISTLER).getFirst().getTrackerIssueKey();

        final String req2TicketKey = dispenser().quotaChangeRequests()
                .create(BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(MDB, RAM, bigOrder.getId(), Collections.emptySet(), DiAmount.of(256, DiUnit.GIBIBYTE))
                        .changes(YP, SEGMENT_HDD, bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(64, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(WHISTLER).getFirst().getTrackerIssueKey();

        final Map<ExportRowKey, Map<String, String>> rowByKey = getRequestExportDataByKey(
                createAuthorizedLocalClient(AMOSOV_F, ServiceBase.TEXT_CSV_UTF_8).path("/v1/quota-requests/export/csv")
        );

        assertEquals(4, rowByKey.size());

        final Map<String, String> nirvanaRow = rowByKey.get(new ExportRowKey(req1TicketKey, "Nirvana", "", "2020-12-31"));
        assertEquals("512", nirvanaRow.get("Nirvana YT CPU (units)"));

        final Map<String, String> ypRow = rowByKey.get(new ExportRowKey(req2TicketKey, "YP", "LOCATION_1, Segment1", "2020-12-31"));
        assertEquals("64", ypRow.get("YP Segment Storage (GiB)"));

        final Map<String, String> mdb1Row = rowByKey.get(new ExportRowKey(req1TicketKey, "MDB", "", "2020-12-31"));
        assertEquals("128", mdb1Row.get("MDB CPU (cores)"));

        final Map<String, String> mdb2Row = rowByKey.get(new ExportRowKey(req2TicketKey, "MDB", "", "2020-12-31"));
        assertEquals("256", mdb2Row.get("MDB RAM (GiB)"));
    }

    @Test
    public void exportToCsvShouldContainReadyAndAllocatedIfFlagIsPresent() throws IOException {
        Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .setType(Campaign.Type.AGGREGATED)
                .build());
        prepareCampaignResources();

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final ImmutableSet<String> cpuSegments = ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1);
        final Body body = BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                .projectKey(project.getPublicKey())
                .changes(YP, SEGMENT_CPU, bigOrder.getId(), cpuSegments, DiAmount.of(100, DiUnit.CORES))
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .additionalProperties(Collections.singletonMap("segment", "default"))
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                .chartLinksAbsenceExplanation("foo")
                .build();

        final DiQuotaChangeRequest req = dispenser().quotaChangeRequests()
                .create(body, campaign.getId())
                .performBy(AMOSOV_F)
                .getFirst();

        updateHierarchy();

        final ImmutableMap<Long, String> growthAnswers = ImmutableMap.of(
                4L, "yes!",
                5L, "idk!",
                6L, "too!"
        );

        dispenser().quotaChangeRequests()
                .byId(req.getId())
                .update(new BodyUpdate.BodyUpdateBuilder().requestGoalAnswers(growthAnswers).build())
                .performBy(AMOSOV_F);

        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + req.getId() + "/status/READY_FOR_REVIEW")
                .put(null);

        assertEquals(200, response.getStatus());

        createAuthorizedLocalClient(WHISTLER)
                .path("/v1/quota-requests/" + req.getId() + "/status/APPROVED")
                .put(null);
        response = createAuthorizedLocalClient(KEYD)
                .path("/v1/quota-requests/" + req.getId() + "/status/CONFIRMED")
                .put(null);

        assertEquals(200, response.getStatus());

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, new SetResourceAmountBody(ImmutableList.of(
                        new SetResourceAmountBody.Item(req.getId(), null, ImmutableList.of(
                                new SetResourceAmountBody.ChangeBody(YP, bigOrder.getId(), SEGMENT_CPU, cpuSegments, DiAmount.of(10, DiUnit.CORES), DiAmount.of(20, DiUnit.CORES))
                        ), null)
                )));

        List<String[]> table = table();
        String[] headers = table.get(0);

        assertEquals(HEADERS_SIZE + 2, headers.length);

        table = table(ImmutableMap.of("showReadyAndAllocated", true));
        headers = table.get(0);
        final String[] values = table.get(1);

        assertEquals(HEADERS_SIZE + 4, headers.length);
        assertEquals("10", values[HEADERS_SIZE + 2]);
        assertEquals("20", values[HEADERS_SIZE + 3]);

        final long availableResourcesByProvidersCount = Stream.of(Provider.values())
                .map(Provider::getService)
                .filter(Objects::nonNull)
                .map(Hierarchy.get().getResourceReader()::getByService)
                .mapToLong(Set::size)
                .sum();

        table = table(ImmutableMap.of("showReadyAndAllocated", true, "allResources", true, "allResourcesCampaignId", -1));
        headers = table.get(0);

        assertEquals(HEADERS_SIZE + availableResourcesByProvidersCount * 4, headers.length);
    }

    @Test
    public void exportToCsvShouldContainGoalsHierarchyIfFlagIsPresent() throws IOException {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .build());
        prepareCampaignResources();

        goalDao.clear();
        goalDao.create(new Goal(200L, "G1", ru.yandex.inside.goals.model.Goal.Importance.COMPANY, ru.yandex.inside.goals.model.Goal.Status.PLANNED, OkrAncestors.EMPTY));
        goalDao.create(new Goal(100L, "ValueStream", ru.yandex.inside.goals.model.Goal.Importance.OKR, ru.yandex.inside.goals.model.Goal.Status.PLANNED, OkrAncestors.EMPTY));
        goalDao.create(new Goal(201L, "G2", ru.yandex.inside.goals.model.Goal.Importance.COMPANY, ru.yandex.inside.goals.model.Goal.Status.PLANNED, new OkrAncestors(ImmutableMap.of(OkrAncestors.OkrType.VALUE_STREAM, 100L))));

        goalDao.create(new Goal(101L, "Umbrella", ru.yandex.inside.goals.model.Goal.Importance.OKR, ru.yandex.inside.goals.model.Goal.Status.PLANNED, new OkrAncestors(ImmutableMap.of(OkrAncestors.OkrType.VALUE_STREAM, 100L))));
        goalDao.create(new Goal(202L, "G3", ru.yandex.inside.goals.model.Goal.Importance.COMPANY, ru.yandex.inside.goals.model.Goal.Status.PLANNED, new OkrAncestors(ImmutableMap.of(OkrAncestors.OkrType.VALUE_STREAM, 100L, OkrAncestors.OkrType.UMBRELLA, 101L))));

        goalDao.create(new Goal(102L, "Contour", ru.yandex.inside.goals.model.Goal.Importance.OKR, ru.yandex.inside.goals.model.Goal.Status.PLANNED, new OkrAncestors(ImmutableMap.of(OkrAncestors.OkrType.VALUE_STREAM, 100L, OkrAncestors.OkrType.UMBRELLA, 101L))));
        goalDao.create(new Goal(203L, "G3", ru.yandex.inside.goals.model.Goal.Importance.COMPANY, ru.yandex.inside.goals.model.Goal.Status.PLANNED, new OkrAncestors(ImmutableMap.of(OkrAncestors.OkrType.VALUE_STREAM, 100L, OkrAncestors.OkrType.UMBRELLA, 101L, OkrAncestors.OkrType.CONTOUR, 102L))));

        quotaChangeRequestDao.clear();

        final Map<Long, String> requestTicketKeyByGoalId = new HashMap<>();

        for (final Goal goal : goalDao.getAll()) {
            final DiQuotaChangeRequest req = dispenser().quotaChangeRequests()
                    .create(BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                            .projectKey(project.getPublicKey())
                            .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.COUNT))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                            .goalId(goal.getId())
                            .chartLinksAbsenceExplanation("foo")
                            .build(), null)
                    .performBy(AMOSOV_F)
                    .getFirst();

            requestTicketKeyByGoalId.put(goal.getId(), req.getTrackerIssueKey());
        }

        final DiQuotaChangeRequest growthReq = dispenser().quotaChangeRequests()
                .create(BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .chartLinksAbsenceExplanation("foo")
                        .build(), null)
                .performBy(AMOSOV_F)
                .getFirst();


        updateHierarchy();

        final Stream<Map<String, String>> rowStream = getRowStream(createAuthorizedLocalClient(AMOSOV_F, ServiceBase.TEXT_CSV_UTF_8)
                .path("/v1/quota-requests/export/csv")
                .query("showGoalHierarchy", true)
                .query("service", NIRVANA)
        );


        final Map<String, List<String>> rowByTicketKey = rowStream.collect(Collectors.toMap(r -> r.get("SUBTICKET"),
                r -> Arrays.asList(r.get("VALUE_STREAM: Url"), r.get("VALUE_STREAM: Name"), r.get("UMBRELLA: Url"), r.get("UMBRELLA: Name"), r.get("CONTOUR: Url"), r.get("CONTOUR: Name"))));

        assertEquals(rowByTicketKey.get(requestTicketKeyByGoalId.get(100L)), Arrays.asList("", "", "", "", "", ""));
        assertEquals(rowByTicketKey.get(requestTicketKeyByGoalId.get(101L)), Arrays.asList("https://goals.yandex-team.ru/filter?goal=100", "ValueStream", "", "", "", ""));
        assertEquals(rowByTicketKey.get(requestTicketKeyByGoalId.get(102L)), Arrays.asList("https://goals.yandex-team.ru/filter?goal=100", "ValueStream", "https://goals.yandex-team.ru/filter?goal=101", "Umbrella", "", ""));
        assertEquals(rowByTicketKey.get(requestTicketKeyByGoalId.get(200L)), Arrays.asList("", "", "", "", "", ""));
        assertEquals(rowByTicketKey.get(requestTicketKeyByGoalId.get(201L)), Arrays.asList("https://goals.yandex-team.ru/filter?goal=100", "ValueStream", "", "", "", ""));
        assertEquals(rowByTicketKey.get(requestTicketKeyByGoalId.get(202L)), Arrays.asList("https://goals.yandex-team.ru/filter?goal=100", "ValueStream", "https://goals.yandex-team.ru/filter?goal=101", "Umbrella", "", ""));
        assertEquals(rowByTicketKey.get(requestTicketKeyByGoalId.get(203L)), Arrays.asList("https://goals.yandex-team.ru/filter?goal=100", "ValueStream", "https://goals.yandex-team.ru/filter?goal=101", "Umbrella", "https://goals.yandex-team.ru/filter?goal=102", "Contour"));

        assertEquals(rowByTicketKey.get(growthReq.getTrackerIssueKey()), Arrays.asList("", "", "", "", "", ""));
    }

    @Test
    public void requestResponsibleShouldBeInReport() throws IOException {
        final String responsible1 = BINARY_CAT.getLogin();
        final String responsible2 = STARLIGHT.getLogin();
        final String responsible3 = ILLYUSION.getLogin();

        createProject("Test", INFRA_SPECIAL, responsible1);
        createProject("Test1", "Test", responsible2);
        createProject("Test2", "Test1");
        createProject("Test3", "Test2", responsible3);

        campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .build());
        prepareCampaignResources();

        final Map<String, String> ticketKeyByProjectKey = new HashMap<>();

        for (final String projectKey : ImmutableSet.of("Test", "Test1", "Test2", "Test3")) {
            final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                    .create(BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                            .projectKey(projectKey)
                            .changes(YP, SEGMENT_CPU, bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(100, DiUnit.CORES))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .additionalProperties(Collections.singletonMap("segment", "default"))
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                            .build(), null)
                    .performBy(AMOSOV_F);
            ticketKeyByProjectKey.put(projectKey, requests.getFirst().getTrackerIssueKey());
        }

        final Collection<Map<String, String>> rows = getRequestExportDataByKey(
                createAuthorizedLocalClient(AMOSOV_F, ServiceBase.TEXT_CSV_UTF_8).path("/v1/quota-requests/export/csv")
        ).values();

        final Set<Pair<String, String>> ticketAndResponsible = rows.stream()
                .map(r -> Pair.of(r.get("SUBTICKET"), r.get("RESPONSIBLES")))
                .collect(Collectors.toSet());


        assertEquals(ticketAndResponsible, ImmutableSet.of(
                Pair.of(ticketKeyByProjectKey.get("Test"), responsible1),
                Pair.of(ticketKeyByProjectKey.get("Test1"), responsible2),
                Pair.of(ticketKeyByProjectKey.get("Test2"), responsible2),
                Pair.of(ticketKeyByProjectKey.get("Test3"), responsible3)
        ));


    }

    @Test
    public void emptyChangesCanBeFiltered() throws IOException {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .build());
        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(100, DiUnit.CORES))
                        .changes(YP, SEGMENT_CPU, bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_2), DiAmount.of(0, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .additionalProperties(Collections.singletonMap("segment", "default"))
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        List<String[]> rows = table();
        assertEquals(rows.size(), 3);

        rows = table(Collections.singletonMap("filterEmptyResources", true));
        assertEquals(rows.size(), 2);
    }

    @Test
    public void importantFlagShouldBeInReportTest() throws IOException {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        campaignDao.create(defaultCampaignBuilder(bigOrder)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2020, 12, 31))))
                .build());
        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(100, DiUnit.CORES))
                        .changes(YP, SEGMENT_CPU, bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_2), DiAmount.of(0, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .summary("IMPORTANT")
                        .build(), null)
                .performBy(AMOSOV_F);

        final DiListResponse<DiQuotaChangeRequest> requests2 = dispenser().quotaChangeRequests()
                .create(BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(100, DiUnit.CORES))
                        .changes(YP, SEGMENT_CPU, bigOrder.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_2), DiAmount.of(0, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(false)
                        .summary("NOT IMPORTANT")
                        .build(), null)
                .performBy(AMOSOV_F);

        List<String[]> rows = table();
        assertEquals(5, rows.size());

        assertTrue(rows.stream()
                .skip(1)
                .allMatch(strings -> (strings[21].equals("IMPORTANT") && strings[11].equals("true"))
                        || (strings[21].equals("NOT IMPORTANT") && strings[11].equals("false"))));
    }

    @Test
    public void percentageOwningCostReturnedByReportTest() throws IOException {
        campaignDao.clear();
        BigOrder bigOrderOne = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE));
        BigOrder bigOrderThree = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE_3));
        BigOrder bigOrderTwo = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE_2));
        LocalDate date100 = LocalDate.of(2020, Month.AUGUST, 1);
        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderThree)
                .setKey("aug2020")
                .setName("aug2020")
                .setId(100L)
                .setStartDate(date100)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderThree.getId(),
                        date100)))
                .build());

        LocalDate date142 = LocalDate.of(2021, Month.FEBRUARY, 1);
        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderTwo)
                .setKey("feb2021")
                .setName("feb2021")
                .setId(142L)
                .setStartDate(date142)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderTwo.getId(),
                        date142)))
                .build());

        LocalDate date = LocalDate.of(2021, Month.AUGUST, 1);
        Campaign campaign = campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2021")
                .setName("aug2021")
                .setId(143L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date)))
                .build());

        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2022aggregated")
                .setName("aug2022aggregated")
                .setId(176L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date)))
                .build());

        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2022draft")
                .setName("aug2022draft")
                .setId(177L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date)))
                .build());

        botCampaignGroupDao.create(
                defaultCampaignGroupBuilder()
                        .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrderOne.getId())))
                        .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values().iterator().next())
                        .build()
        );

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

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
                        .projectKey(TEST_PROJECT_KEY)
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
                .performBy(AMOSOV_F);

        long id = quotaRequests.getFirst().getId();
        QuotaChangeRequest read = quotaChangeRequestDao.read(id);
        quotaChangeRequestDao.update(read.copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());

        campaignOwningCostRefreshManager.refresh();


        List<String[]> rows = table();

        assertEquals("100", rows.get(1)[13]);
    }

    @Test
    public void percentageOwningCostCalculatedShownOnlyForRequestInValidStatusesInReportTest() throws IOException {
        campaignDao.clear();
        BigOrder bigOrderOne = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE));
        BigOrder bigOrderThree = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE_3));
        BigOrder bigOrderTwo = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE_2));
        LocalDate date100 = LocalDate.of(2020, Month.AUGUST, 1);
        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderThree)
                .setKey("aug2020")
                .setName("aug2020")
                .setId(100L)
                .setStartDate(date100)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderThree.getId(),
                        date100)))
                .build());

        LocalDate date142 = LocalDate.of(2021, Month.FEBRUARY, 1);
        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderTwo)
                .setKey("feb2021")
                .setName("feb2021")
                .setId(142L)
                .setStartDate(date142)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderTwo.getId(),
                        date142)))
                .build());

        LocalDate date = LocalDate.of(2021, Month.AUGUST, 1);
        Campaign campaign = campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2021")
                .setName("aug2021")
                .setId(143L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date)))
                .build());

        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2022aggregated")
                .setName("aug2022aggregated")
                .setId(176L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date)))
                .build());

        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2022draft")
                .setName("aug2022draft")
                .setId(177L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date)))
                .build());

        botCampaignGroupDao.create(
                defaultCampaignGroupBuilder()
                        .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrderOne.getId())))
                        .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values().iterator().next())
                        .build()
        );

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

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

        DiQuotaChangeRequest first = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
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
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest second = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest third = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest four = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest five = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest six = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest seven = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest eight = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest nine = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        quotaChangeRequestDao.update(quotaChangeRequestDao.read(first.getId()).copyBuilder().status(QuotaChangeRequest.Status.NEW).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(second.getId()).copyBuilder().status(QuotaChangeRequest.Status.CANCELLED).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(third.getId()).copyBuilder().status(QuotaChangeRequest.Status.REJECTED).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(four.getId()).copyBuilder().status(QuotaChangeRequest.Status.CONFIRMED).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(five.getId()).copyBuilder().status(QuotaChangeRequest.Status.READY_FOR_REVIEW).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(six.getId()).copyBuilder().status(QuotaChangeRequest.Status.COMPLETED).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(seven.getId()).copyBuilder().status(QuotaChangeRequest.Status.APPLIED).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(eight.getId()).copyBuilder().status(QuotaChangeRequest.Status.NEED_INFO).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(nine.getId()).copyBuilder().status(QuotaChangeRequest.Status.APPROVED).build());

        updateHierarchy();

        campaignOwningCostRefreshManager.refresh();

        updateHierarchy();

        campaignOwningCostRefreshManager.refresh();


        List<String[]> rows = table();

        Assertions.assertEquals(10, rows.size());
        rows.stream()
                .skip(1)
                .forEach(row -> assertEquals(VALID_STATUSES.contains(QuotaChangeRequest.Status.valueOf(row[10])),
                        !"".equals(row[13])));
    }

    @NotNull
    public static Map<ExportRowKey, Map<String, String>> getRequestExportDataByKey(final WebClient query) throws IOException {
        return getRowStream(query)
                .collect(Collectors.toMap(r -> new ExportRowKey(r.get("SUBTICKET"), r.get("PROVIDER"), r.get("DC"), r.get("BIG_ORDER")), Function.identity()));
    }

    public static Stream<Map<String, String>> getRowStream(final WebClient query) throws IOException {
        final String entity = query.get(String.class);

        final CSVReader reader = new CSVReader(new StringReader(entity));
        final List<String[]> rows = reader.readAll();
        final String[] headers = rows.get(0);

        final Stream<Map<String, String>> mapStream = rows.stream()
                .skip(1)
                .map(r -> {
                    final Map<String, String> result = new HashMap<>();

                    for (int i = 0; i < headers.length; i++) {
                        result.put(headers[i], r[i]);
                    }

                    return result;
                });
        return mapStream;
    }

    public static class ExportRowKey {
        private final String ticketId;
        private final String service;
        private final String dc;
        private final String bigOrder;

        public ExportRowKey(String ticketId, String service, String dc, String bigOrder) {
            this.ticketId = ticketId;
            this.service = service;
            this.dc = dc;
            this.bigOrder = bigOrder;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ExportRowKey that = (ExportRowKey) o;
            return Objects.equals(ticketId, that.ticketId) &&
                    Objects.equals(service, that.service) &&
                    Objects.equals(dc, that.dc) &&
                    Objects.equals(bigOrder, that.bigOrder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ticketId, service, dc, bigOrder);
        }
    }

}
