package ru.yandex.general.userMenuPopup;

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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.GeneralFeatures.USER_MENU_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(USER_MENU_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншот попапа юзера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class UserMenuPopupScreenshotTest {

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

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample().build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        passportSteps.commonAccountLogin();
        urlSteps.testing().open();
        basePageSteps.onBasePage().header().burger().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот попапа юзера")
    public void shouldSeeUserPopupScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        urlSteps.setProductionHost().open();
        basePageSteps.onBasePage().header().burger().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
