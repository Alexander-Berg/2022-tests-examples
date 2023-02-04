package ru.auto.tests.cabinet.backonsale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.TestData.CLIENT_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.BACK_ON_SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.CATALOG_FILTER;
import static ru.auto.tests.desktop.consts.QueryParams.CREATION_DATE_FROM;
import static ru.auto.tests.desktop.consts.QueryParams.CREATION_DATE_TO;
import static ru.auto.tests.desktop.consts.QueryParams.SORTING;
import static ru.auto.tests.desktop.element.cabinet.backonsale.Filters.ALL_PARAMETERS;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_BACK_ON_SALE_PLACEHOLDER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Кабинет дилера. Снова в продаже. Блок фильтров")
@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.BACK_ON_SALE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class FiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    public LoginSteps loginSteps;

    @Before
    public void before() throws IOException {
        loginSteps.loginAs(CLIENT_PROVIDER.get());

        cookieSteps.setCookieForBaseDomain(IS_SHOWING_BACK_ON_SALE_PLACEHOLDER, "1");
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Разворачивание и сворачивание блока фильтров")
    public void shouldExpandAndHide() {
        basePageSteps.onCabinetOnSaleAgainPage().filters().select("Состояние при продаже")
                .should(not(isDisplayed()));

        basePageSteps.onCabinetOnSaleAgainPage().filters().expansionButton().should(hasText(ALL_PARAMETERS))
                .click();
        basePageSteps.onCabinetOnSaleAgainPage().filters().select("Тип записи").should(isDisplayed());

        basePageSteps.onCabinetOnSaleAgainPage().filters().expansionButton().should(hasText("Свернуть")).click();
        basePageSteps.onCabinetOnSaleAgainPage().filters().select("Тип записи").should(not(isDisplayed()));
        basePageSteps.onCabinetOnSaleAgainPage().filters().expansionButton().should(hasText(ALL_PARAMETERS));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Сброс фильтров")
    public void shouldRestFilters() {
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).addParam(CATALOG_FILTER, "mark=TOYOTA,model=COROLLA").open();
        basePageSteps.onCabinetOnSaleAgainPage().filters().resetButton().should(isDisplayed()).click();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).ignoreParams(CREATION_DATE_FROM, CREATION_DATE_TO, SORTING)
                .shouldNotSeeDiff();
        basePageSteps.onCabinetOnSaleAgainPage().filters().resetButton().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Поиск по марке")
    public void shouldSearchMark() {
        basePageSteps.onCabinetOnSaleAgainPage().filters().selectItem("Марка", "Toyota");
        basePageSteps.onCabinetOnSaleAgainPage().filters().searchButton().click();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).ignoreParams(CREATION_DATE_FROM, CREATION_DATE_TO, SORTING)
                .addParam(CATALOG_FILTER, "mark=TOYOTA").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Поиск по марке, модели")
    public void shouldSearchMarkModel() {
        basePageSteps.onCabinetOnSaleAgainPage().filters().selectItem("Марка", "Toyota");
        basePageSteps.onCabinetOnSaleAgainPage().filters().selectItem("Модель", "Corolla");
        basePageSteps.onCabinetOnSaleAgainPage().filters().searchButton().click();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).ignoreParams(CREATION_DATE_FROM, CREATION_DATE_TO, SORTING)
                .addParam(CATALOG_FILTER, "mark=TOYOTA,model=COROLLA").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Поиск по марке, модели, поколению")
    public void shouldSearchMarkModelGen() {
        basePageSteps.onCabinetOnSaleAgainPage().filters().selectItem("Марка", "Toyota");
        basePageSteps.onCabinetOnSaleAgainPage().filters().selectItem("Модель", "Corolla");
        basePageSteps.onCabinetOnSaleAgainPage().filters().selectItem("Поколение", "XII (E210)");
        basePageSteps.onCabinetOnSaleAgainPage().filters().searchButton().click();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).ignoreParams(CREATION_DATE_FROM, CREATION_DATE_TO, SORTING)
                .addParam(CATALOG_FILTER, "mark=TOYOTA,model=COROLLA,generation=21491371")
                .shouldNotSeeDiff();
    }

}
