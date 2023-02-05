package com.yandex.frankenstein.device.android;

import com.yandex.frankenstein.device.android.helper.application.AndroidApplication;
import com.yandex.frankenstein.device.android.helper.application.AndroidApplicationProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class AndroidApplicationProviderTest {

    private static final String BUILDS_DIR = "/dir/project/build";
    private static final String SUPPLEMENTARY_BUILDS_ROOT_DIR = "/dir/project/native/build";
    private static final String AGENT_APK_LOCAL_PATH = "agent_test_application.apk";
    private static final String APK_LOCAL_PATH = "test_application.apk";
    private static final String APPLICATION_ID = "fake_application_id";
    private static final String COMPONENT_NAME = "fake_component_name";
    private static final List<String> AGENT_INTENT_ARGUMENTS = Arrays.asList("agent", "arguments");
    private static final List<String> INTENT_ARGUMENTS = Arrays.asList("some", "arguments");

    private final Map<String, String> mCapabilities = new HashMap<>();
    {
        mCapabilities.put("builds_dir", BUILDS_DIR);
        mCapabilities.put("supplementary_builds_root_dir", SUPPLEMENTARY_BUILDS_ROOT_DIR);
        mCapabilities.put("application_id", APPLICATION_ID);
        mCapabilities.put("component_name", COMPONENT_NAME);
    }

    private AndroidApplicationProvider mApplicationProvider;

    @Rule public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Before
    public void setUp() {
        mApplicationProvider = new AndroidApplicationProvider(
                mCapabilities, AGENT_APK_LOCAL_PATH, AGENT_INTENT_ARGUMENTS);
    }

    @Test
    public void testGetSupplementaryApplication() {
        final AndroidApplication application = mApplicationProvider.getSupplementaryApplication(
                APK_LOCAL_PATH, INTENT_ARGUMENTS);

        assertApplication(application, APK_LOCAL_PATH, SUPPLEMENTARY_BUILDS_ROOT_DIR, INTENT_ARGUMENTS);
    }

    @Test
    public void testGetSupplementaryApplicationWithoutArguments() {
        final AndroidApplication application = mApplicationProvider.getSupplementaryApplication(APK_LOCAL_PATH);

        assertApplication(application, APK_LOCAL_PATH, SUPPLEMENTARY_BUILDS_ROOT_DIR, Collections.emptyList());
    }

    @Test
    public void testGetSupplementaryApplicationIfNotAbsent() {
        final AndroidApplication createdApplication = mApplicationProvider.getSupplementaryApplication(APK_LOCAL_PATH);
        final AndroidApplication presentApplication = mApplicationProvider.getSupplementaryApplication(APK_LOCAL_PATH);

        assertThat(presentApplication).isSameAs(createdApplication);
    }

    @Test
    public void testGetAgent() {
        final AndroidApplication application = mApplicationProvider.getAgent();

        assertApplication(application, AGENT_APK_LOCAL_PATH, BUILDS_DIR, AGENT_INTENT_ARGUMENTS);
    }

    private void assertApplication(final AndroidApplication application,
                                   final String localPath,
                                   final String apkSubdirectory,
                                   final List<String> intentArguments) {
        softly.assertThat(application.getLocalPath()).isEqualTo(String.format("%s/%s", apkSubdirectory, localPath));
        softly.assertThat(application.getExtraIntentArguments()).containsExactlyElementsOf(intentArguments);
    }
}
