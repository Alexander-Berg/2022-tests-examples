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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;

import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Отзывы - по-ап плюсов/минусов")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PlusMinusTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        urlSteps.testing().path(CARS).path(REVIEWS).path(ADD).open();
        formsSteps.createReviewsCarsForm();
        formsSteps.fillForm(formsSteps.getModification().getBlock());
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор плюсов и минусов из поп-апа")
    public void shouldSelectPlusesAndMinuses() {
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewPluses().getBlock()).getInput(0).click();
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewPluses().getBlock()).plusMinusPopup().waitUntil(isDisplayed());
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewPluses().getBlock()).plusMinusPopup()
                .plusMinus("Безопасность").waitUntil(isDisplayed()).click();
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewPluses().getBlock()).getInput(0)
                .waitUntil(hasValue("Безопасность"));
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewPluses().getBlock()).plusMinusPopup()
                .waitUntil(not(isDisplayed()));

        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewMinuses().getBlock()).getInput(1).click();
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewMinuses().getBlock()).plusMinusPopup().waitUntil(isDisplayed());
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewMinuses().getBlock()).plusMinusPopup()
                .plusMinus("Безопасность").should(not(isDisplayed()));
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewMinuses().getBlock()).plusMinusPopup()
                .plusMinus("Динамика").click();
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewMinuses().getBlock()).getInput(1)
                .waitUntil(hasValue("Динамика"));
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getReviewPluses().getBlock()).plusMinusPopup()
                .waitUntil(not(isDisplayed()));
    }
}