package ru.yandex.qe.dispenser.ws.logic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.ExpandQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.api.v1.response.DiListPageResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.BotCampaignGroup;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResource;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceLinearRelation;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceLinearRelationTerm;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceMapping;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceMdsRelation;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceMdsRelationTerm;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceRelation;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceType;
import ru.yandex.qe.dispenser.domain.base_resources.MdsLocation;
import ru.yandex.qe.dispenser.domain.base_resources.MdsStorageType;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceChangeDao;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceDao;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceMappingDao;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceTypeDao;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.project.ProjectDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Role;
import ru.yandex.qe.dispenser.domain.mds.MdsConfigApi;
import ru.yandex.qe.dispenser.standalone.MockMdsConfigApi;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;

public class QuotaChangeRequestBaseResourcesTest extends BaseQuotaRequestTest {

    @Autowired
    private BigOrderManager bigOrderManager;
    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;
    @Autowired
    private CampaignDao campaignDao;
    @Autowired
    private BaseResourceTypeDao baseResourceTypeDao;
    @Autowired
    private BaseResourceDao baseResourceDao;
    @Autowired
    private ServiceDao serviceDao;
    @Autowired
    private SegmentDao segmentDao;
    @Autowired
    private SegmentationDao segmentationDao;
    @Autowired
    private BaseResourceMappingDao baseResourceMappingDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private MdsConfigApi mdsConfigApi;
    @Autowired
    private BaseResourceChangeDao baseResourceChangeDao;
    @Autowired
    private ProjectDao projectDao;
    @Autowired
    private PersonDao personDao;

