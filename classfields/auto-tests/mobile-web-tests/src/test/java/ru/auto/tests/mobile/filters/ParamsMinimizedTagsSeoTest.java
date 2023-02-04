package ru.auto.tests.mobile.filters;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Расширенные фильтры - тэги, ЧПУ")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParamsMinimizedTagsSeoTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String filterName;

    @Parameterized.Parameter(3)
    public String tag;

    @Parameterized.Parameter(4)
    public String path;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {4}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "Кузов", "Седан", "body-sedan"},
                {CARS, ALL, "Кузов", "Хэтчбек 3 дв.", "body-hatchback_3_doors"},
                {CARS, ALL, "Кузов", "Хэтчбек 5 дв.", "body-hatchback_5_doors"},
                {CARS, ALL, "Кузов", "Лифтбек", "body-liftback"},
                {CARS, ALL, "Кузов", "Внедорожник 3 дв.", "body-allroad_3_doors"},
                {CARS, ALL, "Кузов", "Внедорожник 5 дв.", "body-allroad_5_doors"},
                {CARS, ALL, "Кузов", "Универсал", "body-wagon"},
                {CARS, ALL, "Кузов", "Купе", "body-coupe"},
                {CARS, ALL, "Кузов", "Минивэн", "body-minivan"},
                {CARS, ALL, "Кузов", "Пикап", "body-pickup"},
                {CARS, ALL, "Кузов", "Лимузин", "body-limousine"},
                {CARS, ALL, "Кузов", "Фургон", "body-van"},
                {CARS, ALL, "Кузов", "Кабриолет", "body-cabrio"},
                {CARS, ALL, "Двигатель", "Бензин", "engine-benzin"},
                {CARS, ALL, "Двигатель", "Дизель", "engine-dizel"},
                {CARS, ALL, "Двигатель", "Гибрид", "engine-gibrid"},
                {CARS, ALL, "Двигатель", "Электро", "engine-electro"},
                {CARS, ALL, "Двигатель", "Турбированный", "engine-turbo"},
                {CARS, ALL, "Двигатель", "Атмосферный", "engine-atmo"},
                {CARS, ALL, "Двигатель", "Газобаллонное оборудование", "engine-lpg"},
                {CARS, ALL, "Привод", "Передний", "drive-forward_wheel"},
                {CARS, ALL, "Цвет", "Белый", "color-belyj"},
                {CARS, ALL, "Коробка", "Робот", "transmission-robot"},
                {CARS, ALL, "Коробка", "Вариатор", "transmission-variator"},
                {CARS, ALL, "Коробка", "Механическая", "transmission-mechanical"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
        basePageSteps.onListingPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().param(filterName).hover().click();
    }

    @Test
    @DisplayName("Тэги")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldClickTag() {
        basePageSteps.onListingPage().paramsPopup().tags(filterName).button(tag).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.path(path).path("/").shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
        urlSteps.refresh();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
    }
}
