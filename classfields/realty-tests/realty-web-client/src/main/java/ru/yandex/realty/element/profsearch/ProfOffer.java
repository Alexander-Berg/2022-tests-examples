package ru.yandex.realty.element.profsearch;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.RealtyElement;

public interface ProfOffer extends Link {

    @Name("Частичный номер телефона")
    @FindBy(".//button[contains(@class,'profsearch-offer-owner__phone')]")
    AtlasWebElement showPhoneButton();

    @Name("Ссылка оффера")
    @FindBy(".//a[contains(@class, 'profsearch-offer-name__name')]")
    AtlasWebElement offerLink();

    @Name("Чекбокс оффера")
    @FindBy(".//label[contains(@class, 'Checkbox')]")
    RealtyElement offerCheckbox();

    @Name("Показать/скрыть похожие для оффера")
    @FindBy(".//button[@class='profsearch-cluster__toggle hide-from-print']")
    RealtyElement showExtra();

    default String convertToProdLink() {
        String link = offerLink().getAttribute("href");
        link = link.substring(0, link.length() - 1).replace(".test.vertis", "");
        link = link.replaceFirst("branch-realtyfront-\\d*\\.", "");
        return link;
    }
}
