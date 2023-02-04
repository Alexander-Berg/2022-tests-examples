package ru.auto.tests.desktop.vas;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.RELATED;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("«Предложения дня» в листинге новых")
@Feature(RELATED)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DailyOffersNewListingTest {

    private static final String FIRST_OFFER = "/kia/sportage/21409404/21409408/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/OfferCarsPhones",
                "desktop/SearchCarsNew",
                "desktop/SearchCarsNewTop").post();

        basePageSteps.setWideWindowSize();
        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока «Предложения дня»")
    public void shouldSeeDailyOffers() {
        basePageSteps.onListingPage().verticalDailyOffers().should(hasText("Предложения дня\nKia Sportage\n" +
                "2.4 AT (184 л.с.) 4WD\nLuxe\nот 1 494 900 ₽\nМосква\nKia Cerato\n1.6 AT (128 л.с.)\nLuxe\n" +
                "от 1 006 900 ₽\nМосква\nRenault Kaptur\n2.0 MT (143 л.с.) 4WD\nExtreme\nот 987 500 ₽\nМосква\nОбновить"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по предложению")
    public void shouldClickOffer() {
        basePageSteps.onListingPage().verticalDailyOffers().getItem(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(FIRST_OFFER).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Обновить»")
    public void shouldClickUpdateButton() {
        String firstOfferUrl = basePageSteps.onListingPage().verticalDailyOffers().getItem(0).url()
                .getAttribute("href");
        basePageSteps.onListingPage().verticalDailyOffers().updateButton().should(isDisplayed()).click();
        basePageSteps.onListingPage().verticalDailyOffers().getItem(0).url()
                .waitUntil(not(hasAttribute("href", firstOfferUrl)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке показа телефонов")
    public void shouldClickShowPhonesButton() {
        basePageSteps.onListingPage().verticalDailyOffers().getItem(0).showPhonesButton().click();
        basePageSteps.onListingPage().contactsPopup().phonesList().should(hasSize(2)).get(0)
                .should(hasText("+7 916 039-84-27"));
        basePageSteps.onListingPage().contactsPopup().getPhone(1).should(hasText("+7 916 039-84-28"));
    }
}
