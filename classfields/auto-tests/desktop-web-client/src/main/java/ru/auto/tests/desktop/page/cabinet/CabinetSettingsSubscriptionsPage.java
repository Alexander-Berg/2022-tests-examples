package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.element.cabinet.settings.MailingListBlock;
import ru.auto.tests.desktop.element.cabinet.settings.Section;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 24.08.18
 */
public interface CabinetSettingsSubscriptionsPage extends BasePage, WithNotifier {
    String CARS_SWITCHER = "Легковые";
    String COM_TC_SWITCHER = "Коммерческий транспорт";
    String MOTO_SWITCHER = "Мото";
    String AUTOMATIC_PHOTO_ORDERING_SWITCHER = "Автоматический порядок фотографий";
    String HIDE_LICENSE_PLATE_SWITCHER = "Скрывать госномера";
    String ACTIVATE_OFFER_AFTER_IDLE_TIME_SECTION = "Активировать объявления после простоя";
    String AUTOMATIC_PHOTO_ORDERING_SECTION = "Автоматический порядок фотографий";
    String HIDE_LICENSE_PLATE = "Скрывать госномера";
    String DATA_SAVED_POPUP = "Данные сохранены";
    String PROJECT_NEWS_BLOCK = "Новости проекта";
    String DATA_SUCCESSFULLY_SAVED_POPUP = "Данные успешно сохранены";

    @Name("Секция «{{ name }}»")
    @FindBy("//div[@class = 'SettingsOffers__section' and ./div[contains(., '{{ name }}')]]")
    Section section(@Param("name") String name);

    @Name("Настройки рассылок «'{{ name }}'»")
    @FindBy("//div[@class = 'SettingsSubscription__category' and .//span[contains(., '{{ name }}')]]")
    MailingListBlock mailingListBlock(@Param("name") String name);

    @Name("Поп-ап «{{ popupText }}»")
    @FindBy("//div[(contains(@class, 'notifier_autocloseable') or contains(@class, 'Notifier')) " +
            "and contains(., '{{ popupText }}')]")
    VertisElement serviceStatusPopup(@Param("popupText") String popupText);

}
