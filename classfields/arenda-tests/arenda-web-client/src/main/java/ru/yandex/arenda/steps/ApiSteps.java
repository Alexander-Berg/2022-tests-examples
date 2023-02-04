package ru.yandex.arenda.steps;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import ru.auto.tests.passport.account.Account;

public class ApiSteps {

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Inject
    private PassportSteps passportSteps;

    public Account createYandexAccount(Account account) {
        passportSteps.login(account);
        return account;
    }
}
