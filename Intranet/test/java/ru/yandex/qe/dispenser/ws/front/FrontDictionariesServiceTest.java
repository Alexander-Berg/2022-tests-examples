package ru.yandex.qe.dispenser.ws.front;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiSegmentation;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.CampaignServiceResourcesPreset;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.ResourceUnit;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.unit.ResourceUnitDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationReader;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.dao.service.resource.preset.CampaignServiceResourcesPresetDao;
import ru.yandex.qe.dispenser.domain.dictionaries.impl.FrontDictionariesManager;
import ru.yandex.qe.dispenser.domain.dictionaries.model.CampaignProvidersSettingsDictionary;
import ru.yandex.qe.dispenser.domain.dictionaries.model.FrontDictionaries;
import ru.yandex.qe.dispenser.domain.dictionaries.model.RequestStatusesDictionary;
import ru.yandex.qe.dispenser.domain.dictionaries.model.ResourcesWithUnitAliasesDictionary;
import ru.yandex.qe.dispenser.domain.dictionaries.model.SegmentationsWithCampaignsDictionary;
import ru.yandex.qe.dispenser.domain.dictionaries.model.UnitsDictionary;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.index.LongIndexBase;
import ru.yandex.qe.dispenser.ws.common.model.response.ErrorResponse;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;
import ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class FrontDictionariesServiceTest extends BaseQuotaRequestTest {

    @Autowired
    private CampaignResourceDao campaignResourceDao;

    @Autowired
    private ResourceUnitDao resourceUnitDao;
    @Autowired
    private CampaignServiceResourcesPresetDao presetDao;
    @Autowired
    private FrontDictionariesManager dictionariesManager;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
    }

    @Test
    public void getDictionariesTest() {
        prepareCampaignResources();
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/front/dictionaries")
                .replaceQueryParam("dictionaries", FrontDictionaries.REQUEST_STATUS,
                        FrontDictionaries.REQUEST_REASON, FrontDictionaries.PROVIDERS_WITH_CAMPAIGNS,
                        FrontDictionaries.CAMPAIGNS_WITH_BIG_ORDERS, FrontDictionaries.SORT_FIELDS,
                        FrontDictionaries.RESOURCES_WITH_CAMPAIGNS, FrontDictionaries.SEGMENTATIONS_WITH_CAMPAIGNS)
                .invoke(HttpMethod.GET, null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final FrontDictionaries result = response.readEntity(FrontDictionaries.class);
        assertNotNull(result.getRequestStatusesDictionary());
        assertNotNull(result.getRequestReasonsDictionary());
        assertNotNull(result.getProvidersWithCampaignsDictionary());
        assertNotNull(result.getCampaignsWithBigOrdersDictionary());
        assertNotNull(result.getSortFieldsDictionary());
        assertNotNull(result.getResourcesWithCampaignsDictionary());
        assertNotNull(result.getSegmentationsWithCampaignsDictionary());
        assertFalse(result.getRequestStatusesDictionary().getStatuses().isEmpty());
        assertFalse(result.getRequestReasonsDictionary().getReasons().isEmpty());
        assertFalse(result.getProvidersWithCampaignsDictionary().getProviders().isEmpty());
        assertFalse(result.getCampaignsWithBigOrdersDictionary().getCampaigns().isEmpty());
        assertFalse(result.getSortFieldsDictionary().getSortFields().isEmpty());
        assertFalse(result.getResourcesWithCampaignsDictionary().getResources().isEmpty());
        assertFalse(result.getSegmentationsWithCampaignsDictionary().getSegmentations().isEmpty());
    }

    @Test
    public void getDictionariesInvalidKeyTest() {
        prepareCampaignResources();
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/front/dictionaries")
                .replaceQueryParam("dictionaries", FrontDictionaries.REQUEST_STATUS,
                        FrontDictionaries.REQUEST_REASON, FrontDictionaries.PROVIDERS_WITH_CAMPAIGNS,
                        FrontDictionaries.CAMPAIGNS_WITH_BIG_ORDERS, FrontDictionaries.SORT_FIELDS,
                        FrontDictionaries.RESOURCES_WITH_CAMPAIGNS, FrontDictionaries.SEGMENTATIONS_WITH_CAMPAIGNS,
                        "unsupported")
                .invoke(HttpMethod.GET, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        assertTrue(result.getFieldErrors().containsKey("dictionaries"));
    }

    @Test
    public void getDictionariesActiveCampaignTest() {
        prepareCampaignResources();
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/front/dictionaries")
                .replaceQueryParam("dictionaries", FrontDictionaries.CAMPAIGN_PROVIDERS_SETTINGS,
                        FrontDictionaries.REQUEST_REASON)
                .invoke(HttpMethod.GET, null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final FrontDictionaries result = response.readEntity(FrontDictionaries.class);
        assertNotNull(result.getRequestReasonsDictionary());
        assertNotNull(result.getCampaignProvidersSettingsDictionary());
        assertNotNull(result.getCampaignProvidersSettingsDictionary().getCampaign());
        assertNotNull(result.getCampaignProvidersSettingsDictionary().getProviders());
        assertNotNull(result.getCampaignProvidersSettingsDictionary().getSegmentations());
        assertFalse(result.getRequestReasonsDictionary().getReasons().isEmpty());
        assertFalse(result.getCampaignProvidersSettingsDictionary().getProviders().isEmpty());
        assertFalse(result.getCampaignProvidersSettingsDictionary().getSegmentations().isEmpty());
    }

    @Test
    public void getDictionariesForCampaignTest() {
        prepareCampaignResources();
        final Campaign campaign = campaignDao.getAllSorted(Collections.emptySet()).get(0);
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/front/dictionaries")
                .replaceQueryParam("dictionaries", FrontDictionaries.CAMPAIGN_PROVIDERS_SETTINGS,
                        FrontDictionaries.REQUEST_REASON)
                .replaceQueryParam("campaignId", campaign.getId())
                .invoke(HttpMethod.GET, null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final FrontDictionaries result = response.readEntity(FrontDictionaries.class);
        assertNotNull(result.getRequestReasonsDictionary());
        assertNotNull(result.getCampaignProvidersSettingsDictionary());
        assertNotNull(result.getCampaignProvidersSettingsDictionary().getCampaign());
        assertNotNull(result.getCampaignProvidersSettingsDictionary().getProviders());
        assertNotNull(result.getCampaignProvidersSettingsDictionary().getSegmentations());
        assertFalse(result.getRequestReasonsDictionary().getReasons().isEmpty());
        assertFalse(result.getCampaignProvidersSettingsDictionary().getProviders().isEmpty());
        assertFalse(result.getCampaignProvidersSettingsDictionary().getSegmentations().isEmpty());
    }

    @Test
    public void anyShouldCanFetchUnitDictionary() {

        final FrontDictionaries dictionaries = createAuthorizedLocalClient(BINARY_CAT)
                .path("/front/dictionaries")
                .query("dictionaries", FrontDictionaries.UNITS, FrontDictionaries.REQUEST_REASON)
                .get(FrontDictionaries.class);

        final UnitsDictionary unitsDictionary = dictionaries.getUnitsDictionary();
        assertNotNull(unitsDictionary);

        assertFalse(unitsDictionary.getEnsembles().isEmpty());
        assertTrue(unitsDictionary.getEnsembles().stream().anyMatch(e -> e.getKey().equals("BINARY_BPS") && e.getBase() == 1024));

        assertFalse(unitsDictionary.getUnits().isEmpty());
        assertTrue(unitsDictionary.getUnits().stream().anyMatch(u -> u.getKey().equals(DiUnit.MIBPS.name())
                && u.getName().equals(DiUnit.MIBPS.getAbbreviation())
                && u.getEnsemble().equals("BINARY_BPS")
                && u.getMultiplier().getPower() == 2)
        );

        assertFalse(unitsDictionary.getResourceTypes().isEmpty());
        assertTrue(unitsDictionary.getResourceTypes().stream().anyMatch(rt -> rt.getKey().equals(DiResourceType.BINARY_TRAFFIC.name())
                && rt.getBaseUnit().equals(DiResourceType.BINARY_TRAFFIC.getBaseUnit().name()))
        );
    }

    @Test
    public void anyShouldCanFetchResourcesWithUnitDictionary() {
        dictionariesManager.clearCache();
        final Resource ytCpu = resourceDao.read(new Resource.Key(YT_CPU, serviceDao.read(NIRVANA)));
        TransactionWrapper.INSTANCE.execute(this::prepareCampaignResources);

        resourceUnitDao.clear();
        resourceUnitDao.create(new ResourceUnit(ytCpu.getId(), DiUnit.KILO.name(),
                new ResourceUnit.UnitsSettings(ImmutableMap.of(
                        DiUnit.COUNT.name(), new ResourceUnit.UnitProperties("ht core", "unit.ht_core")
                ))));

        final FrontDictionaries dictionaries = createAuthorizedLocalClient(BINARY_CAT)
                .path("/front/dictionaries")
                .query("dictionaries", FrontDictionaries.RESOURCES_WITH_UNIT_ALIASES, FrontDictionaries.CAMPAIGN_PROVIDERS_SETTINGS)
                .get(FrontDictionaries.class);

        final ResourcesWithUnitAliasesDictionary aliasesDictionary = dictionaries.getResourcesWithUnitAliasesDictionary();
        assertNotNull(aliasesDictionary);

        assertEquals(1, aliasesDictionary.getResources().size());

        final ResourcesWithUnitAliasesDictionary.Resource resource = aliasesDictionary.getResources().iterator().next();
        assertEquals(DiUnit.KILO.name(), resource.getDefaultUnit());

        final Map<String, ResourceUnit.UnitProperties> propertiesByUnitKey = resource.getUnits().getPropertiesByUnitKey();
        assertEquals(1, propertiesByUnitKey.size());
        final ResourceUnit.UnitProperties unitProperties = propertiesByUnitKey.get(DiUnit.COUNT.name());
        assertEquals("ht core", unitProperties.getName());
        assertEquals("unit.ht_core", unitProperties.getLocalizationKey());

        final CampaignProvidersSettingsDictionary providerSettings = dictionaries.getCampaignProvidersSettingsDictionary();

        final CampaignProvidersSettingsDictionary.Provider nirvana = providerSettings.getProviders().stream()
                .filter(p -> p.getKey().equals(NIRVANA))
                .findAny().get();

        final CampaignProvidersSettingsDictionary.Resource ytCpuSetting = nirvana.getResources().stream()
                .filter(r -> r.getId() == ytCpu.getId())
                .findFirst().get();

        assertEquals(DiUnit.KILO.name(), ytCpuSetting.getDefaultUnit());

        assertEquals(1, ytCpuSetting.getUnits().getPropertiesByUnitKey().size());
        final ResourceUnit.UnitProperties countUnitProperties = ytCpuSetting.getUnits().getPropertiesByUnitKey().get(DiUnit.COUNT.name());
        assertEquals("ht core", countUnitProperties.getName());
        assertEquals("unit.ht_core", countUnitProperties.getLocalizationKey());

        final CampaignProvidersSettingsDictionary.Resource ytGpuSetting = nirvana.getResources().stream()
                .filter(r -> r.getKey().equals(YT_GPU))
                .findFirst().get();

        assertNull(ytGpuSetting.getDefaultUnit());
        assertNull(ytGpuSetting.getUnits());
    }

    @Test
    private void campaignSegmentationDictionaryShouldContainsSortedSegmentations() {
        dictionariesManager.clearCache();

        final DiSegmentation seg1 = dispenser()
                .segmentations()
                .create(new DiSegmentation.Builder("test-segmentation-1")
                        .withName("name")
                        .withDescription("description")
                        .withPriority(20)
                        .build())
                .performBy(AMOSOV_F);

        final DiSegmentation seg2 = dispenser()
                .segmentations()
                .create(new DiSegmentation.Builder("test-segmentation-2")
                        .withName("name")
                        .withDescription("description")
                        .withPriority(30)
                        .build())
                .performBy(AMOSOV_F);

        final DiSegmentation seg3 = dispenser()
                .segmentations()
                .create(new DiSegmentation.Builder("test-segmentation-3")
                        .withName("name")
                        .withDescription("description")
                        .withPriority(10)
                        .build())
                .performBy(AMOSOV_F);

        dispenser().service(NIRVANA)
                .resource("time").create()
                .withName("Nirvana time")
                .withType(DiResourceType.ENUMERABLE)
                .inMode(DiQuotingMode.SYNCHRONIZATION)
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser().service(NIRVANA)
                .resource("time")
                .segmentations()
                .update(ImmutableList.of(seg3, seg2, seg1))
                .performBy(AMOSOV_F);

        updateHierarchy();

        prepareCampaignResources();

        final FrontDictionaries dicts = createAuthorizedLocalClient(BINARY_CAT)
                .path("/front/dictionaries")
                .query("dictionaries", FrontDictionaries.SEGMENTATIONS_WITH_CAMPAIGNS,
                        FrontDictionaries.CAMPAIGN_PROVIDERS_SETTINGS)
                .get(FrontDictionaries.class);

        final SegmentationsWithCampaignsDictionary segmentationWithCmapignsDict = dicts.getSegmentationsWithCampaignsDictionary();

        final List<String> orderedKeys = new ArrayList<>();

        final ImmutableList<String> expectedOrder = ImmutableList.of(
                "test-segmentation-3",
                "test-segmentation-1",
                "test-segmentation-2"
        );
        Integer priority = null;
        for (final SegmentationsWithCampaignsDictionary.Segmentation segmentation : segmentationWithCmapignsDict.getSegmentations()) {
            if (expectedOrder.contains(segmentation.getKey())) {
                orderedKeys.add(segmentation.getKey());
            }

            if (priority != null) {
                assertTrue(priority <= segmentation.getPriority());
            }
            priority = segmentation.getPriority();
        }
        assertEquals(expectedOrder, orderedKeys);

        final CampaignProvidersSettingsDictionary campaignProvidersSettingsDictionary = dicts.getCampaignProvidersSettingsDictionary();

        priority = null;
        orderedKeys.clear();
        for (final CampaignProvidersSettingsDictionary.Segmentation segmentation : campaignProvidersSettingsDictionary.getSegmentations()) {
            if (expectedOrder.contains(segmentation.getKey())) {
                orderedKeys.add(segmentation.getKey());
            }

            if (priority != null) {
                assertTrue(priority <= segmentation.getPriority());
            }
            priority = segmentation.getPriority();
        }
        assertEquals(expectedOrder, orderedKeys);
    }

    @Test
    private void campaignResourceDaoShouldReturnAllRequestedSegmentationCampaigns() {
        dispenser()
                .segmentations()
                .create(new DiSegmentation.Builder("test-segmentation-1")
                        .withName("name")
                        .withDescription("description")
                        .withPriority(20)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final Set<Segmentation> segmentations = Hierarchy.get().getSegmentationReader().getAll();
        assertTrue(segmentations.stream().anyMatch(s -> s.getKey().getPublicKey().equals("test-segmentation-1")));

        final Map<Long, Set<Long>> availableCampaignsForSegmentations =
                TransactionWrapper.INSTANCE.execute(() -> campaignResourceDao.getAvailableCampaignsForSegmentations(segmentations));

        assertEquals(availableCampaignsForSegmentations.keySet(), segmentations.stream().map(LongIndexBase::getId).collect(Collectors.toSet()));
    }

    @Test
    public void statusDictionaryShouldNotContainsAppliedStatus() {
        final FrontDictionaries dicts = createAuthorizedLocalClient(BINARY_CAT)
                .path("/front/dictionaries")
                .query("dictionaries", FrontDictionaries.REQUEST_STATUS)
                .get(FrontDictionaries.class);

        final Set<DiQuotaChangeRequest.Status> statusNames = dicts.getRequestStatusesDictionary()
                .getStatuses().stream()
                .map(RequestStatusesDictionary.RequestStatus::getKey)
                .collect(Collectors.toSet());

        assertFalse(statusNames.contains(DiQuotaChangeRequest.Status.APPLIED));

    }

    @Test
    public void serviceResourcesPresetShouldBeInCampaignProviderDictionary() {
        TransactionWrapper.INSTANCE.execute(this::prepareCampaignResources);

        FrontDictionaries dicts = createAuthorizedLocalClient(BINARY_CAT)
                .path("/front/dictionaries")
                .query("dictionaries", FrontDictionaries.CAMPAIGN_PROVIDERS_SETTINGS)
                .get(FrontDictionaries.class);

        List<CampaignProvidersSettingsDictionary.ResourcePreset> defaultResources = dicts.getCampaignProvidersSettingsDictionary().getProviders().stream()
                .filter(p -> p.getKey().equals(YP))
                .findFirst().get()
                .getDefaultResources();

        assertEquals(Collections.emptyList(), defaultResources);

        final Service service = hierarchy.get().getServiceReader().read(YP);

        final Map<String, Resource> resourceByKey = hierarchy.get().getResourceReader().getByService(service).stream()
                .collect(Collectors.toMap(r -> r.getKey().getPublicKey(), Function.identity()));

        final SegmentationReader segmentationReader = hierarchy.get().getSegmentationReader();
        final Segmentation dcSegmentation = segmentationReader.read(new Segmentation.Key(DC_SEGMENTATION));
        final long segmentationid = dcSegmentation.getId();

        final List<Long> dcSegmentIds = hierarchy.get().getSegmentReader().get(dcSegmentation).stream()
                .map(LongIndexBase::getId)
                .collect(Collectors.toList());

        final Long segment1 = dcSegmentIds.get(0);
        final Long segment2 = dcSegmentIds.get(1);
        final Long segment3 = dcSegmentIds.get(2);

        final Campaign campaign = campaignDao.getAllSorted(Collections.singleton(Campaign.Status.ACTIVE)).iterator().next();
        final long cpuId = resourceByKey.get(SEGMENT_CPU).getId();
        final long hddId = resourceByKey.get(SEGMENT_HDD).getId();
        final CampaignServiceResourcesPreset preset = presetDao.create(new CampaignServiceResourcesPreset(new CampaignServiceResourcesPreset.Key(
                campaign.getId(),
                service.getId()
        ), ImmutableList.of(
                new CampaignProvidersSettingsDictionary.ResourcePreset(cpuId, Collections.emptyList()),
                new CampaignProvidersSettingsDictionary.ResourcePreset(hddId, ImmutableList.of(
                        new CampaignProvidersSettingsDictionary.ResourcePresetSegment(segmentationid, segment1),
                        new CampaignProvidersSettingsDictionary.ResourcePresetSegment(segmentationid, segment2
                        )))
        )));

        dictionariesManager.clearCache();

        dicts = createAuthorizedLocalClient(BINARY_CAT)
                .path("/front/dictionaries")
                .query("dictionaries", FrontDictionaries.CAMPAIGN_PROVIDERS_SETTINGS)
                .get(FrontDictionaries.class);

        defaultResources = dicts.getCampaignProvidersSettingsDictionary().getProviders().stream()
                .filter(p -> p.getKey().equals(YP))
                .findFirst().get()
                .getDefaultResources();

        assertEquals(2, defaultResources.size());

        Map<Long, List<CampaignProvidersSettingsDictionary.ResourcePresetSegment>> segmentByResource = defaultResources.stream()
                .collect(Collectors.toMap(CampaignProvidersSettingsDictionary.ResourcePreset::getResourceId, CampaignProvidersSettingsDictionary.ResourcePreset::getSegments));

        assertTrue(segmentByResource.get(cpuId).isEmpty());
        assertEquals(2, segmentByResource.get(hddId).size());

        Map<Long, Set<Long>> hddSegmentIdsBySegmentations = segmentByResource.get(hddId).stream()
                .collect(Collectors.groupingBy(CampaignProvidersSettingsDictionary.ResourcePresetSegment::getSegmentationId, Collectors.mapping(CampaignProvidersSettingsDictionary.ResourcePresetSegment::getSegmentId, Collectors.toSet())));

        assertEquals(hddSegmentIdsBySegmentations.get(segmentationid), ImmutableSet.of(segment1, segment2));

        final long invalidId = -1;
        presetDao.update(new CampaignServiceResourcesPreset(new CampaignServiceResourcesPreset.Key(
                campaign.getId(),
                service.getId()
        ), ImmutableList.of(
                new CampaignProvidersSettingsDictionary.ResourcePreset(cpuId, Collections.emptyList()),
                new CampaignProvidersSettingsDictionary.ResourcePreset(hddId, ImmutableList.of(
                        new CampaignProvidersSettingsDictionary.ResourcePresetSegment(segmentationid, segment3),
                        new CampaignProvidersSettingsDictionary.ResourcePresetSegment(segmentationid, invalidId),
                        new CampaignProvidersSettingsDictionary.ResourcePresetSegment(invalidId, segment3),
                        new CampaignProvidersSettingsDictionary.ResourcePresetSegment(invalidId, invalidId)
                )),
                new CampaignProvidersSettingsDictionary.ResourcePreset(2334, Collections.emptyList())
        )));

        dictionariesManager.clearCache();

        dicts = createAuthorizedLocalClient(BINARY_CAT)
                .path("/front/dictionaries")
                .query("dictionaries", FrontDictionaries.CAMPAIGN_PROVIDERS_SETTINGS)
                .get(FrontDictionaries.class);

        defaultResources = dicts.getCampaignProvidersSettingsDictionary().getProviders().stream()
                .filter(p -> p.getKey().equals(YP))
                .findFirst().get()
                .getDefaultResources();

        assertEquals(2, defaultResources.size());

        segmentByResource = defaultResources.stream()
                .collect(Collectors.toMap(CampaignProvidersSettingsDictionary.ResourcePreset::getResourceId, CampaignProvidersSettingsDictionary.ResourcePreset::getSegments));

        assertTrue(segmentByResource.get(cpuId).isEmpty());
        assertEquals(1, segmentByResource.get(hddId).size());

        hddSegmentIdsBySegmentations = segmentByResource.get(hddId).stream()
                .collect(Collectors.groupingBy(CampaignProvidersSettingsDictionary.ResourcePresetSegment::getSegmentationId, Collectors.mapping(CampaignProvidersSettingsDictionary.ResourcePresetSegment::getSegmentId, Collectors.toSet())));

        assertEquals(hddSegmentIdsBySegmentations.get(segmentationid), ImmutableSet.of(segment3));

        presetDao.delete(preset);
        assertTrue(presetDao.getPresetsByCampaignId(campaign.getId()).isEmpty());
    }
}
