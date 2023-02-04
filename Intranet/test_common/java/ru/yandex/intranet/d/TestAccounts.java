package ru.yandex.intranet.d;

import java.time.Instant;
import java.util.Set;

import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.model.GrammaticalCases;
import ru.yandex.intranet.d.model.MultilingualGrammaticalForms;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountSpaceModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel.OperationType;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel.RequestStatus;
import ru.yandex.intranet.d.model.accounts.OperationChangesModel;
import ru.yandex.intranet.d.model.accounts.OperationErrorKind;
import ru.yandex.intranet.d.model.accounts.OperationOrdersModel;
import ru.yandex.intranet.d.model.accounts.OperationSource;
import ru.yandex.intranet.d.model.providers.ProviderUISettings;
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel;

/**
 * TestAccounts.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 19.10.2020
 */
public class TestAccounts {
    public static final String TEST_ACCOUNT_SPACE_1_ID = "ea5c58c9-72d1-4b84-baac-33067092e87e";
    public static final String TEST_ACCOUNT_SPACE_2_ID = "978bd75a-cf67-44ac-b944-e8ca949bdf7e";
    public static final String TEST_ACCOUNT_SPACE_3_ID = "9c44cf69-76c5-45a3-9335-57e2669f03ff";
    public static final String TEST_ACCOUNT_SPACE_4_ID = "e8d7fc08-52e6-4733-8303-ef0efc73905c";
    public static final String TEST_ACCOUNT_SPACE_5_ID = "53ec7196-bd8f-480d-80d8-c6195f0d6395";
    public static final String TEST_ACCOUNT_SPACE_6_ID = "ad934bd4-94ac-4665-967f-975f0b7c788c";

    public static final String TEST_ACCOUNT_1_ID = "56a41608-84df-41c4-9653-89106462e0ce";
    public static final String TEST_ACCOUNT_3_ID = "2fa47267-6a83-4c21-9f1c-c4d1e9ca9e58";
    public static final String TEST_ACCOUNT_4_ID = "0cc2d776-a8f3-497d-8eff-c813da57fc04";
    public static final String TEST_ACCOUNT_5_ID = "8cb97894-8d30-4869-b68a-58110b0b221b"; // без квот
    public static final String TEST_ACCOUNT_6_ID = "33e6abe9-6292-4143-89c2-c75a44c6d65c";
    public static final String TEST_ACCOUNT_IN_CLOSING_SERVICE = "bab63272-644a-42f9-b9a5-9eeb4900fe9d";
    public static final String TEST_ACCOUNT_IN_NON_EXPORTABLE_SERVICE = "920067f2-a94f-46e9-a1c0-299b13747650";
    public static final String TEST_ACCOUNT_IN_RENAMING_SERVICE = "0c963e32-ffe7-4789-a4d3-1821910bf687";
    public static final String TEST_ACCOUNT_7_ID = "e18b9566-b55b-464c-abbd-8633bdd32b15";
    public static final String TEST_ACCOUNT_17_ID = "5377922e-b309-4b95-ae1e-c0da6ccd064c"; // default quotas provider

    public static final String TEST_ACCOUNT_8_ID = "2e07ed56-6ac5-41ed-94f7-5898b18de5d3";
    public static final String TEST_ACCOUNT_WITH_ONLY_VIRTUAL_RESOURCES_ID = "4f1e2cb9-7d0b-7a14-809b-1722adebda14";
    public static final String TEST_ACCOUNT_WITH_NON_VIRTUAL_RESOURCES_ID = "b3e45802-8f87-a958-28bc-3ce9e9a8823c";
    public static final String TEST_UNMANAGED_PROVIDER_ACCOUNT_ID =  "fe2a3108a1f7a27e22f038e7491510d6";
    public static final String TEST_ACCOUNT_10_ID = "3ae91e3a-33ef-6b47-63f0-747553eba40a";
    public static final String TEST_YT_POOL_ID = "60b96cfa-2d98-490d-8e3f-3c509b2cb5a4";


