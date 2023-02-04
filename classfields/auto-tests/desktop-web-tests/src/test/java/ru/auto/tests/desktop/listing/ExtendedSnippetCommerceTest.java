package ru.auto.tests.desktop.listing;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - расширенный сниппет дилера в комТС")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ExtendedSnippetCommerceTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchTrucksBreadcrumbs",
                "desktop/SearchTrucksExtended",
                "desktop/OfferTrucksPhones").post();

        urlSteps.testing().path(MOSKVA).path(TRUCK).path(ALL)
                .addParam("autoru_billing_service_type", "extended").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение сниппета")
    public void shouldSeeSnippet() {
        basePageSteps.onListingPage().getSale(0).should(hasText(matchesPattern("Hyundai HD78\nтент\nг/п 4.3 т\n3.9 л / " +
                "140 л.с. / Дизель\n3-х местная без спального\n4x2\nбелый\nмеханика\n1 799 000 ₽\n" +
                "2017\n58 378 км\nТрак-Сток\nМосква\nНа Авто.ру \\d+ (год|года|лет)\n" +
                "165 т.с. в наличии\nПоказать телефон")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов")
    public void shouldSeePhones() {
        basePageSteps.onListingPage().getSale(0).showPhonesButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().contactsPopup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «На Авто.ру с N года»")
    public void shouldClickAutoruYearUrl() {
        basePageSteps.onListingPage().getSale(0).autoruYearUrl().should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DILER_OFICIALNIY).path(TRUCK).path(ALL).path("/avilon_bmw_belaya_dacha_kotelniki/")
                .addParam("from", "auto-snippet").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «В наличии N т.с.»")
    public void shouldClickInStockUrl() {
        basePageSteps.onListingPage().getSale(0).inStockUrl().should(isDisplayed()).hover().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DILER_OFICIALNIY).path(TRUCK).path(ALL).path("/avilon_bmw_belaya_dacha_kotelniki/")
                .addParam("from", "auto-snippet").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по названию дилера")
    public void shouldClickDealerUrl() {
        basePageSteps.onListingPage().getSale(0).dealerUrl().should(isDisplayed()).hover().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DILER_OFICIALNIY).path(TRUCK).path(ALL).path("/avilon_bmw_belaya_dacha_kotelniki/")
                .addParam("from", "auto-snippet").shouldNotSeeDiff();
    }
}