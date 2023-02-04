package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.archive.OffersArchiveAddressForm;
import ru.yandex.realty.element.archive.RelatedOffersBlock;
import ru.yandex.realty.element.archive.SearchResultsBlock;
import ru.yandex.realty.element.archive.SelectorPopup;

/**
 * @author kantemirov
 */
public interface ArchivePage extends WebPage, BasePage {

    @Name("Приветственный Блок")
    @FindBy("//div[contains(@class, 'OffersArchive_suggest-opened')]")
    AtlasWebElement welcomeBlock();

    @Name("Блок поиска")
    @FindBy("//div[@class='OffersArchiveAddressForm']")
    OffersArchiveAddressForm searchForm();

    @Name("Информация об объекте")
    @FindBy("//div[@class='BuildingInfo _with-details']")
    AtlasWebElement buildingInfo();

    @Name("Кнопка фильтра по «{{ value }}»")
    @FindBy("//div[@class='OffersArchiveSearchFilters']//button[contains(.,'{{ value }}')]")
    AtlasWebElement filterButton(@Param("value") String value);

    @Name("Селектор «Кол-во комнат»")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    SelectorPopup selectorPopup();

    @Name("Блок результатов поиска")
    @FindBy("//div[contains(@class, 'OffersArchiveSearchOffers__body')]")
    SearchResultsBlock searchResultBlock();

    @Name("Кнопка пейджера «{{ value }}»")
    @FindBy("//div[contains(@class, 'Pager')]//button[contains(.,'{{ value }}')]")
    AtlasWebElement pagerButton(@Param("value") String value);

    @Name("Чекнутая кнопка пейджера «{{ value }}»")
    @FindBy("//div[contains(@class, 'Pager')]//button[contains(.,'{{ value }}') and contains(@class,'_checked')]")
    AtlasWebElement pagerButtonChecked(@Param("value") String value);

    @Name("Блок похожих")
    @FindBy("//div[contains(@class, 'OffersArchiveRelatedOffers')]")
    RelatedOffersBlock relatedOffersBlock();
}
