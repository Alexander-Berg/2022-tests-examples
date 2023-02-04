package ru.yandex.intranet.d.tms.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.users.AbcServiceMemberDao;
import ru.yandex.intranet.d.dao.users.UsersDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.users.AbcServiceMemberModel;
import ru.yandex.intranet.d.model.users.UserModel;
import ru.yandex.intranet.d.model.users.UserServiceRoles;

import static ru.yandex.intranet.d.TestUsers.USER_6_UID;
import static ru.yandex.intranet.d.model.users.StaffAffiliation.UNKNOWN;
import static ru.yandex.intranet.d.model.users.UserServiceRoles.QUOTA_MANAGER;
import static ru.yandex.intranet.d.model.users.UserServiceRoles.RESPONSIBLE_OF_PROVIDER;

/**
 * Sync ABC Users Test.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 */
@IntegrationTest
class SyncAbcUsersTest {
    private static final String TEST_USER_UID_1 = "2120000000000000";
    private static final long TEST_USER_STAFF_ID_1 = 10L;
    private static final String TEST_USER_UID_2 = "2120000000000002";
    private static final long TEST_USER_STAFF_ID_2 = 12L;
    public static final String EXISTING_USER_UID = "1120000000000001";

    @Autowired
    private SyncAbcUsers syncAbcUsers;
    @Autowired
    private UsersDao usersDao;
    @Autowired
    private AbcServiceMemberDao abcServiceMemberDao;
    @Autowired
    private YdbTableClient ydbTableClient;
    private final Map<Long, UserServiceRoles> roleByIdMap;

    SyncAbcUsersTest(@Value("${abc.roles.quotaManager}") long quotaManagerId,
                     @Value("${abc.roles.responsibleOfProvider}") long responsibleOfProviderId,
                     @Value("${abc.roles.serviceProductHead}") long serviceProductHeadRoleId,
                     @Value("${abc.roles.serviceProductDeputyHead}") long serviceProductDeputyHeadRoleId,
                     @Value("${abc.roles.serviceResponsible}") long serviceResponsibleRoleId) {
        roleByIdMap = Map.of(
                quotaManagerId, UserServiceRoles.QUOTA_MANAGER,
                responsibleOfProviderId, UserServiceRoles.RESPONSIBLE_OF_PROVIDER,
                serviceProductHeadRoleId, UserServiceRoles.SERVICE_PRODUCT_HEAD,
                serviceProductDeputyHeadRoleId, UserServiceRoles.SERVICE_PRODUCT_DEPUTY_HEAD,
                serviceResponsibleRoleId, UserServiceRoles.SERVICE_RESPONSIBLE
        );
    }

    @Test
    void newUsersSync() {
        Optional<UserModel> user1 = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(user1);
        Assertions.assertFalse(user1.isPresent());

        Optional<UserModel> user2 = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_2, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(user2);
        Assertions.assertFalse(user2.isPresent());

        syncAbcUsers.execute();

        user1 = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(user1);
        Assertions.assertTrue(user1.isPresent());

        user2 = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_2, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(user2);
        Assertions.assertTrue(user2.isPresent());
    }

    @Test
    void allChangesCounts() {
        syncAbcUsers.execute();

        Optional<UserModel> expectedUserO = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(expectedUserO);
        Assertions.assertTrue(expectedUserO.isPresent());
        UserModel expectedUser = expectedUserO.get();

        testField(expectedUser.copyBuilder().passportLogin("cassandra").build(), expectedUser);
        testField(expectedUser.copyBuilder().staffId(2077L).build(), expectedUser);
        testField(expectedUser.copyBuilder().staffDismissed(true).build(), expectedUser);
        testField(expectedUser.copyBuilder().staffRobot(true).build(), expectedUser);
        testField(expectedUser.copyBuilder().staffAffiliation(UNKNOWN).build(), expectedUser);
        testField(expectedUser.copyBuilder().firstNameEn("Albert").build(), expectedUser);
        testField(expectedUser.copyBuilder().firstNameRu("Альберт").build(), expectedUser);
        testField(expectedUser.copyBuilder().lastNameEn("Einstein").build(), expectedUser);
        testField(expectedUser.copyBuilder().lastNameEn("Эйнштtейн").build(), expectedUser);
        testField(expectedUser.copyBuilder().roles(Map.of(QUOTA_MANAGER, Set.of(4L))).build(), expectedUser);
    }

