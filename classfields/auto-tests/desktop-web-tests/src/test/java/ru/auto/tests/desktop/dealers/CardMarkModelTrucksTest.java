package ru.auto.tests.desktop.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Карточка дилера - фильтр по марке/модели")
@Epic(DEALERS)
@Feature(DEALER_CARD)
@Story(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CardMarkModelTrucksTest {

    private static final String DEALER = "/sollers_finans_moskva/";

    private static final String MARK = "ЗИЛ";
    private static final String MARK_URL = "zil";
    private static final String MODEL = "5301 \"Бычок\"";
    private static final String MODEL_URL = "5301";

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(stub("desktop/SalonTrucks"),
                stub("desktop/SearchTrucksBreadcrumbs"),
                stub("desktop/SearchTrucksBreadcrumbsMark"),
                stub("desktop/SearchTrucksAllDealerId"),
                stub("desktop/SearchTrucksAllDealerIdZil"),
                stub("desktop/SearchTrucksCount")).create();

        urlSteps.testing().path(DILER).path(TRUCK).path(ALL).path(DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Выбор марки")
    public void shouldSelectMark() {
        basePageSteps.onDealerCardPage().filter().selectItem("Марка", MARK);
        basePageSteps.onDealerCardPage().filter().select(MARK).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.path(MARK_URL).path(SLASH).shouldNotSeeDiff();

        basePageSteps.onDealerCardPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.nameLink()
                .should(hasText(startsWith(MARK))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Выбор модели")
    public void shouldSelectModel() {
        mockRule.setStubs(stub("desktop/SearchTrucksBreadcrumbsMarkModel"),
                stub("desktop/SearchTrucksAllDealerIdZil_5301")).update();

        basePageSteps.onDealerCardPage().filter().selectItem("Марка", MARK);
        basePageSteps.onDealerCardPage().filter().selectItem("Модель", MODEL);
        basePageSteps.onDealerCardPage().filter().select(MARK).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().select(MODEL).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.path(MARK_URL).path(MODEL_URL).path(SLASH).shouldNotSeeDiff();

        basePageSteps.onDealerCardPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.nameLink()
                .should(hasText(startsWith(format("%s %s", MARK, MODEL)))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Исключение марки")
    public void shouldExcludeMark() {
        mockRule.setStubs(stub("desktop/SearchTrucksAllDealerIdExcludeZil")).update();

        basePageSteps.onDealerCardPage().filter().select("Марка").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onDealerCardPage().filter().selectPopup().radioButton("Исключить").click();
        basePageSteps.onDealerCardPage().filter().selectPopup().item(MARK).click();
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.addParam("exclude_catalog_filter", format("mark=%s", MARK_URL.toUpperCase()))
                .shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.nameLink().should(not(hasText(containsString(MARK)))));
        basePageSteps.refresh();
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.nameLink().should(not(hasText(containsString(MARK)))));
    }
}
