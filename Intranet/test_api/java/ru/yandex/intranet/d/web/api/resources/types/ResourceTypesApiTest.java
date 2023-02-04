package ru.yandex.intranet.d.web.api.resources.types;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestResourceTypes;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.providers.AggregationSettingsInputDto;
import ru.yandex.intranet.d.web.model.providers.FreeProvisionAggregationModeInputDto;
import ru.yandex.intranet.d.web.model.providers.UsageModeInputDto;
import ru.yandex.intranet.d.web.model.resources.directory.types.ResourceTypeCreateDto;
import ru.yandex.intranet.d.web.model.resources.directory.types.ResourceTypeDto;
import ru.yandex.intranet.d.web.model.resources.directory.types.ResourceTypePutDto;

/**
 * Resource types public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ResourceTypesApiTest {

    @Autowired
    private WebTestClient webClient;

    private void getResourceTypeTest(ResourceTypeModel typeModel) {
        ResourceTypeDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes/{id}",
                        typeModel.getProviderId(), typeModel.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceTypeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertEquals(ResourceTypeDto.from(typeModel), result);
    }

    public void createResourceTypeTest(ResourceTypeCreateDto createDto, String providerId) {
        ResourceTypeDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes",
                        providerId)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceTypeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        Assertions.assertEquals(createDto, ResourceTypeCreateDto.from(result));
    }

    public void updateResourceTypeTest(ResourceTypeCreateDto createDto, ResourceTypePutDto putDto, String providerId) {
        ResourceTypeDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes",
                        providerId)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceTypeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        Assertions.assertEquals(createDto, ResourceTypeCreateDto.from(createResult));
        ResourceTypeDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes/{id}?version={version}",
                        providerId, createResult.getId(), createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceTypeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(updateResult);
        Assertions.assertNotNull(updateResult.getId());
        Assertions.assertEquals(createResult.getVersion() + 1, updateResult.getVersion());
        Assertions.assertEquals(putDto, ResourceTypePutDto.from(updateResult));
    }

    @Test
    public void getResourceTypeTest() {
        getResourceTypeTest(TestResourceTypes.YP_CPU_MODEL);
    }

    @Test
    public void getResourceTypeWithNullOrderTest() {
        getResourceTypeTest(TestResourceTypes.YP_HDD_MODEL);
    }

    @Test
    public void getResourceTypeNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes/{id}",
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
    public void getResourceTypesPageTest() {
        PageDto<ResourceTypeDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceTypeDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getResourceTypesTwoPagesTest() {
        PageDto<ResourceTypeDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes?limit={limit}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceTypeDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<ResourceTypeDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes" +
                                "?limit={limit}&pageToken={token}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", 1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceTypeDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }

    @Test
    public void createResourceTypeTest() {
        ResourceTypeCreateDto createDto = new ResourceTypeCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", "b02344bf-96af-4cc5-937c-66a479989ce8", 3L,
                new AggregationSettingsInputDto(FreeProvisionAggregationModeInputDto.NONE, UsageModeInputDto.UNDEFINED,
                        null));
        createResourceTypeTest(createDto, "96e779cf-7d3f-4e74-ba41-c2acc7f04235");
    }

    @Test
    public void createResourceTypeWithNullOrderTest() {
        ResourceTypeCreateDto createDto = new ResourceTypeCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", "b02344bf-96af-4cc5-937c-66a479989ce8", null, null);
        createResourceTypeTest(createDto, "96e779cf-7d3f-4e74-ba41-c2acc7f04235");
    }

    @Test
    public void createResourceTypeEmptyNameTest() {
        ResourceTypeCreateDto createDto = new ResourceTypeCreateDto("test", "", "Тест", "Test description",
                "Тестовое описание", "b02344bf-96af-4cc5-937c-66a479989ce8", 10L, null);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes",
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
    public void createResourceTypeKeyConflictTest() {
        ResourceTypeCreateDto createDto = new ResourceTypeCreateDto("cpu", "Test", "Тест", "Test description",
                "Тестовое описание", "b02344bf-96af-4cc5-937c-66a479989ce8", 10L, null);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes",
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
    public void updateResourceTypeTest() {
        ResourceTypeCreateDto createDto = new ResourceTypeCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", "b02344bf-96af-4cc5-937c-66a479989ce8", null, null);
        ResourceTypePutDto putDto = new ResourceTypePutDto("Test-1", "Тест-1", "Test description-1",
                "Тестовое описание-1", 10L,
                new AggregationSettingsInputDto(FreeProvisionAggregationModeInputDto.NONE, UsageModeInputDto.UNDEFINED,
                        null));
        updateResourceTypeTest(createDto, putDto, "96e779cf-7d3f-4e74-ba41-c2acc7f04235");
    }

    @Test
    public void updateResourceTypeToNullTest() {
        ResourceTypeCreateDto createDto = new ResourceTypeCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", "b02344bf-96af-4cc5-937c-66a479989ce8", null,
                new AggregationSettingsInputDto(FreeProvisionAggregationModeInputDto.NONE, UsageModeInputDto.UNDEFINED,
                        null));
        ResourceTypePutDto putDto = new ResourceTypePutDto("Test-1", "Тест-1", "Test description-1",
                "Тестовое описание-1", null, null);
        updateResourceTypeTest(createDto, putDto, "96e779cf-7d3f-4e74-ba41-c2acc7f04235");
    }

    @Test
    public void updateResourceTypeNotFoundTest() {
        ResourceTypeCreateDto createDto = new ResourceTypeCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", "b02344bf-96af-4cc5-937c-66a479989ce8", 10L, null);
        ResourceTypeDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceTypeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourceTypePutDto putDto = new ResourceTypePutDto("Test-1", "Тест-1", "Test description-1",
                "Тестовое описание-1", null, null);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes/{id}?version={version}",
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
    public void updateResourceTypeVersionMismatchTest() {
        ResourceTypeCreateDto createDto = new ResourceTypeCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", "b02344bf-96af-4cc5-937c-66a479989ce8", 10L, null);
        ResourceTypeDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceTypeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourceTypePutDto putDto = new ResourceTypePutDto("Test-1", "Тест-1", "Test description-1",
                "Тестовое описание-1", 20L, null);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes/{id}?version={version}",
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
    public void updateResourceTypeEmptyNameTest() {
        ResourceTypeCreateDto createDto = new ResourceTypeCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", "b02344bf-96af-4cc5-937c-66a479989ce8", 10L, null);
        ResourceTypeDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceTypeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourceTypePutDto putDto = new ResourceTypePutDto("", "Тест-1", "Test description-1",
                "Тестовое описание-1", null, null);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes/{id}?version={version}",
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
    public void getResourceTypeAcceptableForDAdminsTest() {
        ResourceTypeDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes/{id}",
                        TestResourceTypes.YP_CPU_MODEL.getProviderId(), TestResourceTypes.YP_CPU_MODEL.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceTypeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ResourceTypeDto.from(TestResourceTypes.YP_CPU_MODEL), result);
    }

    @Test
    public void createResourceTypeAcceptableForDAdminsTest() {
        ResourceTypeCreateDto createDto = new ResourceTypeCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", "b02344bf-96af-4cc5-937c-66a479989ce8", null, null);
        ResourceTypeDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceTypeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        Assertions.assertEquals(createDto, ResourceTypeCreateDto.from(result));
    }

    @Test
    public void patchResourceTypeTest() {
        ResourceTypeCreateDto createDto = new ResourceTypeCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", "b02344bf-96af-4cc5-937c-66a479989ce8", null, null);
        Map<String, Object> patchBody = new HashMap<>();
        patchBody.put("nameEn", "test-1-en");
        patchBody.put("nameRu", "test-1-ru");
        patchBody.put("aggregationSettings", null);

        ResourceTypeDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceTypeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        Assertions.assertEquals(createDto, ResourceTypeCreateDto.from(createResult));
        ResourceTypeDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .patch()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/resourceTypes/{id}?version={version}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", createResult.getId(), createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(patchBody)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceTypeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(updateResult);
        Assertions.assertNotNull(updateResult.getId());
        Assertions.assertEquals(createResult.getVersion() + 1, updateResult.getVersion());
        Assertions.assertEquals(patchBody.get("nameEn"), updateResult.getNameEn());
        Assertions.assertEquals(patchBody.get("nameRu"), updateResult.getNameRu());
        Assertions.assertNull(patchBody.get("aggregationSettings"));
        Assertions.assertEquals(createResult.getDescriptionEn(), updateResult.getDescriptionEn());
        Assertions.assertEquals(createResult.getDescriptionRu(), updateResult.getDescriptionRu());
    }

}
