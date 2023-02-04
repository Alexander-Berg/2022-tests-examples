package ru.auto.tests.desktop.page.embed;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.element.VerticalCarouselItem;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CarouselListingPage extends BasePage, WithButton {

    @Name("Список элементов карусели")
    @FindBy(".//ul[@class = 'CarouselUniversal__list'] | " +
            ".//a[contains(@class, 'ListingItemCarousel_type_mobile')]")
    ElementsCollection<VerticalCarouselItem> offersList();

    @Step("Получаем элемент с индексом {i}")
    default VerticalCarouselItem getItem(int i) {
        return offersList().should(hasSize(greaterThan(i))).get(i);
    }

}
