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
import ru.yandex.general.mock.MockCurrentUser;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.BANNED_USER_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.UserStatus.UserBanDescriptions.FRAUD;
import static ru.yandex.general.consts.UserStatus.UserBanDescriptions.SPAM;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;

@Epic(BANNED_USER_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншот аллерта забаненного юзера в ЛК")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BanReasonScreenshotTest {

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
    public String title;

    @Parameterized.Parameter(1)
    public MockCurrentUser user;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Бан пользователя по одной причине",
                        currentUserExample().setUserBannedWithDescription(FRAUD)
                },
                {"Бан пользователя по двум причинам",
                        currentUserExample().setUserBannedWithDescription(FRAUD, SPAM)
                }

        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        compareSteps.resize(1920, 1080);

        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(user.build())
                .setRegionsTemplate()
                .build()).withDefaults().create();

        urlSteps.testing().path(MY).path(OFFERS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот сообщения о бане в ЛК, светлая тема")
    public void shouldSeeBanMessageScreenshot() {
        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().banMessage());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().banMessage());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот сообщения о бане в ЛК, тёмная тема")
    public void shouldSeeBanMessageDarkThemeScreenshot() {
        basePageSteps.setDarkThemeCookie();
        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().banMessage());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().banMessage());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
