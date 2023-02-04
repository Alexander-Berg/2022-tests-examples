package ru.yandex.general.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.yandex.general.config.GeneralWebConfig;
import ru.yandex.general.page.BasePage;
import ru.yandex.general.page.ContactsPage;
import ru.yandex.general.page.FavoritesPage;
import ru.yandex.general.page.FeedPage;
import ru.yandex.general.page.FormPage;
import ru.yandex.general.page.ListingPage;
import ru.yandex.general.page.MyOffersPage;
import ru.yandex.general.page.OfferCardPage;
import ru.yandex.general.page.PaymentPage;
import ru.yandex.general.page.PublicProfilePage;
import ru.yandex.general.page.StatisticsPage;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static ru.yandex.general.consts.AdBlockCookies.POSSIBLE_ADBLOCK_COOKIES;

public class BasePageSteps extends WebDriverSteps {

    public static final String CLASSIFIED_REGION_ID = "classified_region_id";
    public static final String YANDEX_GID = "yandex_gid";
    public static final String CLASSIFIED_USER_THEME = "classified_user_theme";
    public static final String CLASSIFIED_USER_HAS_SEEN_PROFILE = "classified_user_has_seen_profile";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String DARK_THEME = "dark";
    public static final String LIGHT_THEME = "light";
    public static final String SYSTEM_THEME = "system";
    public static final String ADULT_CONFIRMED = "classified_adult_confirmed";
    public static final String CLASSIFIED_LISTING_DISPLAY_TYPE = "classified_listing_display_type";
    public static final String GRID = "grid";
    public static final String LIST = "list";
    public static final String CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW = "classified_expired_dialog_was_shown";

    @Inject
    private GeneralWebConfig config;

    @Inject
    private WebDriverManager wm;

    public BasePage onBasePage() {
        return on(BasePage.class);
    }

    public ContactsPage onContactsPage() {
        return on(ContactsPage.class);
    }

    public ListingPage onListingPage() {
        return on(ListingPage.class);
    }

    public FavoritesPage onFavoritesPage() {
        return on(FavoritesPage.class);
    }

    public MyOffersPage onMyOffersPage() {
        return on(MyOffersPage.class);
    }

    public OfferCardPage onOfferCardPage() {
        return on(OfferCardPage.class);
    }

    public PublicProfilePage onProfilePage() {
        return on(PublicProfilePage.class);
    }

    public StatisticsPage onStatisticsPage() {
        return on(StatisticsPage.class);
    }

    public PaymentPage onPaymentPage() {
        return on(PaymentPage.class);
    }

    public FeedPage onFeedPage() {
        return on(FeedPage.class);
    }

    public FormPage onFormPage() {
        return on(FormPage.class);
    }

    public WebDriver getDriver() {
        return super.getDriver();
    }

    @Step("Выставляем куку «{name} = {value}»")
    public void setCookie(String name, String value) {
        setCookie(name, value, config.getBaseDomain());
    }

    public void setMoscowCookie() {
        setCookie(CLASSIFIED_REGION_ID, "213");
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

    public void scrollingToElement(VertisElement element) {
        scrolling(element.getCoordinates().onPage().getY(), 30);
    }

    public void slowScrolling(int pxls) {
        scrolling(pxls, 5);
    }

    public void scrollToTop() {
        ((JavascriptExecutor) wm.getDriver()).executeScript("window.scrollTo(0, 0)");
    }

    @Step("Ресайз «{x} x {y}»")
    public void resize(int x, int y) {
        wm.getDriver().manage().window().setSize(new Dimension(x, y));
    }

    public int getMaxPageHeight() {
        return Integer.parseInt(((JavascriptExecutor) getDriver())
                .executeScript("return document.documentElement.scrollHeight").toString());
    }

    @Step("Браузерное «Назад»")
    public void back() {
        wm.getDriver().navigate().back();
    }

    public void scrollToBottom() {
        ((JavascriptExecutor) wm.getDriver()).executeScript("window.scrollTo(0, document.body.scrollHeight)");
    }

    public void setDarkThemeCookie() {
        setCookie(CLASSIFIED_USER_THEME, DARK_THEME);
    }

    public void wait500MS() {
        waitSomething(500, MILLISECONDS);
    }

    @Step("Получаем кол-во открытых вкладок")
    public int getWindowCount() {
        return getDriver().getWindowHandles().size();
    }

    @Step("Нажимем «Command + Click» по элементу")
    public void cmdClick(WebElement element) {
        Actions actions = new Actions(getDriver());
        actions.keyDown(Keys.COMMAND)
                .click(element)
                .keyUp(Keys.COMMAND)
                .build()
                .perform();
    }

    @Step("Клик правой кнопкой мыши по элементу")
    public void contextClick(WebElement element) {
        Actions actions = new Actions(getDriver());
        actions.contextClick(element).perform();
    }

    @Step("Получаем куку для адблока")
    public void findAdblockCookie() {
        switchToTab(0);
        refresh();
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(1, SECONDS).atMost(20, SECONDS).ignoreExceptions()
                .alias("должна быть хотя бы одна кука из списка")
                .untilAsserted(() -> Assertions.assertThat(getDriver().manage().getCookies().stream()
                        .map(c -> c.getName()).filter(name -> POSSIBLE_ADBLOCK_COOKIES.contains(name))
                        .findAny().get()).isNotNull());
    }

}
