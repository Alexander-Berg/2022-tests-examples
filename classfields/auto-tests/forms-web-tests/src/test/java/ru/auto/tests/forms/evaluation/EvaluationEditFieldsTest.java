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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Оценка авто - редактирование полей")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class EvaluationEditFieldsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        formsSteps.createEvaluationCarsForm();
        urlSteps.testing().path(CARS).path(EVALUATION).open();
        formsSteps.fillForm(formsSteps.getEvaluateReason().getBlock());
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Сброс всех полей при редактировании марки")
    public void shouldEditMark() {
        formsSteps.onFormsEvaluationPage().foldedBlock("Марка").click();
        formsSteps.onFormsEvaluationPage().unfoldedBlock(formsSteps.getMarkSearch().getBlock())
                .input(formsSteps.markSearch.name, "Ford");
        formsSteps.onFormsEvaluationPage().unfoldedBlock(formsSteps.getMarkSearch().getBlock()).getSearchResult(0)
                .should(hasText("Ford")).click();
        formsSteps.onFormsEvaluationPage().foldedBlock("Марка").should(isDisplayed());
        formsSteps.onFormsEvaluationPage().unfoldedBlock(formsSteps.getModelSearch().getBlock())
                .input(formsSteps.modelSearch.name).should(hasText(""));
        formsSteps.onFormsEvaluationPage().foldedBlock(formsSteps.getYear().getBlock()).should(not(isDisplayed()));
        formsSteps.onFormsEvaluationPage().unfoldedBlock(formsSteps.getYear().getBlock()).should(not(isDisplayed()));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Редактирование Модификации")
    public void shouldEditModification() {
        formsSteps.onFormsEvaluationPage().foldedBlock(formsSteps.getModification().getBlock()).click();
        formsSteps.onFormsEvaluationPage().radioButton("1.6 AT (123\u00a0л.с.) 2020 - н.в.").click();
        formsSteps.submitForm();
        formsSteps.onFormsEvaluationPage().foldedBlock(formsSteps.getModification().getBlock())
                .should(hasText("Модификация\n1.6 AT (123 л.с.) 2020 - н.в."));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Редактирование Опций")
    public void shouldEditOptions() {
        formsSteps.onFormsEvaluationPage().foldedBlock(formsSteps.getMultimedia().getBlock()).click();
        formsSteps.onFormsEvaluationPage().checkbox("USB").click();
        formsSteps.submitForm();
        formsSteps.onFormsEvaluationPage().checkbox("USB").click();
        formsSteps.onFormsEvaluationPage().checkbox("AUX").click();
        formsSteps.onFormsEvaluationPage().unfoldedBlock(formsSteps.getMultimedia().getBlock()).title().click();
        formsSteps.submitForm();
        formsSteps.onFormsEvaluationPage().foldedBlock(formsSteps.getMultimedia().getBlock())
                .should(hasText("Мультимедиа\nAUX"));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Редактирование Цвета")
    public void shouldEditColor() {
        formsSteps.onFormsEvaluationPage().foldedBlock("Цвет").click();
        formsSteps.onFormsEvaluationPage().unfoldedBlock(formsSteps.getColor().getBlock()).color("007F00").click();
        formsSteps.submitForm();
        formsSteps.onFormsEvaluationPage().foldedBlock("Цвет")
                .should(hasText("Цвет\nЗелёный"));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Редактирование Пробега")
    public void shouldEditMileage() {
        formsSteps.onFormsEvaluationPage().unfoldedBlock(formsSteps.getRun().getBlock()).input("Пробег, км", "777");
        formsSteps.submitForm();
        formsSteps.onFormsEvaluationPage().unfoldedBlock(formsSteps.getRun().getBlock()).input("Пробег, км")
                .should(hasValue("777"));
    }
}
