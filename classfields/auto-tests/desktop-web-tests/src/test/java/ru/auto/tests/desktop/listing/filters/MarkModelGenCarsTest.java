package ru.auto.tests.desktop.listing.filters;

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
import org.openqa.selenium.Keys;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
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
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Легковые - фильтр по марке, модели, поколению")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MarkModelGenCarsTest {

    private static final String MARK = "Audi";
    private static final String MODEL = "A8";
    private static final String NAMEPLATE = "A8 Long";
    private static final String NAMEPLATE_IN_URL = "a8-long";
    private static final String GENERATION = "III (D4) Рестайлинг";
    private static final String GENERATION_CODE = "20071435";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки")
    public void shouldSelectMark() {
        basePageSteps.onListingPage().filter().selectItem("Марка", MARK);
        basePageSteps.onListingPage().filter().select(MARK).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().waitForListingReload();

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK.toLowerCase()).path(ALL)
                .shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().salesList().forEach(sale -> sale.nameLink().should(hasText(startsWith(MARK))));
        basePageSteps.onListingPage().filter().select(MARK).waitUntil(isDisplayed());

        basePageSteps.refresh();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().salesList().forEach(sale -> sale.nameLink().should(hasText(startsWith(MARK))));
        basePageSteps.onListingPage().filter().select(MARK).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор вендора")
    public void shouldSelectVendor() {
        basePageSteps.onListingPage().filter().selectItem("Марка", "Иномарки");
        basePageSteps.onListingPage().filter().select("Иномарки").waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path("/vendor-foreign/").path(ALL)
                .shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().select("Иномарки").waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор вендора через «+»")
    public void shouldSelectVendorViaPlus() {
        basePageSteps.onListingPage().filter().select("Марка").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onListingPage().filter().selectPopup().plusButton("Иномарки").click();
        basePageSteps.onListingPage().filter().selectPopup().item("Европейские").click();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path("/vendor-european/").path(ALL)
                .shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().select("Европейские").waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели")
    public void shouldSelectModel() {
        basePageSteps.onListingPage().filter().selectItem("Марка", MARK);
        basePageSteps.onListingPage().filter().selectItem("Модель", MODEL);
        basePageSteps.onListingPage().filter().select(MARK).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(MODEL).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase()).path(ALL)
                .shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().select(MARK).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(MODEL).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор шильда")
    public void shouldSelectNameplate() {
        basePageSteps.onListingPage().filter().selectItem("Марка", MARK);
        basePageSteps.onListingPage().filter().select("Модель").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onListingPage().filter().selectPopup().plusButton(MODEL).click();
        basePageSteps.onListingPage().filter().selectPopup().item(NAMEPLATE).click();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK.toLowerCase()).path(NAMEPLATE_IN_URL).path(ALL)
                .shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().select(MARK).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(NAMEPLATE).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор поколения")
    public void shouldSelectGeneration() {
        basePageSteps.onListingPage().filter().selectItem("Марка", MARK);
        basePageSteps.onListingPage().filter().select("Модель").waitUntil(isEnabled());
        basePageSteps.onListingPage().filter().selectItem("Модель", MODEL);
        basePageSteps.onListingPage().filter().select("Поколение").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onListingPage().filter().generationsPopup().generationItem(GENERATION).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().body().sendKeys(Keys.ESCAPE);
        basePageSteps.onListingPage().filter().select(GENERATION).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(GENERATION_CODE).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().select(MARK).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(MODEL).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(GENERATION).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Исключение марки")
    public void shouldExcludeMark() {
        basePageSteps.onListingPage().filter().select("Марка").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onListingPage().filter().selectPopup().radioButton("Исключить").click();
        basePageSteps.onListingPage().filter().selectPopup().item(MARK).click();
        basePageSteps.onListingPage().filter().select(format("Кроме %s", MARK)).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("exclude_catalog_filter", format("mark=%s", MARK.toUpperCase()))
                .shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        waitSomething(3, TimeUnit.SECONDS);
        basePageSteps.onListingPage().salesList().forEach(sale -> sale.nameLink().should(not(hasText(containsString(MARK)))));
        basePageSteps.onListingPage().filter().select(format("Кроме %s", MARK)).waitUntil(isDisplayed());
        basePageSteps.refresh();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        waitSomething(3, TimeUnit.SECONDS);
        basePageSteps.onListingPage().salesList().forEach(sale -> sale.nameLink().should(not(hasText(containsString(MARK)))));
        basePageSteps.onListingPage().filter().select(format("Кроме %s", MARK)).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Исключение вендора")
    public void shouldExcludeVendor() {
        basePageSteps.onListingPage().filter().select("Марка").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onListingPage().filter().selectPopup().radioButton("Исключить").click();
        basePageSteps.onListingPage().filter().selectPopup().item("Иномарки").click();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("exclude_catalog_filter", "vendor=VENDOR2").shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().select("Кроме иномарок").waitUntil(isDisplayed());
        basePageSteps.refresh();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().select("Кроме иномарок").waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Исключение вендора через «+»")
    public void shouldExcludeVendorViaPlus() {
        basePageSteps.onListingPage().filter().select("Марка").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onListingPage().filter().selectPopup().radioButton("Исключить").click();
        basePageSteps.onListingPage().filter().selectPopup().plusButton("Иномарки").click();
        basePageSteps.onListingPage().filter().selectPopup().item("Европейские").click();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("exclude_catalog_filter", "vendor=VENDOR3").shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().select("Кроме европейских").waitUntil(isDisplayed());
    }
}
