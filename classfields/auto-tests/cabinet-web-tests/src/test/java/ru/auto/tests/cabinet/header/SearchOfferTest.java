package ru.auto.tests.cabinet.header;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Шапка. Поиск по объявлением")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class SearchOfferTest {

    private static final int PRICE_AFTER = 500000;
    private static final int PRICE_BEFORE = 1000000;
    private static final String PRICE_TYPE_AFTER = "Цена от, \u20BD";
    private static final String PRICE_TYPE_BEFORE = "до";
    private static final String MARK = "Citroen";
    private static final String MODEL = "C5";
    private static final String VIN1 = "RUMKEEW7A00013653";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/UserOffersCarsUsedVin"),
                stub("cabinet/UserOffersCarsUsedMarkModel"),
                stub("cabinet/UserOffersCarsUsedMark"),
                stub("cabinet/UserOffersCarsUsed"),
                stub("cabinet/UserOffersCarsMarkModelsUsedActive")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Блок с параметрами поиска")
    @Owner(TIMONDL)
    public void shouldSeeSearchBlock() {
        steps.onCabinetOffersPage().salesFiltersBlock().should(hasText("Марка\nМодель\nПоколение\nЦена от, ₽\nдо\n" +
                "Год, c\nдо\nНа складе, от\nдо, дней\nVIN номер или несколько через запятую\nПроверки по VIN\n" +
                "С услугами\nВсе параметры\nПоказано 2 объявления\nВыбрать все\nВыбрано 0\nСортировка"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Поиск по VIN-коду")
    public void shouldFindByVin() {
        inputVin(VIN1);
        steps.onCabinetOffersPage().salesFiltersBlock().buttonContains("Показать").click();
        steps.onCabinetOffersPage().snippets().waitUntil(hasSize(1));
        steps.onCabinetOffersPage().snippet(0).vin().should(hasText(VIN1));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Поиск по марке")
    public void shouldFindByMark() {
        selectMark(MARK);
        waitSomething(2, TimeUnit.SECONDS);
        steps.onCabinetOffersPage().salesFiltersBlock().buttonContains("Показать").click();
        waitSomething(2, TimeUnit.SECONDS);
        steps.onCabinetOffersPage().snippets().should(not(empty()))
                .forEach(item -> item.title().should(hasText(containsString(MARK))));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Поиск по марке и модели")
    public void shouldFindByMarkAndModel() {
        selectMark(MARK);
        selectModel(MODEL);
        waitSomething(2, TimeUnit.SECONDS);
        steps.onCabinetOffersPage().salesFiltersBlock().buttonContains("Показать").click();
        waitSomething(2, TimeUnit.SECONDS);
        steps.onCabinetOffersPage().snippets().should(not(empty()))
                .forEach(item -> item.title().should(hasText(containsString(format("%s %s", MARK, MODEL)))));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Поиск по цене")
    public void shouldFindByPriceModel() {
        waitSomething(2, TimeUnit.SECONDS);
        setPrice(PRICE_TYPE_AFTER, PRICE_AFTER);
        setPrice(PRICE_TYPE_BEFORE, PRICE_BEFORE);
        List<Integer> offersPrices = steps.onCabinetOffersPage().snippets().stream()
                .map(snippet -> snippet.priceBlock().offerPrice())
                .collect(toList());
        shouldSeeOffersFoundByPrice(offersPrices, PRICE_AFTER, PRICE_BEFORE);
    }

    @Step("Вводим VIN-код {vin}")
    private void inputVin(String vin) {
        steps.onCabinetOffersPage().salesFiltersBlock().input("VIN", vin);
    }

    @Step("Выбираем марку {mark}")
    private void selectMark(String mark) {
        steps.onCabinetOffersPage().salesFiltersBlock().selectItem("Марка", mark);
    }

    @Step("Выбираем модель {model}")
    private void selectModel(String model) {
        steps.onCabinetOffersPage().salesFiltersBlock().selectItem("Модель", model);

    }

    @Step("Устанавливаем цену {type} {price}")
    private void setPrice(String type, int price) {
        steps.onCabinetOffersPage().salesFiltersBlock().input(type, valueOf(price));
    }

    @Step("Цены найденных офферов должны быть в приделах от {priceAfter} до {priceBefore}")
    private void shouldSeeOffersFoundByPrice(List<Integer> offersPrices, int priceAfter, int priceBefore) {
        assertThat(offersPrices, everyItem(anyOf(greaterThanOrEqualTo(priceAfter), lessThan(priceBefore))));
    }
}
