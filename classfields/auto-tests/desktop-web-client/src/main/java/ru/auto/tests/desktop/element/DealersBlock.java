package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface DealersBlock extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//h3//a | " +
            ".//a[contains(@class, 'GenericItemsList__header')]")
    VertisElement headerUrl();

    @Name("Список дилеров")
    @FindBy(".//div[contains(@class, 'related-dealers-item services-crosslinks-item')] | " +
            ".//div[@class = 'RelatedDealersItem']")
    ElementsCollection<VertisElement> dealersList();

    @Name("Кнопка «Все дилеры»")
    @FindBy(".//div[contains(@class, 'all')]/a | " +
            ".//a[contains(@class, 'GenericItemsList__footerButton')] | " +
            ".//div[contains(@class, 'GenericItemsList__footer')]/a | " +
            ".//div[contains(@class, 'ListingDealersList__footer')]/a")
    VertisElement allDealersButton();

    @Step("Получаем дилера с индексом {i}")
    default VertisElement getDealer(int i) {
        return dealersList().should(hasSize(greaterThan(i))).get(i);
    }
}