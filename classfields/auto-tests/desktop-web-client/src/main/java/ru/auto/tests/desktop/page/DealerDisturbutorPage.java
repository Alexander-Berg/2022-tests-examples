package ru.auto.tests.desktop.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.component.WithListingFilter;
import ru.auto.tests.desktop.element.dealers.DealerListItem;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface DealerDisturbutorPage extends BasePage, WithListingFilter {

    @Name("Список дилеров")
    @FindBy("//div[contains(@class, 'DealerDistributorListing__item')]")
    ElementsCollection<DealerListItem> dealerList();

    @Step("Получаем дилера с индексом {i}")
    default DealerListItem getDealer(int i) {
        return dealerList().should(hasSize(greaterThan(i))).get(i);
    }
}