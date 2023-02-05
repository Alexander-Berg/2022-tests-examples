package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.pages.ItemList;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.steps.MessageViewSteps;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.MessageViewToastsFakeDataRule;

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
import static com.yandex.mail.DefaultSteps.shouldSeeSnackBarWithText;
import static com.yandex.mail.pages.MessageViewPage.Toasts.snackBarMoveToSpam;
import static com.yandex.mail.pages.MessageViewPage.Toasts.snackBarMoveToTrash;
import static com.yandex.mail.pages.MessageViewPage.Toasts.toastMarkAsNotSpam;
import static com.yandex.mail.pages.MessageViewPage.Toasts.toastMoveToFolder;
import static com.yandex.mail.pages.TopBarBlock.upButton;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.SPAM_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;
import static com.yandex.mail.util.OperationsConst.ForMessageView.FAKE_TEST_MESSAGE_IN_INBOX;
import static com.yandex.mail.util.OperationsConst.ForMessageView.FAKE_TEST_MESSAGE_IN_SPAM;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.DELETE_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MOVE_TO_FOLDER_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.NOT_SPAM;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.SPAM_MENU_EXCLAMATION;
import static com.yandex.mail.util.SetUpFailureHandler.setUpFailureHandler;

@RunWith(AndroidJUnit4.class)
public class MessageViewSnackBarsTest {

    @NonNull
    private static MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private static MessageViewSteps onMessageView = new MessageViewSteps();

    @NonNull
    public ItemList folderLabelList = new ItemList();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new MessageViewToastsFakeDataRule(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @BusinessLogic
    public void shouldSeeSnackBarWhenMoveToTrashFromMenuSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(DELETE_MENU);

        shouldSeeSnackBarWithText(snackBarMoveToTrash());
    }

    @Test
    @BusinessLogic
    public void shouldSeeSnackBarWhenMoveToInboxFromSpamSingleMessage() {
        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_MESSAGE_IN_SPAM, SPAM_FOLDER)
                .openMessage(FAKE_TEST_MESSAGE_IN_SPAM);
        onMessageView.makeOperationFromMenuForExpandedMessage(NOT_SPAM);

        shouldSeeSnackBarWithText(toastMarkAsNotSpam());
    }

    @Test
    @BusinessLogic
    public void shouldSeeSnackBarWhenMoveToFolderSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(MOVE_TO_FOLDER_MENU);
        folderLabelList.chooseFolderWithTextFromList(USERS_FOLDER_MEDUZA);

        shouldSeeSnackBarWithText(toastMoveToFolder());
    }

    @Test
    @BusinessLogic
    public void shouldSeeSnackBarWhenMoveToSpamSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(SPAM_MENU_EXCLAMATION);

        shouldSeeSnackBarWithText(snackBarMoveToSpam());
    }
}
