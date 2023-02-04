package ru.yandex.general.element;

import ru.auto.tests.commons.extension.element.VertisElement;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

public interface Meta extends VertisElement {

    String CONTENT = "content";

    default void shouldHasContent(String content) {
        should(hasAttribute(CONTENT, content));
    }

    default void shouldHasContent(int content) {
        should(hasAttribute(CONTENT, String.valueOf(content)));
    }

}
