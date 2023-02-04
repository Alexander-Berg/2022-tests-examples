package ru.yandex.realty.mappage;

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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;
import static ru.yandex.realty.element.map.Sidebar.ADD_TO_COMPARISON;
import static ru.yandex.realty.element.map.Sidebar.BACK_TO_FILTERS;
import static ru.yandex.realty.element.map.Sidebar.IN_FAVORITE;
import static ru.yandex.realty.element.map.Sidebar.SEE_ONLY_YOU;
import static ru.yandex.realty.element.map.Sidebar.SHOW_PHONE;
import static ru.yandex.realty.element.map.Sidebar.TO_FAVORITE;
import static ru.yandex.realty.element.map.Sidebar.YOUR_NOTE;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.IS_EXACT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.NEW_FLAT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.NO_VALUE;

@DisplayName("Карта. Общее. Сниппет смешанный")
@Feature(MAP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SnippetOfferTest {

    private static final int MAP_OFFER = 3;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        compareSteps.resize(1600, 1800);
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(NEW_FLAT_URL_PARAM, NO_VALUE).open();
        basePageSteps.onMapPage().filters().waitUntil(isDisplayed());
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(MAP_OFFER));
        basePageSteps.onMapPage().filters().waitUntil(not(isDisplayed()), 20);
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход с сайдбара обратно к фильтрам")
    public void shouldSeeBackToFilters() {
        basePageSteps.onMapPage().sidebar().spanLink(BACK_TO_FILTERS).click();
        basePageSteps.onMapPage().sidebar().spanLink(BACK_TO_FILTERS).waitUntil(not(isDisplayed()));
        basePageSteps.onMapPage().filters().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход на карточку оффера")
    public void shouldSeePassToOfferCard() {
        String offerId = basePageSteps.getOfferId(basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).link());
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).link().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(OFFER).path(offerId).path("/").ignoreParam(IS_EXACT_URL_PARAM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот попапа «Показать телефон»")
    public void shouldSeeShowPhoneButton() {
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).button(SHOW_PHONE).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMapPage().showPhonePopup());
        urlSteps.setProductionHost().open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(MAP_OFFER));
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).button(SHOW_PHONE).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMapPage().showPhonePopup());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем «Добавить в избранное»")
    public void shouldSeeAddToFavorite() {
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).button(TO_FAVORITE).click();
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).button(IN_FAVORITE).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем «Добавить в избранное» -> видим попап с переходом в избранное")
    public void shouldSeePassToFavoritePage() {
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).button(TO_FAVORITE).click();
        basePageSteps.onMapPage().openedPopup().link().should(hasHref(containsString("/favorites/")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Убираем из избранного")
    public void shouldSeeRemoveFromFavorite() {
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).button(TO_FAVORITE).click();
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).button(IN_FAVORITE).click();
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).button(TO_FAVORITE).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем «Добавить к сравнению»")
    public void shouldSeeAddToComparison() {
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).buttonWithTitle(ADD_TO_COMPARISON).click();
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).removeFromComparison().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Убираем из сравнения")
    public void shouldSeeRemoveFromComparison() {
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).buttonWithTitle(ADD_TO_COMPARISON).click();
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).removeFromComparison().click();
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).buttonWithTitle(ADD_TO_COMPARISON).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем «Добавить заметку»")
    public void shouldSeeAddNote() {
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).buttonWithTitle(YOUR_NOTE).click();
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).input(SEE_ONLY_YOU).should(isDisplayed());
    }

    @Ignore("тест на переход по ссылке в сешанной выдаче в жк")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переходим в жк")
    public void shouldSeeJk() {
    }
}
