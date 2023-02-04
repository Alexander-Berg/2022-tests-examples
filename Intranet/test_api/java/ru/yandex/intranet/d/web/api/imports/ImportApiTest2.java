package ru.yandex.intranet.d.web.api.imports;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestServices;
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
import ru.yandex.intranet.d.web.model.imports.ResourceIdentityDto;
import ru.yandex.intranet.d.web.model.imports.SegmentKey;

/**
 * Import API test.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@IntegrationTest
public class ImportApiTest2 {
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
    public void simpleImportWithResourceIdentityTest() {
        ImportDto quotasToImport = new ImportDto(
                List.of(
                        new ImportFolderDto("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                                null,
                                List.of(
                                        new ImportResourceDto(
                                                null,
                                                new ResourceIdentityDto(
                                                        "hdd",
                                                        new AccountSpaceIdentityDto(null,
                                                                List.of(
                                                                        new SegmentKey("location", "sas"),
                                                                        new SegmentKey("segment", "default")
                                                                )
                                                        ),
                                                        null
                                                ),
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
    public void simpleImportToClosingServiceTest() {
        ImportDto quotasToImport = new ImportDto(
                List.of(new ImportFolderDto(null,
                        TestServices.TEST_SERVICE_ID_CLOSING,
                        List.of(new ImportResourceDto(
                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                                null,
                                "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                100L,
                                "gigabytes",
                                100L,
                                "gigabytes"
                        )),
                        List.of()
                )));

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
    }

    @Test
    public void simpleImportToFolderInClosingServiceTest() {
        ImportDto quotasToImport = new ImportDto(
                List.of(new ImportFolderDto(TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE,
                        null,
                        List.of(new ImportResourceDto(
                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                                null,
                                "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                100L,
                                "gigabytes",
                                100L,
                                "gigabytes"
                        )),
                        List.of()
                )));

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
    }

    @Test
    public void simpleImportToRenamingServiceTest() {
        ImportDto quotasToImport = new ImportDto(
                List.of(new ImportFolderDto(null,
                        TestServices.TEST_SERVICE_ID_RENAMING,
                        List.of(new ImportResourceDto(
                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                                null,
                                "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                100L,
                                "gigabytes",
                                100L,
                                "gigabytes"
                        )),
                        List.of()
                )));

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/import/_importQuotas")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(quotasToImport)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void simpleImportToFolderInRenamingServiceTest() {
        ImportDto quotasToImport = new ImportDto(
                List.of(new ImportFolderDto(TestFolders.TEST_FOLDER_IN_RENAMING_SERVICE,
                        null,
                        List.of(new ImportResourceDto(
                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                                null,
                                "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                100L,
                                "gigabytes",
                                100L,
                                "gigabytes"
                        )),
                        List.of()
                )));

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/import/_importQuotas")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(quotasToImport)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void simpleImportToNonExportableServiceTest() {
        ImportDto quotasToImport = new ImportDto(
                List.of(new ImportFolderDto(null,
                        TestServices.TEST_SERVICE_ID_NON_EXPORTABLE,
                        List.of(new ImportResourceDto(
                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                                null,
                                "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                100L,
                                "gigabytes",
                                100L,
                                "gigabytes"
                        )),
                        List.of()
                )));

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
    }

    @Test
    public void simpleImportToFolderInNonExportableServiceTest() {
        ImportDto quotasToImport = new ImportDto(
                List.of(new ImportFolderDto(TestFolders.TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE,
                        null,
                        List.of(new ImportResourceDto(
                                "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2",
                                null,
                                "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                                100L,
                                "gigabytes",
                                100L,
                                "gigabytes"
                        )),
                        List.of()
                )));

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
    }

    @Test
    public void importToFolderInProviderWithDisplayNameUnsupportedTest() {
        ImportDto quotasToImport = new ImportDto(
                List.of(new ImportFolderDto(TestFolders.TEST_FOLDER_1_ID,
                        null,
                        List.of(),
                        List.of(new ImportAccountDto(
                                UUID.randomUUID().toString(),
                                "test-account",
                                null,
                                false,
                                TestProviders.CLAUD2_ID,
                                List.of(),
                                new AccountSpaceIdentityDto(
                                        "b74ecfec-842f-4669-955a-3a7112fd8387",
                                        null
                                ),
                                false
                        ))
                )));

        webClient
                .mutateWith(MockUser.uid(TestUsers.CLAUD2_ADMIN_UID))
                .post()
                .uri("/api/v1/import/_importQuotas")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(quotasToImport)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void dAdminWithoutRolesCanImportTest() {
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
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_WITHOUT_ROLES_UID))
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
        Assertions.assertEquals(TestUsers.D_ADMIN_WITHOUT_ROLES_ID, log.get(0).getAuthorUserId().get());
        Assertions.assertTrue(log.get(0).getAuthorUserUid().isPresent());
        Assertions.assertEquals(TestUsers.D_ADMIN_WITHOUT_ROLES_UID, log.get(0).getAuthorUserUid().get());
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
}
