package ru.yandex.realty.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matcher;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.yandex.realty.page.AlfabankLandingPage;
import ru.yandex.realty.page.ArchivePage;
import ru.yandex.realty.page.BasePage;
import ru.yandex.realty.page.CommercialPage;
import ru.yandex.realty.page.ComparisonPage;
import ru.yandex.realty.page.DeveloperPage;
import ru.yandex.realty.page.DocumentsPage;
import ru.yandex.realty.page.FavoritesPage;
import ru.yandex.realty.page.IpotekaCalculatorPage;
import ru.yandex.realty.page.IpotekaPage;
import ru.yandex.realty.page.JournalPage;
import ru.yandex.realty.page.MainPage;
import ru.yandex.realty.page.ManagementNewPage;
import ru.yandex.realty.page.MapPage;
import ru.yandex.realty.page.MortgageProgramCardPage;
import ru.yandex.realty.page.NewBuildingPage;
import ru.yandex.realty.page.NewBuildingSitePage;
import ru.yandex.realty.page.NewBuildingSpecSitePage;
import ru.yandex.realty.page.OfferCardPage;
import ru.yandex.realty.page.OffersSearchPage;
import ru.yandex.realty.page.PassportLoginPage;
import ru.yandex.realty.page.ProfSearchPage;
import ru.yandex.realty.page.RailwaysPage;
import ru.yandex.realty.page.SamoletPage;
import ru.yandex.realty.page.VillageListing;
import ru.yandex.realty.page.VillageSitePage;

import java.net.URI;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;

/**
 * Created by vicdev on 21.04.17.
 */
public class BasePageSteps extends CommonSteps {

    //кука может быть любой из этих
    public static final List<String> POSSIBLE_ADBLOCK_COOKIES = asList(
            "bltsr",
            "qgZTpupNMGJBM",
            "mcBaGDt",
            "BgeeyNoBJuyII",
            "orrXTfJaS",
            "FgkKdCjPqoMFm",
            "EIXtkCTlX",
            "JPIqApiY",
            "KIykI",
            "HgGedof",
            "ancQTZw",
            "involved",
            "instruction",
            "engineering",
            "telecommunications",
            "discussion",
            "computer",
            "substantial",
            "specific",
            "engineer",
            "adequate",
            "Silver",
            "Mercury",
            "Bismuth",
            "Silicon",
            "Tennessine",
            "Zinc",
            "Sulfur",
            "Nickel",
            "Radon",
            "Manganese",
            "LBCBNrZSu",
            "VTouhmwR",
            "TbwgcPzRMgzVo",
            "liPkbtFdIkYqc",
            "HOhdORSx",
            "EMCzniGaQ",
            "PIwsfZeu",
            "FxuGQqNNo",
            "sMLIIeQQeFnYt",
            "pClnKCSBXcHUp",
            "tCTmkfFoXn",
            "zmFQeXtI",
            "ScSvCIlBC",
            "kNAcVGYFWhx",
            "jsOmqPGh",
            "OqYspIFcUpLY",
            "XcfPaDInQpzKj",
            "hcxWnzbUzfz",
            "MGphYZof",
            "NBgfDVFir");

    @Inject
    public UrlSteps urlSteps;

    public WebDriver getDriver() {
        return super.getDriver();
    }

    public BasePage onBasePage() {
        return on(BasePage.class);
    }

    public ComparisonPage onComparisonPage() {
        return on(ComparisonPage.class);
    }

    public FavoritesPage onFavoritesPage() {
        return on(FavoritesPage.class);
    }

    public MainPage onMainPage() {
        return on(MainPage.class);
    }

    public MapPage onMapPage() {
        return on(MapPage.class);
    }

    public NewBuildingPage onNewBuildingPage() {
        return on(NewBuildingPage.class);
    }

    public NewBuildingSitePage onNewBuildingSitePage() {
        return on(NewBuildingSitePage.class);
    }

    public NewBuildingSpecSitePage onNewBuildingSpecSitePage() {
        return on(NewBuildingSpecSitePage.class);
    }

    public VillageListing onVillageListing() {
        return on(VillageListing.class);
    }

    public VillageSitePage onVillageSitePage() {
        return on(VillageSitePage.class);
    }

    public OfferCardPage onOfferCardPage() {
        return on(OfferCardPage.class);
    }

    public SamoletPage onSamoletPage() {
        return on(SamoletPage.class);
    }

    public OffersSearchPage onOffersSearchPage() {
        return on(OffersSearchPage.class);
    }

    public CommercialPage onCommercialPage() {
        return on(CommercialPage.class);
    }

    public ManagementNewPage onManagementNewPage() {
        return on(ManagementNewPage.class);
    }

    public ArchivePage onArchivePage() {
        return on(ArchivePage.class);
    }

    public IpotekaCalculatorPage onIpotekaCalculatorPage() {
        return on(IpotekaCalculatorPage.class);
    }

    public MortgageProgramCardPage onMortgageProgramCardPage() {
        return on(MortgageProgramCardPage.class);
    }

    public IpotekaPage onIpotekaPage() {
        return on(IpotekaPage.class);
    }

