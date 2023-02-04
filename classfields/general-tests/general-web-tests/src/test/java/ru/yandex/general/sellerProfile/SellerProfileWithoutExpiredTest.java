package ru.yandex.general.sellerProfile;

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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.PROFILE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.mock.MockPublicProfile.profileResponse;
import static ru.yandex.general.mock.MockPublicProfileSnippet.PROFILE_BASIC_SNIPPET;
import static ru.yandex.general.mock.MockPublicProfileSnippet.PROFILE_REZUME_SNIPPET;
import static ru.yandex.general.mock.MockPublicProfileSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.PublicProfilePage.EXPIRED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(PROFILE_FEATURE)
@DisplayName("Страница профиля без таба «Завершенные»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class SellerProfileWithoutExpiredTest {

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

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setPublicProfile(
                        profileResponse().snippets(asList(
                                mockSnippet(PROFILE_BASIC_SNIPPET),
                                mockSnippet(PROFILE_REZUME_SNIPPET)))
                                .setActiveCount(2).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        basePageSteps.setMoscowCookie();
        compareSteps.resize(1920, 1080);
        urlSteps.testing().path(PROFILE).path("testprofile").open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет таба «Завершенные»")
    public void shouldNotSeeExpired() {
        basePageSteps.onProfilePage().tab(EXPIRED).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SCREENSHOT_TESTS)
    @DisplayName("Скриншот страницы профиля без таба «Завершенные»")
    public void shouldSeeProfileScreenshotWithoutExpired() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onProfilePage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onProfilePage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
