package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.element.main.HeatMapBlock;
import ru.yandex.realty.element.main.PremiumNewBuilding;
import ru.yandex.realty.element.main.SendAppSmsForm;
import ru.yandex.realty.element.saleads.WithApartmentFilters;

import java.util.List;

public interface MainPage extends WebPage, WithApartmentFilters, BasePage, Button, Link {

    @Name("Главный блок")
    @FindBy("//div[contains(@class,'WelcomeScreenWithPromo__wrapper')]")
    AtlasWebElement welcomeBlock();

    @Name("Фильтры главного блока")
    @FindBy("//div[contains(@class,'WelcomeScreenWithPromo__main')]")
    AtlasWebElement welcomeBlockFilters();

    @Name("Блок  «{{ value }}»")
    @FindBy("//*[contains(@class, 'Presets__section-main') and contains(.,'{{ value }}')]")
    Link mainBlock(@Param("value") String value);

    @Name("Блок «Новостройки»")
    @FindBy("//div[@class='Newbuildings']")
    Link mainBlockNewBuilding();

    @Name("Блок «Температурные карты»")
    @FindBy("//div[contains(@class, 'HeatmapsPromoBlock')]")
    HeatMapBlock heatMapsBlock();

    @Name("Блок «Получить ссылку на приложение по SMS»")
    @FindBy("//form[@class='SendAppSmsForm']")
    SendAppSmsForm sendAppSmsForm();

    @Name("Премиум новостройки")
    @FindBy("//div[contains(@class,'Newbuildings__item')]")
    ElementsCollection<PremiumNewBuilding> premiumNewBuilding();

    @Name("Премиум офферы")
    @FindBy("//div[contains(@class,'PremiumOffers__item')]")
    ElementsCollection<Link> premiumOffer();

    @Name("Картинка с alt текстом  «{{ value }}»")
    @FindBy("//a[.//img[@alt='{{ value }}']]")
    AtlasWebElement img(@Param("value") String value);

    @Name("Ранее вы искали")
    @FindBy("//div[contains(@class, 'LastSearch')]//a")
    AtlasWebElement lastSearch();

    @Name("Рекламный блок «RTB»")
    @FindBy("//div[contains(@class, 'Ad_rtb-rendered')]")
    AtlasWebElement rtbAdBlock();

    @Name("RTB на странице оффера")
    @FindBy("//div[contains(@class, 'rtb_align_center')]")
    List<RealtyElement> rtbAds();

    @Name("Рекламный блок С3")
    @FindBy("//div[@id='homepage-ad__rtb']")
    Link c3Banner();

    @Name("Рекламный блок R1")
    @FindBy("//div[@id='page-aside-ad__adfox']")
    AtlasWebElement r1Banner();
}
