package ru.auto.tests.desktop.dealers;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ATV;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SCOOTERS;
import static ru.auto.tests.desktop.consts.Pages.SNOWMOBILE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(DEALERS)
@DisplayName("Подкатегории мото")
@Story(DEALER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardSubCategoriesMotoTest {

    private static final String DEALER_CODE = "/motogarazh_moskva/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startBreadcrumbsMock;

    @Parameterized.Parameter(1)
    public String resultBreadcrumbsMock;

    @Parameterized.Parameter(2)
    public String startSubCategory;

    @Parameterized.Parameter(3)
    public String subCategory;

    @Parameterized.Parameter(4)
    public String subCategoryUrl;

    @Parameterized.Parameters(name = "name = {index}: {2} {3} {4}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                {
                        "desktop/SearchMotoBreadcrumbsScooters",
                        "desktop/SearchMotoBreadcrumbs",
                        SCOOTERS, "Мотоциклы", MOTORCYCLE
                },
                {
                        "desktop/SearchMotoBreadcrumbs",
                        "desktop/SearchMotoBreadcrumbsScooters",
                        MOTORCYCLE, "Скутеры", SCOOTERS
                },
                {
                        "desktop/SearchMotoBreadcrumbs",
                        "desktop/SearchMotoBreadcrumbsAtv",
                        MOTORCYCLE, "Мотовездеходы", ATV
                },
                {
                        "desktop/SearchMotoBreadcrumbs",
                        "desktop/SearchMotoBreadcrumbsSnowmobile",
                        MOTORCYCLE, "Снегоходы", SNOWMOBILE
                }
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionUnauth"),
                stub("desktop/SearchMotoCount"),
                stub("desktop/SearchMotoCountCategories"),
                stub("desktop/SalonMoto"),
                stub(startBreadcrumbsMock),
                stub(resultBreadcrumbsMock)
        ).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(startSubCategory).path(ALL).path(DEALER_CODE).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по подкатегории")
    public void shouldClickSubCategory() {
        basePageSteps.onDealerCardPage().subCategories().subCategory(subCategory).should(isDisplayed()).click();

        urlSteps.testing().path(DILER_OFICIALNIY).path(subCategoryUrl).path(ALL).path(DEALER_CODE).shouldNotSeeDiff();
    }
}