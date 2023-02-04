package ru.auto.tests.publicapi.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;

import static ru.auto.tests.publicapi.consts.DealerConsts.DEALER_LOGIN;
import static ru.auto.tests.publicapi.consts.DealerConsts.DEALER_PASS;
import static ru.auto.tests.publicapi.consts.DealerConsts.DEALER_UID;

/**
 * Created by dskuznetsov on 25.12.18
 */

public class DealerAccountProvider implements Provider<Account> {
    @Inject
    private AccountKeeper accountKeeper;

    @Override
    public Account get() {
        Account account = Account.builder()
                .login(DEALER_LOGIN).password(DEALER_PASS)
                .id(DEALER_UID).build();
        accountKeeper.add(account);
        return account;
    }
}
