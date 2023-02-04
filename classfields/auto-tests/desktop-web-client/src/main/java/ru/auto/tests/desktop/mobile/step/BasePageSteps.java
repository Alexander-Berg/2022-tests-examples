package ru.auto.tests.desktop.mobile.step;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteExecuteMethod;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.html5.RemoteWebStorage;
import org.openqa.selenium.support.ui.Select;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.auto.tests.desktop.mobile.page.AuthPage;
import ru.auto.tests.desktop.mobile.page.BasePage;
import ru.auto.tests.desktop.mobile.page.BestOfferPage;
import ru.auto.tests.desktop.mobile.page.CardPage;
import ru.auto.tests.desktop.mobile.page.CardPhotosPage;
import ru.auto.tests.desktop.mobile.page.CatalogBodyPage;
import ru.auto.tests.desktop.mobile.page.CatalogGenerationPage;
import ru.auto.tests.desktop.mobile.page.CatalogMainPage;
import ru.auto.tests.desktop.mobile.page.CatalogMarkPage;
import ru.auto.tests.desktop.mobile.page.CatalogModelPage;
import ru.auto.tests.desktop.mobile.page.CatalogPage;
import ru.auto.tests.desktop.mobile.page.ChatPage;
import ru.auto.tests.desktop.mobile.page.CompareCarsPage;
import ru.auto.tests.desktop.mobile.page.DealPage;
import ru.auto.tests.desktop.mobile.page.DealerCardPage;
import ru.auto.tests.desktop.mobile.page.DealerMapPage;
import ru.auto.tests.desktop.mobile.page.DealersListingPage;
import ru.auto.tests.desktop.mobile.page.ElectroModelPage;
import ru.auto.tests.desktop.mobile.page.ElectroPage;
import ru.auto.tests.desktop.mobile.page.FavoritesPage;
import ru.auto.tests.desktop.mobile.page.FiltersPage;
import ru.auto.tests.desktop.mobile.page.GarageAddPage;
import ru.auto.tests.desktop.mobile.page.GarageAllPromoPage;
import ru.auto.tests.desktop.mobile.page.GarageCardPage;
import ru.auto.tests.desktop.mobile.page.GaragePage;
import ru.auto.tests.desktop.mobile.page.GroupAboutModelPage;
import ru.auto.tests.desktop.mobile.page.GroupPage;
import ru.auto.tests.desktop.mobile.page.HistoryPage;
import ru.auto.tests.desktop.mobile.page.ListingPage;
import ru.auto.tests.desktop.mobile.page.LkCreditsDraftPage;
import ru.auto.tests.desktop.mobile.page.LkCreditsPage;
import ru.auto.tests.desktop.mobile.page.LkDealPage;
import ru.auto.tests.desktop.mobile.page.LkPage;
import ru.auto.tests.desktop.mobile.page.LkPromocodesPage;
import ru.auto.tests.desktop.mobile.page.LkReviewsPage;
import ru.auto.tests.desktop.mobile.page.MagPage;
import ru.auto.tests.desktop.mobile.page.MainPage;
import ru.auto.tests.desktop.mobile.page.MarksAndModelsPage;
import ru.auto.tests.desktop.mobile.page.PofferPage;
import ru.auto.tests.desktop.mobile.page.PromoDealPage;
import ru.auto.tests.desktop.mobile.page.PromoDealerPage;
import ru.auto.tests.desktop.mobile.page.PromoPage;
import ru.auto.tests.desktop.mobile.page.ResellerPage;
import ru.auto.tests.desktop.mobile.page.SearchesPage;
import ru.auto.tests.desktop.mobile.page.StatsPage;
import ru.auto.tests.desktop.mobile.page.VideoPage;
import ru.auto.tests.desktop.mobile.page.mobilereviews.ReviewPage;
import ru.auto.tests.desktop.mobile.page.mobilereviews.ReviewsListingPage;
import ru.auto.tests.desktop.mobile.page.mobilereviews.ReviewsMainPage;
import ru.auto.tests.desktop.page.AdsPage;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * Created by kopitsa on 14.09.17.
 */
public class BasePageSteps extends WebDriverSteps {

