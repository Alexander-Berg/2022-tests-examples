package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithCrossLinksBlock;
import ru.auto.tests.desktop.mobile.element.catalog.BodyComplectations;
import ru.auto.tests.desktop.mobile.element.catalog.BodyDescription;
import ru.auto.tests.desktop.mobile.element.catalog.ComplectationDescription;
import ru.auto.tests.desktop.mobile.element.catalog.Gallery;
import ru.auto.tests.desktop.mobile.element.catalog.PopularVideos;

public interface CatalogBodyPage extends CatalogPage, WithCrossLinksBlock {

    @Name("Галерея")
    @FindBy("//div[contains(@class, 'Carousel')]")
    Gallery gallery();

    @Name("Блок объявлений")
    @FindBy("//div[contains(@class, 'banner_type_catalog')] | " +
            "//div[@class = 'Counter']")
    VertisElement sales();

    @Name("Описание модели")
    @FindBy("//div[contains(@class, 'listing-item listing-item_view_promo catalog__item')] | " +
            "//div[contains(@class, 'ConfigurationDetails')]")
    BodyDescription description();

    @Name("Комплектации")
    @FindBy("//div[@class = 'catalog__packages-list'] | " +
            "//div[contains(@class, 'Complectations')]")
    BodyComplectations bodyComplectations();

    @Name("Описание комплектации")
    @FindBy("//div[@class = 'catalog__package']")
    ComplectationDescription complectationDescription();

    @Name("Рекламный блок с2")
    @FindBy("//div[contains(@class, 'catalog__features')]/div/ancestor::div")
    VertisElement c2advert();

    @Name("Ссылка на листинг в описании модификации")
    @FindBy("//div[@class='catalog__item-summary-value']//a")
    VertisElement modificationListingUrl();

    @Name("Ссылка в источнике данных")
    @FindBy("//div[@class='catalog-provider-info']//a")
    VertisElement providerInfoUrl();

    @Name("Селектор «{{ filterName }}»")
    @FindBy("//div[contains(@class, 'Select') and .//span/text() = '{{ filterName }}']")
    VertisElement selector(@Param("filterName") String filterName);

    @Name("Опция комплектации «{{ text }}»")
    @FindBy("//span[@class='catalog-option__name' and contains(., '{{ text }}')]//..//..//span[@class='Checkbox__box']")
    VertisElement option(@Param("text") String text);

    @Name("Пакет опций")
    @FindBy("//span[@class='catalog-package__name' and contains(., '{{ text }}')]//..//..//span[@class='Checkbox__box']")
    VertisElement optionPackage(@Param("text") String text);

    @Name("Популярные видео")
    @FindBy("//div[@class = 'RelatedItems'] | " +
            "//div[contains(@class, 'VideoCarousel')]")
    PopularVideos popularVideos();

    @Name("Кнока закрытия фрейма свидео")
    @FindBy("//div[contains(@class, 'VideoItem__close')]")
    VertisElement closeVideo();

    @Name("Кнопка «Следующие»")
    @FindBy("//a[contains(@class, 'amp-next-page-link')]")
    VertisElement nextButton();

    @Name("Кнопка «Предыдущие»")
    @FindBy("//div[@class='index-presets-content__more']")
    VertisElement prevButton();

    @Name("Кнопка «Купить новый»")
    @FindBy("//a[contains(@class, 'new_offers_button')] | " +
            "//button[contains(@class, 'new_offers_button')]")
    VertisElement buyNewButton();
}
