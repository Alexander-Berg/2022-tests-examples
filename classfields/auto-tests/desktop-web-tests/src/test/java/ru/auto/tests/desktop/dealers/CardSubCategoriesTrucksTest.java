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
import static ru.auto.tests.desktop.consts.Pages.AGRICULTURAL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ARTIC;
import static ru.auto.tests.desktop.consts.Pages.AUTOLOADER;
import static ru.auto.tests.desktop.consts.Pages.BULLDOZERS;
import static ru.auto.tests.desktop.consts.Pages.BUS;
import static ru.auto.tests.desktop.consts.Pages.CONSTRUCTION;
import static ru.auto.tests.desktop.consts.Pages.CRANE;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.DREDGE;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MUNICIPAL;
import static ru.auto.tests.desktop.consts.Pages.TRAILER;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(DEALERS)
@DisplayName("Подкатегории КомТС")
@Story(DEALER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardSubCategoriesTrucksTest {

    private static final String DEALER_CODE = "/sollers_finans_moskva/";

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

    @Parameterized.Parameters(name = "name = {index}: {2} {4}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                {
                        "desktop/SearchTrucksBreadcrumbsLcv",
                        "desktop/SearchTrucksBreadcrumbs",
                        LCV, "Грузовики", TRUCK},
                {
                        "desktop/SearchTrucksBreadcrumbsLcv",
                        "desktop/SearchTrucksBreadcrumbsArctic",
                        LCV, "Седельные тягачи", ARTIC},
                {
                        "desktop/SearchTrucksBreadcrumbsLcv",
                        "desktop/SearchTrucksBreadcrumbsBus",
                        LCV, "Автобусы", BUS},
                {
                        "desktop/SearchTrucksBreadcrumbsLcv",
                        "desktop/SearchTrucksBreadcrumbsTrailer",
                        LCV, "Прицепы и полуприцепы", TRAILER},
                {
                        "desktop/SearchTrucksBreadcrumbsLcv",
                        "desktop/SearchTrucksBreadcrumbsAgricultural",
                        LCV, "Сельскохозяйственная", AGRICULTURAL},
                {
                        "desktop/SearchTrucksBreadcrumbsLcv",
                        "desktop/SearchTrucksBreadcrumbsConstruction",
                        LCV, "Строительная и дорожная", CONSTRUCTION},
                {
                        "desktop/SearchTrucksBreadcrumbsLcv",
                        "desktop/SearchTrucksBreadcrumbsAutoloader",
                        LCV, "Погрузчики", AUTOLOADER},
                {
                        "desktop/SearchTrucksBreadcrumbsLcv",
                        "desktop/SearchTrucksBreadcrumbsCrane",
                        LCV, "Автокраны", CRANE},
                {
                        "desktop/SearchTrucksBreadcrumbsLcv",
                        "desktop/SearchTrucksBreadcrumbsDredge",
                        LCV, "Экскаваторы", DREDGE},
                {
                        "desktop/SearchTrucksBreadcrumbsLcv",
                        "desktop/SearchTrucksBreadcrumbsBulldozers",
                        LCV, "Бульдозеры", BULLDOZERS},
                {
                        "desktop/SearchTrucksBreadcrumbsLcv",
                        "desktop/SearchTrucksBreadcrumbsMunicipal",
                        LCV, "Коммунальная", MUNICIPAL},
                {
                        "desktop/SearchTrucksBreadcrumbs",
                        "desktop/SearchTrucksBreadcrumbsLcv",
                        TRUCK, "Лёгкие коммерческие", LCV
                }
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionUnauth"),
                stub("desktop/SearchTrucksCount"),
                stub("desktop/SearchTrucksCountCategories"),
                stub("desktop/SalonTrucks"),
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