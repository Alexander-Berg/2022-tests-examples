package ru.auto.tests.realtyapi.v1.testdata;

import ru.auto.tests.passport.account.Account;

import java.util.Optional;

public class PartnerUser {

    // Юзер с привязанным партнерским ID в капе.
    // Создан Тарасом 24 сентября 2020 специально для регрессионных тестов.

    public static final String UID = "4054723953";

    public static final String LOGIN = "autotester01";

    public static final String PASSWORD = "passpass1";

    public static final String PARTNER_ID = "1069251692";

    public static final Account PASSPORT_ACCOUNT = Account.builder()
            .id(UID)
            .login(LOGIN)
            .password(PASSWORD)
            .name("autotester01")
            .phone(Optional.of("+7 (999) 999-99-99"))
            .build();

}
