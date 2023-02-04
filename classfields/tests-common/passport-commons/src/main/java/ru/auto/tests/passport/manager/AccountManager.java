package ru.auto.tests.passport.manager;

import ru.auto.tests.passport.account.Account;

/**
 * Created by vicdev on 02.11.17.
 */
public interface AccountManager {

    Account create();

    void delete(String id);
}
