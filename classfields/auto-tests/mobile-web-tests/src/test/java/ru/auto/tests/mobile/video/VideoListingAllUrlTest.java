package ru.auto.tests.mobile.video;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.VIDEO)
@DisplayName("Блок видео - клик по ссылке «Все видео»")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VideoListingAllUrlTest {

    private static final int VIDEO_BAR_POSITION = 25;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "mobile/SearchCarsAll",
                "mobile/VideoSearchCars").post();

        urlSteps.testing().path(category).path(ALL).open();
        basePageSteps.onListingPage().getSale(VIDEO_BAR_POSITION).hover();
        basePageSteps.onListingPage().videos().waitUntil(isDisplayed()).hover();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Ignore // TODO видео выпилили, добавить баг или удалить
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Все видео»")
    public void shouldClickAllVideosUrl() {
        basePageSteps.onListingPage().getSale(VIDEO_BAR_POSITION).hover();
        basePageSteps.onListingPage().videos().waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().videos().button("Все видео").waitUntil(isDisplayed()).click();
        urlSteps.testing().path(VIDEO).addParam("from", "listing").shouldNotSeeDiff();
    }
}
