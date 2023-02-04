package ru.yandex.general.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.general.element.Link;
import ru.yandex.general.mobile.element.Button;
import ru.yandex.general.mobile.element.ListingSnippet;
import ru.yandex.general.mobile.element.ProfileUserInfo;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface ProfilePage extends BasePage, Link, Button {

    String YANDEX_ACTIVITY = "Активность на Яндексе";
    String ACTIVE = "Активные";
    String EXPIRED = "Завершенные";
    String SUBSCRIBE = "Подписаться";
    String LET = "Давайте";
    String SET = "Настроить";
    String UNSUBSCRIBE = "Отписаться";

    @Name("Информация о продавце")
    @FindBy("//div[contains(@class, 'ProfileUserInfo')]")
    ProfileUserInfo userInfo();

    @Name("Сниппет")
    @FindBy("//div[@role = 'listItem' or @role = 'gridItem']")
    ElementsCollection<ListingSnippet> snippets();

    @Name("Первый сниппет")
    @FindBy("//div[@role = 'listItem' or @role = 'gridItem'][contains(@style, 'top: 0px; left: 0px;')]")
    ListingSnippet firstSnippet();

    default ListingSnippet snippetFirst() {
        snippets().waitUntil(hasSize(greaterThan(0)));
        return firstSnippet();
    }

}
