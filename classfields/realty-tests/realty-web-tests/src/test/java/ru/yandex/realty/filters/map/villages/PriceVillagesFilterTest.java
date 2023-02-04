package ru.yandex.realty.filters.map.villages;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.PRICE_FROM;
import static ru.yandex.realty.element.saleads.FiltersBlock.TO;

@DisplayName("Карта. Фильтр поиска по коттеджным поселкам.")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PriceVillagesFilterTest {

    private static final String EMPTY = "";
    private static final String PRICE_MAX = "priceMax";
    private static final String PRICE_MIN = "priceMin";
    private static final int MILLION = 1000000;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Цена от»")
    public void shouldSeePriceFrom() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).open();
        String priceMin = valueOf(getRandomShortInt() * MILLION);
        basePageSteps.onMapPage().filters().price().input(PRICE_FROM, priceMin);
        basePageSteps.onMapPage().filters().submitWithWait();
        urlSteps.queryParam(PRICE_MIN, priceMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Цена до»")
    public void shouldSeePriceTo() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        String priceMax = valueOf(getRandomShortInt() * MILLION);
        basePageSteps.onMapPage().filters().price().input(TO, priceMax);
        basePageSteps.onMapPage().filters().submitWithWait();
        urlSteps.queryParam(PRICE_MAX, priceMax).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Цена от» должен оттображаться ппри переходе по урлу")
    public void shouldSeePriceFromEnteredValue() {
        String priceMin = valueOf(getRandomShortInt() * MILLION);
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).queryParam(PRICE_MIN, priceMin)
                .path(KARTA).open();
        String actual = basePageSteps.onMapPage().filters().price().input(PRICE_FROM).getAttribute("value");
        assertThat(actual.replaceAll("\u00a0", EMPTY)).isEqualTo(priceMin);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Цена до» должен оттображаться при переходе по урлу")
    public void shouldSeePriceToEnteredValue() {
        String priceMax = valueOf(getRandomShortInt() * MILLION);
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).queryParam(PRICE_MAX, priceMax)
                .path(KARTA).open();
        String actual = basePageSteps.onMapPage().filters().price().input(TO).getAttribute("value");
        assertThat(actual.replaceAll("\u00a0", EMPTY)).isEqualTo(priceMax);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Очищаем поле «Цена от»")
    public void shouldSeePriceFromClear() {
        String priceMin = valueOf(getRandomShortInt() * MILLION);
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).queryParam(PRICE_MIN, priceMin)
                .path(KARTA).open();
        basePageSteps.moveCursor(basePageSteps.onMapPage().filters().price().input(PRICE_FROM));
        basePageSteps.onMapPage().filters().price().clearSign(PRICE_FROM).click();
        basePageSteps.onMapPage().filters().submitWithWait();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Очищаем поле «Цена до»")
    public void shouldSeePriceToClear() {
        String priceMax = valueOf(getRandomShortInt() * MILLION);
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).queryParam(PRICE_MAX, priceMax)
                .path(KARTA).open();
        basePageSteps.moveCursor(basePageSteps.onMapPage().filters().price().input(TO));
        basePageSteps.onMapPage().filters().price().clearSign(TO).click();
        basePageSteps.onMapPage().filters().submitWithWait();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поле «Цена от» очищается если «Цена до» меньше")
    public void shouldSeePriceFromAutoClear() {
        String priceMin = valueOf(getRandomShortInt() * MILLION);
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).queryParam(PRICE_MIN, priceMin)
                .path(KARTA).open();
        basePageSteps.onMapPage().filters().price().input(TO, valueOf(Integer.parseInt(priceMin) - 1));
        basePageSteps.onMapPage().filters().submitWithWait();
        basePageSteps.onMapPage().filters().price().input(PRICE_FROM).should(hasValue(EMPTY));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поле «Цена до» очищается если «Цена от» больше")
    public void shouldSeePriceToAutoClear() {
        String priceMax = valueOf(getRandomShortInt() * MILLION);
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).queryParam(PRICE_MAX, priceMax)
                .path(KARTA).open();
        basePageSteps.onMapPage().filters().price().input(PRICE_FROM, valueOf(Integer.parseInt(priceMax) + 1));
        basePageSteps.onMapPage().filters().submitWithWait();
        basePageSteps.onMapPage().filters().price().input(TO).should(hasValue(EMPTY));
    }
}
