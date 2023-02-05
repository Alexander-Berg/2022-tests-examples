package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.pages.ItemList;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.FolderSteps;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.steps.MessageViewSteps;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.MessageViewThreadsFakeDataRule;
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

import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.userLabel;
import static com.yandex.mail.pages.MessageViewPage.TopBar.deleteButton;
import static com.yandex.mail.pages.TopBarBlock.upButton;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.ARCHIVE_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.IMPORTANT_LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.SPAM_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.TRASH_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;
import static com.yandex.mail.util.OperationsConst.ForMessageView.FAKE_TEST_THREAD_IN_INBOX;
import static com.yandex.mail.util.OperationsConst.ForMessageView.MESSAGES_NUMBER_IN_THREAD;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.ARCHIVE_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.DELETE_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MAKE_IMPORTANT;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MARK_AS;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MOVE_TO_FOLDER_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.SPAM_MENU_EXCLAMATION;
import static java.util.concurrent.TimeUnit.SECONDS;

@RunWith(AndroidJUnit4.class)
public class MessageViewThreadsTest {

    private static final int WAIT_ELEMENT_TIMEOUT = 6;

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private MessageViewSteps onMessageView = new MessageViewSteps();

    @NonNull
    private ItemList folderLabelList = new ItemList();

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new MessageViewThreadsFakeDataRule(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }


    @Test
    @BusinessLogic
    public void shouldDeleteFullThread() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX)
                .openMessage(FAKE_TEST_THREAD_IN_INBOX);
        clickOn(deleteButton());

        onMessageList.shouldNotSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX);

        clickOn(upButton());
        onMessageList.shouldSeeNMessagesInFolder(FAKE_TEST_THREAD_IN_INBOX, MESSAGES_NUMBER_IN_THREAD, TRASH_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldMoveToSpamOneMessageFromThread() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX)
                .openMessage(FAKE_TEST_THREAD_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(SPAM_MENU_EXCLAMATION);
        clickOn(upButton());

        onMessageList.shouldNotSeeThreadCounterOnMessage(FAKE_TEST_THREAD_IN_INBOX);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_THREAD_IN_INBOX, SPAM_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldMoveToSpamFullThread() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX)
                .openMessage(FAKE_TEST_THREAD_IN_INBOX);

        onMessageView.makeOperationFromMenuForCollapsedMessageWithIndex(SPAM_MENU_EXCLAMATION, MESSAGES_NUMBER_IN_THREAD);
        onMessageView.makeOperationFromMenuForExpandedMessage(SPAM_MENU_EXCLAMATION);

        onMessageList.shouldNotSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX);

        clickOn(upButton());
        onMessageList.shouldSeeNMessagesInFolder(FAKE_TEST_THREAD_IN_INBOX, MESSAGES_NUMBER_IN_THREAD, SPAM_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldArchiveOneMessageFromThread() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX)
                .openMessage(FAKE_TEST_THREAD_IN_INBOX);

        onMessageView.makeOperationFromMenuForExpandedMessage(ARCHIVE_MENU);
        clickOn(upButton());

        onMessageList.shouldSeeThreadCounterOnMessage(FAKE_TEST_THREAD_IN_INBOX);

        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_THREAD_IN_INBOX, ARCHIVE_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldArchiveFullThread() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX)
                .openMessage(FAKE_TEST_THREAD_IN_INBOX);

        onMessageView.makeOperationFromMenuForCollapsedMessageWithIndex(ARCHIVE_MENU, MESSAGES_NUMBER_IN_THREAD);
        onMessageView.makeOperationFromMenuForExpandedMessage(ARCHIVE_MENU);
        clickOn(upButton());

        onMessageList.shouldNotSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX);

        clickOn(upButton());
        onMessageList.shouldSeeNMessagesInFolder(FAKE_TEST_THREAD_IN_INBOX, MESSAGES_NUMBER_IN_THREAD, ARCHIVE_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldMarkAsImportantOneMessageFromThread() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX)
                .openMessage(FAKE_TEST_THREAD_IN_INBOX);

        onMessageView.makeOperationFromMenuForExpandedMessage(MAKE_IMPORTANT);
        clickOn(upButton());

        onMessageList.shouldSeeImportantLabelOnMessage(FAKE_TEST_THREAD_IN_INBOX);

        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_THREAD_IN_INBOX, IMPORTANT_LABEL_NAME);
    }

    @Test
    @BusinessLogic
    public void shouldMarkWithLabelOneMessageFromThread() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX)
                .openMessage(FAKE_TEST_THREAD_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(MARK_AS);
        folderLabelList.chooseLabelWithTextFromList(LABEL_NAME);

        onMessageView.expandMessageArrow();
        shouldSee(WAIT_ELEMENT_TIMEOUT, SECONDS, userLabel());
        clickOn(upButton());

        onMessageList.shouldSeeUserLabelOnMessage(FAKE_TEST_THREAD_IN_INBOX);

        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_THREAD_IN_INBOX, LABEL_NAME);
    }

    @Test
    @BusinessLogic
    public void shouldMoveToFolderOneMessageFromThread() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX)
                .openMessage(FAKE_TEST_THREAD_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(MOVE_TO_FOLDER_MENU);
        folderLabelList.chooseFolderWithTextFromList(USERS_FOLDER_MEDUZA);
        onMessageView.shouldFolderBeChanged(USERS_FOLDER_MEDUZA, 3, SECONDS);
        clickOn(upButton());

        onMessageList.shouldSeeThreadCounterOnMessage(FAKE_TEST_THREAD_IN_INBOX);

        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_THREAD_IN_INBOX, USERS_FOLDER_MEDUZA);
    }

    @Test
    @BusinessLogic
    public void shouldMoveToFolderFullThread() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX)
                .openMessage(FAKE_TEST_THREAD_IN_INBOX);
        onMessageView.makeOperationFromMenuForCollapsedMessageWithIndex(MOVE_TO_FOLDER_MENU, MESSAGES_NUMBER_IN_THREAD);
        folderLabelList.chooseFolderWithTextFromList(USERS_FOLDER_MEDUZA);
        onMessageView.makeOperationFromMenuForExpandedMessage(MOVE_TO_FOLDER_MENU);
        folderLabelList.chooseFolderWithTextFromList(USERS_FOLDER_MEDUZA);
        clickOn(upButton());

        onMessageList.shouldNotSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX);

        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_THREAD_IN_INBOX, USERS_FOLDER_MEDUZA);
        onMessageList.shouldSeeThreadCounterOnMessage(FAKE_TEST_THREAD_IN_INBOX);
    }

    @Test
    @BusinessLogic
    public void shouldDeleteFullThreadFromMenu() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX)
                .openMessage(FAKE_TEST_THREAD_IN_INBOX);

        onMessageView.makeOperationFromMenuForCollapsedMessageWithIndex(DELETE_MENU, MESSAGES_NUMBER_IN_THREAD);
        onMessageView.makeOperationFromMenuForExpandedMessage(DELETE_MENU);

        onMessageList.shouldNotSeeMessageInCurrentFolder(FAKE_TEST_THREAD_IN_INBOX);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeNMessagesInFolder(FAKE_TEST_THREAD_IN_INBOX, MESSAGES_NUMBER_IN_THREAD, TRASH_FOLDER);
    }
}
