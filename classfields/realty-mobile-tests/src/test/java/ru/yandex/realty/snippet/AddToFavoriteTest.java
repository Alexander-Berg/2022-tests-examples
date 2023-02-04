package ru.yandex.realty.snippet;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.SNIPPET;
import static ru.yandex.realty.mobile.element.listing.TouchOffer.ADD_TO_FAVORITES;
import static ru.yandex.realty.mobile.element.listing.TouchOffer.DELETE_FROM_FAVORITES;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@Issue("VERTISTEST-1352")
@Feature(SNIPPET)
@DisplayName("Избранные офферы")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class AddToFavoriteTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Before
    public void before() {
        api.createYandexAccount(account);
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавление в избранное")
    public void shouldAddFavoriteOffer() {
        basePageSteps.onMobileSaleAdsPage().offer(FIRST).buttonWithTitle(ADD_TO_FAVORITES).click();
        String offerId = basePageSteps.onMobileSaleAdsPage().getOfferId(FIRST);

        retrofitApiSteps.checkItemInFavorites(account.getId(), offerId);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем из избранного")
    public void shouldDeleteFavoriteOffer() {
        basePageSteps.onMobileSaleAdsPage().offer(FIRST).buttonWithTitle(ADD_TO_FAVORITES).click();
        String offerId = basePageSteps.onMobileSaleAdsPage().getOfferId(FIRST);
        retrofitApiSteps.checkItemInFavorites(account.getId(), offerId);
        basePageSteps.onMobileSaleAdsPage().offer(FIRST).buttonWithTitle(DELETE_FROM_FAVORITES).click();

        retrofitApiSteps.checkItemNotInFavorites(account.getId(), offerId);
    }

}