    public WebDriver getDriver() {
        return super.getDriver();
    }

    public AuthPage onAuthPage() {
        return on(AuthPage.class);
    }

    public BasePage onBasePage() {
        return on(BasePage.class);
    }

    public BestOfferPage onBestOfferPage() {
        return on(BestOfferPage.class);
    }

    public PofferPage onPofferPage() {
        return on(PofferPage.class);
    }

    public CardPage onCardPage() {
        return on(CardPage.class);
    }

    public CardPhotosPage onCardPhotosPage() {
        return on(CardPhotosPage.class);
    }

    public CatalogPage onCatalogPage() {
        return on(CatalogPage.class);
    }

    public CatalogBodyPage onCatalogBodyPage() {
        return on(CatalogBodyPage.class);
    }

    public CatalogMainPage onCatalogMainPage() {
        return on(CatalogMainPage.class);
    }

    public CatalogMarkPage onCatalogMarkPage() {
        return on(CatalogMarkPage.class);
    }

    public CatalogModelPage onCatalogModelPage() {
        return on(CatalogModelPage.class);
    }

    public CatalogGenerationPage onCatalogGenerationPage() {
        return on(CatalogGenerationPage.class);
    }

    public ChatPage onChatPage() {
        return on(ChatPage.class);
    }

    public CompareCarsPage onCompareCarsPage() {
        return on(CompareCarsPage.class);
    }

    public DealPage onDealPage() {
        return on(DealPage.class);
    }

    public DealerCardPage onDealerCardPage() {
        return on(DealerCardPage.class);
    }

    public DealersListingPage onDealersListingPage() {
        return on(DealersListingPage.class);
    }

    public DealerMapPage onDealerMapPage() {
        return on(DealerMapPage.class);
    }

    public ElectroModelPage onElectroModelPage() {
        return on(ElectroModelPage.class);
    }

    public ElectroPage onElectroPage() {
        return on(ElectroPage.class);
    }

    public FavoritesPage onFavoritesPage() {
        return on(FavoritesPage.class);
    }

    public FiltersPage onFiltersPage() {
        return on(FiltersPage.class);
    }

    public GaragePage onGaragePage() {
        return on(GaragePage.class);
    }

    public GarageAddPage onGarageAddPage() {
        return on(GarageAddPage.class);
    }

    public GarageCardPage onGarageCardPage() {
        return on(GarageCardPage.class);
    }

    public GarageAllPromoPage onGarageAllPromoPage() {
        return on(GarageAllPromoPage.class);
    }

    public GroupPage onGroupPage() {
        return on(GroupPage.class);
    }

    public GroupAboutModelPage onGroupAboutModelPage() {
        return on(GroupAboutModelPage.class);
    }

    public HistoryPage onHistoryPage() {
        return on(HistoryPage.class);
    }

    public ListingPage onListingPage() {
        return on(ListingPage.class);
    }

    public LkDealPage onLkDealPage() {
        return on(LkDealPage.class);
    }

    public LkPage onLkPage() {
        return on(LkPage.class);
    }

    public LkPromocodesPage onLkPromocodesPage() {
        return on(LkPromocodesPage.class);
    }

    public LkReviewsPage onLkReviewsPage() {
        return on(LkReviewsPage.class);
    }

    public MagPage onMagPage() {
        return on(MagPage.class);
    }

    public MainPage onMainPage() {
        return on(MainPage.class);
    }

    public MarksAndModelsPage onMarksAndModelsPage() {
        return on(MarksAndModelsPage.class);
    }

    public PromoPage onPromoPage() {
        return on(PromoPage.class);
    }

    public PromoDealPage onPromoDealPage() {
        return on(PromoDealPage.class);
    }

    public PromoDealerPage onPromoDealerPage() {
        return on(PromoDealerPage.class);
    }

    public ReviewsMainPage onReviewsMainPage() {
        return on(ReviewsMainPage.class);
    }

    public ReviewsListingPage onReviewsListingPage() {
        return on(ReviewsListingPage.class);
    }

    public ReviewPage onReviewPage() {
        return on(ReviewPage.class);
    }

    public ResellerPage onResellerPage() {
        return on(ResellerPage.class);
    }

