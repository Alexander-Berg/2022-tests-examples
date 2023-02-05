package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.steps.MessageViewSteps;
import com.yandex.mail.steps.SearchSteps;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.ViewActionsInSearchFakeDataRule;
import com.yandex.mail.util.SetUpFailureHandler;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.setUpLandscape;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.searchButton;
import static com.yandex.mail.pages.MessageViewPage.MESSAGE_DATE_TEXT;
import static com.yandex.mail.tests.data.ViewActionsInSearchFakeDataRule.MESSAGE_WITH_ATTACH_IN_SEARCH_FIRSTLINE;
import static com.yandex.mail.tests.data.ViewActionsInSearchFakeDataRule.MESSAGE_WITH_ATTACH_IN_SEARCH_SUBJ;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;

@RunWith(AndroidJUnit4.class)
public class LandscapeSearchTest {

    @NonNull
    private SearchSteps onSearch = new SearchSteps();

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private MessageViewSteps onMessageView = new MessageViewSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new ViewActionsInSearchFakeDataRule(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @BusinessLogic
    public void shouldLoadMessagesInSearchAfterRotation() {
        clickOn(searchButton());
        onSearch.inputTextAndTapOnSearch("some_query");
        onMessageList.waitForMessageList();
        setUpLandscape();
        onMessageList.waitForMessageList();
    }

    @Ignore("todo: fix this test")
    @Test
    @BusinessLogic
    public void shouldLoadMessageViewInSearchAfterRotation() {
        clickOn(searchButton());
        onSearch.inputTextAndTapOnSearch(MESSAGE_WITH_ATTACH_IN_SEARCH_SUBJ);
        onMessageList.waitForMessageList()
                .openMessage(MESSAGE_WITH_ATTACH_IN_SEARCH_SUBJ);
        onMessageView.shouldSeeAllMessageElements(
                MESSAGE_WITH_ATTACH_IN_SEARCH_SUBJ,
                MESSAGE_DATE_TEXT,
                MESSAGE_WITH_ATTACH_IN_SEARCH_FIRSTLINE
        );

        setUpLandscape();

        onMessageView.shouldSeeAllMessageElements(
                MESSAGE_WITH_ATTACH_IN_SEARCH_SUBJ,
                MESSAGE_DATE_TEXT,
                MESSAGE_WITH_ATTACH_IN_SEARCH_FIRSTLINE
        );
//        shouldSee(TIMEOUT_WAIT_FOR_WEB_ITEMS, SECONDS, messageAttachment());
    }
}
