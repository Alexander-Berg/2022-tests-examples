package com.yandex.mail.tests;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.pages.Account;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeAccountRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.RealAccount;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.FolderSteps;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.steps.SettingsSteps;
import com.yandex.mail.suites.Acceptance;
import com.yandex.mail.suites.BusinessLogic;
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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static com.yandex.mail.DefaultSteps.TIMEOUT_WAIT_FOR_ITEMS;
import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.shouldNotSee;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.DefaultSteps.shouldSeeSnackBarWithText;
import static com.yandex.mail.DefaultSteps.shouldSeeText;
import static com.yandex.mail.matchers.Matchers.withAccountIconsNumber;
import static com.yandex.mail.pages.FolderPage.AccountSwitcher.accountEmail;
import static com.yandex.mail.pages.FolderPage.AccountSwitcher.scrollContainer;
import static com.yandex.mail.pages.SettingsPage.aboutOption;
import static com.yandex.mail.pages.SettingsPage.generalSettings;
import static com.yandex.mail.pages.SettingsPage.supportOption;
import static com.yandex.mail.pages.SettingsPage.toastOneAccountWarning;
import static com.yandex.mail.pages.SettingsPage.userEmailInAccountFragment;
import static com.yandex.mail.pages.TopBarBlock.upButton;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.INBOX_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.SETTINGS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static kotlin.collections.CollectionsKt.map;
import static kotlin.ranges.RangesKt.until;

@RunWith(AndroidJUnit4.class)
public class SettingsTest {

    public static final String MESSAGE_THREAD_NONTHREAD_MODE = "Zed's dead, baby. Zed's dead";

    public static final int MESSAGES_NUMBER_IN_THREAD = 3;

    public static final String ACCOUNT_TO_ADD_LOGIN = "testkarma1@yandex.ru";

    public static final String ACCOUNT_TO_ADD_PASSWORD = "111111";

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @NonNull
    private SettingsSteps onSettings = new SettingsSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(
                    new FakeAccountRule(FAKE_USER) {
                        @Override
                        public void initialize(@NonNull AccountWrapper account) {
                            account.addThreads(
                                    account.newThread(
                                            map(until(0, MESSAGES_NUMBER_IN_THREAD),
                                                    i -> account
                                                            .newUnreadMessage(account.getInboxFolder())
                                                            .subjText(MESSAGE_THREAD_NONTHREAD_MODE))).build());
                        }
                    }
            )
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)))
            .around(
                    new ExternalResource() {
                        @Override
                        protected void before() throws Throwable {
                            onFolderList.goBackToFolderList()
                                    .chooseButtonOption(SETTINGS);
                        }
                    }
            );

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @BusinessLogic
    public void shouldTurnOnNonThreadMode() {
        onSettings.turnOnThreadModeForAccount(FAKE_USER);
        onFolderList.goBackToFolderList();

        onMessageList.shouldSeeNMessagesInFolder(MESSAGE_THREAD_NONTHREAD_MODE,
                MESSAGES_NUMBER_IN_THREAD,
                INBOX_FOLDER
        );
    }

    @Test
    @Acceptance
    @RealAccount
    public void shouldAddSecondAccount() {
        onSettings.addAccount(ACCOUNT_TO_ADD_LOGIN, ACCOUNT_TO_ADD_PASSWORD);

        shouldSeeText(accountEmail(), ACCOUNT_TO_ADD_LOGIN, TIMEOUT_WAIT_FOR_ITEMS, SECONDS);
        onView(scrollContainer()).check(matches(withAccountIconsNumber(2)));

        onFolderList.chooseButtonOption(SETTINGS);
        shouldSee(userEmailInAccountFragment(ACCOUNT_TO_ADD_LOGIN));
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldDisableSecondAccount() {
        onSettings.addAccount(ACCOUNT_TO_ADD_LOGIN, ACCOUNT_TO_ADD_PASSWORD);

        onFolderList.chooseButtonOption(SETTINGS);
        onSettings.removeAccount(ACCOUNT_TO_ADD_LOGIN);
        clickOn(upButton());

        shouldNotSee(userEmailInAccountFragment(ACCOUNT_TO_ADD_LOGIN));

        //https://st.yandex-team.ru/MOBILEMAIL-6657
        onFolderList.goBackToFolderList();

        shouldSeeText(accountEmail(), USER_LOGIN, TIMEOUT_WAIT_FOR_ITEMS, SECONDS);
        onView(scrollContainer()).check(matches(withAccountIconsNumber(1)));
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldNotDisableLastAccount() {
        onSettings.addAccount(ACCOUNT_TO_ADD_LOGIN, ACCOUNT_TO_ADD_PASSWORD);

        onFolderList.chooseButtonOption(SETTINGS);
        onSettings.removeAccount(ACCOUNT_TO_ADD_LOGIN, USER_LOGIN);

        shouldSeeSnackBarWithText(toastOneAccountWarning());

        clickOn(upButton());
        shouldSee(userEmailInAccountFragment(USER_LOGIN));
        shouldNotSee(userEmailInAccountFragment(ACCOUNT_TO_ADD_LOGIN));
    }

    @Test
    @Acceptance
    @RealAccount
    public void shouldAllOptionsBePresentInSettings() {
        onSettings.shouldSeeOptionsOnSettingsPage();
        clickOn(aboutOption());
        onSettings.shouldSeeOptionsOnAboutPage();
        clickOn(upButton());

        clickOn(supportOption());
        onSettings.shouldSeeOptionsOnSupportPage();
        clickOn(upButton());

        clickOn(generalSettings());
        onSettings.shouldSeeOptionsInGeneralSettings();
        clickOn(upButton());

        clickOn(userEmailInAccountFragment(USER_LOGIN));
        onSettings.shouldSeeOptionsInAccountSettings();
    }
}