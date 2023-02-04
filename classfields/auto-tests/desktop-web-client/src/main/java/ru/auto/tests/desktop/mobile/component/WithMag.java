package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.main.Mag;

public interface WithMag {

    @Name("Блок «Журнал»")
    @FindBy("//div[contains(@class, 'IndexJournal IndexBlock')] | " +
            "//div[contains(@class, 'IndexJournal PageCard__sectionVertical')] | " +
            "//div[contains(@class, 'CardGroupJournal')]")
    Mag mag();
}