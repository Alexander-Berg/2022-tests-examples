package ru.yandex.spirit.it_tests;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.spirit.it_tests.configuration.SpiritKKT;
import ru.yandex.darkspirit.it_tests.template_classes.OfdSyncBodyEntry;
import ru.yandex.whitespirit.it_tests.templates.TemplatesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static java.util.Collections.emptyMap;
import static ru.yandex.spirit.it_tests.Utils.getFile;
import static ru.yandex.whitespirit.it_tests.utils.Utils.generateRNM;


@UtilityClass
public class WireMocker {
    private static final String wiremockHost = IntegrationManager.getWiremockHost();
    private static final int wiremockPort = IntegrationManager.getWiremockPort();
    private final TemplatesManager templatesManager = new TemplatesManager();

    static {
        WireMock.configureFor(wiremockHost, wiremockPort);
    }

    @SneakyThrows
    public static void setUpOfdMock(List<SpiritKKT> kkts, int count) {
        ObjectMapper mapper = new ObjectMapper();
        List<OfdSyncBodyEntry> bodyObject = new ArrayList<>();
        for (val kkt: kkts) {
            OfdSyncBodyEntry entry = new OfdSyncBodyEntry(kkt.fnSN, generateRNM(kkt.inn, kkt.kktSN), count);
            bodyObject.add(entry);
        }
        String bodyString = mapper.writeValueAsString(bodyObject);
        makeJsonWiremockStub(
                200,
                post(urlEqualTo("/fns_send_status")),
                bodyString
        );
        fnsSendMock("true");
    }

    public static void fnsSendMock(String success) {
        String body = String.format("[{\"success\":%s}]", success);
        makeJsonWiremockStub(
                200,
                post(urlPathMatching("/kkts/.*")),
                body
        );
        makeJsonWiremockStub(
                200,
                get(urlPathMatching("/kkts/.*")),
                body
        );
    }

    public static void setUpBotMock(String ticket) {
        makeJsonWiremockStub(
                200,
                get(urlPathMatching("/dc/.*")),
                String.format("{\"result\":{\"StNum\":\"%s\"}}", ticket)
        );
    }

    public static void setUpYSignGetUserCertList() {
        val body = templatesManager.processTemplate(
                "mock_ysign_get_user_cert_list.json.flth", emptyMap()
        );
        makeJsonWiremockStub(
                200,
                post(urlEqualTo("/api/cert/getUserCertList")),
                body
        );
    }

    public static void setUpYSignUniSignCMS() {
        val body = templatesManager.processTemplate(
                "mock_ysign_uni_sign_cms.json.flth", emptyMap()
        );
        makeJsonWiremockStub(
                200,
                post(urlEqualTo("/api/sign/uniSignCMS")),
                body
        );
    }

    public static void setUpFnsApiRegisterMock() {
        val body = templatesManager.processTemplate(
                "mock_fns_api_register.json.flth", emptyMap()
        );
        makeJsonWiremockStub(
                200,
                post(urlPathMatching(".*/rkktapi/kkt/applications/register")),
                body
        );
    }

    public static void setUpFnsApiError(String code, String message) {
        val body = templatesManager.processTemplate(
                "mock_fns_api_error.json.flth",
                Map.of(
                        "code", code,
                        "message", message
                )
        );
        makeJsonWiremockStub(
                422,
                get(urlPathMatching(".*/rkktapi/kkt/documents/register/.*")),
                body
        );
    }

    public static void setUpFnsApiRegisterStatusNotFoundMock() {
        setUpFnsApiError("application.by.request.not.found", "Заявление по заявке не найдено");
    }

    public static void setUpFnsApiRegisterStatusInProcess() {
        val body = templatesManager.processTemplate(
                "mock_fns_api_register_status_in_process.json.flth", emptyMap()
        );
        makeJsonWiremockStub(
                200,
                get(urlPathMatching(".*/rkktapi/kkt/documents/register/.*")),
                body
        );
    }

