package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.mobile.element.subscriptions.SubscriptionItem;
import ru.yandex.realty.mobile.element.subscriptions.SubscriptionPopup;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;

public interface SubscriptionPage extends BasePage {

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
    @FindBy("//a[contains(@class,'SubscriptionsTabSelector__tab')][contains(.,'{{ value }}')]")
    AtlasWebElement tab(@Param("value") String value);

    @Name("Тумблер «Все подписки»")
    @FindBy("//div[contains(@class, 'SubscriptionsAllActiveTumbler')]//div[contains(@class,'Tumbler_view_yellow')]")
    AtlasWebElement allSubscriptionsTumbler();

    @Name("Контент")
    @FindBy("//section[contains(@class,'PageContentMain')]")
    AtlasWebElement subscriptionContent();

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
