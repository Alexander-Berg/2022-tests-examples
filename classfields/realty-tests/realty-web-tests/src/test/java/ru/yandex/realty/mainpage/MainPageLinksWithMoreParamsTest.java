package ru.yandex.realty.mainpage;


import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KOMNATA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.ROSSIYA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Owners.TARAS;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.step.UrlSteps.REDIRECT_FROM_RGID;
import static ru.yandex.realty.step.UrlSteps.TRUE_VALUE;

@DisplayName("Главная. Ссылки")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MainPageLinksWithMoreParamsTest {

    private static final String COMMERCIAL_TYPE = "commercialType";
    private static final String FREE_PURPOSE = "FREE_PURPOSE";
    private static final String RETAIL = "RETAIL";
    private static final String BEZ_POSREDNIKOV = "/bez-posrednikov/";
    private static final String POSUTOCHNO = "/posutochno/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(ROSSIYA).queryParam(REDIRECT_FROM_RGID, TRUE_VALUE).open();
        urlSteps.testing().path(MOSKVA).queryParam(REDIRECT_FROM_RGID, TRUE_VALUE);
    }

    @Ignore
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем урл при переходе по ссылке «Рядом с метро»")
    public void shouldSeeLinkWithMetro() {
        basePageSteps.onMainPage().mainBlockNewBuilding().link("Рядом с метро").waitUntil(isDisplayed()).click();
        urlSteps.path(KUPIT).path(NOVOSTROJKA).path("/ryadom-metro/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем урл при переходе по ссылке «Аренда помещения»")
    public void shouldSeeLinkRentWithCommercialTypeFreePurpose() {
        basePageSteps.onMainPage().mainBlock("Коммерческая недвижимость").link("Аренда помещения")
                .waitUntil(isDisplayed()).click();
        urlSteps.path(SNYAT).path(COMMERCIAL).path("/pomeshchenie-svobodnogo-naznacheniya/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(TARAS)
    @DisplayName("Проверяем урл при переходе по ссылке «Аренда торговой площади»")
    public void shouldSeeLinkRentWithCommercialTypeRetail() {
        basePageSteps.onMainPage().mainBlock("Коммерческая недвижимость").link("Аренда торговой площади")
                .waitUntil(isDisplayed()).click();
        urlSteps.path(SNYAT).path(COMMERCIAL).path("/torgovoe-pomeshchenie/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем урл при переходе по ссылке «Купить помещение»")
    public void shouldSeeLinkBuyWithCommercialTypeFreePurpose() {
        basePageSteps.onMainPage().mainBlock("Коммерческая недвижимость").link("Купить помещение")
                .waitUntil(isDisplayed()).click();
        urlSteps.path(KUPIT).path(COMMERCIAL)
                .path("/pomeshchenie-svobodnogo-naznacheniya/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем урл при переходе по ссылке «Снять дом надолго»")
    public void shouldSeeLinWithRentHouse() {
        basePageSteps.onMainPage().mainBlock("Загородная недвижимость").link("Снять дом надолго")
                .waitUntil(isDisplayed()).click();
        urlSteps.path(SNYAT).path(DOM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем урл при переходе по ссылке «Снять квартиру -> Без посредников»")
    public void shouldSeeRentWithoutAgentsLink() {
        basePageSteps.onMainPage().mainBlock("Снять квартиру").link("Без посредников")
                .waitUntil(isDisplayed()).click();
        urlSteps.path(SNYAT).path(KVARTIRA).path(BEZ_POSREDNIKOV).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем урл при переходе по ссылке «Купить квартиру -> Без посредников»")
    public void shouldSeeBuyWithoutAgentsLink() {
        basePageSteps.onMainPage().mainBlock("Купить квартиру").link("Без посредников")
                .waitUntil(isDisplayed()).click();
        urlSteps.path(KUPIT).path(KVARTIRA).path(BEZ_POSREDNIKOV).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем урл при переходе по ссылке «Снять посуточно -> Квартиры»")
    public void shouldSeeRentByDayUrl() {
        basePageSteps.onMainPage().mainBlock("Снять посуточно").link("Квартиры")
                .waitUntil(isDisplayed()).click();
        urlSteps.path(SNYAT).path(KVARTIRA).path(POSUTOCHNO).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем урл при переходе по ссылке «Снять посуточно -> Комнаты»")
    public void shouldSeeRentByDayRoomUrl() {
        basePageSteps.onMainPage().mainBlock("Снять посуточно").link("Комнаты")
                .waitUntil(isDisplayed()).click();
        urlSteps.path(SNYAT).path(KOMNATA).path(POSUTOCHNO).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем урл при переходе по ссылке «Снять посуточно -> Дома»")
    public void shouldSeeRentByDayHouseUrl() {
        basePageSteps.onMainPage().mainBlock("Снять посуточно").link("Дома")
                .waitUntil(isDisplayed()).click();
        urlSteps.path(SNYAT).path(DOM).path(POSUTOCHNO).shouldNotDiffWithWebDriverUrl();
    }
}
