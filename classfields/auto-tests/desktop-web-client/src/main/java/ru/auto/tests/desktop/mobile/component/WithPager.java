package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.Pager;

public interface WithPager {

    @Name("Пагинатор")
    @FindBy("//div[contains(@class, 'ReviewComments__pagination')]")
    Pager pager();
}