    public SearchesPage onSearchesPage() {
        return on(SearchesPage.class);
    }

    public StatsPage onStatsPage() {
        return on(StatsPage.class);
    }

    public VideoPage onVideoPage() {
        return on(VideoPage.class);
    }

    public LkCreditsPage onLkCreditsPage() {
        return on(LkCreditsPage.class);
    }

    public LkCreditsDraftPage onLkCreditsDraftPage() {
        return on(LkCreditsDraftPage.class);
    }

    @Step("Выбираем значение селектора = «{option}»")
    public void selectOption(VertisElement selector, String option) {
        new Select(selector.waitUntil(isDisplayed())).selectByVisibleText(option);
    }

    @Step("Два элемента должны совпадать")
    public <T> void shouldEqual(String reason, T first, T second) {
        assertThat(reason, first, equalTo(second));
    }

    @Step("Двигаем курсор на {element} со смещением и кликаем")
    public void moveCursorAndClick(VertisElement element, int x, int y) {
        (new Actions(this.getDriver())).moveToElement(element, x, y).click().perform();
    }

    @Step("Задаем атрибут «{attr}» = «{value}» элемента «{xpath}»")
    public void setElementAttribute(String xpath, String attr, String value) {
        ((JavascriptExecutor) getDriver()).executeScript(format("arguments[0].setAttribute('%s', '%s')",
                attr, value), getDriver().findElement(By.xpath(xpath)));
    }

    @Step("Задаем атрибут «{attr}» = «{value}» элемента «{element}»")
    public void setElementAttribute(VertisElement element, String attr, String value) {
        if (isElementExist(element)) {
            ((JavascriptExecutor) getDriver()).executeScript(format("arguments[0].setAttribute('%s', '%s')",
                    attr, value), element);
        }
    }

    @Step("Открываем новую вкладку браузера")
    public void openNewTab() {
        ((JavascriptExecutor) getDriver()).executeScript("window.open()");
    }

    @Step("Жмём ОК в алерте")
    public void acceptAlert() {
        getDriver().switchTo().alert().accept();
    }

