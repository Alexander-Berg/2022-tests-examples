package ru.yandex.general.vas;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.mobile.step.PaymentSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.GraphqlSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.GeneralFeatures.VAS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.mobile.element.MyOfferSnippet.RAISE_UP_FOR;
import static ru.yandex.general.mobile.page.OfferCardPage.RAISE_UP;
import static ru.yandex.general.mobile.page.PaymentPage.PAYMENT_SUCCESS;
import static ru.yandex.general.mobile.page.PaymentPage.THANKS;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(VAS_FEATURE)
@DisplayName("Тесты на васы")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class VasUseTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private PaymentSteps paymentSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private GraphqlSteps graphqlSteps;

    @Before
    public void before() {
        passportSteps.createAccountAndLogin();
        offerAddSteps.setCookie(CLASSIFIED_REGION_ID, "65");
        urlSteps.testing().path(FORM).open();
        offerAddSteps.addOffer();
    }

    @Test
    @Ignore("Не работает на гриде")
    @Owner(ALEKS_IVANOV)
    @DisplayName("Применение VAS с карточки, возвращаемся на карточку после оплаты")
    public void shouldSeeVasSuccessfullFromCard() {
        offerAddSteps.onOfferCardPage().button(RAISE_UP).click();
        offerAddSteps.waitSomething(15, TimeUnit.SECONDS);
        paymentSteps.makePayment();
        paymentSteps.onPaymentPage().statusText().should(hasText(PAYMENT_SUCCESS));
        paymentSteps.onPaymentPage().button(THANKS).click();

        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());
    }

    @Test
    @Ignore("Не работает на гриде")
    @Owner(ALEKS_IVANOV)
    @DisplayName("Применение VAS с личного кабинета, возвращаемся на ЛК после оплаты")
    public void shouldSeeVasSuccessfullFromLk() {
        urlSteps.testing().path(MY).path(OFFERS).open();
        offerAddSteps.scrollingToElement(offerAddSteps.onMyOffersPage().snippetFirst().button(RAISE_UP_FOR));
        offerAddSteps.onMyOffersPage().snippetFirst().button(RAISE_UP_FOR).click();
        offerAddSteps.waitSomething(15, TimeUnit.SECONDS);
        paymentSteps.makePayment();
        paymentSteps.onPaymentPage().statusText().should(hasText(PAYMENT_SUCCESS));
        paymentSteps.onPaymentPage().button(THANKS).click();

        offerAddSteps.onMyOffersPage().snippetFirst().waitUntil(isDisplayed());
    }

}
