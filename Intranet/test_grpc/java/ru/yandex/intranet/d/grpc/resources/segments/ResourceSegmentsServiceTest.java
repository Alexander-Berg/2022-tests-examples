package ru.yandex.intranet.d.grpc.resources.segments;

import java.util.Optional;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.proto.CreateResourceSegmentRequest;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.FullResourceSegment;
import ru.yandex.intranet.d.backend.service.proto.GetResourceSegmentRequest;
import ru.yandex.intranet.d.backend.service.proto.ListResourceSegmentsBySegmentationRequest;
import ru.yandex.intranet.d.backend.service.proto.ListResourceSegmentsBySegmentationResponse;
import ru.yandex.intranet.d.backend.service.proto.NewResourceSegment;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementLimit;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementPageToken;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.UpdateResourceSegmentRequest;
import ru.yandex.intranet.d.backend.service.proto.UpdatedResourceSegment;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.utils.ErrorsHelper;

/**
 * Resource segments GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ResourceSegmentsServiceTest {

    @GrpcClient("inProcess")
    private ResourcesManagementServiceGrpc.ResourcesManagementServiceBlockingStub resourcesManagementService;

    @Test
    public void getResourceSegmentTest() {
        GetResourceSegmentRequest resourceSegmentRequest = GetResourceSegmentRequest.newBuilder()
                .setResourceSegmentId("540b6c0f-d0b9-4f62-9b75-af7c9cfd2e95")
                .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceSegment resourceSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getResourceSegment(resourceSegmentRequest);
        Assertions.assertNotNull(resourceSegment);
        Assertions.assertEquals("540b6c0f-d0b9-4f62-9b75-af7c9cfd2e95", resourceSegment.getResourceSegmentId());
    }

    @Test
    public void getResourceSegmentNotFoundTest() {
        GetResourceSegmentRequest resourceSegmentationRequest = GetResourceSegmentRequest.newBuilder()
                .setResourceSegmentId("12345678-9012-3456-7890-123456789012")
                .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getResourceSegment(resourceSegmentationRequest);
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
    public void getResourceSegmentsPageTest() {
        ListResourceSegmentsBySegmentationRequest resourceSegmentsRequest
                = ListResourceSegmentsBySegmentationRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                .build();
        ListResourceSegmentsBySegmentationResponse page = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourceSegmentsBySegmentation(resourceSegmentsRequest);
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getResourceSegmentsCount() > 0);
    }

    @Test
    public void getResourceSegmentsTwoPagesTest() {
        ListResourceSegmentsBySegmentationRequest firstRequest = ListResourceSegmentsBySegmentationRequest
                .newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                .setLimit(ResourcesManagementLimit.newBuilder().setLimit(1L).build())
                .build();
        ListResourceSegmentsBySegmentationResponse firstPage = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourceSegmentsBySegmentation(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getResourceSegmentsCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());
        ListResourceSegmentsBySegmentationRequest secondRequest = ListResourceSegmentsBySegmentationRequest
                .newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                .setPageToken(ResourcesManagementPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        ListResourceSegmentsBySegmentationResponse secondPage = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourceSegmentsBySegmentation(secondRequest);
        Assertions.assertNotNull(secondPage);
        Assertions.assertTrue(secondPage.getResourceSegmentsCount() > 0);
    }

    @Test
    public void createResourceSegmentTest() {
        CreateResourceSegmentRequest resourceSegmentRequest = CreateResourceSegmentRequest.newBuilder()
                .setResourceSegment(NewResourceSegment.newBuilder()
                        .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceSegment resourceSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegment(resourceSegmentRequest);
        Assertions.assertNotNull(resourceSegment);
        Assertions.assertEquals("7fbd778f-d803-44c8-831a-c1de5c05885c", resourceSegment.getResourceSegmentationId());
    }

    @Test
    public void createResourceSegmentOnlyAcceptableByProviderAdmin() {
        CreateResourceSegmentRequest resourceSegmentRequest = CreateResourceSegmentRequest.newBuilder()
                .setResourceSegment(NewResourceSegment.newBuilder()
                        .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YDB_ADMIN_UID))
                .createResourceSegment(resourceSegmentRequest));
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .createResourceSegment(resourceSegmentRequest));
        FullResourceSegment resourceSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YP_ADMIN_UID))
                .createResourceSegment(resourceSegmentRequest);
        Assertions.assertNotNull(resourceSegment);
        Assertions.assertEquals("7fbd778f-d803-44c8-831a-c1de5c05885c", resourceSegment.getResourceSegmentationId());
    }

    @Test
    public void createResourceSegmentEmptyNameTest() {
        CreateResourceSegmentRequest resourceSegmentRequest = CreateResourceSegmentRequest.newBuilder()
                .setResourceSegment(NewResourceSegment.newBuilder()
                        .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                        .setKey("test")
                        .setNameEn("")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .createResourceSegment(resourceSegmentRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getFieldErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void createResourceSegmentKeyConflictTest() {
        CreateResourceSegmentRequest resourceSegmentRequest = CreateResourceSegmentRequest.newBuilder()
                .setResourceSegment(NewResourceSegment.newBuilder()
                        .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                        .setKey("sas")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .createResourceSegment(resourceSegmentRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.ALREADY_EXISTS, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getFieldErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void updateResourceSegmentTest() {
        CreateResourceSegmentRequest resourceSegmentRequest = CreateResourceSegmentRequest.newBuilder()
                .setResourceSegment(NewResourceSegment.newBuilder()
                        .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceSegment resourceSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegment(resourceSegmentRequest);
        Assertions.assertNotNull(resourceSegment);
        Assertions.assertEquals("7fbd778f-d803-44c8-831a-c1de5c05885c", resourceSegment.getResourceSegmentationId());
        UpdateResourceSegmentRequest updateResourceSegmentRequest = UpdateResourceSegmentRequest
                .newBuilder()
                .setResourceSegment(UpdatedResourceSegment.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceSegmentId(resourceSegment.getResourceSegmentId())
                .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                .setVersion(resourceSegment.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceSegment updatedResourceSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .updateResourceSegment(updateResourceSegmentRequest);
        Assertions.assertNotNull(updatedResourceSegment);
        Assertions.assertEquals("Test-1", updatedResourceSegment.getNameEn());
    }

    @Test
    public void updateResourceSegmentOnlyAcceptableByProviderAdmin() {
        CreateResourceSegmentRequest resourceSegmentRequest = CreateResourceSegmentRequest.newBuilder()
                .setResourceSegment(NewResourceSegment.newBuilder()
                        .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceSegment resourceSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegment(resourceSegmentRequest);
        Assertions.assertNotNull(resourceSegment);
        Assertions.assertEquals("7fbd778f-d803-44c8-831a-c1de5c05885c", resourceSegment.getResourceSegmentationId());
        UpdateResourceSegmentRequest updateResourceSegmentRequest = UpdateResourceSegmentRequest
                .newBuilder()
                .setResourceSegment(UpdatedResourceSegment.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceSegmentId(resourceSegment.getResourceSegmentId())
                .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                .setVersion(resourceSegment.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YDB_ADMIN_UID))
                .updateResourceSegment(updateResourceSegmentRequest));
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .updateResourceSegment(updateResourceSegmentRequest));
        FullResourceSegment updatedResourceSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YP_ADMIN_UID))
                .updateResourceSegment(updateResourceSegmentRequest);
        Assertions.assertNotNull(updatedResourceSegment);
        Assertions.assertEquals("Test-1", updatedResourceSegment.getNameEn());
    }

    @Test
    public void updateResourceSegmentNotFoundTest() {
        CreateResourceSegmentRequest resourceSegmentRequest = CreateResourceSegmentRequest.newBuilder()
                .setResourceSegment(NewResourceSegment.newBuilder()
                        .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceSegment resourceSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegment(resourceSegmentRequest);
        Assertions.assertNotNull(resourceSegment);
        Assertions.assertEquals("7fbd778f-d803-44c8-831a-c1de5c05885c", resourceSegment.getResourceSegmentationId());
        UpdateResourceSegmentRequest updateResourceSegmentRequest = UpdateResourceSegmentRequest
                .newBuilder()
                .setResourceSegment(UpdatedResourceSegment.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceSegmentId("12345678-9012-3456-7890-123456789012")
                .setVersion(resourceSegment.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResourceSegment(updateResourceSegmentRequest);
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
    public void updateResourceSegmentVersionMismatchTest() {
        CreateResourceSegmentRequest resourceSegmentRequest = CreateResourceSegmentRequest.newBuilder()
                .setResourceSegment(NewResourceSegment.newBuilder()
                        .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceSegment resourceSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegment(resourceSegmentRequest);
        Assertions.assertNotNull(resourceSegment);
        Assertions.assertEquals("7fbd778f-d803-44c8-831a-c1de5c05885c", resourceSegment.getResourceSegmentationId());
        UpdateResourceSegmentRequest updateResourceSegmentRequest = UpdateResourceSegmentRequest
                .newBuilder()
                .setResourceSegment(UpdatedResourceSegment.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceSegmentId(resourceSegment.getResourceSegmentId())
                .setVersion(resourceSegment.getVersion() + 1L)
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResourceSegment(updateResourceSegmentRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.FAILED_PRECONDITION, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getFieldErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void updateResourceSegmentEmptyNameTest() {
        CreateResourceSegmentRequest resourceSegmentRequest = CreateResourceSegmentRequest.newBuilder()
                .setResourceSegment(NewResourceSegment.newBuilder()
                        .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceSegment resourceSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegment(resourceSegmentRequest);
        Assertions.assertNotNull(resourceSegment);
        Assertions.assertEquals("7fbd778f-d803-44c8-831a-c1de5c05885c", resourceSegment.getResourceSegmentationId());
        UpdateResourceSegmentRequest updateResourceSegmentRequest = UpdateResourceSegmentRequest
                .newBuilder()
                .setResourceSegment(UpdatedResourceSegment.newBuilder()
                        .setNameEn("")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceSegmentId(resourceSegment.getResourceSegmentId())
                .setVersion(resourceSegment.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResourceSegment(updateResourceSegmentRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getFieldErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

}
