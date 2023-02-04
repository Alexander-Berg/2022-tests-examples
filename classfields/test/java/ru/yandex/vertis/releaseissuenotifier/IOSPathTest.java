package ru.yandex.vertis.releaseissuenotifier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import ru.yandex.vertis.releaseissuenotifier.bean.PushBean;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static ru.yandex.vertis.releaseissuenotifier.matcher.FileExistMatcher.fileExist;
import static ru.yandex.vertis.releaseissuenotifier.message.MessageAppsReleaseStatus.appsRelease;
import static ru.yandex.vertis.releaseissuenotifier.message.StatusMessagePathBuilder.messagePath;

/**
 * @author kurau (Yuri Kalinin)
 */
@RunWith(Parameterized.class)
public class IOSPathTest {

    private static final String COMPONENTS = ":,7ios 556";
    private static final String PREFIX = "#";

    private PushBean pushBean = new PushBean();

    @Parameter
    public String previousStatus;

    @Parameter(1)
    public String newStatus;

    @Parameter(2)
    public String resolution;

    @Parameter(3)
    public String templateName;

    @Parameterized.Parameters(name = "{index} -> {3}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Открыт",              "Smoke",            "",         "ios_open_smoke.md"},
                {"Открыт",              "Регресс",          "",         "ios_open_regress.md"},
                {"Регресс",             "Готов к выкладке", "",         "ios_regress_ready.md"},
                {"Smoke",               "Готов к выкладке", "",         "ios_smoke_ready.md"},
                {"Готов к выкладке",    "Ждет ревью",       "",         "ios_ready_review.md"},
                {"Ждет ревью",          "Выкладка",         "",         "ios_review_deploy.md"},
                {"Выкладка",            "Закрыт",           "Неполный", "ios_deploy_close_incomplete.md"},
                {"Выкладка",            "Закрыт",           "Решен",    "ios_deploy_close_decided.md"},

                {"Open",                "Smoke",            "",         "ios_open_smoke.md"},
                {"Open",                "Regress",          "",         "ios_open_regress.md"},
                {"Regress",             "Ready for Release","",         "ios_regress_ready.md"},
                {"Smoke",               "Ready for Release","",         "ios_smoke_ready.md"},
                {"Ready for Release",   "Awaiting Review",  "",         "ios_ready_review.md"},
                {"Awaiting Review",     "Releasing",        "",         "ios_review_deploy.md"},
                {"Releasing",           "Closed",           "Incomplete","ios_deploy_close_incomplete.md"},
                {"Releasing",           "Closed",           "Fixed",    "ios_deploy_close_decided.md"}
        });
    }

    @Before
    public void setUp() {
        pushBean.setPreviousStatus(previousStatus)
                .setStatus(newStatus)
                .setResolution(resolution)
                .setComponents(COMPONENTS);

    }

    @Test
    public void shouldGeneratePath() {
        String path = messagePath().use(pushBean).generatePathToTemplate();
        assertThat("Should generate path", path, is(path));
    }

    @Test
    public void shouldSeeFile() {
        String path = messagePath().use(pushBean).generatePathToTemplate();
        assertThat("Should see file", path, fileExist());
    }

    @Test
    public void shouldCompileMessage() {
        pushBean.setFixVersions("not empty string");
        String message =  appsRelease(pushBean).message();
        assertThat("Should compile message", message, both(not(isEmptyString())).and(startsWith(PREFIX)));
    }

}