    @Override
    @BeforeAll
    public void beforeClass() {
        super.beforeClass();
        bigOrderManager.create(BigOrder.builder(LocalDate.of(2119, 1, 1)));
        bigOrderManager.create(BigOrder.builder(LocalDate.of(2119, 2, 1)));
    }

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        Campaign campaign = campaignDao.create(defaultCampaignBuilder().build());
        botCampaignGroupDao.create(
                defaultCampaignGroupBuilder()
                        .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrderOne.getId())))
                        .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values()
                                .iterator().next())
                        .build()
        );
        newCampaignId = campaign.getId();
        prepareCampaignResources();
    }

    @Test
    public void quotaRequestWithTypeResourcePreorder() {
        Campaign activeCampaign = campaignDao.getAllSorted(Set.of(Campaign.Status.ACTIVE))
                .stream().findFirst().get();
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        MutableLong baseResourceId = new MutableLong();
        MutableLong mappingId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Resource resourceThree = resourceDao.read(new Resource.Key(SEGMENT_HDD, yp));
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long typeTwo = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-2")
                    .name("Test-2")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResourceOne = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            long mappingOne = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(7)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(4)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceThree.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build()));
            baseResourceId.setValue(baseResourceOne);
            mappingId.setValue(mappingOne);
        });
        DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                DiAmount.of(1000L, DiUnit.PERMILLE_CORES))
                        .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                DiAmount.of(1000L, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);
        updateHierarchy();
        DiQuotaChangeRequest request = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .replaceQueryParam("expand", ExpandQuotaChangeRequest.BASE_RESOURCES)
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(request);
        Assertions.assertNotNull(request.getBaseResourceChanges());
        Assertions.assertEquals(1, request.getBaseResourceChanges().size());
        Assertions.assertEquals(bigOrderOne.getId(),
                new ArrayList<>(request.getBaseResourceChanges()).get(0).getBigOrder().getId());
        Assertions.assertEquals(baseResourceId.longValue(),
                new ArrayList<>(request.getBaseResourceChanges()).get(0).getBaseResource().getId());
        Assertions.assertEquals(1181L,
                new ArrayList<>(request.getBaseResourceChanges()).get(0).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES,
                new ArrayList<>(request.getBaseResourceChanges()).get(0).getTotalAmount().getUnit());
        Assertions.assertEquals(1,
                new ArrayList<>(request.getBaseResourceChanges()).get(0).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(new ArrayList<>(request.getBaseResourceChanges()).get(0)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(1181L, new ArrayList<>(new ArrayList<>(request.getBaseResourceChanges()).get(0)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(new ArrayList<>(request.getBaseResourceChanges())
                .get(0).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(new ArrayList<>(request.getBaseResourceChanges()).get(0)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingId.longValue(), new ArrayList<>(new ArrayList<>(new ArrayList<>(request
                .getBaseResourceChanges()).get(0).getPerProviderAmounts()).get(0).getMappingIds()).get(0));
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderPaginated() {
        Campaign activeCampaign = campaignDao.getAllSorted(Set.of(Campaign.Status.ACTIVE))
                .stream().findFirst().get();
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        MutableLong baseResourceId = new MutableLong();
        MutableLong mappingId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Resource resourceThree = resourceDao.read(new Resource.Key(SEGMENT_HDD, yp));
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long typeTwo = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-2")
                    .name("Test-2")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResourceOne = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            long mappingOne = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(7)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(4)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceThree.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build()));
            baseResourceId.setValue(baseResourceOne);
            mappingId.setValue(mappingOne);
        });
        DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                DiAmount.of(1000L, DiUnit.PERMILLE_CORES))
                        .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                DiAmount.of(1000L, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);
        updateHierarchy();
        DiListPageResponse<DiQuotaChangeRequest> request = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests")
                .replaceQueryParam("expand", ExpandQuotaChangeRequest.BASE_RESOURCES)
                .replaceQueryParam("pagination", true)
                .get(new GenericType<DiListPageResponse<DiQuotaChangeRequest>>() { });
        Assertions.assertNotNull(request);
        Assertions.assertNotNull(request.getFirst());
        Assertions.assertNotNull(request.getFirst().getBaseResourceChanges());
        Assertions.assertEquals(1, request.getFirst().getBaseResourceChanges().size());
        Assertions.assertEquals(bigOrderOne.getId(),
                new ArrayList<>(request.getFirst().getBaseResourceChanges()).get(0).getBigOrder().getId());
        Assertions.assertEquals(baseResourceId.longValue(),
                new ArrayList<>(request.getFirst().getBaseResourceChanges()).get(0).getBaseResource().getId());
        Assertions.assertEquals(1181L,
                new ArrayList<>(request.getFirst().getBaseResourceChanges()).get(0).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES,
                new ArrayList<>(request.getFirst().getBaseResourceChanges()).get(0).getTotalAmount().getUnit());
        Assertions.assertEquals(1,
                new ArrayList<>(request.getFirst().getBaseResourceChanges()).get(0).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(new ArrayList<>(request.getFirst().getBaseResourceChanges()).get(0)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(1181L, new ArrayList<>(new ArrayList<>(request.getFirst()
                .getBaseResourceChanges()).get(0).getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(new ArrayList<>(request.getFirst()
                .getBaseResourceChanges()).get(0).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(new ArrayList<>(request.getFirst()
                .getBaseResourceChanges()).get(0).getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingId.longValue(), new ArrayList<>(new ArrayList<>(new ArrayList<>(request
                .getFirst().getBaseResourceChanges()).get(0).getPerProviderAmounts()).get(0).getMappingIds()).get(0));
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderTwoResources() {
        Campaign activeCampaign = campaignDao.getAllSorted(Set.of(Campaign.Status.ACTIVE))
                .stream().findFirst().get();
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong mappingIdOne = new MutableLong();
        MutableLong mappingIdTwo = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Resource resourceThree = resourceDao.read(new Resource.Key(SEGMENT_HDD, yp));
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long typeTwo = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-2")
                    .name("Test-2")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResourceOne = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            long mappingOne = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(7)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(4)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            long mappingTwo = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(0)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceThree.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            baseResourceIdOne.setValue(baseResourceOne);
            baseResourceIdTwo.setValue(baseResourceTwo);
            mappingIdOne.setValue(mappingOne);
            mappingIdTwo.setValue(mappingTwo);
        });
        DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                DiAmount.of(1000L, DiUnit.PERMILLE_CORES))
                        .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                DiAmount.of(1000L, DiUnit.BYTE))
                        .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                DiAmount.of(2000L, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);
        updateHierarchy();
        DiQuotaChangeRequest request = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .replaceQueryParam("expand", ExpandQuotaChangeRequest.BASE_RESOURCES)
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(request);
        Assertions.assertNotNull(request.getBaseResourceChanges());
        Assertions.assertEquals(2, request.getBaseResourceChanges().size());
        List<DiQuotaChangeRequest.BaseResourceChange> baseResourceChanges = new ArrayList<>(request
                .getBaseResourceChanges()).stream().sorted(Comparator.comparing(v -> v.getBaseResource().getId()))
                .collect(Collectors.toList());
        Assertions.assertEquals(bigOrderOne.getId(), baseResourceChanges.get(0).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdOne.longValue(), baseResourceChanges.get(0).getBaseResource().getId());
        Assertions.assertEquals(1181L, baseResourceChanges.get(0).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, baseResourceChanges.get(0).getTotalAmount().getUnit());
        Assertions.assertEquals(1, baseResourceChanges.get(0).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(1181L, new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(baseResourceChanges
                .get(0).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdOne.longValue(), new ArrayList<>(new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds()).get(0));
        Assertions.assertEquals(bigOrderOne.getId(), baseResourceChanges.get(1).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdTwo.longValue(), baseResourceChanges.get(1).getBaseResource().getId());
        Assertions.assertEquals(6000L, baseResourceChanges.get(1).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, baseResourceChanges.get(1).getTotalAmount().getUnit());
        Assertions.assertEquals(1, baseResourceChanges.get(1).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(baseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(6000L, new ArrayList<>(baseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(baseResourceChanges
                .get(1).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(baseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdTwo.longValue(), new ArrayList<>(new ArrayList<>(baseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getMappingIds()).get(0));
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderTwoResourcesDoUpdate() {
        Campaign activeCampaign = campaignDao.getAllSorted(Set.of(Campaign.Status.ACTIVE))
                .stream().findFirst().get();
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong mappingIdOne = new MutableLong();
        MutableLong mappingIdTwo = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Resource resourceThree = resourceDao.read(new Resource.Key(SEGMENT_HDD, yp));
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long typeTwo = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-2")
                    .name("Test-2")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResourceOne = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            long mappingOne = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(7)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(4)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            long mappingTwo = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(0)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceThree.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            baseResourceIdOne.setValue(baseResourceOne);
            baseResourceIdTwo.setValue(baseResourceTwo);
            mappingIdOne.setValue(mappingOne);
            mappingIdTwo.setValue(mappingTwo);
        });
        DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                DiAmount.of(500L, DiUnit.PERMILLE_CORES))
                        .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                DiAmount.of(1000L, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(BINARY_CAT);
        updateHierarchy();
        DiQuotaChangeRequest request = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .replaceQueryParam("expand", ExpandQuotaChangeRequest.BASE_RESOURCES)
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(request);
        Assertions.assertNotNull(request.getBaseResourceChanges());
        Assertions.assertEquals(1, request.getBaseResourceChanges().size());
        List<DiQuotaChangeRequest.BaseResourceChange> baseResourceChanges = new ArrayList<>(request
                .getBaseResourceChanges()).stream().sorted(Comparator.comparing(v -> v.getBaseResource().getId()))
                .collect(Collectors.toList());
        Assertions.assertEquals(bigOrderOne.getId(), baseResourceChanges.get(0).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdOne.longValue(), baseResourceChanges.get(0).getBaseResource().getId());
        Assertions.assertEquals(967L, baseResourceChanges.get(0).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, baseResourceChanges.get(0).getTotalAmount().getUnit());
        Assertions.assertEquals(1, baseResourceChanges.get(0).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(967L, new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(baseResourceChanges
                .get(0).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdOne.longValue(), new ArrayList<>(new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds()).get(0));
        BodyUpdate update = new BodyUpdate.BodyUpdateBuilder()
                .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                        DiAmount.of(1000L, DiUnit.PERMILLE_CORES))
                .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                        DiAmount.of(2000L, DiUnit.BYTE))
                .build();
        DiQuotaChangeRequest updatedRequest = updateRequest(request.getId(), update, BINARY_CAT);
        Assertions.assertNotNull(updatedRequest);
        updateHierarchy();
        DiQuotaChangeRequest requestAfterUpdate = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .replaceQueryParam("expand", ExpandQuotaChangeRequest.BASE_RESOURCES)
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(requestAfterUpdate);
        Assertions.assertNotNull(requestAfterUpdate.getBaseResourceChanges());
        Assertions.assertEquals(2, requestAfterUpdate.getBaseResourceChanges().size());
        List<DiQuotaChangeRequest.BaseResourceChange> updatedBaseResourceChanges = new ArrayList<>(requestAfterUpdate
                .getBaseResourceChanges()).stream().sorted(Comparator.comparing(v -> v.getBaseResource().getId()))
                .collect(Collectors.toList());
        Assertions.assertEquals(bigOrderOne.getId(), updatedBaseResourceChanges.get(0).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdOne.longValue(), updatedBaseResourceChanges
                .get(0).getBaseResource().getId());
        Assertions.assertEquals(431L, updatedBaseResourceChanges.get(0).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, updatedBaseResourceChanges.get(0).getTotalAmount().getUnit());
        Assertions.assertEquals(1, updatedBaseResourceChanges.get(0).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(updatedBaseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(431L, new ArrayList<>(updatedBaseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(updatedBaseResourceChanges
                .get(0).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(updatedBaseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdOne.longValue(), new ArrayList<>(new ArrayList<>(updatedBaseResourceChanges
                .get(0).getPerProviderAmounts()).get(0).getMappingIds()).get(0));
        Assertions.assertEquals(bigOrderOne.getId(), updatedBaseResourceChanges.get(1).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdTwo.longValue(), updatedBaseResourceChanges.get(1)
                .getBaseResource().getId());
        Assertions.assertEquals(6000L, updatedBaseResourceChanges.get(1).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, updatedBaseResourceChanges.get(1).getTotalAmount().getUnit());
        Assertions.assertEquals(1, updatedBaseResourceChanges.get(1).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(updatedBaseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(6000L, new ArrayList<>(updatedBaseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(updatedBaseResourceChanges
                .get(1).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(updatedBaseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdTwo.longValue(), new ArrayList<>(new ArrayList<>(updatedBaseResourceChanges
                .get(1).getPerProviderAmounts()).get(0).getMappingIds()).get(0));
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderTwoResourcesMdsMapping() {
        MockMdsConfigApi mdsConfig = (MockMdsConfigApi) mdsConfigApi;
        Campaign activeCampaign = campaignDao.getAllSorted(Set.of(Campaign.Status.ACTIVE))
                .stream().findFirst().get();
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong mappingIdOne = new MutableLong();
        MutableLong mappingIdTwo = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Resource resourceThree = resourceDao.read(new Resource.Key(SEGMENT_HDD, yp));
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long typeTwo = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-2")
                    .name("Test-2")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.STORAGE_BASE)
            ).getId();
            long baseResourceOne = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            long mappingOne = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(7)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(4)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            long mappingTwo = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .mds(BaseResourceMdsRelation.builder()
                                    .addTerm(BaseResourceMdsRelationTerm.builder()
                                            .resourceId(resourceThree.getId())
                                            .numerator(1)
                                            .denominator(1024L * 1024L * 1024L)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .storageType(MdsStorageType.MDS)
                                            .location(MdsLocation.SAS)
                                            .build())
                                    .build())
                            .build())).getId();
            baseResourceIdOne.setValue(baseResourceOne);
            baseResourceIdTwo.setValue(baseResourceTwo);
            mappingIdOne.setValue(mappingOne);
            mappingIdTwo.setValue(mappingTwo);
        });
        DiListResponse<DiQuotaChangeRequest> quotaRequests;
        mdsConfig.setPassThrough(true);
        try {
            quotaRequests = dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .projectKey(TEST_PROJECT_KEY)
                            .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                    DiAmount.of(1000L, DiUnit.PERMILLE_CORES))
                            .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                    DiAmount.of(1000L, DiUnit.BYTE))
                            .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                    DiAmount.of(2L * 1024L * 1024L * 1024L, DiUnit.BYTE))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                            .build(), null)
                    .performBy(BINARY_CAT);
        } finally {
            mdsConfig.setPassThrough(false);
        }
        updateHierarchy();
        DiQuotaChangeRequest request = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .replaceQueryParam("expand", ExpandQuotaChangeRequest.BASE_RESOURCES)
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(request);
        Assertions.assertNotNull(request.getBaseResourceChanges());
        Assertions.assertEquals(2, request.getBaseResourceChanges().size());
        List<DiQuotaChangeRequest.BaseResourceChange> baseResourceChanges = new ArrayList<>(request
                .getBaseResourceChanges()).stream().sorted(Comparator.comparing(v -> v.getBaseResource().getId()))
                .collect(Collectors.toList());
        Assertions.assertEquals(bigOrderOne.getId(), baseResourceChanges.get(0).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdOne.longValue(), baseResourceChanges.get(0).getBaseResource().getId());
        Assertions.assertEquals(1181L, baseResourceChanges.get(0).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, baseResourceChanges.get(0).getTotalAmount().getUnit());
        Assertions.assertEquals(1, baseResourceChanges.get(0).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(1181L, new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(baseResourceChanges
                .get(0).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdOne.longValue(), new ArrayList<>(new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds()).get(0));
        Assertions.assertEquals(bigOrderOne.getId(), baseResourceChanges.get(1).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdTwo.longValue(), baseResourceChanges.get(1).getBaseResource().getId());
        Assertions.assertEquals(2L, baseResourceChanges.get(1).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.GIBIBYTE_BASE, baseResourceChanges.get(1).getTotalAmount().getUnit());
        Assertions.assertEquals(1, baseResourceChanges.get(1).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(baseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(2L, new ArrayList<>(baseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.GIBIBYTE_BASE, new ArrayList<>(baseResourceChanges
                .get(1).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(baseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdTwo.longValue(), new ArrayList<>(new ArrayList<>(baseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getMappingIds()).get(0));
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderTwoResourcesMdsMappingRefresh() {
        MockMdsConfigApi mdsConfig = (MockMdsConfigApi) mdsConfigApi;
        Campaign activeCampaign = campaignDao.getAllSorted(Set.of(Campaign.Status.ACTIVE))
                .stream().findFirst().get();
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong mappingIdOne = new MutableLong();
        MutableLong mappingIdTwo = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Resource resourceThree = resourceDao.read(new Resource.Key(SEGMENT_HDD, yp));
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long typeTwo = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-2")
                    .name("Test-2")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.STORAGE_BASE)
            ).getId();
            long baseResourceOne = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            long mappingOne = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(7)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(4)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            long mappingTwo = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .mds(BaseResourceMdsRelation.builder()
                                    .addTerm(BaseResourceMdsRelationTerm.builder()
                                            .resourceId(resourceThree.getId())
                                            .numerator(1)
                                            .denominator(1024L * 1024L * 1024L)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .storageType(MdsStorageType.MDS)
                                            .location(MdsLocation.SAS)
                                            .build())
                                    .build())
                            .build())).getId();
            baseResourceIdOne.setValue(baseResourceOne);
            baseResourceIdTwo.setValue(baseResourceTwo);
            mappingIdOne.setValue(mappingOne);
            mappingIdTwo.setValue(mappingTwo);
        });
        DiListResponse<DiQuotaChangeRequest> quotaRequests;
        mdsConfig.setPassThrough(true);
        try {
            quotaRequests = dispenser().quotaChangeRequests()
                    .batchCreate(List.of(requestBodyBuilderWithDefaultFields()
                            .projectKey(TEST_PROJECT_KEY)
                            .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                    DiAmount.of(1000L, DiUnit.PERMILLE_CORES))
                            .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                    DiAmount.of(1000L, DiUnit.BYTE))
                            .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                    DiAmount.of(2L * 1024L * 1024L * 1024L, DiUnit.BYTE))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                            .build(),
                            requestBodyBuilderWithDefaultFields()
                                    .projectKey(TEST_PROJECT_KEY)
                                    .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                            DiAmount.of(2000L, DiUnit.PERMILLE_CORES))
                                    .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                            DiAmount.of(2000L, DiUnit.BYTE))
                                    .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                            DiAmount.of(4L * 1024L * 1024L * 1024L, DiUnit.BYTE))
                                    .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                                    .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                                    .build()), null)
                    .performBy(BINARY_CAT);
        } finally {
            mdsConfig.setPassThrough(false);
        }
        updateHierarchy();
        DiQuotaChangeRequest requestOne = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .replaceQueryParam("expand", ExpandQuotaChangeRequest.BASE_RESOURCES)
                .get(DiQuotaChangeRequest.class);
        DiQuotaChangeRequest requestTwo = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + new ArrayList<>(quotaRequests.getResults()).get(1).getId())
                .replaceQueryParam("expand", ExpandQuotaChangeRequest.BASE_RESOURCES)
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(requestOne);
        Assertions.assertNotNull(requestOne.getBaseResourceChanges());
        Assertions.assertEquals(2, requestOne.getBaseResourceChanges().size());
        List<DiQuotaChangeRequest.BaseResourceChange> baseResourceChangesOne = new ArrayList<>(requestOne
                .getBaseResourceChanges()).stream().sorted(Comparator.comparing(v -> v.getBaseResource().getId()))
                .collect(Collectors.toList());
        Assertions.assertEquals(bigOrderOne.getId(), baseResourceChangesOne.get(0).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdOne.longValue(), baseResourceChangesOne.get(0).getBaseResource().getId());
        Assertions.assertEquals(1181L, baseResourceChangesOne.get(0).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, baseResourceChangesOne.get(0).getTotalAmount().getUnit());
        Assertions.assertEquals(1, baseResourceChangesOne.get(0).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(baseResourceChangesOne.get(0)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(1181L, new ArrayList<>(baseResourceChangesOne.get(0)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(baseResourceChangesOne
                .get(0).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(baseResourceChangesOne.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdOne.longValue(), new ArrayList<>(new ArrayList<>(baseResourceChangesOne.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds()).get(0));
        Assertions.assertEquals(bigOrderOne.getId(), baseResourceChangesOne.get(1).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdTwo.longValue(), baseResourceChangesOne.get(1).getBaseResource().getId());
        Assertions.assertEquals(2L, baseResourceChangesOne.get(1).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.GIBIBYTE_BASE, baseResourceChangesOne.get(1).getTotalAmount().getUnit());
        Assertions.assertEquals(1, baseResourceChangesOne.get(1).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(baseResourceChangesOne.get(1)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(2L, new ArrayList<>(baseResourceChangesOne.get(1)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.GIBIBYTE_BASE, new ArrayList<>(baseResourceChangesOne
                .get(1).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(baseResourceChangesOne.get(1)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdTwo.longValue(), new ArrayList<>(new ArrayList<>(baseResourceChangesOne.get(1)
                .getPerProviderAmounts()).get(0).getMappingIds()).get(0));
        Assertions.assertNotNull(requestTwo);
        Assertions.assertNotNull(requestTwo.getBaseResourceChanges());
        Assertions.assertEquals(2, requestTwo.getBaseResourceChanges().size());
        List<DiQuotaChangeRequest.BaseResourceChange> baseResourceChangesTwo = new ArrayList<>(requestTwo
                .getBaseResourceChanges()).stream().sorted(Comparator.comparing(v -> v.getBaseResource().getId()))
                .collect(Collectors.toList());
        Assertions.assertEquals(bigOrderOne.getId(), baseResourceChangesTwo.get(0).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdOne.longValue(), baseResourceChangesTwo.get(0).getBaseResource().getId());
        Assertions.assertEquals(2360L, baseResourceChangesTwo.get(0).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, baseResourceChangesTwo.get(0).getTotalAmount().getUnit());
        Assertions.assertEquals(1, baseResourceChangesTwo.get(0).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(baseResourceChangesTwo.get(0)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(2360L, new ArrayList<>(baseResourceChangesTwo.get(0)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(baseResourceChangesTwo
                .get(0).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(baseResourceChangesTwo.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdOne.longValue(), new ArrayList<>(new ArrayList<>(baseResourceChangesTwo.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds()).get(0));
        Assertions.assertEquals(bigOrderOne.getId(), baseResourceChangesTwo.get(1).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdTwo.longValue(), baseResourceChangesTwo.get(1).getBaseResource().getId());
        Assertions.assertEquals(4L, baseResourceChangesTwo.get(1).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.GIBIBYTE_BASE, baseResourceChangesTwo.get(1).getTotalAmount().getUnit());
        Assertions.assertEquals(1, baseResourceChangesTwo.get(1).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(baseResourceChangesTwo.get(1)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(4L, new ArrayList<>(baseResourceChangesTwo.get(1)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.GIBIBYTE_BASE, new ArrayList<>(baseResourceChangesTwo
                .get(1).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(baseResourceChangesTwo.get(1)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdTwo.longValue(), new ArrayList<>(new ArrayList<>(baseResourceChangesTwo.get(1)
                .getPerProviderAmounts()).get(0).getMappingIds()).get(0));
        TransactionWrapper.INSTANCE.execute(() -> {
            baseResourceChangeDao.clear();
        });
        mdsConfig.setPassThrough(true);
        try {
            Response refreshResponse = createAuthorizedLocalClient(AMOSOV_F)
                    .path("/admin/requests/_refreshBaseResourceChanges")
                    .replaceQueryParam("campaignIds", String.valueOf(activeCampaign.getId()))
                    .invoke(HttpMethod.POST, null, Response.class);
            Assertions.assertEquals(204, refreshResponse.getStatus());
        } finally {
            mdsConfig.setPassThrough(false);
        }
        DiQuotaChangeRequest updatedRequestOne = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .replaceQueryParam("expand", ExpandQuotaChangeRequest.BASE_RESOURCES)
                .get(DiQuotaChangeRequest.class);
        DiQuotaChangeRequest updatedRequestTwo = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + new ArrayList<>(quotaRequests.getResults()).get(1).getId())
                .replaceQueryParam("expand", ExpandQuotaChangeRequest.BASE_RESOURCES)
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(updatedRequestOne);
        Assertions.assertNotNull(updatedRequestOne.getBaseResourceChanges());
        Assertions.assertEquals(2, updatedRequestOne.getBaseResourceChanges().size());
        List<DiQuotaChangeRequest.BaseResourceChange> updatedBaseResourceChangesOne = new ArrayList<>(updatedRequestOne
                .getBaseResourceChanges()).stream().sorted(Comparator.comparing(v -> v.getBaseResource().getId()))
                .collect(Collectors.toList());
        Assertions.assertEquals(bigOrderOne.getId(), updatedBaseResourceChangesOne.get(0).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdOne.longValue(), updatedBaseResourceChangesOne.get(0)
                .getBaseResource().getId());
        Assertions.assertEquals(1181L, updatedBaseResourceChangesOne.get(0).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, updatedBaseResourceChangesOne.get(0).getTotalAmount().getUnit());
        Assertions.assertEquals(1, updatedBaseResourceChangesOne.get(0).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(updatedBaseResourceChangesOne.get(0)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(1181L, new ArrayList<>(updatedBaseResourceChangesOne.get(0)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(updatedBaseResourceChangesOne
                .get(0).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(updatedBaseResourceChangesOne.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdOne.longValue(), new ArrayList<>(new ArrayList<>(updatedBaseResourceChangesOne
                .get(0).getPerProviderAmounts()).get(0).getMappingIds()).get(0));
        Assertions.assertEquals(bigOrderOne.getId(), updatedBaseResourceChangesOne.get(1).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdTwo.longValue(), updatedBaseResourceChangesOne.get(1).getBaseResource()
                .getId());
        Assertions.assertEquals(2L, updatedBaseResourceChangesOne.get(1).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.GIBIBYTE_BASE, updatedBaseResourceChangesOne.get(1).getTotalAmount().getUnit());
        Assertions.assertEquals(1, updatedBaseResourceChangesOne.get(1).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(updatedBaseResourceChangesOne.get(1)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(2L, new ArrayList<>(updatedBaseResourceChangesOne.get(1)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.GIBIBYTE_BASE, new ArrayList<>(updatedBaseResourceChangesOne
                .get(1).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(updatedBaseResourceChangesOne.get(1)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdTwo.longValue(), new ArrayList<>(new ArrayList<>(updatedBaseResourceChangesOne
                .get(1).getPerProviderAmounts()).get(0).getMappingIds()).get(0));
        Assertions.assertNotNull(updatedRequestTwo);
        Assertions.assertNotNull(updatedRequestTwo.getBaseResourceChanges());
        Assertions.assertEquals(2, updatedRequestTwo.getBaseResourceChanges().size());
        List<DiQuotaChangeRequest.BaseResourceChange> updatedBaseResourceChangesTwo = new ArrayList<>(updatedRequestTwo
                .getBaseResourceChanges()).stream().sorted(Comparator.comparing(v -> v.getBaseResource().getId()))
                .collect(Collectors.toList());
        Assertions.assertEquals(bigOrderOne.getId(), updatedBaseResourceChangesTwo.get(0).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdOne.longValue(), updatedBaseResourceChangesTwo.get(0).getBaseResource()
                .getId());
        Assertions.assertEquals(2360L, updatedBaseResourceChangesTwo.get(0).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, updatedBaseResourceChangesTwo.get(0).getTotalAmount().getUnit());
        Assertions.assertEquals(1, updatedBaseResourceChangesTwo.get(0).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(updatedBaseResourceChangesTwo.get(0)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(2360L, new ArrayList<>(updatedBaseResourceChangesTwo.get(0)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(updatedBaseResourceChangesTwo
                .get(0).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(updatedBaseResourceChangesTwo.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdOne.longValue(), new ArrayList<>(new ArrayList<>(updatedBaseResourceChangesTwo
                .get(0).getPerProviderAmounts()).get(0).getMappingIds()).get(0));
        Assertions.assertEquals(bigOrderOne.getId(), updatedBaseResourceChangesTwo.get(1).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdTwo.longValue(), updatedBaseResourceChangesTwo.get(1).getBaseResource()
                .getId());
        Assertions.assertEquals(4L, updatedBaseResourceChangesTwo.get(1).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.GIBIBYTE_BASE, updatedBaseResourceChangesTwo.get(1).getTotalAmount().getUnit());
        Assertions.assertEquals(1, updatedBaseResourceChangesTwo.get(1).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(updatedBaseResourceChangesTwo.get(1)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(4L, new ArrayList<>(updatedBaseResourceChangesTwo.get(1)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.GIBIBYTE_BASE, new ArrayList<>(updatedBaseResourceChangesTwo
                .get(1).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(updatedBaseResourceChangesTwo.get(1)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdTwo.longValue(), new ArrayList<>(new ArrayList<>(updatedBaseResourceChangesTwo
                .get(1).getPerProviderAmounts()).get(0).getMappingIds()).get(0));
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderPaginatedExtraReportFields() {
        Campaign activeCampaign = campaignDao.getAllSorted(Set.of(Campaign.Status.ACTIVE))
                .stream().findFirst().get();
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        MutableLong baseResourceId = new MutableLong();
        MutableLong mappingId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Resource resourceThree = resourceDao.read(new Resource.Key(SEGMENT_HDD, yp));
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long typeTwo = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-2")
                    .name("Test-2")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResourceOne = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            long mappingOne = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(7)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(4)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceThree.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build()));
            baseResourceId.setValue(baseResourceOne);
            mappingId.setValue(mappingOne);
        });
        Project project = projectDao.read(YANDEX);
        Project valueStreamProject = projectDao.createIfAbsent(Project
                .withKey("valueStream")
                .name("Value stream")
                .description("Value stream")
                .parent(project)
                .abcServiceId(42)
                .build());
        projectDao.attach(personDao.read(BINARY_CAT.getLogin()), valueStreamProject, Role.RESPONSIBLE);
        projectDao.attach(personDao.read(SLONNN.getLogin()), valueStreamProject, Role.STEWARD);
        projectDao.attach(personDao.read(QDEEE.getLogin()), valueStreamProject, Role.VS_LEADER);
        for (int i = 1; i <= 10; i++) {
            final String name = "Project_" + i;
            project = projectDao.createIfAbsent(Project.withKey(name).name(name).description(name).parent(project)
                    .valueStreamAbcServiceId(valueStreamProject.getAbcServiceId().longValue()).abcServiceId(144).build());
            projectDao.attach(personDao.read(AMOSOV_F.getLogin()), project, Role.RESPONSIBLE);
        }
        updateHierarchy();
        DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                DiAmount.of(1000L, DiUnit.PERMILLE_CORES))
                        .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                DiAmount.of(1000L, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);
        updateHierarchy();
        DiListPageResponse<DiQuotaChangeRequest> request = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests")
                .replaceQueryParam("expand", ExpandQuotaChangeRequest.BASE_RESOURCES,
                        ExpandQuotaChangeRequest.EXTRA_REPORT_FIELDS)
                .replaceQueryParam("pagination", true)
                .get(new GenericType<DiListPageResponse<DiQuotaChangeRequest>>() { });
        Assertions.assertNotNull(request);
        Assertions.assertNotNull(request.getFirst());
        Assertions.assertNotNull(request.getFirst().getBaseResourceChanges());
        Assertions.assertEquals(1, request.getFirst().getBaseResourceChanges().size());
        Assertions.assertEquals(bigOrderOne.getId(),
                new ArrayList<>(request.getFirst().getBaseResourceChanges()).get(0).getBigOrder().getId());
        Assertions.assertEquals(baseResourceId.longValue(),
                new ArrayList<>(request.getFirst().getBaseResourceChanges()).get(0).getBaseResource().getId());
        Assertions.assertEquals(1181L,
                new ArrayList<>(request.getFirst().getBaseResourceChanges()).get(0).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES,
                new ArrayList<>(request.getFirst().getBaseResourceChanges()).get(0).getTotalAmount().getUnit());
        Assertions.assertEquals(1,
                new ArrayList<>(request.getFirst().getBaseResourceChanges()).get(0).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(new ArrayList<>(request.getFirst().getBaseResourceChanges()).get(0)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(1181L, new ArrayList<>(new ArrayList<>(request.getFirst()
                .getBaseResourceChanges()).get(0).getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(new ArrayList<>(request.getFirst()
                .getBaseResourceChanges()).get(0).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(new ArrayList<>(request.getFirst()
                .getBaseResourceChanges()).get(0).getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingId.longValue(), new ArrayList<>(new ArrayList<>(new ArrayList<>(request
                .getFirst().getBaseResourceChanges()).get(0).getPerProviderAmounts()).get(0).getMappingIds()).get(0));
        Assertions.assertTrue(request.getFirst().getExtraReportFields().isPresent());
        Assertions.assertEquals(Set.of(AMOSOV_F.getLogin()),
                request.getFirst().getExtraReportFields().get().getHeadFirst());
        Assertions.assertEquals(Set.of(AMOSOV_F.getLogin()),
                request.getFirst().getExtraReportFields().get().getHeadSecond());
        Assertions.assertEquals(Set.of(AMOSOV_F.getLogin()),
                request.getFirst().getExtraReportFields().get().getHeadThird());
        Assertions.assertEquals("Project_1",
                request.getFirst().getExtraReportFields().get().getHeadDepartmentFirst().get().getKey());
        Assertions.assertEquals("Project_2",
                request.getFirst().getExtraReportFields().get().getHeadDepartmentSecond().get().getKey());
        Assertions.assertEquals("Project_9",
                request.getFirst().getExtraReportFields().get().getHeadDepartmentThird().get().getKey());
        Assertions.assertEquals("valueStream",
                request.getFirst().getExtraReportFields().get().getValueStream().get().getKey());
        Assertions.assertEquals(Set.of(BINARY_CAT.getLogin()),
                request.getFirst().getExtraReportFields().get().getValueStreamCapacityPlanner());
        Assertions.assertEquals(Set.of(QDEEE.getLogin()),
                request.getFirst().getExtraReportFields().get().getValueStreamLeader());
        Assertions.assertEquals(Set.of(SLONNN.getLogin()),
                request.getFirst().getExtraReportFields().get().getValueStreamManager());
    }

    @Test
    public void quotaRequestWithTypeResourcePreorderTwoResourcesMdsMappingFixedService() {
        MockMdsConfigApi mdsConfig = (MockMdsConfigApi) mdsConfigApi;
        Campaign activeCampaign = campaignDao.getAllSorted(Set.of(Campaign.Status.ACTIVE))
                .stream().findFirst().get();
        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong mappingIdOne = new MutableLong();
        MutableLong mappingIdTwo = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Resource resourceThree = resourceDao.read(new Resource.Key(SEGMENT_HDD, yp));
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long typeTwo = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-2")
                    .name("Test-2")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.STORAGE_BASE)
            ).getId();
            long baseResourceOne = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            long mappingOne = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(7)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(4)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            long mappingTwo = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(activeCampaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .mds(BaseResourceMdsRelation.builder()
                                    .addTerm(BaseResourceMdsRelationTerm.builder()
                                            .resourceId(resourceThree.getId())
                                            .numerator(1)
                                            .denominator(1024L * 1024L * 1024L)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .storageType(MdsStorageType.MDS)
                                            .location(MdsLocation.SAS)
                                            .abcServiceId((long) yp.getAbcServiceId())
                                            .build())
                                    .build())
                            .build())).getId();
            baseResourceIdOne.setValue(baseResourceOne);
            baseResourceIdTwo.setValue(baseResourceTwo);
            mappingIdOne.setValue(mappingOne);
            mappingIdTwo.setValue(mappingTwo);
        });
        DiListResponse<DiQuotaChangeRequest> quotaRequests;
        mdsConfig.setPassThrough(true);
        try {
            quotaRequests = dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .projectKey(TEST_PROJECT_KEY)
                            .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                    DiAmount.of(1000L, DiUnit.PERMILLE_CORES))
                            .changes(YP, SEGMENT_STORAGE, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                    DiAmount.of(1000L, DiUnit.BYTE))
                            .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), Set.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1),
                                    DiAmount.of(2L * 1024L * 1024L * 1024L, DiUnit.BYTE))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                            .build(), null)
                    .performBy(BINARY_CAT);
        } finally {
            mdsConfig.setPassThrough(false);
        }
        updateHierarchy();
        DiQuotaChangeRequest request = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/quota-requests/" + quotaRequests.getFirst().getId())
                .replaceQueryParam("expand", ExpandQuotaChangeRequest.BASE_RESOURCES)
                .get(DiQuotaChangeRequest.class);
        Assertions.assertNotNull(request);
        Assertions.assertNotNull(request.getBaseResourceChanges());
        Assertions.assertEquals(2, request.getBaseResourceChanges().size());
        List<DiQuotaChangeRequest.BaseResourceChange> baseResourceChanges = new ArrayList<>(request
                .getBaseResourceChanges()).stream().sorted(Comparator.comparing(v -> v.getBaseResource().getId()))
                .collect(Collectors.toList());
        Assertions.assertEquals(bigOrderOne.getId(), baseResourceChanges.get(0).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdOne.longValue(), baseResourceChanges.get(0).getBaseResource().getId());
        Assertions.assertEquals(1181L, baseResourceChanges.get(0).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, baseResourceChanges.get(0).getTotalAmount().getUnit());
        Assertions.assertEquals(1, baseResourceChanges.get(0).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(1181L, new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, new ArrayList<>(baseResourceChanges
                .get(0).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdOne.longValue(), new ArrayList<>(new ArrayList<>(baseResourceChanges.get(0)
                .getPerProviderAmounts()).get(0).getMappingIds()).get(0));
        Assertions.assertEquals(bigOrderOne.getId(), baseResourceChanges.get(1).getBigOrder().getId());
        Assertions.assertEquals(baseResourceIdTwo.longValue(), baseResourceChanges.get(1).getBaseResource().getId());
        Assertions.assertEquals(2L, baseResourceChanges.get(1).getTotalAmount().getValue());
        Assertions.assertEquals(DiUnit.GIBIBYTE_BASE, baseResourceChanges.get(1).getTotalAmount().getUnit());
        Assertions.assertEquals(1, baseResourceChanges.get(1).getPerProviderAmounts().size());
        Assertions.assertEquals(YP, new ArrayList<>(baseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getProvider().getKey());
        Assertions.assertEquals(2L, new ArrayList<>(baseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getAmount().getValue());
        Assertions.assertEquals(DiUnit.GIBIBYTE_BASE, new ArrayList<>(baseResourceChanges
                .get(1).getPerProviderAmounts()).get(0).getAmount().getUnit());
        Assertions.assertEquals(1, new ArrayList<>(baseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getMappingIds().size());
        Assertions.assertEquals(mappingIdTwo.longValue(), new ArrayList<>(new ArrayList<>(baseResourceChanges.get(1)
                .getPerProviderAmounts()).get(0).getMappingIds()).get(0));
    }

    private DiQuotaChangeRequest updateRequest(final long reqId, BodyUpdate bodyUpdate, final DiPerson person) {
        return createAuthorizedLocalClient(person)
                .path("/v1/quota-requests/" + reqId)
                .invoke(HttpMethod.PATCH, bodyUpdate, DiQuotaChangeRequest.class);
    }

    private Campaign.Builder defaultCampaignBuilder() {
        return Campaign.builder()
                .setKey(TEST_CAMPAIGN_KEY)
                .setName("Test")
                .setStatus(Campaign.Status.ACTIVE)
                .setStartDate(TEST_BIG_ORDER_DATE)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE)))
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

    private BotCampaignGroup.Builder defaultCampaignGroupBuilder() {
        return BotCampaignGroup.builder()
                .setKey("test_campaign_group")
                .setName("Test Campaign Group")
                .setActive(true)
                .setBotPreOrderIssueKey("DISPENSERREQ-1");
    }

}
