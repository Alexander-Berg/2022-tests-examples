package ru.yandex.whitespirit.it_tests.whitespirit.client;

import java.util.EnumSet;

import io.restassured.http.Header;
import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.yandex.whitespirit.it_tests.utils.Constants;

@AllArgsConstructor
@Getter
enum HttpPathTemplate {
    CONFIGURE("/v1/cashmachines/{kktSN}/configure"),
    REGISTER("/v1/cashmachines/{kktSN}/register"),
    CLEAR_DEBUG_FN("/v1/cashmachines/{kktSN}/clear_debug_fn?mysecret={mysecret}"),
    OPEN_SHIFT("/v1/cashmachines/{kktSN}/open_shift"),
    CLOSE_SHIFT("/v1/cashmachines/{kktSN}/close_shift"),
    RECEIPTS("/v1/receipts?wait4free=10"),
    RECEIPTS_WITH_GROUP("/v1/receipts?wait4free=10&group={group}"),
    CASHMACHINES("/v1/cashmachines"),
    INFO("/v1/info"),
    PING("/v1/ping", Constants.JSON_ACCEPT_HEADER, EnumSet.allOf(TemplateOptions.class)),
    SSH_PING("/v1/cashmachines/{kktSN}/ssh_ping?use_password={useSshPassword}"),
    GET_PASSWORD("/v1/cashmachines/{kktSN}/get_admin_password?use_password={useSshPassword}"),
    SETUP_SSH_CONNECTION("/v1/cashmachines/{kktSN}/setup_ssh_connection"),
    STATUS("/v1/cashmachines/{kktSN}/status"),
    UPLOAD("/upload", Constants.JSON_ACCEPT_HEADER, EnumSet.noneOf(TemplateOptions.class)),
    UPLOADS("/v1/uploads"),
    IDENT("/v1/cashmachines/{kktSN}/ident?on_={on}"),
    LOG("/v1/cashmachines/{kktSN}/log?log_size=65535", Constants.TEXT_PLAIN_ACCEPT_HEADER),
    SET_DATETIME("/v1/cashmachines/{kktSN}/set_datetime?dt={dt}"),
    GET_DOCUMENT("/v1/cashmachines/{kktSN}/document/{documentNumber}?with_fullform={withFullForm}&with_rawform={withRawForm}&with_printform={withPrintForm}",
            Constants.JSON_ACCEPT_HEADER),
    GET_DOCUMENT_SCHEMALESS("/v1/cashmachines/{kktSN}/document/{documentNumber}?with_fullform={withFullForm}&with_rawform={withRawForm}&with_printform={withPrintForm}",
            Constants.JSON_ACCEPT_HEADER, EnumSet.noneOf(TemplateOptions.class)),
    HUDSUCKER("/hudsucker", Constants.JSON_ACCEPT_HEADER, EnumSet.noneOf(TemplateOptions.class)),
    REBOOT("/v1/cashmachines/{kktSN}/reboot?cold={cold}"),
    UPGRADE_USING_SSH("/v1/cashmachines/{kktSN}/upgrade?use_ssh=true&filename={filename}"),
    RECEIPTS_COMPLEX("/v1/cashmachines/{kktSN}/make_receipt_complex"),
    ;


    String template;
    Header acceptHeader;
    EnumSet<TemplateOptions> templateOptions;

    HttpPathTemplate(String template) {
        this(template, Constants.JSON_ACCEPT_HEADER);
    }

    HttpPathTemplate(String template, Header acceptHeader) {
        this(template, acceptHeader, EnumSet.of(TemplateOptions.OPEN_API_VALIDATION));
    }

    public boolean isQueryPathArgs() {
        return templateOptions.contains(TemplateOptions.QUERY_PATH_ARGS);
    }

    public boolean isValidated() {
        return templateOptions.contains(TemplateOptions.OPEN_API_VALIDATION);
    }
}
