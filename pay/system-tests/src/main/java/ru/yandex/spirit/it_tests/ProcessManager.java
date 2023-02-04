package ru.yandex.spirit.it_tests;

import lombok.experimental.UtilityClass;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.darkspirit.it_tests.DarkspiritClient;
import ru.yandex.darkspirit.it_tests.template_classes.LaunchStageInfoField;
import ru.yandex.spirit.it_tests.configuration.SpiritKKT;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.is;

@UtilityClass
public class ProcessManager {
    private static DarkspiritClient darkspiritClient;
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        darkspiritClient = IntegrationManager.getDarkspiritClient();
    }

    public static void startReregistration(String app_version) {
        Map<String, Object> data = Map.of(
                "config", Map.of(
                        "fns_app_version", app_version, "wait_minutes_after_fail_result", 0,
                        "wait_hours_after_waiting_result", 0, "wait_days_after_ticket_resolution", 0,
                        "fnsreg_app_upload_wait_minutes", 0, "fns_send_status_wait_minutes", 0
                )
        );
        darkspiritClient.taskRun("start_reregistration_process", data);
    }

    public static void multipleLaunchStage(String process_name, int number) {
        for (int i = 0; i < number; i += 1) {
            darkspiritClient.launchStage(process_name).then().statusCode(200)
                    .body("success", is(1))
                    .body("waiting", is(0));
        }
    }

    public static void waitingLaunchStage(String process_name) {
        given().ignoreExceptions().with().pollInterval(3, TimeUnit.SECONDS).await().atMost(3, TimeUnit.MINUTES)
                .until(() -> {
                    darkspiritClient.launchStage(process_name).then().statusCode(200)
                            .body("success", is(1)).body("waiting", is(0));
                    return true;
                });
    }

    public static void finishingLaunchStage(String process_name) {
        darkspiritClient.launchStage(process_name).then().statusCode(200)
                .body("success", is(1)).body("finished", is(1));
    }

    public static LaunchStageInfoField launchAndExtractStageInfo(String process_name) throws IOException {
        String launchStageResponse = darkspiritClient.launchStage(process_name).then().statusCode(200).extract().asString();
        return mapper.readValue(launchStageResponse, LaunchStageInfoField.class);
    }

    public static void skipToStage(SpiritKKT kkt, String stageName) {
        darkspiritClient.setStage(stageName, kkt.id).then().statusCode(200);
    }
}
