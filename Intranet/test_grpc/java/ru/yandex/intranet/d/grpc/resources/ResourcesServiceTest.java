package ru.yandex.intranet.d.grpc.resources;

import java.util.Optional;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.GetResourceRequest;
import ru.yandex.intranet.d.backend.service.proto.IncludeResourcesSegmentation;
import ru.yandex.intranet.d.backend.service.proto.ListResourcesByProviderRequest;
import ru.yandex.intranet.d.backend.service.proto.ListResourcesByProviderResponse;
import ru.yandex.intranet.d.backend.service.proto.Resource;
import ru.yandex.intranet.d.backend.service.proto.ResourcesLimit;
import ru.yandex.intranet.d.backend.service.proto.ResourcesPageToken;
import ru.yandex.intranet.d.backend.service.proto.ResourcesServiceGrpc;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.utils.ErrorsHelper;

/**
 * Resources GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ResourcesServiceTest {

    @GrpcClient("inProcess")
    private ResourcesServiceGrpc.ResourcesServiceBlockingStub resourcesService;

    @Test
    public void getResourceTest() {
        GetResourceRequest resourceRequest = GetResourceRequest.newBuilder()
                .setResourceId("ef333da9-b076-42f5-b7f5-84cd04ab7fcc")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        Resource resourceType = resourcesService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getResource(resourceRequest);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals("ef333da9-b076-42f5-b7f5-84cd04ab7fcc", resourceType.getResourceId());
    }

    @Test
    public void getResourceWithSegmentationsTest() {
        GetResourceRequest resourceRequest = GetResourceRequest.newBuilder()
                .setResourceId("ef333da9-b076-42f5-b7f5-84cd04ab7fcc")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setWithSegmentations(IncludeResourcesSegmentation.newBuilder().setWithSegmentations(true).build())
                .build();
        Resource resourceType = resourcesService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getResource(resourceRequest);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals("ef333da9-b076-42f5-b7f5-84cd04ab7fcc", resourceType.getResourceId());
    }

    @Test
    public void getResourceNotFoundTest() {
        GetResourceRequest resourceRequest = GetResourceRequest.newBuilder()
                .setResourceId("12345678-9012-3456-7890-123456789012")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getResource(resourceRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void getResourcesPageTest() {
        ListResourcesByProviderRequest resourcesRequest = ListResourcesByProviderRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        ListResourcesByProviderResponse page = resourcesService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourcesByProvider(resourcesRequest);
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getResourcesCount() > 0);
    }

    @Test
    public void getResourcesPageWithSegmentationsTest() {
        ListResourcesByProviderRequest resourcesRequest = ListResourcesByProviderRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setWithSegmentations(IncludeResourcesSegmentation.newBuilder().setWithSegmentations(true).build())
                .build();
        ListResourcesByProviderResponse page = resourcesService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourcesByProvider(resourcesRequest);
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getResourcesCount() > 0);
    }

    @Test
    public void getResourcesTwoPagesTest() {
        ListResourcesByProviderRequest firstRequest = ListResourcesByProviderRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setLimit(ResourcesLimit.newBuilder().setLimit(1L).build())
                .build();
        ListResourcesByProviderResponse firstPage = resourcesService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourcesByProvider(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getResourcesCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());
        ListResourcesByProviderRequest secondRequest = ListResourcesByProviderRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setPageToken(ResourcesPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        ListResourcesByProviderResponse secondPage = resourcesService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourcesByProvider(secondRequest);
        Assertions.assertNotNull(secondPage);
        Assertions.assertTrue(secondPage.getResourcesCount() > 0);
    }

}
