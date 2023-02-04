package ru.auto.tests.desktop.element.history;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithFullScreenGallery;
import ru.auto.tests.desktop.component.WithGallery;

public interface VinReport extends VertisElement, WithButton, WithGallery, WithFullScreenGallery {

    String BLOCK_AUTORU_OFFERS = "block-autoru_offers";
    String BLOCK_SELL_TIME = "block-sell_time";
    String BLOCK_VEHICLE_PHOTOS = "block-vehicle_photos";

    @Name("Запись о готовности отчёта")
    @FindBy(".//div[contains(@class, 'VinReportStatus')]")
    VertisElement status();

    @Name("Блок оценки истории")
    @FindBy(".//div[contains(@class, 'VinHistoryScore')]")
    VinReportScore score();

    @Name("Содержание отчёта")
    @FindBy(".//section[contains(@class, 'VinReportContents')]")
    ReportContent contents();

    @Name("Оценка истории")
    @FindBy(".//div[contains(@class, 'VinReportScoreHealth')]")
    VertisElement scoreHealth();

    @Name("Расчёты стоимости ремонта")
    @FindBy(".//div[@id = 'block-repair_calculations']")
    VinReportRepairCalculations repairCalculations();

    @Name("Оффер")
    @FindBy("//div[@class = 'VinReportOffer']")
    VinOffer offer();

    @Name("Блок «{{ blockId }}»")
    @FindBy(".//div[@id = '{{ blockId }}']")
    VertisElement block(@Param("blockId") String blockId);

}
