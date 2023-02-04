package ru.yandex.realty.listing;

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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.SUBSCRIPTIONS;
import static ru.yandex.realty.consts.RealtyFeatures.LISTING;
import static ru.yandex.realty.consts.RealtyFeatures.SUBSCRIPTION;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.mobile.element.listing.SubscriptionForm.CONFIRMATION;
import static ru.yandex.realty.mobile.element.listing.SubscriptionForm.PODPISKI;
import static ru.yandex.realty.mobile.element.listing.SubscriptionForm.REPEAT;
import static ru.yandex.realty.mobile.element.listing.SubscriptionForm.SUBSCRIBE;
import static ru.yandex.realty.mobile.element.listing.SubscriptionForm.TERMS_OF_USE;
import static ru.yandex.realty.step.UrlSteps.SEARCH_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.TAB_URL_PARAM;

@Issue("VERTISTEST-1352")
@Epic(LISTING)
@Feature(SUBSCRIPTION)
@DisplayName("Блок подписки для незалогина")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SubscriptionFormUnauthorizedTest {

    private static final String TERMS_OF_USE_URL = "https://yandex.ru/legal/realty_termsofuse/";
    private static final String CYRILLIC_EMAIL = "ололо@ололо.рф";

    private String email;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        email = getRandomEmail();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер неавторизован. Email пустой")
    public void shouldSeeEmailEmptyUnauthorized() {
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().emailInput().should(hasValue(""));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер неавторизован. Кнопка «Подписаться» задизейблена")
    public void shouldSeeSubscribeButtonDisabledUnauthorized() {
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE)
                .should(hasAttribute("disabled", "true"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер неавторизован. Чекбокс подписки на спам активен")
    public void shouldSeeCheckboxEnabledUnauthorized() {
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().checkbox().should(isChecked());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер неавторизован. Текст подтверждения емейла")
    public void shouldSeeEmailConfirmationUnauthorized() {
        basePageSteps.scrollToElement(basePageSteps.onMobileSaleAdsPage().subscriptionForm());
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().emailInput().sendKeys(email);
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE).click();

        basePageSteps.onMobileSaleAdsPage().subscriptionForm().description().should(hasText(
                String.format("%s %s.", CONFIRMATION, email)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер неавторизован. Ссылка на «Подписки» после оформления")
    public void shouldSeeUrlToSubscriptionsUnauthorized() {
        basePageSteps.scrollToElement(basePageSteps.onMobileSaleAdsPage().subscriptionForm());
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().emailInput().sendKeys(email);
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE).click();

        basePageSteps.onMobileSaleAdsPage().subscriptionForm().link(PODPISKI).should(hasHref(equalTo(
                urlSteps.testing().path(SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, SEARCH_TAB_VALUE).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер неавторизован. Ссылка на пользовательское соглашение")
    public void shouldSeeTermsOfUseURLUnauthorized() {
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().link(TERMS_OF_USE).should(hasHref(
                equalTo(TERMS_OF_USE_URL)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер неавторизован. После рефреша - подтверждение емейла")
    public void shouldSeeSuccessSubscriptionAfterRefreshUnauthorized() {
        basePageSteps.scrollToElement(basePageSteps.onMobileSaleAdsPage().subscriptionForm());
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().emailInput().sendKeys(email);
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE).click();
        basePageSteps.refresh();

        basePageSteps.onMobileSaleAdsPage().subscriptionForm().description().should(hasText(
                String.format("%s %s.", CONFIRMATION, email)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер неавторизован. Ошибка с кириллической почтой")
    public void shouldSeeErrorWithCyrillicEmail() {
        basePageSteps.scrollToElement(basePageSteps.onMobileSaleAdsPage().subscriptionForm());
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().emailInput().sendKeys(CYRILLIC_EMAIL);
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE).click();

        basePageSteps.onMobileSaleAdsPage().subscriptionForm().error().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер неавторизован. Появляется инпут, после «Повторить»")
    public void shouldSeeEmailInputAfterRepeatButton() {
        basePageSteps.scrollToElement(basePageSteps.onMobileSaleAdsPage().subscriptionForm());
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().emailInput().sendKeys(CYRILLIC_EMAIL);
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE).click();
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().error().button(REPEAT).click();

        basePageSteps.onMobileSaleAdsPage().subscriptionForm().emailInput().should(isDisplayed());
    }

}
