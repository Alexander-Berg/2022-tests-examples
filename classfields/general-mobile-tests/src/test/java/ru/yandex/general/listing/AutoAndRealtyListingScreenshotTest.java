package ru.yandex.general.listing;

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
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.GRID;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.TEXT_SEARCH_TEMPLATE;
import static ru.yandex.general.mock.MockSearch.mockSearch;
import static ru.yandex.general.mock.MockWizardResponse.AUTO_EXAMPLE;
import static ru.yandex.general.mock.MockWizardResponse.REALTY_EXAMPLE;
import static ru.yandex.general.mock.MockWizardResponse.mockWizardResponse;
import static ru.yandex.general.step.BasePageSteps.LIST;

@Epic(LISTING_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншот выдачи авто/недвижки")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AutoAndRealtyListingScreenshotTest {

    private static final String TEXT = "поисковый текст";

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
    public String text;

    @Parameterized.Parameter(1)
    public String mockPath;

    @Parameterized.Parameters(name = "{index}. Выдача «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Авто", AUTO_EXAMPLE},
                {"Недвижимость", REALTY_EXAMPLE}
        });
    }

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse().setSearch(mockSearch(TEXT_SEARCH_TEMPLATE).addOffers(0).setRequestText(TEXT)
                        .build()).build())
                .wizardStub(mockWizardResponse(mockPath).build()).withDefaults().create();
        basePageSteps.resize(375, 1300);
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, text);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот листинга авто/недвижимости. Листинг плиткой")
    public void shouldSeeAutoRealtyGridListingScreenshot() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.open();

        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот листинга авто/недвижимости. Листинг списком")
    public void shouldSeeAutoRealtyListListingScreenshot() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
        urlSteps.open();

        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
