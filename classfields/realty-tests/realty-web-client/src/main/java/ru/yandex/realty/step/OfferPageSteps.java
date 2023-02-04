package ru.yandex.realty.step;

import io.qameta.atlas.webdriver.AtlasWebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.yandex.realty.page.AuthorOfferPage;
import ru.yandex.realty.page.OfferCardPage;
import ru.yandex.realty.page.VillageSitePage;

import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class OfferPageSteps extends CommonSteps {

    private static final long Y_OFFSET = 1500L;

    public OfferCardPage onOfferCardPage() {
        return on(OfferCardPage.class);
    }

    public AuthorOfferPage onAuthorOfferPage() {
        return on(AuthorOfferPage.class);
    }

    public VillageSitePage onVillageSitePage() {
        return on(VillageSitePage.class);
    }

    public AtlasWebElement findShortcut(String value) {
        return onOfferCardPage().shortcut(value).isDisplayed() ? onOfferCardPage().shortcut(value) :
                given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                        .pollInterval(2, SECONDS).atMost(20, SECONDS).ignoreExceptions()
                        .until(() -> {
                            moveCursorAndClick(onOfferCardPage().swipeShortcutsForward());
                            waitSomething(1, SECONDS);
                            return onOfferCardPage().shortcut(value);
                        }, AtlasWebElement::isDisplayed);
    }

    public AtlasWebElement findOldShortcut(String value) {
        return onOfferCardPage().shortcut(value).isDisplayed() ? onOfferCardPage().shortcut(value) :
                given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                        .pollInterval(2, SECONDS).atMost(20, SECONDS).ignoreExceptions()
                        .until(() -> {
                            moveCursorAndClick(onOfferCardPage().swipeShortcutsForwardOld());
                            waitSomething(1, SECONDS);
                            return onOfferCardPage().shortcut(value);
                        }, AtlasWebElement::isDisplayed);
    }

    public void clickOnElementShouldScroll(Supplier<AtlasWebElement> element) {
        long initOffset = (long) ((JavascriptExecutor) getDriver()).executeScript("return pageYOffset;");
        element.get().click();
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).atMost(5, SECONDS).ignoreExceptions()
                .alias(format("Ждем прокрутки больше чем %s", Y_OFFSET)).untilAsserted(() -> {
                    long finalOffset = (long) ((JavascriptExecutor) getDriver()).executeScript("return pageYOffset;");
                    assertThat(finalOffset - initOffset, greaterThan(Y_OFFSET));
                });

    }

    @Override
    public WebDriver getDriver() {
        return super.getDriver();
    }
}
