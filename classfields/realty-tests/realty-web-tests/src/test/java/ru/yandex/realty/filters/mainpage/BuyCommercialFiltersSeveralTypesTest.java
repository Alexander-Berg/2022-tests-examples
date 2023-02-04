package ru.yandex.realty.filters.mainpage;

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
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.KVARTIRU_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.TYPE_BUTTON;

/**
 * @author kantemirov
 */
@DisplayName("Главная страница. Фильтры коммерческой недвижимости")
@Feature(MAINFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BuyCommercialFiltersSeveralTypesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA).open();
        user.onMainPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Главная страница. Параметр типа коммерческой недвижимости. Несколько фильтров")
    public void shouldSeeSeveralTypesOfCommercial() {
        user.onMainPage().filters().button(KVARTIRU_BUTTON).click();
        user.onMainPage().filters().selectPopup().item("Коммерческую недвижимость").click();
        user.onMainPage().filters().button(TYPE_BUTTON).click();
        user.onMainPage().filters().selectPopup().item("Офисное помещение").click();
        user.onMainPage().filters().selectPopup().item("Производственное помещение").click();
        user.onMainPage().filters().submitButton().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(COMMERCIAL).queryParam("commercialType", "OFFICE")
                .queryParam("commercialType", "MANUFACTURING").shouldNotDiffWithWebDriverUrl();
    }
}
