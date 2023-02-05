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
import com.yandex.mail.suites.Acceptance;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.MessageViewActionsFakeDataRule;
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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static com.yandex.mail.DefaultSteps.TIMEOUT_WAIT_FOR_ITEMS;
import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.espresso.ViewActions.waitForExistance;
import static com.yandex.mail.matchers.Matchers.isMenuSortedAs;
import static com.yandex.mail.pages.ItemList.OperationList.operationList;
import static com.yandex.mail.pages.MessageViewPage.ExpandedMessageHead.moreOptions;
import static com.yandex.mail.pages.MessageViewPage.MENU_EXPECTED_ORDER;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.messageSubject;
import static com.yandex.mail.pages.MessageViewPage.TopBar.deleteButton;
import static com.yandex.mail.pages.TopBarBlock.upButton;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.ARCHIVE_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.INBOX_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.SPAM_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.TRASH_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;
import static com.yandex.mail.util.OperationsConst.ForMessageView.FAKE_TEST_MESSAGE_IN_INBOX;
import static com.yandex.mail.util.OperationsConst.ForMessageView.FAKE_TEST_MESSAGE_IN_SPAM;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.ARCHIVE_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.DELETE_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MOVE_TO_FOLDER_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.NOT_SPAM;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.REPLY_ALL_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.SPAM_MENU_EXCLAMATION;
import static java.util.concurrent.TimeUnit.SECONDS;

@RunWith(AndroidJUnit4.class)
public class MessageViewActionsTest {

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private MessageViewSteps onMessageView = new MessageViewSteps();

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @NonNull
    public ItemList folderLabelList = new ItemList();

    @NonNull
    public ItemList menuItemsList = new ItemList();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new MessageViewActionsFakeDataRule(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @BusinessLogic
    public void shouldMoveToSpamSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(SPAM_MENU_EXCLAMATION);

        onMessageList.shouldNotSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_MESSAGE_IN_INBOX, SPAM_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldMoveToArchiveSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(ARCHIVE_MENU);
        clickOn(upButton());

        onMessageList.shouldNotSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_MESSAGE_IN_INBOX, ARCHIVE_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldMoveToFolderSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(MOVE_TO_FOLDER_MENU);
        folderLabelList.chooseFolderWithTextFromList(USERS_FOLDER_MEDUZA);

        onMessageView.shouldFolderBeChanged(USERS_FOLDER_MEDUZA, TIMEOUT_WAIT_FOR_ITEMS, SECONDS);

        clickOn(upButton());
        onMessageList.shouldNotSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX);

        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_MESSAGE_IN_INBOX, USERS_FOLDER_MEDUZA);
    }

    @Test
    @BusinessLogic
    public void shouldMoveToTrashFromMenuSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(DELETE_MENU);

        onMessageList.shouldNotSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX);

        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_MESSAGE_IN_INBOX, TRASH_FOLDER);
    }

    @Test
    @Acceptance
    public void shouldMoveToTrashFromTopBarSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX);
        clickOn(deleteButton());

        onMessageList.shouldNotSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX);

        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_MESSAGE_IN_INBOX, TRASH_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldMoveToInboxFromSpamSingleMessage() {
        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_MESSAGE_IN_SPAM, SPAM_FOLDER)
                .openMessage(FAKE_TEST_MESSAGE_IN_SPAM);
        onMessageView.makeOperationFromMenuForExpandedMessage(NOT_SPAM)
                .shouldFolderBeChanged(INBOX_FOLDER, TIMEOUT_WAIT_FOR_ITEMS, SECONDS);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_MESSAGE_IN_SPAM, INBOX_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldSeeAllActiveOperationsInMenuForInbox() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX);

        clickOn(moreOptions());
        onView(isRoot()).perform(waitForExistance(operationList(REPLY_ALL_MENU), TIMEOUT_WAIT_FOR_ITEMS, SECONDS));
        onView(menuItemsList.list()).check(matches(isMenuSortedAs(MENU_EXPECTED_ORDER)));

        clickOn(menuItemsList.cancelButton());
        shouldSee(messageSubject());
    }
}
