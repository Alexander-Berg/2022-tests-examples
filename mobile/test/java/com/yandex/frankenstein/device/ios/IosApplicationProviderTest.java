package com.yandex.frankenstein.device.ios;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class IosApplicationProviderTest {

    private static final String BUILDS_DIR = "/dir/project/build";
    private static final String AGENT_APK_LOCAL_PATH = "agent_test_application.apk";
    private static final String APK_LOCAL_PATH = "test_application.apk";
    private static final String APPLICATION_ID = "fake_application_id";
    private static final String COMPONENT_NAME = "fake_component_name";
    private static final List<String> AGENT_ARGUMENTS = Arrays.asList("agent", "arguments");
    private static final List<String> ARGUMENTS = Arrays.asList("some", "arguments");

    private final Map<String, String> mCapabilities = new HashMap<>();
    {
        mCapabilities.put("builds_dir", BUILDS_DIR);
        mCapabilities.put("application_id", APPLICATION_ID);
        mCapabilities.put("component_name", COMPONENT_NAME);
    }

    private IosApplicationProvider mApplicationProvider;

    @Rule public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Before
    public void setUp() {
        mApplicationProvider = new IosApplicationProvider(
                mCapabilities, AGENT_APK_LOCAL_PATH, AGENT_ARGUMENTS);
    }

    @Test
    public void testGetSupplementaryApplication() {
        final IosApplication application = mApplicationProvider.getSupplementaryApplication(
                APK_LOCAL_PATH, ARGUMENTS);

        assertApplication(application, APK_LOCAL_PATH, ARGUMENTS);
    }

    @Test
    public void testGetSupplementaryApplicationWithoutArguments() {
        final IosApplication application = mApplicationProvider.getSupplementaryApplication(APK_LOCAL_PATH);

        assertApplication(application, APK_LOCAL_PATH, Collections.emptyList());
    }

    @Test
    public void testGetSupplementaryApplicationIfNotAbsent() {
        final IosApplication createdApplication =
                mApplicationProvider.getSupplementaryApplication(APK_LOCAL_PATH);
        final IosApplication presentApplication =
                mApplicationProvider.getSupplementaryApplication(APK_LOCAL_PATH);

        assertThat(presentApplication).isSameAs(createdApplication);
    }

    @Test
    public void testGetAgent() {
        final IosApplication application = mApplicationProvider.getAgent();

        assertApplication(application, AGENT_APK_LOCAL_PATH, AGENT_ARGUMENTS);
    }

    private void assertApplication(final IosApplication application,
                                   final String localPath,
                                   final List<String> extraArguments) {
        softly.assertThat(application.getLocalPath()).isEqualTo(String.format("%s/%s", BUILDS_DIR, localPath));
        softly.assertThat(application.getExtraArguments()).containsExactlyElementsOf(extraArguments);
    }
}
