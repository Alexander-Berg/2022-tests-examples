package com.yandex.mail.model;

import com.yandex.mail.di.AccountComponent;
import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.network.response.SettingsJson;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.User;
import com.yandex.mail.util.BaseIntegrationTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class SettingsModelTest extends BaseIntegrationTest {

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private AccountWrapper account;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private AccountComponent accountComponent;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private SettingsModel settingsModel;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private GeneralSettingsModel model;

    @Before
    public void setup() {
        account = FakeServer.getInstance().createAccountWrapper(Accounts.testLoginData);
        final User user = User.create(Accounts.testLoginData);
        user.initialLoad();

        accountComponent = IntegrationTestRunner.app().getAccountComponent(user.getUid());

        settingsModel = accountComponent.settingsModel();
        model = IntegrationTestRunner.app().getApplicationComponent().settingsModel();
    }

    @Test
    public void updateShowFolderTabs() {
        SettingsJson settingsJson = account.getSettings().generateSettingsResponse(true);
        settingsModel.storeSettings(settingsJson);
        assertThat(model.accountSettings(account.loginData.uid).areTabsEnabled()).isEqualTo(true);

        settingsJson = account.getSettings().generateSettingsResponse(false);
        settingsModel.storeSettings(settingsJson);
        assertThat(model.accountSettings(account.loginData.uid).areTabsEnabled()).isEqualTo(false);

        settingsJson = account.getSettings().generateSettingsResponse(true);
        settingsModel.storeSettings(settingsJson);
        assertThat(model.accountSettings(account.loginData.uid).areTabsEnabled()).isEqualTo(true);
    }
}
