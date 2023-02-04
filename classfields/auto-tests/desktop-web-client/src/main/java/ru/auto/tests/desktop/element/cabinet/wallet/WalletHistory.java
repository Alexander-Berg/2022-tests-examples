package ru.auto.tests.desktop.element.cabinet.wallet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.Pager;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface WalletHistory extends VertisElement {

    @Name("Услуга «{{ text }}»")
    @FindBy("//div[contains(@class, 'WalletHistoryItem__name') and .= '{{ text }}']")
    VertisElement service(@Param("text") String text);

    @Name("Ссылка на объявление")
    @FindBy("//a[contains(@class, 'WalletHistoryItemLink__link')]")
    VertisElement offerUrl();

    @Name("Список объявлений")
    @FindBy(".//div[contains(@class, 'WalletHistoryOffer__container')]")
    ElementsCollection<WalletHistoryOffer> offersList();

    @Name("Пагинатор")
    @FindBy("//div[@class = 'ListingPagination']")
    Pager pager();

    @Step("Получаем объявление с индексом {i}")
    default WalletHistoryOffer getOffer(int i) {
        return offersList().should(hasSize(greaterThan(i))).get(i);
    }
}