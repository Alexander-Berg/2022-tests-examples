package ru.auto.tests.passport.rules;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Parameter;
import org.junit.rules.ExternalResource;
import ru.auto.tests.passport.account.AccountKeeper;

import javax.inject.Inject;

/**
 * Created by vicdev on 17.08.17.
 */
public class AddParameterRule extends ExternalResource {

    @Inject
    private AccountKeeper accountKeeper;

    @Override
    protected void after() {
        accountKeeper.get().forEach(a -> {
            addParameter("uid", a.getId());
            addParameter("login", a.getLogin());
            addParameter("passw", a.getPassword());
        });
    }

    private static void addParameter(String name, String value) {
        Allure.getLifecycle().updateTestCase(testResult -> testResult.getParameters()
                .add(new Parameter().withName(name).withValue(value)));
    }
}
