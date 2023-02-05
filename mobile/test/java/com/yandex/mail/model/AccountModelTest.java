package com.yandex.mail.model;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

import com.yandex.mail.LoginAccountsChangedReceiver;
import com.yandex.mail.LoginData;
import com.yandex.mail.account.MailProvider;
import com.yandex.mail.am.AMbundle;
import com.yandex.mail.am.MockPassportApi;
import com.yandex.mail.container.AccountInfoContainer;
import com.yandex.mail.db.MailSupportSQLiteOpenHelper;
import com.yandex.mail.entity.AccountEntity;
import com.yandex.mail.entity.AccountType;
import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.metrica.MetricaConstns.ExternalMailsMetrics;
import com.yandex.mail.provider.Constants;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.settings.AccountSettingsEditor;
import com.yandex.mail.settings.MailSettings.SignaturePlace;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.MockPassportAccountWrapper;
import com.yandex.mail.tools.User;
import com.yandex.mail.util.AccountNotInDBException;
import com.yandex.mail.util.AmException;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.passport.api.PassportActions;
import com.yandex.passport.api.PassportUid;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import io.reactivex.subscribers.TestSubscriber;
import kotlin.collections.CollectionsKt;

import static android.os.Looper.getMainLooper;
import static com.yandex.mail.LoginData.SYSTEM_ACCOUNT_TYPE;
import static com.yandex.mail.model.AccountModel.getLcnByUid;
import static com.yandex.mail.runners.IntegrationTestRunner.app;
import static java.util.Collections.singletonMap;
import static kotlin.collections.CollectionsKt.listOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

@RunWith(IntegrationTestRunner.class)
public class AccountModelTest extends BaseIntegrationTest {

    @NonNull
    private static final LoginData account1 = new LoginData("account1", Accounts.LOGIN_TYPE, "token1");

    @NonNull
    private static final LoginData account2 = new LoginData("account2", Accounts.TEAM_TYPE, "token2");

    @NonNull
    private static final LoginData account3 = new LoginData("account3", Accounts.MAILISH_TYPE, "token3");

    @NonNull
    private static final LoginData accountWithAuthError = new LoginData(MockPassportApi.AUTH_ERROR_ACCOUNT_NAME, Accounts.LOGIN_TYPE, "token3");

    @NonNull
    private static final LoginData unAuthorizedAccount = new LoginData("unauthorized_account", Accounts.LOGIN_TYPE, null);

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private AccountWrapper serverAccount1;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private AccountWrapper serverAccount2;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private AccountWrapper serverAccount3;

    @Before
    public void beforeEachTest() {
        serverAccount1 = FakeServer.getInstance().createAccountWrapper(account1);
        serverAccount2 = FakeServer.getInstance().createAccountWrapper(account2);
        serverAccount3 = FakeServer.getInstance().createAccountWrapper(account3);

        initBase();

        metrica.clearEvents();
        metrica.clearEnvironment();
    }

    @Test
    public void getAccount_throwsOnNoUid() throws AccountNotInDBException {
        try {
            AccountModel.getAccount(app(), Constants.NO_UID);
            failBecauseExceptionWasNotThrown(IllegalAccessException.class);
        } catch (IllegalArgumentException noSuchAccountException) {
            assertThat(noSuchAccountException.getMessage()).containsIgnoringCase("uid cannot be");
        }
    }

    @Test
    public void getAccount_throwsAccountDeletedOnDeletedUid() {
        try {
            AccountModel.getAccount(app(), 333);
            failBecauseExceptionWasNotThrown(AccountNotInDBException.class);
        } catch (AccountNotInDBException expected) {
            //noinspection EmptyCatchBlock
        }
    }

    @Test
    public void getAccount_returnsAccount() throws Exception {
        final User user1 = User.createInactive(account1);
        final User user2 = User.createInactive(account2);
        final User user3 = User.create(account3);
        final Account account = AccountModel.getAccount(app(), user2.getUid());
        assertThat(account.name).isEqualTo(account2.name);
    }

    @Test
    public void getSelectedUid_returnsSelected() {
        final User user1 = User.createInactive(account1);
        final User user2 = User.create(account2);
        final User user3 = User.createInactive(account3);

        user2.select();

        assertThat(AccountModel.getSelectedUid(app())).isEqualTo(user2.getUid());
    }

