package ru.yandex.realty.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.FAVORITES;
import static ru.yandex.realty.mobile.element.listing.TouchOffer.ADD_TO_FAVORITES;
import static ru.yandex.realty.mobile.element.listing.TouchOffer.DELETE_FROM_FAVORITES;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@Issue("VERTISTEST-1355")
@Epic(RealtyFeatures.FAVORITES)
@DisplayName("Удаление из избранного")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class RemoveFavoritesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void before() {
        api.createYandexAccount(account);
        basePageSteps.resize(400, 2000);
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path("/vtorichniy-rynok/").open();
        basePageSteps.onMobileSaleAdsPage().offer(1).buttonWithTitle(ADD_TO_FAVORITES).click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаление оффера из избранного")
    public void shouldDeleteFromFavorites() {
        basePageSteps.onMobileSaleAdsPage().offer(2).buttonWithTitle(ADD_TO_FAVORITES).click();

        urlSteps.testing().path(FAVORITES).open();
        String offerId = basePageSteps.onFavoritesPage().offersList().get(FIRST).getOfferId();
        basePageSteps.onFavoritesPage().offersList().get(FIRST).buttonWithTitle(DELETE_FROM_FAVORITES).click();

        basePageSteps.onFavoritesPage().offersList().should(hasSize(1));
        retrofitApiSteps.checkItemNotInFavorites(account.getId(), offerId);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаление последнего оффера из избранного")
    public void shouldDeleteLastOfferFromFavorites() {
        urlSteps.testing().path(FAVORITES).open();
        String offerId = basePageSteps.onFavoritesPage().offersList().get(FIRST).getOfferId();
        basePageSteps.onFavoritesPage().offersList().get(FIRST).buttonWithTitle(DELETE_FROM_FAVORITES).click();

        basePageSteps.onFavoritesPage().offersList().should(not(isDisplayed()));
        retrofitApiSteps.checkItemNotInFavorites(account.getId(), offerId);
    }

}
