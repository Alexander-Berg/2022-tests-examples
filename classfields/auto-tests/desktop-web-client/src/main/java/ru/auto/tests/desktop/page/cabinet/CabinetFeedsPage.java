package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.element.cabinet.feeds.DownloadHistory;
import ru.auto.tests.desktop.element.cabinet.feeds.Feed;
import ru.auto.tests.desktop.element.cabinet.feeds.FeedStatusBlock;

public interface CabinetFeedsPage extends BasePage, WithNotifier, WithInput, WithPager, WithButton {

    String MANUAL = "Ручная";
    String CARS_USED = "Легковые с пробегом";
    String NOTIFY_SUCCESS = "Фид успешно сохранён";
    String DONT_DELETE_PHOTO_CHECKBOX = "Не\u00a0удалять загруженные вручную фотографии и видео";
    String AUTO = "Автоматическая";
    String HEAVY_TRUCKS = "Тяжелые коммерческие с пробегом";
    String TRUCKS = "Грузовики";
    String DELETE_MANUAL_CHECKBOX = "Удалять размещённые вручную объявления";
    String DONT_DELETE_SERVICES_CHECKBOX = "Не\u00a0удалять услуги объявлений";
    String DELETE_FEED = "Удалить фид";

    @Name("Категория фида «{{ text }}»")
    @FindBy(".//button[contains(@class, 'Button_radius_round') and .='{{ text }}']")
    VertisElement category(@Param("text") String Text);

    @Name("Чекбокс «{{ text }}»")
    @FindBy(".//label[contains(@class, 'Checkbox') and .='{{ text }}']")
    VertisElement checkbox(@Param("text") String text);

    @Name("Блок заполнения фида")
    @FindBy(".//div[(@class='FeedsSettingsAddForm__container')]")
    VertisElement addFeedBlock();

    @Name("Фид «{{ text }}»")
    @FindBy(".//div[contains(@class, 'FeedsSettings__item') and " +
            ".//div[contains(@class, 'CollapseCard__title') and .='{{ text }}']]")
    Feed feed(@Param("text") String Text);

    @Name("Раздел «{{ text }}»")
    @FindBy(".//a[contains(@class, 'ServiceNavigation__link') and .='{{ text }}']")
    VertisElement linkNavigation(@Param("text") String text);

    @Name("Дата «{{ text }}»")
    @FindBy(".//td[contains(@class, 'FeedsHistory__dateCell') and .='{{ text }}']")
    VertisElement dataFeed(@Param("text") String text);

    @Name("Блок с записями про фиды")
    @FindBy(".//table[contains(@class, 'FeedsHistory__table')]")
    VertisElement blockFeedsHistory();

    @Name("История загрузок")
    @FindBy(".//table[contains(@class, 'FeedsHistory__table')]")
    DownloadHistory downloadHistory();

    @Name("Блок статусов")
    @FindBy(".//div[contains(@class, 'FeedsDetailsOffersBase__container')]")
    FeedStatusBlock feedStatusBlock();

    @Name("Блок конкретного фида")
    @FindBy(".//div[contains(@class, 'FeedsDetailsBase__container')]")
    VertisElement feedBlock();
}