    public static final AccountModel TEST_ACCOUNT_1 = new AccountModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(TEST_ACCOUNT_1_ID)
            .setVersion(1)
            .setDeleted(false)
            .setDisplayName("тестовый аккаунт")
            .setProviderId(TestProviders.YP_ID)
            .setOuterAccountIdInProvider("123")
            .setOuterAccountKeyInProvider("dummy")
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setLastAccountUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
            .setLastReceivedVersion(1L)
            .setLatestSuccessfulAccountOperationId(null)
            .setAccountsSpacesId(TEST_ACCOUNT_SPACE_3_ID)
            .build();
    public static final String TEST_ACCOUNT_2_ID = "9567ae7c-9b76-44bc-87c7-e18d998778b3";
    public static final AccountModel TEST_ACCOUNT_2 = new AccountModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(TEST_ACCOUNT_2_ID)
            .setVersion(1)
            .setDeleted(false)
            .setDisplayName("тестовый аккаунт 2")
            .setProviderId(TestProviders.YDB_ID)
            .setOuterAccountIdInProvider("1234")
            .setOuterAccountKeyInProvider("dummy2")
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setLastAccountUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
            .setLastReceivedVersion(1L)
            .setLatestSuccessfulAccountOperationId(null)
            .build();

    public static final AccountModel TEST_ACCOUNT_3 = new AccountModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(TEST_ACCOUNT_3_ID)
            .setVersion(1)
            .setDeleted(true)
            .setDisplayName("тестовый аккаунт 3")
            .setProviderId(TestProviders.YDB_ID)
            .setOuterAccountIdInProvider("12345")
            .setOuterAccountKeyInProvider("dummy3")
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setLastAccountUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
            .setLastReceivedVersion(1L)
            .setLatestSuccessfulAccountOperationId(null)
            .setAccountsSpacesId(null)
            .build();

    public static final AccountModel DELETED_ACCOUNT = TEST_ACCOUNT_3;

    public static final AccountModel TEST_ACCOUNT_5 = new AccountModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(TEST_ACCOUNT_5_ID)
            .setVersion(1)
            .setDeleted(false)
            .setDisplayName("тестовый аккаунт 5")
            .setProviderId(TestProviders.YDB_ID)
            .setOuterAccountIdInProvider("123456")
            .setOuterAccountKeyInProvider("dummy5")
            .setFolderId(TestFolders.TEST_FOLDER_4_ID)
            .setLastAccountUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
            .setLastReceivedVersion(1L)
            .setLatestSuccessfulAccountOperationId(null)
            .setAccountsSpacesId(null)
            .build();

    public static final AccountModel TEST_ACCOUNT_6 = new AccountModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(TEST_ACCOUNT_6_ID)
            .setVersion(1)
            .setDeleted(false)
            .setDisplayName("тестовый аккаунт 6")
            .setProviderId(TestProviders.YP_ID)
            .setOuterAccountIdInProvider("123456")
            .setOuterAccountKeyInProvider("dummy6")
            .setFolderId(TestFolders.TEST_FOLDER_2_ID)
            .setLastAccountUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
            .setLastReceivedVersion(1L)
            .setLatestSuccessfulAccountOperationId(null)
            .setAccountsSpacesId(TEST_ACCOUNT_SPACE_3_ID)
            .build();

    public static final AccountModel TEST_ACCOUNT_7 = new AccountModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId("b17b65db-aff1-4c02-8b9a-7330632066c6")
            .setVersion(1)
            .setDeleted(false)
            .setDisplayName("YDB account")
            .setProviderId(TestProviders.YDB_ID)
            .setOuterAccountIdInProvider("8498y4292")
            .setOuterAccountKeyInProvider("acc8498y4292")
            .setFolderId(TestFolders.TEST_FOLDER_6_ID)
            .setLastAccountUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
            .setLastReceivedVersion(1L)
            .setLatestSuccessfulAccountOperationId(null)
            .setAccountsSpacesId(null)
            .build();

