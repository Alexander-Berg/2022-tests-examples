package ru.yandex.arenda.element.common;

import io.qameta.atlas.webdriver.AtlasWebElement;
import org.hamcrest.Matcher;
import ru.auto.tests.commons.extension.interfaces.ClickIfMethod;
import ru.auto.tests.commons.extension.interfaces.ClickWhile;
import ru.auto.tests.commons.extension.interfaces.HoverMethod;

public interface ArendaElement extends AtlasWebElement {

    @ClickIfMethod
    void clickIf(Matcher matcher);

    @HoverMethod
    void hover();

    @Override
    ArendaElement should(Matcher matcher);

    @Override
    ArendaElement waitUntil(Matcher matcher);

    @ClickWhile
    ArendaElement clickWhile(Matcher matcher);

}