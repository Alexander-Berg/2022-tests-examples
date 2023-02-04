package ru.yandex.partner.coreexperiment.entity.user.convertor;

import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.Test;

import ru.yandex.partner.coreexperiment.entity.user.model.User;
import ru.yandex.partner.dbschema.partner.tables.records.UsersRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.partner.test.utils.TestUtils.compareToDataFromFile;

public class TestUserConvertors {
    static String testDataFile = "user_after_convert.json";

    UsersRecordToUserConverter usersRecordToUser = new UsersRecordToUserConverter();

    UserToUsersRecordConverter userToUsersRecord = new UserToUsersRecordConverter();

    @Test
    void checkUserConvertors() throws Exception {
        UsersRecord usersRecord = new UsersRecord();

        usersRecord.setId(100500L);
        usersRecord.setLogin("test-login");
        usersRecord.setLastname("TestLastname");
        usersRecord.setName("TestName");
        usersRecord.setMidname("TestMidname");
        usersRecord.setEmail("TestEmail");
        usersRecord.setMultistate(1L);
        usersRecord.setClientId(200500L);
        usersRecord.setPhone("8976543210");
        usersRecord.setNewsletter(1L);
        usersRecord.setBusinessUnit(1L);
        usersRecord.setCreateDate(LocalDateTime.of(2019, Month.SEPTEMBER, 4, 12, 18, 40));
        usersRecord.setIsMobileMediation(1L);
        usersRecord.setOpts("{\"has_rsya\":1,\"has_mobile_mediation\":0,\"has_approved\":1,\"has_tutby_agreement\":0," +
                "\"has_common_offer\":0}");

        User user = usersRecordToUser.convert(usersRecord);

        compareToDataFromFile(user, TestUserConvertors.class, testDataFile);

        UsersRecord usersRecordAfterConvert = userToUsersRecord.convert(user);

        assertThat(usersRecordAfterConvert).isEqualToComparingFieldByFieldRecursively(usersRecord); //isEqualTo();
    }
}
