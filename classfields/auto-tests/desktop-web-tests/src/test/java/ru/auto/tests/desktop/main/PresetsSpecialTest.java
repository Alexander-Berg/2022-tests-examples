package ru.auto.tests.desktop.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.KOPITSA;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - пресеты - спецпредложения")
@Feature(MAIN)
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class PresetsSpecialTest {

    private static final String PRESET_TITLE = "Спецпредложения";
    private static final int PRESET_NUM = 2;
    private static final int PRESET_SALES_CNT = 20;
    private static final int PRESET_VISIBLE_SALES_CNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsPresetSpecials").post();

        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.setWideWindowSize(HEIGHT_1024);
        basePageSteps.onMainPage().presets().getPreset(PRESET_NUM).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().presets().preset(PRESET_TITLE)
                .waitUntil(hasClass(containsString("Radio_checked")));
        basePageSteps.onMainPage().presets().salesList().waitUntil(hasSize(PRESET_SALES_CNT));
        basePageSteps.onMainPage().presets().salesList().subList(0, PRESET_VISIBLE_SALES_CNT)
                .forEach(item -> item.waitUntil(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        mockRule.with("desktop/OfferCarsUsedUser").update();

        basePageSteps.onMainPage().presets().getSale(0).waitUntil(isDisplayed()).click();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/land_rover/discovery/1076842087-f1e84/")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Смотреть все")
    public void shouldClickShowAllButton() {
        basePageSteps.onMainPage().presets().showAllUrl().waitUntil(isDisplayed()).click();
        urlSteps.path(CARS).path(ALL).addParam("special", "true").fragment("priceRange")
                .shouldNotSeeDiff();
        waitSomething(1, TimeUnit.SECONDS);
        assertThat("Не произошел скролл к блоку", basePageSteps.getPageYOffset() > 0);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Переключение фотографий в объявлении")
    public void shouldSwitchPhotos() {
        screenshotSteps.setWindowSizeForScreenshot();
        basePageSteps.onMainPage().presets().getSale(0).hover();
        String firstPhoto = basePageSteps.onMainPage().presets().getSale(0).getImg(0).getAttribute("src");
        basePageSteps.onMainPage().presets().getSale(0).getImg(1).hover();
        basePageSteps.onMainPage().presets().getSale(0).getImg(1)
                .should(not(hasAttribute("src", firstPhoto)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке обновления пресета")
    public void shouldRefreshPreset() {
        String saleUrl = basePageSteps.onMainPage().presets().getSale(0).url().getAttribute("href");
        basePageSteps.onMainPage().presets().refreshButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().presets().preset(PRESET_TITLE)
                .waitUntil(hasClass(containsString("Radio_checked")));
        basePageSteps.onMainPage().presets().getSale(0).url().waitUntil(hasAttribute("href",
                not(isEmptyString())));
        basePageSteps.onMainPage().presets().getSale(0).url().should(not(hasAttribute("href", saleUrl)));
    }
}
