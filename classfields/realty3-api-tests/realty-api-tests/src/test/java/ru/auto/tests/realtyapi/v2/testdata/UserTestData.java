package ru.auto.tests.realtyapi.v2.testdata;

import ru.auto.tests.realtyapi.v2.model.RealtyApiUsersUserInfo;
import ru.auto.tests.realtyapi.v2.model.RealtyApiUsersUserInfo.PaymentTypeEnum;
import ru.auto.tests.realtyapi.v2.model.RealtyApiUsersUserInfo.UserTypeEnum;
import ru.auto.tests.realtyapi.v2.model.RealtyApiUsersUserUpdate;

import static ru.auto.tests.realtyapi.v2.model.RealtyApiUsersUserInfo.PaymentTypeEnum.NATURAL_PERSON;
import static ru.auto.tests.realtyapi.v2.model.RealtyApiUsersUserInfo.UserTypeEnum.OWNER;

public class UserTestData {
    private UserTestData() {}

    public static RealtyApiUsersUserUpdate getUpdate(UserTypeEnum type, PaymentTypeEnum paymentType) {
        return new RealtyApiUsersUserUpdate().userInfo(
                getUserInfo(type, paymentType)
        );
    }
    public static RealtyApiUsersUserUpdate getUpdate() {
        return new RealtyApiUsersUserUpdate().userInfo(
                getUserInfo()
        );
    }

    public static RealtyApiUsersUserInfo getUserInfo(UserTypeEnum type, PaymentTypeEnum paymentType) {
        return new RealtyApiUsersUserInfo()
                .userType(type)
                .paymentType(paymentType);
    }

    public static RealtyApiUsersUserInfo getUserInfo() {
        return new RealtyApiUsersUserInfo()
                .userType(OWNER)
                .paymentType(NATURAL_PERSON);
    }
}
