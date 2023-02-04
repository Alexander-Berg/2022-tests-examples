package ru.auto.tests.desktop.lk.sales.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.ALEXANDERREX;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROFILE;
import static ru.auto.tests.desktop.element.lk.reseller.ProfessionalSellerBanner.MORE_DETAILS;
import static ru.auto.tests.desktop.element.lk.reseller.ProfessionalSellerPopup.GET_STATUS;
import static ru.auto.tests.desktop.element.lk.reseller.ProfessionalSellerPopup.LATER;
import static ru.auto.tests.desktop.element.lk.reseller.ProfessionalSellerTooltip.SETTINGS_BUTTON;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.step.CookieSteps.RESELLER_PUBLIC_PROFILE_LK_BANNER_SHOWN;
import static ru.auto.tests.desktop.step.CookieSteps.RESELLER_PUBLIC_PROFILE_POPUP_SHOWN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Промо профессионального аккаунта для перекупа")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_RESELLER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ResellerPromoPopupTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthResellerWithoutProfessionalSeller"),
                stub("desktop/UserProfileAllowOffersShowPost"),
                stub("desktop-lk/UserReseller")
        ).create();

        cookieSteps.deleteCookie(RESELLER_PUBLIC_PROFILE_POPUP_SHOWN);

        urlSteps.testing().path(MY).path(ALL).open();
    }

    @Test
    @Owner(ALEXANDERREX)
    @Category({Regression.class, Testing.class})
    @DisplayName("Показ промо поп-апа для перекупа")
    public void shouldSeeResellerPromoPopup() {
        basePageSteps.onLkResellerSalesPage().proffessionalSellerBanner().button(MORE_DETAILS).click();

        basePageSteps.onLkResellerSalesPage().professionalSellerPopup().should(hasText(
                "Станьте профессиональным продавцом\nОсобый статус\nВаши объявления получат пометку «Профессиональный продавец»\n" +
                        "Персональная страница\nПользователи видят все ваши объявления на одной странице. Как у дилера\n" +
                        "Больше актуальных звонков\nВам позвонят только те, кто готов покупать у профессиональных продавцов\nПозже\nПолучить статус"));
    }

    @Test
    @Owner(ALEXANDERREX)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрытие промо поп-апа для перекупа")
    public void shouldNotSeeResellerPromoPopup() {
        basePageSteps.onLkResellerSalesPage().proffessionalSellerBanner().button(MORE_DETAILS).click();
        basePageSteps.onLkResellerSalesPage().professionalSellerPopup().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().professionalSellerPopup().closeIcon().click();

        basePageSteps.onLkResellerSalesPage().professionalSellerPopup().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEXANDERREX)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ставится кука «reseller-public-profile-popup-shown» при показе промо поп-апа для перекупа")
    public void shouldSeeResellerPromoShownCookie() {
        basePageSteps.onLkResellerSalesPage().proffessionalSellerBanner().button(MORE_DETAILS).click();
        basePageSteps.onLkResellerSalesPage().professionalSellerPopup().waitUntil(isDisplayed());

        cookieSteps.shouldSeeCookie(RESELLER_PUBLIC_PROFILE_POPUP_SHOWN);
    }

    @Test
    @Owner(ALEXANDERREX)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ставится кука «reseller_public_profile_lk_banner_shown» при показе промо поп-апа для перекупа")
    public void shouldSeeResellerBannerShownCookie() {
        basePageSteps.onLkResellerSalesPage().proffessionalSellerBanner().closeIcon().click();

        cookieSteps.shouldSeeCookie(RESELLER_PUBLIC_PROFILE_LK_BANNER_SHOWN);
    }

    @Test
    @Owner(ALEXANDERREX)
    @Category({Regression.class, Testing.class})
    @DisplayName("Показ тултипа по нажатию на кнопку «Позже» в промо поп-апе профессионального продавца")
    public void shouldSeePofessionalSellerTooltip() {
        basePageSteps.onLkResellerSalesPage().proffessionalSellerBanner().button(MORE_DETAILS).click();
        basePageSteps.onLkResellerSalesPage().professionalSellerPopup().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().professionalSellerPopup().button(LATER).click();

        basePageSteps.onLkResellerSalesPage().professionalSellerTooltip().should(isDisplayed());
    }

    @Test
    @Owner(ALEXANDERREX)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход на страницу «Профиль» из тултипа профессионального продавца")
    public void shouldSeeMyProfilePage() {
        basePageSteps.onLkResellerSalesPage().proffessionalSellerBanner().button(MORE_DETAILS).click();
        basePageSteps.onLkResellerSalesPage().professionalSellerPopup().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().professionalSellerPopup().button(LATER).click();
        basePageSteps.onLkResellerSalesPage().professionalSellerTooltip().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().professionalSellerTooltip().button(SETTINGS_BUTTON).click();
        basePageSteps.switchToTab(1);

        urlSteps.testing().path(MY).path(PROFILE).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEXANDERREX)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подключение статуса профессионального продавца с промо поп-апа")
    public void shouldBecomeProfessional() {
        basePageSteps.onLkResellerSalesPage().proffessionalSellerBanner().button(MORE_DETAILS).click();
        basePageSteps.onLkResellerSalesPage().professionalSellerPopup().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().professionalSellerPopup().button(GET_STATUS).click();
        basePageSteps.onLkResellerSalesPage().notifier().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().professionalSellerPopup().buttonContains("настройках").click();
        basePageSteps.switchToTab(1);

        urlSteps.testing().path(MY).path(PROFILE).shouldNotSeeDiff();
    }

}
