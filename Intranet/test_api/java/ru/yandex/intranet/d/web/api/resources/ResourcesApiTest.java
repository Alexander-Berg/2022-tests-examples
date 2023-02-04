package ru.yandex.intranet.d.web.api.resources;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.resources.InnerResourceSegmentDto;
import ru.yandex.intranet.d.web.model.resources.InnerResourceSegmentationSegmentDto;
import ru.yandex.intranet.d.web.model.resources.ResourceDto;

import static ru.yandex.intranet.d.TestProviders.YDB_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResourceTypes.YP_HDD;
import static ru.yandex.intranet.d.TestSegmentations.YDB_LOCATION_SAS;
import static ru.yandex.intranet.d.TestSegmentations.YP_LOCATION_VLA;
import static ru.yandex.intranet.d.TestSegmentations.YP_SEGMENT_DEFAULT;

/**
 * Resources public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ResourcesApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getResourceTest() {
        ResourceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resources/{id}",
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
    public void getResourceWithSegmentationsTest() {
        ResourceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resources/{id}?withSegmentations={withSegmentations}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "ef333da9-b076-42f5-b7f5-84cd04ab7fcc", true)
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
    public void getResourceNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resources/{id}",
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
                .uri("/api/v1/providers/{providerId}/resources",
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
    public void getResourcesWithSegmentationsPageTest() {
        PageDto<ResourceDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resources?withSegmentations={withSegmentations}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", true)
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
                .uri("/api/v1/providers/{providerId}/resources?limit={limit}",
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
                .uri("/api/v1/providers/{providerId}/resources" +
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
    public void getResourceAcceptableForDAdminsTest() {
        ResourceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resources/{id}",
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
    public void getResourcesPageByProviderTypeAndSegmentsTest() {
        PageDto<ResourceDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId1}&segmentId={segmentId2}&withSegmentations={withSegmentations}",
                        YP_ID, YP_HDD, YP_LOCATION_VLA, YP_SEGMENT_DEFAULT, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() {
                })
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
        Set<String> resultSegmentIds = result.getItems().stream()
                .map(ResourceDto::getSegmentations)
                .map(Optional::orElseThrow)
                .flatMap(i -> i.getSegments().stream())
                .map(InnerResourceSegmentationSegmentDto::getSegment)
                .map(InnerResourceSegmentDto::getId)
                .collect(Collectors.toSet());
        Assertions.assertTrue(result.getItems().stream().allMatch(r -> r.getProviderId().equals(YP_ID)));
        Assertions.assertTrue(result.getItems().stream()
                .allMatch(r -> r.getSegmentations().orElseThrow().getResourceType().getId().equals(YP_HDD)));
        Assertions.assertTrue(resultSegmentIds.stream()
                .allMatch(i -> i.equals(YP_LOCATION_VLA) || i.equals(YP_SEGMENT_DEFAULT)));
    }

    @Test
    public void getResourcesPageByProviderAndTypeTest() {
        PageDto<ResourceDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resources/filterByType/{resourceTypeId}" +
                                "?withSegmentations={withSegmentations}",
                        YP_ID, YP_HDD, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() {
                })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
        Assertions.assertTrue(result.getItems().stream().allMatch(r -> r.getProviderId().equals(YP_ID)));
        Assertions.assertTrue(result.getItems().stream()
                .allMatch(r -> r.getSegmentations().orElseThrow().getResourceType().getId().equals(YP_HDD)));
    }

    @Test
    public void getResourcesPageByProviderTypeSegmentsEmptyResultTest() {
        PageDto<ResourceDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId1}&segmentId={segmentId2}&withSegmentations={withSegmentations}",
                        YDB_ID, YP_HDD, YP_LOCATION_VLA, YP_SEGMENT_DEFAULT, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() {
                })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getItems().isEmpty());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId1}&segmentId={segmentId2}&withSegmentations={withSegmentations}",
                        YDB_ID, YDB_LOCATION_SAS, YP_LOCATION_VLA, YP_SEGMENT_DEFAULT, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() {
                })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getItems().isEmpty());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId1}&segmentId={segmentId2}&withSegmentations={withSegmentations}",
                        YP_ID, YP_HDD, YDB_LOCATION_SAS, YP_SEGMENT_DEFAULT, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() {
                })
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
                .uri("/api/v1/providers/{providerId}/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId}&limit={limit}",
                        YP_ID, YP_HDD, YP_SEGMENT_DEFAULT, 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() {
                })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());

        PageDto<ResourceDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/resources/filterByType/{resourceTypeId}" +
                                "?segmentId={segmentId}&limit={limit}&pageToken={token}",
                        YP_ID, YP_HDD, YP_SEGMENT_DEFAULT, 1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ResourceDto>>() {
                })
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
                .uri("/api/v1/providers/{providerId}/resources/filterByType/{resourceTypeId}" +
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
                .uri("/api/v1/providers/{providerId}/resources/filterByType/{resourceTypeId}" +
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
