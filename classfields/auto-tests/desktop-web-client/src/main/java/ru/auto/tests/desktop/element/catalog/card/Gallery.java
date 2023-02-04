package ru.auto.tests.desktop.element.catalog.card;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Gallery extends VertisElement {

    @Name("Активное фото")
    @FindBy(".//a[contains(@class, 'link_active')]//div[contains(@class, 'photo-gallery__photo')]")
    VertisElement activePhoto();

    @Name("Панорама")
    @FindBy(".//a[contains(@class, 'photo-gallery__item_view_panorama')]")
    VertisElement panorama();

    @Name("Список превью")
    @FindBy(".//a[contains(@class,'photo-gallery__item')]")
    ElementsCollection<VertisElement> thumbList();

    @Step("Получаем превью с индексом {i}")
    default VertisElement getThumb(int i) {
        return thumbList().should(hasSize(greaterThan(i))).get(i);
    }
}
