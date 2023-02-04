package ru.yandex.arenda.rule;

import com.google.inject.Inject;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ru.auto.tests.passport.account.AccountKeeper;

public class LogTestNameRule extends TestWatcher {
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(LogTestNameRule.class);

    @Inject
    private AccountKeeper accountKeeper;

    @Override
    protected void starting(Description description) {
        if (!accountKeeper.get().isEmpty()) {
            LOGGER.info(String.format("USER: «%s», TEST: «%s:%s»", accountKeeper.get().get(0).getId(),
                    description.getClassName(), description.getMethodName()));
        }
    }

}
