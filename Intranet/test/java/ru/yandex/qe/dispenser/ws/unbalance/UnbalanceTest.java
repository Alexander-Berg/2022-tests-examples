package ru.yandex.qe.dispenser.ws.unbalance;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.api.v1.request.unbalance.DiQuotaChangeRequestUnbalancedContext;
import ru.yandex.qe.dispenser.api.v1.request.unbalance.DiQuotaChangeRequestUnbalancedResult;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.domain.BotCampaignGroup;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest;
import ru.yandex.qe.dispenser.ws.quota.request.unbalanced.QuotaChangeUnbalanceRefreshManager;

import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignGroupBuilder;

/**
 * Unbalance calculation tests.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
public class UnbalanceTest extends BaseQuotaRequestTest {
    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;
    @Autowired
    private SegmentationDao segmentationDao;
    @Autowired
    private ResourceSegmentationDao resourceSegmentationDao;
    @Autowired
    private QuotaChangeRequestDao quotaChangeRequestDao;
    @Autowired
    private QuotaChangeUnbalanceRefreshManager quotaChangeUnbalanceRefreshManager;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private SegmentDao segmentDao;

    private final String locationSegmentationKey;
    private Campaign campaign;
    private BotCampaignGroup botCampaignGroup;

    public UnbalanceTest(@Value("${dispenser.location.segmentation.key}") final String locationSegmentationKey) {
        this.locationSegmentationKey = locationSegmentationKey;
    }

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        LocalDate date = LocalDate.of(2021, Month.AUGUST, 1);
        LocalDate date2 = LocalDate.of(2021, Month.SEPTEMBER, 1);
        List<Campaign.BigOrder> bigOrders = new ArrayList<>();
        bigOrders.add(new Campaign.BigOrder(bigOrderOne.getId(), date));
        bigOrders.add(new Campaign.BigOrder(bigOrderTwo.getId(), date2));
        campaign = campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2021")
                .setName("aug2021")
                .setId(143L)
                .setStartDate(date)
                .setBigOrders(bigOrders)
                .build());

        botCampaignGroup = botCampaignGroupDao.create(
                defaultCampaignGroupBuilder()
                        .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrderOne.getId())))
                        .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrderTwo.getId())))
                        .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values().iterator().next())
                        .build()
        );
        newCampaignId = campaign.getId();
    }

    @Test
    public void YPUnbalanceFormulaTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String SSD = "ssd_segmented";
        String HDD = "hdd_segmented";
        String CPU = "cpu_segmented";
        String RAM = "ram_segmented";
        String GPU = "gpu_segmented";
        String IO_HDD = "io_hdd";
        String IO_SSD = "io_ssd";

        Service service = Hierarchy.get().getServiceReader().read(YP);

        Resource ssd =
                resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());
        Resource hdd =
                resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        Resource cpu =
                resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        Resource ram =
                resourceDao.create(new Resource.Builder(RAM, service).name(RAM).type(DiResourceType.MEMORY).build());
        Resource gpu =
                resourceDao.create(new Resource.Builder(GPU, service).name(GPU).type(DiResourceType.ENUMERABLE).build());
        Resource ioHdd =
                resourceDao.create(new Resource.Builder(IO_HDD, service).name(IO_HDD).type(DiResourceType.BINARY_TRAFFIC).build());
        Resource ioSsd =
                resourceDao.create(new Resource.Builder(IO_SSD, service).name(IO_SSD).type(DiResourceType.BINARY_TRAFFIC).build());

        Segmentation location = segmentationDao.read(new Segmentation.Key(locationSegmentationKey));
        Segmentation segment = segmentationDao.create(new Segmentation.Builder("yp_segment")
                .name("yp_segment")
                .description("yp_segment")
                .priority(0)
                .build());

        updateHierarchy();

        Segment def = segmentDao.create(new Segment.Builder("default", segment).priority((short) 1).name("default").description("default").build());
        Segment dev = segmentDao.create(new Segment.Builder("dev", segment).priority((short) 1).name("dev").description("dev").build());

        Segment vla = segmentDao.create(new Segment.Builder("VLA", location).priority((short) 1).name("VLA").description("abc").build());
        Segment sas = segmentDao.create(new Segment.Builder("SAS", location).priority((short) 1).name("SAS").description("abc").build());
        Segment iva = segmentDao.create(new Segment.Builder("IVA", location).priority((short) 1).name("IVA").description("abc").build());
        Segment myt = segmentDao.create(new Segment.Builder("MYT", location).priority((short) 1).name("MYT").description("abc").build());
        Segment man = segmentDao.create(new Segment.Builder("MAN", location).priority((short) 1).name("MAN").description("abc").build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(ssd, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(hdd, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(cpu, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(ram, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(gpu, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(ioHdd, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(ioSsd, Set.of(location, segment));

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertFalse(fetchedRequest.isUnbalanced());

        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(6, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertFalse(fetchedRequest.isUnbalanced());

        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(6L, DiUnit.COUNT))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertTrue(fetchedRequest.isUnbalanced());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests2.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertTrue(fetchedRequest.isUnbalanced());

        dispenser().quotaChangeRequests()
                .byId(quotaRequests2.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(200L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(40L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(6, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(20, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(40, DiUnit.MIBPS))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests2.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertFalse(fetchedRequest.isUnbalanced());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests3 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests3.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertFalse(fetchedRequest.isUnbalanced());

        dispenser().quotaChangeRequests()
                .byId(quotaRequests3.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(200L, DiUnit.CORES))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests3.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertFalse(fetchedRequest.isUnbalanced());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests4 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(8L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(93L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(170L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(50L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests4.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertFalse(fetchedRequest.isUnbalanced());

        dispenser().quotaChangeRequests()
                .byId(quotaRequests4.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(9L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(93L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(170L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(50L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests4.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertTrue(fetchedRequest.isUnbalanced());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests5 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests5.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertFalse(fetchedRequest.isUnbalanced());

        dispenser().quotaChangeRequests()
                .byId(quotaRequests5.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(5L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests5.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertTrue(fetchedRequest.isUnbalanced());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests6 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests6.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertFalse(fetchedRequest.isUnbalanced());

        dispenser().quotaChangeRequests()
                .byId(quotaRequests6.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(5L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests6.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertTrue(fetchedRequest.isUnbalanced());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests7 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests7.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertFalse(fetchedRequest.isUnbalanced());

        dispenser().quotaChangeRequests()
                .byId(quotaRequests7.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(5L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests7.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertTrue(fetchedRequest.isUnbalanced());
    }

    @Test
    public void unbalanceRefreshTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String SSD = "ssd_segmented";
        String HDD = "hdd_segmented";
        String CPU = "cpu_segmented";
        String RAM = "ram_segmented";
        String GPU = "gpu_segmented";
        String IO_HDD = "io_hdd";
        String IO_SSD = "io_ssd";

        Service service = Hierarchy.get().getServiceReader().read(YP);

        Resource ssd =
                resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());
        Resource hdd =
                resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        Resource cpu =
                resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        Resource ram =
                resourceDao.create(new Resource.Builder(RAM, service).name(RAM).type(DiResourceType.MEMORY).build());
        Resource gpu =
                resourceDao.create(new Resource.Builder(GPU, service).name(GPU).type(DiResourceType.ENUMERABLE).build());
        Resource ioHdd =
                resourceDao.create(new Resource.Builder(IO_HDD, service).name(IO_HDD).type(DiResourceType.BINARY_TRAFFIC).build());
        Resource ioSsd =
                resourceDao.create(new Resource.Builder(IO_SSD, service).name(IO_SSD).type(DiResourceType.BINARY_TRAFFIC).build());

        Segmentation location = segmentationDao.read(new Segmentation.Key(locationSegmentationKey));
        Segmentation segment = segmentationDao.create(new Segmentation.Builder("yp_segment")
                .name("yp_segment")
                .description("yp_segment")
                .priority(0)
                .build());

        updateHierarchy();

        Segment def = segmentDao.create(new Segment.Builder("default", segment).priority((short) 1).name("default").description("default").build());
        Segment dev = segmentDao.create(new Segment.Builder("dev", segment).priority((short) 1).name("dev").description("dev").build());

        Segment vla = segmentDao.create(new Segment.Builder("VLA", location).priority((short) 1).name("VLA").description("abc").build());
        Segment sas = segmentDao.create(new Segment.Builder("SAS", location).priority((short) 1).name("SAS").description("abc").build());
        Segment iva = segmentDao.create(new Segment.Builder("IVA", location).priority((short) 1).name("IVA").description("abc").build());
        Segment myt = segmentDao.create(new Segment.Builder("MYT", location).priority((short) 1).name("MYT").description("abc").build());
        Segment man = segmentDao.create(new Segment.Builder("MAN", location).priority((short) 1).name("MAN").description("abc").build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(ssd, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(hdd, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(cpu, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(ram, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(gpu, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(ioHdd, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(ioSsd, Set.of(location, segment));

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(5L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        long id = quotaRequests.getFirst().getId();
        DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + id)
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertTrue(fetchedRequest.isUnbalanced());

        quotaChangeRequestDao.updateUnbalanced(Map.of(id, false));

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + id)
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertFalse(fetchedRequest.isUnbalanced());

        quotaChangeUnbalanceRefreshManager.refresh();

        updateHierarchy();

        fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + id)
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertTrue(fetchedRequest.isUnbalanced());
    }

    @Test
    public void quotaRequestWithWrongResourceAmountNotFailUnbalanceCalculationTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String SSD = "ssd_segmented";
        String HDD = "hdd_segmented";
        String CPU = "cpu_segmented";
        String RAM = "ram_segmented";
        String GPU = "gpu_segmented";
        String IO_HDD = "io_hdd";
        String IO_SSD = "io_ssd";

        Service service = Hierarchy.get().getServiceReader().read(YP);

        Resource ssd =
                resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());
        Resource hdd =
                resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        Resource cpu =
                resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        Resource ram =
                resourceDao.create(new Resource.Builder(RAM, service).name(RAM).type(DiResourceType.MEMORY).build());
        Resource gpu =
                resourceDao.create(new Resource.Builder(GPU, service).name(GPU).type(DiResourceType.ENUMERABLE).build());
        Resource ioHdd =
                resourceDao.create(new Resource.Builder(IO_HDD, service).name(IO_HDD).type(DiResourceType.BINARY_TRAFFIC).build());
        Resource ioSsd =
                resourceDao.create(new Resource.Builder(IO_SSD, service).name(IO_SSD).type(DiResourceType.BINARY_TRAFFIC).build());

        Segmentation location = segmentationDao.read(new Segmentation.Key(locationSegmentationKey));
        Segmentation segment = segmentationDao.create(new Segmentation.Builder("yp_segment")
                .name("yp_segment")
                .description("yp_segment")
                .priority(0)
                .build());

        updateHierarchy();

        Segment def = segmentDao.create(new Segment.Builder("default", segment).priority((short) 1).name("default").description("default").build());
        Segment dev = segmentDao.create(new Segment.Builder("dev", segment).priority((short) 1).name("dev").description("dev").build());

        Segment vla = segmentDao.create(new Segment.Builder("VLA", location).priority((short) 1).name("VLA").description("abc").build());
        Segment sas = segmentDao.create(new Segment.Builder("SAS", location).priority((short) 1).name("SAS").description("abc").build());
        Segment iva = segmentDao.create(new Segment.Builder("IVA", location).priority((short) 1).name("IVA").description("abc").build());
        Segment myt = segmentDao.create(new Segment.Builder("MYT", location).priority((short) 1).name("MYT").description("abc").build());
        Segment man = segmentDao.create(new Segment.Builder("MAN", location).priority((short) 1).name("MAN").description("abc").build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(ssd, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(hdd, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(cpu, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(ram, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(gpu, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(ioHdd, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(ioSsd, Set.of(location, segment));

        updateHierarchy();

        prepareCampaignResources();

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10241L, DiUnit.MEBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        Assertions.assertFalse(fetchedRequest.isUnbalanced());
    }

    @Test
    public void unbalanceDryRunTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String SSD = "ssd_segmented";
        String HDD = "hdd_segmented";
        String CPU = "cpu_segmented";
        String RAM = "ram_segmented";
        String GPU = "gpu_segmented";
        String IO_HDD = "io_hdd";
        String IO_SSD = "io_ssd";

        Service service = Hierarchy.get().getServiceReader().read(YP);

        Resource ssd =
                resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());
        Resource hdd =
                resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        Resource cpu =
                resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        Resource ram =
                resourceDao.create(new Resource.Builder(RAM, service).name(RAM).type(DiResourceType.MEMORY).build());
        Resource gpu =
                resourceDao.create(new Resource.Builder(GPU, service).name(GPU).type(DiResourceType.ENUMERABLE).build());
        Resource ioHdd =
                resourceDao.create(new Resource.Builder(IO_HDD, service).name(IO_HDD).type(DiResourceType.BINARY_TRAFFIC).build());
        Resource ioSsd =
                resourceDao.create(new Resource.Builder(IO_SSD, service).name(IO_SSD).type(DiResourceType.BINARY_TRAFFIC).build());

        Segmentation location = segmentationDao.read(new Segmentation.Key(locationSegmentationKey));
        Segmentation segment = segmentationDao.create(new Segmentation.Builder("yp_segment")
                .name("yp_segment")
                .description("yp_segment")
                .priority(0)
                .build());

        updateHierarchy();

        Segment def = segmentDao.create(new Segment.Builder("default", segment).priority((short) 1).name("default").description("default").build());
        Segment dev = segmentDao.create(new Segment.Builder("dev", segment).priority((short) 1).name("dev").description("dev").build());

        Segment vla = segmentDao.create(new Segment.Builder("VLA", location).priority((short) 1).name("VLA").description("abc").build());
        Segment sas = segmentDao.create(new Segment.Builder("SAS", location).priority((short) 1).name("SAS").description("abc").build());
        Segment iva = segmentDao.create(new Segment.Builder("IVA", location).priority((short) 1).name("IVA").description("abc").build());
        Segment myt = segmentDao.create(new Segment.Builder("MYT", location).priority((short) 1).name("MYT").description("abc").build());
        Segment man = segmentDao.create(new Segment.Builder("MAN", location).priority((short) 1).name("MAN").description("abc").build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(ssd, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(hdd, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(cpu, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(ram, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(gpu, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(ioHdd, Set.of(location, segment));
        resourceSegmentationDao.setSegmentations(ioSsd, Set.of(location, segment));

        updateHierarchy();

        prepareCampaignResources();

        BodyUpdate.BodyUpdateBuilder bodyUpdateBuilder = new BodyUpdate.BodyUpdateBuilder();

        DiQuotaChangeRequestUnbalancedContext body = DiQuotaChangeRequestUnbalancedContext.builder()
                .campaignId(143L)
                .providerKey("yp")
                .changes(bodyUpdateBuilder.changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .build().getChanges())
                .build();
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/unbalance/dryRun")
                .invoke(HttpMethod.PUT, body);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatus());
        DiQuotaChangeRequestUnbalancedResult diQuotaChangeRequestUnbalancedResult =
                response.readEntity(DiQuotaChangeRequestUnbalancedResult.class);
        Assertions.assertNotNull(diQuotaChangeRequestUnbalancedResult);
        Assertions.assertEquals("yp", diQuotaChangeRequestUnbalancedResult.getProviderKey());
        Assertions.assertFalse(diQuotaChangeRequestUnbalancedResult.isUnbalanced());
        Assertions.assertEquals(24, diQuotaChangeRequestUnbalancedResult.getChanges().size());

        Map<DiQuotaChangeRequestUnbalancedResult.DiResourceKey, DiQuotaChangeRequestUnbalancedResult.DiChange> changeByKeyMap = diQuotaChangeRequestUnbalancedResult.getChanges().stream()
                .collect(Collectors.toMap(DiQuotaChangeRequestUnbalancedResult.DiChange::getResourceKey,
                        Function.identity()));

        // cpu

        DiQuotaChangeRequestUnbalancedResult.DiResourceKey key =
                new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(CPU, bigOrderOne.getId(),
                        Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                                .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(null).recommendedMin(DiAmount.of(2L, DiUnit.CORES)).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(CPU, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(null).recommendedMin(DiAmount.of(2L, DiUnit.CORES)).build(), changeByKeyMap.get(key));

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(CPU, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(null).recommendedMin(DiAmount.of(2L, DiUnit.CORES)).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(CPU, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(null).recommendedMin(DiAmount.of(2L, DiUnit.CORES)).build(), changeByKeyMap.get(key));

        // ram

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(RAM, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(8L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(RAM, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(8L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(RAM, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(8L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(RAM, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(8L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));

        // ssd

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(SSD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(93L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(SSD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(93L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(SSD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(93L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(SSD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(93L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));

        // hdd

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(HDD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(170L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(HDD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(170L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(HDD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(170L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(HDD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(170L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));

        // hdd io

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_HDD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(2L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_HDD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(2L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_HDD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(2L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_HDD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(2L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));

        // ssd io

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_SSD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(50L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_SSD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(50L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_SSD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(50L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_SSD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(50L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));

        bodyUpdateBuilder = new BodyUpdate.BodyUpdateBuilder();

        body = DiQuotaChangeRequestUnbalancedContext.builder()
                .campaignId(143L)
                .providerKey("yp")
                .changes(bodyUpdateBuilder.changes(YP, CPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderOne.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))

                        .changes(YP, CPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(2L, DiUnit.CORES))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(5L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(90L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(70L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), def.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(YP, RAM, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(4L, DiUnit.GIBIBYTE))
                        .changes(YP, SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(YP, GPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey(), dev.getPublicKey()), DiAmount.of(3L, DiUnit.COUNT))
                        .build().getChanges())
                .build();
        response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/unbalance/dryRun")
                .invoke(HttpMethod.PUT, body);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatus());
        diQuotaChangeRequestUnbalancedResult =
                response.readEntity(DiQuotaChangeRequestUnbalancedResult.class);
        Assertions.assertNotNull(diQuotaChangeRequestUnbalancedResult);
        Assertions.assertEquals("yp", diQuotaChangeRequestUnbalancedResult.getProviderKey());
        Assertions.assertTrue(diQuotaChangeRequestUnbalancedResult.isUnbalanced());
        Assertions.assertEquals(24, diQuotaChangeRequestUnbalancedResult.getChanges().size());

        changeByKeyMap = diQuotaChangeRequestUnbalancedResult.getChanges().stream()
                .collect(Collectors.toMap(DiQuotaChangeRequestUnbalancedResult.DiChange::getResourceKey,
                        Function.identity()));

        // cpu

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(CPU, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(null).recommendedMin(DiAmount.of(2L, DiUnit.CORES)).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(CPU, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(null).recommendedMin(DiAmount.of(2L, DiUnit.CORES)).build(), changeByKeyMap.get(key));

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(CPU, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(null).recommendedMin(DiAmount.of(2L, DiUnit.CORES)).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(CPU, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(null).recommendedMin(DiAmount.of(3L, DiUnit.CORES)).build(), changeByKeyMap.get(key));

        // ram

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(RAM, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(8L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(RAM, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(8L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(RAM, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(8L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(RAM, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(8L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));

        // ssd

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(SSD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(93L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(SSD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(93L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(SSD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(93L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(SSD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(93L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));

        // hdd

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(HDD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(170L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(HDD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(170L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(HDD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(170L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(HDD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(170L, DiUnit.GIBIBYTE)).recommendedMin(null).build(), changeByKeyMap.get(key));

        // hdd io

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_HDD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(2L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_HDD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(2L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_HDD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(2L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_HDD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(2L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));

        // ssd io

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_SSD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(50L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_SSD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(vla.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(50L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));

        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_SSD, bigOrderOne.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(50L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));
        key = new DiQuotaChangeRequestUnbalancedResult.DiResourceKey(IO_SSD, bigOrderTwo.getId(),
                Set.of(DiQuotaChangeRequestUnbalancedResult.DiSegmentKey.builder()
                        .segmentKey(sas.getPublicKey()).segmentationKey(locationSegmentationKey).build()));
        Assertions.assertEquals(DiQuotaChangeRequestUnbalancedResult.DiChange.builder()
                .resourceKey(key).recommendedMax(DiAmount.of(50L, DiUnit.MIBPS)).recommendedMin(null).build(), changeByKeyMap.get(key));
    }
}
