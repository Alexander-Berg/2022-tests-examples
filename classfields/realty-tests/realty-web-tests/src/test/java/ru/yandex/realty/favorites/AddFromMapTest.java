package ru.yandex.realty.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FAVORITES;

@DisplayName("Добавление в избранное со страницы карты")
@Feature(FAVORITES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class AddFromMapTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в избранное оффер со страницы карты")
    public void shouldAddFromMap() {
        basePageSteps.resize(1920, 3000);
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().wizardTip().closeButton().clickIf(isDisplayed());
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(5));
        basePageSteps.onMapPage().sidebar().snippetOffer(0).button("В избранное").click();
        String offerId = basePageSteps.getOfferId(basePageSteps.onMapPage().sidebar().snippetOffer(0).link());

        urlSteps.testing().path(Pages.FAVORITES).open();
        String offerIdFromFavoritesPage = basePageSteps.getOfferId(basePageSteps.onFavoritesPage()
                .favoritesList().get(0).offerLink());
        basePageSteps.shouldEqual("Id со страницы избранного и с карты должны совпадать",
                offerId, offerIdFromFavoritesPage);
    }
}
