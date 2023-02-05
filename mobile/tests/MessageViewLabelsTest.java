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
import com.yandex.mail.tests.data.MessageViewLabelsFakeDataRule;
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
import static com.yandex.mail.DefaultSteps.shouldNotSee;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.importantLabel;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.userLabel;
import static com.yandex.mail.pages.TopBarBlock.upButton;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.IMPORTANT_LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_UNREAD;
import static com.yandex.mail.util.OperationsConst.ForMessageView.FAKE_TEST_MESSAGE_IN_INBOX;
import static com.yandex.mail.util.OperationsConst.ForMessageView.FAKE_TEST_MESSAGE_IN_INBOX_IMPORTANT;
import static com.yandex.mail.util.OperationsConst.ForMessageView.FAKE_TEST_MESSAGE_IN_INBOX_USER_LABEL;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MAKE_IMPORTANT;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MAKE_UNIMPORTANT;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MAKE_UNREAD;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MARK_AS;
import static java.util.concurrent.TimeUnit.SECONDS;

@RunWith(AndroidJUnit4.class)
public class MessageViewLabelsTest {

    @NonNull
    private static MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private static MessageViewSteps onMessageView = new MessageViewSteps();

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @NonNull
    public ItemList folderLabelList = new ItemList();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new MessageViewLabelsFakeDataRule(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @BusinessLogic
    public void shouldMakeImportantSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(MAKE_IMPORTANT);

        onMessageView.expandMessageArrow();
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, importantLabel());
        clickOn(upButton());

        onMessageList.shouldSeeImportantLabelOnMessage(FAKE_TEST_MESSAGE_IN_INBOX);

        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_MESSAGE_IN_INBOX, IMPORTANT_LABEL_NAME);
    }

    @Test
    @BusinessLogic
    public void shouldMarkAsUserLabelSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(MARK_AS);
        folderLabelList.chooseLabelWithTextFromList(LABEL_NAME);

        onMessageView.expandMessageArrow();
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, userLabel());

        clickOn(upButton());
        onMessageList.shouldSeeUserLabelOnMessage(FAKE_TEST_MESSAGE_IN_INBOX);
    }

    @Test
    @BusinessLogic
    public void shouldUnmarkAsImportantSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX_IMPORTANT)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX_IMPORTANT);
        onMessageView.makeOperationFromMenuForExpandedMessage(MAKE_UNIMPORTANT);

        clickOn(upButton());
        onMessageList.shouldNotSeeImportantLabelOnMessage(FAKE_TEST_MESSAGE_IN_INBOX_IMPORTANT);

        clickOn(upButton());
        onFolderList.chooseFolderOrLabel(IMPORTANT_LABEL_NAME);
        onMessageList.shouldNotSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX_IMPORTANT);
    }

    @Test
    @BusinessLogic
    public void shouldMakeUnreadSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(MAKE_UNREAD);

        clickOn(upButton());
        onMessageList.shouldSeeUnreadIconOnMessage(FAKE_TEST_MESSAGE_IN_INBOX);

        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(FAKE_TEST_MESSAGE_IN_INBOX, LABEL_UNREAD);
    }

    @Test
    @BusinessLogic
    public void shouldRemoveUserLabelSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX_USER_LABEL)
                .openMessage(FAKE_TEST_MESSAGE_IN_INBOX_USER_LABEL);
        onMessageView.expandMessageArrow();
        onMessageView.makeOperationFromMenuForExpandedMessage(MARK_AS);
        folderLabelList.chooseLabelWithTextFromList(LABEL_NAME);

        shouldNotSee(userLabel(), TIMEOUT_WAIT_FOR_ITEMS, SECONDS);
        clickOn(upButton());
        onMessageList.shouldNotSeeUserLabelOnMessage(FAKE_TEST_MESSAGE_IN_INBOX_USER_LABEL);

        clickOn(upButton());
        onFolderList.chooseFolderOrLabel(LABEL_NAME);
        onMessageList.shouldNotSeeMessageInCurrentFolder(FAKE_TEST_MESSAGE_IN_INBOX_USER_LABEL);
    }
}
