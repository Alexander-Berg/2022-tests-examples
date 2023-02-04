package ru.auto.tests.desktop.lk.wallet;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Кошелёк - пагинация")
@Feature(LK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PagerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop-lk/BillingAutoruPaymentHistoryPage1",
                "desktop-lk/BillingAutoruPaymentHistoryPage2").post();

        urlSteps.testing().path(MY).path(WALLET).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход на следующую страницу по кнопке «Следующая»")
    public void shouldClickNextButton() {
        String payment = basePageSteps.onWalletPage().getPayment(0).getText();
        basePageSteps.onWalletPage().pager().next().click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onWalletPage().paymentsList().waitUntil(hasSize(10));
        basePageSteps.onWalletPage().getPayment(0).should(hasText(not(equalTo(payment))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход на предыдущую страницу по кнопке «Предыдущая»")
    public void shouldClickPrevButton() throws InterruptedException {
        String payment = basePageSteps.onWalletPage().getPayment(1).getText();
        basePageSteps.onWalletPage().pager().next().click();
        TimeUnit.SECONDS.sleep(1);
        basePageSteps.onWalletPage().pager().prev().click();
        TimeUnit.SECONDS.sleep(1);
        basePageSteps.onWalletPage().paymentsList().waitUntil(hasSize(10));
        TimeUnit.SECONDS.sleep(1);
        basePageSteps.onWalletPage().getPayment(1).waitUntil(hasText(equalTo(payment)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход на страницу")
    public void shouldClickPage() {
        String payment = basePageSteps.onWalletPage().getPayment(0).getText();
        basePageSteps.onWalletPage().pager().page("2").click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onWalletPage().paymentsList().waitUntil(hasSize(10));
        basePageSteps.onWalletPage().getPayment(0).should(hasText(not(equalTo(payment))));
    }
}