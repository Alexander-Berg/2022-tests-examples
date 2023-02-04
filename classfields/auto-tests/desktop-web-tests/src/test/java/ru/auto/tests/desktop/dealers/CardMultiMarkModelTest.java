package ru.auto.tests.desktop.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.QueryParams.CATALOG_FILTER;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Мультивыбор марок/моделей")
@Feature(DEALERS)
@Story(DEALER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CardMultiMarkModelTest {

    private static final String MARK1 = "Audi";
    private static final String MARK1_CODE = "AUDI";
    private static final String MODEL1 = "A3";
    private static final String MODEL1_CODE = "A3";
    private static final String GENERATION1 = "III (8V) Рестайлинг";
    private static final String GENERATION1_CODE = "20785010";
    private static final String MARK2 = "Mercedes-Benz";
    private static final String MARK2_CODE = "MERCEDES";
    private static final String MODEL2 = "A-Класс";
    private static final String MODEL2_CODE = "A_KLASSE";
    private static final String GENERATION2 = "IV (W177)";
    private static final String GENERATION2_CODE = "21198161";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionUnauth"),
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/SearchCarsBreadcrumbsAudi"),
                stub("desktop/SearchCarsBreadcrumbsMercedes"),
                stub("desktop/Salon"),
                stub("desktop/SearchCarsCountDealerId"),
                stub("desktop/SearchCarsMarkModelFiltersAllDealerIdSeveralMarks"),
                stub("desktop/SearchCarsAllDealerId")
        ).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path(CARS_OFFICIAL_DEALER).path("/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Мультивыбор марка + марка")
    public void shouldSeeMarksInUrl() {
        basePageSteps.onDealerCardPage().filter().selectItem("Марка", MARK1);
        basePageSteps.onDealerCardPage().filter().addIcon().click();
        basePageSteps.onDealerCardPage().filter().selectPopup().item(MARK2).click();

        urlSteps.addParam(CATALOG_FILTER, format("mark=%s", MARK1_CODE))
                .addParam(CATALOG_FILTER, format("mark=%s", MARK2_CODE)).shouldNotSeeDiff();

        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();

        basePageSteps.onDealerCardPage().salesList().should(hasSize(greaterThan(0)));
        basePageSteps.onDealerCardPage().filter().select(MARK1).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().select(MARK2).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Мультивыбор модель + модель")
    public void shouldSeeModelsInUrl() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsAudi_A3_Mercedes")).update();

        basePageSteps.onDealerCardPage().filter().selectItem("Марка", MARK1);
        basePageSteps.onDealerCardPage().filter().selectItem("Модель", MODEL1);
        basePageSteps.onDealerCardPage().filter().addIcon().click();
        basePageSteps.onDealerCardPage().filter().selectPopup().item(MARK2).click();
        basePageSteps.onDealerCardPage().filter().select("Модель").waitUntil(isEnabled());
        basePageSteps.onDealerCardPage().filter().selectItem("Модель", MODEL2);

        String expectedParam1 = format("mark=%s,model=%s", MARK1_CODE, MODEL1_CODE);
        String expectedParam2 = format("mark=%s,model=%s", MARK2_CODE, MODEL2_CODE);

        urlSteps.addParam(CATALOG_FILTER, expectedParam1)
                .addParam(CATALOG_FILTER, expectedParam2).shouldNotSeeDiff();

        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();

        basePageSteps.onDealerCardPage().salesList().should(hasSize(greaterThan(0)));
        basePageSteps.onDealerCardPage().filter().select(MARK1).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().select(MARK2).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().select(MODEL1).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().select(MODEL2).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Мультивыбор поколение + поколение")
    public void shouldSeeGenerationsInUrl() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsAudi_A3"),
                stub("desktop/SearchCarsBreadcrumbsAudi_A3_20785010_Mercedes"),
                stub("desktop/SearchCarsBreadcrumbsAudi_A3_20785010_Mercedes_Aklasse")).update();

        basePageSteps.onDealerCardPage().filter().selectItem("Марка", MARK1);
        basePageSteps.onDealerCardPage().filter().selectItem("Модель", MODEL1);
        basePageSteps.onDealerCardPage().filter().select("Поколение").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onDealerCardPage().filter().generationsPopup().generationItem(GENERATION1)
                .waitUntil(isDisplayed()).click();
        basePageSteps.onDealerCardPage().filter().addIcon().click();
        basePageSteps.onDealerCardPage().filter().selectPopup().item(MARK2).click();
        basePageSteps.onDealerCardPage().filter().selectItem("Модель", MODEL2);
        basePageSteps.onDealerCardPage().filter().select("Поколение").selectButton().waitUntil(isEnabled()).click();
        basePageSteps.onDealerCardPage().filter().generationsPopup().generationItem(GENERATION2)
                .waitUntil(isDisplayed()).click();
        basePageSteps.onDealerCardPage().body().sendKeys(Keys.ESCAPE);

        String expectedParam1 = format("mark=%s,model=%s,generation=%s", MARK1_CODE, MODEL1_CODE, GENERATION1_CODE);
        String expectedParam2 = format("mark=%s,model=%s,generation=%s", MARK2_CODE, MODEL2_CODE, GENERATION2_CODE);

        urlSteps.addParam(CATALOG_FILTER, expectedParam1)
                .addParam(CATALOG_FILTER, expectedParam2).shouldNotSeeDiff();

        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();

        basePageSteps.onDealerCardPage().salesList().should(hasSize(greaterThan(0)));
        basePageSteps.onDealerCardPage().filter().select(MARK1).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().select(MARK2).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().select(MODEL1).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().select(MODEL2).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().select(GENERATION1).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().filter().select(GENERATION2).waitUntil(isDisplayed());
    }
}