package ru.auto.tests.passport.providers;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import retrofit2.Response;
import ru.auto.test.passport.model.CreateUserResult;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.passport.adaptor.PassportApiAdaptor;

import java.util.Optional;

import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.auto.tests.passport.adaptor.PassportApiAdaptor.DEFAULT_PASSWORD;

/**
 * Created by vicdev on 15.09.17.
 */
public class AccountWithPhoneProvider extends AbstractModule {

    @Provides
    public Account providesPassportWithPhoneAccount(PassportApiAdaptor passportAdaptor, AccountKeeper accountKeeper) {
        String phone = getRandomPhone();
        String pass = DEFAULT_PASSWORD;
        Response<CreateUserResult> userResp = passportAdaptor.createAccountWithoutConfirmationByPhone(phone, pass);
        passportAdaptor.confirmPhone(userResp.body().getConfirmationCode(), phone);
        Account account = Account.builder().id(userResp.body().getUser().getId()).login(phone).password(pass)
            .phone(Optional.of(phone)).build();
        accountKeeper.add(account);
        return account;
    }

    @Override
    protected void configure() {
    }
}