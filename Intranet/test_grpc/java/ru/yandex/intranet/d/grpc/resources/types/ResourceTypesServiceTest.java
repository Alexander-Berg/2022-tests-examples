package ru.yandex.intranet.d.grpc.resources.types;

import java.util.Optional;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.proto.AggregationSettings;
import ru.yandex.intranet.d.backend.service.proto.CreateResourceTypeRequest;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.FreeProvisionAggregationMode;
import ru.yandex.intranet.d.backend.service.proto.FullResourceType;
import ru.yandex.intranet.d.backend.service.proto.GetResourceTypeRequest;
import ru.yandex.intranet.d.backend.service.proto.ListResourceTypesByProviderRequest;
import ru.yandex.intranet.d.backend.service.proto.ListResourceTypesByProviderResponse;
import ru.yandex.intranet.d.backend.service.proto.NewResourceType;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementLimit;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementPageToken;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.UpdateResourceTypeRequest;
import ru.yandex.intranet.d.backend.service.proto.UpdatedResourceType;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.utils.ErrorsHelper;

/**
 * Resource types GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ResourceTypesServiceTest {

    @GrpcClient("inProcess")
    private ResourcesManagementServiceGrpc.ResourcesManagementServiceBlockingStub resourcesManagementService;

    @Test
    public void getResourceTypeTest() {
        GetResourceTypeRequest resourceTypeRequest = GetResourceTypeRequest.newBuilder()
                .setResourceTypeId("86407d21-0bd9-48ca-9a81-5e40fb3d8477")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceType resourceType = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getResourceType(resourceTypeRequest);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals("86407d21-0bd9-48ca-9a81-5e40fb3d8477", resourceType.getResourceTypeId());
    }

    @Test
    public void getResourceTypeNotFoundTest() {
        GetResourceTypeRequest resourceTypeRequest = GetResourceTypeRequest.newBuilder()
                .setResourceTypeId("12345678-9012-3456-7890-123456789012")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getResourceType(resourceTypeRequest);
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
    public void getResourceTypesPageTest() {
        ListResourceTypesByProviderRequest resourceTypesRequest = ListResourceTypesByProviderRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        ListResourceTypesByProviderResponse page = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourceTypesByProvider(resourceTypesRequest);
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getResourceTypesCount() > 0);
    }

    @Test
    public void getResourceTypesTwoPagesTest() {
        ListResourceTypesByProviderRequest firstRequest = ListResourceTypesByProviderRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setLimit(ResourcesManagementLimit.newBuilder().setLimit(1L).build())
                .build();
        ListResourceTypesByProviderResponse firstPage = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourceTypesByProvider(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getResourceTypesCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());
        ListResourceTypesByProviderRequest secondRequest = ListResourceTypesByProviderRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setPageToken(ResourcesManagementPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        ListResourceTypesByProviderResponse secondPage = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourceTypesByProvider(secondRequest);
        Assertions.assertNotNull(secondPage);
        Assertions.assertTrue(secondPage.getResourceTypesCount() > 0);
    }

    @Test
    public void createResourceTypeTest() {
        CreateResourceTypeRequest resourceTypeRequest = CreateResourceTypeRequest.newBuilder()
                .setResourceType(NewResourceType.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setAggregationSettings(AggregationSettings.newBuilder()
                                .setFreeProvisionMode(FreeProvisionAggregationMode.NONE)
                                .build())
                        .build())
                .build();
        FullResourceType resourceType = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceType(resourceTypeRequest);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceType.getProviderId());
    }

    @Test
    public void createResourceTypeEmptyNameTest() {
        CreateResourceTypeRequest resourceTypeRequest = CreateResourceTypeRequest.newBuilder()
                .setResourceType(NewResourceType.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .build())
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .createResourceType(resourceTypeRequest);
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
    public void createResourceTypeKeyConflictTest() {
        CreateResourceTypeRequest resourceTypeRequest = CreateResourceTypeRequest.newBuilder()
                .setResourceType(NewResourceType.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("cpu")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .build())
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .createResourceType(resourceTypeRequest);
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
    public void updateResourceTypeTest() {
        CreateResourceTypeRequest resourceTypeRequest = CreateResourceTypeRequest.newBuilder()
                .setResourceType(NewResourceType.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .build())
                .build();
        FullResourceType resourceType = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceType(resourceTypeRequest);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceType.getProviderId());
        UpdateResourceTypeRequest updateResourceTypeRequest = UpdateResourceTypeRequest.newBuilder()
                .setResourceType(UpdatedResourceType.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .setAggregationSettings(AggregationSettings.newBuilder()
                                .setFreeProvisionMode(FreeProvisionAggregationMode.NONE)
                                .build())
                        .build())
                .setResourceTypeId(resourceType.getResourceTypeId())
                .setVersion(resourceType.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceType updatedResourceType = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .updateResourceType(updateResourceTypeRequest);
        Assertions.assertNotNull(updatedResourceType);
        Assertions.assertEquals("Test-1", updatedResourceType.getNameEn());
    }

    @Test
    public void updateResourceTypeOnlyAcceptableByProviderAdmin() {
        CreateResourceTypeRequest resourceTypeRequest = CreateResourceTypeRequest.newBuilder()
                .setResourceType(NewResourceType.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .build())
                .build();
        FullResourceType resourceType = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceType(resourceTypeRequest);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceType.getProviderId());
        UpdateResourceTypeRequest updateResourceTypeRequest = UpdateResourceTypeRequest.newBuilder()
                .setResourceType(UpdatedResourceType.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceTypeId(resourceType.getResourceTypeId())
                .setVersion(resourceType.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YDB_ADMIN_UID))
                .updateResourceType(updateResourceTypeRequest));
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .updateResourceType(updateResourceTypeRequest));
        FullResourceType updatedResourceType = resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YP_ADMIN_UID))
                .updateResourceType(updateResourceTypeRequest);
        Assertions.assertNotNull(updatedResourceType);
        Assertions.assertEquals("Test-1", updatedResourceType.getNameEn());
    }

    @Test
    public void updateResourceTypeNotFoundTest() {
        CreateResourceTypeRequest resourceTypeRequest = CreateResourceTypeRequest.newBuilder()
                .setResourceType(NewResourceType.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .build())
                .build();
        FullResourceType resourceType = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceType(resourceTypeRequest);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceType.getProviderId());
        UpdateResourceTypeRequest updateResourceTypeRequest = UpdateResourceTypeRequest.newBuilder()
                .setResourceType(UpdatedResourceType.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceTypeId("12345678-9012-3456-7890-123456789012")
                .setVersion(resourceType.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResourceType(updateResourceTypeRequest);
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
    public void updateResourceTypeVersionMismatchTest() {
        CreateResourceTypeRequest resourceTypeRequest = CreateResourceTypeRequest.newBuilder()
                .setResourceType(NewResourceType.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .build())
                .build();
        FullResourceType resourceType = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceType(resourceTypeRequest);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceType.getProviderId());
        UpdateResourceTypeRequest updateResourceTypeRequest = UpdateResourceTypeRequest.newBuilder()
                .setResourceType(UpdatedResourceType.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceTypeId(resourceType.getResourceTypeId())
                .setVersion(resourceType.getVersion() + 1L)
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResourceType(updateResourceTypeRequest);
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
    public void updateResourceTypeEmptyNameTest() {
        CreateResourceTypeRequest resourceTypeRequest = CreateResourceTypeRequest.newBuilder()
                .setResourceType(NewResourceType.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .build())
                .build();
        FullResourceType resourceType = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceType(resourceTypeRequest);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceType.getProviderId());
        UpdateResourceTypeRequest updateResourceTypeRequest = UpdateResourceTypeRequest.newBuilder()
                .setResourceType(UpdatedResourceType.newBuilder()
                        .setNameEn("")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceTypeId(resourceType.getResourceTypeId())
                .setVersion(resourceType.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResourceType(updateResourceTypeRequest);
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
    public void createResourceTypeOnlyAcceptableByProviderAdmin() {
        CreateResourceTypeRequest resourceTypeRequest = CreateResourceTypeRequest.newBuilder()
                .setResourceType(NewResourceType.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .build())
                .build();
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YDB_ADMIN_UID))
                .createResourceType(resourceTypeRequest));
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .createResourceType(resourceTypeRequest));
        FullResourceType resourceType = resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YP_ADMIN_UID))
                .createResourceType(resourceTypeRequest);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceType.getProviderId());
    }

}
