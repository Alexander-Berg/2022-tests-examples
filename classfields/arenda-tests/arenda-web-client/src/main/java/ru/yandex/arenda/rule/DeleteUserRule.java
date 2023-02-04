package ru.yandex.arenda.rule;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.junit.rules.ExternalResource;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.yandex.arenda.steps.RetrofitApiSteps;

public class DeleteUserRule extends ExternalResource {
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(DeleteUserRule.class);

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Inject
    private AccountKeeper accountKeeper;

    @Override
    protected void after() {
        accountKeeper.get().forEach(account -> deleteUser(account));
    }

    @Step("Удаляем юзера для аккаунта {account.id}")
    public void deleteUser(Account account) {
        try {
            String userId = retrofitApiSteps.getUserId(account.getId());
            retrofitApiSteps.deleteUser(userId);
        } catch (Exception e) {
            LOGGER.info(String.format("Can't delete user %s. Exception: %s", account.getId(), e.getMessage()));
        }
    }
}
