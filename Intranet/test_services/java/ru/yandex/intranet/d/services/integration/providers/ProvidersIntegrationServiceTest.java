package ru.yandex.intranet.d.services.integration.providers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.google.protobuf.Empty;
import com.google.protobuf.util.Timestamps;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.LogCollector;
import ru.yandex.intranet.d.TestGrpcResponses;
import ru.yandex.intranet.d.backend.service.provider_proto.Account;
import ru.yandex.intranet.d.backend.service.provider_proto.AccountsPageToken;
import ru.yandex.intranet.d.backend.service.provider_proto.Amount;
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundResourceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate;
import ru.yandex.intranet.d.backend.service.provider_proto.ListAccountsResponse;
import ru.yandex.intranet.d.backend.service.provider_proto.MoveProvisionResponse;
import ru.yandex.intranet.d.backend.service.provider_proto.PassportUID;
import ru.yandex.intranet.d.backend.service.provider_proto.Provision;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey;
import ru.yandex.intranet.d.backend.service.provider_proto.StaffLogin;
import ru.yandex.intranet.d.backend.service.provider_proto.UserID;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.i18n.Locales;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.services.integration.providers.rest.model.AccountDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.CreateAccountAndProvideRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.CreateAccountRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.DeleteAccountRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.GetAccountRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.KnownAccountProvisionsDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.KnownProvisionDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.ListAccountsByFolderRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.ListAccountsRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.ListAccountsResponseDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.MoveAccountRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.MoveProvisionRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.MoveProvisionResponseDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.ProvisionRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.RenameAccountRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.ResourceKeyRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.SegmentKeyRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.UpdateProvisionRequestDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.UpdateProvisionResponseDto;
import ru.yandex.intranet.d.services.integration.providers.rest.model.UserIdDto;
import ru.yandex.intranet.d.util.result.Result;

