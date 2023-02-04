package ru.auto.tests.desktop.step;

import org.openqa.selenium.JavascriptExecutor;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.commons.webdriver.WebDriverSteps;

public class AdsSteps extends WebDriverSteps {

    public Long getLinksCountOfAnYandexAdvert(VertisElement element) {
        return (Long) ((JavascriptExecutor) getDriver()).executeScript(
                "return arguments[0].shadowRoot.querySelectorAll('a').length",
                element
        );
    }

    public void clickFirstLinkInAnYandexAdvert(VertisElement element) {
        ((JavascriptExecutor) getDriver()).executeScript(
                "return arguments[0].shadowRoot.querySelector('a').click()",
                element
        );
    }

}
