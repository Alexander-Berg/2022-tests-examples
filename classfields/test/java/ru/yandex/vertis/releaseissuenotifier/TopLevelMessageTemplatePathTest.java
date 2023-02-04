package ru.yandex.vertis.releaseissuenotifier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.vertis.releaseissuenotifier.bean.TopLevelBean;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static ru.yandex.vertis.releaseissuenotifier.matcher.FileExistMatcher.fileExist;
import static ru.yandex.vertis.releaseissuenotifier.message.MessageTopLevelStatus.topLevel;
import static ru.yandex.vertis.releaseissuenotifier.message.MessageTopLevelStatus.topLevelTemplatePath;

@RunWith(Parameterized.class)
public class TopLevelMessageTemplatePathTest {

    private static final String PREFIX = "#";

    private TopLevelBean topLevelBean;

    @Parameterized.Parameter
    public String oldStatus;

    @Parameterized.Parameter(1)
    public String newStatus;

    @Parameterized.Parameter(2)
    public String filePath;

    @Parameterized.Parameters(name = "{index} -> {0} -> {1}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Беклог", "Формулируем", "messages/toplevel/backlog_invention.md"},
                {"Формулируем", "В работе", "messages/toplevel/invention_progress.md"},
                {"В работе", "Закрыт", "messages/toplevel/progress_close.md"},

                {"Формулируем", "Беклог", "messages/toplevel/invention_backlog.md"},
                {"В работе", "Беклог", "messages/toplevel/progress_backlog.md"},

                {"Backlog", "Invention Description", "messages/toplevel/backlog_invention.md"},
                {"Invention Description", "In Progress", "messages/toplevel/invention_progress.md"},
                {"In Progress", "Closed", "messages/toplevel/progress_close.md"}
        });
    }

    @Before
    public void setUp() {
        topLevelBean = new TopLevelBean().setPreviousStatus(oldStatus).setStatus(newStatus);
    }

    @Test
    public void shouldCompileFileName() {
        assertThat("", topLevelTemplatePath(topLevelBean), is(filePath));
    }

    @Test
    public void shouldSeeFile() {
        assertThat("Should see file", topLevelTemplatePath(topLevelBean), fileExist());
    }

    @Test
    public void shouldCompileMessage() {
        topLevelBean.setIssue("777").setDescription("name");
        String message =  topLevel(topLevelBean).message();
        System.out.println(message);
        assertThat("Should compile message", message, both(not(isEmptyString())).and(startsWith(PREFIX)));
    }
}
