package ru.yandex.intranet.d.grpc.resources;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestSegmentations;
import ru.yandex.intranet.d.backend.service.proto.CreateAccountsSpaceRequest;
import ru.yandex.intranet.d.backend.service.proto.CreateResourceSegmentRequest;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.FieldError;
import ru.yandex.intranet.d.backend.service.proto.FullAccountsSpace;
import ru.yandex.intranet.d.backend.service.proto.FullResourceSegment;
import ru.yandex.intranet.d.backend.service.proto.GetFullAccountsSpaceRequest;
import ru.yandex.intranet.d.backend.service.proto.ListFullAccountsSpacesByProviderRequest;
import ru.yandex.intranet.d.backend.service.proto.ListFullAccountsSpacesByProviderResponse;
import ru.yandex.intranet.d.backend.service.proto.MultilingualGrammaticalForms;
import ru.yandex.intranet.d.backend.service.proto.MultilingualGrammaticalForms.GrammaticalCases;
import ru.yandex.intranet.d.backend.service.proto.NewAccountsSpace;
import ru.yandex.intranet.d.backend.service.proto.NewResourceSegment;
import ru.yandex.intranet.d.backend.service.proto.ProviderUISettings;
import ru.yandex.intranet.d.backend.service.proto.ResourceSegmentIds;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementLimit;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementPageToken;
import ru.yandex.intranet.d.backend.service.proto.ResourcesManagementServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.SetAccountsSpaceReadOnlyRequest;
import ru.yandex.intranet.d.backend.service.proto.UpdateAccountsSpaceRequest;
import ru.yandex.intranet.d.backend.service.proto.UpdatedAccountsSpace;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsSpacesDao;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.datasource.model.WithTxId;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.model.accounts.AccountSpaceModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel;
import ru.yandex.intranet.d.utils.ErrorsHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_1_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_2;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_2_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_3_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_4_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_5_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_6_ID;
import static ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID;

/**
 * AccountsSpacesServiceTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 08.02.2021
 */
@IntegrationTest
public class AccountsSpacesManagementTest {
    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private ResourcesManagementServiceGrpc.ResourcesManagementServiceBlockingStub resourcesManagementService;
    @Autowired
    AccountsSpacesDao accountsSpacesDao;
    @Autowired
    ProvidersDao providersDao;
    @Autowired
    private YdbTableClient ydbTableClient;

    private static final AccountSpaceModel ACCOUNT_SPACE_MODEL = AccountSpaceModel.newBuilder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setNameEn("VLA default")
            .setNameRu("VLA default")
            .setDescriptionEn("VLA default")
            .setDescriptionRu("VLA default")
            .setProviderId(TestProviders.YDB_ID)
            .setOuterKeyInProvider(null)
            .setVersion(0)
            .setSegments(Set.of(
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("f29da801-01bd-4c1c-96d3-0dcd9aeb16ce")
                            .segmentId("e1c16d18-e9ec-4ed6-a9c0-342aac008e9a")
                            .build(),
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("4c820f85-1e72-4f4a-bf1a-81d4ce1c9bdc")
                            .segmentId("8bfe89a6-b2f6-4b75-a0e7-afee4fb37fa0")
                            .build()
            ))
            .build();

