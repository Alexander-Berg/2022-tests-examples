package ru.yandex.intranet.d.grpc.providers;

import java.util.Optional;
import java.util.Set;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.GetProviderExternalAccountUrlTemplateRequest;
import ru.yandex.intranet.d.backend.service.proto.GetProviderExternalAccountUrlTemplateResponse;
import ru.yandex.intranet.d.backend.service.proto.GetProviderRelatedResourcesSettingsRequest;
import ru.yandex.intranet.d.backend.service.proto.GetProviderRelatedResourcesSettingsResponse;
import ru.yandex.intranet.d.backend.service.proto.GetProviderRequest;
import ru.yandex.intranet.d.backend.service.proto.ListProvidersRequest;
import ru.yandex.intranet.d.backend.service.proto.ListProvidersResponse;
import ru.yandex.intranet.d.backend.service.proto.MultilingualGrammaticalForms;
import ru.yandex.intranet.d.backend.service.proto.Provider;
import ru.yandex.intranet.d.backend.service.proto.ProviderExternalAccountUrlTemplate;
import ru.yandex.intranet.d.backend.service.proto.ProviderUISettings;
import ru.yandex.intranet.d.backend.service.proto.ProvidersLimit;
import ru.yandex.intranet.d.backend.service.proto.ProvidersPageToken;
import ru.yandex.intranet.d.backend.service.proto.ProvidersServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.RelatedResource;
import ru.yandex.intranet.d.backend.service.proto.RelatedResourcesForResource;
import ru.yandex.intranet.d.backend.service.proto.RelatedResourcesSettings;
import ru.yandex.intranet.d.backend.service.proto.SetProviderExternalAccountUrlTemplateRequest;
import ru.yandex.intranet.d.backend.service.proto.SetProviderExternalAccountUrlTemplateResponse;
import ru.yandex.intranet.d.backend.service.proto.SetProviderReadOnlyRequest;
import ru.yandex.intranet.d.backend.service.proto.SetProviderRelatedResourcesSettingsRequest;
import ru.yandex.intranet.d.backend.service.proto.SetProviderRelatedResourcesSettingsResponse;
import ru.yandex.intranet.d.backend.service.proto.SetProviderUISettingsRequest;
import ru.yandex.intranet.d.backend.service.proto.SetProviderUISettingsResponse;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.utils.ErrorsHelper;

