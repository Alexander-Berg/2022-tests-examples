package ru.yandex.general.collorScheme;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.COLOR_SCHEME;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;

@Epic(COLOR_SCHEME)
@Feature("Отображение цветовой схемы")
@DisplayName("Отображение страниц с соответствующей цветовой схемой")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CollorSchemeViewTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String theme;

    @Parameterized.Parameters(name = "{index}. Тема «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {LIGHT_THEME},
                {DARK_THEME}
        });
    }

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается главная в соответствующей цветовой схеме")
    public void shouldSeeColorThemeOnHomepage() {
        urlSteps.testing().open();

        basePageSteps.onListingPage().pageColor(theme).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается листинг в соответствующей цветовой схеме")
    public void shouldSeeColorThemeOnListing() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();

        basePageSteps.onListingPage().pageColor(theme).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается текстовый поиск в соответствующей цветовой схеме")
    public void shouldSeeColorThemeOnTextSearch() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, "ноутбук apple").open();

        basePageSteps.onListingPage().pageColor(theme).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается карточка оффера в соответствующей цветовой схеме")
    public void shouldSeeColorThemeOnOfferCard() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());

        basePageSteps.onOfferCardPage().pageColor(theme).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается страница продавца в соответствующей цветовой схеме")
    public void shouldSeeColorThemeOnSellerProfile() {
        urlSteps.testing().path(PROFILE).path(SELLER_PATH).open();

        basePageSteps.onListingPage().pageColor(theme).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается форма подачи в соответствующей цветовой схеме")
    public void shouldSeeColorThemeOnForm() {
        urlSteps.testing().path(FORM).open();

        basePageSteps.onListingPage().pageColor(theme).should(isDisplayed());
    }

}
