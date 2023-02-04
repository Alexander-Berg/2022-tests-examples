package ru.auto.tests.desktop.mobile.element.electro;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PostsSection extends Section {

    @Name("Карусель постов")
    @FindBy(".//div[contains(@class, '_carouselItemWrapper')]")
    ElementsCollection<VertisElement> posts();

}
