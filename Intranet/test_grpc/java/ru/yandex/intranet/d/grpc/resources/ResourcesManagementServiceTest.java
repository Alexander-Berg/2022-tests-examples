package ru.yandex.intranet.d.grpc.resources;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.protobuf.util.FieldMaskUtil;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.proto.AggregationSettings;
import ru.yandex.intranet.d.backend.service.proto.CreateResourceRequest;
import ru.yandex.intranet.d.backend.service.proto.CreateResourceTypeRequest;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.FeatureState;
import ru.yandex.intranet.d.backend.service.proto.FieldError;
import ru.yandex.intranet.d.backend.service.proto.FreeProvisionAggregationMode;
import ru.yandex.intranet.d.backend.service.proto.FullResource;
import ru.yandex.intranet.d.backend.service.proto.FullResourceType;
import ru.yandex.intranet.d.backend.service.proto.GetFullResourceRequest;
import ru.yandex.intranet.d.backend.service.proto.ListFullResourcesByProviderRequest;
import ru.yandex.intranet.d.backend.service.proto.ListFullResourcesByProviderResponse;
import ru.yandex.intranet.d.backend.service.proto.NewResource;
import ru.yandex.intranet.d.backend.service.proto.NewResourceType;
import ru.yandex.intranet.d.backend.service.proto.PatchResourceTypeRequest;
import ru.yandex.intranet.d.backend.service.proto.ResourceSegmentIds;
import ru.yandex.intranet.d.backend.service.proto.ResourceSegmentationsIds;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementLimit;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementPageToken;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.SetResourceReadOnlyRequest;
import ru.yandex.intranet.d.backend.service.proto.UpdateResourceRequest;
import ru.yandex.intranet.d.backend.service.proto.UpdatedResource;
import ru.yandex.intranet.d.backend.service.proto.UpdatedResourceType;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.utils.ErrorsHelper;

