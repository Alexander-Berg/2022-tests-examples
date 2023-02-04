package ru.auto.tests.desktop.step;

import com.google.common.base.Splitter;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.auto.tests.desktop.page.AdsPage;
import ru.auto.tests.desktop.page.AuctionPage;
import ru.auto.tests.desktop.page.BasePage;
import ru.auto.tests.desktop.page.BuyoutPage;
import ru.auto.tests.desktop.page.CardPage;
import ru.auto.tests.desktop.page.CatalogMarkPage;
import ru.auto.tests.desktop.page.CatalogNewPage;
import ru.auto.tests.desktop.page.CatalogPage;
import ru.auto.tests.desktop.page.CompareCarsPage;
import ru.auto.tests.desktop.page.ComparePage;
import ru.auto.tests.desktop.page.DealPage;
import ru.auto.tests.desktop.page.DealerCardPage;
import ru.auto.tests.desktop.page.DealerDisturbutorPage;
import ru.auto.tests.desktop.page.DealerListingPage;
import ru.auto.tests.desktop.page.ElectroModelPage;
import ru.auto.tests.desktop.page.ElectroPage;
import ru.auto.tests.desktop.page.GarageAddPage;
import ru.auto.tests.desktop.page.GarageAllPromoPage;
import ru.auto.tests.desktop.page.GarageCardPage;
import ru.auto.tests.desktop.page.GaragePage;
import ru.auto.tests.desktop.page.GroupPage;
import ru.auto.tests.desktop.page.HistoryPage;
import ru.auto.tests.desktop.page.ListingPage;
import ru.auto.tests.desktop.page.MagPage;
import ru.auto.tests.desktop.page.MainPage;
import ru.auto.tests.desktop.page.NotFoundPage;
import ru.auto.tests.desktop.page.PromoAppPage;
import ru.auto.tests.desktop.page.PromoCarsLandingPage;
import ru.auto.tests.desktop.page.PromoCreditPage;
import ru.auto.tests.desktop.page.PromoDealPage;
import ru.auto.tests.desktop.page.PromoDealerConferencesPage;
import ru.auto.tests.desktop.page.PromoDealerPage;
import ru.auto.tests.desktop.page.PromoDealerPanoramasPage;
import ru.auto.tests.desktop.page.PromoLoyaltyPage;
import ru.auto.tests.desktop.page.PromoOsagoPage;
import ru.auto.tests.desktop.page.PromoPage;
import ru.auto.tests.desktop.page.ResellerPage;
import ru.auto.tests.desktop.page.StatsPage;
import ru.auto.tests.desktop.page.VideoPage;
import ru.auto.tests.desktop.page.YandexTrustPage;
import ru.auto.tests.desktop.page.auth.AuthPage;
import ru.auto.tests.desktop.page.cabinet.AutobidderPage;
import ru.auto.tests.desktop.page.cabinet.CabinetBackOnSalePage;
import ru.auto.tests.desktop.page.cabinet.CabinetBookingPage;
import ru.auto.tests.desktop.page.cabinet.CabinetCallsPage;
import ru.auto.tests.desktop.page.cabinet.CabinetCallsRedirectPhonesPage;
import ru.auto.tests.desktop.page.cabinet.CabinetCallsSettingsPage;
import ru.auto.tests.desktop.page.cabinet.CabinetDashboardPage;
import ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage;
import ru.auto.tests.desktop.page.cabinet.CabinetOffersPage;
import ru.auto.tests.desktop.page.cabinet.CabinetOrdersCreditPage;
import ru.auto.tests.desktop.page.cabinet.CabinetOrdersNewCarsPage;
import ru.auto.tests.desktop.page.cabinet.CabinetPlacementReportPage;
import ru.auto.tests.desktop.page.cabinet.CabinetPriceReportPage;
import ru.auto.tests.desktop.page.cabinet.CabinetReportValidatorPage;
import ru.auto.tests.desktop.page.cabinet.CabinetSalonCardPage;
import ru.auto.tests.desktop.page.cabinet.CabinetSettingsChatsPage;
import ru.auto.tests.desktop.page.cabinet.CabinetSettingsPage;
import ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage;
import ru.auto.tests.desktop.page.cabinet.CabinetSettingsWhiteListPage;
import ru.auto.tests.desktop.page.cabinet.CabinetTradeInPage;
import ru.auto.tests.desktop.page.cabinet.CabinetUsersPage;
import ru.auto.tests.desktop.page.cabinet.CabinetVinPage;
import ru.auto.tests.desktop.page.cabinet.CabinetWalkInPage;
import ru.auto.tests.desktop.page.cabinet.CabinetWalletPage;
import ru.auto.tests.desktop.page.cabinet.CalculatorPage;
import ru.auto.tests.desktop.page.cabinet.NewCalculatorPage;
import ru.auto.tests.desktop.page.cabinet.StartPage;
import ru.auto.tests.desktop.page.cabinet.agency.AgencyCabinetClientsPage;
import ru.auto.tests.desktop.page.cabinet.agency.AgencyCabinetMainPage;
import ru.auto.tests.desktop.page.desktopreviews.ReviewPage;
import ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage;
import ru.auto.tests.desktop.page.desktopreviews.ReviewsMainPage;
import ru.auto.tests.desktop.page.embed.CarouselListingPage;
import ru.auto.tests.desktop.page.embed.CarouselWidgetPage;
import ru.auto.tests.desktop.page.embed.VinSearchPage;
import ru.auto.tests.desktop.page.forms.FormsEvaluationPage;
import ru.auto.tests.desktop.page.forms.FormsPage;
import ru.auto.tests.desktop.page.forms.FormsRequisitesPage;
import ru.auto.tests.desktop.page.lk.DealsPage;
import ru.auto.tests.desktop.page.lk.LkCreditsDraftPage;
import ru.auto.tests.desktop.page.lk.LkCreditsPage;
import ru.auto.tests.desktop.page.lk.LkResellerSalesPage;
import ru.auto.tests.desktop.page.lk.LkReviewsPage;
import ru.auto.tests.desktop.page.lk.LkSalesNewPage;
import ru.auto.tests.desktop.page.lk.LkSalesPage;
import ru.auto.tests.desktop.page.lk.ProfilePage;
import ru.auto.tests.desktop.page.lk.SettingsPage;
import ru.auto.tests.desktop.page.lk.WalletPage;
import ru.auto.tests.desktop.page.poffer.PofferPage;
import ru.auto.tests.desktop.page.poffer.beta.BetaPofferPage;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_NARROW_PAGE;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;
import static ru.auto.tests.desktop.utils.Utils.getMatchedString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * Created by kopitsa on 25.08.17.
 */
