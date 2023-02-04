package ru.yandex.realty.newbuilding.snippet;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING;
import static ru.yandex.realty.consts.RealtyFeatures.SNIPPET;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.NEW_FLAT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;

@Issue("VERTISTEST-1352")
@Epic(NEWBUILDING)
@Feature(SNIPPET)
@DisplayName("Избранные новостройки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class AddToFavoriteNewbuildingTest {

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
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавление в избранное")
    public void shouldAddFavoriteSite() {
        basePageSteps.onNewBuildingPage().site(FIRST).favorite().click();
        String siteId = basePageSteps.onNewBuildingPage().getSiteId(FIRST);

        retrofitApiSteps.checkItemInFavorites(account.getId(), siteId);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем из избранного")
    public void shouldDeleteFavoriteSite() {
        basePageSteps.onNewBuildingPage().site(FIRST).favorite().click();
        String siteId = basePageSteps.onNewBuildingPage().getSiteId(FIRST);
        retrofitApiSteps.checkItemInFavorites(account.getId(), siteId);
        basePageSteps.onNewBuildingPage().subscriptionModal().close().click();
        basePageSteps.onNewBuildingPage().site(FIRST).favorite().click();

        retrofitApiSteps.checkItemNotInFavorites(account.getId(), siteId);
    }

}
