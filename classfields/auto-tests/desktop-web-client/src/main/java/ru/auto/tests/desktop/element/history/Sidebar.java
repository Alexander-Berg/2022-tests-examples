package ru.auto.tests.desktop.element.history;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface Sidebar extends VertisElement, WithButton, WithInput {

    @Name("Промо покупки пакета отчётов")
    @FindBy("//div[contains(@class, 'HistoryByVinPackagePromoMini')]")
    TopBlockVinPackagePromo vinPackagePromo();

    @Name("Остаток отчётов в пакете")
    @FindBy(".//div[contains(@class, 'VinCheckSnippetMini__quota')]")
    VertisElement packageQuotaInfo();

    @Name("Ошибка")
    @FindBy(".//div[@class = 'VinCheckSnippetMini__error']")
    VinError error();
}
