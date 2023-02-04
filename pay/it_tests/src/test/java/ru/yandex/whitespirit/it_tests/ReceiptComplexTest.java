package ru.yandex.whitespirit.it_tests;

import java.util.Map;
import java.util.stream.Stream;

import io.restassured.response.Response;
import lombok.SneakyThrows;
import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import ru.yandex.whitespirit.it_tests.configuration.KKT;
import ru.yandex.whitespirit.it_tests.templates.Template;
import ru.yandex.whitespirit.it_tests.templates.TemplatesManager;
import ru.yandex.whitespirit.it_tests.templates.VariableDocumentData;
import ru.yandex.whitespirit.it_tests.whitespirit.WhiteSpiritManager;
import ru.yandex.whitespirit.it_tests.whitespirit.client.HudsuckerClient;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import static org.hamcrest.Matchers.equalTo;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static ru.yandex.whitespirit.it_tests.templates.Template.RECEIPT_WITH_ITEM_CODE_REQUEST_BODY;
import static ru.yandex.whitespirit.it_tests.utils.Constants.HORNS_AND_HOOVES;
import static ru.yandex.whitespirit.it_tests.utils.Utils.checkResponseCode;
import static ru.yandex.whitespirit.it_tests.utils.Utils.executeWithAttempts;

public class ReceiptComplexTest {
    private static final WhiteSpiritManager whiteSpiritManager = Context.getWhiteSpiritManager();
    private static final WhiteSpiritClient whiteSpiritClient = whiteSpiritManager.getWhiteSpiritClient();
    private static final TemplatesManager templatesManager = Context.getTemplatesManager();
    private static final HudsuckerClient hudsuckerClient = whiteSpiritManager.getHudsuckerClient();
    private static KKT correctionKKT;

    @BeforeAll
    public static void getCorrectionKKT() {
        correctionKKT = StreamEx.of(whiteSpiritManager.getKKTs().values())
                .findAny(kkt -> kkt.getGroup().equals("CORRECTION")).orElseThrow();
    }

    @AfterAll
    public static void closeShift() {
        checkResponseCode(whiteSpiritClient.closeShift(correctionKKT.getKktSN()));
    }

    private Response makeReceiptComplexWithItemCode(String itemCode) {
        val requestBody = templatesManager.processTemplate(
                RECEIPT_WITH_ITEM_CODE_REQUEST_BODY,
                Map.of("firm", HORNS_AND_HOOVES, "item_code", itemCode));

        return whiteSpiritClient.makeReceiptComplex(correctionKKT.getKktSN(), requestBody);
    }

    @ParameterizedTest(name = "{index}. item_code = {0}")
    @ValueSource(strings = {"010460043993125621JgXJ5", "010460406000600021N4N57RSCBUZTQ"})
    @DisplayName("item_code должен корректно обрабатываться в ручке complex и совпадать с тем, что выбивает касса " +
            "обычной ручкой")
    public void receiptWithItemCodeField(String itemCode) {
        val itemCodePath = "receipt_content.rows[0].item_code";
        val receiptId = makeReceiptComplexWithItemCode(itemCode).then()
                .statusCode(200).extract().body().jsonPath().getInt("id");

        executeWithAttempts(() -> hudsuckerClient.getDocument(correctionKKT.getKktSN(), receiptId, false, false)
                .then()
                .statusCode(200), 3)
                .body("fullform." + itemCodePath, equalTo("444D+" + itemCode));
    }


    @SneakyThrows
    @DisplayName("Проверка корректного пробития чека коррекциии с использованием ручки Complex")
    @Test
    public void testCorrectionReceipt() {
        val body = templatesManager.processTemplate(Template.CORRECTION_RECEIPT, Map.of("firm", HORNS_AND_HOOVES));
        val receiptId = whiteSpiritClient.makeReceiptComplex(correctionKKT.getKktSN(), body)
                .then().statusCode(200).extract().body().jsonPath().getInt("id");

        val response = whiteSpiritClient.getDocument(correctionKKT.getKktSN(), receiptId, true, false)
                .then().statusCode(200).extract().body();


        val responseKkt = new KKT(
                response.jsonPath().getString("fullform.kkt.sn"),
                response.jsonPath().getString("fullform.fn.sn"),
                "", false, "", false, "",
                false, "", true
        );
        val expectedResponse = templatesManager.processTemplate(
                Template.GET_CORRECTION_DOCUMENT,
                Map.of("firm", HORNS_AND_HOOVES,
                        "kkt", correctionKKT.isUseVirtualFn() ? correctionKKT : responseKkt,
                        "data", new VariableDocumentData(
                                response.jsonPath().getString("dt"),
                                response.jsonPath().getInt("id"),
                                response.jsonPath().getInt("fp"),
                                response.jsonPath().getString("fullform.receipt_extra_content.fp"),
                                response.jsonPath().getString("fullform.kkt.rn"),
                                response.jsonPath().getInt("fullform.shift_number")
                        )
                )
        );

        val sensitiveData = new String[]{"fullform.check_url", "fullform.fn.sn", "fullform.fp", "fullform.kkt.rn",
                "fullform.kkt.sn", "fullform.receipt_extra_content.fp", "fp",
                "fullform.document_index", "fullform.id", "id", "fullform.shift_number"};
        val customizations =
                Stream.of(sensitiveData).map(path -> new Customization(path, (o1, o2) -> true)).toArray(Customization[]::new);
        val comparator = new CustomComparator(JSONCompareMode.NON_EXTENSIBLE, customizations);

        assertEquals(expectedResponse, response.asString(), comparator);
    }
}