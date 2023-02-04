package ru.auto.tests.forms.user;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник - чекбокс НДС")
@Feature(FORMS)
@GuiceModules(DesktopTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VatCheckboxTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private AccountManager am;

    @Inject
    private LoginSteps loginSteps;

    @Parameterized.Parameter
    public String type;

    @Parameterized.Parameter(1)
    public String category;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {TRUCKS, "Лёгкие коммерческие"},
                {MOTO, "Мотоциклы"}
        });
    }

    @Before
    public void before() throws IOException {
        Account account = am.create();
        loginSteps.loginAs(account);

        urlSteps.testing().path(type).path(ADD).addXRealIP(MOSCOW_IP).open();

        basePageSteps.onFormsPage().unfoldedBlock(formsSteps.getCategory().getBlock())
                .radioButton(category).click();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Показ и скрытие чекбокса НДС при редактировании описания")
    public void shouldShowAndHideVatCheckbox() {
        basePageSteps.onFormsPage().unfoldedBlock(formsSteps.getDescription().getBlock())
                .input(formsSteps.getDescription().getName(), "НДС");

        formsSteps.unfoldBlock(formsSteps.getPrice().getBlock());
        basePageSteps.onFormsPage().unfoldedBlock(formsSteps.getPrice().getBlock()).checkbox("Учитывать НДС")
                .should(isDisplayed())
                .should(hasAttribute("class", containsString("Checkbox_checked")));

        basePageSteps.onFormsPage().unfoldedBlock(formsSteps.getDescription().getBlock())
                .clearInput(formsSteps.getDescription().getName());
        basePageSteps.onFormsPage().unfoldedBlock(formsSteps.getDescription().getBlock())
                .input(formsSteps.getDescription().getName(), "Test");

        basePageSteps.onFormsPage().unfoldedBlock(formsSteps.getPrice().getBlock()).checkbox("Учитывать НДС")
                .should(not(isDisplayed()));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подсказка про НДС")
    public void shouldSeeVatTooltip() {
        basePageSteps.onFormsPage().unfoldedBlock(formsSteps.getDescription().getBlock())
                .input(formsSteps.getDescription().getName(), "НДС");

        formsSteps.unfoldBlock(formsSteps.getPrice().getBlock());
        basePageSteps.onFormsPage().unfoldedBlock(formsSteps.getPrice().getBlock()).priceVatInfoButton()
                .should(isDisplayed()).hover();

        basePageSteps.onFormsPage().popup().should(isDisplayed())
                .should(hasText("Выберите эту опцию, если будете готовы предоставить " +
                        "покупателю-юрлицу счёт-фактуру для вычета НДС."));
    }
}

