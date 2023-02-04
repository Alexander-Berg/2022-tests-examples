package ru.auto.tests.commons.extension.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.auto.tests.commons.extension.interfaces.ListElement;

public interface VertisCollection<E> extends ElementsCollection<E> {

    @ListElement
    E element(int i);

}
