package ru.yandex.general.mobile.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.Badge;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface MyOfferSnippet extends VertisElement, Checkbox, Link, Button {

    String RAISE_UP_FOR = "Поднять в топ за";
    String KNOW_REASON = "Узнать причину";
    String KNOW_REASONS = "Узнать причины";
    String REMOVE_FROM_PUBLICATION = "Снять с публикации";
    String DELETE = "Удалить";

    @Name("Блок статистики")
    @FindBy(".//div[contains(@class, 'mediaStatistics')]")
    Statistics statistics();

    @Name("Бейдж")
    @FindBy(".//span[contains(@class, 'Badge')]")
    Badge badge();

    @Name("Кнопка действий")
    @FindBy(".//button[contains(@class, 'OffersShowMore')]")
    VertisElement offerAction();

    @Name("Тайтл")
    @FindBy(".//span[contains(@class, 'Snippet__title')]")
    VertisElement title();

    @Name("Цена")
    @FindBy(".//span[contains(@class, '_price_')]")
    Link price();

    @Name("Текст тоста")
    @FindBy(".//span[contains(@class, 'Toast__text')]")
    VertisElement toastText();

    @Name("Блок «Ещё актуально?»")
    @FindBy(".//div[contains(@class, 'ActiveSnippet__expired')]")
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

}
