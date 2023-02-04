package ru.auto.tests.desktop.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithAuthPopup;
import ru.auto.tests.desktop.component.WithCardHeader;
import ru.auto.tests.desktop.component.WithFullScreenGallery;
import ru.auto.tests.desktop.component.WithListingFilter;
import ru.auto.tests.desktop.component.WithListingSortBar;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.component.WithSalesList;
import ru.auto.tests.desktop.component.WithSavedSearchesPopup;
import ru.auto.tests.desktop.component.WithSubCategories;
import ru.auto.tests.desktop.element.dealers.card.Gallery;
import ru.auto.tests.desktop.element.dealers.card.Info;
import ru.auto.tests.desktop.element.dealers.card.Stub;

import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;

public interface DealerCardPage extends BasePage, WithListingFilter, WithSalesList, WithPager, WithListingSortBar,
        WithCardHeader, WithFullScreenGallery, WithSubCategories, WithAuthPopup, WithSavedSearchesPopup {

    @Name("Инфо")
    @FindBy("//div[contains(@class, 'SalonHeader__column_left')]")
    Info info();

    @Name("Хлебные крошки")
    @FindBy(".//ul[contains(@class, 'SalonHeader__breadcrumbs')]")
    VertisElement breadcrumbs();

    @Name("Хлебная крошка «{{ text }}»")
    @FindBy(".//ul[contains(@class, 'SalonHeader__breadcrumbs')]//*[contains(., '{{ text }}')]")
    VertisElement breadcrumb(@Param("text") String Text);

    @Name("Заголовок поп-апа с адресом")
    @FindBy(".//div[contains(@class,'Modal_visible')]//div[contains(@class, 'SalonLocationModal__info')]")
    VertisElement addressPopupTitle();

    @Name("Кнопка «Показать телефон»")
    @FindBy("//div[contains(@class, 'SalonPhoneButton')] | " +
            "//div[contains(@data-bem,'dealer-info-phones')]")
    VertisElement showPhonesButton();

    @Name("Специальная подложка для дилеров ВАЗ")
    @FindBy("//div[contains(@class, 'page_custombg_vaz')]")
    VertisElement vazBackground();

    @Name("Заглушка")
    @FindBy("//div[contains(@class, 'SalonEmptyPlaceholder')]")
    Stub stub();

    @Name("Галерея")
    @FindBy("//div[contains(@class, 'SalonHeader__carousel')]")
    Gallery gallery();

    @Step("Ждём обновления листинга")
    default void waitForListingReload() {
        waitSomething(2, TimeUnit.SECONDS);
    }
}
