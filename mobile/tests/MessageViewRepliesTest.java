package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.pages.ComposePage;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.ComposeSteps;
import com.yandex.mail.steps.FolderSteps;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.steps.MessageViewSteps;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.MessageViewRepliesFakeData;
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
import static com.yandex.mail.DefaultSteps.fillInTextField;
import static com.yandex.mail.TestUtil.randomText;
import static com.yandex.mail.pages.ComposePage.sendButton;
import static com.yandex.mail.pages.ComposePage.textBody;
import static com.yandex.mail.pages.MessageViewPage.ADDRESS_FORWARD_TO;
import static com.yandex.mail.pages.MessageViewPage.NAME_ATTACH_FROM_FORWARDING_MESSAGE;
import static com.yandex.mail.pages.MessageViewPage.PREFIX_FWD_SUBJECT;
import static com.yandex.mail.pages.MessageViewPage.PREFIX_RE_SUBJECT;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.SENT_FOLDER;
import static com.yandex.mail.util.OperationsConst.ForMessageView.MESSAGE_FORWARD_WITH_ATTACH;
import static com.yandex.mail.util.OperationsConst.ForMessageView.MESSAGE_REPLY_FORWARD;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.FORWARD_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.REPLY_ALL_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.REPLY_TO_MENU;
import static java.lang.String.format;

@RunWith(AndroidJUnit4.class)
public class MessageViewRepliesTest {

    @NonNull
    private static MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private static MessageViewSteps onMessageView = new MessageViewSteps();

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @NonNull
    private ComposeSteps onCompose = new ComposeSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new MessageViewRepliesFakeData(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @BusinessLogic
    public void shouldReplyToSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(MESSAGE_REPLY_FORWARD)
                .openMessage(MESSAGE_REPLY_FORWARD);
        onMessageView.makeOperationFromMenuForExpandedMessage(REPLY_TO_MENU);

        fillInTextField(textBody(), randomText(20));
        clickOn(sendButton());
        onFolderList.goBackToFolderList();

        onMessageList.shouldSeeMessageInFolder(format(PREFIX_RE_SUBJECT, MESSAGE_REPLY_FORWARD), SENT_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldReplyAllSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(MESSAGE_REPLY_FORWARD)
                .openMessage(MESSAGE_REPLY_FORWARD);
        onMessageView.makeOperationFromMenuForExpandedMessage(REPLY_ALL_MENU);

        fillInTextField(textBody(), randomText(20));
        clickOn(sendButton());
        onFolderList.goBackToFolderList();

        onMessageList.shouldSeeMessageInFolder(format(PREFIX_RE_SUBJECT, MESSAGE_REPLY_FORWARD), SENT_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldForwardSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(MESSAGE_REPLY_FORWARD)
                .openMessage(MESSAGE_REPLY_FORWARD);
        onMessageView.makeOperationFromMenuForExpandedMessage(FORWARD_MENU);

        fillInTextField(ComposePage.toField(), ADDRESS_FORWARD_TO);
        fillInTextField(textBody(), randomText(20));

        clickOn(sendButton());
        onFolderList.goBackToFolderList();

        onMessageList.shouldSeeMessageInFolder(format(PREFIX_FWD_SUBJECT, MESSAGE_REPLY_FORWARD), SENT_FOLDER);
    }

    @Test
    @BusinessLogic
    // TODO: https://github.yandex-team.ru/mobmail/mobile-yandex-mail-client-android/issues/2455
    public void shouldForwardWithAttachSingleMessage() {
        onMessageList.shouldSeeMessageInCurrentFolder(MESSAGE_FORWARD_WITH_ATTACH)
                .openMessage(MESSAGE_FORWARD_WITH_ATTACH);
        onMessageView.makeOperationFromMenuForExpandedMessage(FORWARD_MENU);

        fillInTextField(ComposePage.toField(), ADDRESS_FORWARD_TO);
        fillInTextField(textBody(), randomText(20));
        onCompose.shouldSeeAttachedFile(NAME_ATTACH_FROM_FORWARDING_MESSAGE);

        clickOn(sendButton());
        onFolderList.goBackToFolderList();

        onMessageList.shouldSeeMessageInFolder(format(PREFIX_FWD_SUBJECT, MESSAGE_FORWARD_WITH_ATTACH), SENT_FOLDER)
                .openMessage(format(PREFIX_FWD_SUBJECT, MESSAGE_FORWARD_WITH_ATTACH));
        onMessageView.shouldSeeDiskAndFileAttaches(NAME_ATTACH_FROM_FORWARDING_MESSAGE);
    }
}
