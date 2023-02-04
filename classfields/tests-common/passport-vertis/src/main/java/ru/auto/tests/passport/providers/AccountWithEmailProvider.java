package ru.auto.tests.passport.providers;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import retrofit2.Response;
import ru.auto.test.passport.model.CreateUserResult;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.passport.adaptor.PassportApiAdaptor;

import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.passport.adaptor.PassportApiAdaptor.DEFAULT_PASSWORD;

/**
 * Created by vicdev on 15.09.17.
 */
public class AccountWithEmailProvider extends AbstractModule {

    @Provides
    public Account providesPassportWithEmailAccount(PassportApiAdaptor passportAdaptor, AccountKeeper accountKeeper) {
        String login = getRandomEmail();
        String pass = DEFAULT_PASSWORD;
        Response<CreateUserResult> userResp = passportAdaptor.createAccountWithoutConfirmationByEmail(login, pass);
        passportAdaptor.confirmEmail(userResp.body().getConfirmationCode(), login);
        Account account = Account.builder().id(userResp.body().getUser().getId()).login(login).password(pass).build();
        accountKeeper.add(account);
        return account;
    }

    @Override
    protected void configure() {
    }
}
