package ru.auto.tests.desktop.page.forms;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithBillingModalPopup;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.element.forms.DealerVas;
import ru.auto.tests.desktop.element.forms.FoldedBlock;
import ru.auto.tests.desktop.element.forms.UnfoldedBlock;
import ru.auto.tests.desktop.element.forms.UserVas;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface FormsPage extends BasePage, WithBillingModalPopup, WithRadioButton {

    @Name("Форма")
    @FindBy("//div[contains(@class, 'FormPage')]")
    VertisElement form();

    @Name("Свёрнутый блок «{{ text }}»")
    @FindBy("//div[contains(@class, 'FormSection') and .//div[contains(@class, 'FormSection__Title') " +
            "and contains(., '{{ text }}')]]")
    FoldedBlock foldedBlock(@Param("text") String Text);

    @Name("Развёрнутый блок «{{ text }}»")
    @FindBy("//div[contains(@class, 'FormSection_opened') and .//div[contains(@class, 'FormSection__') " +
            "and .= '{{ text }}']] | " +
            "//div[contains(@class, 'MdsPhotosList__wrapper') and .//div[.= '{{ text }}']] | " +
            "//div[@class = 'RichContent' and .//div[.= '{{ text }}']] | " +
            "//div[@class = 'ReviewRatingsForms' and .//div[.= '{{ text }}']] | " +
            "//div[@class = 'ProsAndContras__col' and .//div[.= '{{ text }}']] | " +
            "//div[contains(@class, 'FormSection_opened') and .//*[.= '{{ text }}']] | " +
            "//div[@class = 'UserProfile' and .//*[.= '{{ text }}']]")
    UnfoldedBlock unfoldedBlock(@Param("text") String Text);

    @Name("Фото")
    @FindBy("//input[@type = 'file']")
    VertisElement photo();

    @Name("Кнопка удаления текста отзыва")
    @FindBy(".//button[contains(@class, 'ReviewEditor__removeButton')]")
    VertisElement removeTextButton();

    @Name("Инпут для фото в отзыве")
    @FindBy("//input[@class = 'PhotosAdd__fileInput']")
    VertisElement reviewPhotoInput();

    @Name("Фото в отзыве")
    @FindBy("//img[@class = 'image-gallery-image']")
    VertisElement reviewPhoto();

    @Name("Кнопка удаления фото в отзыве")
    @FindBy(".//button[contains(@class, 'ReviewEditorImageGallery__removeButton')]")
    VertisElement removePhotoButton();

    @Name("Кнопка добавления видео")
    @FindBy(".//*[contains(@class, 'IconSvg_review-video')]")
    VertisElement addVideoButton();

    @Name("Кнопка «Текст»")
    @FindBy(".//*[contains(@class, 'IconSvg_review-text')]")
    VertisElement textButton();

    @Name("Кнопка «Подзаголовок»")
    @FindBy(".//*[contains(@class, 'IconSvg_review-paragraph')]")
    VertisElement paragraphButton();

    @Name("Кнопка добавления фото")
    @FindBy(".//div[contains(@class, 'ReviewEditor__photosAdd')]")
    VertisElement addPhotoButton();

    @Name("Текст в тултипе с подсказкой")
    @FindBy(".//div[contains(@class, 'HoveredTooltip__text')]")
    VertisElement tooltipText();

    @Name("Ошибка добавления видео")
    @FindBy(".//div[contains(@class, '__addVideoError')]")
    VertisElement addVideoError();

    @Name("Контейнер с добавленным видео")
    @FindBy(".//div[contains(@class, 'ReviewEditor__video')]")
    VertisElement addedVideo();

    @Name("Список загруженных фотографий")
    @FindBy("//div[contains(@class, 'MdsPhotosList__photo')]")
    ElementsCollection<VertisElement> photosList();

    @Name("Образец фото")
    @FindBy("//div[@class = 'Wysiwyg__photo']/img")
    VertisElement photoExample();

    @Name("Блок услуг для частника")
    @FindBy(".//div[@class = 'UserVas'] | " +
            ".//div[contains(@class, 'VasFormUser')]")
    UserVas userVas();

    @Name("Блок услуг для дилера")
    @FindBy(".//div[contains(@class, 'VasFormDealer')]")
    DealerVas dealerVas();

    @Name("Сообщение об ошибке при сохранении формы")
    @FindBy(".//div[contains(@class, 'Submit__formError')]")
    VertisElement submitErrorMessage();

    @Name("Кнопка «Разместить»")
    @FindBy(".//div[@class = 'Submit'] | " +
            ".//button[.= 'Опубликовать'] | " +
            ".//div[contains(@class, 'EvaluationFormSubmit')]/button | " +
            ".//button[contains(@class, 'VasFormUserFooter__button')] | " +
            ".//button[contains(@class, 'VasFormDealerSubmit__button')] | " +
            ".//div[contains(@class, 'VasFormUserSnippet ')][3]//button[contains(@class, 'VasFormUserSnippet__button')]")
    VertisElement submitButton();

    @Name("Сайдбар")
    @FindBy("//div[contains(@class, '__sidebar')]")
    VertisElement sidebar();

    @Name("Таймер скидки")
    @FindBy("//div[@class = 'VasFormUserTimer']")
    VertisElement discountTimer();

    @Name("Нотификация")
    @FindBy("//div[contains(@class, 'VasFormUserNotification_visible')]")
    VertisElement notification();

    @Step("Получаем фото с индексом {i}")
    default VertisElement getPhoto(int i) {
        return photosList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Статус сохранения черновика")
    @FindBy("//div[@class = 'ReviewEditor__saveStatus']")
    VertisElement saveStatus();
}
