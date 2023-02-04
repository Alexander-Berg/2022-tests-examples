package ru.yandex.vertis.releaseissuenotifier;

import org.junit.Test;
import ru.yandex.vertis.releaseissuenotifier.bean.PushBean;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.vertis.releaseissuenotifier.message.StatusMessagePathBuilder.messagePath;

/**
 * @author kurau (Yuri Kalinin)
 */
public class PathWrongStatusTest {

    private static final String FAKE_TEMPLATE = "messages/autoru/_not_found_not_found.md";
    private PushBean pushBean = new PushBean();

    @Test
    public void shouldSkipWrongStatus() {
        pushBean.setStatus("wrong");
        String template = messagePath().use(pushBean).generatePathToTemplate();

        assertThat("Should parse status", template, is(FAKE_TEMPLATE));
    }

    @Test
    public void shouldSkipEmptyStatus() {
        pushBean.setStatus("");
        String template = messagePath().use(pushBean).generatePathToTemplate();

        assertThat("Should parse status", template, is(FAKE_TEMPLATE));
    }

    @Test
    public void shouldSkipNullStatus() {
        pushBean.setStatus(null);
        String template = messagePath().use(pushBean).generatePathToTemplate();

        assertThat("Should parse status", template, is(FAKE_TEMPLATE));
    }

}
