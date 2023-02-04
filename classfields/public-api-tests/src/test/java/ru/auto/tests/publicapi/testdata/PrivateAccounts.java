package ru.auto.tests.publicapi.testdata;

import ru.auto.tests.passport.account.Account;

public class PrivateAccounts {
    private PrivateAccounts() {
    }

    public static Account getAccountWithRedirectedPhone() {
        return Account.builder().login("79854406468").password("autoru").id("69233848").build();
    }
}
