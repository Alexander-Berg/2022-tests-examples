package ru.yandex.realty.favorites;

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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FAVORITES;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Добавление/удаление из избранного без авторизации")
@Feature(FAVORITES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class AddRemoveTest {

    private final static int COUNT_OF_OFFERS_TO_COMPARE = 2;
    private static final String ADD_TO_FAV = "Добавить в избранное";
    private static final String DEL_FROM_FAV = "Удалить из избранного";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Owner(KURAU)
    @DisplayName("Добавляем оффер. Удаляем его через апи. Проверяем, что «Объявление устарело»")
    public void shouldNotSeeDeletedOffer() {
        apiSteps.createVos2AccountWithoutLogin(account, OWNER);
        String id = offerBuildingSteps.addNewOffer(account).withSearcherWait().create().getId();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam("offerId", id).open();
        basePageSteps.onOfferCardPage().offerCardSummary().addToFavButton().click();
        urlSteps.testing().path(Pages.FAVORITES).open();
        basePageSteps.onFavoritesPage().favoritesList().should(hasSize(1));
        apiSteps.deleteOffer(account, id);
        apiSteps.waitOfferDeleted(account.getId(), id);
        basePageSteps.refreshUntil(() -> {
            apiSteps.waitSearcherOfferStatusInactive(id);
            return basePageSteps.onFavoritesPage().offer(FIRST).soldMessage();
        }, isDisplayed(), 240);
        basePageSteps.onFavoritesPage().offer(FIRST).soldMessage().should(hasText("Снято менее часа назад"));
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Проверяем, что отображается надпись об отсутствии офферов в избранном после удаления всех офферов")
    public void shouldNotSeeOffers() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().offersList()
                .should(hasSize(greaterThanOrEqualTo(COUNT_OF_OFFERS_TO_COMPARE)));
        basePageSteps.onOffersSearchPage().offersList().get(0).actionBar().buttonWithTitle(ADD_TO_FAV).click();
        basePageSteps.onOffersSearchPage().offersList().get(1).actionBar().buttonWithTitle(ADD_TO_FAV).click();
        basePageSteps.onOffersSearchPage().headerMain().favoritesButton().click();

        basePageSteps.onFavoritesPage().favoritesList().should(hasSize(COUNT_OF_OFFERS_TO_COMPARE));
        basePageSteps.onFavoritesPage().favoritesList().get(1).actionBar().buttonWithTitle(DEL_FROM_FAV)
                .waitUntil(isDisplayed()).click();
        basePageSteps.onFavoritesPage().favoritesList().get(0).actionBar().buttonWithTitle(DEL_FROM_FAV)
                .waitUntil(isDisplayed()).click();

        basePageSteps.refresh();
        basePageSteps.onFavoritesPage().noOffersMessage().should(isDisplayed());
    }
}
