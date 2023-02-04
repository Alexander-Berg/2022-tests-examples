package ru.yandex.intranet.d.grpc.provisions;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.protobuf.util.Timestamps;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple2;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.ProviderProvision;
import ru.yandex.intranet.d.backend.service.proto.ProvisionAmount;
import ru.yandex.intranet.d.backend.service.proto.ProvisionOperationStatus;
import ru.yandex.intranet.d.backend.service.proto.ProvisionsServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.UpdateProvisionsRequest;
import ru.yandex.intranet.d.backend.service.proto.UpdateProvisionsResponse;
import ru.yandex.intranet.d.backend.service.proto.UpdatedProvision;
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
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.utils.ErrorsHelper;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.operations.OperationDto;
import ru.yandex.intranet.d.web.model.operations.OperationStatusDto;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_3;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YP_CPU_MAN;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_RAM_MAN;
import static ru.yandex.intranet.d.TestResources.YP_SSD_MAN;

/**
 * Update provisions public GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class UpdateProvisionsServiceTest {

    @GrpcClient("inProcess")
    private ProvisionsServiceGrpc.ProvisionsServiceBlockingStub provisionsService;
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
        UpdateProvisionsResponse responseBody = provisionsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                .updateProvisions(
                UpdateProvisionsRequest.newBuilder()
                        .setFolderId(TEST_FOLDER_1_ID)
                        .setAccountId(TEST_ACCOUNT_1_ID)
                        .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                .setProviderId(YP_ID)
                                .setResourceId(YP_HDD_MAN)
                                .setProvidedAmount(ProvisionAmount.newBuilder()
                                        .setValue(100L)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                .setProviderId(YP_ID)
                                .setResourceId(YP_SSD_MAN)
                                .setProvidedAmount(ProvisionAmount.newBuilder()
                                        .setValue(10L)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build());
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(ProvisionOperationStatus.PROVISION_SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getProvisionsList().isEmpty());
        Assertions.assertEquals(3, responseBody.getProvisionsList().size());
        Map<String, ProviderProvision> resultProvisionsByResource = responseBody.getProvisionsList().stream()
                .collect(Collectors.toMap(ProviderProvision::getResourceId, Function.identity()));
        Assertions.assertEquals(100000000L,
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(50000000L,
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(10L,
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(80L,
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getUnitKey());
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
        UpdateProvisionsResponse responseBody = provisionsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                .updateProvisions(
                        UpdateProvisionsRequest.newBuilder()
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setAccountId(TEST_ACCOUNT_1_ID)
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_HDD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(100L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_SSD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(10L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build());
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(ProvisionOperationStatus.PROVISION_SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getProvisionsList().isEmpty());
        Assertions.assertEquals(3, responseBody.getProvisionsList().size());
        Map<String, ProviderProvision> resultProvisionsByResource = responseBody.getProvisionsList().stream()
                .collect(Collectors.toMap(ProviderProvision::getResourceId, Function.identity()));
        Assertions.assertEquals(100000000L,
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(50000000L,
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(20L,
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(80L,
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getUnitKey());
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
        UpdateProvisionsResponse responseBody = provisionsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                .updateProvisions(
                        UpdateProvisionsRequest.newBuilder()
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setAccountId(TEST_ACCOUNT_1_ID)
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_HDD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(0L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_SSD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(10L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build());
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(ProvisionOperationStatus.PROVISION_SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getProvisionsList().isEmpty());
        Assertions.assertEquals(3, responseBody.getProvisionsList().size());
        Map<String, ProviderProvision> resultProvisionsByResource = responseBody.getProvisionsList().stream()
                .collect(Collectors.toMap(ProviderProvision::getResourceId, Function.identity()));
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(10L,
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(80L,
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getUnitKey());
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
        UpdateProvisionsResponse responseBody = provisionsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                .updateProvisions(
                        UpdateProvisionsRequest.newBuilder()
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setAccountId(TEST_ACCOUNT_1_ID)
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_SSD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(10L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build());
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(ProvisionOperationStatus.PROVISION_SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getProvisionsList().isEmpty());
        Assertions.assertEquals(3, responseBody.getProvisionsList().size());
        Map<String, ProviderProvision> resultProvisionsByResource = responseBody.getProvisionsList().stream()
                .collect(Collectors.toMap(ProviderProvision::getResourceId, Function.identity()));
        Assertions.assertEquals(200000000L,
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(100000000L,
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(20L,
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(80L,
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getUnitKey());
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
    public void testNonRetryableFailure() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Test failure")))));
        boolean error = false;
        try {
            provisionsService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                    .updateProvisions(
                            UpdateProvisionsRequest.newBuilder()
                                    .setFolderId(TEST_FOLDER_1_ID)
                                    .setAccountId(TEST_ACCOUNT_1_ID)
                                    .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setResourceId(YP_HDD_MAN)
                                            .setProvidedAmount(ProvisionAmount.newBuilder()
                                                    .setValue(100L)
                                                    .setUnitKey("gigabytes")
                                                    .build())
                                            .build())
                                    .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setResourceId(YP_SSD_MAN)
                                            .setProvidedAmount(ProvisionAmount.newBuilder()
                                                    .setValue(10L)
                                                    .setUnitKey("gigabytes")
                                                    .build())
                                            .build())
                                    .build());
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            Assertions.assertEquals(
                    Set.of("Error occurred while performing the operation on the provider side. Test failure"),
                    new HashSet<>(details.get().getErrorsList()));
            error = true;
        }
        Assertions.assertTrue(error);
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
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
        boolean error = false;
        try {
            provisionsService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                    .updateProvisions(
                            UpdateProvisionsRequest.newBuilder()
                                    .setFolderId(TEST_FOLDER_1_ID)
                                    .setAccountId(TEST_ACCOUNT_1_ID)
                                    .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setResourceId(YP_HDD_MAN)
                                            .setProvidedAmount(ProvisionAmount.newBuilder()
                                                    .setValue(100L)
                                                    .setUnitKey("gigabytes")
                                                    .build())
                                            .build())
                                    .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setResourceId(YP_SSD_MAN)
                                            .setProvidedAmount(ProvisionAmount.newBuilder()
                                                    .setValue(10L)
                                                    .setUnitKey("gigabytes")
                                                    .build())
                                            .build())
                                    .build());
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.FAILED_PRECONDITION, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            Assertions.assertEquals(Set.of("Error occurred while performing the operation on the provider side. " +
                            "FAILED_PRECONDITION Test failure"),
                    new HashSet<>(details.get().getErrorsList()));
            error = true;
        }
        Assertions.assertTrue(error);
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
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
        boolean error = false;
        try {
            provisionsService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                    .updateProvisions(
                            UpdateProvisionsRequest.newBuilder()
                                    .setFolderId(TEST_FOLDER_1_ID)
                                    .setAccountId(TEST_ACCOUNT_1_ID)
                                    .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setResourceId(YP_HDD_MAN)
                                            .setProvidedAmount(ProvisionAmount.newBuilder()
                                                    .setValue(10000L)
                                                    .setUnitKey("gigabytes")
                                                    .build())
                                            .build())
                                    .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setResourceId(YP_SSD_MAN)
                                            .setProvidedAmount(ProvisionAmount.newBuilder()
                                                    .setValue(10000L)
                                                    .setUnitKey("gigabytes")
                                                    .build())
                                            .build())
                                    .build());
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getFieldErrorsCount() > 0);
            Assertions.assertEquals("Not enough quota on the balance.",
                    details.get().getFieldErrorsList().get(0).getErrors(0));
            error = true;
        }
        Assertions.assertTrue(error);
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
        UpdateProvisionsResponse responseBody = provisionsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                .updateProvisions(
                        UpdateProvisionsRequest.newBuilder()
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setAccountId(TEST_ACCOUNT_1_ID)
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_HDD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(100L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_SSD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(10L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build());
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(ProvisionOperationStatus.PROVISION_SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getProvisionsList().isEmpty());
        Assertions.assertEquals(3, responseBody.getProvisionsList().size());
        Map<String, ProviderProvision> resultProvisionsByResource = responseBody.getProvisionsList().stream()
                .collect(Collectors.toMap(ProviderProvision::getResourceId, Function.identity()));
        Assertions.assertEquals(100000000L,
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(50000000L,
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(10L,
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(80L,
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getUnitKey());
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
        UpdateProvisionsResponse responseBody = provisionsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                .updateProvisions(
                        UpdateProvisionsRequest.newBuilder()
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setAccountId(TEST_ACCOUNT_1_ID)
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_HDD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(100L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_SSD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(10L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build());
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(ProvisionOperationStatus.PROVISION_SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getProvisionsList().isEmpty());
        Assertions.assertEquals(3, responseBody.getProvisionsList().size());
        Map<String, ProviderProvision> resultProvisionsByResource = responseBody.getProvisionsList().stream()
                .collect(Collectors.toMap(ProviderProvision::getResourceId, Function.identity()));
        Assertions.assertEquals(100000000L,
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(50000000L,
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(10L,
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(80L,
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getUnitKey());
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
        UpdateProvisionsResponse responseBody = provisionsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                .updateProvisions(
                        UpdateProvisionsRequest.newBuilder()
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setAccountId(TEST_ACCOUNT_1_ID)
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_HDD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(100L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_SSD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(10L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build());
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(ProvisionOperationStatus.PROVISION_IN_PROGRESS, responseBody.getOperationStatus());
        Assertions.assertTrue(responseBody.getProvisionsList().isEmpty());
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
        Metadata extraHeaders = new Metadata();
        Metadata.Key<String> headerKey = Metadata.Key.of("Idempotency-Key", Metadata.ASCII_STRING_MARSHALLER);
        extraHeaders.put(headerKey, idempotencyKey);
        UpdateProvisionsResponse responseBody = provisionsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .updateProvisions(
                        UpdateProvisionsRequest.newBuilder()
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setAccountId(TEST_ACCOUNT_1_ID)
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_HDD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(100L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_SSD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(10L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build());
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(ProvisionOperationStatus.PROVISION_SUCCESS, responseBody.getOperationStatus());
        Assertions.assertFalse(responseBody.getProvisionsList().isEmpty());
        Assertions.assertEquals(3, responseBody.getProvisionsList().size());
        Map<String, ProviderProvision> resultProvisionsByResource = responseBody.getProvisionsList().stream()
                .collect(Collectors.toMap(ProviderProvision::getResourceId, Function.identity()));
        Assertions.assertEquals(100000000L,
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(50000000L,
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                resultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(10L,
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(80L,
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                resultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_HDD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_SSD_MAN).getProviderId());
        Assertions.assertEquals(YP_ID, resultProvisionsByResource.get(YP_RAM_MAN).getProviderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getFolderId());
        Assertions.assertEquals(TEST_FOLDER_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getFolderId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_HDD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_SSD_MAN).getAccountId());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, resultProvisionsByResource.get(YP_RAM_MAN).getAccountId());
        UpdateProvisionsResponse idempotencyResponseBody = provisionsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .updateProvisions(
                        UpdateProvisionsRequest.newBuilder()
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setAccountId(TEST_ACCOUNT_1_ID)
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_HDD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(100L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_SSD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(10L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build());
        Assertions.assertNotNull(idempotencyResponseBody);
        Assertions.assertEquals(operationId, idempotencyResponseBody.getOperationId());
        Assertions.assertEquals(ProvisionOperationStatus.PROVISION_SUCCESS,
                idempotencyResponseBody.getOperationStatus());
        Assertions.assertFalse(idempotencyResponseBody.getProvisionsList().isEmpty());
        Assertions.assertEquals(4, idempotencyResponseBody.getProvisionsList().size());
        Map<String, ProviderProvision> idempotencyResultProvisionsByResource = idempotencyResponseBody
                .getProvisionsList().stream()
                .collect(Collectors.toMap(ProviderProvision::getResourceId, Function.identity()));
        Assertions.assertEquals(100000000L,
                idempotencyResultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                idempotencyResultProvisionsByResource.get(YP_HDD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(50000000L,
                idempotencyResultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("kilobytes",
                idempotencyResultProvisionsByResource.get(YP_HDD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(10L,
                idempotencyResultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                idempotencyResultProvisionsByResource.get(YP_SSD_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                idempotencyResultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                idempotencyResultProvisionsByResource.get(YP_SSD_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(80L,
                idempotencyResultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                idempotencyResultProvisionsByResource.get(YP_RAM_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                idempotencyResultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes",
                idempotencyResultProvisionsByResource.get(YP_RAM_MAN).getAllocatedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                idempotencyResultProvisionsByResource.get(YP_CPU_MAN).getProvidedAmount().getValue());
        Assertions.assertEquals("millicores",
                idempotencyResultProvisionsByResource.get(YP_CPU_MAN).getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L,
                idempotencyResultProvisionsByResource.get(YP_CPU_MAN).getAllocatedAmount().getValue());
        Assertions.assertEquals("millicores",
                idempotencyResultProvisionsByResource.get(YP_CPU_MAN).getAllocatedAmount().getUnitKey());
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
        Metadata extraHeaders = new Metadata();
        Metadata.Key<String> headerKey = Metadata.Key.of("Idempotency-Key", Metadata.ASCII_STRING_MARSHALLER);
        extraHeaders.put(headerKey, idempotencyKey);
        UpdateProvisionsResponse responseBody = provisionsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .updateProvisions(
                        UpdateProvisionsRequest.newBuilder()
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setAccountId(TEST_ACCOUNT_1_ID)
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_HDD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(100L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_SSD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(10L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build());
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(operationId, responseBody.getOperationId());
        Assertions.assertEquals(ProvisionOperationStatus.PROVISION_IN_PROGRESS, responseBody.getOperationStatus());
        Assertions.assertTrue(responseBody.getProvisionsList().isEmpty());
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
        UpdateProvisionsResponse idempotentResponseBody = provisionsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .updateProvisions(
                        UpdateProvisionsRequest.newBuilder()
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setAccountId(TEST_ACCOUNT_1_ID)
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_HDD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(100L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_SSD_MAN)
                                        .setProvidedAmount(ProvisionAmount.newBuilder()
                                                .setValue(10L)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .build());
        Assertions.assertNotNull(idempotentResponseBody);
        Assertions.assertEquals(operationId, idempotentResponseBody.getOperationId());
        Assertions.assertEquals(ProvisionOperationStatus.PROVISION_IN_PROGRESS,
                idempotentResponseBody.getOperationStatus());
        Assertions.assertTrue(idempotentResponseBody.getProvisionsList().isEmpty());
    }

    @Test
    @SuppressWarnings("MethodLength")
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
        Metadata extraHeaders = new Metadata();
        Metadata.Key<String> headerKey = Metadata.Key.of("Idempotency-Key", Metadata.ASCII_STRING_MARSHALLER);
        extraHeaders.put(headerKey, idempotencyKey);
        boolean error = false;
        try {
            provisionsService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                    .updateProvisions(
                            UpdateProvisionsRequest.newBuilder()
                                    .setFolderId(TEST_FOLDER_1_ID)
                                    .setAccountId(TEST_ACCOUNT_1_ID)
                                    .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setResourceId(YP_HDD_MAN)
                                            .setProvidedAmount(ProvisionAmount.newBuilder()
                                                    .setValue(100L)
                                                    .setUnitKey("gigabytes")
                                                    .build())
                                            .build())
                                    .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setResourceId(YP_SSD_MAN)
                                            .setProvidedAmount(ProvisionAmount.newBuilder()
                                                    .setValue(10L)
                                                    .setUnitKey("gigabytes")
                                                    .build())
                                            .build())
                                    .build());
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.FAILED_PRECONDITION, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            Assertions.assertEquals(Set.of("Error occurred while performing the operation on the provider side. " +
                            "FAILED_PRECONDITION Test failure"),
                    new HashSet<>(details.get().getErrorsList()));
            error = true;
        }
        Assertions.assertTrue(error);
        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();
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
        boolean idempotencyError = false;
        try {
            provisionsService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_3_UID))
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                    .updateProvisions(
                            UpdateProvisionsRequest.newBuilder()
                                    .setFolderId(TEST_FOLDER_1_ID)
                                    .setAccountId(TEST_ACCOUNT_1_ID)
                                    .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setResourceId(YP_HDD_MAN)
                                            .setProvidedAmount(ProvisionAmount.newBuilder()
                                                    .setValue(100L)
                                                    .setUnitKey("gigabytes")
                                                    .build())
                                            .build())
                                    .addUpdatedProvisions(UpdatedProvision.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setResourceId(YP_SSD_MAN)
                                            .setProvidedAmount(ProvisionAmount.newBuilder()
                                                    .setValue(10L)
                                                    .setUnitKey("gigabytes")
                                                    .build())
                                            .build())
                                    .build());
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.FAILED_PRECONDITION, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            Assertions.assertEquals(Set.of("Error occurred while performing the operation on the provider side. " +
                    "FAILED_PRECONDITION Test failure"),
                    new HashSet<>(details.get().getErrorsList()));
            idempotencyError = true;
        }
        Assertions.assertTrue(idempotencyError);
    }

}
