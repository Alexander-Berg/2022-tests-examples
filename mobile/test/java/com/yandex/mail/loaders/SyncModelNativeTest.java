package com.yandex.mail.loaders;

import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.metrica.MetricaConstns.PerfMetrics.RootEvents;
import com.yandex.mail.model.SettingsModel;
import com.yandex.mail.model.SyncModel;
import com.yandex.mail.network.response.SettingsJson;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.settings.AccountSettings;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.User;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.wrappers.SettingsWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;

import static com.yandex.mail.wrappers.SettingsWrapper.makeAddress;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class SyncModelNativeTest extends BaseIntegrationTest {

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private SyncModel syncModel;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private SettingsModel settingsModel;

    @Before
    public void setup() {
        account = FakeServer.getInstance().createAccountWrapper(Accounts.testLoginData);
        user = User.create(Accounts.testLoginData);
        accountComponent = IntegrationTestRunner.app().getAccountComponent(user.getUid());
        syncModel = accountComponent.syncModel();
        settingsModel = accountComponent.settingsModel();
    }

    /*
        No settings yet on new account
     */
    @Test
    public void testNewAccount() {
        assertThat(accountComponent.settings().syncedWithServer()).isFalse();
    }

    @Test
    public void loadSettings_emails() {
        String login = user.getLoginData().name;
        String[] domains = new String[] {"ya.ru", "yandex.ru", "narod.ru"};

        Set<String> expectedEmails = new HashSet<>(Arrays.asList
                (login + "@" + domains[0]
                        , login + "@" + domains[1]
                        , login + "@" + domains[2]
                ));

        SettingsJson.Emails.Address[] addresses = {
                makeAddress(login, domains[0]),
                makeAddress(login, domains[1]),
                makeAddress(login, domains[2])
        };

        account.setSettings(SettingsWrapper.defaultSettings(user.loginData).addresses(addresses).build());

        syncModel.loadSettings(RootEvents.NO_OP);
        AccountSettings settings = accountComponent.settings();

        assertThat(settings).isNotNull();
        Set<String> emails = settingsModel.getEmailsOrLoad().blockingGet();
        assertThat(emails).hasSameElementsAs(expectedEmails);
    }

    @Test
    public void loadSettings_signature() {
        String login = user.getLoginData().name;
        String expectedSignature = "signature";

        account.setSettings(SettingsWrapper.defaultSettings(user.loginData).signature(expectedSignature).build());

        syncModel.loadSettings(RootEvents.NO_OP);
        AccountSettings settings = accountComponent.settings();

        assertThat(settings).isNotNull();
        String signature = settings.signature();
        assertThat(signature).isEqualTo(expectedSignature);
    }

    @Test
    public void getEmailsOrLoad_shouldReturnExistingEmails() {
        settingsModel.insertEmails(Arrays.asList("email1@test.ru", "email2@test.ru"));

        final Set<String> emails = settingsModel.getEmailsOrLoad().blockingGet();

        assertThat(emails).containsExactly("email1@test.ru", "email2@test.ru");
    }

    @Test
    public void getEmailsOrLoad_shouldLoadSettingsIfNoEmailsAndReturnLoaded() {
        String login = user.getLoginData().name;
        String[] domains = new String[] {"ya.ru", "yandex.ru", "narod.ru"};

        Set<String> expectedEmails = new HashSet<>(Arrays.asList
                (login + "@" + domains[0]
                        , login + "@" + domains[1]
                        , login + "@" + domains[2]
                ));

        SettingsJson.Emails.Address[] addresses = {
                makeAddress(login, domains[0]),
                makeAddress(login, domains[1]),
                makeAddress(login, domains[2])
        };

        account.setSettings(SettingsWrapper.defaultSettings(user.loginData).addresses(addresses).build());

        assertThat(settingsModel.getEmails().firstOrError().blockingGet()).isEmpty();

        final Set<String> emails = settingsModel.getEmailsOrLoad().blockingGet();
        assertThat(emails).containsOnlyElementsOf(expectedEmails);
    }
}
