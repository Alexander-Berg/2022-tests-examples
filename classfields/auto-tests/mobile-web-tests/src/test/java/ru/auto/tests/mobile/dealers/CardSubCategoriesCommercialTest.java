package ru.auto.tests.mobile.dealers;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.AGRICULTURAL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ARTIC;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(DEALERS)
@DisplayName("Подкатегории мото")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardSubCategoriesCommercialTest {

    private static final String DEALER_CODE = "/trak_stok_moskva/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startSubCategory;

    @Parameterized.Parameter(1)
    public String subCategory;

    @Parameterized.Parameter(2)
    public String subCategoryUrl;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {LCV, "Грузовики", TRUCK},
                {LCV, "Седельные тягачи", ARTIC},
                {LCV, "Автобусы", BUS},
                {LCV, "Прицепы и кузова", TRAILER},
                {LCV, "Сельхоз", AGRICULTURAL},
                {LCV, "Строительная", CONSTRUCTION},
                {LCV, "Автокраны", CRANE},
                {LCV, "Экскаваторы", DREDGE},
                {LCV, "Бульдозеры", BULLDOZERS},
                {LCV, "Коммунальная", MUNICIPAL},
                {TRUCK, "Лёгкий комтранс", LCV}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(DILER_OFICIALNIY).path(startSubCategory).path(ALL).path(DEALER_CODE).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по подкатегории")
    public void shouldClickSubCategory() {
        basePageSteps.onDealerCardPage().subCategories().subCategory(subCategory).should(isDisplayed()).click();
        urlSteps.testing().path(DILER_OFICIALNIY).path(subCategoryUrl).path(ALL).path(DEALER_CODE).shouldNotSeeDiff();
    }
}
