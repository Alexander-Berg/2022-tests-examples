package ru.auto.tests.desktop.mobile.step;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;


public class LkCreditsFormSteps extends BasePageSteps {

    @Step("Вводим текст в инпут саджеста")
    public void enterTextInSuggestInput(String input, String text) {
        onLkCreditsPage().creditsForm().suggestInput(input).clear();
        onLkCreditsPage().creditsForm().suggestInput(input).sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END, Keys.DELETE));
        onLkCreditsPage().creditsForm().suggestInput(input).sendKeys(text);
    }
}
