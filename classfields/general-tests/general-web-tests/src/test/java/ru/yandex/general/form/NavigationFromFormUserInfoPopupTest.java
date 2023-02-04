package ru.yandex.general.form;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.element.Header.HELP;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с формы")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class NavigationFromFormUserInfoPopupTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с формы на паспорт по клику на имя из попапа «Юзер инфо»")
    public void shouldSeeFormToPassportFromUserInfoNameClick() {
        offerAddSteps.onFormPage().header().userName().click();
        offerAddSteps.onFormPage().userInfoPopup().link("Vasily Pupkin").waitUntil(isDisplayed()).click();
        offerAddSteps.switchToNextTab();

        urlSteps.fromUri("https://passport.yandex.ru/profile").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с формы по клику на «Помощь» из попапа «Юзер инфо»")
    public void shouldSeeFormToHelpFromUserInfoPopup() {
        offerAddSteps.onFormPage().header().userName().click();
        offerAddSteps.onFormPage().userInfoPopup().link(HELP).waitUntil(isDisplayed()).click();
        offerAddSteps.switchToNextTab();

        urlSteps.fromUri("https://yandex.ru/support/o-desktop/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с формы по клику на «Добавить профиль» из попапа «Юзер инфо»")
    public void shouldSeeFormToAddAccountFromUserInfoPopup() {
        offerAddSteps.onFormPage().header().userName().click();
        offerAddSteps.onFormPage().userInfoPopup().link("Добавить профиль").waitUntil(isDisplayed()).click();

        urlSteps.fromUri(format("https://passport.yandex.ru/auth?mode=add-user&retpath=%s", urlSteps))
                .shouldNotDiffWithWebDriverUrl();
    }

}
