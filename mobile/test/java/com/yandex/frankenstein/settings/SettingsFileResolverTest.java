package com.yandex.frankenstein.settings;

import com.yandex.frankenstein.io.ResourceReader;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class SettingsFileResolverTest {

    private static final String WITH_SINGLE_UNOCCUPIED_SETTINGS_DIR = "single_unoccupied";
    private static final String WITH_MULTIPLE_UNOCCUPIED_SETTINGS_DIR = "multiple_unoccupied";
    private static final String WITH_NO_JSON_SETTINGS_DIR = "no_json";
    private static final String WITH_ALL_OCCUPIED_SETTINGS_DIR = "all_occupied";

    @Test
    public void testGetSingleUnoccupiedSettingsFile() {
        final SettingsFileResolver settingsFileResolver =
                new SettingsFileResolver(WITH_SINGLE_UNOCCUPIED_SETTINGS_DIR, new ResourceReader());

        final File settingsFile = settingsFileResolver.getSettingsFile();
        assertThat(settingsFile).isNotNull();
        assertThat(settingsFile.getName()).isEqualTo("settings-1.json");
    }

    @Test
    public void testGetMultipleUnoccupiedSettingsFile() {
        final SettingsFileResolver settingsFileResolver =
                new SettingsFileResolver(WITH_MULTIPLE_UNOCCUPIED_SETTINGS_DIR, new ResourceReader());

        final File settingsFile = settingsFileResolver.getSettingsFile();
        assertThat(settingsFile).isNotNull();
        assertThat(settingsFile.getName()).isIn("settings-1.json", "settings-2.json");
    }

    @Test
    public void testGetSettingsFileWhenAllOccupied() {
        final SettingsFileResolver settingsFileResolver =
                new SettingsFileResolver(WITH_ALL_OCCUPIED_SETTINGS_DIR, new ResourceReader());

        final File settingsFile = settingsFileResolver.getSettingsFile();
        assertThat(settingsFile).isNull();
    }

    @Test
    public void testGetSettingsFileWhenNoJson() {
        final SettingsFileResolver settingsFileResolver =
                new SettingsFileResolver(WITH_NO_JSON_SETTINGS_DIR, new ResourceReader());

        final File settingsFile = settingsFileResolver.getSettingsFile();
        assertThat(settingsFile).isNull();
    }
}
