package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SuspiciousActivityContacts extends VertisElement {

    String SUSPICIOUS_ACTIVITY_TITLE = "Подозрительная активность";
    String SUSPICIOUS_ACTIVITY_TEXT = "Заметили в вашем аккаунте странные действия. Обновите страницу и попробуйте ещё " +
            "раз или напишите в службу поддержки Яндекс.Паспорта.";


    @Name("Тайтл")
    @FindBy(".//span[contains(@class, 'Suspicious__title')]")
    VertisElement title();

    @Name("Текст")
    @FindBy(".//span[contains(@class, 'Suspicious__text')]")
    VertisElement text();

    @Name("Кнопка «Узнать больше»")
    @FindBy(".//a[contains(@class, 'Suspicious__button')]")
    VertisElement knowMore();

}
