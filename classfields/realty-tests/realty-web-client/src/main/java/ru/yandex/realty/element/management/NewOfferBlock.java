package ru.yandex.realty.element.management;


import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.RealtyElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface NewOfferBlock extends Button {

    int SHOWS = 0;
    int PHONES = 1;

    @Name("Сообщение оффера")
    @FindBy(".//div[contains(@class, 'MessagePanel__wrap')]")
    AtlasWebElement offerMessage();

    @Name("Панель управления оффером")
    @FindBy(".//div[contains(@class, 'owner-offer-preview-info-panel__controls')]")
    OfferControlPanel controlPanel();

    @Name("Инфо")
    @FindBy(".//div[contains(@class, 'owner-offer-preview-info-panel__wrapper')]")
    OfferInfo offerInfo();

    @Name("Панель сервисов")
    @FindBy(".//div[@class='VasPanel__services']")
    OfferServicesPanel servicesPanel();

    @Name("Галерея фото")
    @FindBy(".//div[@class='gallery']")
    PhotoGallery photoGallery();

    @Name("Список статистик")
    @FindBy(".//div[contains(@class,'OwnerOfferPreviewStatFunnel__item')]")
    ElementsCollection<OfferStat> statsList();

    @Name("Линк на продление")
    @FindBy(".//label[contains(.,'Автопродление')]")
    RealtyElement renewalLink();

    @Name("Информация о статусе")
    @FindBy(".//div[contains(@class,'OwnerOfferPreviewStat__summaryPrimary')]")
    AtlasWebElement statusInfo();

    @Name("Бейдж «{{ value }}»")
    @FindBy(".//span[contains(@class,'OfferBadge__content')][contains(.,'{{ value }}')]")
    AtlasWebElement badge(@Param("value") String value);

    default OfferStat stat(int i) {
        return statsList().should(hasSize(greaterThan(i))).get(i);
    }
}
