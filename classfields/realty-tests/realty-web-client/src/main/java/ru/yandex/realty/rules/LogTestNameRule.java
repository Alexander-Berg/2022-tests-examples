package ru.yandex.realty.rules;

import com.google.inject.Inject;
import lombok.extern.java.Log;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ru.auto.tests.passport.account.AccountKeeper;

/**
 * @author kurau (Yuri Kalinin)
 */
@Log
public class LogTestNameRule extends TestWatcher {

    @Inject
    private AccountKeeper accountKeeper;

    @Override
    protected void starting(Description description) {
        if (!accountKeeper.get().isEmpty()) {
            log.info(String.format("USER: «%s», TEST: «%s:%s»", accountKeeper.get().get(0).getId(),
                    description.getClassName(), description.getMethodName()));
        }
    }

}
