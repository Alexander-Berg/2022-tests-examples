package ru.auto.tests.mobile.filters;

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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;

@DisplayName("Сброс параметров")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParamsResetTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "top_days=1&autoru_body_type=CABRIO&color=FAFBFB&dealer_org_type=4" +
                        "&displacement_from=200&displacement_to=2000&engine_type=DIESEL&gear_type=REAR_DRIVE" +
                        "&is_clear=true&km_age_from=100&km_age_to=100000" +
                        "&mark-model-nameplate=AUDI%23100%23%237879464&owners_count=1&owning_time=0_12&power_from=100" +
                        "&power_to=500&price_from=100000&price_to=10000000&pts_status=1&search_tag=certificate" +
                        "&steering_wheel=RIGHT&video=true&warranty_status=1" +
                        "&year_from=2006&year_to=2017&exchange_status=1"},
                {CARS, USED, "mark-model-nameplate=AUDI%23100%23%237879464"},
                {CARS, NEW, "mark-model-nameplate=AUDI%23100%23%237879464"},

                {TRUCK, ALL, "sort_offers=cr_date-DESC&top_days=31&body_key=GASOLINE_TANKER" +
                        "&cabin_key=2_SEAT_WO_SLEEP&dealer_org_type=1_2_3_5&displacement_from=200" +
                        "&displacement_to=1400&engine_type=GASOLINE&euro_class=1&exchange_status=1&in_stock=IN_STOCK" +
                        "&km_age_from=100&km_age_to=100000&loading=0_1500&mark-model-nameplate=HYUNDAI%23HD78" +
                        "&power_from=10&power_to=100&price_from=100000&price_to=1000000&steering_wheel=RIGHT" +
                        "&suspension_cabin=MECHANICAL&suspension_chassis=SPRING_SPRING&transmission_full=MECHANICAL" +
                        "&truck_color=FFFFFF&wheel_drive=10x6&year_from=2010&year_to=2018&haggle=POSSIBLE"},

                {MOTORCYCLE, ALL, "top_days=31&sort_offers=cr_date-DESC&cylinders=10" +
                        "&cylinders_type=V_TYPE&dealer_org_type=4&displacement_from=50&displacement_to=2000" +
                        "&drive_key=CHAIN&engine_type=INJECTOR&exchange_status=1&km_age_from=100&km_age_to=100000" +
                        "&mark-model-nameplate=APRILIA%23CLASSIC_50&moto_color=FF0000&moto_type=OFF_ROAD_GROUP" +
                        "&power_from=100&power_to=500&price_from=100000&price_to=1000000&strokes=2" +
                        "&transmission_full=5&year_from=2009&year_to=2018&in_stock=IN_STOCK"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).replaceQuery(url).open();
        basePageSteps.onListingPage().filters().paramsButton().click();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Сбросить»")
    public void shouldClickResetButton() {
        basePageSteps.onListingPage().paramsPopup().resetButton().click();
        urlSteps.testing().path(MOSKVA).path(category).path(section).shouldNotSeeDiff();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
    }
}
