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
import com.yandex.mail.suites.Acceptance;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.SwipesFakeDataRule;
import com.yandex.mail.util.AccountsConst;
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

import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.pages.TopBarBlock.upButton;
import static com.yandex.mail.tests.data.SwipesFakeDataRule.MESSAGE_FOR_SWIPE2;
import static com.yandex.mail.tests.data.SwipesFakeDataRule.UNREAD_MESSAGE_FOR_SWIPE;
import static com.yandex.mail.tests.data.SwipesFakeDataRule.USER_FOLDER_FOR_SWIPE;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.FoldersAndLabelsConst.ARCHIVE_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.IMPORTANT_LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.SPAM_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.TRASH_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.ARCHIVE_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MAKE_IMPORTANT;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MOVE_TO_FOLDER_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.SPAM_MENU_EXCLAMATION;

@RunWith(AndroidJUnit4.class)
public class SwipesTest {

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private ItemList folderLabelList = new ItemList();

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
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @Acceptance
    public void shouldSwipeLeftToDeleteSingleMessage() {
        onMessageList.shouldSeeMessageInFolder(MESSAGE_FOR_SWIPE2, USER_FOLDER_FOR_SWIPE)
                .swipeLeftMessage(MESSAGE_FOR_SWIPE2)
                .shouldNotSeeMessageInCurrentFolder(MESSAGE_FOR_SWIPE2);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MESSAGE_FOR_SWIPE2, TRASH_FOLDER);
    }

    @Test
    @Acceptance
    public void shouldSwipeRightToUnreadSingleMessage() {
        onMessageList.shouldSeeMessageInFolder(MESSAGE_FOR_SWIPE2, USER_FOLDER_FOR_SWIPE)
                .swipeRightMessage(MESSAGE_FOR_SWIPE2)
                .shouldSeeUnreadIconOnMessage(MESSAGE_FOR_SWIPE2);
    }

    @Test
    @Acceptance
    public void shouldSwipeRightToReadSingleMessage() {
        onMessageList.shouldSeeMessageInFolder(UNREAD_MESSAGE_FOR_SWIPE, USER_FOLDER_FOR_SWIPE)
                .swipeRightMessage(UNREAD_MESSAGE_FOR_SWIPE)
                .shouldNotSeeUnreadIconOnMessage(UNREAD_MESSAGE_FOR_SWIPE);
    }

    @Test
    @Acceptance
    public void shouldMoveToSpamFromSwipeMenu() {
        onMessageList.shouldSeeMessageInFolder(MESSAGE_FOR_SWIPE2, USER_FOLDER_FOR_SWIPE)
                .shortSwipeMessageAndSelect(SPAM_MENU_EXCLAMATION, MESSAGE_FOR_SWIPE2)
                .shouldNotSeeMessageInCurrentFolder(MESSAGE_FOR_SWIPE2);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MESSAGE_FOR_SWIPE2, SPAM_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldArchiveFromSwipeMenu() {
        onMessageList.shouldSeeMessageInFolder(MESSAGE_FOR_SWIPE2, USER_FOLDER_FOR_SWIPE)
                .shortSwipeMessageAndSelect(ARCHIVE_MENU, MESSAGE_FOR_SWIPE2)
                .shouldNotSeeMessageInCurrentFolder(MESSAGE_FOR_SWIPE2);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MESSAGE_FOR_SWIPE2, ARCHIVE_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldMoveToFolderFromSwipeMenu() {
        onMessageList.shouldSeeMessageInFolder(MESSAGE_FOR_SWIPE2, USER_FOLDER_FOR_SWIPE)
                .shortSwipeMessageAndSelect(MOVE_TO_FOLDER_MENU, MESSAGE_FOR_SWIPE2);
        folderLabelList.chooseFolderWithTextFromList(USERS_FOLDER_MEDUZA);
        onMessageList.shouldNotSeeMessageInCurrentFolder(MESSAGE_FOR_SWIPE2);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MESSAGE_FOR_SWIPE2, USERS_FOLDER_MEDUZA);
    }

    @Test
    @BusinessLogic
    public void shouldMakeImportantFromSwipeMenu() {
        onMessageList.shouldSeeMessageInFolder(MESSAGE_FOR_SWIPE2, USER_FOLDER_FOR_SWIPE)
                .shortSwipeMessageAndSelect(MAKE_IMPORTANT, MESSAGE_FOR_SWIPE2)
                .shouldSeeImportantLabelOnMessage(MESSAGE_FOR_SWIPE2);

        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(MESSAGE_FOR_SWIPE2, IMPORTANT_LABEL_NAME);
    }
}
