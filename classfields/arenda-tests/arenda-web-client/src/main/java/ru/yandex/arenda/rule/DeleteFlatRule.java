package ru.yandex.arenda.rule;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.junit.rules.ExternalResource;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.yandex.arenda.account.FlatsKeeper;
import ru.yandex.arenda.steps.RetrofitApiSteps;

public class DeleteFlatRule extends ExternalResource {
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(DeleteFlatRule.class);

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    private FlatsKeeper flatsKeeper;

    @Override
    protected void after() {
        accountKeeper.get().forEach(this::deleteUserFlats);
        flatsKeeper.get().forEach(id -> retrofitApiSteps.deleteFlat(id));
    }

    @Step("Удаляем все квартиры для аккаунта {account.id}")
    public void deleteUserFlats(Account account) {
        try {
            retrofitApiSteps.getUserFlats(account.getId()).getAsJsonArray("flats")
                    .forEach(flat -> {
                        String flatId = flat.getAsJsonObject().getAsJsonPrimitive("flatId").getAsString();
                        retrofitApiSteps.deleteFlat(flatId);
                        flatsKeeper.get().remove(flatId);
                    });
        } catch (Exception e) {
            LOGGER.info(String.format("Can't delete offers on %s. Exception: %s", account.getId(), e.getMessage()));
        }
    }
}
