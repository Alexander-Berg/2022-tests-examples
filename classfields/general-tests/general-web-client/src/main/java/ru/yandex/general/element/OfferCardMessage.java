package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.mobile.element.Link;

public interface OfferCardMessage extends VertisElement, Link {

    String SOLD_TITLE_OWNER = "Продано";
    String SOLD_TITLE_BUYER = "Товар продан";
    String SOLD_TEXT_OWNER = "Поздравляем с успешной сделкой! Ваше объявление скрыто из поиска. Может, продадим что-нибудь ещё?";
    String SOLD_TEXT_BUYER = "Этот товар больше не продается, но у нас почти наверняка есть что-то похожее.";
    String ENDED_TITLE = "Завершено";
    String ENDED_TEXT = "Ваше объявление больше никто не видит: вы сняли его с публикации или закончился срок размещения.";
    String DONE_TITLE = "Готово";
    String DONE_TEXT = "Спасибо! Скоро опубликуем ваше объявление.";
    String SHOW_MORE = "Показать ещё";
    String COLLAPSE = "Скрыть";

    @Name("Тайтл")
    @FindBy(".//div[contains(@class,'_title_')]")
    VertisElement title();

    @Name("Текст сообщения")
    @FindBy(".//p")
    VertisElement text();

    @Name("Список причин бана")
    @FindBy(".//li[contains(@class, 'BanMessage__reason')]")
    ElementsCollection<BanReason> banReasons();

    @Name("Развернуть/свернуть")
    @FindBy(".//div[contains(@class, '_btn')]")
    VertisElement actionButton();

}
