package ru.yandex.vertis.releaseissuenotifier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import ru.yandex.vertis.releaseissuenotifier.bean.PushBean;
import ru.yandex.vertis.releaseissuenotifier.bean.Status;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.vertis.releaseissuenotifier.message.StatusMessagePathBuilder.messagePath;

/**
 * @author kurau (Yuri Kalinin)
 */
@RunWith(Parameterized.class)
public class PathParseStatusParserTest {

    private PushBean pushBean = new PushBean();

    @Parameter
    public Status status;

    @Parameters(name = "{index} - {0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {Status.OPEN},
                {Status.SMOKE},
                {Status.REGRESS},
                {Status.REVIEW},
                {Status.READY},
                {Status.DEPLOY},
                {Status.CLOSE}
        });
    }

    @Test
    public void shouldParseNewStatus() {
        pushBean.setStatus(status.getName());
        Status newStatus = messagePath().use(pushBean).getNewStatus();

        assertThat("Should parse status", newStatus, is(status));
    }

    @Test
    public void shouldParsePreviousStatus() {
        pushBean.setPreviousStatus(status.getName());
        Status previousStatus = messagePath().use(pushBean).getPreviousStatus();

        assertThat("Should parse status", previousStatus, is(status));
    }
}
