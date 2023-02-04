package ru.auto.tests.commons.extension.element;

import io.qameta.atlas.core.api.Timeout;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.hamcrest.Matcher;
import ru.auto.tests.commons.extension.EnabledMethod;
import ru.auto.tests.commons.extension.interfaces.ClickIfMethod;
import ru.auto.tests.commons.extension.interfaces.ClickWhile;
import ru.auto.tests.commons.extension.interfaces.HoverMethod;

public interface VertisElement extends AtlasWebElement {

    @ClickIfMethod
    void clickIf(Matcher matcher);

    @HoverMethod
    VertisElement hover();

    @EnabledMethod
    VertisElement waitEnabled();

    @Override
    VertisElement should(Matcher matcher);

    @Override
    VertisElement waitUntil(Matcher matcher);

    @Override
    VertisElement waitUntil(Matcher matcher, @Timeout Integer timeoutInSeconds);

    @Override
    VertisElement waitUntil(String message, Matcher matcher, @Timeout Integer timeoutInSeconds);

    @ClickWhile
    VertisElement clickWhile(Matcher matcher);

}