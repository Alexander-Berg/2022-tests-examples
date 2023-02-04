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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - пресеты - сниженная цена")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PresetsDiscountTest {

    private static final String PRESET_TITLE = "Сниженная цена";
    private static final int PRESET_NUM = 3;
    private static final int PRESET_SALES_CNT = 40;
    private static final int PRESET_VISIBLE_SALES_CNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        screenshotSteps.setWindowSizeForScreenshot();
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onMainPage().presets(), 0, 300);
        basePageSteps.onMainPage().presets().getPreset(PRESET_NUM).waitUntil(isDisplayed()).hover().click();
        basePageSteps.onMainPage().presets().preset(PRESET_TITLE)
                .waitUntil(hasClass(containsString("Radio_checked")));
        basePageSteps.onMainPage().presets().salesList().waitUntil(hasSize(PRESET_SALES_CNT));
        basePageSteps.onMainPage().presets().salesList().subList(0, PRESET_VISIBLE_SALES_CNT)
                .forEach(item -> {
                    item.waitUntil(isDisplayed());
                    item.getImg(0).waitUntil(isDisplayed());
                    item.discountPrice().waitUntil(isDisplayed());
                });
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        basePageSteps.onMainPage().presets().getSale(0).waitUntil(isDisplayed()).hover().click();
        basePageSteps.onCardPage().cardHeader().price().loweredPriceIcon().should(isDisplayed());
        basePageSteps.onCardPage().cardHeader().price().hover();
        basePageSteps.onCardPage().pricePopup().loweredPrice().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Смотреть все")
    public void shouldClickShowAllUrl() {
        basePageSteps.onMainPage().presets().showAllUrl().waitUntil(isDisplayed()).click();
        urlSteps.path(CARS).path(USED).addParam("search_tag", "history_discount").fragment("priceRange")
                .shouldNotSeeDiff();
        waitSomething(2, TimeUnit.SECONDS);
        assertThat("Не произошел скролл к блоку", basePageSteps.getPageYOffset() > 0);
    }
}
