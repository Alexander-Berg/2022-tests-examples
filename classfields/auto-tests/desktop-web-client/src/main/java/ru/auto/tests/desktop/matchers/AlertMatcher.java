package ru.auto.tests.desktop.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;

public class AlertMatcher extends TypeSafeMatcher<WebDriver> {

    private final String expectedAlertText;

    private AlertMatcher(String expectedAlertText) {
        this.expectedAlertText = expectedAlertText;
    }

    @Override
    protected boolean matchesSafely(WebDriver webDriver) {
        try {
            String actualAlertText = webDriver.switchTo().alert().getText();
            return actualAlertText.equals(expectedAlertText);
        } catch (NoAlertPresentException ex) {
            return false;
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Алерт должен иметь текст ").appendValue(expectedAlertText);
    }

    @Override
    protected void describeMismatchSafely(WebDriver webDriver, Description mismatchDescription) {
        try {
            String actualAlertText = webDriver.switchTo().alert().getText();
            mismatchDescription.appendText("Алерт имеет текст ").appendValue(actualAlertText);
        } catch (NoAlertPresentException ex) {
            mismatchDescription.appendText("Алерта нету :(");
        }
    }

    @Factory
    public static AlertMatcher hasAlertWithText(String expectedAlertText) {
        return new AlertMatcher(expectedAlertText);
    }
}
