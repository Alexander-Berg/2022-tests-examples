package ru.yandex.whitespirit.it_tests;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.restassured.path.json.JsonPath;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.whitespirit.it_tests.templates.DocumentType;
import ru.yandex.whitespirit.it_tests.templates.Payment;
import ru.yandex.whitespirit.it_tests.templates.PaymentType;
import ru.yandex.whitespirit.it_tests.templates.PaymentTypeType;
import ru.yandex.whitespirit.it_tests.templates.Receipt;
import ru.yandex.whitespirit.it_tests.templates.Row;
import ru.yandex.whitespirit.it_tests.templates.TaxType;
import ru.yandex.whitespirit.it_tests.templates.TemplatesManager;
import ru.yandex.whitespirit.it_tests.whitespirit.WhiteSpiritManager;
import ru.yandex.whitespirit.it_tests.whitespirit.client.HudsuckerClient;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.whitespirit.it_tests.templates.Template.RECEIPTS;
import static ru.yandex.whitespirit.it_tests.utils.Constants.HORNS_AND_HOOVES;
import static ru.yandex.whitespirit.it_tests.utils.Utils.checkResponseCode;
import static ru.yandex.whitespirit.it_tests.utils.Utils.executeWithAttempts;

public class DocumentTest {
    private static final WhiteSpiritManager whiteSpiritManager = Context.getWhiteSpiritManager();
    private static final WhiteSpiritClient whiteSpiritClient = whiteSpiritManager.getWhiteSpiritClient();

    private static final HudsuckerClient hudsuckerClient = whiteSpiritManager.getHudsuckerClient();
    private static final TemplatesManager templatesManager = Context.getTemplatesManager();
    private static Set<String> kktSNs;

    @BeforeAll
    public static void openShift() {
        kktSNs = whiteSpiritManager.getKktSerialNumbersByInn(HORNS_AND_HOOVES.getInn());
        kktSNs.forEach(kktSN -> {
            checkResponseCode(whiteSpiritClient.openShift(kktSN));
        });
    }

    @AfterAll
    public static void closeShift() {
        kktSNs.forEach(kktSN -> {
            checkResponseCode(whiteSpiritClient.closeShift(kktSN));
        });
    }

    @Test
    @DisplayName("Получение документа для простого выбитого чека")
    public void testSimpleReceiptDocument() {
        val receiptJson = makeReceiptAndReturnResponseAsJson();
        val documentJson = getDocumentAsJson(receiptJson, false);

        assertThat(documentJson.get("document_type"), equalToIgnoringCase(DocumentType.RECEIPT.getValue()));
        assertThat(documentJson.get("dt"), equalTo(receiptJson.get("dt")));
        assertThat(documentJson.get("id"), equalTo(receiptJson.get("id")));
        assertThat(documentJson.get("receipt_type"), equalTo(receiptJson.get("receipt_content.receipt_type")));
        assertThat(documentJson.getBoolean("ofd_ticket_received"), notNullValue());
    }


    private JsonPath getDocumentAsJson(JsonPath receiptJson, boolean rawForm) {
        val sn = receiptJson.getString("kkt.sn");
        val id = receiptJson.getInt("id");

        return hudsuckerClient.getDocument(sn, id, rawForm, false).then().statusCode(200).extract().body().jsonPath();
    }

    private JsonPath makeReceiptAndReturnResponseAsJson() {
        val rows = List.of(new Row(10, 20, TaxType.NDS_20, PaymentTypeType.PREPAYMENT));
        val payments = List.of(new Payment(200, PaymentType.CARD));
        val receipt = Receipt.builder().firm(HORNS_AND_HOOVES)
                .rows(rows).payments(payments).build();

        val requestBody = templatesManager.processTemplate(
                RECEIPTS,
                Map.of("receipt", receipt)
        );

        val responseBody = executeWithAttempts(() -> whiteSpiritClient.receipt(requestBody).then().statusCode(200)
                .extract().body(), 3, Duration.ofSeconds(10));
        return responseBody.jsonPath();
    }

    @Test
    @DisplayName("Проверка сырой формы документа")
    public void testRawform() {
        val receiptJson = makeReceiptAndReturnResponseAsJson();
        val documentJson = getDocumentAsJson(receiptJson, true);

        assertThat(documentJson.getInt("rawform.TagID"), equalTo(3));
        assertThat(documentJson.getString("rawform.TagType"), equalTo("stlv"));
        assertThat(documentJson.get("rawform.Value"), notNullValue());
    }

    @Test
    @DisplayName("Проверка полной формы документа")
    public void testFullForm() {
        val receiptJson = makeReceiptAndReturnResponseAsJson();
        val documentJson = getDocumentAsJson(receiptJson, false);

        val intFields = List.of("document_index", "id", "shift_number", "fp");

        for (val field : intFields) {
            assertThat(documentJson.getInt("fullform." + field), equalTo(receiptJson.getInt(field)));
        }

        val stringFields = List.of("dt", "firm.inn", "fn.sn", "kkt.rn", "kkt.sn",
                "ofd.check_url", "receipt_content.agent_type", "receipt_content.receipt_type",
                "receipt_content.taxation_type", "receipt_calculated_content.total", "receipt_calculated_content.money_received_total");

        for (val field : stringFields) {
            assertThat(documentJson.getString("fullform." + field), equalTo(receiptJson.getString(field)));
        }
    }

    @Test
    @DisplayName("Проверка печатной формы - она должна порождать ошибку")
    public void testPrintForm() {
        val receiptJson = makeReceiptAndReturnResponseAsJson();
        val sn = receiptJson.getString("kkt.sn");
        val id = receiptJson.getInt("id");
        hudsuckerClient.getDocument(sn, id, false,true).then().statusCode(498)
                .body("value", containsString("Внутренняя ошибка устройство"));
    }
}
