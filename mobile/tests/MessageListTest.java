package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.suites.Acceptance;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.MessageListFakeDataRule;
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

import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.DefaultSteps.shouldSeeText;
import static com.yandex.mail.pages.MessageListPage.GroupModeActionMenu.deselectAll;
import static com.yandex.mail.pages.MessageListPage.GroupModeActionMenu.selectAll;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.composeButton;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.folderIcon;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.searchButton;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.unreadCounter;
import static com.yandex.mail.pages.MessageListPage.attachPreview;
import static com.yandex.mail.pages.MessageListPage.iconUserLabel;
import static com.yandex.mail.pages.MessageListPage.messageFirstlineWithText;
import static com.yandex.mail.pages.MessageListPage.messageSender;
import static com.yandex.mail.pages.MessageListPage.messageSubjectWithText;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.CHECK_META_MESSAGE_FIRSTLINE;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.CHECK_META_MESSAGE_SUBJECT;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.MESSAGE_IN_INBOX1;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.MESSAGE_IN_INBOX2;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.THREAD_IN_INBOX_MESSAGES_IN_DIFF_FOLDERS_SUBJ;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.INBOX_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_NAME;

@RunWith(AndroidJUnit4.class)
public class MessageListTest {

    private static final String UNREAD_COUNTER = "3";

    private static final int NUM_OF_SELECTED_MESSAGES = 5;

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new MessageListFakeDataRule(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @Acceptance
    public void shouldSeeAllElementsOnTopBarMessageList() {
        shouldSee(
                folderIcon(INBOX_FOLDER),
                composeButton(),
                searchButton()
        );
        shouldSeeText(unreadCounter(), UNREAD_COUNTER);
    }

    @Test
    @Acceptance
    public void shouldSeeAllMetaElements() {
        shouldSee(
                iconUserLabel(CHECK_META_MESSAGE_SUBJECT, LABEL_NAME),
                messageSender(CHECK_META_MESSAGE_SUBJECT),
                attachPreview(CHECK_META_MESSAGE_SUBJECT)
        );
        shouldSee(
                messageSubjectWithText(CHECK_META_MESSAGE_SUBJECT),
                messageFirstlineWithText(CHECK_META_MESSAGE_FIRSTLINE)
        );
    }

    @Test
    @Acceptance
    public void seeGroupModeIsOn() {
        onMessageList.selectMessage(MESSAGE_IN_INBOX1)
                .selectMessage(MESSAGE_IN_INBOX2)
                .selectMessage(THREAD_IN_INBOX_MESSAGES_IN_DIFF_FOLDERS_SUBJ)
                .seeMessagesAreSelected(MESSAGE_IN_INBOX1, MESSAGE_IN_INBOX2)
                .seeGroupModeIsOnWithSelectedMessages(NUM_OF_SELECTED_MESSAGES);
    }

    @Test
    @Acceptance
    public void seeGroupModeIsOff() {
        onMessageList.selectMessage(MESSAGE_IN_INBOX1)
                .selectMessage(MESSAGE_IN_INBOX2)
                .deselectMessage(MESSAGE_IN_INBOX1)
                .seeMessagesAreNotSelected(MESSAGE_IN_INBOX1)
                .seeGroupModeIsOnWithSelectedMessages(1)
                .deselectMessage(MESSAGE_IN_INBOX2)
                .seeMessagesAreNotSelected(MESSAGE_IN_INBOX2)
                .shouldNotSeeGroupMode();
    }

    @Test
    @BusinessLogic
    public void shouldSelectAll() {
        onMessageList
                .waitForMessageList()
                .selectMessage(MESSAGE_IN_INBOX1)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(selectAll())
                .shouldSeeAllMessagesSelected();
    }

    @Test
    @BusinessLogic
    public void shouldDeselectAllAfterSingleSelection() {
        onMessageList
                .waitForMessageList()
                .selectMessage(MESSAGE_IN_INBOX1)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(deselectAll())
                .shouldNotSeeSelectedMessages();
    }

    @Test
    @BusinessLogic
    public void shouldDeselectAllAfterSelectAll() {
        onMessageList
                .waitForMessageList()
                .selectMessage(MESSAGE_IN_INBOX1)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(selectAll())
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(deselectAll())
                .shouldNotSeeSelectedMessages();
    }
}
