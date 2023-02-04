package ru.yandex.realty.element.offercard;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface PhoneBlock extends Link {

    String TEL_HREF_PATTERN = "tel:%s";
    String MESSAGE_500 = "Ошибка";

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//button[contains(@class, 'Phone')]")
    AtlasWebElement showPhoneButton();

    @Name("Показанный телефон")
    @FindBy(".//div[contains(@class,'OfferPhoneButton__button_visible_always')]")
    AtlasWebElement revealedPhone();

    @Name("Шильдик «Телефон защищен»")
    @FindBy(".//div[contains(@class,'_redirectIndicator')]")
    AtlasWebElement phoneProtected();

    @Name("Шильдик «Телефон защищен»")
    @FindBy(".//div[contains(@class,'PhoneButton__redirect')]")
    AtlasWebElement phoneProtectedOnRelated();

    @Name("Телефоны")
    @FindBy(".//a[contains(@class,'_phone')]")
    ElementsCollection<AtlasWebElement> phones();

    default AtlasWebElement phone() {
        return phones().waitUntil(hasSize(greaterThan(0))).get(0);
    }
}
