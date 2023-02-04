package ru.auto.tests.desktop.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - пресеты - скоро в продаже")
@Feature(MAIN)
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class PresetsComingSoonTest {

    private static final String PRESET_TITLE = "Скоро в продаже";
    private static final int PRESET_NUM = 1;
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
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.setWideWindowSize(HEIGHT_1024);
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onMainPage().presets(), 0, 200);
        basePageSteps.onMainPage().presets().getPreset(PRESET_NUM).waitUntil(isDisplayed()).hover().click();
        basePageSteps.onMainPage().presets().getSale(0).getImg(0).waitUntil(isDisplayed());
    }


    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение пресета")
    public void shouldSeePreset() {
        basePageSteps.onMainPage().presets().preset(PRESET_TITLE)
                .waitUntil(hasClass(containsString("Radio_checked")));
        basePageSteps.onMainPage().presets().salesList().subList(0, PRESET_VISIBLE_SALES_CNT)
                .forEach(item -> {
                    item.waitUntil(isDisplayed());
                    item.getImg(0).waitUntil(isDisplayed());
                });
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        basePageSteps.onMainPage().presets().getSale(0).click();
        basePageSteps.onCatalogPage().h1().waitUntil(hasText(startsWith("Технические характеристики")));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @Ignore
    @DisplayName("Клик по кнопке «Обновить»")
    public void shouldRefreshPreset() {
        String saleUrl = basePageSteps.onMainPage().presets().getSale(0).url().getAttribute("href");
        basePageSteps.onMainPage().presets().refreshButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().presets().preset(PRESET_TITLE)
                .waitUntil(hasClass(containsString("Radio_checked")));
        basePageSteps.onMainPage().presets().getSale(0).url().waitUntil(hasAttribute("href",
                not(isEmptyString())));
        basePageSteps.onMainPage().presets().getSale(0).url().should(not(hasAttribute("href", saleUrl)));
        basePageSteps.onMainPage().presets().getSale(0).getImg(0).waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @Ignore
    @DisplayName("Скролл пресета")
    public void shouldScrollPreset() {
        basePageSteps.onMainPage().presets().prevButton().should(not(isDisplayed()));
        basePageSteps.onMainPage().presets().nextButton().hover().click();
        basePageSteps.onMainPage().presets().salesList().subList(0, 3).forEach(item -> {
            item.waitUntil(isDisplayed());
            item.getImg(0).waitUntil(isDisplayed());
        });
        basePageSteps.onMainPage().presets().prevButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().presets().salesList().subList(0, 3).forEach(item -> {
            item.waitUntil(isDisplayed());
            item.getImg(0).waitUntil(isDisplayed());
        });
        basePageSteps.onMainPage().presets().nextButton().waitUntil(isDisplayed());
        basePageSteps.onMainPage().presets().prevButton().waitUntil(not(isDisplayed()));
    }
}
