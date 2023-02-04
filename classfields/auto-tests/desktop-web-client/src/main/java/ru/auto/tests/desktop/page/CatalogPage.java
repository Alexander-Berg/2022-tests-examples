package ru.auto.tests.desktop.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCrossLinksBlock;
import ru.auto.tests.desktop.component.WithOffers;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.component.WithVideos;
import ru.auto.tests.desktop.element.catalog.ClassicsBlock;
import ru.auto.tests.desktop.element.catalog.Complectation;
import ru.auto.tests.desktop.element.catalog.ComplectationDescription;
import ru.auto.tests.desktop.element.catalog.News;
import ru.auto.tests.desktop.element.catalog.OptionPackage;
import ru.auto.tests.desktop.element.catalog.Presets;
import ru.auto.tests.desktop.element.catalog.card.BodyItem;
import ru.auto.tests.desktop.element.catalog.card.Gallery;
import ru.auto.tests.desktop.element.catalog.card.ModelSummary;
import ru.auto.tests.desktop.element.catalog.card.OpinionsBlock;
import ru.auto.tests.desktop.element.listing.Filter;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CatalogPage extends BasePage, WithPager, WithVideos, WithOffers, WithCrossLinksBlock {

    @Name("Вкладка '{{ text }}'")
    @FindBy("//div[contains(@class, 'catalog__links')]//a[text()='{{ text }}']")
    VertisElement tab(@Param("text") String Text);

    @Name("Фильтр")
    @FindBy("//form[contains(@class,'search-form ')]")
    Filter filter();

    @Name("Кнопка добавления в сравнение на карточке модели")
    @FindBy("//div[contains(@class, 'compare-add-button')]//span[text()='Сравнить']")
    VertisElement compareAddButton();

    @Name("Кнопка удаления из сравнения на карточке модели")
    @FindBy("//div[contains(@class, 'compare-add-button')]//span[text()='В сравнении']")
    VertisElement compareDeleteButton();

    @Name("Кнопка добавления в сравнение в характеристиках модели")
    @FindBy("//a[contains(@class, 'catalog__compare')]")
    VertisElement compareAddButtonSpecifications();

    @Name("Кнопка удаления из сравнения в характеристиках модели")
    @FindBy("//a[contains(@class, 'catalog__compare_added')]")
    VertisElement compareDeleteButtonSpecifications();

    @Name("Вкладка на карточке")
    @FindBy("//div[contains(@class, 'tabs_view_classic')]//a[text()='{{ text }}']")
    VertisElement cardTab(@Param("text") String Text);

    @Name("Сводка о модели")
    @FindBy("//div[contains(@class, 'catalog__page')]/div[contains(@class, 'catalog-generation-summary')]")
    ModelSummary modelSummary();

    @Name("Поколения на карточке")
    @FindBy("//div[contains(@class, 'catalog__generations')]")
    VertisElement cardGenerations();

    @Name("Кузова")
    @FindBy("//div[contains(@class, 'mosaic_size_s')]")
    ElementsCollection<BodyItem> bodiesList();

    @Name("Отзывы")
    @FindBy("//div[contains(@class, 'catalog__section_classic')]")
    OpinionsBlock opinionsBlock();

    @Name("Галерея")
    @FindBy("//div[contains(@class, 'model-gallery')]")
    Gallery gallery();

    @Name("Описание кузова")
    @FindBy("//div[contains(@class, 'catalog-generation__about')]")
    VertisElement bodyAbout();

    @Name("Лого марки")
    @FindBy("//div[contains(@class, 'brand-info__logo')]")
    VertisElement markLogo();

    @Name("Описание марки")
    @FindBy("//div[contains(@class, 'brand-info')]")
    VertisElement markInfo();

    @Name("Подробнее")
    @FindBy("//span[contains(@class, 'brand-info__more')]")
    VertisElement moreInfoButton();

    @Name("Комплектации")
    @FindBy("//h2[contains(., 'Комплектации')]//..")
    VertisElement bodyComplectations();

    @Name("Комплектация")
    @FindBy("//tr[contains(@class, 'catalog-table__row_highlight')]")
    VertisElement bodyComplectation();

    @Name("Блок комплектаций")
    @FindBy("//div[contains(@class, 'catalog-table_packages')]")
    VertisElement packages();

    @Name("Список комплектаций")
    @FindBy("//tr[@class = 'catalog-table__row']")
    ElementsCollection<Complectation> complectationsList();

    @Name("Список модификаций")
    @FindBy("//tr[contains(@class, 'catalog-table__row_highlight')]//a[contains(@class, 'catalog-tab-item')]")
    ElementsCollection<VertisElement> modificationsList();

    @Name("Описание комплектации")
    @FindBy("//div[@class = 'catalog__content']")
    ComplectationDescription complectationDescription();

    @Name("Название комплектации")
    @FindBy("//div[@class = 'catalog__package-name']/h2")
    VertisElement complectationTitle();

    @Name("Название модификации")
    @FindBy("//div[contains(@class, 'catalog__column')]/h2")
    VertisElement modificationTitle();

    @Name("Источник данных")
    @FindBy("//div[contains(@class, 'catalog-provider-info')]")
    VertisElement providerInfo();

    @Name("Ссылка на листинг в описании модификации")
    @FindBy("//a[contains(@class, 'catalog__details-sales-item-v')]")
    VertisElement modificationListingUrl();

    @Name("Опция комплектации")
    @FindBy("//span[contains(@class, 'checkbox__text')]")
    ElementsCollection<VertisElement> optionsList();

    @Name("Пакет опций")
    @FindBy("//div[contains(@class, 'package-option i-bem')]")
    ElementsCollection<OptionPackage> optionPackagesList();

    @Name("Блок контролов")
    @FindBy("//span[contains(@class,'sorting__control')] | " +
            "//span[contains(@class, 'output-type-switcher')]")
    VertisElement controls();

    @Name("Переключение типа выдачи")
    @FindBy("//span[contains(@class,'output-type-switcher')]//span[contains(@class,'icon_type_{{ title }}')]")
    VertisElement viewTypeSwitcher(@Param("title") String title);

    @Name("Новинки")
    @FindBy("//div[contains(@class, 'catalog-newest_news')]")
    News news();

    @Name("Пресеты")
    @FindBy("//div[contains(@class, 'catalog-presets catalog__section')]")
    Presets presets();

    @Name("Блок «Классика»")
    @FindBy("//div[contains(@class, 'catalog__section_classic')]")
    ClassicsBlock classics();

    @Name("Калькулятор комплектации")
    @FindBy("//div[contains(@class, 'catalog-complectation-calculator ')]")
    VertisElement complectationCalculator();

    @Name("Плашка с калькулятором комплектации")
    @FindBy("//div[contains(@class, 'catalog-complectation-calculator ')]")
    VertisElement complectationCalculatorPanel();

    @Name("Электро баннер")
    @FindBy("//div[contains(@class, 'electro-banner')]")
    VertisElement electroBanner();

    @Step("Получаем комплектацию с индексом {i}")
    default Complectation getComplectation(int i) {
        return complectationsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем модификацию с индексом {i}")
    default VertisElement getModification(int i) {
        return modificationsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем опцию с индексом {i}")
    default VertisElement getOption(int i) {
        return optionsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем пакет опций с индексом {i}")
    default OptionPackage getOptionPackage(int i) {
        return optionPackagesList().should(hasSize(greaterThan(i))).get(i);
    }
}
