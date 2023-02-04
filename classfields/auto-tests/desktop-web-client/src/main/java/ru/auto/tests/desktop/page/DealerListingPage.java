package ru.auto.tests.desktop.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithCrossLinksBlock;
import ru.auto.tests.desktop.component.WithSubHeader;
import ru.auto.tests.desktop.element.dealers.DealerListItem;
import ru.auto.tests.desktop.element.dealers.MapPopup;
import ru.auto.tests.desktop.element.dealers.SearchBlock;
import ru.auto.tests.desktop.element.listing.Filter;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface DealerListingPage extends BasePage, WithSubHeader, WithCrossLinksBlock, WithCheckbox {

    @Name("Блок поиска")
    @FindBy("//div[contains(@class, 'PageDealersListing__content-left')]")
    SearchBlock searchBlock();

    @Name("Секция «{{ text }}»")
    @FindBy("//div[contains(@class, 'DealerSearchForm__tab ')]//a[.='{{ text }}']")
    VertisElement section(@Param("text") String Text);

    @Name("Кнопка «Официальный дилер»")
    @FindBy("//div[contains(@class, 'InfoPopup_theme_white')]")
    VertisElement infoPopupButton();

    @Name("Список дилеров")
    @FindBy("//div[contains(@class, 'DealerList__item')]")
    ElementsCollection<DealerListItem> dealerList();

    @Step("Получаем дилера с индексом {i}")
    default DealerListItem getDealer(int i) {
        return dealerList().should(hasSize(greaterThan(i))).get(i);
    }

    @FindBy("//input[@name='dealer-autocomplete']")
    VertisElement nameInput();

    @FindBy("//div[@id='react-autowhatever-1']//div[text()='{{ text }}']")
    VertisElement suggestItem(@Param("text") String text);

    @FindBy("//i[contains(@class, 'Input__clear_visible')]")
    VertisElement inputClear();

    @Name("Поп-ап на карте")
    @FindBy("//div[contains(@class, 'YandexMapBalloon__content')]")
    MapPopup mapPopup();

    @Name("Фильтр")
    @FindBy("//div[contains(@class,'search-form ')]")
    Filter filter();

    @Name("Карта")
    @FindBy("//div[contains(@class, 'DealerSearchMap')]")
    VertisElement map();
}
