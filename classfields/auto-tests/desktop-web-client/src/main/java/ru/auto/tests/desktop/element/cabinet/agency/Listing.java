package ru.auto.tests.desktop.element.cabinet.agency;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 19.09.18
 */
public interface Listing extends VertisElement {

    Pattern CLIENT_ID_REGEXP = Pattern.compile(".*\"clientId\":(.*?)([,}]).*");

    @Name("Код клиента")
    @FindBy(".//div[contains(@class, 'listing-new__column listing-new__column_type_code')] | " +
            ".//div[contains(@class, 'ClientsItemHeader__code')]")
    VertisElement code();

    @Name("Клиент")
    @FindBy(".//div[contains(@class, 'listing-new__column_type_client')]")
    VertisElement client();

    @Name("Активен до")
    @FindBy(".//div[contains(@class, 'listing-new__column_type_active')]")
    VertisElement paidTill();

    @Name("Статус")
    @FindBy(".//div[contains(@class, 'listing-new__column_type_status')] | " +
            ".//div[@class = 'ClientsItemHeader__status']")
    VertisElement status();

    @Name("Регистрация")
    @FindBy(".//div[contains(@class, 'listing-new__column_type_reg')]")
    VertisElement registration();

    @Name("Кнопка «{{ name }}»")
    @FindBy(".//div[@class = 'listing-new__column'][contains(., '{{ name }}')]")
    VertisElement button(@Param("name") String name);

    @Name("Автопополнение")
    @FindBy(".//div[contains(@class, 'listing-new__column_type_auto')]//span")
    VertisElement autoprolong();

    default String clientId() {
        Matcher matcher = CLIENT_ID_REGEXP.matcher(this.getAttribute("data-bem"));
        return matcher.find() ? matcher.group(1) : "";

    }
}
