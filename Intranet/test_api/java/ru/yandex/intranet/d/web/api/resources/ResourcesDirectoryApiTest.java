package ru.yandex.intranet.d.web.api.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.UnitsEnsembleIds;
import ru.yandex.intranet.d.backend.service.proto.CreateAccountsSpaceRequest;
import ru.yandex.intranet.d.backend.service.proto.CreateResourceSegmentRequest;
import ru.yandex.intranet.d.backend.service.proto.FullAccountsSpace;
import ru.yandex.intranet.d.backend.service.proto.FullResourceSegment;
import ru.yandex.intranet.d.backend.service.proto.NewAccountsSpace;
import ru.yandex.intranet.d.backend.service.proto.NewResourceSegment;
import ru.yandex.intranet.d.backend.service.proto.ResourceSegmentIds;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementServiceGrpc;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.FeatureStateDto;
import ru.yandex.intranet.d.web.model.FeatureStateInputDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.ProviderDto;
import ru.yandex.intranet.d.web.model.providers.AggregationSettingsInputDto;
import ru.yandex.intranet.d.web.model.providers.FreeProvisionAggregationModeInputDto;
import ru.yandex.intranet.d.web.model.providers.UsageModeInputDto;
import ru.yandex.intranet.d.web.model.resources.directory.CreateResourceSegmentationSegmentDto;
import ru.yandex.intranet.d.web.model.resources.directory.CreateResourceTypeSegmentsDto;
import ru.yandex.intranet.d.web.model.resources.directory.InnerResourceSegmentationSegmentDto;
import ru.yandex.intranet.d.web.model.resources.directory.ResourceCreateDto;
import ru.yandex.intranet.d.web.model.resources.directory.ResourceDto;
import ru.yandex.intranet.d.web.model.resources.directory.ResourcePutDto;

import static java.util.Locale.ENGLISH;
import static ru.yandex.intranet.d.TestProviders.YDB_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResourceTypes.YP_HDD;
import static ru.yandex.intranet.d.TestSegmentations.YDB_LOCATION_SAS;
import static ru.yandex.intranet.d.TestSegmentations.YP_LOCATION2;
import static ru.yandex.intranet.d.TestSegmentations.YP_LOCATION2_MAN2;
import static ru.yandex.intranet.d.TestSegmentations.YP_LOCATION_VLA;
import static ru.yandex.intranet.d.TestSegmentations.YP_SEGMENT;
import static ru.yandex.intranet.d.TestSegmentations.YP_SEGMENT_DEFAULT;
import static ru.yandex.intranet.d.UnitIds.GIGABYTES;
import static ru.yandex.intranet.d.UnitIds.PETABYTES;
import static ru.yandex.intranet.d.UnitIds.TERABYTES;

