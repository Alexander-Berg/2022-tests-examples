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
import static ru.auto.tests.desktop.consts.Owners.NIKOVCHARENKO;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EVALUATION;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Оценка авто - перерасчёт цены")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class EvaluationPriceChangeTest {

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
        mockRule.newMock().with("forms/SearchTechParamId21796129",
                "forms/SearchRid1107",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        formsSteps.createEvaluationCarsForm();
        urlSteps.testing().path(CARS).path(EVALUATION).open();
        formsSteps.fillForm(formsSteps.getEvaluateReason().getBlock());
        formsSteps.submitForm();
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Перерасчёт цены")
    public void shouldRenewPrice() {
        String firstTradeInPrice =
                formsSteps.onFormsEvaluationPage().evaluationResult().snippet("Дилеру в трейд-ин")
                        .evaluationPrice().getText();
        String firstSalePrice =
                formsSteps.onFormsEvaluationPage().evaluationResult().snippet("Продажа на Авто.ру")
                        .evaluationPrice().getText();
        String firstSaleTitle = formsSteps.onFormsEvaluationPage().evaluationResult().getSale(0).getText();
        formsSteps.onFormsEvaluationPage().foldedBlock("Город продажи").click();
        formsSteps.onFormsEvaluationPage().unfoldedBlock("Город продажи")
                .input("Город продажи", "Анапа");
        formsSteps.onFormsEvaluationPage().unfoldedBlock("Город продажи").geoSuggest().getItem(0).click();
        formsSteps.submitForm();
        formsSteps.onFormsEvaluationPage().evaluationResult().snippet("Дилеру в " +
                "трейд-ин").evaluationPrice().waitUntil(not(hasText(firstTradeInPrice)));
        formsSteps.onFormsEvaluationPage().evaluationResult().snippet("Продажа на " +
                "Авто.ру").evaluationPrice().waitUntil(not(hasText(firstSalePrice)));
        formsSteps.onFormsEvaluationPage().evaluationResult().getSale(0).waitUntil(not(hasText(firstSaleTitle)));
    }
}
