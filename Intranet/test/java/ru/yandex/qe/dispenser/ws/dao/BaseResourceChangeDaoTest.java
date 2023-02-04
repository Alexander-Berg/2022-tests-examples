package ru.yandex.qe.dispenser.ws.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResource;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceChange;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceChangeByService;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceLinearRelation;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceLinearRelationTerm;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceMapping;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceRelation;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceType;
import ru.yandex.qe.dispenser.domain.base_resources.ServiceBaseResourceChange;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceChangeDao;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceDao;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceMappingDao;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceTypeDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.project.ProjectDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;
import ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest;

import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class BaseResourceChangeDaoTest extends BaseQuotaRequestTest {

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
    private CampaignDao campaignDao;
    @Autowired
    private BigOrderManager bigOrderManager;
    @Autowired
    private QuotaChangeRequestDao quotaChangeRequestDao;
    @Autowired
    private PersonDao personDao;
    @Autowired
    private ProjectDao projectDao;
    @Autowired
    private BaseResourceChangeDao baseResourceChangeDao;

    @Test
    public void testCreate() {
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            BigOrder bigOrder = bigOrderManager.getById(bigOrderOne.getId());
            long baseResourceType = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResource = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(baseResourceType)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceMapping = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResource)
                    .campaignId(campaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            Person author = personDao.readPersonByLogin(AMOSOV_F.getLogin());
            Project project = projectDao.read(INFRA);
            long quotaRequest = quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                    .author(author)
                    .status(QuotaChangeRequest.Status.NEW)
                    .created(Instant.now().getMillis())
                    .updated(Instant.now().getMillis())
                    .project(project)
                    .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                    .calculations("Test")
                    .additionalProperties(Map.of())
                    .readyForAllocation(false)
                    .cost(0.0d)
                    .requestOwningCost(0L)
                    .chartLinks(List.of())
                    .summary("Test")
                    .campaign(QuotaChangeRequest.Campaign.from(campaign))
                    .campaignType(campaign.getType())
                    .changes(List.of(QuotaChangeRequest.Change.builder()
                            .resource(resourceOne)
                            .segments(Set.of())
                            .order(new QuotaChangeRequest.BigOrder(bigOrderOne.getId(), bigOrder.getDate(), true))
                            .amount(1000L)
                            .amountReady(0L)
                            .amountAllocated(0L)
                            .amountAllocating(0L)
                            .owningCost(BigDecimal.ZERO)
                            .build()))
                    .build()).getId();
            BaseResourceChange result = baseResourceChangeDao.create(BaseResourceChange.builder()
                    .quotaRequestId(quotaRequest)
                    .bigOrderId(bigOrderOne.getId())
                    .baseResourceId(baseResource)
                    .amount(1000L)
                    .amountByService(BaseResourceChangeByService.builder()
                            .addChange(ServiceBaseResourceChange.builder()
                                    .serviceId(yp.getId())
                                    .amount(1000L)
                                    .addMappingId(baseResourceMapping)
                                    .build())
                            .build()));
            Assertions.assertNotNull(result);
        });
    }

    @Test
    public void testCreateMany() {
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            BigOrder bigOrder = bigOrderManager.getById(bigOrderOne.getId());
            long baseResourceType = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResource = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(baseResourceType)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceMapping = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResource)
                    .campaignId(campaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            Person author = personDao.readPersonByLogin(AMOSOV_F.getLogin());
            Project project = projectDao.read(INFRA);
            List<Long> requests = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                requests.add(quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                        .author(author)
                        .status(QuotaChangeRequest.Status.NEW)
                        .created(Instant.now().getMillis())
                        .updated(Instant.now().getMillis())
                        .project(project)
                        .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .calculations("Test")
                        .additionalProperties(Map.of())
                        .readyForAllocation(false)
                        .cost(0.0d)
                        .requestOwningCost(0L)
                        .chartLinks(List.of())
                        .summary("Test")
                        .campaign(QuotaChangeRequest.Campaign.from(campaign))
                        .campaignType(campaign.getType())
                        .changes(List.of(QuotaChangeRequest.Change.builder()
                                .resource(resourceOne)
                                .segments(Set.of())
                                .order(new QuotaChangeRequest.BigOrder(bigOrderOne.getId(), bigOrder.getDate(), true))
                                .amount(1000L)
                                .amountReady(0L)
                                .amountAllocated(0L)
                                .amountAllocating(0L)
                                .owningCost(BigDecimal.ZERO)
                                .build()))
                        .build()).getId());
            }
            List<BaseResourceChange.Builder> toCreate = requests.stream()
                    .map(quotaRequest -> BaseResourceChange.builder()
                            .quotaRequestId(quotaRequest)
                            .bigOrderId(bigOrderOne.getId())
                            .baseResourceId(baseResource)
                            .amount(1000L)
                            .amountByService(BaseResourceChangeByService.builder()
                                    .addChange(ServiceBaseResourceChange.builder()
                                            .serviceId(yp.getId())
                                            .amount(1000L)
                                            .addMappingId(baseResourceMapping)
                                            .build())
                                    .build()))
                    .collect(Collectors.toList());
            Set<BaseResourceChange> result = baseResourceChangeDao.create(toCreate);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(1000, result.size());
        });
    }

    @Test
    public void testGetById() {
        Long changeId = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            BigOrder bigOrder = bigOrderManager.getById(bigOrderOne.getId());
            long baseResourceType = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResource = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(baseResourceType)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceMapping = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResource)
                    .campaignId(campaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            Person author = personDao.readPersonByLogin(AMOSOV_F.getLogin());
            Project project = projectDao.read(INFRA);
            long quotaRequest = quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                    .author(author)
                    .status(QuotaChangeRequest.Status.NEW)
                    .created(Instant.now().getMillis())
                    .updated(Instant.now().getMillis())
                    .project(project)
                    .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                    .calculations("Test")
                    .additionalProperties(Map.of())
                    .readyForAllocation(false)
                    .cost(0.0d)
                    .requestOwningCost(0L)
                    .chartLinks(List.of())
                    .summary("Test")
                    .campaign(QuotaChangeRequest.Campaign.from(campaign))
                    .campaignType(campaign.getType())
                    .changes(List.of(QuotaChangeRequest.Change.builder()
                            .resource(resourceOne)
                            .segments(Set.of())
                            .order(new QuotaChangeRequest.BigOrder(bigOrderOne.getId(), bigOrder.getDate(), true))
                            .amount(1000L)
                            .amountReady(0L)
                            .amountAllocated(0L)
                            .amountAllocating(0L)
                            .owningCost(BigDecimal.ZERO)
                            .build()))
                    .build()).getId();
            BaseResourceChange result = baseResourceChangeDao.create(BaseResourceChange.builder()
                    .quotaRequestId(quotaRequest)
                    .bigOrderId(bigOrderOne.getId())
                    .baseResourceId(baseResource)
                    .amount(1000L)
                    .amountByService(BaseResourceChangeByService.builder()
                            .addChange(ServiceBaseResourceChange.builder()
                                    .serviceId(yp.getId())
                                    .amount(1000L)
                                    .addMappingId(baseResourceMapping)
                                    .build())
                            .build()));
            Assertions.assertNotNull(result);
            return result.getId();
        });
        TransactionWrapper.INSTANCE.execute(() -> {
            Assertions.assertNotNull(baseResourceChangeDao.getById(changeId).get());
        });
    }

    @Test
    public void testGetByIds() {
        Set<Long> ids = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            BigOrder bigOrder = bigOrderManager.getById(bigOrderOne.getId());
            long baseResourceType = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResource = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(baseResourceType)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceMapping = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResource)
                    .campaignId(campaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            Person author = personDao.readPersonByLogin(AMOSOV_F.getLogin());
            Project project = projectDao.read(INFRA);
            List<Long> requests = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                requests.add(quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                        .author(author)
                        .status(QuotaChangeRequest.Status.NEW)
                        .created(Instant.now().getMillis())
                        .updated(Instant.now().getMillis())
                        .project(project)
                        .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .calculations("Test")
                        .additionalProperties(Map.of())
                        .readyForAllocation(false)
                        .cost(0.0d)
                        .requestOwningCost(0L)
                        .chartLinks(List.of())
                        .summary("Test")
                        .campaign(QuotaChangeRequest.Campaign.from(campaign))
                        .campaignType(campaign.getType())
                        .changes(List.of(QuotaChangeRequest.Change.builder()
                                .resource(resourceOne)
                                .segments(Set.of())
                                .order(new QuotaChangeRequest.BigOrder(bigOrderOne.getId(), bigOrder.getDate(), true))
                                .amount(1000L)
                                .amountReady(0L)
                                .amountAllocated(0L)
                                .amountAllocating(0L)
                                .owningCost(BigDecimal.ZERO)
                                .build()))
                        .build()).getId());
            }
            List<BaseResourceChange.Builder> toCreate = requests.stream()
                    .map(quotaRequest -> BaseResourceChange.builder()
                            .quotaRequestId(quotaRequest)
                            .bigOrderId(bigOrderOne.getId())
                            .baseResourceId(baseResource)
                            .amount(1000L)
                            .amountByService(BaseResourceChangeByService.builder()
                                    .addChange(ServiceBaseResourceChange.builder()
                                            .serviceId(yp.getId())
                                            .amount(1000L)
                                            .addMappingId(baseResourceMapping)
                                            .build())
                                    .build()))
                    .collect(Collectors.toList());
            Set<BaseResourceChange> result = baseResourceChangeDao.create(toCreate);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(1000, result.size());
            return result.stream().map(BaseResourceChange::getId).collect(Collectors.toSet());
        });
        TransactionWrapper.INSTANCE.execute(() -> {
            Set<BaseResourceChange> result = baseResourceChangeDao.getByIds(ids);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(1000, result.size());
        });
    }

    @Test
    public void testGetByQuotaRequestId() {
        Long quotaRequestId = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            BigOrder bigOrder = bigOrderManager.getById(bigOrderOne.getId());
            long baseResourceType = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResource = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(baseResourceType)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceMapping = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResource)
                    .campaignId(campaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            Person author = personDao.readPersonByLogin(AMOSOV_F.getLogin());
            Project project = projectDao.read(INFRA);
            long quotaRequest = quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                    .author(author)
                    .status(QuotaChangeRequest.Status.NEW)
                    .created(Instant.now().getMillis())
                    .updated(Instant.now().getMillis())
                    .project(project)
                    .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                    .calculations("Test")
                    .additionalProperties(Map.of())
                    .readyForAllocation(false)
                    .cost(0.0d)
                    .requestOwningCost(0L)
                    .chartLinks(List.of())
                    .summary("Test")
                    .campaign(QuotaChangeRequest.Campaign.from(campaign))
                    .campaignType(campaign.getType())
                    .changes(List.of(QuotaChangeRequest.Change.builder()
                            .resource(resourceOne)
                            .segments(Set.of())
                            .order(new QuotaChangeRequest.BigOrder(bigOrderOne.getId(), bigOrder.getDate(), true))
                            .amount(1000L)
                            .amountReady(0L)
                            .amountAllocated(0L)
                            .amountAllocating(0L)
                            .owningCost(BigDecimal.ZERO)
                            .build()))
                    .build()).getId();
            BaseResourceChange result = baseResourceChangeDao.create(BaseResourceChange.builder()
                    .quotaRequestId(quotaRequest)
                    .bigOrderId(bigOrderOne.getId())
                    .baseResourceId(baseResource)
                    .amount(1000L)
                    .amountByService(BaseResourceChangeByService.builder()
                            .addChange(ServiceBaseResourceChange.builder()
                                    .serviceId(yp.getId())
                                    .amount(1000L)
                                    .addMappingId(baseResourceMapping)
                                    .build())
                            .build()));
            Assertions.assertNotNull(result);
            return result.getQuotaRequestId();
        });
        TransactionWrapper.INSTANCE.execute(() -> {
            Set<BaseResourceChange> result = baseResourceChangeDao.getByQuotaRequestId(quotaRequestId);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(1, result.size());
        });
    }

    @Test
    public void testGetByQuotaRequestIds() {
        Set<Long> quotaRequestIds = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            BigOrder bigOrder = bigOrderManager.getById(bigOrderOne.getId());
            long baseResourceType = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResource = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(baseResourceType)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceMapping = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResource)
                    .campaignId(campaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            Person author = personDao.readPersonByLogin(AMOSOV_F.getLogin());
            Project project = projectDao.read(INFRA);
            List<Long> requests = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                requests.add(quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                        .author(author)
                        .status(QuotaChangeRequest.Status.NEW)
                        .created(Instant.now().getMillis())
                        .updated(Instant.now().getMillis())
                        .project(project)
                        .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .calculations("Test")
                        .additionalProperties(Map.of())
                        .readyForAllocation(false)
                        .cost(0.0d)
                        .requestOwningCost(0L)
                        .chartLinks(List.of())
                        .summary("Test")
                        .campaign(QuotaChangeRequest.Campaign.from(campaign))
                        .campaignType(campaign.getType())
                        .changes(List.of(QuotaChangeRequest.Change.builder()
                                .resource(resourceOne)
                                .segments(Set.of())
                                .order(new QuotaChangeRequest.BigOrder(bigOrderOne.getId(), bigOrder.getDate(), true))
                                .amount(1000L)
                                .amountReady(0L)
                                .amountAllocated(0L)
                                .amountAllocating(0L)
                                .owningCost(BigDecimal.ZERO)
                                .build()))
                        .build()).getId());
            }
            List<BaseResourceChange.Builder> toCreate = requests.stream()
                    .map(quotaRequest -> BaseResourceChange.builder()
                            .quotaRequestId(quotaRequest)
                            .bigOrderId(bigOrderOne.getId())
                            .baseResourceId(baseResource)
                            .amount(1000L)
                            .amountByService(BaseResourceChangeByService.builder()
                                    .addChange(ServiceBaseResourceChange.builder()
                                            .serviceId(yp.getId())
                                            .amount(1000L)
                                            .addMappingId(baseResourceMapping)
                                            .build())
                                    .build()))
                    .collect(Collectors.toList());
            Set<BaseResourceChange> result = baseResourceChangeDao.create(toCreate);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(1000, result.size());
            return result.stream().map(BaseResourceChange::getQuotaRequestId).collect(Collectors.toSet());
        });
        TransactionWrapper.INSTANCE.execute(() -> {
            Set<BaseResourceChange> result = baseResourceChangeDao.getByQuotaRequestIds(quotaRequestIds);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(1000, result.size());
        });
    }

    @Test
    public void updateById() {
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            BigOrder bigOrder = bigOrderManager.getById(bigOrderOne.getId());
            long baseResourceType = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResource = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(baseResourceType)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceMapping = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResource)
                    .campaignId(campaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            Person author = personDao.readPersonByLogin(AMOSOV_F.getLogin());
            Project project = projectDao.read(INFRA);
            long quotaRequest = quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                    .author(author)
                    .status(QuotaChangeRequest.Status.NEW)
                    .created(Instant.now().getMillis())
                    .updated(Instant.now().getMillis())
                    .project(project)
                    .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                    .calculations("Test")
                    .additionalProperties(Map.of())
                    .readyForAllocation(false)
                    .cost(0.0d)
                    .requestOwningCost(0L)
                    .chartLinks(List.of())
                    .summary("Test")
                    .campaign(QuotaChangeRequest.Campaign.from(campaign))
                    .campaignType(campaign.getType())
                    .changes(List.of(QuotaChangeRequest.Change.builder()
                            .resource(resourceOne)
                            .segments(Set.of())
                            .order(new QuotaChangeRequest.BigOrder(bigOrderOne.getId(), bigOrder.getDate(), true))
                            .amount(1000L)
                            .amountReady(0L)
                            .amountAllocated(0L)
                            .amountAllocating(0L)
                            .owningCost(BigDecimal.ZERO)
                            .build()))
                    .build()).getId();
            BaseResourceChange change = baseResourceChangeDao.create(BaseResourceChange.builder()
                    .quotaRequestId(quotaRequest)
                    .bigOrderId(bigOrderOne.getId())
                    .baseResourceId(baseResource)
                    .amount(1000L)
                    .amountByService(BaseResourceChangeByService.builder()
                            .addChange(ServiceBaseResourceChange.builder()
                                    .serviceId(yp.getId())
                                    .amount(1000L)
                                    .addMappingId(baseResourceMapping)
                                    .build())
                            .build()));
            Assertions.assertNotNull(change);
            BaseResourceChange updated = baseResourceChangeDao.update(BaseResourceChange.update(change)
                    .amount(2000L)
                    .amountByService(BaseResourceChangeByService.builder()
                            .addChange(ServiceBaseResourceChange.builder()
                                    .serviceId(yp.getId())
                                    .amount(2000L)
                                    .addMappingId(baseResourceMapping)
                                    .build())
                            .build())).get();
            Assertions.assertNotNull(updated);
            Assertions.assertEquals(2000L, updated.getAmount());
            Assertions.assertEquals(2000L, updated.getAmountByService()
                    .getChangesByService().get(yp.getId()).getAmount());
        });
    }

    @Test
    public void updateByIds() {
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            BigOrder bigOrder = bigOrderManager.getById(bigOrderOne.getId());
            long baseResourceType = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResource = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(baseResourceType)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceMapping = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResource)
                    .campaignId(campaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            Person author = personDao.readPersonByLogin(AMOSOV_F.getLogin());
            Project project = projectDao.read(INFRA);
            List<Long> requests = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                requests.add(quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                        .author(author)
                        .status(QuotaChangeRequest.Status.NEW)
                        .created(Instant.now().getMillis())
                        .updated(Instant.now().getMillis())
                        .project(project)
                        .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .calculations("Test")
                        .additionalProperties(Map.of())
                        .readyForAllocation(false)
                        .cost(0.0d)
                        .requestOwningCost(0L)
                        .chartLinks(List.of())
                        .summary("Test")
                        .campaign(QuotaChangeRequest.Campaign.from(campaign))
                        .campaignType(campaign.getType())
                        .changes(List.of(QuotaChangeRequest.Change.builder()
                                .resource(resourceOne)
                                .segments(Set.of())
                                .order(new QuotaChangeRequest.BigOrder(bigOrderOne.getId(), bigOrder.getDate(), true))
                                .amount(1000L)
                                .amountReady(0L)
                                .amountAllocated(0L)
                                .amountAllocating(0L)
                                .owningCost(BigDecimal.ZERO)
                                .build()))
                        .build()).getId());
            }
            List<BaseResourceChange.Builder> toCreate = requests.stream()
                    .map(quotaRequest -> BaseResourceChange.builder()
                            .quotaRequestId(quotaRequest)
                            .bigOrderId(bigOrderOne.getId())
                            .baseResourceId(baseResource)
                            .amount(1000L)
                            .amountByService(BaseResourceChangeByService.builder()
                                    .addChange(ServiceBaseResourceChange.builder()
                                            .serviceId(yp.getId())
                                            .amount(1000L)
                                            .addMappingId(baseResourceMapping)
                                            .build())
                                    .build()))
                    .collect(Collectors.toList());
            Set<BaseResourceChange> result = baseResourceChangeDao.create(toCreate);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(1000, result.size());
            Set<BaseResourceChange.Update> updates = result.stream().map(r -> BaseResourceChange.update(r)
                    .amount(2000L)
                    .amountByService(BaseResourceChangeByService.builder()
                            .addChange(ServiceBaseResourceChange.builder()
                                    .serviceId(yp.getId())
                                    .amount(2000L)
                                    .addMappingId(baseResourceMapping)
                                    .build())
                            .build())).collect(Collectors.toSet());
            Set<BaseResourceChange> updated = baseResourceChangeDao.update(updates);
            Assertions.assertNotNull(updated);
            Assertions.assertEquals(1000, updated.size());
            Assertions.assertTrue(updated.stream().allMatch(v -> v.getAmount() == 2000L));
            Assertions.assertTrue(updated.stream().allMatch(v -> v.getAmountByService()
                    .getChangesByService().get(yp.getId()).getAmount() == 2000L));
        });
    }

    @Test
    public void testDeleteById() {
        Long changeId = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            BigOrder bigOrder = bigOrderManager.getById(bigOrderOne.getId());
            long baseResourceType = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResource = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(baseResourceType)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceMapping = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResource)
                    .campaignId(campaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            Person author = personDao.readPersonByLogin(AMOSOV_F.getLogin());
            Project project = projectDao.read(INFRA);
            long quotaRequest = quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                    .author(author)
                    .status(QuotaChangeRequest.Status.NEW)
                    .created(Instant.now().getMillis())
                    .updated(Instant.now().getMillis())
                    .project(project)
                    .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                    .calculations("Test")
                    .additionalProperties(Map.of())
                    .readyForAllocation(false)
                    .cost(0.0d)
                    .requestOwningCost(0L)
                    .chartLinks(List.of())
                    .summary("Test")
                    .campaign(QuotaChangeRequest.Campaign.from(campaign))
                    .campaignType(campaign.getType())
                    .changes(List.of(QuotaChangeRequest.Change.builder()
                            .resource(resourceOne)
                            .segments(Set.of())
                            .order(new QuotaChangeRequest.BigOrder(bigOrderOne.getId(), bigOrder.getDate(), true))
                            .amount(1000L)
                            .amountReady(0L)
                            .amountAllocated(0L)
                            .amountAllocating(0L)
                            .owningCost(BigDecimal.ZERO)
                            .build()))
                    .build()).getId();
            BaseResourceChange result = baseResourceChangeDao.create(BaseResourceChange.builder()
                    .quotaRequestId(quotaRequest)
                    .bigOrderId(bigOrderOne.getId())
                    .baseResourceId(baseResource)
                    .amount(1000L)
                    .amountByService(BaseResourceChangeByService.builder()
                            .addChange(ServiceBaseResourceChange.builder()
                                    .serviceId(yp.getId())
                                    .amount(1000L)
                                    .addMappingId(baseResourceMapping)
                                    .build())
                            .build()));
            Assertions.assertNotNull(result);
            return result.getId();
        });
        TransactionWrapper.INSTANCE.execute(() -> {
            Assertions.assertTrue(baseResourceChangeDao.deleteById(changeId).isPresent());
            Assertions.assertTrue(baseResourceChangeDao.getById(changeId).isEmpty());
        });
    }

    @Test
    public void testDeleteByIds() {
        Set<Long> ids = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            BigOrder bigOrder = bigOrderManager.getById(bigOrderOne.getId());
            long baseResourceType = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResource = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(baseResourceType)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            long baseResourceMapping = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResource)
                    .campaignId(campaign.getId())
                    .bigOrderId(bigOrderOne.getId())
                    .serviceId(yp.getId())
                    .relation(BaseResourceRelation.builder()
                            .linear(BaseResourceLinearRelation.builder()
                                    .constantTermNumerator(2)
                                    .constantTermDenominator(1)
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceOne.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .addTerm(BaseResourceLinearRelationTerm.builder()
                                            .resourceId(resourceTwo.getId())
                                            .numerator(3)
                                            .denominator(1)
                                            .addSegmentIds(segmentOne.getId(), segmentTwo.getId())
                                            .build())
                                    .build())
                            .build())).getId();
            Person author = personDao.readPersonByLogin(AMOSOV_F.getLogin());
            Project project = projectDao.read(INFRA);
            List<Long> requests = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                requests.add(quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                        .author(author)
                        .status(QuotaChangeRequest.Status.NEW)
                        .created(Instant.now().getMillis())
                        .updated(Instant.now().getMillis())
                        .project(project)
                        .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .calculations("Test")
                        .additionalProperties(Map.of())
                        .readyForAllocation(false)
                        .cost(0.0d)
                        .requestOwningCost(0L)
                        .chartLinks(List.of())
                        .summary("Test")
                        .campaign(QuotaChangeRequest.Campaign.from(campaign))
                        .campaignType(campaign.getType())
                        .changes(List.of(QuotaChangeRequest.Change.builder()
                                .resource(resourceOne)
                                .segments(Set.of())
                                .order(new QuotaChangeRequest.BigOrder(bigOrderOne.getId(), bigOrder.getDate(), true))
                                .amount(1000L)
                                .amountReady(0L)
                                .amountAllocated(0L)
                                .amountAllocating(0L)
                                .owningCost(BigDecimal.ZERO)
                                .build()))
                        .build()).getId());
            }
            List<BaseResourceChange.Builder> toCreate = requests.stream()
                    .map(quotaRequest -> BaseResourceChange.builder()
                            .quotaRequestId(quotaRequest)
                            .bigOrderId(bigOrderOne.getId())
                            .baseResourceId(baseResource)
                            .amount(1000L)
                            .amountByService(BaseResourceChangeByService.builder()
                                    .addChange(ServiceBaseResourceChange.builder()
                                            .serviceId(yp.getId())
                                            .amount(1000L)
                                            .addMappingId(baseResourceMapping)
                                            .build())
                                    .build()))
                    .collect(Collectors.toList());
            Set<BaseResourceChange> result = baseResourceChangeDao.create(toCreate);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(1000, result.size());
            return result.stream().map(BaseResourceChange::getId).collect(Collectors.toSet());
        });
        TransactionWrapper.INSTANCE.execute(() -> {
            Set<BaseResourceChange> deleted = baseResourceChangeDao.deleteByIds(ids);
            Assertions.assertNotNull(deleted);
            Assertions.assertEquals(1000, deleted.size());
            Set<BaseResourceChange> result = baseResourceChangeDao.getByIds(ids);
            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.isEmpty());
        });
    }

}
