package ru.yandex.whitespirit.it_tests;

import java.util.Map;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class PingTest {
    private static final WhiteSpiritClient whiteSpiritClient = Context.getWhiteSpiritManager().getWhiteSpiritClient();

    @Test
    @DisplayName("/ping должен работать")
    public void testPing() {
        whiteSpiritClient.ping()
                .then().statusCode(200);
    }

    @Test
    @DisplayName("Запрос с backlog_ratio должен работать")
    public void testBacklogRatio() {
        whiteSpiritClient.ping(Map.of("backlog_ratio", "2")).then().statusCode(200);
    }

    @Test
    @DisplayName("Некорректный запрос должен породить ошибку")
    public void testIncorrectInput() {
        whiteSpiritClient.ping(Map.of("group", "_NOGROUP"))
            .then().statusCode(400).body("error", equalTo("BadDataInput"));
    }

    @Test
    @DisplayName("Проверка статуса кассы со старой прошивкой после конфигурации - не должно содержать extended_work_mode и окп/оисм полей в ofd_info")
    public void testOldStatus() {
        val kkt = Context.getWhiteSpiritManager().getKKTs().values()
                .stream().filter(k -> k.getGroup().equals("_NOGROUP"))
                .findFirst().orElseThrow();

        whiteSpiritClient.status(kkt.getKktSN()).then().statusCode(200)
                .body("registration_info", not(hasKey("extended_work_mode")))
                .body("ofd_info", not(hasKey("oism_addr")))
                .body("ofd_info", not(hasKey("oism_timeout")))
                .body("ofd_info", not(hasKey("okp_addr")))
                .body("ofd_info", not(hasKey("okp_timeout")));
    }

    @Test
    @DisplayName("Проверка статуса кассы с новой прошивкой после конфигурации - должно содержать extended_work_mode и поля оисм/окп")
    @RunOnlyIfMGMAreEnabled
    public void testNewStatus() {
        val kkt = Context.getWhiteSpiritManager().getKKTs().values()
                .stream().filter(k -> k.getVersion().startsWith("4"))
                .findFirst().orElseThrow();

        whiteSpiritClient.status(kkt.getKktSN()).then().statusCode(200)
                .body("registration_info", hasEntry(is("extended_work_mode"), containsInAnyOrder("marking")))
                .body("ofd_info", hasEntry(is("oism_addr"), is("test.kkt.ofd.yandex.net:54321")))
                .body("ofd_info", hasEntry(is("oism_timeout"), is(60)))
                .body("ofd_info", hasEntry(is("okp_addr"), is("prod01.okp-fn.ru:26101")))
                .body("ofd_info", hasEntry(is("okp_timeout"), is(30)));
    }
}
