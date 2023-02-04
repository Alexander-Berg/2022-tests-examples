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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник, мотоциклы, перекуп - чекбокс НДС")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MotorcyclesVatCheckboxResellerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthReseller",
                "forms/UserDraftMotoPutReseller",
                "forms/UserDraftMotoReseller",
                "poffer/ReferenceCatalogCarsParseOptionsVat").post();

        urlSteps.testing().path(MOTO).path(ADD).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class})
    @DisplayName("Появление и скрытие чекбокса НДС")
    public void shouldShowAndHideVatCheckbox() {
        basePageSteps.onFormsPage().unfoldedBlock(formsSteps.getDescription().getBlock())
                .input(formsSteps.getDescription().getName(), "НДС");

        formsSteps.unfoldBlock(formsSteps.getPrice().getBlock());
        basePageSteps.onFormsPage().unfoldedBlock(formsSteps.getPrice().getBlock()).checkbox("Учитывать НДС")
                .should(isDisplayed())
                .should(hasAttribute("class", containsString("Checkbox_checked")));

        mockRule.overwriteStub(3, "poffer/ReferenceCatalogCarsParseOptionsEmpty");

        basePageSteps.onFormsPage().unfoldedBlock(formsSteps.getDescription().getBlock())
                .clearInput(formsSteps.getDescription().getName());
        basePageSteps.onFormsPage().unfoldedBlock(formsSteps.getDescription().getBlock())
                .input(formsSteps.getDescription().getName(), "Test");

        basePageSteps.onFormsPage().unfoldedBlock(formsSteps.getPrice().getBlock()).checkbox("Учитывать НДС")
                .should(not(isDisplayed()));
    }
}