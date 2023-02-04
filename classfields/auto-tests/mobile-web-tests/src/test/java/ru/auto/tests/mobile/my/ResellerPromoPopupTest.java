package ru.auto.tests.mobile.my;

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
import ru.auto.tests.desktop.mobile.element.lk.ProfessionalSellerPopup;
import ru.auto.tests.desktop.mobile.page.LkPage;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Notifications.INFORMATION_WAS_SAVED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROFILE;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.QueryParams.TRUE;
import static ru.auto.tests.desktop.mobile.element.lk.ProfessionalSellerPopup.GET_STATUS;
import static ru.auto.tests.desktop.mobile.element.lk.ProfessionalSellerPopup.LATER;
import static ru.auto.tests.desktop.mobile.element.lk.ProfessionalSellerPopup.VIEW_OFFERS_PAGE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.step.CookieSteps.RESELLER_PUBLIC_PROFILE_LK_BANNER_SHOWN;
import static ru.auto.tests.desktop.step.CookieSteps.RESELLER_PUBLIC_PROFILE_POPUP_SHOWN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Промо профессионального аккаунта для перекупа")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_RESELLER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ResellerPromoPopupTest {

    private static final String RESELLER_ID = "ix57je7ThI5WAhtVmbWXjw";

    private static final String POPUP_TEXT = "Станьте профессиональным продавцом\nОсобый статус\nВаши объявления " +
            "получат пометку «Профессиональный продавец»\nПерсональная страница\nПользователи видят все ваши " +
            "объявления на одной странице. Как у дилера\nБольше актуальных звонков\nВам позвонят только те, кто " +
            "готов покупать у профессиональных продавцов\nПолучить статус\nПозже";

    private static final String SUCCESS_BECOME_RESELLER_TEXT = "Вы — профессиональный продавец\nТеперь у вас есть " +
            "собственная страница с объявлениями. Управлять статусом можно в профиле.\nПосмотреть страницу объявлений";

    private static final String NOTIFICATION_TEXT = "Стать профессиональным продавцом можно в профиле\nПрофиль";

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
                stub("mobile/UserReseller")
        ).create();

        cookieSteps.deleteCookie(RESELLER_PUBLIC_PROFILE_POPUP_SHOWN);

        urlSteps.testing().path(MY).path(ALL).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст промо поп-апа для перекупа")
    public void shouldSeeResellerPromoPopupText() {
        basePageSteps.onLkPage().professionalSellerPopup().should(hasText(POPUP_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ставится кука «reseller-public-profile-popup-shown» при показе промо поп-апа для перекупа")
    public void shouldSeeResellerPromoPopupShownCookie() {
        basePageSteps.onLkPage().professionalSellerPopup().waitUntil(isDisplayed());

        cookieSteps.shouldSeeCookie(RESELLER_PUBLIC_PROFILE_POPUP_SHOWN);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ставится кука «reseller_public_profile_lk_banner_shown» после закрытия промо баннера для перекупа")
    public void shouldSeeResellerBannerShownCookie() {
        cookieSteps.setCookieForBaseDomain(RESELLER_PUBLIC_PROFILE_POPUP_SHOWN, TRUE);
        basePageSteps.refresh();

        basePageSteps.onLkPage().proffessionalSellerBanner().close().click();

        cookieSteps.shouldSeeCookie(RESELLER_PUBLIC_PROFILE_LK_BANNER_SHOWN);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Не отображается баннер с кукой «reseller_public_profile_lk_banner_shown»")
    public void shouldNotSeeResellerBannerWithCookie() {
        cookieSteps.setCookieForBaseDomain(RESELLER_PUBLIC_PROFILE_LK_BANNER_SHOWN, TRUE);
        basePageSteps.refresh();

        basePageSteps.onLkPage().proffessionalSellerBanner().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Показ нотификации по нажатию на кнопку «Позже» в поп-апе профессионального продавца")
    public void shouldSeeNotificationAfterLaterClick() {
        basePageSteps.onLkPage().professionalSellerPopup().button(LATER).waitUntil(isDisplayed()).click();

        basePageSteps.onLkPage().notifier().waitUntil(isDisplayed()).should(hasText(NOTIFICATION_TEXT));
        basePageSteps.onLkPage().professionalSellerPopup().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход на страницу «Профиль» из нотификации по нажатию на кнопку «Позже»")
    public void shouldGoToProfileFromNotification() {
        basePageSteps.onLkPage().professionalSellerPopup().button(LATER).waitUntil(isDisplayed()).click();
        basePageSteps.onLkPage().notifier().button(LkPage.PROFILE).waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(MY).path(PROFILE).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подключение статуса профессионального продавца с промо поп-апа, текст успешного подключения")
    public void shouldBecomeProfessionalPopupText() {
        becomeProfessionalSeller();

        basePageSteps.onLkPage().professionalSellerPopup().should(hasText(SUCCESS_BECOME_RESELLER_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переходим в профиль с попапа «Вы — профессиональный продавец»")
    public void shouldGoToProfileFromYouResellerPopup() {
        becomeProfessionalSeller();
        basePageSteps.onLkPage().professionalSellerPopup().button(ProfessionalSellerPopup.PROFILE).click();

        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MY).path(PROFILE).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переходим на публичную страницу с попапа «Вы — профессиональный продавец»")
    public void shouldGoToPublicProfileFromYouResellerPopup() {
        becomeProfessionalSeller();
        basePageSteps.onLkPage().professionalSellerPopup().button(VIEW_OFFERS_PAGE).click();

        basePageSteps.switchToNextTab();
        urlSteps.testing().path(RESELLER).path(RESELLER_ID).path(ALL).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрываем попап «Вы — профессиональный продавец»")
    public void shouldCloseYouResellerPopup() {
        becomeProfessionalSeller();
        basePageSteps.onLkPage().professionalSellerPopup().close().waitUntil(isDisplayed()).click();

        basePageSteps.onLkPage().professionalSellerPopup().should(not(isDisplayed()));
    }

    private void becomeProfessionalSeller() {
        basePageSteps.onLkPage().professionalSellerPopup().button(GET_STATUS).click();
        basePageSteps.onLkPage().notifier(INFORMATION_WAS_SAVED).waitUntil(isDisplayed());
    }

}
