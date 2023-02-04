package ru.yandex.general.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.Button;
import ru.yandex.general.element.FeedErrorRow;
import ru.yandex.general.element.FeedHistoryModal;
import ru.yandex.general.element.Input;
import ru.yandex.general.element.Link;

public interface FeedPage extends BasePage, Input, Button {

    String DOWNLOAD_HISTORY = "История загрузок";
    String SEND = "Отправить";
    String EDIT = "Редактировать";
    String DELETE = "Удалить";
    String XML = "XML";
    String YML = "YML";
    String HELP = "справке";
    String YMARKET_FOR_BUSINESS = "«Яндекс.Маркет для бизнеса»";
    String TERMS = "условиями";
    String CARS = "Автомобили";
    String REALTY = "Недвижимость";
    String PROCESSING = "Обрабатывается";
    String PROCESSED = "Обработан";
    String FAILED = "Отклонён";
    String FEED_URL_INPUT = "Ссылка на фид";
    String FILE = "Файл";
    String OFFERS = "Объявления";
    String ACTIVE = "Активные";
    String ERRORS = "Ошибки";
    String CRITICAL = "Критические";
    String WARNINGS = "Предупреждения";
    String ALL = "Все";
    String ADD_ADDRESS = "Добавить адрес";
    String ADD_PHONE_NUMBER = "Добавить номер телефона";
    String PHONE_INPUT = "Телефон";
    String ADDRESS_INPUT = "Адрес";

    @Name("Статистика по фиду")
    @FindBy("//div[contains(@class, 'FeedStats__stats')]")
    VertisElement feedStats();

    @Name("Блок информации")
    @FindBy("//div[contains(@class, 'FeedPageComponents__info')]")
    Link info();

    @Name("Иконка успешной загрузки фида")
    @FindBy("//div[contains(@class, '_iconSuccess')]")
    VertisElement iconSuccess();

    @Name("Статус фида")
    @FindBy("//span[contains(@class, 'FeedStatus__status')]")
    VertisElement feedStatus();

    @Name("Кнопка «Удалить» в аплоадере")
    @FindBy("//div[contains(@class, 'FileUploadControl')]//button[contains(., 'Удалить')]")
    VertisElement deleteInUploader();

    @Name("Столбец статистики «{{ value }}»")
    @FindBy("//div[contains(@class, 'FeedStats__column')][contains(., '{{ value }}')]/div[contains(@class, '_value')]")
    VertisElement statsColumn(@Param("value") String value);

    @Name("Таблица ошибок")
    @FindBy("//div[contains(@class, '_bodyRow')]")
    ElementsCollection<FeedErrorRow> errorsTable();

    @Name("Модалка с историей загрузок")
    @FindBy("//div[contains(@class, 'Modal_visible')][.//div[contains(@class, 'FeedHistoryModal__container_')]]")
    FeedHistoryModal feedHistoryModal();

}
