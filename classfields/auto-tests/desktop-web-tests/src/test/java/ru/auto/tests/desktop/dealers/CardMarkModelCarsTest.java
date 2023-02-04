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
import org.openqa.selenium.Keys;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Карточка дилера - фильтр по марке/модели/поколению")
@Epic(DEALERS)
@Feature(DEALER_CARD)
@Story(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CardMarkModelCarsTest {

    private static final String DEALER = "/avilon_mercedes_benz_moskva_vozdvizhenka/";

    private static final String MARK = "Audi";
    private static final String MODEL = "A3";
    private static final String GENERATION = "III (8V) Рестайлинг";
    private static final String GENERATION_CODE = "20785010";

    private static final String MODEL_2 = "A8";
    private static final String NAMEPLATE = "A8 Long";
    private static final String NAMEPLATE_IN_URL = "long";

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
        mockRule.setStubs(stub("desktop/Salon"),
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/SearchCarsBreadcrumbsAudi"),
                stub("desktop/SearchCarsAllDealerId"),
                stub("desktop/SearchCarsCountDealerId")).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path(DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Выбор марки")
    public void shouldSelectMark() {
        mockRule.setStubs(stub("desktop/SearchCarsAllDealerIdAudi")).update();

        basePageSteps.onDealerCardPage().filter().selectItem("Марка", MARK);
        basePageSteps.onDealerCardPage().filter().select(MARK).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.path(MARK.toLowerCase()).path(SLASH).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.nameLink().should(hasText(startsWith(MARK))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Выбор модели")
    public void shouldSelectModel() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsAudi_A3"),
                stub("desktop/SearchCarsAllDealerIdAudi_A3")).update();

        basePageSteps.onDealerCardPage().filter().selectItem("Марка", MARK);
        basePageSteps.onDealerCardPage().filter().selectItem("Модель", MODEL);
        basePageSteps.onDealerCardPage().filter().select(MARK).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().select(MODEL).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.path(MARK.toLowerCase()).path(MODEL.toLowerCase()).path(SLASH).shouldNotSeeDiff();

        basePageSteps.onDealerCardPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.nameLink()
                .should(hasText(startsWith(format("%s %s", MARK, MODEL)))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Выбор поколения")
    public void shouldSelectGeneration() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsAudi_A3"),
                stub("desktop/SearchCarsBreadcrumbsAudi_A3_20785010"),
                stub("desktop/SearchCarsAllDealerIdAudi_A3_20785010")).update();

        basePageSteps.onDealerCardPage().filter().selectItem("Марка", MARK);
        basePageSteps.onDealerCardPage().filter().select("Модель").waitUntil(isEnabled());
        basePageSteps.onDealerCardPage().filter().selectItem("Модель", MODEL);
        basePageSteps.onDealerCardPage().filter().select("Поколение").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onDealerCardPage().filter().generationsPopup().generationItem(GENERATION).waitUntil(isDisplayed()).click();
        basePageSteps.onDealerCardPage().body().sendKeys(Keys.ESCAPE);
        basePageSteps.onDealerCardPage().filter().select(GENERATION).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        waitSomething(2, TimeUnit.SECONDS);
        urlSteps.path(MARK.toLowerCase()).path(MODEL.toLowerCase()).path(GENERATION_CODE).path(SLASH)
                .shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.nameLink()
                .should(hasText(startsWith(format("%s %s %s", MARK, MODEL, GENERATION)))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Выбор шильда")
    public void shouldSelectNameplate() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsAudi_A3"),
                stub("desktop/SearchCarsAllDealerIdAudi_A8_Long")).update();

        basePageSteps.onDealerCardPage().filter().selectItem("Марка", MARK);
        basePageSteps.onDealerCardPage().filter().select("Модель").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onDealerCardPage().filter().selectPopup().plusButton(MODEL_2).click();
        basePageSteps.onDealerCardPage().filter().selectPopup().item(NAMEPLATE).click();
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.path(MARK.toLowerCase()).path(MODEL_2.toLowerCase()).path(SLASH)
                .addParam("nameplate_name", NAMEPLATE_IN_URL).shouldNotSeeDiff();

        basePageSteps.onDealerCardPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.nameLink()
                .should(hasText(startsWith(format("%s %s", MARK, NAMEPLATE)))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Исключение марки")
    public void shouldExcludeMark() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsAudi"),
                stub("desktop/SearchCarsAllDealerIdExcludeAudi")).update();

        basePageSteps.onDealerCardPage().filter().select("Марка").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onDealerCardPage().filter().selectPopup().radioButton("Исключить").click();
        basePageSteps.onDealerCardPage().filter().selectPopup().item(MARK).click();
        basePageSteps.onDealerCardPage().filter().select(format("Кроме %s", MARK)).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.addParam("exclude_catalog_filter", format("mark=%s", MARK.toUpperCase()))
                .shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().waitForListingReload();
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.nameLink().should(not(hasText(containsString(MARK)))));
        basePageSteps.refresh();
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.nameLink().should(not(hasText(containsString(MARK)))));
    }
}
