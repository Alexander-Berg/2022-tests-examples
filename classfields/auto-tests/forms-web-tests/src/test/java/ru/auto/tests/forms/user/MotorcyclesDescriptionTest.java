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

import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;

@DisplayName("Частник, мотоциклы - блок «Описание»")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MotorcyclesDescriptionTest {

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
        formsSteps.fillForm(formsSteps.getCategory().getBlock());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Добавление тегов в описание")
    public void shouldAddTagsToDescription() {
        String block = formsSteps.getDescription().getBlock();
        formsSteps.onFormsPage().foldedBlock(block).click();
        formsSteps.onFormsPage().unfoldedBlock(block).descriptionTag("Посреднические услуги не предлагать").click();
        formsSteps.onFormsPage().unfoldedBlock(block).descriptionTag("Торг возможен при осмотре").click();
        formsSteps.onFormsPage().unfoldedBlock(block).input("description")
                .waitUntil(hasValue("Посреднические услуги не предлагать. Торг возможен при осмотре."));
    }
}