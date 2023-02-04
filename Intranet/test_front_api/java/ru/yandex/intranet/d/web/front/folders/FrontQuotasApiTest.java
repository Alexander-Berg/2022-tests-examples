package ru.yandex.intranet.d.web.front.folders;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.protobuf.util.Timestamps;
import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple2;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.LogCollector;
import ru.yandex.intranet.d.TestGrpcResponses;
import ru.yandex.intranet.d.TestSegmentations;
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
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.AccountDto;
import ru.yandex.intranet.d.web.model.AmountDto;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.folders.FrontAmountsDto;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedAccount;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedAccountResource;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedProvider;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedResource;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedResourceBuilder;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedResourceType;
import ru.yandex.intranet.d.web.model.quotas.AccountsQuotasOperationsDto;
import ru.yandex.intranet.d.web.model.quotas.ProvisionLiteDto;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionsAnswerDto;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionsRequestDto;

import static java.util.Locale.ENGLISH;
import static ru.yandex.intranet.d.TestAccounts.DELETED_ACCOUNT;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_2_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_4_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_3_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_2_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_4_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResourceTypes.YP_HDD;
import static ru.yandex.intranet.d.TestResourceTypes.YP_SSD;
import static ru.yandex.intranet.d.TestResources.DELETED_RESOURCE;
import static ru.yandex.intranet.d.TestResources.YDB_RAM_SAS;
import static ru.yandex.intranet.d.TestResources.YP_CPU_SAS;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MYT;
import static ru.yandex.intranet.d.TestResources.YP_HDD_SAS;
import static ru.yandex.intranet.d.TestResources.YP_SSD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_SSD_VLA;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_D;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_DISPENSER;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_LAVKA;
import static ru.yandex.intranet.d.UnitIds.BYTES;
import static ru.yandex.intranet.d.UnitIds.CORES;
import static ru.yandex.intranet.d.UnitIds.GIGABYTES;
import static ru.yandex.intranet.d.UnitIds.TERABYTES;

