package ru.auto.tests.forms.reviews;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.NIKOVCHARENKO;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Отзывы - редактирование полей отзыва")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CarsEditTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        formsSteps.createReviewsCarsForm();
        urlSteps.testing().path(CARS).path(REVIEWS).path(ADD).open();
        formsSteps.fillForm(formsSteps.getReviewText().getBlock());
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Сброс всех полей при редактировании марки")
    public void shouldEditMark() {
        formsSteps.onFormsPage().foldedBlock("Марка").click();
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getMarkSearch().getBlock())
                .input(formsSteps.markSearch.name, "Ford");
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getMarkSearch().getBlock()).getSearchResult(0)
                .should(hasText("Ford")).click();
        formsSteps.onFormsPage().foldedBlock("Марка").should(isDisplayed());
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getModelSearch().getBlock())
                .input(formsSteps.modelSearch.name).should(hasText(""));
        formsSteps.onFormsPage().foldedBlock(formsSteps.getYear().getBlock()).should(not(isDisplayed()));
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getYear().getBlock()).should(not(isDisplayed()));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Редактирование Модификации")
    public void shouldEditModification() {
        formsSteps.onFormsPage().foldedBlock(formsSteps.getModification().getBlock()).click();
        formsSteps.onFormsPage().radioButton("1.8 AT (125\u00a0л.с.) 1999 - 2001").click();
        formsSteps.onFormsPage().saveStatus().waitUntil(hasText("Сохранён"));
        urlSteps.refresh();
        formsSteps.onFormsPage().foldedBlock(formsSteps.getModification().getBlock())
                .should(hasText("Модификация\n1.8 AT (125 л.с.) 1999 - 2001"));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Редактирование Заголовка отзыва")
    public void shouldEditReviewTitle() {
        formsSteps.onFormsPage().input("Заголовок", "Выпьем за любовь");
        formsSteps.onFormsPage().saveStatus().waitUntil(hasText("Сохранён"));
        urlSteps.refresh();
        formsSteps.onFormsPage().input("Заголовок").should(hasText("Выпьем за любовь"));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Редактирование Оценки авто")
    public void shouldEditRating() {
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewRatingExterior().getBlock())
                .rating("Внешний вид").star("5").click();
        formsSteps.onFormsPage().saveStatus().waitUntil(hasText("Сохраняется..."));
        formsSteps.onFormsPage().saveStatus().waitUntil(hasText("Сохранён"));
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewRatingExterior().getBlock())
                .rating("Внешний вид").star("3").click();
        formsSteps.onFormsPage().saveStatus().waitUntil(hasText("Сохраняется..."));
        formsSteps.onFormsPage().saveStatus().waitUntil(hasText("Сохранён"));
        urlSteps.refresh();
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewRatingExterior().getBlock())
                .rating("Внешний вид").filledStarsList().should(hasSize(3));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Редактирование Минусов")
    public void shouldEditMinuses() {
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewMinuses().getBlock()).getInput(0).click();
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewMinuses().getBlock()).plusMinusPopup()
                .plusMinus("Динамика").click();
        formsSteps.onFormsPage().saveStatus().waitUntil(hasText("Сохранён"));
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewMinuses().getBlock()).getInput(0)
                .sendKeys("Разложения");
        formsSteps.onFormsPage().saveStatus().waitUntil(hasText("Сохраняется..."));
        formsSteps.onFormsPage().saveStatus().waitUntil(hasText("Сохранён"));
        urlSteps.refresh();
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewMinuses().getBlock()).getInput(0)
                .waitUntil(hasValue("ДинамикаРазложения"));
    }
}