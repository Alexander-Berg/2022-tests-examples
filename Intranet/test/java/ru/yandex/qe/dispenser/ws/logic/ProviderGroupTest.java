package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.domain.BotCampaignGroup;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.CampaignUpdate;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.ProviderGroup;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.bot.BigOrderConfig;
import ru.yandex.qe.dispenser.domain.dao.bot.provider.group.ProviderGroupDao;
import ru.yandex.qe.dispenser.domain.dao.bot.provider.group.report.ProviderGroupReportDao;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.property.Property;
import ru.yandex.qe.dispenser.domain.property.PropertyManager;
import ru.yandex.qe.dispenser.ws.ProviderGroupReportService;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.quota.request.workflow.ResourceWorkflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest.TEST_BIG_ORDER_DATE;
import static ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class ProviderGroupTest extends BusinessLogicTestBase {

    @Autowired
    private ProviderGroupDao providerGroupDao;
    @Autowired
    private ProviderGroupReportDao providerGroupReportDao;
    @Autowired
    private BigOrderManager bigOrderManager;
    @Autowired
    private QuotaChangeRequestDao quotaChangeRequestDao;
    @Autowired
    private PropertyManager propertyManager;
    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;
    @Autowired
    private SegmentDao segmentDao;

    private long campaignId;
    private Campaign campaign;
    private BotCampaignGroup botCampaignGroup;
    private List<BigOrder> bigOrders;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        providerGroupDao.clear();
        providerGroupReportDao.clear();
        botCampaignGroupDao.clear();

        bigOrderManager.clear();
        bigOrders = ImmutableList.of(
                bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE).configs(ImmutableList.of(
                        new BigOrderConfig(1000L, "forecast",  DC_SEGMENT_1, "new"),
                        new BigOrderConfig(1001L, "forecast",  DC_SEGMENT_2,"new"),
                        new BigOrderConfig(1003L, "forecast",  DC_SEGMENT_3, "new")
                ))),
                bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE.plusMonths(3)).configs(ImmutableList.of(
                        new BigOrderConfig(2000L, "forecast",  DC_SEGMENT_1, "new"),
                        new BigOrderConfig(2001L, "forecast",  DC_SEGMENT_2, "new"),
                        new BigOrderConfig(2003L, "forecast",  DC_SEGMENT_3, "new")
                ))),
                bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE.plusMonths(6)).configs(ImmutableList.of(
                        new BigOrderConfig(3000L, "forecast",  DC_SEGMENT_1, "new"),
                        new BigOrderConfig(3001L, "forecast",  DC_SEGMENT_2, "new"),
                        new BigOrderConfig(3003L, "forecast",  DC_SEGMENT_3, "new")
                )))
        );

        campaignDao.clear();
        campaign = campaignDao.create(defaultCampaignBuilder(bigOrders.get(0))
                .setBigOrders(bigOrders.stream()
                        .map(Campaign.BigOrder::new)
                        .collect(Collectors.toList()))
                .build());
        campaignId = campaign.getId();

        final Map<Long, BigOrder> ordersById = bigOrders.stream().collect(Collectors.toMap(BigOrder::getId, Function.identity()));
        botCampaignGroup = botCampaignGroupDao.create(BotCampaignGroup.builder()
                .setKey("test_campaign_group")
                .setActive(true)
                .setName("Test Campaign Group")
                .setBotPreOrderIssueKey("DISPENSERREQ-1")
                .setBigOrders(bigOrders.stream()
                        .sorted(Comparator.comparing(BigOrder::getId))
                        .collect(Collectors.toList())
                )
                .addCampaign(campaign.forBot(ordersById))
                .build()
        );

        prepareCampaignResources();

        final Service service = serviceDao.read(YP);
        final Segment segment1 = segmentDao.read(DC_SEGMENT_1);
        final Segment segment2 = segmentDao.read(DC_SEGMENT_2);
        final Segment segment3 = segmentDao.read(DC_SEGMENT_3);

        propertyManager.setProperty(ResourceWorkflow.PROPERTY_RESOURCE_PREORDER_ENTITY_KEY,
                "mapping_" + service.getKey() + "_" + SEGMENT_CPU, Property.Type.STRING.createValue("cpu"));

        propertyManager.setProperty(ResourceWorkflow.PROPERTY_RESOURCE_PREORDER_ENTITY_KEY,
                "mapping_" + service.getKey() + "_" + SEGMENT_HDD, Property.Type.STRING.createValue("hdd"));

        final Service yt = serviceDao.create(Service.withKey("yt")
                .withSettings(Service.Settings.builder().build())
                .withPriority(9)
                .withName("yt")
                .withAbcServiceId(TEST_ABC_SERVICE_ID)
                .build());

        updateHierarchy();
    }

    @Test
    public void providerGroupCanBeCreated() {

        providerGroupDao.create(new ProviderGroup("YP, RTC, SaaS, Qloud", "Many providers", campaignId, 10, new ProviderGroup.ProviderGroupSettings(Collections.singletonList(2L), ImmutableList.of(
                new ProviderGroup.ResourceGroup("CPU", ImmutableList.of(11L, 22L)),
                new ProviderGroup.ResourceGroup("RAM", Collections.singletonList(10L))
        ))));


        final List<ProviderGroup> groups = providerGroupDao.getProviderGroupsByCampaignId(campaignId);
        assertEquals(1, groups.size());
        final ProviderGroup group = groups.iterator().next();
        assertEquals("YP, RTC, SaaS, Qloud", group.getName());
        assertEquals("Many providers", group.getDescription());
        assertEquals(10, group.getPriority());
        assertEquals(group.getCampaignId(), campaignId);
        assertEquals(Collections.singletonList(2L), group.getSettings().getServiceIds());
        assertEquals(2, group.getSettings().getResourceGroups().size());
        assertEquals("CPU", group.getSettings().getResourceGroups().iterator().next().getName());
        assertEquals(ImmutableList.of(11L, 22L), group.getSettings().getResourceGroups().iterator().next().getResourceIds());
    }

    @Test
    public void providerGroupReportShouldBeRequestedForAnyCampaign() {
        prepareRequest();

        campaignDao.create(defaultCampaignBuilder(bigOrders.get(0))
                .setKey("new-active-campaign")
                .setName("Test Two")
                .setBigOrders(Collections.emptyList())
                .setStartDate(TEST_BIG_ORDER_DATE.plusDays(1))
                .build());

        campaignDao.partialUpdate(campaign, CampaignUpdate.builder().setStatus(Campaign.Status.CLOSED).build());

        updateHierarchy();

        ProviderGroupReportService.FullReport fullReport = createAuthorizedLocalClient(SANCHO)
                .path("/v1/provider-group-report")
                .get(ProviderGroupReportService.FullReport.class);

        assertEquals(fullReport.getBigOrders().size(), 0);
        assertEquals(fullReport.getClouds().size(), 0);
    }

    private void prepareRequest() {
        final Service service = serviceDao.read(YP);

        providerGroupDao.create(new ProviderGroup("YP and Ko", "...", campaignId, 1, new ProviderGroup.ProviderGroupSettings(Collections.singletonList(service.getId()), ImmutableList.of(
                new ProviderGroup.ResourceGroup("CPU", Collections.singletonList(resourceDao.read(new Resource.Key(SEGMENT_CPU, service)).getId())),
                new ProviderGroup.ResourceGroup("HDD", Collections.singletonList(resourceDao.read(new Resource.Key(SEGMENT_HDD, service)).getId()))
        ))));

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .changes(YP, SEGMENT_CPU, bigOrders.get(1).getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(100, DiUnit.CORES))
                        .changes(YP, SEGMENT_HDD, bigOrders.get(1).getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(8000, DiUnit.GIBIBYTE))
                        .changes(YP, SEGMENT_CPU, bigOrders.get(0).getId(), ImmutableSet.of(DC_SEGMENT_3, SEGMENT_SEGMENT_1), DiAmount.of(300, DiUnit.CORES))
                        .changes(YP, SEGMENT_HDD, bigOrders.get(0).getId(), ImmutableSet.of(DC_SEGMENT_3, SEGMENT_SEGMENT_1), DiAmount.of(4000, DiUnit.GIBIBYTE))

                        .build(), null)
                .performBy(AMOSOV_F);
    }

    @Test
    public void onlyAllowedPersonsCanViewProviderGroupReport() {
        prepareRequest();

        assertThrowsForbiddenWithMessage(() -> createAuthorizedLocalClient(AQRU) // random member
                .path("/v1/provider-group-report")
                .get(ProviderGroupReportService.FullReport.class));

        createAuthorizedLocalClient(AMOSOV_F) // Dispenser admin
                .path("/v1/provider-group-report")
                .get(ProviderGroupReportService.FullReport.class);

        createAuthorizedLocalClient(SANCHO) // provider admin
                .path("/v1/provider-group-report")
                .get(ProviderGroupReportService.FullReport.class);

        createAuthorizedLocalClient(KEYD) // PROCESS_RESPONSIBLE
                .path("/v1/provider-group-report")
                .get(ProviderGroupReportService.FullReport.class);

        createAuthorizedLocalClient(WHISTLER) // CAPACITY_PLANNER
                .path("/v1/provider-group-report")
                .get(ProviderGroupReportService.FullReport.class);
    }
}
