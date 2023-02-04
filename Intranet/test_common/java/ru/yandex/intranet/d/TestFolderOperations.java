package ru.yandex.intranet.d;

import java.time.Instant;
import java.util.Map;

import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.model.folders.FolderHistoryFieldsModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.folders.FolderOperationType;
import ru.yandex.intranet.d.model.folders.ProvisionHistoryModel;
import ru.yandex.intranet.d.model.folders.ProvisionsByResource;
import ru.yandex.intranet.d.model.folders.QuotasByAccount;
import ru.yandex.intranet.d.model.folders.QuotasByResource;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1;
import static ru.yandex.intranet.d.TestResources.YDB_RAM_SAS;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;

/**
 * TestFolderOperations.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 26.10.2020
 */
public class TestFolderOperations {
    public static final String TEST_FOLDER_OPERATION_LOG_1_ID = "583c265c-da59-4764-8278-ec1f01d5478c";
    public static final String TEST_FOLDER_OPERATION_LOG_2_ID = "66b0ce87-4271-4258-82f2-2ff9f733c252";
    public static final FolderOperationLogModel TEST_FOLDER_OPERATION_LOG_1 = new FolderOperationLogModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setOperationDateTime(Instant.ofEpochSecond(1603385085))
            .setId(TEST_FOLDER_OPERATION_LOG_1_ID)
            .setProviderRequestId("123")
            .setOperationType(FolderOperationType.QUOTA_TRANSFER)
            .setAuthorUserId(TestUsers.USER_1_ID)
            .setAuthorUserUid(TestUsers.USER_1_UID)
            .setAuthorProviderId(null)
            .setSourceFolderOperationsLogId(null)
            .setDestinationFolderOperationsLogId(TEST_FOLDER_OPERATION_LOG_2_ID)
            .setOldFolderFields(FolderHistoryFieldsModel.builder().displayName("old name").build())
            .setNewFolderFields(FolderHistoryFieldsModel.builder().displayName(TEST_FOLDER_1.getDisplayName()).build())
            .setOldQuotas(new QuotasByResource(Map.of(
                    YP_HDD_MAN, 10L,
                    YDB_RAM_SAS, 20L
            )))
            .setOldBalance(new QuotasByResource(Map.of()))
            .setOldProvisions(new QuotasByAccount(Map.of(
                    TEST_ACCOUNT_1_ID, new ProvisionsByResource(Map.of(
                            YP_HDD_MAN, new ProvisionHistoryModel(8L, null)
                    ))
            )))
            .setOldAccounts(null)
            .setNewQuotas(new QuotasByResource(Map.of(
                    YP_HDD_MAN, 10L,
                    YDB_RAM_SAS, 15L
            )))
            .setNewBalance(new QuotasByResource(Map.of()))
            .setNewProvisions(new QuotasByAccount(Map.of(
                    TEST_ACCOUNT_1_ID, new ProvisionsByResource(Map.of(
                            YP_HDD_MAN, new ProvisionHistoryModel(8L, null)
                    ))
            )))
            .setNewAccounts(null)
            .setActuallyAppliedProvisions(null)
            .setAccountsQuotasOperationsId(null)
            .setQuotasDemandsId(null)
            .setOrder(1L)
            .build();

    public static final FolderOperationLogModel TEST_FOLDER_OPERATION_LOG_2 = new FolderOperationLogModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setFolderId(TestFolders.TEST_FOLDER_2_ID)
            .setOperationDateTime(Instant.ofEpochSecond(1603385085))
            .setId(TEST_FOLDER_OPERATION_LOG_2_ID)
            .setProviderRequestId("123")
            .setOperationType(FolderOperationType.QUOTA_TRANSFER)
            .setAuthorUserId(TestUsers.USER_1_ID)
            .setAuthorUserUid(TestUsers.USER_1_UID)
            .setAuthorProviderId(null)
            .setSourceFolderOperationsLogId(TEST_FOLDER_OPERATION_LOG_1_ID)
            .setDestinationFolderOperationsLogId(null)
            .setOldFolderFields(FolderHistoryFieldsModel.builder().displayName("old name").build())
            .setNewFolderFields(FolderHistoryFieldsModel.builder().displayName(TEST_FOLDER_1.getDisplayName()).build())
            .setOldQuotas(new QuotasByResource(Map.of(
                    YP_HDD_MAN, 10L,
                    YDB_RAM_SAS, 10L
            )))
            .setOldBalance(new QuotasByResource(Map.of()))
            .setOldProvisions(new QuotasByAccount(Map.of(
                    TEST_ACCOUNT_1_ID, new ProvisionsByResource(Map.of(
                            YP_HDD_MAN, new ProvisionHistoryModel(8L, null)
                    ))
            )))
            .setOldAccounts(null)
            .setNewQuotas(new QuotasByResource(Map.of(
                    YP_HDD_MAN, 10L,
                    YDB_RAM_SAS, 15L
            )))
            .setNewBalance(new QuotasByResource(Map.of()))
            .setNewProvisions(new QuotasByAccount(Map.of(
                    TEST_ACCOUNT_1_ID, new ProvisionsByResource(Map.of(
                            YP_HDD_MAN, new ProvisionHistoryModel(8L, null)
                    ))
            )))
            .setNewAccounts(null)
            .setActuallyAppliedProvisions(null)
            .setAccountsQuotasOperationsId(null)
            .setQuotasDemandsId(null)
            .setOrder(2L)
            .build();

