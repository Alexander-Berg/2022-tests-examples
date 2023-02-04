package ru.auto.tests.desktop.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.HISTORY_DISCOUNT;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.consts.QueryParams.SEARCH_TAG;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок с изменением цены в листинге")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PriceHistoryListingTest {

    private static final String POPUP_TEXT = "900 000 ₽\n12 063 $\n · \n10 151 €\nот 17 358 ₽ / мес.\n" +
            "7 марта 2021\nНачальная цена\n1 100 000 ₽\n18 марта 2021\n- 200 000 ₽\n900 000 ₽\n" +
            "О скидках и акциях узнавайте по телефону";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SearchCarsAllHistoryDiscount")
        ).create();

    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа с историей цен")
    public void shouldSeePriceHistoryPopup() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam(SEARCH_TAG, HISTORY_DISCOUNT).open();

        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).price().priceDownIcon().hover();

        basePageSteps.onListingPage().popup().waitUntil(isDisplayed()).should(hasText(POPUP_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение поп-апа с историей цен, тип листинга «Карусель»")
    public void shouldSeePriceHistoryPopupCarousel() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam(SEARCH_TAG, HISTORY_DISCOUNT)
                .addParam(OUTPUT_TYPE, CAROUSEL).open();

        basePageSteps.onListingPage().getCarouselSale(0).hover();
        basePageSteps.onListingPage().getCarouselSale(0).price().priceDownIcon().hover();

        basePageSteps.onListingPage().popup().waitUntil(isDisplayed()).should(hasText(POPUP_TEXT));
    }

}
