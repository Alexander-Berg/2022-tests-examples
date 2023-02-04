package ru.yandex.darkspirit.it_tests;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.whitespirit.it_tests.templates.Firm;
import ru.yandex.whitespirit.it_tests.templates.TemplatesManager;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static ru.yandex.spirit.it_tests.Utils.*;


@AllArgsConstructor
public class DarkspiritClient {
    private final TemplatesManager templatesManager = new TemplatesManager();

    private final ObjectMapper mapper = new ObjectMapper();

    private static void disableSsl() {
        // Disable SSL certificate validation in RestAssured requests
        RestAssured.useRelaxedHTTPSValidation();
        // Disable SSL certificate validation in swagger-parser requests
        System.setProperty(io.swagger.v3.parser.util.RemoteUrl.class.getName() + ".trustAll", "true");
        System.setProperty(io.swagger.parser.util.RemoteUrl.class.getName() + ".trustAll", "true");
    }

    static {
        disableSsl();
    }

    private final String baseUrl;

    public Response uploadFirmware(String patchFileName) {
        Map<String, Object> parts = Map.of("file", getPatch(patchFileName));
        return execute(Method.POST, "/v1/admin/upload-firmware", parts, empty());
    }

    public Response uploadFirmwareNew(String version, String cashregisterType, String patchFileName) {
        Map<String, Object> parts = Map.of(
                "file", getPatch(patchFileName),
                "cashregister_type", cashregisterType,
                "firmware_name", "dummy",
                "version", version
        );

        return execute(Method.POST, "/v1/admin/upload-firmware-new", parts, empty());
    }

    public Response putFirm(Firm firm) {
        val body = templatesManager.processTemplate(
                "firm.json.flth",
                Map.of("ogrn", firm.getOgrn(),
                        "kpp", firm.getKpp())
        );

        return execute(Method.PUT, String.format("/v1/firms/%s", firm.getInn()), emptyMap(), of(body));
    }

    public Response launchProcess(int cashRegisterId, String processName) {
        return execute(Method.POST, String.format("/v1/process/launch/%d/%s", cashRegisterId, processName), emptyMap(), empty());
    }

    @SneakyThrows
    public Response updateConfigProcess(String processName, Map<String, Object> config) {
        val body = templatesManager.processTemplate(
                "process_update_config.json.flth",
                Map.of("config", mapper.writeValueAsString(config))
        );
        return execute(Method.POST, String.format("/v1/process/update_config/%s", processName), emptyMap(), of(body));
    }

    public Response deleteProcess(int cashRegisterId, String processName) {
        return execute(Method.DELETE, String.format("/v1/process/launch/%d/%s", cashRegisterId, processName), emptyMap(), empty());
    }

    public Response processStatus(String serialNumber, String version, String processName) {
        val url = String.format("/v1/process/status?serial_number=%s&min_sw_version=%s&process_name=%s", serialNumber, version, processName);
        return execute(Method.GET, url, emptyMap(), empty());
    }

    public Response reregisterFetch() {
        return execute(Method.GET, "/v1/reregistrations/reregister-fetch", emptyMap(), empty());
    }

    public Response syncCashregisters() {
        return execute(Method.POST, "/v1/admin/sync-cashregisters", emptyMap(), empty());
    }

    public Response upgradeCrs(Collection<String> sn, String filename, String version) {
        val body = templatesManager.processTemplate(
                "upgrade-crs.json.flth",
                Map.of("serial_numbers", sn, "filename", filename, "fw_version", version)
        );

        return execute(Method.POST, "/v1/admin/upgrade-crs", emptyMap(), of(body));
    }

    @SneakyThrows
    public Response taskRun(String taskName, Map<String, Object> data) {
        val body = templatesManager.processTemplate(
                "task_run.json.flth",
                Map.of("data", mapper.writeValueAsString(data))
        );
        return execute(Method.POST, String.format("/v1/task/%s/run", taskName), emptyMap(), of(body));
    }

    public Response setPassword(int cashRegisterId, String password) {
        val body = templatesManager.processTemplate(
                "set_password_darkspirit.json.flth",
                Map.of("password", password)
        );

        return execute(Method.PATCH, String.format("/v1/cash-registers/%d", cashRegisterId), emptyMap(), of(body));
    }

    public Response launchStage(String processName) {
        val body = templatesManager.processTemplate(
                "launch.json.flth",
                Map.of("process_name", processName)
        );

        return execute(Method.POST, "/v1/process/launch-stage", emptyMap(), of(body));
    }

    public Response setStage(String stageName, int cashRegisterId) {
        return execute(Method.POST, String.format("/v1/process/%d/set_stage/%s", cashRegisterId, stageName), emptyMap(), empty());
    }

    public Response applyMaintenanceAction(int cashRegisterId, String maintenanceActionName) {
        return execute(
                Method.POST, String.format("/v1/process/%d/apply_maintenance_action/%s", cashRegisterId,
                        maintenanceActionName), emptyMap(), empty()
        );
    }

    public Response changeState(int cashRegisterId, String targetState, String reason, Boolean skip_checks) {
        val body = templatesManager.processTemplate(
                "change_state.json.flth",
                Map.of("target_state", targetState, "reason", reason, "skip_checks", skip_checks.toString())
        );

        return execute(Method.POST, String.format("/v1/cash-registers/%d/change_state", cashRegisterId), emptyMap(), of(body));
    }

    public Response register(String kktSerialNumber) {
        val body = templatesManager.processTemplate(
                "register_darkspirit.json.flth",
                Map.of("sn", kktSerialNumber)
        );

        return execute(Method.POST, "/v1/registrations/register", emptyMap(), of(body));
    }

    public Response closeFiscalMode(int cashRegisterId, String reason, Boolean startReregistration) {
        val body = templatesManager.processTemplate(
                "close_fiscal_mode_darkspirit.json.flth",
                Map.of("reason", reason, "start_reregistration", startReregistration.toString())
        );

        return execute(Method.POST, String.format("/v1/cash-registers/%d/close_fiscal_mode", cashRegisterId), emptyMap(), of(body));
    }

    public Response createApplications(Boolean bso, String kktSerialNumber, String inn) {
        val body = templatesManager.processTemplate(
                "create_applications.json.flth",
                Map.of("bso", bso.toString(),"sn", kktSerialNumber, "inn", inn)
        );

        return execute(Method.POST, "/v1/registrations/create-applications", emptyMap(), of(body));
    }

    public Response configure(File file) {
        Map<String, Object> parts = Map.of("file", file);
        return execute(Method.POST, "/v1/registrations/configure", parts, empty());
    }

    private Response execute(Method method, String path, Map<String, Object> multiparts, Optional<String> body) {
        RequestSpecification request = prepare_request_specification(baseUrl, multiparts, body);
        return request.request(method, path);
    }

    private static File getPatch(String patchName) {
        return getFile(String.format("whitespirit/patches/starrus/%s", patchName));
    }
}
