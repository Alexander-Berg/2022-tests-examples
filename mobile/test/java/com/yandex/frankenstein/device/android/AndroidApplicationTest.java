package com.yandex.frankenstein.device.android;

import com.yandex.frankenstein.device.android.helper.AaptHelper;
import com.yandex.frankenstein.device.android.helper.application.AndroidApplication;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class AndroidApplicationTest {

    @Mock private AaptHelper mHelper;

    private static final String LOCAL_PATH = "/local/path/agent_name.apk";
    private static final String REMOTE_PATH = "/data/data/application/remote_agent_name.apk";
    private static final List<String> INTENT_ARGUMENTS = Arrays.asList("-a", "some_argument");

    private static final String APPLICATION_ID = "application_id";
    private static final String COMPONENT_NAME = "component_name";
    private static final String MOCKED_APPLICATION_ID = "mocked_application_id";
    private static final String MOCKED_COMPONENT_NAME = "mocked_component_name";

    private AndroidApplication mApplication;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mHelper.getApplicationId(LOCAL_PATH)).thenReturn(MOCKED_APPLICATION_ID);
        when(mHelper.getComponentName(LOCAL_PATH)).thenReturn(MOCKED_COMPONENT_NAME);

        mApplication = new AndroidApplication(
                LOCAL_PATH, REMOTE_PATH, APPLICATION_ID, COMPONENT_NAME, mHelper, INTENT_ARGUMENTS);
    }

    @Test
    public void testGetIntentArguments() {
        assertThat(mApplication.getExtraIntentArguments()).containsExactlyElementsOf(INTENT_ARGUMENTS);
    }

    @Test
    public void testGetApplicationId() {
        assertThat(mApplication.getApplicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    public void testGetApplicationIdIfNull() {
        final AndroidApplication application = new AndroidApplication(
                LOCAL_PATH, REMOTE_PATH, null, COMPONENT_NAME, mHelper, INTENT_ARGUMENTS);
        assertThat(application.getApplicationId()).isEqualTo(MOCKED_APPLICATION_ID);
    }

    @Test
    public void testGetComponentName() {
        assertThat(mApplication.getComponentName()).isEqualTo(COMPONENT_NAME);
    }

    @Test
    public void testGetComponentNameIfNull() {
        final AndroidApplication application = new AndroidApplication(
                LOCAL_PATH, REMOTE_PATH, APPLICATION_ID, null, mHelper, INTENT_ARGUMENTS);
        assertThat(application.getComponentName()).isEqualTo(MOCKED_COMPONENT_NAME);
    }

    @Test
    public void testGetRemotePath() {
        assertThat(mApplication.getRemotePath()).isEqualTo(REMOTE_PATH);
    }

    @Test
    public void testGetLocalPath() {
        assertThat(mApplication.getLocalPath()).isEqualTo(LOCAL_PATH);
    }
}
