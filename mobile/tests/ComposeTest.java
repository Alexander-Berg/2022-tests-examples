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
import com.yandex.mail.steps.ComposeSteps;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.steps.MessageViewSteps;
import com.yandex.mail.suites.BusinessLogic;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static com.yandex.mail.DefaultSteps.TIMEOUT_WAIT_FOR_ITEMS;
import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.doImeAction;
import static com.yandex.mail.DefaultSteps.fillInTextField;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.DefaultSteps.shouldSeeFocusedField;
import static com.yandex.mail.DefaultSteps.shouldSeeSnackBarWithText;
import static com.yandex.mail.DefaultSteps.shouldSeeText;
import static com.yandex.mail.TestUtil.randomText;
import static com.yandex.mail.pages.ComposePage.ADDRESS_TO;
import static com.yandex.mail.pages.ComposePage.ANOTHER_ADDRESS_TO;
import static com.yandex.mail.pages.ComposePage.INVALID_ADDRESS;
import static com.yandex.mail.pages.ComposePage.LINK_TO_SEND_IN_MESSAGE;
import static com.yandex.mail.pages.ComposePage.NAME_FOR_ADDRESS_TO;
import static com.yandex.mail.pages.ComposePage.NAME_FOR_ANOTHER_ADDRESS_TO;
import static com.yandex.mail.pages.ComposePage.sendButton;
import static com.yandex.mail.pages.ComposePage.snackBarInvalidEmail;
import static com.yandex.mail.pages.ComposePage.snackBarNoToError;
import static com.yandex.mail.pages.ComposePage.subject;
import static com.yandex.mail.pages.ComposePage.textBody;
import static com.yandex.mail.pages.ComposePage.toField;
import static com.yandex.mail.pages.ComposePage.yableText;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.composeButton;
import static com.yandex.mail.pages.MessageViewPage.ExpandedMessageHead.nameInYable;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.messageBodyLink;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.messageSubject;
import static com.yandex.mail.pages.TopBarBlock.upButton;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.SENT_FOLDER;
import static com.yandex.mail.util.SetUpFailureHandler.setUpFailureHandler;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.emptyRuleChain;

@RunWith(AndroidJUnit4.class)
public class ComposeTest {

    @NonNull
    private ComposeSteps onCompose = new ComposeSteps();

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private MessageViewSteps onMessageView = new MessageViewSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new FakeAccountRule(FAKE_USER) {
                        @Override
                        public void initialize(@NonNull AccountWrapper account) {
                        }
                    }
            )
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldNavigateBetweenFields() {
        clickOn(composeButton());
        clickOn(toField());
        doImeAction(toField());
        shouldSeeFocusedField(subject(), TIMEOUT_WAIT_FOR_ITEMS, SECONDS);
        doImeAction(subject());
        shouldSeeFocusedField(textBody());
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldKeepFocusOnImeActionForNonEmptyFields() {
        clickOn(composeButton());
        fillInTextField(toField(), ADDRESS_TO);
        doImeAction(toField());
        shouldSeeFocusedField(toField(), TIMEOUT_WAIT_FOR_ITEMS, SECONDS);
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldSelectContactFromSuggest() {
        clickOn(composeButton());
        clickOn(toField());
        onCompose.selectFromSuggestContactWithAddress(ADDRESS_TO);

        shouldSee(yableText(ADDRESS_TO));
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldShowErrorToastWhenSendMessageWithNoRecipients() {
        clickOn(composeButton());
        clickOn(sendButton());
        shouldSeeSnackBarWithText(snackBarNoToError());
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldShowErrorSnackBarWhenSendMessageWithInvalidAddress() {
        clickOn(composeButton());
        fillInTextField(toField(), INVALID_ADDRESS);
        clickOn(sendButton());
        shouldSeeSnackBarWithText(snackBarInvalidEmail());
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldSendLinkInBodyAsLink() {
        clickOn(composeButton());

        fillInTextField(toField(), ADDRESS_TO);
        fillInTextField(subject(), LINK_TO_SEND_IN_MESSAGE);
        fillInTextField(textBody(), LINK_TO_SEND_IN_MESSAGE);

        clickOn(sendButton());
        clickOn(upButton());

        onMessageList.shouldSeeMessageInFolder(LINK_TO_SEND_IN_MESSAGE, SENT_FOLDER)
                .openMessage(LINK_TO_SEND_IN_MESSAGE);

        // todo https://github.yandex-team.ru/mobmail/mobile-yandex-mail-client-android/issues/2646
        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, messageBodyLink(LINK_TO_SEND_IN_MESSAGE));
        shouldSeeText(messageSubject(), LINK_TO_SEND_IN_MESSAGE);
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldSendMessageWithAllYablesTypes() {
        String subject = randomText(10);
        clickOn(composeButton());

        fillInTextField(toField(), ADDRESS_TO);
        clickOn(subject()); // click on anything to form the yable
        clickOn(toField());
        onCompose.selectFromSuggestContactWithAddress(ANOTHER_ADDRESS_TO);

        fillInTextField(subject(), subject);
        clickOn(sendButton());
        clickOn(upButton());

        onMessageList.shouldSeeMessageInFolder(subject, SENT_FOLDER)
                .openMessage(subject);

        onMessageView.expandMessageArrow();
        shouldSee(
                TIMEOUT_WAIT_FOR_ITEMS, SECONDS,
                nameInYable(NAME_FOR_ADDRESS_TO), nameInYable(NAME_FOR_ANOTHER_ADDRESS_TO)
        );
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldSeeFocusedToField() {
        clickOn(composeButton());
        shouldSeeFocusedField(toField(), TIMEOUT_WAIT_FOR_ITEMS, SECONDS);
    }
}
