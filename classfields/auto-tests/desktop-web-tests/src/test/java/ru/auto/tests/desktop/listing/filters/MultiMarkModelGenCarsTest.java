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

import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Легковые - мультивыбор марок/моделей")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MultiMarkModelGenCarsTest {

    private static final String MARK1 = "Audi";
    private static final String MODEL1 = "A3";
    private static final String GENERATION1 = "III (8V) Рестайлинг";
    private static final String GENERATION1_CODE = "20785010";
    private static final String MARK2 = "BMW";
    private static final String MODEL2 = "X3";
    private static final String GENERATION2 = "III (G01)";
    private static final String GENERATION2_CODE = "21029610";

    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(RUSSIA).path(CARS).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор марка + марка")
    public void shouldSeeMarksInUrl() {
        basePageSteps.onListingPage().filter().selectItem("Марка", MARK1);
        basePageSteps.onListingPage().filter().addIcon().click();
        basePageSteps.onListingPage().filter().selectPopup().item(MARK2).click();

        urlSteps.addParam("catalog_filter", format("mark=%s", MARK1.toUpperCase()))
                .addParam("catalog_filter", format("mark=%s", MARK2.toUpperCase())).shouldNotSeeDiff();

        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().select(MARK1).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(MARK2).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор модель + модель")
    public void shouldSeeModelsInUrl() {
        basePageSteps.onListingPage().filter().selectItem("Марка", MARK1);
        basePageSteps.onListingPage().filter().selectItem("Модель", MODEL1);
        basePageSteps.onListingPage().filter().addIcon().click();
        basePageSteps.onListingPage().filter().selectPopup().item(MARK2).click();
        basePageSteps.onListingPage().filter().select("Модель").waitUntil(isEnabled());
        basePageSteps.onListingPage().filter().selectItem("Модель", MODEL2);

        String expectedParam1 = format("mark=%s,model=%s", MARK1.toUpperCase(), MODEL1.toUpperCase());
        String expectedParam2 = format("mark=%s,model=%s", MARK2.toUpperCase(), MODEL2.toUpperCase());

        urlSteps.addParam("catalog_filter", expectedParam1)
                .addParam("catalog_filter", expectedParam2).shouldNotSeeDiff();

        basePageSteps.onListingPage().filter().submitButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().select(MARK1).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(MARK2).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(MODEL1).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(MODEL2).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор поколение + поколение")
    public void shouldSeeGenerationsInUrl() {
        basePageSteps.onListingPage().filter().selectItem("Марка", MARK1);
        basePageSteps.onListingPage().filter().selectItem("Модель", MODEL1);
        basePageSteps.onListingPage().filter().select("Поколение").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onListingPage().filter().generationsPopup().generationItem(GENERATION1).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().filter().addIcon().click();
        basePageSteps.onListingPage().filter().selectPopup().item(MARK2).click();
        basePageSteps.onListingPage().filter().selectItem("Модель", MODEL2);
        basePageSteps.onListingPage().filter().select("Поколение").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onListingPage().filter().generationsPopup().generationItem(GENERATION2).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().body().sendKeys(Keys.ESCAPE);

        String expectedParam1 = format("mark=%s,model=%s,generation=%s", MARK1.toUpperCase(), MODEL1.toUpperCase(),
                GENERATION1_CODE);
        String expectedParam2 = format("mark=%s,model=%s,generation=%s", MARK2.toUpperCase(), MODEL2.toUpperCase(),
                GENERATION2_CODE);

        urlSteps.addParam("catalog_filter", expectedParam1)
                .addParam("catalog_filter", expectedParam2).shouldNotSeeDiff();

        basePageSteps.onListingPage().filter().submitButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().select(MARK1).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(MARK2).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(MODEL1).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(MODEL2).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(GENERATION1).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select(GENERATION2).waitUntil(isDisplayed());
    }
}