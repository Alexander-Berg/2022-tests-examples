package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import org.hamcrest.Matcher;
import ru.auto.tests.commons.extension.interfaces.ClickIfMethod;
import ru.auto.tests.commons.extension.interfaces.ClickWhile;
import ru.auto.tests.commons.extension.interfaces.HoverMethod;

public interface RealtyElement extends AtlasWebElement {

    @ClickIfMethod
    void clickIf(Matcher matcher);

    @HoverMethod
    void hover();

    @Override
    RealtyElement should(Matcher matcher);

    @Override
    RealtyElement waitUntil(Matcher matcher);

    @ClickWhile
    RealtyElement clickWhile(Matcher matcher);

}