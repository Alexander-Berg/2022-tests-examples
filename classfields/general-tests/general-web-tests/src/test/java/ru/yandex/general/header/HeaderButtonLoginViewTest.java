package ru.yandex.general.header;

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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static ru.yandex.general.consts.GeneralFeatures.HEADER_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.element.Header.LOGIN;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@Epic(HEADER_FEATURE)
@Feature("Отображение кнопки «Войти»")
@DisplayName("Отображение кнопки «Войти»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class HeaderButtonLoginViewTest {

    private static final String TEXT = "ноутбук macbook";
    private static final String PASSPORT_URL = "https://passport.yandex.ru/passport?mode=auth&retpath=";

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
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Войти» на главной")
    public void shouldSeeLoginButtonOnHomepage() {
        urlSteps.testing().path(MOSKVA).open();

        basePageSteps.onListingPage().header().link(LOGIN).should(
                hasAttribute(HREF, format("%s%s", PASSPORT_URL, urlSteps.toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Войти» на главной, прилипший хэдер")
    public void shouldSeeLoginButtonOnHomepageFloatedHeader() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);

        basePageSteps.onListingPage().floatedHeader().link(LOGIN).should(
                hasAttribute(HREF, format("%s%s", PASSPORT_URL, urlSteps.toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Войти» на листинге категории")
    public void shouldSeeLoginButtonOnCategoryListing() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();

        basePageSteps.onListingPage().header().link(LOGIN).should(
                hasAttribute(HREF, format("%s%s", PASSPORT_URL, urlSteps.toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Войти» на листинге категории, прилипший хэдер")
    public void shouldSeeLoginButtonOnCategoryListingFloatedHeader() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);

        basePageSteps.onListingPage().floatedHeader().link(LOGIN).should(
                hasAttribute(HREF, format("%s%s", PASSPORT_URL, urlSteps.toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Войти» на текстовом поиске")
    public void shouldSeeLoginButtonOnTextSearch() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();

        basePageSteps.onListingPage().header().link(LOGIN).should(
                hasAttribute(HREF, format("%s%s", PASSPORT_URL, urlSteps.toString().replace("+", "%20"))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Войти» на текстовом поиске, прилипший хэдер")
    public void shouldSeeLoginButtonOnTextSearchFloatedHeader() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);

        basePageSteps.onListingPage().floatedHeader().link(LOGIN).should(
                hasAttribute(HREF, format("%s%s", PASSPORT_URL, urlSteps.toString().replace("+", "%20"))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Войти» на карточке оффера")
    public void shouldSeeLoginButtonOnOfferCard() {
        urlSteps.testing().path(jSoupSteps.getActualOfferCardUrl()).open();

        basePageSteps.onListingPage().header().link(LOGIN).should(
                hasAttribute(HREF, format("%s%s", PASSPORT_URL, urlSteps.toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Войти» на карточке оффера, прилипший хэдер")
    public void shouldSeeLoginButtonOnOfferCardFloatedHeader() {
        urlSteps.testing().path(jSoupSteps.getActualOfferCardUrl()).open();
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);

        basePageSteps.onListingPage().floatedHeader().link(LOGIN).should(
                hasAttribute(HREF, format("%s%s", PASSPORT_URL, urlSteps.toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Войти» на профиле продавца")
    public void shouldSeeLoginButtonOnPublicProfile() {
        urlSteps.testing().path(PROFILE).path(SELLER_PATH).open();

        basePageSteps.onListingPage().header().link(LOGIN).should(
                hasAttribute(HREF, format("%s%s", PASSPORT_URL, urlSteps.toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Войти» на профиле продавца, прилипший хэдер")
    public void shouldSeeLoginButtonOnPublicProfileFloatedHeader() {
        urlSteps.testing().path(PROFILE).path(SELLER_PATH).open();
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);

        basePageSteps.onListingPage().floatedHeader().link(LOGIN).should(
                hasAttribute(HREF, format("%s%s", PASSPORT_URL, urlSteps.toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Войти» на форме подачи оффера")
    public void shouldSeeLoginButtonOnForm() {
        urlSteps.testing().path(FORM).open();

        basePageSteps.onListingPage().header().link(LOGIN).should(
                hasAttribute(HREF, format("%s%s", PASSPORT_URL, urlSteps.toString())));
    }

}
