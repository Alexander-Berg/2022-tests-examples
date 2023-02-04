package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Screenshooter;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.MANAGEMENT_NEW;
import static ru.yandex.realty.page.OfferAddPage.NORMAL_SALE;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Скриншот «Продвижения» при публикации")
@Feature(MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class ScreenPaymentBlockOnCreateTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private Account account;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void openWallet() {
        apiSteps.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellFlat();
        offerAddSteps.onOfferAddPage().publishBlock().sellTab(NORMAL_SALE).click();
    }

    @Test
    @Owner(KURAU)
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @DisplayName("Сравниваем блок «Продвижение»")
    public void shouldPublishOfferWithCard() {
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(
                offerAddSteps.onOfferAddPage().publishBlock().paySelector("Продвижение").should(isDisplayed()));

        urlSteps.production().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellFlat();
        offerAddSteps.onOfferAddPage().publishBlock().sellTab(NORMAL_SALE).click();
        Screenshot productionScreenshot = compareSteps.getElementScreenshot(
                offerAddSteps.onOfferAddPage().publishBlock().paySelector("Продвижение").should(isDisplayed()));

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
