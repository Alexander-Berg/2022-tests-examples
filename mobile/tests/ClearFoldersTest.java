package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.FolderSteps;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.ClearFoldersFakeDataRule;
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

import static com.yandex.mail.DefaultSteps.shouldSeeSnackBarWithText;
import static com.yandex.mail.pages.FolderPage.ClearOption.toastClearedFolder;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.SPAM_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.TRASH_FOLDER;

@RunWith(AndroidJUnit4.class)
public class ClearFoldersTest {

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new ClearFoldersFakeDataRule(FAKE_USER))
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @BusinessLogic
    public void shouldCancelClearTrashFolder() {
        onFolderList.goBackToFolderList()
                .tapClearOptionOnFolder(TRASH_FOLDER)
                .shouldSeeEmptyFolderAlert()
                .cancelEmptyFolder()
                .shouldNotSeeEmptyFolderAlert()
                .chooseFolderOrLabel(TRASH_FOLDER);
        onMessageList.waitForMessageList();
    }

    @Test
    @BusinessLogic
    public void shouldClearTrashFolder() {
        onFolderList.goBackToFolderList()
                .tapClearOptionOnFolder(TRASH_FOLDER)
                .makeFolderEmpty()
                .shouldNotSeeEmptyOptionForFolder(TRASH_FOLDER)
                .chooseFolderOrLabel(TRASH_FOLDER);
        onMessageList.shouldSeeEmptyMessageList();
    }

    @Test
    @BusinessLogic
    public void shouldClearSpamFolder() {
        onFolderList.goBackToFolderList()
                .tapClearOptionOnFolder(SPAM_FOLDER)
                .makeFolderEmpty()
                .shouldNotSeeEmptyOptionForFolder(SPAM_FOLDER)
                .chooseFolderOrLabel(SPAM_FOLDER);
        onMessageList.shouldSeeEmptyMessageList();
    }

    @Test
    @BusinessLogic
    public void shouldSeeToastAfterClearFolderTrash() {
        onFolderList.goBackToFolderList()
                .tapClearOptionOnFolder(TRASH_FOLDER)
                .makeFolderEmpty();
        shouldSeeSnackBarWithText(toastClearedFolder());
    }

    @Test
    @BusinessLogic
    public void shouldSeeToastAfterClearFolderSpam() {
        onFolderList.goBackToFolderList()
                .tapClearOptionOnFolder(SPAM_FOLDER)
                .makeFolderEmpty();
        shouldSeeSnackBarWithText(toastClearedFolder());
    }
}
