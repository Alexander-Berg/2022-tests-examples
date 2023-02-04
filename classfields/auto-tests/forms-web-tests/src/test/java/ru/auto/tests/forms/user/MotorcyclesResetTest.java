package ru.auto.tests.forms.user;

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
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник, мотоциклы - сброс полей при смене категории")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MotorcyclesResetTest {

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
        formsSteps.createMotorcyclesForm();
        formsSteps.setReg(false);

        urlSteps.testing().path(MOTO).path(ADD).open();
        formsSteps.fillForm(formsSteps.getModel().getBlock());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс полей при смене категории")
    public void shouldResetFormFields() {
        formsSteps.onFormsPage().foldedBlock(formsSteps.getCategory().getBlock()).click();
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getCategory().getBlock()).radioButton("Скутеры").click();
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getMark().getBlock()).waitUntil(isDisplayed());
        formsSteps.onFormsPage().foldedBlock(formsSteps.getModel().getBlock()).waitUntil(not(isDisplayed()));
    }
}