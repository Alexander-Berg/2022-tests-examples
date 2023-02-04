package ru.yandex.whitespirit.it_tests;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import lombok.val;
import ru.yandex.whitespirit.it_tests.whitespirit.WhiteSpiritManager;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static ru.yandex.whitespirit.it_tests.utils.Constants.HERKULES;
import static ru.yandex.whitespirit.it_tests.utils.Utils.executeWithAttempts;

public class AdminTest {
    private static final WhiteSpiritClient whiteSpiritClient = Context.getWhiteSpiritManager().getWhiteSpiritClient();
    private static final WhiteSpiritManager whiteSpiritManager = Context.getWhiteSpiritManager();

    private String getSerialNumber() {
        val kktSNs = whiteSpiritManager.getKktSerialNumbersByInn(HERKULES.getInn());
        return kktSNs.stream().findAny().orElseThrow(() -> new IllegalStateException("No kkt for the firm."));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @DisplayName("/ident должен работать")
    public void testIdent(boolean on) {
        whiteSpiritClient.ident(getSerialNumber(), on).then().statusCode(200);
    }

    @Test
    @DisplayName("/log должен быть непустым")
    public void testLog() {
        val body = executeWithAttempts(() -> whiteSpiritClient.log(getSerialNumber())
                .then().statusCode(200)
                .extract().body().asString(), 3, Duration.ofSeconds(20));
        assertThat(body, not(blankOrNullString()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "2020-10-07T21:26:00" })
    @DisplayName("/set_datetime для касс в закрытом режиме должен работать")
    public void testSetDatetime(String dt) {
        whiteSpiritClient.setDatetime(getSerialNumber(), dt).then().statusCode(200);
    }
}
