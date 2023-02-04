package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.subscriptions.ConfirmDeletePopup;
import ru.yandex.realty.element.subscriptions.SubscriptionItem;
import ru.yandex.realty.element.subscriptions.SubscriptionPopup;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;

public interface SubscriptionPage extends BasePage, Link {

    String SAVE_BUTTON = "Сохранить";
    String MY_SEARCHES_TAB = "Мои поиски";
    String NEWBUILDING_TAB = "Новостройки";
    String PRICE_CHANGE_TAB = "Изменение цены";
    String NEW_SEARCH_BUTTON = "Начать новый поиск";

    @Name("Информация об одной подписке")
    @FindBy("//div[contains(@class,'SubscriptionList__subscription')]")
    ElementsCollection<SubscriptionItem> subscriptionList();

    @Name("Попап настройки подписки")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    SubscriptionPopup subscriptionPopup();

    @Name("Таб «{{ value }}»")
    @FindBy("//a[contains(@class,'SubscriptionsTabSwitcher__tab')][contains(.,'{{ value }}')]")
    AtlasWebElement tab(@Param("value") String value);

    @Name("Контент страницы")
    @FindBy("//div[contains(@class,'Subscriptions__content')]")
    AtlasWebElement subscriptionContent();

    @Name("Тумблер «Все подписки»")
    @FindBy("//div[contains(@class, 'SubscriptionsAllActiveTumbler')]//div[contains(@class,'Tumbler_view_yellow')]")
    AtlasWebElement allSubscriptionsTumbler();

    @Name("Попап удаления")
    @FindBy("//div[contains(@class,'Modal_visible')]")
    ConfirmDeletePopup deletePopup();

    default SubscriptionItem subscription(int i) {
        return subscriptionList().waitUntil(hasSize(greaterThan(i))).get(i);
    }

    default void checkAllSubscriptions() {
        allSubscriptionsTumbler().should(not(isChecked())).click();
        allSubscriptionsTumbler().should(isChecked());
    }

    default void unCheckAllSubscriptions() {
        allSubscriptionsTumbler().should(isChecked()).click();
        allSubscriptionsTumbler().should(not(isChecked()));
    }
}
