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
public class ComponentTest {

    private static final String ANDROID = "android";
    private static final String IOS = "ios";
    private static final String PREFIX = "#";

    private PushBean pushBean = new PushBean();

    @Before
    public void setUp() {
        pushBean.setPreviousStatus("Открыт")
                .setStatus("Smoke")
                .setResolution("")
                .setFixVersions("not empty");
    }

    @Test
    public void shouldNotCompileMessageWithEmptyComponent() {
        pushBean.setComponents("");
        String message = appsRelease(pushBean).message();
        assertThat("Should not compile message with empty fix versions", message, isEmptyString());
    }

    @Test
    public void shouldNotCompileMessageWithNullComponent() {
        pushBean.setComponents(null);
        String message = appsRelease(pushBean).message();
        assertThat("Should not compile message with null fix versions", message, isEmptyString());
    }

    @Test
    public void shouldNotCompileWithWrongComponent() {
        pushBean.setComponents("not empty");
        String message = appsRelease(pushBean).message();
        assertThat("Should not compile message with null fix versions", message, isEmptyString());
    }

    @Test
    public void shouldCompileMessageWithAndroid() {
        pushBean.setComponents(ANDROID);
        String message = appsRelease(pushBean).message();
        assertThat("Should compile message with android", message, both(not(isEmptyString())).and(startsWith(PREFIX)));
    }

    @Test
    public void shouldCompileMessageWithIOS() {
        pushBean.setComponents(IOS);
        String message = appsRelease(pushBean).message();
        assertThat("Should compile message with ios", message, both(not(isEmptyString())).and(startsWith(PREFIX)));
    }
}
