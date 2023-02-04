package ru.yandex.vertis.releaseissuenotifier;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.vertis.releaseissuenotifier.bean.TopLevelBean;
import ru.yandex.vertis.releaseissuenotifier.message.MessageTopLevelNewIdea;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static ru.yandex.vertis.releaseissuenotifier.matcher.FileExistMatcher.fileExist;
import static ru.yandex.vertis.releaseissuenotifier.message.MessageTopLevelNewIdea.newIdea;

public class TopLevelMessageNewIdeaPathTest {

    private static final String PREFIX = "#";

    private TopLevelBean topLevelBean;

    @Before
    public void setUp() {
        topLevelBean = new TopLevelBean().setPreviousStatus("oldStatus").setStatus("newStatus");
    }

    @Test
    public void shouldCompileNewIdeaPath() {
        assertThat("Should see file", MessageTopLevelNewIdea.topLevelNewIdeaPath(topLevelBean),
                is("messages/toplevel/new_idea.md"));
    }

    @Test
    public void shouldSeeFile() {
        assertThat("Should see file", "messages/toplevel/new_idea.md", fileExist());
    }

    @Test
    public void shouldCompileMessageWithTag() {
        topLevelBean.setIssue("777").setDescription("asdfsdfsfd");
        String message = newIdea(topLevelBean).message();
        System.out.println(message);
        assertThat("Should compile message", message, both(not(isEmptyString())).and(startsWith(PREFIX)));
    }

    @Test
    public void shouldCompileMessageCancelWithoutTag() {
        topLevelBean.setIssue("777").setDescription("asdfsdfsfd");
        String message = newIdea(topLevelBean).message();
        System.out.println(message);
        assertThat("Should compile message", message, both(not(isEmptyString())).and(startsWith(PREFIX)));
    }
}
