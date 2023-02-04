package ru.auto.tests.desktop.element.poffer;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface UserVas extends VertisElement {

    @Name("Сниппет «{{ text }}»")
    @FindBy(".//div[contains(@class, 'vas-pack ') and .= '{{ text }}'] | " +
            ".//div[contains(@class, 'VasFormUserTabs__tab') and .= '{{ text }}'] | " +
            ".//div[contains(@class, 'VasFormUserSnippet ') and " +
            ".//div[contains(@class, 'VasFormUserSnippet__title') and .= '{{ text }}']]")
    UserVasSnippet snippet(@Param("text") String text);

    @Name("Список сниппетов услуг")
    @FindBy(".//div[contains(@class, 'vas-pack ')] | " +
            ".//div[contains(@class, 'VasFormUserTabs__tab')] | " +
            "//div[contains(@class, 'VasFormUserSnippet ')]")
    ElementsCollection<UserVasSnippet> snippetsList();

    @Step("Получаем сниппет с индексом {i}")
    default UserVasSnippet getSnippet(int i) {
        return snippetsList().should(hasSize(greaterThan(i))).get(i);
    }
}