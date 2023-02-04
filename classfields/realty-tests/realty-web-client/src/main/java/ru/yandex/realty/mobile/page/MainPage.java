package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.mobile.element.ButtonWithText;
import ru.yandex.realty.mobile.element.PageBanner;
import ru.yandex.realty.mobile.element.Slider;
import ru.yandex.realty.mobile.element.main.ExtendedFiltersBlock;
import ru.yandex.realty.mobile.element.main.FiltersBlock;
import ru.yandex.realty.mobile.element.main.PresetsSection;

import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;


public interface MainPage extends BasePage, Link, ButtonWithText {

    String KUPIT_OPTION = "Купить";
    String SNYAT_OPTION = "Снять";
    String KVARTIRU_OPTION = "Квартиру";
    String DOM_OPTION = "Дом";
    String LOT_OPTION = "Участок";
    String GARAZH_OPTION = "Гараж или машиноместо";
    String KOMNATU_OPTION = "Комнату";
    String KOMMERCHESKUYU_OPTION = "Коммерческую недвижимость";
    String ON_MAP = "На карте";

    @Name("Блок фильтров")
    @FindBy("//form[contains(@class, 'FiltersForm_main-expanded') and not(contains(@class,'FiltersForm_extra-expanded'))]")
    FiltersBlock searchFilters();

    @Name("Блок расширенных фильтров")
    @FindBy("//form[contains(@class, 'FiltersForm_extra-expanded')]")
    ExtendedFiltersBlock extendFilters();

    @Name("Список офферов")
    @FindBy("//div[contains(@class,'serp-item_lazy_yes')]")
    ElementsCollection<AtlasWebElement> offers();

    @Name("Смарт-баннер")
    @FindBy("//div[@class = 'SmartBanner PageBanner']")
    PageBanner smartBanner();

    @Name("Сплэш-баннер")
    @FindBy("//div[contains(@class,'Modal_visible')][.//div[contains(@class, 'SplashBanner')]]")
    PageBanner splashBanner();

    @Name("Добавьте объявление через приложение")
    @FindBy("//div[@class ='AddOfferAppPromo']")
    AtlasWebElement addOfferAppPromo();

    @Name("Секция пресета «{{ value }}»")
    @FindBy("//div[contains(@class, 'PresetsSection')][h2[contains(., '{{ value }}')]]")
    PresetsSection preset(@Param("value") String value);

    @Name("Справочник недвижимости")
    @FindBy("//div[@class = 'BlogPostsSection']")
    Slider spravochnik();

    @Name("Тепловые карты")
    @FindBy("//section[@class = 'HeatmapsSection']")
    Link heatmap();

    @Name("Премиум объявления")
    @FindBy("//div[@class = 'PremiumOffers']")
    Slider premiumOffers();

    default void openExtFilter() {
        spanLink("Больше параметров").click();
        waitSomething(3, TimeUnit.SECONDS);
    }

    default void hideExtFilters() {
        spanLink("Меньше параметров").click();
        waitSomething(3, TimeUnit.SECONDS);
    }
}