/**
 * Tests for page /front/quotas
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@IntegrationTest
public class FrontQuotasApiTest {
    @Autowired
    private WebTestClient webClient;
    @Autowired
    @Qualifier("messageSource")
    private MessageSource messages;
    @Autowired
    private StubProviderService stubProviderService;
    @Autowired
    private YdbTableClient tableClient;
    @Autowired
    private AccountsQuotasDao accountsQuotasDao;
    @Autowired
    private QuotasDao quotasDao;

    static void setUpUpdateAnswer(StubProviderService stubProviderService) {
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
                                        .setValue(100)
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
                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                        .addAllResourceSegmentKeys(List.of(ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("segment")
                                                        .setResourceSegmentKey("default")
                                                        .build(),
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("location")
                                                        .setResourceSegmentKey("man")
                                                        .build())
                                        )
                                        .build())
                                .build())
                        .build())));
    }

    public static UpdateProvisionsRequestDto.Builder getBody() {
        return UpdateProvisionsRequestDto.builder()
                .setAccountId(TEST_ACCOUNT_1_ID)
                .setFolderId(TEST_FOLDER_1_ID) // folder in Dispenser
                .setServiceId(TEST_SERVICE_ID_DISPENSER) // Dispenser
                .setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "100", // provided amount
                                GIGABYTES, // provided amount unit id
                                "200", // old provided amount
                                GIGABYTES))); // old provided amount unit id
    }

    @Test
    public void onlyQuotaManagerCanUpdateProvisionsTest() {
        setUpUpdateAnswer(stubProviderService);

        webClient
                .mutateWith(MockUser.uid(TestUsers.DISPENSER_QUOTA_MANAGER_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().build())
                .exchange()
                .expectStatus()
                .isOk();

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_QUOTA_MANAGER_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().build())
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    public void dAdminsCanUpdateProvisionsTest() {
        setUpUpdateAnswer(stubProviderService);
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().build())
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void updateProvisionsErrorOnEmptyUpdatedProvisionsTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(Collections.emptyList()).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.field.is.required", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions").iterator().next());
    }

    private ErrorCollectionDto getResponseBody(UpdateProvisionsRequestDto body) {
        return webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    public void updateProvisionsErrorOnWrongUpdatedProvisionsTest() {
        ErrorCollectionDto result = getResponseBody(getBody().setUpdatedProvisions(Collections.singletonList(
                new ProvisionLiteDto(null, // resourceId
                        "100", // provided amount
                        BYTES, // provided amount unit id
                        "200", // old provided amount
                        BYTES))).build());

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.resource.id.is.required", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.resourceId").iterator().next());

        result = getResponseBody(getBody().setUpdatedProvisions(Collections.singletonList(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        BYTES, // provided amount unit key
                        null, // old provided amount
                        BYTES))).build());

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.account.provided.amount.is.required", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.oldProvidedAmount").iterator().next());

        result = getResponseBody(getBody().setUpdatedProvisions(Collections.singletonList(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        BYTES, // provided amount unit id
                        "aaa", // old provided amount
                        BYTES))).build());

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.number.invalid.format", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.oldProvidedAmount").iterator().next());

        result = getResponseBody(getBody().setUpdatedProvisions(Collections.singletonList(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        BYTES, // provided amount unit id
                        "-1", // old provided amount
                        BYTES))).build());

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.number.must.be.non.negative", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.oldProvidedAmount").iterator().next());

        result = getResponseBody(getBody().setUpdatedProvisions(Collections.singletonList(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        null, // provided amount
                        BYTES, // provided amount unit id
                        "200", // old provided amount
                        BYTES))).build());

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.account.provided.amount.is.required", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.providedAmount").iterator().next());

        result = getResponseBody(getBody().setUpdatedProvisions(Collections.singletonList(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "aaa", // provided amount
                        BYTES, // provided amount unit id
                        "200", // old provided amount
                        BYTES))).build());

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.number.invalid.format", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.providedAmount").iterator().next());

        result = getResponseBody(getBody().setUpdatedProvisions(Collections.singletonList(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "-1", // provided amount
                        BYTES, // provided amount unit id
                        "200", // old provided amount
                        BYTES))).build());

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.number.must.be.non.negative", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.providedAmount").iterator().next());

        result = getResponseBody(getBody().setUpdatedProvisions(Collections.singletonList(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        BYTES, // provided amount unit id
                        "200", // old provided amount
                        null))).build());

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.unit.id.is.required", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.oldProvidedAmountUnitId").iterator().next());

        result = getResponseBody(getBody().setUpdatedProvisions(Collections.singletonList(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        null, // provided amount unit key
                        "200", // old provided amount
                        BYTES))).build());

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.unit.id.is.required", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.providedAmountUnitId").iterator().next());
    }

    @Test
    public void updateProvisionsErrorOnWrongUpdatedProvisionsResourcesTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(List.of(
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "100", // provided amount
                                BYTES, // provided amount unit id
                                "200", // old provided amount
                                BYTES),
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "100", // provided amount
                                BYTES, // provided amount unit id
                                "200", // old provided amount
                                BYTES))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.resource.id.duplicate", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions").iterator().next());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(List.of(
                        new ProvisionLiteDto("fake-fake-fake-fake-fake", // resourceId
                                "100", // provided amount
                                BYTES, // provided amount unit id
                                "200", // old provided amount
                                BYTES))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();


        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.resource.not.found", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.resourceId").iterator().next());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(List.of(
                        new ProvisionLiteDto(DELETED_RESOURCE, // resourceId
                                "100", // provided amount
                                BYTES, // provided amount unit id
                                "200", // old provided amount
                                BYTES))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.resource.not.found", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.resourceId").iterator().next());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(List.of(
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "100", // provided amount
                                BYTES, // provided amount unit id
                                "200", // old provided amount
                                BYTES),
                        new ProvisionLiteDto(YDB_RAM_SAS, // resourceId
                                "100", // provided amount
                                BYTES, // provided amount unit id
                                "200", // old provided amount
                                BYTES))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.wrong.provider.for.resource", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions").iterator().next());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(List.of(
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "100", // provided amount
                                BYTES, // provided amount unit id
                                "200", // old provided amount
                                BYTES),
                        new ProvisionLiteDto(YP_SSD_VLA, // resourceId
                                "100", // provided amount
                                BYTES, // provided amount unit id
                                "200", // old provided amount
                                BYTES))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.wrong.resource.account.space.for.account", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions").iterator().next());
    }

    @Test
    public void updateProvisionsErrorOnWrongUpdatedProvisionsUnitsTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "100", // provided amount
                                "kekBytes", // provided amount unit key
                                "200", // old provided amount
                                GIGABYTES))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.unit.not.found", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.providedAmountUnitId").iterator().next());

        result = webClient.mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "100", // provided amount
                                BYTES, // provided amount unit id
                                "200", // old provided amount
                                GIGABYTES))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.unit.not.allowed", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.providedAmountUnitId").iterator().next());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "100", // provided amount
                                GIGABYTES, // provided amount unit key
                                "200", // old provided amount
                                "kekBytes"))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.unit.not.found", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.oldProvidedAmountUnitId").iterator().next());

        result = webClient.mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "100", // provided amount
                                GIGABYTES, // provided amount unit key
                                "200", // old provided amount
                                BYTES))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.unit.not.allowed", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.oldProvidedAmountUnitId").iterator().next());
    }

    @Test
    public void updateProvisionsErrorOnInvalidAccountTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setAccountId(null).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.field.is.required", null, ENGLISH),
                result.getFieldErrors().get("accountId").iterator().next());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setAccountId(DELETED_ACCOUNT.getId()).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.account.not.found", null, ENGLISH),
                result.getFieldErrors().get("accountId").iterator().next());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setAccountId(TEST_ACCOUNT_2_ID).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.wrong.provider.for.resource", null, ENGLISH),
                result.getFieldErrors().get("accountId").iterator().next());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_SSD_VLA, // resourceId
                                "100", // provided amount
                                GIGABYTES, // provided amount unit key
                                "200", // old provided amount
                                GIGABYTES))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.wrong.resource.account.space.for.account", null, ENGLISH),
                result.getFieldErrors().get("accountId").iterator().next());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setAccountId("some-fake-account-id").build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.account.not.found", null, ENGLISH),
                result.getFieldErrors().get("accountId").iterator().next());
    }

    @Test
    public void updateProvisionsErrorOnInvalidFolderTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setFolderId(null).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.folder.is.required", null, ENGLISH),
                result.getFieldErrors().get("folderId").iterator().next());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setFolderId(TEST_FOLDER_2_ID).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage(
                        "errors.folder.not.match.account.one", new String[]{TEST_FOLDER_1_ID}, ENGLISH),
                result.getFieldErrors().get("folderId").iterator().next());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setServiceId(TEST_SERVICE_ID_D).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.service.not.match.folder.one", new String[]{"1"}, ENGLISH),
                result.getFieldErrors().get("serviceId").iterator().next());
    }

    @Test
    public void updateProvisionsErrorOnInvalidQuotasTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setAccountId(TEST_ACCOUNT_4_ID)
                        .setServiceId(TEST_SERVICE_ID_LAVKA)
                        .setFolderId(TEST_FOLDER_4_ID)
                        .setUpdatedProvisions(List.of(
                                new ProvisionLiteDto(YP_CPU_SAS, // resourceId
                                        "100", // provided amount
                                        CORES, // provided amount unit key
                                        "0", // old provided amount
                                        CORES),
                                new ProvisionLiteDto(YP_HDD_SAS, // resourceId
                                        "100", // provided amount
                                        GIGABYTES, // provided amount unit key
                                        "200", // old provided amount
                                        GIGABYTES))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.quota.not.found", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0").iterator().next());
    }

    @Test
    public void updateProvisionsErrorOnInvalidProvisionsTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(List.of(
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "10000", // provided amount
                                GIGABYTES, // provided amount unit key
                                "200", // old provided amount
                                GIGABYTES))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.balance.is.to.low", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.providedAmount").iterator().next());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(List.of(
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "10000", // provided amount
                                GIGABYTES, // provided amount unit key
                                "199", // old provided amount
                                GIGABYTES))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.provision.mismatch", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions.0.oldProvidedAmount").iterator().next());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(List.of(
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "200", // provided amount
                                GIGABYTES, // provided amount unit key
                                "200", // old provided amount
                                GIGABYTES))).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
        Assertions.assertEquals(messages.getMessage("errors.provision.no.change", null, ENGLISH),
                result.getFieldErrors().get("updatedProvisions").iterator().next());
    }

    @Test
    public void updateProvisionsFrozenQuotaCalculateTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.UNAVAILABLE))));

        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isBadRequest();

        QuotaModel quotaOne = Objects.requireNonNull(tableClient.usingSessionMonoRetryable(
                        s -> s.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                ts -> quotasDao.getOneQuota(ts, TEST_FOLDER_1_ID, YP_ID, YP_HDD_MAN,
                                        Tenants.DEFAULT_TENANT_ID)))
                .block()).orElseThrow();
        QuotaModel quotaTwo = Objects.requireNonNull(tableClient.usingSessionMonoRetryable(
                        s -> s.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                ts -> quotasDao.getOneQuota(ts, TEST_FOLDER_1_ID, YP_ID, YP_SSD_MAN,
                                        Tenants.DEFAULT_TENANT_ID)))
                .block()).orElseThrow();
        AccountsQuotasModel provisionOne = Objects.requireNonNull(tableClient.usingSessionMonoRetryable(
                        s -> s.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                ts -> accountsQuotasDao.getById(ts, new AccountsQuotasModel.Identity(TEST_ACCOUNT_1_ID,
                                        YP_HDD_MAN), Tenants.DEFAULT_TENANT_ID)))
                .block()).orElseThrow();
        Optional<AccountsQuotasModel> provisionTwo = tableClient.usingSessionMonoRetryable(
                        s -> s.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                ts -> accountsQuotasDao.getById(ts, new AccountsQuotasModel.Identity(TEST_ACCOUNT_1_ID,
                                        YP_SSD_MAN), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertEquals(1_000_000_000_000L, quotaOne.getQuota());
        Assertions.assertEquals(800_000_000_000L, quotaOne.getBalance());
        Assertions.assertEquals(0L, quotaOne.getFrozenQuota());
        Assertions.assertEquals(2_000_000_000_000L, quotaTwo.getQuota());
        Assertions.assertEquals(1_990_000_000_000L, quotaTwo.getBalance());
        Assertions.assertEquals(10_000_000_000L, quotaTwo.getFrozenQuota());
        Assertions.assertEquals(200_000_000_000L, provisionOne.getProvidedQuota());
        Assertions.assertTrue(Objects.requireNonNull(provisionTwo).isEmpty());
    }

    public static void prepareUpdateProvisionsOkResponseTest(StubProviderService stubProviderService) {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse.newBuilder()
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
                                        .setValue(100)
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
    }

    /**
     * Update provisions successful response test.
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontQuotasController#updateProvision
     */
    @Test
    public void updateProvisionsOkResponseTest() {
        prepareUpdateProvisionsOkResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        UpdateProvisionsAnswerDto responseBody = LogCollector.collectLogs(() -> webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class)
                .returnResult()
                .getResponseBody());

        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();

        Assertions.assertNotNull(responseBody);
        AccountsQuotasOperationsDto accountsQuotasOperationsDto = responseBody.getAccountsQuotasOperationsDto();
        Assertions.assertNotNull(accountsQuotasOperationsDto);
        AccountsQuotasOperationsDto expectedAccountsQuotasOperationsDto = new AccountsQuotasOperationsDto(operationId,
                AccountsQuotasOperationsModel.RequestStatus.OK);
        Assertions.assertEquals(expectedAccountsQuotasOperationsDto, accountsQuotasOperationsDto);

        ExpandedProvider expandedProvider = responseBody.getExpandedProvider();
        List<ExpandedResourceType> resourceTypes = expandedProvider.getResourceTypes();
        Assertions.assertNotNull(resourceTypes);
        Assertions.assertEquals(3, resourceTypes.size());

        Map<String, ExpandedResourceType> resourceTypeMap = resourceTypes.stream()
                .collect(Collectors.toMap(ExpandedResourceType::getResourceTypeId, Function.identity()));

        ExpandedResourceType resourceType = resourceTypeMap.get(YP_HDD);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals(YP_HDD, resourceType.getResourceTypeId());
        List<ExpandedResource> resources = resourceType.getResources();
        Assertions.assertNotNull(resources);
        Assertions.assertEquals(2, resources.size());

        List<ExpandedResource> expectedResources = getExpectedResourcesYpHDD();
        Assertions.assertEquals(expectedResources, resources);

        FrontAmountsDto sums = resourceType.getSums();
        Assertions.assertNotNull(sums);

        Assertions.assertEquals(getExpectedSumsYpHDD(), sums);

        resourceType = resourceTypeMap.get(YP_SSD);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals(YP_SSD, resourceType.getResourceTypeId());
        resources = resourceType.getResources();
        Assertions.assertNotNull(resources);
        Assertions.assertEquals(1, resources.size());

        expectedResources = getExpectedResourcesYpSSD();
        Assertions.assertEquals(expectedResources, resources);

        sums = resourceType.getSums();
        Assertions.assertNotNull(sums);

        Assertions.assertEquals(getExpectedSumsYpSSD(), sums);

        List<ExpandedAccount> accounts = expandedProvider.getAccounts();
        Assertions.assertNotNull(accounts);
        Assertions.assertEquals(1, accounts.size());

        ExpandedAccount expandedAccount = accounts.iterator().next();
        Assertions.assertNotNull(expandedAccount);

        AccountDto account = expandedAccount.getAccount();
        String expectedUrlKey = TestSegmentations.YP_SEGMENT_DEFAULT + "." +
                TestSegmentations.YP_LOCATION_MAN + ":Yandex Deploy";
        String expectedUrl = "https://deploy.yandex-team.ru/yp/man/pod-sets?accountId=abc:service:1&segments=default";

        Assertions.assertNotNull(expandedAccount.getExternalAccountUrls());
        Assertions.assertFalse(expandedAccount.getExternalAccountUrls().isEmpty());
        Assertions.assertTrue(expandedAccount.getUrlsForSegments());
        Assertions.assertTrue(expandedAccount.getExternalAccountUrls().containsKey(expectedUrlKey));
        Assertions.assertEquals(expectedUrl, expandedAccount.getExternalAccountUrls().get(expectedUrlKey));

        Assertions.assertNotNull(account);
        Assertions.assertEquals(new AccountDto(
                TEST_ACCOUNT_1_ID,
                "тестовый аккаунт",
                TEST_FOLDER_1_ID,
                false,
                TEST_ACCOUNT_SPACE_3_ID,
                false,
                YP_ID, null), account);

        List<ExpandedAccountResource> resources1 = expandedAccount.getResources();
        Assertions.assertNotNull(resources1);
        Assertions.assertEquals(2, resources1.size());

        List<ExpandedAccountResource> expectedExpandedResource = getExpectedExpandedResource();
        Assertions.assertEquals(expectedExpandedResource, resources1);

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

    public static void prepareFailedPreconditionResponseTest(StubProviderService stubProviderService) {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(TestGrpcResponses.failedPreconditionTestResponse()));

        stubProviderService.setGetAccountResponses(List.of(GrpcResponse
                .success(Account.newBuilder()
                        .setAccountId(TEST_ACCOUNT_1_ID)
                        .setDeleted(false)
                        .setDisplayName("test")
                        .setFolderId(TEST_FOLDER_1_ID)
                        .setKey("test")
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(200)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(200)
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
    }

    /**
     * Update provisions failed preconditions response test.
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontQuotasController#updateProvision
     */
    @Test
    public void updateProvisionsFailedPreconditionResponseTest() {
        prepareFailedPreconditionResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES));
        ErrorCollectionDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(Set.of("Error occurred while performing the operation on the provider side."),
                responseBody.getErrors());
        Assertions.assertNotNull(responseBody.getDetails());
        Assertions.assertEquals(1, responseBody.getDetails().get("errorFromProvider").size());
        String errorFromProvider = (String) responseBody.getDetails().get("errorFromProvider")
                .stream().findFirst().get();
        Assertions.assertTrue(errorFromProvider.startsWith("""
                FAILED_PRECONDITION Test error Test error
                test: Test error description
                Request id:"""));
    }

    public static void prepareUpdateProvisionsUnknownResponseTest(StubProviderService stubProviderService) {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(
                TestGrpcResponses.unknownTestResponse(),
                TestGrpcResponses.unknownTestResponse()
        ));
    }

    /**
     * Update provisions unknown response test.
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontQuotasController#updateProvision
     */
    @Test
    public void updateProvisionsUnknownResponseTest() {
        prepareUpdateProvisionsUnknownResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES));
        ErrorCollectionDto responseBody = LogCollector.collectLogs(() -> webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody());
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(Set.of("Error occurred while performing the operation on the provider side."),
                responseBody.getErrors());
        Assertions.assertNotNull(responseBody.getDetails());
        String errorFromProvider = (String) responseBody.getDetails().get("errorFromProvider")
                .stream().findFirst().get();
        Assertions.assertTrue(errorFromProvider.startsWith("Test error\nRequest id:"));

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

    public static List<ExpandedAccountResource> getExpectedExpandedResource() {
        return List.of(
                new ExpandedResourceBuilder()
                        .setResourceId(YP_HDD_MAN)
                        .setBalance(new AmountDto(
                                "800", "GB",
                                "800000000000", "B",
                                "800", GIGABYTES,
                                "800", GIGABYTES
                        ))
                        .setProvided(new AmountDto(
                                "100", "GB",
                                "100000000000", "B",
                                "100", GIGABYTES,
                                "100", GIGABYTES
                        ))
                        .setProvidedRatio(BigDecimal.ZERO)
                        .setAllocated(new AmountDto(
                                "100", "GB",
                                "100000000000", "B",
                                "100", GIGABYTES,
                                "100", GIGABYTES
                        ))
                        .setAllocatedRatio(BigDecimal.ZERO)
                        .setProvidedAndNotAllocated(new AmountDto(
                                "0", "GB",
                                "0", "GB",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .buildExpandedAccountResource(),
                new ExpandedResourceBuilder()
                        .setResourceId(YP_SSD_MAN)
                        .setBalance(new AmountDto(
                                "1.99", "TB",
                                "1990000000000", "B",
                                "1990", GIGABYTES,
                                "1990", GIGABYTES
                        ))
                        .setProvided(new AmountDto(
                                "10", "GB",
                                "10000000000", "B",
                                "10", GIGABYTES,
                                "10", GIGABYTES
                        ))
                        .setProvidedRatio(BigDecimal.ZERO)
                        .setAllocated(new AmountDto(
                                "0", "GB",
                                "0", "GB",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .setAllocatedRatio(BigDecimal.ZERO)
                        .setProvidedAndNotAllocated(new AmountDto(
                                "10", "GB",
                                "10000000000", "B",
                                "10", GIGABYTES,
                                "10", GIGABYTES
                        ))
                        .buildExpandedAccountResource()
        );
    }

    public static List<ExpandedResource> getExpectedResourcesYpHDD() {
        return List.of(
                new ExpandedResourceBuilder()
                        .setResourceId(YP_HDD_MAN)
                        .setQuota(new AmountDto(
                                "1", "TB",
                                "1000000000000", "B",
                                "1", TERABYTES,
                                "1000", GIGABYTES
                        ))
                        .setBalance(new AmountDto(
                                "900", "GB",
                                "900000000000", "B",
                                "900", GIGABYTES,
                                "900", GIGABYTES
                        ))
                        .setPositiveBalance(new AmountDto(
                                "900", "GB",
                                "900000000000", "B",
                                "900", GIGABYTES,
                                "900", GIGABYTES
                        ))
                        .setFrozenQuota(new AmountDto(
                                "0", "GB",
                                "0", "GB",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .setProvided(new AmountDto(
                                "100", "GB",
                                "100000000000", "B",
                                "100", GIGABYTES,
                                "100", GIGABYTES
                        ))
                        .setProvidedRatio(new BigDecimal("0.1", new MathContext(4)))
                        .setAllocated(new AmountDto(
                                "100", "GB",
                                "100000000000", "B",
                                "100", GIGABYTES,
                                "100", GIGABYTES
                        ))
                        .setAllocatedRatio(new BigDecimal("0.1", new MathContext(4)))
                        .setProvidedAndNotAllocated(new AmountDto(
                                "0", "GB",
                                "0", "GB",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .buildExpandedResource(),
                new ExpandedResourceBuilder()
                        .setResourceId(YP_HDD_MYT)
                        .setQuota(new AmountDto(
                                "0", "GB",
                                "0", "GB",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .setBalance(new AmountDto(
                                "-80", "GB",
                                "-80000000000", "B",
                                "-80", GIGABYTES,
                                "-80", GIGABYTES
                        ))
                        .setNegativeBalance(new AmountDto(
                                "-80", "GB",
                                "-80000000000", "B",
                                "-80", GIGABYTES,
                                "-80", GIGABYTES
                        ))
                        .setFrozenQuota(new AmountDto(
                                "0", "GB",
                                "0", "GB",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .setProvided(new AmountDto(
                                "80", "GB",
                                "80000000000", "B",
                                "80", GIGABYTES,
                                "80", GIGABYTES
                        ))
                        .setProvidedRatio(BigDecimal.ZERO)
                        .setAllocated(new AmountDto(
                                "0", "GB",
                                "0", "GB",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .setAllocatedRatio(BigDecimal.ZERO)
                        .setProvidedAndNotAllocated(new AmountDto(
                                "80", "GB",
                                "80000000000", "B",
                                "80", GIGABYTES,
                                "80", GIGABYTES
                        ))
                        .buildExpandedResource()
        );
    }

    public static FrontAmountsDto getExpectedSumsYpHDD() {
        return new FrontAmountsDto(
                new AmountDto(
                        "1", "TB",
                        "1000000000000", "B",
                        "1", TERABYTES,
                        "1000", GIGABYTES
                ),
                new AmountDto(
                        "820", "GB",
                        "820000000000", "B",
                        "820", GIGABYTES,
                        "820", GIGABYTES
                ),
                new AmountDto(
                        "900", "GB",
                        "900000000000", "B",
                        "900", GIGABYTES,
                        "900", GIGABYTES
                ),
                new AmountDto(
                        "-80", "GB",
                        "-80000000000", "B",
                        "-80", GIGABYTES,
                        "-80", GIGABYTES
                ),
                new AmountDto(
                        "0", "GB",
                        "0", "GB",
                        "0", GIGABYTES,
                        "0", GIGABYTES
                ),
                new AmountDto(
                        "180", "GB",
                        "180000000000", "B",
                        "180", GIGABYTES,
                        "180", GIGABYTES
                ),
                new BigDecimal("0.18"),
                new AmountDto(
                        "100", "GB",
                        "100000000000", "B",
                        "100", GIGABYTES,
                        "100", GIGABYTES
                ),
                new BigDecimal("0.1")
        );
    }

    public static List<ExpandedResource> getExpectedResourcesYpSSD() {
        return List.of(
                new ExpandedResourceBuilder()
                        .setResourceId(YP_SSD_MAN)
                        .setQuota(new AmountDto(
                                "2", "TB",
                                "2000000000000", "B",
                                "2", TERABYTES,
                                "2000", GIGABYTES
                        ))
                        .setBalance(new AmountDto(
                                "1.99", "TB",
                                "1990000000000", "B",
                                "1.99", TERABYTES,
                                "1990", GIGABYTES
                        ))
                        .setPositiveBalance(new AmountDto(
                                "1.99", "TB",
                                "1990000000000", "B",
                                "1.99", TERABYTES,
                                "1990", GIGABYTES
                        ))
                        .setFrozenQuota(new AmountDto(
                                "0", "GB",
                                "0", "GB",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .setProvided(new AmountDto(
                                "10", "GB",
                                "10000000000", "B",
                                "10", GIGABYTES,
                                "10", GIGABYTES
                        ))
                        .setProvidedRatio(new BigDecimal("0.005", new MathContext(4)))
                        .setAllocated(new AmountDto(
                                "0", "GB",
                                "0", "GB",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .setAllocatedRatio(BigDecimal.valueOf(0L))
                        .setProvidedAndNotAllocated(new AmountDto(
                                "10", "GB",
                                "10000000000", "B",
                                "10", GIGABYTES,
                                "10", GIGABYTES
                        ))
                        .buildExpandedResource()
        );
    }

    public static FrontAmountsDto getExpectedSumsYpSSD() {
        return new FrontAmountsDto(
                new AmountDto(
                        "2", "TB",
                        "2000000000000", "B",
                        "2", TERABYTES,
                        "2000", GIGABYTES
                ),
                new AmountDto(
                        "1.99", "TB",
                        "1990000000000", "B",
                        "1.99", TERABYTES,
                        "1990", GIGABYTES
                ),
                new AmountDto(
                        "1.99", "TB",
                        "1990000000000", "B",
                        "1.99", TERABYTES,
                        "1990", GIGABYTES
                ),
                null,
                new AmountDto(
                        "0", "GB",
                        "0", "GB",
                        "0", GIGABYTES,
                        "0", GIGABYTES
                ),
                new AmountDto(
                        "10", "GB",
                        "10000000000", "B",
                        "10", GIGABYTES,
                        "10", GIGABYTES
                ),
                new BigDecimal("0.005"),
                new AmountDto(
                        "0", "GB",
                        "0", "GB",
                        "0", GIGABYTES,
                        "0", GIGABYTES
                ),
                new BigDecimal("0", new MathContext(1))
        );
    }

    /**
     * Update provisions incorrect response test.
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontQuotasController#updateProvision
     */
    @Test
    public void updateProvisionsIncorrectResponseTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(TestGrpcResponses.permissionDeniedTestResponse()));

        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        ErrorCollectionDto errorCollectionDto = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(errorCollectionDto);
        Assertions.assertEquals(Set.of("Error occurred while performing the operation on the provider side."),
                errorCollectionDto.getErrors());
        Assertions.assertNotNull(errorCollectionDto.getDetails());
        Assertions.assertEquals(1, errorCollectionDto.getDetails().get("errorFromProvider").size());
        String errorFromProvider = (String) errorCollectionDto.getDetails().get("errorFromProvider")
                .stream().findFirst().get();
        Assertions.assertTrue(errorFromProvider.startsWith("Test error\nRequest id:"));
    }

    @Test
    public void updateProvisionsConflictResponseNotCompletedTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(TestGrpcResponses.alreadyExistsTestResponse()));

        stubProviderService.setGetAccountResponses(List.of(GrpcResponse
                .success(Account.newBuilder()
                        .setAccountId(TEST_ACCOUNT_1_ID)
                        .setDeleted(false)
                        .setDisplayName("test")
                        .setFolderId(TEST_FOLDER_1_ID)
                        .setKey("test")
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
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
                                        .setValue(200)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(200)
                                        .setUnitKey("gigabytes")
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
                                                        .build())
                                        ))
                                .build())
                        .build())));

        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        ErrorCollectionDto errorCollectionDto = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(errorCollectionDto);
        Set<String> errors = errorCollectionDto.getErrors();
        Assertions.assertNotNull(errors);
        Assertions.assertEquals(Set.of("Error occurred while performing the operation on the provider side."), errors);
        Assertions.assertNotNull(errorCollectionDto.getDetails());
        Assertions.assertEquals(1, errorCollectionDto.getDetails().get("errorFromProvider").size());
        String errorFromProvider = (String) errorCollectionDto.getDetails().get("errorFromProvider")
                .stream().findFirst().get();
        Assertions.assertTrue(errorFromProvider.startsWith("""
                ALREADY_EXISTS Test error Test error
                test: Test error description
                Request id:"""));

        long updateProvisionCallCount = stubProviderService.getUpdateProvisionCallCount();
        Assertions.assertEquals(1, updateProvisionCallCount);

        long getAccountCallCount = stubProviderService.getGetAccountCallCount();
        Assertions.assertEquals(1, getAccountCallCount);
    }

    @Test
    public void updateProvisionsConflictResponseCompletedAfterRetryTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(TestGrpcResponses.alreadyExistsTestResponse()));

        stubProviderService.setGetAccountResponses(List.of(
                TestGrpcResponses.unavailableTestResponse(),
                GrpcResponse
                        .success(Account.newBuilder()
                                .setAccountId(TEST_ACCOUNT_1_ID)
                                .setDeleted(false)
                                .setDisplayName("test")
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setKey("test")
                                .addProvisions(Provision.newBuilder()
                                        .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("hdd")
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
                                                .setValue(100)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .setAllocated(Amount.newBuilder()
                                                .setValue(100)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .build())
                                .addProvisions(Provision.newBuilder()
                                        .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("ssd")
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
                                                .setValue(10)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .setAllocated(Amount.newBuilder()
                                                .setValue(0)
                                                .setUnitKey("gigabytes")
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

        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();

        long updateProvisionCallCount = stubProviderService.getUpdateProvisionCallCount();
        Assertions.assertEquals(1, updateProvisionCallCount);

        long getAccountCallCount = stubProviderService.getGetAccountCallCount();
        Assertions.assertEquals(2, getAccountCallCount);
    }

    @Test
    public void updateProvisionsConflictResponseCompletedTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(TestGrpcResponses.alreadyExistsTestResponse()));

        stubProviderService.setGetAccountResponses(List.of(GrpcResponse
                .success(Account.newBuilder()
                        .setAccountId(TEST_ACCOUNT_1_ID)
                        .setDeleted(false)
                        .setDisplayName("test")
                        .setFolderId(TEST_FOLDER_1_ID)
                        .setKey("test")
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
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
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("ssd")
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
                                        .setValue(10)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("gigabytes")
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

        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();

        long updateProvisionCallCount = stubProviderService.getUpdateProvisionCallCount();
        Assertions.assertEquals(1, updateProvisionCallCount);

        long getAccountCallCount = stubProviderService.getGetAccountCallCount();
        Assertions.assertEquals(1, getAccountCallCount);
    }
}