    public static void setUpFnsApiRegisterStatusRejected(int rejectionCode, String rejectionReason) {
        val body = templatesManager.processTemplate(
                "mock_fns_api_register_status_rejected.json.flth",
                Map.of(
                        "rejectionCode", rejectionCode, "rejectionReason", rejectionReason
                )
        );
        makeJsonWiremockStub(
                200,
                get(urlPathMatching(".*/rkktapi/kkt/documents/register/.*")),
                body
        );
    }

    @SneakyThrows
    public static void setUpFnsApiRegisterStatusFoundMock(SpiritKKT kkt) {
        String encodedBase64String = getDocument(kkt);
        val body = templatesManager.processTemplate(
                "mock_fns_api_register_status_found.json.flth",
                Map.of("documentBase64", encodedBase64String)
        );
        makeJsonWiremockStub(
                200,
                get(urlPathMatching(".*/rkktapi/kkt/documents/register/.*")),
                body
        );
    }

    @SneakyThrows
    private static String getDocument(SpiritKKT kkt) {
        byte[] inFileBytes =  FileUtils.readFileToByteArray(
                getFile(String.format("rereg_cards/rereg_card_%s_%s.pdf", kkt.kktSN, kkt.fnSN))
        );
        byte[] encoded = java.util.Base64.getEncoder().encode(inFileBytes);
        return new String(encoded);
    }

    @SneakyThrows
    public static void setUpFnsApiBasicMocks() {
        setUpFnsApiMaintenanceMock();
        setUpFnsApiHealthMock();
        setUpFnsApiKktModelsMock();
        setUpFnsApiFnModelsMock();
        setUpFnsApiValidateFnMock();
        setUpFnsApiValidateKktMock();
    };

    private static void setUpFnsApiValidateFnMock() {
        val body = templatesManager.processTemplate(
                "mock_fns_api_validate_fn.json.flth",
                emptyMap()
        );
        makeJsonWiremockStub(
                200,
                post(urlPathMatching(".*/rkktapi/validations/kkt/fn/serial")),
                body
        );
    }

    private static void setUpFnsApiValidateKktMock() {
        val body = templatesManager.processTemplate(
                "mock_fns_api_validate_kkt.json.flth",
                emptyMap()
        );
        makeJsonWiremockStub(
                200,
                post(urlPathMatching(".*/rkktapi/validations/kkt/serial")),
                body
        );
    }

    @SneakyThrows
    private static void setUpFnsApiKktModelsMock() {
        val body = templatesManager.processTemplate("mock_fns_api_kkt_models.json.flth", emptyMap());
        makeJsonWiremockStub(
                200,
                get(urlPathMatching(".*/rkktapi/dicts/kkt/models")),
                body
        );
    }

    @SneakyThrows
    private static void setUpFnsApiFnModelsMock() {
        val body = templatesManager.processTemplate("mock_fns_api_fn_models.json.flth", emptyMap());
        makeJsonWiremockStub(
                200,
                get(urlPathMatching(".*/rkktapi/dicts/kkt/fn/models")),
                body
        );
    }

    @SneakyThrows
    private static void setUpFnsApiMaintenanceMock() {
        makeJsonWiremockStub(
                200,
                get(urlPathMatching(".*/rkktapi/health/scheduled_maintenance")),
                "[]"
        );
    }

    @SneakyThrows
    private static void setUpFnsApiHealthMock() {
        val body = templatesManager.processTemplate("mock_fns_api_health.json.flth", emptyMap());
        makeJsonWiremockStub(
                200,
                get(urlPathMatching(".*/rkktapi/health/current")),
                body
        );
    }

    private static void makeJsonWiremockStub(int statusCode, MappingBuilder stb, String body) {
        stubFor(stb
                .willReturn(
                        aResponse()
                                .withStatus(statusCode)
                                .withHeader("Content-Type", "application/json")
                                .withBody(body)
                )
        );
    }
}
