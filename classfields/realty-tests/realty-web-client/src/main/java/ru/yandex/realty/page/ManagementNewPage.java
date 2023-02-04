package ru.yandex.realty.page;


import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.DomikPopup;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.Tab;
import ru.yandex.realty.element.management.AgencyOffer;
import ru.yandex.realty.element.management.AgencyOfferFilters;
import ru.yandex.realty.element.management.EditPopupRow;
import ru.yandex.realty.element.management.Feeds;
import ru.yandex.realty.element.management.HeaderAgencyOffers;
import ru.yandex.realty.element.management.NewOfferBlock;
import ru.yandex.realty.element.management.SettingsContent;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.element.saleads.popup.PublishControlPopup;

import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface ManagementNewPage extends WebPage, DomikPopup, BasePage, Link, Tab, Button, InputField {

    String RENEW = "Продлить размещение";
    String EDIT = "Изменить";
    String REDACT = "Редактировать";
    String CANCEL_DELETE = "Отменить удаление";
    String PUBLISH = "Опубликовать";
    String REMOVE_FROM_PUBLICATION = "Снять с публикации";
    String DELETE = "Удалить";
    String OTHER_REASON = "Другая причина";
    String ACTIVATE = "Активировать";
    String CHANGE = "Изменить";
    String STATISTIC = "Статистика";
    String NEW_OFFER = "Новое объявление";
    String ADJUST_XML = "Настроить XML-загрузку";
    String MY_OFFERS = "Мои объявления";
    String REFILL_BUDGET = "Пополнить бюджет";
    String REFILL_SUM = "Сумма пополнения от";
    String RUN_OUT_OF_BUDGET = "Закончился бюджет";
    String MORE_FILTERS = "Ещё фильтры";
    String BUDGET_PROBLEM = "Проблема с доступом к бюджету";
    String REFILL = "Пополнить";
    String ADD_PAYER = "Добавить плательщика";

    @Name("Паранжа забаненного пользователя")
    @FindBy(".//div[contains(@class,'page__paranja-wrapper')]")
    AtlasWebElement lkParanja();

    @Name("Список офферов")
    @FindBy("//div[contains(@class, 'offers-list__offer')]")
    ElementsCollection<NewOfferBlock> offersList();

    @Name("Список офферов колл центра")
    @FindBy("//div[contains(@class, 'callcenter-offer-list__offer')]")
    ElementsCollection<NewOfferBlock> ccOffersList();

    @Name("Список офферов агентства")
    @FindBy("//div[@class='agency-offer']")
    ElementsCollection<AgencyOffer> agencyOffersList();

    @Name("Кнопка «Показать еще»")
    @FindBy("//div[contains(@class, 'load-more-offers')]/button")
    AtlasWebElement showMoreButton();

    @Name("Блок оферров")
    @FindBy("//div[contains(@class, 'offers-new-list')]")
    AtlasWebElement offersBlock();

    @Name("Страница личного кабинета, кроме заголовков")
    @FindBy("//div[@id='root']")
    AtlasWebElement root();

    @Name("Системное сообщение «{{ value }}»")
    @FindBy("//div[contains(@class,'Notification__notification') and contains(.,'{{ value }}')]")
    Button notification(@Param("value") String value);

    @Name("Фильтры оферов для агентства")
    @FindBy("//div[@class='agency-list-filters']")
    AgencyOfferFilters agentOfferFilters();

    @Name("Хедер списка офферов агентства")
    @FindBy("//div[contains(@class, 'agency-offers-list__head')]")
    HeaderAgencyOffers headerAgentOffers();

    @Name("Попап «Причина снятия объявления»")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    PublishControlPopup publishControlPopup();

    @Name("Панель управления выделенными офферами")
    @FindBy(".//div[contains(@class,'offers-action-panel__content')]")
    Button offersControlPanel();

    @Name("Попап дополнительных действий над объявлениями")
    @FindBy("//div[contains(@class, 'Popup') and contains(@class, 'visible')]")
    Button actionsPopup();

    @Name("Контент таба")
    @FindBy("//div[@class='page__content page__content_type_lk']")
    AtlasWebElement content();

    @Name("Фиды")
    @FindBy("//div[@class='feeds']")
    Feeds feeds();

    @Name("Контент контактов")
    @FindBy("//div[contains(@class,'Settings__settings')]")
    SettingsContent settingsContent();

    @Name("Блок -> «{{ value }}»")
    @FindBy("//div[contains(@class,'contacts-form-new__section') and contains(., '{{ value }}')]")
    EditPopupRow userInfo(@Param("value") String value);

    default NewOfferBlock offer(int i) {
        return offersList().should(hasSize(greaterThan(i))).get(i);
    }

    default NewOfferBlock ccOffer(int i) {
        return ccOffersList().should(hasSize(greaterThan(i))).get(i);
    }

    default NewOfferBlock offerById(String id) {
        return offersList().waitUntil(hasSize(greaterThan(0))).stream()
                .filter(offer -> offer.offerInfo().offerLink().getAttribute("href").contains(id)).findFirst()
                .orElseThrow(() -> new RuntimeException(format("Не нашли оффер с id: %s", id)));
    }

    default AgencyOffer agencyOffer(int i) {
        return agencyOffersList().should(hasSize(greaterThan(i))).get(i);
    }
}