    public AlfabankLandingPage onAlfabankLandingPage() {
        return on(AlfabankLandingPage.class);
    }

    public ProfSearchPage onProfSearchPage() {
        return on(ProfSearchPage.class);
    }

    public DeveloperPage onDeveloperPage() {
        return on(DeveloperPage.class);
    }

    public PassportLoginPage onPassportLoginPage() {
        return on(PassportLoginPage.class);
    }

    public JournalPage onJournalPage() {
        return on(JournalPage.class);
    }

    public DocumentsPage onDocumentsPage() {
        return on(DocumentsPage.class);
    }

    public RailwaysPage onRailwaysPage() {
        return on(RailwaysPage.class);
    }

    @Step("Извлекаем id из аттрибута href")
    public String getOfferId(AtlasWebElement element) {
        return removeEnd(removeStart(URI.create(getHrefLink(element)).getPath(), "/offer/"), "/");
    }

    @Step("Два элемента должны совпадать")
    public <T> void shouldEqual(String reason, T first, T second) {
        assertThat(reason, first, equalTo(second));
    }

    @Step("Первый список должен содержать второй")
    public <T> void shouldMatchLists(List<T> firstList, List<T> secondList) {
        assertThat("Размер списков не совпадает", firstList, hasSize(secondList.size()));
        assertThat(firstList, containsInAnyOrder(secondList.toArray()));
    }

    @Step("Создаём одно событие в истории")
    public String addSearchHistoryItem(int priceMin) {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA)
                .queryParam("priceMin", Long.toString(priceMin)).open();
        return urlSteps.toString();
    }

    @Step("Ждем пока лоадер загрузится до конца")
    public void loaderWait() {
        onBasePage().loader().waitUntil(hasClass(not(containsString("_running"))), urlSteps.getUrlTimeout());
    }

    @Step("Двигаем элемент {element} по горизонтали на {xOffset} по вертикали на {yOffset}")
    public void moveSlider(WebElement element, int xOffset, int yOffset) {
        new Actions(getDriver()).dragAndDropBy(element, xOffset, yOffset).build().perform();
    }

    @Step("Двигаем элемент {element} на {toElement}")
    public void moveToElement(WebElement element, WebElement toElement) {
        new Actions(getDriver()).clickAndHold(element).build().perform();
        new Actions(getDriver()).moveToElement(toElement).build().perform();
        new Actions(getDriver()).release().build().perform();
    }

    @Step("Получаем куку для адблока")
    public void findAdblockCookie() {
        switchToTab(1);
        refresh();
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(1, SECONDS).atMost(20, SECONDS).ignoreExceptions()
                .alias("должна быть хотя бы одна кука из списка")
                .untilAsserted(() -> Assertions.assertThat(getDriver().manage().getCookies().stream()
                        .map(c -> c.getName()).filter(name -> POSSIBLE_ADBLOCK_COOKIES.contains(name))
                        .findAny().get()).isNotNull());
    }

    @Step("Убираем куку для перехода в престэйбл")
    public void removePrestableCookie() {
        clearCookie("prestable");
    }

    @Step("Прокручиваем до появления ссылки")
    public void swipeUntilSeeLink(String link) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await().ignoreExceptions()
                .pollInterval(5, MILLISECONDS).atMost(10, SECONDS).then()
                .until(() -> {
                    onMainPage().heatMapsBlock().swipeForward().should(isDisplayed()).click();
                    onMainPage().heatMapsBlock().link(link).should("Ссылка не отображена", isDisplayed(), 1);
                    return true;
                });
    }

    @Step("Кликаем на {banner}, проверяем что открывается вкладка и есть урл")
    public void shouldSeeNotNullClick(AtlasWebElement banner) {
        banner.waitUntil("Должен отобразиться баннер", isDisplayed(), 20);
        moveCursorAndClick(banner);
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(1, SECONDS).atMost(10, SECONDS)
                .alias("Должно быть 2 вкладки")
                .until(() -> getDriver().getWindowHandles().size(), equalTo(2));
        switchToNextTab();
        Assertions.assertThat(getDriver().getCurrentUrl()).describedAs("Кликнули на баннер. Текущий урл должен быть")
                .isNotNull().isNotEmpty();
    }

    //Бывает что карта неотображает первый оффер
    @Step("Отдаляем карту и жмем на оффер на карте")
    public void clickMapOfferAndShowSnippetOffers() {
        onMapPage().unzoom().click();
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(1, SECONDS).atMost(30, SECONDS).ignoreExceptions()
                .until(() -> {
                    moveCursorAndClick(onMapPage().mapOffer(FIRST));
                    onMapPage().sidebar().mapOfferList()
                            .waitUntil("Список офферов появился", hasSize(greaterThan(0)), 3);
                    return true;
                });
    }

    public static Matcher urlParam(String param, String value) {
        return containsString(format("%s=%s", param, value));
    }

    @Step("Выбираем из выплывающего хедера {from} -> {to}")
    public void chooseFromHeader(String from, String to) {
        moveCursor(onBasePage().headerUnder().link(from));
        onBasePage().headerUnder().expandedMenu().link(to).click();
    }
}
