package ru.yandex.realty.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.FAVORITES;
import static ru.yandex.realty.element.saleads.ActionBar.ADD_TO_FAV;

/**
 * Created by vicdev on 21.04.17.
 */

@DisplayName("Вид страницы Избранного")
@Feature(FAVORITES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class FavoritesPageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Проверяем, что отображаются добавленные офферы")
    public void shouldSeeOffers() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();

        basePageSteps.onOffersSearchPage().offersList().should(hasSize(greaterThanOrEqualTo(1)));
        String expectedId = basePageSteps.getOfferId(basePageSteps.onOffersSearchPage().offersList().get(0).offerLink());
        basePageSteps.onOffersSearchPage().offersList().get(0).actionBar().buttonWithTitle(ADD_TO_FAV).click();
        basePageSteps.onOffersSearchPage().headerMain().favoritesButton().click();

        basePageSteps.onFavoritesPage().favoritesList().waitUntil(IsCollectionWithSize.hasSize(greaterThanOrEqualTo(1)));
        String actualId = basePageSteps.getOfferId(basePageSteps.onFavoritesPage().favoritesList().get(0).offerLink());
        basePageSteps.shouldEqual("Id двух офферов должны совпадать", expectedId, actualId);
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Проверяем, что отображается надпись об отсутствии офферов в избранном")
    public void shouldNotSeeOffers() {
        urlSteps.testing().path(ru.yandex.realty.consts.Pages.FAVORITES).open();
        basePageSteps.onFavoritesPage().noOffersMessage().should(isDisplayed());
    }
}
