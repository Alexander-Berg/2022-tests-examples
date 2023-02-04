package ru.auto.tests.desktop.mobile.element.poffer;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.WithInput;

public interface PhotoBlock extends VertisElement, WithInput {

    @Name("Список загруженных фото")
    @FindBy(".//div[@class = 'MdsPhoto__item' and not(descendant::*[@class='MdsPhoto__loader'])]")
    ElementsCollection<PhotoItem> photos();

}
