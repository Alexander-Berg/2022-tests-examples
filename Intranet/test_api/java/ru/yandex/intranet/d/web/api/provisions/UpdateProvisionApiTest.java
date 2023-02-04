package ru.yandex.intranet.d.web.api.provisions;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.protobuf.util.Timestamps;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple2;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.provider_proto.Account;
import ru.yandex.intranet.d.backend.service.provider_proto.AccountsSpaceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.Amount;
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundAccountsSpaceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundResourceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate;
import ru.yandex.intranet.d.backend.service.provider_proto.PassportUID;
import ru.yandex.intranet.d.backend.service.provider_proto.Provision;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey;
import ru.yandex.intranet.d.backend.service.provider_proto.StaffLogin;
import ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse;
import ru.yandex.intranet.d.backend.service.provider_proto.UserID;
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.operations.OperationDto;
import ru.yandex.intranet.d.web.model.operations.OperationStatusDto;
import ru.yandex.intranet.d.web.model.provisions.AccountProvisionDto;
import ru.yandex.intranet.d.web.model.provisions.UpdateProvisionDto;
import ru.yandex.intranet.d.web.model.provisions.UpdateProvisionOperationDto;
import ru.yandex.intranet.d.web.model.provisions.UpdateProvisionOperationStatusDto;
import ru.yandex.intranet.d.web.model.provisions.UpdateProvisionsDto;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_3;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YP_CPU_MAN;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_RAM_MAN;
import static ru.yandex.intranet.d.TestResources.YP_SSD_MAN;

