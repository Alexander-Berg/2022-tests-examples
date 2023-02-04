package ru.auto.tests.desktop.mobile.step;

import io.qameta.allure.Step;
import org.openqa.selenium.JavascriptExecutor;

public class CardPageSteps extends BasePageSteps {

    @Step("Нажимаем на хлебную крошку с индексом {breadcrumbIndex}")
    public void clickBreadcrumbItem(int breadcrumbIndex) {
        onCardPage().breadcrumbs().getItem(breadcrumbIndex).hover();

        JavascriptExecutor jse = (JavascriptExecutor) getDriver();
        jse.executeScript("arguments[0].scrollLeft -= 100", onCardPage().breadcrumbsScrollView());

        onCardPage().breadcrumbs().getItem(breadcrumbIndex).click();
    }
}
