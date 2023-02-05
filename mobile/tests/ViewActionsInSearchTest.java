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
import com.yandex.mail.steps.SearchSteps;
import com.yandex.mail.suites.Acceptance;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.ViewActionsInSearchFakeDataRule;
import com.yandex.mail.util.SetUpFailureHandler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.yandex.mail.DefaultSteps.TIMEOUT_WAIT_FOR_ITEMS;
import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.shouldNotSee;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.searchButton;
import static com.yandex.mail.pages.MessageViewPage.MESSAGE_DATE_TEXT;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.importantLabel;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.importantLabelRemovingIcon;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.unreadIcon;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.userLabel;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.userLabelRemovingIcon;
import static com.yandex.mail.pages.SearchPage.SearchInputBlock.searchSpinner;
import static com.yandex.mail.pages.TopBarBlock.upButton;
import static com.yandex.mail.tests.data.ViewActionsInSearchFakeDataRule.IMPORTANT_MESSAGE_IN_SEARCH;
import static com.yandex.mail.tests.data.ViewActionsInSearchFakeDataRule.MESSAGE_WITH_ATTACH_IN_SEARCH_FIRSTLINE;
import static com.yandex.mail.tests.data.ViewActionsInSearchFakeDataRule.MESSAGE_WITH_ATTACH_IN_SEARCH_SUBJ;
import static com.yandex.mail.tests.data.ViewActionsInSearchFakeDataRule.SOCIAL_MESSAGE1_IN_INBOX;
import static com.yandex.mail.tests.data.ViewActionsInSearchFakeDataRule.SPAM_MESSAGE_SUBJECT;
import static com.yandex.mail.tests.data.ViewActionsInSearchFakeDataRule.VIEW_OPERATION_IN_SEARCH_MESSAGE;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.ARCHIVE_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.IMPORTANT_LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.INBOX_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_UNREAD;
import static com.yandex.mail.util.FoldersAndLabelsConst.SPAM_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.TRASH_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.ARCHIVE_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.DELETE_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MAKE_IMPORTANT;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MAKE_UNIMPORTANT;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MAKE_UNREAD;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MARK_AS;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MOVE_TO_FOLDER_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.NOT_SPAM;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.SPAM_MENU_EXCLAMATION;
import static java.util.concurrent.TimeUnit.SECONDS;

@RunWith(AndroidJUnit4.class)
public class ViewActionsInSearchTest {

