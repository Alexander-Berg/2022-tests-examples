package com.yandex.mail.model;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.settings.AccountSettings;
import com.yandex.mail.settings.GeneralSettings;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.User;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import kotlin.Pair;

import static com.yandex.mail.settings.GeneralSettingsConstants.DEFAULT_CACHE_SIZE_LIMIT;
import static com.yandex.mail.settings.GeneralSettingsConstants.DEFAULT_COMPACT_MODE;
import static com.yandex.mail.settings.GeneralSettingsConstants.DEFAULT_DO_NOT_DISTURB_ENABLED;
import static com.yandex.mail.settings.GeneralSettingsConstants.DEFAULT_DO_NOT_DISTURB_TIME_FROM_HOURS;
import static com.yandex.mail.settings.GeneralSettingsConstants.DEFAULT_DO_NOT_DISTURB_TIME_FROM_MINUTES;
import static com.yandex.mail.settings.GeneralSettingsConstants.DEFAULT_DO_NOT_DISTURB_TIME_TO_HOURS;
import static com.yandex.mail.settings.GeneralSettingsConstants.DEFAULT_DO_NOT_DISTURB_TIME_TO_MINUTES;
import static com.yandex.mail.settings.GeneralSettingsConstants.DEFAULT_IS_AD_SHOWN;
import static com.yandex.mail.settings.GeneralSettingsConstants.DEFAULT_NOTIFICATION_BEEP;
import static com.yandex.mail.settings.GeneralSettingsConstants.DEFAULT_NOTIFICATION_BEEP_ENABLED;
import static com.yandex.mail.settings.GeneralSettingsConstants.DEFAULT_NOTIFICATION_VIBRATION_ENABLED;
import static com.yandex.mail.settings.GeneralSettingsConstants.DEFAULT_SWIPE_ACTION;
import static com.yandex.mail.settings.SwipeAction.ARCHIVE;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore("TODO: remove this. we will use nanomail model")
@RunWith(IntegrationTestRunner.class)
public class GeneralSettingsModelTest {

    @NonNull
    private static final String INBOX_FID = "1000"; // value from ContainersGenerator for Inbox

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private AccountWrapper account;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private User user;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private GeneralSettingsModel model;

    @Before
    public void setup() {
        account = FakeServer.getInstance().createAccountWrapper(Accounts.testLoginData);
        user = User.create(Accounts.testLoginData);

        user.initialLoad();

        model = IntegrationTestRunner.app().getApplicationComponent().settingsModel();
    }

    @Test
    public void testGetAccountSettingsForExistentAccount() throws Exception {
        final AccountSettings settings = model.accountSettings(user.getUid());

        assertThat(settings).isNotNull();
    }

    @Test
    public void saveGeneralSettings() {
        GeneralSettings generalSettings = model.getGeneralSettings();

        generalSettings.edit()
                .setCompactModeEnabled(true)
                .setSwipeAction(ARCHIVE)
                .apply();

        generalSettings = model.getGeneralSettings();

        assertThat(generalSettings.isCompactMode()).isTrue();
        assertThat(generalSettings.swipeAction()).isEqualTo(ARCHIVE);
    }

    @Test
    public void generalSettings_shouldSaveChanges() {
        GeneralSettings generalSettings = model.getGeneralSettings();

        generalSettings.edit()
                .setDoNotDisturbTimeFrom(12, 23)
                .apply();

        final GeneralSettings newSettings = model.getGeneralSettings();
        Assertions.assertThat(newSettings.doNotDisturbTimeFrom()).isEqualTo(new Pair<>(12, 23));
    }

    @Test
    public void generalSettings_setsDefaultValues() {
        final GeneralSettings modelSettings = model.getGeneralSettings();

        assertThat(modelSettings.swipeAction()).isEqualTo(DEFAULT_SWIPE_ACTION);
        assertThat(modelSettings.isCompactMode()).isEqualTo(DEFAULT_COMPACT_MODE);
        assertThat(modelSettings.cacheSizeLimit()).isEqualTo(DEFAULT_CACHE_SIZE_LIMIT);
        assertThat(modelSettings.isDoNotDisturbEnabled()).isEqualTo(DEFAULT_DO_NOT_DISTURB_ENABLED);
        Assertions.assertThat(modelSettings.doNotDisturbTimeFrom())
                .isEqualTo(new Pair<>(DEFAULT_DO_NOT_DISTURB_TIME_FROM_HOURS, DEFAULT_DO_NOT_DISTURB_TIME_FROM_MINUTES));
        Assertions.assertThat(modelSettings.doNotDisturbTimeTo())
                .isEqualTo(new Pair(DEFAULT_DO_NOT_DISTURB_TIME_TO_HOURS, DEFAULT_DO_NOT_DISTURB_TIME_TO_MINUTES));
        assertThat(modelSettings.isNotificationBeepEnabled()).isEqualTo(DEFAULT_NOTIFICATION_BEEP_ENABLED);
        assertThat(modelSettings.notificationBeep()).isEqualTo(DEFAULT_NOTIFICATION_BEEP);
        assertThat(modelSettings.isNotificationVibrationEnabled()).isEqualTo(DEFAULT_NOTIFICATION_VIBRATION_ENABLED);
        assertThat(modelSettings.isAdShown()).isEqualTo(DEFAULT_IS_AD_SHOWN);
        assertThat(modelSettings.isSmartReplyTurnedOn(false)).isEqualTo(false);
        assertThat(modelSettings.isSmartReplyTurnedOn(true)).isEqualTo(true);
    }

//    @Ignore // we don't have sync preference in design of preferences now but it should appear in 3.15+
//    @Test
//    public void getFoldersToSync_shouldReturnEmptyListOnSyncDisabledAccount() {
//        AccountSettings accountSettings = model.accountSettings(user.getUidFromDB());
//        accountSettings.editAndSync().setSyncEnabled(false).commitAndSync();
//
//        SolidList<String> folders = model.getFoldersToSync(user.getUidFromDB()).blockingGet();
//        assertThat(folders).isEqualTo(empty());
//    }
//
//    @Test
//    public void getFoldersToSync_shouldReturnNonEmptyListOnSyncEnabledAccount() {
//        SolidList<String> folders = model.getFoldersToSync(user.getUidFromDB()).blockingGet();
//
//        assertThat(folders).isEqualTo(SolidList.list(INBOX_FID));
//    }
//
//    @Test
//    public void checkFolderExists_inboxShouldExists() {
//        boolean inboxExists = model.checkFolderExists(user.getUidFromDB(), INBOX_FID).blockingGet();
//
//        assertThat(inboxExists).isTrue();
//    }
}
