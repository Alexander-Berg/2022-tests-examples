package com.yandex.mail.storage.preferences;

import androidx.annotation.NonNull;

import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class ReleaseDeveloperSettingsTest {

    @SuppressWarnings("NullableProblems") // Before
    @NonNull
    DeveloperSettings developerSettings;

    @Before
    public void beforeEachTestCase() {
        developerSettings = new DeveloperSettings(IntegrationTestRunner.app());
    }

    @Test
    public void isStethoEnabled_shouldBeFalseByDefault() {
        assertThat(developerSettings.isStethoEnabled()).isFalse();
    }
}