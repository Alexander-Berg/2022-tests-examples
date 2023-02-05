package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.FolderSteps;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.suites.Acceptance;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.SwipesFakeDataRule;
import com.yandex.mail.util.AccountsConst;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static com.yandex.mail.DefaultSteps.TIMEOUT_WAIT_FOR_ITEMS;
import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.fillInTextField;
import static com.yandex.mail.DefaultSteps.shouldNotSeeItemsOnData;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.TestUtil.randomText;
import static com.yandex.mail.matchers.Matchers.withOperationTitle;
import static com.yandex.mail.pages.ComposePage.sendButton;
import static com.yandex.mail.pages.ComposePage.textBody;
import static com.yandex.mail.pages.ComposePage.toField;
import static com.yandex.mail.pages.MessageViewPage.ADDRESS_FORWARD_TO;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.messageAttachment;
import static com.yandex.mail.pages.MessageViewPage.PREFIX_FWD_SUBJECT;
import static com.yandex.mail.tests.data.SwipesFakeDataRule.MESSAGE_FOR_FORWARD_WITH_ATTACH;
import static com.yandex.mail.tests.data.SwipesFakeDataRule.MESSAGE_FOR_SWIPE2;
import static com.yandex.mail.tests.data.SwipesFakeDataRule.SWIPE_ACTION_FOR_THREAD;
import static com.yandex.mail.tests.data.SwipesFakeDataRule.USER_FOLDER_FOR_SWIPE;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.FoldersAndLabelsConst.SENT_FOLDER;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.FORWARD_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.REPLY_ALL_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.REPLY_TO_MENU;
import static com.yandex.mail.util.SetUpFailureHandler.setUpFailureHandler;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

@RunWith(AndroidJUnit4.class)
public class SwipesRepliesTest {

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new SwipesFakeDataRule(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(AccountsConst.USER_LOGIN, AccountsConst.USER_PASSWORD)))
            .around(
                    new ExternalResource() {
                        @Override
                        protected void before() throws Throwable {
                            onFolderList.goBackToFolderList();
                        }
                    }
            );

    @Before
    public void setUp() throws Exception {
        setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Ignore("TODO: https://github.yandex-team.ru/mobmail/mobile-yandex-mail-client-android/issues/2455")
    @Test
    @BusinessLogic
    public void shouldForwardMessageWithAttachFromSwipeMenu() {
        String fwdSubject = format(PREFIX_FWD_SUBJECT, MESSAGE_FOR_FORWARD_WITH_ATTACH);

        onMessageList.shouldSeeMessageInFolder(MESSAGE_FOR_FORWARD_WITH_ATTACH, USER_FOLDER_FOR_SWIPE)
                .shortSwipeMessageAndSelect(FORWARD_MENU, MESSAGE_FOR_FORWARD_WITH_ATTACH);

        fillInTextField(toField(), ADDRESS_FORWARD_TO);
        fillInTextField(textBody(), randomText(20));
        clickOn(sendButton());

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(fwdSubject, SENT_FOLDER)
                .openMessage(fwdSubject);
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, messageAttachment());
    }

    @Test
    @BusinessLogic
    public void shouldNotSeeRepliesForThreadFromSwipeMenu() {
        onMessageList.shouldSeeMessageInFolder(SWIPE_ACTION_FOR_THREAD, USER_FOLDER_FOR_SWIPE)
                .shortSwipeAndOpenMenu(SWIPE_ACTION_FOR_THREAD);

        shouldNotSeeItemsOnData(
                withOperationTitle(REPLY_TO_MENU),
                withOperationTitle(REPLY_ALL_MENU),
                withOperationTitle(FORWARD_MENU)
        );
    }

    @Test
    @Acceptance
    //TODO: Espresso+WebView bug https://code.google.com/p/android/issues/detail?id=211947
    public void shouldReplyFromSwipeMenu() {
        onMessageList.shouldSeeMessageInFolder(MESSAGE_FOR_SWIPE2, USER_FOLDER_FOR_SWIPE)
                .shortSwipeMessageAndSelect(REPLY_TO_MENU, MESSAGE_FOR_SWIPE2);
        fillInTextField(textBody(), randomText(20));
        clickOn(sendButton());
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(format("Re: %s", MESSAGE_FOR_SWIPE2), SENT_FOLDER);
    }
}
