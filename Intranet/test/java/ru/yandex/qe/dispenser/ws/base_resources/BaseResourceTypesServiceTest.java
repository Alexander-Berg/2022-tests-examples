package ru.yandex.qe.dispenser.ws.base_resources;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.base_resources.BaseResourceType;
import ru.yandex.qe.dispenser.domain.dao.base_resources.BaseResourceTypeDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.ws.base_resources.model.BaseResourceTypesPageDto;
import ru.yandex.qe.dispenser.ws.base_resources.model.ExpandBaseResourceType;
import ru.yandex.qe.dispenser.ws.base_resources.model.SingleBaseResourceTypeDto;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;
import ru.yandex.qe.dispenser.ws.logic.BusinessLogicTestBase;

public class BaseResourceTypesServiceTest extends BusinessLogicTestBase {

    @Autowired
    private BaseResourceTypeDao baseResourceTypeDao;
    @Autowired
    private ServiceDao serviceDao;

    @Test
    public void testGetFirstPage() {
        TransactionWrapper.INSTANCE.execute(() -> {
            Service nirvana = serviceDao.read(NIRVANA);
            baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(nirvana.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            );
            baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-2")
                    .name("Test-2")
                    .serviceId(nirvana.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            );
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-types")
                .replaceQueryParam("expand", ExpandBaseResourceType.PROVIDERS)
                .replaceQueryParam("limit", 50)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceTypesPageDto result = response.readEntity(BaseResourceTypesPageDto.class);
        Assertions.assertEquals(2, result.getTypes().size());
        Assertions.assertEquals(1, result.getProviders().size());
    }

    @Test
    public void testGetNextPage() {
        Long nextFrom = TransactionWrapper.INSTANCE.execute(() -> {
            Service nirvana = serviceDao.read(NIRVANA);
            BaseResourceType typeOne = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(nirvana.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            );
            baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-2")
                    .name("Test-2")
                    .serviceId(nirvana.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            );
            return typeOne.getId();
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-types")
                .replaceQueryParam("expand", ExpandBaseResourceType.PROVIDERS)
                .replaceQueryParam("limit", 50)
                .replaceQueryParam("from", nextFrom)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceTypesPageDto result = response.readEntity(BaseResourceTypesPageDto.class);
        Assertions.assertEquals(1, result.getTypes().size());
        Assertions.assertEquals(1, result.getProviders().size());
    }

    @Test
    public void testGetLastPage() {
        Long nextFrom = TransactionWrapper.INSTANCE.execute(() -> {
            Service nirvana = serviceDao.read(NIRVANA);
            baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(nirvana.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            );
            BaseResourceType typeTwo = baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-2")
                    .name("Test-2")
                    .serviceId(nirvana.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            );
            return typeTwo.getId();
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-types")
                .replaceQueryParam("expand", ExpandBaseResourceType.PROVIDERS)
                .replaceQueryParam("limit", 50)
                .replaceQueryParam("from", nextFrom)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceTypesPageDto result = response.readEntity(BaseResourceTypesPageDto.class);
        Assertions.assertEquals(0, result.getTypes().size());
        Assertions.assertNull(result.getProviders());
    }

    @Test
    public void testGetFirstPageNoExpand() {
        TransactionWrapper.INSTANCE.execute(() -> {
            Service nirvana = serviceDao.read(NIRVANA);
            baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(nirvana.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            );
            baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-2")
                    .name("Test-2")
                    .serviceId(nirvana.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            );
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-types")
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        BaseResourceTypesPageDto result = response.readEntity(BaseResourceTypesPageDto.class);
        Assertions.assertEquals(2, result.getTypes().size());
        Assertions.assertNull(result.getProviders());
    }

    @Test
    public void testById() {
        Long id = TransactionWrapper.INSTANCE.execute(() -> {
            Service nirvana = serviceDao.read(NIRVANA);
            return baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(nirvana.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
        });
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-types/" + id)
                .replaceQueryParam("expand", ExpandBaseResourceType.PROVIDERS)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceTypeDto result = response.readEntity(SingleBaseResourceTypeDto.class);
        Assertions.assertEquals(id, Long.valueOf(result.getType().getId()));
        Assertions.assertTrue(result.getProvider().isPresent());
    }

    @Test
    public void testByIdNoExpand() {
        Long id = TransactionWrapper.INSTANCE.execute(() -> {
            Service nirvana = serviceDao.read(NIRVANA);
            return baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(nirvana.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-types/" + id)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        SingleBaseResourceTypeDto result = response.readEntity(SingleBaseResourceTypeDto.class);
        Assertions.assertEquals(id, Long.valueOf(result.getType().getId()));
        Assertions.assertFalse(result.getProvider().isPresent());
    }

    @Test
    public void testByIdNotFound() {
        Long id = TransactionWrapper.INSTANCE.execute(() -> {
            Service nirvana = serviceDao.read(NIRVANA);
            return baseResourceTypeDao.create(BaseResourceType.builder()
                    .key("test-1")
                    .name("Test-1")
                    .serviceId(nirvana.getId())
                    .resourceType(DiResourceType.PROCESSOR)
            ).getId();
        });
        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/base-resource-types/" + (id + 999L))
                .replaceQueryParam("expand", ExpandBaseResourceType.PROVIDERS)
                .invoke(HttpMethod.GET, null);
        Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

}
