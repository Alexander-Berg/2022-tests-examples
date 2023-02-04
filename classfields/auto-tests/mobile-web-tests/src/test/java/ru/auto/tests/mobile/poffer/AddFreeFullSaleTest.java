package ru.auto.tests.mobile.poffer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mobile.step.PofferSteps;
import ru.auto.tests.desktop.models.MobileOffer;
import ru.auto.tests.desktop.module.MobileDevToolsTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;

import javax.inject.Inject;
import java.io.IOException;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.ADD_OFFER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.requestsMatch;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Добавление бесплатного объявления со всеми возможными полями")
@Epic(BETA_POFFER)
@Feature(ADD_OFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileDevToolsTestsModule.class)
public class AddFreeFullSaleTest {

    private static final String OFFER_TEMPLATE = "offers/beta_cars_user_used_full_offer.json";
    private String currentUrl;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private AccountManager am;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private MobileOffer offer;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() throws IOException {
        Account account = am.create();
        loginSteps.loginAs(account);

        cookieSteps.setExpFlags(EXP_AUTORUFRONT_21494);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).addXRealIP(MOSCOW_IP).open();
        currentUrl = urlSteps.getCurrentUrl();
        basePageSteps.onPofferPage().addOfferNavigateModal().closeIcon().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавление бесплатного объявления со всеми возможными полями")
    public void shouldAddFreeFullSale() {
        pofferSteps.fillAllFields();
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(requestsMatch(
                "/-/ajax/poffer/saveDraftFormsToPublicApi/",
                hasJsonBody(OFFER_TEMPLATE),
                2
        ));

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal("M_USER_CARS_FORM_ADD"),
                currentUrl
        )));
        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal("M_CARS_NEW_FORM_ADD"),
                currentUrl
        )));

        String offerTitle = format("%s %s %s, %s", offer.getMark(), offer.getModel(), offer.getGeneration(), offer.getYear());
        pofferSteps.onCardPage().title()
                .waitUntil("Объявление не добавилось", hasText(offerTitle), 10);
    }

}
