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

/**
 * Import API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ImportApiTest {

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
    public void simpleImportTest() {
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
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void simpleImportRetryTest() {
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
        ImportResultDto retryResult = webClient
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
        Assertions.assertNotNull(retryResult);
        Assertions.assertTrue(retryResult.getImportFailures().isEmpty());
        Assertions.assertEquals(1, retryResult.getSuccessfullyImported().size());
        Assertions.assertTrue(retryResult.getSuccessfullyImported().get(0).getFolderId().isPresent());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                retryResult.getSuccessfullyImported().get(0).getFolderId().get());
        Assertions.assertTrue(retryResult.getSuccessfullyImported().get(0).getServiceId().isEmpty());
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
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void simpleImportDeleteAccountTest() {
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
        ImportDto deleteAccount = new ImportDto(
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
        ImportResultDto deleteAccountResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/import/_importQuotas")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(deleteAccount)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ImportResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(deleteAccountResult);
        Assertions.assertTrue(deleteAccountResult.getImportFailures().isEmpty());
        Assertions.assertEquals(1, deleteAccountResult.getSuccessfullyImported().size());
        Assertions.assertTrue(deleteAccountResult.getSuccessfullyImported().get(0).getFolderId().isPresent());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                deleteAccountResult.getSuccessfullyImported().get(0).getFolderId().get());
        Assertions.assertTrue(deleteAccountResult.getSuccessfullyImported().get(0).getServiceId().isEmpty());
        Optional<AccountModel> deletedAccount = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsDao.getAllByExternalId(ts, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId("96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                        "1", "978bd75a-cf67-44ac-b944-e8ca949bdf7e")))))
                .block();
        Assertions.assertNotNull(deletedAccount);
        Assertions.assertTrue(deletedAccount.isPresent());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, deletedAccount.get().getTenantId());
        Assertions.assertEquals(1L, deletedAccount.get().getVersion());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", deletedAccount.get().getProviderId());
        Assertions.assertEquals("1", deletedAccount.get().getOuterAccountIdInProvider());
        Assertions.assertTrue(deletedAccount.get().getOuterAccountKeyInProvider().isPresent());
        Assertions.assertEquals("test", deletedAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", deletedAccount.get().getFolderId());
        Assertions.assertTrue(deletedAccount.get().getDisplayName().isPresent());
        Assertions.assertEquals("Test", deletedAccount.get().getDisplayName().get());
        Assertions.assertTrue(deletedAccount.get().isDeleted());
        List<QuotaModel> quotasAfterDelete = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        quotasDao.getByFolders(ts, List.of("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1"),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(quotasAfterDelete);
        Assertions.assertEquals(1, quotasAfterDelete.size());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", quotasAfterDelete.get(0).getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", quotasAfterDelete.get(0).getProviderId());
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", quotasAfterDelete.get(0).getResourceId());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, quotasAfterDelete.get(0).getTenantId());
        Assertions.assertEquals(100000000000L, quotasAfterDelete.get(0).getQuota());
        Assertions.assertEquals(100000000000L, quotasAfterDelete.get(0).getBalance());
        Optional<AccountsQuotasModel> accountQuotaAfterDelete = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsQuotasDao.getById(ts, new AccountsQuotasModel.Identity(newAccount.get().getId(),
                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2"), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(accountQuotaAfterDelete);
        Assertions.assertTrue(accountQuotaAfterDelete.isPresent());
        Assertions.assertEquals(0L, accountQuotaAfterDelete.get().getProvidedQuota());
        Assertions.assertEquals(0L, accountQuotaAfterDelete.get().getAllocatedQuota());
        List<FolderOperationLogModel> logAfterDelete = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                        .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                                SortOrderDto.ASC, 100))).block();
        Assertions.assertNotNull(logAfterDelete);
        logAfterDelete.sort(Comparator.comparing(FolderOperationLogModel::getOperationDateTime));
        Assertions.assertEquals(2, logAfterDelete.size());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, logAfterDelete.get(0).getTenantId());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", logAfterDelete.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.QUOTA_IMPORT, logAfterDelete.get(0).getOperationType());
        Assertions.assertTrue(logAfterDelete.get(0).getAuthorUserId().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_ID, logAfterDelete.get(0).getAuthorUserId().get());
        Assertions.assertTrue(logAfterDelete.get(0).getAuthorUserUid().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_UID, logAfterDelete.get(0).getAuthorUserUid().get());
        Assertions.assertTrue(logAfterDelete.get(0).getOldFolderFields().isEmpty());
        Assertions.assertTrue(logAfterDelete.get(0).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 0L)),
                logAfterDelete.get(0).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 100000000000L)),
                logAfterDelete.get(0).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 0L)),
                logAfterDelete.get(0).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 50000000000L)),
                logAfterDelete.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(0L, null))))), logAfterDelete.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(50000000000L, null))))), logAfterDelete.get(0).getNewProvisions());
        Assertions.assertEquals(0L, logAfterDelete.get(0).getOrder());
        Assertions.assertTrue(logAfterDelete.get(0).getOldAccounts().isEmpty());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(0L, "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "1", "test",
                        "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", "Test", false,
                        null, "978bd75a-cf67-44ac-b944-e8ca949bdf7e", null, null)))),
                logAfterDelete.get(0).getNewAccounts());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, logAfterDelete.get(1).getTenantId());
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", logAfterDelete.get(1).getFolderId());
        Assertions.assertEquals(FolderOperationType.QUOTA_IMPORT, logAfterDelete.get(1).getOperationType());
        Assertions.assertTrue(logAfterDelete.get(1).getAuthorUserId().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_ID, logAfterDelete.get(1).getAuthorUserId().get());
        Assertions.assertTrue(logAfterDelete.get(1).getAuthorUserUid().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_UID, logAfterDelete.get(1).getAuthorUserUid().get());
        Assertions.assertTrue(logAfterDelete.get(1).getOldFolderFields().isEmpty());
        Assertions.assertTrue(logAfterDelete.get(1).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of()), logAfterDelete.get(1).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), logAfterDelete.get(1).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 50000000000L)),
                logAfterDelete.get(1).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 100000000000L)),
                logAfterDelete.get(1).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(50000000000L, null))))), logAfterDelete.get(1).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(0L, null))))), logAfterDelete.get(1).getNewProvisions());
        Assertions.assertEquals(1L, logAfterDelete.get(1).getOrder());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(0L, null, null, null, null, null, false, null, null, null, null)))),
                logAfterDelete.get(1).getOldAccounts());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(1L, null, null, null,
                        null, null, true, null, null, null, null)))), logAfterDelete.get(1).getNewAccounts());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void simpleImportMoveAccountTest() {
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
        Assertions.assertFalse(movedAccount.get().isDeleted());
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
        Assertions.assertEquals(50000000000L, destinationQuotasAfterMove.get(0).getBalance());
        Optional<AccountsQuotasModel> accountQuotaAfterMove = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsQuotasDao.getById(ts, new AccountsQuotasModel.Identity(newAccount.get().getId(),
                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2"), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(accountQuotaAfterMove);
        Assertions.assertTrue(accountQuotaAfterMove.isPresent());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, accountQuotaAfterMove.get().getTenantId());
        Assertions.assertEquals(newAccount.get().getId(), accountQuotaAfterMove.get().getAccountId());
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", accountQuotaAfterMove.get().getResourceId());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", accountQuotaAfterMove.get().getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", accountQuotaAfterMove.get().getProviderId());
        Assertions.assertEquals(50000000000L, accountQuotaAfterMove.get().getProvidedQuota());
        Assertions.assertEquals(25000000000L, accountQuotaAfterMove.get().getAllocatedQuota());
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
                new AccountHistoryModel(0L, null, null,
                        null, "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", null, null,
                        null, null, null, null)))), sourceLogAfterMove.get(1).getOldAccounts());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(1L, null, null, null,
                        "f6509efe-6496-4cd9-9019-92c8776ab0a4", null, null, null, null, null, null)))),
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
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 50000000000L)),
                destinationLogAfterMove.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(0L, null))))), destinationLogAfterMove.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(50000000000L, null))))),
                destinationLogAfterMove.get(0).getNewProvisions());
        Assertions.assertEquals(0L, destinationLogAfterMove.get(0).getOrder());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(0L, null, null,
                        null, "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", null, null,
                        null, null, null, null)))), destinationLogAfterMove.get(0).getOldAccounts());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(1L, null, null, null,
                        "f6509efe-6496-4cd9-9019-92c8776ab0a4", null, null, null, null, null, null)))),
                destinationLogAfterMove.get(0).getNewAccounts());
    }

    @Test
    public void simpleImportToServiceTest() {
        ImportDto quotasToImport = new ImportDto(
                List.of(
                        new ImportFolderDto(null,
                                4L,
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
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getServiceId().isPresent());
        Assertions.assertEquals(4L,
                result.getSuccessfullyImported().get(0).getServiceId().get());
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getFolderId().isEmpty());
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
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", newAccount.get().getFolderId());
        Assertions.assertTrue(newAccount.get().getDisplayName().isPresent());
        Assertions.assertEquals("Test", newAccount.get().getDisplayName().get());
        Assertions.assertFalse(newAccount.get().isDeleted());
        List<QuotaModel> newQuotas = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        quotasDao.getByFolders(ts, List.of("f6509efe-6496-4cd9-9019-92c8776ab0a4"),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(newQuotas);
        Assertions.assertEquals(1, newQuotas.size());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", newQuotas.get(0).getFolderId());
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
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", accountQuota.get().getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", accountQuota.get().getProviderId());
        Assertions.assertEquals(50000000000L, accountQuota.get().getProvidedQuota());
        Assertions.assertEquals(25000000000L, accountQuota.get().getAllocatedQuota());
        List<FolderOperationLogModel> log = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                        .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, "f6509efe-6496-4cd9-9019-92c8776ab0a4",
                                SortOrderDto.ASC, 100))).block();
        Assertions.assertNotNull(log);
        Assertions.assertEquals(1, log.size());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, log.get(0).getTenantId());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", log.get(0).getFolderId());
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
                        "f6509efe-6496-4cd9-9019-92c8776ab0a4", "Test", false,
                        null, "978bd75a-cf67-44ac-b944-e8ca949bdf7e", null, null)))),
                log.get(0).getNewAccounts());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void simpleImportToServiceDeleteAccountTest() {
        ImportDto quotasToImport = new ImportDto(
                List.of(
                        new ImportFolderDto(null,
                                4L,
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
        ImportDto deleteAccount = new ImportDto(
                List.of(
                        new ImportFolderDto(null,
                                4L,
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
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getServiceId().isPresent());
        Assertions.assertEquals(4L,
                result.getSuccessfullyImported().get(0).getServiceId().get());
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getFolderId().isEmpty());
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
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", newAccount.get().getFolderId());
        Assertions.assertTrue(newAccount.get().getDisplayName().isPresent());
        Assertions.assertEquals("Test", newAccount.get().getDisplayName().get());
        Assertions.assertFalse(newAccount.get().isDeleted());
        List<QuotaModel> newQuotas = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        quotasDao.getByFolders(ts, List.of("f6509efe-6496-4cd9-9019-92c8776ab0a4"),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(newQuotas);
        Assertions.assertEquals(1, newQuotas.size());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", newQuotas.get(0).getFolderId());
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
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", accountQuota.get().getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", accountQuota.get().getProviderId());
        Assertions.assertEquals(50000000000L, accountQuota.get().getProvidedQuota());
        Assertions.assertEquals(25000000000L, accountQuota.get().getAllocatedQuota());
        List<FolderOperationLogModel> log = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                        .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, "f6509efe-6496-4cd9-9019-92c8776ab0a4",
                                SortOrderDto.ASC, 100))).block();
        Assertions.assertNotNull(log);
        Assertions.assertEquals(1, log.size());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, log.get(0).getTenantId());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", log.get(0).getFolderId());
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
                        "f6509efe-6496-4cd9-9019-92c8776ab0a4", "Test", false,
                        null, "978bd75a-cf67-44ac-b944-e8ca949bdf7e", null, null)))),
                log.get(0).getNewAccounts());
        ImportResultDto deleteAccountResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/import/_importQuotas")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(deleteAccount)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ImportResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(deleteAccountResult);
        Assertions.assertTrue(deleteAccountResult.getImportFailures().isEmpty());
        Assertions.assertEquals(1, deleteAccountResult.getSuccessfullyImported().size());
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getServiceId().isPresent());
        Assertions.assertEquals(4L,
                result.getSuccessfullyImported().get(0).getServiceId().get());
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getFolderId().isEmpty());
        Optional<AccountModel> deletedAccount = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsDao.getAllByExternalId(ts, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId("96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                        "1", "978bd75a-cf67-44ac-b944-e8ca949bdf7e")))))
                .block();
        Assertions.assertNotNull(deletedAccount);
        Assertions.assertTrue(deletedAccount.isPresent());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, deletedAccount.get().getTenantId());
        Assertions.assertEquals(1L, deletedAccount.get().getVersion());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", deletedAccount.get().getProviderId());
        Assertions.assertEquals("1", deletedAccount.get().getOuterAccountIdInProvider());
        Assertions.assertTrue(deletedAccount.get().getOuterAccountKeyInProvider().isPresent());
        Assertions.assertEquals("test", deletedAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", deletedAccount.get().getFolderId());
        Assertions.assertTrue(deletedAccount.get().getDisplayName().isPresent());
        Assertions.assertEquals("Test", deletedAccount.get().getDisplayName().get());
        Assertions.assertTrue(deletedAccount.get().isDeleted());
        List<QuotaModel> quotasAfterDelete = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        quotasDao.getByFolders(ts, List.of("f6509efe-6496-4cd9-9019-92c8776ab0a4"),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(quotasAfterDelete);
        Assertions.assertEquals(1, quotasAfterDelete.size());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", quotasAfterDelete.get(0).getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", quotasAfterDelete.get(0).getProviderId());
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", quotasAfterDelete.get(0).getResourceId());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, quotasAfterDelete.get(0).getTenantId());
        Assertions.assertEquals(100000000000L, quotasAfterDelete.get(0).getQuota());
        Assertions.assertEquals(100000000000L, quotasAfterDelete.get(0).getBalance());
        Optional<AccountsQuotasModel> accountQuotaAfterDelete = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsQuotasDao.getById(ts, new AccountsQuotasModel.Identity(newAccount.get().getId(),
                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2"), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(accountQuotaAfterDelete);
        Assertions.assertTrue(accountQuotaAfterDelete.isPresent());
        Assertions.assertEquals(0L, accountQuotaAfterDelete.get().getProvidedQuota());
        Assertions.assertEquals(0L, accountQuotaAfterDelete.get().getAllocatedQuota());
        List<FolderOperationLogModel> logAfterDelete = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                        .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, "f6509efe-6496-4cd9-9019-92c8776ab0a4",
                                SortOrderDto.ASC, 100))).block();
        Assertions.assertNotNull(logAfterDelete);
        logAfterDelete.sort(Comparator.comparing(FolderOperationLogModel::getOperationDateTime));
        Assertions.assertEquals(2, logAfterDelete.size());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, logAfterDelete.get(0).getTenantId());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", logAfterDelete.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.QUOTA_IMPORT, logAfterDelete.get(0).getOperationType());
        Assertions.assertTrue(logAfterDelete.get(0).getAuthorUserId().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_ID, logAfterDelete.get(0).getAuthorUserId().get());
        Assertions.assertTrue(logAfterDelete.get(0).getAuthorUserUid().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_UID, logAfterDelete.get(0).getAuthorUserUid().get());
        Assertions.assertTrue(logAfterDelete.get(0).getOldFolderFields().isEmpty());
        Assertions.assertTrue(logAfterDelete.get(0).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 0L)),
                logAfterDelete.get(0).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 100000000000L)),
                logAfterDelete.get(0).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 0L)),
                logAfterDelete.get(0).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 50000000000L)),
                logAfterDelete.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(0L, null))))), logAfterDelete.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(50000000000L, null))))), logAfterDelete.get(0).getNewProvisions());
        Assertions.assertEquals(0L, logAfterDelete.get(0).getOrder());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, logAfterDelete.get(1).getTenantId());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", logAfterDelete.get(1).getFolderId());
        Assertions.assertEquals(FolderOperationType.QUOTA_IMPORT, logAfterDelete.get(1).getOperationType());
        Assertions.assertTrue(logAfterDelete.get(1).getAuthorUserId().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_ID, logAfterDelete.get(1).getAuthorUserId().get());
        Assertions.assertTrue(logAfterDelete.get(1).getAuthorUserUid().isPresent());
        Assertions.assertEquals(TestUsers.USER_1_UID, logAfterDelete.get(1).getAuthorUserUid().get());
        Assertions.assertTrue(logAfterDelete.get(1).getOldFolderFields().isEmpty());
        Assertions.assertTrue(logAfterDelete.get(1).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of()), logAfterDelete.get(1).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), logAfterDelete.get(1).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 50000000000L)),
                logAfterDelete.get(1).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 100000000000L)),
                logAfterDelete.get(1).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(50000000000L, null))))), logAfterDelete.get(1).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(0L, null))))), logAfterDelete.get(1).getNewProvisions());
        Assertions.assertEquals(1L, logAfterDelete.get(1).getOrder());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(0L, null, null, null, null, null, false, null, null, null, null)))),
                logAfterDelete.get(1).getOldAccounts());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(1L, null, null, null,
                        null, null, true, null, null, null, null)))), logAfterDelete.get(1).getNewAccounts());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void simpleImportToServiceMoveAccountTest() {
        ImportDto quotasToImport = new ImportDto(
                List.of(
                        new ImportFolderDto(null,
                                4L,
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
                        new ImportFolderDto(null,
                                7L,
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
                        ),
                        new ImportFolderDto(null,
                                4L,
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
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getServiceId().isPresent());
        Assertions.assertEquals(4L,
                result.getSuccessfullyImported().get(0).getServiceId().get());
        Assertions.assertTrue(result.getSuccessfullyImported().get(0).getFolderId().isEmpty());
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
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", newAccount.get().getFolderId());
        Assertions.assertTrue(newAccount.get().getDisplayName().isPresent());
        Assertions.assertEquals("Test", newAccount.get().getDisplayName().get());
        Assertions.assertFalse(newAccount.get().isDeleted());
        List<QuotaModel> newQuotas = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        quotasDao.getByFolders(ts, List.of("f6509efe-6496-4cd9-9019-92c8776ab0a4"),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(newQuotas);
        Assertions.assertEquals(1, newQuotas.size());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", newQuotas.get(0).getFolderId());
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
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", accountQuota.get().getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", accountQuota.get().getProviderId());
        Assertions.assertEquals(50000000000L, accountQuota.get().getProvidedQuota());
        Assertions.assertEquals(25000000000L, accountQuota.get().getAllocatedQuota());
        List<FolderOperationLogModel> log = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                        .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, "f6509efe-6496-4cd9-9019-92c8776ab0a4",
                                SortOrderDto.ASC, 100))).block();
        Assertions.assertNotNull(log);
        Assertions.assertEquals(1, log.size());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, log.get(0).getTenantId());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", log.get(0).getFolderId());
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
                        "f6509efe-6496-4cd9-9019-92c8776ab0a4", "Test", false,
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
        Assertions.assertTrue(moveAccountResult.getSuccessfullyImported().get(0).getServiceId().isPresent());
        Assertions.assertTrue(moveAccountResult.getSuccessfullyImported().get(1).getServiceId().isPresent());
        Assertions.assertEquals(Set.of(4L, 7L),
                moveAccountResult.getSuccessfullyImported().stream().map(v -> v.getServiceId().get())
                        .collect(Collectors.toSet()));
        Assertions.assertTrue(moveAccountResult.getSuccessfullyImported().get(0).getFolderId().isEmpty());
        Assertions.assertTrue(moveAccountResult.getSuccessfullyImported().get(1).getFolderId().isEmpty());
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
        Assertions.assertEquals("64cf1d41-c8fc-4789-8ef0-5c8cbe144084", movedAccount.get().getFolderId());
        Assertions.assertTrue(movedAccount.get().getDisplayName().isPresent());
        Assertions.assertEquals("Test", movedAccount.get().getDisplayName().get());
        Assertions.assertFalse(movedAccount.get().isDeleted());
        List<QuotaModel> sourceQuotasAfterMove = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        quotasDao.getByFolders(ts, List.of("f6509efe-6496-4cd9-9019-92c8776ab0a4"),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        List<QuotaModel> destinationQuotasAfterMove = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        quotasDao.getByFolders(ts, List.of("64cf1d41-c8fc-4789-8ef0-5c8cbe144084"),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(sourceQuotasAfterMove);
        Assertions.assertEquals(1, sourceQuotasAfterMove.size());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", sourceQuotasAfterMove.get(0).getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", sourceQuotasAfterMove.get(0).getProviderId());
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", sourceQuotasAfterMove.get(0).getResourceId());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, sourceQuotasAfterMove.get(0).getTenantId());
        Assertions.assertEquals(100000000000L, sourceQuotasAfterMove.get(0).getQuota());
        Assertions.assertEquals(100000000000L, sourceQuotasAfterMove.get(0).getBalance());
        Assertions.assertNotNull(destinationQuotasAfterMove);
        Assertions.assertEquals(1, destinationQuotasAfterMove.size());
        Assertions.assertEquals("64cf1d41-c8fc-4789-8ef0-5c8cbe144084",
                destinationQuotasAfterMove.get(0).getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                destinationQuotasAfterMove.get(0).getProviderId());
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                destinationQuotasAfterMove.get(0).getResourceId());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, destinationQuotasAfterMove.get(0).getTenantId());
        Assertions.assertEquals(100000000000L, destinationQuotasAfterMove.get(0).getQuota());
        Assertions.assertEquals(50000000000L, destinationQuotasAfterMove.get(0).getBalance());
        Optional<AccountsQuotasModel> accountQuotaAfterMove = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        accountsQuotasDao.getById(ts, new AccountsQuotasModel.Identity(newAccount.get().getId(),
                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2"), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(accountQuotaAfterMove);
        Assertions.assertTrue(accountQuotaAfterMove.isPresent());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, accountQuotaAfterMove.get().getTenantId());
        Assertions.assertEquals(newAccount.get().getId(), accountQuotaAfterMove.get().getAccountId());
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", accountQuotaAfterMove.get().getResourceId());
        Assertions.assertEquals("64cf1d41-c8fc-4789-8ef0-5c8cbe144084", accountQuotaAfterMove.get().getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", accountQuotaAfterMove.get().getProviderId());
        Assertions.assertEquals(50000000000L, accountQuotaAfterMove.get().getProvidedQuota());
        Assertions.assertEquals(25000000000L, accountQuotaAfterMove.get().getAllocatedQuota());
        List<FolderOperationLogModel> sourceLogAfterMove = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                        .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, "f6509efe-6496-4cd9-9019-92c8776ab0a4",
                                SortOrderDto.ASC, 100))).block();
        List<FolderOperationLogModel> destinationLogAfterMove = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                        .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, "64cf1d41-c8fc-4789-8ef0-5c8cbe144084",
                                SortOrderDto.ASC, 100))).block();
        Assertions.assertNotNull(sourceLogAfterMove);
        sourceLogAfterMove.sort(Comparator.comparing(FolderOperationLogModel::getOperationDateTime));
        Assertions.assertEquals(2, sourceLogAfterMove.size());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, sourceLogAfterMove.get(0).getTenantId());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", sourceLogAfterMove.get(0).getFolderId());
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
                        "f6509efe-6496-4cd9-9019-92c8776ab0a4", "Test", false,
                        null, "978bd75a-cf67-44ac-b944-e8ca949bdf7e", null, null)))),
                sourceLogAfterMove.get(0).getNewAccounts());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, sourceLogAfterMove.get(1).getTenantId());
        Assertions.assertEquals("f6509efe-6496-4cd9-9019-92c8776ab0a4", sourceLogAfterMove.get(1).getFolderId());
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
                        "f6509efe-6496-4cd9-9019-92c8776ab0a4", null, null,
                        null, null, null, null)))), sourceLogAfterMove.get(1).getOldAccounts());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(1L, null, null, null,
                        "64cf1d41-c8fc-4789-8ef0-5c8cbe144084", null, null, null, null, null, null)))),
                sourceLogAfterMove.get(1).getNewAccounts());
        Assertions.assertNotNull(destinationLogAfterMove);
        Assertions.assertEquals(1, destinationLogAfterMove.size());
        Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, destinationLogAfterMove.get(0).getTenantId());
        Assertions.assertEquals("64cf1d41-c8fc-4789-8ef0-5c8cbe144084", destinationLogAfterMove.get(0).getFolderId());
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
        Assertions.assertEquals(new QuotasByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", 50000000000L)),
                destinationLogAfterMove.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(0L, null))))), destinationLogAfterMove.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                new ProvisionsByResource(Map.of("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                        new ProvisionHistoryModel(50000000000L, null))))),
                destinationLogAfterMove.get(0).getNewProvisions());
        Assertions.assertEquals(0L, destinationLogAfterMove.get(0).getOrder());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(0L, null, null, null,
                        "f6509efe-6496-4cd9-9019-92c8776ab0a4", null, null,
                        null, null, null, null)))), destinationLogAfterMove.get(0).getOldAccounts());
        Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                new AccountHistoryModel(1L, null, null, null,
                        "64cf1d41-c8fc-4789-8ef0-5c8cbe144084", null, null, null, null, null, null)))),
                destinationLogAfterMove.get(0).getNewAccounts());
    }
}
