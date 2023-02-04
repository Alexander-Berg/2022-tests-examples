package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithMmmPopup;
import ru.auto.tests.desktop.mobile.component.WithSalesList;
import ru.auto.tests.desktop.mobile.component.WithSavedSearch;
import ru.auto.tests.desktop.mobile.component.WithSortBar;
import ru.auto.tests.desktop.mobile.element.Filters;
import ru.auto.tests.desktop.mobile.element.dealers.card.Info;
import ru.auto.tests.desktop.mobile.element.dealers.card.Stub;
import ru.auto.tests.desktop.mobile.element.dealers.card.SubCategories;
import ru.auto.tests.desktop.mobile.element.listing.ParamsPopup;

public interface DealerCardPage extends BasePage, WithSortBar, WithSalesList, WithMmmPopup, WithSavedSearch {

    String DEALERSHIP_USED = "/rolf_severo_zapad_avtomobili_s_probegom_moskva/";
    String DEALERSHIP_NEW = "/ug_avto_hyundai_krasnodar_uralskaya/";
    String DEALERSHIP_MARK = "Hyundai";
    String DEALERSHIP_MODEL = "Creta";
    String DEALERSHIP_GEN = "I Рестайлинг";
    String DEALERSHIP_GEN_CODE = "21831433";

    String DEALERSHIP_MODEL_2 = "Solaris";

    @Name("Название дилера")
    @FindBy("//h1")
    VertisElement name();

    @Name("Адрес")
    @FindBy("//div[contains(@class, 'dealer-card__location')] | " +
            "//div[contains(@class, 'DealerInfo__location')]")
    VertisElement address();

    @Name("Хлебные крошки")
    @FindBy(".//div[@class='dealer-card__breadcrumbs'] | " +
            ".//div[contains(@class, 'DealerInfo__breadcrumbs')]")
    VertisElement breadcrumbs();

    @Name("Кнопка «Показать телефон»")
    @FindBy("//button[contains(@class, 'Button_size_xl')]")
    VertisElement showPhone();

    @Name("Фильтры")
    @FindBy("//section[contains(@class, 'ListingHeadMobile')]")
    Filters filters();

    @Name("Поп-ап параметров")
    @FindBy("//div[contains(@class, 'FiltersPopup')]")
    ParamsPopup paramsPopup();

    @Name("Хлебная крошка «{{ text }}»")
    @FindBy("//a[contains(@class, 'DealerInfo__breadcrumbs-item') and .= '{{ text }}']")
    VertisElement breadcrumb(@Param("text") String Text);

    @Name("Инфо")
    @FindBy("//div[@class = 'DealerInfo']")
    Info info();

    @Name("Кнопка «Показать телефон»")
    @FindBy("//div[contains(@class, 'PageSalon__phone')]")
    VertisElement showPhoneButton();

    @Name("Блок подкатегорий мото/комТС")
    @FindBy("//div[contains(@class, 'IndexSubcategorySelector')]")
    SubCategories subCategories();

    @Name("Заглушка")
    @FindBy("//div[contains(@class, 'SalonEmptyPlaceholder')]")
    Stub stub();
}
