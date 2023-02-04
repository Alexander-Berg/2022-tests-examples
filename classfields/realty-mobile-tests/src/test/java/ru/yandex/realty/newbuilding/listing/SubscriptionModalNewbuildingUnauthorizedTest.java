package ru.yandex.realty.newbuilding.listing;

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
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.SUBSCRIPTIONS;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING;
import static ru.yandex.realty.consts.RealtyFeatures.SUBSCRIPTION;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.mobile.element.newbuilding.NewbuildingSubscriptionModal.CONFIRMATION;
import static ru.yandex.realty.mobile.element.newbuilding.NewbuildingSubscriptionModal.PODPISKI;
import static ru.yandex.realty.mobile.element.newbuilding.NewbuildingSubscriptionModal.REPEAT;
import static ru.yandex.realty.mobile.element.newbuilding.NewbuildingSubscriptionModal.SUBSCRIBE;
import static ru.yandex.realty.mobile.element.newbuilding.NewbuildingSubscriptionModal.TERMS_OF_USE;
import static ru.yandex.realty.mobile.element.newbuilding.NewbuildingSubscriptionModal.TRY_ONE_MORE_TIME;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.NEWBUILDING_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.NEW_FLAT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.TAB_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;

@Issue("VERTISTEST-1352")
@Epic(NEWBUILDING)
@Feature(SUBSCRIPTION)
@DisplayName("Модалка подписки для незалогина")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SubscriptionModalNewbuildingUnauthorizedTest {

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
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).open();
        basePageSteps.onNewBuildingPage().site(FIRST).favorite().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер неавторизован. Email пустой")
    public void shouldSeeEmailEmptyUnauthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().emailInput().should(hasValue(""));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер неавторизован. Кнопка «Подписаться» задизейблена")
    public void shouldSeeSubscribeButtonDisabledUnauthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE)
                .should(hasAttribute("disabled", "true"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер неавторизован. Чекбокс подписки на спам активен")
    public void shouldSeeCheckboxEnabledUnauthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().checkbox().should(isChecked());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер неавторизован. Текст подтверждения емейла")
    public void shouldSeeEmailConfirmationUnauthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().emailInput().sendKeys(email);
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE).click();
        basePageSteps.onNewBuildingPage().subscriptionModal().subscriptionSuccess().waitUntil(isDisplayed());

        basePageSteps.onNewBuildingPage().subscriptionModal().description().should(hasText(
                String.format("%s %s.", CONFIRMATION, email)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер неавторизован. Ссылка на «Подписки» после оформления")
    public void shouldSeeUrlToSubscriptionsUnauthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().emailInput().sendKeys(email);
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE).click();
        basePageSteps.onNewBuildingPage().subscriptionModal().subscriptionSuccess().waitUntil(isDisplayed());

        basePageSteps.onNewBuildingPage().subscriptionModal().link(PODPISKI).should(hasHref(equalTo(
                urlSteps.testing().path(SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, NEWBUILDING_TAB_VALUE).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер неавторизован. Ссылка на пользовательское соглашение")
    public void shouldSeeTermsOfUseURLUnauthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().link(TERMS_OF_USE).should(hasHref(
                equalTo(TERMS_OF_USE_URL)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер неавторизован. Ошибка с кириллической почтой")
    public void shouldSeeErrorWithCyrillicEmail() {
        basePageSteps.onNewBuildingPage().subscriptionModal().emailInput().sendKeys(CYRILLIC_EMAIL);
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE).click();

        basePageSteps.onNewBuildingPage().subscriptionModal().description().should(hasText(TRY_ONE_MORE_TIME));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер неавторизован. Появляется инпут, после «Повторить»")
    public void shouldSeeEmailInputAfterRepeatButton() {
        basePageSteps.onNewBuildingPage().subscriptionModal().emailInput().sendKeys(CYRILLIC_EMAIL);
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE).click();
        basePageSteps.onNewBuildingPage().subscriptionModal().button(REPEAT).click();

        basePageSteps.onNewBuildingPage().subscriptionModal().emailInput().should(isDisplayed());
    }
}
