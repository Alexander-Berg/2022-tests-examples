package ru.yandex.general.commonCases;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static ru.yandex.general.consts.GeneralFeatures.HOMEPAGE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.PROFILE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SEARCH_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.element.Header.LOGIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Переход по кнопке «Войти» в хэдере")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class NavigationHeaderButtonLoginClickTest {

    private static final String TEXT = "ноутбук macbook";
    private static final String LOGIN_WITH_YANDEX_ID = "Войдите с Яндекс ID";
    private static final String PASSPORT_URL = "https://passport.yandex.ru/auth?mode=auth&retpath=";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Test
    @Epic(HOMEPAGE_FEATURE)
    @Feature(NAVIGATION_FEATURE)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке «Войти» на главной")
    public void shouldSeeLoginButtonOnHomepage() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().header().link(LOGIN).click();
        basePageSteps.onBasePage().h1().waitUntil(hasText(LOGIN_WITH_YANDEX_ID));

        urlSteps.shouldNotDiffWith(format("%s%s", PASSPORT_URL, urlSteps));
    }

    @Test
    @Epic(LISTING_FEATURE)
    @Feature(NAVIGATION_FEATURE)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке «Войти» на листинге категории")
    public void shouldSeeLoginButtonOnCategoryListing() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().header().link(LOGIN).click();
        basePageSteps.onBasePage().h1().waitUntil(hasText(LOGIN_WITH_YANDEX_ID));

        urlSteps.shouldNotDiffWith(format("%s%s", PASSPORT_URL, urlSteps));
    }

    @Test
    @Epic(SEARCH_FEATURE)
    @Feature(NAVIGATION_FEATURE)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке «Войти» на текстовом поиске")
    public void shouldSeeLoginButtonOnTextSearch() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();
        basePageSteps.onListingPage().header().link(LOGIN).click();
        basePageSteps.onBasePage().h1().waitUntil(hasText(LOGIN_WITH_YANDEX_ID));

        urlSteps.shouldNotDiffWith(format("%s%s", PASSPORT_URL, urlSteps));
    }

    @Test
    @Epic(OFFER_CARD_FEATURE)
    @Feature(NAVIGATION_FEATURE)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке «Войти» на карточке оффера")
    public void shouldSeeLoginButtonOnOfferCard() {
        urlSteps.testing().path(jSoupSteps.getActualOfferCardUrl()).open();
        basePageSteps.onListingPage().header().link(LOGIN).click();
        basePageSteps.onBasePage().h1().waitUntil(hasText(LOGIN_WITH_YANDEX_ID));

        urlSteps.shouldNotDiffWith(format("%s%s", PASSPORT_URL, urlSteps));
    }

    @Test
    @Epic(PROFILE_FEATURE)
    @Feature(NAVIGATION_FEATURE)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке «Войти» на профиле продавца")
    public void shouldSeeLoginButtonOnPublicProfile() {
        urlSteps.testing().path(PROFILE).path(SELLER_PATH).open();
        basePageSteps.onListingPage().header().link(LOGIN).click();
        basePageSteps.onBasePage().h1().waitUntil(hasText(LOGIN_WITH_YANDEX_ID));

        urlSteps.shouldNotDiffWith(format("%s%s", PASSPORT_URL, urlSteps));
    }

}
