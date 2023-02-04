package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;


public interface Share extends VertisElement {

    @Name("Ссылка на фейсбук")
    @FindBy(".//li/a[@title = 'Facebook']")
    VertisElement fb();

    @Name("Ссылка на вконтакте")
    @FindBy(".//li/a[@title = 'ВКонтакте']")
    VertisElement vk();

    @Name("Ссылка на твиттер")
    @FindBy(".//li/a[@title = 'Twitter']")
    VertisElement twitter();

    @Name("Ссылка на одноклассники")
    @FindBy(".//li/a[@title = 'Одноклассники']")
    VertisElement ok();

}
