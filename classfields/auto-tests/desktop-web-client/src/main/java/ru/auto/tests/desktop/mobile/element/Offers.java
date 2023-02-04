package ru.auto.tests.desktop.mobile.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Offers extends VertisElement {

    @Name("Вкладка «{{ text }}»")
    @FindBy(".//button[.= '{{ text }}']")
    VertisElement tab(@Param("text") String Text);

    @Name("Список элементов")
    @FindBy(".//li[contains(@class, 'carousel__item')] | " +
            ".//li[contains(@class, 'Carousel__item')] | " +
            ".//li[contains(@class, 'CarouselUniversal__item')]")
    ElementsCollection<VertisElement> itemsList();

    @Name("Кнопка «Все»")
    @FindBy(".//a[contains(@class, '__footerLink')]")
    VertisElement allButton();

    @Name("Кнопка «Узнать о поступлении»")
    @FindBy(".//div[contains(@class, 'CarouselSubscriptionMobile')]")
    VertisElement subscribeButton();

    @Step("Получаем элемент карусели с индексом {i}")
    default VertisElement getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }
}