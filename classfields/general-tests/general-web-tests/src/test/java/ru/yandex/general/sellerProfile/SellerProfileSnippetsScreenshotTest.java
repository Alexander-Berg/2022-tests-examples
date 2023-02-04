package ru.yandex.general.sellerProfile;

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
import static ru.yandex.general.consts.GeneralFeatures.PROFILE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.mock.MockPublicProfile.profileResponse;
import static ru.yandex.general.mock.MockPublicProfileSnippet.PROFILE_BASIC_SNIPPET;
import static ru.yandex.general.mock.MockPublicProfileSnippet.PROFILE_REZUME_SNIPPET;
import static ru.yandex.general.mock.MockPublicProfileSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;

@Epic(PROFILE_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Страница профиля. Скриншотные тесты.")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SellerProfileSnippetsScreenshotTest {

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
    public int width;

    @Parameterized.Parameter(1)
    public String theme;

    @Parameterized.Parameters(name = "Скриншот профиля на разных разрешениях экрана. Тема «{1}»")
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
                .setPublicProfile(
                        profileResponse().snippets(asList(
                                mockSnippet(PROFILE_BASIC_SNIPPET).setPrice(2500),
                                mockSnippet(PROFILE_REZUME_SNIPPET).setSallaryPrice("39500"),
                                mockSnippet(PROFILE_BASIC_SNIPPET).setFreePrice(),
                                mockSnippet(PROFILE_REZUME_SNIPPET).setUnsetPrice(),
                                mockSnippet(PROFILE_BASIC_SNIPPET).setUnsetPrice(),
                                mockSnippet(PROFILE_BASIC_SNIPPET).addPhoto(20)))
                                .setActiveCount(6)
                                .setExpiredCount(2)
                                .setUserBadgeScore("Twenty").build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        compareSteps.resize(width, 1080);
        urlSteps.testing().path(PROFILE).path("testprofile").open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот страницы профиля, светлая/темная темы на разных разрешениях")
    public void shouldSeeProfileScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onProfilePage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onProfilePage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
