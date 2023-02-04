package ru.yandex.intranet.d.web.api.resources.segments;

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
import ru.yandex.intranet.d.web.model.resources.directory.segments.ResourceSegmentCreateDto;
import ru.yandex.intranet.d.web.model.resources.directory.segments.ResourceSegmentDto;
import ru.yandex.intranet.d.web.model.resources.directory.segments.ResourceSegmentPutDto;

/**
 * Resource segments public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ResourceSegmentsApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getResourceSegmentTest() {
        ResourceSegmentDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments/{id}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c",
                        "540b6c0f-d0b9-4f62-9b75-af7c9cfd2e95")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("540b6c0f-d0b9-4f62-9b75-af7c9cfd2e95", result.getId());
    }

    @Test
    public void getResourceSegmentNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments/{id}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c",
                        "12345678-9012-3456-7890-123456789012")
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
    public void getResourceSegmentsPageTest() {
        PageDto<ResourceSegmentDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceSegmentDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getResourceSegmentsTwoPagesTest() {
        PageDto<ResourceSegmentDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments" +
                                "?limit={limit}", "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                        "7fbd778f-d803-44c8-831a-c1de5c05885c", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceSegmentDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<ResourceSegmentDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments" +
                                "?limit={limit}&pageToken={token}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c",
                        1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceSegmentDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }

    @Test
    public void createResourceSegmentTest() {
        ResourceSegmentCreateDto createDto = new ResourceSegmentCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", false);
        ResourceSegmentDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
    }

    @Test
    public void createResourceSegmentEmptyNameTest() {
        ResourceSegmentCreateDto createDto = new ResourceSegmentCreateDto("test", "", "Тест", "Test description",
                "Тестовое описание", false);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c")
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
    public void createResourceSegmentKeyConflictTest() {
        ResourceSegmentCreateDto createDto = new ResourceSegmentCreateDto("sas", "Test", "Тест", "Test description",
                "Тестовое описание", false);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c")
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
    public void updateResourceSegmentTest() {
        ResourceSegmentCreateDto createDto = new ResourceSegmentCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", null);
        ResourceSegmentDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourceSegmentPutDto putDto = new ResourceSegmentPutDto("Test-1", "Тест-1", "Test description-1",
                "Тестовое описание-1", true);
        ResourceSegmentDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments" +
                                "/{id}?version={version}", "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                        "7fbd778f-d803-44c8-831a-c1de5c05885c", createResult.getId(), createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(updateResult);
        Assertions.assertNotNull(updateResult.getId());
        Assertions.assertEquals(createResult.getVersion() + 1, updateResult.getVersion());
    }

    @Test
    public void updateResourceSegmentNotFoundTest() {
        ResourceSegmentCreateDto createDto = new ResourceSegmentCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", false);
        ResourceSegmentDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourceSegmentPutDto putDto = new ResourceSegmentPutDto("Test-1", "Тест-1", "Test description-1",
                "Тестовое описание-1", true);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments" +
                                "/{id}?version={version}", "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                        "7fbd778f-d803-44c8-831a-c1de5c05885c", "12345678-9012-3456-7890-123456789012",
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
    public void updateResourceSegmentVersionMismatchTest() {
        ResourceSegmentCreateDto createDto = new ResourceSegmentCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", null);
        ResourceSegmentDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourceSegmentPutDto putDto = new ResourceSegmentPutDto("Test-1", "Тест-1", "Test description-1",
                "Тестовое описание-1", true);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments" +
                                "/{id}?version={version}", "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                        "7fbd778f-d803-44c8-831a-c1de5c05885c", createResult.getId(), createResult.getVersion() + 1L)
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
    public void updateResourceSegmentEmptyNameTest() {
        ResourceSegmentCreateDto createDto = new ResourceSegmentCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", null);
        ResourceSegmentDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(createResult);
        Assertions.assertNotNull(createResult.getId());
        ResourceSegmentPutDto putDto = new ResourceSegmentPutDto("", "Тест-1", "Test description-1",
                "Тестовое описание-1", true);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments" +
                                "/{id}?version={version}", "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                        "7fbd778f-d803-44c8-831a-c1de5c05885c", createResult.getId(), createResult.getVersion())
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
    public void getResourceSegmentAcceptableForDAdminsTest() {
        ResourceSegmentDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments/{id}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c",
                        "540b6c0f-d0b9-4f62-9b75-af7c9cfd2e95")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("540b6c0f-d0b9-4f62-9b75-af7c9cfd2e95", result.getId());
    }

    @Test
    public void createResourceSegmentAcceptableForDAdminsTest() {
        ResourceSegmentCreateDto createDto = new ResourceSegmentCreateDto("test", "Test", "Тест", "Test description",
                "Тестовое описание", false);
        ResourceSegmentDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/api/v1/providers/{providerId}/resourcesDirectory/segmentations/{segmentationId}/segments",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "7fbd778f-d803-44c8-831a-c1de5c05885c")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResourceSegmentDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
    }

}