/**
 * Resource directory public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ResourcesDirectoryApiTest {

    @GrpcClient("inProcess")
    private ResourcesManagementServiceGrpc.ResourcesManagementServiceBlockingStub resourcesManagementService;
    @Autowired
    private WebTestClient webClient;
    @Autowired
    @Qualifier("messageSource")
    private MessageSource messages;

    @Test
    public void getResourceTest() {
        ResourceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "ef333da9-b076-42f5-b7f5-84cd04ab7fcc")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("ef333da9-b076-42f5-b7f5-84cd04ab7fcc", result.getId());
        Assertions.assertFalse(result.isVirtual());
    }

    @Test
    public void getResourceTypeNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "12345678-9012-3456-7890-123456789012")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void getResourcesPageTest() {
        PageDto<ResourceDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getResourcesTwoPagesTest() {
        PageDto<ResourceDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources?limit={limit}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<ResourceDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources" +
                                "?limit={limit}&pageToken={token}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", 1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }

    @Test
    public void createResourceTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED,
                new AggregationSettingsInputDto(FreeProvisionAggregationModeInputDto.NONE, UsageModeInputDto.UNDEFINED,
                        null));
        ResourceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        Assertions.assertFalse(result.isVirtual());
    }

    @Test
    public void createResourceAllocatedSupportedTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.ENABLED, null);
        ResourceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        Assertions.assertFalse(result.isVirtual());
        Assertions.assertEquals(FeatureStateDto.ENABLED, result.getAllocatedSupported());
    }

    @Test
    public void createResourceNonSegmentedTest() {
        ResourceCreateDto createDto = new ResourceCreateDto(
                "test",
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test",
                "Тест",
                "Description",
                "Описание",
                false,
                true,
                true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto(
                        "5fb2d884-614d-44e7-9aca-bed438b6e73d",
                        null
                ),
                null, false, FeatureStateInputDto.UNSPECIFIED,
                new AggregationSettingsInputDto(FreeProvisionAggregationModeInputDto.NONE, UsageModeInputDto.UNDEFINED,
                        null));
        ResourceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.YDB_ADMIN_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        TestProviders.YDB_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
    }

    /**
     * Check the conflict with resource id="c79455ac-a88f-40e3-9f6b-117c5c2cd4a2"
     * It has resource_type_id="44f93060-e367-44e6-b069-98c20d03dd81"
     * and segments [
     *     {
     *         "segmentationId": "7fbd778f-d803-44c8-831a-c1de5c05885c",
     *         "segmentId": "540b6c0f-d0b9-4f62-9b75-af7c9cfd2e95"
     *     },
     *     {
     *         "segmentationId": "4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
     *         "segmentId": "e9552be0-7b24-4c70-a1e4-dd842299a802"
     *     }
     * ]
     * We will try to create resource with the same resource_type_id and segments
     */
    @Test
    public void createResourceWithExistingTypeAndSegmentationTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("7fbd778f-d803-44c8-831a-c1de5c05885c",
                                        "540b6c0f-d0b9-4f62-9b75-af7c9cfd2e95"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of(messages.getMessage(
                "errors.resource.with.non.unique.combination.of.resource.type.and.segments",
                new Object[]{"c79455ac-a88f-40e3-9f6b-117c5c2cd4a2"},
                ENGLISH)
        ), result.getErrors());
    }

    /**
     * Check the conflict with resource id="c79455ac-a88f-40e3-9f6b-117c5c2cd4a2"
     * It has resource_type_id="44f93060-e367-44e6-b069-98c20d03dd81"
     * and segments [
     * {
     * "segmentationId": "7fbd778f-d803-44c8-831a-c1de5c05885c",
     * "segmentId": "540b6c0f-d0b9-4f62-9b75-af7c9cfd2e95"
     * },
     * {
     * "segmentationId": "4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
     * "segmentId": "e9552be0-7b24-4c70-a1e4-dd842299a802"
     * }
     * ]
     * We will try to create resource with the same resource_type_id and subset of their segments
     */
    @Test
    public void createResourceWithExistingTypeAndSubsetSegmentationTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("7fbd778f-d803-44c8-831a-c1de5c05885c",
                                "540b6c0f-d0b9-4f62-9b75-af7c9cfd2e95"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                Set.of(messages.getMessage(
                        "errors.resource.with.non.unique.combination.of.resource.type.and.segments",
                        new Object[]{"c79455ac-a88f-40e3-9f6b-117c5c2cd4a2"},
                        ENGLISH),
                        messages.getMessage(
                                "errors.accounts.space.not.found",
                                null,
                                ENGLISH)
                ), result.getErrors());
    }

    /**
     * Check the conflict with resource id="c79455ac-a88f-40e3-9f6b-117c5c2cd4a2"
     * It has resource_type_id="44f93060-e367-44e6-b069-98c20d03dd81"
     * and segments [
     * {
     * "segmentationId": "7fbd778f-d803-44c8-831a-c1de5c05885c",
     * "segmentId": "540b6c0f-d0b9-4f62-9b75-af7c9cfd2e95"
     * },
     * {
     * "segmentationId": "4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
     * "segmentId": "e9552be0-7b24-4c70-a1e4-dd842299a802"
     * }
     * ]
     * We will try to create resource with the same resource_type_id and superset of their segments
     */
    @Test
    public void createResourceWithExistingTypeAndSupersetSegmentationTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("7fbd778f-d803-44c8-831a-c1de5c05885c",
                                        "540b6c0f-d0b9-4f62-9b75-af7c9cfd2e95"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"),
                                new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                Set.of(messages.getMessage(
                        "errors.resource.with.non.unique.combination.of.resource.type.and.segments",
                        new Object[]{"c79455ac-a88f-40e3-9f6b-117c5c2cd4a2"},
                        ENGLISH),
                        messages.getMessage(
                                "errors.provider.accounts.space.ambiguous",
                                null,
                                ENGLISH)
                ), result.getErrors());
    }

    @Test
    public void createResourceImmediatelyAfterCreatingAccountSpaceTest() {
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
        FullResourceSegment newSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegment(resourceSegmentRequest);

        CreateAccountsSpaceRequest accountsSpaceRequest = CreateAccountsSpaceRequest.newBuilder()
                .setAccountsSpace(NewAccountsSpace.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                                        .setSegmentId(newSegment.getResourceSegmentId())
                                        .build(),
                                ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                        .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                        .build()))
                        .build())
                .build();
        FullAccountsSpace newAccountsSpace = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createAccountsSpace(accountsSpaceRequest);
        Assertions.assertNotNull(newAccountsSpace);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", newAccountsSpace.getProviderId());

        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("7fbd778f-d803-44c8-831a-c1de5c05885c",
                                        newSegment.getResourceSegmentId()),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
    }

    @Test
    public void createResourceEmptyResourceTypeIdTest() {
        ResourceCreateDto createDto = new ResourceCreateDto(
                "test",
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test",
                "Тест",
                "Description",
                "Описание",
                false,
                true,
                true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, null,
                null, false, FeatureStateInputDto.UNSPECIFIED, null);
        webClient
                .mutateWith(MockUser.uid(TestUsers.YDB_ADMIN_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        TestProviders.YDB_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(r -> {
                    System.out.println(r);
                    ErrorCollectionDto errorCollection = r.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Map<String, Set<String>> fieldErrors = errorCollection.getFieldErrors();
                    Assertions.assertNotNull(fieldErrors);
                    Assertions.assertEquals(
                            Map.of("segmentations.resourceTypeId", Set.of("Field is required.")),
                            fieldErrors
                    );
                });
    }

    @Test
    public void createResourceEmptyNameTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
    }

    @Test
    public void createResourceKeyConflictTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("yp_hdd_sas", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
    }

    @Test
    public void updateResourceTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourcePutDto putDto = new ResourcePutDto("Test-1", "Тест-1", "Description-1", "Описание-1",
                false, false,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5"),
                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                null, null, false, FeatureStateInputDto.UNSPECIFIED,
                new AggregationSettingsInputDto(FreeProvisionAggregationModeInputDto.NONE, UsageModeInputDto.UNDEFINED,
                        null));
        ResourceDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}?version={version}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", createResult.getId(), createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(updateResult);
        Assertions.assertNotNull(updateResult.getId());
        Assertions.assertEquals(createResult.getVersion() + 1, updateResult.getVersion());
    }

    @Test
    public void updateResourceAllocatedSupportedTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED,
                new AggregationSettingsInputDto(FreeProvisionAggregationModeInputDto.NONE, UsageModeInputDto.UNDEFINED,
                        null));
        ResourceDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourcePutDto putDto = new ResourcePutDto("Test-1", "Тест-1", "Description-1", "Описание-1",
                false, false,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5"),
                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                null, null, false, FeatureStateInputDto.ENABLED, null);
        ResourceDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}?version={version}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", createResult.getId(), createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(updateResult);
        Assertions.assertNotNull(updateResult.getId());
        Assertions.assertEquals(createResult.getVersion() + 1, updateResult.getVersion());
        Assertions.assertEquals(FeatureStateDto.ENABLED, updateResult.getAllocatedSupported());
    }

    @Test
    public void updateResourceNotFoundTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourcePutDto putDto = new ResourcePutDto("Test-1", "Тест-1", "Description-1", "Описание-1",
                false, false,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5"),
                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                null, null, false, FeatureStateInputDto.UNSPECIFIED, null);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}?version={version}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "12345678-9012-3456-7890-123456789012",
                        createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(updateResult);
        Assertions.assertFalse(updateResult.getErrors().isEmpty());
    }

    @Test
    public void updateResourceVersionMismatchTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourcePutDto putDto = new ResourcePutDto("Test-1", "Тест-1", "Description-1", "Описание-1",
                false, false,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5"),
                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                null, null, false, FeatureStateInputDto.UNSPECIFIED, null);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}?version={version}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", createResult.getId(),
                        createResult.getVersion() + 1L)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.PRECONDITION_FAILED)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(updateResult);
        Assertions.assertFalse(updateResult.getFieldErrors().isEmpty());
    }

    @Test
    public void updateResourceEmptyNameTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourcePutDto putDto = new ResourcePutDto("", "Тест-1", "Description-1", "Описание-1",
                false, false,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5"),
                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                null, null, false, FeatureStateInputDto.UNSPECIFIED, null);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}?version={version}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", createResult.getId(), createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(updateResult);
        Assertions.assertFalse(updateResult.getFieldErrors().isEmpty());
    }

    @Test
    public void setResourceReadOnlyTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}/_readOnly?readOnly={readOnly}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", createResult.getId(), true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void setProviderReadOnlyUnchangedTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}/_readOnly?readOnly={readOnly}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", createResult.getId(), false)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void setProviderReadOnlyNotFoundTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}/_readOnly?readOnly={readOnly}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "12345678-9012-3456-7890-123456789012", true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void getResourceAcceptableForDAdminsTest() {
        ResourceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "ef333da9-b076-42f5-b7f5-84cd04ab7fcc")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("ef333da9-b076-42f5-b7f5-84cd04ab7fcc", result.getId());
    }

    @Test
    public void createResourceAcceptableForDAdminsTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                null, new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
    }

    @Test
    public void createUpdateResourceWithDefaultQuotasTest() {
        ResourceCreateDto createDto = new ResourceCreateDto(
                "test",
                UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID,
                "Test",
                "Тест",
                "Description",
                "Описание",
                false,
                true,
                true,
                List.of(GIGABYTES, TERABYTES, PETABYTES),
                GIGABYTES,
                null, new CreateResourceTypeSegmentsDto(YP_HDD, List.of(
                        new CreateResourceSegmentationSegmentDto(YP_LOCATION2, YP_LOCATION2_MAN2),
                        new CreateResourceSegmentationSegmentDto(YP_SEGMENT, YP_SEGMENT_DEFAULT)
                )),
                1000L,
                false, FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto resource = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources", TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resource);
        Assertions.assertNotNull(resource.getId());

        ru.yandex.intranet.d.web.model.ProviderDto providerWithDefaults = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{id}", TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(providerWithDefaults);
        Assertions.assertTrue(providerWithDefaults.isHasDefaultQuotas());

        ResourcePutDto putDto = new ResourcePutDto(
                resource.getNameEn(),
                resource.getNameRu(),
                resource.getDescriptionEn(),
                resource.getDescriptionRu(),
                resource.isOrderable(),
                resource.isManaged(),
                new ArrayList<>(resource.getAllowedUnitIds()),
                resource.getDefaultUnitId(),
                null, 0L,
                false, FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}?version={version}",
                        TestProviders.YP_ID, resource.getId(), resource.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(updateResult);

        ru.yandex.intranet.d.web.model.ProviderDto providerWithoutDefaults = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{id}", TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(providerWithoutDefaults);
        Assertions.assertFalse(providerWithoutDefaults.isHasDefaultQuotas());
    }

    @Test
    public void createAndUpdateResourceProviderApiUnitTest() {
        //if providerApiUnitId is empty then minAllowedUnitId is set
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                "", //empty
                new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        Assertions.assertEquals("74fe1983-144a-4156-8839-aa791cc2deb6", createResult.getProviderApiUnitId());

        //specify providerApiUnitId
        ResourcePutDto putDto = new ResourcePutDto("Test-1", "Тест-1", "Description-1", "Описание-1",
                false, false,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5"),
                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", null, false, FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}?version={version}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", createResult.getId(), createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(updateResult);
        Assertions.assertNotNull(updateResult.getId());
        Assertions.assertEquals("a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", updateResult.getProviderApiUnitId());
        Assertions.assertEquals(createResult.getVersion() + 1, updateResult.getVersion());
    }

    @Test
    public void createResourceWithoutVirtualFlagTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, null,
                FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        Assertions.assertFalse(result.isVirtual());
    }

    @Test
    public void updateResourceVirtualFlagTest() {
        ResourceCreateDto createDto = new ResourceCreateDto("test", "b02344bf-96af-4cc5-937c-66a479989ce8",
                "Test", "Тест", "Description", "Описание", false, true, true,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"),
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                "74fe1983-144a-4156-8839-aa791cc2deb6",
                new CreateResourceTypeSegmentsDto("44f93060-e367-44e6-b069-98c20d03dd81",
                        List.of(new CreateResourceSegmentationSegmentDto("37e28d62-1462-46dc-8989-c2ebf44cde00",
                                        "9051becd-7e21-4e94-9015-80ba738c0a0d"),
                                new CreateResourceSegmentationSegmentDto("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a",
                                        "e9552be0-7b24-4c70-a1e4-dd842299a802"))), null, false,
                FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());

        //checks that flag changes on update with 'virtual' flag set
        ResourcePutDto putDto = new ResourcePutDto("Test-1", "Тест-1", "Description-1", "Описание-1",
                false, false,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5"),
                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                null, null, true, FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}?version={version}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", createResult.getId(), createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(updateResult);
        Assertions.assertNotNull(updateResult.getId());
        Assertions.assertTrue(updateResult.isVirtual());

        //checks that 'virtual' == false on update without 'virtual' flag set
        ResourcePutDto secondPutDto = new ResourcePutDto("Test-1", "Тест-1", "Description-1", "Описание-1",
                false, false,
                List.of("74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5"),
                "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                null, null, null, FeatureStateInputDto.UNSPECIFIED, null);
        ResourceDto secondUpdateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/{id}?version={version}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", createResult.getId(), updateResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(secondPutDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondUpdateResult);
        Assertions.assertNotNull(secondUpdateResult.getId());
        Assertions.assertFalse(secondUpdateResult.isVirtual());
    }

    @Test
    public void getResourcesPageByProviderTypeAndSegmentsTest() {
        PageDto<ResourceDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId1}&segmentId={segmentId2}&withSegmentations={withSegmentations}",
                        YP_ID, YP_HDD, YP_LOCATION_VLA, YP_SEGMENT_DEFAULT, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() { })
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
        Set<String> resultSegmentIds = result.getItems().stream()
                .map(ResourceDto::getSegmentations)
                .map(Optional::orElseThrow)
                .flatMap(i -> i.getSegmentations().stream())
                .map(InnerResourceSegmentationSegmentDto::getSegmentId)
                .collect(Collectors.toSet());
        Assertions.assertTrue(result.getItems().stream().allMatch(r -> r.getProviderId().equals(YP_ID)));
        Assertions.assertTrue(result.getItems().stream()
                .allMatch(r -> r.getSegmentations().orElseThrow().getResourceTypeId().equals(YP_HDD)));
        Assertions.assertTrue(resultSegmentIds.stream()
                .allMatch(i -> i.equals(YP_LOCATION_VLA) || i.equals(YP_SEGMENT_DEFAULT)));
    }

    @Test
    public void getResourcesPageByProviderAndTypeTest() {
        PageDto<ResourceDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/filterByType/{resourceTypeId}" +
                                "?withSegmentations={withSegmentations}",
                        YP_ID, YP_HDD, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
        Assertions.assertTrue(result.getItems().stream().allMatch(r -> r.getProviderId().equals(YP_ID)));
        Assertions.assertTrue(result.getItems().stream()
                .allMatch(r -> r.getSegmentations().orElseThrow().getResourceTypeId().equals(YP_HDD)));
    }

    @Test
    public void getResourcesPageByProviderTypeSegmentsEmptyResultTest() {
        PageDto<ResourceDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId1}&segmentId={segmentId2}&withSegmentations={withSegmentations}",
                        YDB_ID, YP_HDD, YP_LOCATION_VLA, YP_SEGMENT_DEFAULT, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getItems().isEmpty());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId1}&segmentId={segmentId2}&withSegmentations={withSegmentations}",
                        YDB_ID, YDB_LOCATION_SAS, YP_LOCATION_VLA, YP_SEGMENT_DEFAULT, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getItems().isEmpty());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId1}&segmentId={segmentId2}&withSegmentations={withSegmentations}",
                        YP_ID, YP_HDD, YDB_LOCATION_SAS, YP_SEGMENT_DEFAULT, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getItems().isEmpty());
    }

    @Test
    public void getResourcesTwoPagesByProviderTypeSegmentsTest() {
        PageDto<ResourceDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId}&limit={limit}",
                        YP_ID, YP_HDD, YP_SEGMENT_DEFAULT, 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());

        PageDto<ResourceDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId}&limit={limit}&pageToken={token}",
                        YP_ID, YP_HDD, YP_SEGMENT_DEFAULT, 1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
    }

    @Test
    public void getResourcesPageByProviderTypeSegmentsInvalidUuidsTest() {
        ErrorCollectionDto firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId}",
                        "invalid", YP_HDD, YP_SEGMENT_DEFAULT)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertFalse(firstResult.getErrors().isEmpty());

        ErrorCollectionDto secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId}",
                        YP_ID, "invalid", YP_SEGMENT_DEFAULT)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getErrors().isEmpty());
    }
}
