package ru.yandex.vertis.releaseissuenotifier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import ru.yandex.vertis.releaseissuenotifier.bean.PushBean;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.vertis.releaseissuenotifier.message.StatusMessagePathBuilder.messagePath;

/**
 * @author kurau (Yuri Kalinin)
 */
@RunWith(Parameterized.class)
public class PathParsePlatformTest {

    @Parameter
    public String component;

    @Parameter(1)
    public String expectedPlatform;

    @Parameters(name = "{index} - platform {0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"iOS_123.6", "ios"},
                {"Android 6.4", "android"}
        });
    }

    @Test
    public void shouldParsePlatform() {
        PushBean pushBean = new PushBean().setComponents(component);
        String platform = messagePath().use(pushBean).getPlatform();

        assertThat("Should parse ios", platform, is(expectedPlatform));
    }

}
