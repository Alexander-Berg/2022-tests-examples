package ru.yandex.realty.step;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.hamcrest.Matcher;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.webdriver.WebDriverSteps;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.lambdas.WatchException.watchException;

public class CommonSteps extends WebDriverSteps {

    public static final int EGRN_TIMEOUT = 120;
    public static int FIRST = 0;

    @Step("Отключаем рекламу на старнице")
    public void disableAd() {
        setCookie("isAdDisabled", "1", ".yandex.ru");
    }

    @Step("Обновляем пока {object} не будет {matcher}")
    public <T> void refreshUntil(Supplier<T> object, Matcher<T> matcher) {
        refreshUntil(object, matcher, 30);
    }

    @Step("Обновляем {timeout} секунд пока нужный объект не будет {matcher}")
    public <T> void refreshUntil(Supplier<T> object, Matcher<T> matcher, int timeout) {
        T obj = object.get();
        if (!matcher.matches(obj)) {
            watchException(() ->
                    given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                            .alias(obj.toString())
                            .ignoreExceptions()
                            .pollInterval(1000, MILLISECONDS).atMost(timeout, SECONDS).pollInSameThread()
                            .until(() -> {
                                refresh();
                                T actual = object.get();
                                return matcher.matches(actual);
                            }));
        }
    }

    @Step("Скролл к элементу {element}")
    public void scrollToElement(AtlasWebElement element) {
        element.waitUntil(isDisplayed());
        scroll(element.getLocation().getY());
    }

    @Step("Скролл к элементу {element} в центр экрана")
    public void scrollElementToCenter(AtlasWebElement element) {
        element.waitUntil(isDisplayed());
        int i = element.getLocation().getY();
        int cur = getCurrentYPosition();
        int elementHeight = element.getSize().getHeight();
        int screen = getDriver().manage().window().getSize().getHeight();
        scroll(i - cur - screen / 2 + elementHeight/2);
    }

    @Step("Прокручиваем окно до {totalPixels} px с шагом в {stepPixels} px")
    public void scrolling(int totalPixels, int stepPixels) {
        int i = stepPixels;

        do {
            this.scroll(stepPixels);
            i += stepPixels;
            waitSomething(100, MILLISECONDS);
        } while (Math.abs(i) < Math.abs(totalPixels));
    }

    public void scrollUntilExists(Supplier<? extends AtlasWebElement> object) {
        scrollingUntil(object, exists());
        scrollElementToCenter(object.get());

    }

    public void scrollUntilExistsTouch(Supplier<? extends AtlasWebElement> object) {
        scrollingUntil(object, exists());
    }

    public void scrollingUntil(Supplier object, Matcher matcher) {
        scrollingUntil(object, matcher, 20);
    }

    @Step("Прокручиваем пока {object} не будет {matcher}")
    public void scrollingUntil(Supplier object, Matcher matcher, int timeout) {
        await().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions()
                .alias("Скроллили, скроллили но так и не появился элемент")
                .pollInSameThread().atMost(timeout, SECONDS)
                .until(() -> {
                    scroll(600);
                    object.get();
                    return matcher.matches(object.get());
                });
    }

    @Step("Двигаем элемент {element} по горизонтали на {xOffset} по вертикали на {yOffset}")
    public void moveSlider(WebElement element, int xOffset, int yOffset) {
        new Actions(getDriver()).dragAndDropBy(element, xOffset, yOffset).build().perform();
    }

    @Step("Прокручиваем {slider} пока {object} не будет {matcher}")
    public void slidingUntil(Supplier<WebElement> slider, Supplier<WebElement> object, Matcher matcher, int timeout) {
        int screen = getDriver().manage().window().getSize().getWidth();
        await().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions()
                .alias("Слайдили, слайдили но так и не появился элемент")
                .pollInSameThread().atMost(timeout, SECONDS)
                .until(() -> {
                    moveSlider(slider.get(),screen / 3,0);
                    object.get();
                    return matcher.matches(object.get());
                });
    }

    @Step("Ресайз «{x} x {y}»")
    public void resize(int x, int y) {
        getDriver().manage().window().setSize(new Dimension(x, y));
    }


    @Step("Ждем пока количество табов не будет {count}")
    public void waitUntilSeeTabsCount(int count) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(1000, MILLISECONDS).atMost(20000, MILLISECONDS).ignoreExceptions().pollInSameThread()
                .until(() -> equalTo(count).matches(getDriver().getWindowHandles().size()));
    }

    @Step("Ждем алерт")
    public void acceptAlert() {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(500, MILLISECONDS).atMost(10000, MILLISECONDS).ignoreExceptions().pollInSameThread()
                .until(() -> notNullValue().matches(getDriver().switchTo().alert()));
        getDriver().switchTo().alert().accept();
    }

    @Step("Кликаем на {element} пока {targetElement} не будет {matcher}")
    public void clickUntil(AtlasWebElement element, AtlasWebElement targetElement,
                           Matcher<? extends WebElement> matcher) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .alias(format("Кликали на %s, но %s не стал %s", element, targetElement, matcher))
                .pollInterval(2000, MILLISECONDS).atMost(10000, MILLISECONDS).ignoreExceptions().pollInSameThread()
                .until(() -> {
                    element.click();
                    return matcher.matches(targetElement);
                });
    }

    public void clearInputByBackSpace(Supplier<? extends AtlasWebElement> element) {
        clearInputByBackSpace(element, equalTo(""));
    }

    public void clearInputByBackSpace(Supplier<? extends AtlasWebElement> element, Matcher matcher) {
        await().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions().pollInSameThread()
                .pollInterval(500, MILLISECONDS).atMost(15, TimeUnit.SECONDS)
                .until(() -> {
                    element.get().sendKeys(Keys.BACK_SPACE);
                    return matcher.matches(element.get().getAttribute("value"));
                });
        element.get().sendKeys(Keys.BACK_SPACE);
    }

    public List<JsonElement> performanceLog() {
        String scriptToExecute = "var performance = window.performance || window.mozPerformance || window.msPerformance " +
                "|| window.webkitPerformance || {}; var network = performance.getEntries() || {}; " +
                "return JSON.stringify(network, ['name']);";
        String netData = ((JavascriptExecutor) getDriver()).executeScript(scriptToExecute).toString();
        return StreamSupport.stream(new Gson().fromJson(netData, JsonArray.class).spliterator(), false)
                .collect(toList());
    }

    public List<String> performanceUrls() {
        return performanceLog().stream()
                .map(log -> log.getAsJsonObject().get("name").getAsString())
                .collect(toList());
    }

    @Step("Должны видеть запрос в логах браузера «{matcher}»")
    public void shouldSeeRequestInBrowser(Matcher matcher) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await().ignoreExceptions()
                .pollInterval(5, MILLISECONDS).atMost(10, SECONDS)
                .then()
                .until(() -> performanceUrls().stream()
                        .anyMatch(url -> containsString("gate/react-page/get").matches(url)));

        List<String> reactPageGet = performanceUrls().stream()
                .filter(url -> containsString("gate/react-page/get").matches(url)).collect(toList());
        assertThat("Должны видеть запрос в логе запросов браузера", reactPageGet, matcher);
    }
}
