package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithAppPromo;
import ru.auto.tests.desktop.mobile.component.WithFiltersPopup;
import ru.auto.tests.desktop.mobile.component.WithGeoPopup;
import ru.auto.tests.desktop.mobile.component.WithGeoRadiusPopup;
import ru.auto.tests.desktop.mobile.component.WithMmmPopup;
import ru.auto.tests.desktop.mobile.component.WithOptionsPopup;
import ru.auto.tests.desktop.mobile.component.WithParamsPopup;
import ru.auto.tests.desktop.mobile.component.WithPaymentMethodsPopup;
import ru.auto.tests.desktop.mobile.component.WithSearchLine;
import ru.auto.tests.desktop.mobile.element.Filters;
import ru.auto.tests.desktop.mobile.element.main.Stories;
import ru.auto.tests.desktop.mobile.element.main.StoriesGallery;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface MainPage extends BasePage, WithPaymentMethodsPopup, WithAppPromo,
        WithSearchLine, WithParamsPopup, WithMmmPopup, WithGeoPopup, WithGeoRadiusPopup, WithFiltersPopup,
        WithOptionsPopup {

    @Name("Фильтры")
    @FindBy("//div[contains(@class, 'IndexHeader__filter-line')]")
    Filters filters();

    @Name("Блок историй")
    @FindBy("//ul[contains(@class, 'Stories')]")
    Stories stories();

    @Name("Галерея с историями")
    @FindBy("//div[@class = 'StoriesGallery']")
    StoriesGallery storiesGallery();

    @Name("FAB «Разместить объявление»")
    @FindBy("//div[contains(@class, 'PageIndex__addOfferFab')]")
    VertisElement fabAddSale();

    @Name("Листинг спиппетов блока «Рекомендации»")
    @FindBy("//div[contains(@class, ' IndexInfinityListing__item')]")
    ElementsCollection<VertisElement> snippets();

}
