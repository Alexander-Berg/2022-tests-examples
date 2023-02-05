package com.yandex.frankenstein.settings;

import com.yandex.frankenstein.io.IOReader;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeviceSettingsTest {

    private static final String VERSION = "4.2";
    private static final String UNKNOWN_VERSION = "1.0.0";
    private static final String DEVICE_SETTINGS_FILE = "device_settings.txt";

    private final Map<String, String> mCapabilities = Collections.singletonMap("capability_key", "capability_value");
    private final JSONObject mCapabilitiesJson = new JSONObject(mCapabilities);
    private final JSONObject mRunnerConfiguration = new JSONObject().put("capabilities", mCapabilitiesJson);

    private final Map<String, String> mVersionSettings = new HashMap<>();
    private final Map<String, String> mCommonVersionSettings = new HashMap<>();
    {
        final String overriddenCommonKey = "overridden_common_key";

        mVersionSettings.put("key", "value");
        mVersionSettings.put(overriddenCommonKey, "custom_value");

        mCommonVersionSettings.put("common_key", "common_value");
        mCommonVersionSettings.put(overriddenCommonKey, "overridden_common_value");
    }

    private final JSONObject mVersionSettingsJson = new JSONObject(mVersionSettings);
    private final JSONObject mCommonVersionSettingsJson = new JSONObject(mCommonVersionSettings);

    private final JSONObject mAllVersionSettings = new JSONObject().put(VERSION, mVersionSettingsJson);

    private final JSONObject mDeviceConfiguration = new JSONObject()
            .put("versions", mAllVersionSettings).put("common", mCommonVersionSettingsJson);

    private final JSONObject mDeviceSettingsJson = new JSONObject()
            .put("runner", mRunnerConfiguration).put("device", mDeviceConfiguration);

    private final IOReader mReader = mock(IOReader.class);
    private DeviceSettings<String> mDeviceSettings;

    @Before
    public void setUp() {
        when(mReader.readAsString(DEVICE_SETTINGS_FILE)).thenReturn(mDeviceSettingsJson.toString());

        mDeviceSettings = new DeviceSettings<>(DEVICE_SETTINGS_FILE, mReader, Function.identity());
    }

    @Test
    public void testGetCapabilities() {
        final Map<String, String> actualCapabilities = mDeviceSettings.getCapabilities();

        assertThat(actualCapabilities).isEqualTo(mCapabilities);
    }

    @Test
    public void testGetVersions() {
        final Set<String> actualVersions = mDeviceSettings.getVersions();

        assertThat(actualVersions).containsExactly(VERSION);
    }

    @Test
    public void testGetVersionSettings() {
        final JSONObject actualVersionSettings = mDeviceSettings.getVersionSettings(VERSION);

        final Map<String, String> expectedVersionSettings = new HashMap<>(mCommonVersionSettings);
        expectedVersionSettings.putAll(mVersionSettings);

        assertThat(actualVersionSettings)
                .usingComparator((json1, json2) -> json1.similar(json2) ? 0 : -1)
                .isEqualTo(new JSONObject(expectedVersionSettings));
    }

    @Test
    public void testGetCommonSetting() {
        final JSONObject actualValue = mDeviceSettings.getCommonSettings();
        assertThat(actualValue)
                .usingComparator((json1, json2) -> json1.similar(json2) ? 0 : -1)
                .isEqualTo(mCommonVersionSettingsJson);
    }

    @Test
    public void testGetUnknownVersionSettings() {
        final JSONObject actualVersionSettings = mDeviceSettings.getVersionSettings(UNKNOWN_VERSION);

        assertThat(actualVersionSettings).isNull();
    }
}
