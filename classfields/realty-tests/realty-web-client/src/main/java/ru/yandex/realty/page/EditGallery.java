package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface EditGallery extends AtlasWebElement {

    @Name("Список карточек фотографий")
    @FindBy(".//div[contains(@class,'attach-card_can-drop')]")
    ElementsCollection<AttachCard> attachCards();

    default AttachCard attachCard(int i) {
        return attachCards().should(hasSize(greaterThan(i))).get(i);
    }

    default void deletePhoto(int i) {
        int initialSize = attachCards().waitUntil(hasSize(greaterThan(i))).size();
        attachCard(i).hover();
        attachCard(i).buttonWithTitle("Удалить").waitUntil(WebElementMatchers.isDisplayed(), 20).click();
        attachCards().waitUntil("Должен уменьшится на 1", hasSize(initialSize - 1));
    }

    default void deleteAllPhotos() {
        int initialSize = attachCards().size();
        for (int i = 0; i < initialSize; i++) {
            deletePhoto(0);
        }
    }
}
