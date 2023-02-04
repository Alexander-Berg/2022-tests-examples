package ru.yandex.general.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.mobile.element.BanMessage;
import ru.yandex.general.mobile.element.MyOfferSnippet;

import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.mobile.element.Link.HREF;

public interface MyOffersPage extends BasePage {

    String ACTIVE_TAB = "Активные";
    String ENDED_TAB = "Завершенные";
    String SOLDED_TAB = "Проданные";
    String DELETE = "Удалить";

    @Name("Список моих офферов")
    @FindBy("//div[contains(@class, 'OffersList__item')]")
    ElementsCollection<MyOfferSnippet> myOffers();

    @Name("Действие с офферами")
    @FindBy("//div[contains(@class, 'OffersControls__icons')]//button")
    VertisElement offerAction();

    @Name("Блок сообщения забаненному юзеру")
    @FindBy("//div[contains(@class, 'Content__banMessage')]")
    BanMessage banMessage();

    default MyOfferSnippet snippetWithId(String id) {
        return myOffers().waitUntil(hasSize(greaterThan(0)))
                .filter(snippet -> snippet.link().getAttribute(HREF).contains(format("/offer/%s/", id)))
                .should(hasSize(1))
                .get(0);
    }

    @Step("Проверяем что сниппет с id = «{id}» не отображается")
    default void shouldNoSnippetWithId(String id) {
        if (myOffers().size() > 0)
            myOffers().filter(snippet -> snippet.link().getAttribute(HREF).contains(format("/offer/%s/", id)))
                    .should(hasSize(0));
    }

    default MyOfferSnippet snippetFirst() {
        return myOffers().waitUntil(hasSize(greaterThan(0))).get(0);
    }

}