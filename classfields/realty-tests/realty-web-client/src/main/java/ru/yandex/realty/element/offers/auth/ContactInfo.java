package ru.yandex.realty.element.offers.auth;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.offers.FeatureField;
import ru.yandex.realty.element.offers.InnerContent;

import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;

/**
 * Created by ivanvan on 31.07.17.
 */
public interface ContactInfo extends InnerContent, Link, Button {

    String YOUR_PHONE = "Ваш телефон";
    String EDIT_CONTACTS = "Редактировать контакты";
    String HOW_ADDRESS_SECTION = "Как обращаться";
    String ADD_MANAGER = "Добавить менеджера";

    @Name("Блок со свойством «{{ value }}»")
    @FindBy(".//div[contains(@class, 'form-row') and contains(.,'{{ value }}')]")
    FeatureField featureField(@Param("value") String value);

    @Name("ОГРН/ОГРНИП")
    @FindBy("//div[@class='ogrn-panel']//input")
    AtlasWebElement ogrn();

    @Step("Вводим «{field}» -> «{text}»")
    default void input(String field, String text) {
        featureField(field).input().sendKeys(text);
        featureField(field).input().should(hasValue(not("")));
    }
}
