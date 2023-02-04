package ru.yandex.qe.dispenser.ws.base_resources;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResource;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceLimit;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceLinearRelation;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceLinearRelationTerm;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceMapping;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceRelation;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceType;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceDao;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceLimitDao;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceMappingDao;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceTypeDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.ws.base_resources.model.BaseResourceLimitAmountDto;
import ru.yandex.qe.dispenser.ws.base_resources.model.BaseResourceLimitAmountInputDto;
import ru.yandex.qe.dispenser.ws.base_resources.model.BaseResourceLimitKeyDto;
import ru.yandex.qe.dispenser.ws.base_resources.model.BaseResourceLimitReportDto;
import ru.yandex.qe.dispenser.ws.base_resources.model.BaseResourceLimitReportInputDto;
import ru.yandex.qe.dispenser.ws.base_resources.model.BaseResourceLimitsPageDto;
import ru.yandex.qe.dispenser.ws.base_resources.model.BaseResourceLimitsReportDto;
import ru.yandex.qe.dispenser.ws.base_resources.model.BaseResourceLimitsReportInputDto;
import ru.yandex.qe.dispenser.ws.base_resources.model.CreateBaseResourceLimitDto;
import ru.yandex.qe.dispenser.ws.base_resources.model.ExpandBaseResourceLimit;
import ru.yandex.qe.dispenser.ws.base_resources.model.SingleBaseResourceLimitDto;
import ru.yandex.qe.dispenser.ws.base_resources.model.UpdateBaseResourceLimitDto;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;
import ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest;
import ru.yandex.qe.dispenser.ws.param.DiExceptionMapper;