    public static final AccountsQuotasModel TEST_ACCOUNT_1_QUOTA_1 = new AccountsQuotasModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setAccountId(TEST_ACCOUNT_1_ID)
            .setResourceId(TestResources.YP_HDD_MAN)
            .setProvidedQuota(200000000000L)
            .setAllocatedQuota(100000000000L)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setProviderId(TestProviders.YP_ID)
            .setLastProvisionUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
            .setLastReceivedProvisionVersion(1L)
            .setLatestSuccessfulProvisionOperationId(null)
            .build();
    public static final AccountsQuotasModel TEST_ACCOUNT_2_QUOTA_1 = new AccountsQuotasModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setAccountId(TEST_ACCOUNT_2_ID)
            .setResourceId(TestResources.YDB_RAM_SAS)
            .setProvidedQuota(25L)
            .setAllocatedQuota(15L)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setProviderId(TestProviders.YDB_ID)
            .setLastProvisionUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
            .setLastReceivedProvisionVersion(1L)
            .setLatestSuccessfulProvisionOperationId(null)
            .build();

    public static final AccountsQuotasOperationsModel TEST_ACCOUNTS_QUOTAS_OPERATIONS_1 =
            new AccountsQuotasOperationsModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setOperationId("273c0fee-5cf1-4083-b9dc-8ec0e855e150")
                    .setLastRequestId("3223343")
                    .setCreateDateTime(Instant.ofEpochSecond(1603385085))
                    .setOperationSource(OperationSource.USER)
                    .setOperationType(OperationType.DELETE_ACCOUNT)
                    .setAuthorUserId(TestUsers.USER_1_ID)
                    .setAuthorUserUid(null)
                    .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                    .setAccountsSpaceId(null)
                    .setUpdateDateTime(Instant.ofEpochSecond(1603385085))
                    .setRequestStatus(RequestStatus.OK)
                    .setErrorMessage(null)
                    .setFullErrorMessage(null)
                    .setRequestedChanges(OperationChangesModel.builder()
                            .accountId("56a41608-84df-41c4-9653-89106462e0ce")
                            .build())
                    .setOrders(OperationOrdersModel.builder().submitOrder(1).build())
                    .setErrorKind(OperationErrorKind.UNKNOWN)
                    .build();
    public static final AccountsQuotasOperationsModel TEST_ACCOUNTS_QUOTAS_OPERATIONS_2 =
            new AccountsQuotasOperationsModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setOperationId("481ab698-24ad-4c11-a7b0-3dbbbae0f3c1")
                    .setLastRequestId("79868689")
                    .setCreateDateTime(Instant.ofEpochSecond(1603385086))
                    .setOperationSource(OperationSource.USER)
                    .setOperationType(OperationType.DELETE_ACCOUNT)
                    .setAuthorUserId(TestUsers.USER_1_ID)
                    .setAuthorUserUid(null)
                    .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                    .setAccountsSpaceId(null)
                    .setUpdateDateTime(Instant.ofEpochSecond(1603385086))
                    .setRequestStatus(RequestStatus.OK)
                    .setErrorMessage(null)
                    .setFullErrorMessage(null)
                    .setRequestedChanges(OperationChangesModel.builder()
                            .accountId("56a41608-84df-41c4-9653-89106462e0ce")
                            .build())
                    .setOrders(OperationOrdersModel.builder().submitOrder(1).build())
                    .build();

    public static final AccountSpaceModel TEST_ACCOUNT_SPACE_1 = AccountSpaceModel.newBuilder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(TEST_ACCOUNT_SPACE_1_ID)
            .setDeleted(false)
            .setNameEn("VLA default")
            .setNameRu("VLA default")
            .setDescriptionEn("VLA default")
            .setDescriptionRu("VLA default")
            .setProviderId(TestProviders.YP_ID)
            .setOuterKeyInProvider(null)
            .setVersion(0)
            .setSegments(Set.of(
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                            .segmentId("8f6a2b58-b10c-4742-bee6-b3587793b5e8")
                            .build(),
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                            .segmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                            .build()
            ))
            .build();

