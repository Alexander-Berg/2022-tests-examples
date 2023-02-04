package ru.yandex.whitespirit.it_tests.whitespirit.client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.val;

import static io.restassured.RestAssured.given;
import static io.restassured.http.Method.GET;
import static io.restassured.http.Method.POST;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static ru.yandex.whitespirit.it_tests.utils.Constants.CONTENT_TYPE_HEADER;
import static ru.yandex.whitespirit.it_tests.whitespirit.client.HttpPathTemplate.*;
public class WhiteSpiritClient {
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
    private final OpenApiValidationFilter filter;

    public WhiteSpiritClient(String baseUrl) {
        this(baseUrl, new OpenApiValidationFilter(baseUrl + "/swagger.yaml"));
    }

    public WhiteSpiritClient(String baseUrl, OpenApiValidationFilter filter) {
        this.baseUrl = baseUrl;
        this.filter = filter;
    }

    public Response info() {
        return doGET(INFO, emptyMap());
    }

    public Response uploads() {
        return doGET(UPLOADS, emptyMap());
    }

    public Response ping() {
        return ping(emptyMap());
    }

    public Response ping(Map<String, String> params) {        
        return doGET(PING, params);
    }

    public Response sshPing(String kktSerialNumber, boolean usePassword) {
        return doGET(SSH_PING, Map.of("kktSN", kktSerialNumber, "useSshPassword", String.valueOf(usePassword)));
    }

    public Response upgradeUsingSsh(String kktSerialNumber, String filename) {
        return doPOST(UPGRADE_USING_SSH, Map.of("kktSN", kktSerialNumber, "filename", filename), empty());
    }

    public Response getPassword(String kktSerialNumber, boolean usePassword) {
        return doGET(GET_PASSWORD, Map.of("kktSN", kktSerialNumber, "useSshPassword", String.valueOf(usePassword)));
    }

    public Response setupSshConnection(String kktSerialNumber) {
        return doPOST(SETUP_SSH_CONNECTION, Map.of("kktSN", kktSerialNumber), empty());
    }

    public Response closeShift(String kktSerialNumber) {
        return doPOST(CLOSE_SHIFT, Map.of("kktSN", kktSerialNumber), "{}");
    }

    public Response openShift(String kktSerialNumber) {
        return doRequest(OPEN_SHIFT, POST, empty(), Map.of("kktSN", kktSerialNumber), empty());
    }

    public Response reboot(String kktSerialNumber, String cold) {
        return doPOST(REBOOT, Map.of("kktSN", kktSerialNumber, "cold", cold), empty());
    }

    public Response configure(String kktSerialNumber, String body) {
        return doPOST(CONFIGURE, Map.of("kktSN", kktSerialNumber), body);
    }

    public Response register(String kktSerialNumber, String body) {
        return doPOST(REGISTER, Map.of("kktSN", kktSerialNumber), body);
    }

    public Response clearDebugFn(String kktSerialNumber, String secret, String body) {
        return doPOST(CLEAR_DEBUG_FN, Map.of("kktSN", kktSerialNumber, "mysecret", secret), body);
    }

    public Response receipt(String body) {
        return doPOST(RECEIPTS, emptyMap(), body);
    }

    public Response receipt(String body, String group) {
        return doPOST(RECEIPTS_WITH_GROUP, Map.of("group", group), body);
    }

    public Response makeReceiptComplex(String kktSN, String body) {
        return doPOST(RECEIPTS_COMPLEX, Map.of("kktSN", kktSN), body);
    }

    public Response ident(String kktSerialNumber, boolean on) {
        return doRequest(IDENT, POST, empty(), Map.of("kktSN", kktSerialNumber, "on", String.valueOf(on)), empty());
    }

    public Response cashmachines() {
        return doGET(CASHMACHINES, emptyMap());
    }

    public Response upload(File file) {
        return doRequest(UPLOAD, POST, empty(), emptyMap(), of(file));
    }

    public Response log(String kktSerialNumber) {
        return doGET(LOG, Map.of("kktSN", kktSerialNumber));
    }

    public Response setDatetime(String kktSerialNumber, String datetime) {
        return doPOST(SET_DATETIME, Map.of("kktSN", kktSerialNumber, "dt", datetime), empty());
    }

    public Response status(String kktSerialNumber) {
        return doGET(HttpPathTemplate.STATUS, Map.of("kktSN", kktSerialNumber));
    }

    public Response getDocument(String kktSerialNumber, int receiptId, boolean rawForm, boolean printForm) {
        val args = Map.of(
                "kktSN", kktSerialNumber,
                "documentNumber", Integer.toString(receiptId),
                "withFullForm", "true",
                "withRawForm", String.valueOf(rawForm),
                "withPrintForm", String.valueOf(printForm));
        if (rawForm) {
            return  doGET(GET_DOCUMENT_SCHEMALESS, args);
        }
        return doGET(GET_DOCUMENT, args);
    }

    protected Response doGET(HttpPathTemplate httpPathTemplate, Map<String, String> pathArgs) {
        return doRequest(httpPathTemplate, GET, empty(), pathArgs, empty());
    }

    private Response doPOST(HttpPathTemplate httpPathTemplate, Map<String, String> pathArgs, String body) {
        return doPOST(httpPathTemplate, pathArgs, of(body));
    }

    private Response doPOST(HttpPathTemplate httpPathTemplate, Map<String, String> pathArgs, Optional<String> body) {
        return doRequest(httpPathTemplate, POST, body, pathArgs, empty());
    }

    private Response doRequest(HttpPathTemplate pathTemplate, Method method, Optional<String> bodyOpt, Map<String, String> pathArgs,
                               Optional<File> file) {
        RequestSpecification requestTemplate = bodyOpt
                .map(body -> given().body(body).header(CONTENT_TYPE_HEADER))
                .orElse(given());

        val filters = new ArrayList<>(List.of(new RequestLoggingFilter(), new ResponseLoggingFilter()));

        if (pathTemplate.isValidated()) {
            filters.add(filter);
        }

        if (file.isPresent()) {
            requestTemplate = requestTemplate.multiPart("file", file.get());
        }

        return requestTemplate
                .filters(filters)
                .baseUri(baseUrl)
                .header(pathTemplate.getAcceptHeader())
                .pathParams(pathTemplate.isQueryPathArgs() ? emptyMap() : pathArgs)
                .params(pathTemplate.isQueryPathArgs() ? pathArgs : emptyMap())
                .request(method, pathTemplate.getTemplate());
    }
}
