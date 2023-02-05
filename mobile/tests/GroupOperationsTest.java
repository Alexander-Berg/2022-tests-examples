package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.pages.ItemList;
import com.yandex.mail.pages.MessageListPage;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeAdsRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.FolderSteps;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.steps.MessageViewSteps;
import com.yandex.mail.suites.Acceptance;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.MessageListFakeDataRule;
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

import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.pages.MessageListPage.GroupModeActionMenu.markImportantAction;
import static com.yandex.mail.pages.MessageListPage.GroupModeActionMenu.markWithLabelAction;
import static com.yandex.mail.pages.MessageListPage.GroupModeActionMenu.unmarkImportantAction;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.importantLabel;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.userLabel;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.ARCHIVE_SINGLE_MESSAGE;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.DELETE_FROM_TRASH_FOLDER_MESSAGE_SUBJ;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.DELETE_SINGLE_MESSAGE;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.GROUP_OPERATIONS_THREADS_FOLDER;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.MARK_AS_IMPORTANT_SINGLE_MESSAGE;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.MARK_AS_READ_SINGLE_MESSAGE;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.MARK_UNREAD_SINGLE_MESSAGE;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.MARK_WITH_LABEL_SINGLE_MESSAGE;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.MESSAGE_COUNT;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.MOVE_TO_ACTION_FULL_THREAD1;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.MOVE_TO_ACTION_FULL_THREAD2;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.MOVE_TO_FOLDER_SINGLE_MESSAGE;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.MOVE_TO_SPAM_SINGLE_MESSAGE;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.MOVING_MARKING_FOLDER;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.UNMARK_AS_IMPORTANT_SINGLE_MESSAGE;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.UNMARK_LABEL_SINGLE_MESSAGE;
import static com.yandex.mail.tests.data.MessageListFakeDataRule.UNSPAM_MESSAGE;
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
import static java.util.concurrent.TimeUnit.SECONDS;

@RunWith(AndroidJUnit4.class)
public class GroupOperationsTest {

    private static final int WAIT_ELEMENT_TIMEOUT = 6;

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @NonNull
    private MessageViewSteps onMessageView = new MessageViewSteps();

