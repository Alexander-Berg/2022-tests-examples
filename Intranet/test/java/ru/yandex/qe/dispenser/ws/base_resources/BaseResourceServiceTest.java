package ru.yandex.qe.dispenser.ws.base_resources;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResource;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceType;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceDao;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceTypeDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.ws.base_resources.model.BaseResourcesPageDto;
import ru.yandex.qe.dispenser.ws.base_resources.model.ExpandBaseResource;
import ru.yandex.qe.dispenser.ws.base_resources.model.SingleBaseResourceDto;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;
import ru.yandex.qe.dispenser.ws.logic.BusinessLogicTestBase;

public class BaseResourceServiceTest extends BusinessLogicTestBase {

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

    @Test
    public void testGetFirstPage() {
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
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
            baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId()));
            baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo));
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resources")
                .replaceQueryParam("expand", ExpandBaseResource.PROVIDERS, ExpandBaseResource.SEGMENTS,
                        ExpandBaseResource.BASE_RESOURCE_TYPES)
                .replaceQueryParam("limit", 50)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourcesPageDto result = response.readEntity(BaseResourcesPageDto.class);
        Assertions.assertEquals(2, result.getResources().size());
        Assertions.assertEquals(2, result.getResourceTypes().size());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertEquals(1, result.getProviders().size());
    }

    @Test
    public void testGetNextPage() {
        Long nextFrom = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
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
            BaseResource resourceOne = baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId()));
            baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo));
            return resourceOne.getId();
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resources")
                .replaceQueryParam("expand", ExpandBaseResource.PROVIDERS, ExpandBaseResource.SEGMENTS,
                        ExpandBaseResource.BASE_RESOURCE_TYPES)
                .replaceQueryParam("limit", 50)
                .replaceQueryParam("from", nextFrom)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourcesPageDto result = response.readEntity(BaseResourcesPageDto.class);
        Assertions.assertEquals(1, result.getResources().size());
        Assertions.assertEquals(1, result.getResourceTypes().size());
        Assertions.assertNull(result.getSegments());
        Assertions.assertEquals(1, result.getProviders().size());
    }

    @Test
    public void testGetLastPage() {
        Long nextFrom = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
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
            baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId()));
            BaseResource resourceTwo = baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo));
            return resourceTwo.getId();
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resources")
                .replaceQueryParam("expand", ExpandBaseResource.PROVIDERS, ExpandBaseResource.SEGMENTS,
                        ExpandBaseResource.BASE_RESOURCE_TYPES)
                .replaceQueryParam("limit", 50)
                .replaceQueryParam("from", nextFrom)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourcesPageDto result = response.readEntity(BaseResourcesPageDto.class);
        Assertions.assertEquals(0, result.getResources().size());
        Assertions.assertNull(result.getResourceTypes());
        Assertions.assertNull(result.getSegments());
        Assertions.assertNull(result.getProviders());
    }

    @Test
    public void testGetFirstPageNoExpand() {
        TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
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
            baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId()));
            baseResourceDao.create(BaseResource.builder()
                    .key("test-2")
                    .name("Test-2")
                    .baseResourceTypeId(typeTwo));
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resources")
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourcesPageDto result = response.readEntity(BaseResourcesPageDto.class);
        Assertions.assertEquals(2, result.getResources().size());
        Assertions.assertNull(result.getResourceTypes());
        Assertions.assertNull(result.getSegments());
        Assertions.assertNull(result.getProviders());
    }

    @Test
    public void testById() {
        Long id = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            return baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resources/" + id)
                .replaceQueryParam("expand", ExpandBaseResource.PROVIDERS, ExpandBaseResource.SEGMENTS,
                        ExpandBaseResource.BASE_RESOURCE_TYPES)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceDto result = response.readEntity(SingleBaseResourceDto.class);
        Assertions.assertEquals(id, Long.valueOf(result.getResource().getId()));
        Assertions.assertTrue(result.getResourceType().isPresent());
        Assertions.assertEquals(2, result.getSegments().size());
        Assertions.assertTrue(result.getProvider().isPresent());
    }

    @Test
    public void testByIdNoExpand() {
        Long id = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            return baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resources/" + id)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceDto result = response.readEntity(SingleBaseResourceDto.class);
        Assertions.assertEquals(id, Long.valueOf(result.getResource().getId()));
        Assertions.assertFalse(result.getResourceType().isPresent());
        Assertions.assertNull(result.getSegments());
        Assertions.assertFalse(result.getProvider().isPresent());
    }

    @Test
    public void testByIdNotFound() {
        Long id = TransactionWrapper.INSTANCE.execute(() -> {
            Service yp = serviceDao.read(YP);
            Segmentation segmentationOne = segmentationDao.read(new Segmentation.Key(DC_SEGMENTATION));
            Segmentation segmentationTwo = segmentationDao.read(new Segmentation.Key(SEGMENT_SEGMENTATION));
            Segment segmentOne = segmentDao.read(new Segment.Key(DC_SEGMENT_1, segmentationOne));
            Segment segmentTwo = segmentDao.read(new Segment.Key(SEGMENT_SEGMENT_1, segmentationTwo));
            long typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(yp.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
            return baseResourceDao.create(BaseResource.builder()
                    .key("test-1")
                    .name("Test-1")
                    .baseResourceTypeId(typeOne)
                    .addSegmentIds(segmentOne.getId(), segmentTwo.getId())).getId();
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resources/" + (id + 999L))
                .replaceQueryParam("expand", ExpandBaseResource.PROVIDERS, ExpandBaseResource.SEGMENTS,
                        ExpandBaseResource.BASE_RESOURCE_TYPES)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

}
