package ru.yandex.general.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.Button;
import ru.yandex.general.element.Link;
import ru.yandex.general.element.ProfileSidebar;
import ru.yandex.general.element.ListingSnippet;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface PublicProfilePage extends BasePage, Link {

    String YANDEX_ACTIVITY = "Активность на Яндексе";
    String ACTIVE = "Активные";
    String EXPIRED = "Завершенные";
    String SUBSCRIBE = "Подписаться";
    String LET = "Давайте";
    String SET = "Настроить";
    String UNSUBSCRIBE = "Отписаться";
    String CANCEL = "Отмена";
    String EDIT = "Редактировать";

    @Name("Сайдбар")
    @FindBy("//aside[contains(@class, 'Sidebar')]/div[contains(@class, 'Page__navigationWrapper')]")
    ProfileSidebar sidebar();

    @Name("Имя продавца")
    @FindBy("//h1")
    VertisElement sellerName();

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