    @Test
    public void getSelectedUid_throwsOnNoSelectedAccount() {
        try {
            AccountModel.getSelectedUid(app());
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage()).containsIgnoringCase("was not found in the database");
        }
    }

    @Test
    public void getSelectedAccount_returnsSelectedAccountIfHasSelected() {
        final User user1 = User.createInactive(account1);
        final User user2 = User.create(account2);
        final User user3 = User.createInactive(account3);

        user2.select();

        final AccountEntity account = accountModel.getSelectedAccount();
        assertThat(account.getName()).isEqualTo(account2.name);
    }

    @Test
    public void getSelectedAccount_returnsNullIfNoSelectedAccount() {
        User.createInactive(account1);
        User.createInactive(account2);
        User.createInactive(account3);

        final AccountEntity account = accountModel.getSelectedAccount();
        assertThat(account).isNull();
    }


    @Test
    public void isAccountUsedInAppAndLogged_returnsIfAccountUsedAndLogged() {
        final User user1 = User.createInactive(account1);
        final User user2 = User.createInactive(account2);
        final User user3 = User.create(account3);

        user1.enableAccount();
        user2.disableAccount();
        user3.enableAccount();
        assertThat(AccountModel.isAccountUsedInAppAndLogged(app(), user1.getUid())).isTrue();
        assertThat(AccountModel.isAccountUsedInAppAndLogged(app(), user2.getUid())).isFalse();
        assertThat(AccountModel.isAccountUsedInAppAndLogged(app(), user3.getUid())).isTrue();
    }

    @Test
    public void isAccountUsedInAppAndLogged_returnsFalseOnInvalidAccount() {
        assertThat(AccountModel.isAccountUsedInAppAndLogged(app(), Constants.NO_UID)).isFalse();
        assertThat(AccountModel.isAccountUsedInAppAndLogged(app(), 234)).isFalse();
    }

    @Test
    public void getUsedInAppAccountsCount_returnsCount() {
        final User user1 = User.createInactive(account1);
        final User user2 = User.createInactive(account2);
        final User user3 = User.create(account3);

        user1.enableAccount();
        user2.disableAccount();
        user3.enableAccount();

        final List<AccountEntity> accounts = accountModel.getAllAccounts().blockingGet();
        final List<AccountEntity> usedInAppAccounts = CollectionsKt.filter(accounts, account -> account.isUsedInApp());
        assertThat(usedInAppAccounts).extracting(AccountEntity::getUid).containsExactlyInAnyOrder(user1.getUid(), user3.getUid());
    }

    @Test
    public void getAccountsInfo_returnsAccountsInfo() {
        final User user1 = User.createInactive(account1);
        final User user2 = User.create(account2);
        final User user3 = User.createInactive(account3);

        user1.enableAccount();
        user2.enableAccount();
        user3.disableAccount();

        user2.select();

        final List<AccountInfoContainer> accountsInfo = accountModel.getAccountsInfo().blockingGet();
        assertThat(accountsInfo).hasSize(3);
        // sort for determinism in tests
        Collections.sort(accountsInfo, (lhs, rhs) -> (int) (lhs.getId() - rhs.getId()));
        final AccountInfoContainer accountInfo1 = accountsInfo.get(0);
        final AccountInfoContainer accountInfo2 = accountsInfo.get(1);
        final AccountInfoContainer accountInfo3 = accountsInfo.get(2);

        assertThat(accountInfo1.getId()).isEqualTo(user1.getUid());
        assertThat(accountInfo1.getSelected()).isFalse();
        assertThat(accountInfo1.getUsedInApp()).isTrue();

        assertThat(accountInfo2.getId()).isEqualTo(user2.getUid());
        assertThat(accountInfo2.getSelected()).isTrue();
        assertThat(accountInfo2.getUsedInApp()).isTrue();

        assertThat(accountInfo3.getId()).isEqualTo(user3.getUid());
        assertThat(accountInfo3.getSelected()).isFalse();
        assertThat(accountInfo3.getUsedInApp()).isFalse();
    }

    @Test
    public void isUidRegisteredForPushkin_validUids() {
        final User user1 = User.createInactive(account1);
        final User user2 = User.create(account2);
        final User user3 = User.createInactive(account3);

        user1.enableAccount();
        user2.enableAccount();
        user3.disableAccount();

        user2.select();
        shadowOf(getMainLooper()).idle();

        SystemClock.setCurrentTimeMillis(System.currentTimeMillis());
        shadowOf(getMainLooper()).idle();
        accountModel.updatePushSubscribeState(user1.getUid(), false);
        accountModel.updatePushSubscribeState(user2.getUid(), true);
        accountModel.updatePushSubscribeState(user3.getUid(), false);
        shadowOf(getMainLooper()).idle();

        assertThat(user1.getDbAccount().getPushSubscribeTime()).isEqualTo(Constants.PUSH_NOT_SUBSCRIBED);
        assertThat(user2.getDbAccount().getPushSubscribeTime()).isNotEqualTo(Constants.PUSH_NOT_SUBSCRIBED);
        assertThat(user3.getDbAccount().getPushSubscribeTime()).isEqualTo(Constants.PUSH_NOT_SUBSCRIBED);
    }

    @Test
    public void isXTokenChanged_forUnAuthorizedAccount_returnsFalse() {
        final User user = User.create(unAuthorizedAccount);
        assertThat(accountModel.isXTokenChanged(user.getDbAccount())).isFalse();
    }

    @Test
    public void isXTokenChanged_returnsTrue() {
        final User user = User.create(account1);
        app().getPassportApi().setAccountXToken(account1.toAccount(), app, "newToken");
        assertThat(accountModel.isXTokenChanged(user.getDbAccount())).isTrue();
    }

    @Test
    public void readAccountSwitcherAccounts_readsAccounts() {
        final User user1 = User.createInactive(account1);
        final User user2 = User.create(account2);

        user1.enableAccount();
        user2.enableAccount();

        user2.select();

        app().getPassportApi().addAccountsToPassport(account3);

        final List<AccountInfoContainer> accounts = accountModel.readAccountSwitcherAccounts();
        assertThat(accounts).hasSize(3);
        // sort by name for determinism in tests
        Collections.sort(accounts, (lhs, rhs) -> lhs.getManagerName().compareTo(rhs.getManagerName()));
        final AccountInfoContainer account1 = accounts.get(0);
        final AccountInfoContainer account2 = accounts.get(1);
        final AccountInfoContainer account3 = accounts.get(2);

        assertThat(account1.getManagerName()).isEqualTo(AccountModelTest.account1.name);
        assertThat(account1.getInDb()).isTrue();

        assertThat(account2.getManagerName()).isEqualTo(AccountModelTest.account2.name);
        assertThat(account2.getInDb()).isTrue();

        assertThat(account3.getManagerName()).isEqualTo(AccountModelTest.account3.name);
        assertThat(account3.getInDb()).isFalse();
    }

    @Test
    public void getLcn_returnsLcn() {
        final User user1 = User.create(account1);
        accountModel.updateLcn(user1.getUid(), 123);

        assertThat(getLcnByUid(RuntimeEnvironment.application, user1.getUid())).isEqualTo(123L);
    }

    @Test
    public void getLcn_throwsOnInvalidAccount() {
        try {
            getLcnByUid(RuntimeEnvironment.application, Constants.NO_UID);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage()).containsIgnoringCase("was not found in the database");
        }
    }

    @Test
    public void deleteAccountsTask_handlesNotAccounts() {
        Intent intent = new Intent();
        intent.setAction("com.yandex.passport.client.ACCOUNT_REMOVED");
        new LoginAccountsChangedReceiver().onReceive(RuntimeEnvironment.application, intent);
        // should not crash
    }

    @Test
    public void deleteAccountsTask_handlesNoChanges() {
        final User user1 = User.create(account1);
        final User user2 = User.create(account2);

        Intent intent = new Intent();
        intent.setAction("com.yandex.passport.client.ACCOUNT_REMOVED");
        new LoginAccountsChangedReceiver().onReceive(RuntimeEnvironment.application, intent);
        // shouldn't delete anything

        assertThat(user1.existsIsDB()).isTrue();
        assertThat(user2.existsIsDB()).isTrue();
    }

    @Test
    public void deleteAccountsTask_handlesDeletedAccount() {
        final User user1 = User.create(account1);
        final User user2 = User.create(account2);
        user1.removeAccountFromAM();

        Intent intent = new Intent();
        intent.setAction("com.yandex.passport.client.ACCOUNT_REMOVED");
        intent.putExtra(PassportUid.Factory.KEY_UID, account1.uid);
        new LoginAccountsChangedReceiver().onReceive(RuntimeEnvironment.application, intent);

        assertThat(user1.existsIsDB()).isFalse();
        assertThat(user2.existsIsDB()).isTrue();
    }

    @Test
    public void deleteAccountsTask_handlesAllDeletedAccounts() {
        final User user1 = User.create(account1);
        final User user2 = User.create(account2);
        user1.removeAccountFromAM();
        user2.removeAccountFromAM();

        final LoginAccountsChangedReceiver receiver = new LoginAccountsChangedReceiver();
        sendRemoveAccountBroadcast(receiver, account1);
        sendRemoveAccountBroadcast(receiver, account2);

        assertThat(user1.existsIsDB()).isFalse();
        assertThat(user2.existsIsDB()).isFalse();
    }

    private void sendRemoveAccountBroadcast(@NonNull LoginAccountsChangedReceiver receiver, @NonNull LoginData account) {
        Intent intent = new Intent(PassportActions.CLIENT_ACTION_ACCOUNT_REMOVED);
        intent.putExtra(PassportActions.EXTRA_UID, account.uid);
        final int environment = Accounts.TEAM_TYPE.equals(account.type) ? 2 : 1;
        intent.putExtra(PassportActions.EXTRA_ENVIRONMENT, environment);
        receiver.onReceive(RuntimeEnvironment.application, intent);
    }

    @Test
    public void observeAllAccounts_returnsAccounts() {
        final User user1 = User.create(account1);
        final User user2 = User.create(account2);
        final User user3 = User.create(account3);

        final List<AccountEntity> accounts = accountModel.observeAllAccounts().blockingFirst();

        assertThat(accounts)
                .extracting(AccountEntity::getUid)
                .containsExactlyInAnyOrder(user1.getUid(), user2.getUid(), user3.getUid());
    }

    @Test
    public void deleteAllDbAccounts_deletesAccounts() {
        User.create(account1);
        User.create(account2);
        User.create(account3);

        accountModel.deleteAllDbAccounts();

        final List<AccountEntity> accounts = accountModel.observeAllAccounts().blockingFirst();
        assertThat(accounts).isEmpty();
    }

    @Test
    public void deleteDbAccounts_deleteDatabases() {
        final User user1 = User.create(account1);
        final User user2 = User.create(account2);
        final User user3 = User.create(account3);

        serverAccount1.addMessages(serverAccount1.newUnreadMessage(serverAccount1.getSpamFolder()).build());

        serverAccount2.addMessages(serverAccount2.newReadMessage(serverAccount2.getTrashFolder()).build());

        serverAccount3.addThreads(
                serverAccount3.newThread(
                        serverAccount3.newReadMessage(serverAccount3.getTrashFolder()),
                        serverAccount3.newReadMessage(serverAccount3.getTrashFolder())
                ).build()
        );

        user1.initialLoad();
        user2.initialLoad();
        user3.initialLoad();

        List<Long> ids = listOf(user1.getUid(), user3.getUid());

        accountModel.deleteDbAccounts(ids, false);

        assertThat(app().getDatabasePath(MailSupportSQLiteOpenHelper.Companion.getDatabaseName(user1.loginData.name))).doesNotExist();
        assertThat(app().getDatabasePath(MailSupportSQLiteOpenHelper.Companion.getDatabaseName(user2.loginData.name))).exists();
        assertThat(app().getDatabasePath(MailSupportSQLiteOpenHelper.Companion.getDatabaseName(user3.loginData.name))).doesNotExist();
    }

    @Test
    public void deleteDbAccounts_deletesAccounts() {
        final User user1 = User.create(account1);
        final User user2 = User.create(account2);
        final User user3 = User.create(account3);

        accountModel.deleteDbAccounts(listOf(user1.getUid(), user3.getUid()), false);

        final List<AccountEntity> accounts = accountModel.observeAllAccounts().blockingFirst();
        assertThat(accounts)
                .extracting(AccountEntity::getUid)
                .containsExactlyInAnyOrder(user2.getUid());
    }

    @Test
    public void selectAccount_selectsAccount() {
        final User user1 = User.create(account1);
        final User user2 = User.create(account2);
        final User user3 = User.create(account3);

        accountModel.selectAccount(user2.getUid());

        final List<AccountEntity> accounts = new ArrayList<>(accountModel.observeAllAccounts().blockingFirst());
        Collections.sort(accounts, (a, b) -> (int) (a.getUid() - b.getUid())); // sort for predictable testing

        assertThat(accounts.get(0).isSelected()).isFalse();
        assertThat(accounts.get(1).isSelected()).isTrue();
        assertThat(accounts.get(2).isSelected()).isFalse();
    }

    @Test
    public void observeAccountsInfoWithMails_shouldMergeAccountsFromDbAndFromAm() {
        final User user1 = User.create(account1);
        user1.initialLoad();
        app().getPassportApi().addAccountsToPassport(account2);

        loadDefaultTestSettingsToUser(user1.getUid(), "name1", "email1");

        TestSubscriber<List<AccountInfoContainer>> testSubscriber = accountModel.observeAccountsInfoWithMails().test();



        testSubscriber.assertNotComplete();
        final List<AccountInfoContainer> firstValue = listOf(
                AccountInfoContainer.create(
                        account2.uid,
                        account2.name,
                        SYSTEM_ACCOUNT_TYPE,
                        true,
                        false,
                        true,
                        AccountType.TEAM,
                        MailProvider.YANDEX,
                        true,
                        account2.name,
                        account2.name,
                        false
                ),
                AccountInfoContainer.create(
                        user1.getUid(),
                        user1.loginData.name,
                        SYSTEM_ACCOUNT_TYPE,
                        true,
                        true,
                        true,
                        AccountType.LOGIN,
                        MailProvider.YANDEX,
                        false,
                        "name1",
                        "email1",
                        true
                )
        );
        final List<AccountInfoContainer> secondValue = listOf(
                AccountInfoContainer.create(
                        account2.uid,
                        account2.name,
                        SYSTEM_ACCOUNT_TYPE,
                        true,
                        false,
                        true,
                        AccountType.TEAM,
                        MailProvider.YANDEX,
                        true,
                        account2.name,
                        account2.name,
                        true
                ),
                AccountInfoContainer.create(
                        user1.getUid(),
                        user1.loginData.name,
                        SYSTEM_ACCOUNT_TYPE,
                        true,
                        true,
                        true,
                        AccountType.LOGIN,
                        MailProvider.YANDEX,
                        false,
                        "name1",
                        "email1",
                        true
                )
        );
        testSubscriber.assertValues(firstValue, secondValue);
    }

    @Test
    public void observeAccountsInfoWithMails_shouldLoadAccountsFromDb() {
        final User user1 = User.create(account1);
        user1.initialLoad();
        final User user2 = User.createInactive(account2);
        user2.initialLoad();
        final User user3 = User.createInactive(account3);
        user3.initialLoad();

        loadDefaultTestSettingsToUser(user1.getUid(), "name1", "email1");
        loadDefaultTestSettingsToUser(user2.getUid(), "name2", "email2");
        loadDefaultTestSettingsToUser(user3.getUid(), "name3", "email3");

        TestSubscriber<List<AccountInfoContainer>> testSubscriber = accountModel.observeAccountsInfoWithMails().test();

        testSubscriber.assertNotComplete();
        testSubscriber.assertValue(listOf(
                AccountInfoContainer.create(
                        user2.getUid(),
                        user2.loginData.name,
                        SYSTEM_ACCOUNT_TYPE,
                        true,
                        false,
                        true,
                        AccountType.TEAM,
                        MailProvider.YANDEX,
                        false,
                        "name2",
                        "email2",
                        true
                ),
                AccountInfoContainer.create(
                        user1.getUid(),
                        user1.loginData.name,
                        SYSTEM_ACCOUNT_TYPE,
                        true,
                        true,
                        true,
                        AccountType.LOGIN,
                        MailProvider.YANDEX,
                        false,
                        "name1",
                        "email1",
                        true
                ),
                AccountInfoContainer.create(
                        user3.getUid(),
                        user3.loginData.name,
                        SYSTEM_ACCOUNT_TYPE,
                        true,
                        false,
                        true,
                        AccountType.MAILISH,
                        MailProvider.GMAIL,
                        false,
                        "name3",
                        "email3",
                        true
                )
        ));
    }

    @Test
    public void getAccountsInfoWithMails_shouldSetDefaultNameIfNotSynced() {
        final User user1 = User.createInactive(account1);
        user1.initialLoad();

        loadDefaultTestSettingsToUser(user1.getUid(), "name", "email");
        settingsModel.accountSettings(user1.getUid()).edit().setSyncedWithServer(false).commitAndSync();

        List<AccountInfoContainer> result = accountModel.getAccountsInfoWithMails().blockingGet();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(user1.getUid());
        // for inactive users we don't use name and email even if they have
        assertThat(result.get(0).getName()).isEqualTo("account1");
        assertThat(result.get(0).getEmail()).isEqualTo("account1");
    }

    @Test
    public void getAccountsInfoWithMails_shouldSetNameAndEmailIfSynced() {
        final User user1 = User.create(account1);
        user1.initialLoad();

        final String name = "name";
        final String email = "email";

        loadDefaultTestSettingsToUser(user1.getUid(), name, email);

        List<AccountInfoContainer> result = accountModel.getAccountsInfoWithMails().blockingGet();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(user1.getUid());
        assertThat(result.get(0).getName()).isEqualTo("name");
        assertThat(result.get(0).getEmail()).isEqualTo("email");
    }


    @Test
    public void getAccountsInfoWithMails_shouldReturnCorrectList() {
        final User user1 = User.createInactive(account1);
        final User user2 = User.create(account2);
        user1.initialLoad();
        user2.initialLoad();

        List<AccountInfoContainer> result = accountModel.getAccountsInfoWithMails().blockingGet();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(user2.getUid()); //team account first!
        assertThat(result.get(1).getId()).isEqualTo(user1.getUid());
    }

    @Test
    public void getAccountsInfoWithMails_shouldNotReturnNotAmAccounts() throws Exception {
        final User user1 = User.create(account2);
        user1.initialLoad();

        List<AccountInfoContainer> result = accountModel.getAccountsInfoWithMails().blockingGet();
        assertThat(result).hasSize(1);

        deleteAccountFromAm(result.get(0).getId());

        result = accountModel.getAccountsInfoWithMails().blockingGet();
        assertThat(result).hasSize(0);
    }

    @Test
    public void setAccountLogged_setsLoggedState() throws AccountNotInDBException {
        final User user1 = User.create(account1);
        final User user2 = User.createInactive(account2);

        accountModel.setHasToken(user1.getUid(), true);
        accountModel.setHasToken(user2.getUid(), false);

        assertThat(accountModel.isAccountLogged(user1.getUid())).isTrue();
        assertThat(accountModel.isAccountLogged(user2.getUid())).isFalse();

        accountModel.setHasToken(user1.getUid(), false);

        assertThat(accountModel.isAccountLogged(user1.getUid())).isFalse();
    }

    @Test
    public void removingData() {
        AccountModel spyAccountModel = spy(accountModel);

        final MockPassportAccountWrapper passportAccount = new MockPassportAccountWrapper(
                1,
                "test",
                "type",
                true,
                AccountType.MAILISH,
                MailProvider.GMAIL,
                false,
                false
        );
        spyAccountModel.insertPassportAccount(passportAccount, false);
        spyAccountModel.setHasToken(1, false);
        Mockito.verify(spyAccountModel).clearAccountData(1);
    }

    @Test
    public void setAccountsEnabled_enablesAccounts() throws AccountNotInDBException {
        final User user1 = User.create(account1);
        final User user2 = User.createInactive(account2);
        final User user3 = User.createInactive(account3);
        user2.select();

        Map<Long, Boolean> enabledMap = new HashMap<>(3);
        enabledMap.put(user1.getUid(), true);
        enabledMap.put(user2.getUid(), false);
        enabledMap.put(user3.getUid(), true);
        setAccountsEnabled(enabledMap);

        final AccountEntity acc1 = accountModel.observeAccountByUid(user1.getUid()).blockingFirst().get();
        final AccountEntity acc2 = accountModel.observeAccountByUid(user2.getUid()).blockingFirst().get();
        final AccountEntity acc3 = accountModel.observeAccountByUid(user3.getUid()).blockingFirst().get();

        assertThat(acc1.isSelected()).isFalse();
        assertThat(acc2.isSelected()).isFalse();
        assertThat(acc3.isSelected()).isFalse();

        assertThat(acc1.isUsedInApp()).isTrue();
        assertThat(acc2.isUsedInApp()).isFalse();
        assertThat(acc3.isUsedInApp()).isTrue();
    }

    @Test
    public void setAccountsEnabled_preservesSelectionForIrrelevantAccount() throws AccountNotInDBException {
        final User user1 = User.create(account1);
        final User user2 = User.createInactive(account2);
        final User user3 = User.createInactive(account3);
        user2.select();

        Map<Long, Boolean> enabledMap = new HashMap<>(3);
        enabledMap.put(user3.getUid(), true);
        setAccountsEnabled(enabledMap);

        final AccountEntity acc1 = accountModel.observeAccountByUid(user1.getUid()).blockingFirst().get();
        final AccountEntity acc2 = accountModel.observeAccountByUid(user2.getUid()).blockingFirst().get();
        final AccountEntity acc3 = accountModel.observeAccountByUid(user3.getUid()).blockingFirst().get();

        assertThat(acc1.isSelected()).isFalse();
        assertThat(acc2.isSelected()).isTrue();
        assertThat(acc3.isSelected()).isFalse();
    }

    @Test
    public void setAccountEnabled_changesAccountAvailability() throws AccountNotInDBException {
        final User user1 = User.create(account1);
        final User user2 = User.create(account2);

        accountModel.setAccountEnabled(user1.getUid(), false);
        AccountEntity acc1 = accountModel.observeAccountByUid(user1.getUid()).blockingFirst().get();
        AccountEntity acc2 = accountModel.observeAccountByUid(user2.getUid()).blockingFirst().get();
        assertThat(acc1.isUsedInApp()).isFalse();
        assertThat(acc2.isUsedInApp()).isTrue();

        accountModel.setAccountEnabled(user1.getUid(), true);
        acc1 = accountModel.observeAccountByUid(user1.getUid()).blockingFirst().get();
        acc2 = accountModel.observeAccountByUid(user2.getUid()).blockingFirst().get();
        assertThat(acc1.isUsedInApp()).isTrue();
        assertThat(acc2.isUsedInApp()).isTrue();
    }


    @Test
    public void getAuthUrl_shouldReturnAmUrl() {
        final User user1 = User.create(account1);
        String url = accountModel.getAuthUrl(user1.getUid(), "https://passport.yandex.ru/phones").blockingGet();
        assertThat(url).isEqualTo(MockPassportApi.AUTH_URL);
    }

    @Test
    public void getAuthUrl_shouldRethrowAmException() {
        final User user1 = User.create(accountWithAuthError);
        accountModel.getAuthUrl(user1.getUid(), "https://passport.yandex.ru/phones")
                .test()
                .assertError(AmException.class)
                .awaitTerminalEvent();
    }

    @Test
    public void getAuthUrl_shouldRethrowAccountDeletedException() {
        accountModel.getAuthUrl(121244, "https://passport.yandex.ru/phones")
                .test()
                .assertError(AmException.class)
                .awaitTerminalEvent();
    }

    @Test
    public void test_getAccountByUid() throws AccountNotInDBException {
        final User user1 = User.create(account1);
        final User user2 = User.createInactive(account2);

        final Account acc1 = accountModel.getAccountByUid(user1.getUid());
        assertThat(acc1.name).isEqualTo(account1.getName());

        final Account acc2 = accountModel.getAccountByUid(user2.getUid());
        assertThat(acc2.name).isEqualTo(account2.getName());

        // hit cache
        final Account cachedAcc1 = accountModel.getAccountByUid(user1.getUid());
        assertThat(cachedAcc1.name).isEqualTo(account1.getName());
    }

    @Test
    public void test_isAccountIsLogged() throws AccountNotInDBException {
        final User user1 = User.create(account1);
        final User user2 = User.createNotLogged(account2);

        long loggedAccId = user1.getUid();
        long notLoggedAccId = user2.getUid();
        long notExistedAccId = 999L;

        assertThat(accountModel.isAccountLogged(loggedAccId)).isTrue();
        assertThat(accountModel.isAccountLogged(notLoggedAccId)).isFalse();
        assertThatThrownBy(() -> accountModel.isAccountLogged(notExistedAccId)).isInstanceOf(AccountNotInDBException.class);
    }

    @Test
    public void insertOfMailishAccountReportsToMetrica() {
        final MockPassportAccountWrapper passportAccount =
                new MockPassportAccountWrapper(1, "test", "type", true, AccountType.MAILISH, MailProvider.GMAIL, false, false);
        accountModel.insertPassportAccount(passportAccount, false);
        metrica.assertLastEvent(
                ExternalMailsMetrics.EXTERNAL_MAIL_SUCCESSFUL_LOGIN,
                singletonMap(ExternalMailsMetrics.EXTERNAL_MAIL_PROVIDER_NAME, MailProvider.GMAIL.getStringRepresentation())
        );
    }

    @Test
    public void insertAccount_shouldDisableAdOnAddTeamAccount() throws Exception {
        initBase();
        generalSettings.edit().setAdShown(true).apply();

        FakeServer.getInstance().createAccountWrapper(Accounts.teamLoginData);
        User.create(Accounts.teamLoginData);

        assertThat(generalSettings.isAdShown()).isFalse();
    }

    @Test
    public void insertAccount_shouldLeftAdEnabledOnAddRegularAccount() throws Exception {
        initBase();
        generalSettings.edit().setAdShown(true).apply();

        FakeServer.getInstance().createAccountWrapper(Accounts.testLoginData);
        User.create(Accounts.testLoginData);

        assertThat(generalSettings.isAdShown()).isTrue();
    }

    @Test
    public void insertAccount_shouldLeftAdDisabledOnAddRegularAccount() throws Exception {
        initBase();
        generalSettings.edit().setAdShown(false).apply();

        FakeServer.getInstance().createAccountWrapper(Accounts.testLoginData);
        User.create(Accounts.testLoginData);

        assertThat(generalSettings.isAdShown()).isFalse();
    }

    @Test
    public void getAuthorizedAccounts_returnsNotAuthorizedAccounts() {
        final User user1 = User.create(account1);
        final User user2 = User.createInactive(account2);
        final User user3 = User.createInactive(account3);

        accountModel.setHasToken(user1.getUid(), false);
        assertThat(accountModel.getAuthorizedAccounts().blockingFirst()).containsExactly(user2.getUid(), user3.getUid());
    }

    private void loadDefaultTestSettingsToUser(long uid, @NonNull String name, @NonNull String email) {
        final AccountSettingsEditor editor = settingsModel.accountSettings(uid).edit();
        editor
                .setComposeCheck("check")
                .setThreadModeEnabled(false)
                .setPushNotificationsEnabled(true)
                .setSuid("suid")
                .setUid("uid")
                .setDefaultName(name)
                .setDefaultEmail(email)
                .setSignaturePlace(SignaturePlace.AFTER_REPLY)
                .setThemeEnabled(false)
                .setTheme(null)
                .setUseDefaultSignature(true)
                .setSyncedWithServer()
                .commitAndSync();
    }

    private void deleteAccountFromAm(long uid) throws Exception {
        passportApi.removeAccount(PassportUid.Factory.from(uid));
    }

    @NonNull
    private AMbundle createBundle(@NonNull LoginData loginData) {
        Bundle sourceBundle = new Bundle();
        Intent intent = new Intent();
        intent.putExtra(AMbundle.AM_BUNDLE_KEY_ACCOUNT_NAME, loginData.getName());
        sourceBundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return AMbundle.from(sourceBundle);
    }
}
