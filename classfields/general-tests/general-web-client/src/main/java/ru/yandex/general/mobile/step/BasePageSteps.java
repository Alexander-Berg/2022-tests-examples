package ru.yandex.general.mobile.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.yandex.general.config.GeneralWebConfig;
import ru.yandex.general.mobile.page.BasePage;
import ru.yandex.general.mobile.page.ContactsPage;
import ru.yandex.general.mobile.page.FavoritesPage;
import ru.yandex.general.mobile.page.FormPage;
import ru.yandex.general.mobile.page.ListingPage;
import ru.yandex.general.mobile.page.MyOffersPage;
import ru.yandex.general.mobile.page.OfferCardPage;
import ru.yandex.general.mobile.page.ProfilePage;
import ru.yandex.general.mobile.page.PaymentPage;
import ru.yandex.general.mobile.page.StatisticsPage;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class BasePageSteps extends WebDriverSteps {

    public static final String CLASSIFIED_USER_THEME = "classified_user_theme";
    public static final String DARK_THEME = "dark";
    public static final String LIGHT_THEME = "light";
    public static final String SYSTEM_THEME = "system";
    public static final String CLASSIFIED_USER_HAS_SEEN_PROFILE = "classified_user_has_seen_profile";
    public static final String TRUE = "true";
    public static final String CLASSIFIED_LISTING_DISPLAY_TYPE = "classified_listing_display_type";
    public static final String GRID = "grid";
    public static final String LIST = "list";
    public static final String ADULT_CONFIRMED = "classified_adult_confirmed";
    public static final String CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW = "classified_expired_dialog_was_shown";
    public static final String APP_SPLASH_SHOWN = "app_splash_shown";
    public static final String SEEN = "seen";

    @Inject
    private GeneralWebConfig config;

    @Inject
    private WebDriverManager wm;

    public BasePage onBasePage() {
        return on(BasePage.class);
    }

    public FormPage onFormPage() {
        return on(FormPage.class);
    }

    public MyOffersPage onMyOffersPage() {
        return on(MyOffersPage.class);
    }

    public ListingPage onListingPage() {
        return on(ListingPage.class);
    }

    public OfferCardPage onOfferCardPage() {
        return on(OfferCardPage.class);
    }

    public FavoritesPage onFavoritesPage() {
        return on(FavoritesPage.class);
    }

    public ContactsPage onContactsPage() {
        return on(ContactsPage.class);
    }

    public ProfilePage onProfilePage() {
        return on(ProfilePage.class);
    }

    public PaymentPage onPaymentPage() {
        return on(PaymentPage.class);
    }

    public StatisticsPage onStatisticsPage() {
        return on(StatisticsPage.class);
    }

    public WebDriver getDriver() {
        return super.getDriver();
    }

    @Step("Выставляем куку «{name}»")
    public void setCookie(String name, String value) {
        setCookie(name, value, config.getBaseDomain());
    }

    public void setMoscowCookie() {
        setCookie("classified_region_id", "213");
    }

    @Step("Проверяем наличие куки «{name} = {value}»")
    public void shouldSeeCookie(String name, String value) {
        await().ignoreException(NullPointerException.class).untilAsserted(() ->
                assertThat("Значение куки соответствует", getCookieBy(name).getValue(), is(value)));
    }

    @Step("Проверяем отсутствие куки «{name}»")
    public void shouldNotSeeCookie(String name) {
        await().pollDelay(3, TimeUnit.SECONDS)
                .pollInterval(1, SECONDS)
                .atMost(8, SECONDS).untilAsserted(() ->
                assertThat("Значение куки соответствует", getCookieBy(name), is(nullValue())));
    }

    @Step("Проверяем что значение куки «{name} = {value}»")
    public void assertCookieValueIs(String name, String value) {
        await().ignoreException(NullPointerException.class).untilAsserted(() ->
                assertThat("Значение куки соответствует", getCookieBy(name).getValue(), is(value)));
    }

    public void scrollingToElement(VertisElement element) {
        scrolling(element.getCoordinates().onPage().getY(), 30);
    }

    public void slowScrolling(int pxls) {
        scrolling(pxls, 3);
    }

    @Step("Ресайз «{x} x {y}»")
    public void resize(int x, int y) {
        wm.getDriver().manage().window().setSize(new Dimension(x, y));
    }

    public void setDarkThemeCookie() {
        setCookie(CLASSIFIED_USER_THEME, DARK_THEME);
    }

    public void scrollToTop() {
        ((JavascriptExecutor) wm.getDriver()).executeScript("window.scrollTo(0, 0)");
    }

    public void scrollToBottom() {
        ((JavascriptExecutor) wm.getDriver()).executeScript("window.scrollTo(0, document.body.scrollHeight)");
    }

    public int getMaxPageHeight() {
        return Integer.parseInt(((JavascriptExecutor) getDriver())
                .executeScript("return document.documentElement.scrollHeight").toString());
    }

    @Step("Двигаем элемент «{element}» к элементу «{elementTo}»")
    public void moveSlider(WebElement element, WebElement elementTo) {
        Actions action = new Actions(wm.getDriver());
        action.dragAndDrop(element, elementTo)
                .pause(Duration.ofSeconds(1)).release().build().perform();
    }

    @Step("Браузерное «Назад»")
    public void back() {
        wm.getDriver().navigate().back();
    }

    public void wait500MS() {
        waitSomething(500, MILLISECONDS);
    }

}
