package ru.auto.tests.mobile.group;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Групповая карточка - фильтры")
@Feature(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class GroupFiltersTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";

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
        mockRule.newMock().with("mobile/SearchCarsBreadcrumbsMarkModelGroup",
                "desktop/ReferenceCatalogTagsV1New",
                "mobile/SearchCarsGroupContextGroup",
                "mobile/SearchCarsGroupContextListing",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree",
                "mobile/SearchCarsCountKiaOptima").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтр по двигателю")
    public void shouldFilterByEngine() {
        mockRule.with("desktop/SearchCarsCountEngine",
                "mobile/SearchCarsGroupContextGroupEngine").update();

        basePageSteps.onGroupPage().filters().button("Двигатель").click();
        basePageSteps.onGroupPage().filtersPopup().item("Бензин 2.0 л, 150 л.c.").click();
        basePageSteps.onGroupPage().filtersPopup().applyFiltersButton().should(hasText("Показать 3 предложения"))
                .click();
        urlSteps.addParam("catalog_filter", "mark=KIA,model=OPTIMA,generation=21342050," +
                "configuration=21342121,tech_param=21342125").shouldNotSeeDiff();
        basePageSteps.onGroupPage().filtersPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onGroupPage().filters().button("Бензин 2.0 л, 150 л.c.").should(isDisplayed());
        basePageSteps.onGroupPage().sortBar().offersCount().should(hasText("3 предложения"));
        basePageSteps.onGroupPage().salesList().should(hasSize(greaterThan(0)));
        basePageSteps.onGroupPage().getSale(0).info().should(hasText("В наличии, 2020\n2.0 л / 150 л.с. /" +
                " Бензин\n55 базовых опций\nавтомат\nпередний\n5 доп. опций"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтр по коробке")
    public void shouldFilterByTransmission() {
        mockRule.with("desktop/SearchCarsCountTransmission",
                "mobile/SearchCarsGroupContextGroupTransmission").update();

        basePageSteps.onGroupPage().filters().button("Коробка").click();
        basePageSteps.onGroupPage().filtersPopup().item("Автоматическая").click();
        basePageSteps.onGroupPage().filtersPopup().applyFiltersButton().should(hasText("Показать 22 предложения"))
                .click();
        urlSteps.addParam("transmission", "AUTOMATIC").shouldNotSeeDiff();
        basePageSteps.onGroupPage().filtersPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onGroupPage().filters().button("Автоматическая").should(isDisplayed());
        basePageSteps.onGroupPage().sortBar().offersCount().should(hasText("22 предложения"));
        basePageSteps.onGroupPage().salesList().should(hasSize(greaterThan(0)));
        basePageSteps.onGroupPage().getSale(0).info().should(hasText("В наличии, 2020\n2.0 л / 150 л.с. /" +
                " Бензин\n55 базовых опций\nавтомат\nпередний\n5 доп. опций"));
    }


    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтр по приводу")
    public void shouldFilterByGear() {
        mockRule.with("desktop/SearchCarsCountGearType",
                "mobile/SearchCarsGroupContextGroupGearType").update();

        basePageSteps.onGroupPage().filters().button("Привод").click();
        basePageSteps.onGroupPage().filtersPopup().item("Передний").click();
        basePageSteps.onGroupPage().filtersPopup().applyFiltersButton().should(hasText("Показать 3 предложения"))
                .click();
        urlSteps.addParam("gear_type", "FORWARD_CONTROL").shouldNotSeeDiff();
        basePageSteps.onGroupPage().filtersPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onGroupPage().filters().button("Передний").should(isDisplayed());
        basePageSteps.onGroupPage().sortBar().offersCount().should(hasText("3 предложения"));
        basePageSteps.onGroupPage().salesList().should(hasSize(greaterThan(0)));
        basePageSteps.onGroupPage().getSale(0).info().should(hasText("В наличии, 2020\n2.0 л / 150 л.с. /" +
                " Бензин\n55 базовых опций\nавтомат\nпередний\n5 доп. опций"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтр по цвету")
    public void shouldFilterByColor() {
        mockRule.with("desktop/SearchCarsCountColor",
                "mobile/SearchCarsGroupContextGroupColor").update();

        basePageSteps.onGroupPage().filters().button("Цвет").click();
        basePageSteps.onGroupPage().filtersPopup().itemContains("чёрный").click();
        basePageSteps.onGroupPage().filtersPopup().applyFiltersButton().should(hasText("Показать 12 предложений"))
                .click();
        urlSteps.addParam("color", "040001").shouldNotSeeDiff();
        basePageSteps.onGroupPage().filtersPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onGroupPage().filters().button("Чёрный").should(isDisplayed());
        basePageSteps.onGroupPage().sortBar().offersCount().should(hasText("3 предложения"));
        basePageSteps.onGroupPage().salesList().should(hasSize(greaterThan(0)));
        basePageSteps.onGroupPage().getSale(0).info().should(hasText("В наличии, 2019\n2.0 л / 150 л.с. /" +
                " Бензин\n55 базовых опций\nавтомат\nпередний\n4 доп. опции"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтр по наличию")
    public void shouldFilterByStock() {
        mockRule.with("mobile/SearchCarsGroupContextGroupInStock").update();

        basePageSteps.onGroupPage().filters().button("В наличии").click();
        urlSteps.addParam("in_stock", "IN_STOCK").shouldNotSeeDiff();
        basePageSteps.onGroupPage().filters().button("В наличии").resetButton().should(isDisplayed());
        basePageSteps.onGroupPage().sortBar().offersCount().should(hasText("61 предложение"));
        basePageSteps.onGroupPage().salesList().should(hasSize(greaterThan(0)));
        basePageSteps.onGroupPage().getSale(0).info().should(hasText("В наличии, 2019\n2.4 л / 188 л.с. /" +
                " Бензин\n58 базовых опций\nавтомат\nпередний\n3 доп. опции"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтр по тегу")
    public void shouldFilterByTag() {
        mockRule.with("desktop/SearchCarsCountSearchTagBig",
                "mobile/SearchCarsGroupContextGroupSearchTagBig").update();

        basePageSteps.onGroupPage().filters().button("Параметры").click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onGroupPage().paramsPopup().button("Большой").click();
        urlSteps.addParam("search_tag", "big").shouldNotSeeDiff();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onGroupPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onGroupPage().sortBar().offersCount().should(hasText("3 предложения"));
        basePageSteps.onGroupPage().salesList().should(hasSize(3));
    }
}