/**
 * Update provisions public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class UpdateProvisionApiTest {

    @Autowired
    private WebTestClient webClient;
    @Autowired
    private StubProviderService stubProviderService;

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccess() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(50)
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("ssd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(10)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("cpu")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                        .addAllResourceSegmentKeys(List.of(
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("location")
                                                        .setResourceSegmentKey("man")
                                                        .build(),
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("segment")
                                                        .setResourceSegmentKey("default")
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build())));
        UpdateProvisionOperationDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(100L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(10L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionOperationDto.class)
                .returnResult()
                .getResponseBody();
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(UpdateProvisionOperationStatusDto.SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getResult().isEmpty());
        Assertions.assertEquals(3, responseBody.getResult().size());
        Map<String, AccountProvisionDto> resultProvisionsByResource = responseBody.getResult().stream()
                .collect(Collectors.toMap(AccountProvisionDto::getResourceId, Function.identity()));
        Assertions.assertEquals(100000000L, resultProvisionsByResource.get(YP_HDD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(50000000L, resultProvisionsByResource.get(YP_HDD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(10L, resultProvisionsByResource.get(YP_SSD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_SSD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(80L, resultProvisionsByResource.get(YP_RAM_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_RAM_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_HDD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_SSD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_RAM_MAN).getProviderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getFolderId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getAccountId());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessProviderOverride() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(50)
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("ssd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(20)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("cpu")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                        .addAllResourceSegmentKeys(List.of(
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("location")
                                                        .setResourceSegmentKey("man")
                                                        .build(),
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("segment")
                                                        .setResourceSegmentKey("default")
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build())));
        UpdateProvisionOperationDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(100L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(10L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionOperationDto.class)
                .returnResult()
                .getResponseBody();
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(UpdateProvisionOperationStatusDto.SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getResult().isEmpty());
        Assertions.assertEquals(3, responseBody.getResult().size());
        Map<String, AccountProvisionDto> resultProvisionsByResource = responseBody.getResult().stream()
                .collect(Collectors.toMap(AccountProvisionDto::getResourceId, Function.identity()));
        Assertions.assertEquals(100000000L, resultProvisionsByResource.get(YP_HDD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(50000000L, resultProvisionsByResource.get(YP_HDD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(20L, resultProvisionsByResource.get(YP_SSD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_SSD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(80L, resultProvisionsByResource.get(YP_RAM_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_RAM_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_HDD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_SSD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_RAM_MAN).getProviderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getFolderId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getAccountId());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessMakeZero() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(0L)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0L)
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("ssd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(10)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("cpu")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                        .addAllResourceSegmentKeys(List.of(
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("location")
                                                        .setResourceSegmentKey("man")
                                                        .build(),
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("segment")
                                                        .setResourceSegmentKey("default")
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build())));
        UpdateProvisionOperationDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(0L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(10L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionOperationDto.class)
                .returnResult()
                .getResponseBody();
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(UpdateProvisionOperationStatusDto.SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getResult().isEmpty());
        Assertions.assertEquals(3, responseBody.getResult().size());
        Map<String, AccountProvisionDto> resultProvisionsByResource = responseBody.getResult().stream()
                .collect(Collectors.toMap(AccountProvisionDto::getResourceId, Function.identity()));
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_HDD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_HDD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(10L, resultProvisionsByResource.get(YP_SSD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_SSD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(80L, resultProvisionsByResource.get(YP_RAM_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_RAM_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_HDD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_SSD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_RAM_MAN).getProviderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getFolderId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getAccountId());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessProviderUnsolicitedIgnored() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(50)
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("ssd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(20)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("cpu")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                        .addAllResourceSegmentKeys(List.of(
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("location")
                                                        .setResourceSegmentKey("man")
                                                        .build(),
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("segment")
                                                        .setResourceSegmentKey("default")
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build())));
        UpdateProvisionOperationDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(10L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionOperationDto.class)
                .returnResult()
                .getResponseBody();
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(UpdateProvisionOperationStatusDto.SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getResult().isEmpty());
        Assertions.assertEquals(3, responseBody.getResult().size());
        Map<String, AccountProvisionDto> resultProvisionsByResource = responseBody.getResult().stream()
                .collect(Collectors.toMap(AccountProvisionDto::getResourceId, Function.identity()));
        Assertions.assertEquals(200000000L, resultProvisionsByResource.get(YP_HDD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(100000000L, resultProvisionsByResource.get(YP_HDD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(20L, resultProvisionsByResource.get(YP_SSD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_SSD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(80L, resultProvisionsByResource.get(YP_RAM_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_RAM_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_HDD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_SSD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_RAM_MAN).getProviderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getFolderId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getAccountId());
    }

    @Test
    @SuppressWarnings({"unchecked", "MethodLength"})
    public void testNonRetryableFailure() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Test failure")))));
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(100L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(10L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                Set.of("Error occurred while performing the operation on the provider side. Test failure"),
                result.getErrors());
        Map<String, ?> operationMeta = (Map<String, ?>) ((Set<?>) result.getDetails().get("operationMeta"))
                .iterator().next();
        Assertions.assertTrue(operationMeta.containsKey("operationId"));
        String responseOperationId = (String) operationMeta.get("operationId");
        Assertions.assertEquals(operationId, responseOperationId);
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", operationId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(operationId, operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_3.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.FAILURE, operationResult.getStatus());
        Assertions.assertTrue(operationResult.getFailure().isPresent());
    }

    @Test
    @SuppressWarnings({"unchecked", "MethodLength"})
    public void testConflictNoMatch() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Test failure")))));
        stubProviderService.setGetAccountResponses(List.of(GrpcResponse.success(Account
                .newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                        .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                .addAllResourceSegmentKeys(List.of(
                                        ResourceSegmentKey.newBuilder()
                                                .setResourceSegmentationKey("location")
                                                .setResourceSegmentKey("man")
                                                .build(),
                                        ResourceSegmentKey.newBuilder()
                                                .setResourceSegmentationKey("segment")
                                                .setResourceSegmentKey("default")
                                                .build()
                                ))
                                .build())
                        .build())
                .setAccountId("123")
                .setDeleted(false)
                .setFolderId(TEST_FOLDER_1_ID)
                .setDisplayName("тестовый аккаунт")
                .setKey("dummy")
                .setFreeTier(false)
                .addAllProvisions(List.of())
                .build())));
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(1000L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(1000L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.PRECONDITION_FAILED)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of("Error occurred while performing the operation on the provider side. " +
                "FAILED_PRECONDITION Test failure"), result.getErrors());
        Map<String, ?> operationMeta = (Map<String, ?>) ((Set<?>) result.getDetails().get("operationMeta"))
                .iterator().next();
        Assertions.assertTrue(operationMeta.containsKey("operationId"));
        String responseOperationId = (String) operationMeta.get("operationId");
        Assertions.assertEquals(operationId, responseOperationId);
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", operationId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(operationId, operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_3.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.FAILURE, operationResult.getStatus());
        Assertions.assertTrue(operationResult.getFailure().isPresent());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testInsufficientBalance() {
        stubProviderService.reset();
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(10000L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(10000L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of("Not enough quota on the balance."),
                result.getFieldErrors().get("updatedProvisions.0.providedAmount"));
        Assertions.assertEquals(Set.of("Not enough quota on the balance."),
                result.getFieldErrors().get("updatedProvisions.1.providedAmount"));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testConflictHasMatch() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Test failure")))));
        stubProviderService.setGetAccountResponses(List.of(GrpcResponse.success(Account
                .newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                        .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                .addAllResourceSegmentKeys(List.of(
                                        ResourceSegmentKey.newBuilder()
                                                .setResourceSegmentationKey("location")
                                                .setResourceSegmentKey("man")
                                                .build(),
                                        ResourceSegmentKey.newBuilder()
                                                .setResourceSegmentationKey("segment")
                                                .setResourceSegmentKey("default")
                                                .build()
                                ))
                                .build())
                        .build())
                .setAccountId("123")
                .setDeleted(false)
                .setFolderId(TEST_FOLDER_1_ID)
                .setDisplayName("тестовый аккаунт")
                .setKey("dummy")
                .setFreeTier(false)
                .addProvisions(Provision.newBuilder()
                        .setResourceKey(ResourceKey.newBuilder()
                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                        .setResourceTypeKey("hdd")
                                        .build())
                                .build())
                        .setProvided(Amount.newBuilder()
                                .setValue(100)
                                .setUnitKey("gigabytes")
                                .build())
                        .setAllocated(Amount.newBuilder()
                                .setValue(50)
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
                                .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                .build())
                        .build())
                .addProvisions(Provision.newBuilder()
                        .setResourceKey(ResourceKey.newBuilder()
                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                        .setResourceTypeKey("ssd")
                                        .build())
                                .build())
                        .setProvided(Amount.newBuilder()
                                .setValue(10)
                                .setUnitKey("gigabytes")
                                .build())
                        .setAllocated(Amount.newBuilder()
                                .setValue(0)
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
                                .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                .build())
                        .build())
                .addProvisions(Provision.newBuilder()
                        .setResourceKey(ResourceKey.newBuilder()
                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                        .setResourceTypeKey("cpu")
                                        .build())
                                .build())
                        .setProvided(Amount.newBuilder()
                                .setValue(0)
                                .setUnitKey("millicores")
                                .build())
                        .setAllocated(Amount.newBuilder()
                                .setValue(0)
                                .setUnitKey("millicores")
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
                                .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                .build())
                        .build())
                .build())));
        UpdateProvisionOperationDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(100L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(10L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionOperationDto.class)
                .returnResult()
                .getResponseBody();
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(UpdateProvisionOperationStatusDto.SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getResult().isEmpty());
        Assertions.assertEquals(3, responseBody.getResult().size());
        Map<String, AccountProvisionDto> resultProvisionsByResource = responseBody.getResult().stream()
                .collect(Collectors.toMap(AccountProvisionDto::getResourceId, Function.identity()));
        Assertions.assertEquals(100000000L, resultProvisionsByResource.get(YP_HDD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(50000000L, resultProvisionsByResource.get(YP_HDD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(10L, resultProvisionsByResource.get(YP_SSD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_SSD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(80L, resultProvisionsByResource.get(YP_RAM_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_RAM_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_HDD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_SSD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_RAM_MAN).getProviderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getFolderId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getAccountId());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testRetryableFailureSuccessfulInlineRetry() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Test failure"))),
                GrpcResponse.success(UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(50)
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("ssd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(10)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("cpu")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                        .addAllResourceSegmentKeys(List.of(
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("location")
                                                        .setResourceSegmentKey("man")
                                                        .build(),
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("segment")
                                                        .setResourceSegmentKey("default")
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build())));
        UpdateProvisionOperationDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(100L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(10L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionOperationDto.class)
                .returnResult()
                .getResponseBody();
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(UpdateProvisionOperationStatusDto.SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getResult().isEmpty());
        Assertions.assertEquals(3, responseBody.getResult().size());
        Map<String, AccountProvisionDto> resultProvisionsByResource = responseBody.getResult().stream()
                .collect(Collectors.toMap(AccountProvisionDto::getResourceId, Function.identity()));
        Assertions.assertEquals(100000000L, resultProvisionsByResource.get(YP_HDD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(50000000L, resultProvisionsByResource.get(YP_HDD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(10L, resultProvisionsByResource.get(YP_SSD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_SSD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(80L, resultProvisionsByResource.get(YP_RAM_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_RAM_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_HDD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_SSD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_RAM_MAN).getProviderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getFolderId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getAccountId());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testRetryableFailureFailedInlineRetry() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                        .failure(new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Test failure")))));
        UpdateProvisionOperationDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(100L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(10L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isAccepted()
                .expectBody(UpdateProvisionOperationDto.class)
                .returnResult()
                .getResponseBody();
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(UpdateProvisionOperationStatusDto.IN_PROGRESS, responseBody.getOperationStatus());
        Assertions.assertTrue(responseBody.getResult().isEmpty());
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", responseBody.getOperationId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(responseBody.getOperationId(), operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_3.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.IN_PROGRESS, operationResult.getStatus());
        Assertions.assertTrue(operationResult.getFailure().isEmpty());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessIdempotency() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(50)
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("ssd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(10)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("cpu")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                        .addAllResourceSegmentKeys(List.of(
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("location")
                                                        .setResourceSegmentKey("man")
                                                        .build(),
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("segment")
                                                        .setResourceSegmentKey("default")
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build())));
        String idempotencyKey = UUID.randomUUID().toString();
        UpdateProvisionOperationDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(100L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(10L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionOperationDto.class)
                .returnResult()
                .getResponseBody();
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(UpdateProvisionOperationStatusDto.SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getResult().isEmpty());
        Assertions.assertEquals(3, responseBody.getResult().size());
        Map<String, AccountProvisionDto> resultProvisionsByResource = responseBody.getResult().stream()
                .collect(Collectors.toMap(AccountProvisionDto::getResourceId, Function.identity()));
        Assertions.assertEquals(100000000L, resultProvisionsByResource.get(YP_HDD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(50000000L, resultProvisionsByResource.get(YP_HDD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("kilobytes", resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(10L, resultProvisionsByResource.get(YP_SSD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_SSD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(80L, resultProvisionsByResource.get(YP_RAM_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L, resultProvisionsByResource.get(YP_RAM_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_HDD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_SSD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_RAM_MAN).getProviderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getFolderId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getAccountId());
        UpdateProvisionOperationDto idempotencyResponseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(100L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(10L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionOperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(idempotencyResponseBody);
        Assertions.assertEquals(operationId, idempotencyResponseBody.getOperationId());
        Assertions.assertEquals(UpdateProvisionOperationStatusDto.SUCCESS,
                idempotencyResponseBody.getOperationStatus());
        Assertions.assertFalse(idempotencyResponseBody.getResult().isEmpty());
        Assertions.assertEquals(4, idempotencyResponseBody.getResult().size());
        Map<String, AccountProvisionDto> idempotencyResultProvisionsByResource = idempotencyResponseBody.getResult()
                .stream().collect(Collectors.toMap(AccountProvisionDto::getResourceId, Function.identity()));
        Assertions.assertEquals(100000000L,
                idempotencyResultProvisionsByResource.get(YP_HDD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("kilobytes",
                idempotencyResultProvisionsByResource.get(YP_HDD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(50000000L,
                idempotencyResultProvisionsByResource.get(YP_HDD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("kilobytes",
                idempotencyResultProvisionsByResource.get(YP_HDD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(10L,
                idempotencyResultProvisionsByResource.get(YP_SSD_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes",
                idempotencyResultProvisionsByResource.get(YP_SSD_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L,
                idempotencyResultProvisionsByResource.get(YP_SSD_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes",
                idempotencyResultProvisionsByResource.get(YP_SSD_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(80L,
                idempotencyResultProvisionsByResource.get(YP_RAM_MAN).getProvided().longValueExact());
        Assertions.assertEquals("gigabytes",
                idempotencyResultProvisionsByResource.get(YP_RAM_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L,
                idempotencyResultProvisionsByResource.get(YP_RAM_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes",
                idempotencyResultProvisionsByResource.get(YP_RAM_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(0L,
                idempotencyResultProvisionsByResource.get(YP_CPU_MAN).getProvided().longValueExact());
        Assertions.assertEquals("millicores",
                idempotencyResultProvisionsByResource.get(YP_CPU_MAN).getProvidedUnitKey());
        Assertions.assertEquals(0L,
                idempotencyResultProvisionsByResource.get(YP_CPU_MAN).getAllocated().longValueExact());
        Assertions.assertEquals("millicores",
                idempotencyResultProvisionsByResource.get(YP_CPU_MAN).getAllocatedUnitKey());
        Assertions.assertEquals(YP_ID, idempotencyResultProvisionsByResource.get(YP_HDD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, idempotencyResultProvisionsByResource.get(YP_SSD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, idempotencyResultProvisionsByResource.get(YP_RAM_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, idempotencyResultProvisionsByResource.get(YP_CPU_MAN).getProviderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, idempotencyResultProvisionsByResource.get(YP_HDD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, idempotencyResultProvisionsByResource.get(YP_SSD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, idempotencyResultProvisionsByResource.get(YP_RAM_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, idempotencyResultProvisionsByResource.get(YP_CPU_MAN).getFolderId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID,
                idempotencyResultProvisionsByResource.get(YP_HDD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID,
                idempotencyResultProvisionsByResource.get(YP_SSD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID,
                idempotencyResultProvisionsByResource.get(YP_RAM_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID,
                idempotencyResultProvisionsByResource.get(YP_CPU_MAN).getAccountId());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testRetryableFailureFailedInlineRetryIdempotency() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Test failure")))));
        String idempotencyKey = UUID.randomUUID().toString();
        UpdateProvisionOperationDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(100L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(10L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isAccepted()
                .expectBody(UpdateProvisionOperationDto.class)
                .returnResult()
                .getResponseBody();
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(UpdateProvisionOperationStatusDto.IN_PROGRESS, responseBody.getOperationStatus());
        Assertions.assertTrue(responseBody.getResult().isEmpty());
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", responseBody.getOperationId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(responseBody.getOperationId(), operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_3.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.IN_PROGRESS, operationResult.getStatus());
        Assertions.assertTrue(operationResult.getFailure().isEmpty());
        UpdateProvisionOperationDto idempotencyResponseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(100L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(10L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isAccepted()
                .expectBody(UpdateProvisionOperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(idempotencyResponseBody);
        Assertions.assertEquals(operationId, idempotencyResponseBody.getOperationId());
        Assertions.assertEquals(UpdateProvisionOperationStatusDto.IN_PROGRESS,
                idempotencyResponseBody.getOperationStatus());
        Assertions.assertTrue(idempotencyResponseBody.getResult().isEmpty());
    }

    @Test
    @SuppressWarnings({"unchecked", "MethodLength"})
    public void testConflictNoMatchIdempotency() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Test failure")))));
        stubProviderService.setGetAccountResponses(List.of(GrpcResponse.success(Account
                .newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                        .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                .addAllResourceSegmentKeys(List.of(
                                        ResourceSegmentKey.newBuilder()
                                                .setResourceSegmentationKey("location")
                                                .setResourceSegmentKey("man")
                                                .build(),
                                        ResourceSegmentKey.newBuilder()
                                                .setResourceSegmentationKey("segment")
                                                .setResourceSegmentKey("default")
                                                .build()
                                ))
                                .build())
                        .build())
                .setAccountId("123")
                .setDeleted(false)
                .setFolderId(TEST_FOLDER_1_ID)
                .setDisplayName("тестовый аккаунт")
                .setKey("dummy")
                .setFreeTier(false)
                .addAllProvisions(List.of())
                .build())));
        String idempotencyKey = UUID.randomUUID().toString();
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(1000L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(1000L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.PRECONDITION_FAILED)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of("Error occurred while performing the operation on the provider side. " +
                "FAILED_PRECONDITION Test failure"), result.getErrors());
        Map<String, ?> operationMeta = (Map<String, ?>) ((Set<?>) result.getDetails().get("operationMeta"))
                .iterator().next();
        Assertions.assertTrue(operationMeta.containsKey("operationId"));
        String responseOperationId = (String) operationMeta.get("operationId");
        Assertions.assertEquals(operationId, responseOperationId);
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", operationId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(operationId, operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_3.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.FAILURE, operationResult.getStatus());
        Assertions.assertTrue(operationResult.getFailure().isPresent());
        ErrorCollectionDto idempotencyResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/_provide", TEST_FOLDER_1_ID, TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(UpdateProvisionsDto.builder()
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_HDD_MAN)
                                .provided(1000L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .addProvision(UpdateProvisionDto.builder()
                                .providerId(YP_ID)
                                .resourceId(YP_SSD_MAN)
                                .provided(1000L)
                                .providedUnitKey("gigabytes")
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.PRECONDITION_FAILED)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(idempotencyResult);
        Assertions.assertEquals(Set.of("Error occurred while performing the operation on the provider side. " +
                        "FAILED_PRECONDITION Test failure"),
                idempotencyResult.getErrors());
        Map<String, ?> idempotencyOperationMeta = (Map<String, ?>) ((Set<?>) idempotencyResult.getDetails()
                .get("operationMeta")).iterator().next();
        Assertions.assertTrue(idempotencyOperationMeta.containsKey("operationId"));
        String idempotencyResponseOperationId = (String) idempotencyOperationMeta.get("operationId");
        Assertions.assertEquals(idempotencyResponseOperationId, responseOperationId);
    }

}
