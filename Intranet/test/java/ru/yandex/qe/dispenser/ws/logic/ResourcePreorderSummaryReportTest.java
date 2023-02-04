package ru.yandex.qe.dispenser.ws.logic;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.domain.BotCampaignGroup;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.PersonAffiliation;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.ResourceSegmentation;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.dispenser_admins.DispenserAdminsDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.project.role.ProjectRoleDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Role;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;
import ru.yandex.qe.dispenser.ws.ServiceBase;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.bot.Provider;
import ru.yandex.qe.dispenser.ws.quota.request.report.summary.ResourcePreorderSummaryReport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.qe.dispenser.ws.logic.BusinessLogicTestBase.assertThrowsForbiddenWithMessage;
import static ru.yandex.qe.dispenser.ws.logic.BusinessLogicTestBase.assertThrowsWithMessage;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class ResourcePreorderSummaryReportTest extends AcceptanceTestBase {
    @Autowired
    private BigOrderManager bigOrderManager;
    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;
    @Autowired
    private SegmentationDao segmentationDao;
    @Autowired
    private QuotaChangeRequestDao quotaChangeRequestDao;
    @Autowired
    private ResourceSegmentationDao resourceSegmentationDao;
    @Autowired
    private ProjectRoleDao projectRoleDao;
    @Autowired
    private DispenserAdminsDao dispenserAdminsDao;
    @Autowired
    private PersonDao personDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;
    @Autowired
    private SegmentDao segmentDao;

    private Project abcdProject;
    private Project abcProject;
    private Project dispenserProject;
    private Project dProject;
    private Project dispenserProjectWithVs;

    private Segment vla;
    private Segment sas;
    private Segment man;

    private Service ytService;
    private Service ypService;

    private Campaign campaign;
    private BotCampaignGroup botCampaignGroup;

    private Campaign.BigOrder bigOrder37;
    private Campaign.BigOrder bigOrder38;
    private Campaign.BigOrder bigOrder39;

    private Resource ypCpu;
    private Resource ytCpu;

    @BeforeAll
    public void beforeClass() {
        reinitialize();

        final Project yandex = projectDao.createIfAbsent(Project.withKey(YANDEX).name("Yandex").build());
        final Project searchPortal = projectDao.createIfAbsent(Project.withKey("meta_search").abcServiceId(851).name("Search Portal").description("Search Portal").parent(yandex).build());
        final Project tools = projectDao.createIfAbsent(Project.withKey("tools").abcServiceId(872).name("Tools + b2b").description("Tools + b2b").parent(searchPortal).build());
        abcdProject = projectDao.createIfAbsent(Project.withKey("abcd").abcServiceId(24483).name("ABCD").description("ABCD").parent(tools).build());
        abcProject = projectDao.createIfAbsent(Project.withKey("adc").abcServiceId(989).name("ABC").description("ABC").parent(abcdProject).build());
        dispenserProject = projectDao.createIfAbsent(Project.withKey("dispenser-test").abcServiceId(1357).name("Dispenser").description("Dispenser").parent(abcProject).build());
        dProject = projectDao.createIfAbsent(Project.withKey("d").abcServiceId(31966).name("D Service").description("D Service").parent(abcdProject).build());
        updateHierarchy();

        projectDao.attach(personDao.read(KEYD.getLogin()), searchPortal, Role.RESPONSIBLE);

        bigOrderManager.clear();

        List<BigOrder> bigOrders = new ArrayList<>();
        BigOrder bigOrderOne = bigOrderManager.create(BigOrder.builder(LocalDate.of(3019, 11, 30)));
        BigOrder bigOrderTwo = bigOrderManager.create(BigOrder.builder(LocalDate.of(3019, 12, 30)));
        BigOrder bigOrderThree = bigOrderManager.create(BigOrder.builder(LocalDate.of(3019, 12, 31)));
        bigOrders.add(bigOrderOne);
        bigOrders.add(bigOrderTwo);
        bigOrders.add(bigOrderThree);

        segmentationDao.clear();
        segmentDao.clear();
        final Segmentation dcSegmentation = segmentationDao.create(new Segmentation.Builder(DC_SEGMENTATION)
                .name("Data Centers")
                .description("Data centers segmentation")
                .build());
        updateHierarchy();

        vla = segmentDao.create(new Segment.Builder("VLA", dcSegmentation)
                .name("VLA")
                .description("VLA")
                .priority((short) 1)
                .build());

        sas = segmentDao.create(new Segment.Builder("SAS", dcSegmentation)
                .name("SAS")
                .description("SAS")
                .priority((short) 2)
                .build());

        man = segmentDao.create(new Segment.Builder("MAN", dcSegmentation)
                .name("MAN")
                .description("MAN")
                .priority((short) 3)
                .build());

        final Person amosovf = personDao.createIfAbsent(new Person(AMOSOV_F.getLogin(), 1120000000022901L, false, false, false, PersonAffiliation.YANDEX));
        dispenserAdminsDao.setDispenserAdmins(Collections.singleton(amosovf));

        ytService = serviceDao.create(Service.withKey(Provider.YT.getServiceKey())
                .withName("YT")
                .withAbcServiceId(470)
                .withSettings(Service.Settings.builder()
                        .accountActualValuesInQuotaDistribution(false)
                        .usesProjectHierarchy(true)
                        .manualQuotaAllocation(false)
                        .build())
                .build());
        ypService = Provider.YP.getService();

        updateHierarchy();

        ypCpu = resourceDao.create(new Resource.Builder(CPU, ypService)
                .name("CPU")
                .type(DiResourceType.ENUMERABLE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        ytCpu = resourceDao.create(new Resource.Builder(RAM, ytService)
                .name("CPU")
                .type(DiResourceType.ENUMERABLE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        updateHierarchy();

        resourceSegmentationDao.createAll(ImmutableList.of(
                new ResourceSegmentation.Builder(ypCpu, dcSegmentation).build()
        ));

        campaignDao.clear();
        campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setBigOrders(bigOrders.stream()
                        .map(bo -> new Campaign.BigOrder(bo.getId(), bo.getDate()))
                        .collect(Collectors.toList()))
                .build());

        final Set<BigOrder> simpleBigOrders = bigOrderManager.getByIds(ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId(), bigOrderThree.getId()));

        botCampaignGroupDao.clear();
        botCampaignGroup = botCampaignGroupDao.create(BotCampaignGroup.builder()
                .setKey("test_campaign_group")
                .setActive(true)
                .setName("Test Campaign Group")
                .setBotPreOrderIssueKey("DISPENSERREQ-1")
                .setBigOrders(simpleBigOrders.stream()
                        .sorted(Comparator.comparing(BigOrder::getId))
                        .collect(Collectors.toList())
                )
                .addCampaign(campaign.forBot(simpleBigOrders.stream().collect(Collectors.toMap(BigOrder::getId, Function.identity()))))
                .build()
        );

        final Map<Long, Campaign.BigOrder> bigOrderById = campaign.getBigOrders().stream()
                .collect(Collectors.toMap(Campaign.BigOrder::getBigOrderId, Function.identity()));

        bigOrder37 = bigOrderById.get(bigOrderOne.getId());
        bigOrder38 = bigOrderById.get(bigOrderTwo.getId());
        bigOrder39 = bigOrderById.get(bigOrderThree.getId());

        dispenserProjectWithVs = projectDao.createIfAbsent(Project.withKey("dispenser2")
                .abcServiceId(1357)
                .name("Dispenser")
                .description("Dispenser")
                .parent(abcProject)
                .valueStreamAbcServiceId(24483L)
                .build());

        final Person bc = personDao.tryReadPersonByLogin(BINARY_CAT.getLogin()).get();
        final Person lt = personDao.tryReadPersonByLogin(LOTREK.getLogin()).get();
        final Person sl = personDao.tryReadPersonByLogin(SLONNN.getLogin()).get();

        projectDao.attach(bc, abcdProject, Role.STEWARD);

        projectDao.attach(lt, abcdProject, Role.VS_LEADER);
        projectDao.attach(sl, abcdProject, Role.RESPONSIBLE);

        final Person ws = personDao.tryReadPersonByLogin(WHISTLER.getLogin()).get();
        projectDao.attach(ws, dispenserProjectWithVs, Role.RESPONSIBLE);

        updateHierarchy();
    }

    @Override
    @BeforeEach
    public void setUp() {
        quotaChangeRequestDao.clear();
    }

    private QuotaChangeRequest.Builder defaultBuilder() {
        final Person person = hierarchy.get().getPersonReader().read(AMOSOV_F.getLogin());
        return new QuotaChangeRequest.Builder()
                .project(dispenserProject)
                .campaign(QuotaChangeRequest.Campaign.from(campaign))
                .campaignType(campaign.getType())
                .author(person)
                .description("Default")
                .summary("Test")
                .comment("Default")
                .calculations("Default")
                .created(0)
                .updated(0)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                .chartLinks(Collections.emptyList())
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                .cost(0)
                .requestOwningCost(0L);
    }

    private List<Map<ResourcePreorderSummaryReport.Column, String>> readReport(final WebClient client) {
        final Response response = client
                .replaceHeader(HttpHeaders.ACCEPT, ServiceBase.TEXT_CSV_UTF_8)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.valueOf("text/csv;charset=utf-8"), response.getMediaType());
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));

        final Map<String, ResourcePreorderSummaryReport.Column> columnByTitle = Arrays.stream(ResourcePreorderSummaryReport.Column.values())
                .collect(Collectors.toMap(ResourcePreorderSummaryReport.Column::getTitle, Function.identity()));

        final String entity = response.readEntity(String.class);
        final CSVReader reader = new CSVReader(new StringReader(entity));
        final List<String[]> rows;
        try {
            rows = reader.readAll();
        } catch (IOException e) {
            return fail("Can't read report");
        }
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        final List<ResourcePreorderSummaryReport.Column> headers = Arrays.stream(rows.get(0))
                .map(columnByTitle::get)
                .collect(Collectors.toList());

        return rows.stream()
                .skip(1)
                .map(r -> {
                    final Map<ResourcePreorderSummaryReport.Column, String> result = new HashMap<>();

                    for (int i = 0; i < headers.size(); i++) {
                        if (StringUtils.isNoneEmpty(r[i])) {
                            result.put(headers.get(i), r[i]);
                        }
                    }

                    return result;
                })
                .collect(Collectors.toList());
    }

    @Test
    public void vsFieldsShouldBeInReport() {

        final QuotaChangeRequest request = quotaChangeRequestDao.create(defaultBuilder()
                .project(dispenserProjectWithVs)
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(bigOrder37)
                                .resource(ytCpu)
                                .segments(Collections.emptySet())
                                .amount(10_000).build()
                ))
                .build());

        List<Map<ResourcePreorderSummaryReport.Column, String>> rows = readReport(createAuthorizedLocalClient(KEYD)
                .path("/v1/resource-preorder-summary/export/csv")
        );
        assertEquals(1, rows.size());

        final Map<ResourcePreorderSummaryReport.Column, String> row = rows.get(0);

        assertEqualsRow(row, ImmutableMap.<ResourcePreorderSummaryReport.Column, String>builder()
                .put(ResourcePreorderSummaryReport.Column.STATUS, "CONFIRMED")
                .put(ResourcePreorderSummaryReport.Column.SUMMARY, "Test")
                .put(ResourcePreorderSummaryReport.Column.ID, request.getId() + "-yt-3019-11-30-by-3019-11-30-")
                .put(ResourcePreorderSummaryReport.Column.RESPONSIBLES, WHISTLER.getLogin())
                .put(ResourcePreorderSummaryReport.Column.CAMPAIGN, "test-campaign")
                .put(ResourcePreorderSummaryReport.Column.BIG_ORDER, "3019-11-30")
                .put(ResourcePreorderSummaryReport.Column.JUSTIFICATION, "GROWTH")
                .put(ResourcePreorderSummaryReport.Column.PROVIDER, "YT")
                .put(ResourcePreorderSummaryReport.Column.ABC_SERVICE, "Dispenser")
                .put(ResourcePreorderSummaryReport.Column.HEAD_DEPARTMENT_1, "Search Portal")
                .put(ResourcePreorderSummaryReport.Column.HEAD_1, KEYD.getLogin())
                .put(ResourcePreorderSummaryReport.Column.HEAD_DEPARTMENT_2, "Tools + b2b")
                .put(ResourcePreorderSummaryReport.Column.HEAD_DEPARTMENT_3, "ABC")

                .put(ResourcePreorderSummaryReport.Column.SERVERS_TOTAL, "0")
                .put(ResourcePreorderSummaryReport.Column.TOTAL_PRICE, "0")
                .put(ResourcePreorderSummaryReport.Column.D_SERVERS_SUM_COMPUTE, "0")
                .put(ResourcePreorderSummaryReport.Column.D_SERVERS_SUM_GPU, "0")
                .put(ResourcePreorderSummaryReport.Column.PS_D_SERVERS_SUM_COMPUTE, "0")
                .put(ResourcePreorderSummaryReport.Column.PS_D_SERVERS_SUM_GPU, "0")
                .put(ResourcePreorderSummaryReport.Column.SERVERS_TOTAL_GROWTH, "0")
                .put(ResourcePreorderSummaryReport.Column.TOTAL_PRICE_GROWTH, "0")
                .put(ResourcePreorderSummaryReport.Column.UPGRADES_COUNT, "0")
                .put(ResourcePreorderSummaryReport.Column.HW_UPGRADES_PRICE, "0")

                .put(ResourcePreorderSummaryReport.Column.VALUE_STREAM, "ABCD")
                .put(ResourcePreorderSummaryReport.Column.VS_CAPACITY_PLANNER, "slonnn")
                .put(ResourcePreorderSummaryReport.Column.VS_LIDER, "lotrek")
                .put(ResourcePreorderSummaryReport.Column.VS_MANAGER, "binarycat")
                .put(ResourcePreorderSummaryReport.Column.DELIVERY_DATE, "3019-11-30")
                .build());

        dispenserProject = Project.copyOf(dispenserProject)
                .valueStreamAbcServiceId(null)
                .build();
        projectDao.update(dispenserProject);
    }

    @Test
    public void onlyProcessResponsibleMustViewReport() {
        assertThrowsForbiddenWithMessage(() -> {
            readReport(createAuthorizedLocalClient(AMOSOV_F)
                    .path("/v1/resource-preorder-summary/export/csv")
            );
        }, "Only process responsible can view report");
        readReport(createAuthorizedLocalClient(KEYD)
                .path("/v1/resource-preorder-summary/export/csv")
        );
    }

    @Test
    public void reportShouldReturn404IfNoCampaignGroupActive() {
        botCampaignGroupDao.update(botCampaignGroup.copyBuilder()
                .setActive(false)
                .build()
        );
        assertThrowsWithMessage(() -> {
            readReport(createAuthorizedLocalClient(KEYD)
                    .path("/v1/resource-preorder-summary/export/csv")
            );
        }, HttpStatus.SC_NOT_FOUND, "No active campaign group");

        botCampaignGroupDao.update(botCampaignGroup);
    }

    @Test
    public void requestWithoutPreOrderShouldBeInReport() {
        final QuotaChangeRequest request = quotaChangeRequestDao.create(defaultBuilder()
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(bigOrder37)
                                .resource(ytCpu)
                                .segments(Collections.emptySet())
                                .amount(10_000).build()
                ))
                .build());

        List<Map<ResourcePreorderSummaryReport.Column, String>> rows = readReport(createAuthorizedLocalClient(KEYD)
                .path("/v1/resource-preorder-summary/export/csv")
        );
        assertEquals(1, rows.size());

        final Map<ResourcePreorderSummaryReport.Column, String> row = rows.get(0);

        assertEqualsRow(row, ImmutableMap.<ResourcePreorderSummaryReport.Column, String>builder()
                .put(ResourcePreorderSummaryReport.Column.STATUS, "CONFIRMED")
                .put(ResourcePreorderSummaryReport.Column.SUMMARY, "Test")
                .put(ResourcePreorderSummaryReport.Column.ID, request.getId() + "-yt-3019-11-30-by-3019-11-30-")
                .put(ResourcePreorderSummaryReport.Column.RESPONSIBLES, SLONNN.getLogin())
                .put(ResourcePreorderSummaryReport.Column.CAMPAIGN, "test-campaign")
                .put(ResourcePreorderSummaryReport.Column.BIG_ORDER, "3019-11-30")
                .put(ResourcePreorderSummaryReport.Column.JUSTIFICATION, "GROWTH")
                .put(ResourcePreorderSummaryReport.Column.PROVIDER, "YT")
                .put(ResourcePreorderSummaryReport.Column.ABC_SERVICE, "Dispenser")
                .put(ResourcePreorderSummaryReport.Column.HEAD_DEPARTMENT_1, "Search Portal")
                .put(ResourcePreorderSummaryReport.Column.HEAD_1, KEYD.getLogin())
                .put(ResourcePreorderSummaryReport.Column.HEAD_DEPARTMENT_2, "Tools + b2b")
                .put(ResourcePreorderSummaryReport.Column.HEAD_DEPARTMENT_3, "ABC")

                .put(ResourcePreorderSummaryReport.Column.SERVERS_TOTAL, "0")
                .put(ResourcePreorderSummaryReport.Column.TOTAL_PRICE, "0")
                .put(ResourcePreorderSummaryReport.Column.D_SERVERS_SUM_COMPUTE, "0")
                .put(ResourcePreorderSummaryReport.Column.D_SERVERS_SUM_GPU, "0")
                .put(ResourcePreorderSummaryReport.Column.PS_D_SERVERS_SUM_COMPUTE, "0")
                .put(ResourcePreorderSummaryReport.Column.PS_D_SERVERS_SUM_GPU, "0")
                .put(ResourcePreorderSummaryReport.Column.SERVERS_TOTAL_GROWTH, "0")
                .put(ResourcePreorderSummaryReport.Column.TOTAL_PRICE_GROWTH, "0")
                .put(ResourcePreorderSummaryReport.Column.DELIVERY_DATE, "3019-11-30")
                .put(ResourcePreorderSummaryReport.Column.UPGRADES_COUNT, "0")
                .put(ResourcePreorderSummaryReport.Column.HW_UPGRADES_PRICE, "0")
                .build());
    }

    public static void assertEqualsRow(Map<ResourcePreorderSummaryReport.Column, String> expected, Map<ResourcePreorderSummaryReport.Column, String> actual) {
        final Set<Map.Entry<ResourcePreorderSummaryReport.Column, String>> expectedEntries = expected.entrySet();
        final Set<Map.Entry<ResourcePreorderSummaryReport.Column, String>> actualEntries = actual.entrySet();
        final Sets.SetView<Map.Entry<ResourcePreorderSummaryReport.Column, String>> missingEntries = Sets.difference(expectedEntries, actualEntries);
        final Sets.SetView<Map.Entry<ResourcePreorderSummaryReport.Column, String>> newEntries = Sets.difference(actualEntries, expectedEntries);
        assertEquals(missingEntries, newEntries);
    }

}
