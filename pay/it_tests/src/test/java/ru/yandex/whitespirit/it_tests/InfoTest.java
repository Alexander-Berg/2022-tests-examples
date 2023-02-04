package ru.yandex.whitespirit.it_tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

public class InfoTest {
    private static final WhiteSpiritClient whiteSpiritClient = Context.getWhiteSpiritManager().getWhiteSpiritClient();

    @Test
    @DisplayName("Ручка info должна работать и возвращать ответ в согласованным со Swagger формате.")
    public void testInfo() {
        whiteSpiritClient.info()
                .then().statusCode(200);
    }
}