    private void testField(UserModel changedUser, UserModel expectedUser) {
        Assertions.assertNotEquals(changedUser, expectedUser);

        ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.updateUserRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        changedUser)).block();

        Optional<UserModel> changedUserO = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(changedUserO);
        Assertions.assertTrue(changedUserO.isPresent());
        Assertions.assertEquals(changedUser, changedUserO.get());

        syncAbcUsers.execute();

        changedUserO = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(changedUserO);
        Assertions.assertTrue(changedUserO.isPresent());
        Assertions.assertEquals(expectedUser, changedUserO.get());
    }

    @Test
    void newRolesSync() {
        List<UserModel> userModels = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUids(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TEST_USER_UID_2, Tenants.DEFAULT_TENANT_ID))))
                .block();

        Assertions.assertNotNull(userModels);
        Assertions.assertTrue(userModels.isEmpty());

        List<AbcServiceMemberModel> allAbcServiceMemberModels =
                ydbTableClient.usingSessionMonoRetryable(session -> abcServiceMemberDao.getAllRows(
                        session, AbcServiceMemberDao.Fields.values())
                        .collectList())
                        .block();

        Assertions.assertNotNull(allAbcServiceMemberModels);
        Assertions.assertEquals(19, allAbcServiceMemberModels.size());

        List<AbcServiceMemberModel> abcServiceMemberModels =
                ydbTableClient.usingSessionMonoRetryable(session -> abcServiceMemberDao.getByUsersAndRoles(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        Set.of(TEST_USER_STAFF_ID_1, TEST_USER_STAFF_ID_2), roleByIdMap.keySet()))
                        .block();

        Assertions.assertNotNull(abcServiceMemberModels);
        Assertions.assertEquals(6, abcServiceMemberModels.size());
        Assertions.assertTrue(abcServiceMemberModels.stream()
                .map(AbcServiceMemberModel::getId)
                .toList()
                .containsAll(List.of(3033L, 3036L, 3495L, 1337L, 7331L)));

        Optional<UserModel> optionalUserModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        EXISTING_USER_UID, Tenants.DEFAULT_TENANT_ID))
                .block();

        Assertions.assertNotNull(optionalUserModel);
        Assertions.assertTrue(optionalUserModel.isPresent());

        Map<UserServiceRoles, Set<Long>> expectedForExistingUserMap = Map.of(QUOTA_MANAGER, Set.of(2L, 4L),
                RESPONSIBLE_OF_PROVIDER, Set.of(1L));

        Assertions.assertEquals(expectedForExistingUserMap, optionalUserModel.get().getRoles());

        syncAbcUsers.execute();

        Map<Long, Map<UserServiceRoles, Set<Long>>> expectedRoles =
                abcServiceMemberModels.stream()
                        .collect(Collectors.groupingBy(AbcServiceMemberModel::getStaffId,
                                Collectors.groupingBy(abcServiceMemberModel -> roleByIdMap.get(
                                        abcServiceMemberModel.getRoleId()),
                                        Collectors.mapping(AbcServiceMemberModel::getServiceId,
                                                Collectors.toSet())
                                )
                        ));

        userModels = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUids(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TEST_USER_UID_2, Tenants.DEFAULT_TENANT_ID))))
                .block();

        Assertions.assertNotNull(userModels);

        Assertions.assertTrue(userModels.stream()
                .allMatch(userModel -> expectedRoles.get(userModel.getStaffId().orElseThrow())
                        .equals(userModel.getRoles())));

        optionalUserModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        EXISTING_USER_UID, Tenants.DEFAULT_TENANT_ID))
                .block();

        Assertions.assertNotNull(optionalUserModel);
        Assertions.assertTrue(optionalUserModel.isPresent());
        Assertions.assertEquals(expectedForExistingUserMap, optionalUserModel.get().getRoles());
    }

    @Test
    void userGenderShouldBeSynced() {
        Optional<UserModel> userModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID))
                .block();

        Assertions.assertTrue(userModel.isEmpty());

        syncAbcUsers.execute();

        userModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID))
                .block();

        Assertions.assertFalse(userModel.isEmpty());
        Assertions.assertEquals("F", userModel.get().getGender());

        final UserModel invalidUser = userModel.get().copyBuilder()
                .gender(null)
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.updateUserRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        invalidUser))
                .block();

        userModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID))
                .block();

        Assertions.assertFalse(userModel.isEmpty());
        Assertions.assertEquals("null", userModel.get().getGender());

        syncAbcUsers.execute();

        userModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID))
                .block();

        Assertions.assertFalse(userModel.isEmpty());
        Assertions.assertEquals("F", userModel.get().getGender());
    }

    @Test
    void userStaffIdShouldBeSynced() {
        Optional<UserModel> userModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID))
                .block();
        Assertions.assertTrue(userModel.isEmpty());
        syncAbcUsers.execute();
        userModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID))
                .block();
        Assertions.assertFalse(userModel.isEmpty());
        Assertions.assertEquals(10L, userModel.get().getStaffId().get());
        final UserModel invalidUser = userModel.get().copyBuilder()
                .staffId(0L)
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.updateUserRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        invalidUser))
                .block();
        userModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID))
                .block();
        Assertions.assertFalse(userModel.isEmpty());
        Assertions.assertEquals(0L, userModel.get().getStaffId().get());
        syncAbcUsers.execute();
        userModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_1, Tenants.DEFAULT_TENANT_ID))
                .block();
        Assertions.assertFalse(userModel.isEmpty());
        Assertions.assertEquals(10L, userModel.get().getStaffId().get());
    }

    @Test
    void onlyUserActiveRolesShouldBeSynced() {
        Optional<UserModel> userModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_2, Tenants.DEFAULT_TENANT_ID))
                .block();
        Assertions.assertNotNull(userModel);
        Assertions.assertTrue(userModel.isEmpty());

        syncAbcUsers.execute();

        userModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_USER_UID_2, Tenants.DEFAULT_TENANT_ID))
                .block();
        Assertions.assertNotNull(userModel);
        Assertions.assertFalse(userModel.isEmpty());
        Assertions.assertEquals(Map.of(QUOTA_MANAGER, Set.of(1L, 2L)), userModel.get().getRoles());
    }

    @Test
    void userDeprivedRolesShouldBeRemoved() {
        Optional<UserModel> userModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        USER_6_UID, Tenants.DEFAULT_TENANT_ID))
                .block();
        Assertions.assertNotNull(userModel);
        Assertions.assertFalse(userModel.isEmpty());
        Assertions.assertEquals(Map.of(QUOTA_MANAGER, Set.of(2L)), userModel.get().getRoles());

        syncAbcUsers.execute();

        userModel = ydbTableClient.usingSessionMonoRetryable(session ->
                usersDao.getByPassportUid(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        USER_6_UID, Tenants.DEFAULT_TENANT_ID))
                .block();
        Assertions.assertNotNull(userModel);
        Assertions.assertFalse(userModel.isEmpty());
        Assertions.assertEquals(new HashMap<UserServiceRoles, Set<Long>>(), userModel.get().getRoles());
    }
}
