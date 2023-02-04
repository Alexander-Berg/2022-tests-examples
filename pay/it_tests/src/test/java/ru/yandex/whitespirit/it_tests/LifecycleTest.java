package ru.yandex.whitespirit.it_tests;

import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lombok.val;
import ru.yandex.whitespirit.it_tests.templates.TemplatesManager;
import ru.yandex.whitespirit.it_tests.whitespirit.WhiteSpiritManager;
import ru.yandex.whitespirit.it_tests.whitespirit.client.HudsuckerClient;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.whitespirit.it_tests.utils.Constants.HORNS_AND_HOOVES;
import static ru.yandex.whitespirit.it_tests.templates.Template.CONFIGURE_GROUP_REQUEST_BODY;
import static ru.yandex.whitespirit.it_tests.utils.Utils.getMySecret;

public class LifecycleTest {
    private static final WhiteSpiritManager whiteSpiritManager = Context.getWhiteSpiritManager();
    private static final WhiteSpiritClient whiteSpiritClient = whiteSpiritManager.getWhiteSpiritClient();
    private static final HudsuckerClient hudsuckerClient = whiteSpiritManager.getHudsuckerClient();
    private static final TemplatesManager templatesManager = Context.getTemplatesManager();

    private String getSerialNumber() {
        val kktSNs = whiteSpiritManager.getKktSerialNumbersByInn(HORNS_AND_HOOVES.getInn());
        return kktSNs.stream().findAny().orElseThrow(() -> new IllegalStateException("No kkt for the firm."));
    }

    private String getRequestBody(String group, boolean hidden, String logicalState, String kktSn, boolean useComplex) {
        return templatesManager.processTemplate(CONFIGURE_GROUP_REQUEST_BODY,
                Map.of(
                    "group", group,
                    "hidden", hidden,
                    "logical_state", logicalState,
                    "use_complex", useComplex,
                    "mysecret", getMySecret(kktSn)
                ));
    }

    private void cleanUp(String kktSn) {
        val kkt = StreamEx.of(whiteSpiritManager.getKKTs().values())
                .findAny(cash -> cash.getKktSN().equals(kktSn)).orElseThrow();
        val requestBody = getRequestBody("\"" + kkt.getGroup() + "\"", kkt.isHidden(), kkt.getLogicalState(), kktSn, false);
        whiteSpiritClient.configure(kktSn, requestBody).then().statusCode(200);
    }

    @Test
    @DisplayName("Попытка установить неверную группу должна приводить к ошибке")
    public void testWrongGroupConfigure() {
        val kktSn = getSerialNumber();
        val requestBody = getRequestBody("\"!!!\"", false, "OK", kktSn, false);
        whiteSpiritClient.configure(kktSn, requestBody).then().statusCode(400).body("error", equalTo("BadDataInput"),
                "value",
                equalTo("{'custom_data': DataError({'groups': DataError({0: DataError(does not match pattern ^([0-9]|[a-z]|[A-Z]|-|_)+$)})})}"));
    }

    @Test
    @DisplayName("Попытка сбросить список групп должна приводить к установлению группы по умолчанию в кассе")
    public void testEmptyGroups() {
        val kktSn = getSerialNumber();
        val requestBody = getRequestBody("", true, "SOME_STATE", kktSn, true);
        whiteSpiritClient.configure(kktSn, requestBody).then().statusCode(200);
        hudsuckerClient.status(kktSn).then().statusCode(200).body("groups", hasItem("_NOGROUP"));
        cleanUp(kktSn);
    }
}
