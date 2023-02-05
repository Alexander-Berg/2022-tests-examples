package ru.yandex.navi.tests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.qameta.allure.Issue;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.FineInfoScreen;
import ru.yandex.navi.ui.FinesListScreen;
import ru.yandex.navi.ui.FinesScreen;
import ru.yandex.navi.ui.PaymentMethodsScreen;

@RunWith(RetryRunner.class)
public final class FinesTest extends BaseTest {
    @Test
    @Category({UnstableIos.class})
    @Issue("MOBNAVI-22064")
    public void payFine() {
        FinesScreen finesScreen = mapScreen.clickMenu().clickFines();
        finesScreen.typeSts("9915615781").clickCheck();

        FinesListScreen.getVisible().clickFine();

        FineInfoScreen.getVisible().inputPayer("Иван Иванович").clickPay();

        PaymentMethodsScreen.getVisible().clickWallet();
    }
}
