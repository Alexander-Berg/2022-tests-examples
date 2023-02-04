package ru.yandex.general.element;

import ru.yandex.general.consts.CardStatus;

import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;

public interface RecallModal extends Popup {

    String REMOVE_FROM_SALE = "Снять с продажи";

    default void deactivateOfferWithReason(CardStatus.CardDeactivateStatuses deactivateStatus) {
        waitSomething(500, TimeUnit.MILLISECONDS);
        radioButtonWithLabel(deactivateStatus.getName()).click();
        button(REMOVE_FROM_SALE).click();
    }

}
