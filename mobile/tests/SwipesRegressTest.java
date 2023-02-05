package com.yandex.mail.tests;

import com.yandex.mail.TestCaseId;
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
import com.yandex.mail.suites.Regression;
import com.yandex.mail.tests.data.SwipesRegressFakeDataRule;
import com.yandex.mail.util.AccountsConst;
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

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static com.yandex.mail.DefaultSteps.TIMEOUT_WAIT_FOR_ITEMS;
import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.matchers.Matchers.withOperationTitle;
import static com.yandex.mail.pages.ItemList.OperationList.operationList;
import static com.yandex.mail.pages.MessageListPage.swipeMenuButton;
import static com.yandex.mail.pages.MessageViewPage.CollapsedMessages.arrowToExpandForMessage;
import static com.yandex.mail.pages.MessageViewPage.CollapsedMessages.headOfMessage;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.importantLabel;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.userLabel;
import static com.yandex.mail.tests.data.SwipesRegressFakeDataRule.FIRST_MESSAGE_WITH_ATTACH;
import static com.yandex.mail.tests.data.SwipesRegressFakeDataRule.MESSAGE_TO_ARCHIVE;
import static com.yandex.mail.tests.data.SwipesRegressFakeDataRule.THREAD_FOR_SWIPE;
import static com.yandex.mail.tests.data.SwipesRegressFakeDataRule.THREAD_FOR_SWIPE_IMPORTANT;
import static com.yandex.mail.tests.data.SwipesRegressFakeDataRule.USER_FOLDER_FOR_SWIPE;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.FoldersAndLabelsConst.ARCHIVE_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.IMPORTANT_LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.INBOX_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.SENT_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.SPAM_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.TRASH_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;
import static com.yandex.mail.util.FoldersAndLabelsConst.WITH_ATTACHMENT;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.ARCHIVE_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.DELETE_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MAKE_IMPORTANT;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MAKE_UNIMPORTANT;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MARK_AS;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.MOVE_TO_FOLDER_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.SPAM_MENU_EXCLAMATION;
import static java.util.concurrent.TimeUnit.SECONDS;

@RunWith(AndroidJUnit4.class)
public class SwipesRegressTest {

    private static final int INDEX_SECOND_MESSAGE = 2;

    private static final int INDEX_THIRD_MESSAGE = 3;

    private static final int NUMBER_OF_MESSAGES_IN_THREAD = 3;

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @NonNull
    private MessageViewSteps onMessageView = new MessageViewSteps();