/**
 * Providers integration service test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ProvidersIntegrationServiceTest {

    private static final String GRPC_URI = "in-process:test";

    @Autowired
    private ProvidersIntegrationService providersIntegrationService;
    @Autowired
    private StubProviderService stubProviderService;

    @Test
    public void testListAccounts() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        stubProviderService.setListAccountsResponses(List.of(GrpcResponse
                .success(ListAccountsResponse.newBuilder()
                        .setNextPageToken(AccountsPageToken.newBuilder().setToken("test").build())
                        .addAccounts(Account.newBuilder()
                                .setAccountId("test")
                                .setDeleted(false)
                                .setDisplayName("test")
                                .setFolderId(UUID.randomUUID().toString())
                                .setKey("test")
                                .addProvisions(Provision.newBuilder()
                                        .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
                                                        .addAllResourceSegmentKeys(List.of(
                                                                ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())
                                        .setLastUpdate(LastUpdate.newBuilder()
                                                .setAuthor(UserID.newBuilder()
                                                        .setPassportUid(PassportUID.newBuilder()
                                                                .setPassportUid("1")
                                                                .build())
                                                        .setStaffLogin(StaffLogin.newBuilder()
                                                                .setStaffLogin("test")
                                                                .build())
                                                        .build())
                                                .setOperationId(UUID.randomUUID().toString())
                                                .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                                .build())
                                        .setProvided(Amount.newBuilder()
                                                .setValue(1)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .setAllocated(Amount.newBuilder()
                                                .setValue(1)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build())
                        .build())));
        ListAccountsRequestDto request = new ListAccountsRequestDto(100, "test", true, null, null);
        Result<Response<ListAccountsResponseDto>> result = LogCollector.collectLogs(() -> providersIntegrationService
                .listAccounts(provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isSuccess, e -> false).booleanValue());

        final LogCollector.Logs logs = LogCollector.takeLogs();
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
    public void testListAccountsRetryError() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        stubProviderService.setListAccountsResponses(List.of(
                TestGrpcResponses.internalTestResponse(),
                TestGrpcResponses.internalTestResponse(),
                GrpcResponse.success(ru.yandex.intranet.d.backend.service.provider_proto.ListAccountsResponse
                        .newBuilder()
                        .setNextPageToken(AccountsPageToken.newBuilder().setToken("test").build())
                        .addAccounts(Account.newBuilder()
                                .setAccountId("test")
                                .setDeleted(false)
                                .setDisplayName("test")
                                .setFolderId(UUID.randomUUID().toString())
                                .setKey("test")
                                .addProvisions(Provision.newBuilder()
                                        .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
                                                        .addAllResourceSegmentKeys(List.of(
                                                                ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())
                                        .setLastUpdate(LastUpdate.newBuilder()
                                                .setAuthor(UserID.newBuilder()
                                                        .setPassportUid(PassportUID.newBuilder()
                                                                .setPassportUid("1")
                                                                .build())
                                                        .setStaffLogin(StaffLogin.newBuilder()
                                                                .setStaffLogin("test")
                                                                .build())
                                                        .build())
                                                .setOperationId(UUID.randomUUID().toString())
                                                .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                                .build())
                                        .setProvided(Amount.newBuilder()
                                                .setValue(1)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .setAllocated(Amount.newBuilder()
                                                .setValue(1)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build())
                        .build())));
        ListAccountsRequestDto request = new ListAccountsRequestDto(100, "test", true, null, null);
        Result<Response<ListAccountsResponseDto>> result = LogCollector.collectLogs(() -> providersIntegrationService
                .listAccounts(provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isSuccess, e -> false).booleanValue());

        final LogCollector.Logs logs = LogCollector.takeLogs();
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Provider request \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "opId: {}, requestId: {}, request: {}"));
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}, result: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}, error: {}"));
    }

    @Test
    public void testListAccountsRetryErrorFailed() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        stubProviderService.setListAccountsResponses(List.of(
                TestGrpcResponses.internalTestResponse(),
                TestGrpcResponses.internalTestResponse(),
                TestGrpcResponses.internalTestResponse(),
                GrpcResponse.success(ru.yandex.intranet.d.backend.service.provider_proto.ListAccountsResponse
                        .newBuilder()
                        .setNextPageToken(AccountsPageToken.newBuilder().setToken("test").build())
                        .addAccounts(Account.newBuilder()
                                .setAccountId("test")
                                .setDeleted(false)
                                .setDisplayName("test")
                                .setFolderId(UUID.randomUUID().toString())
                                .setKey("test")
                                .addProvisions(Provision.newBuilder()
                                        .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
                                                        .addAllResourceSegmentKeys(List.of(
                                                                ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())
                                        .setLastUpdate(LastUpdate.newBuilder()
                                                .setAuthor(UserID.newBuilder()
                                                        .setPassportUid(PassportUID.newBuilder()
                                                                .setPassportUid("1")
                                                                .build())
                                                        .setStaffLogin(StaffLogin.newBuilder()
                                                                .setStaffLogin("test")
                                                                .build())
                                                        .build())
                                                .setOperationId(UUID.randomUUID().toString())
                                                .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                                .build())
                                        .setProvided(Amount.newBuilder()
                                                .setValue(1)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .setAllocated(Amount.newBuilder()
                                                .setValue(1)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build())
                        .build())));
        ListAccountsRequestDto request = new ListAccountsRequestDto(100, "test", true, null, null);
        Result<Response<ListAccountsResponseDto>> result = LogCollector.collectLogs(() -> providersIntegrationService
                .listAccounts(provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isError, e -> false).booleanValue());

        final LogCollector.Logs logs = LogCollector.takeLogs();
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Provider request \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "opId: {}, requestId: {}, request: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}, result: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}, error: {}"));
    }

    @Test
    public void testListAccountsUnknownError() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        stubProviderService.setListAccountsResponses(List.of(
                TestGrpcResponses.unknownTestResponse(),
                TestGrpcResponses.unknownTestResponse()));
        ListAccountsRequestDto request = new ListAccountsRequestDto(100, "test", true, null, null);
        Result<Response<ListAccountsResponseDto>> result = LogCollector.collectLogs(() -> providersIntegrationService
                .listAccounts(provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isError, e -> false).booleanValue());
        long listAccountsCallCount = stubProviderService.getListAccountsCallCount();
        Assertions.assertEquals(1, listAccountsCallCount);

        final LogCollector.Logs logs = LogCollector.takeLogs();
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Provider request \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "opId: {}, requestId: {}, request: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}, error: {}"));
    }

    @Test
    public void testListAccountsByFolder() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        String folderId = UUID.randomUUID().toString();
        stubProviderService.setListAccountsByFolderResponses(List.of(GrpcResponse
                .success(ru.yandex.intranet.d.backend.service.provider_proto.ListAccountsByFolderResponse.newBuilder()
                        .setNextPageToken(AccountsPageToken.newBuilder().setToken("test").build())
                        .addAccounts(Account.newBuilder()
                                .setAccountId("test")
                                .setDeleted(false)
                                .setDisplayName("test")
                                .setFolderId(folderId)
                                .setKey("test")
                                .addProvisions(Provision.newBuilder()
                                        .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
                                                        .addAllResourceSegmentKeys(List.of(
                                                                ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())
                                        .setLastUpdate(LastUpdate.newBuilder()
                                                .setAuthor(UserID.newBuilder()
                                                        .setPassportUid(PassportUID.newBuilder()
                                                                .setPassportUid("1")
                                                                .build())
                                                        .setStaffLogin(StaffLogin.newBuilder()
                                                                .setStaffLogin("test")
                                                                .build())
                                                        .build())
                                                .setOperationId(UUID.randomUUID().toString())
                                                .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                                .build())
                                        .setProvided(Amount.newBuilder()
                                                .setValue(1)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .setAllocated(Amount.newBuilder()
                                                .setValue(1)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build())
                        .build())));
        ListAccountsByFolderRequestDto request = new ListAccountsByFolderRequestDto(100, "test",
                true, null, "test", 42L, null);
        Result<Response<ListAccountsResponseDto>> result = LogCollector.collectLogs(() -> providersIntegrationService
                .listAccountsByFolder(provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isSuccess, e -> false).booleanValue());

        final LogCollector.Logs logs = LogCollector.takeLogs();
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
    public void testListAccountsByFolderUnknownError() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        stubProviderService.setListAccountsByFolderResponses(List.of(
                TestGrpcResponses.unknownTestResponse(),
                TestGrpcResponses.unknownTestResponse()));
        ListAccountsByFolderRequestDto request = new ListAccountsByFolderRequestDto(100, "test",
                true, null, "test", 42L, null);
        Result<Response<ListAccountsResponseDto>> result = LogCollector.collectLogs(() -> providersIntegrationService
                .listAccountsByFolder(provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isError, e -> false).booleanValue());
        long listAccountsByFolderCallCount = stubProviderService.getListAccountsByFolderCallCount();
        Assertions.assertEquals(1, listAccountsByFolderCallCount);

        final LogCollector.Logs logs = LogCollector.takeLogs();
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Provider request \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "opId: {}, requestId: {}, request: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}, error: {}"));
    }

    @Test
    public void testUpdateProvision() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
                                                        .addAllResourceSegmentKeys(List.of(
                                                                ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                                .setPassportUid("1")
                                                                .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                                .setStaffLogin("test")
                                                                .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                        .build())
                                .build())
                        .build())));
        String accountId = UUID.randomUUID().toString();
        ResourceKeyRequestDto resourceKey = new ResourceKeyRequestDto("test",
                List.of(new SegmentKeyRequestDto("location", "VLA")));
        UpdateProvisionRequestDto request = new UpdateProvisionRequestDto(UUID.randomUUID().toString(), 42L,
                List.of(new ProvisionRequestDto(resourceKey, 1, "gigabytes")),
                List.of(new KnownAccountProvisionsDto(accountId, List.of(new KnownProvisionDto(resourceKey,
                        2, "gigabytes")))),
                new UserIdDto("1", "test"), UUID.randomUUID().toString(), null);

        Result<Response<UpdateProvisionResponseDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.updateProvision(accountId, provider, request, Locales.ENGLISH).block());
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
    public void testUpdateProvisionUnknownError() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        stubProviderService.setListAccountsByFolderResponses(List.of(
                TestGrpcResponses.unknownTestResponse(),
                TestGrpcResponses.unknownTestResponse()));
        String accountId = UUID.randomUUID().toString();
        ResourceKeyRequestDto resourceKey = new ResourceKeyRequestDto("test",
                List.of(new SegmentKeyRequestDto("location", "VLA")));
        UpdateProvisionRequestDto request = new UpdateProvisionRequestDto(UUID.randomUUID().toString(), 42L,
                List.of(new ProvisionRequestDto(resourceKey, 1, "gigabytes")),
                List.of(new KnownAccountProvisionsDto(accountId, List.of(new KnownProvisionDto(resourceKey,
                        2, "gigabytes")))),
                new UserIdDto("1", "test"), UUID.randomUUID().toString(), null);

        Result<Response<UpdateProvisionResponseDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.updateProvision(accountId, provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isError, e -> false).booleanValue());
        long updateProvisionCallCount = stubProviderService.getUpdateProvisionCallCount();
        Assertions.assertEquals(1, updateProvisionCallCount);

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
    public void testGetAccount() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        String folderId = UUID.randomUUID().toString();
        stubProviderService.setGetAccountResponses(List.of(GrpcResponse
                .success(Account.newBuilder()
                        .setAccountId("test")
                        .setDeleted(false)
                        .setDisplayName("test")
                        .setFolderId(folderId)
                        .setKey("test")
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
                                                        .addAllResourceSegmentKeys(List.of(
                                                                ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                                .setPassportUid("1")
                                                                .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                                .setStaffLogin("test")
                                                                .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build())));
        String accountId = UUID.randomUUID().toString();
        GetAccountRequestDto request = new GetAccountRequestDto(true, null, folderId, 42L, null);
        Result<Response<AccountDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.getAccount(accountId, provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isSuccess, e -> false).booleanValue());

        final LogCollector.Logs logs = LogCollector.takeLogs();
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
    public void testGetAccountUnknownError() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        String folderId = UUID.randomUUID().toString();
        stubProviderService.setGetAccountResponses(List.of(
                TestGrpcResponses.unknownTestResponse(),
                TestGrpcResponses.unknownTestResponse()));
        String accountId = UUID.randomUUID().toString();
        GetAccountRequestDto request = new GetAccountRequestDto(true, null, folderId, 42L, null);
        Result<Response<AccountDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.getAccount(accountId, provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isError, e -> false).booleanValue());
        long getAccountCallCount = stubProviderService.getGetAccountCallCount();
        Assertions.assertEquals(1, getAccountCallCount);

        final LogCollector.Logs logs = LogCollector.takeLogs();
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Provider request \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "opId: {}, requestId: {}, request: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}, error: {}"));
    }

    @Test
    public void testCreateAccount() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        String folderId = UUID.randomUUID().toString();
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .success(Account.newBuilder()
                        .setAccountId("test")
                        .setDeleted(false)
                        .setDisplayName("test")
                        .setFolderId(folderId)
                        .setKey("test")
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
                                                        .addAllResourceSegmentKeys(List.of(
                                                                ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                                .setPassportUid("1")
                                                                .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                                .setStaffLogin("test")
                                                                .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build())));
        CreateAccountRequestDto request = new CreateAccountRequestDto("test", "test",
                UUID.randomUUID().toString(), 42L, new UserIdDto("1", "test"),
                UUID.randomUUID().toString(), null, false, "abc");
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
    public void testCreateAccountUnknownError() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        stubProviderService.setCreateAccountResponses(List.of(
                TestGrpcResponses.unknownTestResponse(),
                TestGrpcResponses.unknownTestResponse()
        ));
        CreateAccountRequestDto request = new CreateAccountRequestDto("test", "test",
                UUID.randomUUID().toString(), 42L, new UserIdDto("1", "test"),
                UUID.randomUUID().toString(), null, false, "abc");
        Result<Response<AccountDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.createAccount(provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isError, e -> false).booleanValue());
        long createAccountCallCount = stubProviderService.getCreateAccountCallCount();
        Assertions.assertEquals(1, createAccountCallCount);

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
    public void testCreateAccountAfterRetryableError() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        String folderId = UUID.randomUUID().toString();
        stubProviderService.setCreateAccountResponses(List.of(
                TestGrpcResponses.internalTestResponse(),
                GrpcResponse.success(Account.newBuilder()
                        .setAccountId("test")
                        .setDeleted(false)
                        .setDisplayName("test")
                        .setFolderId(folderId)
                        .setKey("test")
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("test")
                                                .addAllResourceSegmentKeys(List.of(
                                                        ResourceSegmentKey.newBuilder()
                                                                .setResourceSegmentationKey("location")
                                                                .setResourceSegmentKey("VLA")
                                                                .build()
                                                ))
                                                .build())
                                        .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                        .setPassportUid("1")
                                                        .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                        .setStaffLogin("test")
                                                        .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build())));
        CreateAccountRequestDto request = new CreateAccountRequestDto("test", "test",
                UUID.randomUUID().toString(), 42L, new UserIdDto("1", "test"),
                UUID.randomUUID().toString(), null, false, "abc");
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
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}, error: {}"));
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}, result: {}"));
    }

    @Test
    public void testCreateAccountAndProvide() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        String folderId = UUID.randomUUID().toString();
        stubProviderService.setCreateAccountAndProvideResponses(List.of(GrpcResponse
                .success(Account.newBuilder()
                        .setAccountId("test")
                        .setDeleted(false)
                        .setDisplayName("test")
                        .setFolderId(folderId)
                        .setKey("test")
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
                                                        .addAllResourceSegmentKeys(List.of(
                                                                ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                                .setPassportUid("1")
                                                                .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                                .setStaffLogin("test")
                                                                .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build())));
        ResourceKeyRequestDto resourceKey = new ResourceKeyRequestDto("test",
                List.of(new SegmentKeyRequestDto("location", "VLA")));
        CreateAccountAndProvideRequestDto request = new CreateAccountAndProvideRequestDto("test", "test",
                UUID.randomUUID().toString(), 42L,
                List.of(new ProvisionRequestDto(resourceKey, 1, "gigabytes")),
                List.of(new KnownAccountProvisionsDto("test", List.of(new KnownProvisionDto(resourceKey,
                        2, "gigabytes")))),
                new UserIdDto("1", "test"), UUID.randomUUID().toString(), null, null, "abc");
        Result<Response<AccountDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.createAccountAndProvide(provider, request, Locales.ENGLISH).block());
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
    public void testCreateAccountAndProvideUnknownError() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        stubProviderService.setCreateAccountAndProvideResponses(List.of(
                TestGrpcResponses.unknownTestResponse(),
                TestGrpcResponses.unknownTestResponse()
        ));
        ResourceKeyRequestDto resourceKey = new ResourceKeyRequestDto("test",
                List.of(new SegmentKeyRequestDto("location", "VLA")));
        CreateAccountAndProvideRequestDto request = new CreateAccountAndProvideRequestDto("test", "test",
                UUID.randomUUID().toString(), 42L,
                List.of(new ProvisionRequestDto(resourceKey, 1, "gigabytes")),
                List.of(new KnownAccountProvisionsDto("test", List.of(new KnownProvisionDto(resourceKey,
                        2, "gigabytes")))),
                new UserIdDto("1", "test"), UUID.randomUUID().toString(), null, null, "abc");
        Result<Response<AccountDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.createAccountAndProvide(provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isError, e -> false).booleanValue());
        long createAccountAndProvideCallCount = stubProviderService.getCreateAccountAndProvideCallCount();
        Assertions.assertEquals(1, createAccountAndProvideCallCount);

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
    public void testDeleteAccount() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        stubProviderService.setDeleteAccountResponses(List.of(GrpcResponse
                .success(Empty.newBuilder().build())));
        DeleteAccountRequestDto request = new DeleteAccountRequestDto(UUID.randomUUID().toString(), 42L,
                new UserIdDto("1", "test"), UUID.randomUUID().toString(), null);
        Result<Response<Void>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.deleteAccount("test", provider, request, Locales.ENGLISH)
                .block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isSuccess, e -> false).booleanValue());

        final LogCollector.Logs logs = LogCollector.takeLogs();
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
    public void testDeleteAccountUnknownError() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        stubProviderService.setDeleteAccountResponses(List.of(
                TestGrpcResponses.unknownTestResponse(),
                TestGrpcResponses.unknownTestResponse()
        ));
        DeleteAccountRequestDto request = new DeleteAccountRequestDto(UUID.randomUUID().toString(), 42L,
                new UserIdDto("1", "test"), UUID.randomUUID().toString(), null);
        Result<Response<Void>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.deleteAccount("test", provider, request, Locales.ENGLISH)
                .block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isError, e -> false).booleanValue());
        long deleteAccountCallCount = stubProviderService.getDeleteAccountCallCount();
        Assertions.assertEquals(1, deleteAccountCallCount);

        final LogCollector.Logs logs = LogCollector.takeLogs();
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Provider request \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "opId: {}, requestId: {}, request: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}, error: {}"));
    }

    @Test
    public void testRenameAccount() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        String folderId = UUID.randomUUID().toString();
        stubProviderService.setRenameAccountResponses(List.of(GrpcResponse
                .success(Account.newBuilder()
                        .setAccountId("test")
                        .setDeleted(false)
                        .setDisplayName("test")
                        .setFolderId(folderId)
                        .setKey("test")
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
                                                        .addAllResourceSegmentKeys(List.of(
                                                                ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                                .setPassportUid("1")
                                                                .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                                .setStaffLogin("test")
                                                                .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build())));
        RenameAccountRequestDto request = new RenameAccountRequestDto(UUID.randomUUID().toString(), 42L,
                "test update", new UserIdDto("1", "test"), UUID.randomUUID().toString(), null);
        Result<Response<AccountDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.renameAccount("test", provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isSuccess, e -> false).booleanValue());

        final LogCollector.Logs logs = LogCollector.takeLogs();
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
    public void testRenameAccountUnknownError() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        stubProviderService.setRenameAccountResponses(List.of(
                TestGrpcResponses.unknownTestResponse(),
                TestGrpcResponses.unknownTestResponse()
        ));
        RenameAccountRequestDto request = new RenameAccountRequestDto(UUID.randomUUID().toString(), 42L,
                "test update", new UserIdDto("1", "test"), UUID.randomUUID().toString(), null);
        Result<Response<AccountDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.renameAccount("test", provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isError, e -> false).booleanValue());
        long renameAccountCallCount = stubProviderService.getRenameAccountCallCount();
        Assertions.assertEquals(1, renameAccountCallCount);

        final LogCollector.Logs logs = LogCollector.takeLogs();
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Provider request \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "opId: {}, requestId: {}, request: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}, error: {}"));
    }

    @Test
    public void testMoveAccount() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        String folderId = UUID.randomUUID().toString();
        stubProviderService.setMoveAccountResponses(List.of(GrpcResponse
                .success(Account.newBuilder()
                        .setAccountId("test")
                        .setDeleted(false)
                        .setDisplayName("test")
                        .setFolderId(folderId)
                        .setKey("test")
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
                                                        .addAllResourceSegmentKeys(List.of(
                                                                ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                                .setPassportUid("1")
                                                                .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                                .setStaffLogin("test")
                                                                .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build())));
        ResourceKeyRequestDto resourceKey = new ResourceKeyRequestDto("test",
                List.of(new SegmentKeyRequestDto("location", "VLA")));
        MoveAccountRequestDto request = new MoveAccountRequestDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), 42L, 69L,
                List.of(new KnownAccountProvisionsDto("test", List.of(new KnownProvisionDto(resourceKey,
                        2, "gigabytes")))),
                List.of(new KnownAccountProvisionsDto("test", List.of(new KnownProvisionDto(resourceKey,
                        2, "gigabytes")))),
                new UserIdDto("1", "test"), UUID.randomUUID().toString(), null);
        Result<Response<AccountDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.moveAccount("test", provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isSuccess, e -> false).booleanValue());

        final LogCollector.Logs logs = LogCollector.takeLogs();
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
    public void testMoveAccountUnknownError() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        String folderId = UUID.randomUUID().toString();
        stubProviderService.setMoveAccountResponses(List.of(
                TestGrpcResponses.unknownTestResponse(),
                TestGrpcResponses.unknownTestResponse()
        ));
        ResourceKeyRequestDto resourceKey = new ResourceKeyRequestDto("test",
                List.of(new SegmentKeyRequestDto("location", "VLA")));
        MoveAccountRequestDto request = new MoveAccountRequestDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), 42L, 69L,
                List.of(new KnownAccountProvisionsDto("test", List.of(new KnownProvisionDto(resourceKey,
                        2, "gigabytes")))),
                List.of(new KnownAccountProvisionsDto("test", List.of(new KnownProvisionDto(resourceKey,
                        2, "gigabytes")))),
                new UserIdDto("1", "test"), UUID.randomUUID().toString(), null);
        Result<Response<AccountDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.moveAccount("test", provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isError, e -> false).booleanValue());
        long moveAccountCallCount = stubProviderService.getMoveAccountCallCount();
        Assertions.assertEquals(1, moveAccountCallCount);

        final LogCollector.Logs logs = LogCollector.takeLogs();
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Provider request \"{}\", opId: {}, requestId: {}"));
        Assertions.assertFalse(logs.contains(Level.INFO,
                "opId: {}, requestId: {}, request: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.ERROR,
                "Failure on provider \"{}\", opId: {}, requestId: {}, error: {}"));
    }

    @Test
    public void testMoveProvision() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        String folderId = UUID.randomUUID().toString();
        stubProviderService.setMoveProvisionResponses(List.of(GrpcResponse
                .success(MoveProvisionResponse.newBuilder()
                        .addSourceProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
                                                        .addAllResourceSegmentKeys(List.of(
                                                                ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                                .setPassportUid("1")
                                                                .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                                .setStaffLogin("test")
                                                                .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .addDestinationProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
                                                        .addAllResourceSegmentKeys(List.of(
                                                                ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                                .setPassportUid("1")
                                                                .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                                .setStaffLogin("test")
                                                                .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build())));
        ResourceKeyRequestDto resourceKey = new ResourceKeyRequestDto("test",
                List.of(new SegmentKeyRequestDto("location", "VLA")));
        MoveProvisionRequestDto request = new MoveProvisionRequestDto("destionation",
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), 42L, 69L,
                List.of(new KnownAccountProvisionsDto("test", List.of(new KnownProvisionDto(resourceKey,
                        2, "gigabytes")))),
                List.of(new KnownAccountProvisionsDto("test", List.of(new KnownProvisionDto(resourceKey,
                        2, "gigabytes")))),
                List.of(new ProvisionRequestDto(resourceKey, 1, "gigabytes")),
                List.of(new ProvisionRequestDto(resourceKey, 1, "gigabytes")),
                new UserIdDto("1", "test"), UUID.randomUUID().toString(), null);
        Result<Response<MoveProvisionResponseDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.moveProvision("test", provider, request, Locales.ENGLISH).block());
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
    public void testMoveProvisionUnknownError() {
        ProviderModel provider = providerModel(GRPC_URI, null);
        stubProviderService.setMoveProvisionResponses(List.of(
                TestGrpcResponses.unknownTestResponse(),
                TestGrpcResponses.unknownTestResponse()
        ));
        ResourceKeyRequestDto resourceKey = new ResourceKeyRequestDto("test",
                List.of(new SegmentKeyRequestDto("location", "VLA")));
        MoveProvisionRequestDto request = new MoveProvisionRequestDto("destionation",
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), 42L, 69L,
                List.of(new KnownAccountProvisionsDto("test", List.of(new KnownProvisionDto(resourceKey,
                        2, "gigabytes")))),
                List.of(new KnownAccountProvisionsDto("test", List.of(new KnownProvisionDto(resourceKey,
                        2, "gigabytes")))),
                List.of(new ProvisionRequestDto(resourceKey, 1, "gigabytes")),
                List.of(new ProvisionRequestDto(resourceKey, 1, "gigabytes")),
                new UserIdDto("1", "test"), UUID.randomUUID().toString(), null);
        Result<Response<MoveProvisionResponseDto>> result = LogCollector.collectLogs(() ->
                providersIntegrationService.moveProvision("test", provider, request, Locales.ENGLISH).block());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.match(Response::isError, e -> false).booleanValue());
        long moveProvisionCallCount = stubProviderService.getMoveProvisionCallCount();
        Assertions.assertEquals(1, moveProvisionCallCount);

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

    private ProviderModel providerModel(String grpcUri, String restUri) {
        return ProviderModel.builder()
                .id(UUID.randomUUID().toString())
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
