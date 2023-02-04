package ru.yandex.partner.coreexperiment.entity.user.builder;

import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.Test;

import ru.yandex.partner.coreexperiment.entity.user.model.User;

import static ru.yandex.partner.test.utils.TestUtils.compareToDataFromFile;

public class TestUserBuilder {
    static String testDataFile = "user.json";

    @Test
    void checkUserBuilder() throws Exception {
        UserBuilder builder = new UserBuilder();

        builder.setId(100500L);

        builder.setLogin("test-login");

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

        User user = builder.build();

        compareToDataFromFile(user, TestUserBuilder.class, testDataFile);
    }
}