public class BasePageSteps extends WebDriverSteps {

    @Inject
    private UrlSteps urlSteps;

    public BasePage onBasePage() {
        return on(BasePage.class);
    }

    public BuyoutPage onBuyoutPage() {
        return on(BuyoutPage.class);
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

    public CardPage onCardPage() {
        return on(CardPage.class);
    }

    public ListingPage onListingPage() {
        return on(ListingPage.class);
    }

    public MainPage onMainPage() {
        return on(MainPage.class);
    }

    public DealerCardPage onDealerCardPage() {
        return on(DealerCardPage.class);
    }

    public DealerListingPage onDealerListingPage() {
        return on(DealerListingPage.class);
    }

    public ElectroModelPage onElectroModelPage() {
        return on(ElectroModelPage.class);
    }

    public ElectroPage onElectroPage() {
        return on(ElectroPage.class);
    }

    public PromoPage onPromoPage() {
        return on(PromoPage.class);
    }

    public PromoAppPage onAppPromoPage() {
        return on(PromoAppPage.class);
    }

    public PromoCreditPage onPromoCreditPage() {
        return on(PromoCreditPage.class);
    }

    public PromoDealPage onPromoDealPage() {
        return on(PromoDealPage.class);
    }

    public PromoDealerPage onPromoDealerPage() {
        return on(PromoDealerPage.class);
    }

    public PromoDealerConferencesPage onPromoDealerConferencesPage() {
        return on(PromoDealerConferencesPage.class);
    }

    public PromoDealerPanoramasPage onPromoDealerPanoramasPage() {
        return on(PromoDealerPanoramasPage.class);
    }

    public PromoCarsLandingPage onPromoCarsLandingPage() {
        return on(PromoCarsLandingPage.class);
    }

    public PromoLoyaltyPage onLoyaltyPromoPage() {
        return on(PromoLoyaltyPage.class);
    }

    public PromoOsagoPage onPromoOsagoPage() {
        return on(PromoOsagoPage.class);
    }

    public CatalogPage onCatalogPage() {
        return on(CatalogPage.class);
    }

    public CatalogNewPage onCatalogNewPage() {
        return on(CatalogNewPage.class);
    }

    public CatalogMarkPage onCatalogMarkPage() {
        return on(CatalogMarkPage.class);
    }

    public VideoPage onVideoPage() {
        return on(VideoPage.class);
    }

    public StatsPage onStatsPage() {
        return on(StatsPage.class);
    }

    public ComparePage onComparePage() {
        return on(ComparePage.class);
    }

    public CompareCarsPage onCompareCarsPage() {
        return on(CompareCarsPage.class);
    }

    public NotFoundPage onNotFoundPage() {
        return on(NotFoundPage.class);
    }

    public CabinetOffersPage onCabinetOffersPage() {
        return on(CabinetOffersPage.class);
    }

    public HistoryPage onHistoryPage() {
        return on(HistoryPage.class);
    }

    public CabinetVinPage onCabinetHistoryPage() {
        return on(CabinetVinPage.class);
    }

    public CabinetDashboardPage onCabinetDashboardPage() {
        return on(CabinetDashboardPage.class);
    }

    public CalculatorPage onCalculatorPage() {
        return on(CalculatorPage.class);
    }

    public CabinetWalkInPage onCabinetWalkInPage() {
        return on(CabinetWalkInPage.class);
    }

    public CabinetWalletPage onCabinetWalletPage() {
        return on(CabinetWalletPage.class);
    }

    public CabinetBackOnSalePage onCabinetOnSaleAgainPage() {
        return on(CabinetBackOnSalePage.class);
    }

    public CabinetFeedsPage onCabinetFeedsPage() {
        return on(CabinetFeedsPage.class);
    }

    public CabinetSalonCardPage onCabinetSalonCardPage() {
        return on(CabinetSalonCardPage.class);
    }

    public CabinetOrdersNewCarsPage onCabinetOrdersNewCarsPage() {
        return on(CabinetOrdersNewCarsPage.class);
    }

    public CabinetOrdersCreditPage onCabinetOrdersCreditPage() {
        return on(CabinetOrdersCreditPage.class);
    }

    public CabinetTradeInPage onCabinetTradeInPage() {
        return on(CabinetTradeInPage.class);
    }

    public CabinetReportValidatorPage onCabinetReportValidatorPage() {
        return on(CabinetReportValidatorPage.class);
    }

    public CabinetPlacementReportPage onCabinetPlacementReportPage() {
        return on(CabinetPlacementReportPage.class);
    }

    public CabinetCallsPage onCallsPage() {
        return on(CabinetCallsPage.class);
    }

    public CabinetCallsRedirectPhonesPage onCallsRedirectPhonesPage() {
        return on(CabinetCallsRedirectPhonesPage.class);
    }

    public CabinetCallsSettingsPage onCallsSettingsPage() {
        return on(CabinetCallsSettingsPage.class);
    }

    public CabinetUsersPage onCabinetUsersPage() {
        return on(CabinetUsersPage.class);
    }

    public CabinetSettingsPage onCabinetSettingsPage() {
        return on(CabinetSettingsPage.class);
    }

    public SettingsPage onSettingsPage() {
        return on(SettingsPage.class);
    }

    public CabinetSettingsSubscriptionsPage onSettingsSubscriptionsPage() {
        return on(CabinetSettingsSubscriptionsPage.class);
    }

    public CabinetSettingsChatsPage onSettingsChatsPage() {
        return on(CabinetSettingsChatsPage.class);
    }

    public CabinetSettingsWhiteListPage onSettingsWhiteListPage() {
        return on(CabinetSettingsWhiteListPage.class);
    }

    public CabinetBookingPage onCabinetBookingPage() {
        return on(CabinetBookingPage.class);
    }

    public AgencyCabinetMainPage onAgencyCabinetMainPage() {
        return on(AgencyCabinetMainPage.class);
    }

    public AgencyCabinetClientsPage onAgencyCabinetClientsPage() {
        return on(AgencyCabinetClientsPage.class);
    }

    public AutobidderPage onAutobidderPage() {
        return on(AutobidderPage.class);
    }

    public NewCalculatorPage onNewCalculatorPage() {
        return on(NewCalculatorPage.class);
    }

    public StartPage onStartPage() {
        return on(StartPage.class);
    }

    public AdsPage onAdsPage() {
        return on(AdsPage.class);
    }

    public YandexTrustPage onYandexTrustPage() {
        return on(YandexTrustPage.class);
    }

    public LkSalesPage onLkSalesPage() {
        return on(LkSalesPage.class);
    }

    public LkSalesNewPage onLkSalesNewPage() {
        return on(LkSalesNewPage.class);
    }

    public LkResellerSalesPage onLkResellerSalesPage() {
        return on(LkResellerSalesPage.class);
    }

    public LkReviewsPage onLkReviewsPage() {
        return on(LkReviewsPage.class);
    }

    public ProfilePage onProfilePage() {
        return on(ProfilePage.class);
    }

    public LkCreditsPage onLkCreditsPage() {
        return on(LkCreditsPage.class);
    }

    public LkCreditsDraftPage onLkCreditsDraftPage() {
        return on(LkCreditsDraftPage.class);
    }

    public WalletPage onWalletPage() {
        return on(WalletPage.class);
    }

    public MagPage onMagPage() {
        return on(MagPage.class);
    }

    public PofferPage onPofferPage() {
        return on(PofferPage.class);
    }

    public CabinetPriceReportPage onPriceReportPage() {
        return on(CabinetPriceReportPage.class);
    }

    public BetaPofferPage onBetaPofferPage() {
        return on(BetaPofferPage.class);
    }

    public GroupPage onGroupPage() {
        return on(GroupPage.class);
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

    public AuthPage onAuthPage() {
        return on(AuthPage.class);
    }

    public FormsPage onFormsPage() {
        return on(FormsPage.class);
    }

    public FormsRequisitesPage onFormsRequisitesPage() {
        return on(FormsRequisitesPage.class);
    }

    public FormsEvaluationPage onFormsEvaluationPage() {
        return on(FormsEvaluationPage.class);
    }

    public WebDriver driver() {
        return super.getDriver();
    }

    public CarouselWidgetPage onCarouselWigetPage() {
        return on(CarouselWidgetPage.class);
    }

    public CarouselListingPage onCarouselListingPage() {
        return on(CarouselListingPage.class);
    }

    public VinSearchPage onVinSearchPage() {
        return on(VinSearchPage.class);
    }

    public DealerDisturbutorPage onDealerDisturbutorPage() {
        return on(DealerDisturbutorPage.class);
    }

    public DealsPage onLkDealsPage() {
        return on(DealsPage.class);
    }

    public DealPage onDealPage() {
        return on(DealPage.class);
    }

    public AuctionPage onAuctionPage() {
        return on(AuctionPage.class);
    }

    @Step("Извлекаем id оффера из аттрибута href")
    public String getOfferId(VertisElement element) {
        return getMatchedString(element.getAttribute("href"), "/(\\d+)-");
    }

    @Step("Извлекаем id оффера из урла")
    public String getOfferId(String url) {
        return getMatchedString(url, "/(\\d+)-");
    }

    @Step("Извлекаем hash оффера из урла")
    public String getOfferHash(String url) {
        return getMatchedString(url, "-([\\d\\D]+?)/");
    }

    @Step("Извлекаем id оффера из текущего урла")
    public String getOfferId() {
        List<String> paths = Splitter.on("/").splitToList(urlSteps.getCurrentUrl());
        return paths.get(paths.size() - 2);
    }

    @Step("Два элемента должны совпадать")
    public <T> void shouldEqual(String reason, T first, T second) {
        assertThat(reason, first, equalTo(second));
    }

    @Step("Открываем новую вкладку браузера")
    public void openNewTab() {
        ((JavascriptExecutor) getDriver()).executeScript("window.open()");
    }

    @Step("Находим объявление, на карточке которого {num} или больше похожих")
    public void findSaleWithSimilar(int num) {
        List<String> urls = onListingPage().salesList().stream()
                .map(sale -> sale.nameLink().getAttribute("href")).collect(toList());

        boolean found = urls.stream().anyMatch(url -> {
            urlSteps.open(url);
            focusElementByScrollingOffset(onCardPage().footer(), 0, -300);
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return onCardPage()
                    .findElements(By.xpath("//div[contains(@class, 'sale-similar-carousel')]" +
                            "//li[contains(@class, 'carousel__item')] | " +
                            "//div[contains(@class, 'CarouselLazyOffers')][contains(., 'Похожие')]" +
                            "//li[contains(@class, 'Carousel__item')]"))
                    .size() >= num;
        });

        assertThat("Не нашли объявление с нужным количеством похожих", found);
    }

    @Step("Задаем атрибут «{attr}» = «{value}» элемента «{xpath}»")
    public void setElementAttribute(VertisElement element, String attr, String value) {
        if (isElementExist(element)) {
            ((JavascriptExecutor) getDriver()).executeScript(format("arguments[0].setAttribute('%s', '%s')",
                    attr, value), element);
        }
    }

    // решение проблемы не работающего ховера на некоторых элементах в свежем хроме
    @Step("Наводим на элемент")
    public void mouseOver(WebElement webElement) {
        String javaScript = "var evObj = document.createEvent('MouseEvents');" +
                "evObj.initMouseEvent(\"mouseover\",true, false, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);" +
                "arguments[0].dispatchEvent(evObj);";

        ((JavascriptExecutor) getDriver()).executeScript(javaScript, webElement);
    }

    @Step("Drag and drop")
    public void dragAndDrop(VertisElement element, int xOffset, int yOffset) {
        (new Actions(this.getDriver()))
                .dragAndDropBy(element, xOffset, yOffset).build().perform();
    }

    @Step("Клик мышкой")
    public void mouseClick(VertisElement element) {
        (new Actions(this.getDriver())).clickAndHold(element).build().perform();
        (new Actions(this.getDriver())).release().build().perform();
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

    @Step("Рефрешим страницу, пока не увидим пресет {presetTitle}")
    public void waitForPreset(String presetTitle) {
        if (onMainPage().presets().preset(presetTitle).isDisplayed()) {
            return;
        }
        await().ignoreExceptions().atMost(60, SECONDS).pollInterval(3, SECONDS)
                .until(() -> {
                    refresh();
                    return onMainPage().presets().preset(presetTitle).isDisplayed();
                });
    }

    @Step("Проверяем, есть ли алерт на странице")
    public boolean isAlertPresent() {
        try {
            driver().switchTo().alert();
            return true;
        } catch (NoAlertPresentException ex) {
            return false;
        }
    }

    @Step("Двигаем курсор на {element} со смещением и кликаем")
    public void moveCursorAndClick(VertisElement element, int x, int y) {
        (new Actions(this.getDriver())).moveToElement(element, x, y).click().perform();
    }

    @Step("Прячем элемент «{element}»")
    public void hideElement(VertisElement element) {
        setElementAttribute(element, "style", "display:none");
    }

    @Step("Показываем элемент «{element}»")
    public void showElement(VertisElement element) {
        setElementAttribute(element, "style", "null");
    }

    @Step("Снимаем фокус с элемента «{element}»")
    public void unfocusElement(VertisElement element) {
        if (isElementExist(element)) {
            ((JavascriptExecutor) getDriver()).executeScript("arguments[0].blur()", element);
        }
    }

    @Step("Переключаемся на дефолтный фрейм")
    public void switchToDefaultFrame() {
        getDriver().switchTo().defaultContent();
    }

    @Step("Рефрешим страницу, пока в галерее не будет блока с отчётом")
    public void waitForVinBlock() {
        await().ignoreExceptions().atMost(60, SECONDS).pollInterval(5, SECONDS)
                .until(() -> {
                    refresh();
                    onCardPage().gallery().currentImage().click();
                    return onCardPage().fullScreenGallery().vinReport().getText()
                            .equals("Отчёт по автомобилю\n2 записи в истории пробегов\nЕщё 1 размещение на Авто.ру\n" +
                                    "2 записи в истории эксплуатации\nОдин отчёт за 499 ₽");
                });
    }

    @Step("Добавляем файл «{path}» в файл-инпут в попапе")
    public void addFileInPopup(String path) {
        onBasePage().popup().fileInput().sendKeys(getFileByPath(path));
    }

    private static String getFileByPath(String path) {
        return new File(path).getAbsolutePath();
    }

    // todo: workaround for https://bugs.chromium.org/p/chromedriver/issues/detail?id=3999
    @Step("Вводим значение «{value}» в переданный инпут")
    public void input(WebElement input, String value) {
        input.clear();
        input.sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END, Keys.DELETE));

        input.click();
        Actions action = new Actions(getDriver());
        action.sendKeys(value);
        action.perform();
    }

    @Step("Авторизуемся в Авто.ру за уже авторизованный паспортный аккаунт")
    public void bindYandexAccount() {
        onBasePage().header().button("Войти").click();
        onAuthPage().yandexIdLoginButton().click();
        onBasePage().passportAccount().click();
        onMainPage().header().logo().waitUntil(isDisplayed());
    }

    @Step("Ждём появления оффера «{offerId}» в ЛК")
    public void waitOfferInLk(String offerId) {
        await().atMost(30, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).ignoreExceptions()
                .until(() ->
                {
                    if (onLkSalesPage().salesList().stream().anyMatch(salesListItem ->
                            salesListItem.link().getAttribute("href").contains(offerId))) {
                        return true;
                    } else {
                        refresh();
                        return false;
                    }
                });
    }

    public void setWindowMaxHeight() {
        setWindowSize(getScrollWidth(), getScrollHeight());
    }

    public void setWindowHeight(int height) {
        setWindowSize(getScrollWidth(), height);
    }

    public void setWindowWidth(int width) {
        setWindowSize(width, getScrollHeight());
    }

    public void setNarrowWindowSize() {
        setWindowSize(WIDTH_NARROW_PAGE, getScrollHeight());
    }

    public void setNarrowWindowSize(int height) {
        setWindowSize(WIDTH_NARROW_PAGE, height);
    }

    public void setWideWindowSize() {
        setWindowSize(WIDTH_WIDE_PAGE, getScrollHeight());
    }

    public void setWideWindowSize(int height) {
        setWindowSize(WIDTH_WIDE_PAGE, height);
    }

    public void scrollByHeightPercent(int percent) {
        scrollDown(getScrollHeight() * percent / 100);
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

    @Override
    public void switchToNextTab() {
        try {
            super.switchToNextTab();
        } catch (IndexOutOfBoundsException error) {
            throw new AssertionError(format("Не открылась новая вкладка\n%s", error));
        }
    }

}
