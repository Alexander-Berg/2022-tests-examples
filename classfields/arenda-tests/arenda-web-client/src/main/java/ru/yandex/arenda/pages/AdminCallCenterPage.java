package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Button;
import ru.yandex.arenda.element.lk.admin.callcenter.CallCenterFlatSnippet;
import ru.yandex.arenda.element.lk.admin.callcenter.TenantInfo;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface AdminCallCenterPage extends BasePage {

    String FIND_BUTTON = "Найти";
    String ADDRESS_ID = "ADDRESS";
    String SEND_BUTTON = "Отправить заявку на просмотр";
    String TENANT_NAME_ID = "TENANT_NAME";
    String TENANT_PHONE_ID = "TENANT_PHONE";
    String ESTIMATED_RENT_DURATION_ID = "ESTIMATED_RENT_DURATION";
    String NUMBER_OF_ADULTS_ID = "NUMBER_OF_ADULTS";
    String NUMBER_OF_CHILDREN_ID = "NUMBER_OF_CHILDREN";
    String SHOWING_TYPE_ID = "SHOWING_TYPE";
    String ONLINE_SHOWING_DATE_ID = "ONLINE_SHOWING_DATE";
    String ONLINE_SHOWING_SLOT_ID = "ONLINE_SHOWING_SLOT";

    @Name("Форма колл-центра")
    @FindBy(".//div[contains(@class,'OutstaffCallCenterForm__container')]")
    AtlasWebElement callCenterForm();

    @Name("Элементы саджеста")
    @FindBy(".//ul[contains(@class,'SuggestList__list')]/li")
    ElementsCollection<AtlasWebElement> suggestItems();

    @Name("Сниппеты квартир")
    @FindBy(".//label[contains(@class,'OutstaffCallCenterForm__radioOption')]")
    ElementsCollection<CallCenterFlatSnippet> flatSnippet();

    default CallCenterFlatSnippet firstSnippet() {
        return flatSnippet().waitUntil(hasSize(greaterThan(0))).get(0);
    }

    @Name("Форма информации о жильце")
    @FindBy(".//div[contains(@class,'OutstaffCallCenterForm__tenantInfoWrapper')]")
    TenantInfo tenantInfo();

    @Name("Ошибка в поле «{{ value }}» на странице персональных данных")
    @FindBy("//div[contains(@class,'withManagerFormClasses__group')][.//*[@id='{{ value }}']]" +
            "//div[contains(@class, 'InputDescription__isInvalid')]/span")
    AtlasWebElement invalidInputCallCenter(@Param("value") String value);

    @Name("Попап принятой заявки")
    @FindBy(".//div[@data-test='ArendaModalContent']")
    Button successModal();
}
