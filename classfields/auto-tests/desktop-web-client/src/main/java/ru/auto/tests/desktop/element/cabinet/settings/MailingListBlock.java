package ru.auto.tests.desktop.element.cabinet.settings;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 29.08.18
 */
public interface MailingListBlock extends VertisElement {

    @Name("Чекбокс")
    @FindBy("./label[contains(@class, 'Checkbox')]")
    VertisElement checkbox();

    @Name("Почтовый адрес")
    @FindBy(".//div[@class = 'SettingsSubscription__controls']")
    ElementsCollection<EmailBlock> emailBlocks();

    default EmailBlock lastEmailBlock() {
        int size = emailBlocks().should(not(empty())).size();
        return emailBlocks().get(size - 1);
    }
}
