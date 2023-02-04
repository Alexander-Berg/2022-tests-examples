package ru.auto.tests.forms.reviews;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Отзывы - переключатель категорий")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CategoryTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startCategory;

    @Parameterized.Parameter(1)
    public String category;

    @Parameterized.Parameter(2)
    public String categoryUrl;

    @Parameterized.Parameter(3)
    public String categoryDescription;

    @Parameterized.Parameters(name = "name = {index}: {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {MOTO, "Легковые", CARS, "автомобиле"},
                {CARS, "Мото", MOTO, "мототранспорте"},
                {CARS, "Коммерческие", TRUCKS, "коммерческом транспорте"}
        });
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор категории")
    public void shouldSelectCategory() {
        urlSteps.testing().path(startCategory).path(REVIEWS).path(ADD).open();
        basePageSteps.onFormsPage().radioButton(category).should(isDisplayed()).click();
        urlSteps.testing().path(categoryUrl).path(REVIEWS).path(ADD).shouldNotSeeDiff();
        basePageSteps.onFormsPage().h1().should(hasText(format("Оставьте отзыв о вашем %s", categoryDescription)));
    }
}