    /**
     * Get accounts space test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcResourcesManagementServiceImpl#getAccountsSpace
     */
    @Test
    public void getAccountsSpaceTest() {
        GetFullAccountsSpaceRequest accountsSpaceRequest = GetFullAccountsSpaceRequest.newBuilder()
                .setProviderId(TEST_ACCOUNT_SPACE_2.getProviderId())
                .setAccountsSpaceId(TEST_ACCOUNT_SPACE_2.getId())
                .build();
        FullAccountsSpace accountsSpace = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getAccountsSpace(accountsSpaceRequest);
        Assertions.assertNotNull(accountsSpace);
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getNameEn(), accountsSpace.getNameEn());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getDescriptionEn(), accountsSpace.getDescriptionEn());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getProviderId(), accountsSpace.getProviderId());
        Assertions.assertEquals("", accountsSpace.getKey());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getVersion(), accountsSpace.getVersion());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.isReadOnly(), accountsSpace.getReadOnly());
        Set<ResourceSegmentSettingsModel> segments =
                accountsSpace.getSegmentsList().stream().map(segment -> new ResourceSegmentSettingsModel(
                        segment.getSegmentationId(),
                        segment.getSegmentId()
                )).collect(Collectors.toSet());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getSegments(), segments);
    }

    /**
     * Get accounts space not found test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcResourcesManagementServiceImpl#getAccountsSpace
     */
    @Test
    public void getAccountsSpaceNotFoundTest() {
        GetFullAccountsSpaceRequest accountsSpaceRequest = GetFullAccountsSpaceRequest.newBuilder()
                .setProviderId(TestProviders.YP_ID)
                .setAccountsSpaceId("12345678-9012-3456-7890-123456789012")
                .build();
        try {
            //noinspection ResultOfMethodCallIgnored
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getAccountsSpace(accountsSpaceRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    /**
     * Get accounts spaces page test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcResourcesManagementServiceImpl#listAccountsSpacesByProvider
     */
    @Test
    public void getAccountsSpacesPageTest() {
        ListFullAccountsSpacesByProviderRequest accountsSpacesRequest = ListFullAccountsSpacesByProviderRequest
                .newBuilder()
                .setProviderId(TestProviders.YP_ID)
                .build();
        ListFullAccountsSpacesByProviderResponse page = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listAccountsSpacesByProvider(accountsSpacesRequest);
        Assertions.assertNotNull(page);
        Assertions.assertEquals(6, page.getAccountsSpacesCount());
        Optional<FullAccountsSpace> optionalAccountsSpace =
                page.getAccountsSpacesList().stream().filter(accountsSpace -> accountsSpace.getAccountsSpaceId()
                        .equals(TEST_ACCOUNT_SPACE_2.getId())).findFirst();
        Assertions.assertTrue(optionalAccountsSpace.isPresent());
        FullAccountsSpace accountsSpace = optionalAccountsSpace.get();
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getNameEn(), accountsSpace.getNameEn());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getDescriptionEn(), accountsSpace.getDescriptionEn());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getProviderId(), accountsSpace.getProviderId());
        Assertions.assertEquals("", accountsSpace.getKey());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getVersion(), accountsSpace.getVersion());
        Set<ResourceSegmentSettingsModel> segments =
                accountsSpace.getSegmentsList().stream().map(segment -> new ResourceSegmentSettingsModel(
                        segment.getSegmentationId(),
                        segment.getSegmentId()
                )).collect(Collectors.toSet());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getSegments(), segments);
        List<String> accountsSpaceIds = page.getAccountsSpacesList().stream().map(FullAccountsSpace::getAccountsSpaceId)
                .collect(Collectors.toList());
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_1_ID));
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_2_ID));
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_3_ID));
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_4_ID));
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_5_ID));
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_6_ID));
    }

    /**
     * Get two accounts spaces pages test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcResourcesManagementServiceImpl#listAccountsSpacesByProvider
     */
    @Test
    public void getAccountsSpacesTwoPagesTest() {
        ListFullAccountsSpacesByProviderRequest firstRequest = ListFullAccountsSpacesByProviderRequest.newBuilder()
                .setProviderId(TestProviders.YP_ID)
                .setLimit(ResourcesManagementLimit.newBuilder().setLimit(1L).build())
                .build();
        ListFullAccountsSpacesByProviderResponse firstPage = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listAccountsSpacesByProvider(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getAccountsSpacesCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());

        ListFullAccountsSpacesByProviderRequest secondRequest = ListFullAccountsSpacesByProviderRequest.newBuilder()
                .setProviderId(TestProviders.YP_ID)
                .setLimit(ResourcesManagementLimit.newBuilder().setLimit(1L).build())
                .setPageToken(ResourcesManagementPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        ListFullAccountsSpacesByProviderResponse secondPage = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listAccountsSpacesByProvider(secondRequest);
        Assertions.assertNotNull(secondPage);
        Assertions.assertEquals(1, secondPage.getAccountsSpacesCount());

        List<String> accountsSpaceIds = List.of(
                firstPage.getAccountsSpaces(0).getAccountsSpaceId(),
                secondPage.getAccountsSpaces(0).getAccountsSpaceId()
        );
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_5_ID));
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_2_ID));
    }

    /**
     * Accounts spaces page not found test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcResourcesManagementServiceImpl#listAccountsSpacesByProvider
     */
    @Test
    public void getAccountsSpacesPageNotFoundTest() {
        ListFullAccountsSpacesByProviderRequest accountsSpacesRequest = ListFullAccountsSpacesByProviderRequest
                .newBuilder()
                .setProviderId("dummy")
                .build();
        try {
            //noinspection ResultOfMethodCallIgnored
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .listAccountsSpacesByProvider(accountsSpacesRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    /**
     * Create accounts space test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcResourcesManagementServiceImpl#createAccountsSpace
     */
    @Test
    public void createAccountsSpaceTest() {
        CreateResourceSegmentRequest resourceSegmentRequest = CreateResourceSegmentRequest.newBuilder()
                .setResourceSegment(NewResourceSegment.newBuilder()
                        .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceSegment newSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegment(resourceSegmentRequest);

        CreateAccountsSpaceRequest accountsSpaceRequest = CreateAccountsSpaceRequest.newBuilder()
                .setAccountsSpace(NewAccountsSpace.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                                        .setSegmentId(newSegment.getResourceSegmentId())
                                        .build(),
                                ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                        .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                        .build()))
                        .build())
                .build();
        FullAccountsSpace newAccountsSpace = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createAccountsSpace(accountsSpaceRequest);
        Assertions.assertNotNull(newAccountsSpace);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", newAccountsSpace.getProviderId());

        NewAccountsSpace requestedAccountsSpace = accountsSpaceRequest.getAccountsSpace();
        GetFullAccountsSpaceRequest getRequest = GetFullAccountsSpaceRequest.newBuilder()
                .setProviderId(requestedAccountsSpace.getProviderId())
                .setAccountsSpaceId(newAccountsSpace.getAccountsSpaceId())
                .build();
        FullAccountsSpace accountsSpace = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getAccountsSpace(getRequest);
        Assertions.assertNotNull(accountsSpace);
        Assertions.assertEquals(requestedAccountsSpace.getNameEn(), accountsSpace.getNameEn());
        Assertions.assertEquals(requestedAccountsSpace.getDescriptionEn(), accountsSpace.getDescriptionEn());
        Assertions.assertEquals(requestedAccountsSpace.getProviderId(), accountsSpace.getProviderId());
        Assertions.assertEquals(requestedAccountsSpace.getKey(), accountsSpace.getKey());
        Assertions.assertEquals(0, accountsSpace.getVersion());
        assertThat(requestedAccountsSpace.getSegmentsList())
                .containsExactlyInAnyOrderElementsOf(accountsSpace.getSegmentsList());
    }

    /**
     * Create accounts space when already exists test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcResourcesManagementServiceImpl#createAccountsSpace
     */
    @Test
    public void createAccountsSpaceAlreadyExistsTest() {
        CreateAccountsSpaceRequest accountsSpaceRequest = CreateAccountsSpaceRequest.newBuilder()
                .setAccountsSpace(NewAccountsSpace.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                                        .setSegmentId("8f6a2b58-b10c-4742-bee6-b3587793b5e8")
                                        .build(),
                                ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                        .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                        .build()))
                        .build())
                .build();
        try {
            //noinspection ResultOfMethodCallIgnored
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .createAccountsSpace(accountsSpaceRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.ALREADY_EXISTS, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getFieldErrorsCount() > 0);
            FieldError error = details.get().getFieldErrors(0);
            Assertions.assertEquals("segments", error.getKey());
            Assertions.assertEquals("Accounts space already exists.", error.getErrors(0));
            return;
        }
        Assertions.fail();
    }

    /**
     * Update accounts space test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcResourcesManagementServiceImpl#updateAccountsSpace
     */
    @Test
    public void updateAccountsSpaceTest() {
        CreateResourceSegmentRequest resourceSegmentRequest = CreateResourceSegmentRequest.newBuilder()
                .setResourceSegment(NewResourceSegment.newBuilder()
                        .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceSegment newSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegment(resourceSegmentRequest);

        CreateAccountsSpaceRequest accountsSpaceRequest = CreateAccountsSpaceRequest.newBuilder()
                .setAccountsSpace(NewAccountsSpace.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                                        .setSegmentId(newSegment.getResourceSegmentId())
                                        .build(),
                                ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                        .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                        .build()))
                        .build())
                .build();
        FullAccountsSpace newAccountsSpace = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createAccountsSpace(accountsSpaceRequest);
        Assertions.assertNotNull(newAccountsSpace);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", newAccountsSpace.getProviderId());

        UpdateAccountsSpaceRequest updateAccountsSpaceRequest = UpdateAccountsSpaceRequest
                .newBuilder()
                .setAccountsSpace(UpdatedAccountsSpace.newBuilder()
                        .setNameEn("Test-1")
                        .setNameRu("Тест-1")
                        .setDescriptionEn("Test description-1")
                        .setDescriptionRu("Тестовое описание-1")
                        .build())
                .setAccountsSpaceId(newAccountsSpace.getAccountsSpaceId())
                .setVersion(newAccountsSpace.getVersion())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullAccountsSpace updatedAccountsSpace = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .updateAccountsSpace(updateAccountsSpaceRequest);
        Assertions.assertNotNull(updatedAccountsSpace);

        GetFullAccountsSpaceRequest getRequest = GetFullAccountsSpaceRequest.newBuilder()
                .setProviderId(newAccountsSpace.getProviderId())
                .setAccountsSpaceId(newAccountsSpace.getAccountsSpaceId())
                .build();
        FullAccountsSpace accountsSpace = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getAccountsSpace(getRequest);
        Assertions.assertNotNull(accountsSpace);

        UpdatedAccountsSpace requestedAccountsSpace = updateAccountsSpaceRequest.getAccountsSpace();
        Assertions.assertEquals(requestedAccountsSpace.getNameEn(), accountsSpace.getNameEn());
        Assertions.assertEquals(requestedAccountsSpace.getDescriptionEn(), accountsSpace.getDescriptionEn());
        Assertions.assertEquals(requestedAccountsSpace.getNameRu(), accountsSpace.getNameRu());
        Assertions.assertEquals(requestedAccountsSpace.getDescriptionRu(), accountsSpace.getDescriptionRu());
    }

    /**
     * Set accounts space read only test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcResourcesManagementServiceImpl#setAccountsSpaceReadOnly
     */
    @Test
    public void setAccountsSpaceReadOnlyTest() {
        CreateResourceSegmentRequest resourceSegmentRequest = CreateResourceSegmentRequest.newBuilder()
                .setResourceSegment(NewResourceSegment.newBuilder()
                        .setResourceSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .build())
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .build();
        FullResourceSegment newSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegment(resourceSegmentRequest);

        CreateAccountsSpaceRequest accountsSpaceRequest = CreateAccountsSpaceRequest.newBuilder()
                .setAccountsSpace(NewAccountsSpace.newBuilder()
                        .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                        .setKey("test")
                        .setNameEn("Test")
                        .setNameRu("Тест")
                        .setDescriptionEn("Test description")
                        .setDescriptionRu("Тестовое описание")
                        .setReadOnly(false)
                        .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                                        .setSegmentId(newSegment.getResourceSegmentId())
                                        .build(),
                                ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                        .setSegmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                        .build()))
                        .build())
                .build();
        FullAccountsSpace newAccountsSpace = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createAccountsSpace(accountsSpaceRequest);
        Assertions.assertNotNull(newAccountsSpace);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", newAccountsSpace.getProviderId());

        //noinspection ResultOfMethodCallIgnored
        resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .setAccountsSpaceReadOnly(SetAccountsSpaceReadOnlyRequest.newBuilder()
                        .setAccountsSpaceId(newAccountsSpace.getAccountsSpaceId())
                        .setProviderId(newAccountsSpace.getProviderId())
                        .setReadOnly(true)
                        .build());

        GetFullAccountsSpaceRequest getRequest = GetFullAccountsSpaceRequest.newBuilder()
                .setProviderId(newAccountsSpace.getProviderId())
                .setAccountsSpaceId(newAccountsSpace.getAccountsSpaceId())
                .build();
        FullAccountsSpace accountsSpace = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getAccountsSpace(getRequest);
        Assertions.assertNotNull(accountsSpace);
        Assertions.assertTrue(accountsSpace.getReadOnly());
    }

    private void upsertNewAccountSpace() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsSpacesDao.upsertOneTxRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        ACCOUNT_SPACE_MODEL
                )
        ).block();

        WithTxId<Optional<AccountSpaceModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsSpacesDao.getByIdStartTx(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        ACCOUNT_SPACE_MODEL.getId(),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(ACCOUNT_SPACE_MODEL, res.get().get());
    }

    @Test
    public void createAccountsSpaceAlreadyExistsWithRaceTest() {
        Optional<ProviderModel> ydbO = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestProviders.YDB_ID, DEFAULT_TENANT_ID))
                .block();
        Assertions.assertTrue(ydbO != null && ydbO.isPresent());
        ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.updateProviderRetryable(session.asTxCommitRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE), ProviderModel.builder(ydbO.get())
                        .accountsSpacesSupported(true).build())).block();

        CreateAccountsSpaceRequest accountsSpaceRequestSas = CreateAccountsSpaceRequest.newBuilder()
                .setAccountsSpace(NewAccountsSpace.newBuilder()
                        .setProviderId(TestProviders.YDB_ID)
                        .setKey("test2")
                        .setNameEn("SAS default")
                        .setNameRu("SAS default")
                        .setDescriptionEn("SAS default")
                        .setDescriptionRu("SAS default")
                        .setReadOnly(false)
                        .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("f29da801-01bd-4c1c-96d3-0dcd9aeb16ce")
                                        .setSegmentId("f701d159-e6c1-46a2-a254-de6adb6c1c8e")
                                        .build(),
                                ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("4c820f85-1e72-4f4a-bf1a-81d4ce1c9bdc")
                                        .setSegmentId("8bfe89a6-b2f6-4b75-a0e7-afee4fb37fa0")
                                        .build()))
                        .build())
                .build();

        //noinspection ResultOfMethodCallIgnored
        resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YDB_SOURCE_TVM_ID))
                .createAccountsSpace(accountsSpaceRequestSas);

        upsertNewAccountSpace();

        CreateAccountsSpaceRequest accountsSpaceRequestVla = CreateAccountsSpaceRequest.newBuilder()
                .setAccountsSpace(NewAccountsSpace.newBuilder()
                        .setProviderId(TestProviders.YDB_ID)
                        .setKey("test")
                        .setNameEn("VLA default")
                        .setNameRu("VLA default")
                        .setDescriptionEn("VLA default")
                        .setDescriptionRu("VLA default")
                        .setReadOnly(false)
                        .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("f29da801-01bd-4c1c-96d3-0dcd9aeb16ce")
                                        .setSegmentId("e1c16d18-e9ec-4ed6-a9c0-342aac008e9a")
                                        .build(),
                                ResourceSegmentIds.newBuilder()
                                        .setSegmentationId("4c820f85-1e72-4f4a-bf1a-81d4ce1c9bdc")
                                        .setSegmentId("8bfe89a6-b2f6-4b75-a0e7-afee4fb37fa0")
                                        .build()))
                        .build())
                .build();
        try {
            //noinspection ResultOfMethodCallIgnored
            resourcesManagementService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YDB_SOURCE_TVM_ID))
                    .createAccountsSpace(accountsSpaceRequestVla);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.ALREADY_EXISTS, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getFieldErrorsCount() > 0);
            FieldError error = details.get().getFieldErrors(0);
            Assertions.assertEquals("segments", error.getKey());
            Assertions.assertEquals("Accounts space already exists.", error.getErrors(0));
            return;
        }
        Assertions.fail();
    }

    /**
     * Create accounts space test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcResourcesManagementServiceImpl#createAccountsSpace
     */
    @Test
    public void createAccountsSpaceWithUiSettingsTest() {
        FullResourceSegment newSegment = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createResourceSegment(CreateResourceSegmentRequest.newBuilder()
                        .setResourceSegment(NewResourceSegment.newBuilder()
                                .setResourceSegmentationId(TestSegmentations.YP_LOCATION)
                                .setKey("test")
                                .setNameEn("Test")
                                .setNameRu("Тест")
                                .setDescriptionEn("Test description")
                                .setDescriptionRu("Тестовое описание")
                                .build())
                        .setProviderId(TestProviders.YP_ID)
                        .build()
                );

        FullAccountsSpace newAccountsSpace = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .createAccountsSpace(CreateAccountsSpaceRequest.newBuilder()
                        .setAccountsSpace(NewAccountsSpace.newBuilder()
                                .setProviderId(TestProviders.YP_ID)
                                .setKey("test")
                                .setNameEn("Test")
                                .setNameRu("Тест")
                                .setDescriptionEn("Test description")
                                .setDescriptionRu("Тестовое описание")
                                .setReadOnly(false)
                                .addAllSegments(List.of(ResourceSegmentIds.newBuilder()
                                                .setSegmentationId(TestSegmentations.YP_LOCATION)
                                                .setSegmentId(newSegment.getResourceSegmentId())
                                                .build(),
                                        ResourceSegmentIds.newBuilder()
                                                .setSegmentationId(TestSegmentations.YP_SEGMENT)
                                                .setSegmentId(TestSegmentations.YP_SEGMENT_DEFAULT)
                                                .build()))
                                .setUiSettings(ProviderUISettings.newBuilder()
                                        .setTitleForTheAccount(MultilingualGrammaticalForms.newBuilder()
                                                .setNameSingularRu(GrammaticalCases.newBuilder()
                                                        .setNominative("Пул")
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build());
        Assertions.assertNotNull(newAccountsSpace);
        Assertions.assertEquals("Пул",
                newAccountsSpace.getUiSettings().getTitleForTheAccount().getNameSingularRu().getNominative());

        FullAccountsSpace accountsSpace = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getAccountsSpace(GetFullAccountsSpaceRequest.newBuilder()
                        .setProviderId(TestProviders.YP_ID)
                        .setAccountsSpaceId(newAccountsSpace.getAccountsSpaceId())
                        .build());
        Assertions.assertNotNull(accountsSpace);
        Assertions.assertEquals("Пул",
                accountsSpace.getUiSettings().getTitleForTheAccount().getNameSingularRu().getNominative());

        FullAccountsSpace updatedAccountsSpace = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .updateAccountsSpace(UpdateAccountsSpaceRequest
                        .newBuilder()
                        .setAccountsSpace(UpdatedAccountsSpace.newBuilder()
                                .setNameEn(accountsSpace.getNameEn())
                                .setNameRu(accountsSpace.getNameRu())
                                .setDescriptionEn(accountsSpace.getDescriptionEn())
                                .setDescriptionRu(accountsSpace.getDescriptionRu())
                                .setUiSettings(ProviderUISettings.newBuilder(accountsSpace.getUiSettings())
                                        .setTitleForTheAccount(MultilingualGrammaticalForms
                                                .newBuilder(accountsSpace.getUiSettings().getTitleForTheAccount())
                                                .setNameSingularRu(GrammaticalCases
                                                        .newBuilder(accountsSpace.getUiSettings()
                                                                .getTitleForTheAccount().getNameSingularRu())
                                                        .setAccusative("Пула")
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .setAccountsSpaceId(newAccountsSpace.getAccountsSpaceId())
                        .setVersion(newAccountsSpace.getVersion())
                        .setProviderId(accountsSpace.getProviderId())
                        .build());
        Assertions.assertNotNull(updatedAccountsSpace);
        Assertions.assertEquals("Пул",
                updatedAccountsSpace.getUiSettings().getTitleForTheAccount().getNameSingularRu().getNominative());
        Assertions.assertEquals("Пула",
                updatedAccountsSpace.getUiSettings().getTitleForTheAccount().getNameSingularRu().getAccusative());

        FullAccountsSpace accountsSpace2 = resourcesManagementService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getAccountsSpace(GetFullAccountsSpaceRequest.newBuilder()
                        .setProviderId(TestProviders.YP_ID)
                        .setAccountsSpaceId(newAccountsSpace.getAccountsSpaceId())
                        .build());
        Assertions.assertNotNull(accountsSpace2);
        Assertions.assertEquals("Пул",
                accountsSpace2.getUiSettings().getTitleForTheAccount().getNameSingularRu().getNominative());
        Assertions.assertEquals("Пула",
                accountsSpace2.getUiSettings().getTitleForTheAccount().getNameSingularRu().getAccusative());
    }
}
