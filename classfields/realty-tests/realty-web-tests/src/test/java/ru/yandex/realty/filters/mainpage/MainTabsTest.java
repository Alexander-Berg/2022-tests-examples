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

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.DELIVERY_DATE_ITEM;
import static ru.yandex.realty.element.saleads.FiltersBlock.KOMMERCHESKUY_ITEM;
import static ru.yandex.realty.element.saleads.FiltersBlock.KUPIT_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.KVARTIRU_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.NEWBUILDINGS_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.OFIS_ITEM;
import static ru.yandex.realty.element.saleads.FiltersBlock.POSUTOCHO_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.SKLAD_ITEM;
import static ru.yandex.realty.element.saleads.FiltersBlock.SNYAT_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.TO;
import static ru.yandex.realty.element.saleads.FiltersBlock.TYPE_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.YEAR_1_ITEM;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;

@DisplayName("Главная страница. Базовые фильтры.")
@Feature(MAINFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MainTabsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем что специфичный фильтр типа коммерческой недвижимости пропадает")
    public void shouldNotSeeTypeDate() {
        basePageSteps.onMainPage().filters().select(KVARTIRU_BUTTON, "Коммерческую недвижимость");
        basePageSteps.onMainPage().filters().select(TYPE_BUTTON, SKLAD_ITEM);
        basePageSteps.onMainPage().filters().selectButton(NEWBUILDINGS_BUTTON);
        basePageSteps.onMainPage().filters().filtersBlock(DELIVERY_DATE_ITEM).waitUntil(isDisplayed());
        basePageSteps.onMainPage().filters().button(SKLAD_ITEM).should(not(isDisplayed()));
        basePageSteps.onMainPage().filters().filtersBlock(TYPE_BUTTON).should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем что специфичный фильтр срока сдачи пропадает")
    public void shouldNotSeeDeliveryDate() {
        basePageSteps.onMainPage().filters().selectButton(NEWBUILDINGS_BUTTON);
        basePageSteps.onMainPage().filters().select(DELIVERY_DATE_ITEM, YEAR_1_ITEM);
        basePageSteps.onMainPage().filters().selectButton(KUPIT_BUTTON);
        basePageSteps.onMainPage().filters().button(YEAR_1_ITEM).should(not(isDisplayed()));
        basePageSteps.onMainPage().filters().filtersBlock(DELIVERY_DATE_ITEM).should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем что специфичный фильтр коммерческой остается")
    public void shouldSeeSameTypeCommercial() {
        basePageSteps.onMainPage().filters().select(KVARTIRU_BUTTON, KOMMERCHESKUY_ITEM);
        basePageSteps.onMainPage().filters().select(TYPE_BUTTON, OFIS_ITEM);
        basePageSteps.onMainPage().filters().selectButton(SNYAT_BUTTON);
        basePageSteps.onMainPage().filters().filtersBlock(KOMMERCHESKUY_ITEM).should(isDisplayed());
        basePageSteps.onMainPage().filters().filtersBlock(OFIS_ITEM).should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем что цена остается")
    public void shouldSeeSamePrice() {
        basePageSteps.onMainPage().filters().selectButton(NEWBUILDINGS_BUTTON);
        String priceMax = valueOf(getRandomShortInt());
        basePageSteps.onCommercialPage().filters().price().input(TO).sendKeys(priceMax);
        basePageSteps.onMainPage().filters().selectButton(KUPIT_BUTTON);
        basePageSteps.onCommercialPage().filters().price().input(TO).should(hasValue(priceMax));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем что комнатность остается")
    public void shouldSeeSameRooms() {
        basePageSteps.onMainPage().filters().deselectCheckBox("1");
        basePageSteps.onMainPage().filters().deselectCheckBox("2");
        basePageSteps.onMainPage().filters().selectCheckBox("3");
        basePageSteps.onMainPage().filters().selectCheckBox("4+");
        basePageSteps.onMainPage().filters().selectButton(NEWBUILDINGS_BUTTON);
        basePageSteps.onMainPage().filters().checkBox("3").should(isChecked());
        basePageSteps.onMainPage().filters().checkBox("4+").should(isChecked());
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем какие типы недвижимости можно купить")
    public void shouldSeeBuyTypes() {
        basePageSteps.onMainPage().filters().button(KUPIT_BUTTON).should(isDisplayed());
        basePageSteps.onMainPage().filters().button(KVARTIRU_BUTTON).click();
        List<String> items = basePageSteps.onMainPage().filters().selectPopup().items().stream()
                .map(item -> item.getText()).collect(Collectors.toList());
        assertThat(items).containsExactly("Квартиру", "Комнату", "Дом", "Участок", "Гараж или машиноместо",
                "Коммерческую недвижимость");
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем какие типы недвижимости можно снять")
    public void shouldSeeRentTypes() {
        basePageSteps.onMainPage().filters().selectButton(SNYAT_BUTTON);
        basePageSteps.onMainPage().filters().button(KVARTIRU_BUTTON).click();
        List<String> items = basePageSteps.onMainPage().filters().selectPopup().items().stream()
                .map(item -> item.getText()).collect(Collectors.toList());
        assertThat(items).containsExactly("Квартиру", "Комнату", "Дом", "Гараж или машиноместо",
                "Коммерческую недвижимость");
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем какие типы недвижимости можно снять посуточно")
    public void shouldSeeRentByDaysTypes() {
        basePageSteps.onMainPage().filters().selectButton(POSUTOCHO_BUTTON);
        basePageSteps.onMainPage().filters().button(KVARTIRU_BUTTON).click();
        List<String> items = basePageSteps.onMainPage().filters().selectPopup().items().stream()
                .map(item -> item.getText()).collect(Collectors.toList());
        assertThat(items).containsExactly("Квартиру", "Комнату", "Дом");
    }
}
