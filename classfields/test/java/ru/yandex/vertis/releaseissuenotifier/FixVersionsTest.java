package ru.yandex.vertis.releaseissuenotifier;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.vertis.releaseissuenotifier.bean.PushBean;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static ru.yandex.vertis.releaseissuenotifier.message.MessageAppsReleaseStatus.appsRelease;

/**
 * @author kurau (Yuri Kalinin)
 */
public class FixVersionsTest {

    private static final String EXISTING_COMPONENT = "android";
    private PushBean pushBean = new PushBean();

    @Before
    public void setUp() {
        pushBean.setPreviousStatus("Открыт")
                .setStatus("Smoke")
                .setResolution("")
                .setComponents(EXISTING_COMPONENT);
    }

    @Test
    public void shouldNotCompileMessageWithEmptyFixVersions() {
        pushBean.setFixVersions("");
        String message = appsRelease(pushBean).message();
        assertThat("Should not compile message with empty fix versions", message, isEmptyString());
    }

    @Test
    public void shouldNotCompileMessageWithNullFixVersions() {
        pushBean.setFixVersions(null);
        String message = appsRelease(pushBean).message();
        assertThat("Should not compile message with null fix versions", message, isEmptyString());
    }

    @Test
    public void shouldMessageWithFixVersions() {
        pushBean.setFixVersions("not empty");
        String message = appsRelease(pushBean).message();
        assertThat("Should not compile message with null fix versions", message,
                both(not(isEmptyString())).and(startsWith("#")));
    }
}
