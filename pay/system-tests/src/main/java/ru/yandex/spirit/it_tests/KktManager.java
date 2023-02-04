package ru.yandex.spirit.it_tests;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.json.JSONObject;
import ru.yandex.darkspirit.it_tests.DarkspiritClient;
import ru.yandex.darkspirit.it_tests.DatabaseClient;
import ru.yandex.spirit.it_tests.configuration.SpiritKKT;
import ru.yandex.spirit.it_tests.configuration.SpiritKKTInstances;
import ru.yandex.whitespirit.it_tests.templates.TemplatesManager;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.spirit.it_tests.Utils.getOrCreateFileInWINDOWS1251;
import static ru.yandex.whitespirit.it_tests.templates.Template.REGISTER_KKT_REQUEST_BODY;
import static ru.yandex.whitespirit.it_tests.utils.Utils.generateRNM;

@UtilityClass
public class KktManager {
    private static DarkspiritClient darkspiritClient;
    private static WhiteSpiritClient whiteSpiritClient;
    private static DatabaseClient databaseClient;
    private static final TemplatesManager templatesManager = new TemplatesManager();

    static {
        darkspiritClient = IntegrationManager.getDarkspiritClient();
        whiteSpiritClient = IntegrationManager.getWhiteSpiritClient();
        databaseClient = IntegrationManager.getDatabaseClient();
    }

    public static void setUpKkts(List<SpiritKKT> kkts, String swVersion, int swBuild, String cashRegisterType) {
        String minPatch = String.format("FR.%s.bin", String.format("%s.%d", swVersion, swBuild));
        darkspiritClient.syncCashregisters().then().statusCode(200);
        darkspiritClient.uploadFirmwareNew(minPatch, cashRegisterType, minPatch).then().statusCode(200);
        for (val kkt: kkts) {
            kkt.id = databaseClient.getId(kkt.kktSN);
            given().ignoreExceptions().with().pollInterval(5, TimeUnit.SECONDS).await().atMost(2, TimeUnit.MINUTES)
                    .until(() -> {
                        darkspiritClient.upgradeCrs(Set.of(kkt.kktSN), minPatch, minPatch).then().statusCode(200);
                        return true;
                    });
            given().ignoreExceptions().with().pollInterval(5, TimeUnit.SECONDS).await().atMost(2, TimeUnit.MINUTES)
                    .until(() -> {
                        whiteSpiritClient.status(kkt.kktSN).then().statusCode(200)
                                .body("lowlevel.software_version", is(swVersion))
                                .body("lowlevel.software_build", is(swBuild))
                                .body("state", is("NONCONFIGURED"));
                        return true;
                    });
            setAdminPasswordToDarkspirit(kkt);
            whiteSpiritClient.setupSshConnection(kkt.kktSN).then().statusCode(200);
            fillKktAddress(kkt);
            changeFn(kkt, SpiritKKTInstances.generateFnSerial(), true);
        }
    }

    public static void configureRegister(SpiritKKT kkt, String test_key, Boolean bso) {
        darkspiritClient.createApplications(bso, kkt.kktSN, kkt.inn).then().statusCode(200);

        ds_configure(kkt);

        ds_register(kkt);

        darkspiritClient.changeState(
                kkt.id, "OK", "testing_" + test_key, true // CASH_CARDS_OK will fail
        ).then().statusCode(200);

        darkspiritClient.syncCashregisters().then().statusCode(200);

        given().ignoreExceptions().with().pollInterval(2, TimeUnit.SECONDS).await().atMost(1, TimeUnit.MINUTES)
                .until(() -> {
                    whiteSpiritClient.status(kkt.kktSN).then().statusCode(200)
                            .body("state", is("CLOSE_SHIFT"))
                            .body("logical_state", is("OK"));
                    return true;
                });
    }

