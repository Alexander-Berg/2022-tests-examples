package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow;
import ru.auto.tests.desktop.element.cabinet.autobidder.PromoBlock;
import ru.auto.tests.desktop.element.cabinet.autobidder.Row;

public interface AutobidderPage extends BasePage {

    String CREATE_FIRST_CAMPAIGN = "Создать первую кампанию";
    String CREATE_CAMPAIGN = "Создать кампанию";

    String DESCRIPTION_PROMO_BLOCK = "Автостратегии в легковых с пробегом\nИнструмент для автоматического продвижения " +
            "авто с пробегом с помощью аукциона стоимости звонков.\nАвтостратегия поможет получить максимальный " +
            "процент интереса пользователей по минимально возможной цене. Без вашего участия и строго в рамках " +
            "бюджета\nСоздать первую кампанию\nКак это работает\n1. Создание\nВы создаёте рекламную кампанию, " +
            "указываете срок её работы и выбираете автомобили, к которым будет применяться автостратегия\n2. " +
            "Оптимизация\nОпределяете максимальную ставку за звонок: автостратегия, никогда не превысит это " +
            "значение\n3. Оценка\nНаблюдаете, как распределится интерес пользователей к вашим объявлениям в " +
            "зависимости от максимальной стоимости звонка\n4. Результат\nСтратегия будет автоматически подбирать " +
            "минимальную ставку для того, чтобы объявление получило наибольший процент интереса. Вам остается только " +
            "получать звонки";

    String ADVANTAGE_PROMO_BLOCK = "Преимущества автостратегии\nБесплатно\nАвтостратерия — это полностью бесплатный " +
            "инструмент. Вы платите только за целевые звонки по ставке в аукционе\nБез лишний усилий\nЗапускаете " +
            "кампанию на выбранные объявления — и больше не тратите своё время на корректировку ставок и анализ " +
            "рынка\nБольше звонков\nПроцент внимания к каждому объявлению всегда на высоте. А значит, вы получаете " +
            "больше трафика\nЭкономия вашего бюджета\nАвтостратегия автоматически понизит стоимость звонка, если " +
            "максимум внимания можно привлечь дешевле\nВнимание на все объявления\nВ зависимости от ваших задач " +
            "звонки могут распределяться равномерно по всем объявлениям. Самые популярные объявления больше не " +
            "заберут на себя весь бюджет\nБыстрая реакция\nАвтостратегия сразу реагирует на изменения в аукционе. " +
            "Ставки меняются каждую секунду, но автостратегия всегда успевает подстраиваться\nСоздать первую кампанию";

    @Name("Свернутые ряды в таблице")
    @FindBy("//tr[contains(@class, 'AuctionUsedAutobidderTableRow') and not(contains(@class, '_extended'))]")
    ElementsCollection<Row> rows();

    @Name("Развёрнутый ряд")
    @FindBy("//tr[contains(@class, 'AuctionUsedAutobidderTableRow_extended')]")
    ExtendedRow extendedRow();

    @Name("Блок описания на промо странице")
    @FindBy("//div[@class = 'AuctionUsedAutobidderPromo__background']")
    PromoBlock promoDescription();

    @Name("Блок преимуществ на промо странице")
    @FindBy("//section[@class = 'AuctionUsedAutobidderPromo__advantage']")
    PromoBlock promoAdvantageSection();

    @Name("Тултип по ховеру названия столбца таблицы")
    @FindBy("//div[contains(@class, 'Popup_visible')]//div[contains(@class, '_headerTooltipContent')]")
    VertisElement tableHeaderTooltip();

}