/**
 * Providers GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ProvidersServiceTest {

    @GrpcClient("inProcess")
    private ProvidersServiceGrpc.ProvidersServiceBlockingStub providersService;

    @Test
    public void getProviderTest() {
        GetProviderRequest providerRequest = GetProviderRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        Provider provider = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getProvider(providerRequest);
        Assertions.assertNotNull(provider);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", provider.getProviderId());
    }

    @Test
    public void getProviderNotFoundTest() {
        GetProviderRequest providerRequest = GetProviderRequest.newBuilder()
                .setProviderId("12345678-9012-3456-7890-123456789012")
                .build();
        try {
            providersService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getProvider(providerRequest);
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
    public void setProviderReadOnlyTest() {
        SetProviderReadOnlyRequest providerRequest = SetProviderReadOnlyRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setReadOnly(true)
                .build();
        Provider provider = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .setProviderReadOnly(providerRequest);
        Assertions.assertNotNull(provider);
    }

    @Test
    public void setProviderReadOnlyNotChangedTest() {
        SetProviderReadOnlyRequest providerRequest = SetProviderReadOnlyRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setReadOnly(false)
                .build();
        Provider provider = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .setProviderReadOnly(providerRequest);
        Assertions.assertNotNull(provider);
    }

    @Test
    public void setProviderReadOnlyNotFoundTest() {
        SetProviderReadOnlyRequest providerRequest = SetProviderReadOnlyRequest.newBuilder()
                .setProviderId("12345678-9012-3456-7890-123456789012")
                .setReadOnly(true)
                .build();
        try {
            providersService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .setProviderReadOnly(providerRequest);
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
    public void getProvidersPageTest() {
        ListProvidersRequest providersRequest = ListProvidersRequest.newBuilder()
                .build();
        ListProvidersResponse page = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listProviders(providersRequest);
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getProvidersCount() > 0);
    }

    @Test
    public void getProvidersTwoPagesTest() {
        ListProvidersRequest firstRequest = ListProvidersRequest.newBuilder()
                .setLimit(ProvidersLimit.newBuilder().setLimit(1L).build())
                .build();
        ListProvidersResponse firstPage = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listProviders(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getProvidersCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());
        ListProvidersRequest secondRequest = ListProvidersRequest.newBuilder()
                .setPageToken(ProvidersPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        ListProvidersResponse secondPage = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listProviders(secondRequest);
        Assertions.assertNotNull(secondPage);
        Assertions.assertTrue(secondPage.getProvidersCount() > 0);
    }

    @Test
    public void getProviderRelatedResourcesTest() {
        GetProviderRelatedResourcesSettingsRequest providerRequest = GetProviderRelatedResourcesSettingsRequest
                .newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        GetProviderRelatedResourcesSettingsResponse response = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getProviderRelatedResourcesSettings(providerRequest);
        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.getSettings().getRelatedResourcesByResourceList().isEmpty());
    }

    @Test
    public void getProviderRelatedResourcesNotFoundTest() {
        GetProviderRelatedResourcesSettingsRequest providerRequest = GetProviderRelatedResourcesSettingsRequest
                .newBuilder()
                .setProviderId("12345678-9012-3456-7890-123456789012")
                .build();
        try {
            providersService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getProviderRelatedResourcesSettings(providerRequest);
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
    public void setProviderRelatedResourcesTest() {
        GetProviderRequest providerRequest = GetProviderRequest.newBuilder()
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        Provider provider = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getProvider(providerRequest);
        Assertions.assertNotNull(provider);
        SetProviderRelatedResourcesSettingsRequest request = SetProviderRelatedResourcesSettingsRequest.newBuilder()
                .setSettings(RelatedResourcesSettings.newBuilder()
                        .addRelatedResourcesByResource(RelatedResourcesForResource.newBuilder()
                                .setResourceId("71aa2e62-d26e-4f53-b581-29c7610b300f")
                                .addRelatedResources(RelatedResource.newBuilder()
                                        .setResourceId("f1038280-1eca-4df4-bcac-feee2deb8c79")
                                        .setNumerator(1L)
                                        .setDenominator(1L)
                                        .build())
                                .build())
                        .build())
                .setProviderVersion(provider.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        SetProviderRelatedResourcesSettingsResponse response = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .setProviderRelatedResourcesSettings(request);
        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.getSettings().getRelatedResourcesByResourceList().isEmpty());
        SetProviderRelatedResourcesSettingsRequest nextRequest = SetProviderRelatedResourcesSettingsRequest.newBuilder()
                .setSettings(RelatedResourcesSettings.newBuilder().build())
                .setProviderVersion(response.getProviderVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        SetProviderRelatedResourcesSettingsResponse nextResponse = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .setProviderRelatedResourcesSettings(nextRequest);
        Assertions.assertNotNull(nextResponse);
        Assertions.assertTrue(nextResponse.getSettings().getRelatedResourcesByResourceList().isEmpty());
    }

    @Test
    public void setProviderRelatedResourcesNotFoundTest() {
        SetProviderRelatedResourcesSettingsRequest request = SetProviderRelatedResourcesSettingsRequest.newBuilder()
                .setSettings(RelatedResourcesSettings.newBuilder()
                        .addRelatedResourcesByResource(RelatedResourcesForResource.newBuilder()
                                .setResourceId("71aa2e62-d26e-4f53-b581-29c7610b300f")
                                .addRelatedResources(RelatedResource.newBuilder()
                                        .setResourceId("f1038280-1eca-4df4-bcac-feee2deb8c79")
                                        .setNumerator(1L)
                                        .setDenominator(1L)
                                        .build())
                                .build())
                        .build())
                .setProviderVersion(0L)
                .setProviderId("12345678-9012-3456-7890-123456789012")
                .build();
        try {
            providersService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .setProviderRelatedResourcesSettings(request);
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
    public void setProviderUISettingsTest() {
        Provider provider = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getProvider(GetProviderRequest.newBuilder()
                        .setProviderId(TestProviders.YP_ID)
                        .build());
        Assertions.assertNotNull(provider);

        SetProviderUISettingsResponse response = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .setProviderUISettings(SetProviderUISettingsRequest.newBuilder()
                        .setProviderId(provider.getProviderId())
                        .setProviderVersion(provider.getVersion())
                        .setUiSettings(ProviderUISettings.newBuilder()
                                .setTitleForTheAccount(MultilingualGrammaticalForms.newBuilder()
                                        .setNameSingularRu(MultilingualGrammaticalForms.GrammaticalCases.newBuilder()
                                                .setNominative("Пул")
                                                .build())
                                        .build())
                                .build())
                        .build());
        Assertions.assertNotNull(response);
        Assertions.assertEquals("Пул",
                response.getUiSettings().getTitleForTheAccount().getNameSingularRu().getNominative());

        Provider provider2 = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getProvider(GetProviderRequest.newBuilder()
                        .setProviderId(TestProviders.YP_ID)
                        .build());
        Assertions.assertNotNull(provider2);
        Assertions.assertEquals("Пул",
                provider2.getUiSettings().getTitleForTheAccount().getNameSingularRu().getNominative());

        try {
            System.out.println(JsonFormat.printer().print(provider2.getUiSettings()));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void setProviderExternalAccountUrlTemplate() {
        Provider provider = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getProvider(GetProviderRequest.newBuilder()
                        .setProviderId(TestProviders.YP_ID)
                        .build());
        Assertions.assertNotNull(provider);

        SetProviderExternalAccountUrlTemplateResponse response = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .setProviderExternalAccountUrlTemplate(SetProviderExternalAccountUrlTemplateRequest.newBuilder()
                        .setProviderId(provider.getProviderId())
                        .setProviderVersion(provider.getVersion())
                        .addExternalAccountUrlTemplate(ProviderExternalAccountUrlTemplate.newBuilder()
                                .setDefaultTemplate(true)
                                .setUrlsForSegments(true)
                                .addUrlTemplates(ProviderExternalAccountUrlTemplate.UrlTemplate.newBuilder()
                                        .setName("k")
                                        .setTemplate("v")
                                        .build())
                                .addSegments(ProviderExternalAccountUrlTemplate.Segment.newBuilder()
                                        .setSegmentationKey("segmentations")
                                        .addAllSegmentKey(Set.of("s1", "s2"))
                                        .build()))
                        .build());
        Assertions.assertNotNull(response);
        Assertions.assertEquals(provider.getVersion() + 1, response.getProviderVersion());
        Assertions.assertEquals(1, response.getExternalAccountUrlTemplateCount());
        Assertions.assertTrue(response.getExternalAccountUrlTemplateList().get(0).getDefaultTemplate());
        Assertions.assertTrue(response.getExternalAccountUrlTemplateList().get(0).getUrlsForSegments());
        Assertions.assertEquals(1, response.getExternalAccountUrlTemplateList().get(0).getUrlTemplatesCount());
        Assertions.assertEquals("k",
                response.getExternalAccountUrlTemplateList().get(0).getUrlTemplatesList().get(0).getName());
        Assertions.assertEquals("v",
                response.getExternalAccountUrlTemplateList().get(0).getUrlTemplatesList().get(0).getTemplate());
    }

    @Test
    public void getProviderExternalAccountUrlTemplate() {
        Provider provider = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getProvider(GetProviderRequest.newBuilder()
                        .setProviderId(TestProviders.YP_ID)
                        .build());
        Assertions.assertNotNull(provider);

        providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .setProviderExternalAccountUrlTemplate(SetProviderExternalAccountUrlTemplateRequest.newBuilder()
                        .setProviderId(provider.getProviderId())
                        .setProviderVersion(provider.getVersion())
                        .addExternalAccountUrlTemplate(ProviderExternalAccountUrlTemplate.newBuilder()
                                .setDefaultTemplate(true)
                                .setUrlsForSegments(true)
                                .addUrlTemplates(ProviderExternalAccountUrlTemplate.UrlTemplate.newBuilder()
                                        .setName("k")
                                        .setTemplate("v")
                                        .build())
                                .addSegments(ProviderExternalAccountUrlTemplate.Segment.newBuilder()
                                        .setSegmentationKey("segmentations")
                                        .addAllSegmentKey(Set.of("s1", "s2"))
                                        .build()))
                        .build());

        GetProviderExternalAccountUrlTemplateResponse getResponse = providersService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getProviderExternalAccountUrlTemplate(GetProviderExternalAccountUrlTemplateRequest.newBuilder()
                        .setProviderId(provider.getProviderId())
                        .build());

        Assertions.assertNotNull(getResponse);
        Assertions.assertEquals(provider.getVersion() + 1, getResponse.getProviderVersion());
        Assertions.assertEquals(1, getResponse.getExternalAccountUrlTemplateCount());
        Assertions.assertTrue(getResponse.getExternalAccountUrlTemplateList().get(0).getDefaultTemplate());
        Assertions.assertTrue(getResponse.getExternalAccountUrlTemplateList().get(0).getUrlsForSegments());
        Assertions.assertEquals(1, getResponse.getExternalAccountUrlTemplateList().get(0).getUrlTemplatesCount());
        Assertions.assertEquals("k",
                getResponse.getExternalAccountUrlTemplateList().get(0).getUrlTemplatesList().get(0).getName());
        Assertions.assertEquals("v",
                getResponse.getExternalAccountUrlTemplateList().get(0).getUrlTemplatesList().get(0).getTemplate());
    }
}