    @Step("Очищаем инпут")
    public void clearInput(VertisElement element) {
        //input().clear иногда не срабатывает
        element.click();
        element.sendKeys(Keys.chord(Keys.CONTROL, Keys.HOME));
        element.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, Keys.END));
    }

    @Step("Переключаемся на дефолтный фрейм")
    public void switchToDefaultFrame() {
        getDriver().switchTo().defaultContent();
    }

    @Step("Переключаемся на фрейм способов оплаты")
    public void switchToPaymentMethodsFrame() {
        getDriver().switchTo().frame(getDriver().findElement(By.className("PaymentDialogContainer__frame")));
    }

    @Step("Переключаемся на фрейм Я.Кассы")
    public void switchToYaKassaFrame() {
        getDriver().switchTo().frame(getDriver().findElement(By.className("BillingMobile__cardFrame")));
    }

    @Step("Получаем смещение страницы по вертикали")
    public long getPageYOffset() {
        return (Long) ((JavascriptExecutor) getDriver()).executeScript("return window.pageYOffset;");
    }

    @Step("Проверяем существование элемента {element}")
    public boolean isElementExist(VertisElement element) {
        try {
            element.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    @Step("Открываем поп-ап оплаты услуги {vasTitle}")
    public void openVasPopup(String vasTitle) {
        scrollAndClick(onBasePage().vas().service(vasTitle));
        onBasePage().vasPopup().waitUntil(isDisplayed());
        onBasePage().vasPopup().payment().click();
    }

    @Step("Выбираем способ оплаты «{paymentMethod}»")
    public void selectPaymentMethod(String paymentMethod) {
        switchToPaymentMethodsFrame();
        onBasePage().paymentMethodsFrameContent().paymentMethod(paymentMethod).waitUntil(isDisplayed()).hover().click();
        switchToDefaultFrame();
    }

    @Step("Выбираем пакет с названием «{vinPackageName}»")
    public void selectVinPackage(String vinPackageName) {
        switchToPaymentMethodsFrame();
        onBasePage().paymentMethodsFrameContent().vinPackageName(vinPackageName).waitUntil(isDisplayed()).hover()
                .click();
        switchToDefaultFrame();
    }

    @Step("Клиакаем кнопку «{button}»")
    public void clickButton(String button) {
        switchToPaymentMethodsFrame();
        onBasePage().paymentMethodsFrameContent().button(button).waitUntil(isDisplayed()).hover().click();
        switchToDefaultFrame();
    }

    @Step("Клиакаем кнопку «Оплатить ...»")
    public void clickPayButton() {
        switchToPaymentMethodsFrame();
        onBasePage().paymentMethodsFrameContent().payButton().hover().click();
        switchToDefaultFrame();
        waitSomething(1, TimeUnit.SECONDS);
    }

    @Step("Скроллим вниз, чтобы не мешали плавающие элементы, и кликаем")
    public void scrollAndClick(VertisElement element) {
        element.hover();
        scrollDown(200);
        element.click();
    }

    @Step("Скроллим вверх и кликаем")
    public void scrollUpAndClick(VertisElement element) {
        element.hover();
        scrollUp(200);
        element.click();
    }

    @Step("Прячем элемент «{element}»")
    public void hideElement(VertisElement element) {
        setElementAttribute(element, "style", "display:none");
    }

    @Step("Показываем элемент «{element}»")
    public void showElement(VertisElement element) {
        setElementAttribute(element, "style", "display:block");
    }

    @Step("Прячем кнопку применения фильтров")
    public void hideApplyFiltersButton() {
        hideElement(onListingPage().applyFilters());
    }

    @Step("Показываем кнопку применения фильтров")
    public void showApplyFiltersButton() {
        showElement(onListingPage().applyFilters());
    }

    public AdsPage onAdsPage() {
        return on(AdsPage.class);
    }

    @Step("Drag and drop with hover")
    public void dragAndDropWithHover(VertisElement element, int xOffset, int yOffset) {
        element.hover();
        (new Actions(getDriver()))
                .dragAndDropBy(element, xOffset, yOffset).build().perform();
    }

    @Step("Добавляем файл «{path}» в файл-инпут в попапе")
    public void addFileInPopup(String path) {
        onBasePage().popup().fileInput().sendKeys(getFileByPath(path));
    }

    private static String getFileByPath(String path) {
        return new File(path).getAbsolutePath();
    }

    @Step("Записываем в LocalStorage значение «{value}» по ключу «{key}»")
    public void writeInLocalStorage(String key, String value) {
        RemoteExecuteMethod executeMethod = new RemoteExecuteMethod((RemoteWebDriver) getDriver());
        RemoteWebStorage webStorage = new RemoteWebStorage(executeMethod);
        LocalStorage localStorage = webStorage.getLocalStorage();

        localStorage.setItem(key, value);
    }

    public void setWindowHeight(int height) {
        setWindowSize(getScrollWidth(), height);
    }

    public void setWindowMaxHeight() {
        setWindowSize(getScrollWidth(), getScrollHeight());
    }

    private int getScrollWidth() {
        String width = ((JavascriptExecutor) getDriver())
                .executeScript("return document.documentElement.scrollWidth").toString();
        return Integer.parseInt(width);
    }

    private int getScrollHeight() {
        String height = ((JavascriptExecutor) getDriver())
                .executeScript("return document.documentElement.scrollHeight").toString();
        return Integer.parseInt(height);
    }

    public void waitScrollDownStopped() {
        AtomicReference<Long> lastScrollPosition = new AtomicReference<>(getPageYOffset());

        await("Ждём остановку скрола")
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .ignoreExceptions()
                .until(() -> {
                    if (getPageYOffset() > lastScrollPosition.get()) {
                        lastScrollPosition.set(getPageYOffset());
                        return false;
                    } else {
                        return true;
                    }
                });
    }

    public void assertThatPageBeenScrolledToElement(WebElement element) {
        // Ассерт может падать при локальных запусках в браузере, для локальной проверки надежнее запускать на гриде

        assertThat("Позиция скролла страницы не соответствует смещению элемента относительно начала страницы",
                getPageYOffset(), equalTo(parseLong(element.getAttribute("offsetTop"))));
    }

}
