package ru.auto.tests.desktop.page.cabinet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.element.cabinet.booking.BookingItem;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CabinetBookingPage extends BasePage, WithPager {

    @Name("Список заявок")
    @FindBy("//div[@class = 'BookingItem']")
    ElementsCollection<BookingItem> bookingItemsList();

    @Step("Получаем заявку с индексом {i}")
    default BookingItem getBookingItem(int i) {
        return bookingItemsList().should(hasSize(greaterThan(i))).get(i);
    }
}