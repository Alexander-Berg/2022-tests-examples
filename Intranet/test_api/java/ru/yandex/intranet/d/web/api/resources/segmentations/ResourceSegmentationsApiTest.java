package ru.yandex.intranet.d.web.api.resources.segmentations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.resources.directory.segmentations.ResourceSegmentationCreateDto;
import ru.yandex.intranet.d.web.model.resources.directory.segmentations.ResourceSegmentationDto;
import ru.yandex.intranet.d.web.model.resources.directory.segmentations.ResourceSegmentationPutDto;

/**
 * Resource segmentations public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ResourceSegmentationsApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getResourceSegmentationTest() {
        ResourceSegmentationDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{id}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("7fbd778f-d803-44c8-831a-c1de5c05885c", result.getId());
    }

    @Test
    public void getResourceSegmentationNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{id}",
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
    public void getResourceSegmentationsPageTest() {
        PageDto<ResourceSegmentationDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceSegmentationDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getResourceSegmentationsTwoPagesTest() {
        PageDto<ResourceSegmentationDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations?limit={limit}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceSegmentationDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<ResourceSegmentationDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations" +
                                "?limit={limit}&pageToken={token}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", 1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceSegmentationDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }

    @Test
    public void createResourceSegmentationTest() {
        ResourceSegmentationCreateDto createDto = new ResourceSegmentationCreateDto("test", "Test",
                "Тест", "Test description", "Тестовое описание", null, null);
        ResourceSegmentationDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
    }

    @Test
    public void createResourceSegmentationEmptyNameTest() {
        ResourceSegmentationCreateDto createDto = new ResourceSegmentationCreateDto("test", "",
                "Тест", "Test description", "Тестовое описание", null, null);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations",
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
    public void createResourceSegmentationKeyConflictTest() {
        ResourceSegmentationCreateDto createDto = new ResourceSegmentationCreateDto("location", "Test",
                "Тест", "Test description", "Тестовое описание", null, null);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations",
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
    public void updateResourceSegmentationTest() {
        ResourceSegmentationCreateDto createDto = new ResourceSegmentationCreateDto("test", "Test",
                "Тест", "Test description", "Тестовое описание", null, null);
        ResourceSegmentationDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourceSegmentationPutDto putDto = new ResourceSegmentationPutDto("Test-1", "Тест-1",
                "Test description-1", "Тестовое описание-1", null, null);
        ResourceSegmentationDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{id}?version={version}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", createResult.getId(), createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(updateResult);
        Assertions.assertNotNull(updateResult.getId());
        Assertions.assertEquals(createResult.getVersion() + 1, updateResult.getVersion());
    }

    @Test
    public void updateResourceSegmentationNotFoundTest() {
        ResourceSegmentationCreateDto createDto = new ResourceSegmentationCreateDto("test", "Test",
                "Тест", "Test description", "Тестовое описание", null, null);
        ResourceSegmentationDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourceSegmentationPutDto putDto = new ResourceSegmentationPutDto("Test-1", "Тест-1",
                "Test description-1", "Тестовое описание-1", null, null);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{id}?version={version}",
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
    public void updateResourceSegmentationVersionMismatchTest() {
        ResourceSegmentationCreateDto createDto = new ResourceSegmentationCreateDto("test", "Test",
                "Тест", "Test description", "Тестовое описание", null, null);
        ResourceSegmentationDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourceSegmentationPutDto putDto = new ResourceSegmentationPutDto("Test-1", "Тест-1",
                "Test description-1", "Тестовое описание-1", null, null);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{id}?version={version}",
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
    public void updateResourceSegmentationEmptyNameTest() {
        ResourceSegmentationCreateDto createDto = new ResourceSegmentationCreateDto("test", "Test",
                "Тест", "Test description", "Тестовое описание", null, null);
        ResourceSegmentationDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourceSegmentationPutDto putDto = new ResourceSegmentationPutDto("", "Тест-1",
                "Test description-1", "Тестовое описание-1", null, null);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{id}?version={version}",
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
    public void getResourceSegmentationAcceptableForDAdminsTest() {
        ResourceSegmentationDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{id}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("7fbd778f-d803-44c8-831a-c1de5c05885c", result.getId());
    }

    @Test
    public void createResourceSegmentationAcceptableForDAdminsTest() {
        ResourceSegmentationCreateDto createDto = new ResourceSegmentationCreateDto("test", "Test",
                "Тест", "Test description", "Тестовое описание", null, null);
        ResourceSegmentationDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
    }

}
