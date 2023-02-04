package ru.yandex.general.adult;

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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.FOR_ADULTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.INTIM_TOVARI;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;
import static ru.yandex.general.step.BasePageSteps.LIST;

@Epic(FOR_ADULTS_FEATURE)
@DisplayName("Скриншоты сниппета «Только для взрослых»")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AdultSnippetScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Rule
    @Inject
    public MockRule mockRule;

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
        mockRule.graphqlStub(mockResponse().setSearch(listingCategoryResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).getMockSnippet().setCategoryForAdults(true))).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();

        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        compareSteps.resize(1920, 1080);
        urlSteps.testing().path(INTIM_TOVARI);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature("Сниппет «Только для взрослых», листинг плиткой")
    @DisplayName("Скриншот сниппета товара для взрослых на листинге плиткой, светлая/тёмная темы")
    public void shouldSeeAdultSnippetScreenshotGridListing() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().snippetFirst());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().snippetFirst());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature("Сниппет «Только для взрослых», листинг списком")
    @DisplayName("Скриншот сниппета товара для взрослых на листинге списком, светлая/тёмная темы")
    public void shouldSeeAdultSnippetScreenshotListListing() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().snippetFirst());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().snippetFirst());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