    public static final FolderOperationLogModel TEST_FOLDER_OPERATION_LOG_NEW = new FolderOperationLogModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setOperationDateTime(Instant.ofEpochSecond(1603385086))
            .setId("94ff8d60-0120-4d02-a168-a8edec4b0b1f")
            .setProviderRequestId("123")
            .setOperationType(FolderOperationType.QUOTA_TRANSFER)
            .setAuthorUserId(TestUsers.USER_1_ID)
            .setAuthorUserUid(TestUsers.USER_1_UID)
            .setAuthorProviderId(null)
            .setSourceFolderOperationsLogId(null)
            .setDestinationFolderOperationsLogId(null)
            .setOldFolderFields(FolderHistoryFieldsModel.builder().displayName("old name").build())
            .setNewFolderFields(FolderHistoryFieldsModel.builder().displayName(TEST_FOLDER_1.getDisplayName()).build())
            .setOldQuotas(new QuotasByResource(Map.of(
                    YP_HDD_MAN, 10L,
                    YDB_RAM_SAS, 20L
            )))
            .setOldBalance(new QuotasByResource(Map.of()))
            .setOldProvisions(new QuotasByAccount(Map.of(
                    TEST_ACCOUNT_1_ID, new ProvisionsByResource(Map.of(
                            YP_HDD_MAN, new ProvisionHistoryModel(8L, null)
                    ))
            )))
            .setOldAccounts(null)
            .setNewQuotas(new QuotasByResource(Map.of(
                    YP_HDD_MAN, 10L,
                    YDB_RAM_SAS, 15L
            )))
            .setNewBalance(new QuotasByResource(Map.of()))
            .setNewProvisions(new QuotasByAccount(Map.of(
                    TEST_ACCOUNT_1_ID, new ProvisionsByResource(Map.of(
                            YP_HDD_MAN, new ProvisionHistoryModel(8L, null)
                    ))
            )))
            .setNewAccounts(null)
            .setActuallyAppliedProvisions(null)
            .setAccountsQuotasOperationsId(null)
            .setQuotasDemandsId(null)
            .setOrder(3L)
            .build();


    public static final FolderOperationLogModel TEST_FOLDER_OPERATION_LOG_NEW_2 = new FolderOperationLogModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setOperationDateTime(Instant.ofEpochSecond(1603385087))
            .setId("a66f19d2-2df5-4c06-a772-99d262341762")
            .setProviderRequestId("123")
            .setOperationType(FolderOperationType.QUOTA_TRANSFER)
            .setAuthorUserId(TestUsers.USER_1_ID)
            .setAuthorUserUid(TestUsers.USER_1_UID)
            .setAuthorProviderId(null)
            .setSourceFolderOperationsLogId(null)
            .setDestinationFolderOperationsLogId(null)
            .setOldFolderFields(FolderHistoryFieldsModel.builder().displayName("old name").build())
            .setNewFolderFields(FolderHistoryFieldsModel.builder().displayName(TEST_FOLDER_1.getDisplayName()).build())
            .setOldQuotas(new QuotasByResource(Map.of(
                    YP_HDD_MAN, 10L,
                    YDB_RAM_SAS, 20L
            )))
            .setOldBalance(new QuotasByResource(Map.of()))
            .setOldProvisions(new QuotasByAccount(Map.of(
                    TEST_ACCOUNT_1_ID, new ProvisionsByResource(Map.of(
                            YP_HDD_MAN, new ProvisionHistoryModel(8L, null)
                    ))
            )))
            .setOldAccounts(null)
            .setNewQuotas(new QuotasByResource(Map.of(
                    YP_HDD_MAN, 10L,
                    YDB_RAM_SAS, 15L
            )))
            .setNewBalance(new QuotasByResource(Map.of()))
            .setNewProvisions(new QuotasByAccount(Map.of(
                    TEST_ACCOUNT_1_ID, new ProvisionsByResource(Map.of(
                            YP_HDD_MAN, new ProvisionHistoryModel(8L, null)
                    ))
            )))
            .setNewAccounts(null)
            .setActuallyAppliedProvisions(null)
            .setAccountsQuotasOperationsId(null)
            .setQuotasDemandsId(null)
            .setOrder(4L)
            .build();

    private TestFolderOperations() {
    }
}
