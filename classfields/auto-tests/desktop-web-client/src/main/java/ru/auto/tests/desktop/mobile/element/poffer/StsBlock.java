package ru.auto.tests.desktop.mobile.element.poffer;

import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;
import ru.auto.tests.desktop.mobile.element.WithInput;

public interface StsBlock extends VertisElement, WithInput, WithCheckbox {

    String PLATE_NUMBER = "Госномер";
    String VIN = "VIN / номер кузова";

}
