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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.DELIVERY_DATE_ITEM;
import static ru.yandex.realty.element.saleads.FiltersBlock.DOM_ITEM;
import static ru.yandex.realty.element.saleads.FiltersBlock.FINISHED_ITEM;
import static ru.yandex.realty.element.saleads.FiltersBlock.GARAGE_ITEM;
import static ru.yandex.realty.element.saleads.FiltersBlock.KOMMERCHESKUY_ITEM;
import static ru.yandex.realty.element.saleads.FiltersBlock.KUPIT_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.KVARTIRU_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.NEWBUILDINGS_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.POSUTOCHO_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.SNYAT_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.TYPE_BUTTON;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;

@DisplayName("Главная страница. Базовые фильтры.")
@Feature(MAINFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MainTabsScreenShotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void openSaleAdsPage() {
        basePageSteps.disableAd();
        urlSteps.testing().path(MOSKVA).open();
        compareSteps.resize(1600, 3000);
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот с блоком «Купить»")
    public void shouldSeeBuyFilters() {
        basePageSteps.onMainPage().filters().button(KUPIT_BUTTON).waitUntil(isChecked());
        basePageSteps.onMainPage().filters().filtersBlock(KVARTIRU_BUTTON).waitUntil(isDisplayed());
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMainPage().welcomeBlock());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMainPage().welcomeBlock());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот с блоком «Купить коммерческую»")
    public void shouldSeeBuyCommercialFilters() {
        basePageSteps.onMainPage().filters().select(KVARTIRU_BUTTON, KOMMERCHESKUY_ITEM);
        basePageSteps.onMainPage().filters().filtersBlock(TYPE_BUTTON).waitUntil(isDisplayed());
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMainPage().welcomeBlock());
        urlSteps.setProductionHost().open();
        basePageSteps.onMainPage().filters().select(KVARTIRU_BUTTON, KOMMERCHESKUY_ITEM);
        basePageSteps.onMainPage().filters().filtersBlock(TYPE_BUTTON).waitUntil(isDisplayed());
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMainPage().welcomeBlock());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот с блоком «Снять гараж»")
    public void shouldSeeRentGarageFilters() {
        basePageSteps.onMainPage().filters().button(SNYAT_BUTTON).clickWhile(isChecked());
        basePageSteps.onMainPage().filters().select(KVARTIRU_BUTTON, GARAGE_ITEM);
        basePageSteps.onMainPage().filters().filtersBlock(TYPE_BUTTON).waitUntil(isDisplayed());
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMainPage().welcomeBlock());
        urlSteps.setProductionHost().open();
        basePageSteps.onMainPage().filters().button(SNYAT_BUTTON).clickWhile(isChecked());
        basePageSteps.onMainPage().filters().select(KVARTIRU_BUTTON, GARAGE_ITEM);
        basePageSteps.onMainPage().filters().filtersBlock(TYPE_BUTTON).waitUntil(isDisplayed());
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMainPage().welcomeBlock());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот с блоком «Посуточно дом»")
    public void shouldSeePosutochnoDomFilters() {
        basePageSteps.onMainPage().filters().button(POSUTOCHO_BUTTON).clickWhile(isChecked());
        basePageSteps.onMainPage().filters().select(KVARTIRU_BUTTON, DOM_ITEM);
        basePageSteps.onMainPage().filters().filtersBlock(TYPE_BUTTON).waitUntil(isDisplayed());
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMainPage().welcomeBlock());
        urlSteps.setProductionHost().open();
        basePageSteps.onMainPage().filters().button(POSUTOCHO_BUTTON).clickWhile(isChecked());
        basePageSteps.onMainPage().filters().select(KVARTIRU_BUTTON, DOM_ITEM);
        basePageSteps.onMainPage().filters().filtersBlock(TYPE_BUTTON).waitUntil(isDisplayed());
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMainPage().welcomeBlock());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот с блоком «Новостройка сдана»")
    public void shouldSeeNewBuildingFinishedFilters() {
        basePageSteps.onMainPage().filters().button(NEWBUILDINGS_BUTTON).clickWhile(isChecked());
        basePageSteps.onMainPage().filters().button(KVARTIRU_BUTTON).waitUntil(not(isDisplayed()));
        basePageSteps.onMainPage().filters().select(DELIVERY_DATE_ITEM, FINISHED_ITEM);
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMainPage().welcomeBlock());
        urlSteps.setProductionHost().open();
        basePageSteps.onMainPage().filters().button(NEWBUILDINGS_BUTTON).clickWhile(isChecked());
        basePageSteps.onMainPage().filters().button(KVARTIRU_BUTTON).waitUntil(not(isDisplayed()));
        basePageSteps.onMainPage().filters().select(DELIVERY_DATE_ITEM, FINISHED_ITEM);
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMainPage().welcomeBlock());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот с блоком «Купить дом»")
    public void shouldSeeBuyHouseFilters() {
        basePageSteps.onMainPage().filters().select(KVARTIRU_BUTTON, DOM_ITEM);
        basePageSteps.onMainPage().filters().button(TYPE_BUTTON).waitUntil(isDisplayed());
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMainPage().welcomeBlock());
        urlSteps.setProductionHost().open();
        basePageSteps.onMainPage().filters().select(KVARTIRU_BUTTON, DOM_ITEM);
        basePageSteps.onMainPage().filters().button(TYPE_BUTTON).waitUntil(isDisplayed());
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMainPage().welcomeBlock());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
