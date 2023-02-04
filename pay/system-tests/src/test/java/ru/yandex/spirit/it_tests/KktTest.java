package ru.yandex.spirit.it_tests;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.darkspirit.it_tests.DarkspiritClient;
import ru.yandex.darkspirit.it_tests.DatabaseClient;
import ru.yandex.spirit.it_tests.IntegrationManager;
import ru.yandex.spirit.it_tests.configuration.SpiritKKT;
import ru.yandex.spirit.it_tests.configuration.SpiritKKTInstances;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

public class KktTest {
    private static final SpiritKKT kkt1 = SpiritKKTInstances.AllKKTS.get(0);

    private static final String FIRST_PATCH = "FR.3.3.9.bin";
    private static final String SECOND_PATCH = "FR.3.5.20.bin";
    private static final String MIN_SW_VERSION = "3.5.30";
    private static final String MIN_PATCH = String.format("FR.%s.bin", MIN_SW_VERSION);
    private static final String CASH_REGISTER_TYPE = "rp_sistema_1fs";
    private static final String CHPASSOLD_PATCH = "chpassold.bin";
    private static final String PROCESS_NAME = "prepare_cash_new";

    private static final Map<String, String> PATCHES = Map.of(FIRST_PATCH, "11743230dd5444b2f5e4216ffed118d1",
            SECOND_PATCH, "e2aec668167124a6f6b81c058e5f0d20");

    private static final DarkspiritClient darkspiritClient = IntegrationManager.getDarkspiritClient();
    private static final WhiteSpiritClient whiteSpiritClient = IntegrationManager.getWhiteSpiritClient();
    private static final DatabaseClient databaseClient = IntegrationManager.getDatabaseClient();

    @BeforeAll
    public static void start() {
        InteractionsManager.waitWhitespiritDarkspirit(List.of(kkt1));

        darkspiritClient.syncCashregisters().then().statusCode(200);
        databaseClient.setOEBSAddressCodeInTCashRegister(kkt1.kktSN, "IVA>IVNIT");

        darkspiritClient.uploadFirmwareNew(MIN_PATCH, CASH_REGISTER_TYPE, MIN_PATCH).then().statusCode(200);
    }

//    @Test
//    @DisplayName("Проверка того, что при подключении новой кассы в Darkspirit она создается в состоянии NEW, а процесс переводит ее в NON_CONFIGURED")
//    public void testCashRegisterState() {
//        darkspiritClient.processStatus(KKT, MIN_SW_VERSION, PROCESS_NAME).then().statusCode(200)
//                .body("UPLOAD_CHPASSOLD_TO_WHITESPIRIT", hasItem("DARKSPIRIT_HAS_CHPASSOLD"));
//        assertThat(databaseClient.getDsState(KKT), is("NEW"));
//        darkspiritClient.uploadFirmwareNew(CHPASSOLD_PATCH, CASH_REGISTER_TYPE, getPatch(CHPASSOLD_PATCH)).then().statusCode(200);
//
//        uploadFirmwareToKkt(MIN_PATCH);
//        waitForKktToRebootWithRequiredVersion("3.5", 30);
//
//        val kktId = databaseClient.getId(KKT);
//        darkspiritClient.launchProcess(kktId, PROCESS_NAME).then().statusCode(200);
//
//        waitForAllStages();
//
//        // Мы ожидаем, что касса не просто прочухалась, а в результате заливки chpassold.bin в ней ничего не поменялось в плане версии
//        waitForKktToRebootWithRequiredVersion("3.5", 30);
//        await().atMost(30, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS)
//                .until(() -> "READY_TO_REGISTRATION".equals(databaseClient.getDsState(KKT)));
//    }

    @Test
    @DisplayName("Проверка полного цикла обновления прошивок на одной из касс")
    public void testFullUpgradeCycle() {

        darkspiritClient.uploadFirmwareNew(SECOND_PATCH, CASH_REGISTER_TYPE, SECOND_PATCH).then().statusCode(200);
        darkspiritClient.uploadFirmwareNew(SECOND_PATCH + "duplicate", CASH_REGISTER_TYPE, SECOND_PATCH).then().statusCode(200);

        whiteSpiritClient.status(kkt1.kktSN).then().statusCode(200)
                .body("lowlevel.software_version", is("3.5"))
                .body("lowlevel.software_build", is(30));


        uploadFirmwareToKkt(SECOND_PATCH);

        whiteSpiritClient.uploads().then().statusCode(200)
                .body("$", containsPatch(SECOND_PATCH));


        waitForKktToRebootWithRequiredVersion("3.5", 20);
    }



    @Test
    @DisplayName("Проверка заливки прошивки на whitespirit через darkspirit")
    public void testUploadCombo() {

        darkspiritClient.uploadFirmware(FIRST_PATCH).then().statusCode(200);

        given().ignoreExceptions().with().pollInterval(10, TimeUnit.SECONDS).await().atMost(1, TimeUnit.MINUTES)
                .until(() -> {
                    whiteSpiritClient.uploads().then().statusCode(200)
                            .body("$", containsPatch(FIRST_PATCH));
                    return true;
                });
    }

    private static Matcher containsPatch(String name) {
        return hasItem(
                allOf(
                        hasEntry("name", name),
                        hasEntry("checksum", PATCHES.get(name))
                )
        );
    }

    private static void uploadFirmwareToKkt(String filename) {
        given().ignoreExceptions().with().pollInterval(5, TimeUnit.SECONDS).await().atMost(2, TimeUnit.MINUTES)
                .until(() -> {
                    darkspiritClient.upgradeCrs(Set.of(kkt1.kktSN), filename, filename).then().statusCode(200);
                    return true;
                });
    }

    private static void waitForAllStages() {
        given().ignoreExceptions().pollInterval(2, TimeUnit.SECONDS).await().atMost(2, TimeUnit.MINUTES)
                .until(() -> {
                    darkspiritClient.launchStage(PROCESS_NAME).then().statusCode(200)
                            .body("finished", is(1));
                    return true;
                });
    }

    private static void waitForKktToRebootWithRequiredVersion(String version, int build) {
        given().ignoreExceptions().with().pollInterval(5, TimeUnit.SECONDS).await().atMost(2, TimeUnit.MINUTES)
                .until(() -> {
                    whiteSpiritClient.status(kkt1.kktSN).then().statusCode(200)
                            .body("lowlevel.software_version", is(version))
                            .body("lowlevel.software_build", is(build))
                            .body("state", is("NONCONFIGURED"));
                    return true;
                });
    }
}