    public static void moveToPostfiscal(SpiritKKT kkt, String test_key, Boolean startReregistration) {
        clearQueueSsh(kkt);
        given().ignoreExceptions().with().pollInterval(2, TimeUnit.SECONDS).await().atMost(1, TimeUnit.MINUTES)
                .until(() -> {
                    darkspiritClient.closeFiscalMode(kkt.id, "testing_" + test_key, startReregistration)
                            .then().statusCode(200);
                    return true;
                });

        waitForOnline(kkt);

        whiteSpiritClient.status(kkt.kktSN).then().statusCode(200)
                .body("state", is("POSTFISCAL"));

        given().ignoreExceptions().with().pollInterval(2, TimeUnit.SECONDS).await().atMost(2, TimeUnit.MINUTES)
                .until(() -> {
                    makeFnArchiveSsh(kkt);
                    whiteSpiritClient.status(kkt.kktSN)
                            .then().statusCode(200).body("fn_state", is("ARCHIVE_READING"));
                    return true;
                });

        darkspiritClient.syncCashregisters().then().statusCode(200);
    }

    public static void ds_configure(SpiritKKT kkt) {
        val rnm = generateRNM(kkt.inn, kkt.kktSN);
        val content = templatesManager.processTemplate("configure_darkspirit.csv.flth",
                Map.of("sn", kkt.kktSN,
                        "rnm", rnm));
        val conf_file = getOrCreateFileInWINDOWS1251(
                String.format("src/main/resources/configure/%s_%s.csv", kkt.kktSN, rnm), content
        );
        darkspiritClient.configure(conf_file).then().statusCode(200);
    }

    public static void ds_register(SpiritKKT kkt) {
        darkspiritClient.register(kkt.kktSN).then().statusCode(200);

        waitForOnline(kkt);

        darkspiritClient.register(kkt.kktSN).then().statusCode(200).body("error", is(emptyList()));
    }

    public static void ws_register(SpiritKKT kkt) {
        val registerBody = templatesManager.processTemplate(REGISTER_KKT_REQUEST_BODY, emptyMap());
        whiteSpiritClient.register(kkt.kktSN, registerBody).then().statusCode(200);
    }

    public static void fillKktAddress(SpiritKKT kkt) {
        String jsonStringStatus = whiteSpiritClient.status(kkt.kktSN).then().extract().asString();
        JSONObject jsonObjectStatus = new JSONObject(jsonStringStatus);
        JSONObject jsonObjectLowlevel = (JSONObject) jsonObjectStatus.get("lowlevel");
        kkt.address = jsonObjectLowlevel.get("addr").toString();
    }

    public static void setAdminPasswordToDarkspirit(SpiritKKT kkt) {
        String jsonStringPassword = whiteSpiritClient.getPassword(kkt.kktSN, true)
                .then().statusCode(200).extract().asString();
        JSONObject jsonObjectPassword = new JSONObject(jsonStringPassword);
        String Password = jsonObjectPassword.get("Password").toString();
        darkspiritClient.setPassword(kkt.id, Password).then().statusCode(200);
    }

    public static void waitForOnline(SpiritKKT kkt) {
        given().ignoreExceptions().with().pollInterval(3, TimeUnit.SECONDS).await().atMost(3, TimeUnit.MINUTES)
                .until(() -> {
                    whiteSpiritClient.status(kkt.kktSN)
                            .then().statusCode(200).body("state", not(is("OFFLINE")));
                    return true;
                });
    }

    public static void waitForOffline(SpiritKKT kkt) {
        given().ignoreExceptions().with().pollInterval(3, TimeUnit.SECONDS).await().atMost(3, TimeUnit.MINUTES)
                .until(() -> {
                    whiteSpiritClient.status("ip:" + kkt.address + ":3333")
                            .then().statusCode(200).body("state", is("OFFLINE"));
                    return true;
                });
    }

    public static void clearQueueSsh(SpiritKKT kkt) {
        SshEngineer.clearQueue(kkt);

        whiteSpiritClient.reboot(kkt.kktSN, "false").then().statusCode(200);

        waitForOnline(kkt);
    }

    public static void makeFnArchiveSsh(SpiritKKT kkt) {
        SshEngineer.makeFnArchive(kkt);

        whiteSpiritClient.reboot(kkt.kktSN, "true").then().statusCode(200);

        waitForOnline(kkt);
    }

    public static void changeFn(SpiritKKT kkt, String newFn, Boolean reboot) {
        SshEngineer.changeFn(kkt, newFn);

        if (reboot) {
            whiteSpiritClient.reboot(kkt.kktSN, "true").then().statusCode(200);
            waitForOnline(kkt);
        }

        kkt.fnSN = newFn;
    }
}
