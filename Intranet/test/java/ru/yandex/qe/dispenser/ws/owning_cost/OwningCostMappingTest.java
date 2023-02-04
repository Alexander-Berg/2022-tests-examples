package ru.yandex.qe.dispenser.ws.owning_cost;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.domain.BotCampaignGroup;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.standalone.MockMdsConfigApi;
import ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest;
import ru.yandex.qe.dispenser.ws.quota.request.owning_cost.QuotaChangeOwningCostRefreshManager;

import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignGroupBuilder;
import static ru.yandex.qe.dispenser.ws.quota.request.owning_cost.formula.ProviderOwningCostFormula.DEFAULT_OWNING_COST;
import static ru.yandex.qe.dispenser.ws.quota.request.owning_cost.formula.ProviderOwningCostFormula.MATH_CONTEXT;

/**
 * Owning cost mapping tests.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
public class OwningCostMappingTest extends BaseQuotaRequestTest {

    @Autowired
    QuotaChangeOwningCostRefreshManager quotaChangeOwningCostRefreshManager;
    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private SegmentationDao segmentationDao;
    @Autowired
    private SegmentDao segmentDao;
    @Autowired
    private ServiceDao serviceDao;
    @Autowired
    private ResourceSegmentationDao resourceSegmentationDao;
    @Autowired
    private MockMdsConfigApi mockMdsConfigApi;
    @Autowired
    private PersonDao personDao;

    private final String ytClusterSegmentationKey;
    private final String locationSegmentationKey;
    private final String mdbDbSegmentationKey;
    private final String distbuildSegmentationKey;

    private static final String MDS_STORAGE = "mds";
    private static final String AVATARS = "avatars";
    private static final String S3_API = "s3-api";

    public OwningCostMappingTest(@Value("${dispenser.location.segmentation.key}") final String locationSegmentationKey,
                                 @Value("${dispenser.yt.segmentation.key}") String ytClusterSegmentationKey,
                                 @Value("${dispenser.mdb.segmentation.key}") final String mdbDbSegmentationKey,
                                 @Value("${dispenser.distbuild.segmentation.key}") final String distbuildSegmentationKey) {
        this.ytClusterSegmentationKey = ytClusterSegmentationKey;
        this.locationSegmentationKey = locationSegmentationKey;
        this.mdbDbSegmentationKey = mdbDbSegmentationKey;
        this.distbuildSegmentationKey = distbuildSegmentationKey;
    }

    private Campaign campaign;
    private BotCampaignGroup botCampaignGroup;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        LocalDate date = LocalDate.of(2021, Month.AUGUST, 1);
        campaign = campaignDao.insertForTest(defaultCampaignBuilder(bigOrderTwo)
                .setKey("aug2021")
                .setName("aug2021")
                .setId(143L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderTwo.getId(), date)))
                .build());

        botCampaignGroup = botCampaignGroupDao.create(
                defaultCampaignGroupBuilder()
                        .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrderTwo.getId())))
                        .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values().iterator().next())
                        .build()
        );
        newCampaignId = campaign.getId();

        mockMdsConfigApi.setPassThrough(true);
        mockMdsConfigApi.setFail(false);
    }

    @AfterEach
    public void end() {
        mockMdsConfigApi.setFail(false);
    }

    @Test
    public void YPOwningCostFormulaTest() {
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
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(7, changes.size());
        Map<String, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        extracted(SSD, HDD, CPU, RAM, GPU, IO_HDD, IO_SSD, owningCostByResourceKey);
        Assertions.assertEquals(owningCostByResourceKey.values().stream()
                .map(BigInteger::new)
                .reduce(BigInteger::add)
                .map(BigInteger::toString)
                .orElse("0"), fetchedRequest.getRequestOwningCost());

        quotaChangeOwningCostRefreshManager.refresh();

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(7, changes2.size());
        Map<String, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        extracted(SSD, HDD, CPU, RAM, GPU, IO_HDD, IO_SSD, owningCostByResourceKey2);
        Assertions.assertEquals(owningCostByResourceKey2.values().stream()
                .map(BigInteger::new)
                .reduce(BigInteger::add)
                .map(BigInteger::toString)
                .orElse("0"), fetchedRequest2.getRequestOwningCost());

        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(40L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(2, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(6, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(40, DiUnit.MIBPS))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(7, changes2.size());
        Map<String, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        Assertions.assertEquals(new BigDecimal("162.81").multiply(new BigDecimal("2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(CPU));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal("2"), MATH_CONTEXT).multiply(new BigDecimal(10L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(SSD));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal("2"), MATH_CONTEXT).multiply(new BigDecimal(20L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(HDD));
        Assertions.assertEquals(new BigDecimal("25.12").multiply(new BigDecimal("2"), MATH_CONTEXT).multiply(new BigDecimal(1L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(RAM));
        Assertions.assertEquals(new BigDecimal("37807").multiply(new BigDecimal("2"), MATH_CONTEXT).add(new BigDecimal("16203").multiply(new BigDecimal("2"), MATH_CONTEXT), MATH_CONTEXT)
                        .multiply(new BigDecimal(3L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(GPU));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey3.get(IO_HDD));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey3.get(IO_SSD));

        Assertions.assertEquals(owningCostByResourceKey3.values().stream()
                .map(BigInteger::new)
                .reduce(BigInteger::add)
                .map(BigInteger::toString)
                .orElse("0"), fetchedRequest3.getRequestOwningCost());
    }

    private void extracted(String SSD, String HDD, String CPU, String RAM, String GPU, String IO_HDD, String IO_SSD,
                           Map<String, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("162.81").multiply(new BigDecimal(100L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(CPU));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(10L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(SSD));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(20L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(HDD));
        Assertions.assertEquals(new BigDecimal("25.12").multiply(new BigDecimal(1L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(RAM));
        Assertions.assertEquals(new BigDecimal("37807").add(new BigDecimal("16203"), MATH_CONTEXT)
                        .multiply(new BigDecimal(3L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(GPU));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(IO_HDD));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(IO_SSD));
    }

    @Test
    public void YTOwningCostFormulaTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String BURST_GUARANTEE_CPU = "burst_guarantee_cpu";
        String CPU = "cpu";
        String CPU_FLOW = "cpu_flow";
        String GPU = "gpu";
        String HDD = "hdd";
        String MEM_BURST = "mem_burst";
        String MEM_RELAXED = "mem_relaxed";
        String MEM_STRONG = "mem_strong";
        String RPC_PROXY = "rpc_proxy";
        String SSD = "ssd";
        String TABLET_CELL_BUNDLE = "tablet_cell_bundle";
        String TABLET_STATIC_MEMORY = "tablet_static_memory";

        Service service = serviceDao.create(Service.withKey(YT).withName("YT").withAbcServiceId(5656).withPriority(190).build());

        updateHierarchy();

        Resource burstGuaranteeCpu =
                resourceDao.create(new Resource.Builder(BURST_GUARANTEE_CPU, service).name(BURST_GUARANTEE_CPU).type(DiResourceType.PROCESSOR).build());
        Resource cpu =
                resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        Resource cpuFlow =
                resourceDao.create(new Resource.Builder(CPU_FLOW, service).name(CPU_FLOW).type(DiResourceType.PROCESSOR).build());
        Resource gpu =
                resourceDao.create(new Resource.Builder(GPU, service).name(GPU).type(DiResourceType.ENUMERABLE).build());
        Resource hdd =
                resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        Resource memBurst =
                resourceDao.create(new Resource.Builder(MEM_BURST, service).name(MEM_BURST).type(DiResourceType.STORAGE).build());
        Resource memRelax =
                resourceDao.create(new Resource.Builder(MEM_RELAXED, service).name(MEM_RELAXED).type(DiResourceType.STORAGE).build());
        Resource memStrong =
                resourceDao.create(new Resource.Builder(MEM_STRONG, service).name(MEM_STRONG).type(DiResourceType.STORAGE).build());
        Resource rpc =
                resourceDao.create(new Resource.Builder(RPC_PROXY, service).name(RPC_PROXY).type(DiResourceType.ENUMERABLE).build());
        Resource ssd =
                resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());
        Resource tableCell =
                resourceDao.create(new Resource.Builder(TABLET_CELL_BUNDLE, service).name(TABLET_CELL_BUNDLE).type(DiResourceType.ENUMERABLE).build());
        Resource tableStaticMemory =
                resourceDao.create(new Resource.Builder(TABLET_STATIC_MEMORY, service).name(TABLET_STATIC_MEMORY).type(DiResourceType.STORAGE).build());

        Segmentation segmentation = segmentationDao.create(new Segmentation.Builder(ytClusterSegmentationKey)
                .name("Кластер YT")
                .description("Кластер YT")
                .priority(0)
                .build());

        updateHierarchy();

        Segment hahn = segmentDao.create(new Segment.Builder("hahn", segmentation).priority((short) 1).name("hahn").description("abc").build());
        Segment arnold = segmentDao.create(new Segment.Builder("arnold", segmentation).priority((short) 1).name("arnold").description("abc").build());
        Segment seneca_sas = segmentDao.create(new Segment.Builder("seneca-sas", segmentation).priority((short) 1).name("seneca-sas").description("abc").build());
        Segment seneca_vla = segmentDao.create(new Segment.Builder("seneca-vla", segmentation).priority((short) 1).name("seneca-vla").description("abc").build());
        Segment seneca_man = segmentDao.create(new Segment.Builder("seneca-man", segmentation).priority((short) 1).name("seneca-man").description("abc").build());
        Segment freud = segmentDao.create(new Segment.Builder("freud", segmentation).priority((short) 1).name("freud").description("abc").build());
        Segment hume = segmentDao.create(new Segment.Builder("hume", segmentation).priority((short) 1).name("hume").description("abc").build());
        Segment landau = segmentDao.create(new Segment.Builder("landau", segmentation).priority((short) 1).name("landau").description("abc").build());
        Segment bohr = segmentDao.create(new Segment.Builder("bohr", segmentation).priority((short) 1).name("bohr").description("abc").build());
        Segment zeno = segmentDao.create(new Segment.Builder("zeno", segmentation).priority((short) 1).name("zeno").description("abc").build());
        Segment locke = segmentDao.create(new Segment.Builder("locke", segmentation).priority((short) 1).name("locke").description("abc").build());
        Segment markov = segmentDao.create(new Segment.Builder("markov", segmentation).priority((short) 1).name("markov").description("abc").build());
        Segment vanga = segmentDao.create(new Segment.Builder("vanga", segmentation).priority((short) 1).name("vanga").description("abc").build());
        Segment pythia = segmentDao.create(new Segment.Builder("pythia", segmentation).priority((short) 1).name("pythia").description("abc").build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(burstGuaranteeCpu, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(cpu, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(cpuFlow, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(gpu, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(hdd, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(memBurst, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(memRelax, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(memStrong, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(rpc, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(ssd, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(tableCell, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(tableStaticMemory, Set.of(segmentation));

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)

                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))

                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))

                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))

                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))

                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_BURST, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_RELAXED, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, MEM_STRONG, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))

                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))

                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(168, changes.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElseThrow()), DiQuotaChangeRequest.Change::getOwningCost));
        // cpu burst
        Assertions.assertEquals(new BigDecimal("24.872").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("24.872").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("24.872").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("24.872").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("24.872").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("24.872").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, zeno.getPublicKey())));
        // cpu
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, zeno.getPublicKey())));
        // relaxed cpu
        Assertions.assertEquals(new BigDecimal("99.489").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("99.489").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("99.489").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("99.489").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("99.489").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("99.489").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, zeno.getPublicKey())));
        // gpu
        Assertions.assertEquals(new BigDecimal("37807").add(new BigDecimal("16203"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("37807").add(new BigDecimal("16203"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, zeno.getPublicKey())));
        // hdd
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, zeno.getPublicKey())));
        // ssd
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, zeno.getPublicKey())));
        // MEM_BURST
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal("0.2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_BURST, zeno.getPublicKey())));
        // mem relax
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal("0.8"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_RELAXED, zeno.getPublicKey())));
        // mem strong
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MEM_STRONG, zeno.getPublicKey())));
        // rpc
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("0").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("0").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("0").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("0").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("0").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("0").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, zeno.getPublicKey())));
        // TABLET_CELL_BUNDLE
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1711.85").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1711.85").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1711.85").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1711.85").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1711.85").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, zeno.getPublicKey())));
        // TABLET_STATIC_MEMORY
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, zeno.getPublicKey())));
    }

    @Test
    public void gencfgOwningCostFormulaTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String ssd = "ssd";
        String hdd = "hdd";
        String cpu = "cpu";
        String ram = "ram";
        Service service = Hierarchy.get().getServiceReader().read(GENCFG);
        resourceDao.create(new Resource.Builder(ssd, service).name(ssd).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(hdd, service).name(hdd).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(cpu, service).name(cpu).type(DiResourceType.PROCESSOR).build());
        resourceDao.create(new Resource.Builder(ram, service).name(ram).type(DiResourceType.MEMORY).build());
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(GENCFG, cpu, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(GENCFG, ssd, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(GENCFG, hdd, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(GENCFG, ram, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(4, changes.size());
        Map<String, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        extracted(ssd, hdd, cpu, ram, owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(4, changes2.size());
        Map<String, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        extracted(ssd, hdd, cpu, ram, owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(GENCFG, cpu, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.CORES))
                        .changes(GENCFG, ssd, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(GENCFG, hdd, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(40L, DiUnit.GIBIBYTE))
                        .changes(GENCFG, ram, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(2, DiUnit.GIBIBYTE))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(4, changes2.size());
        Map<String, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        Assertions.assertEquals(new BigDecimal("162.81").multiply(new BigDecimal("2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(cpu));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal("2"), MATH_CONTEXT).multiply(new BigDecimal(10L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(ssd));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal("2"), MATH_CONTEXT).multiply(new BigDecimal(20L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(hdd));
        Assertions.assertEquals(new BigDecimal("25.12").multiply(new BigDecimal("2"), MATH_CONTEXT).multiply(new BigDecimal(1L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(ram));
    }

    private void extracted(String ssd, String hdd, String cpu, String ram,
                           Map<String, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("162.81").multiply(new BigDecimal(100L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(cpu));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(10L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(ssd));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(20L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(hdd));
        Assertions.assertEquals(new BigDecimal("25.12").multiply(new BigDecimal(1L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(ram));
    }

    @Test
    public void saasOwningCostFormulaTest() {
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SAAS)
                .withName("SAAS")
                .withAbcServiceId(SAAS_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String ssd = "ssd";
        String hdd = "hdd";
        String cpu = "cpu";
        String ram = "ram";
        String ioHdd = "io_hdd";
        String ioSsd = "io_ssd";
        resourceDao.create(new Resource.Builder(ssd, service).name(ssd).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(hdd, service).name(hdd).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(cpu, service).name(cpu).type(DiResourceType.PROCESSOR).build());
        resourceDao.create(new Resource.Builder(ram, service).name(ram).type(DiResourceType.MEMORY).build());
        resourceDao.create(new Resource.Builder(ioHdd, service).name(ioHdd).type(DiResourceType.BINARY_TRAFFIC).build());
        resourceDao.create(new Resource.Builder(ioSsd, service).name(ioSsd).type(DiResourceType.BINARY_TRAFFIC).build());
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SAAS, cpu, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SAAS, ssd, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(SAAS, hdd, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(SAAS, ram, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(SAAS, ioHdd, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(SAAS, ioSsd, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(6, changes.size());
        Map<String, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        extractedSaas(ssd, hdd, cpu, ram, ioHdd, ioSsd, owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(6, changes2.size());
        Map<String, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        extractedSaas(ssd, hdd, cpu, ram, ioHdd, ioSsd, owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SAAS, cpu, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.CORES))
                        .changes(SAAS, ssd, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(SAAS, hdd, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(40L, DiUnit.GIBIBYTE))
                        .changes(SAAS, ram, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(2, DiUnit.GIBIBYTE))
                        .changes(SAAS, ioHdd, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .changes(SAAS, ioSsd, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(40, DiUnit.MIBPS))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(6, changes2.size());
        Map<String, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        Assertions.assertEquals(new BigDecimal("187.2").multiply(new BigDecimal("2"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(cpu));
        Assertions.assertEquals(new BigDecimal("1.8").multiply(new BigDecimal("2"), MATH_CONTEXT).multiply(new BigDecimal(10L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(ssd));
        Assertions.assertEquals(new BigDecimal("0.132").multiply(new BigDecimal("2"), MATH_CONTEXT).multiply(new BigDecimal(20L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(hdd));
        Assertions.assertEquals(new BigDecimal("28.88").multiply(new BigDecimal("2"), MATH_CONTEXT).multiply(new BigDecimal(1L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(ram));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey3.get(ioHdd));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey3.get(ioSsd));
    }

    private void extractedSaas(String ssd, String hdd, String cpu, String ram, String ioHdd, String ioSsd,
                               Map<String, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("187.2").multiply(new BigDecimal(100L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(cpu));
        Assertions.assertEquals(new BigDecimal("1.8").multiply(new BigDecimal(10L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(ssd));
        Assertions.assertEquals(new BigDecimal("0.132").multiply(new BigDecimal(20L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(hdd));
        Assertions.assertEquals(new BigDecimal("28.88").multiply(new BigDecimal(1L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(ram));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(ioHdd));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(ioSsd));
    }

    @Test
    public void nirvanaOwningCostFormulaTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String CPU = "cpu";
        String GPU = "gpu";
        String S3_STORAGE = "s3-storage";
        String RAM_GIG = "ram_gig";

        Service service = Hierarchy.get().getServiceReader().read(NIRVANA);

        updateHierarchy();

        Resource cpu =
                resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        Resource gpu =
                resourceDao.create(new Resource.Builder(GPU, service).name(GPU).type(DiResourceType.ENUMERABLE).build());
        Resource s3Storage =
                resourceDao.create(new Resource.Builder(S3_STORAGE, service).name(S3_STORAGE).type(DiResourceType.STORAGE).build());
        Resource ram =
                resourceDao.create(new Resource.Builder(RAM_GIG, service).name(RAM_GIG).type(DiResourceType.MEMORY).build());

        Segmentation location = segmentationDao.read(new Segmentation.Key(locationSegmentationKey));

        updateHierarchy();

        Segment vla = segmentDao.create(new Segment.Builder("VLA", location).priority((short) 1).name("VLA").description("abc").build());
        Segment sas = segmentDao.create(new Segment.Builder("SAS", location).priority((short) 1).name("SAS").description("abc").build());
        Segment iva = segmentDao.create(new Segment.Builder("IVA", location).priority((short) 1).name("IVA").description("abc").build());
        Segment myt = segmentDao.create(new Segment.Builder("MYT", location).priority((short) 1).name("MYT").description("abc").build());
        Segment man = segmentDao.create(new Segment.Builder("MAN", location).priority((short) 1).name("MAN").description("abc").build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(location));
        resourceSegmentationDao.setSegmentations(gpu, Set.of(location));
        resourceSegmentationDao.setSegmentations(ram, Set.of(location));

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)

                        .changes(NIRVANA, CPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))

                        .changes(NIRVANA, RAM_GIG, bigOrderTwo.getId(), Set.of(sas.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .changes(NIRVANA, GPU, bigOrderTwo.getId(), Set.of(sas.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(NIRVANA, GPU, bigOrderTwo.getId(), Set.of(vla.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))

                        .changes(NIRVANA, S3_STORAGE, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(5, changes.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));
        // cpu
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, sas.getPublicKey())));
        // ram
        Assertions.assertEquals(new BigDecimal("29.693", MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RAM_GIG, sas.getPublicKey())));
        // gpu
        Assertions.assertEquals(new BigDecimal("37807").add(new BigDecimal("16203"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("37807").add(new BigDecimal("16203"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, sas.getPublicKey())));
        // s3
        Assertions.assertEquals(new BigDecimal("0.1047",MATH_CONTEXT).multiply(new BigDecimal(100L)
                        .multiply(new BigDecimal(5, MATH_CONTEXT), MATH_CONTEXT) // mockMdsConfigApi by default answer with 5 dc with exact storage from request
                , MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(S3_STORAGE, null)));
    }

    @Test
    public void mdsOwningCostFormulaTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        Service service = Hierarchy.get().getServiceReader().read(MDS_STORAGE);

        updateHierarchy();

        Resource mds =
                resourceDao.create(new Resource.Builder(MDS_STORAGE, service).name(MDS_STORAGE).type(DiResourceType.STORAGE).build());
        Resource avatars =
                resourceDao.create(new Resource.Builder(AVATARS, service).name(AVATARS).type(DiResourceType.STORAGE).build());
        Resource s3Api =
                resourceDao.create(new Resource.Builder(S3_API, service).name(S3_API).type(DiResourceType.STORAGE).build());

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(MDS, MDS_STORAGE, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(MDS, AVATARS, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(MDS, S3_API, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertCalculated(fetchedRequest, 1);
    }

    @Test
    public void mdsPreCalculationTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        Service service = Hierarchy.get().getServiceReader().read(MDS_STORAGE);

        updateHierarchy();

        Resource mds =
                resourceDao.create(new Resource.Builder(MDS_STORAGE, service).name(MDS_STORAGE).type(DiResourceType.STORAGE).build());
        Resource avatars =
                resourceDao.create(new Resource.Builder(AVATARS, service).name(AVATARS).type(DiResourceType.STORAGE).build());
        Resource s3Api =
                resourceDao.create(new Resource.Builder(S3_API, service).name(S3_API).type(DiResourceType.STORAGE).build());

        updateHierarchy();

        prepareCampaignResources();

        mockMdsConfigApi.setFail(true);

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(MDS, MDS_STORAGE, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(MDS, AVATARS, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(MDS, S3_API, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertZeroes(fetchedRequest);

        mockMdsConfigApi.setFail(false);

        quotaChangeOwningCostRefreshManager.refresh();

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertCalculated(fetchedRequest2, 1);

        mockMdsConfigApi.setFail(true);

        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(MDS, MDS_STORAGE, bigOrderTwo.getId(), Set.of(), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(MDS, AVATARS, bigOrderTwo.getId(), Set.of(), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(MDS, S3_API, bigOrderTwo.getId(), Set.of(), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertZeroes(fetchedRequest3);

        mockMdsConfigApi.setFail(false);

        quotaChangeOwningCostRefreshManager.refresh();

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest4 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertCalculated(fetchedRequest4, 2);

        mockMdsConfigApi.setFail(true);

        quotaChangeOwningCostRefreshManager.refresh();

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest5 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertCalculated(fetchedRequest5, 2);
    }

    @Test
    public void mdsPreCalculationFailTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        Service service = Hierarchy.get().getServiceReader().read(MDS_STORAGE);

        updateHierarchy();

        resourceDao.create(new Resource.Builder(MDS_STORAGE, service).name(MDS_STORAGE).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(AVATARS, service).name(AVATARS).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(S3_API, service).name(S3_API).type(DiResourceType.STORAGE).build());

        updateHierarchy();

        prepareCampaignResources();

        mockMdsConfigApi.setFail(true);

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(MDS, MDS_STORAGE, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(MDS, AVATARS, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(MDS, S3_API, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        final DiListResponse<DiQuotaChangeRequest> quotaRequests2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(MDS, MDS_STORAGE, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(MDS, AVATARS, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(MDS, S3_API, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertZeroes(fetchedRequest);

        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests2.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertZeroes(fetchedRequest2);

        quotaChangeOwningCostRefreshManager.refresh();

        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertZeroes(fetchedRequest3);

        final DiQuotaChangeRequest fetchedRequest4 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests2.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertZeroes(fetchedRequest4);

        mockMdsConfigApi.setFail(false);

        quotaChangeOwningCostRefreshManager.refresh();

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest5 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertCalculated(fetchedRequest5, 1);

        final DiQuotaChangeRequest fetchedRequest6 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests2.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        assertCalculated(fetchedRequest5, 1);
    }

    private void assertZeroes(DiQuotaChangeRequest fetchedRequest) {
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(3, changes.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        String zero = new BigDecimal("0", MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString();
        // mds
        Assertions.assertEquals(zero, owningCostByResourceKey.get(Tuple2.tuple(MDS_STORAGE, null)));
        // avatars
        Assertions.assertEquals(zero, owningCostByResourceKey.get(Tuple2.tuple(AVATARS, null)));
        // s3Api
        Assertions.assertEquals(zero, owningCostByResourceKey.get(Tuple2.tuple(S3_API, null)));
    }

    private void assertCalculated(DiQuotaChangeRequest fetchedRequest, int times) {
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(3, changes.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        // mds
        Assertions.assertEquals(new BigDecimal("0.1047",MATH_CONTEXT).multiply(new BigDecimal(100L)
                        .multiply(new BigDecimal(5, MATH_CONTEXT), MATH_CONTEXT) // mockMdsConfigApi by default answer with 5 dc with exact storage from request
                        .multiply(new BigDecimal(times, MATH_CONTEXT), MATH_CONTEXT)
                , MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(MDS_STORAGE, null)));
        // avatars
        Assertions.assertEquals(new BigDecimal("0.1047",MATH_CONTEXT).multiply(new BigDecimal(100L)
                        .multiply(new BigDecimal(5, MATH_CONTEXT), MATH_CONTEXT) // mockMdsConfigApi by default answer with 5 dc with exact storage from request
                        .multiply(new BigDecimal(times, MATH_CONTEXT), MATH_CONTEXT)
                , MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(AVATARS, null)));
        // s3Api
        Assertions.assertEquals(new BigDecimal("0.1047",MATH_CONTEXT).multiply(new BigDecimal(100L)
                        .multiply(new BigDecimal(5, MATH_CONTEXT), MATH_CONTEXT) // mockMdsConfigApi by default answer with 5 dc with exact storage from request
                        .multiply(new BigDecimal(times, MATH_CONTEXT), MATH_CONTEXT)
                , MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(S3_API, null)));
    }

    @Test
    public void MDSOwningCostFormulaTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String CPU = "cpu";
        String HDD = "hdd";
        String SSD = "ssd";
        String RAM = "ram";
        String S3 = "storage-s3";

        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey("dbaas")
                .withName(MDB)
                .withAbcServiceId(12345)
                .withSettings(settings)
                .build());

        updateHierarchy();

        Resource cpu =
                resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        Resource hdd =
                resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        Resource ram =
                resourceDao.create(new Resource.Builder(RAM, service).name(RAM).type(DiResourceType.MEMORY).build());
        Resource ssd =
                resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());
        Resource s3 =
                resourceDao.create(new Resource.Builder(S3, service).name(S3).type(DiResourceType.STORAGE).build());


        Segmentation segmentation = segmentationDao.create(new Segmentation.Builder(mdbDbSegmentationKey)
                .name("База данных")
                .description("База данных")
                .priority(0)
                .build());

        updateHierarchy();

        Segment kafka = segmentDao.create(new Segment.Builder("dbaas_kafka", segmentation).priority((short) 1).name("dbaas_kafka").description("abc").build());
        Segment elasticsearch = segmentDao.create(new Segment.Builder("dbaas_elasticsearch", segmentation).priority((short) 1).name("dbaas_elasticsearch").description("abc").build());
        Segment clickhouse = segmentDao.create(new Segment.Builder("dbaas_clickhouse", segmentation).priority((short) 1).name("dbaas_clickhouse").description("abc").build());
        Segment mongodb = segmentDao.create(new Segment.Builder("dbaas_mongodb", segmentation).priority((short) 1).name("dbaas_mongodb").description("abc").build());
        Segment mysql = segmentDao.create(new Segment.Builder("dbaas_mysql", segmentation).priority((short) 1).name("dbaas_mysql").description("abc").build());
        Segment redis = segmentDao.create(new Segment.Builder("dbaas_redis", segmentation).priority((short) 1).name("dbaas_redis").description("abc").build());
        Segment pg = segmentDao.create(new Segment.Builder("dbaas_pgsql", segmentation).priority((short) 1).name("dbaas_pgsql").description("abc").build());
        Segment greenplum = segmentDao.create(new Segment.Builder("dbaas_greenplum", segmentation).priority((short) 1).name("dbaas_greenplum").description("abc").build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(hdd, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(ssd, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(ram, Set.of(segmentation));

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)

                        .changes("dbaas", CPU, bigOrderTwo.getId(), Set.of(kafka.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes("dbaas", CPU, bigOrderTwo.getId(), Set.of(elasticsearch.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes("dbaas", CPU, bigOrderTwo.getId(), Set.of(clickhouse.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes("dbaas", CPU, bigOrderTwo.getId(), Set.of(mongodb.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes("dbaas", CPU, bigOrderTwo.getId(), Set.of(mysql.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes("dbaas", CPU, bigOrderTwo.getId(), Set.of(redis.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes("dbaas", CPU, bigOrderTwo.getId(), Set.of(pg.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes("dbaas", CPU, bigOrderTwo.getId(), Set.of(greenplum.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))

                        .changes("dbaas", RAM, bigOrderTwo.getId(), Set.of(kafka.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", RAM, bigOrderTwo.getId(), Set.of(elasticsearch.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", RAM, bigOrderTwo.getId(), Set.of(clickhouse.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", RAM, bigOrderTwo.getId(), Set.of(mongodb.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", RAM, bigOrderTwo.getId(), Set.of(mysql.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", RAM, bigOrderTwo.getId(), Set.of(redis.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", RAM, bigOrderTwo.getId(), Set.of(pg.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", RAM, bigOrderTwo.getId(), Set.of(greenplum.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .changes("dbaas", SSD, bigOrderTwo.getId(), Set.of(kafka.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", SSD, bigOrderTwo.getId(), Set.of(elasticsearch.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", SSD, bigOrderTwo.getId(), Set.of(clickhouse.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", SSD, bigOrderTwo.getId(), Set.of(mongodb.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", SSD, bigOrderTwo.getId(), Set.of(mysql.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", SSD, bigOrderTwo.getId(), Set.of(redis.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", SSD, bigOrderTwo.getId(), Set.of(pg.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes("dbaas", SSD, bigOrderTwo.getId(), Set.of(greenplum.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .changes("dbaas", HDD, bigOrderTwo.getId(), Set.of(clickhouse.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .changes("dbaas", S3, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(26, changes.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        // cpu
        Assertions.assertEquals(new BigDecimal("0.2435").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, kafka.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.2551").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, elasticsearch.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.2551").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, mongodb.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.2435").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, mysql.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.2319").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, pg.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.2435").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, redis.getPublicKey())));

        Assertions.assertEquals(new BigDecimal("0.2482").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, clickhouse.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.2319").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, greenplum.getPublicKey())));

        // ram
        Assertions.assertEquals(new BigDecimal("0.0366").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RAM, kafka.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.0384").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RAM, elasticsearch.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.0384").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RAM, mongodb.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.0366").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RAM, mysql.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.0349").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RAM, pg.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.0366").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RAM, redis.getPublicKey())));

        Assertions.assertEquals(new BigDecimal("0.0373").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RAM, clickhouse.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.0349").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RAM, greenplum.getPublicKey())));

        // ssd
        Assertions.assertEquals(new BigDecimal("0.0023").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, kafka.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.0023").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, elasticsearch.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.001295").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, mongodb.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.0023").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, mysql.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.0023").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, pg.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.0023").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, redis.getPublicKey())));

        Assertions.assertEquals(new BigDecimal("0.0023").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, clickhouse.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.0023").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, greenplum.getPublicKey())));

        //hdd
        Assertions.assertEquals(new BigDecimal("0.0002").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(720L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, clickhouse.getPublicKey())));

        // s3
        Assertions.assertEquals(new BigDecimal("0.1047").multiply(new BigDecimal(100L), MATH_CONTEXT).multiply(new BigDecimal(5), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(S3, null)));
    }

    @Test
    public void logbrokerOwningCostFormulaTest() {
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(LOGBROKER)
                .withName("Logbroker")
                .withAbcServiceId(LOGBROKER_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String dataflow = "data_flow";
        String dataflowBinary = "data_flow_binary";
        Resource dataflowResource = resourceDao.create(new Resource.Builder(dataflow, service).name(dataflow)
                .type(DiResourceType.TRAFFIC).build());
        Resource dataflowBinaryResource = resourceDao.create(new Resource.Builder(dataflowBinary, service)
                .name(dataflowBinary).type(DiResourceType.BINARY_TRAFFIC).build());
        updateHierarchy();
        Segmentation location = segmentationDao.create(new Segmentation.Builder("logbroker").name("logbroker")
                .description("logbroker").priority(1).build());
        updateHierarchy();
        Segment lbkx = segmentDao.create(new Segment.Builder("lbkx", location).priority((short) 1)
                .name("lbkx").description("lbkx").build());
        Segment logbrokerSAS = segmentDao.create(new Segment.Builder("logbroker_SAS", location).priority((short) 1)
                .name("logbroker_SAS").description("logbroker_SAS").build());
        Segment logbrokerVLA = segmentDao.create(new Segment.Builder("logbroker_VLA", location).priority((short) 1)
                .name("logbroker_VLA").description("logbroker_VLA").build());
        Segment logbrokerMAN = segmentDao.create(new Segment.Builder("logbroker_MAN", location).priority((short) 1)
                .name("logbroker_MAN").description("logbroker_MAN").build());
        Segment logbrokerMYT = segmentDao.create(new Segment.Builder("logbroker_MYT", location).priority((short) 1)
                .name("logbroker_MYT").description("logbroker_MYT").build());
        Segment logbrokerIVA = segmentDao.create(new Segment.Builder("logbroker_IVA", location).priority((short) 1)
                .name("logbroker_IVA").description("logbroker_IVA").build());
        Segment logbrokerPrestableSAS = segmentDao.create(new Segment.Builder("logbroker-prestable_SAS", location)
                .priority((short) 1).name("logbroker-prestable_SAS").description("logbroker-prestable_SAS").build());
        Segment logbrokerPrestableVLA = segmentDao.create(new Segment.Builder("logbroker-prestable_VLA", location)
                .priority((short) 1).name("logbroker-prestable_VLA").description("logbroker-prestable_VLA").build());
        Segment logbrokerPrestableMAN = segmentDao.create(new Segment.Builder("logbroker-prestable_MAN", location)
                .priority((short) 1).name("logbroker-prestable_MAN").description("logbroker-prestable_MAN").build());
        Segment logbrokerPrestableMYT = segmentDao.create(new Segment.Builder("logbroker-prestable_MYT", location)
                .priority((short) 1).name("logbroker-prestable_MYT").description("logbroker-prestable_MYT").build());
        Segment logbrokerPrestableIVA = segmentDao.create(new Segment.Builder("logbroker-prestable_IVA", location)
                .priority((short) 1).name("logbroker-prestable_IVA").description("logbroker-prestable_IVA").build());
        Segment logbrokerYC = segmentDao.create(new Segment.Builder("logbroker-yc", location).priority((short) 1)
                .name("logbroker-yc").description("logbroker-yc").build());
        Segment logbrokerPreprodYC = segmentDao.create(new Segment.Builder("logbroker-yc-preprod", location)
                .priority((short) 1).name("logbroker-yc-preprod").description("logbroker-yc-preprod").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(dataflowResource, Set.of(location));
        resourceSegmentationDao.setSegmentations(dataflowBinaryResource, Set.of(location));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(lbkx.getPublicKey()), DiAmount.of(1L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerSAS.getPublicKey()), DiAmount.of(2L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerVLA.getPublicKey()), DiAmount.of(3L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerMAN.getPublicKey()), DiAmount.of(4L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerMYT.getPublicKey()), DiAmount.of(5L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerIVA.getPublicKey()), DiAmount.of(6L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableSAS.getPublicKey()), DiAmount.of(7L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableVLA.getPublicKey()), DiAmount.of(8L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableMAN.getPublicKey()), DiAmount.of(9L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableMYT.getPublicKey()), DiAmount.of(10L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableIVA.getPublicKey()), DiAmount.of(11L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerYC.getPublicKey()), DiAmount.of(12L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPreprodYC.getPublicKey()), DiAmount.of(13L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(lbkx.getPublicKey()), DiAmount.of(1L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerSAS.getPublicKey()), DiAmount.of(2L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerVLA.getPublicKey()), DiAmount.of(3L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerMAN.getPublicKey()), DiAmount.of(4L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerMYT.getPublicKey()), DiAmount.of(5L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerIVA.getPublicKey()), DiAmount.of(6L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerPrestableSAS.getPublicKey()), DiAmount.of(7L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerPrestableVLA.getPublicKey()), DiAmount.of(8L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerPrestableMAN.getPublicKey()), DiAmount.of(9L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerPrestableMYT.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerPrestableIVA.getPublicKey()), DiAmount.of(11L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerYC.getPublicKey()), DiAmount.of(12L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerPreprodYC.getPublicKey()), DiAmount.of(13L, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(26, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateLogbroker(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(26, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateLogbroker(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(lbkx.getPublicKey()), DiAmount.of(10L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerSAS.getPublicKey()), DiAmount.of(20L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerVLA.getPublicKey()), DiAmount.of(30L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerMAN.getPublicKey()), DiAmount.of(40L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerMYT.getPublicKey()), DiAmount.of(50L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerIVA.getPublicKey()), DiAmount.of(60L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableSAS.getPublicKey()), DiAmount.of(70L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableVLA.getPublicKey()), DiAmount.of(80L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableMAN.getPublicKey()), DiAmount.of(90L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableMYT.getPublicKey()), DiAmount.of(100L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableIVA.getPublicKey()), DiAmount.of(110L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerYC.getPublicKey()), DiAmount.of(120L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPreprodYC.getPublicKey()), DiAmount.of(130L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(lbkx.getPublicKey()), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerSAS.getPublicKey()), DiAmount.of(20L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerVLA.getPublicKey()), DiAmount.of(30L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerMAN.getPublicKey()), DiAmount.of(40L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerMYT.getPublicKey()), DiAmount.of(50L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerIVA.getPublicKey()), DiAmount.of(60L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerPrestableSAS.getPublicKey()), DiAmount.of(70L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerPrestableVLA.getPublicKey()), DiAmount.of(80L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerPrestableMAN.getPublicKey()), DiAmount.of(90L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerPrestableMYT.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerPrestableIVA.getPublicKey()), DiAmount.of(110L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerYC.getPublicKey()), DiAmount.of(120L, DiUnit.MIBPS))
                        .changes(LOGBROKER, dataflowBinary, bigOrderTwo.getId(), Set.of(logbrokerPreprodYC.getPublicKey()), DiAmount.of(130L, DiUnit.MIBPS))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(26, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateLogbrokerUpdated(owningCostByResourceKey3);
    }

    private void validateLogbroker(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("1735.17").multiply(new BigDecimal(1L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "lbkx")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(2L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker_SAS")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(3L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker_VLA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(4L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker_MAN")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(5L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker_MYT")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(6L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker_IVA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(7L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-prestable_SAS")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(8L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-prestable_VLA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(9L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-prestable_MAN")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(10L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-prestable_MYT")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(11L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-prestable_IVA")));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(12L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-yc")));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(13L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-yc-preprod")));
        Assertions.assertEquals(new BigDecimal("1735.17").multiply(toMibps(new BigDecimal(1L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "lbkx")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(2L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_SAS")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(3L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_VLA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(4L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_MAN")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(5L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_MYT")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(6L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_IVA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(7L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_SAS")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(8L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_VLA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(9L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_MAN")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(10L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_MYT")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(11L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_IVA")));
        Assertions.assertEquals(new BigDecimal("0").multiply(toMibps(new BigDecimal(12L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-yc")));
        Assertions.assertEquals(new BigDecimal("0").multiply(toMibps(new BigDecimal(13L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-yc-preprod")));
    }

    private void validateLogbrokerUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("1735.17").multiply(new BigDecimal(10L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "lbkx")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(20L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker_SAS")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(30L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker_VLA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(40L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker_MAN")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(50L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker_MYT")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(60L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker_IVA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(70L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-prestable_SAS")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(80L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-prestable_VLA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(90L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-prestable_MAN")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(100L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-prestable_MYT")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(new BigDecimal(110L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-prestable_IVA")));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(120L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-yc")));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(130L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow_binary", "logbroker-yc-preprod")));
        Assertions.assertEquals(new BigDecimal("1735.17").multiply(toMibps(new BigDecimal(10L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "lbkx")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(20L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_SAS")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(30L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_VLA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(40L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_MAN")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(50L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_MYT")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(60L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_IVA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(70L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_SAS")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(80L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_VLA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(90L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_MAN")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(100L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_MYT")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(110L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_IVA")));
        Assertions.assertEquals(new BigDecimal("0").multiply(toMibps(new BigDecimal(120L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-yc")));
        Assertions.assertEquals(new BigDecimal("0").multiply(toMibps(new BigDecimal(130L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-yc-preprod")));
    }

    private BigDecimal toMibps(BigDecimal mbps) {
        return mbps.multiply(BigDecimal.valueOf(1_000_000L), MATH_CONTEXT)
                .divide(BigDecimal.valueOf(1024L).pow(2), MATH_CONTEXT);
    }

    @Test
    public void distbuildOwningCostFormulaTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String CPU = "cpu";

        Service service = serviceDao.create(Service.withKey(DISTBUILD).withName("DISTBUILD").withAbcServiceId(5656).withPriority(190).build());

        updateHierarchy();

        Resource cpu =
                resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());

        Segmentation segmentation = segmentationDao.create(new Segmentation.Builder(distbuildSegmentationKey).name(distbuildSegmentationKey)
                .description(distbuildSegmentationKey).priority(1).build());

        updateHierarchy();

        Segment distbuild_autocheck = segmentDao.create(new Segment.Builder("distbuild_autocheck", segmentation).priority((short) 1).name("distbuild_autocheck").description("abc").build());
        Segment distbuild_user = segmentDao.create(new Segment.Builder("distbuild_user", segmentation).priority((short) 1).name("distbuild_user").description("abc").build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(segmentation));

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)

                        .changes(DISTBUILD, CPU, bigOrderTwo.getId(), Set.of(distbuild_autocheck.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(DISTBUILD, CPU, bigOrderTwo.getId(), Set.of(distbuild_user.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))

                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(2, changes.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElseThrow()), DiQuotaChangeRequest.Change::getOwningCost));
        // cpu
        Assertions.assertEquals(new BigDecimal("532.22").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, distbuild_autocheck.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("462.8").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, distbuild_user.getPublicKey())));
    }

    @Test
    public void rtmrOwningCostFormulaTest() {
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service rtmrProcessing = serviceDao.create(Service.withKey(RTMR_PROCESSING)
                .withName("RTMR Processing")
                .withAbcServiceId(RTMR_PROCESSING_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Service rtmrMirror = serviceDao.create(Service.withKey(RTMR_MIRROR)
                .withName("RTMR Mirror")
                .withAbcServiceId(RTMR_MIRROR_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(rtmrProcessing, slonnn);
        serviceDao.attachAdmin(rtmrProcessing, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        serviceDao.attachAdmin(rtmrMirror, slonnn);
        serviceDao.attachAdmin(rtmrMirror, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpu = "cpu";
        String dataFlow = "dataflow";
        String processingData = "data";
        String yfSlot = "yf_slot";
        String mirrorData = "data";
        String kps = "kps";
        resourceDao.create(new Resource.Builder(cpu, rtmrProcessing).name(cpu).type(DiResourceType.PROCESSOR).build());
        resourceDao.create(new Resource.Builder(dataFlow, rtmrProcessing).name(dataFlow).type(DiResourceType.BINARY_TRAFFIC).build());
        resourceDao.create(new Resource.Builder(processingData, rtmrProcessing).name(processingData).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(yfSlot, rtmrProcessing).name(yfSlot).type(DiResourceType.ENUMERABLE).build());
        resourceDao.create(new Resource.Builder(mirrorData, rtmrMirror).name(mirrorData).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(kps, rtmrMirror).name(kps).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(RTMR_PROCESSING, cpu, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(RTMR_PROCESSING, dataFlow, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.MIBPS))
                        .changes(RTMR_PROCESSING, processingData, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(RTMR_PROCESSING, yfSlot, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.COUNT))
                        .changes(RTMR_MIRROR, mirrorData, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.GIBIBYTE))
                        .changes(RTMR_MIRROR, kps, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(6, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getService().getKey(), k.getResource().getKey()),
                        DiQuotaChangeRequest.Change::getOwningCost));
        extractedRtmr(cpu, dataFlow, processingData, yfSlot, mirrorData, kps, owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(6, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getService().getKey(), k.getResource().getKey()),
                        DiQuotaChangeRequest.Change::getOwningCost));
        extractedRtmr(cpu, dataFlow, processingData, yfSlot, mirrorData, kps, owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(RTMR_PROCESSING, cpu, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.CORES))
                        .changes(RTMR_PROCESSING, dataFlow, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.MIBPS))
                        .changes(RTMR_PROCESSING, processingData, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(40L, DiUnit.GIBIBYTE))
                        .changes(RTMR_PROCESSING, yfSlot, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(2, DiUnit.COUNT))
                        .changes(RTMR_MIRROR, mirrorData, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.GIBIBYTE))
                        .changes(RTMR_MIRROR, kps, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(40, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(6, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getService().getKey(), k.getResource().getKey()),
                        DiQuotaChangeRequest.Change::getOwningCost));
        Assertions.assertEquals(new BigDecimal("769.03")
                        .multiply(new BigDecimal(200L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of(RTMR_PROCESSING, cpu)));
        Assertions.assertEquals(new BigDecimal("100.05")
                        .multiply(new BigDecimal(20L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of(RTMR_PROCESSING, dataFlow)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey3.get(Pair.of(RTMR_PROCESSING, processingData)));
        Assertions.assertEquals(new BigDecimal("605.43")
                        .multiply(new BigDecimal(2L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of(RTMR_PROCESSING, yfSlot)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey3.get(Pair.of(RTMR_MIRROR, mirrorData)));
        Assertions.assertEquals(new BigDecimal("1.15")
                        .multiply(new BigDecimal(40L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of(RTMR_MIRROR, kps)));
    }

    private void extractedRtmr(String cpu, String dataFlow, String processingData, String yfSlot, String mirrorData,
                               String kps, Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("769.03").multiply(new BigDecimal(100L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of(RTMR_PROCESSING, cpu)));
        Assertions.assertEquals(new BigDecimal("100.05").multiply(new BigDecimal(10L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of(RTMR_PROCESSING, dataFlow)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of(RTMR_PROCESSING, processingData)));
        Assertions.assertEquals(new BigDecimal("605.43").multiply(new BigDecimal(1L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of(RTMR_PROCESSING, yfSlot)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of(RTMR_MIRROR, mirrorData)));
        Assertions.assertEquals(new BigDecimal("1.15").multiply(new BigDecimal(20L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of(RTMR_MIRROR, kps)));
    }

    @Test
    public void ydbOwningCostFormulaTest() {
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(YDB)
                .withName("YDB")
                .withAbcServiceId(SAAS_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String dataSize = "data_size";
        String userpoolCores = "userpool_cores";
        resourceDao.create(new Resource.Builder(dataSize, service).name(dataSize).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(userpoolCores, service).name(userpoolCores).type(DiResourceType.PROCESSOR).build());
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YDB, dataSize, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YDB, userpoolCores, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(2, changes.size());
        Map<String, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        extractedYdb(dataSize, userpoolCores, owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(2, changes2.size());
        Map<String, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        extractedYdb(dataSize, userpoolCores, owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(YDB, dataSize, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(YDB, userpoolCores, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.CORES))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(2, changes2.size());
        Map<String, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        Assertions.assertEquals(new BigDecimal("1.566", MATH_CONTEXT).multiply(new BigDecimal("1632", MATH_CONTEXT)
                        .multiply(new BigDecimal(2000L, MATH_CONTEXT)
                                .multiply(new BigDecimal("1024").pow(3, MATH_CONTEXT), MATH_CONTEXT)
                                .divide(new BigDecimal(10, MATH_CONTEXT).pow(9, MATH_CONTEXT), MATH_CONTEXT)
                                .divide(new BigDecimal("240", MATH_CONTEXT), MATH_CONTEXT)
                                .setScale(0, RoundingMode.UP), MATH_CONTEXT)
                        .multiply(new BigDecimal(10, MATH_CONTEXT).pow(9, MATH_CONTEXT), MATH_CONTEXT)
                        .divide(new BigDecimal("1024").pow(3, MATH_CONTEXT), MATH_CONTEXT), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(dataSize));

        Assertions.assertEquals(new BigDecimal("0.46875", MATH_CONTEXT)
                .multiply(new BigDecimal(200L, MATH_CONTEXT), MATH_CONTEXT)
                .setScale(0, RoundingMode.UP)
                .max(new BigDecimal(3, MATH_CONTEXT))
                .multiply(new BigDecimal("12", MATH_CONTEXT), MATH_CONTEXT)
                .multiply(new BigDecimal("162.81", MATH_CONTEXT), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(userpoolCores));
    }

    private void extractedYdb(String dataSize, String userpoolCores, Map<String, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("1.566", MATH_CONTEXT).multiply(new BigDecimal("1632", MATH_CONTEXT)
                        .multiply(new BigDecimal(100L, MATH_CONTEXT)
                                .multiply(new BigDecimal("1024").pow(3, MATH_CONTEXT), MATH_CONTEXT)
                                .divide(new BigDecimal(10, MATH_CONTEXT).pow(9, MATH_CONTEXT), MATH_CONTEXT)
                                .divide(new BigDecimal("240", MATH_CONTEXT), MATH_CONTEXT)
                                .setScale(0, RoundingMode.UP), MATH_CONTEXT)
                        .multiply(new BigDecimal(10, MATH_CONTEXT).pow(9, MATH_CONTEXT), MATH_CONTEXT)
                        .divide(new BigDecimal("1024").pow(3, MATH_CONTEXT), MATH_CONTEXT), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(dataSize));

        Assertions.assertEquals(new BigDecimal("0.46875", MATH_CONTEXT).multiply(new BigDecimal(100L, MATH_CONTEXT), MATH_CONTEXT)
                .setScale(0, RoundingMode.UP)
                .max(new BigDecimal(3, MATH_CONTEXT))
                .multiply(new BigDecimal("12", MATH_CONTEXT), MATH_CONTEXT)
                .multiply(new BigDecimal("162.81", MATH_CONTEXT), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(userpoolCores));
    }

    @Test
    public void YT2020OwningCostFormulaTest() {
        prepareCampaign2020();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String BURST_GUARANTEE_CPU = "burst_guarantee_cpu";
        String CLUSTER_NETWORK_TRAFFIC = "cluster_network_traffic";
        String CPU = "cpu";
        String CPU_CLICKHOUSE = "cpu_clickhouse";
        String CPU_FLOW = "cpu_flow";
        String GPU = "gpu";
        String HDD = "hdd";
        String RPC_PROXY = "rpc_proxy";
        String SSD = "ssd";
        String TABLET_CELL_BUNDLE = "tablet_cell_bundle";
        String TABLET_STATIC_MEMORY = "tablet_static_memory";

        Service service = serviceDao.create(Service.withKey(YT).withName("YT").withAbcServiceId(5656).withPriority(190).build());

        updateHierarchy();

        Resource clusterNetworkTraffic =
                resourceDao.create(new Resource.Builder(CLUSTER_NETWORK_TRAFFIC, service).name(CLUSTER_NETWORK_TRAFFIC).type(DiResourceType.BINARY_TRAFFIC).build());
        Resource cpuClickhouse =
                resourceDao.create(new Resource.Builder(CPU_CLICKHOUSE, service).name(CPU_CLICKHOUSE).type(DiResourceType.PROCESSOR).build());
        Resource burstGuaranteeCpu =
                resourceDao.create(new Resource.Builder(BURST_GUARANTEE_CPU, service).name(BURST_GUARANTEE_CPU).type(DiResourceType.PROCESSOR).build());
        Resource cpu =
                resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        Resource cpuFlow =
                resourceDao.create(new Resource.Builder(CPU_FLOW, service).name(CPU_FLOW).type(DiResourceType.PROCESSOR).build());
        Resource gpu =
                resourceDao.create(new Resource.Builder(GPU, service).name(GPU).type(DiResourceType.ENUMERABLE).build());
        Resource hdd =
                resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        Resource rpc =
                resourceDao.create(new Resource.Builder(RPC_PROXY, service).name(RPC_PROXY).type(DiResourceType.ENUMERABLE).build());
        Resource ssd =
                resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());
        Resource tableCell =
                resourceDao.create(new Resource.Builder(TABLET_CELL_BUNDLE, service).name(TABLET_CELL_BUNDLE).type(DiResourceType.ENUMERABLE).build());
        Resource tableStaticMemory =
                resourceDao.create(new Resource.Builder(TABLET_STATIC_MEMORY, service).name(TABLET_STATIC_MEMORY).type(DiResourceType.STORAGE).build());

        Segmentation segmentation = segmentationDao.create(new Segmentation.Builder(ytClusterSegmentationKey)
                .name("Кластер YT")
                .description("Кластер YT")
                .priority(0)
                .build());

        updateHierarchy();

        Segment hahn = segmentDao.create(new Segment.Builder("hahn", segmentation).priority((short) 1).name("hahn").description("abc").build());
        Segment arnold = segmentDao.create(new Segment.Builder("arnold", segmentation).priority((short) 1).name("arnold").description("abc").build());
        Segment seneca_sas = segmentDao.create(new Segment.Builder("seneca-sas", segmentation).priority((short) 1).name("seneca-sas").description("abc").build());
        Segment seneca_vla = segmentDao.create(new Segment.Builder("seneca-vla", segmentation).priority((short) 1).name("seneca-vla").description("abc").build());
        Segment seneca_man = segmentDao.create(new Segment.Builder("seneca-man", segmentation).priority((short) 1).name("seneca-man").description("abc").build());
        Segment freud = segmentDao.create(new Segment.Builder("freud", segmentation).priority((short) 1).name("freud").description("abc").build());
        Segment hume = segmentDao.create(new Segment.Builder("hume", segmentation).priority((short) 1).name("hume").description("abc").build());
        Segment landau = segmentDao.create(new Segment.Builder("landau", segmentation).priority((short) 1).name("landau").description("abc").build());
        Segment bohr = segmentDao.create(new Segment.Builder("bohr", segmentation).priority((short) 1).name("bohr").description("abc").build());
        Segment zeno = segmentDao.create(new Segment.Builder("zeno", segmentation).priority((short) 1).name("zeno").description("abc").build());
        Segment locke = segmentDao.create(new Segment.Builder("locke", segmentation).priority((short) 1).name("locke").description("abc").build());
        Segment markov = segmentDao.create(new Segment.Builder("markov", segmentation).priority((short) 1).name("markov").description("abc").build());
        Segment vanga = segmentDao.create(new Segment.Builder("vanga", segmentation).priority((short) 1).name("vanga").description("abc").build());
        Segment pythia = segmentDao.create(new Segment.Builder("pythia", segmentation).priority((short) 1).name("pythia").description("abc").build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(burstGuaranteeCpu, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(cpu, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(cpuFlow, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(gpu, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(hdd, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(rpc, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(ssd, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(tableCell, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(tableStaticMemory, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(cpuClickhouse, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(clusterNetworkTraffic, Set.of(segmentation));

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)

                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, BURST_GUARANTEE_CPU, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))

                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))

                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_FLOW, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))

                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, GPU, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))

                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, HDD, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))
                        .changes(YT, CLUSTER_NETWORK_TRAFFIC, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.MIBPS))

                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YT, CPU_CLICKHOUSE, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))

                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, RPC_PROXY, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))

                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, SSD, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))
                        .changes(YT, TABLET_CELL_BUNDLE, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.COUNT))

                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YT, TABLET_STATIC_MEMORY, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(154, changes.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElseThrow()), DiQuotaChangeRequest.Change::getOwningCost));
        // cpu burst
        Assertions.assertEquals(new BigDecimal("24.872").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("24.872").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("24.872").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("24.872").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("24.872").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("24.872").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(BURST_GUARANTEE_CPU, zeno.getPublicKey())));
        // cpu
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, zeno.getPublicKey())));
        // relaxed cpu
        Assertions.assertEquals(new BigDecimal("99.489").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("99.489").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("99.489").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("99.489").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("99.489").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("99.489").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_FLOW, zeno.getPublicKey())));
        // gpu
        Assertions.assertEquals(new BigDecimal("37807").add(new BigDecimal("16203"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("37807").add(new BigDecimal("16203"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(GPU, zeno.getPublicKey())));
        // hdd
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, zeno.getPublicKey())));
        // ssd
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, zeno.getPublicKey())));
        // cpu clickhouse
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU_CLICKHOUSE, zeno.getPublicKey())));
        // cluster network traffic
        Assertions.assertEquals(new BigDecimal("1711.85").multiply(new BigDecimal(100L).divide(new BigDecimal(50, MATH_CONTEXT), MATH_CONTEXT).setScale(0, RoundingMode.UP), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(Tuple2.tuple(CLUSTER_NETWORK_TRAFFIC, zeno.getPublicKey())));
        // rpc
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("0").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("0").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("0").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("0").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("0").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("29.693").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal("13"), MATH_CONTEXT).add(new BigDecimal("0").multiply(new BigDecimal("20"), MATH_CONTEXT), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(RPC_PROXY, zeno.getPublicKey())));
        // TABLET_CELL_BUNDLE
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1711.85").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1711.85").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1711.85").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1711.85").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("679.313").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1711.85").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_CELL_BUNDLE, zeno.getPublicKey())));
        // TABLET_STATIC_MEMORY
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("29.693").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(TABLET_STATIC_MEMORY, zeno.getPublicKey())));
    }

    private void prepareCampaign2020() {
        campaignDao.clear();
        botCampaignGroupDao.clear();

        LocalDate date = LocalDate.of(2020, Month.AUGUST, 1);
        Campaign campaign = campaignDao.insertForTest(defaultCampaignBuilder(bigOrderTwo)
                .setKey("aug2020")
                .setName("aug2020")
                .setId(100L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderTwo.getId(), date)))
                .build());

        botCampaignGroupDao.create(
                defaultCampaignGroupBuilder()
                        .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrderTwo.getId())))
                        .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values().iterator().next())
                        .build()
        );
    }

    @Test
    public void logbroker2020OwningCostFormulaTest() {
        prepareCampaign2020();

        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(LOGBROKER)
                .withName("Logbroker")
                .withAbcServiceId(LOGBROKER_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String dataflow = "data_flow";
        Resource dataflowResource = resourceDao.create(new Resource.Builder(dataflow, service).name(dataflow)
                .type(DiResourceType.TRAFFIC).build());
        updateHierarchy();
        Segmentation location = segmentationDao.create(new Segmentation.Builder("logbroker").name("logbroker")
                .description("logbroker").priority(1).build());
        updateHierarchy();
        Segment lbkx = segmentDao.create(new Segment.Builder("lbkx", location).priority((short) 1)
                .name("lbkx").description("lbkx").build());
        Segment logbrokerSAS = segmentDao.create(new Segment.Builder("logbroker_SAS", location).priority((short) 1)
                .name("logbroker_SAS").description("logbroker_SAS").build());
        Segment logbrokerVLA = segmentDao.create(new Segment.Builder("logbroker_VLA", location).priority((short) 1)
                .name("logbroker_VLA").description("logbroker_VLA").build());
        Segment logbrokerMAN = segmentDao.create(new Segment.Builder("logbroker_MAN", location).priority((short) 1)
                .name("logbroker_MAN").description("logbroker_MAN").build());
        Segment logbrokerMYT = segmentDao.create(new Segment.Builder("logbroker_MYT", location).priority((short) 1)
                .name("logbroker_MYT").description("logbroker_MYT").build());
        Segment logbrokerIVA = segmentDao.create(new Segment.Builder("logbroker_IVA", location).priority((short) 1)
                .name("logbroker_IVA").description("logbroker_IVA").build());
        Segment logbrokerPrestableSAS = segmentDao.create(new Segment.Builder("logbroker-prestable_SAS", location)
                .priority((short) 1).name("logbroker-prestable_SAS").description("logbroker-prestable_SAS").build());
        Segment logbrokerPrestableVLA = segmentDao.create(new Segment.Builder("logbroker-prestable_VLA", location)
                .priority((short) 1).name("logbroker-prestable_VLA").description("logbroker-prestable_VLA").build());
        Segment logbrokerPrestableMAN = segmentDao.create(new Segment.Builder("logbroker-prestable_MAN", location)
                .priority((short) 1).name("logbroker-prestable_MAN").description("logbroker-prestable_MAN").build());
        Segment logbrokerPrestableMYT = segmentDao.create(new Segment.Builder("logbroker-prestable_MYT", location)
                .priority((short) 1).name("logbroker-prestable_MYT").description("logbroker-prestable_MYT").build());
        Segment logbrokerPrestableIVA = segmentDao.create(new Segment.Builder("logbroker-prestable_IVA", location)
                .priority((short) 1).name("logbroker-prestable_IVA").description("logbroker-prestable_IVA").build());
        Segment logbrokerYC = segmentDao.create(new Segment.Builder("logbroker-yc", location).priority((short) 1)
                .name("logbroker-yc").description("logbroker-yc").build());
        Segment logbrokerPreprodYC = segmentDao.create(new Segment.Builder("logbroker-yc-preprod", location)
                .priority((short) 1).name("logbroker-yc-preprod").description("logbroker-yc-preprod").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(dataflowResource, Set.of(location));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(lbkx.getPublicKey()), DiAmount.of(1L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerSAS.getPublicKey()), DiAmount.of(2L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerVLA.getPublicKey()), DiAmount.of(3L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerMAN.getPublicKey()), DiAmount.of(4L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerMYT.getPublicKey()), DiAmount.of(5L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerIVA.getPublicKey()), DiAmount.of(6L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableSAS.getPublicKey()), DiAmount.of(7L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableVLA.getPublicKey()), DiAmount.of(8L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableMAN.getPublicKey()), DiAmount.of(9L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableMYT.getPublicKey()), DiAmount.of(10L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableIVA.getPublicKey()), DiAmount.of(11L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerYC.getPublicKey()), DiAmount.of(12L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPreprodYC.getPublicKey()), DiAmount.of(13L, DiUnit.MBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(13, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateLogbroker2020(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(13, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateLogbroker2020(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(lbkx.getPublicKey()), DiAmount.of(10L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerSAS.getPublicKey()), DiAmount.of(20L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerVLA.getPublicKey()), DiAmount.of(30L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerMAN.getPublicKey()), DiAmount.of(40L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerMYT.getPublicKey()), DiAmount.of(50L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerIVA.getPublicKey()), DiAmount.of(60L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableSAS.getPublicKey()), DiAmount.of(70L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableVLA.getPublicKey()), DiAmount.of(80L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableMAN.getPublicKey()), DiAmount.of(90L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableMYT.getPublicKey()), DiAmount.of(100L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPrestableIVA.getPublicKey()), DiAmount.of(110L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerYC.getPublicKey()), DiAmount.of(120L, DiUnit.MBPS))
                        .changes(LOGBROKER, dataflow, bigOrderTwo.getId(), Set.of(logbrokerPreprodYC.getPublicKey()), DiAmount.of(130L, DiUnit.MBPS))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(13, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        Assertions.assertEquals(new BigDecimal("1735.17").multiply(toMibps(new BigDecimal(10L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of("data_flow", "lbkx")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(20L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of("data_flow", "logbroker_SAS")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(30L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of("data_flow", "logbroker_VLA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(40L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of("data_flow", "logbroker_MAN")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(50L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of("data_flow", "logbroker_MYT")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(60L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of("data_flow", "logbroker_IVA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(70L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of("data_flow", "logbroker-prestable_SAS")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(80L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of("data_flow", "logbroker-prestable_VLA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(90L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of("data_flow", "logbroker-prestable_MAN")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(100L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of("data_flow", "logbroker-prestable_MYT")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(110L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of("data_flow", "logbroker-prestable_IVA")));
        Assertions.assertEquals(new BigDecimal("0").multiply(toMibps(new BigDecimal(120L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey3.get(Pair.of("data_flow", "logbroker-yc")));
        Assertions.assertEquals(new BigDecimal("0").multiply(toMibps(new BigDecimal(130L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-yc-preprod")));
    }

    private void validateLogbroker2020(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("1735.17").multiply(toMibps(new BigDecimal(1L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "lbkx")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(2L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_SAS")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(3L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_VLA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(4L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_MAN")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(5L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_MYT")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(6L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker_IVA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(7L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_SAS")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(8L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_VLA")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(9L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_MAN")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(10L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_MYT")));
        Assertions.assertEquals(new BigDecimal("542.24").multiply(toMibps(new BigDecimal(11L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-prestable_IVA")));
        Assertions.assertEquals(new BigDecimal("0").multiply(toMibps(new BigDecimal(12L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-yc")));
        Assertions.assertEquals(new BigDecimal("0").multiply(toMibps(new BigDecimal(13L)), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("data_flow", "logbroker-yc-preprod")));
    }

    @Test
    public void Logfeller2020OwningCostFormulaTest() {
        prepareCampaign2020();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String CPU = "cpu";
        String HDD = "hdd";
        String SSD = "ssd";

        Service service = serviceDao.create(Service.withKey(LOGFELLER).withName(LOGFELLER).withAbcServiceId(5656).withPriority(190).build());

        updateHierarchy();

        Resource cpu =
                resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        Resource hdd =
                resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        Resource ssd =
                resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());

        Segmentation segmentation = segmentationDao.create(new Segmentation.Builder(ytClusterSegmentationKey)
                .name("Кластер YT")
                .description("Кластер YT")
                .priority(0)
                .build());

        updateHierarchy();

        Segment hahn = segmentDao.create(new Segment.Builder("hahn", segmentation).priority((short) 1).name("hahn").description("abc").build());
        Segment arnold = segmentDao.create(new Segment.Builder("arnold", segmentation).priority((short) 1).name("arnold").description("abc").build());
        Segment seneca_sas = segmentDao.create(new Segment.Builder("seneca-sas", segmentation).priority((short) 1).name("seneca-sas").description("abc").build());
        Segment seneca_vla = segmentDao.create(new Segment.Builder("seneca-vla", segmentation).priority((short) 1).name("seneca-vla").description("abc").build());
        Segment seneca_man = segmentDao.create(new Segment.Builder("seneca-man", segmentation).priority((short) 1).name("seneca-man").description("abc").build());
        Segment freud = segmentDao.create(new Segment.Builder("freud", segmentation).priority((short) 1).name("freud").description("abc").build());
        Segment hume = segmentDao.create(new Segment.Builder("hume", segmentation).priority((short) 1).name("hume").description("abc").build());
        Segment landau = segmentDao.create(new Segment.Builder("landau", segmentation).priority((short) 1).name("landau").description("abc").build());
        Segment bohr = segmentDao.create(new Segment.Builder("bohr", segmentation).priority((short) 1).name("bohr").description("abc").build());
        Segment zeno = segmentDao.create(new Segment.Builder("zeno", segmentation).priority((short) 1).name("zeno").description("abc").build());
        Segment locke = segmentDao.create(new Segment.Builder("locke", segmentation).priority((short) 1).name("locke").description("abc").build());
        Segment markov = segmentDao.create(new Segment.Builder("markov", segmentation).priority((short) 1).name("markov").description("abc").build());
        Segment vanga = segmentDao.create(new Segment.Builder("vanga", segmentation).priority((short) 1).name("vanga").description("abc").build());
        Segment pythia = segmentDao.create(new Segment.Builder("pythia", segmentation).priority((short) 1).name("pythia").description("abc").build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(hdd, Set.of(segmentation));
        resourceSegmentationDao.setSegmentations(ssd, Set.of(segmentation));

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)

                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(LOGFELLER, CPU, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))

                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, HDD, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(hahn.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(arnold.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(seneca_sas.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(seneca_vla.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(seneca_man.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(freud.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(hume.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(landau.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(bohr.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(zeno.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(locke.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(markov.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(vanga.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(LOGFELLER, SSD, bigOrderTwo.getId(), Set.of(pythia.getPublicKey()), DiAmount.of(100L, DiUnit.GIBIBYTE))

                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(42, changes.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElseThrow()), DiQuotaChangeRequest.Change::getOwningCost));
        // cpu
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("124.361").add(new BigDecimal("53.297"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").add(new BigDecimal("0"), MATH_CONTEXT).multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(CPU, zeno.getPublicKey())));
        // hdd
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0.115").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(HDD, zeno.getPublicKey())));
        // ssd
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, arnold.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, bohr.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, freud.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, hahn.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, hume.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, landau.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("0").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, locke.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, markov.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, pythia.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, seneca_man.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, seneca_sas.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, seneca_vla.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, vanga.getPublicKey())));
        Assertions.assertEquals(new BigDecimal("1.566").multiply(new BigDecimal(100L), MATH_CONTEXT).setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(SSD, zeno.getPublicKey())));
    }

    @Test
    public void strmOwningCostFormulaTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        Service service = serviceDao.create(Service.withKey(STRM).withName(STRM).withAbcServiceId(5656).withPriority(190).build());
        updateHierarchy();

        String strm_outbound_external_traffic = "strm_outbound_external_traffic";
        String strm_simultaneous_live_casts = "strm_simultaneous_live_casts";
        resourceDao.create(new Resource.Builder(strm_outbound_external_traffic, service).name(strm_outbound_external_traffic).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(strm_simultaneous_live_casts, service).name(strm_simultaneous_live_casts).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(STRM, strm_outbound_external_traffic, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.TEBIBYTE))
                        .changes(STRM, strm_simultaneous_live_casts, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(2, changes.size());
        Map<String, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        extractedSTRM(strm_outbound_external_traffic, strm_simultaneous_live_casts, owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(2, changes2.size());
        Map<String, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        extractedSTRM(strm_outbound_external_traffic, strm_simultaneous_live_casts, owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(STRM, strm_outbound_external_traffic, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.TEBIBYTE))
                        .changes(STRM, strm_simultaneous_live_casts, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(2, changes2.size());
        Map<String, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        Assertions.assertEquals(new BigDecimal("107").multiply(new BigDecimal(200L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(strm_outbound_external_traffic));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey3.get(strm_simultaneous_live_casts));
    }

    private void extractedSTRM(String strm_outbound_external_traffic, String strm_simultaneous_live_casts,
                               Map<String, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("107").multiply(new BigDecimal(100L), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(strm_outbound_external_traffic));
        Assertions.assertEquals(new BigDecimal("0").toString(), owningCostByResourceKey.get(strm_simultaneous_live_casts));
    }

    @Test
    public void ydb2020OwningCostFormulaTest() {
        prepareCampaign2020();

        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(YDB)
                .withName("YDB")
                .withAbcServiceId(SAAS_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String ydb_ru_rps = "ydb_ru-rps";
        String ydb_ru_data_size = "ydb_ru-data_size";
        resourceDao.create(new Resource.Builder(ydb_ru_data_size, service).name(ydb_ru_data_size).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(ydb_ru_rps, service).name(ydb_ru_rps).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YDB, ydb_ru_data_size, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YDB, ydb_ru_rps, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(2, changes.size());
        Map<String, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        extractedYdb2020(ydb_ru_rps, ydb_ru_data_size, owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(2, changes2.size());
        Map<String, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        extractedYdb2020(ydb_ru_rps, ydb_ru_data_size, owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(YDB, ydb_ru_data_size, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(YDB, ydb_ru_rps, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(2, changes2.size());
        Map<String, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> k.getResource().getKey(), DiQuotaChangeRequest.Change::getOwningCost));
        Assertions.assertEquals(new BigDecimal("1.566", MATH_CONTEXT).multiply(new BigDecimal("1632", MATH_CONTEXT)
                        .multiply(new BigDecimal(2000L, MATH_CONTEXT)
                                .multiply(new BigDecimal("1024").pow(3, MATH_CONTEXT), MATH_CONTEXT)
                                .divide(new BigDecimal(10, MATH_CONTEXT).pow(9, MATH_CONTEXT), MATH_CONTEXT)
                                .divide(new BigDecimal("240", MATH_CONTEXT), MATH_CONTEXT)
                                .setScale(0, RoundingMode.UP), MATH_CONTEXT)
                        .multiply(new BigDecimal(10, MATH_CONTEXT).pow(9, MATH_CONTEXT), MATH_CONTEXT)
                        .divide(new BigDecimal("1024").pow(3, MATH_CONTEXT), MATH_CONTEXT), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(ydb_ru_data_size));

        Assertions.assertEquals(new BigDecimal("0.46875", MATH_CONTEXT)
                .multiply(new BigDecimal(200L, MATH_CONTEXT).divide(new BigDecimal("500", MATH_CONTEXT), MATH_CONTEXT).setScale(0, RoundingMode.UP), MATH_CONTEXT)
                .setScale(0, RoundingMode.UP)
                .max(new BigDecimal(3, MATH_CONTEXT))
                .multiply(new BigDecimal("12", MATH_CONTEXT), MATH_CONTEXT)
                .multiply(new BigDecimal("162.81", MATH_CONTEXT), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(ydb_ru_rps));
    }

    private void extractedYdb2020(String ydb_ru_rps, String ydb_ru_data_size, Map<String, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("1.566", MATH_CONTEXT).multiply(new BigDecimal("1632", MATH_CONTEXT)
                        .multiply(new BigDecimal(100L, MATH_CONTEXT)
                                .multiply(new BigDecimal("1024").pow(3, MATH_CONTEXT), MATH_CONTEXT)
                                .divide(new BigDecimal(10, MATH_CONTEXT).pow(9, MATH_CONTEXT), MATH_CONTEXT)
                                .divide(new BigDecimal("240", MATH_CONTEXT), MATH_CONTEXT)
                                .setScale(0, RoundingMode.UP), MATH_CONTEXT)
                        .multiply(new BigDecimal(10, MATH_CONTEXT).pow(9, MATH_CONTEXT), MATH_CONTEXT)
                        .divide(new BigDecimal("1024").pow(3, MATH_CONTEXT), MATH_CONTEXT), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(ydb_ru_data_size));

        Assertions.assertEquals(new BigDecimal("0.46875", MATH_CONTEXT).multiply(
                        new BigDecimal(100L, MATH_CONTEXT).divide(new BigDecimal("500", MATH_CONTEXT), MATH_CONTEXT).setScale(0, RoundingMode.UP),
                        MATH_CONTEXT)
                .setScale(0, RoundingMode.UP)
                .max(new BigDecimal(3, MATH_CONTEXT))
                .multiply(new BigDecimal("12", MATH_CONTEXT), MATH_CONTEXT)
                .multiply(new BigDecimal("162.81", MATH_CONTEXT), MATH_CONTEXT)
                .setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(ydb_ru_rps));
    }

    @Test
    public void sandbox2021CpuDominatesOwningCostFormulaTest() {
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, cpuLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(400L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(500L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(600L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(700L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(7, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021CpuDominates(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(7, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021CpuDominates(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, cpuLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(4000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(5000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(6000L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(7000L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(7, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021CpuDominatesUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2021CpuDominates(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(300L), BigDecimal.valueOf(400L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(100L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(500L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    private void validateSandbox2021CpuDominatesUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(3000L), BigDecimal.valueOf(4000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(1000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(5000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    private BigDecimal combinedSegmentsCost(BigDecimal ssdGib, BigDecimal hddGib, BigDecimal hddSegmentPrice,
                                            BigDecimal ssdSegmentPrice, BigDecimal computeCores) {
        BigDecimal ssdBytesPerLinuxCore = new BigDecimal("11.7", MATH_CONTEXT)
                .multiply(BigDecimal.valueOf(10L).pow(9, MATH_CONTEXT), MATH_CONTEXT);
        BigDecimal hddBytesPerLinuxCore = new BigDecimal("250", MATH_CONTEXT)
                .multiply(BigDecimal.valueOf(10L).pow(9, MATH_CONTEXT), MATH_CONTEXT);
        BigDecimal coresForHddBytes = hddGib.multiply(BigDecimal.valueOf(1024).pow(3, MATH_CONTEXT), MATH_CONTEXT)
                .divide(hddBytesPerLinuxCore, MATH_CONTEXT).setScale(0, RoundingMode.UP);
        BigDecimal coresForSsdBytes = ssdGib.multiply(BigDecimal.valueOf(1024).pow(3, MATH_CONTEXT), MATH_CONTEXT)
                .divide(ssdBytesPerLinuxCore, MATH_CONTEXT).setScale(0, RoundingMode.UP);
        BigDecimal hddShare;
        BigDecimal ssdShare;
        if (coresForHddBytes.compareTo(BigDecimal.ZERO) == 0 && coresForSsdBytes.compareTo(BigDecimal.ZERO) == 0) {
            hddShare = new BigDecimal("0.0", MATH_CONTEXT);
            ssdShare = new BigDecimal("1.0", MATH_CONTEXT);
        } else {
            hddShare = coresForHddBytes.divide(coresForHddBytes.add(coresForSsdBytes, MATH_CONTEXT), MATH_CONTEXT);
            ssdShare = coresForSsdBytes.divide(coresForHddBytes.add(coresForSsdBytes, MATH_CONTEXT), MATH_CONTEXT);
        }
        return computeCores.multiply(hddShare, MATH_CONTEXT).multiply(hddSegmentPrice, MATH_CONTEXT)
                .add(computeCores.multiply(ssdShare, MATH_CONTEXT).multiply(ssdSegmentPrice, MATH_CONTEXT),
                        MATH_CONTEXT);
    }

    @Test
    public void sandbox2021CpuDominatesNoStorageOwningCostFormulaTest() {
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, cpuLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(500L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(600L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(700L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(5, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021CpuDominatesNoStorage(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(5, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021CpuDominatesNoStorage(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, cpuLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(5000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(6000L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(7000L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(5, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021CpuDominatesNoStorageUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2021CpuDominatesNoStorage(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(0L), BigDecimal.valueOf(0L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(100L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(500L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    private void validateSandbox2021CpuDominatesNoStorageUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(0L), BigDecimal.valueOf(0L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(1000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(5000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    @Test
    public void sandbox2021RamDominatesOwningCostFormulaTest() {
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, cpuLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(800L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(400L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(500L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(600L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(700L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(7, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021RamDominates(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(7, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021RamDominates(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, cpuLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(8000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(4000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(5000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(6000L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(7000L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(7, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021RamDominatesUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2021RamDominates(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(300L), BigDecimal.valueOf(400L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(200L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(500L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    private void validateSandbox2021RamDominatesUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(3000L), BigDecimal.valueOf(4000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(2000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(5000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    @Test
    public void sandbox2021RamDominatesNoStorageOwningCostFormulaTest() {
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, cpuLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(800L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(500L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(600L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(700L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(5, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021RamDominatesNoStorage(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(5, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021RamDominatesNoStorage(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, cpuLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(8000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(5000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(6000L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(7000L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(5, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021RamDominatesNoStorageUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2021RamDominatesNoStorage(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(0L), BigDecimal.valueOf(0L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(200L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(500L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    private void validateSandbox2021RamDominatesNoStorageUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(0L), BigDecimal.valueOf(0L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(2000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(5000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    @Test
    public void sandbox2021RamDominatesNoCpuOwningCostFormulaTest() {
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(800L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(400L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(500L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(600L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(700L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(6, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021RamDominatesNoCpu(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(6, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021RamDominatesNoCpu(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(8000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(4000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(5000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(6000L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(7000L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(6, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021RamDominatesNoCpuUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2021RamDominatesNoCpu(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(300L), BigDecimal.valueOf(400L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(200L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(500L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    private void validateSandbox2021RamDominatesNoCpuUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(3000L), BigDecimal.valueOf(4000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(2000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(5000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    @Test
    public void sandbox2021HddDominatesOwningCostFormulaTest() {
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, cpuLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(40000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(500L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(600L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(700L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(7, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021HddDominates(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(7, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021HddDominates(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, cpuLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(400000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(5000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(6000L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(7000L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(7, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021HddDominatesUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2021HddDominates(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(sumSegmentsCost(BigDecimal.valueOf(300L), BigDecimal.valueOf(40000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(500L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    private void validateSandbox2021HddDominatesUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(sumSegmentsCost(BigDecimal.valueOf(3000L), BigDecimal.valueOf(400000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(5000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    private BigDecimal sumSegmentsCost(BigDecimal ssdGib, BigDecimal hddGib, BigDecimal hddSegmentPrice,
                                       BigDecimal ssdSegmentPrice) {
        BigDecimal ssdBytesPerLinuxCore = new BigDecimal("11.7", MATH_CONTEXT)
                .multiply(BigDecimal.valueOf(10L).pow(9, MATH_CONTEXT), MATH_CONTEXT);
        BigDecimal hddBytesPerLinuxCore = new BigDecimal("250", MATH_CONTEXT)
                .multiply(BigDecimal.valueOf(10L).pow(9, MATH_CONTEXT), MATH_CONTEXT);
        BigDecimal coresForHddBytes = hddGib.multiply(BigDecimal.valueOf(1024).pow(3, MATH_CONTEXT), MATH_CONTEXT)
                .divide(hddBytesPerLinuxCore, MATH_CONTEXT).setScale(0, RoundingMode.UP);
        BigDecimal coresForSsdBytes = ssdGib.multiply(BigDecimal.valueOf(1024).pow(3, MATH_CONTEXT), MATH_CONTEXT)
                .divide(ssdBytesPerLinuxCore, MATH_CONTEXT).setScale(0, RoundingMode.UP);
        return coresForHddBytes.multiply(hddSegmentPrice, MATH_CONTEXT)
                .add(coresForSsdBytes.multiply(ssdSegmentPrice, MATH_CONTEXT), MATH_CONTEXT);
    }

    @Test
    public void sandbox2021SsdDominatesOwningCostFormulaTest() {
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, cpuLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(30000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(400L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(500L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(600L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(700L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(7, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021SsdDominates(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(7, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021SsdDominates(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, cpuLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(300000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(4000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(5000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(6000L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(7000L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(7, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021SsdDominatesUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2021SsdDominates(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(sumSegmentsCost(BigDecimal.valueOf(30000L), BigDecimal.valueOf(400L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(500L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    private void validateSandbox2021SsdDominatesUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(sumSegmentsCost(BigDecimal.valueOf(300000L), BigDecimal.valueOf(4000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(5000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    @Test
    public void sandbox2021HddDominatesNoCpuOwningCostFormulaTest() {
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(40000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(500L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(600L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(700L, DiUnit.COUNT))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(6, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021HddDominatesNoCpu(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(6, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021HddDominatesNoCpu(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, ramLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddLinux, bigOrderTwo.getId(), Set.of(), DiAmount.of(400000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, s3Storage, bigOrderTwo.getId(), Set.of(), DiAmount.of(5000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, macMini, bigOrderTwo.getId(), Set.of(), DiAmount.of(6000L, DiUnit.COUNT))
                        .changes(SANDBOX, windows, bigOrderTwo.getId(), Set.of(), DiAmount.of(7000L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(6, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2021HddDominatesNoCpuUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2021HddDominatesNoCpu(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(hddSegmentCost(BigDecimal.valueOf(40000L), new BigDecimal("350.52"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("hdd_linux", null)));
        Assertions.assertEquals(ssdSegmentCost(BigDecimal.valueOf(300L), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("ssd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(500L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    private void validateSandbox2021HddDominatesNoCpuUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_linux", null)));
        Assertions.assertEquals(hddSegmentCost(BigDecimal.valueOf(400000L), new BigDecimal("350.52"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("hdd_linux", null)));
        Assertions.assertEquals(ssdSegmentCost(BigDecimal.valueOf(3000L), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("ssd_linux", null)));
        Assertions.assertEquals(new BigDecimal("0.5235").multiply(new BigDecimal(5000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("s3_storage", null)));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("mac_mini", null)));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("windows", null)));
    }

    private BigDecimal hddSegmentCost(BigDecimal hddGib, BigDecimal hddSegmentPrice) {
        BigDecimal hddBytesPerLinuxCore = new BigDecimal("250", MATH_CONTEXT)
                .multiply(BigDecimal.valueOf(10L).pow(9, MATH_CONTEXT), MATH_CONTEXT);
        BigDecimal coresForHddBytes = hddGib.multiply(BigDecimal.valueOf(1024).pow(3, MATH_CONTEXT), MATH_CONTEXT)
                .divide(hddBytesPerLinuxCore, MATH_CONTEXT).setScale(0, RoundingMode.UP);
        return coresForHddBytes.multiply(hddSegmentPrice, MATH_CONTEXT);
    }

    private BigDecimal ssdSegmentCost(BigDecimal ssdGib, BigDecimal ssdSegmentPrice) {
        BigDecimal ssdBytesPerLinuxCore = new BigDecimal("11.7", MATH_CONTEXT)
                .multiply(BigDecimal.valueOf(10L).pow(9, MATH_CONTEXT), MATH_CONTEXT);
        BigDecimal coresForSsdBytes = ssdGib.multiply(BigDecimal.valueOf(1024).pow(3, MATH_CONTEXT), MATH_CONTEXT)
                .divide(ssdBytesPerLinuxCore, MATH_CONTEXT).setScale(0, RoundingMode.UP);
        return coresForSsdBytes.multiply(ssdSegmentPrice, MATH_CONTEXT);
    }

    @Test
    public void sandbox2020CpuDominatesOwningCostFormulaTest() {
        prepareCampaign2020();
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(400L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(400L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(4800L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(11200L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(10, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020CpuDominates(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(10, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020CpuDominates(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(4000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(4000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(48000L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(112000L, DiUnit.CORES))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(10, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020CpuDominatesUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2020CpuDominates(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(300L), BigDecimal.valueOf(400L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(100L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(300L), BigDecimal.valueOf(400L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(100L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    private void validateSandbox2020CpuDominatesUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(3000L), BigDecimal.valueOf(4000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(1000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(3000L), BigDecimal.valueOf(4000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(1000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    @Test
    public void sandbox2020CpuDominatesNoStorageOwningCostFormulaTest() {
        prepareCampaign2020();
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(4800L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(11200L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(6, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020CpuDominatesNoStorage(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(6, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020CpuDominatesNoStorage(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(48000L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(112000L, DiUnit.CORES))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(6, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020CpuDominatesNoStorageUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2020CpuDominatesNoStorage(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(0L), BigDecimal.valueOf(0L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(100L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(0L), BigDecimal.valueOf(0L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(100L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    private void validateSandbox2020CpuDominatesNoStorageUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(0L), BigDecimal.valueOf(0L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(1000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(0L), BigDecimal.valueOf(0L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(1000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    @Test
    public void sandbox2020RamDominatesOwningCostFormulaTest() {
        prepareCampaign2020();
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(800L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(400L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(800L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(400L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(4800L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(11200L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(10, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020RamDominates(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(10, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020RamDominates(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(8000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(4000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(8000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(4000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(48000L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(112000L, DiUnit.CORES))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(10, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020RamDominatesUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2020RamDominates(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(300L), BigDecimal.valueOf(400L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(200L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(300L), BigDecimal.valueOf(400L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(200L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    private void validateSandbox2020RamDominatesUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(3000L), BigDecimal.valueOf(4000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(2000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(3000L), BigDecimal.valueOf(4000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(2000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    @Test
    public void sandbox2020RamDominatesNoStorageOwningCostFormulaTest() {
        prepareCampaign2020();
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(800L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(800L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(4800L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(11200L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(6, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020RamDominatesNoStorage(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(6, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020RamDominatesNoStorage(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(8000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(8000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(48000L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(112000L, DiUnit.CORES))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(6, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020RamDominatesNoStorageUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2020RamDominatesNoStorage(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(0L), BigDecimal.valueOf(0L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(200L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(0L), BigDecimal.valueOf(0L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(200L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    private void validateSandbox2020RamDominatesNoStorageUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(0L), BigDecimal.valueOf(0L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(2000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(0L), BigDecimal.valueOf(0L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(2000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    @Test
    public void sandbox2020RamDominatesNoCpuOwningCostFormulaTest() {
        prepareCampaign2020();
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(800L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(400L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(800L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(400L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(4800L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(11200L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(8, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020RamDominatesNoCpu(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(8, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020RamDominatesNoCpu(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(8000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(4000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(8000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(4000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(48000L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(112000L, DiUnit.CORES))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(8, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020RamDominatesNoCpuUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2020RamDominatesNoCpu(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(300L), BigDecimal.valueOf(400L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(200L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(300L), BigDecimal.valueOf(400L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(200L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    private void validateSandbox2020RamDominatesNoCpuUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(3000L), BigDecimal.valueOf(4000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(2000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(combinedSegmentsCost(BigDecimal.valueOf(3000L), BigDecimal.valueOf(4000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"), BigDecimal.valueOf(2000L))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    @Test
    public void sandbox2020HddDominatesOwningCostFormulaTest() {
        prepareCampaign2020();
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(40000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(40000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(4800L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(11200L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(10, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020HddDominates(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(10, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020HddDominates(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(400000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(400000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(48000L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(112000L, DiUnit.CORES))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(10, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020HddDominatesUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2020HddDominates(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(sumSegmentsCost(BigDecimal.valueOf(300L), BigDecimal.valueOf(40000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(sumSegmentsCost(BigDecimal.valueOf(300L), BigDecimal.valueOf(40000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    private void validateSandbox2020HddDominatesUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(sumSegmentsCost(BigDecimal.valueOf(3000L), BigDecimal.valueOf(400000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(sumSegmentsCost(BigDecimal.valueOf(3000L), BigDecimal.valueOf(400000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    @Test
    public void sandbox2020SsdDominatesOwningCostFormulaTest() {
        prepareCampaign2020();
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(30000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(400L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(100L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(30000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(400L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(4800L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(11200L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(10, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020SsdDominates(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(10, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020SsdDominates(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(300000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(4000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(300000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(4000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(48000L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(112000L, DiUnit.CORES))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(10, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020SsdDominatesUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2020SsdDominates(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(sumSegmentsCost(BigDecimal.valueOf(30000L), BigDecimal.valueOf(400L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(sumSegmentsCost(BigDecimal.valueOf(30000L), BigDecimal.valueOf(400L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    private void validateSandbox2020SsdDominatesUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(sumSegmentsCost(BigDecimal.valueOf(300000L), BigDecimal.valueOf(4000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(sumSegmentsCost(BigDecimal.valueOf(300000L), BigDecimal.valueOf(4000L),
                        new BigDecimal("350.52"), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    @Test
    public void sandbox2020HddDominatesNoCpuOwningCostFormulaTest() {
        prepareCampaign2020();
        final Service.Settings settings = Service.Settings.builder()
                .accountActualValuesInQuotaDistribution(true)
                .requireZeroQuotaUsageForProjectDeletion(true)
                .build();
        final Service service = serviceDao.create(Service.withKey(SANDBOX)
                .withName("Sandbox")
                .withAbcServiceId(SANDBOX_ABC_SERVICE_ID)
                .withSettings(settings)
                .build());
        final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
        serviceDao.attachAdmin(service, slonnn);
        serviceDao.attachAdmin(service, personDao.readPersonByLogin(ZOMB_MOBSEARCH));
        updateHierarchy();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String cpuSegmented = "cpu_segmented";
        String ramSegmented = "ram_segmented";
        String ssdSegmented = "ssd_segmented";
        String hddSegmented = "hdd_segmented";
        String cpuLinux = "cpu_linux";
        String ramLinux = "ram_linux";
        String ssdLinux = "ssd_linux";
        String hddLinux = "hdd_linux";
        String s3Storage = "s3_storage";
        String macMini = "mac_mini";
        String windows = "windows";
        Resource cpuSegmentedResource = resourceDao.create(new Resource.Builder(cpuSegmented, service)
                .name(cpuSegmented).type(DiResourceType.PROCESSOR).build());
        Resource ramSegmentedResource = resourceDao.create(new Resource.Builder(ramSegmented, service)
                .name(ramSegmented).type(DiResourceType.MEMORY).build());
        Resource ssdSegmentedResource = resourceDao.create(new Resource.Builder(ssdSegmented, service)
                .name(ssdSegmented).type(DiResourceType.STORAGE).build());
        Resource hddSegmentedResource = resourceDao.create(new Resource.Builder(hddSegmented, service)
                .name(hddSegmented).type(DiResourceType.STORAGE).build());
        Resource cpuLinuxResource = resourceDao.create(new Resource.Builder(cpuLinux, service)
                .name(cpuLinux).type(DiResourceType.PROCESSOR).build());
        Resource ramLinuxResource = resourceDao.create(new Resource.Builder(ramLinux, service)
                .name(ramLinux).type(DiResourceType.MEMORY).build());
        Resource ssdLinuxResource = resourceDao.create(new Resource.Builder(ssdLinux, service)
                .name(ssdLinux).type(DiResourceType.STORAGE).build());
        Resource hddLinuxResource = resourceDao.create(new Resource.Builder(hddLinux, service)
                .name(hddLinux).type(DiResourceType.STORAGE).build());
        Resource s3StorageResource = resourceDao.create(new Resource.Builder(s3Storage, service)
                .name(s3Storage).type(DiResourceType.STORAGE).build());
        Resource macMiniResource = resourceDao.create(new Resource.Builder(macMini, service)
                .name(macMini).type(DiResourceType.ENUMERABLE).build());
        Resource windowsResource = resourceDao.create(new Resource.Builder(windows, service)
                .name(windows).type(DiResourceType.ENUMERABLE).build());
        updateHierarchy();
        Segmentation sandboxSegmentation = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox type").description("sandbox type").priority(1).build());
        updateHierarchy();
        Segment linuxBareMetalSegment = segmentDao
                .create(new Segment.Builder("sandbox_linux_bare_metal", sandboxSegmentation)
                        .priority((short) 1).name("sandbox_linux_bare_metal").description("sandbox_linux_bare_metal").build());
        Segment linuxYpSegment = segmentDao.create(new Segment.Builder("sandbox_linux_yp", sandboxSegmentation)
                .priority((short) 1).name("sandbox_linux_yp").description("sandbox_linux_yp").build());
        Segment windowsSegment = segmentDao.create(new Segment.Builder("sandbox_windows", sandboxSegmentation)
                .priority((short) 1).name("sandbox_windows").description("sandbox_windows").build());
        Segment macMiniSegment = segmentDao.create(new Segment.Builder("sandbox_mac_mini", sandboxSegmentation)
                .priority((short) 1).name("sandbox_mac_mini").description("sandbox_mac_mini").build());
        updateHierarchy();
        resourceSegmentationDao.setSegmentations(cpuSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ramSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(ssdSegmentedResource, Set.of(sandboxSegmentation));
        resourceSegmentationDao.setSegmentations(hddSegmentedResource, Set.of(sandboxSegmentation));
        updateHierarchy();
        prepareCampaignResources();
        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(40000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(300L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(40000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(4800L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(11200L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(8, changes.size());
        Map<Pair<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020HddDominatesNoCpu(owningCostByResourceKey);
        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest2);
        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(8, changes2.size());
        Map<Pair<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020HddDominatesNoCpu(owningCostByResourceKey2);
        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxBareMetalSegment.getPublicKey()), DiAmount.of(400000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ramSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(2000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, ssdSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(3000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, hddSegmented, bigOrderTwo.getId(), Set.of(linuxYpSegment.getPublicKey()), DiAmount.of(400000L, DiUnit.GIBIBYTE))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(macMiniSegment.getPublicKey()), DiAmount.of(48000L, DiUnit.CORES))
                        .changes(SANDBOX, cpuSegmented, bigOrderTwo.getId(), Set.of(windowsSegment.getPublicKey()), DiAmount.of(112000L, DiUnit.CORES))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(8, changes3.size());
        Map<Pair<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Pair.of(k.getResource().getKey(),
                                k.getSegmentKeys().stream().findFirst().orElse(null)),
                        DiQuotaChangeRequest.Change::getOwningCost));
        validateSandbox2020HddDominatesNoCpuUpdated(owningCostByResourceKey3);
    }

    private void validateSandbox2020HddDominatesNoCpu(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(hddSegmentCost(BigDecimal.valueOf(40000L), new BigDecimal("350.52"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(ssdSegmentCost(BigDecimal.valueOf(300L), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(hddSegmentCost(BigDecimal.valueOf(40000L), new BigDecimal("350.52"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(ssdSegmentCost(BigDecimal.valueOf(300L), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(600L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(700L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    private void validateSandbox2020HddDominatesNoCpuUpdated(Map<Pair<String, String>, String> owningCostByResourceKey) {
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(hddSegmentCost(BigDecimal.valueOf(400000L), new BigDecimal("350.52"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(ssdSegmentCost(BigDecimal.valueOf(3000L), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_bare_metal")));
        Assertions.assertEquals(new BigDecimal("0").toString(),
                owningCostByResourceKey.get(Pair.of("ram_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(hddSegmentCost(BigDecimal.valueOf(400000L), new BigDecimal("350.52"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("hdd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(ssdSegmentCost(BigDecimal.valueOf(3000L), new BigDecimal("404.51"))
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("ssd_segmented", "sandbox_linux_yp")));
        Assertions.assertEquals(new BigDecimal("3236.08").multiply(new BigDecimal(6000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_mac_mini")));
        Assertions.assertEquals(new BigDecimal("6472.16").multiply(new BigDecimal(7000L), MATH_CONTEXT)
                        .setScale(0, RoundingMode.HALF_UP).toString(),
                owningCostByResourceKey.get(Pair.of("cpu_segmented", "sandbox_windows")));
    }

    @Test
    public void solomonOwningCostFormulaTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String METRICS_WRITE_FLOW_SEGMENTED = "metrics_write_flow_segmented";
        String METRICS_STORED_COUNT_SEGMENTED = "metrics_stored_count_segmented";
        String METRICS_STORED_COUNT = "metrics_stored_count";

        Service service = serviceDao.create(Service.withKey(SOLOMON).withName(SOLOMON).withAbcServiceId(5656).withPriority(190).build());

        updateHierarchy();

        Resource flowSegmented =
                resourceDao.create(new Resource.Builder(METRICS_WRITE_FLOW_SEGMENTED, service).name(METRICS_WRITE_FLOW_SEGMENTED).type(DiResourceType.ENUMERABLE).build());
        Resource metricsSegmented =
                resourceDao.create(new Resource.Builder(METRICS_STORED_COUNT_SEGMENTED, service).name(METRICS_STORED_COUNT_SEGMENTED).type(DiResourceType.ENUMERABLE).build());
        Resource metrics =
                resourceDao.create(new Resource.Builder(METRICS_STORED_COUNT, service).name(METRICS_STORED_COUNT).type(DiResourceType.ENUMERABLE).build());

        Segmentation location = segmentationDao.read(new Segmentation.Key(locationSegmentationKey));

        updateHierarchy();

        Segment vla = segmentDao.create(new Segment.Builder("VLA", location).priority((short) 1).name("VLA").description("abc").build());
        Segment sas = segmentDao.create(new Segment.Builder("SAS", location).priority((short) 1).name("SAS").description("abc").build());
        Segment iva = segmentDao.create(new Segment.Builder("IVA", location).priority((short) 1).name("IVA").description("abc").build());
        Segment myt = segmentDao.create(new Segment.Builder("MYT", location).priority((short) 1).name("MYT").description("abc").build());
        Segment man = segmentDao.create(new Segment.Builder("MAN", location).priority((short) 1).name("MAN").description("abc").build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(flowSegmented, Set.of(location));
        resourceSegmentationDao.setSegmentations(metricsSegmented, Set.of(location));

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)

                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(vla.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(sas.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(iva.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(myt.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(man.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))

                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(vla.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(sas.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(iva.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(myt.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(man.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))

                        .changes(SOLOMON, METRICS_STORED_COUNT, bigOrderTwo.getId(), Set.of(), DiAmount.of(1_000_000L, DiUnit.COUNT))

                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(11, changes.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        BigDecimal flow = new BigDecimal("100000", MATH_CONTEXT).multiply(new BigDecimal("5", MATH_CONTEXT), MATH_CONTEXT);
        BigDecimal metricsInRequest = new BigDecimal("1000000", MATH_CONTEXT);
        BigDecimal totalCores = new BigDecimal("2", MATH_CONTEXT).multiply(
                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT).add(
                                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                                        .max(metricsInRequest.divide(new BigDecimal("400000000", MATH_CONTEXT), MATH_CONTEXT))
                                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT)
                        ).setScale(0, RoundingMode.UP)
                , MATH_CONTEXT);
        BigDecimal totalGibRam = totalCores.multiply(new BigDecimal("8", MATH_CONTEXT), MATH_CONTEXT);
        BigDecimal cost = totalCores.multiply(new BigDecimal("162.81", MATH_CONTEXT), MATH_CONTEXT).add(
                totalGibRam.multiply(new BigDecimal("25.12", MATH_CONTEXT), MATH_CONTEXT)
        );
        BigDecimal weight = new BigDecimal("100000", MATH_CONTEXT).divide(flow, MATH_CONTEXT);
        BigDecimal locationCost = cost.multiply(weight, MATH_CONTEXT);

        // flow segmented
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, vla.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, sas.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, iva.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, myt.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, man.getPublicKey())));
        // metricsSegmented
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, vla.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, sas.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, iva.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, myt.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, man.getPublicKey())));
        // METRICS_STORED_COUNT
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT, null)));

        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(11, changes2.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        // flow segmented
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, vla.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, sas.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, iva.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, myt.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, man.getPublicKey())));
        // metricsSegmented
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, vla.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, sas.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, iva.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, myt.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, man.getPublicKey())));
        // METRICS_STORED_COUNT
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT, null)));

        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(vla.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(sas.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(iva.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(myt.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(man.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))

                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(vla.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(sas.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(iva.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(myt.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(man.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))

                        .changes(SOLOMON, METRICS_STORED_COUNT, bigOrderTwo.getId(), Set.of(), DiAmount.of(2_000_000L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(11, changes3.size());

        Map<Tuple2<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        flow = new BigDecimal("200000", MATH_CONTEXT).multiply(new BigDecimal("5", MATH_CONTEXT), MATH_CONTEXT);
        metricsInRequest = new BigDecimal("2000000", MATH_CONTEXT);
        totalCores = new BigDecimal("2", MATH_CONTEXT).multiply(
                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT).add(
                                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                                        .max(metricsInRequest.divide(new BigDecimal("400000000", MATH_CONTEXT), MATH_CONTEXT))
                                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT)
                        ).setScale(0, RoundingMode.UP)
                , MATH_CONTEXT);
        totalGibRam = totalCores.multiply(new BigDecimal("8", MATH_CONTEXT), MATH_CONTEXT);
        cost = totalCores.multiply(new BigDecimal("162.81", MATH_CONTEXT), MATH_CONTEXT).add(
                totalGibRam.multiply(new BigDecimal("25.12", MATH_CONTEXT), MATH_CONTEXT)
        );
        weight = new BigDecimal("200000", MATH_CONTEXT).divide(flow, MATH_CONTEXT);
        locationCost = cost.multiply(weight, MATH_CONTEXT);

        // flow segmented
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, vla.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, sas.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, iva.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, myt.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, man.getPublicKey())));
        // metricsSegmented
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, vla.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, sas.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, iva.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, myt.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, man.getPublicKey())));
        // METRICS_STORED_COUNT
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT, null)));
    }

    @Test
    public void solomon2020OwningCostFormulaTest() {
        prepareCampaign2020();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String METRICS_WRITE_FLOW = "metrics_write_flow";
        String METRICS_STORED_COUNT = "metrics_stored_count";

        Service service = serviceDao.create(Service.withKey(SOLOMON).withName(SOLOMON).withAbcServiceId(5656).withPriority(190).build());

        updateHierarchy();

        resourceDao.create(new Resource.Builder(METRICS_WRITE_FLOW, service).name(METRICS_WRITE_FLOW).type(DiResourceType.ENUMERABLE).build());
        resourceDao.create(new Resource.Builder(METRICS_STORED_COUNT, service).name(METRICS_STORED_COUNT).type(DiResourceType.ENUMERABLE).build());

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)

                        .changes(SOLOMON, METRICS_WRITE_FLOW, bigOrderTwo.getId(), Set.of(), DiAmount.of(100_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT, bigOrderTwo.getId(), Set.of(), DiAmount.of(1_000_000L, DiUnit.COUNT))

                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(2, changes.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        BigDecimal flow = new BigDecimal("100000", MATH_CONTEXT);
        BigDecimal metricsInRequest = new BigDecimal("1000000", MATH_CONTEXT);
        BigDecimal totalCores = new BigDecimal("2", MATH_CONTEXT).multiply(
                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT).add(
                                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                                        .max(metricsInRequest.divide(new BigDecimal("400000000", MATH_CONTEXT), MATH_CONTEXT))
                                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT)
                        ).setScale(0, RoundingMode.UP)
                , MATH_CONTEXT);
        BigDecimal totalGibRam = totalCores.multiply(new BigDecimal("8", MATH_CONTEXT), MATH_CONTEXT);
        BigDecimal cost = totalCores.multiply(new BigDecimal("162.81", MATH_CONTEXT), MATH_CONTEXT).add(
                totalGibRam.multiply(new BigDecimal("25.12", MATH_CONTEXT), MATH_CONTEXT)
        );

        // flow
        Assertions.assertEquals(cost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_WRITE_FLOW, null)));
        // METRICS_STORED_COUNT
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT, null)));

        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(2, changes2.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        // flow
        Assertions.assertEquals(cost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_WRITE_FLOW, null)));
        // METRICS_STORED_COUNT
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT, null)));

        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SOLOMON, METRICS_WRITE_FLOW, bigOrderTwo.getId(), Set.of(), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT, bigOrderTwo.getId(), Set.of(), DiAmount.of(2_000_000L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(2, changes3.size());

        Map<Tuple2<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        flow = new BigDecimal("200000", MATH_CONTEXT);
        metricsInRequest = new BigDecimal("2000000", MATH_CONTEXT);
        totalCores = new BigDecimal("2", MATH_CONTEXT).multiply(
                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT).add(
                                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                                        .max(metricsInRequest.divide(new BigDecimal("400000000", MATH_CONTEXT), MATH_CONTEXT))
                                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT)
                        ).setScale(0, RoundingMode.UP)
                , MATH_CONTEXT);
        totalGibRam = totalCores.multiply(new BigDecimal("8", MATH_CONTEXT), MATH_CONTEXT);
        cost = totalCores.multiply(new BigDecimal("162.81", MATH_CONTEXT), MATH_CONTEXT).add(
                totalGibRam.multiply(new BigDecimal("25.12", MATH_CONTEXT), MATH_CONTEXT)
        );

        // flow
        Assertions.assertEquals(cost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_WRITE_FLOW, null)));
        // METRICS_STORED_COUNT
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT, null)));
    }

    @Test
    public void solomonOwningCostFormulaFailOnZeroFlowTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String METRICS_WRITE_FLOW_SEGMENTED = "metrics_write_flow_segmented";
        String METRICS_STORED_COUNT_SEGMENTED = "metrics_stored_count_segmented";
        String METRICS_STORED_COUNT = "metrics_stored_count";

        Service service = serviceDao.create(Service.withKey(SOLOMON).withName(SOLOMON).withAbcServiceId(5656).withPriority(190).build());

        updateHierarchy();

        Resource flowSegmented =
                resourceDao.create(new Resource.Builder(METRICS_WRITE_FLOW_SEGMENTED, service).name(METRICS_WRITE_FLOW_SEGMENTED).type(DiResourceType.ENUMERABLE).build());
        Resource metricsSegmented =
                resourceDao.create(new Resource.Builder(METRICS_STORED_COUNT_SEGMENTED, service).name(METRICS_STORED_COUNT_SEGMENTED).type(DiResourceType.ENUMERABLE).build());
        Resource metrics =
                resourceDao.create(new Resource.Builder(METRICS_STORED_COUNT, service).name(METRICS_STORED_COUNT).type(DiResourceType.ENUMERABLE).build());

        Segmentation location = segmentationDao.read(new Segmentation.Key(locationSegmentationKey));

        updateHierarchy();

        Segment vla = segmentDao.create(new Segment.Builder("VLA", location).priority((short) 1).name("VLA").description("abc").build());
        Segment sas = segmentDao.create(new Segment.Builder("SAS", location).priority((short) 1).name("SAS").description("abc").build());
        Segment iva = segmentDao.create(new Segment.Builder("IVA", location).priority((short) 1).name("IVA").description("abc").build());
        Segment myt = segmentDao.create(new Segment.Builder("MYT", location).priority((short) 1).name("MYT").description("abc").build());
        Segment man = segmentDao.create(new Segment.Builder("MAN", location).priority((short) 1).name("MAN").description("abc").build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(flowSegmented, Set.of(location));
        resourceSegmentationDao.setSegmentations(metricsSegmented, Set.of(location));

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)

                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(vla.getPublicKey()), DiAmount.of(0, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(sas.getPublicKey()), DiAmount.of(0, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(iva.getPublicKey()), DiAmount.of(0, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(myt.getPublicKey()), DiAmount.of(0, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(man.getPublicKey()), DiAmount.of(0, DiUnit.COUNT))

                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(vla.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(sas.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(iva.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(myt.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(man.getPublicKey()), DiAmount.of(100_000L, DiUnit.COUNT))

                        .changes(SOLOMON, METRICS_STORED_COUNT, bigOrderTwo.getId(), Set.of(), DiAmount.of(1_000_000L, DiUnit.COUNT))

                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(11, changes.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        BigDecimal flow = BigDecimal.ZERO;
        BigDecimal metricsInRequest = new BigDecimal("1000000", MATH_CONTEXT);
        BigDecimal totalCores = new BigDecimal("2", MATH_CONTEXT).multiply(
                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT).add(
                                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                                        .max(metricsInRequest.divide(new BigDecimal("400000000", MATH_CONTEXT), MATH_CONTEXT))
                                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT)
                        ).setScale(0, RoundingMode.UP)
                , MATH_CONTEXT);
        BigDecimal totalGibRam = totalCores.multiply(new BigDecimal("8", MATH_CONTEXT), MATH_CONTEXT);
        BigDecimal cost = totalCores.multiply(new BigDecimal("162.81", MATH_CONTEXT), MATH_CONTEXT).add(
                totalGibRam.multiply(new BigDecimal("25.12", MATH_CONTEXT), MATH_CONTEXT)
        );

        // flow segmented
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, vla.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, sas.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, iva.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, myt.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, man.getPublicKey())));
        // metricsSegmented
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, vla.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, sas.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, iva.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, myt.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, man.getPublicKey())));
        // METRICS_STORED_COUNT
        Assertions.assertEquals(cost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT, null)));

        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(11, changes2.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        // flow segmented
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, vla.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, sas.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, iva.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, myt.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, man.getPublicKey())));
        // metricsSegmented
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, vla.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, sas.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, iva.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, myt.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, man.getPublicKey())));
        // METRICS_STORED_COUNT
        Assertions.assertEquals(cost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT, null)));

        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(vla.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(sas.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(iva.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(myt.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_WRITE_FLOW_SEGMENTED, bigOrderTwo.getId(), Set.of(man.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))

                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(vla.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(sas.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(iva.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(myt.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT_SEGMENTED, bigOrderTwo.getId(), Set.of(man.getPublicKey()), DiAmount.of(200_000L, DiUnit.COUNT))

                        .changes(SOLOMON, METRICS_STORED_COUNT, bigOrderTwo.getId(), Set.of(), DiAmount.of(2_000_000L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(11, changes3.size());

        Map<Tuple2<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        flow = new BigDecimal("200000", MATH_CONTEXT).multiply(new BigDecimal("5", MATH_CONTEXT), MATH_CONTEXT);
        metricsInRequest = new BigDecimal("2000000", MATH_CONTEXT);
        totalCores = new BigDecimal("2", MATH_CONTEXT).multiply(
                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT).add(
                                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                                        .max(metricsInRequest.divide(new BigDecimal("400000000", MATH_CONTEXT), MATH_CONTEXT))
                                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT)
                        ).setScale(0, RoundingMode.UP)
                , MATH_CONTEXT);
        totalGibRam = totalCores.multiply(new BigDecimal("8", MATH_CONTEXT), MATH_CONTEXT);
        cost = totalCores.multiply(new BigDecimal("162.81", MATH_CONTEXT), MATH_CONTEXT).add(
                totalGibRam.multiply(new BigDecimal("25.12", MATH_CONTEXT), MATH_CONTEXT)
        );
        BigDecimal weight = new BigDecimal("200000", MATH_CONTEXT).divide(flow, MATH_CONTEXT);
        BigDecimal locationCost = cost.multiply(weight, MATH_CONTEXT);

        // flow segmented
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, vla.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, sas.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, iva.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, myt.getPublicKey())));
        Assertions.assertEquals(locationCost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_WRITE_FLOW_SEGMENTED, man.getPublicKey())));
        // metricsSegmented
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, vla.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, sas.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, iva.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, myt.getPublicKey())));
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT_SEGMENTED, man.getPublicKey())));
        // METRICS_STORED_COUNT
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT, null)));
    }

    @Test
    public void solomon2020OwningCostFormulaWithoutFlowTest() {
        prepareCampaign2020();

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String METRICS_WRITE_FLOW = "metrics_write_flow";
        String METRICS_STORED_COUNT = "metrics_stored_count";

        Service service = serviceDao.create(Service.withKey(SOLOMON).withName(SOLOMON).withAbcServiceId(5656).withPriority(190).build());

        updateHierarchy();

        resourceDao.create(new Resource.Builder(METRICS_WRITE_FLOW, service).name(METRICS_WRITE_FLOW).type(DiResourceType.ENUMERABLE).build());
        resourceDao.create(new Resource.Builder(METRICS_STORED_COUNT, service).name(METRICS_STORED_COUNT).type(DiResourceType.ENUMERABLE).build());

        updateHierarchy();

        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)

                        .changes(SOLOMON, METRICS_WRITE_FLOW, bigOrderTwo.getId(), Set.of(), DiAmount.of(0L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT, bigOrderTwo.getId(), Set.of(), DiAmount.of(1_000_000L, DiUnit.COUNT))

                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        Assertions.assertNotNull(fetchedRequest);
        List<DiQuotaChangeRequest.Change> changes = fetchedRequest.getChanges();
        Assertions.assertEquals(2, changes.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey = changes.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        BigDecimal flow = BigDecimal.ZERO;
        BigDecimal metricsInRequest = new BigDecimal("1000000", MATH_CONTEXT);
        BigDecimal totalCores = new BigDecimal("2", MATH_CONTEXT).multiply(
                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT).add(
                                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                                        .max(metricsInRequest.divide(new BigDecimal("400000000", MATH_CONTEXT), MATH_CONTEXT))
                                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT)
                        ).setScale(0, RoundingMode.UP)
                , MATH_CONTEXT);
        BigDecimal totalGibRam = totalCores.multiply(new BigDecimal("8", MATH_CONTEXT), MATH_CONTEXT);
        BigDecimal cost = totalCores.multiply(new BigDecimal("162.81", MATH_CONTEXT), MATH_CONTEXT).add(
                totalGibRam.multiply(new BigDecimal("25.12", MATH_CONTEXT), MATH_CONTEXT)
        );

        // flow
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_WRITE_FLOW, null)));
        // METRICS_STORED_COUNT
        Assertions.assertEquals(cost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey.get(Tuple2.tuple(METRICS_STORED_COUNT, null)));

        quotaChangeOwningCostRefreshManager.refresh();
        updateHierarchy();

        final DiQuotaChangeRequest fetchedRequest2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);

        List<DiQuotaChangeRequest.Change> changes2 = fetchedRequest2.getChanges();
        Assertions.assertEquals(2, changes2.size());
        Map<Tuple2<String, String>, String> owningCostByResourceKey2 = changes2.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        // flow
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_WRITE_FLOW, null)));
        // METRICS_STORED_COUNT
        Assertions.assertEquals(cost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey2.get(Tuple2.tuple(METRICS_STORED_COUNT, null)));

        dispenser().quotaChangeRequests()
                .byId(quotaRequests.getFirst().getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(SOLOMON, METRICS_WRITE_FLOW, bigOrderTwo.getId(), Set.of(), DiAmount.of(200_000L, DiUnit.COUNT))
                        .changes(SOLOMON, METRICS_STORED_COUNT, bigOrderTwo.getId(), Set.of(), DiAmount.of(2_000_000L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiQuotaChangeRequest fetchedRequest3 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(fetchedRequest3);
        List<DiQuotaChangeRequest.Change> changes3 = fetchedRequest3.getChanges();
        Assertions.assertEquals(2, changes3.size());

        Map<Tuple2<String, String>, String> owningCostByResourceKey3 = changes3.stream()
                .collect(Collectors.toMap(k -> Tuple2.tuple(k.getResource().getKey(), k.getSegmentKeys().stream().findFirst().orElse(null)), DiQuotaChangeRequest.Change::getOwningCost));

        flow = new BigDecimal("200000", MATH_CONTEXT);
        metricsInRequest = new BigDecimal("2000000", MATH_CONTEXT);
        totalCores = new BigDecimal("2", MATH_CONTEXT).multiply(
                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT).add(
                                flow.divide(new BigDecimal("4000000", MATH_CONTEXT), MATH_CONTEXT)
                                        .max(metricsInRequest.divide(new BigDecimal("400000000", MATH_CONTEXT), MATH_CONTEXT))
                                        .multiply(new BigDecimal("56", MATH_CONTEXT), MATH_CONTEXT)
                        ).setScale(0, RoundingMode.UP)
                , MATH_CONTEXT);
        totalGibRam = totalCores.multiply(new BigDecimal("8", MATH_CONTEXT), MATH_CONTEXT);
        cost = totalCores.multiply(new BigDecimal("162.81", MATH_CONTEXT), MATH_CONTEXT).add(
                totalGibRam.multiply(new BigDecimal("25.12", MATH_CONTEXT), MATH_CONTEXT)
        );

        // flow
        Assertions.assertEquals(cost.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_WRITE_FLOW, null)));
        // METRICS_STORED_COUNT
        Assertions.assertEquals(DEFAULT_OWNING_COST.setScale(0, RoundingMode.HALF_UP).toString(), owningCostByResourceKey3.get(Tuple2.tuple(METRICS_STORED_COUNT, null)));
    }
}
