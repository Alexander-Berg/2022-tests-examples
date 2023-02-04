package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface OnboardingPopup extends VertisElement, WithButton {

    String ARCHIVE_POPUP = "Это новый раздел. Здесь мы показываем уже проданные объявления. " +
            "Смотрите, как и за какую цену продавались похожие авто, и какие услуги применяли.\nПерейти в архив";
    String GROUP_OPERATIONS_POPUP = "Групповые операции\nНапоминаем: вы можете выполнять действия сразу с несколькими " +
            "объявлениями на странице. Например, применять услуги, снимать с продажи или активировать. " +
            "Это здорово экономит время!\nЯ в курсе\nОк, понятно";
    String OFFER_FILTER_POPUP = "Фильтр объявлений\nБыстро находите объявления по нужным вам характеристикам. " +
            "Самые популярные праметры всегда перед глазами, все остальные скрыты под кнопкой «Все параметры»\n" +
            "Смотреть все параметры";

    String GO_TO_ARCHIVE = "Перейти в архив";
    String I_KNOW = "Я в курсе";
    String OK_CLEARLY = "Ок, понятно";
    String SEE_ALL_PARAMETERS = "Смотреть все параметры";

    @Name("Кнопка «Закрыть»")
    @FindBy(".//div[contains(@class, 'OnboardingPopup__closer')]")
    VertisElement close();

}
