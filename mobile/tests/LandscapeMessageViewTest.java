package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.steps.MessageViewSteps;
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
import static com.yandex.mail.DefaultSteps.setUpLandscape;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.pages.MessageViewPage.MESSAGE_DATE_TEXT;
import static com.yandex.mail.pages.MessageViewPage.MESSAGE_SUBJECT_TEXT;
import static com.yandex.mail.pages.MessageViewPage.MESSAGE_VIEW_FIRSTLINE_TEXT;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.messageAttachment;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static java.util.concurrent.TimeUnit.SECONDS;

@RunWith(AndroidJUnit4.class)
public class LandscapeMessageViewTest {

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
    @BusinessLogic
    public void seeAllElementsOfMessageAfterRotation() {
        onMessageList.shouldSeeMessageInCurrentFolder(MESSAGE_SUBJECT_TEXT)
                .openMessage(MESSAGE_SUBJECT_TEXT);

        onMessageView.shouldSeeAllMessageElements(
                MESSAGE_SUBJECT_TEXT,
                MESSAGE_DATE_TEXT,
                MESSAGE_VIEW_FIRSTLINE_TEXT
        );
        setUpLandscape();
        onMessageView.shouldSeeAllMessageElements(
                MESSAGE_SUBJECT_TEXT,
                MESSAGE_DATE_TEXT,
                MESSAGE_VIEW_FIRSTLINE_TEXT
        );
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, messageAttachment());
    }
}
