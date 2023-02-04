package ru.yandex.general.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface MyOfferSnippet extends VertisElement, Popup, Link, Image {

    String SOLD = "Товар продан";
    String ENDED = "Завершено";
    String RAISE_UP = "Поднять в топ за";
    String EDIT = "Редактировать";
    String ACTIVATE = "Активировать";
    String CHAT_SUPPORT = "Написать в поддержку";
    String KNOW_REASON = "Узнать причину";

    @Name("Кнопка действий")
    @FindBy(".//button[contains(@class, 'PersonalOffersActions')]")
    VertisElement offerAction();

    @Name("Блок статистики")
    @FindBy(".//a[contains(@class, 'Snippet__stats')]")
    Statistics statistics();

    @Name("Бейдж неактивного сниппета")
    @FindBy(".//span[contains(@class, 'InactiveSnippet__badge')]")
    VertisElement inactiveBage();

    @Name("Цена")
    @FindBy(".//span[contains(@class, '_price_')]")
    Link price();

    @Name("Текст статуса")
    @FindBy(".//span[contains(@class, 'StatusText__container')]")
    VertisElement statusText();

    @Name("Бейдж цвета «{{ value }}»")
    @FindBy(".//div[contains(@class, '_status_')]/span[contains(@class, 'Badge__{{ value }}')]")
    VertisElement badge(@Param("value") String value);

    @Name("Блок «Ещё актуально?»")
    @FindBy(".//div[contains(@class, 'ActiveSnippet__actualize')]")
    ActualizationBlock actualizationBlock();

    @Step("Получаем offerId")
    default String getOfferId() {
        String offerUrl = link().getAttribute("href");
        Pattern pattern = Pattern.compile("\\/offer\\/(.+)\\/");
        Matcher matcher = pattern.matcher(offerUrl);
        matcher.find();
        return matcher.group(1);
    }

    default String getUrl() {
        return link().getAttribute("href");
    }

    default String getEditUrl() {
        return link(EDIT).getAttribute("href");
    }

}
