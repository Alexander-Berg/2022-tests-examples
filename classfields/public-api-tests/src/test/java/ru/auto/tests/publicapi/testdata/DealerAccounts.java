package ru.auto.tests.publicapi.testdata;

import ru.auto.tests.passport.account.Account;

public class DealerAccounts {
    private DealerAccounts() {
    }

    public static Account getBMWEurosibAccount() {
        return Account.builder().login("anastasiya.belyaeva@bmw-eurosib.ru").password("autoru").id("3562031").build();
    }

    public static Account getDemoAccount() {
        return Account.builder().login("demo@auto.ru").password("autoru").id("11296277").build();
    }

    public static Account getFeedsDisabledTestAccount() {
        return Account.builder().login("feeds-disabled@regress.apiauto.ru").password("autoru").id("70003008").build();
    }

    public static Account getMajorAccount() {
        return Account.builder().login("aristos@ma.ru").password("autoru").id("14090654").build();
    }

    public static Account getTestAccount() {
        return Account.builder().login("test.autoru@yandex.ru").password("autoru").id("23117336").build();
    }

    public static Account getMaseratiUralAccount() {
        return Account.builder().login("maserati@maserati-ural.ru").password("autoru").id("22863796").build();
    }

    public static Account getAtcBelgorodAccount() {
        return Account.builder().login("atc.belgorod@yandex.ru").password("autoru").id("17221837").build();
    }

    public static Account getMercedesIrkAccount() {
        return Account.builder().login("mstartsev@mercedes-irk.ru").password("autoru").id("11618471").build();
    }

    public static Account getAutoAbsolutionAccount() {
        return Account.builder().login("tradeauto09@mail.ru").password("autoru").id("16360085").build();
    }

    public static Account getAgencyAccount() {
        return Account.builder().login("agency@auto.ru").password("autoru").id("70597077").build();
    }

    public static Account getAgencyMaxAccount() {
        return Account.builder().login("billing@maxposter.ru").password("autoru").id("14439810").build();
    }

    public static Account getModeratorAccount() {
        return Account.builder().login("moderator@auto.ru").password("autoru").id("70597082").build();
    }

    public static Account getManagerAccount() {
        return Account.builder().login("avgribanov@yandex-team.ru").password("autoru").id("19565983").build();
    }
}
