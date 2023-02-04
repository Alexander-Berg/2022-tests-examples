package ru.yandex.intranet.d.grpc.resources.segmentations;

import java.util.Optional;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.proto.CreateResourceSegmentationRequest;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.FullResourceSegmentation;
import ru.yandex.intranet.d.backend.service.proto.GetResourceSegmentationRequest;
import ru.yandex.intranet.d.backend.service.proto.ListResourceSegmentationsByProviderRequest;
import ru.yandex.intranet.d.backend.service.proto.ListResourceSegmentationsByProviderResponse;
import ru.yandex.intranet.d.backend.service.proto.MultilingualGrammaticalForms;
import ru.yandex.intranet.d.backend.service.proto.MultilingualGrammaticalForms.GrammaticalCases;
import ru.yandex.intranet.d.backend.service.proto.NewResourceSegmentation;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementLimit;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementPageToken;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.SegmentationUISettings;
import ru.yandex.intranet.d.backend.service.proto.UpdateResourceSegmentationRequest;
import ru.yandex.intranet.d.backend.service.proto.UpdatedResourceSegmentation;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.utils.ErrorsHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resource segmentations GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ResourceSegmentationsServiceTest {

    @GrpcClient("inProcess")
    private ResourcesManagementServiceGrpc.ResourcesManagementServiceBlockingStub resourcesManagementService;

    @Test
    public void getResourceSegmentationTest() {
        GetResourceSegmentationRequest resourceSegmentationRequest = GetResourceSegmentationRequest.newBuilder()
                .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceSegmentation resourceSegmentation = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getResourceSegmentation(resourceSegmentationRequest);
        assertNotNull(resourceSegmentation);
        assertEquals("7fbd778f-d803-44c8-831a-c1de5c05885c",
                resourceSegmentation.getResourceSegmentationId());
    }

    @Test
    public void getResourceSegmentationNotFoundTest() {
        GetResourceSegmentationRequest resourceSegmentationRequest = GetResourceSegmentationRequest.newBuilder()
                .setResourceSegmentationId("12345678-9012-3456-7890-123456789012")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getResourceSegmentation(resourceSegmentationRequest);
        } catch (StatusRuntimeException e) {
            assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            assertTrue(details.isPresent());
            assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void getResourceSegmentationsPageTest() {
        ListResourceSegmentationsByProviderRequest resourceSegmentationsRequest
                = ListResourceSegmentationsByProviderRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        ListResourceSegmentationsByProviderResponse page = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourceSegmentationsByProvider(resourceSegmentationsRequest);
        assertNotNull(page);
        assertTrue(page.getResourceSegmentationsCount() > 0);
    }

    @Test
    public void getResourceSegmentationsTwoPagesTest() {
        ListResourceSegmentationsByProviderRequest firstRequest = ListResourceSegmentationsByProviderRequest
                .newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setLimit(ResourcesManagementLimit.newBuilder().setLimit(1L).build())
                .build();
        ListResourceSegmentationsByProviderResponse firstPage = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourceSegmentationsByProvider(firstRequest);
        assertNotNull(firstPage);
        assertEquals(1, firstPage.getResourceSegmentationsCount());
        assertTrue(firstPage.hasNextPageToken());
        ListResourceSegmentationsByProviderRequest secondRequest = ListResourceSegmentationsByProviderRequest
                .newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setPageToken(ResourcesManagementPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        ListResourceSegmentationsByProviderResponse secondPage = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listResourceSegmentationsByProvider(secondRequest);
        assertNotNull(secondPage);
        assertTrue(secondPage.getResourceSegmentationsCount() > 0);
    }

    @Test
    public void createResourceSegmentationTest() {
        CreateResourceSegmentationRequest resourceSegmentationRequest = CreateResourceSegmentationRequest.newBuilder()
                .setResourceSegmentation(NewResourceSegmentation.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .build();
        FullResourceSegmentation resourceSegmentation = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegmentation(resourceSegmentationRequest);
        assertNotNull(resourceSegmentation);
        assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceSegmentation.getProviderId());
    }

    @Test
    public void createResourceSegmentationOnlyAcceptableByProviderAdmin() {
        CreateResourceSegmentationRequest resourceSegmentationRequest = CreateResourceSegmentationRequest.newBuilder()
                .setResourceSegmentation(NewResourceSegmentation.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .build();
        assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YDB_ADMIN_UID))
                .createResourceSegmentation(resourceSegmentationRequest));
        assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .createResourceSegmentation(resourceSegmentationRequest));
        FullResourceSegmentation resourceSegmentation = resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YP_ADMIN_UID))
                .createResourceSegmentation(resourceSegmentationRequest);
        assertNotNull(resourceSegmentation);
        assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceSegmentation.getProviderId());
    }

    @Test
    public void createResourceSegmentationEmptyNameTest() {
        CreateResourceSegmentationRequest resourceSegmentationRequest = CreateResourceSegmentationRequest.newBuilder()
                .setResourceSegmentation(NewResourceSegmentation.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .createResourceSegmentation(resourceSegmentationRequest);
        } catch (StatusRuntimeException e) {
            assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            assertTrue(details.isPresent());
            assertTrue(details.get().getFieldErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void createResourceSegmentationKeyConflictTest() {
        CreateResourceSegmentationRequest resourceSegmentationRequest = CreateResourceSegmentationRequest.newBuilder()
                .setResourceSegmentation(NewResourceSegmentation.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("location")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .createResourceSegmentation(resourceSegmentationRequest);
        } catch (StatusRuntimeException e) {
            assertEquals(Status.Code.ALREADY_EXISTS, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            assertTrue(details.isPresent());
            assertTrue(details.get().getFieldErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void updateResourceSegmentationTest() {
        CreateResourceSegmentationRequest resourceSegmentationRequest = CreateResourceSegmentationRequest.newBuilder()
                .setResourceSegmentation(NewResourceSegmentation.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .build();
        FullResourceSegmentation resourceSegmentation = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegmentation(resourceSegmentationRequest);
        assertNotNull(resourceSegmentation);
        assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceSegmentation.getProviderId());
        UpdateResourceSegmentationRequest updateResourceSegmentationRequest = UpdateResourceSegmentationRequest
                .newBuilder()
                .setResourceSegmentation(UpdatedResourceSegmentation.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceSegmentationId(resourceSegmentation.getResourceSegmentationId())
                .setVersion(resourceSegmentation.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceSegmentation updatedResourceSegmentation = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .updateResourceSegmentation(updateResourceSegmentationRequest);
        assertNotNull(updatedResourceSegmentation);
        assertEquals("Test-1", updatedResourceSegmentation.getNameEn());
    }

    @Test
    public void updateResourceSegmentationOnlyAcceptableByProviderAdmin() {
        CreateResourceSegmentationRequest resourceSegmentationRequest = CreateResourceSegmentationRequest.newBuilder()
                .setResourceSegmentation(NewResourceSegmentation.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .build();
        FullResourceSegmentation resourceSegmentation = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegmentation(resourceSegmentationRequest);
        assertNotNull(resourceSegmentation);
        assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceSegmentation.getProviderId());
        UpdateResourceSegmentationRequest updateResourceSegmentationRequest = UpdateResourceSegmentationRequest
                .newBuilder()
                .setResourceSegmentation(UpdatedResourceSegmentation.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceSegmentationId(resourceSegmentation.getResourceSegmentationId())
                .setVersion(resourceSegmentation.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YDB_ADMIN_UID))
                .updateResourceSegmentation(updateResourceSegmentationRequest));
        assertThrows(io.grpc.StatusRuntimeException.class, () -> resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .updateResourceSegmentation(updateResourceSegmentationRequest));
        FullResourceSegmentation updatedResourceSegmentation = resourcesManagementService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.YP_ADMIN_UID))
                .updateResourceSegmentation(updateResourceSegmentationRequest);
        assertNotNull(updatedResourceSegmentation);
        assertEquals("Test-1", updatedResourceSegmentation.getNameEn());
    }

    @Test
    public void updateResourceSegmentationNotFoundTest() {
        CreateResourceSegmentationRequest resourceSegmentationRequest = CreateResourceSegmentationRequest.newBuilder()
                .setResourceSegmentation(NewResourceSegmentation.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .build();
        FullResourceSegmentation resourceSegmentation = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegmentation(resourceSegmentationRequest);
        assertNotNull(resourceSegmentation);
        assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceSegmentation.getProviderId());
        UpdateResourceSegmentationRequest updateResourceSegmentationRequest = UpdateResourceSegmentationRequest
                .newBuilder()
                .setResourceSegmentation(UpdatedResourceSegmentation.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceSegmentationId("12345678-9012-3456-7890-123456789012")
                .setVersion(resourceSegmentation.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResourceSegmentation(updateResourceSegmentationRequest);
        } catch (StatusRuntimeException e) {
            assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            assertTrue(details.isPresent());
            assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void updateResourceSegmentationVersionMismatchTest() {
        CreateResourceSegmentationRequest resourceSegmentationRequest = CreateResourceSegmentationRequest.newBuilder()
                .setResourceSegmentation(NewResourceSegmentation.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .build();
        FullResourceSegmentation resourceSegmentation = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegmentation(resourceSegmentationRequest);
        assertNotNull(resourceSegmentation);
        assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceSegmentation.getProviderId());
        UpdateResourceSegmentationRequest updateResourceSegmentationRequest = UpdateResourceSegmentationRequest
                .newBuilder()
                .setResourceSegmentation(UpdatedResourceSegmentation.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceSegmentationId(resourceSegmentation.getResourceSegmentationId())
                .setVersion(resourceSegmentation.getVersion() + 1L)
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResourceSegmentation(updateResourceSegmentationRequest);
        } catch (StatusRuntimeException e) {
            assertEquals(Status.Code.FAILED_PRECONDITION, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            assertTrue(details.isPresent());
            assertTrue(details.get().getFieldErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void updateResourceSegmentationEmptyNameTest() {
        CreateResourceSegmentationRequest resourceSegmentationRequest = CreateResourceSegmentationRequest.newBuilder()
                .setResourceSegmentation(NewResourceSegmentation.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .build();
        FullResourceSegmentation resourceSegmentation = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegmentation(resourceSegmentationRequest);
        assertNotNull(resourceSegmentation);
        assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", resourceSegmentation.getProviderId());
        UpdateResourceSegmentationRequest updateResourceSegmentationRequest = UpdateResourceSegmentationRequest
                .newBuilder()
                .setResourceSegmentation(UpdatedResourceSegmentation.newBuilder()
                        .setNameEn("")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setResourceSegmentationId(resourceSegmentation.getResourceSegmentationId())
                .setVersion(resourceSegmentation.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        try {
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .updateResourceSegmentation(updateResourceSegmentationRequest);
        } catch (StatusRuntimeException e) {
            assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            assertTrue(details.isPresent());
            assertTrue(details.get().getFieldErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void createResourceSegmentationWithUiSettingsTest() {
        FullResourceSegmentation createdSegmentation = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegmentation(CreateResourceSegmentationRequest.newBuilder()
                        .setResourceSegmentation(NewResourceSegmentation.newBuilder()
                                .setProviderId(TestProviders.YP_ID)
                                .setKey("test")
                                .setNameEn("Test")
                                .setNameRu("Тест")
                                .setDescriptionEn("Test description")
                                .setDescriptionRu("Тестовое описание")
                                .setUiSettings(SegmentationUISettings.newBuilder()
                                        .setChoiceOrder(2)
                                        .setInSameBlockWithPrevious(true)
                                        .setTitle(MultilingualGrammaticalForms.newBuilder()
                                                .setNameSingularRu(GrammaticalCases.newBuilder()
                                                        .setAccusative("Тестовую сегментацию")
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build());
        assertNotNull(createdSegmentation);
        assertNotNull(createdSegmentation.getUiSettings());
        String segmentationId = createdSegmentation.getResourceSegmentationId();
        assertNotNull(segmentationId);

        FullResourceSegmentation segmentation = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getResourceSegmentation(GetResourceSegmentationRequest.newBuilder()
                        .setResourceSegmentationId(segmentationId)
                        .setProviderId(TestProviders.YP_ID)
                        .build());
        assertNotNull(segmentation);
        SegmentationUISettings uiSettings = segmentation.getUiSettings();
        assertNotNull(uiSettings);
        assertEquals(2, uiSettings.getChoiceOrder());
        assertTrue(uiSettings.getInSameBlockWithPrevious());
        assertEquals("Тестовую сегментацию", uiSettings.getTitle().getNameSingularRu().getAccusative());

        FullResourceSegmentation updatedResourceSegmentation = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .updateResourceSegmentation(UpdateResourceSegmentationRequest
                        .newBuilder()
                        .setResourceSegmentation(UpdatedResourceSegmentation.newBuilder()
                                .setNameEn(segmentation.getNameEn())
                                .setNameRu(segmentation.getNameRu())
                                .setDescriptionEn(segmentation.getDescriptionEn())
                                .setDescriptionRu(segmentation.getDescriptionRu())
                                .setGroupingOrder(segmentation.getGroupingOrder())
                                .setUiSettings(SegmentationUISettings.newBuilder(segmentation.getUiSettings())
                                        .setChoiceOrder(3)
                                        .setTitle(MultilingualGrammaticalForms
                                                .newBuilder(segmentation.getUiSettings().getTitle())
                                                .setNameSingularRu(GrammaticalCases
                                                        .newBuilder(segmentation.getUiSettings().getTitle()
                                                                .getNameSingularRu())
                                                        .setDative("Тестовой сегментации")
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .setResourceSegmentationId(segmentationId)
                        .setVersion(0)
                        .setProviderId(TestProviders.YP_ID)
                        .build());
        assertNotNull(updatedResourceSegmentation);
        assertNotNull(updatedResourceSegmentation.getUiSettings());

        FullResourceSegmentation segmentation2 = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getResourceSegmentation(GetResourceSegmentationRequest.newBuilder()
                        .setResourceSegmentationId(segmentationId)
                        .setProviderId(TestProviders.YP_ID)
                        .build());
        assertNotNull(segmentation2);
        SegmentationUISettings uiSettings2 = segmentation2.getUiSettings();
        assertNotNull(uiSettings2);
        assertEquals(3, uiSettings2.getChoiceOrder());
        assertTrue(uiSettings2.getInSameBlockWithPrevious());
        GrammaticalCases nameSingularRu = uiSettings2.getTitle().getNameSingularRu();
        assertEquals("Тестовую сегментацию", nameSingularRu.getAccusative());
        assertEquals("Тестовой сегментации", nameSingularRu.getDative());
    }
}
