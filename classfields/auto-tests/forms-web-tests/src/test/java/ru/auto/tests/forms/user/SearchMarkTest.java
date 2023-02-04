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

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник, мотоциклы - поиск марки")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SearchMarkTest {

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
        urlSteps.testing().path(MOTO).path(ADD).open();

        formsSteps.createMotorcyclesForm();
        formsSteps.fillForm(formsSteps.getCategory().getBlock());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Поиск марки")
    public void shouldSearchMark() {
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getMarkSearch().getBlock())
                .input(formsSteps.markSearch.name, "honda");
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getMarkSearch().getBlock())
                .searchResultsList().should(hasSize(1));
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getMarkSearch().getBlock()).getSearchResult(0)
                .should(hasText("Honda")).click();
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getModel().getBlock()).waitUntil(isDisplayed());
    }
}