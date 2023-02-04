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
import org.openqa.selenium.Keys;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;

import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник, мотоциклы - отображение сообщений об ошибках")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MotorcyclesErrorsTest {

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
    @DisplayName("Сообщение об ошибке при попытке сохранить незаполненную форму")
    public void shouldSeeErrorMessage() {
        formsSteps.onFormsPage().userVas().getSnippet(2).submitButton().click();
        formsSteps.onFormsPage().notification().waitUntil(isDisplayed()).should(hasText("Пропущены обязательные поля " +
                "или заполнены неправильно: марка, модель, цена, цвет, город продажи и место осмотра, номер телефона, " +
                "фотографии, электронная почта, как к вам обращаться, год выпуска, пробег, объём двигателя."));
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение сообщений об ошибках в блоке «Год»")
    public void shouldSeeYearErrorMessage() {
        formsSteps.onFormsPage().foldedBlock("Год").click();
        formsSteps.onFormsPage().unfoldedBlock("Год выпуска").input("Год", "1889");
        formsSteps.onFormsPage().unfoldedBlock("Год выпуска").input("Год").sendKeys(Keys.TAB);
        formsSteps.onFormsPage().unfoldedBlock("Год выпуска")
                .waitUntil(hasAttribute("class", containsString("FormSection_error")));
        formsSteps.onFormsPage().unfoldedBlock("Год выпуска").errorMessage()
                .waitUntil(hasText("Год не может быть меньше 1890"));

        formsSteps.onFormsPage().unfoldedBlock("Год выпуска").input("Год", "2025");
        formsSteps.onFormsPage().unfoldedBlock("Год выпуска").input("Год").should(hasValue("2022"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение сообщений об ошибках в блоке «Пробег»")
    public void shouldSeeRunErrorMessage() {
        String block = formsSteps.getRun().getBlock();
        formsSteps.onFormsPage().foldedBlock(block).click();
        formsSteps.onFormsPage().unfoldedBlock(block).input(block, "0");
        formsSteps.onFormsPage().unfoldedBlock(block).input(block).should(hasValue(""));

        formsSteps.onFormsPage().unfoldedBlock(block).input(block, "1000001");
        formsSteps.onFormsPage().unfoldedBlock(block).input(block).should(hasValue("1 000 000"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение сообщений об ошибках в блоке «Мощность»")
    public void shouldSeePowerErrorMessage() {
        String block = formsSteps.getPower().getBlock();
        formsSteps.onFormsPage().foldedBlock(block).click();
        formsSteps.onFormsPage().unfoldedBlock(block).input(block, "0");
        formsSteps.onFormsPage().unfoldedBlock(block).input(block).should(hasValue(""));

        formsSteps.onFormsPage().unfoldedBlock(block).input(block, "1001");
        formsSteps.onFormsPage().unfoldedBlock(block).input(block).should(hasValue("1 000"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение сообщений об ошибках в блоке «Цена»")
    public void shouldSeePriceErrorMessage() {
        String block = formsSteps.getPrice().getBlock();
        formsSteps.onFormsPage().foldedBlock(block).click();
        formsSteps.onFormsPage().unfoldedBlock(block).input(block, "0");
        formsSteps.onFormsPage().unfoldedBlock(block).input(block).should(hasValue(""));

        formsSteps.onFormsPage().unfoldedBlock(block).input(block, "1000000001");
        formsSteps.onFormsPage().unfoldedBlock(block).input(block).should(hasValue("1 000 000 000"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение сообщений об ошибках в блоке «Номер телефона»")
    public void shouldSeePhonesErrorMessage() {
        String block = formsSteps.getPhone().getBlock();
        formsSteps.onFormsPage().foldedBlock(block).click();

        formsSteps.onFormsPage().unfoldedBlock(block).input(block, "zZаА");
        formsSteps.onFormsPage().unfoldedBlock(block).input(block).should(hasValue(""));

        formsSteps.onFormsPage().unfoldedBlock(block).input(block, "891111111115");
        formsSteps.onFormsPage().unfoldedBlock(block).button("Подтвердить").click();

        formsSteps.onFormsPage().unfoldedBlock(block).errorMessage().should(hasText("Неправильный телефон"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение сообщений об ошибках в блоке «Паспорт транспортного средства»")
    public void shouldSeeBuyDateErrorMessage() {
        String block = formsSteps.getBuyDateYear().getBlock();
        String name = formsSteps.getBuyDateYear().getName();

        formsSteps.onFormsPage().foldedBlock("Год").click();
        formsSteps.onFormsPage().unfoldedBlock("Год выпуска").input("Год", "2019");
        formsSteps.onFormsPage().foldedBlock("Паспорт транспортного средства").click();

        formsSteps.onFormsPage().unfoldedBlock(block).input(name, "0");
        formsSteps.onFormsPage().unfoldedBlock(block).input(name).should(hasValue("0"));
        formsSteps.onFormsPage().unfoldedBlock(block).input(name).sendKeys(Keys.TAB);
        formsSteps.onFormsPage().unfoldedBlock(block).errorMessage()
                .waitUntil(hasText("Год покупки младше года производства"));

        formsSteps.onFormsPage().unfoldedBlock(block).input(name, "2018");
        formsSteps.onFormsPage().unfoldedBlock(block).input(name).sendKeys(Keys.TAB);
        formsSteps.onFormsPage().unfoldedBlock(block).input(name).should(hasValue("2019"));
    }
}