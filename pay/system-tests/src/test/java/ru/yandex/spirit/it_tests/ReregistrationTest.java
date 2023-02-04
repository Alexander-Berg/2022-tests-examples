package ru.yandex.spirit.it_tests;

import lombok.val;
import org.junit.jupiter.api.*;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.darkspirit.it_tests.DarkspiritClient;
import ru.yandex.darkspirit.it_tests.DatabaseClient;
import ru.yandex.spirit.it_tests.IntegrationManager;
import ru.yandex.spirit.it_tests.KktManager;
import ru.yandex.spirit.it_tests.ProcessManager;
import ru.yandex.spirit.it_tests.WireMocker;
import ru.yandex.spirit.it_tests.configuration.SpiritKKT;
import ru.yandex.spirit.it_tests.configuration.SpiritKKTInstances;
import ru.yandex.darkspirit.it_tests.template_classes.LaunchStageInfoField;
import ru.yandex.darkspirit.it_tests.template_classes.LaunchStageProcessInfoField;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReregistrationTest {
    // После выставления NEED_MOCK_BOT нужно расскоментить соответствующие строчки в конфиге даркспирита для системных тестов
    private static final boolean NEED_MOCK_BOT = false;
    private static final String MOCK_TICKET = "ITDC-55468";
    private static final boolean SKIP_TRACKER = false;

    private static final String TEST_KEY = "reregistration";
    private static final String PROCESS_NAME = "reregistration";
    private static final String SW_VERSION = "3.5";
    private static final int SW_BUILD = 84;
    private static final String CASH_REGISTER_TYPE = "rp_sistema_1fa";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final DarkspiritClient darkspiritClient = IntegrationManager.getDarkspiritClient();
    private static final WhiteSpiritClient whiteSpiritClient = IntegrationManager.getWhiteSpiritClient();
    private static final DatabaseClient databaseClient = IntegrationManager.getDatabaseClient();

    private static SpiritKKT kkt2 = SpiritKKTInstances.AllKKTS.get(1);

    private static SpiritKKT kkt3 = SpiritKKTInstances.AllKKTS.get(2);

    private static final String newKkt2Fn = "9999100000000002";
    private static final String newKkt3Fn = "9999100000000003";

    private static List<SpiritKKT> KKTS = List.of(kkt2, kkt3);

    @BeforeAll
    public static void start() {
        InteractionsManager.waitWhitespiritDarkspirit(KKTS);

        KktManager.setUpKkts(KKTS, SW_VERSION, SW_BUILD, CASH_REGISTER_TYPE);

        databaseClient.setOEBS(
                1, "101461844", kkt2.kktSN,
                "BU>CASH_REGISTER>STARRUS>KKT RP Sistema 1FA", "IVA", "IVNIT"
        );

        databaseClient.setOEBS(
                2, "101461845", kkt3.kktSN,
                "BU>CASH_REGISTER>STARRUS>KKT RP Sistema 1FA", "IVA", "IVNIT"
        );


        WireMocker.setUpOfdMock(KKTS, 2);
        WireMocker.setUpFnsApiBasicMocks();
        WireMocker.setUpFnsApiRegisterStatusNotFoundMock();
        WireMocker.setUpYSignGetUserCertList();
        WireMocker.setUpYSignUniSignCMS();
        WireMocker.setUpFnsApiRegisterMock();
        if (NEED_MOCK_BOT) {
            WireMocker.setUpBotMock(MOCK_TICKET);
        }
        if (!SKIP_TRACKER) {
            waitForTracker();
            InteractionsManager.removeStartrekReregTagInReregistrationForPreviousRuns(kkt2.kktSN, newKkt2Fn);
            InteractionsManager.removeStartrekReregTagInReregistrationForPreviousRuns(kkt3.kktSN, newKkt3Fn);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Testing whole 'reregistration' process run")
    public void testCashReregistration() throws IOException {
        KktManager.configureRegister(kkt2, TEST_KEY, false);
        KktManager.moveToPostfiscal(kkt2, TEST_KEY, false);
        ProcessManager.startReregistration("V504");

        // sync_documents -> ... -> wait_for_fs_ticket_closed
        launchStagesTillWaitForFSTicket(kkt2);

        KktManager.changeFn(kkt2, newKkt2Fn, true);
        if (!SKIP_TRACKER) {
            if (!NEED_MOCK_BOT) {
                String ticket = extractTicketOnWaittingStage("change_fs");
                waitForTracker();
                InteractionsManager.closeStartrekTicket(ticket);
            }

            // wait_for_fs_ticket_closed
            ProcessManager.waitingLaunchStage(PROCESS_NAME);
            // issue_after_fs_change, wait_for_issue_after_fs_ticket_closed
            ProcessManager.multipleLaunchStage(PROCESS_NAME, 2);
        } else {
            ProcessManager.skipToStage(kkt2, "check_online_after_issue");
        }

        // check_online_after_issue, turn_off_led
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 2);

        // clear_device_reregistration -> ... -> create_application_issue_ticket_for_reregistration
        launchCashReregisterStages(kkt2);

        assertThat(
                databaseClient.getField(kkt2.kktSN, "STATE", String.class),
                is("CLOSE_SHIFT")
        );
        whiteSpiritClient.status(kkt2.kktSN).then()
                .body("fn_state", is("FISCAL"));

        if (!SKIP_TRACKER) {
            // create_application_issue_ticket_for_reregistration -> ... -> upload_to_s3_reregistration_card
            launchCreateApplicationStages(kkt2, true, true);

            // upload_to_s3_reregistration_card
            ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);

            // wait_for_cash_cards
            ProcessManager.waitingLaunchStage(PROCESS_NAME);
        } else {
            ProcessManager.skipToStage(kkt2, "reregistration_fns_resend");
        }

        // reregistration_fns_resend, wait_fns_reregistration_status_success
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 2);
        ProcessManager.finishingLaunchStage(PROCESS_NAME); // make_ok

        whiteSpiritClient.status(kkt2.kktSN).then()
                .body("logical_state", is("OK")).body("hidden", is(false));
    }

    @Test
    @Order(2)
    @DisplayName(
            "Testing a scenario, where cash goes offline after fs change," +
            "then skips fnsapi reregistration and waits fns-send"
    )
    public void testBadScenarios() throws IOException {
        WireMocker.setUpFnsApiRegisterStatusNotFoundMock();
        KktManager.configureRegister(kkt3, TEST_KEY, false);
        KktManager.moveToPostfiscal(kkt3, TEST_KEY, false);
        ProcessManager.startReregistration("V504");

        // sync_documents -> wait_for_fs_ticket_closed
        launchStagesTillWaitForFSTicket(kkt3);

        if (!SKIP_TRACKER) {
            IntegrationManager.stopService(kkt3.serviceName);
            KktManager.waitForOffline(kkt3);
            darkspiritClient.syncCashregisters().then().statusCode(200);
            if (!NEED_MOCK_BOT) {
                waitForTracker();
                String fsTicket = extractTicketOnWaittingStage("change_fs"); // wait_for_fs_ticket_closed
                InteractionsManager.closeStartrekTicket(fsTicket);
            }
            ProcessManager.waitingLaunchStage(PROCESS_NAME); // wait_for_fs_ticket_closed


            ProcessManager.waitingLaunchStage(PROCESS_NAME); // issue_after_fs_change

            if (!NEED_MOCK_BOT) {
                waitForTracker();
                String issueTicket = extractTicketOnWaittingStage("issue_after_fs_change");
                InteractionsManager.closeStartrekTicket(issueTicket);
            }

            ProcessManager.waitingLaunchStage(PROCESS_NAME); // wait_for_issue_after_fs_ticket_closed

            // check_online_after_issue
            darkspiritClient.launchStage(PROCESS_NAME).then().statusCode(200).body("failed", is(1));

            IntegrationManager.startService(kkt3.serviceName);
            KktManager.waitForOnline(kkt3);
        } else {
            ProcessManager.skipToStage(kkt3, "check_online_after_issue");
        }

        KktManager.changeFn(kkt3, newKkt3Fn, true);

        // check_online_after_issue, turn_off_led
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 2);

        // clear_device_reregistration -> ... -> create_application_issue_ticket_for_reregistration
        launchCashReregisterStages(kkt3);

        if (!SKIP_TRACKER) {
            // create_application_issue_ticket_for_reregistration
            ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);

            // wait_for_reregistration_application_issue_found
            ProcessManager.waitingLaunchStage(PROCESS_NAME);

            // create_attach_upload_to_s3_reregistration_application
            ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);

            // wait_for_application_attached_to_issue_ticket_for_reregistration
            ProcessManager.waitingLaunchStage(PROCESS_NAME);

            darkspiritClient.updateConfigProcess(
                    PROCESS_NAME, Map.of("inn_fns_api_not_available_list", List.of(kkt3.inn))
            ).then().statusCode(200);
            // switch_to_wait_for_application_issue_ticket_for_reregistration_closed
            ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);

            String ticket = extractTicketOnWaittingStage("create_application_issue_ticket_for_reregistration");
            InteractionsManager.postStartrekReregCardAttachment(ticket, kkt3);
            darkspiritClient.launchStage(PROCESS_NAME).then().body("waiting", is(1));
            InteractionsManager.closeStartrekTicket(ticket);

            // wait_for_application_issue_ticket_for_reregistration_closed
            ProcessManager.waitingLaunchStage(PROCESS_NAME);

            // upload_to_s3_reregistration_card, wait_for_cash_cards
            ProcessManager.multipleLaunchStage(PROCESS_NAME, 2);
        } else {
            ProcessManager.skipToStage(kkt3, "reregistration_fns_resend");
        }

        // reregistration_fns_resend,
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);

        WireMocker.fnsSendMock("false");

        // wait_fns_reregistration_status_success
        darkspiritClient.launchStage(PROCESS_NAME).then().body("waiting", is(1));

        WireMocker.fnsSendMock("true");

        // wait_fns_reregistration_status_success
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);

        ProcessManager.finishingLaunchStage(PROCESS_NAME); // make_ok
        whiteSpiritClient.status(kkt2.kktSN).then()
                .body("logical_state", is("OK")).body("hidden", is(false));
    }

    private static void waitForTracker() {
        given().ignoreExceptions().with().pollInterval(5, TimeUnit.SECONDS).await().atMost(2, TimeUnit.MINUTES)
                .until(() -> {
                    InteractionsManager.getStartrekIssue(MOCK_TICKET);
                    return true;
                });
    }

    private static void launchCashReregisterStages(SpiritKKT kkt) throws IOException {
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 1); // clear_device_reregistration
        KktManager.waitForOnline(kkt);
        ProcessManager.waitingLaunchStage(PROCESS_NAME); // wait_for_online_after_clear_device
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 4); // set_datetime_after_fs_stages, make_in_reregistration, new_reregistration, configure
        ProcessManager.waitingLaunchStage(PROCESS_NAME); // wait_for_online_after_configure

        // касса не хочет перерегистрироваться с виртуальным фном,
        // смотрим, что даркcпирит отработал нормально и проблема с кассой
        // пихаем документ о перерегистрации и меняем состояние регистрации руками
        // reregister
        // TODO: сделать честную перерегистрацию
        String launchStageResponse = darkspiritClient.launchStage(PROCESS_NAME).then().statusCode(200).extract().asString();
        LaunchStageInfoField launchStageInfo = mapper.readValue(launchStageResponse, LaunchStageInfoField.class);
        String error = launchStageInfo.processes_info.get(0).error;
        assertThat(error, containsString("ReRegistrationReportWithFNChange"));
        assertThat(error, containsString("StarrusDeviceException"));
        KktManager.ws_register(kkt);
        int fiscalStorageId = databaseClient.getField(kkt.kktSN, "fiscal_storage_id", Integer.class);
        databaseClient.addDocument(
                "ReRegistrationReport", 1,
                fiscalStorageId
        );
        int documentId = databaseClient.getDocumentId(1, fiscalStorageId);
        databaseClient.setLastRegistrationDocument(kkt.id, documentId);
        databaseClient.setLastRegistrationState(kkt.id, "REGISTERED");

        ProcessManager.skipToStage(kkt, "create_application_issue_ticket_for_reregistration");
        darkspiritClient.syncCashregisters();
    }

    private static void launchStagesTillWaitForFSTicket(SpiritKKT kkt) {
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 2); // sync_documents, reboot_cash
        KktManager.waitForOnline(kkt);
        // wait_for_reboot, make_ready_to_reregistration
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 2);

        if (!SKIP_TRACKER) {
            // change_fs
            ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);
        }
    }

    private static void launchCreateApplicationStages(
            SpiritKKT kkt, boolean loop_on_address_changed, boolean loop_on_upload
    ) {
        // create_application_issue_ticket_for_reregistration
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);

        // wait_for_reregistration_application_issue_found
        ProcessManager.waitingLaunchStage(PROCESS_NAME);

        // create_attach_upload_to_s3_reregistration_application
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);

        // wait_for_application_attached_to_issue_ticket_for_reregistration
        ProcessManager.waitingLaunchStage(PROCESS_NAME);

        // switch_to_wait_for_application_issue_ticket_for_reregistration_closed,
        // upload_to_fnsreg_reregistration_application
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 2);

        WireMocker.setUpFnsApiRegisterStatusInProcess();

        darkspiritClient.launchStage(PROCESS_NAME).then().statusCode(200).body("waiting", is(1));

        if (loop_on_address_changed) {
            loopOnAddressChanged();
        }

        if (loop_on_upload) {
            loopOnUpload();
        }

        WireMocker.setUpFnsApiRegisterStatusFoundMock(kkt);
        // wait_fns_reregistration_upload
        ProcessManager.waitingLaunchStage(PROCESS_NAME);

        // retry_fnsreg_application, attach_rereg_card_to_ticket
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 2);

        // wait_for_reregistration_cash_card_from_tracker_action
        ProcessManager.waitingLaunchStage(PROCESS_NAME);

        //close_rereg_ticket
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);

        // wait_for_application_issue_ticket_for_reregistration_closed
        ProcessManager.waitingLaunchStage(PROCESS_NAME);
    }

    private static void loopOnAddressChanged() {
        WireMocker.setUpFnsApiRegisterStatusRejected(
                32,  "Не указан код ФИАС для адреса места установки ККТ"
        );
        // wait_fns_reregistration_upload
        ProcessManager.waitingLaunchStage(PROCESS_NAME);
        // retry_fnsreg_application
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);
        WireMocker.setUpFnsApiRegisterStatusNotFoundMock();
        // create_and_attach_reregistration_application
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);
        // wait_for_application_attached_to_issue_ticket_for_reregistration
        ProcessManager.waitingLaunchStage(PROCESS_NAME);
        // switch_to_wait_for_application_issue_ticket_for_reregistration_closed,
        // upload_to_fnsreg_reregistration_application
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 2);
    }

    private static void loopOnUpload() {
        WireMocker.setUpFnsApiError(
                "outer.service.timeout",
                "Не удалось получить ответ за отведённое время от внешней системы." +
                        " Попробуйте отправить заявление повторно, с новым requestId."
        );
        // wait_fns_reregistration_upload
        ProcessManager.waitingLaunchStage(PROCESS_NAME);
        // retry_fnsreg_application
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);
        WireMocker.setUpFnsApiRegisterStatusNotFoundMock();
        // create_and_attach_reregistration_application
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);
        // wait_for_application_attached_to_issue_ticket_for_reregistration
        ProcessManager.waitingLaunchStage(PROCESS_NAME);
        // switch_to_wait_for_application_issue_ticket_for_reregistration_closed
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);

        WireMocker.setUpFnsApiRegisterStatusNotFoundMock();
        // upload_to_fnsreg_reregistration_application
        ProcessManager.multipleLaunchStage(PROCESS_NAME, 1);
    }

    private static String extractTicketOnWaittingStage(String ticketStageName) throws IOException {
        LaunchStageInfoField launchStageInfo = ProcessManager.launchAndExtractStageInfo(PROCESS_NAME);
        assertThat(launchStageInfo.waiting, equalTo(1));
        LaunchStageProcessInfoField launchStageProcessInfo =  launchStageInfo.processes_info.get(0);
        return launchStageProcessInfo.data.get(ticketStageName).get("ticket").toString();
    }

    private static void changeTicketResolveDateInDatabase(SpiritKKT kkt, String stageName) throws IOException {
        class DataField extends HashMap<String, HashMap<String, Object>> {}
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxx", Locale.ENGLISH);

        val dataField = databaseClient.getProcessData(kkt.id);
        DataField data = mapper.readValue(dataField, DataField.class);
        ZonedDateTime prevDateZone = ZonedDateTime.from(formatter.parse((String)data.get(stageName).get("ticket_resolved_datetime")));
        ZonedDateTime newDateZone = prevDateZone.minusDays(1);
        data.get(stageName).put("ticket_resolved_datetime", newDateZone.format(formatter));
        databaseClient.setProcessData(
                mapper.writeValueAsString(data),
                kkt.id
        );
    }
}
