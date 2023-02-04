package ru.yandex.realty.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.MANAGEMENT_NEW;
import static ru.yandex.realty.element.offers.ServicePayment.PROMOTION;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */

@DisplayName("Скриншот «Продвижения» на странице оффера")
@Feature(MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class PromotionScreenBlockTest {

    private String offerId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private OfferBuildingSteps createOfferSteps;

    @Inject
    private BasePageSteps basePageSteps;


    @Before
    public void openWallet() {
        apiSteps.createVos2Account(account, OWNER);
        compareSteps.resize(1920, 3000);
        offerId = createOfferSteps.addNewOffer(account).withType(APARTMENT_SELL).withSearcherWait().create().getId();
        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.refreshUntil(() ->
                basePageSteps.onOfferCardPage().servicePayment().service(PROMOTION), isDisplayed());
    }

    @Test
    @DisplayName("Сравниваем блок «Продвижение»")
    public void shouldPublishOfferWithCard() {
        Screenshot testingScreenshot = compareSteps.takeScreenshot(
                basePageSteps.onOfferCardPage().servicePayment().service(PROMOTION).should(isDisplayed()));

        urlSteps.production().path(OFFER).path(offerId).open();
        Screenshot productionScreenshot = compareSteps.takeScreenshot(
                basePageSteps.onOfferCardPage().servicePayment().service(PROMOTION).should(isDisplayed()));

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
