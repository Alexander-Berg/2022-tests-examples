package ru.auto.tests.passport;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.passport.module.PassportAccountCaptchaNeverWithPhoneModule;

import static org.assertj.core.api.Assertions.assertThat;

@GuiceModules(PassportAccountCaptchaNeverWithPhoneModule.class)
@RunWith(GuiceTestRunner.class)
public class CreateAccountCaptchaNeverWithPhoneTest {

    @Inject
    private AccountManager am;

    @Inject
    private Account account;

    @Test
    public void shouldCreateAccountByPhoneTest() {
        assertThat(account.getId()).isNotNull();
        assertThat(account.getLogin()).isNotNull();
        assertThat(account.getPassword()).isNotNull();
        assertThat(account.getPhone()).isNotNull();
    }

    @After
    public void deleteAccount() {
        am.delete(account.getId());
    }
}
