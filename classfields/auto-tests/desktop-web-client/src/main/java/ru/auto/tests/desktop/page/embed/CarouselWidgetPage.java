package ru.auto.tests.desktop.page.embed;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.element.VerticalCarouselItem;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CarouselWidgetPage extends BasePage, WithButton {

    @Name("Список элементов виджета")
    @FindBy(".//a[contains(@class, 'WidgetBanner240x400__item')]" +
            " | .//a[contains(@class, 'WidgetBanner250x250__item')]" +
            " | .//a[contains(@class, 'WidgetBanner468x60__item')]")
    ElementsCollection<VerticalCarouselItem> itemList();

    @Step("Получаем элемент с индексом {i}")
    default VerticalCarouselItem getItem(int i) {
        return itemList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Заголовок")
    @FindBy(".//div[@class = 'WidgetBanner240x400__header']//span | " +
            ".//div[@class = 'WidgetBanner250x250__header']//strong | " +
            ".//div[@class = 'WidgetBanner468x60__title']//span")
    VertisElement title();
}