/**
 * Resources management GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ResourcesManagementServiceTest {

    @GrpcClient("inProcess")
    private ResourcesManagementServiceGrpc.ResourcesManagementServiceBlockingStub resourcesManagementService;

    @Test
    public void getResourceTest() {
        GetFullResourceRequest resourceRequest = GetFullResourceRequest.newBuilder()
                .setResourceId("ef333da9-b076-42f5-b7f5-84cd04ab7fcc")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("ef333da9-b076-42f5-b7f5-84cd04ab7fcc", resource.getResourceId());
        Assertions.assertFalse(resource.getVirtual());
    }

    @Test
    public void getResourceNotFoundTest() {
        GetFullResourceRequest resourceRequest = GetFullResourceRequest.newBuilder()
                .setResourceId("12345678-9012-3456-7890-123456789012")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
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
        ListFullResourcesByProviderRequest resourceRequest = ListFullResourcesByProviderRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        ListFullResourcesByProviderResponse page = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourcesByProvider(resourceRequest);
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getResourcesCount() > 0);
    }

    @Test
    public void getResourcesTwoPagesTest() {
        ListFullResourcesByProviderRequest firstRequest = ListFullResourcesByProviderRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setLimit(ResourcesManagementLimit.newBuilder().setLimit(1L).build())
                .build();
        ListFullResourcesByProviderResponse firstPage = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourcesByProvider(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getResourcesCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());
        ListFullResourcesByProviderRequest secondRequest = ListFullResourcesByProviderRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setPageToken(ResourcesManagementPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        ListFullResourcesByProviderResponse secondPage = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourcesByProvider(secondRequest);
        Assertions.assertNotNull(secondPage);
        Assertions.assertTrue(secondPage.getResourcesCount() > 0);
    }

    @Test
    public void createResourceTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .setVirtual(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .setAggregationSettings(AggregationSettings.newBuilder()
                                .setFreeProvisionMode(FreeProvisionAggregationMode.NONE)
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        Assertions.assertTrue(resource.getVirtual());
        Assertions.assertNotNull(resource.getProviderApiUnitId());
        Assertions.assertNotEquals("", resource.getProviderApiUnitId());
        Assertions.assertEquals(FeatureState.UNSPECIFIED, resource.getAllocatedSupported());
    }

    @Test
    public void createResourceAllocatedSupportedTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .setVirtual(true)
                        .setAllocatedSupported(FeatureState.ENABLED)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        Assertions.assertTrue(resource.getVirtual());
        Assertions.assertNotNull(resource.getProviderApiUnitId());
        Assertions.assertNotEquals("", resource.getProviderApiUnitId());
        Assertions.assertEquals(FeatureState.ENABLED, resource.getAllocatedSupported());
    }

    @Test
    public void createResourceNonSegmentedTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId(TestProviders.YDB_ID)
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("5fb2d884-614d-44e7-9aca-bed438b6e73d")
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YDB_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals(TestProviders.YDB_ID, resource.getProviderId());
    }

    @Test
    public void createResourceWithUnitsEnsembleByTypeTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        try {
            FullResource resource = resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .createResource(resourceRequest);
            Assertions.assertNotNull(resource);
            Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(ErrorsHelper.extractErrorDetails(e).toString(), e);
        }
    }

    @Test
    public void createResourceEmptyNameTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .createResource(resourceRequest);
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
    public void createResourceKeyConflictTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("yp_hdd_sas")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                                                .setSegmentId("8f6a2b58-b10c-4742-bee6-b3587793b5e8")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .createResource(resourceRequest);
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
    public void updateResourceTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setProviderApiUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        UpdateResourceRequest updateResourceRequest = UpdateResourceRequest.newBuilder()
                .setResource(UpdatedResource.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .setManaged(false)
                        .setOrderable(false)
                        .addAllAllowedUnitIds(List.of("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                                "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5")
                        .setProviderApiUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setAggregationSettings(AggregationSettings.newBuilder()
                                .setFreeProvisionMode(FreeProvisionAggregationMode.NONE)
                                .build())
                        .build())
                .setResourceId(resource.getResourceId())
                .setVersion(resource.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            FullResource updatedResource = resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResource(updateResourceRequest);
            Assertions.assertNotNull(updatedResource);
            Assertions.assertEquals("Test-1", updatedResource.getNameEn());
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(
                    new String(e.getTrailers().get(io.grpc.Metadata.Key.of("grpc-status-details-bin",
                            Metadata.BINARY_BYTE_MARSHALLER)), StandardCharsets.UTF_8), e
            );
        }
    }

    @Test
    public void updateResourceAllocatedSupportedTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setProviderApiUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        UpdateResourceRequest updateResourceRequest = UpdateResourceRequest.newBuilder()
                .setResource(UpdatedResource.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .setManaged(false)
                        .setOrderable(false)
                        .addAllAllowedUnitIds(List.of("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                                "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5")
                        .setProviderApiUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setAllocatedSupported(FeatureState.ENABLED)
                        .build())
                .setResourceId(resource.getResourceId())
                .setVersion(resource.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            FullResource updatedResource = resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResource(updateResourceRequest);
            Assertions.assertNotNull(updatedResource);
            Assertions.assertEquals("Test-1", updatedResource.getNameEn());
            Assertions.assertEquals(FeatureState.ENABLED, updatedResource.getAllocatedSupported());
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(
                    new String(e.getTrailers().get(io.grpc.Metadata.Key.of("grpc-status-details-bin",
                            Metadata.BINARY_BYTE_MARSHALLER)), StandardCharsets.UTF_8), e
            );
        }
    }

    @Test
    public void updateResourceNotFoundTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        UpdateResourceRequest updateResourceRequest = UpdateResourceRequest.newBuilder()
                .setResource(UpdatedResource.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .setManaged(false)
                        .setOrderable(false)
                        .addAllAllowedUnitIds(List.of("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                                "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5")
                        .build())
                .setResourceId("12345678-9012-3456-7890-123456789012")
                .setVersion(resource.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResource(updateResourceRequest);
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
    public void updateResourceVersionMismatchTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        UpdateResourceRequest updateResourceRequest = UpdateResourceRequest.newBuilder()
                .setResource(UpdatedResource.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .setManaged(false)
                        .setOrderable(false)
                        .addAllAllowedUnitIds(List.of("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                                "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5")
                        .build())
                .setResourceId(resource.getResourceId())
                .setVersion(resource.getVersion() + 1L)
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResource(updateResourceRequest);
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
    public void updateResourceEmptyNameTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        UpdateResourceRequest updateResourceRequest = UpdateResourceRequest.newBuilder()
                .setResource(UpdatedResource.newBuilder()
                        .setNameEn("")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .setManaged(false)
                        .setOrderable(false)
                        .addAllAllowedUnitIds(List.of("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                                "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5")
                        .build())
                .setResourceId(resource.getResourceId())
                .setVersion(resource.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResource(updateResourceRequest);
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
    public void setResourceReadOnlyTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .setResourceReadOnly(SetResourceReadOnlyRequest.newBuilder()
                        .setResourceId(resource.getResourceId())
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setReadOnly(true)
                        .build());
    }

    /**
     * Set resource read-only, state unchanged test
     *
     * @see ru.yandex.intranet.d.services.resources.ResourcesService#setReadOnly
     */
    @Test
    public void setResourceReadOnlyUnchangedTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .setResourceReadOnly(SetResourceReadOnlyRequest.newBuilder()
                        .setResourceId(resource.getResourceId())
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setReadOnly(false)
                        .build());
    }

    @Test
    public void setResourceReadOnlyNotFoundTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .setResourceReadOnly(SetResourceReadOnlyRequest.newBuilder()
                            .setResourceId("12345678-9012-3456-7890-123456789012")
                            .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                            .setReadOnly(true)
                            .build());
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
    public void createResourceNotMatchUnitForTypeTest() {
        final String resourceTypeId = "86407d21-0bd9-48ca-9a81-5e40fb3d8477";
        final String unitsEnsembleId1 = "c2807482-a3b9-4e16-822c-64ff47154ee2";
        final String unitsEnsembleId2 = "b02344bf-96af-4cc5-937c-66a479989ce8";

        FullResource resource1 = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(CreateResourceRequest.newBuilder().setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test1")
                        .setUnitsEnsembleId(unitsEnsembleId1)
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("616eacbc-5b9c-4f79-a82c-5d096c8c0726",
                                "57091a7e-8461-4453-8094-a5ff737d1702"))
                        .setDefaultUnitId("616eacbc-5b9c-4f79-a82c-5d096c8c0726")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId(resourceTypeId)
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                                                .setSegmentId("8f6a2b58-b10c-4742-bee6-b3587793b5e8")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build()).build()
                );
        Assertions.assertNotNull(resource1);
        Assertions.assertEquals(unitsEnsembleId1, resource1.getUnitsEnsembleId());

        try {
            //noinspection ResultOfMethodCallIgnored
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .createResource(CreateResourceRequest.newBuilder().setResource(NewResource.newBuilder()
                            .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                            .setResourceKey("test2")
                            .setUnitsEnsembleId(unitsEnsembleId2)
                            .setNameEn("Test")
                            .setNameRu("Тест")
                            .setDescriptionEn("Test description")
                            .setDescriptionRu("Тестовое описание")
                            .setReadOnly(false)
                            .setManaged(true)
                            .setOrderable(true)
                            .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                    "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                            .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                            .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                    .setResourceTypeId(resourceTypeId)
                                    .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                    .setSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                                                    .setSegmentId("8f6a2b58-b10c-4742-bee6-b3587793b5e8")
                                                    .build(),
                                            ResourceSegmentIds.newBuilder()
                                                    .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                    .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                    .build()))
                                    .build())
                            .build()).build()
                    );
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertEquals(1, details.get().getFieldErrorsCount());
            FieldError errors = details.get().getFieldErrors(0);
            Assertions.assertEquals("unitsEnsembleId", errors.getKey());
            Assertions.assertEquals(1, errors.getErrorsCount());
            Assertions.assertEquals(
                    "Units ensemble for the resource does not match the one specified for the resource type.",
                    errors.getErrors(0)
            );
            return;
        }
        Assertions.fail();
    }

    @Test
    public void setResourceReadOnlyAcceptableByProviderAdmin() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YDB_ADMIN_UID))
                .createResource(resourceRequest));
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .createResource(resourceRequest));
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YP_ADMIN_UID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
    }

    @Test
    public void createResourceOnlyAcceptableByProviderAdmin() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YDB_ADMIN_UID))
                .createResource(resourceRequest));
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .createResource(resourceRequest));
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YP_ADMIN_UID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
    }

    @Test
    public void updateResourceOnlyAcceptableByProviderAdmin() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        UpdateResourceRequest updateResourceRequest = UpdateResourceRequest.newBuilder()
                .setResource(UpdatedResource.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .setManaged(false)
                        .setOrderable(false)
                        .addAllAllowedUnitIds(List.of("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                                "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5")
                )
                .setResourceId(resource.getResourceId())
                .setVersion(resource.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YDB_ADMIN_UID))
                .updateResource(updateResourceRequest));
        Assertions.assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .updateResource(updateResourceRequest));
        FullResource updatedResource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YP_ADMIN_UID))
                .updateResource(updateResourceRequest);
        Assertions.assertNotNull(updatedResource);
        Assertions.assertEquals("Test-1", updatedResource.getNameEn());
    }

    @Test
    public void createAndUpdateResourceProviderApiUnitTest() {
        //if providerApiUnitId is empty then minAllowedUnitId is set
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setProviderApiUnitId("") //empty
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        Assertions.assertEquals("74fe1983-144a-4156-8839-aa791cc2deb6", resource.getProviderApiUnitId());

        //specify providerApiUnitId
        UpdateResourceRequest updateResourceRequest = UpdateResourceRequest.newBuilder()
                .setResource(UpdatedResource.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .setManaged(false)
                        .setOrderable(false)
                        .addAllAllowedUnitIds(List.of("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                                "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5")
                        .setProviderApiUnitId("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5")
                        .build())
                .setResourceId(resource.getResourceId())
                .setVersion(resource.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            FullResource updatedResource = resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResource(updateResourceRequest);
            Assertions.assertNotNull(updatedResource);
            Assertions.assertEquals("Test-1", updatedResource.getNameEn());
            Assertions.assertEquals("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", updatedResource.getProviderApiUnitId());
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(
                    new String(Objects.requireNonNull(e.getTrailers().get(Metadata.Key.of("grpc-status-details-bin",
                            Metadata.BINARY_BYTE_MARSHALLER)))), e
            );
        }
    }

    @Test
    public void createResourceWithoutVirtualFlagTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        //no 'virtual' flag
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resource.getProviderId());
        Assertions.assertFalse(resource.getVirtual());
    }

    @Test
    public void updateResourceVirtualFlagTest() {
        CreateResourceRequest resourceRequest = CreateResourceRequest.newBuilder()
                .setResource(NewResource.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setResourceKey("test")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .setManaged(true)
                        .setOrderable(true)
                        .addAllAllowedUnitIds(List.of("74fe1983-144a-4156-8839-aa791cc2deb6",
                                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", "d1456370-c8bb-442f-8ded-b2c96afacb3c"))
                        .setDefaultUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setProviderApiUnitId("74fe1983-144a-4156-8839-aa791cc2deb6")
                        .setSegmentations(ResourceSegmentationsIds.newBuilder()
                                .setResourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                                                .setSegmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                                .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                                .build()))
                                .build())
                        .build())
                .build();
        FullResource resource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResource(resourceRequest);
        Assertions.assertNotNull(resource);
        //checks that flag changes on update with 'virtual' flag set
        boolean newVirtualFlag = !resource.getVirtual();
        UpdateResourceRequest updateResourceRequest = UpdateResourceRequest.newBuilder()
                .setResourceId(resource.getResourceId())
                .setVersion(resource.getVersion())
                .setProviderId(resource.getProviderId())
                .setResource(UpdatedResource.newBuilder()
                        .setNameEn(resource.getNameEn())
                        .setNameRu(resource.getNameRu())
                        .setDescriptionEn(resource.getDescriptionEn())
                        .setDescriptionRu(resource.getDescriptionRu())
                        .setManaged(resource.getManaged())
                        .setOrderable(resource.getOrderable())
                        .addAllAllowedUnitIds(resource.getAllowedUnitIdsList())
                        .setDefaultUnitId(resource.getDefaultUnitId())
                        .setProviderApiUnitId(resource.getProviderApiUnitId())
                        .setVirtual(newVirtualFlag)
                        .build())
                .build();
        FullResource responseResource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .updateResource(updateResourceRequest);
        Assertions.assertNotNull(responseResource);
        Assertions.assertEquals(newVirtualFlag, responseResource.getVirtual());

        //checks that 'virtual' == false on update without 'virtual' flag set
        UpdateResourceRequest secondUpdateResourceRequest = UpdateResourceRequest.newBuilder()
                .setResourceId(resource.getResourceId())
                .setVersion(responseResource.getVersion())
                .setProviderId(resource.getProviderId())
                .setResource(UpdatedResource.newBuilder()
                        .setNameEn(responseResource.getNameEn())
                        .setNameRu(responseResource.getNameRu())
                        .setDescriptionEn(responseResource.getDescriptionEn())
                        .setDescriptionRu(responseResource.getDescriptionRu())
                        .setManaged(responseResource.getManaged())
                        .setOrderable(responseResource.getOrderable())
                        .addAllAllowedUnitIds(responseResource.getAllowedUnitIdsList())
                        .setDefaultUnitId(responseResource.getDefaultUnitId())
                        .setProviderApiUnitId(responseResource.getProviderApiUnitId())
                        //no virtual flag
                        .build())
                .build();
        responseResource = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .updateResource(secondUpdateResourceRequest);
        Assertions.assertNotNull(responseResource);
        Assertions.assertFalse(responseResource.getVirtual());
    }

    @Test
    public void patchResourceTypeTest() {
        CreateResourceTypeRequest createResourceTypeRequest = CreateResourceTypeRequest.newBuilder()
                .setResourceType(NewResourceType.newBuilder()
                        .setProviderId(TestProviders.YP_ID)
                        .setKey("test_key")
                        .setNameEn("Name en")
                        .setNameRu("Name ru")
                        .setDescriptionEn("Desc en")
                        .setDescriptionRu("Desc ru")
                        .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                        .setSortingOrder(0L)
                        .setAggregationSettings(AggregationSettings.newBuilder()
                                .setFreeProvisionMode(FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
                                .build())
                        .build())
                .build();

        FullResourceType responseCreateResourceType = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceType(createResourceTypeRequest);

        PatchResourceTypeRequest patchResourceTypeRequest = PatchResourceTypeRequest.newBuilder()
                .setProviderId(responseCreateResourceType.getProviderId())
                .setVersion(responseCreateResourceType.getVersion())
                .setResourceTypeId(responseCreateResourceType.getResourceTypeId())
                .setResourceType(UpdatedResourceType.newBuilder()
                        .setNameEn("Test name en")
                        .setNameRu("Test name ru")
                        .clearAggregationSettings()
                        .build())
                .setFieldMask(FieldMaskUtil.fromFieldNumbers(UpdatedResourceType.class,
                        UpdatedResourceType.NAME_EN_FIELD_NUMBER,
                        UpdatedResourceType.NAME_RU_FIELD_NUMBER,
                        UpdatedResourceType.AGGREGATION_SETTINGS_FIELD_NUMBER))
                .build();

        FullResourceType responsePatchResourceType = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .patchResourceType(patchResourceTypeRequest);

        Assertions.assertEquals(responseCreateResourceType.getResourceTypeId(),
                responsePatchResourceType.getResourceTypeId());
        Assertions.assertEquals(responseCreateResourceType.getProviderId(),
                responsePatchResourceType.getProviderId());
        Assertions.assertEquals(responseCreateResourceType.getVersion() + 1,
                responsePatchResourceType.getVersion());
        Assertions.assertEquals(patchResourceTypeRequest.getResourceType().getNameEn(),
                responsePatchResourceType.getNameEn());
        Assertions.assertEquals(patchResourceTypeRequest.getResourceType().getNameRu(),
                responsePatchResourceType.getNameRu());
        Assertions.assertFalse(responsePatchResourceType.hasAggregationSettings());
    }
}
