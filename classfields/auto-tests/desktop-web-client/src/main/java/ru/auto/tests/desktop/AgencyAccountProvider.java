package ru.auto.tests.desktop;

import ru.auto.tests.passport.account.Account;

import javax.inject.Provider;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 14.09.18
 */
public class AgencyAccountProvider {

    public static final Provider<Account> MAIN_TEST_AGENT = () -> Account.builder()
            .id("14439810")
            .login("billing@maxposter.ru")
            .password("autoru").build();
}
