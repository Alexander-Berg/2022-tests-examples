package ru.auto.tests.commons.webdriver;

import io.qameta.allure.Step;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.WebPage;
import lombok.Getter;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.auto.tests.commons.extension.VertisWebDriverConfiguration;
import ru.auto.tests.commons.extension.context.LocatorStorage;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.commons.extension.listener.AllureListener;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class WebDriverSteps {

    @Getter
    @Inject
    private LocatorStorage locatorStorage;

    @Inject
    private WebDriverManager driverManager;

    protected WebDriver getDriver() {
        return driverManager.getDriver();
    }

    public <T extends WebPage> T on(Class<T> pageClass) {
        Atlas atlas = new Atlas(new VertisWebDriverConfiguration(getDriver()));
        atlas.listener(new AllureListener().setLocatorsList(locatorStorage.getStepsList()));

        return atlas.create(getDriver(), pageClass);
    }

    @Step("Переключаемся на другую вкладку")
    public void switchToTab(int tabIndex) {
        // ждать немного другую вкладку
        List<String> tabs = newArrayList(getDriver().getWindowHandles());
        getDriver().switchTo().window(tabs.get(tabIndex));
    }

    @Step("Переключаемся на следующую вкладку")
    public void switchToNextTab() {
        String currentWindowHandle = getDriver().getWindowHandle();
        List<String> windowHandles = new ArrayList<>(getDriver().getWindowHandles());
        int nextWindowIndex = windowHandles.indexOf(currentWindowHandle) + 1;
        getDriver().switchTo().window(windowHandles.get(
                windowHandles.size() >= nextWindowIndex ? nextWindowIndex : 0
        ));
    }

    @Step("Скроллинг: {pixels} px")
    public void scroll(int pixels) {
        JavascriptExecutor jse = (JavascriptExecutor) getDriver();
        jse.executeScript(String.format("window.scrollBy(0, %d)", pixels), "");
    }

    public void scrollUp(int pixels) {
        scroll(pixels * -1);
    }

    public void scrollDown(int pixels) {
        scroll(pixels);
    }

    @Step("Прокручиваем окно до {totalPixels} px с шагом в {stepPixels} px")
    public void scrolling(int totalPixels, int stepPixels) {
        int i = stepPixels;
        do {
            scroll(stepPixels);
            i += stepPixels;
        } while (Math.abs(i) < Math.abs(totalPixels));
    }

    @Step("Получаем текущую позицию 'Y' браузера")
    public int getCurrentYPosition() {
        JavascriptExecutor jse = (JavascriptExecutor) getDriver();
        return Math.toIntExact((Long) jse.executeScript("return window.pageYOffset;"));
    }

    @Step("Очистка кук")
    public void clearCookies() {
        getDriver().manage().deleteAllCookies();
    }

    @Step("Очистка куки: {name}")
    public void clearCookie(String name) {
        getDriver().manage().deleteCookieNamed(name);
    }

    @Step("Выставляем куку: name={cookieName}, value={cookieValue}, domain={cookieDomain}")
    public void setCookie(String cookieName, String cookieValue, String cookieDomain) {
        Cookie cookie = getDriver().manage().getCookieNamed(cookieName);
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.add(GregorianCalendar.YEAR, 10);
        Cookie newCookie = new Cookie(cookieName, cookieValue, cookieDomain, "/",
                calendar.getTime());
        if (cookie != null) {
            getDriver().manage().deleteCookieNamed(cookieName);
        }
        getDriver().manage().addCookie(newCookie);
    }

    public void onYandexPage() {
        open("https://yandex.ru/");
    }

    @Step("Открываем страницу - {url}")
    public void open(String url) {
        getDriver().get(url);
    }

    public String session() {
        return ((RemoteWebDriver) getDriver()).getSessionId().toString();
    }

    @Step("Обновляем страницу")
    public void refresh() {
        getDriver().navigate().refresh();
    }

    @Step("Проверяем url {matcher}")
    public void shouldUrl(Matcher<String> matcher) {
        await().pollInterval(1, SECONDS).atMost(10, SECONDS)
                .untilAsserted(() -> Assert.assertThat(getDriver().getCurrentUrl(), matcher));
    }

    @Step("Проверяем url: {reason}")
    public void shouldUrl(String reason, Matcher<String> matcher) {
        await().pollInterval(1, SECONDS).atMost(10, SECONDS)
                .untilAsserted(() -> Assert.assertThat(reason, getDriver().getCurrentUrl(), matcher));
    }

    @Step("Ждем {time} {unit} ...")
    public static void waitSomething(long time, TimeUnit unit) {
        try {
            sleep(unit.toMillis(time));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Соглашаемся на браузерный алерт")
    public void acceptAlert() {
        getDriver().switchTo().alert().accept();
    }

    @Step("НЕ соглашаемся на браузерный алерт")
    public void dismissAlert() {
        getDriver().switchTo().alert().dismiss();
    }

    @Step("Получаем куку по имени - {cookieName}")
    public Cookie getCookieBy(String cookieName) {
        return getDriver().manage().getCookieNamed(cookieName);
    }

    @Step("Изменяем размеры окна на {width}x{height}")
    public void setWindowSize(int width, int height) {
        getDriver().manage().window().setSize(new Dimension(width, height));
    }

    @Step("Фокус на элемент со смещением")
    public void focusElementByScrollingOffset(VertisElement element, int scrollBeforeFocus, int scrollAfterFocus) {
        scroll(scrollBeforeFocus);
        element.hover();
        scroll(scrollAfterFocus);
    }

    @Step
    public String getHrefLink(AtlasWebElement element) {
        return element.getAttribute("href");
    }

    public Object executeJavaScript(String script) {
        return ((JavascriptExecutor) getDriver())
                .executeScript(script);
    }

    @Step("Двигаем курсор на «{element}» и кликаем")
    public void moveCursorAndClick(AtlasWebElement element) {
        new Actions(getDriver()).moveToElement(element).click().build().perform();
    }

    @Step("Ставим галку на элемент {element}")
    public void selectElement(AtlasWebElement element) {
        if (!element.isSelected()) {
            element.click();
        }
    }

    @Step("Убираем галку с элемента {element}")
    public void deselectElement(AtlasWebElement element) {
        if (element.isSelected()) {
            element.click();
        }
    }

    @Step("Двигаем курсор на {element}")
    public void moveCursor(AtlasWebElement element) {
        new Actions(getDriver()).moveToElement(element).build().perform();
    }
}
