package com.yandex.mail.tools;

import com.yandex.mail.LoginData;
import com.yandex.mail.account.MailProvider;
import com.yandex.mail.entity.AccountType;
import com.yandex.mail.model.AccountModel;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.util.Utils;

import androidx.annotation.NonNull;

public class AccountsTools {

    private AccountsTools() { }

    public static int insertAccount(@NonNull LoginData data, boolean isSelected, boolean isLogged) {
        IntegrationTestRunner.app().getPassportApi().addAccountsToPassport(data);
        final AccountModel accountModel = IntegrationTestRunner.app().getApplicationComponent().accountModel();

        final AccountType accountType = AccountType.fromStringType(data.type);

        MailProvider mailProvider = MailProvider.unknownIfMailish(accountType == AccountType.MAILISH);
        final MockPassportAccountWrapper passportAccount = new MockPassportAccountWrapper(
                data.uid,
                data.name,
                LoginData.SYSTEM_ACCOUNT_TYPE,
                isLogged,
                accountType,
                mailProvider,
                false,
                false
        );
        long localAccountId = accountModel.insertPassportAccount(passportAccount, false);
        if (isSelected) {
            switchAccountTo(data.uid);
        }
        return (int) localAccountId;
    }

    public static int insertAccount(@NonNull LoginData data, boolean isSelected) {
        return insertAccount(data, isSelected, true);
    }

    public static void switchAccountTo(long toId) {
        Utils.selectAccount(IntegrationTestRunner.app(), toId);
    }
}
