package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;

public interface AuthorOfferPage extends BasePage {

    String ADD_VAS_FOR = "Подключить за";
    String ADD_VAS_FREE = "Подключить бесплатно";
    String RISE_FOR = "Поднять за";
    String PREMIUM = "Премиум на";
    String PROMOTION = "Продвижение на";
    String RAISING = "Поднятие";

    @Name("Вас «{{ value }}»")
    @FindBy("//div[contains(@class,'OfferVasServices__service')][contains(.,'{{ value }}')]")
    Button vas(@Param("value") String value);

    @Name("Промоблок егрн-проверки")
    @FindBy(".//div[contains(@class,'OfferDescription__egrn-promo')]")
    AtlasWebElement egrnPromo();

    @Name("Телефоны")
    @FindBy(".//span[contains(@class,'OfferCardOwnerPhone__phone')]")
    AtlasWebElement phones();
}
