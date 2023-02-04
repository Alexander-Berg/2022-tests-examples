package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.village.CardPhoneVillage;
import ru.yandex.realty.element.village.VillageSerpItem;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface VillageSitePage extends BasePage, Button {

    String SHOW_PHONE = "Показать телефон";

    @Name("Карточка информации")
    @FindBy("//div[@class = 'VillageCardAbout']")
    CardPhoneVillage villageCardAbout();

    @Name("Плавающая карточка")
    @FindBy("//div[contains(@class,'VillageCard__content_contacts-up')]//div[@class = 'VillageCardInfo']")
    CardPhoneVillage hideableBlock();

    @Name("Информация о застройщике")
    @FindBy("//div[@class = 'CardDev']")
    CardPhoneVillage cardDev();

    @Name("Картинка галереи")
    @FindBy("//div[contains(@class,'GallerySlidePic')]")
    AtlasWebElement galleryPic();

    @Name("Объекты от застройщика")
    @FindBy("//div[contains(@class,'CardDevVillages__snippet')]")
    ElementsCollection<VillageSerpItem> fromDevList();

    @Name("Блок информации сбоку в галерее")
    @FindBy("//div[contains(@class,'VillageCardGallery__aside')]")
    CardPhoneVillage galleryAside();

    @Name("Закрыть галерею")
    @FindBy(".//button[contains(@class,'FSGalleryClose')]")
    AtlasWebElement closeGallery();

    @Name("Кнопка «Показать телефон» в галерее")
    @FindBy("//div[@class='VillageCardGallery__aside']//button[contains(.,'Показать телефон')]")
    AtlasWebElement showPhoneButtonGallery();

    default VillageSerpItem fromDev(int i) {
        return fromDevList().waitUntil(hasSize(greaterThan(i))).get(i);
    }
}
