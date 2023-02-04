package ru.auto.tests.desktop.mobile.element.history;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ReportContent extends VertisElement {

    String AVERAGE_SELL_TIME = "Среднее время продажи";
    String HD_PHOTO = "HD фото";

    @Name("Блок «{{ blockTitle }}» в содержании отчета")
    @FindBy(".//li[.//div[contains(@class, '_itemTitle')]/a[contains(., '{{ blockTitle }}')]]")
    ReportContentBlock block(@Param("blockTitle") String blockTitle);

}
