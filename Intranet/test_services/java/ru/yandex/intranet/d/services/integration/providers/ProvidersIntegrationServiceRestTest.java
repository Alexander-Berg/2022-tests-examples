package ru.yandex.intranet.d.services.integration.providers;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.protobuf.util.Timestamps;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.mockserver.socket.PortFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.LogCollector;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.i18n.Locales;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.services.integration.providers.rest.model.AccountDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.AccountsSpaceKeyRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.AccountsSpaceKeyResponseDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.CreateAccountRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.KnownAccountProvisionsDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.KnownProvisionDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.LastUpdateDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.ListAccountsByFolderRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.ListAccountsRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.ListAccountsResponseDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.ProvisionDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.ProvisionRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.ResourceKeyRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.ResourceKeyResponseDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.SegmentKeyRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.SegmentKeyResponseDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.UpdateProvisionRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.UpdateProvisionResponseDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.UserIdDto;
import ru.yandex.intranet.d.util.result.Result;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

/**
 * Providers integration service test for REST API.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ProvidersIntegrationServiceRestTest {

    @Autowired
    private ProvidersIntegrationService providersIntegrationService;

    private ClientAndServer mockServer;

    @BeforeEach
    public void startServer() {
        mockServer = startClientAndServer(PortFactory.findFreePort());
    }

    @AfterEach
    public void stopServer() {
        mockServer.stop();
    }

    @Test
    public void testUpdateProvisionRetryErrorFailed() {
        String accountId = UUID.randomUUID().toString();
        ResourceKeyRequestDto resourceKey = new ResourceKeyRequestDto("test",
                List.of(new SegmentKeyRequestDto("location", "VLA")));
        UpdateProvisionRequestDto request = new UpdateProvisionRequestDto(UUID.randomUUID().toString(), 42L,
                List.of(new ProvisionRequestDto(resourceKey, 1, "gigabytes")),
                List.of(new KnownAccountProvisionsDto(accountId, List.of(new KnownProvisionDto(resourceKey,
                        2, "gigabytes")))),
                new UserIdDto("1", "test"), UUID.randomUUID().toString(), null);
        String providerId = UUID.randomUUID().toString();
        String host = mockServer.remoteAddress().getHostString();
        int port = mockServer.remoteAddress().getPort();
        ProviderModel provider = providerModel(providerId, null, "http://" + host + ":" + port);

        HttpRequest httpRequest = HttpRequest.request()
                .withMethod("POST")
                .withPath("/quotaManagement/v1/providers/" + providerId + "/accounts/" + accountId + "/_provide")
                .withHeader("X-Ya-Service-Ticket", "test-tvm-ticket")
                .withHeader("Accept", MediaType.APPLICATION_JSON.toString());
        mockServer.when(httpRequest, Times.unlimited())
                .respond(HttpResponse.response()
                .withStatusCode(504)
                .withContentType(MediaType.APPLICATION_JSON));
        Result<Response<UpdateProvisionResponseDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.updateProvision(accountId, provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isError, e -> false).booleanValue());

        Set<String> ids = Arrays.stream(mockServer.retrieveRecordedRequests(httpRequest))
                .map(req -> ((HttpRequest) req).getHeader("X-Request-ID"))
                .map(headerIds -> {
                    Assertions.assertEquals(1, headerIds.size());
                    return headerIds.get(0);
                })
                .collect(Collectors.toSet());
        Assertions.assertTrue(ids.size() > 1);

        LogCollector.Logs logs = LogCollector.takeLogs();
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Provider request \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.INFO,
                "opId: {}, requestId: {}, request: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}, error: {}"));
    }

    @Test
    public void testCreateAccount() throws JsonProcessingException {
        CreateAccountRequestDto request = new CreateAccountRequestDto("test", "test",
                UUID.randomUUID().toString(), 42L, new UserIdDto("1", "test"),
                UUID.randomUUID().toString(), null, false, "abc");
        String providerId = UUID.randomUUID().toString();
        String folderId = UUID.randomUUID().toString();
        String host = mockServer.remoteAddress().getHostString();
        int port = mockServer.remoteAddress().getPort();
        ProviderModel provider = providerModel(providerId, null, "http://" + host + ":" + port);

        AccountDto expectedResponse = new AccountDto("test",
                "test",
                "test",
                folderId,
                false,
                List.of(new ProvisionDto(
                        new ResourceKeyResponseDto("test", List.of(new SegmentKeyResponseDto("location", "VLA"))),
                        1L, "gigabytes",
                        1L, "gigabytes",
                        new LastUpdateDto(Timestamps.fromMillis(Instant.now().toEpochMilli()).getSeconds(),
                                new UserIdDto("1", "test"),
                                UUID.randomUUID().toString()),
                        1L
                )),
                1L,
                null,
                new LastUpdateDto(Timestamps.fromMillis(Instant.now().toEpochMilli()).getSeconds(),
                        new UserIdDto("1", "test"),
                UUID.randomUUID().toString()),
                false);
        mockServer.when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath("/quotaManagement/v1/providers/" + providerId + "/accounts")
                        .withHeader("X-Ya-Service-Ticket", "test-tvm-ticket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(AccountDto.class)
                        .writeValueAsString(expectedResponse)));
        Result<Response<AccountDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.createAccount(provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isSuccess, e -> false).booleanValue());

        LogCollector.Logs logs = LogCollector.takeLogs();
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Provider request \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.INFO,
                "opId: {}, requestId: {}, request: {}"));
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}, result: {}"));
    }

    @Test
    public void testListAccounts() throws JsonProcessingException {
        String providerId = UUID.randomUUID().toString();
        ResourceKeyResponseDto resourceKey = new ResourceKeyResponseDto("test",
                List.of(new SegmentKeyResponseDto("location", "VLA")));
        LastUpdateDto lastUpdate = new LastUpdateDto(Instant.now().toEpochMilli(), new UserIdDto("1", "test"),
                UUID.randomUUID().toString());
        AccountsSpaceKeyResponseDto accountSpaceKey = new AccountsSpaceKeyResponseDto(List
                .of(new SegmentKeyResponseDto("type", "compute")));
        ListAccountsResponseDto expectedResponse = new ListAccountsResponseDto(List.of(new AccountDto("test", "test",
        "Test", UUID.randomUUID().toString(), false, List.of(new ProvisionDto(resourceKey,
                1L, "gigabyte", 1L, "gigabyte",
                lastUpdate, 1L)), 1L, accountSpaceKey, lastUpdate, false)), "test");
        mockServer.when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath("/quotaManagement/v1/providers/" + providerId + "/accounts/_getPage")
                        .withHeader("X-Ya-Service-Ticket", "test-tvm-ticket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(ListAccountsResponseDto.class)
                        .writeValueAsString(expectedResponse)));
        String host = mockServer.remoteAddress().getHostString();
        int port = mockServer.remoteAddress().getPort();
        ProviderModel provider = providerModel(providerId, null, "http://" + host + ":" + port);
        AccountsSpaceKeyRequestDto accountSpaceKeyRequest = new AccountsSpaceKeyRequestDto(List
                .of(new SegmentKeyRequestDto("type", "compute")));
        ListAccountsRequestDto request = new ListAccountsRequestDto(100, "test", true, null, accountSpaceKeyRequest);
        Result<Response<ListAccountsResponseDto>> result = LogCollector.collectLogs(() -> providersIntegrationService
                .listAccounts(provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isSuccess, e -> false).booleanValue());

        LogCollector.Logs logs = LogCollector.takeLogs();
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Provider request \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "opId: {}, requestId: {}, request: {}"));
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}, result: {}"));
    }

    @Test
    public void testListAccountsByFolder() throws JsonProcessingException {
        String providerId = UUID.randomUUID().toString();
        String folderId = UUID.randomUUID().toString();
        long abcServiceId = 42L;
        ResourceKeyResponseDto resourceKey = new ResourceKeyResponseDto("test",
                List.of(new SegmentKeyResponseDto("location", "VLA")));
        LastUpdateDto lastUpdate = new LastUpdateDto(Instant.now().toEpochMilli(), new UserIdDto("1", "test"),
                UUID.randomUUID().toString());
        AccountsSpaceKeyResponseDto accountSpaceKey = new AccountsSpaceKeyResponseDto(List
                .of(new SegmentKeyResponseDto("type", "compute")));
        ListAccountsResponseDto expectedResponse = new ListAccountsResponseDto(List.of(new AccountDto("test", "test",
                "Test", UUID.randomUUID().toString(), false, List.of(new ProvisionDto(resourceKey,
                1L, "gigabyte", 1L, "gigabyte",
                lastUpdate, 1L)), 1L, accountSpaceKey, lastUpdate, false)), "test");
        mockServer.when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath("/quotaManagement/v1/providers/" + providerId + "/accounts/_getPage")
                        .withHeader("X-Ya-Service-Ticket", "test-tvm-ticket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(ListAccountsResponseDto.class)
                        .writeValueAsString(expectedResponse)));
        String host = mockServer.remoteAddress().getHostString();
        int port = mockServer.remoteAddress().getPort();
        ProviderModel provider = providerModel(providerId, null, "http://" + host + ":" + port);
        AccountsSpaceKeyRequestDto accountSpaceKeyRequest = new AccountsSpaceKeyRequestDto(List
                .of(new SegmentKeyRequestDto("type", "compute")));
        ListAccountsByFolderRequestDto request = new ListAccountsByFolderRequestDto(100, "test",
                true, null, folderId, abcServiceId, accountSpaceKeyRequest);
        Result<Response<ListAccountsResponseDto>> result = LogCollector.collectLogs(() -> providersIntegrationService
                .listAccountsByFolder(provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isSuccess, e -> false).booleanValue());

        LogCollector.Logs logs = LogCollector.takeLogs();
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Provider request \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "opId: {}, requestId: {}, request: {}"));
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}, result: {}"));
    }

    private ProviderModel providerModel(String providerId, String grpcUri, String restUri) {
        return ProviderModel.builder()
                .id(providerId)
                .grpcApiUri(grpcUri)
                .restApiUri(restUri)
                .destinationTvmId(42L)
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .sourceTvmId(42L)
                .serviceId(69L)
                .deleted(false)
                .readOnly(false)
                .multipleAccountsPerFolder(true)
                .accountTransferWithQuota(true)
                .managed(true)
                .key("test")
                .trackerComponentId(1L)
                .accountsSettings(AccountsSettingsModel.builder()
                        .displayNameSupported(true)
                        .keySupported(true)
                        .deleteSupported(true)
                        .softDeleteSupported(true)
                        .moveSupported(true)
                        .renameSupported(true)
                        .perAccountVersionSupported(true)
                        .perProvisionVersionSupported(true)
                        .perAccountLastUpdateSupported(true)
                        .perProvisionLastUpdateSupported(true)
                        .operationIdDeduplicationSupported(true)
                        .syncCoolDownDisabled(false)
                        .retryCoolDownDisabled(false)
                        .accountsSyncPageSize(1000L)
                        .build())
                .importAllowed(true)
                .accountsSpacesSupported(true)
                .syncEnabled(true)
                .grpcTlsOn(true)
                .build();
    }

}
