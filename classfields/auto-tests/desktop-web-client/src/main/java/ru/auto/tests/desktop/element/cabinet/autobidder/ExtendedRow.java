package ru.auto.tests.desktop.element.cabinet.autobidder;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface ExtendedRow extends VertisElement, WithInput, WithButton {

    String INPUT_CAMPAIGN_NAME = "Введите название кампании";
    String DAYS_FROM = "Дней, от";
    String TO = "до";
    String MAX_CALL_PRICE = "Макс. стоимость звонка";
    String TARGETED_CALLS_PER_DAY = "Целевых звонков в день на объявление";
    String ALL_OFFERS = "Все объявления";
    String MINIMAL_ATTENTION = "Минимальное внимание";
    String LOW_ATTENTION = "Низкое внимание";
    String MIDDLE_ATTENTION = "Среднее внимание";
    String HIGH_ATTENTION = "Высокое внимание";
    String MAXIMUM_ATTENTION = "Максимальное внимание";
    String LISTING_PLACEHOLDER = "Установите состояние и параметры объявлений и посмотрите как распределится интерес";
    String DAYS_IN_WAREHOUSE_COLUMN = "Дней на\u00a0складе";
    String DAYS_WITHOUT_CALLS_COLUMN = "Дней без\u00a0звонков";
    String ATTENTION_INFO_COLUMN = "Прогноз интереса в\u00a0аукционе";
    String FORECAST_COLUMN = "Прогноз ставки";
    String DEFICIT_COLUMN = "Не\u00a0хватает до\u00a0макс. интереса";
    String DAYS_IN_WAREHOUSE_TOOLTIP = "Кол-во дней с момента размещения объявления";
    String DAYS_WITHOUT_CALLS_TOOLTIP = "Кол-во дней в которые непрерывно не было звонков, без учета текущего дня";
    String ATTENTION_INFO_TOOLTIP = "Интерес пользователей к объявлению по прогнозируемой ставке";
    String FORECAST_TOOLTIP = "Прогнозируемая ставка в аукционе, которая будет назначена атостратегией";
    String DEFICIT_TOOLTIP = "Сумма, на которую нужно увеличить максимальную стоимость звонка, чтобы объявление получило максимальный процент интереса пользователей";
    String RUN_CAMPAIGN = "Запустить кампанию";
    String SAVE_CAMPAIGN = "Сохранить кампанию";

    @Name("Блок «Нет целевых звонков»")
    @FindBy(".//div[@class = 'AuctionUsedAutobidderCondition__col'][./div[. = 'Нет целевых звонков']]")
    WithInput noTargetedCallsBlock();

    @Name("Блок «На складе»")
    @FindBy(".//div[@class = 'AuctionUsedAutobidderCondition__col'][./div[. = 'На складе']]")
    WithInput inWarehouseBlock();

    @Name("Блок фильтров")
    @FindBy(".//div[@class = 'AuctionUsedAutobidderFilters']")
    Filters filters();

    @Name("Блок «Оптимизация бюджета»")
    @FindBy(".//div[@id = 'autobidder_money']")
    WithInput budgetBlock();

    @Name("Сегмент внимания «{{ segment }}»")
    @FindBy(".//div[contains(@class, 'SegmentFiltersItem')][.//span[. = '{{ segment }}']]")
    AttentionSegment attentionSegment(@Param("segment") String segment);

    @Name("Список снипетов")
    @FindBy(".//div[contains(@class, 'OfferSnippetAutoBidderListing__listItem')]")
    ElementsCollection<Snippet> snippets();

    @Name("Заглушка листинга снипетов")
    @FindBy(".//div[contains(@class, '_listingPlaceholder')]")
    VertisElement listingPlaceholder();

    @Name("Иконка с вопросом в столбце «{{ title }}» шапки таблицы")
    @FindBy(".//div[contains(@class, '_headerCol')][. = '{{ title }}']//*[contains(@class, '_headerTooltipIcon')]")
    VertisElement headerColumnTooltipIcon(@Param("title") String title);

}
