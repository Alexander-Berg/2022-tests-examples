package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.pages.ItemList;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.steps.SearchSteps;
import com.yandex.mail.suites.Acceptance;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.SearchFakeDataRule;
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
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.shouldNotSee;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.DefaultSteps.shouldSeeText;
import static com.yandex.mail.TestUtil.randomText;
import static com.yandex.mail.matchers.RecyclerViewMatchers.withListSize;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.searchButton;
import static com.yandex.mail.pages.MessageListPage.emailListRecycler;
import static com.yandex.mail.pages.SearchPage.SEARCH_SPINNER_DEFAULT_TEXT;
import static com.yandex.mail.pages.SearchPage.SearchInputBlock.searchInputArea;
import static com.yandex.mail.pages.SearchPage.SearchInputBlock.searchSpinner;
import static com.yandex.mail.tests.data.SearchFakeDataRule.SEARCH_IN_CURRENT_FOLDER_MESSAGE;
import static com.yandex.mail.tests.data.SearchFakeDataRule.SOCIAL_MESSAGE1_IN_INBOX;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.INBOX_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;

@RunWith(AndroidJUnit4.class)
public class SearchTest {

    @NonNull
    private SearchSteps onSearch = new SearchSteps();

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    public ItemList folderListInSearch = new ItemList();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new SearchFakeDataRule(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @Acceptance
    public void shouldSeeAllSearchScreenElements() {
        clickOn(searchButton());
        shouldSee(searchInputArea());
        shouldSeeText(searchSpinner(), SEARCH_SPINNER_DEFAULT_TEXT);
    }

    @Test
    @BusinessLogic
    public void shouldFindMessagesWithSearchInAllFolders() {
        clickOn(searchButton());
        onSearch.inputTextAndTapOnSearch(SEARCH_IN_CURRENT_FOLDER_MESSAGE);
        onMessageList.waitForMessageList();
        onView(emailListRecycler()).check(matches(withListSize(8)));
    }

    @Test
    @BusinessLogic
    public void shouldNotFindNotExistedMessage() {
        clickOn(searchButton());
        onSearch.inputTextAndTapOnSearch(randomText(10));
        shouldNotSee(emailListRecycler());
    }

    @Test
    @BusinessLogic
    public void shouldFindMessageFromCurrentFolderInbox() {
        clickOn(searchButton());
        clickOn(searchSpinner());
        folderListInSearch.chooseItemFromPopup(withText(INBOX_FOLDER));
        onSearch.inputTextAndTapOnSearch(SOCIAL_MESSAGE1_IN_INBOX);
        onMessageList.waitForMessageList();
        onView(emailListRecycler()).check(matches(withListSize(1)));
    }

    @Test
    @BusinessLogic
    public void shouldNotSeeMessageFromInboxWhenSearchAndChangeFolder() {
        clickOn(searchButton());
        onSearch.inputTextAndTapOnSearch(SOCIAL_MESSAGE1_IN_INBOX);
        onMessageList.waitForMessageList();
        clickOn(searchSpinner());
        folderListInSearch.chooseItemFromPopup(withText(USERS_FOLDER_MEDUZA));
        shouldNotSee(emailListRecycler());
    }

    @Test
    @BusinessLogic
    public void shouldNotSeeMessageFromInboxWhenChangeFolderAndSearch() {
        clickOn(searchButton());
        clickOn(searchSpinner());
        folderListInSearch.chooseItemFromPopup(withText(USERS_FOLDER_MEDUZA));
        onSearch.inputTextAndTapOnSearch(SOCIAL_MESSAGE1_IN_INBOX);
        shouldNotSee(emailListRecycler());
    }
}
