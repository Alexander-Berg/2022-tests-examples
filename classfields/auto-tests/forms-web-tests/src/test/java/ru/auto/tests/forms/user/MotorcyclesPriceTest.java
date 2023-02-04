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

import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;

@DisplayName("Частник, мотоциклы - блок «Цена»")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MotorcyclesPriceTest {

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
        mockRule.newMock().with("forms/Currencies",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        formsSteps.createMotorcyclesForm();
        formsSteps.setReg(false);

        urlSteps.testing().path(MOTO).path(ADD).open();
        formsSteps.fillForm(formsSteps.getCategory().getBlock());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение цены в другой валюте")
    public void shouldChangePriceCurrency() {
        String block = formsSteps.getPrice().getBlock();
        formsSteps.onFormsPage().foldedBlock(block).click();
        formsSteps.onFormsPage().unfoldedBlock(block).input(block, "500000");
        formsSteps.onFormsPage().unfoldedBlock(block).selectItem("\u20BD", "€");
        formsSteps.onFormsPage().unfoldedBlock("Цена, €").input("Цена, €")
                .should(hasValue(startsWith("7 143")));
    }
}