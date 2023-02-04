package ru.auto.tests.bem.catalog;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.EQUIPMENT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Каталог - карточка кузова - комплектации")
@Feature(AutoruFeatures.CATALOG)
public class BodyEquipmentTest {

    private static final String MARK = "audi";
    private static final String MODEL = "q7";
    private static final String GENERATION = "21646875";
    private static final String BODY = "21646934";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(GENERATION).path(BODY).path(EQUIPMENT).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор опций комплектации")
    @Category({Regression.class})
    public void shouldSelectComplectationOptions() {
        basePageSteps.onCatalogPage().getOption(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCatalogPage().complectationCalculatorPanel().waitUntil(hasText("Стоимость комплектации\n" +
                "6 040 000₽\nВыбранные опции\n29 700₽\nОбщая стоимость\n6 069 700₽"));
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCatalogPage().getOptionPackage(0), 0, 200);
        basePageSteps.onCatalogPage().getOptionPackage(0).click();
        basePageSteps.onCatalogPage().getOptionPackage(0).checkbox().click();
        basePageSteps.onCatalogPage().complectationCalculator().waitUntil(hasText("Стоимость комплектации\n" +
                "6 040 000₽\nВыбранные опции\n83 899₽\nОбщая стоимость\n6 123 899₽"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Просмотр другой комплектации")
    public void shouldSeeOtherComplectation() {
        basePageSteps.onCatalogPage().getComplectation(1).modificationUrl().click();
        basePageSteps.onCatalogPage().complectationTitle()
                .should(hasText(equalToIgnoringCase(basePageSteps.onCatalogPage().getComplectation(1).getText())));
    }
}
