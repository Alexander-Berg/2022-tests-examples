package ru.yandex.autotests.mobile.disk.android.core.provider;

import com.google.inject.Provider;
import ru.yandex.autotests.mobile.disk.android.core.accounts.Account;
import ru.yandex.autotests.mobile.disk.android.core.accounts.AccountThreadManager;

public class TestUserAccountProvider implements Provider<Account> {

    @Override
    public Account get() {
        return AccountThreadManager.getTestUserManager().getAccount();
    }
}
