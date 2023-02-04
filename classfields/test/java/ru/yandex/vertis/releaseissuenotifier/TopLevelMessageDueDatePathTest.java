package ru.yandex.vertis.releaseissuenotifier;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.vertis.releaseissuenotifier.bean.TopLevelBean;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static ru.yandex.vertis.releaseissuenotifier.matcher.FileExistMatcher.fileExist;
import static ru.yandex.vertis.releaseissuenotifier.message.MessageTopLevelDuedate.duedate;
import static ru.yandex.vertis.releaseissuenotifier.message.MessageTopLevelDuedate.topLevelDueDatePath;

public class TopLevelMessageDueDatePathTest {

    private static final String PREFIX = "#";

    private TopLevelBean topLevelBean;

    @Before
    public void setUp() {
        topLevelBean = new TopLevelBean().setPreviousStatus("oldStatus").setStatus("newStatus");
    }

    @Test
    public void shouldCompilePathWithoutDueDate() {
        assertThat("Should see file", topLevelDueDatePath(topLevelBean), is("messages/toplevel/due_date_cancel.md"));
    }

    @Test
    public void shouldCompilePathForDueDate() {
        topLevelBean.setDueDate("123");
        assertThat("Should see file", topLevelDueDatePath(topLevelBean), is("messages/toplevel/due_date.md"));
    }

    @Test
    public void shouldSeeFile() {
        assertThat("Should see file", "messages/toplevel/due_date.md", fileExist());
    }

    @Test
    public void shouldSeeCancelFile() {
        assertThat("Should see file", "messages/toplevel/due_date_cancel.md", fileExist());
    }

    @Test
    public void shouldCompileMessage() {
        topLevelBean.setIssue("777").setDescription("asdfsdfsfd")
                .setDueDate("123");
        String message = duedate(topLevelBean).message();
        System.out.println(message);
        assertThat("Should compile message", message, both(not(isEmptyString())).and(startsWith(PREFIX)));
    }

    @Test
    public void shouldCompileMessageCancel() {
        topLevelBean.setIssue("777").setDescription("asdfsdfsfd");
        String message = duedate(topLevelBean).message();
        System.out.println(message);
        assertThat("Should compile message", message, both(not(isEmptyString())).and(startsWith(PREFIX)));
    }
}
