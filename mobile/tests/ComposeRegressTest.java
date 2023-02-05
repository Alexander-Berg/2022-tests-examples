package com.yandex.mail.tests;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.pages.Account;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeAccountRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.RealAccount;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.FolderSteps;
import com.yandex.mail.steps.MessageListSteps;

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
import static com.yandex.mail.pages.ComposePage.subject;
import static com.yandex.mail.pages.ComposePage.toField;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.composeButton;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.INBOX_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.SENT_FOLDER;
import static com.yandex.mail.util.SetUpFailureHandler.setUpFailureHandler;
import static org.junit.rules.RuleChain.emptyRuleChain;

@RunWith(AndroidJUnit4.class)
public class ComposeRegressTest {

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(
                    new FakeAccountRule(FAKE_USER) {
                        @Override
                        public void initialize(@NonNull AccountWrapper account) {
                        }
                    }
            )
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @RealAccount
    //1016
    public void shouldSendMessageToYourself() {
        String subject = randomText(10);
        clickOn(composeButton());

        fillInTextField(subject(), subject);
        fillInTextField(toField(), USER_LOGIN + "@ya.ru");
        clickOn(sendButton());
        onFolderList.goBackToFolderList();

        onMessageList.shouldSeeMessageInFolder(subject, SENT_FOLDER);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(subject, INBOX_FOLDER)
                .shouldSeeThreadCounterOnMessage(subject);
    }
}
