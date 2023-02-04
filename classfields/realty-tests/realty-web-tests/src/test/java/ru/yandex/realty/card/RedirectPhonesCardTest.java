package ru.yandex.realty.card;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.REDIRECT_PHONES;
import static ru.yandex.realty.step.OfferBuildingSteps.getDefaultOffer;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by vicdev on 11.08.17.
 */
@DisplayName("Карточка объявления. Подменные номера на карточке")
@Feature(REDIRECT_PHONES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class RedirectPhonesCardTest {

    @Inject
    private Account account;

    @Inject
    private ApiSteps api;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferBuildingSteps buildingSteps;

    @Inject
    private PassportSteps passport;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Owner(KOPITSA)
    @DisplayName("Просмотр объявления с подменником в карточке автора")
    public void shouldCompareRedirectPhonesPopup() {
        api.createVos2Account(account, OWNER);
        String id = buildingSteps.addNewOffer(account).withBody(getDefaultOffer(APARTMENT_SELL)
                .withRedirectPhones(true)).create().getId();
        passport.login(account);
        urlSteps.testing().path(OFFER).path(id).open();
        basePageSteps.refreshUntil(() -> basePageSteps.onOfferCardPage().iconPhoneProtect(), isDisplayed());
        compareSteps.compareScreenshots(urlSteps, basePageSteps.onOfferCardPage().iconPhoneProtect());
    }
}
