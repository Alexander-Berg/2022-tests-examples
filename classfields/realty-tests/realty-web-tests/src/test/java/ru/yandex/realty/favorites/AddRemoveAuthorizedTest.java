package ru.yandex.realty.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.element.saleads.NewListingOffer;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.GARAZH;
import static ru.yandex.realty.consts.Filters.KOMNATA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Filters.UCHASTOK;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FAVORITES;
import static ru.yandex.realty.element.saleads.ActionBar.ADD_TO_FAV;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Добавление в избранное для авторизованного пользователя")
@Feature(FAVORITES)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddRemoveAuthorizedTest {

    private final static int COUNT_OF_OFFERS_TO_COMPARE = 2;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Inject
    private PassportSteps passport;

    @Parameterized.Parameter
    public String buyRent;

    @Parameterized.Parameter(1)
    public String typeOfRealty;

    @Parameterized.Parameters(name = "{0}-{1}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {KUPIT, KVARTIRA},
                {KUPIT, DOM},
                {KUPIT, UCHASTOK},
                {KUPIT, KOMNATA},
                {KUPIT, GARAZH},
                {KUPIT, COMMERCIAL},
                {SNYAT, KVARTIRA},
                {SNYAT, KOMNATA},
                {SNYAT, GARAZH},
                {SNYAT, DOM},
                {SNYAT, COMMERCIAL}
        });
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Добавляем в избранное два оффера, переходим на страницу избранного. Проверяем, что они отобразились")
    @Description("Параметр 'купить/снять' и тип жилья")
    public void shouldSeeOffers() {
        apiSteps.createVos2Account(account, OWNER);
        urlSteps.testing().path(MOSKVA).path(buyRent).path(typeOfRealty).open();

        List<String> listOfOfferIdsFromSaleAdsPage = basePageSteps.onOffersSearchPage().offersList()
                .should(hasSize(greaterThanOrEqualTo(COUNT_OF_OFFERS_TO_COMPARE))).stream()
                .peek(offer -> offer.actionBar().buttonWithTitle(ADD_TO_FAV).click())
                .map(NewListingOffer::offerLink)
                .map(offerLink -> basePageSteps.getOfferId(offerLink))
                .limit(COUNT_OF_OFFERS_TO_COMPARE)
                .collect(toList());

        basePageSteps.onOffersSearchPage().headerMain().favoritesButton().click();
        basePageSteps.onFavoritesPage().favoritesList()
                .waitUntil("Ждём пока отрисуются избранные офферы", hasSize(COUNT_OF_OFFERS_TO_COMPARE), 30);

        passport.logoff();
        urlSteps.testing().path(Pages.FAVORITES).open();
        basePageSteps.onFavoritesPage().noOffersMessage().should(isDisplayed());

        passport.login(account);
        urlSteps.testing().path(Pages.FAVORITES).open();
        List<String> listOfOfferIdsFromFavoritesPage = basePageSteps.onFavoritesPage()
                .favoritesList().stream().map(offer -> basePageSteps.getOfferId(offer.offerLink()))
                .collect(toList());

        basePageSteps.shouldMatchLists(listOfOfferIdsFromSaleAdsPage, listOfOfferIdsFromFavoritesPage);
    }
}
