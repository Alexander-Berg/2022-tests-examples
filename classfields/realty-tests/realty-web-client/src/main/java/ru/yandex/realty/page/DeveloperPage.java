package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.developer.DiscountTab;
import ru.yandex.realty.element.developer.Map;
import ru.yandex.realty.element.developer.Slider;
import ru.yandex.realty.element.newbuildingsite.DevSnippet;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface DeveloperPage extends BasePage {

    String NEWBUILDINGS = "Новостройки";
    String OFFICES = "Офисы продаж";
    String SITE = "site";
    String OFFICE = "office";

    @Name("Кнопка «Позвонить»")
    @FindBy("//button[contains(@class, 'phoneButton')]")
    AtlasWebElement callButton();

    @Name("Слайдер")
    @FindBy("//div[contains(@class, 'Card__slider')]")
    Slider slider();

    @Name("Табы «Скидки и акции»")
    @FindBy("//div[contains(@class, 'Discounts__tab')]")
    ElementsCollection<DiscountTab> discountTabs();

    @Name("Карта")
    @FindBy("//div[@class = 'Map__container']/ymaps")
    Map map();

    @Name("Сниппет офиса")
    @FindBy("//div[@class = 'OfficeSnippetPin']")
    Button officeSnippet();

    @Name("Сниппет новостройки")
    @FindBy("//div[@class = 'SiteSnippetPin']")
    VertisElement siteSnippet();

    @Name("Буллет карточек")
    @FindBy("//div[contains(@class,'DeveloperCardSlider__bullet') and not(contains(@class,'bullets'))][1]")
    AtlasWebElement firstBullet();

    @Name("Сниппет новостройки")
    @FindBy(".//div[contains(@class,'DeveloperCardTopSites') and contains(@class, 'Snippet')]")
    ElementsCollection<DevSnippet> newbuildingSnippets();

    default DevSnippet newBuildingSnippet(int i) {
        return newbuildingSnippets().waitUntil(hasSize(greaterThan(i))).get(i);
    }

}
