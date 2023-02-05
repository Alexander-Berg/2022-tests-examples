package com.yandex.mail.storage.preferences;

import com.yandex.mail.BuildConfig;
import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class DeveloperSettingsTest {

    @SuppressWarnings("NullableProblems") // Before
    @NonNull
    DeveloperSettings developerSettings;

    @Before
    public void beforeEachTestCase() {
        developerSettings = new DeveloperSettings(IntegrationTestRunner.app());
    }

    @Test
    public void isWebViewDebuggingEnabled_shouldBeFalseByDefault() {
        if (BuildConfig.DEBUG) {
            assertThat(developerSettings.isWebViewDebuggingEnabled()).isTrue();
        } else {
            assertThat(developerSettings.isWebViewDebuggingEnabled()).isFalse();
        }

    }

    @Test
    public void saveWebViewDebuggingEnabled_isWebViewDebuggingEnabled() {
        developerSettings.saveWebViewDebuggingEnabled(true);
        assertThat(developerSettings.isWebViewDebuggingEnabled()).isTrue();

        developerSettings.saveWebViewDebuggingEnabled(false);
        assertThat(developerSettings.isWebViewDebuggingEnabled()).isFalse();
    }

    @Test
    public void saveStethoEnabled_isStethoEnabled() {
        developerSettings.saveStethoEnabled(true);
        assertThat(developerSettings.isStethoEnabled()).isTrue();

        developerSettings.saveStethoEnabled(false);
        assertThat(developerSettings.isStethoEnabled()).isFalse();
    }

    @Test
    public void isTinyDancerEnabled_shouldBeFalseByDefault() {
        assertThat(developerSettings.isTinyDancerEnabled()).isFalse();
    }

    @Test
    public void saveTinyDancerEnabled_isTinyDancerEnabled() {
        developerSettings.saveTinyDancerEnabled(true);
        assertThat(developerSettings.isTinyDancerEnabled()).isTrue();

        developerSettings.saveTinyDancerEnabled(false);
        assertThat(developerSettings.isTinyDancerEnabled()).isFalse();
    }

    @Test
    public void isLeakCanaryEnabled_shouldBeTrueByDefault() {
        assertThat(developerSettings.isLeakCanaryEnabled()).isTrue();
    }

    @Test
    public void saveLeakCanaryEnabled_isLeakCanaryEnabled() {
        developerSettings.saveLeakCanaryEnabled(true);
        assertThat(developerSettings.isLeakCanaryEnabled()).isTrue();

        developerSettings.saveLeakCanaryEnabled(false);
        assertThat(developerSettings.isLeakCanaryEnabled()).isFalse();
    }

    @Test
    public void areTimingsToastsEnabled_shouldBeFalseByDefault() {
        assertThat(developerSettings.areTimingsToastsEnabled()).isFalse();
    }

    @Test
    public void saveTimingsToastsEnabled_areTimingsToastsEnabled() {
        developerSettings.saveTimingsToastsEnabled(true);
        assertThat(developerSettings.areTimingsToastsEnabled()).isTrue();

        developerSettings.saveTimingsToastsEnabled(false);
        assertThat(developerSettings.areTimingsToastsEnabled()).isFalse();
    }

    @Test
    public void setFakeExternalMailsUazEnabled_isFakeExternalMailsUazEnabled() {
        developerSettings.setFakeExternalMailsUazEnabled(true);
        assertThat(developerSettings.isFakeExternalMailsUazEnabled()).isTrue();

        developerSettings.setFakeExternalMailsUazEnabled(false);
        assertThat(developerSettings.isFakeExternalMailsUazEnabled()).isFalse();
    }

    @Test
    public void getFakeAdBlockId_shouldReturnNullByDefault() {
        assertThat(developerSettings.getFakeAdBlockId()).isNull();
    }

    @Test
    public void saveFakeAdBlockId_getFakeAdBlockId() {
        developerSettings.saveFakeAdBlockId("test");
        assertThat(developerSettings.getFakeAdBlockId()).isEqualTo("test");

        developerSettings.saveFakeAdBlockId(null);
        assertThat(developerSettings.getFakeAdBlockId()).isNull();
    }

    @Test
    public void getNetworkSettings_returnsNulLByDefault() {
        final NetworkSettings networkSettings = developerSettings.getNetworkSettings();
        assertThat(networkSettings.getConnectTimeoutMillis()).isNull();
        assertThat(networkSettings.getReadTimeoutMillis()).isNull();
    }

    @Test
    public void saveNetworkSettings_savesSettings() {
        developerSettings.saveNetworkSettings(NetworkSettings.fromTimeouts(10L, 20L));
        assertThat(developerSettings.getNetworkSettings().getConnectTimeoutMillis()).isEqualTo(10L);
        assertThat(developerSettings.getNetworkSettings().getReadTimeoutMillis()).isEqualTo(20L);

        developerSettings.saveNetworkSettings(NetworkSettings.fromTimeouts(123L, null));
        assertThat(developerSettings.getNetworkSettings().getConnectTimeoutMillis()).isEqualTo(123L);
        assertThat(developerSettings.getNetworkSettings().getReadTimeoutMillis()).isNull();

        developerSettings.saveNetworkSettings(NetworkSettings.fromTimeouts(null, null));
        assertThat(developerSettings.getNetworkSettings().getConnectTimeoutMillis()).isNull();
        assertThat(developerSettings.getNetworkSettings().getReadTimeoutMillis()).isNull();
    }
}
