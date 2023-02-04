package ru.auto.tests.desktop.group;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Групповая карточка - фильтры")
@Feature(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GroupFiltersTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";
    private static final int PAGE_SIZE = 10;

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsMarkModelGroup",
                "desktop/SearchCarsGroupContextGroup",
                "desktop/SearchCarsGroupContextListing",
                "desktop/SearchCarsGroupComplectations",
                "desktop/SearchCarsEquipmentFiltersKiaOptima",
                "desktop/ReferenceCatalogCarsComplectations",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsTechInfo",
                "desktop/ReferenceCatalogCarsTechParam",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree",
                "desktop/ReferenceCatalogTagsV1New").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтрация по двигателю")
    public void shouldFilterByEngine() {
        mockRule.with("desktop/SearchCarsCountEngine",
                "desktop/SearchCarsEquipmentFiltersEngine",
                "desktop/SearchCarsGroupContextGroupEngine").update();

        basePageSteps.onGroupPage().groupOffers().filters()
                .selectItem("Двигатель", "Бензин 2.0 л, 150 л.c.");
        urlSteps.addParam("catalog_filter",
                        "mark=KIA,model=OPTIMA,generation=21342050,configuration=21342121,tech_param=21342125")
                .shouldNotSeeDiff();
        basePageSteps.onGroupPage().groupOffers().filters().button("Показать 3\u00a0предложения").click();
        basePageSteps.onGroupPage().groupOffersList().should(hasSize(3));
        basePageSteps.onGroupPage().groupOffers().filters().select("Бензин 2.0 л, 150 л.c.")
                .should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтрация по коробке")
    public void shouldFilterByTransmission() {
        mockRule.with("desktop/SearchCarsCountTransmission",
                "desktop/SearchCarsGroupContextGroupTransmission").update();

        basePageSteps.onGroupPage().groupOffers().filters()
                .selectItem("Коробка", "Автоматическая");
        urlSteps.addParam("transmission", "AUTOMATIC").shouldNotSeeDiff();
        basePageSteps.onGroupPage().groupOffers().filters().button("Показать 22\u00a0предложения").click();
        basePageSteps.onGroupPage().groupOffersList().should(hasSize(10));
        basePageSteps.onGroupPage().groupOffers().filters().select("Автоматическая").should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтрация по приводу")
    public void shouldFilterByGearType() {
        mockRule.with("desktop/SearchCarsCountGearType",
                "desktop/SearchCarsGroupContextGroupGearType").update();

        basePageSteps.onGroupPage().groupOffers().filters().selectItem("Привод", "Передний");
        basePageSteps.onGroupPage().groupOffers().filters().select("Передний").click();
        urlSteps.addParam("gear_type", "FORWARD_CONTROL").shouldNotSeeDiff();
        basePageSteps.onGroupPage().groupOffers().filters().button("Показать 3\u00a0предложения").click();
        basePageSteps.onGroupPage().groupOffersList().should(hasSize(3));
        basePageSteps.onGroupPage().groupOffers().filters().select("Передний").should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтрация по цене")
    public void shouldFilterByPrice() {
        mockRule.with("desktop/SearchCarsCountPrice",
                "desktop/SearchCarsGroupContextGroupPriceFromPriceTo").update();

        basePageSteps.onGroupPage().groupOffers().filters().inputGroup("Цена").input("от")
                .sendKeys("100000");
        basePageSteps.onGroupPage().groupOffers().filters().inputGroup("Цена").input("до")
                .sendKeys("1000000");
        urlSteps.addParam("price_from", "100000").addParam("price_to", "1000000")
                .shouldNotSeeDiff();
        basePageSteps.onGroupPage().groupOffers().filters().button("Показать 3\u00a0предложения").click();
        basePageSteps.onGroupPage().groupOffersList().should(hasSize(3));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтрация по году")
    public void shouldFilterByYear() {
        mockRule.with("desktop/SearchCarsCountYearTo",
                "desktop/SearchCarsGroupContextGroupYearTo").update();

        basePageSteps.onGroupPage().groupOffers().filters().selectItem("Год до", "2020");
        basePageSteps.onGroupPage().groupOffers().filters().select("2020").click();
        urlSteps.addParam("year_to", "2020").shouldNotSeeDiff();
        basePageSteps.onGroupPage().groupOffers().filters().button("Показать 3\u00a0предложения").click();
        basePageSteps.onGroupPage().groupOffersList().should(hasSize(3));
        basePageSteps.onGroupPage().groupOffers().filters().select("2020").should(isDisplayed());
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтрация по цвету")
    public void shouldFilterByColor() {
        mockRule.with("desktop/SearchCarsCountColor",
                "desktop/SearchCarsEquipmentFiltersColor",
                "desktop/SearchCarsGroupContextGroupColor").update();

        basePageSteps.onGroupPage().groupOffers().filters().select("Цвет").click();
        basePageSteps.onGroupPage().groupColorsPopup().getColor(0).click();
        urlSteps.addParam("color", "040001").shouldNotSeeDiff();
        basePageSteps.onGroupPage().groupOffers().filters().button("Показать 12\u00a0предложений").click();
        basePageSteps.onGroupPage().groupColorsPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onGroupPage().groupOffersList().get(0).info()
                .waitUntil(hasText(containsString("Чёрный")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтрация по тегу")
    public void shouldFilterByTag() {
        mockRule.with("desktop/SearchCarsCountSearchTagBig",
                "desktop/SearchCarsGroupContextGroupSearchTagBig").update();

        basePageSteps.onGroupPage().groupOffers().filters().button("Все параметры").click();
        basePageSteps.onGroupPage().groupOffers().filters().button("Большой").click();
        urlSteps.addParam("search_tag", "big").shouldNotSeeDiff();
        basePageSteps.onGroupPage().groupOffers().filters().button("Показать 3\u00a0предложения").click();
        basePageSteps.onGroupPage().groupOffersList().should(hasSize(3));
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтрация по наличию")
    public void shouldFilterByStock() {
        mockRule.with("desktop/SearchCarsGroupContextGroupInStock").update();

        basePageSteps.onGroupPage().checkbox("В наличии").click();
        urlSteps.addParam("in_stock", "IN_STOCK").shouldNotSeeDiff();
        basePageSteps.onGroupPage().groupOffers().title().should(hasText("61 предложение от 1 014 900 до 1 899 900 ₽"));
        basePageSteps.onGroupPage().groupOffersList().should(hasSize(10));
        basePageSteps.onGroupPage().getOffer(0).should(hasText("Prestige\nВ наличии\n2019\n2.4 л / 188 л.с." +
                " / Бензин\nАвтомат\nПередний\nСерый\n58 базовых опций\n3 доп. опции\nот 1 584 900 ₽\n1 779 900 ₽ " +
                "без скидок\nСкидки\nВ кредит\nдо 65 000 ₽\nВ трейд-ин\nдо 130 000 ₽\nМаксимальная\n195 000 ₽\n" +
                "Подробнее о предложении\nЛегкосплавные диски\nAndroid Auto\nПротивотуманные фары\n+58 опций\n" +
                "РОЛЬФ Центр Kia\n ПолежаевскаяХорошёвскаяМоскваРоссия\nПоказать контакты"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтрация по комплектации")
    public void shouldFilterByComplectation() {
        mockRule.with("desktop/SearchCarsCountComfort",
                "desktop/SearchCarsGroupContextGroupComfort",
                "desktop/SearchCarsEquipmentFiltersComfort").update();

        String complectation = "Comfort";

        basePageSteps.onGroupPage().groupOffers().filters().select("Комплектация и опции").click();
        basePageSteps.onGroupPage().groupComplectationsPopup().complectation(complectation).click();
        urlSteps.addParam("catalog_filter", "mark=KIA,model=OPTIMA,generation=21342050,configuration=21342121,complectation_name=Comfort")
                .shouldNotSeeDiff();
        basePageSteps.onGroupPage().groupComplectationsPopup().selectedComplectation().should(hasText(complectation));
        basePageSteps.onGroupPage().groupComplectationsPopup().button("Показать 12\u00a0предложений").click();
        basePageSteps.onGroupPage().groupComplectationsPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onGroupPage().groupOffersList().waitUntil(hasSize(PAGE_SIZE));
        basePageSteps.onGroupPage().groupOffersList().get(0).complectation().waitUntil(hasText(complectation));
    }

}