    @NonNull
    public ItemList folderLabelList = new ItemList();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new MessageListFakeDataRule(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new FakeAdsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)))
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
    @BusinessLogic
    public void shouldMarkWithLabelSingleMessage() {
        onMessageList.shouldSeeMessageInFolder(MARK_WITH_LABEL_SINGLE_MESSAGE, MOVING_MARKING_FOLDER);
        onMessageList
                .selectMessage(MARK_WITH_LABEL_SINGLE_MESSAGE)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(markWithLabelAction());
        folderLabelList.chooseLabelWithTextFromList(LABEL_NAME);
        onMessageList.shouldSeeUserLabelOnMessage(MARK_WITH_LABEL_SINGLE_MESSAGE)
                .openMessage(MARK_WITH_LABEL_SINGLE_MESSAGE);
        onMessageView.expandMessageArrow();
        shouldSee(WAIT_ELEMENT_TIMEOUT, SECONDS, userLabel());
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MARK_WITH_LABEL_SINGLE_MESSAGE, LABEL_NAME);
    }

    @Test
    @BusinessLogic
    public void shouldUnmarkWithLabelSingleMessage() {
        onMessageList.shouldSeeMessageInFolder(UNMARK_LABEL_SINGLE_MESSAGE, MOVING_MARKING_FOLDER);
        onMessageList
                .selectMessage(UNMARK_LABEL_SINGLE_MESSAGE)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(markWithLabelAction());
        folderLabelList.chooseLabelWithTextFromList(LABEL_NAME);
        onMessageList.shouldNotSeeUserLabelOnMessage(UNMARK_LABEL_SINGLE_MESSAGE);
    }

    @Test
    @BusinessLogic
    public void shouldMarkAsUnreadSingleMessage() {
        onMessageList.shouldSeeMessageInFolder(MARK_UNREAD_SINGLE_MESSAGE, MOVING_MARKING_FOLDER);
        onMessageList
                .selectMessage(MARK_UNREAD_SINGLE_MESSAGE)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(MessageListPage.GroupModeActionMenu.markUnreadAction());
        onMessageList.shouldSeeUnreadIconOnMessage(MARK_UNREAD_SINGLE_MESSAGE);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MARK_UNREAD_SINGLE_MESSAGE, LABEL_UNREAD);
    }

    @Test
    @BusinessLogic
    public void shouldMarkAsReadSingleMessage() {
        onMessageList.shouldSeeMessageInFolder(MARK_AS_READ_SINGLE_MESSAGE, MOVING_MARKING_FOLDER);
        onMessageList
                .selectMessage(MARK_AS_READ_SINGLE_MESSAGE)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(MessageListPage.GroupModeActionMenu.markAsReadAction());
        onMessageList.shouldNotSeeUnreadIconOnMessage(MARK_AS_READ_SINGLE_MESSAGE);
    }

    @Test
    @BusinessLogic
    public void shouldMoveToFolderSingleMessage() {
        onMessageList.shouldSeeMessageInFolder(MOVE_TO_FOLDER_SINGLE_MESSAGE, MOVING_MARKING_FOLDER);
        onMessageList
                .selectMessage(MOVE_TO_FOLDER_SINGLE_MESSAGE)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(MessageListPage.GroupModeActionMenu.moveToFolder());
        folderLabelList.chooseFolderWithTextFromList(USERS_FOLDER_MEDUZA);
        onMessageList.shouldNotSeeMessageInCurrentFolder(MOVE_TO_FOLDER_SINGLE_MESSAGE);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MOVE_TO_FOLDER_SINGLE_MESSAGE, USERS_FOLDER_MEDUZA);
    }

    @Test
    @Acceptance
    public void shouldMakeImportantSingleMessageFromGroupMode() {
        onMessageList.shouldSeeMessageInFolder(MARK_AS_IMPORTANT_SINGLE_MESSAGE, MOVING_MARKING_FOLDER);
        onMessageList
                .selectMessage(MARK_AS_IMPORTANT_SINGLE_MESSAGE)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(markImportantAction());
        onMessageList.shouldSeeImportantLabelOnMessage(MARK_AS_IMPORTANT_SINGLE_MESSAGE)
                .openMessage(MARK_AS_IMPORTANT_SINGLE_MESSAGE);
        onMessageView.expandMessageArrow();
        shouldSee(WAIT_ELEMENT_TIMEOUT, SECONDS, importantLabel());
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MARK_AS_IMPORTANT_SINGLE_MESSAGE, IMPORTANT_LABEL_NAME);
    }

    @Test
    @BusinessLogic
    public void shouldUnmarkAsImportantSingleMessage() {
        onMessageList.shouldSeeMessageInFolder(UNMARK_AS_IMPORTANT_SINGLE_MESSAGE, MOVING_MARKING_FOLDER);
        onMessageList
                .selectMessage(UNMARK_AS_IMPORTANT_SINGLE_MESSAGE)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(unmarkImportantAction());
        onMessageList.shouldNotSeeImportantLabelOnMessage(UNMARK_AS_IMPORTANT_SINGLE_MESSAGE);
    }

    @Test
    @Acceptance
    public void shouldMoveToSpamSingleMessage() {
        onMessageList.shouldSeeMessageInFolder(MOVE_TO_SPAM_SINGLE_MESSAGE, MOVING_MARKING_FOLDER);
        onMessageList
                .selectMessage(MOVE_TO_SPAM_SINGLE_MESSAGE)
                .tapSpamButtonInTopBar()
                .shouldNotSeeMessageInCurrentFolder(MOVE_TO_SPAM_SINGLE_MESSAGE);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MOVE_TO_SPAM_SINGLE_MESSAGE, SPAM_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldUnspamSingleMessage() {
        onMessageList.shouldSeeMessageInFolder(UNSPAM_MESSAGE, SPAM_FOLDER)
                .selectMessage(UNSPAM_MESSAGE)
                .tapUnspamButtonInTopBar()
                .shouldNotSeeMessageInCurrentFolder(UNSPAM_MESSAGE);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(UNSPAM_MESSAGE, INBOX_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldDeleteSingleMessage() {
        onMessageList.shouldSeeMessageInFolder(DELETE_SINGLE_MESSAGE, MOVING_MARKING_FOLDER);
        onMessageList
                .selectMessage(DELETE_SINGLE_MESSAGE)
                .tapDeleteButtonInTopBar()
                .shouldNotSeeMessageInCurrentFolder(DELETE_SINGLE_MESSAGE);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(DELETE_SINGLE_MESSAGE, TRASH_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldDeleteFromTrashFolder() {
        onMessageList.shouldSeeMessageInFolder(DELETE_FROM_TRASH_FOLDER_MESSAGE_SUBJ, TRASH_FOLDER);
        onMessageList
                .selectMessage(DELETE_FROM_TRASH_FOLDER_MESSAGE_SUBJ)
                .tapDeleteButtonInTopBar();
        onMessageList.shouldSeeEmptyMessageList();
    }

    @Test
    @BusinessLogic
    public void shouldArchiveSingleMessage() {
        onMessageList.shouldSeeMessageInFolder(ARCHIVE_SINGLE_MESSAGE, MOVING_MARKING_FOLDER);
        onMessageList
                .selectMessage(ARCHIVE_SINGLE_MESSAGE)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(MessageListPage.GroupModeActionMenu.archiveAction())
                .shouldNotSeeMessageInCurrentFolder(ARCHIVE_SINGLE_MESSAGE);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(ARCHIVE_SINGLE_MESSAGE, ARCHIVE_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldDeleteFullThread() {
        onMessageList.shouldSeeMessageInFolder(MOVE_TO_ACTION_FULL_THREAD2, GROUP_OPERATIONS_THREADS_FOLDER);
        onMessageList
                .selectMessage(MOVE_TO_ACTION_FULL_THREAD2)
                .tapDeleteButtonInTopBar()
                .shouldNotSeeMessageInCurrentFolder(MOVE_TO_ACTION_FULL_THREAD2);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeNMessagesInFolder(MOVE_TO_ACTION_FULL_THREAD2, MESSAGE_COUNT, TRASH_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldSpamFullThread() {
        onMessageList.shouldSeeMessageInFolder(MOVE_TO_ACTION_FULL_THREAD2, GROUP_OPERATIONS_THREADS_FOLDER);
        onMessageList
                .selectMessage(MOVE_TO_ACTION_FULL_THREAD2)
                .tapSpamButtonInTopBar()
                .shouldNotSeeMessageInCurrentFolder(MOVE_TO_ACTION_FULL_THREAD2);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeNMessagesInFolder(MOVE_TO_ACTION_FULL_THREAD2, MESSAGE_COUNT, SPAM_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldArchiveFullThread() {
        onMessageList.shouldSeeMessageInFolder(MOVE_TO_ACTION_FULL_THREAD2, GROUP_OPERATIONS_THREADS_FOLDER);
        onMessageList
                .selectMessage(MOVE_TO_ACTION_FULL_THREAD2)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(MessageListPage.GroupModeActionMenu.archiveAction())
                .shouldNotSeeMessageInCurrentFolder(MOVE_TO_ACTION_FULL_THREAD2);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeNMessagesInFolder(MOVE_TO_ACTION_FULL_THREAD2, MESSAGE_COUNT, ARCHIVE_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldMarkAsImportantFullThread() {
        onMessageList.shouldSeeMessageInFolder(MOVE_TO_ACTION_FULL_THREAD2, GROUP_OPERATIONS_THREADS_FOLDER);
        onMessageList
                .selectMessage(MOVE_TO_ACTION_FULL_THREAD2)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(MessageListPage.GroupModeActionMenu.markImportantAction());
        onMessageList.shouldSeeImportantLabelOnMessage(MOVE_TO_ACTION_FULL_THREAD2);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeNMessagesInFolder(MOVE_TO_ACTION_FULL_THREAD2, MESSAGE_COUNT, IMPORTANT_LABEL_NAME);
    }

    @Test
    @BusinessLogic
    public void shouldMoveToFolderFullThread() {
        onMessageList.shouldSeeMessageInFolder(MOVE_TO_ACTION_FULL_THREAD1, GROUP_OPERATIONS_THREADS_FOLDER);
        onMessageList
                .selectMessage(MOVE_TO_ACTION_FULL_THREAD1)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(MessageListPage.GroupModeActionMenu.moveToFolder());
        folderLabelList.chooseFolderWithTextFromList(USERS_FOLDER_MEDUZA);
        onMessageList.shouldNotSeeMessageInCurrentFolder(MOVE_TO_ACTION_FULL_THREAD1);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(MOVE_TO_ACTION_FULL_THREAD1, USERS_FOLDER_MEDUZA);
    }

    @Test
    @BusinessLogic
    public void shouldMarkWithLabelFullThread() {
        onMessageList.shouldSeeMessageInFolder(MOVE_TO_ACTION_FULL_THREAD2, GROUP_OPERATIONS_THREADS_FOLDER);
        onMessageList
                .selectMessage(MOVE_TO_ACTION_FULL_THREAD2)
                .openActionsMenu()
                .chooseOperationFromOverflowMenu(markWithLabelAction());
        folderLabelList.chooseLabelWithTextFromList(LABEL_NAME);
        onMessageList.shouldSeeUserLabelOnMessage(MOVE_TO_ACTION_FULL_THREAD2);
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeNMessagesInFolder(MOVE_TO_ACTION_FULL_THREAD2, MESSAGE_COUNT, LABEL_NAME);
    }
}
