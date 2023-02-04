package ru.auto.tests.desktop.mobile.element.history;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface VinReport extends VertisElement, WithButton {

    String BLOCK_AUTORU_OFFERS = "block-autoru_offers";
    String BLOCK_SELL_TIME = "block-sell_time";
    String BLOCK_VEHICLE_PHOTOS = "block-vehicle_photos";

    @Name("Содержание")
    @FindBy(".//section[contains(@class, 'VinReportBase__block')]")
    ReportContent contentsBlock();

    @Name("Данные из ПТС")
    @FindBy(".//div[@id = 'block-pts_info']")
    VertisElement ptsBlock();

    @Name("Наличие ограничений")
    @FindBy(".//div[@id = 'block-constraints']")
    VertisElement constraintBlock();

    @Name("Нахождение в розыске")
    @FindBy(".//div[@id = 'block-wanted']")
    VertisElement wantedBlock();

    @Name("Нахождение в залоге")
    @FindBy(".//div[@id = 'block-pledge']")
    VertisElement pledgeBlock();

    @Name("Владельцы по ПТС")
    @FindBy(".//div[@id = 'block-pts_owners']")
    VertisElement ptsOwnersBlock();

    @Name("Работа в такси")
    @FindBy(".//div[@id = 'block-taxi']")
    VertisElement taxiBlock();

    @Name("Работа в каршеринге")
    @FindBy(".//div[@id = 'block-car_sharing']")
    VertisElement carSharingBlock();

    @Name("Участие в аукционах битых автомобилей")
    @FindBy(".//div[@id = 'block-total_auction']")
    VertisElement auctionsBlock();

    @Name("ДТП")
    @FindBy(".//div[@id = 'block-dtp']")
    VertisElement dtpBlock();

    @Name("Расчёты стоимости ремонта")
    @FindBy(".//div[@id = 'block-repair_calculations']")
    VinReportRepairCalculations repairCalculations();

    @Name("Техосмотры")
    @FindBy(".//div[@id = 'block-tech_inspection_block']")
    VertisElement techBlock();

    @Name("Отзывные кампании")
    @FindBy(".//div[@id = 'block-recalls']")
    VertisElement recallsBlock();

    @Name("Опции по VIN")
    @FindBy(".//div[@id = 'block-vehicle_options']")
    VertisElement vinEquipmentBlock();

    @Name("Страховые полисы")
    @FindBy(".//div[@id = 'block-insurances']")
    VertisElement insurancesBlock();

    @Name("Штрафы")
    @FindBy(".//div[@id = 'block-fines']")
    VertisElement finesBlock();

    @Name("Объявления на Авто.ру")
    @FindBy(".//div[@id = 'block-autoru_offers']")
    VertisElement autoruOffersBlock();

    @Name("История пробегов")
    @FindBy(".//div[@id = 'block-mileages_graph']")
    VertisElement mileagesBlock();

    @Name("История эксплуатации")
    @FindBy(".//div[@id = 'block-history']")
    VertisElement historyBlock();

    @Name("Оценка стоимости")
    @FindBy(".//div[@id = 'block-price_stats_graph']")
    VertisElement priceStatsBlock();

    @Name("Налог")
    @FindBy(".//div[@id = 'block-tax']")
    VertisElement taxBlock();

    @Name("Запись о готовности отчёта")
    @FindBy("//div[contains(@class, 'VinReportStatus')]")
    VertisElement status();

    @Name("Оценка истории")
    @FindBy(".//div[contains(@class, 'VinHistoryScore')]")
    VinReportScore score();

    @Name("Оценка истории")
    @FindBy(".//div[contains(@class, 'VinReportScoreHealth')]")
    VertisElement scoreHealth();

    @Name("Кнопка избранного")
    @FindBy(".//div[@class = 'ButtonFavoriteMobile']")
    VertisElement favoriteButton();

    @Name("Блок «{{ blockId }}»")
    @FindBy(".//div[@id = '{{ blockId }}']")
    VertisElement block(@Param("blockId") String blockId);

}
