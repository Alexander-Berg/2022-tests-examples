package ru.yandex.intranet.d.grpc.accounts;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.backend.service.proto.AccountsLimit;
import ru.yandex.intranet.d.backend.service.proto.AccountsPageToken;
import ru.yandex.intranet.d.backend.service.proto.AccountsServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.GetFolderAccountRequest;
import ru.yandex.intranet.d.backend.service.proto.ListAccountsByFolderRequest;
import ru.yandex.intranet.d.backend.service.proto.ListAccountsByFolderResponse;
import ru.yandex.intranet.d.backend.service.proto.ListProviderAccountsByFolderRequest;
import ru.yandex.intranet.d.backend.service.proto.ListProviderAccountsByFolderResponse;
import ru.yandex.intranet.d.backend.service.proto.ProviderAccount;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.utils.ErrorsHelper;

/**
 * Accounts GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class AccountsServiceTest {

    @GrpcClient("inProcess")
    private AccountsServiceGrpc.AccountsServiceBlockingStub accountsService;
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
        GetFolderAccountRequest accountRequest = GetFolderAccountRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setAccountId("56a41608-84df-41c4-9653-89106462e0ce")
                .build();
        ProviderAccount account = accountsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getFolderAccount(accountRequest);
        Assertions.assertNotNull(account);
        Assertions.assertEquals("56a41608-84df-41c4-9653-89106462e0ce", account.getId());
        Assertions.assertEquals("f714c483-c347-41cc-91d0-c6722f5daac7", account.getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", account.getProviderId());
        Assertions.assertEquals("123", account.getExternalId());
        Assertions.assertEquals("dummy", account.getExternalKey().getValue());
        Assertions.assertEquals("9c44cf69-76c5-45a3-9335-57e2669f03ff", account.getAccountsSpace().getId());
        Assertions.assertEquals("тестовый аккаунт", account.getDisplayName().getValue());
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
        GetFolderAccountRequest accountRequest = GetFolderAccountRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setAccountId("12345678-9012-3456-7890-123456789012")
                .build();
        try {
            accountsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getFolderAccount(accountRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
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
        GetFolderAccountRequest accountRequest = GetFolderAccountRequest.newBuilder()
                .setFolderId("12345678-9012-3456-7890-123456789012")
                .setAccountId("56a41608-84df-41c4-9653-89106462e0ce")
                .build();
        try {
            accountsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getFolderAccount(accountRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
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
        ListAccountsByFolderRequest accountsRequest = ListAccountsByFolderRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .build();
        ListAccountsByFolderResponse page = accountsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listAccountsByFolder(accountsRequest);
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getAccountsCount() > 0);
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
        ListAccountsByFolderRequest firstRequest = ListAccountsByFolderRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setLimit(AccountsLimit.newBuilder().setLimit(1L).build())
                .build();
        ListAccountsByFolderResponse firstPage = accountsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listAccountsByFolder(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getAccountsCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());
        ListAccountsByFolderRequest secondRequest = ListAccountsByFolderRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setPageToken(AccountsPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        ListAccountsByFolderResponse secondPage = accountsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listAccountsByFolder(secondRequest);
        Assertions.assertNotNull(secondPage);
        Assertions.assertTrue(secondPage.getAccountsCount() > 0);
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
        ListAccountsByFolderRequest accountsRequest = ListAccountsByFolderRequest.newBuilder()
                .setFolderId("12345678-9012-3456-7890-123456789012")
                .build();
        try {
            accountsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .listAccountsByFolder(accountsRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void getAccountsByFolderTwoPagesNotFoundTest() {
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
        ListAccountsByFolderRequest firstRequest = ListAccountsByFolderRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setLimit(AccountsLimit.newBuilder().setLimit(1L).build())
                .build();
        ListAccountsByFolderResponse firstPage = accountsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listAccountsByFolder(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getAccountsCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());
        ListAccountsByFolderRequest secondRequest = ListAccountsByFolderRequest.newBuilder()
                .setFolderId("12345678-9012-3456-7890-123456789012")
                .setPageToken(AccountsPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        try {
            accountsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .listAccountsByFolder(secondRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
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
        ListProviderAccountsByFolderRequest accountsRequest = ListProviderAccountsByFolderRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                .build();
        ListProviderAccountsByFolderResponse page = accountsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listProviderAccountsByFolder(accountsRequest);
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getAccountsCount() > 0);
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
        ListProviderAccountsByFolderRequest firstRequest = ListProviderAccountsByFolderRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                .setLimit(AccountsLimit.newBuilder().setLimit(1L).build())
                .build();
        ListProviderAccountsByFolderResponse firstPage = accountsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listProviderAccountsByFolder(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getAccountsCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());
        ListProviderAccountsByFolderRequest secondRequest = ListProviderAccountsByFolderRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                .setPageToken(AccountsPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        ListProviderAccountsByFolderResponse secondPage = accountsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listProviderAccountsByFolder(secondRequest);
        Assertions.assertNotNull(secondPage);
        Assertions.assertTrue(secondPage.getAccountsCount() > 0);
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
        ListProviderAccountsByFolderRequest accountsRequest = ListProviderAccountsByFolderRequest.newBuilder()
                .setFolderId("12345678-9012-3456-7890-123456789012")
                .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                .build();
        try {
            accountsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .listProviderAccountsByFolder(accountsRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void getAccountsByFolderProviderTwoPagesFolderNotFoundTest() {
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
        ListProviderAccountsByFolderRequest firstRequest = ListProviderAccountsByFolderRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                .setLimit(AccountsLimit.newBuilder().setLimit(1L).build())
                .build();
        ListProviderAccountsByFolderResponse firstPage = accountsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listProviderAccountsByFolder(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getAccountsCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());
        ListProviderAccountsByFolderRequest secondRequest = ListProviderAccountsByFolderRequest.newBuilder()
                .setFolderId("12345678-9012-3456-7890-123456789012")
                .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                .setPageToken(AccountsPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        try {
            accountsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .listProviderAccountsByFolder(secondRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
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
        ListProviderAccountsByFolderRequest accountsRequest = ListProviderAccountsByFolderRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setProviderId("12345678-9012-3456-7890-123456789012")
                .build();
        try {
            accountsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .listProviderAccountsByFolder(accountsRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void getAccountsByFolderProviderTwoPagesProviderNotFoundTest() {
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
        ListProviderAccountsByFolderRequest firstRequest = ListProviderAccountsByFolderRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setProviderId("1437b48c-b2d6-4ba5-84db-5cb1f20f6533")
                .setLimit(AccountsLimit.newBuilder().setLimit(1L).build())
                .build();
        ListProviderAccountsByFolderResponse firstPage = accountsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listProviderAccountsByFolder(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getAccountsCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());
        ListProviderAccountsByFolderRequest secondRequest = ListProviderAccountsByFolderRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setProviderId("12345678-9012-3456-7890-123456789012")
                .setPageToken(AccountsPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        try {
            accountsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .listProviderAccountsByFolder(secondRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

}
