package ru.yandex.realty.mobile.step;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import ru.yandex.realty.mobile.page.AmpSaleAdsPage;
import ru.yandex.realty.mobile.page.ArchivePage;
import ru.yandex.realty.mobile.page.BasePage;
import ru.yandex.realty.mobile.page.DeveloperPage;
import ru.yandex.realty.mobile.page.DocumentsPage;
import ru.yandex.realty.mobile.page.EgrnReportPage;
import ru.yandex.realty.mobile.page.FavoritesPage;
import ru.yandex.realty.mobile.page.IpotekaCalculatorPage;
import ru.yandex.realty.mobile.page.IpotekaPage;
import ru.yandex.realty.mobile.page.MainPage;
import ru.yandex.realty.mobile.page.MapPage;
import ru.yandex.realty.mobile.page.MortgageTouchPage;
import ru.yandex.realty.mobile.page.NewBuildingCardPage;
import ru.yandex.realty.mobile.page.NewBuildingPage;
import ru.yandex.realty.mobile.page.OfferCardPage;
import ru.yandex.realty.mobile.page.SaleAdsPage;
import ru.yandex.realty.mobile.page.SpecProjectPage;
import ru.yandex.realty.mobile.page.VillageCardPage;
import ru.yandex.realty.mobile.page.VillageOffersPage;
import ru.yandex.realty.page.JournalPage;
import ru.yandex.realty.page.PassportLoginPage;
import ru.yandex.realty.step.CommonSteps;

import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;

public class BasePageSteps extends CommonSteps {

    public WebDriver getDriver() {
        return super.getDriver();
    }

    public BasePage onBasePage() {
        return on(BasePage.class);
    }

    public MainPage onMobileMainPage() {
        return on(MainPage.class);
    }

    public SaleAdsPage onMobileSaleAdsPage() {
        return on(SaleAdsPage.class);
    }

    public AmpSaleAdsPage onAmpSaleAdsPage() {
        return on(AmpSaleAdsPage.class);
    }

    public DocumentsPage onDocumentsPage() {
        return on(DocumentsPage.class);
    }

    public MapPage onMobileMapPage() {
        return on(MapPage.class);
    }

    public SpecProjectPage onSpecProjectPage() {
        return on(SpecProjectPage.class);
    }

    public ArchivePage onArchivePage() {
        return on(ArchivePage.class);
    }

    public IpotekaCalculatorPage onIpotekaCalculatorPage() {
        return on(IpotekaCalculatorPage.class);
    }

    public IpotekaPage onIpotekaPage() {
        return on(IpotekaPage.class);
    }

    public OfferCardPage onOfferCardPage() {
        return on(OfferCardPage.class);
    }

    public NewBuildingCardPage onNewBuildingCardPage() {
        return on(NewBuildingCardPage.class);
    }

    public NewBuildingPage onNewBuildingPage() {
        return on(NewBuildingPage.class);
    }

    public VillageCardPage onVillageCardPage() {
        return on(VillageCardPage.class);
    }

    public VillageOffersPage onVillageOffersPage() {
        return on(VillageOffersPage.class);
    }

    public MortgageTouchPage onMortgagePage() {
        return on(MortgageTouchPage.class);
    }

    public PassportLoginPage onPassportLoginPage() {
        return on(PassportLoginPage.class);
    }

    public FavoritesPage onFavoritesPage() {
        return on(FavoritesPage.class);
    }

    public DeveloperPage onDeveloperPage() {
        return on(DeveloperPage.class);
    }

    public EgrnReportPage onEgrnReportPage() {
        return on(EgrnReportPage.class);
    }

    public JournalPage onJournalPage() {
        return on(JournalPage.class);
    }

    @Step("Выбираем значение селектора = «{option}»")
    public void selectOption(AtlasWebElement selector, String option) {
        new Select(selector).selectByVisibleText(option);
    }

    @Step("Выбираем {element}")
    public void selectExtFilterElement(String element) {
        scrollToElement(onMobileMainPage().extendFilters().button(element));
        onMobileMainPage().extendFilters().button(element).click();
        onMobileMainPage().extendFilters().button(element).waitUntil(isChecked());
    }
}
