package ru.yandex.intranet.d.loaders.users;

import java.util.Optional;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.users.UserModel;

/**
 * Users loader test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class UsersLoaderTest {

    @Autowired
    private UsersLoader usersLoader;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    public void testGetUserByPassportUid() {
        Optional<UserModel> userFirst = ydbTableClient.usingSessionMonoRetryable(session ->
                usersLoader.getUserByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID)).block();
        Optional<UserModel> userSecond = ydbTableClient.usingSessionMonoRetryable(session ->
                usersLoader.getUserByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(userFirst);
        Assertions.assertTrue(userFirst.isPresent());
        Assertions.assertNotNull(userSecond);
        Assertions.assertTrue(userSecond.isPresent());
    }

    @Test
    public void testGetUserByPassportUidMissing() {
        Optional<UserModel> userFirst = ydbTableClient.usingSessionMonoRetryable(session ->
                usersLoader.getUserByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "-1", Tenants.DEFAULT_TENANT_ID)).block();
        Optional<UserModel> userSecond = ydbTableClient.usingSessionMonoRetryable(session ->
                usersLoader.getUserByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "-1", Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(userFirst);
        Assertions.assertFalse(userFirst.isPresent());
        Assertions.assertNotNull(userSecond);
        Assertions.assertFalse(userSecond.isPresent());
    }

    @Test
    public void testGetUserByPassportUidImmediate() {
        Optional<UserModel> userFirst = usersLoader
                .getUserByPassportUidImmediate(TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID).block();
        Optional<UserModel> userSecond = usersLoader
                .getUserByPassportUidImmediate(TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID).block();
        Assertions.assertNotNull(userFirst);
        Assertions.assertTrue(userFirst.isPresent());
        Assertions.assertNotNull(userSecond);
        Assertions.assertTrue(userSecond.isPresent());
    }

    @Test
    public void testGetUserByPassportUidImmediateMissing() {
        Optional<UserModel> userFirst = usersLoader
                .getUserByPassportUidImmediate("-1", Tenants.DEFAULT_TENANT_ID).block();
        Optional<UserModel> userSecond = usersLoader
                .getUserByPassportUidImmediate("-1", Tenants.DEFAULT_TENANT_ID).block();
        Assertions.assertNotNull(userFirst);
        Assertions.assertFalse(userFirst.isPresent());
        Assertions.assertNotNull(userSecond);
        Assertions.assertFalse(userSecond.isPresent());
    }

    @Test
    public void testRefresh() {
        Optional<UserModel> userBefore = ydbTableClient.usingSessionMonoRetryable(session ->
                usersLoader.getUserByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID)).block();
        Optional<UserModel> noUserBefore = ydbTableClient.usingSessionMonoRetryable(session ->
                usersLoader.getUserByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "-1", Tenants.DEFAULT_TENANT_ID)).block();
        usersLoader.refreshCache();
        Optional<UserModel> userAfter = ydbTableClient.usingSessionMonoRetryable(session ->
                usersLoader.getUserByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID)).block();
        Optional<UserModel> noUserAfter = ydbTableClient.usingSessionMonoRetryable(session ->
                usersLoader.getUserByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "-1", Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(userBefore);
        Assertions.assertTrue(userBefore.isPresent());
        Assertions.assertNotNull(noUserBefore);
        Assertions.assertFalse(noUserBefore.isPresent());
        Assertions.assertNotNull(userAfter);
        Assertions.assertTrue(userAfter.isPresent());
        Assertions.assertNotNull(noUserAfter);
        Assertions.assertFalse(noUserAfter.isPresent());
    }

}
