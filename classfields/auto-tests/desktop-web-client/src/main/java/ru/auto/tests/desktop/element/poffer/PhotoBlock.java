package ru.auto.tests.desktop.element.poffer;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface PhotoBlock extends Block {

    @Name("Фото")
    @FindBy(".//input[@type = 'file']")
    VertisElement photo();

    @Name("Список загруженных фотографий")
    @FindBy("//div[contains(@class, 'photo-item ')]")
    ElementsCollection<Photo> photosList();

    @Step("Получаем фото с индексом {i}")
    default Photo getPhoto(int i) {
        return photosList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Фотографии для Авито")
    @FindBy(".//div[contains(@class, 'photos-list-classified_classified_avito')]")
    PhotoBlock avitoPhotos();

    @Name("Фотографии для Дрома")
    @FindBy(".//div[contains(@class, 'photos-list-classified_classified_drom')]")
    PhotoBlock dromPhotos();

}