    @NonNull
    private SearchSteps onSearch = new SearchSteps();

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private MessageViewSteps onMessageView = new MessageViewSteps();

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @NonNull
    private ItemList folderLabelList = new ItemList();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new ViewActionsInSearchFakeDataRule(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)))
            .around(
                    new ExternalResource() {
                        @Override
                        protected void before() throws Throwable {
                            clickOn(searchButton());
                        }
                    }
            );

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @Acceptance
    //todo: wait for fix fro attach in search for fake server
    public void shouldOpenUnreadMessageFromSearch() {
        onSearch.inputTextAndTapOnSearch(MESSAGE_WITH_ATTACH_IN_SEARCH_SUBJ);
        onMessageList.waitForMessageList()
                .openMessage(MESSAGE_WITH_ATTACH_IN_SEARCH_SUBJ);

        onMessageView.shouldSeeAllMessageElements(
                MESSAGE_WITH_ATTACH_IN_SEARCH_SUBJ,
                MESSAGE_DATE_TEXT,
                MESSAGE_WITH_ATTACH_IN_SEARCH_FIRSTLINE
        );

//        shouldSee(TIMEOUT_WAIT_FOR_WEB_ITEMS, SECONDS, messageAttachment());

        onMessageView.expandMessageArrow()
                .shouldSeeAllExpandedHeadElements(INBOX_FOLDER);
        shouldSee(
                TIMEOUT_WAIT_FOR_ITEMS, SECONDS, userLabel(),
                importantLabel(),
                userLabelRemovingIcon(),
                importantLabelRemovingIcon()
        );
        shouldNotSee(unreadIcon(), TIMEOUT_WAIT_FOR_ITEMS, SECONDS);
    }

    @Test
    @BusinessLogic
    public void shouldMarkWithLabelFromViewInSearch() {
        onSearch.inputTextAndTapOnSearch(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageList.waitForMessageList()
                .openMessage(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageView.makeOperationFromMenuForExpandedMessage(MARK_AS);
        folderLabelList.chooseLabelWithTextFromList(LABEL_NAME);
        onMessageView.expandMessageArrow();
        shouldSee(userLabel());
        clickOn(upButton());
        onMessageList.shouldSeeUserLabelOnMessage(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(VIEW_OPERATION_IN_SEARCH_MESSAGE, LABEL_NAME);
    }

    @Test
    @BusinessLogic
    public void shouldMarkAsImportantFromViewInSearch() {
        onSearch.inputTextAndTapOnSearch(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageList.waitForMessageList()
                .openMessage(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageView.makeOperationFromMenuForExpandedMessage(MAKE_IMPORTANT)
                .expandMessageArrow();
        shouldSee(importantLabel());
        clickOn(upButton());
        onMessageList.shouldSeeImportantLabelOnMessage(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(VIEW_OPERATION_IN_SEARCH_MESSAGE, IMPORTANT_LABEL_NAME);
    }

    @Test
    @BusinessLogic
    public void shouldUnmarkAsImportantFromViewInSearch() {
        onSearch.inputTextAndTapOnSearch(IMPORTANT_MESSAGE_IN_SEARCH);
        onMessageList.waitForMessageList()
                .shouldSeeImportantLabelOnMessage(IMPORTANT_MESSAGE_IN_SEARCH)
                .openMessage(IMPORTANT_MESSAGE_IN_SEARCH);
        onMessageView.makeOperationFromMenuForExpandedMessage(MAKE_UNIMPORTANT)
                .expandMessageArrow();
        shouldNotSee(importantLabel(), TIMEOUT_WAIT_FOR_ITEMS, SECONDS);
        clickOn(upButton());
        onMessageList.shouldNotSeeImportantLabelOnMessage(IMPORTANT_MESSAGE_IN_SEARCH);
    }

    @Test
    @BusinessLogic
    public void shouldMoveToFolderFromViewInSearch() {
        onSearch.inputTextAndTapOnSearch(SOCIAL_MESSAGE1_IN_INBOX);
        onMessageList.waitForMessageList()
                .openMessage(SOCIAL_MESSAGE1_IN_INBOX);
        onMessageView.makeOperationFromMenuForExpandedMessage(MOVE_TO_FOLDER_MENU);
        folderLabelList.chooseFolderWithTextFromList(USERS_FOLDER_MEDUZA);
        onMessageView.shouldFolderBeChanged(USERS_FOLDER_MEDUZA, TIMEOUT_WAIT_FOR_ITEMS, SECONDS);
        clickOn(upButton());
        onMessageList.shouldSeeMessageInCurrentFolder(SOCIAL_MESSAGE1_IN_INBOX);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(SOCIAL_MESSAGE1_IN_INBOX, USERS_FOLDER_MEDUZA);
    }

    @Test
    @BusinessLogic
    public void shouldMarkAsSpamFromViewInSearch() {
        onSearch.inputTextAndTapOnSearch(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageList.waitForMessageList()
                .openMessage(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageView.makeOperationFromMenuForExpandedMessage(SPAM_MENU_EXCLAMATION);
        clickOn(upButton());
        onMessageList.shouldSeeMessageInCurrentFolder(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(VIEW_OPERATION_IN_SEARCH_MESSAGE, SPAM_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldDeleteFromViewInSearch() {
        onSearch.inputTextAndTapOnSearch(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageList.waitForMessageList()
                .openMessage(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageView.makeOperationFromMenuForExpandedMessage(DELETE_MENU);
        clickOn(upButton());
        onMessageList.shouldSeeMessageInCurrentFolder(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(VIEW_OPERATION_IN_SEARCH_MESSAGE, TRASH_FOLDER);
    }

    @Test
    @BusinessLogic
    //st.yandex-team.ru/MOBILEMAIL-2236
    public void shouldNotSeeMessageFromCurrentFolderAfterDeleteInSearch() {
        clickOn(searchSpinner());
        folderLabelList.chooseItemFromPopup(withText(USERS_FOLDER_MEDUZA));
        onSearch.inputTextAndTapOnSearch(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageList.waitForMessageList()
                .openMessage(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageView.makeOperationFromMenuForExpandedMessage(DELETE_MENU)
                .expandMessageArrow();
        clickOn(upButton());
        onMessageList.shouldNotSeeMessageInCurrentFolder(VIEW_OPERATION_IN_SEARCH_MESSAGE);
    }

    @Test
    @BusinessLogic
    public void shouldArchiveFromViewInSearch() {
        onSearch.inputTextAndTapOnSearch(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageList.waitForMessageList()
                .openMessage(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageView.makeOperationFromMenuForExpandedMessage(ARCHIVE_MENU)
                .shouldFolderBeChanged(ARCHIVE_FOLDER, TIMEOUT_WAIT_FOR_ITEMS, SECONDS);
        clickOn(upButton());
        onMessageList.shouldSeeMessageInCurrentFolder(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(VIEW_OPERATION_IN_SEARCH_MESSAGE, ARCHIVE_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldMarkAsUnreadFromViewInSearch() {
        onSearch.inputTextAndTapOnSearch(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageList.waitForMessageList()
                .openMessage(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onMessageView.makeOperationFromMenuForExpandedMessage(MAKE_UNREAD)
                .expandMessageArrow();
        clickOn(upButton());
        onMessageList.shouldSeeMessageInCurrentFolder(VIEW_OPERATION_IN_SEARCH_MESSAGE);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(VIEW_OPERATION_IN_SEARCH_MESSAGE, LABEL_UNREAD);
    }

    @Test
    @BusinessLogic
    public void shouldMarkAsNotSpamFromViewInSearch() {
        onSearch.inputTextAndTapOnSearch(SPAM_MESSAGE_SUBJECT);
        onMessageList.waitForMessageList()
                .openMessage(SPAM_MESSAGE_SUBJECT);
        onMessageView.makeOperationFromMenuForExpandedMessage(NOT_SPAM)
                .shouldFolderBeChanged(INBOX_FOLDER, TIMEOUT_WAIT_FOR_ITEMS, SECONDS);

        clickOn(upButton());
        onMessageList.shouldSeeMessageInCurrentFolder(SPAM_MESSAGE_SUBJECT);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(SPAM_MESSAGE_SUBJECT, INBOX_FOLDER);
    }
}
