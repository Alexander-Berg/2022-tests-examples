package ru.yandex.intranet.d.web.api.providers;


import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.controllers.api.v1.providers.ApiV1ProvidersController;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.GrammaticalCasesDto;
import ru.yandex.intranet.d.web.model.MultilingualGrammaticalFormsDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.providers.ExternalAccountUrlTemplateDto;
import ru.yandex.intranet.d.web.model.providers.FullProviderDto;
import ru.yandex.intranet.d.web.model.providers.ProviderDto;
import ru.yandex.intranet.d.web.model.providers.ProviderExternalAccountUrlTemplateDto;
import ru.yandex.intranet.d.web.model.providers.ProviderRelatedResourcesSettingsResponseDto;
import ru.yandex.intranet.d.web.model.providers.ProviderUISettingsDto;
import ru.yandex.intranet.d.web.model.providers.PutProviderRelatedResourcesSettingsDto;
import ru.yandex.intranet.d.web.model.providers.PutProviderRelatedResourcesSettingsRequestDto;
import ru.yandex.intranet.d.web.model.providers.PutRelatedResourceDto;
import ru.yandex.intranet.d.web.model.providers.PutRelatedResourcesForResourceDto;

import static ru.yandex.intranet.d.TestUsers.USER_1_UID;

/**
 * Providers public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ProvidersApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getProviderTest() {
        ProviderDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{id}", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", result.getId());
    }

    @Test
    public void getProviderNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{id}", "12345678-9012-3456-7890-123456789012")
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
    public void setProviderReadOnlyTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{id}/_readOnly?readOnly={readOnly}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void setProviderReadOnlyUnchangedTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{id}/_readOnly?readOnly={readOnly}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", false)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void setProviderReadOnlyNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/providers/{id}/_readOnly?readOnly={readOnly}",
                        "12345678-9012-3456-7890-123456789012", true)
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
    public void getProvidersPageTest() {
        PageDto<ProviderDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ProviderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getProvidersTwoPagesTest() {
        PageDto<ProviderDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers?limit={limit}", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ProviderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<ProviderDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers?limit={limit}&pageToken={token}",
                        1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ProviderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }

    @Test
    public void getProvidersPageAcceptableForDAdminsTest() {
        PageDto<ProviderDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/api/v1/providers")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<ProviderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void setProviderReadOnlyAcceptableForDAdminsTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/api/v1/providers/{id}/_readOnly?readOnly={readOnly}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void setProviderReadOnlyAcceptableByProviderAdmin() {
        ErrorCollectionDto errorCollectionDto = webClient
                .mutateWith(MockUser.uid(TestUsers.YDB_ADMIN_UID))
                .post()
                .uri("/api/v1/providers/{id}/_readOnly?readOnly={readOnly}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(errorCollectionDto);
        Assertions.assertFalse(errorCollectionDto.getErrors().isEmpty());

        errorCollectionDto = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/api/v1/providers/{id}/_readOnly?readOnly={readOnly}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(errorCollectionDto);
        Assertions.assertFalse(errorCollectionDto.getErrors().isEmpty());

        webClient
                .mutateWith(MockUser.uid(TestUsers.YP_ADMIN_UID))
                .post()
                .uri("/api/v1/providers/{id}/_readOnly?readOnly={readOnly}",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void providerSyncMustAvailableOnlyForProviderAdmins() {
        ErrorCollectionDto errorCollectionDto = webClient
                .mutateWith(MockUser.uid(TestUsers.YDB_ADMIN_UID))
                .post()
                .uri("/api/v1/providers/{id}/_doSync", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(errorCollectionDto);
        Assertions.assertFalse(errorCollectionDto.getErrors().isEmpty());

        errorCollectionDto = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/api/v1/providers/{id}/_doSync", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(errorCollectionDto);
        Assertions.assertFalse(errorCollectionDto.getErrors().isEmpty());

        errorCollectionDto = webClient
                .mutateWith(MockUser.uid(TestUsers.YP_ADMIN_UID))
                .post()
                .uri("/api/v1/providers/{id}/_doSync", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isEqualTo(422)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(errorCollectionDto);
        Assertions.assertEquals("Provider sync disabled or sync URI not specified.",
                errorCollectionDto.getErrors().iterator().next());
    }

    @Test
    public void getProviderRelatedResourcesTest() {
        ProviderRelatedResourcesSettingsResponseDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{id}/relatedResources", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderRelatedResourcesSettingsResponseDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getSettings().getRelatedResourcesByResource().isEmpty());
    }

    @Test
    public void getProviderRelatedResourcesNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{id}/relatedResources", "12345678-9012-3456-7890-123456789012")
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
    public void setProviderRelatedResourcesTest() {
        ProviderDto provider = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{id}", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(provider);
        PutProviderRelatedResourcesSettingsRequestDto request = new PutProviderRelatedResourcesSettingsRequestDto(
                new PutProviderRelatedResourcesSettingsDto(List.of(new PutRelatedResourcesForResourceDto(
                        "71aa2e62-d26e-4f53-b581-29c7610b300f", List.of(new PutRelatedResourceDto(
                                "f1038280-1eca-4df4-bcac-feee2deb8c79", 1L, 1L))))),
                provider.getVersion()
        );
        ProviderRelatedResourcesSettingsResponseDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{id}/relatedResources", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderRelatedResourcesSettingsResponseDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getSettings().getRelatedResourcesByResource().isEmpty());
        PutProviderRelatedResourcesSettingsRequestDto nextRequest = new PutProviderRelatedResourcesSettingsRequestDto(
                new PutProviderRelatedResourcesSettingsDto(List.of()),
                result.getProviderVersion()
        );
        ProviderRelatedResourcesSettingsResponseDto nextResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{id}/relatedResources", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(nextRequest)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderRelatedResourcesSettingsResponseDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(nextResult);
        Assertions.assertTrue(nextResult.getSettings().getRelatedResourcesByResource().isEmpty());
    }

    @Test
    public void setProviderRelatedResourcesNotFoundTest() {
        PutProviderRelatedResourcesSettingsRequestDto request = new PutProviderRelatedResourcesSettingsRequestDto(
                new PutProviderRelatedResourcesSettingsDto(List.of(new PutRelatedResourcesForResourceDto(
                        "71aa2e62-d26e-4f53-b581-29c7610b300f", List.of(new PutRelatedResourceDto(
                        "f1038280-1eca-4df4-bcac-feee2deb8c79", 1L, 1L))))),
                0L
        );
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{id}/relatedResources", "12345678-9012-3456-7890-123456789012")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
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
    public void setProviderRelatedResourcesWithCycleTest() {
        ProviderDto provider = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{id}", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(provider);
        PutProviderRelatedResourcesSettingsRequestDto request = new PutProviderRelatedResourcesSettingsRequestDto(
                new PutProviderRelatedResourcesSettingsDto(List.of(
                        new PutRelatedResourcesForResourceDto(
                                TestResources.YP_SSD_VLA, List.of(new PutRelatedResourceDto(
                                TestResources.YP_SSD_MAN, 2L, 1L))),
                        new PutRelatedResourcesForResourceDto(
                                TestResources.YP_SSD_MAN, List.of(new PutRelatedResourceDto(
                                TestResources.YP_SSD_VLA, 2L, 1L))))),
                provider.getVersion()
        );
        ProviderRelatedResourcesSettingsResponseDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{id}/relatedResources", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderRelatedResourcesSettingsResponseDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getSettings().getRelatedResourcesByResource().isEmpty());
    }

    @Test
    public void setProviderRelatedResourcesWithLoopErrorTest() {
        ProviderDto provider = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{id}", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(provider);
        PutProviderRelatedResourcesSettingsRequestDto request = new PutProviderRelatedResourcesSettingsRequestDto(
                new PutProviderRelatedResourcesSettingsDto(List.of(
                        new PutRelatedResourcesForResourceDto(
                                TestResources.YP_SSD_VLA, List.of(new PutRelatedResourceDto(
                                TestResources.YP_SSD_VLA, 2L, 1L))))),
                provider.getVersion()
        );
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{id}/relatedResources", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getErrors().contains("Related resources can not have loops."));
    }

    @Test
    public void setProviderUISettingsTest() {
        ProviderDto provider = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{id}", TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(provider);

        ProviderUISettingsDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{id}/uiSettings", TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new ApiV1ProvidersController.SetProviderUISettingsRequestDto(
                        provider.getVersion(), new ProviderUISettingsDto(new MultilingualGrammaticalFormsDto(
                        new GrammaticalCasesDto(
                                "Пул", // nominative
                                null, // genitive
                                null, // dative
                                null, // accusative
                                null, // instrumental
                                null, // prepositional
                                null  //locative
                        ), // nameSingularRu
                        null, // namePluralRu
                        null, // nameSingularEn
                        null  // namePluralEn
                ))))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderUISettingsDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);

        ProviderDto provider2 = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{id}", TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(provider2);
        Assertions.assertTrue(provider2.getUiSettings().isPresent());
        MultilingualGrammaticalFormsDto titleForTheAccount = provider2.getUiSettings().get().getTitleForTheAccount();
        Assertions.assertNotNull(titleForTheAccount);
        GrammaticalCasesDto nameSingularRu = titleForTheAccount.getNameSingularRu();
        Assertions.assertNotNull(nameSingularRu);
        Assertions.assertEquals("Пул", nameSingularRu.getNominative());
    }

    @Test
    public void setProviderExternalAccountUrlTemplateTest() {
        ProviderDto provider = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{id}", TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(provider);

        ProviderExternalAccountUrlTemplateDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/api/v1/providers/{id}/externalAccountUrlTemplate", TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new ProviderExternalAccountUrlTemplateDto(
                        provider.getVersion(), List.of(new ExternalAccountUrlTemplateDto(
                        Map.of("segmentations", Set.of("s1", "s2")), true, Map.of("k", "v"), true))))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderExternalAccountUrlTemplateDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);

        FullProviderDto provider2 = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .get()
                .uri("/admin/providers/{id}", TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(provider2);
        Assertions.assertTrue(provider2.getExternalAccountUrlTemplates().isPresent());
        Assertions.assertEquals(1, provider2.getExternalAccountUrlTemplates().get().size());
        ExternalAccountUrlTemplateDto template = provider2.getExternalAccountUrlTemplates().get().get(0);
        Assertions.assertNotNull(template.getUrlTemplates());
        Assertions.assertNotNull(template.getSegments());
        Assertions.assertNotNull(template.getDefaultTemplate());
        Assertions.assertNotNull(template.getUrlsForSegments());
        Assertions.assertEquals(Map.of("k", "v"), template.getUrlTemplates());
        Assertions.assertEquals(Map.of("segmentations", Set.of("s1", "s2")), template.getSegments());
        Assertions.assertTrue(template.getDefaultTemplate());
        Assertions.assertTrue(template.getUrlsForSegments());
    }

    @Test
    public void getProviderExternalAccountUrlTemplateTest() {
        FullProviderDto provider = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .get()
                .uri("/admin/providers/{id}", TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(provider);

        ProviderExternalAccountUrlTemplateDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{id}/externalAccountUrlTemplate", TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderExternalAccountUrlTemplateDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(provider);
        Assertions.assertTrue(provider.getExternalAccountUrlTemplates().isPresent());
        Assertions.assertEquals(1, provider.getExternalAccountUrlTemplates().get().size());
        Assertions.assertNotNull(result.getExternalAccountUrlTemplate());
        Assertions.assertEquals(1, result.getExternalAccountUrlTemplate().size());
        Assertions.assertEquals(provider.getExternalAccountUrlTemplates().get().get(0),
                result.getExternalAccountUrlTemplate().get(0));
    }
}