    @NonNull
    private ItemList folderLabelList = new ItemList();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new SwipesRegressFakeDataRule(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(AccountsConst.USER_LOGIN, AccountsConst.USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @Regression
    @TestCaseId("3830")
    public void shouldMakeImportantThreadInDifferentFolders() {
        onMessageList.shouldSeeMessageInCurrentFolder(THREAD_FOR_SWIPE)
                .shortSwipeMessageAndSelect(MAKE_IMPORTANT, THREAD_FOR_SWIPE)
                .shouldSeeImportantLabelOnMessage(THREAD_FOR_SWIPE);

        onMessageList.openMessage(THREAD_FOR_SWIPE);
        onMessageView.expandMessageArrow();
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, importantLabel());
        clickOn(headOfMessage(INDEX_SECOND_MESSAGE));
        clickOn(arrowToExpandForMessage(INDEX_SECOND_MESSAGE));
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, importantLabel());
        clickOn(headOfMessage(INDEX_SECOND_MESSAGE)); // strange index of the third message in DOM
        clickOn(arrowToExpandForMessage(INDEX_THIRD_MESSAGE));
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, importantLabel());

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeNMessagesInFolder(THREAD_FOR_SWIPE, NUMBER_OF_MESSAGES_IN_THREAD, IMPORTANT_LABEL_NAME);
    }

    @Test
    @Regression
    @TestCaseId("3844")
    public void shouldMakeImportantThreadWithImportantMessageInDifferentFolders() {
        onMessageList.shouldSeeMessageInCurrentFolder(THREAD_FOR_SWIPE_IMPORTANT)
                .shortSwipeMessage(THREAD_FOR_SWIPE_IMPORTANT);
        clickOn(swipeMenuButton(THREAD_FOR_SWIPE_IMPORTANT));
        shouldSee(operationList(MAKE_IMPORTANT), operationList(MAKE_UNIMPORTANT));
        onData(withOperationTitle(MAKE_IMPORTANT)).perform(click());

        onMessageList.shouldSeeImportantLabelOnMessage(THREAD_FOR_SWIPE_IMPORTANT);

        onMessageList.openMessage(THREAD_FOR_SWIPE_IMPORTANT);
        onMessageView.expandMessageArrow();
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, importantLabel());
        clickOn(headOfMessage(INDEX_SECOND_MESSAGE));
        clickOn(arrowToExpandForMessage(INDEX_SECOND_MESSAGE));
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, importantLabel());
        clickOn(headOfMessage(INDEX_SECOND_MESSAGE)); // strange index of the third message in DOM
        clickOn(arrowToExpandForMessage(INDEX_THIRD_MESSAGE));
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, importantLabel());

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeNMessagesInFolder(THREAD_FOR_SWIPE_IMPORTANT, NUMBER_OF_MESSAGES_IN_THREAD, IMPORTANT_LABEL_NAME);
    }

    @Test
    @Regression
    @TestCaseId("3847")
    public void shouldMarkAsUserLabelThreadInDifferentFolders() {
        onMessageList.shouldSeeMessageInCurrentFolder(THREAD_FOR_SWIPE)
                .shortSwipeMessageAndSelect(MARK_AS, THREAD_FOR_SWIPE);
        folderLabelList.chooseLabelWithTextFromList(LABEL_NAME);
        onMessageList.shouldSeeUserLabelOnMessage(THREAD_FOR_SWIPE);

        onMessageList.openMessage(THREAD_FOR_SWIPE);
        onMessageView.expandMessageArrow();
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, userLabel());
        clickOn(headOfMessage(INDEX_SECOND_MESSAGE));
        clickOn(arrowToExpandForMessage(INDEX_SECOND_MESSAGE));
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, userLabel());
        clickOn(headOfMessage(INDEX_SECOND_MESSAGE)); // strange index of the third message in DOM
        clickOn(arrowToExpandForMessage(INDEX_THIRD_MESSAGE));
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, userLabel());

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeNMessagesInFolder(THREAD_FOR_SWIPE, NUMBER_OF_MESSAGES_IN_THREAD, LABEL_NAME);
    }

    @Test
    @Regression
    @TestCaseId("3821")
    public void shouldArchiveFromSwipeMenuWithoutArchiveFolder() {
        onMessageList.shouldSeeMessageInCurrentFolder(MESSAGE_TO_ARCHIVE)
                .shortSwipeMessageAndSelect(ARCHIVE_MENU, MESSAGE_TO_ARCHIVE)
                .shouldNotSeeMessageInCurrentFolder(MESSAGE_TO_ARCHIVE);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MESSAGE_TO_ARCHIVE, ARCHIVE_FOLDER);
    }

    @Test
    @Regression
    @TestCaseId("4185")
    public void shouldMoveToSpamMessageFromWithAttachLabel() {
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(FIRST_MESSAGE_WITH_ATTACH, WITH_ATTACHMENT)
                .shortSwipeMessageAndSelect(SPAM_MENU_EXCLAMATION, FIRST_MESSAGE_WITH_ATTACH)
                .shouldNotSeeMessageInCurrentFolder(FIRST_MESSAGE_WITH_ATTACH);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(FIRST_MESSAGE_WITH_ATTACH, SPAM_FOLDER)
                .shouldNotSeeMessageInFolder(FIRST_MESSAGE_WITH_ATTACH, INBOX_FOLDER);
    }

    @Test
    @Regression
    @TestCaseId("4238")
    public void shouldSwipeToDeleteMessageFromWithAttachLabel() {
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(FIRST_MESSAGE_WITH_ATTACH, WITH_ATTACHMENT)
                .swipeLeftMessage(FIRST_MESSAGE_WITH_ATTACH)
                .shouldNotSeeMessageInCurrentFolder(FIRST_MESSAGE_WITH_ATTACH);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(FIRST_MESSAGE_WITH_ATTACH, TRASH_FOLDER)
                .shouldNotSeeMessageInFolder(FIRST_MESSAGE_WITH_ATTACH, INBOX_FOLDER);
    }

    @Test
    @Regression
    @TestCaseId("3797")
    public void shouldMoveToSpamThreadInDifferentFolders() {
        onMessageList.shouldSeeMessageInCurrentFolder(THREAD_FOR_SWIPE)
                .shortSwipeMessageAndSelect(SPAM_MENU_EXCLAMATION, THREAD_FOR_SWIPE)
                .shouldNotSeeMessageInCurrentFolder(THREAD_FOR_SWIPE);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeNMessagesInFolder(THREAD_FOR_SWIPE, NUMBER_OF_MESSAGES_IN_THREAD, SPAM_FOLDER)
                .shouldNotSeeMessageInFolder(THREAD_FOR_SWIPE, SENT_FOLDER)
                .shouldNotSeeMessageInFolder(THREAD_FOR_SWIPE, USER_FOLDER_FOR_SWIPE);
    }

    @Test
    @Regression
    @TestCaseId("4189")
    public void shouldArchiveMessageFromWithAttachLabel() {
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(FIRST_MESSAGE_WITH_ATTACH, WITH_ATTACHMENT)
                .shortSwipeMessageAndSelect(ARCHIVE_MENU, FIRST_MESSAGE_WITH_ATTACH)
                .shouldNotSeeMessageInCurrentFolder(FIRST_MESSAGE_WITH_ATTACH);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(FIRST_MESSAGE_WITH_ATTACH, ARCHIVE_FOLDER)
                .shouldNotSeeMessageInFolder(FIRST_MESSAGE_WITH_ATTACH, INBOX_FOLDER);
    }

    @Test
    @Regression
    @TestCaseId("3828")
    public void shouldMoveToFolderThreadInDifferentFolders() {
        onMessageList.shouldSeeMessageInCurrentFolder(THREAD_FOR_SWIPE)
                .shortSwipeMessageAndSelect(MOVE_TO_FOLDER_MENU, THREAD_FOR_SWIPE);
        folderLabelList.chooseFolderWithTextFromList(USERS_FOLDER_MEDUZA);
        onMessageList.shouldNotSeeMessageInCurrentFolder(THREAD_FOR_SWIPE);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(THREAD_FOR_SWIPE, USERS_FOLDER_MEDUZA)
                .shouldNotSeeMessageInFolder(THREAD_FOR_SWIPE, SENT_FOLDER)
                .shouldNotSeeMessageInFolder(THREAD_FOR_SWIPE, USER_FOLDER_FOR_SWIPE);
    }

    @Test
    @Regression
    @TestCaseId("4230")
    public void shouldMoveToFolderMessageFromWithAttachLabel() {
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(FIRST_MESSAGE_WITH_ATTACH, WITH_ATTACHMENT)
                .shortSwipeMessageAndSelect(MOVE_TO_FOLDER_MENU, FIRST_MESSAGE_WITH_ATTACH);
        folderLabelList.chooseFolderWithTextFromList(USERS_FOLDER_MEDUZA);
        onMessageList.shouldSeeMessageInCurrentFolder(FIRST_MESSAGE_WITH_ATTACH);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(FIRST_MESSAGE_WITH_ATTACH, USERS_FOLDER_MEDUZA)
                .shouldNotSeeMessageInFolder(FIRST_MESSAGE_WITH_ATTACH, INBOX_FOLDER);
    }

    @Test
    @Regression
    @TestCaseId("3754")
    public void shouldSwipeToDeleteThreadInDifferentFolders() {
        onMessageList.shouldSeeMessageInCurrentFolder(THREAD_FOR_SWIPE)
                .swipeLeftMessage(THREAD_FOR_SWIPE)
                .shouldNotSeeMessageInCurrentFolder(THREAD_FOR_SWIPE);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeNMessagesInFolder(THREAD_FOR_SWIPE, NUMBER_OF_MESSAGES_IN_THREAD, TRASH_FOLDER)
                .shouldNotSeeMessageInFolder(THREAD_FOR_SWIPE, SENT_FOLDER)
                .shouldNotSeeMessageInFolder(THREAD_FOR_SWIPE, USER_FOLDER_FOR_SWIPE);
    }

    @Test
    @Regression
    @TestCaseId("3795")
    public void shouldDeleteFromMenuThreadInDifferentFolders() {
        onMessageList.shouldSeeMessageInCurrentFolder(THREAD_FOR_SWIPE)
                .shortSwipeMessageAndSelect(DELETE_MENU, THREAD_FOR_SWIPE)
                .shouldNotSeeMessageInCurrentFolder(THREAD_FOR_SWIPE);

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeNMessagesInFolder(THREAD_FOR_SWIPE, NUMBER_OF_MESSAGES_IN_THREAD, TRASH_FOLDER)
                .shouldNotSeeMessageInFolder(THREAD_FOR_SWIPE, SENT_FOLDER)
                .shouldNotSeeMessageInFolder(THREAD_FOR_SWIPE, USER_FOLDER_FOR_SWIPE);
    }
}
