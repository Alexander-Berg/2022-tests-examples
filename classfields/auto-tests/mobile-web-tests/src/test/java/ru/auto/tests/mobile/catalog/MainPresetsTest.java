package ru.auto.tests.mobile.catalog;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - главная - пресеты")
@Feature(AutoruFeatures.CATALOG)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MainPresetsTest {

    private static final Integer PRESET_ITEMS_CNT = 12;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String presetTitle;

    @Parameterized.Parameter(1)
    public String query;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Кроссоверы до миллиона", "autoru_body_type=ALLROAD&autoru_body_type=ALLROAD_3_DOORS" +
                        "&autoru_body_type=ALLROAD_5_DOORS&price_from=850000&price_to=1000000"},
                {"Кабриолеты на лето", "autoru_body_type=CABRIO"},
                {"Семейные седаны", "autoru_body_type=SEDAN&price_from=1000000&price_to=1500000"},
                {"Заряженные внедорожники", "acceleration_to=6&autoru_body_type=ALLROAD" +
                        "&autoru_body_type=ALLROAD_3_DOORS&autoru_body_type=ALLROAD_5_DOORS"},
                {"Суперкары", "acceleration_to=4&autoru_body_type=COUPE"}
        });
    }

    @Before
    public void before() {
        basePageSteps.setWindowSize(WIDTH_WIDE_PAGE, HEIGHT_1024);

        urlSteps.testing().path(CATALOG).open();
        if (!basePageSteps.onCatalogMainPage().presets().getPresetItem(0).getText().equals(presetTitle)) {
            basePageSteps.onCatalogMainPage().presets().preset(presetTitle).click();
        }
    }

    @Test
    @Category({Regression.class})
    @DisplayName("Отображение пресета")
    @Owner(DSVICHIHIN)
    public void shouldSeePreset() {
        basePageSteps.onCatalogMainPage().presets().preset(presetTitle)
                .waitUntil(hasClass(containsString("tabs__item_state_active")));
        basePageSteps.onCatalogMainPage().presets().presetItemsList().waitUntil(hasSize(PRESET_ITEMS_CNT))
                .forEach(item -> item.waitUntil(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @DisplayName("Кнопка «Следующие»")
    @Owner(DSVICHIHIN)
    public void shouldClickNextButton() {
        basePageSteps.onCatalogMainPage().presets().showMore().hover();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onCatalogMainPage().presets().showMore().click();
        basePageSteps.onCatalogMainPage().presets().presetItemsList()
                .waitUntil(hasSize(PRESET_ITEMS_CNT * 2)).forEach(item -> item.waitUntil(isDisplayed()));
        basePageSteps.onCatalogMainPage().presets().showMore().hover();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onCatalogMainPage().presets().showMore().click();
        basePageSteps.onCatalogMainPage().presets().presetItemsList()
                .waitUntil(hasSize(PRESET_ITEMS_CNT * 3)).forEach(item -> item.waitUntil(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @DisplayName("Кнопка «Показать все»")
    @Owner(DSVICHIHIN)
    public void shouldClickShowAllButton() {
        basePageSteps.onCatalogMainPage().presets().showMore().hover();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onCatalogMainPage().presets().showMore().click();
        basePageSteps.onCatalogMainPage().presets().showMore().hover();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onCatalogMainPage().presets().showMore().click();
        basePageSteps.onCatalogMainPage().presets().showMore().waitUntil(not(isDisplayed()));
        basePageSteps.onCatalogMainPage().presets().showAll().waitUntil(isDisplayed()).hover().click();
        urlSteps.path(CARS).path(ALL).replaceQuery(query).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @DisplayName("Клик по модели")
    @Owner(DSVICHIHIN)
    public void shouldClickPresetModel() {
        basePageSteps.onCatalogMainPage().presets().presetItemsList().should(hasSize(PRESET_ITEMS_CNT)).get(0)
                .click();
        basePageSteps.onCatalogModelPage().description().waitUntil(isDisplayed());
    }
}
