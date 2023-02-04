package ru.auto.tests.mobile.footer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FOOTER;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Футер")
@Feature(FOOTER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FooterDealerCardTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение футера")
    public void shouldSeeFooter() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/Salon",
                "desktop/SalonPhones",
                "mobile/SearchCarsAllDealerIdEmpty").post();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path(CARS_OFFICIAL_DEALER).open();
        basePageSteps.onDealerCardPage().footer().waitUntil(isDisplayed()).should(hasText("Дилерам\nО проекте\nПомощь" +
                "\nАналитика Авто.ру\nСаша Котов\nПриложение Авто.ру\nПолная версия\nСтань частью команды" +
                "\nАвто.ру — один из самых посещаемых автомобильных сайтов в российском интернете\nМы предлагаем " +
                "большой выбор легковых автомобилей, грузового и коммерческого транспорта, мототехники, спецтехники " +
                "и многих других видов транспортных средств\n© 1996–2022 ООО «Яндекс.Вертикали»"
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Ссылка «Полная версия»")
    public void shouldClickDesktopUrl() {
        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL)
                .path(CARS_OFFICIAL_DEALER).path("/audi/a3/")
                .addParam("year_to", "2018")
                .addParam("sort", "year-asc")
                .addParam("catalog_equipment", "ptf")
                .addParam("price_from", "1000")
                .addParam("price_to", "100000")
                .addParam("transmission", "AUTO")
                .addParam("engine_group", "GASOLINE")
                .addParam("displacement_from", "200")
                .addParam("displacement_to", "3000")
                .addParam("gear_type", "FORWARD_CONTROL")
                .addParam("power_from", "100")
                .addParam("power_to", "500")
                .addParam("km_age_from", "100")
                .addParam("km_age_to", "500")
                .addParam("acceleration_from", "1")
                .addParam("acceleration_to", "10")
                .addParam("fuel_rate_to", "100")
                .addParam("clearance_from", "100")
                .addParam("trunk_volume_from", "100")
                .addParam("color", "040001")
                .addParam("color", "FAFBFB")
                .addParam("steering_wheel", "RIGHT")
                .addParam("seller_group", "PRIVATE")
                .addParam("owners_count_group", "ONE")
                .addParam("owning_time_group", "LESS_THAN_YEAR")
                .addParam("damage_group", "ANY")
                .addParam("customs_state_group", "DOESNT_MATTER")
                .addParam("pts_status", "1")
                .addParam("search_tag", "certificate_manufacturer")
                .addParam("online_view", "true")
                .addParam("with_warranty", "true")
                .addParam("exchange_group", "POSSIBLE")
                .addParam("on_credit", "true")
                .addParam("with_delivery", "NONE")
                .addParam("top_days", "1").open();

        basePageSteps.hideElement(basePageSteps.onDealerCardPage().showPhoneButton());
        basePageSteps.onDealerCardPage().footer().button("Полная версия").click();

        urlSteps.desktopURI().path(DILER_OFICIALNIY).path(CARS).path(ALL)
                .path(CARS_OFFICIAL_DEALER).path("/audi/a3/")
                .addParam("nomobile", "true")
                .addParam("year_to", "2018")
                .addParam("sort", "year-asc")
                .addParam("catalog_equipment", "ptf")
                .addParam("price_from", "1000")
                .addParam("price_to", "100000")
                .addParam("transmission", "AUTO")
                .addParam("engine_group", "GASOLINE")
                .addParam("displacement_from", "200")
                .addParam("displacement_to", "3000")
                .addParam("gear_type", "FORWARD_CONTROL")
                .addParam("power_from", "100")
                .addParam("power_to", "500")
                .addParam("km_age_from", "100")
                .addParam("km_age_to", "500")
                .addParam("acceleration_from", "1")
                .addParam("acceleration_to", "10")
                .addParam("fuel_rate_to", "100")
                .addParam("clearance_from", "100")
                .addParam("trunk_volume_from", "100")
                .addParam("color", "040001")
                .addParam("color", "FAFBFB")
                .addParam("steering_wheel", "RIGHT")
                .addParam("seller_group", "PRIVATE")
                .addParam("owners_count_group", "ONE")
                .addParam("owning_time_group", "LESS_THAN_YEAR")
                .addParam("damage_group", "ANY")
                .addParam("customs_state_group", "DOESNT_MATTER")
                .addParam("pts_status", "1")
                .addParam("search_tag", "certificate_manufacturer")
                .addParam("online_view", "true")
                .addParam("with_warranty", "true")
                .addParam("exchange_group", "POSSIBLE")
                .addParam("on_credit", "true")
                .addParam("with_delivery", "NONE")
                .addParam("top_days", "1")
                .shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue("nomobile", "1");
    }
}
