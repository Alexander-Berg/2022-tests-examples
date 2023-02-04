package ru.auto.tests.desktop.lk.sales.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Фильтры объявлений. Легковые")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_RESELLER)
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class FiltersTest {

    private static final String VIN = "X6D212140J9003231";
    private static final String PRICE_FROM = "700000";
    private static final String PRICE_TO = "800000";
    private static final String MARK = "LADA (ВАЗ)";
    private static final String MODEL = "2121 (4x4)";

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
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-lk/UserFavoriteReseller",
                "desktop-lk/UserOffersCarsActiveWithEmptyQuery",
                "desktop-lk/UserOffersCarsMarkModels").post();

        urlSteps.testing().path(MY).path(RESELLER).path(CARS).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поиск по VIN")
    public void shouldSearchByVin() {
        mockRule.with("desktop-lk/UserOffersCarsActiveByVin").update();
        basePageSteps.onLkResellerSalesPage().filters().input("VIN", VIN);

        urlSteps.testing().path(MY).path(RESELLER).path(CARS).addParam("vin", VIN).shouldNotSeeDiff();
        basePageSteps.onLkResellerSalesPage().salesList().should(hasSize(1));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поиск по марке")
    public void shouldSearchByMark() {
        mockRule.with("desktop-lk/UserOffersCarsActiveByMark").update();

        basePageSteps.onLkResellerSalesPage().filters().button("Марка").click();
        basePageSteps.onLkResellerSalesPage().item(MARK).waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MY).path(RESELLER).path(CARS).addParam("mark_model", "VAZ").shouldNotSeeDiff();
        basePageSteps.onLkResellerSalesPage().salesList().should(hasSize(1));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поиск по марке и модели")
    public void shouldSearchByMarkAndModel() {
        mockRule.with("desktop-lk/UserOffersCarsActiveByMarkModel").update();

        basePageSteps.onLkResellerSalesPage().filters().button("Марка").click();
        basePageSteps.onLkResellerSalesPage().item(MARK).waitUntil(isDisplayed()).click();
        basePageSteps.onLkResellerSalesPage().filters().button("Модель").click();
        basePageSteps.onLkResellerSalesPage().item(MODEL).waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MY).path(RESELLER).path(CARS).addParam("mark_model", "VAZ%232121").shouldNotSeeDiff();
        basePageSteps.onLkResellerSalesPage().salesList().should(hasSize(1));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поиск по цене От и До")
    public void shouldSearchByPriceFromAndTo() {
        mockRule.with("desktop-lk/UserOffersCarsActiveByPrice").update();

        basePageSteps.onLkResellerSalesPage().input("От, ₽", PRICE_FROM);
        basePageSteps.onLkResellerSalesPage().input("до", PRICE_TO);

        urlSteps.testing().path(MY).path(RESELLER).path(CARS)
                .addParam("price_from", PRICE_FROM).addParam("price_to", PRICE_TO).shouldNotSeeDiff();
        basePageSteps.onLkResellerSalesPage().salesList().should(hasSize(1));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переключатель «Графики у всех»")
    public void shouldToggleCharts() {
        basePageSteps.onLkResellerSalesPage().filters().showAllChartsToggle().click();
        basePageSteps.onLkResellerSalesPage().salesList().forEach(offer -> offer.chart().should(not(isDisplayed())));

        basePageSteps.onLkResellerSalesPage().filters().showAllChartsToggle().click();
        basePageSteps.onLkResellerSalesPage().salesList().forEach(offer -> offer.chart().should(isDisplayed()));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Залипающий при скролле фильтр")
    public void shouldSeeStickyFilterOnScroll() {
        basePageSteps.onLkResellerSalesPage().filtersSticky().should(not(isDisplayed()));
        basePageSteps.onLkResellerSalesPage().footer().hover();
        basePageSteps.onLkResellerSalesPage().filtersSticky().should(isDisplayed());
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подсказка про показ/скрытие всех графиков")
    public void shouldSeeChartsToggleTooltip() {
        basePageSteps.onLkResellerSalesPage().getSale(0).chartToggleButton().click();
        basePageSteps.onLkResellerSalesPage().getSale(0).chartToggleButton().click();
        basePageSteps.onLkResellerSalesPage().getSale(0).chartToggleButton().click();

        basePageSteps.onLkResellerSalesPage().popup()
                .should(hasText("Здесь можно скрыть или раскрыть графики для всех объявлений"));
    }
}
