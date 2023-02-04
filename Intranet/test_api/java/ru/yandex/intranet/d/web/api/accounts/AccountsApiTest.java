package ru.yandex.intranet.d.web.api.accounts;

import java.time.Instant;
import java.util.UUID;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.accounts.AccountDto;

/**
 * Accounts public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class AccountsApiTest {

    @Autowired
    private WebTestClient webClient;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private YdbTableClient tableClient;

    @Test
    public void getAccountTest() {
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                accountsDao.upsertOneRetryable(ts, new AccountModel.Builder()
                                        .setId(UUID.randomUUID().toString())
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                                        .setVersion(0L)
                                        .setDeleted(false)
                                        .setDisplayName("test")
                                        .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                                        .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                                        .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .setLastAccountUpdate(Instant.now())
                                        .build())
                        ))
                .block();
        AccountDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}", "f714c483-c347-41cc-91d0-c6722f5daac7",
                        "56a41608-84df-41c4-9653-89106462e0ce")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AccountDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("56a41608-84df-41c4-9653-89106462e0ce", result.getId());
        Assertions.assertEquals("f714c483-c347-41cc-91d0-c6722f5daac7", result.getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", result.getProviderId());
        Assertions.assertEquals("123", result.getExternalId());
        Assertions.assertEquals("dummy", result.getExternalKey().orElseThrow());
        Assertions.assertEquals("9c44cf69-76c5-45a3-9335-57e2669f03ff", result.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals("тестовый аккаунт", result.getDisplayName().orElseThrow());
    }

    @Test
    public void getAccountNotFoundTest() {
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                accountsDao.upsertOneRetryable(ts, new AccountModel.Builder()
                                        .setId(UUID.randomUUID().toString())
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                                        .setVersion(0L)
                                        .setDeleted(false)
                                        .setDisplayName("test")
                                        .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                                        .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                                        .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .setLastAccountUpdate(Instant.now())
                                        .build())
                        ))
                .block();
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}", "f714c483-c347-41cc-91d0-c6722f5daac7",
                        "12345678-9012-3456-7890-123456789012")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void getAccountFolderNotFoundTest() {
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                accountsDao.upsertOneRetryable(ts, new AccountModel.Builder()
                                        .setId(UUID.randomUUID().toString())
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                                        .setVersion(0L)
                                        .setDeleted(false)
                                        .setDisplayName("test")
                                        .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                                        .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                                        .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .setLastAccountUpdate(Instant.now())
                                        .build())
                        ))
                .block();
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}", "12345678-9012-3456-7890-123456789012",
                        "56a41608-84df-41c4-9653-89106462e0ce")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void getAccountsByFolderPageTest() {
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                accountsDao.upsertOneRetryable(ts, new AccountModel.Builder()
                                        .setId(UUID.randomUUID().toString())
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                                        .setVersion(0L)
                                        .setDeleted(false)
                                        .setDisplayName("test")
                                        .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                                        .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                                        .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .setLastAccountUpdate(Instant.now())
                                        .build())
                        ))
                .block();
        PageDto<AccountDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts", "f714c483-c347-41cc-91d0-c6722f5daac7")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getAccountsByFolderTwoPagesTest() {
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                accountsDao.upsertOneRetryable(ts, new AccountModel.Builder()
                                        .setId(UUID.randomUUID().toString())
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                                        .setVersion(0L)
                                        .setDeleted(false)
                                        .setDisplayName("test")
                                        .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                                        .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                                        .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .setLastAccountUpdate(Instant.now())
                                        .build())
                        ))
                .block();
        PageDto<AccountDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts?limit={limit}", "f714c483-c347-41cc-91d0-c6722f5daac7", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<AccountDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts?limit={limit}&pageToken={token}",
                        "f714c483-c347-41cc-91d0-c6722f5daac7", 1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }

    @Test
    public void getAccountsByFolderNotFoundPageTest() {
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                accountsDao.upsertOneRetryable(ts, new AccountModel.Builder()
                                        .setId(UUID.randomUUID().toString())
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                                        .setVersion(0L)
                                        .setDeleted(false)
                                        .setDisplayName("test")
                                        .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                                        .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                                        .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .setLastAccountUpdate(Instant.now())
                                        .build())
                        ))
                .block();
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts", "12345678-9012-3456-7890-123456789012")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void getAccountsByFolderNotFoundTwoPagesTest() {
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                accountsDao.upsertOneRetryable(ts, new AccountModel.Builder()
                                        .setId(UUID.randomUUID().toString())
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                                        .setVersion(0L)
                                        .setDeleted(false)
                                        .setDisplayName("test")
                                        .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                                        .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                                        .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .setLastAccountUpdate(Instant.now())
                                        .build())
                        ))
                .block();
        PageDto<AccountDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts?limit={limit}", "f714c483-c347-41cc-91d0-c6722f5daac7", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        ErrorCollectionDto secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts?limit={limit}&pageToken={token}",
                        "12345678-9012-3456-7890-123456789012", 1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getErrors().isEmpty());
    }

    @Test
    public void getAccountsByFolderProviderPageTest() {
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                accountsDao.upsertOneRetryable(ts, new AccountModel.Builder()
                                        .setId(UUID.randomUUID().toString())
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                                        .setVersion(0L)
                                        .setDeleted(false)
                                        .setDisplayName("test")
                                        .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                                        .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                                        .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .setLastAccountUpdate(Instant.now())
                                        .build())
                        ))
                .block();
        PageDto<AccountDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/providers/{providerId}/accounts",
                        "f714c483-c347-41cc-91d0-c6722f5daac7", "1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getAccountsByFolderProviderTwoPagesTest() {
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                accountsDao.upsertOneRetryable(ts, new AccountModel.Builder()
                                        .setId(UUID.randomUUID().toString())
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                                        .setVersion(0L)
                                        .setDeleted(false)
                                        .setDisplayName("test")
                                        .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                                        .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                                        .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .setLastAccountUpdate(Instant.now())
                                        .build())
                        ))
                .block();
        PageDto<AccountDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/providers/{providerId}/accounts?limit={limit}",
                        "f714c483-c347-41cc-91d0-c6722f5daac7", "1437b48c-b2d6-4ba5-84db-5cb1f20f6533", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<AccountDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/providers/{providerId}/accounts?limit={limit}&pageToken={token}",
                        "f714c483-c347-41cc-91d0-c6722f5daac7", "1437b48c-b2d6-4ba5-84db-5cb1f20f6533",
                        1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }

    @Test
    public void getAccountsByFolderProviderFolderNotFoundPageTest() {
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                accountsDao.upsertOneRetryable(ts, new AccountModel.Builder()
                                        .setId(UUID.randomUUID().toString())
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                                        .setVersion(0L)
                                        .setDeleted(false)
                                        .setDisplayName("test")
                                        .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                                        .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                                        .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .setLastAccountUpdate(Instant.now())
                                        .build())
                        ))
                .block();
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/providers/{providerId}/accounts",
                        "12345678-9012-3456-7890-123456789012", "1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void getAccountsByFolderProviderFolderNotFoundTwoPagesTest() {
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                accountsDao.upsertOneRetryable(ts, new AccountModel.Builder()
                                        .setId(UUID.randomUUID().toString())
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                                        .setVersion(0L)
                                        .setDeleted(false)
                                        .setDisplayName("test")
                                        .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                                        .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                                        .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .setLastAccountUpdate(Instant.now())
                                        .build())
                        ))
                .block();
        PageDto<AccountDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/providers/{providerId}/accounts?limit={limit}",
                        "f714c483-c347-41cc-91d0-c6722f5daac7", "1437b48c-b2d6-4ba5-84db-5cb1f20f6533", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        ErrorCollectionDto secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/providers/{providerId}/accounts?limit={limit}&pageToken={token}",
                        "12345678-9012-3456-7890-123456789012", "1437b48c-b2d6-4ba5-84db-5cb1f20f6533",
                        1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getErrors().isEmpty());
    }

    @Test
    public void getAccountsByFolderProviderProviderNotFoundPageTest() {
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                accountsDao.upsertOneRetryable(ts, new AccountModel.Builder()
                                        .setId(UUID.randomUUID().toString())
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                                        .setVersion(0L)
                                        .setDeleted(false)
                                        .setDisplayName("test")
                                        .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                                        .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                                        .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .setLastAccountUpdate(Instant.now())
                                        .build())
                        ))
                .block();
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/providers/{providerId}/accounts",
                        "f714c483-c347-41cc-91d0-c6722f5daac7", "12345678-9012-3456-7890-123456789012")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void getAccountsByFolderProviderProviderNotFoundTwoPagesTest() {
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                accountsDao.upsertOneRetryable(ts, new AccountModel.Builder()
                                        .setId(UUID.randomUUID().toString())
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                                        .setVersion(0L)
                                        .setDeleted(false)
                                        .setDisplayName("test")
                                        .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                                        .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                                        .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .setLastAccountUpdate(Instant.now())
                                        .build())
                        ))
                .block();
        PageDto<AccountDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/providers/{providerId}/accounts?limit={limit}",
                        "f714c483-c347-41cc-91d0-c6722f5daac7", "1437b48c-b2d6-4ba5-84db-5cb1f20f6533", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        ErrorCollectionDto secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/providers/{providerId}/accounts?limit={limit}&pageToken={token}",
                        "f714c483-c347-41cc-91d0-c6722f5daac7", "12345678-9012-3456-7890-123456789012",
                        1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getErrors().isEmpty());
    }

}
