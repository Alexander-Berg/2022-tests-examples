package ru.yandex.whitespirit.it_tests;

import java.io.File;
import java.time.Duration;

import lombok.SneakyThrows;
import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.whitespirit.it_tests.configuration.KKT;
import ru.yandex.whitespirit.it_tests.whitespirit.WhiteSpiritManager;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.whitespirit.it_tests.utils.Utils.executeWithAttempts;

@RunOnlyIfMGMAreEnabled
public class UpgradeTest {
    private static final String FILENAME = "FR.4.0.117.bin";
    private static final WhiteSpiritManager whiteSpiritManager = Context.getWhiteSpiritManager();
    private static final WhiteSpiritClient whiteSpiritClient = whiteSpiritManager.getWhiteSpiritClient();
    private static KKT newKkt;
    private static KKT oldKkt;

    @BeforeAll
    public static void initKktAndAwaitGroup() {
        oldKkt = StreamEx.of(whiteSpiritManager.getKKTs().values())
                .findAny(kkt -> kkt.getGroup().equals("CORRECTION")).orElseThrow();
        newKkt = StreamEx.of(whiteSpiritManager.getKKTs().values())
                .findAny(kkt -> kkt.getGroup().equals("MARK")).orElseThrow();
    }

    @SneakyThrows
    private void upgrade(String kktSN) {
        val loader = Thread.currentThread().getContextClassLoader();
        val url = loader.getResource(FILENAME);
        val file = new File(url.toURI());

        assertTrue(file.exists());
        whiteSpiritClient.upload(file).then().statusCode(200);
        whiteSpiritClient.upgradeUsingSsh(kktSN, FILENAME).then().statusCode(200);

        executeWithAttempts(() -> whiteSpiritClient.status(kktSN).then().statusCode(200)
                .body("lowlevel.software_version", equalTo("4.0"))
                .body("lowlevel.software_build", equalTo(117)), 6, Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("Проверка обновления прошивки кассы с ФН-МГМ с 4.0.110 до 4.0.117")
    @SneakyThrows
    public void testUpgradeNew() {
        upgrade(newKkt.getKktSN());
    }

    @Test
    @DisplayName("Проверка обновления прошивки кассы с ФН-МГМ с 3.5.84 до 4.0.117")
    @SneakyThrows
    public void testUpgradeOld() {
        upgrade(oldKkt.getKktSN());
    }
}
