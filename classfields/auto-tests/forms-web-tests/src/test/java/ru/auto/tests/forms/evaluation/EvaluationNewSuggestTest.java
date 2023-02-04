package ru.auto.tests.forms.evaluation;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;

import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EVALUATION;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Оценка авто - блок «Новые автомобили с учётом вашего в трейд-ин»")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class EvaluationNewSuggestTest {

    private static final String EVALUATION_ID = "/CAGAAde0l_AFiAHX0Mz3BZAB6qPIxIK88qIomAGomf2S9anvsYAB/";

    private static final int VISIBLE_ITEMS_CNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "forms/ReferenceCatalogCarsSuggest",
                "forms/TradeInIsAvailable",
                "forms/SearchPriceFrom360000",
                "forms/SearchPriceFrom460000",
                "forms/SearchCars",
                "forms/SearchCarsHistogram",
                "forms/StatsPredict",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        formsSteps.createEvaluationCarsForm();
        urlSteps.testing().path(CARS).path(EVALUATION).open();
        formsSteps.fillForm(formsSteps.getBuyDateYear().getBlock());
        formsSteps.submitForm();
        urlSteps.hideDevtoolsBranch();
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class})
    @DisplayName("Отображение блока")
    public void shouldSeeSalesFromDealers() {
        formsSteps.onFormsEvaluationPage().newForTradeIn().title()
                .should(hasText("Новые автомобили с учётом вашего в трейд-ин"));
        formsSteps.onFormsEvaluationPage().newForTradeIn().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> {
                    item.should(isDisplayed());
                    item.image().should(isDisplayed());
                });
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class})
    @DisplayName("Листание объявлений")
    public void shouldSlideSales() {
        formsSteps.focusElementByScrollingOffset(formsSteps.onFormsEvaluationPage().newForTradeIn(), 0, 1000);
        formsSteps.onFormsEvaluationPage().newForTradeIn().prevButton().should(not(isDisplayed()));
        formsSteps.onFormsEvaluationPage().newForTradeIn().nextButton().click();
        formsSteps.onFormsEvaluationPage().newForTradeIn().prevButton().waitUntil(isDisplayed()).click();
        formsSteps.onFormsEvaluationPage().newForTradeIn().nextButton().waitUntil(isDisplayed());
        formsSteps.onFormsEvaluationPage().newForTradeIn().prevButton().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        formsSteps.onFormsEvaluationPage().newForTradeIn().getItem(0).should(isDisplayed()).click();
        formsSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path("/geely/atlas/21187529/21210855/1093434056-c7979daf/")
                .shouldNotSeeDiff();
    }
}
