package ru.yandex.qe.dispenser.ws.base_resources;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResource;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceLinearRelation;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceLinearRelationTerm;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceMapping;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceRelation;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceType;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceDao;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceMappingDao;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceTypeDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.ws.base_resources.model.BaseResourceMappingsPageDto;
import ru.yandex.qe.dispenser.ws.base_resources.model.ExpandBaseResourceMapping;
import ru.yandex.qe.dispenser.ws.base_resources.model.SingleBaseResourceMappingDto;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;
import ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest;

import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class BaseResourceMappingsServiceTest extends BaseQuotaRequestTest {

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

    @Test
    public void testGetFirstPage() {
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Resource resourceThree = resourceDao.read(new Resource.Key(SEGMENT_HDD, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
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
            baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceOne)
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
                            .build()));
            baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(campaign.getId())
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
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-mappings")
                .replaceQueryParam("expand", ExpandBaseResourceMapping.BASE_RESOURCES,
                        ExpandBaseResourceMapping.BASE_RESOURCE_TYPES, ExpandBaseResourceMapping.PROVIDERS,
                        ExpandBaseResourceMapping.CAMPAIGNS, ExpandBaseResourceMapping.BIG_ORDERS,
                        ExpandBaseResourceMapping.SEGMENTS, ExpandBaseResourceMapping.RESOURCES)
                .replaceQueryParam("limit", 50)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceMappingsPageDto result = response.readEntity(BaseResourceMappingsPageDto.class);
        Assertions.assertEquals(2, result.getMappings().size());
        Assertions.assertEquals(2, result.getBaseResources().size());
        Assertions.assertEquals(2, result.getBaseResourceTypes().size());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertEquals(3, result.getResources().size());
        Assertions.assertEquals(1, result.getCampaigns().size());
        Assertions.assertEquals(1, result.getBigOrders().size());
    }

    @Test
    public void testGetNextPage() {
        Long nextFrom = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Resource resourceThree = resourceDao.read(new Resource.Key(SEGMENT_HDD, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
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
            BaseResourceMapping mappingOne = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceOne)
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
                            .build()));
            baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(campaign.getId())
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
            return mappingOne.getId();
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-mappings")
                .replaceQueryParam("expand", ExpandBaseResourceMapping.BASE_RESOURCES,
                        ExpandBaseResourceMapping.BASE_RESOURCE_TYPES, ExpandBaseResourceMapping.PROVIDERS,
                        ExpandBaseResourceMapping.CAMPAIGNS, ExpandBaseResourceMapping.BIG_ORDERS,
                        ExpandBaseResourceMapping.SEGMENTS, ExpandBaseResourceMapping.RESOURCES)
                .replaceQueryParam("limit", 50)
                .replaceQueryParam("from", nextFrom)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceMappingsPageDto result = response.readEntity(BaseResourceMappingsPageDto.class);
        Assertions.assertEquals(1, result.getMappings().size());
        Assertions.assertEquals(1, result.getBaseResources().size());
        Assertions.assertEquals(1, result.getBaseResourceTypes().size());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertEquals(1, result.getResources().size());
        Assertions.assertEquals(1, result.getCampaigns().size());
        Assertions.assertEquals(1, result.getBigOrders().size());
    }

    @Test
    public void testGetLastPage() {
        Long nextFrom = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Resource resourceThree = resourceDao.read(new Resource.Key(SEGMENT_HDD, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
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
            baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceOne)
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
                            .build()));
            BaseResourceMapping mappingTwo = baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(campaign.getId())
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
            return mappingTwo.getId();
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-mappings")
                .replaceQueryParam("expand", ExpandBaseResourceMapping.BASE_RESOURCES,
                        ExpandBaseResourceMapping.BASE_RESOURCE_TYPES, ExpandBaseResourceMapping.PROVIDERS,
                        ExpandBaseResourceMapping.CAMPAIGNS, ExpandBaseResourceMapping.BIG_ORDERS,
                        ExpandBaseResourceMapping.SEGMENTS, ExpandBaseResourceMapping.RESOURCES)
                .replaceQueryParam("limit", 50)
                .replaceQueryParam("from", nextFrom)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceMappingsPageDto result = response.readEntity(BaseResourceMappingsPageDto.class);
        Assertions.assertEquals(0, result.getMappings().size());
        Assertions.assertNull(result.getBaseResources());
        Assertions.assertNull(result.getBaseResourceTypes());
        Assertions.assertNull(result.getSegments());
        Assertions.assertNull(result.getProviders());
        Assertions.assertNull(result.getResources());
        Assertions.assertNull(result.getCampaigns());
        Assertions.assertNull(result.getBigOrders());
    }

    @Test
    public void testGetFirstPageNoExpand() {
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Resource resourceThree = resourceDao.read(new Resource.Key(SEGMENT_HDD, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
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
            baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceOne)
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
                            .build()));
            baseResourceMappingDao.create(BaseResourceMapping.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(campaign.getId())
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
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-mappings")
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceMappingsPageDto result = response.readEntity(BaseResourceMappingsPageDto.class);
        Assertions.assertEquals(2, result.getMappings().size());
        Assertions.assertNull(result.getBaseResources());
        Assertions.assertNull(result.getBaseResourceTypes());
        Assertions.assertNull(result.getSegments());
        Assertions.assertNull(result.getProviders());
        Assertions.assertNull(result.getResources());
        Assertions.assertNull(result.getCampaigns());
        Assertions.assertNull(result.getBigOrders());
    }

    @Test
    public void testById() {
        Long id = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResource = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            return baseResourceMappingDao.create(BaseResourceMapping.builder()
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
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-mappings/" + id)
                .replaceQueryParam("expand", ExpandBaseResourceMapping.BASE_RESOURCES,
                        ExpandBaseResourceMapping.BASE_RESOURCE_TYPES, ExpandBaseResourceMapping.PROVIDERS,
                        ExpandBaseResourceMapping.CAMPAIGNS, ExpandBaseResourceMapping.BIG_ORDERS,
                        ExpandBaseResourceMapping.SEGMENTS, ExpandBaseResourceMapping.RESOURCES)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceMappingDto result = response.readEntity(SingleBaseResourceMappingDto.class);
        Assertions.assertEquals(id, Long.valueOf(result.getMapping().getId()));
        Assertions.assertTrue(result.getBaseResource().isPresent());
        Assertions.assertTrue(result.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertEquals(2, result.getResources().size());
        Assertions.assertTrue(result.getCampaign().isPresent());
        Assertions.assertTrue(result.getBigOrder().isPresent());
    }

    @Test
    public void testByIdNoExpand() {
        Long id = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResource = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            return baseResourceMappingDao.create(BaseResourceMapping.builder()
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
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-mappings/" + id)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceMappingDto result = response.readEntity(SingleBaseResourceMappingDto.class);
        Assertions.assertEquals(id, Long.valueOf(result.getMapping().getId()));
        Assertions.assertFalse(result.getBaseResource().isPresent());
        Assertions.assertFalse(result.getBaseResourceType().isPresent());
        Assertions.assertNull(result.getSegments());
        Assertions.assertNull(result.getProviders());
        Assertions.assertNull(result.getResources());
        Assertions.assertFalse(result.getCampaign().isPresent());
        Assertions.assertFalse(result.getBigOrder().isPresent());
    }

    @Test
    public void testByIdNotFound() {
        Long id = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Resource resourceOne = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));
            Resource resourceTwo = resourceDao.read(new Resource.Key(SEGMENT_STORAGE, yp));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            long baseResource = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
            return baseResourceMappingDao.create(BaseResourceMapping.builder()
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
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-mappings/" + (id + 999L))
                .replaceQueryParam("expand", ExpandBaseResourceMapping.BASE_RESOURCES,
                        ExpandBaseResourceMapping.BASE_RESOURCE_TYPES, ExpandBaseResourceMapping.PROVIDERS,
                        ExpandBaseResourceMapping.CAMPAIGNS, ExpandBaseResourceMapping.BIG_ORDERS,
                        ExpandBaseResourceMapping.SEGMENTS, ExpandBaseResourceMapping.RESOURCES)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

}
