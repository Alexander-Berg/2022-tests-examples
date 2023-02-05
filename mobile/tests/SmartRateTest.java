package com.yandex.mail.tests;

import android.content.Intent;

import com.yandex.mail.pages.Account;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.GooglePlayInterceptorRule;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.IntentDisplayerSteps;
import com.yandex.mail.steps.SmartRateSteps;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.util.AccountsConst;
import com.yandex.mail.util.IntentFactory;
import com.yandex.mail.util.SetUpFailureHandler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class SmartRateTest {

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(new GooglePlayInterceptorRule())
            .around(new LoginToAppRule(new Account(AccountsConst.USER_LOGIN, AccountsConst.USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @NonNull
    private final SmartRateSteps onSmartRate = new SmartRateSteps();

    @NonNull
    private final IntentDisplayerSteps onIntentDisplayer = new IntentDisplayerSteps();

    @Test
    @BusinessLogic
    public void shouldOpenGooglePlay() {
        onSmartRate
                .showSmartRateDialog()
                .setRating(5.0f)
                .assertThatRateButtonEnabledAndVisible()
                .clickOnRateButton();

        onIntentDisplayer
                .assertThatIntentActionEqualTo(Intent.ACTION_VIEW)
                .assertThatIntentDataUriEqualTo(IntentFactory.getDirectGooglePlayUri(InstrumentationRegistry.getTargetContext()));
    }
}
