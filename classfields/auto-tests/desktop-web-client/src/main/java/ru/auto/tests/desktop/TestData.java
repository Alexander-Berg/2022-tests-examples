package ru.auto.tests.desktop;

import ru.auto.tests.passport.account.Account;

import javax.inject.Provider;

public class TestData {

    public static final Provider<Account> OWNER_USER_2_PROVIDER = () -> Account.builder()
            .login("fake6@auto.ru")
            .password("autoru").build();
    public static final Provider<Account> OWNER_USER_PROVIDER = () -> Account.builder()
            .login("fake4@auto.ru")
            .password("autoru").build();
    public static final Provider<Account> MANAGER_USER_PROVIDER = () -> Account.builder()
            .login("avgribanov@yandex-team.ru")
            .password("autoru").build();
    public static final Provider<Account> USER_2_PROVIDER = () -> Account.builder()
            .login("sosediuser2@mail.ru")
            .password("autoru").build();
    public static final Provider<Account> USER_4_PROVIDER = () -> Account.builder()
            .login("sosediuser3@mail.ru")
            .password("autoru").build();
    public static final Provider<Account> CLIENT_PROVIDER = () -> Account.builder()
            .login("demo@auto.ru")
            .password("autoru").build();
    public static final Provider<Account> CLIENT_2_PROVIDER = () -> Account.builder()
            .login("aristos@ma.ru")
            .password("autoru").build();
}
