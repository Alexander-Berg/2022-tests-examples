package ru.yandex.whitespirit.it_tests;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.whitespirit.it_tests.whitespirit.WhiteSpiritManager;
import ru.yandex.whitespirit.it_tests.whitespirit.client.HudsuckerClient;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.whitespirit.it_tests.utils.Constants.CLOSED_SHIFT_STATUS;
import static ru.yandex.whitespirit.it_tests.utils.Constants.HERKULES;
import static ru.yandex.whitespirit.it_tests.utils.Constants.OPENED_SHIFT_STATUS;
import static ru.yandex.whitespirit.it_tests.utils.Utils.checkResponseCode;

public class ShiftsTest {
    private static final WhiteSpiritManager whiteSpiritManager = Context.getWhiteSpiritManager();
    private static final HudsuckerClient hudsuckerClient = whiteSpiritManager.getHudsuckerClient();
    private static final WhiteSpiritClient whiteSpiritClient = whiteSpiritManager.getWhiteSpiritClient();

    @Test
    @DisplayName("Статус кассы должен соответствовать тому открыта ли на ней смена.")
    public void testOpenAndCloseShift() {
        val kktSNs = whiteSpiritManager.getKktSerialNumbersByInn(HERKULES.getInn());
        val kktSN = kktSNs.stream().findAny()
                .orElseThrow(() -> new IllegalStateException("No kkt for the firm."));

        checkStatus(kktSN, CLOSED_SHIFT_STATUS);
        checkResponseCode(whiteSpiritClient.openShift(kktSN));
        checkStatus(kktSN, OPENED_SHIFT_STATUS);
        checkResponseCode(whiteSpiritClient.closeShift(kktSN));
        checkStatus(kktSN, CLOSED_SHIFT_STATUS);
    }

    private void checkStatus(String kktSN, String status) {
        given().ignoreExceptions().pollInterval(30, TimeUnit.SECONDS).await().atMost(
                2, TimeUnit.MINUTES
        ).until(() -> {
                hudsuckerClient.status(kktSN)
                        .then()
                        .statusCode(200)
                        .body("state", equalTo(status));
                return true;
                }
        );
    }
}
