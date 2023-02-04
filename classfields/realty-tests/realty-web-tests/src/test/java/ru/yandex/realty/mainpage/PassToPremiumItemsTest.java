package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.page.BasePage.PAGE_NOT_FOUND;

@DisplayName("Главная. Премиум переходы")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PassToPremiumItemsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.setMoscowCookie();
        urlSteps.testing().open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход в премиум новостройку")
    public void shouldSeePassToPremiumNewBuilding() {
        String link = basePageSteps.onMainPage().premiumNewBuilding()
                .get(0).link().getAttribute("href");
        basePageSteps.onMainPage().premiumNewBuilding().get(0).link().click();
        basePageSteps.switchToNextTab();
        urlSteps.fromUri(link).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход в премиум оффер")
    public void shouldSeePassToPremiumOffer() {
        String link = basePageSteps.onMainPage().premiumOffer().get(0).link().getAttribute("href");
        basePageSteps.onMainPage().premiumOffer().get(0).link().click();
        basePageSteps.switchToNextTab();
        urlSteps.fromUri(link).ignoreParam(UrlSteps.IS_EXACT_URL_PARAM).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onNewBuildingSitePage().errorPage(PAGE_NOT_FOUND).should(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход в «Как сюда попасть?»")
    public void shouldSeePassToHowTo() {
        basePageSteps.onMainPage().link("Как сюда попасть?")
                .should(hasHref(equalTo(urlSteps.testing().path("/promotion/").fragment("vas").toString())));
    }
}
