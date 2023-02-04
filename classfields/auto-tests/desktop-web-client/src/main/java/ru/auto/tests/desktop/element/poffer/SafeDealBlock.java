package ru.auto.tests.desktop.element.poffer;

import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;

public interface SafeDealBlock extends VertisElement, WithCheckbox, WithButton {

    String RECEIVE_SAFE_DEAL_REQUESTS = "Получать предложения от покупателей";

}