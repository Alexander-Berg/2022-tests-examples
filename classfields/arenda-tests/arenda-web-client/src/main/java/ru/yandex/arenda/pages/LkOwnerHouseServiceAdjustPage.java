package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Button;
import ru.yandex.arenda.element.common.Label;

public interface LkOwnerHouseServiceAdjustPage extends BasePage {

    String BY_MYSELF_LABEL = "Я\u00a0сам";
    String TENANT_LABEL = "Жилец";
    String YES_LABEL = "Да";
    String NO_LABEL = "Нет";
    String METERS_DATA = "Показания счётчиков";
    String SAVE_SETTINGS = "Сохранить настройки";
    String SEND_TO_TENANT_BUTTON = "Отправить жильцу";
    String CANCEL_BUTTON = "Отмена";
    String SEND_BUTTON = "Отправить";
    String TENANT_REFUND_PAYMENTS_DESCRIPTION_ID = "TENANT_REFUND_PAYMENTS_DESCRIPTION";
    String TENANT_REFUND_PAYMENT_AMOUNT_ID = "TENANT_REFUND_PAYMENT_AMOUNT";
    String PAID_BY_TENANT_HOUSE_SERVICES_ID = "PAID_BY_TENANT_HOUSE_SERVICES";
    String PAID_BY_TENANT_AMOUNT_ID = "PAID_BY_TENANT_AMOUNT";
    String PAYMENT_DETAILS_ID = "PAYMENT_DETAILS";
    String PAYMENT_AMOUNT_ID = "PAYMENT_AMOUNT";

    @Name("Хотите, чтобы жилец возмещал вам деньги за коммуналку и/или другие услуги?")
    @FindBy(".//span[@id = 'SHOULD_TENANT_REFUND']")
    Label tenantRefundBlock();

    @Name("Есть услуги, которые жилец должен оплачивать самостоятельно?")
    @FindBy(".//span[@id = 'HAS_SERVICES_PAID_BY_TENANT']")
    Label tenantPaidByTenantBlock();

    @Name("Ошибка «{{ value }}»")
    @FindBy(".//span[contains(@class, 'InputDescription__description') and contains(.,'{{ value }}')]")
    AtlasWebElement errorDescription(@Param("value") String value);

    @Name("Попап «Вы уверены, что заполнили все данные»")
    @FindBy(".//div[@data-test = 'ArendaModalContent']")
    Button sendTenantPopup();

    @Name("Сообщение")
    @FindBy(".//div[contains(@class,'UserMessage__temp')]")
    AtlasWebElement tempMessage();
}
