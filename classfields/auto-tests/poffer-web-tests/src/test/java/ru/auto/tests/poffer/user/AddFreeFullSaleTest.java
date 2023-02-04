package ru.auto.tests.poffer.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.models.Offer;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.element.poffer.beta.BetaFreeVasBlock.FREE_SUBMIT_TEXT;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.pofferHasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasQuery;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.auto.tests.desktop.consts.Metrics.PAGE_REF;
import static ru.auto.tests.desktop.consts.Metrics.PAGE_URL;
import static ru.auto.tests.desktop.step.SeleniumMockSteps.queryPair;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Добавление бесплатного объявления со всеми возможными полями")
@Feature(BETA_POFFER)
@Story("Частник")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class AddFreeFullSaleTest {

    private static final String OFFER_TEMPLATE = "offers/beta/beta_cars_user_used_full_offer.json";
    private String currentUrl;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BetaPofferSteps newPofferSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private AccountManager am;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Inject
    private Offer offer;

    @Before
    public void before() throws IOException {
        Account account = am.create();
        loginSteps.loginAs(account);

        urlSteps.testing().path(CARS).path(USED).path(ADD).addXRealIP(MOSCOW_IP).open();
        currentUrl = urlSteps.getCurrentUrl();

        newPofferSteps.fillAllFields();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавление бесплатного объявления со всеми возможными полями")
    public void shouldAddFreeFullSale() {
        newPofferSteps.onBetaPofferPage().vasBlock().free().submitButton().waitUntil(hasText(FREE_SUBMIT_TEXT));
        newPofferSteps.hideElement(newPofferSteps.onBetaPofferPage().supportFloatingButton());

        waitSomething(10, TimeUnit.SECONDS);
        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveDraftFormsToPublicApi/",
                pofferHasJsonBody(OFFER_TEMPLATE))
        );

        newPofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal("USER_CARS_FORM_ADD"),
                currentUrl
        )));
        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal("CARS_NEW_FORM_ADD"),
                currentUrl
        )));

        String offerTitle = format("%s %s %s", offer.getMark(), offer.getModel(), offer.getGeneration());
        newPofferSteps.onCardPage().cardHeader().title()
                .should("Объявление не добавилось", hasText(offerTitle), 10);
    }
}
