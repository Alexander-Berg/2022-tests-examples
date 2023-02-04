package ru.yandex.intranet.d.dao.users;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.datasource.impl.YdbRetry;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.metrics.YdbMetrics;
import ru.yandex.intranet.d.model.TenantId;
import ru.yandex.intranet.d.model.users.StaffAffiliation;
import ru.yandex.intranet.d.model.users.UserModel;
import ru.yandex.intranet.d.model.users.UserServiceRoles;

/**
 * Users DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class UsersDaoTest {

    @Autowired
    private UsersDao usersDao;
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private YdbMetrics ydbMetrics;
    @Value("${ydb.transactionRetries}")
    private long transactionRetries;

    @Test
    public void testGetById() {
        Optional<UserModel> userOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestUsers.USER_1_ID, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(userOne);
        Assertions.assertTrue(userOne.isPresent());
    }

    @Test
    public void testGetByIdMissing() {
        Optional<UserModel> userOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(userOne);
        Assertions.assertFalse(userOne.isPresent());
    }

    @Test
    public void testGetByPassportUid() {
        Optional<UserModel> userOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(userOne);
        Assertions.assertTrue(userOne.isPresent());
    }

    @Test
    public void testGetByPassportUidMissing() {
        Optional<UserModel> userOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(userOne);
        Assertions.assertFalse(userOne.isPresent());
    }

    @Test
    public void testGetByPassportUidImmediate() {
        Optional<UserModel> userOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.STALE_READ_ONLY),
                        TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(userOne);
        Assertions.assertTrue(userOne.isPresent());
    }

    @Test
    public void testGetByPassportUidImmediateMissing() {
        Optional<UserModel> userOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.STALE_READ_ONLY),
                        "-1", Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(userOne);
        Assertions.assertFalse(userOne.isPresent());
    }

    @Test
    public void testGetByPassportLogin() {
        Optional<UserModel> userOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestUsers.USER_1_LOGIN, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(userOne);
        Assertions.assertTrue(userOne.isPresent());
    }

    @Test
    public void testGetByPassportLoginMissing() {
        Optional<UserModel> userOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "missing", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(userOne);
        Assertions.assertFalse(userOne.isPresent());
    }

    @Test
    public void testGetByStaffId() {
        Optional<UserModel> userOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestUsers.USER_1_STAFF_ID, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(userOne);
        Assertions.assertTrue(userOne.isPresent());
    }

    @Test
    public void testGetByStaffIdMissing() {
        Optional<UserModel> userOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        -1L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(userOne);
        Assertions.assertFalse(userOne.isPresent());
    }

    @Test
    public void testGetByIds() {
        List<UserModel> oneUser = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestUsers.USER_1_ID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<UserModel> twoUsers = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestUsers.USER_1_ID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_ID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(oneUser);
        Assertions.assertNotNull(twoUsers);
        Assertions.assertEquals(1, oneUser.size());
        Assertions.assertEquals(2, twoUsers.size());
    }

    @Test
    public void testGetByIdsMissing() {
        List<UserModel> noUsers = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(noUsers);
        Assertions.assertTrue(noUsers.isEmpty());
    }

    @Test
    public void testGetByPassportUids() {
        List<UserModel> oneUser = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUids(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<UserModel> twoUsers = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUids(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_UID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(oneUser);
        Assertions.assertNotNull(twoUsers);
        Assertions.assertEquals(1, oneUser.size());
        Assertions.assertEquals(2, twoUsers.size());
    }

    @Test
    public void testGetByPassportUidsMissing() {
        List<UserModel> noUsers = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUids(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of("-1", Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(noUsers);
        Assertions.assertTrue(noUsers.isEmpty());
    }

    @Test
    public void testGetByPassportUidsImmediate() {
        List<UserModel> oneUser = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUids(session.asTxCommitRetryable(TransactionMode.STALE_READ_ONLY),
                        List.of(Tuples.of(TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID)))).block();
        List<UserModel> twoUsers = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUids(session.asTxCommitRetryable(TransactionMode.STALE_READ_ONLY),
                        List.of(Tuples.of(TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_UID, Tenants.DEFAULT_TENANT_ID)))).block();
        Assertions.assertNotNull(oneUser);
        Assertions.assertNotNull(twoUsers);
        Assertions.assertEquals(1, oneUser.size());
        Assertions.assertEquals(2, twoUsers.size());
    }

    @Test
    public void testGetByPassportUidsImmediateMissing() {
        List<UserModel> noUsers = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUids(session.asTxCommitRetryable(TransactionMode.STALE_READ_ONLY),
                        List.of(Tuples.of("-1", Tenants.DEFAULT_TENANT_ID)))).block();
        Assertions.assertNotNull(noUsers);
        Assertions.assertTrue(noUsers.isEmpty());
    }

    @Test
    public void testGetByPassportLogins() {
        List<UserModel> oneUser = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogins(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestUsers.USER_1_LOGIN, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<UserModel> twoUsers = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogins(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestUsers.USER_1_LOGIN, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_LOGIN, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(oneUser);
        Assertions.assertNotNull(twoUsers);
        Assertions.assertEquals(1, oneUser.size());
        Assertions.assertEquals(2, twoUsers.size());
    }

    @Test
    public void testGetByPassportLoginsMissing() {
        List<UserModel> noUsers = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogins(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of("missing", Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(noUsers);
        Assertions.assertTrue(noUsers.isEmpty());
    }

    @Test
    public void testGetByStaffIds() {
        List<UserModel> oneUser = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestUsers.USER_1_STAFF_ID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<UserModel> twoUsers = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestUsers.USER_1_STAFF_ID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_STAFF_ID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(oneUser);
        Assertions.assertNotNull(twoUsers);
        Assertions.assertEquals(1, oneUser.size());
        Assertions.assertEquals(2, twoUsers.size());
    }

    @Test
    public void testGetByStaffIdsMissing() {
        List<UserModel> noUsers = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(-1L, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(noUsers);
        Assertions.assertTrue(noUsers.isEmpty());
    }

    @Test
    public void testUpsertUserFull() {
        UserModel user = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "999", "test-login-1", 888L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-1", "Имя-1", "LastName-1", "Фамилия-1", false, false, Collections.emptyMap(), "M");
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.upsertUserRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), user)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUser = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        user.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUser);
        Assertions.assertTrue(newUser.isPresent());
        Assertions.assertEquals(user, newUser.get());
        Assertions.assertNotNull(newUserByPassportUid);
        Assertions.assertTrue(newUserByPassportUid.isPresent());
        Assertions.assertEquals(user, newUserByPassportUid.get());
        Assertions.assertNotNull(newUserByPassportLogin);
        Assertions.assertTrue(newUserByPassportLogin.isPresent());
        Assertions.assertEquals(user, newUserByPassportLogin.get());
        Assertions.assertNotNull(newUserByStaffId);
        Assertions.assertTrue(newUserByStaffId.isPresent());
        Assertions.assertEquals(user, newUserByStaffId.get());
    }

    @Test
    public void testUpsertUserMinimal() {
        UserModel user = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                null, null, null, null, null, null,
                "FirstName-1", "Имя-1", "LastName-1", "Фамилия-1", false, false, Collections.emptyMap(), "F");
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.upsertUserRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), user)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUser = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        user.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUser);
        Assertions.assertTrue(newUser.isPresent());
        Assertions.assertEquals(user, newUser.get());
    }

    @Test
    public void testUpsertUsersFull() {
        UserModel userOne = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "999", "test-login-1", 888L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-1", "Имя-1", "LastName-1", "Фамилия-1", false, false, Collections.emptyMap(), "M");
        UserModel userTwo = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "998", "test-login-2", 889L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-2", "Имя-2", "LastName-2", "Фамилия-2", false, false, Collections.emptyMap(), "F");
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.upsertUsersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(userOne, userTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "998", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-2", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        889L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUserOne);
        Assertions.assertTrue(newUserOne.isPresent());
        Assertions.assertEquals(userOne, newUserOne.get());
        Assertions.assertNotNull(newUserOneByPassportUid);
        Assertions.assertTrue(newUserOneByPassportUid.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportUid.get());
        Assertions.assertNotNull(newUserOneByPassportLogin);
        Assertions.assertTrue(newUserOneByPassportLogin.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportLogin.get());
        Assertions.assertNotNull(newUserOneByStaffId);
        Assertions.assertTrue(newUserOneByStaffId.isPresent());
        Assertions.assertEquals(userOne, newUserOneByStaffId.get());
        Assertions.assertNotNull(newUserTwo);
        Assertions.assertTrue(newUserTwo.isPresent());
        Assertions.assertEquals(userTwo, newUserTwo.get());
        Assertions.assertNotNull(newUserTwoByPassportUid);
        Assertions.assertTrue(newUserTwoByPassportUid.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportUid.get());
        Assertions.assertNotNull(newUserTwoByPassportLogin);
        Assertions.assertTrue(newUserTwoByPassportLogin.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportLogin.get());
        Assertions.assertNotNull(newUserTwoByStaffId);
        Assertions.assertTrue(newUserTwoByStaffId.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByStaffId.get());
    }

    @Test
    public void testUpsertUsersMinimal() {
        UserModel userOne = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                null, null, null, null, null, null,
                "FirstName-1", "Имя-1", "LastName-1", "Фамилия-1", false, false, Collections.emptyMap(), "M");
        UserModel userTwo = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                null, null, null, null, null, null,
                "FirstName-2", "Имя-2", "LastName-2", "Фамилия-2", false, false, Collections.emptyMap(), "F");
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.upsertUsersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(userOne, userTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUserOne);
        Assertions.assertTrue(newUserOne.isPresent());
        Assertions.assertEquals(userOne, newUserOne.get());
        Assertions.assertNotNull(newUserTwo);
        Assertions.assertTrue(newUserTwo.isPresent());
        Assertions.assertEquals(userTwo, newUserTwo.get());
    }

    @Test
    public void testUpsertUsersPrepared() {
        UserModel userOne = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "999", "test-login-1", 888L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-1", "Имя-1", "LastName-1", "Фамилия-1", false, false, Collections.emptyMap(), "M");
        UserModel userTwo = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "998", "test-login-2", 889L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-2", "Имя-2", "LastName-2", "Фамилия-2", false, false, Collections.emptyMap(), "F");
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.prepareUpsertUsers(session)
                .flatMap(query -> usersDao.executeUpsertUsersRetryable(query
                        .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(userOne, userTwo)))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "998", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-2", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        889L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUserOne);
        Assertions.assertTrue(newUserOne.isPresent());
        Assertions.assertEquals(userOne, newUserOne.get());
        Assertions.assertNotNull(newUserOneByPassportUid);
        Assertions.assertTrue(newUserOneByPassportUid.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportUid.get());
        Assertions.assertNotNull(newUserOneByPassportLogin);
        Assertions.assertTrue(newUserOneByPassportLogin.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportLogin.get());
        Assertions.assertNotNull(newUserOneByStaffId);
        Assertions.assertTrue(newUserOneByStaffId.isPresent());
        Assertions.assertEquals(userOne, newUserOneByStaffId.get());
        Assertions.assertNotNull(newUserTwo);
        Assertions.assertTrue(newUserTwo.isPresent());
        Assertions.assertEquals(userTwo, newUserTwo.get());
        Assertions.assertNotNull(newUserTwoByPassportUid);
        Assertions.assertTrue(newUserTwoByPassportUid.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportUid.get());
        Assertions.assertNotNull(newUserTwoByPassportLogin);
        Assertions.assertTrue(newUserTwoByPassportLogin.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportLogin.get());
        Assertions.assertNotNull(newUserTwoByStaffId);
        Assertions.assertTrue(newUserTwoByStaffId.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByStaffId.get());
    }

    @Test
    public void testUpdateUserFull() {
        UserModel user = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "999", "test-login-1", 888L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-1", "Имя-1", "LastName-1", "Фамилия-1", false, false, Collections.emptyMap(), "M");
        UserModel updatedUser = getUser(user.getId(), Tenants.DEFAULT_TENANT_ID,
                "998", "test-login-1-up", 889L,
                true, true, StaffAffiliation.EXTERNAL,
                "FirstName-1-up", "Имя-1-up", "LastName-1-up", "Фамилия-1-up", false, true, Collections.emptyMap(),
                "F");
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.upsertUserRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), user)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUser = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        user.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUser);
        Assertions.assertTrue(newUser.isPresent());
        Assertions.assertEquals(user, newUser.get());
        Assertions.assertNotNull(newUserByPassportUid);
        Assertions.assertTrue(newUserByPassportUid.isPresent());
        Assertions.assertEquals(user, newUserByPassportUid.get());
        Assertions.assertNotNull(newUserByPassportLogin);
        Assertions.assertTrue(newUserByPassportLogin.isPresent());
        Assertions.assertEquals(user, newUserByPassportLogin.get());
        Assertions.assertNotNull(newUserByStaffId);
        Assertions.assertTrue(newUserByStaffId.isPresent());
        Assertions.assertEquals(user, newUserByStaffId.get());
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.updateUserRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), updatedUser)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        user.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByPassportUidUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "998", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByPassportLoginUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1-up", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByStaffIdUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        889L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByPassportUidUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByPassportLoginUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByStaffIdUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUserUpdated);
        Assertions.assertTrue(newUserUpdated.isPresent());
        Assertions.assertEquals(updatedUser, newUserUpdated.get());
        Assertions.assertNotNull(newUserByPassportUidUpdated);
        Assertions.assertTrue(newUserByPassportUidUpdated.isPresent());
        Assertions.assertEquals(updatedUser, newUserByPassportUidUpdated.get());
        Assertions.assertNotNull(newUserByPassportLoginUpdated);
        Assertions.assertTrue(newUserByPassportLoginUpdated.isPresent());
        Assertions.assertEquals(updatedUser, newUserByPassportLoginUpdated.get());
        Assertions.assertNotNull(newUserByStaffIdUpdated);
        Assertions.assertTrue(newUserByStaffIdUpdated.isPresent());
        Assertions.assertEquals(updatedUser, newUserByStaffIdUpdated.get());
        Assertions.assertNotNull(newUserByPassportUidUpdatedPrev);
        Assertions.assertFalse(newUserByPassportUidUpdatedPrev.isPresent());
        Assertions.assertNotNull(newUserByPassportLoginUpdatedPrev);
        Assertions.assertFalse(newUserByPassportLoginUpdatedPrev.isPresent());
        Assertions.assertNotNull(newUserByStaffIdUpdatedPrev);
        Assertions.assertFalse(newUserByStaffIdUpdatedPrev.isPresent());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testUpdateUsersFull() {
        UserModel userOne = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "999", "test-login-1", 888L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-1", "Имя-1", "LastName-1", "Фамилия-1", false, false, Collections.emptyMap(), "M");
        UserModel userTwo = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "979", "test-login-2", 878L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-2", "Имя-2", "LastName-2", "Фамилия-2", false, false, Collections.emptyMap(), "F");
        UserModel updatedUserOne = getUser(userOne.getId(), Tenants.DEFAULT_TENANT_ID,
                "998", "test-login-1-up", 889L,
                true, true, StaffAffiliation.EXTERNAL,
                "FirstName-1-up", "Имя-1-up", "LastName-1-up", "Фамилия-1-up", false, true, Collections.emptyMap(),
                "M");
        UserModel updatedUserTwo = getUser(userTwo.getId(), Tenants.DEFAULT_TENANT_ID,
                "978", "test-login-2-up", 879L,
                true, true, StaffAffiliation.EXTERNAL,
                "FirstName-2-up", "Имя-2-up", "LastName-2-up", "Фамилия-2-up", false, true, Collections.emptyMap(),
                "F");
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.upsertUsersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(userOne, userTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "979", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-2", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        878L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUserOne);
        Assertions.assertTrue(newUserOne.isPresent());
        Assertions.assertEquals(userOne, newUserOne.get());
        Assertions.assertNotNull(newUserOneByPassportUid);
        Assertions.assertTrue(newUserOneByPassportUid.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportUid.get());
        Assertions.assertNotNull(newUserOneByPassportLogin);
        Assertions.assertTrue(newUserOneByPassportLogin.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportLogin.get());
        Assertions.assertNotNull(newUserOneByStaffId);
        Assertions.assertTrue(newUserOneByStaffId.isPresent());
        Assertions.assertEquals(userOne, newUserOneByStaffId.get());
        Assertions.assertNotNull(newUserTwo);
        Assertions.assertTrue(newUserTwo.isPresent());
        Assertions.assertEquals(userTwo, newUserTwo.get());
        Assertions.assertNotNull(newUserTwoByPassportUid);
        Assertions.assertTrue(newUserTwoByPassportUid.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportUid.get());
        Assertions.assertNotNull(newUserTwoByPassportLogin);
        Assertions.assertTrue(newUserTwoByPassportLogin.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportLogin.get());
        Assertions.assertNotNull(newUserTwoByStaffId);
        Assertions.assertTrue(newUserTwoByStaffId.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByStaffId.get());
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.updateUsersRetryable(session
                        .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                List.of(updatedUserOne, updatedUserTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportUidUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "998", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportLoginUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1-up", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByStaffIdUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        889L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportUidUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportLoginUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByStaffIdUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportUidUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "978", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportLoginUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-2-up", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByStaffIdUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        879L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportUidUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "979", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportLoginUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-2", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByStaffIdUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        878L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUserOneUpdated);
        Assertions.assertTrue(newUserOneUpdated.isPresent());
        Assertions.assertEquals(updatedUserOne, newUserOneUpdated.get());
        Assertions.assertNotNull(newUserOneByPassportUidUpdated);
        Assertions.assertTrue(newUserOneByPassportUidUpdated.isPresent());
        Assertions.assertEquals(updatedUserOne, newUserOneByPassportUidUpdated.get());
        Assertions.assertNotNull(newUserOneByPassportLoginUpdated);
        Assertions.assertTrue(newUserOneByPassportLoginUpdated.isPresent());
        Assertions.assertEquals(updatedUserOne, newUserOneByPassportLoginUpdated.get());
        Assertions.assertNotNull(newUserOneByStaffIdUpdated);
        Assertions.assertTrue(newUserOneByStaffIdUpdated.isPresent());
        Assertions.assertEquals(updatedUserOne, newUserOneByStaffIdUpdated.get());
        Assertions.assertNotNull(newUserOneByPassportUidUpdatedPrev);
        Assertions.assertFalse(newUserOneByPassportUidUpdatedPrev.isPresent());
        Assertions.assertNotNull(newUserOneByPassportLoginUpdatedPrev);
        Assertions.assertFalse(newUserOneByPassportLoginUpdatedPrev.isPresent());
        Assertions.assertNotNull(newUserOneByStaffIdUpdatedPrev);
        Assertions.assertFalse(newUserOneByStaffIdUpdatedPrev.isPresent());
        Assertions.assertNotNull(newUserTwoUpdated);
        Assertions.assertTrue(newUserTwoUpdated.isPresent());
        Assertions.assertEquals(updatedUserTwo, newUserTwoUpdated.get());
        Assertions.assertNotNull(newUserTwoByPassportUidUpdated);
        Assertions.assertTrue(newUserTwoByPassportUidUpdated.isPresent());
        Assertions.assertEquals(updatedUserTwo, newUserTwoByPassportUidUpdated.get());
        Assertions.assertNotNull(newUserTwoByPassportLoginUpdated);
        Assertions.assertTrue(newUserTwoByPassportLoginUpdated.isPresent());
        Assertions.assertEquals(updatedUserTwo, newUserTwoByPassportLoginUpdated.get());
        Assertions.assertNotNull(newUserTwoByStaffIdUpdated);
        Assertions.assertTrue(newUserTwoByStaffIdUpdated.isPresent());
        Assertions.assertEquals(updatedUserTwo, newUserTwoByStaffIdUpdated.get());
        Assertions.assertNotNull(newUserTwoByPassportUidUpdatedPrev);
        Assertions.assertFalse(newUserTwoByPassportUidUpdatedPrev.isPresent());
        Assertions.assertNotNull(newUserTwoByPassportLoginUpdatedPrev);
        Assertions.assertFalse(newUserTwoByPassportLoginUpdatedPrev.isPresent());
        Assertions.assertNotNull(newUserTwoByStaffIdUpdatedPrev);
        Assertions.assertFalse(newUserTwoByStaffIdUpdatedPrev.isPresent());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testUpdateUsersPrepared() {
        UserModel userOne = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "999", "test-login-1", 888L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-1", "Имя-1", "LastName-1", "Фамилия-1", false, false, Collections.emptyMap(), "M");
        UserModel userTwo = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "979", "test-login-2", 878L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-2", "Имя-2", "LastName-2", "Фамилия-2", false, false, Collections.emptyMap(), "F");
        UserModel updatedUserOne = getUser(userOne.getId(), Tenants.DEFAULT_TENANT_ID,
                "998", "test-login-1-up", 889L,
                true, true, StaffAffiliation.EXTERNAL,
                "FirstName-1-up", "Имя-1-up", "LastName-1-up", "Фамилия-1-up", false, true, Collections.emptyMap(),
                "M");
        UserModel updatedUserTwo = getUser(userTwo.getId(), Tenants.DEFAULT_TENANT_ID,
                "978", "test-login-2-up", 879L,
                true, true, StaffAffiliation.EXTERNAL,
                "FirstName-2-up", "Имя-2-up", "LastName-2-up", "Фамилия-2-up", false, true, Collections.emptyMap(),
                "F");
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.upsertUsersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(userOne, userTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "979", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-2", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        878L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUserOne);
        Assertions.assertTrue(newUserOne.isPresent());
        Assertions.assertEquals(userOne, newUserOne.get());
        Assertions.assertNotNull(newUserOneByPassportUid);
        Assertions.assertTrue(newUserOneByPassportUid.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportUid.get());
        Assertions.assertNotNull(newUserOneByPassportLogin);
        Assertions.assertTrue(newUserOneByPassportLogin.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportLogin.get());
        Assertions.assertNotNull(newUserOneByStaffId);
        Assertions.assertTrue(newUserOneByStaffId.isPresent());
        Assertions.assertEquals(userOne, newUserOneByStaffId.get());
        Assertions.assertNotNull(newUserTwo);
        Assertions.assertTrue(newUserTwo.isPresent());
        Assertions.assertEquals(userTwo, newUserTwo.get());
        Assertions.assertNotNull(newUserTwoByPassportUid);
        Assertions.assertTrue(newUserTwoByPassportUid.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportUid.get());
        Assertions.assertNotNull(newUserTwoByPassportLogin);
        Assertions.assertTrue(newUserTwoByPassportLogin.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportLogin.get());
        Assertions.assertNotNull(newUserTwoByStaffId);
        Assertions.assertTrue(newUserTwoByStaffId.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByStaffId.get());
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.prepareUpdateUsers(session)
                .flatMap(query -> usersDao.executeUpdateUsersRetryable(query
                                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(updatedUserOne, updatedUserTwo)))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportUidUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "998", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportLoginUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1-up", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByStaffIdUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        889L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportUidUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportLoginUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByStaffIdUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportUidUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "978", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportLoginUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-2-up", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByStaffIdUpdated = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        879L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportUidUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "979", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportLoginUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-2", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByStaffIdUpdatedPrev = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        878L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUserOneUpdated);
        Assertions.assertTrue(newUserOneUpdated.isPresent());
        Assertions.assertEquals(updatedUserOne, newUserOneUpdated.get());
        Assertions.assertNotNull(newUserOneByPassportUidUpdated);
        Assertions.assertTrue(newUserOneByPassportUidUpdated.isPresent());
        Assertions.assertEquals(updatedUserOne, newUserOneByPassportUidUpdated.get());
        Assertions.assertNotNull(newUserOneByPassportLoginUpdated);
        Assertions.assertTrue(newUserOneByPassportLoginUpdated.isPresent());
        Assertions.assertEquals(updatedUserOne, newUserOneByPassportLoginUpdated.get());
        Assertions.assertNotNull(newUserOneByStaffIdUpdated);
        Assertions.assertTrue(newUserOneByStaffIdUpdated.isPresent());
        Assertions.assertEquals(updatedUserOne, newUserOneByStaffIdUpdated.get());
        Assertions.assertNotNull(newUserOneByPassportUidUpdatedPrev);
        Assertions.assertFalse(newUserOneByPassportUidUpdatedPrev.isPresent());
        Assertions.assertNotNull(newUserOneByPassportLoginUpdatedPrev);
        Assertions.assertFalse(newUserOneByPassportLoginUpdatedPrev.isPresent());
        Assertions.assertNotNull(newUserOneByStaffIdUpdatedPrev);
        Assertions.assertFalse(newUserOneByStaffIdUpdatedPrev.isPresent());
        Assertions.assertNotNull(newUserTwoUpdated);
        Assertions.assertTrue(newUserTwoUpdated.isPresent());
        Assertions.assertEquals(updatedUserTwo, newUserTwoUpdated.get());
        Assertions.assertNotNull(newUserTwoByPassportUidUpdated);
        Assertions.assertTrue(newUserTwoByPassportUidUpdated.isPresent());
        Assertions.assertEquals(updatedUserTwo, newUserTwoByPassportUidUpdated.get());
        Assertions.assertNotNull(newUserTwoByPassportLoginUpdated);
        Assertions.assertTrue(newUserTwoByPassportLoginUpdated.isPresent());
        Assertions.assertEquals(updatedUserTwo, newUserTwoByPassportLoginUpdated.get());
        Assertions.assertNotNull(newUserTwoByStaffIdUpdated);
        Assertions.assertTrue(newUserTwoByStaffIdUpdated.isPresent());
        Assertions.assertEquals(updatedUserTwo, newUserTwoByStaffIdUpdated.get());
        Assertions.assertNotNull(newUserTwoByPassportUidUpdatedPrev);
        Assertions.assertFalse(newUserTwoByPassportUidUpdatedPrev.isPresent());
        Assertions.assertNotNull(newUserTwoByPassportLoginUpdatedPrev);
        Assertions.assertFalse(newUserTwoByPassportLoginUpdatedPrev.isPresent());
        Assertions.assertNotNull(newUserTwoByStaffIdUpdatedPrev);
        Assertions.assertFalse(newUserTwoByStaffIdUpdatedPrev.isPresent());
    }

    @Test
    public void testGetUsersByExternalIds() {
        UserModel userOne = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "999", "test-login-1", 888L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-1", "Имя-1", "LastName-1", "Фамилия-1", false, false, Collections.emptyMap(), "M");
        UserModel userTwo = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "998", "test-login-2", 889L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-2", "Имя-2", "LastName-2", "Фамилия-2", false, false, Collections.emptyMap(), "F");
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.upsertUsersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(userOne, userTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "998", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-2", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        889L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUserOne);
        Assertions.assertTrue(newUserOne.isPresent());
        Assertions.assertEquals(userOne, newUserOne.get());
        Assertions.assertNotNull(newUserOneByPassportUid);
        Assertions.assertTrue(newUserOneByPassportUid.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportUid.get());
        Assertions.assertNotNull(newUserOneByPassportLogin);
        Assertions.assertTrue(newUserOneByPassportLogin.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportLogin.get());
        Assertions.assertNotNull(newUserOneByStaffId);
        Assertions.assertTrue(newUserOneByStaffId.isPresent());
        Assertions.assertEquals(userOne, newUserOneByStaffId.get());
        Assertions.assertNotNull(newUserTwo);
        Assertions.assertTrue(newUserTwo.isPresent());
        Assertions.assertEquals(userTwo, newUserTwo.get());
        Assertions.assertNotNull(newUserTwoByPassportUid);
        Assertions.assertTrue(newUserTwoByPassportUid.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportUid.get());
        Assertions.assertNotNull(newUserTwoByPassportLogin);
        Assertions.assertTrue(newUserTwoByPassportLogin.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportLogin.get());
        Assertions.assertNotNull(newUserTwoByStaffId);
        Assertions.assertTrue(newUserTwoByStaffId.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByStaffId.get());
        List<UserModel> twoUsersOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByExternalIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_UID, Tenants.DEFAULT_TENANT_ID)),
                        List.of(Tuples.of(TestUsers.USER_1_LOGIN, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_LOGIN, Tenants.DEFAULT_TENANT_ID)),
                        List.of(Tuples.of(TestUsers.USER_1_STAFF_ID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_STAFF_ID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(twoUsersOne);
        Assertions.assertEquals(2, twoUsersOne.size());
        List<UserModel> twoUsersTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByExternalIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(),
                        List.of(Tuples.of(TestUsers.USER_1_LOGIN, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_LOGIN, Tenants.DEFAULT_TENANT_ID)),
                        List.of(Tuples.of(TestUsers.USER_1_STAFF_ID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_STAFF_ID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(twoUsersTwo);
        Assertions.assertEquals(2, twoUsersTwo.size());
        List<UserModel> twoUsersThree = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByExternalIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_UID, Tenants.DEFAULT_TENANT_ID)),
                        List.of(),
                        List.of(Tuples.of(TestUsers.USER_1_STAFF_ID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_STAFF_ID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(twoUsersThree);
        Assertions.assertEquals(2, twoUsersThree.size());
        List<UserModel> twoUsersFour = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByExternalIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_UID, Tenants.DEFAULT_TENANT_ID)),
                        List.of(Tuples.of(TestUsers.USER_1_LOGIN, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_LOGIN, Tenants.DEFAULT_TENANT_ID)),
                        List.of()).retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                .block();
        Assertions.assertNotNull(twoUsersFour);
        Assertions.assertEquals(2, twoUsersFour.size());
        List<UserModel> twoUsersFive = ydbTableClient.usingSessionMonoRetryable(session ->
                        usersDao.getByExternalIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                        List.of(Tuples.of(TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID),
                                                Tuples.of(TestUsers.USER_2_UID, Tenants.DEFAULT_TENANT_ID)),
                                        List.of(), List.of())
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                .block();
        Assertions.assertNotNull(twoUsersFive);
        Assertions.assertEquals(2, twoUsersFive.size());
        List<UserModel> twoUsersSix = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByExternalIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(),
                        List.of(Tuples.of(TestUsers.USER_1_LOGIN, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_LOGIN, Tenants.DEFAULT_TENANT_ID)),
                        List.of()).retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                .block();
        Assertions.assertNotNull(twoUsersSix);
        Assertions.assertEquals(2, twoUsersSix.size());
        List<UserModel> twoUsersSeven = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByExternalIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(), List.of(),
                        List.of(Tuples.of(TestUsers.USER_1_STAFF_ID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestUsers.USER_2_STAFF_ID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(twoUsersSeven);
        Assertions.assertEquals(2, twoUsersSeven.size());
        List<UserModel> twoUsersEight = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByExternalIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(), List.of(), List.of())
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(twoUsersEight);
        Assertions.assertTrue(twoUsersEight.isEmpty());
    }

    @Test
    public void testGetUsersByExternalIdsPrepared() {
        UserModel userOne = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "999", "test-login-1", 888L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-1", "Имя-1", "LastName-1", "Фамилия-1", false, false, Collections.emptyMap(), "M");
        UserModel userTwo = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "998", "test-login-2", 889L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-2", "Имя-2", "LastName-2", "Фамилия-2", false, false, Collections.emptyMap(), "F");
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.upsertUsersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(userOne, userTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "998", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-2", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        889L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUserOne);
        Assertions.assertTrue(newUserOne.isPresent());
        Assertions.assertEquals(userOne, newUserOne.get());
        Assertions.assertNotNull(newUserOneByPassportUid);
        Assertions.assertTrue(newUserOneByPassportUid.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportUid.get());
        Assertions.assertNotNull(newUserOneByPassportLogin);
        Assertions.assertTrue(newUserOneByPassportLogin.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportLogin.get());
        Assertions.assertNotNull(newUserOneByStaffId);
        Assertions.assertTrue(newUserOneByStaffId.isPresent());
        Assertions.assertEquals(userOne, newUserOneByStaffId.get());
        Assertions.assertNotNull(newUserTwo);
        Assertions.assertTrue(newUserTwo.isPresent());
        Assertions.assertEquals(userTwo, newUserTwo.get());
        Assertions.assertNotNull(newUserTwoByPassportUid);
        Assertions.assertTrue(newUserTwoByPassportUid.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportUid.get());
        Assertions.assertNotNull(newUserTwoByPassportLogin);
        Assertions.assertTrue(newUserTwoByPassportLogin.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportLogin.get());
        Assertions.assertNotNull(newUserTwoByStaffId);
        Assertions.assertTrue(newUserTwoByStaffId.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByStaffId.get());
        List<UserModel> twoUsers = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.prepareGetByExternalIds(session)
                        .flatMap(query -> usersDao.executeGetByExternalIds(query
                                        .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                                List.of(Tuples.of(TestUsers.USER_1_UID, Tenants.DEFAULT_TENANT_ID),
                                        Tuples.of(TestUsers.USER_2_UID, Tenants.DEFAULT_TENANT_ID)),
                                List.of(Tuples.of(TestUsers.USER_1_LOGIN, Tenants.DEFAULT_TENANT_ID),
                                        Tuples.of(TestUsers.USER_2_LOGIN, Tenants.DEFAULT_TENANT_ID)),
                                List.of(Tuples.of(TestUsers.USER_1_STAFF_ID, Tenants.DEFAULT_TENANT_ID),
                                        Tuples.of(TestUsers.USER_2_STAFF_ID, Tenants.DEFAULT_TENANT_ID))))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(twoUsers);
        Assertions.assertEquals(2, twoUsers.size());
    }

    @Test
    public void testRemoveUser() {
        UserModel user = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "999", "test-login-1", 888L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-1", "Имя-1", "LastName-1", "Фамилия-1", false, false, Collections.emptyMap(), "M");
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.upsertUserRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), user)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUser = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        user.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUser);
        Assertions.assertTrue(newUser.isPresent());
        Assertions.assertEquals(user, newUser.get());
        Assertions.assertNotNull(newUserByPassportUid);
        Assertions.assertTrue(newUserByPassportUid.isPresent());
        Assertions.assertEquals(user, newUserByPassportUid.get());
        Assertions.assertNotNull(newUserByPassportLogin);
        Assertions.assertTrue(newUserByPassportLogin.isPresent());
        Assertions.assertEquals(user, newUserByPassportLogin.get());
        Assertions.assertNotNull(newUserByStaffId);
        Assertions.assertTrue(newUserByStaffId.isPresent());
        Assertions.assertEquals(user, newUserByStaffId.get());
        ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.removeUserRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), user)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        user.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByPassportUidDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByPassportLoginDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserByStaffIdDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUserDeleted);
        Assertions.assertFalse(newUserDeleted.isPresent());
        Assertions.assertNotNull(newUserByPassportUidDeleted);
        Assertions.assertFalse(newUserByPassportUidDeleted.isPresent());
        Assertions.assertNotNull(newUserByPassportLoginDeleted);
        Assertions.assertFalse(newUserByPassportLoginDeleted.isPresent());
        Assertions.assertNotNull(newUserByStaffIdDeleted);
        Assertions.assertFalse(newUserByStaffIdDeleted.isPresent());
    }

    @Test
    public void testRemoveUsers() {
        UserModel userOne = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "999", "test-login-1", 888L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-1", "Имя-1", "LastName-1", "Фамилия-1", false, false, Collections.emptyMap(), "M");
        UserModel userTwo = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "998", "test-login-2", 889L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-2", "Имя-2", "LastName-2", "Фамилия-2", false, false, Collections.emptyMap(), "F");
        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.upsertUsersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(userOne, userTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOne = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportUid = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "998", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportLogin = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-2", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByStaffId = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        889L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUserOne);
        Assertions.assertTrue(newUserOne.isPresent());
        Assertions.assertEquals(userOne, newUserOne.get());
        Assertions.assertNotNull(newUserOneByPassportUid);
        Assertions.assertTrue(newUserOneByPassportUid.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportUid.get());
        Assertions.assertNotNull(newUserOneByPassportLogin);
        Assertions.assertTrue(newUserOneByPassportLogin.isPresent());
        Assertions.assertEquals(userOne, newUserOneByPassportLogin.get());
        Assertions.assertNotNull(newUserOneByStaffId);
        Assertions.assertTrue(newUserOneByStaffId.isPresent());
        Assertions.assertEquals(userOne, newUserOneByStaffId.get());
        Assertions.assertNotNull(newUserTwo);
        Assertions.assertTrue(newUserTwo.isPresent());
        Assertions.assertEquals(userTwo, newUserTwo.get());
        Assertions.assertNotNull(newUserTwoByPassportUid);
        Assertions.assertTrue(newUserTwoByPassportUid.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportUid.get());
        Assertions.assertNotNull(newUserTwoByPassportLogin);
        Assertions.assertTrue(newUserTwoByPassportLogin.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByPassportLogin.get());
        Assertions.assertNotNull(newUserTwoByStaffId);
        Assertions.assertTrue(newUserTwoByStaffId.isPresent());
        Assertions.assertEquals(userTwo, newUserTwoByStaffId.get());
        ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.removeUsersRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(userOne, userTwo))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportUidDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "999", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByPassportLoginDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-1", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserOneByStaffIdDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        888L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        userTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportUidDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "998", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByPassportLoginDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportLogin(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        "test-login-2", Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<UserModel> newUserTwoByStaffIdDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByStaffId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        889L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newUserOneDeleted);
        Assertions.assertFalse(newUserOneDeleted.isPresent());
        Assertions.assertNotNull(newUserOneByPassportUidDeleted);
        Assertions.assertFalse(newUserOneByPassportUidDeleted.isPresent());
        Assertions.assertNotNull(newUserOneByPassportLoginDeleted);
        Assertions.assertFalse(newUserOneByPassportLoginDeleted.isPresent());
        Assertions.assertNotNull(newUserOneByStaffIdDeleted);
        Assertions.assertFalse(newUserOneByStaffIdDeleted.isPresent());
        Assertions.assertNotNull(newUserTwoDeleted);
        Assertions.assertFalse(newUserTwoDeleted.isPresent());
        Assertions.assertNotNull(newUserTwoByPassportUidDeleted);
        Assertions.assertFalse(newUserTwoByPassportUidDeleted.isPresent());
        Assertions.assertNotNull(newUserTwoByPassportLoginDeleted);
        Assertions.assertFalse(newUserTwoByPassportLoginDeleted.isPresent());
        Assertions.assertNotNull(newUserTwoByStaffIdDeleted);
        Assertions.assertFalse(newUserTwoByStaffIdDeleted.isPresent());
    }
    @SuppressWarnings({"SameParameterValue", "ParameterNumber", "MethodWithTooManyParameters"})
    private static UserModel getUser(String id, TenantId tenantId, String passportUid, String passportLogin,
                                     Long staffId, Boolean staffDismissed, Boolean staffRobot,
                                     StaffAffiliation staffAffiliation, String firstNameEn, String firstNameRu,
                                     String lastNameEn, String lastNameRu, Boolean dAdmin, boolean deleted,
                                     Map<UserServiceRoles, Set<Long>> roles, String gender) {
        return UserModel.builder()
                .id(id)
                .tenantId(tenantId)
                .passportUid(passportUid)
                .passportLogin(passportLogin)
                .staffId(staffId)
                .staffDismissed(staffDismissed)
                .staffRobot(staffRobot)
                .staffAffiliation(staffAffiliation)
                .firstNameEn(firstNameEn)
                .firstNameRu(firstNameRu)
                .lastNameEn(lastNameEn)
                .lastNameRu(lastNameRu)
                .dAdmin(dAdmin)
                .deleted(deleted)
                .roles(roles)
                .gender(gender)
                .build();
    }

    @Test
    public void testGetDAdmins() {
        UserModel userOne = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "999", "test-login-1", 888L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-1", "Имя-1", "LastName-1", "Фамилия-1", true, false, Collections.emptyMap(), "M");
        UserModel userTwo = getUser(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                "998", "test-login-2", 889L,
                false, false, StaffAffiliation.YANDEX,
                "FirstName-2", "Имя-2", "LastName-2", "Фамилия-2", false, false, Collections.emptyMap(), "F");

        ydbTableClient.usingSessionMonoRetryable(session -> usersDao.upsertUsersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(userOne, userTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        List<UserModel> dAdmins = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getDAdmins(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(dAdmins);
        Assertions.assertEquals(3, dAdmins.size());
        Assertions.assertEquals(
                Set.of(TestUsers.USER_1_ID, TestUsers.D_ADMIN_WITHOUT_ROLES_ID, userOne.getId()),
                dAdmins.stream()
                        .filter(UserModel::getDAdmin)
                        .map(UserModel::getId)
                        .collect(Collectors.toSet())
        );
    }
}
