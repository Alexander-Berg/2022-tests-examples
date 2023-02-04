package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.mobile.element.CardDev;
import ru.yandex.realty.mobile.element.CardSectionExpandable;
import ru.yandex.realty.mobile.element.MobilePopup;
import ru.yandex.realty.mobile.element.newbuilding.CallbackPopup;
import ru.yandex.realty.mobile.element.newbuilding.CardStickyActions;
import ru.yandex.realty.mobile.element.newbuilding.FeaturesBlock;
import ru.yandex.realty.mobile.element.village.FromDevVillage;
import ru.yandex.realty.mobile.element.village.VillageGallery;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface VillageCardPage extends BasePage {

    @Name("Избранное в хедере")
    @FindBy("//div[contains(@class,'VillageCardNavBar__favor')]")
    AtlasWebElement headerFavIcon();

    @Name("Офферы коттеджного поселка")
    @FindBy("//div[@class='VillageCardOffers__list']/a")
    ElementsCollection<AtlasWebElement> villageOffers();

    @Name("Блок списка особенностей")
    @FindBy("//div[contains(@class,'VillageCardDescription__features')]")
    FeaturesBlock featuresBlock();

    @Name("Список объектов от застройщика")
    @FindBy("//li[contains(@class,'SerpListItem_type_offer')]")
    ElementsCollection<FromDevVillage> fromDevVillages();

    @Name("«Показать ещё» от застройщика")
    @FindBy("//div[contains(@class,'VillageCardObjectsFromDeveloper')]//a[contains(@class,'show-more')]")
    AtlasWebElement showMoreFromDev();

    @Name("Блок застройщика")
    @FindBy("//div[@class='VillageCardDev']")
    CardDev villageDev();

    @Name("Попап «Позвоните мне»")
    @FindBy("//div[contains(@class,'Modal_visible BackCall__modal')]")
    CallbackPopup callbackPopup();

    @Name("Попап")
    @FindBy("//div[contains(@class,'Modal_visible') or contains (@class,'Popup_visible')]")
    MobilePopup popupVisible();

    @Name("Плавающий блок «Позвонить»")
    @FindBy("//div[@class='CardPhone']")
    CardStickyActions stickyActions();

    @Name("Фотографии")
    @FindBy("//div[@class='SwipeGallery__thumb']//img")
    ElementsCollection<AtlasWebElement> photo();

    @Name("Галерея")
    @FindBy("//div[@class='SwipeableFSGallery']")
    VillageGallery gallery();

    @Name("Шорткат {{ value }}")
    @FindBy(".//nav[@class='CardShortcuts']//li[contains(.,'{{ value }}')]")
    AtlasWebElement shortcut(@Param("value") String value);

    @Name("Шорткат на тепловой карте {{ value }}")
    @FindBy("//div[contains(@class,'Modal_visible')]//nav[contains(@class,'CardBrowser__shortcuts')]//li[contains(.,'{{ value }}')]")
    AtlasWebElement heatMapShortcut(@Param("value") String value);

    @Name("Секция {{ value }}")
    @FindBy("//div[contains(@class,'CardSection_expandable') and contains(.,'{{ value }}')]")
    CardSectionExpandable cardSection(@Param("value") String value);

    default FromDevVillage cardDev(int i) {
        return fromDevVillages().waitUntil(hasSize(greaterThan(i))).get(i);
    }
}
