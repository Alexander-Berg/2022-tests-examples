package com.yandex.frankenstein.device.ios;

import com.yandex.frankenstein.device.ios.helper.BundleIdHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class IosApplicationTest {

    @Mock private BundleIdHelper mHelper;

    private static final String LOCAL_PATH = "/local/path/agent_name.apk";
    private static final List<String> ARGUMENTS = Arrays.asList("-a", "some_argument");

    private static final String APPLICATION_ID = "application_id";
    private static final String MOCKED_APPLICATION_ID = "mocked_application_id";

    private IosApplication mApplication;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mHelper.getAppBundleId(LOCAL_PATH)).thenReturn(MOCKED_APPLICATION_ID);

        mApplication = new IosApplication(
                LOCAL_PATH, APPLICATION_ID, mHelper, ARGUMENTS);
    }

    @Test
    public void testGetExtraIntentArguments() {
        assertThat(mApplication.getExtraArguments()).containsExactlyElementsOf(ARGUMENTS);
    }

    @Test
    public void testGetAppBundleId() {
        assertThat(mApplication.getAppBundleId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    public void testGetLocalPath() {
        assertThat(mApplication.getLocalPath()).isEqualTo(LOCAL_PATH);
    }

    @Test
    public void testGetApplicationIdIfNull() {
        final IosApplication application = new IosApplication(
                LOCAL_PATH, null, mHelper, ARGUMENTS);
        assertThat(application.getAppBundleId()).isEqualTo(MOCKED_APPLICATION_ID);
    }
}
