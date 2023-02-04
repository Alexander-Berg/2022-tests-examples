package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithBreadcrumbs;
import ru.auto.tests.desktop.mobile.component.WithCallbackPopup;
import ru.auto.tests.desktop.mobile.component.WithParamsPopup;
import ru.auto.tests.desktop.mobile.component.WithSavedSearch;
import ru.auto.tests.desktop.mobile.component.WithSortBar;
import ru.auto.tests.desktop.mobile.element.SaleListItem;
import ru.auto.tests.desktop.mobile.element.group.Filters;
import ru.auto.tests.desktop.mobile.element.group.FiltersPopup;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface GroupPage extends BasePage, WithBreadcrumbs, WithSortBar, WithSavedSearch, WithCallbackPopup,
        WithParamsPopup {

    @Name("Содержимое страницы")
    @FindBy("//div[@class = 'content']")
    VertisElement content();

    @Name("Заголовок группы")
    @FindBy("//div[contains(@class, 'CardGroupHeaderMobile')] | " +
            "//div[contains(@class, 'AmpCardGroupHeader')]")
    VertisElement groupHeader();

    @Name("Кнопка «Поделиться»")
    @FindBy("//*[contains(@class, 'IconSvg_share')] | " +
            "//*[contains(@class, 'AmpIcon_share')]")
    VertisElement shareButton();

    @Name("Кнопка «О модели»")
    @FindBy("//a[contains(@class, 'CardActions__button')]")
    VertisElement aboutModelButton();

    @Name("Фильтры")
    @FindBy("//div[contains(@class, 'CardGroupOffers__filters')]")
    Filters filters();

    @Name("Поп-ап фильтров")
    @FindBy("//div[contains(@class, 'Modal_visible')] | " +
            "//div[@class='FiltersPopup']")
    FiltersPopup filtersPopup();

    @Name("Кнопка «Получить лучшую цену")
    @FindBy("//div[contains(@class, 'CardGroupHeaderMobile__matchApplication')]")
    VertisElement bestOfferButton();

    @Name("Заглушка на пустой групповой карточке")
    @FindBy("//div[@class = 'PageCardGroup']")
    VertisElement groupSaleStub();

    @Name("Кнопка «Смотреть другие предложения» на пустой групповой карточке")
    @FindBy("//a[contains(@class, 'CardGroupEmpty__button')]")
    VertisElement showOtherOffersButton();

    @Name("Список предложений")
    @FindBy("//div[@class = 'ListingItemBig' or @class = 'ListingItemRegular'] |" +
            "//div[@class = 'AmpCardGroupItemRegular']")
    ElementsCollection<SaleListItem> salesList();

    @Step("Получаем объявление с индексом {i}")
    default SaleListItem getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Кнопка «Показать ещё N предложений»")
    @FindBy("//div[contains(@class, 'CardGroupOffers__moreButton')]/button")
    VertisElement showMoreOffersButton();

    @Name("Кнопка «Показать ещё»")
    @FindBy("//a[contains(@class, 'amp-next-page-link')]")
    VertisElement showMoreButton();
}
