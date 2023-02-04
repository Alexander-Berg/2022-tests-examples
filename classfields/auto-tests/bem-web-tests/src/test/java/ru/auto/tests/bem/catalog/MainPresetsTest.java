package ru.auto.tests.bem.catalog;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.CatalogPageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - главная - пресеты")
@Feature(AutoruFeatures.CATALOG)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MainPresetsTest {

    private static final Integer PRESET_ITEMS_CNT = 12;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public CatalogPageSteps catalogPageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String preset;

    @Parameterized.Parameter(1)
    public String query;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
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
        urlSteps.testing().path(CATALOG).open();
        catalogPageSteps.onCatalogPage().presets().title(preset).waitUntil(isDisplayed()).click();
    }

    @Test
    @Category({Regression.class})
    @DisplayName("Отображение пресетов")
    @Owner(DSVICHIHIN)
    public void shouldSeePreset() {
        catalogPageSteps.onCatalogPage().presets().activePreset().waitUntil(hasText(preset));
        catalogPageSteps.onCatalogPage().presets().content().presetItemsList().waitUntil(hasSize(PRESET_ITEMS_CNT))
                .forEach(item -> item.waitUntil(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @DisplayName("Показать еще")
    @Owner(DSVICHIHIN)
    public void shouldClickShowMoreButton() {
        catalogPageSteps.onCatalogPage().presets().content().showMore().waitUntil(isDisplayed()).click();
        catalogPageSteps.onCatalogPage().presets().content().showMore().waitUntil(not(isDisplayed()));
        catalogPageSteps.onCatalogPage().presets().content().showAll().waitUntil(isDisplayed());
        catalogPageSteps.onCatalogPage().presets().content().presetItemsList()
                .waitUntil(hasSize(PRESET_ITEMS_CNT * 2))
                .forEach(item -> item.waitUntil(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @DisplayName("Показать все")
    @Owner(DSVICHIHIN)
    public void shouldClickShowAllButton() {
        catalogPageSteps.onCatalogPage().presets().content().showMore().waitUntil(isDisplayed()).click();
        catalogPageSteps.onCatalogPage().presets().content().showAll().waitUntil(isDisplayed()).click();
        urlSteps.path(CARS).path(ALL).replaceQuery(query).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @DisplayName("Клик по модели")
    @Owner(DSVICHIHIN)
    public void shouldClickPresetModel() {
        catalogPageSteps.onCatalogPage().presets().content().presetItemsList().should(hasSize(PRESET_ITEMS_CNT)).get(0)
                .click();
        catalogPageSteps.onCatalogPage().title()
                .should(hasAttribute("textContent", anyOf(startsWith("Карточка модели"),
                        containsString("технические характеристики и\u00a0комплектации"))));
    }
}