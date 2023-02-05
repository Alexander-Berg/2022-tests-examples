package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.steps.MessageViewSteps;
import com.yandex.mail.suites.Acceptance;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.MessageViewFakeData;
import com.yandex.mail.util.SetUpFailureHandler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static com.yandex.mail.DefaultSteps.TIMEOUT_WAIT_FOR_ITEMS;
import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.shouldNotBeEnabled;
import static com.yandex.mail.DefaultSteps.shouldNotSee;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.DefaultSteps.shouldSeeText;
import static com.yandex.mail.pages.MessageViewPage.DRAFT_COUNTER;
import static com.yandex.mail.pages.MessageViewPage.ExpandedMessageHead.recipientCc;
import static com.yandex.mail.pages.MessageViewPage.MESSAGES_COUNTER;
import static com.yandex.mail.pages.MessageViewPage.MESSAGES_COUNTER_TOTAL;
import static com.yandex.mail.pages.MessageViewPage.MESSAGES_COUNTER_UNREAD;
import static com.yandex.mail.pages.MessageViewPage.MESSAGE_DATE_TEXT;
import static com.yandex.mail.pages.MessageViewPage.MESSAGE_SUBJECT_TEXT;
import static com.yandex.mail.pages.MessageViewPage.MESSAGE_VIEW_FIRSTLINE_TEXT;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.importantLabel;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.importantLabelRemovingIcon;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.messageAttachment;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.messageCounter;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.userLabel;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.userLabelRemovingIcon;
import static com.yandex.mail.pages.MessageViewPage.THREAD_WITH_DRAFTS_SUBJECT;
import static com.yandex.mail.pages.MessageViewPage.ThreadHead.draftSplitter;
import static com.yandex.mail.pages.MessageViewPage.ThreadHead.threadCounterDraft;
import static com.yandex.mail.pages.MessageViewPage.ThreadHead.threadCounterTotal;
import static com.yandex.mail.pages.MessageViewPage.ThreadHead.threadCounterUnread;
import static com.yandex.mail.pages.MessageViewPage.ThreadHead.unreadIcon;
import static com.yandex.mail.pages.MessageViewPage.TopBar.arrowDown;
import static com.yandex.mail.pages.MessageViewPage.TopBar.arrowUp;
import static com.yandex.mail.pages.TopBarBlock.upButton;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.INBOX_FOLDER;
import static com.yandex.mail.util.OperationsConst.ForMessageView.MESSAGE_WITH_LABELS_AND_RECIPIENTS;
import static com.yandex.mail.util.OperationsConst.ForSwitchThreads.FOLDER_FOR_SWITCHING_THREADS;
import static com.yandex.mail.util.OperationsConst.ForSwitchThreads.MESSAGE_FIRST;
import static com.yandex.mail.util.OperationsConst.ForSwitchThreads.MESSAGE_LAST;
import static com.yandex.mail.util.OperationsConst.ForSwitchThreads.MESSAGE_MIDDLE;
import static java.util.concurrent.TimeUnit.SECONDS;

@RunWith(AndroidJUnit4.class)
public class MessageViewTest {

    @NonNull
    private static MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private static MessageViewSteps onMessageView = new MessageViewSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new MessageViewFakeData(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @Acceptance
    public void seeAllElementsOfMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(MESSAGE_SUBJECT_TEXT)
                .openMessage(MESSAGE_SUBJECT_TEXT);

        onMessageView.shouldSeeAllMessageElements(
                MESSAGE_SUBJECT_TEXT,
                MESSAGE_DATE_TEXT,
                MESSAGE_VIEW_FIRSTLINE_TEXT
        );
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, messageAttachment());

        onMessageView.expandMessageArrow()
                .shouldSeeAllExpandedHeadElements(INBOX_FOLDER);
        shouldSee(
                TIMEOUT_WAIT_FOR_ITEMS, SECONDS, userLabel(),
                userLabelRemovingIcon(),
                importantLabel(),
                importantLabelRemovingIcon(),
                recipientCc()
        );
        shouldSeeText(messageCounter(), MESSAGES_COUNTER);
    }

    @Test
    @BusinessLogic
    public void shouldSwitchThreads() {
        clickOn(upButton());

        onMessageList.shouldSeeMessageInFolder(MESSAGE_MIDDLE, FOLDER_FOR_SWITCHING_THREADS)
                .openMessage(MESSAGE_MIDDLE);

        onMessageView.shouldGoDownAndSeeNextThread(MESSAGE_LAST)
                .shouldGoUpAndSeePrevThread(MESSAGE_MIDDLE)
                .shouldGoUpAndSeePrevThread(MESSAGE_FIRST);
    }

    @Test
    @BusinessLogic
    public void shouldNotGoDownFromTheLastThread() {
        clickOn(upButton());

        onMessageList.shouldSeeMessageInFolder(MESSAGE_LAST, FOLDER_FOR_SWITCHING_THREADS)
                .openMessage(MESSAGE_LAST);

        shouldSee(arrowUp());
        shouldNotBeEnabled(arrowDown());
    }

    @Test
    @BusinessLogic
    public void shouldNotGoUpFromTheFirstThread() {
        clickOn(upButton());

        onMessageList.shouldSeeMessageInFolder(MESSAGE_FIRST, FOLDER_FOR_SWITCHING_THREADS)
                .openMessage(MESSAGE_FIRST);

        shouldSee(arrowDown());
        shouldNotBeEnabled(arrowUp());
    }

    @Test
    @BusinessLogic
    public void shouldRemoveUserLabelInExpandedMessageHead() {
        onMessageList.shouldSeeMessageInCurrentFolder(MESSAGE_WITH_LABELS_AND_RECIPIENTS)
                .openMessage(MESSAGE_WITH_LABELS_AND_RECIPIENTS);

        onMessageView.expandMessageArrow();
        clickOn(userLabelRemovingIcon());
        shouldNotSee(userLabel(), TIMEOUT_WAIT_FOR_ITEMS, SECONDS);

        clickOn(upButton());
        onMessageList.shouldNotSeeUserLabelOnMessage(MESSAGE_WITH_LABELS_AND_RECIPIENTS);
    }

    @Test
    @BusinessLogic
    public void shouldRemoveImportantLabelInExpandedMessageHead() {
        onMessageList.shouldSeeMessageInCurrentFolder(MESSAGE_WITH_LABELS_AND_RECIPIENTS)
                .openMessage(MESSAGE_WITH_LABELS_AND_RECIPIENTS);

        onMessageView.expandMessageArrow();
        clickOn(importantLabelRemovingIcon());

        shouldNotSee(importantLabel(), TIMEOUT_WAIT_FOR_ITEMS, SECONDS);

        clickOn(upButton());
        onMessageList.shouldNotSeeImportantLabelOnMessage(MESSAGE_WITH_LABELS_AND_RECIPIENTS);
    }

    @Test
    @BusinessLogic
    public void shouldSeeCounterInThreadWithDraftAndReadUnreadMessages() {
        onMessageList.shouldSeeMessageInCurrentFolder(THREAD_WITH_DRAFTS_SUBJECT)
                .openMessage(THREAD_WITH_DRAFTS_SUBJECT);

        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, threadCounterTotal(MESSAGES_COUNTER_TOTAL),
                  unreadIcon(), threadCounterUnread(MESSAGES_COUNTER_UNREAD),
                  draftSplitter(), threadCounterDraft(DRAFT_COUNTER));
    }
}