    public static final AccountSpaceModel TEST_ACCOUNT_SPACE_2 = AccountSpaceModel.newBuilder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(TEST_ACCOUNT_SPACE_2_ID)
            .setDeleted(false)
            .setNameEn("SAS default")
            .setNameRu("SAS default")
            .setDescriptionEn("SAS default")
            .setDescriptionRu("SAS default")
            .setProviderId(TestProviders.YP_ID)
            .setOuterKeyInProvider(null)
            .setVersion(0)
            .setSegments(Set.of(
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                            .segmentId("540b6c0f-d0b9-4f62-9b75-af7c9cfd2e95")
                            .build(),
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                            .segmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                            .build()
            ))
            .build();

    public static final AccountSpaceModel TEST_ACCOUNT_SPACE_3 = AccountSpaceModel.newBuilder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(TEST_ACCOUNT_SPACE_3_ID)
            .setDeleted(false)
            .setNameEn("MAN default")
            .setNameRu("MAN default")
            .setDescriptionEn("MAN default")
            .setDescriptionRu("MAN default")
            .setProviderId(TestProviders.YP_ID)
            .setOuterKeyInProvider(null)
            .setVersion(0)
            .setSegments(Set.of(
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                            .segmentId("8691406c-2e5f-4873-8bdf-f0bf99ed9bea")
                            .build(),
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                            .segmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                            .build()
            ))
            .build();

    public static final AccountSpaceModel TEST_ACCOUNT_SPACE_4 = AccountSpaceModel.newBuilder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(TEST_ACCOUNT_SPACE_4_ID)
            .setDeleted(false)
            .setNameEn("MYT default")
            .setNameRu("MYT default")
            .setDescriptionEn("MYT default")
            .setDescriptionRu("MYT default")
            .setProviderId(TestProviders.YP_ID)
            .setOuterKeyInProvider(null)
            .setVersion(0)
            .setSegments(Set.of(
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                            .segmentId("39bcc913-61a2-449f-9d07-2600f26df142")
                            .build(),
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                            .segmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                            .build()
            ))
            .build();

    public static final AccountSpaceModel TEST_ACCOUNT_SPACE_5 = AccountSpaceModel.newBuilder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(TEST_ACCOUNT_SPACE_5_ID)
            .setDeleted(false)
            .setNameEn("IVA default")
            .setNameRu("IVA default")
            .setDescriptionEn("IVA default")
            .setDescriptionRu("IVA default")
            .setProviderId(TestProviders.YP_ID)
            .setOuterKeyInProvider(null)
            .setVersion(0)
            .setSegments(Set.of(
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                            .segmentId("de536d75-04fc-4fa2-bf1c-a382616a4f0c")
                            .build(),
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                            .segmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                            .build()
            ))
            .setUiSettings(new ProviderUISettings(new MultilingualGrammaticalForms(
                    new GrammaticalCases(
                            "Пул", null, null, null, null, null, null
                    ), null, null, null
            )))
            .build();

    public static final AccountSpaceModel TEST_ACCOUNT_SPACE_6 = AccountSpaceModel.newBuilder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(TEST_ACCOUNT_SPACE_6_ID)
            .setDeleted(false)
            .setNameEn("MAN2 default")
            .setNameRu("MAN2 default")
            .setDescriptionEn("MAN2 default")
            .setDescriptionRu("MAN2 default")
            .setProviderId(TestProviders.YP_ID)
            .setOuterKeyInProvider(null)
            .setVersion(0)
            .setSegments(Set.of(
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("37e28d62-1462-46dc-8989-c2ebf44cde00")
                            .segmentId("9051becd-7e21-4e94-9015-80ba738c0a0d")
                            .build(),
                    ResourceSegmentSettingsModel.builder()
                            .segmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                            .segmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                            .build()
            ))
            .build();

    private TestAccounts() {
    }
}
