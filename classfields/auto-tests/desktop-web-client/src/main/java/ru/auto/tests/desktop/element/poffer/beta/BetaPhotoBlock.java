package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.mobile.element.poffer.PhotoItem;

public interface BetaPhotoBlock extends VertisElement, WithButton, WithInput, WithCheckbox {

    @Name("Список загруженных фото")
    @FindBy(".//div[@class = 'MdsPhoto__item' and not(descendant::*[@class='MdsPhoto__loader'])]")
    ElementsCollection<PhotoItem> photos();

}
