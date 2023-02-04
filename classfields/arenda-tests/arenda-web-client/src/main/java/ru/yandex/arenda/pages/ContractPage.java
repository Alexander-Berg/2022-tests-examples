package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Button;
import ru.yandex.arenda.element.common.ElementById;
import ru.yandex.arenda.element.common.Input;
import ru.yandex.arenda.element.common.Select;

public interface ContractPage extends BasePage, ElementById, Button, Input, Select {

    String CONTRACTS_BUTTON = "Договоры";
    String CONTRACT_BUTTON = "Договор";
    String CHANGE_STATUS = "Смена статуса";
    String INSURANCE_POLICY = "Страховой полис";

    String ADD_CONTRACT_BUTTON = "Добавить договор";
    String ADD_SIGN = "Подписать";
    String SAVE_CHANGE_STATUS = "Изменить статус";
    String DENY = "Отменить";
    String SAVE_BUTTON = "Сохранить";

    String YES_BUTTON = "Да";

    String RENT_AMOUNT_ID = "RENT_AMOUNT";
    String STATUS_ID = "STATUS";
    String RENT_START_DATE_ID = "RENT_START_DATE";
    String RENT_PAYMENT_DAY_OF_MONTH_ID = "RENT_PAYMENT_DAY_OF_MONTH";
    String CONTRACT_ID_ID = "CONTRACT_ID";
    String RENT_CALCULATED_OWNER_AMOUNT_ID = "RENT_CALCULATED_OWNER_AMOUNT";
    String RENT_CALCULATED_TENANT_AMOUNT_ID = "RENT_CALCULATED_TENANT_AMOUNT";

    String INSURANCE_POLICY_AMOUNT_ID = "INSURANCE_POLICY_AMOUNT";
    String INSURANCE_POLICY_DATE_ID = "INSURANCE_POLICY_DATE";
    String INSURANCE_POLICY_ID_ID = "INSURANCE_POLICY_ID";

    String OWNER_NAME_ID = "OWNER_NAME";
    String OWNER_SURNAME_ID = "OWNER_SURNAME";
    String OWNER_PATRONYMIC_ID = "OWNER_PATRONYMIC";
    String OWNER_PHONE_ID = "OWNER_PHONE";
    String OWNER_EMAIL_ID = "OWNER_EMAIL";
    String OWNER_BANK_ACCOUNT_NUMBER_ID = "OWNER_BANK_ACCOUNT_NUMBER";
    String OWNER_BANK_ACCOUNT_BIK_ID = "OWNER_BANK_ACCOUNT_BIK";
    String OWNER_INN_ID = "OWNER_INN";

    String TENANT_USER_ID_ID = "TENANT_USER_ID";
    String TENANT_NAME_ID = "TENANT_NAME";
    String TENANT_SURNAME_ID = "TENANT_SURNAME";
    String TENANT_PATRONYMIC_ID = "TENANT_PATRONYMIC";
    String TENANT_PHONE_ID = "TENANT_PHONE";
    String TENANT_EMAIL_ID = "TENANT_EMAIL";

    String HOUSING_AND_COMMUNAL_SERVICES_COMMENT_ID = "HOUSING_AND_COMMUNAL_SERVICES_COMMENT";
    String PAYMENT_COMMENT_ID = "PAYMENT_COMMENT";

    String POLICY_DATE_ID = "POLICY_DATE";
    String POLICY_NUMBER = "POLICY_NUMBER";

    String NEW_STATUS = "NEW_STATUS";
    String RESPONSIBLE_MANAGER = "RESPONSIBLE_MANAGER";

    @Name("Ошибка в поле «{{ value }}» на странице договора")
    @FindBy("//div[contains(@class,'withManagerFormClasses__group')][.//*[@id='{{ value }}']]" +
            "//div[contains(@class, 'InputDescription__isInvalid')]/span")
    AtlasWebElement invalidInputFlatPAge(@Param("value") String value);

    @Name("Элементы саджеста адреса")
    @FindBy(".//ul/li[contains(@class,'SuggestList__item')]")
    ElementsCollection<AtlasWebElement> suggestList();

    @Name("Попап диалога о подписании")
    @FindBy(".//div[contains(@class,'Modal_visible')]")
    Button signPopup();

    @Name("Чекбокс «Полис запрошен»")
    @FindBy(".//label[contains(@class,'withManagerFormClasses__input')]")
    AtlasWebElement policyRequired();

    @Name("Селектор кандидата")
    @FindBy(".//select[@id='TENANT_USER_ID']")
    AtlasWebElement tenantSelector();

    @Name("Опция селектора value=«{{ value }}»")
    @FindBy(".//select[@id='TENANT_USER_ID']/option[@value='{{ value }}']")
    AtlasWebElement tenantOption(@Param("value") String value);
}
