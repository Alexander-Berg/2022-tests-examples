package ru.yandex.intranet.d.web.api.imports;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.WithTenant;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.folders.AccountHistoryModel;
import ru.yandex.intranet.d.model.folders.AccountsHistoryModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.folders.FolderOperationType;
import ru.yandex.intranet.d.model.folders.ProvisionHistoryModel;
import ru.yandex.intranet.d.model.folders.ProvisionsByResource;
import ru.yandex.intranet.d.model.folders.QuotasByAccount;
import ru.yandex.intranet.d.model.folders.QuotasByResource;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.imports.AccountSpaceIdentityDto;
import ru.yandex.intranet.d.web.model.imports.ImportAccountDto;
import ru.yandex.intranet.d.web.model.imports.ImportAccountProvisionDto;
import ru.yandex.intranet.d.web.model.imports.ImportDto;
import ru.yandex.intranet.d.web.model.imports.ImportFolderDto;
import ru.yandex.intranet.d.web.model.imports.ImportResourceDto;
import ru.yandex.intranet.d.web.model.imports.ImportResultDto;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_2_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_IMPORT_FOLDER_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YP_CPU_SAS;
import static ru.yandex.intranet.d.TestResources.YP_HDD_SAS;

/**
 * Advanced import API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class AnotherImportApiTest {

    @Autowired
    private WebTestClient webClient;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private QuotasDao quotasDao;
    @Autowired
    private AccountsQuotasDao accountsQuotasDao;
    @Autowired
    private FolderOperationLogDao folderOperationLogDao;
    @Autowired
    private YdbTableClient tableClient;

    @Test
    @SuppressWarnings("MethodLength")
    public void simpleImportMoveAndDeleteAccountTest() {
        ImportDto quotasToImport = new ImportDto(
                List.of(
                        new ImportFolderDto("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                                null,
                                List.of(
                                        new ImportResourceDto(
                                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                                                null,
                                                "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                                100L,
                                                "gigabytes",
                                                50L,
                                                "gigabytes"
                                        )
                                ),
                                List.of(
                                        new ImportAccountDto(
                                                "1",
                                                "test",
                                                "Test",
                                                false,
                                                "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                                List.of(
                                                        new ImportAccountProvisionDto(
                                                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                                                                null,
                                                                50L,
                                                                "gigabytes",
                                                                25L,
                                                                "gigabytes"
                                                        )
                                                ),
                                                new AccountSpaceIdentityDto(
                                                        "978bd75a-cf67-44ac-b944-e8ca949bdf7e",
                                                        null
                                                ), false)
                                )
                        )
                )
        );
        ImportDto moveAccount = new ImportDto(
                List.of(
                        new ImportFolderDto("f6509efe-6496-4cd9-9019-92c8776ab0a4",
                                null,
                                List.of(
                                        new ImportResourceDto(
                                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                                                null,
                                                "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                                100L,
                                                "gigabytes",
                                                100L,
                                                "gigabytes"
                                        )
                                ),
                                List.of(
                                        new ImportAccountDto(
                                                "1",
                                                "test",
                                                "Test",
                                                true,
                                                "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                                List.of(
                                                        new ImportAccountProvisionDto(
                                                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                                                                null,
                                                                0L,
                                                                "gigabytes",
                                                                0L,
                                                                "gigabytes"
                                                        )
                                                ),
                                                new AccountSpaceIdentityDto(
                                                        "978bd75a-cf67-44ac-b944-e8ca949bdf7e",
                                                        null
                                                ), false)
                                )
                        ),
                        new ImportFolderDto("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                                null,
                                List.of(
                                        new ImportResourceDto(
                                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                                                null,
                                                "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                                100L,
                                                "gigabytes",
                                                100L,
                                                "gigabytes"
                                        )
                                ),
                                List.of()
                        )
                )
        );
        ImportResultDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/import/_importQuotas")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(quotasToImport)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ImportResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getImportFailures().isEmpty());
        Assertions.assertEquals(1, result.getSuccessfullyImported().size());
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getFolderId().isPresent());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                result.getSuccessfullyImported().get(0).getFolderId().get());
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getServiceId().isEmpty());
        Optional<AccountModel> newAccount = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsDao.getAllByExternalId(ts, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId("96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                        "1", "978bd75a-cf67-44ac-b944-e8ca949bdf7e")))))
                .block();
        Assertions.assertNotNull(newAccount);
        Assertions.assertTrue(newAccount.isPresent());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, newAccount.get().getTenantId());
        Assertions.assertEquals(0L, newAccount.get().getVersion());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", newAccount.get().getProviderId());
        Assertions.assertEquals("1", newAccount.get().getOuterAccountIdInProvider());
        Assertions.assertTrue(newAccount.get().getOuterAccountKeyInProvider().isPresent());
        Assertions.assertEquals("test", newAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", newAccount.get().getFolderId());
        Assertions.assertTrue(newAccount.get().getDisplayName().isPresent());
        Assertions.assertEquals("Test", newAccount.get().getDisplayName().get());
        Assertions.assertFalse(newAccount.get().isDeleted());
        List<QuotaModel> newQuotas = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        quotasDao.getByFolders(ts, List.of("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1"),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(newQuotas);
        Assertions.assertEquals(1, newQuotas.size());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", newQuotas.get(0).getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", newQuotas.get(0).getProviderId());
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", newQuotas.get(0).getResourceId());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, newQuotas.get(0).getTenantId());
        Assertions.assertEquals(100000000000L, newQuotas.get(0).getQuota());
        Assertions.assertEquals(50000000000L, newQuotas.get(0).getBalance());
        Optional<AccountsQuotasModel> accountQuota = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsQuotasDao.getById(ts, new AccountsQuotasModel.Identity(newAccount.get().getId(),
                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2"), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(accountQuota);
        Assertions.assertTrue(accountQuota.isPresent());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, accountQuota.get().getTenantId());
        Assertions.assertEquals(newAccount.get().getId(), accountQuota.get().getAccountId());
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", accountQuota.get().getResourceId());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", accountQuota.get().getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", accountQuota.get().getProviderId());
        Assertions.assertEquals(50000000000L, accountQuota.get().getProvidedQuota());
        Assertions.assertEquals(25000000000L, accountQuota.get().getAllocatedQuota());
        List<FolderOperationLogModel> log = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                        .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                                SortOrderDto.ASC, 100))).block();
        Assertions.assertNotNull(log);
        Assertions.assertEquals(1, log.size());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, log.get(0).getTenantId());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", log.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.QUOTA_IMPORT, log.get(0).getOperationType());
        Assertions.assertTrue(log.get(0).getAuthorUserId().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_ID, log.get(0).getAuthorUserId().get());
        Assertions.assertTrue(log.get(0).getAuthorUserUid().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_UID, log.get(0).getAuthorUserUid().get());
        Assertions.assertTrue(log.get(0).getOldFolderFields().isEmpty());
        Assertions.assertTrue(log.get(0).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 0L)),
                log.get(0).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 100000000000L)),
                log.get(0).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 0L)),
                log.get(0).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 50000000000L)),
                log.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(0L, null))))), log.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(50000000000L, null))))), log.get(0).getNewProvisions());
        Assertions.assertEquals(0L, log.get(0).getOrder());
        Assertions.assertTrue(log.get(0).getOldAccounts().isEmpty());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(0L, "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "1", "test",
                        "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", "Test", false,
                        null, "978bd75a-cf67-44ac-b944-e8ca949bdf7e", null, null)))),
                log.get(0).getNewAccounts());
        ImportResultDto moveAccountResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/import/_importQuotas")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(moveAccount)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ImportResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(moveAccountResult);
        Assertions.assertTrue(moveAccountResult.getImportFailures().isEmpty());
        Assertions.assertEquals(2, moveAccountResult.getSuccessfullyImported().size());
        Assertions.assertTrue(moveAccountResult.getSuccessfullyImported().get(0).getFolderId().isPresent());
        Assertions.assertTrue(moveAccountResult.getSuccessfullyImported().get(1).getFolderId().isPresent());
        Assertions.assertEquals(Set.of("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", "f6509efe-6496-4cd9-9019-92c8776ab0a4"),
                moveAccountResult.getSuccessfullyImported().stream().map(v -> v.getFolderId().get())
                        .collect(Collectors.toSet()));
        Assertions.assertTrue(moveAccountResult.getSuccessfullyImported().get(0).getServiceId().isEmpty());
        Assertions.assertTrue(moveAccountResult.getSuccessfullyImported().get(1).getServiceId().isEmpty());
        Optional<AccountModel> movedAccount = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsDao.getAllByExternalId(ts, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId("96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                        "1", "978bd75a-cf67-44ac-b944-e8ca949bdf7e")))))
                .block();
        Assertions.assertNotNull(movedAccount);
        Assertions.assertTrue(movedAccount.isPresent());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, movedAccount.get().getTenantId());
        Assertions.assertEquals(1L, movedAccount.get().getVersion());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", movedAccount.get().getProviderId());
        Assertions.assertEquals("1", movedAccount.get().getOuterAccountIdInProvider());
        Assertions.assertTrue(movedAccount.get().getOuterAccountKeyInProvider().isPresent());
        Assertions.assertEquals("test", movedAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", movedAccount.get().getFolderId());
        Assertions.assertTrue(movedAccount.get().getDisplayName().isPresent());
        Assertions.assertEquals("Test", movedAccount.get().getDisplayName().get());
        Assertions.assertTrue(movedAccount.get().isDeleted());
        List<QuotaModel> sourceQuotasAfterMove = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        quotasDao.getByFolders(ts, List.of("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1"),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        List<QuotaModel> destinationQuotasAfterMove = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        quotasDao.getByFolders(ts, List.of("f6509efe-6496-4cd9-9019-92c8776ab0a4"),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(sourceQuotasAfterMove);
        Assertions.assertEquals(1, sourceQuotasAfterMove.size());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", sourceQuotasAfterMove.get(0).getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", sourceQuotasAfterMove.get(0).getProviderId());
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", sourceQuotasAfterMove.get(0).getResourceId());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, sourceQuotasAfterMove.get(0).getTenantId());
        Assertions.assertEquals(100000000000L, sourceQuotasAfterMove.get(0).getQuota());
        Assertions.assertEquals(100000000000L, sourceQuotasAfterMove.get(0).getBalance());
        Assertions.assertNotNull(destinationQuotasAfterMove);
        Assertions.assertEquals(1, destinationQuotasAfterMove.size());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4",
                destinationQuotasAfterMove.get(0).getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                destinationQuotasAfterMove.get(0).getProviderId());
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                destinationQuotasAfterMove.get(0).getResourceId());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, destinationQuotasAfterMove.get(0).getTenantId());
        Assertions.assertEquals(100000000000L, destinationQuotasAfterMove.get(0).getQuota());
        Assertions.assertEquals(100000000000L, destinationQuotasAfterMove.get(0).getBalance());
        Optional<AccountsQuotasModel> accountQuotaAfterMove = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsQuotasDao.getById(ts, new AccountsQuotasModel.Identity(newAccount.get().getId(),
                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2"), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(accountQuotaAfterMove);
        Assertions.assertTrue(accountQuotaAfterMove.isPresent());
        Assertions.assertEquals(0L, accountQuotaAfterMove.get().getProvidedQuota());
        Assertions.assertEquals(0L, accountQuotaAfterMove.get().getAllocatedQuota());
        List<FolderOperationLogModel> sourceLogAfterMove = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                        .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                                SortOrderDto.ASC, 100))).block();
        List<FolderOperationLogModel> destinationLogAfterMove = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                        .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, "f6509efe-6496-4cd9-9019-92c8776ab0a4",
                                SortOrderDto.ASC, 100))).block();
        Assertions.assertNotNull(sourceLogAfterMove);
        sourceLogAfterMove.sort(Comparator.comparing(FolderOperationLogModel::getOperationDateTime));
        Assertions.assertEquals(2, sourceLogAfterMove.size());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, sourceLogAfterMove.get(0).getTenantId());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", sourceLogAfterMove.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.QUOTA_IMPORT, sourceLogAfterMove.get(0).getOperationType());
        Assertions.assertTrue(sourceLogAfterMove.get(0).getAuthorUserId().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_ID, sourceLogAfterMove.get(0).getAuthorUserId().get());
        Assertions.assertTrue(sourceLogAfterMove.get(0).getAuthorUserUid().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_UID, sourceLogAfterMove.get(0).getAuthorUserUid().get());
        Assertions.assertTrue(sourceLogAfterMove.get(0).getOldFolderFields().isEmpty());
        Assertions.assertTrue(sourceLogAfterMove.get(0).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 0L)),
                sourceLogAfterMove.get(0).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 100000000000L)),
                sourceLogAfterMove.get(0).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 0L)),
                sourceLogAfterMove.get(0).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 50000000000L)),
                sourceLogAfterMove.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(0L, null))))), sourceLogAfterMove.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(50000000000L, null))))),
                sourceLogAfterMove.get(0).getNewProvisions());
        Assertions.assertEquals(0L, sourceLogAfterMove.get(0).getOrder());
        Assertions.assertTrue(sourceLogAfterMove.get(0).getOldAccounts().isEmpty());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(0L, "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "1", "test",
                        "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", "Test", false,
                        null, "978bd75a-cf67-44ac-b944-e8ca949bdf7e", null, null)))),
                sourceLogAfterMove.get(0).getNewAccounts());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, sourceLogAfterMove.get(1).getTenantId());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", sourceLogAfterMove.get(1).getFolderId());
        Assertions.assertEquals(FolderOperationType.QUOTA_IMPORT, sourceLogAfterMove.get(1).getOperationType());
        Assertions.assertTrue(sourceLogAfterMove.get(1).getAuthorUserId().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_ID, sourceLogAfterMove.get(1).getAuthorUserId().get());
        Assertions.assertTrue(sourceLogAfterMove.get(1).getAuthorUserUid().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_UID, sourceLogAfterMove.get(1).getAuthorUserUid().get());
        Assertions.assertTrue(sourceLogAfterMove.get(1).getOldFolderFields().isEmpty());
        Assertions.assertTrue(sourceLogAfterMove.get(1).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of()), sourceLogAfterMove.get(1).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), sourceLogAfterMove.get(1).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 50000000000L)),
                sourceLogAfterMove.get(1).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 100000000000L)),
                sourceLogAfterMove.get(1).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(50000000000L, null))))),
                sourceLogAfterMove.get(1).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(0L, null))))), sourceLogAfterMove.get(1).getNewProvisions());
        Assertions.assertEquals(1L, sourceLogAfterMove.get(1).getOrder());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(0L, null, null, null,
                        "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", null, false,
                        null, null, null, null)))), sourceLogAfterMove.get(1).getOldAccounts());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(1L, null, null, null,
                        "f6509efe-6496-4cd9-9019-92c8776ab0a4", null, true, null, null, null, null)))),
                sourceLogAfterMove.get(1).getNewAccounts());
        Assertions.assertNotNull(destinationLogAfterMove);
        Assertions.assertEquals(1, destinationLogAfterMove.size());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, destinationLogAfterMove.get(0).getTenantId());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", destinationLogAfterMove.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.QUOTA_IMPORT, destinationLogAfterMove.get(0).getOperationType());
        Assertions.assertTrue(destinationLogAfterMove.get(0).getAuthorUserId().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_ID, destinationLogAfterMove.get(0).getAuthorUserId().get());
        Assertions.assertTrue(destinationLogAfterMove.get(0).getAuthorUserUid().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_UID, destinationLogAfterMove.get(0).getAuthorUserUid().get());
        Assertions.assertTrue(destinationLogAfterMove.get(0).getOldFolderFields().isEmpty());
        Assertions.assertTrue(destinationLogAfterMove.get(0).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 0L)),
                destinationLogAfterMove.get(0).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 100000000000L)),
                destinationLogAfterMove.get(0).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 0L)),
                destinationLogAfterMove.get(0).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 100000000000L)),
                destinationLogAfterMove.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of()), destinationLogAfterMove.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of()), destinationLogAfterMove.get(0).getNewProvisions());
        Assertions.assertEquals(0L, destinationLogAfterMove.get(0).getOrder());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(0L, null, null, null,
                        "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", null, false,
                        null, null, null, null)))), destinationLogAfterMove.get(0).getOldAccounts());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(1L, null, null, null,
                        "f6509efe-6496-4cd9-9019-92c8776ab0a4", null, true, null, null, null, null)))),
                destinationLogAfterMove.get(0).getNewAccounts());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void simpleImportTestWithZeroQuotaAndAddNewQuota() {
        ImportDto quotasToImport = new ImportDto(
                List.of(
                        new ImportFolderDto(TEST_IMPORT_FOLDER_ID,
                                null,
                                List.of(
                                        new ImportResourceDto(
                                                YP_HDD_SAS,
                                                null,
                                                YP_ID,
                                                100L,
                                                "gigabytes",
                                                50L,
                                                "gigabytes"
                                        )
                                ),
                                List.of(
                                        new ImportAccountDto(
                                                "1",
                                                "test",
                                                "Test",
                                                false,
                                                YP_ID,
                                                List.of(
                                                        new ImportAccountProvisionDto(
                                                                YP_HDD_SAS,
                                                                null,
                                                                50L,
                                                                "gigabytes",
                                                                25L,
                                                                "gigabytes"
                                                        )
                                                ),
                                                new AccountSpaceIdentityDto(
                                                        TEST_ACCOUNT_SPACE_2_ID,
                                                        null
                                                ), false)
                                )
                        )
                )
        );
        ImportDto quotasToImportWithZeroes = new ImportDto(
                List.of(
                        new ImportFolderDto(TEST_IMPORT_FOLDER_ID,
                                null,
                                List.of(
                                        new ImportResourceDto(
                                                YP_HDD_SAS,
                                                null,
                                                YP_ID,
                                                0L,
                                                "gigabytes",
                                                0L,
                                                "gigabytes"
                                        ),
                                        new ImportResourceDto(
                                                YP_CPU_SAS,
                                                null,
                                                YP_ID,
                                                2L,
                                                "millicores",
                                                1L,
                                                "millicores"
                                        )
                                ),
                                List.of(
                                        new ImportAccountDto(
                                                "1",
                                                "test",
                                                "Test",
                                                false,
                                                YP_ID,
                                                List.of(
                                                        new ImportAccountProvisionDto(
                                                                YP_HDD_SAS,
                                                                null,
                                                                0L,
                                                                "gigabytes",
                                                                0L,
                                                                "gigabytes"
                                                        ),
                                                        new ImportAccountProvisionDto(
                                                                YP_CPU_SAS,
                                                                null,
                                                                1L,
                                                                "millicores",
                                                                0L,
                                                                "millicores"
                                                        )
                                                ),
                                                new AccountSpaceIdentityDto(
                                                        TEST_ACCOUNT_SPACE_2_ID,
                                                        null
                                                ), false)
                                )
                        )
                )
        );
        ImportResultDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/import/_importQuotas")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(quotasToImport)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ImportResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getImportFailures().isEmpty());
        Assertions.assertEquals(1, result.getSuccessfullyImported().size());
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getFolderId().isPresent());
        Assertions.assertEquals(TEST_IMPORT_FOLDER_ID,
                result.getSuccessfullyImported().get(0).getFolderId().get());
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getServiceId().isEmpty());
        Optional<AccountModel> newAccount = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsDao.getAllByExternalId(ts, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(YP_ID,
                                        "1", TEST_ACCOUNT_SPACE_2_ID)))))
                .block();
        Assertions.assertNotNull(newAccount);
        Assertions.assertTrue(newAccount.isPresent());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, newAccount.get().getTenantId());
        Assertions.assertEquals(0L, newAccount.get().getVersion());
        Assertions.assertEquals(YP_ID, newAccount.get().getProviderId());
        Assertions.assertEquals("1", newAccount.get().getOuterAccountIdInProvider());
        Assertions.assertTrue(newAccount.get().getOuterAccountKeyInProvider().isPresent());
        Assertions.assertEquals("test", newAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(TEST_IMPORT_FOLDER_ID, newAccount.get().getFolderId());
        Assertions.assertTrue(newAccount.get().getDisplayName().isPresent());
        Assertions.assertEquals("Test", newAccount.get().getDisplayName().get());
        Assertions.assertFalse(newAccount.get().isDeleted());
        List<QuotaModel> newQuotas = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        quotasDao.getByFolders(ts, List.of(TEST_IMPORT_FOLDER_ID),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(newQuotas);
        Assertions.assertEquals(1, newQuotas.size());
        Assertions.assertEquals(TEST_IMPORT_FOLDER_ID, newQuotas.get(0).getFolderId());
        Assertions.assertEquals(YP_ID, newQuotas.get(0).getProviderId());
        Assertions.assertEquals(YP_HDD_SAS, newQuotas.get(0).getResourceId());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, newQuotas.get(0).getTenantId());
        Assertions.assertEquals(100000000000L, newQuotas.get(0).getQuota());
        Assertions.assertEquals(50000000000L, newQuotas.get(0).getBalance());
        Optional<AccountsQuotasModel> accountQuota = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsQuotasDao.getById(ts, new AccountsQuotasModel.Identity(newAccount.get().getId(),
                                YP_HDD_SAS), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(accountQuota);
        Assertions.assertTrue(accountQuota.isPresent());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, accountQuota.get().getTenantId());
        Assertions.assertEquals(newAccount.get().getId(), accountQuota.get().getAccountId());
        Assertions.assertEquals(YP_HDD_SAS, accountQuota.get().getResourceId());
        Assertions.assertEquals(TEST_IMPORT_FOLDER_ID, accountQuota.get().getFolderId());
        Assertions.assertEquals(YP_ID, accountQuota.get().getProviderId());
        Assertions.assertEquals(50000000000L, accountQuota.get().getProvidedQuota());
        Assertions.assertEquals(25000000000L, accountQuota.get().getAllocatedQuota());
        List<FolderOperationLogModel> log = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                        .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, TEST_IMPORT_FOLDER_ID, SortOrderDto.ASC,
                                100))).block();
        Assertions.assertNotNull(log);
        Assertions.assertEquals(1, log.size());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, log.get(0).getTenantId());
        Assertions.assertEquals(TEST_IMPORT_FOLDER_ID, log.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.QUOTA_IMPORT, log.get(0).getOperationType());
        Assertions.assertTrue(log.get(0).getAuthorUserId().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_ID, log.get(0).getAuthorUserId().get());
        Assertions.assertTrue(log.get(0).getAuthorUserUid().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_UID, log.get(0).getAuthorUserUid().get());
        Assertions.assertTrue(log.get(0).getOldFolderFields().isEmpty());
        Assertions.assertTrue(log.get(0).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of(YP_HDD_SAS, 0L)),
                log.get(0).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of(YP_HDD_SAS, 100000000000L)),
                log.get(0).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of(YP_HDD_SAS, 0L)),
                log.get(0).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of(YP_HDD_SAS, 50000000000L)),
                log.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of(YP_HDD_SAS,
                        new ProvisionHistoryModel(0L, null))))), log.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of(YP_HDD_SAS,
                        new ProvisionHistoryModel(50000000000L, null))))), log.get(0).getNewProvisions());
        Assertions.assertEquals(0L, log.get(0).getOrder());
        Assertions.assertTrue(log.get(0).getOldAccounts().isEmpty());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(0L, YP_ID, "1", "test",
                        TEST_IMPORT_FOLDER_ID, "Test", false,
                        null, TEST_ACCOUNT_SPACE_2_ID, null, null)))),
                log.get(0).getNewAccounts());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/import/_importQuotas")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(quotasToImportWithZeroes)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ImportResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getImportFailures().isEmpty());
        Assertions.assertEquals(1, result.getSuccessfullyImported().size());
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getFolderId().isPresent());
        Assertions.assertEquals(TEST_IMPORT_FOLDER_ID,
                result.getSuccessfullyImported().get(0).getFolderId().get());
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getServiceId().isEmpty());
        Optional<AccountModel>  newAccount2 = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsDao.getAllByExternalId(ts, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(YP_ID,
                                        "1", TEST_ACCOUNT_SPACE_2_ID)))))
                .block();
        Assertions.assertNotNull(newAccount2);
        Assertions.assertTrue(newAccount2.isPresent());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, newAccount2.get().getTenantId());
        Assertions.assertEquals(0L, newAccount2.get().getVersion());
        Assertions.assertEquals(YP_ID, newAccount2.get().getProviderId());
        Assertions.assertEquals("1", newAccount2.get().getOuterAccountIdInProvider());
        Assertions.assertTrue(newAccount2.get().getOuterAccountKeyInProvider().isPresent());
        Assertions.assertEquals("test", newAccount2.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(TEST_IMPORT_FOLDER_ID, newAccount2.get().getFolderId());
        Assertions.assertTrue(newAccount2.get().getDisplayName().isPresent());
        Assertions.assertEquals("Test", newAccount2.get().getDisplayName().get());
        Assertions.assertFalse(newAccount2.get().isDeleted());
        newQuotas = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        quotasDao.getByFolders(ts, List.of(TEST_IMPORT_FOLDER_ID),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(newQuotas);
        Assertions.assertEquals(2, newQuotas.size());
        Assertions.assertEquals(List.of(
                QuotaModel.builder()
                        .tenantId(Tenants.DEFAULT_TENANT_ID)
                        .folderId(TEST_IMPORT_FOLDER_ID)
                        .providerId(YP_ID)
                        .resourceId(YP_CPU_SAS)
                        .quota(2L)
                        .balance(1L)
                        .frozenQuota(0L)
                        .build(),
                QuotaModel.builder()
                        .tenantId(Tenants.DEFAULT_TENANT_ID)
                        .folderId(TEST_IMPORT_FOLDER_ID)
                        .providerId(YP_ID)
                        .resourceId(YP_HDD_SAS)
                        .quota(0L)
                        .balance(0L)
                        .frozenQuota(0L)
                        .build()
        ), newQuotas);
        accountQuota = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsQuotasDao.getById(ts, new AccountsQuotasModel.Identity(newAccount2.get().getId(),
                                YP_HDD_SAS), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(accountQuota);
        Assertions.assertTrue(accountQuota.isPresent());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, accountQuota.get().getTenantId());
        Assertions.assertEquals(newAccount2.get().getId(), accountQuota.get().getAccountId());
        Assertions.assertEquals(YP_HDD_SAS, accountQuota.get().getResourceId());
        Assertions.assertEquals(TEST_IMPORT_FOLDER_ID, accountQuota.get().getFolderId());
        Assertions.assertEquals(YP_ID, accountQuota.get().getProviderId());
        Assertions.assertEquals(0L, accountQuota.get().getProvidedQuota());
        Assertions.assertEquals(0L, accountQuota.get().getAllocatedQuota());
        accountQuota = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsQuotasDao.getById(ts, new AccountsQuotasModel.Identity(newAccount2.get().getId(),
                                YP_CPU_SAS), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(accountQuota);
        Assertions.assertTrue(accountQuota.isPresent());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, accountQuota.get().getTenantId());
        Assertions.assertEquals(newAccount2.get().getId(), accountQuota.get().getAccountId());
        Assertions.assertEquals(YP_CPU_SAS, accountQuota.get().getResourceId());
        Assertions.assertEquals(TEST_IMPORT_FOLDER_ID, accountQuota.get().getFolderId());
        Assertions.assertEquals(YP_ID, accountQuota.get().getProviderId());
        Assertions.assertEquals(1L, accountQuota.get().getProvidedQuota());
        Assertions.assertEquals(0L, accountQuota.get().getAllocatedQuota());
        log = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                        .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, TEST_IMPORT_FOLDER_ID, SortOrderDto.ASC,
                                100))).block();
        Assertions.assertNotNull(log);
        Assertions.assertEquals(2, log.size());
        log.sort(Comparator.comparingLong(FolderOperationLogModel::getOrder));
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, log.get(1).getTenantId());
        Assertions.assertEquals(TEST_IMPORT_FOLDER_ID, log.get(1).getFolderId());
        Assertions.assertEquals(FolderOperationType.QUOTA_IMPORT, log.get(1).getOperationType());
        Assertions.assertTrue(log.get(1).getAuthorUserId().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_ID, log.get(1).getAuthorUserId().get());
        Assertions.assertTrue(log.get(1).getAuthorUserUid().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_UID, log.get(1).getAuthorUserUid().get());
        Assertions.assertTrue(log.get(1).getOldFolderFields().isEmpty());
        Assertions.assertTrue(log.get(1).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of(YP_CPU_SAS, 0L,
                YP_HDD_SAS, 100000000000L)),
                log.get(1).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of(YP_CPU_SAS, 2L,
                YP_HDD_SAS, 0L)),
                log.get(1).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of(YP_HDD_SAS, 50000000000L, YP_CPU_SAS, 0L)),
                log.get(1).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of(YP_HDD_SAS, 0L, YP_CPU_SAS, 1L)),
                log.get(1).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount2.get().getId(),
                new ProvisionsByResource(Map.of(YP_CPU_SAS,
                        new ProvisionHistoryModel(0L, null),
                        YP_HDD_SAS,
                        new ProvisionHistoryModel(50000000000L, null))))), log.get(1).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount2.get().getId(),
                new ProvisionsByResource(Map.of(YP_CPU_SAS,
                        new ProvisionHistoryModel(1L, null),
                        YP_HDD_SAS,
                        new ProvisionHistoryModel(0L, null))))), log.get(1).getNewProvisions());
        Assertions.assertEquals(1L, log.get(1).getOrder());
        Assertions.assertTrue(log.get(1).getOldAccounts().isEmpty());
        Assertions.assertTrue(log.get(1).getNewAccounts().isEmpty());
    }

}
