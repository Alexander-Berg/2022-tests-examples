package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.pages.FolderPage;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.FolderSteps;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.suites.BusinessLogic;
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

import static com.yandex.mail.DefaultSteps.setUpLandscape;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_NAME;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.INBOX_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.SPAM_FOLDER;

@RunWith(AndroidJUnit4.class)
public class LandscapeFolderList {

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @NonNull
    private static MessageListSteps onMessageList = new MessageListSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @BusinessLogic
    public void shouldFolderListBeLoadedAfterRotation() {
        onFolderList.goBackToFolderList();
        setUpLandscape();
        onFolderList.shouldSeeAllAccSwitcherElements(USER_NAME);
        shouldSee(FolderPage.folderItem(INBOX_FOLDER));
    }

    @Test
    @BusinessLogic
    public void shouldOpenFolderSpamAfterRotation() {
        onFolderList.goBackToFolderList();
        setUpLandscape();
        onFolderList.chooseFolderOrLabel(SPAM_FOLDER);
        onMessageList.waitForMessageList();
    }
}
