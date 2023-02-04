package ru.yandex.general.myOffers;

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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;

@Epic(MY_OFFERS_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншотные тесты моих объявлений")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MyOffersScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CompareSteps compareSteps;

    @Parameterized.Parameter
    public int width;

    @Parameterized.Parameter(1)
    public String theme;

    @Parameterized.Parameters(name = "Скриншот листинга на разных разрешениях экрана. Тема «{1}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {1920, LIGHT_THEME},
                {1920, DARK_THEME},
                {1366, LIGHT_THEME},
                {1366, DARK_THEME}
        });
    }

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setCabinetListingExample()
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        compareSteps.resize(width, 1500);
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        basePageSteps.setMoscowCookie();
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(OFFERS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот моих объявлений на разных разрешениях экрана")
    public void shouldSeeMyOffersScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMyOffersPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMyOffersPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
