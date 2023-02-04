package ru.yandex.realty.element.newbuildingsite;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers;
import ru.yandex.realty.element.Link;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.utils.UtilsWeb.PHONE_PATTERN_BRACKETS;
import static ru.yandex.realty.utils.UtilsWeb.makePhoneFormatted;

public interface CardPhoneNb extends Link {

    String SHOW_PHONE = "Показать телефон";

    @Name("Кнопка показать телефон")
    @FindBy(".//button[contains(@class,'CardPhone')]")
    AtlasWebElement showPhoneButton();

    @Name("Телефоны")
    @FindBy(".//span[contains(@class,'CardPhone__content')]")
    ElementsCollection<AtlasWebElement> phones();

    default void showPhoneClick() {
        showPhoneButton().waitUntil(hasText(SHOW_PHONE)).click();
        showPhoneButton().should(allOf(hasClass(containsString("CardPhone")), hasClass(containsString("_shown"))));
    }

    default void shouldSeePhone(String phone) {
        phones().should(hasSize(1)).get(0)
                .should(WrapsElementMatchers.hasText(makePhoneFormatted(phone, PHONE_PATTERN_BRACKETS)));
    }
}
