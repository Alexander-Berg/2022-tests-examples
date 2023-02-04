package ru.yandex.whitespirit.it_tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.whitespirit.it_tests.whitespirit.client.HudsuckerClient;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;


public class HudsuckerTest {
    private static final HudsuckerClient client = Context.getWhiteSpiritManager().getHudsuckerClient();

    @Test
    @DisplayName("/hudsucker ручка должна отработать нормально")
    public void testHudsucker() {
        client.hudsucker().then().statusCode(200)
                .body("whitespirits", hasSize(1))
                .and()
                .body("whitespirits.address", hasItems("171.42.42.100:8080"));
    }
}
