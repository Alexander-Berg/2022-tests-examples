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
import static org.hamcrest.CoreMatchers.equalTo;
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
public class AndroidPathTest {

    private static final String COMPONENTS = ":,7Android 556";
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
                {"Открыт",              "Smoke",             "",           "android_open_smoke.md"},
                {"Открыт",              "Регресс",           "",           "android_open_regress.md"},
                {"Регресс",             "Готов к выкладке",  "",           "android_regress_ready.md"},
                {"Smoke",               "Готов к выкладке",  "",           "android_smoke_ready.md"},
                {"Выкладка",            "Закрыт",            "Неполный",   "android_deploy_close_incomplete.md"},
                {"Выкладка",            "Закрыт",            "Решен",      "android_deploy_close_decided.md"},

                {"Open",                "Smoke",             "",           "android_open_smoke.md"},
                {"Open",                "Regress",           "",           "android_open_regress.md"},
                {"Regress",             "Ready for Release", "",           "android_regress_ready.md"},
                {"Smoke",               "Ready for Release", "",           "android_smoke_ready.md"},
                {"Releasing",           "Closed",            "Incomplete", "android_deploy_close_incomplete.md"},
                {"Releasing",           "Closed",            "Fixed",      "android_deploy_close_decided.md"}
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
        assertThat("Should see path", path, equalTo(String.format("messages/autoru/%s", templateName)));
    }

    @Test
    public void shouldExistFile() {
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
