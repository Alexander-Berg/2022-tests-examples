package ru.yandex.partner.coreexperiment.entity.user.repository;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.coreexperiment.configuration.CoreLibraryTest;
import ru.yandex.partner.coreexperiment.dbrequest.DBQuery;
import ru.yandex.partner.coreexperiment.entity.user.builder.UserBuilder;
import ru.yandex.partner.coreexperiment.entity.user.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.partner.dbschema.partner.Tables.USERS;
import static ru.yandex.partner.test.utils.TestUtils.compareToDataFromFile;

@CoreLibraryTest
public class TestUserRepository {
    @Autowired
    UserRepository userRepository;

    @Test
    void checkFindById() throws Exception {
        User user = userRepository.findById(1009L);

        compareToDataFromFile(user, TestUserRepository.class, "user_from_find_by_id.json");
    }

    @Test
    void checkFindWithEmptyQuery() throws Exception {
        DBQuery dbQuery = new DBQuery();
        List<User> users = userRepository.find(dbQuery);
        assertEquals(45, users.size());
    }

    @Test
    void checkFind() throws Exception {
        DBQuery dbQuery = new DBQuery()
                .setCondition(USERS.ID.between(11012L, 11013L));

        List<User> users = userRepository.find(dbQuery);

        compareToDataFromFile(users, TestUserRepository.class, "users_from_find.json");
    }

    @Test
    void checkFindWithOffsetAndLimit() throws Exception {
        DBQuery dbQuery = new DBQuery()
                .setCondition(USERS.ID.between(11012L, 11013L))
                .setOffset(1)
                .setLimit(1);

        List<User> users = userRepository.find(dbQuery);

        compareToDataFromFile(users, TestUserRepository.class, "users_from_find_with_limits.json");
    }

    @Test
    void checkFindwithOrder() throws Exception {
        DBQuery dbQuery = new DBQuery()
                .setCondition(USERS.ID.between(11012L, 11013L))
                .setOrderFields(USERS.LOGIN.asc());

        List<User> usersAsc = userRepository.find(dbQuery);

        compareToDataFromFile(usersAsc, TestUserRepository.class, "users_from_find_with_asc_order.json");

        dbQuery = new DBQuery()
                .setCondition(USERS.ID.between(11012L, 11013L))
                .setOrderFields(USERS.LOGIN.desc());

        List<User> usersDesc = userRepository.find(dbQuery);

        compareToDataFromFile(usersDesc, TestUserRepository.class, "users_from_find_with_desc_order.json");
    }

    @Test
    void checkAdd() throws Exception {
        User user = getNewUser("test-add-login");

        userRepository.add(user);

        Long userId = user.getId().getValue();

        assertNotNull(userId);

        user = userRepository.findById(userId);

        compareToDataFromFile(user, TestUserRepository.class, "user_after_add.json");
    }

    @Test
    void checkStoreNew() throws Exception {
        List<User> users = List.of(getNewUser("test-store-login"), getNewUser("test-store-login2"));

        userRepository.store(users);

        DBQuery query = new DBQuery().setCondition(USERS.LOGIN.like("%test-store-login%"));

        users = userRepository.find(query);

        compareToDataFromFile(users, TestUserRepository.class, "user_after_store.json");
    }

    @Test
    void checkStoreUpdate() throws Exception {
        User user = userRepository.findById(1009L);

        compareToDataFromFile(user, TestUserRepository.class, "user_from_find_by_id.json");

        userRepository.store(List.of(user));

        user = userRepository.findById(1009L);

        compareToDataFromFile(user, TestUserRepository.class, "user_from_find_by_id.json");
    }

    User getNewUser(String login) {
        UserBuilder builder = new UserBuilder();

        builder.setLogin(login);

        builder.setLastname("testLastName");

        builder.setName("TestName");

        builder.setMidname("TestMidname");

        builder.setEmail("TestEmail");

        builder.setMultistate(1L);

        builder.setClientId(200500L);

        builder.setPhone("8976543210");

        builder.setNewsletter(true);

        builder.setBusinessUnit(false);

        builder.setCreateDate(LocalDateTime.of(2019, Month.SEPTEMBER, 4, 12, 18, 40));

        builder.setIsMobileMediation(false);

        builder.setHasRsya(true);

        builder.setHasMobileMediation(false);

        builder.setHasApproved(true);

        builder.setHasTutbyAgreement(false);

        builder.setHasCommonOffer(false);

        return builder.build();
    }
}
