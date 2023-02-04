package ru.yandex.general.sellerProfile;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.PROFILE_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.mobile.page.BasePage.ERROR_404_TEXT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(PROFILE_FEATURE)
@DisplayName("404 ошибка на странице профиля продавца")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class SellerProfile404Test {

    private static final String NOT_FOUND_SELLER_PATH = "214J21J42145J2141";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;


    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("404 ошибка по ссылке на профиль продавца с неправильным ID в ссылке")
    public void shouldSeeError404ErrorSellerId() {
        urlSteps.testing().path(PROFILE).path(NOT_FOUND_SELLER_PATH).open();

        basePageSteps.onProfilePage().paragraph().should(hasText(ERROR_404_TEXT));
        basePageSteps.onProfilePage().errorImage().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("404 ошибка по ссылке на профиль продавца без ID в ссылке")
    public void shouldSeeError404NoSellerId() {
        urlSteps.testing().path(PROFILE).open();

        basePageSteps.onProfilePage().paragraph().should(hasText(ERROR_404_TEXT));
        basePageSteps.onProfilePage().errorImage().should(isDisplayed());
    }

}