import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class BaseResourceLimitsServiceTest extends BaseQuotaRequestTest {

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
    private BaseResourceLimitDao baseResourceLimitDao;
    @Autowired
    private CampaignDao campaignDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private BaseResourceMappingDao baseResourceMappingDao;

    @Test
    public void testGetFirstPage() {
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
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
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(campaign.getId())
                    .limit(1000));
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(campaign.getId())
                    .limit(2000));
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .replaceQueryParam("limit", 50)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceLimitsPageDto result = response.readEntity(BaseResourceLimitsPageDto.class);
        Assertions.assertEquals(2, result.getLimits().size());
        Assertions.assertEquals(2, result.getBaseResources().size());
        Assertions.assertEquals(2, result.getBaseResourceTypes().size());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertEquals(1, result.getCampaigns().size());
    }

    @Test
    public void testGetNextPage() {
        Long nextFrom = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
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
            BaseResourceLimit limitOne = baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(campaign.getId())
                    .limit(1000));
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(campaign.getId())
                    .limit(2000));
            return limitOne.getId();
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .replaceQueryParam("limit", 50)
                .replaceQueryParam("from", nextFrom)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceLimitsPageDto result = response.readEntity(BaseResourceLimitsPageDto.class);
        Assertions.assertEquals(1, result.getLimits().size());
        Assertions.assertEquals(1, result.getBaseResources().size());
        Assertions.assertEquals(1, result.getBaseResourceTypes().size());
        Assertions.assertNull(result.getSegments());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertEquals(1, result.getCampaigns().size());
    }

    @Test
    public void testGetLastPage() {
        Long nextFrom = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
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
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(campaign.getId())
                    .limit(1000));
            BaseResourceLimit limitTwo = baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(campaign.getId())
                    .limit(2000));
            return limitTwo.getId();
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .replaceQueryParam("limit", 50)
                .replaceQueryParam("from", nextFrom)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceLimitsPageDto result = response.readEntity(BaseResourceLimitsPageDto.class);
        Assertions.assertEquals(0, result.getLimits().size());
        Assertions.assertNull(result.getBaseResources());
        Assertions.assertNull(result.getBaseResourceTypes());
        Assertions.assertNull(result.getSegments());
        Assertions.assertNull(result.getProviders());
        Assertions.assertNull(result.getCampaigns());
    }

    @Test
    public void testGetFirstPageNoExpand() {
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
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
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(campaign.getId())
                    .limit(1000));
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(campaign.getId())
                    .limit(2000));
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits")
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceLimitsPageDto result = response.readEntity(BaseResourceLimitsPageDto.class);
        Assertions.assertEquals(2, result.getLimits().size());
        Assertions.assertNull(result.getBaseResources());
        Assertions.assertNull(result.getBaseResourceTypes());
        Assertions.assertNull(result.getSegments());
        Assertions.assertNull(result.getProviders());
        Assertions.assertNull(result.getCampaigns());
    }

    @Test
    public void testById() {
        Long id = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
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
            return baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResource)
                    .campaignId(campaign.getId())
                    .limit(1000)).getId();
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/" + id)
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceLimitDto result = response.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertEquals(id, Long.valueOf(result.getLimit().getId()));
        Assertions.assertTrue(result.getBaseResource().isPresent());
        Assertions.assertTrue(result.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertTrue(result.getCampaign().isPresent());
    }

    @Test
    public void testByIdNoExpand() {
        Long id = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
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
            return baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResource)
                    .campaignId(campaign.getId())
                    .limit(1000)).getId();
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/" + id)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceLimitDto result = response.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertEquals(id, Long.valueOf(result.getLimit().getId()));
        Assertions.assertFalse(result.getBaseResource().isPresent());
        Assertions.assertFalse(result.getBaseResourceType().isPresent());
        Assertions.assertNull(result.getSegments());
        Assertions.assertNull(result.getProviders());
        Assertions.assertFalse(result.getCampaign().isPresent());
    }

    @Test
    public void testByIdNotFound() {
        Long id = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
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
            return baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResource)
                    .campaignId(campaign.getId())
                    .limit(1000)).getId();
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/" + (id + 999L))
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreate() {
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong campaignId = new MutableLong();
        MutableLong serviceId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            serviceId.setValue(yp.getId());
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            campaignId.setValue(campaign.getId());
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
            baseResourceIdOne.setValue(baseResourceOne);
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            baseResourceIdTwo.setValue(baseResourceTwo);
        });
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceLimitDto result = response.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertTrue(result.getBaseResource().isPresent());
        Assertions.assertTrue(result.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertTrue(result.getCampaign().isPresent());
        Assertions.assertEquals(1000, result.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, result.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), result.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), result.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), result.getLimit().getProviderId());
        Response responseById = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/" + result.getLimit().getId())
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), responseById.getStatus());
        SingleBaseResourceLimitDto resultById = responseById.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertEquals(1000, resultById.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, resultById.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), resultById.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), resultById.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), resultById.getLimit().getProviderId());
    }

    @Test
    public void testCreateInvalid() {
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong campaignId = new MutableLong();
        MutableLong serviceId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            serviceId.setValue(yp.getId());
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            campaignId.setValue(campaign.getId());
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
            baseResourceIdOne.setValue(baseResourceOne);
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            baseResourceIdTwo.setValue(baseResourceTwo);
        });
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.BYTE)));
        Assertions.assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(),
                response.getStatus());
    }

    @Test
    public void testCreateConflict() {
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong campaignId = new MutableLong();
        MutableLong serviceId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            serviceId.setValue(yp.getId());
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            campaignId.setValue(campaign.getId());
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
            baseResourceIdOne.setValue(baseResourceOne);
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            baseResourceIdTwo.setValue(baseResourceTwo);
        });
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceLimitDto result = response.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertTrue(result.getBaseResource().isPresent());
        Assertions.assertTrue(result.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertTrue(result.getCampaign().isPresent());
        Response responseConflict = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.CONFLICT.getStatusCode(), responseConflict.getStatus());
    }

    @Test
    public void testUpdate() {
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong campaignId = new MutableLong();
        MutableLong serviceId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            serviceId.setValue(yp.getId());
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            campaignId.setValue(campaign.getId());
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
            baseResourceIdOne.setValue(baseResourceOne);
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            baseResourceIdTwo.setValue(baseResourceTwo);
        });
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceLimitDto result = response.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertTrue(result.getBaseResource().isPresent());
        Assertions.assertTrue(result.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertTrue(result.getCampaign().isPresent());
        Assertions.assertEquals(1000, result.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, result.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), result.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), result.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), result.getLimit().getProviderId());
        Response responseById = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/" + result.getLimit().getId())
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), responseById.getStatus());
        SingleBaseResourceLimitDto resultById = responseById.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertEquals(1000, resultById.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, resultById.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), resultById.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), resultById.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), resultById.getLimit().getProviderId());
        Response updateResponse = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits/" + result.getLimit().getId())
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.PATCH, new UpdateBaseResourceLimitDto(DiAmount.of(2, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());
        SingleBaseResourceLimitDto updateResult = updateResponse.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertTrue(updateResult.getBaseResource().isPresent());
        Assertions.assertTrue(updateResult.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, updateResult.getSegments().size());
        Assertions.assertEquals(1, updateResult.getProviders().size());
        Assertions.assertTrue(updateResult.getCampaign().isPresent());
        Assertions.assertEquals(2000, updateResult.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, updateResult.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), updateResult.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), updateResult.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), updateResult.getLimit().getProviderId());
        Response updatedResponseById = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/" + result.getLimit().getId())
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), updatedResponseById.getStatus());
        SingleBaseResourceLimitDto updatedResultById = updatedResponseById.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertEquals(2000, updatedResultById.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, updatedResultById.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), updatedResultById.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), updatedResultById.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), updatedResultById.getLimit().getProviderId());
    }

    @Test
    public void testUpdateInvalid() {
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong campaignId = new MutableLong();
        MutableLong serviceId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            serviceId.setValue(yp.getId());
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            campaignId.setValue(campaign.getId());
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
            baseResourceIdOne.setValue(baseResourceOne);
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            baseResourceIdTwo.setValue(baseResourceTwo);
        });
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceLimitDto result = response.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertTrue(result.getBaseResource().isPresent());
        Assertions.assertTrue(result.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertTrue(result.getCampaign().isPresent());
        Response updateResponse = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits/" + result.getLimit().getId())
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.PATCH, new UpdateBaseResourceLimitDto(DiAmount.of(2, DiUnit.BYTE)));
        Assertions.assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(),
                updateResponse.getStatus());
    }

    @Test
    public void testUpdateNotFound() {
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong campaignId = new MutableLong();
        MutableLong serviceId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            serviceId.setValue(yp.getId());
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            campaignId.setValue(campaign.getId());
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
            baseResourceIdOne.setValue(baseResourceOne);
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            baseResourceIdTwo.setValue(baseResourceTwo);
        });
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceLimitDto result = response.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertTrue(result.getBaseResource().isPresent());
        Assertions.assertTrue(result.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertTrue(result.getCampaign().isPresent());
        Response updateResponse = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits/" + result.getLimit().getId() + 999L)
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.PATCH, new UpdateBaseResourceLimitDto(DiAmount.of(2, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), updateResponse.getStatus());
    }

    @Test
    public void testDelete() {
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong campaignId = new MutableLong();
        MutableLong serviceId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            serviceId.setValue(yp.getId());
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            campaignId.setValue(campaign.getId());
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
            baseResourceIdOne.setValue(baseResourceOne);
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            baseResourceIdTwo.setValue(baseResourceTwo);
        });
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceLimitDto result = response.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertTrue(result.getBaseResource().isPresent());
        Assertions.assertTrue(result.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertTrue(result.getCampaign().isPresent());
        Assertions.assertEquals(1000, result.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, result.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), result.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), result.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), result.getLimit().getProviderId());
        Response responseById = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/" + result.getLimit().getId())
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), responseById.getStatus());
        SingleBaseResourceLimitDto resultById = responseById.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertEquals(1000, resultById.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, resultById.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), resultById.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), resultById.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), resultById.getLimit().getProviderId());
        Response responseDeleted = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits/" + result.getLimit().getId())
                .invoke(HttpMethod.DELETE, null);
        Assertions.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), responseDeleted.getStatus());
        Response responseByIdDeleted = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/" + result.getLimit().getId())
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), responseByIdDeleted.getStatus());
    }

    @Test
    public void testDeleteNotFound() {
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong campaignId = new MutableLong();
        MutableLong serviceId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            serviceId.setValue(yp.getId());
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            campaignId.setValue(campaign.getId());
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
            baseResourceIdOne.setValue(baseResourceOne);
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            baseResourceIdTwo.setValue(baseResourceTwo);
        });
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceLimitDto result = response.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertTrue(result.getBaseResource().isPresent());
        Assertions.assertTrue(result.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertTrue(result.getCampaign().isPresent());
        Assertions.assertEquals(1000, result.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, result.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), result.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), result.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), result.getLimit().getProviderId());
        Response responseById = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/" + result.getLimit().getId())
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), responseById.getStatus());
        SingleBaseResourceLimitDto resultById = responseById.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertEquals(1000, resultById.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, resultById.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), resultById.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), resultById.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), resultById.getLimit().getProviderId());
        Response responseDeleted = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits/" + result.getLimit().getId() + 999L)
                .invoke(HttpMethod.DELETE, null);
        Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), responseDeleted.getStatus());
    }

    @Test
    public void testGetByKey() {
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong campaignId = new MutableLong();
        MutableLong serviceId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            serviceId.setValue(yp.getId());
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            campaignId.setValue(campaign.getId());
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
            baseResourceIdOne.setValue(baseResourceOne);
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            baseResourceIdTwo.setValue(baseResourceTwo);
        });
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceLimitDto result = response.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertTrue(result.getBaseResource().isPresent());
        Assertions.assertTrue(result.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertTrue(result.getCampaign().isPresent());
        Assertions.assertEquals(1000, result.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, result.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), result.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), result.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), result.getLimit().getProviderId());
        Response responseById = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/_findByKey")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new BaseResourceLimitKeyDto(baseResourceIdOne.longValue(),
                        campaignId.longValue()));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), responseById.getStatus());
        SingleBaseResourceLimitDto resultById = responseById.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertEquals(1000, resultById.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, resultById.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), resultById.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), resultById.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), resultById.getLimit().getProviderId());
    }

    @Test
    public void testGetByKeyNotFound() {
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong campaignId = new MutableLong();
        MutableLong serviceId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            serviceId.setValue(yp.getId());
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            campaignId.setValue(campaign.getId());
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
            baseResourceIdOne.setValue(baseResourceOne);
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            baseResourceIdTwo.setValue(baseResourceTwo);
        });
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceLimitDto result = response.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertTrue(result.getBaseResource().isPresent());
        Assertions.assertTrue(result.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertTrue(result.getCampaign().isPresent());
        Assertions.assertEquals(1000, result.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, result.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), result.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), result.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), result.getLimit().getProviderId());
        Response responseById = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/_findByKey")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new BaseResourceLimitKeyDto(baseResourceIdOne.longValue() + 999L,
                        campaignId.longValue()));
        Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), responseById.getStatus());
    }

    @Test
    public void testCreateNoPermissions() {
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong campaignId = new MutableLong();
        MutableLong serviceId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            serviceId.setValue(yp.getId());
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            campaignId.setValue(campaign.getId());
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
            baseResourceIdOne.setValue(baseResourceOne);
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            baseResourceIdTwo.setValue(baseResourceTwo);
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void testUpdateNoPermissions() {
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong campaignId = new MutableLong();
        MutableLong serviceId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            serviceId.setValue(yp.getId());
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            campaignId.setValue(campaign.getId());
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
            baseResourceIdOne.setValue(baseResourceOne);
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            baseResourceIdTwo.setValue(baseResourceTwo);
        });
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceLimitDto result = response.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertTrue(result.getBaseResource().isPresent());
        Assertions.assertTrue(result.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertTrue(result.getCampaign().isPresent());
        Assertions.assertEquals(1000, result.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, result.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), result.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), result.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), result.getLimit().getProviderId());
        Response responseById = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/" + result.getLimit().getId())
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), responseById.getStatus());
        SingleBaseResourceLimitDto resultById = responseById.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertEquals(1000, resultById.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, resultById.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), resultById.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), resultById.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), resultById.getLimit().getProviderId());
        Response updateResponse = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/" + result.getLimit().getId())
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.PATCH, new UpdateBaseResourceLimitDto(DiAmount.of(2, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), updateResponse.getStatus());
    }

    @Test
    public void testDeleteNoPermissions() {
        MutableLong baseResourceIdOne = new MutableLong();
        MutableLong baseResourceIdTwo = new MutableLong();
        MutableLong campaignId = new MutableLong();
        MutableLong serviceId = new MutableLong();
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            serviceId.setValue(yp.getId());
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
            campaignId.setValue(campaign.getId());
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
            baseResourceIdOne.setValue(baseResourceOne);
            long baseResourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo)).getId();
            baseResourceIdTwo.setValue(baseResourceTwo);
        });
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits")
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.POST, new CreateBaseResourceLimitDto(baseResourceIdOne.longValue(),
                        campaignId.longValue(), DiAmount.of(1, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceLimitDto result = response.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertTrue(result.getBaseResource().isPresent());
        Assertions.assertTrue(result.getBaseResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
        Assertions.assertTrue(result.getCampaign().isPresent());
        Assertions.assertEquals(1000, result.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, result.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), result.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), result.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), result.getLimit().getProviderId());
        Response responseById = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/" + result.getLimit().getId())
                .replaceQueryParam("expand", ExpandBaseResourceLimit.BASE_RESOURCES,
                        ExpandBaseResourceLimit.BASE_RESOURCE_TYPES, ExpandBaseResourceLimit.PROVIDERS,
                        ExpandBaseResourceLimit.CAMPAIGNS,
                        ExpandBaseResourceLimit.SEGMENTS)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), responseById.getStatus());
        SingleBaseResourceLimitDto resultById = responseById.readEntity(SingleBaseResourceLimitDto.class);
        Assertions.assertEquals(1000, resultById.getLimit().getLimit().getValue());
        Assertions.assertEquals(DiUnit.PERMILLE_CORES, resultById.getLimit().getLimit().getUnit());
        Assertions.assertEquals(baseResourceIdOne.longValue(), resultById.getLimit().getBaseResourceId());
        Assertions.assertEquals(campaignId.longValue(), resultById.getLimit().getCampaignId());
        Assertions.assertEquals(serviceId.longValue(), resultById.getLimit().getProviderId());
        Response responseDeleted = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/" + result.getLimit().getId())
                .invoke(HttpMethod.DELETE, null);
        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), responseDeleted.getStatus());
    }

    @Test
    public void testGetByServiceCampaign() {
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
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(campaign.getId())
                    .limit(1000));
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(campaign.getId())
                    .limit(2000));
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/human-readable/_report")
                .replaceQueryParam("campaignKey", "test-campaign")
                .replaceQueryParam("providerKey", YP)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceLimitsReportDto result = response.readEntity(BaseResourceLimitsReportDto.class);
        Assertions.assertEquals(2, result.getLimits().size());
        Assertions.assertEquals(Set.of(new BaseResourceLimitReportDto("test-1", DiAmount.of(1, DiUnit.CORES)),
                new BaseResourceLimitReportDto("test-2", DiAmount.of(2, DiUnit.CORES))),
                new HashSet<>(result.getLimits()));
    }

    @Test
    public void testGetByServiceCampaignDefault() {
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
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(campaign.getId())
                    .limit(1000));
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/human-readable/_report")
                .replaceQueryParam("campaignKey", "test-campaign")
                .replaceQueryParam("providerKey", YP)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceLimitsReportDto result = response.readEntity(BaseResourceLimitsReportDto.class);
        Assertions.assertEquals(2, result.getLimits().size());
        Assertions.assertEquals(Set.of(new BaseResourceLimitReportDto("test-1", DiAmount.of(1, DiUnit.CORES)),
                        new BaseResourceLimitReportDto("test-2", DiAmount.of(0, DiUnit.CORES))),
                new HashSet<>(result.getLimits()));
    }

    @Test
    public void testUpdateAllByServiceCampaign() {
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
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(campaign.getId())
                    .limit(1000));
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(campaign.getId())
                    .limit(2000));
        });
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits/human-readable/_report")
                .replaceQueryParam("campaignKey", "test-campaign")
                .replaceQueryParam("providerKey", YP)
                .invoke(HttpMethod.PUT, new BaseResourceLimitsReportInputDto(List.of(
                        new BaseResourceLimitReportInputDto("test-1", DiAmount.of(3, DiUnit.CORES)),
                        new BaseResourceLimitReportInputDto("test-2", DiAmount.of(4, DiUnit.CORES))
                )));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceLimitsReportDto result = response.readEntity(BaseResourceLimitsReportDto.class);
        Assertions.assertEquals(2, result.getLimits().size());
        Assertions.assertEquals(Set.of(new BaseResourceLimitReportDto("test-1", DiAmount.of(3, DiUnit.CORES)),
                        new BaseResourceLimitReportDto("test-2", DiAmount.of(4, DiUnit.CORES))),
                new HashSet<>(result.getLimits()));
    }

    @Test
    public void testUpdateAllByServiceCampaignDeleteOne() {
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
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(campaign.getId())
                    .limit(1000));
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(campaign.getId())
                    .limit(2000));
        });
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits/human-readable/_report")
                .replaceQueryParam("campaignKey", "test-campaign")
                .replaceQueryParam("providerKey", YP)
                .invoke(HttpMethod.PUT, new BaseResourceLimitsReportInputDto(List.of(
                        new BaseResourceLimitReportInputDto("test-1", DiAmount.of(3, DiUnit.CORES))
                )));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceLimitsReportDto result = response.readEntity(BaseResourceLimitsReportDto.class);
        Assertions.assertEquals(2, result.getLimits().size());
        Assertions.assertEquals(Set.of(new BaseResourceLimitReportDto("test-1", DiAmount.of(3, DiUnit.CORES)),
                        new BaseResourceLimitReportDto("test-2", DiAmount.of(0, DiUnit.CORES))),
                new HashSet<>(result.getLimits()));
    }

    @Test
    public void testUpdateAllByServiceCampaignAddOne() {
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
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(campaign.getId())
                    .limit(1000));
        });
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits/human-readable/_report")
                .replaceQueryParam("campaignKey", "test-campaign")
                .replaceQueryParam("providerKey", YP)
                .invoke(HttpMethod.PUT, new BaseResourceLimitsReportInputDto(List.of(
                        new BaseResourceLimitReportInputDto("test-1", DiAmount.of(3, DiUnit.CORES)),
                        new BaseResourceLimitReportInputDto("test-2", DiAmount.of(4, DiUnit.CORES))
                )));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceLimitsReportDto result = response.readEntity(BaseResourceLimitsReportDto.class);
        Assertions.assertEquals(2, result.getLimits().size());
        Assertions.assertEquals(Set.of(new BaseResourceLimitReportDto("test-1", DiAmount.of(3, DiUnit.CORES)),
                        new BaseResourceLimitReportDto("test-2", DiAmount.of(4, DiUnit.CORES))),
                new HashSet<>(result.getLimits()));
    }

    @Test
    public void testGetByResourceCampaign() {
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
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(campaign.getId())
                    .limit(1000));
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(campaign.getId())
                    .limit(2000));
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/human-readable/_reportOne")
                .replaceQueryParam("campaignKey", "test-campaign")
                .replaceQueryParam("baseResourceKey", "test-1")
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceLimitAmountDto result = response.readEntity(BaseResourceLimitAmountDto.class);
        Assertions.assertEquals(DiAmount.of(1, DiUnit.CORES), result.getLimit());
    }

    @Test
    public void testGetByResourceCampaignDefault() {
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
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(campaign.getId())
                    .limit(1000));
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-limits/human-readable/_reportOne")
                .replaceQueryParam("campaignKey", "test-campaign")
                .replaceQueryParam("baseResourceKey", "test-2")
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceLimitAmountDto result = response.readEntity(BaseResourceLimitAmountDto.class);
        Assertions.assertEquals(DiAmount.of(0, DiUnit.CORES), result.getLimit());
    }

    @Test
    public void testPutByResourceCampaign() {
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
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(campaign.getId())
                    .limit(1000));
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceTwo)
                    .campaignId(campaign.getId())
                    .limit(2000));
        });
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits/human-readable/_reportOne")
                .replaceQueryParam("campaignKey", "test-campaign")
                .replaceQueryParam("baseResourceKey", "test-1")
                .invoke(HttpMethod.PUT, new BaseResourceLimitAmountInputDto(DiAmount.of(3, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceLimitAmountDto result = response.readEntity(BaseResourceLimitAmountDto.class);
        Assertions.assertEquals(DiAmount.of(3, DiUnit.CORES), result.getLimit());
    }

    @Test
    public void testPutByResourceCampaignCreate() {
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
            baseResourceLimitDao.create(BaseResourceLimit.builder()
                    .baseResourceId(baseResourceOne)
                    .campaignId(campaign.getId())
                    .limit(1000));
        });
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/base-resource-limits/human-readable/_reportOne")
                .replaceQueryParam("campaignKey", "test-campaign")
                .replaceQueryParam("baseResourceKey", "test-2")
                .invoke(HttpMethod.PUT, new BaseResourceLimitAmountInputDto(DiAmount.of(3, DiUnit.CORES)));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceLimitAmountDto result = response.readEntity(BaseResourceLimitAmountDto.class);
        Assertions.assertEquals(DiAmount.of(3, DiUnit.CORES), result.getLimit());
    }

}
