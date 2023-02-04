package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.lk.ownerlk.PhotosPreview;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface LkOwnerFlatPhotoPage extends BasePage {

    String SEND_BUTTON = "Отправить";

    @Name("Кнопка добавить фото")
    @FindBy(".//div[contains(@class,'ImageUploaderAddImageButton__addBtn')]")
    AtlasWebElement addPhotoButton();

    @Name("Инпут фото")
    @FindBy(".//input[contains(@type,'file')]")
    AtlasWebElement photoInput();

    @Name("Превьюшки фото")
    @FindBy(".//div[contains(@class,'ImageUploaderImagePreview__wrapper')]")
    ElementsCollection<PhotosPreview> photosPreviews();

    default PhotosPreview photosPreview(int i) {
        return photosPreviews().should(hasSize(greaterThan(i))).get(i);
    }

    default void deletePhoto(int i) {
        int initialSize = photosPreviews().waitUntil(hasSize(greaterThan(i))).size();
        photosPreview(i).hover();
        photosPreview(i).deleteButton().waitUntil(WebElementMatchers.isDisplayed(), 10).click();
        photosPreviews().waitUntil("Должен уменьшится на 1", hasSize(initialSize - 1));
    }

    default void deleteAllPhotos() {
        int initialSize = photosPreviews().size();
        for (int i = 0; i < initialSize; i++) {
            deletePhoto(0);
        }
    }
}
