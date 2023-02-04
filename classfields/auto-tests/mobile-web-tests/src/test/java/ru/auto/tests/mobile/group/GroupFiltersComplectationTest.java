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
@GuiceModules(MobileEmulationTestsModule.class)
public class GroupFiltersComplectationTest {

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
        mockRule.newMock().with(
                "desktop/SessionAuthUser",
                "mobile/SearchCarsBreadcrumbsMarkModelGroup",
                "mobile/SearchCarsGroupContextGroup",
                "mobile/SearchCarsGroupContextListing",
                "mobile/SearchCarsGroupComplectations",
                "desktop/ReferenceCatalogTagsV1New",
                "desktop/ReferenceCatalogCarsComplectations",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsTechInfo",
                "desktop/ReferenceCatalogCarsTechParam",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree",
                "mobile/SearchCarsGroupContextGroupComfort",
                "mobile/SearchCarsEquipmentFiltersComfort",
                "mobile/SearchCarsCountKiaOptima").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Фильтр по комплектации")
    public void shouldFilterByComplectation() {
        String complectation = "Comfort";
        basePageSteps.onGroupPage().filters().button("Комплектация").click();
        basePageSteps.onGroupPage().filtersPopup().complectation(complectation).click();
        basePageSteps.onGroupPage().filtersPopup().selectedComplectation().should(hasText(complectation));
        urlSteps.addParam("catalog_filter", "mark=KIA,model=OPTIMA,generation=21342050," +
                "configuration=21342121,complectation_name=Comfort").shouldNotSeeDiff();
        basePageSteps.onGroupPage().filtersPopup().applyFiltersButton().should(hasText("Показать 12 предложений"))
                .click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onGroupPage().filtersPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onGroupPage().filters().button(complectation).should(isDisplayed());
        basePageSteps.onGroupPage().sortBar().offersCount().should(hasText("12 предложений"));
        basePageSteps.onGroupPage().salesList().should(hasSize(greaterThan(0)));
        basePageSteps.onGroupPage().getSale(0).title().should(hasText(complectation));
        basePageSteps.onGroupPage().getSale(0).info().should(hasText("В наличии, 2019\n2.0 л / 150 л.с. /" +
                " Бензин\n44 базовые опции\nавтомат\nпередний\n4 доп. опции"));

    }
}
