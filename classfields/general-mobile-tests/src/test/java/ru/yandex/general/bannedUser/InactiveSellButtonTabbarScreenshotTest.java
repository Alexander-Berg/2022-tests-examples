package ru.yandex.general.bannedUser;

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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.BANNED_USER_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.UserStatus.UserBanDescriptions.FRAUD;
import static ru.yandex.general.mock.MockHomepage.homepageResponse;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;

@Epic(BANNED_USER_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншот таббара с задизейбленной кнопкой размещения")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class InactiveSellButtonTabbarScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

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
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample().setUserBannedWithDescription(FRAUD).build())
                .setCategoriesTemplate()
                .setHomepage(homepageResponse().addOffers(5).build())
                .setRegionsTemplate()
                .build()).withDefaults().create();
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        urlSteps.testing().open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот таббара с задизейбленной кнопкой размещения, светлая/тёмная темы")
    public void shouldSeeInactiveSellButtonTabbarScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().tabBar());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().tabBar());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
