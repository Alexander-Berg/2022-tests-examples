package ru.yandex.general.mobile.page;


import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.SuspiciousActivityContacts;
import ru.yandex.general.mobile.element.Attribute;
import ru.yandex.general.mobile.element.Button;
import ru.yandex.general.mobile.element.Checkbox;
import ru.yandex.general.mobile.element.DraftSection;
import ru.yandex.general.mobile.element.FormAddressControl;
import ru.yandex.general.mobile.element.Input;
import ru.yandex.general.mobile.element.Link;
import ru.yandex.general.mobile.element.PhotoThumb;
import ru.yandex.general.mobile.element.Screen;
import ru.yandex.general.mobile.element.Switcher;
import ru.yandex.general.mobile.element.Wrapper;

public interface FormPage extends BasePage, Link, Input, Button, Switcher, Checkbox {

    String ADRES = "Адрес";
    String DELIVERY = "Доставка";
    String NEXT = "Дальше";
    String SKIP = "Пропустить";
    String COMPLETE = "Готово";
    String CONTINUE = "Продолжить";
    String START_NEW = "Начать сначала";
    String NEW_PRODUCT = "Новый товар";
    String USED = "Уже использовался";
    String ENTER_NAME = "Введите название";
    String YOUTUBE_LINK = "Ссылка на YouTube";
    String MY_PRICE = "Моя цена, ₽";
    String RUB = "₽";
    String IN_MONTH = "В месяц, ₽";
    String GIVE_FREE = "Отдам даром";
    String ADDRESS_INPUT = "Улица, метро, район";
    String GOROD_INPUT = "Город";
    String PUBLISH = "Опубликовать";
    String ADD_MORE_ADDRESS = "Добавить ещё адрес";
    String WE_READY_PUBLISH = "Мы готовы! Публикуем?";
    String THANKS = "Ого, спасибо!";
    String VESCHI = "Вещи";
    String NAZVANIE = "Название";
    String PHOTOS = "Фотографии";
    String CATEGORY = "Категория";
    String OPISANIE = "Описание";
    String VIDEO = "Видео";
    String SOSTOYANIE = "Состояние";
    String HARAKTERISTIKI = "Характеристики";
    String CENA = "Цена";
    String CENA_R = "Цена, ₽";
    String CONTACTS = "Контакты";
    String ZARPLATA = "Зарплата";
    String HOW_TO = "Как правильно заполнить";
    String NO_SUITABLE = "Нет подходящей";
    String CHANGE_CATEGORY = "Сменить категорию";
    String SAVE = "Сохранить";
    String MOVE_TO_CATEGORY = "Перенести в эту категорию";
    String CHANGE = "Меняем!";
    String SEND_BY_TAXI = "Отправлю на такси или курьером";
    String SEND_RUSSIA = "Отправлю по России";
    String FORM_PAGE_H1 = "Выберите категорию";
    String SAVE_DRAFT = "Сохранить черновик";
    String RESET = "Сбросить";
    String ONLY_CALLS = "Только звонки";
    String CALLS_AND_MESSAGES = "Звонки и сообщения";

    @Name("Название раздела")
    @FindBy("//div[contains(@class, 'FormScreen__header')]//span")
    VertisElement screenTitle();

    @Name("Экран «{{ value }}»")
    @FindBy("//div[contains(@class, 'PageScreen__container')][.//span[contains(@class, 'PageScreen__title')]" +
            "[contains(., '{{ value }}')]]")
    Screen screen(@Param("value") String value);

    @Name("Список фото")
    @FindBy("//div[contains(@class,'FormPhotosControlSortableList__thumbContainer')][.//img[not(contains(@class, 'thumb_pending'))]]")
    ElementsCollection<PhotoThumb> photoList();

    @Name("Элемент-враппер «{{ value }}»")
    @FindBy("//div[contains(@class, 'Screen__wrapper')][contains(.,'{{ value }}')]")
    Wrapper wrapper(@Param("value") String value);

    @Name("Раздел «{{ value }}»")
    @FindBy("//*[contains(@class, 'PresetButton__button')][contains(., '{{ value }}')]")
    VertisElement section(@Param("value") String value);

    @Name("Ссылка на раздел «{{ value }}»")
    @FindBy("//a[contains(@class, 'PresetButton__button')][contains(., '{{ value }}')]")
    VertisElement sectionLink(@Param("value") String value);

    @Name("Блок выбора категории")
    @FindBy("//div[contains(@class, 'FormCategorySelect')]")
    Link categorySelect();

    @Name("Выбранная категория")
    @FindBy("//div[contains(@class, 'CategorySelectRadio__radio_')][.//label[contains(@class, '_labelChecked')]]")
    Link checkedCategory();

    @Name("Список категорий")
    @FindBy("//div[contains(@class, 'CategorySelectRadio__radio_')]/label")
    ElementsCollection<VertisElement> categories();

    @Name("Атрибут «{{ value }}»")
    @FindBy("//div[contains(@class, 'attributeControl')][.//span[contains(@class, '_floatPlaceholder') " +
            "or contains(@class, 'attributeControlLabel')][contains(., '{{ value }}')]]")
    Attribute attribute(@Param("value") String value);

    @Name("Раздел «{{ value }}» на черновике")
    @FindBy("//div[contains(@class, 'Field__container')][./span[contains(@class, 'label')][contains(., '{{ value }}')]]")
    DraftSection draftSection(@Param("value") String value);

    @Name("Тип связи «{{ value }}» на блоке контактов")
    @FindBy("//div[contains(@class, 'ContactsControlAuthorized__control')][contains(., '{{ value }}')]")
    VertisElement contactType(@Param("value") String value);

    @Name("Закрыть экран")
    @FindBy("//button[contains(@class, 'closeButton')]")
    VertisElement close();

    @Name("Кнопка формы снизу")
    @FindBy("//div[contains(@class, 'OfferFormBottomPanel')]//button")
    VertisElement bottomButton();

    @Name("Блок пресетов внешних ссылок")
    @FindBy("//div[contains(@class, 'FormCategoryPresetControl')]")
    VertisElement presetsBlock();

    @Name("Блок контактов с подозрительной активностью ")
    @FindBy("//div[contains(@class, 'ContactsControlSuspicious')]")
    SuspiciousActivityContacts suspiciousActivityContacts();

    @Name("Прогресс-бар")
    @FindBy("//div[contains(@class, 'ProgressBar__progress')]")
    VertisElement progressBar();

    @Name("Список адресов")
    @FindBy("//div[contains(@class, ' OfferFormAddressControlSingle')]")
    ElementsCollection<FormAddressControl> addressesList();

    @Name("Блок с текстом")
    @FindBy("//p")
    VertisElement text();

    @Name("Картинка на экране с видео")
    @FindBy("//img[contains(@class, 'FormVideoControl__image')]")
    VertisElement youtubeImage();

    @Name("Фрейм с видео")
    @FindBy("//iframe[@id = 'videoFrame']")
    VertisElement videoFrame();

    @Name("Кнопка с выбранным состоянием")
    @FindBy("//div[contains(@class, 'FormConditionControlButton__containerSelected')]")
    Link selectedCondition();

    default float getProgressBarWidth() {
        return Float.valueOf(progressBar().getAttribute("style").replaceAll("[^\\d.]", ""));
    }

}
