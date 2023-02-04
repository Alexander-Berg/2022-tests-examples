package ru.yandex.qe.dispenser.ws.logic;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
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
import com.google.common.collect.ImmutableSet;
import com.opencsv.CSVReader;
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
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.BotPreOrderDao;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.dispenser_admins.DispenserAdminsDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;
import ru.yandex.qe.dispenser.ws.ServiceBase;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.quota.request.RequestPreOrderAggregationReport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class ResourcePreOrderExportTest extends AcceptanceTestBase {

    public static final String SERVICE_KEY = "TEST_SERVICE";

    @Autowired
    private BigOrderManager bigOrderManager;

    @Autowired
    private QuotaChangeRequestDao quotaChangeRequestDao;

    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private SegmentationDao segmentationDao;

    @Autowired
    private ResourceSegmentationDao resourceSegmentationDao;

    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;
    @Autowired
    private BotPreOrderDao botPreOrderDao;

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

    private Segment dc1;
    private Segment dc2;
    private Segment dc3;
    private Resource cpu;
    private Resource ram;
    private Service service;
    private Campaign campaign;
    private Project project1;
    private Project project2;
    private Project project3;
    private Campaign.BigOrder bigOrder37;
    private Campaign.BigOrder bigOrder38;
    private Campaign.BigOrder bigOrder39;
    private BotCampaignGroup botCampaignGroup;

    @BeforeAll
    public void beforeClass() {
        reinitialize();

        final Project yandex = projectDao.createIfAbsent(Project.withKey(YANDEX).name("Yandex").build());
        project1 = projectDao.createIfAbsent(Project.withKey("project1").name("Project1").description("Project1").parent(yandex).build());
        project2 = projectDao.createIfAbsent(Project.withKey("project2").name("Project2").description("Project2").parent(project1).build());
        project3 = projectDao.createIfAbsent(Project.withKey("project3").name("Project3").description("Project3").parent(yandex).build());
        updateHierarchy();

        bigOrderManager.clear();

        BigOrder bigOderOne = bigOrderManager.create(BigOrder.builder(LocalDate.of(3019, 11, 30)));
        BigOrder bigOrderTwo = bigOrderManager.create(BigOrder.builder(LocalDate.of(3019, 12, 30)));
        BigOrder bigOrderThree = bigOrderManager.create(BigOrder.builder(LocalDate.of(3019, 12, 31)));

        segmentationDao.clear();
        segmentDao.clear();
        final Segmentation dcSegmentation = segmentationDao.create(new Segmentation.Builder(DC_SEGMENTATION)
                .name("Data Centers")
                .description("Data centers segmentation")
                .build());
        updateHierarchy();

        dc1 = segmentDao.create(new Segment.Builder(DC_SEGMENT_1, dcSegmentation)
                .name("LOCATION_1")
                .description("Data center in location 1")
                .priority((short) 1)
                .build());

        dc2 = segmentDao.create(new Segment.Builder(DC_SEGMENT_2, dcSegmentation)
                .name("LOCATION_2")
                .description("Data center in location 2")
                .priority((short) 1)
                .build());

        dc3 = segmentDao.create(new Segment.Builder(DC_SEGMENT_3, dcSegmentation)
                .name("LOCATION_3")
                .description("Data center in location 3")
                .priority((short) 1)
                .build());

        final Person amosovf = personDao.createIfAbsent(new Person(AMOSOV_F.getLogin(), 1120000000022901L, false, false, false, PersonAffiliation.YANDEX));
        dispenserAdminsDao.setDispenserAdmins(Collections.singleton(amosovf));

        service = serviceDao.create(Service.withKey(SERVICE_KEY)
                .withName("Test service")
                .withAbcServiceId(1)
                .withSettings(Service.Settings.builder()
                        .accountActualValuesInQuotaDistribution(false)
                        .usesProjectHierarchy(true)
                        .manualQuotaAllocation(false)
                        .build())
                .build());

        updateHierarchy();

        cpu = resourceDao.create(new Resource.Builder(CPU, service)
                .name("CPU")
                .type(DiResourceType.ENUMERABLE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        ram = resourceDao.create(new Resource.Builder(RAM, service)
                .name("RAM")
                .type(DiResourceType.ENUMERABLE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        updateHierarchy();

        resourceSegmentationDao.createAll(ImmutableList.of(
                new ResourceSegmentation.Builder(cpu, dcSegmentation).build(),
                new ResourceSegmentation.Builder(ram, dcSegmentation).build()
        ));

        campaignDao.clear();
        campaign = campaignDao.create(defaultCampaignBuilder(bigOderOne)
                .setBigOrders(Arrays.asList(new Campaign.BigOrder(bigOderOne.getId(), LocalDate.now()),
                        new Campaign.BigOrder(bigOrderTwo.getId(), LocalDate.now().plusMonths(12)),
                        new Campaign.BigOrder(bigOrderThree.getId(), LocalDate.now().plusMonths(24))
                ))
                .build());

        final Set<BigOrder> simpleBigOrders = bigOrderManager.getByIds(ImmutableSet.of(bigOderOne.getId(),
                bigOrderTwo.getId(), bigOrderThree.getId()));

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

        bigOrder37 = bigOrderById.get(bigOderOne.getId());
        bigOrder38 = bigOrderById.get(bigOrderTwo.getId());
        bigOrder39 = bigOrderById.get(bigOrderThree.getId());
    }

    @Override
    @BeforeEach
    public void setUp() {
        quotaChangeRequestDao.clear();
    }

    private List<Map<String, String>> readReport(final WebClient client) throws IOException {
        final Response response = client
                .replaceHeader(HttpHeaders.ACCEPT, ServiceBase.TEXT_CSV_UTF_8)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.valueOf("text/csv;charset=utf-8"), response.getMediaType());
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));

        final String entity = response.readEntity(String.class);
        final CSVReader reader = new CSVReader(new StringReader(entity));
        final List<String[]> rows = reader.readAll();
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        final String[] headers = rows.get(0);

        return rows.stream()
                .skip(1)
                .map(r -> {
                    final Map<String, String> result = new HashMap<>();

                    for (int i = 0; i < headers.length; i++) {
                        result.put(headers[i], r[i]);
                    }

                    return result;
                })
                .collect(Collectors.toList());
    }

    protected QuotaChangeRequest.Builder defaultBuilder() {
        final Person person = hierarchy.get().getPersonReader().read(AMOSOV_F.getLogin());
        return new QuotaChangeRequest.Builder()
                .project(project1)
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

    @Test
    public void moneyReportShouldWork() throws IOException {
        quotaChangeRequestDao.create(defaultBuilder()
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(bigOrder37)
                                .resource(cpu)
                                .segments(Collections.singleton(dc1))
                                .amount(10_000).build()
                ))
                .build()
        );

        List<Map<String, String>> rows = readReport(createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/export/csv")
                .query("pivot", RequestPreOrderAggregationReport.Pivot.COST)
        );
        assertEquals(1, rows.size());

        rows = readReport(createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/export/csv")
                .query("pivot", RequestPreOrderAggregationReport.Pivot.COST)
        );
        assertEquals(1, rows.size());
    }

    @Test
    public void serversPivotShouldWorkToo() throws IOException {

        quotaChangeRequestDao.create(defaultBuilder()
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(bigOrder37)
                                .resource(cpu)
                                .segments(Collections.singleton(dc1))
                                .amount(20_000).build(),
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(bigOrder38)
                                .resource(cpu)
                                .segments(Collections.singleton(dc2))
                                .amount(30_000).build()
                ))
                .build()
        );

        quotaChangeRequestDao.create(defaultBuilder()
                .project(project2)
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(bigOrder37)
                                .resource(cpu)
                                .segments(Collections.singleton(dc1))
                                .amount(10_000).build()
                ))
                .build()
        );

        quotaChangeRequestDao.create(defaultBuilder()
                .project(project3)
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(bigOrder37)
                                .resource(cpu)
                                .segments(Collections.singleton(dc1))
                                .amount(50_000).build(),
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(bigOrder37)
                                .resource(cpu)
                                .segments(Collections.singleton(dc2))
                                .amount(10_000).build()
                ))
                .build()
        );

        final List<Map<String, String>> rows = readReport(createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/export/csv")
                .query("pivot", RequestPreOrderAggregationReport.Pivot.SERVERS)
        );

        assertEquals(1, rows.size());
    }

}
