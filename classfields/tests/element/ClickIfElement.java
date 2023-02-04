package ru.yandex.webmaster.tests.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import org.hamcrest.Matcher;
import ru.auto.tests.commons.extension.interfaces.ClickIfMethod;

public interface ClickIfElement extends AtlasWebElement {

    @ClickIfMethod
    void clickIf(Matcher matcher);
}
