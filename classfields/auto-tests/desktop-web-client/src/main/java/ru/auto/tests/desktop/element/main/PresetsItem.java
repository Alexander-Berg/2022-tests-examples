package ru.auto.tests.desktop.element.main;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface PresetsItem extends VertisElement, WithButton {

    @Name("Список фотографий")
    @FindBy(".//img[contains(@class, 'LazyImage__image')] | " +
            ".//img[contains(@class, 'OfferPanorama__previewLayer')]")
    ElementsCollection<VertisElement> imgList();

    @Name("Ссылка")
    @FindBy(".//a")
    VertisElement url();

    @Name("Сниженная цена")
    @FindBy(".//div[contains(@class, 'IndexPresets__offer-price-diff')]")
    VertisElement discountPrice();

    @Step("Получаем фото с индексом {i}")
    default VertisElement getImg(int i) {
        return imgList().should(hasSize(greaterThan(i))).get(i);
    }

}
