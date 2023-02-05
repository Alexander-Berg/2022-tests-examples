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
import com.yandex.mail.steps.SearchSteps;
import com.yandex.mail.suites.Acceptance;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.SearchGroupOperationsFakeDataRule;
import com.yandex.mail.util.SetUpFailureHandler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;

import androidx.annotation.NonNull;
import androidx.test.rule.ActivityTestRule;

import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.pages.MessageListPage.GroupModeActionMenu.archiveAction;
import static com.yandex.mail.pages.MessageListPage.GroupModeActionMenu.markImportantAction;
import static com.yandex.mail.pages.MessageListPage.GroupModeActionMenu.markWithLabelAction;
import static com.yandex.mail.pages.MessageListPage.GroupModeActionMenu.moveToFolder;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.groupModeSpamIcon;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.searchButton;
import static com.yandex.mail.tests.data.SearchGroupOperationsFakeDataRule.MESSAGE_IN_INBOX;
import static com.yandex.mail.tests.data.SearchGroupOperationsFakeDataRule.MESSAGE_IN_USER_FOLDER;
import static com.yandex.mail.tests.data.SearchGroupOperationsFakeDataRule.SWIPE_AFTER_SCROLL_THREAD;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.ARCHIVE_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.IMPORTANT_LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.SPAM_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.TRASH_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;

public class SearchGroupOperationsTest {

    @NonNull
    private SearchSteps onSearch = new SearchSteps();

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

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
            .around(new SearchGroupOperationsFakeDataRule(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @Acceptance
    public void shouldMarkWithLabelInSearch() {
        clickOn(searchButton());
        onSearch.inputTextAndTapOnSearch(MESSAGE_IN_USER_FOLDER);

        onMessageList
                .waitForMessageList()
                .selectMessage(MESSAGE_IN_USER_FOLDER)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(markWithLabelAction());

        folderLabelList.chooseLabelWithTextFromList(LABEL_NAME);
        onMessageList.shouldSeeUserLabelOnMessage(MESSAGE_IN_USER_FOLDER);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MESSAGE_IN_USER_FOLDER, LABEL_NAME);
    }

    @Test
    @Acceptance
    public void shouldArchiveInSearch() {
        clickOn(searchButton());
        onSearch.inputTextAndTapOnSearch(MESSAGE_IN_USER_FOLDER);

        onMessageList
                .waitForMessageList()
                .selectMessage(MESSAGE_IN_USER_FOLDER)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(archiveAction());

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MESSAGE_IN_USER_FOLDER, ARCHIVE_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldMarkAsImportantInSearch() {
        clickOn(searchButton());
        onSearch.inputTextAndTapOnSearch(MESSAGE_IN_USER_FOLDER);

        onMessageList
                .waitForMessageList()
                .selectMessage(MESSAGE_IN_USER_FOLDER)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(markImportantAction());

        onMessageList.shouldSeeImportantLabelOnMessage(MESSAGE_IN_USER_FOLDER);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MESSAGE_IN_USER_FOLDER, IMPORTANT_LABEL_NAME);
    }

    @Test
    @BusinessLogic
    public void shouldMoveToFolderFromSearch() {
        clickOn(searchButton());
        onSearch.inputTextAndTapOnSearch(MESSAGE_IN_INBOX);

        onMessageList
                .waitForMessageList()
                .selectMessage(MESSAGE_IN_INBOX)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(moveToFolder());

        folderLabelList.chooseFolderWithTextFromList(USERS_FOLDER_MEDUZA);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MESSAGE_IN_INBOX, USERS_FOLDER_MEDUZA);
    }

    @Test
    @BusinessLogic
    public void shouldSpamInSearch() {
        clickOn(searchButton());
        onSearch.inputTextAndTapOnSearch(MESSAGE_IN_USER_FOLDER);
        onMessageList
                .waitForMessageList()
                .selectMessage(MESSAGE_IN_USER_FOLDER);

        clickOn(groupModeSpamIcon());
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MESSAGE_IN_USER_FOLDER, SPAM_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldSwipeToDeleteInSearch() {
        clickOn(searchButton());
        onSearch.inputTextAndTapOnSearch(MESSAGE_IN_USER_FOLDER);
        onMessageList.waitForMessageList()
                .swipeLeftMessage(MESSAGE_IN_USER_FOLDER);

        onSearch.shouldSeeEmptySearchScreen();
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MESSAGE_IN_USER_FOLDER, TRASH_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldSwipeToUnreadInSearch() {
        clickOn(searchButton());
        onSearch.inputTextAndTapOnSearch(MESSAGE_IN_USER_FOLDER);
        onMessageList.waitForMessageList()
                .swipeRightMessage(MESSAGE_IN_USER_FOLDER);

        onMessageList.shouldSeeUnreadIconOnMessage(MESSAGE_IN_USER_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldSwipeToUnreadAfterScrollInSearch() {
        clickOn(searchButton());
        onSearch.inputTextAndTapOnSearch(SWIPE_AFTER_SCROLL_THREAD);
        onMessageList.waitForMessageList()
                .swipeRightMessage(SWIPE_AFTER_SCROLL_THREAD + 1);
        onMessageList.shouldSeeUnreadIconOnMessage(SWIPE_AFTER_SCROLL_THREAD + 1);
    }
}
