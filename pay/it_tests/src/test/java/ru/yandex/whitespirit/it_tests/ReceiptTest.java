package ru.yandex.whitespirit.it_tests;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.UtilityClass;
import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.whitespirit.it_tests.configuration.KKT;
import ru.yandex.whitespirit.it_tests.templates.Firm;
import ru.yandex.whitespirit.it_tests.templates.FiscalDocumentType;
import ru.yandex.whitespirit.it_tests.templates.Payment;
import ru.yandex.whitespirit.it_tests.templates.PaymentType;
import ru.yandex.whitespirit.it_tests.templates.PaymentTypeType;
import ru.yandex.whitespirit.it_tests.templates.Receipt;
import ru.yandex.whitespirit.it_tests.templates.ReceiptCalculated;
import ru.yandex.whitespirit.it_tests.templates.Row;
import ru.yandex.whitespirit.it_tests.templates.TaxType;
import ru.yandex.whitespirit.it_tests.templates.TemplatesManager;
import ru.yandex.whitespirit.it_tests.whitespirit.WhiteSpiritManager;
import ru.yandex.whitespirit.it_tests.whitespirit.client.HudsuckerClient;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static ru.yandex.whitespirit.it_tests.ComparisonUtils.areEquals;
import static ru.yandex.whitespirit.it_tests.ComparisonUtils.areSameUnordered;
import static ru.yandex.whitespirit.it_tests.templates.Template.RECEIPTS;
import static ru.yandex.whitespirit.it_tests.templates.Template.RECEIPTS_CALCULATED;
import static ru.yandex.whitespirit.it_tests.templates.Template.RECEIPTS_WITH_CASHREGISTER_PARAMS;
import static ru.yandex.whitespirit.it_tests.templates.Template.RECEIPT_WITH_ITEM_CODE_REQUEST_BODY;
import static ru.yandex.whitespirit.it_tests.templates.Template.SIMPLE_RECEIPTS;
import static ru.yandex.whitespirit.it_tests.templates.Template.SIMPLE_RECEIPTS_WITH_COMPOSITE_ID;
import static ru.yandex.whitespirit.it_tests.utils.Constants.HERKULES;
import static ru.yandex.whitespirit.it_tests.utils.Constants.HORNS_AND_HOOVES;
import static ru.yandex.whitespirit.it_tests.utils.Utils.checkResponseCode;
import static ru.yandex.whitespirit.it_tests.utils.Utils.executeWithAttempts;

@UtilityClass
class ComparisonUtils {
    private static final double EPSILON = 0.01;

    static boolean areEquals(double left, double right) {
        return Math.abs(left - right) <= EPSILON;
    }

    static <T> boolean areSameUnordered(List<T> left, List<T> right) {
        if (left.size() != right.size()) {
            return false;
        }

        val rightCopy = new ArrayList<>(right);
        for (val item : left) {
            if (!rightCopy.remove(item)) {
                return false;
            }
        }

        return true;
    }
}

@Value
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class RowDto {
    PaymentTypeType paymentTypeType;
    double price;
    double qty;
    TaxType taxType;
    String text;
    double amount;
    double taxPct;
    double taxAmount;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null || getClass() != that.getClass()) {
            return false;
        }
        val dto = (RowDto) that;
        return areEquals(dto.price, price)
                && areEquals(dto.qty, qty)
                && areEquals(dto.amount, amount)
                && areEquals(dto.taxPct, taxPct)
                && areEquals(dto.taxAmount, taxAmount)
                && paymentTypeType == dto.paymentTypeType
                && taxType == dto.taxType
                && Objects.equals(text, dto.text);
    }
}

@Value
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class TaxTotalDto {
    TaxType taxType;
    double taxPct;
    double taxAmount;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null || getClass() != that.getClass()) {
            return false;
        }
        val dto = (TaxTotalDto) that;
        return areEquals(dto.taxPct, taxPct)
                && areEquals(dto.taxAmount, taxAmount)
                && taxType == dto.taxType;
    }
}

@Value
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class TotalDto {
    PaymentType paymentType;
    double amount;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null || getClass() != that.getClass()) {
            return false;
        }
        val dto = (TotalDto) that;
        return areEquals(dto.amount, amount)
                && paymentType == dto.paymentType;
    }
}

@Value
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class ReceiptDto {
    double total;
    double moneyReceivedTotal;
    List<RowDto> rows;
    List<TaxTotalDto> taxTotals;
    List<TotalDto> totals;
    String firmReplyEmail;
    String firmUrl;
    String qr;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null || getClass() != that.getClass()) {
            return false;
        }
        ReceiptDto dto = (ReceiptDto) that;
        return areEquals(dto.total, total)
                && areEquals(dto.moneyReceivedTotal, moneyReceivedTotal)
                && areSameUnordered(rows, dto.rows)
                && areSameUnordered(taxTotals, dto.taxTotals)
                && areSameUnordered(totals, dto.totals)
                && Objects.equals(firmReplyEmail, dto.firmReplyEmail)
                && Objects.equals(firmUrl, dto.firmUrl)
                && Objects.equals(qr, dto.qr);
    }
}

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class GroupDto {
    String group;
    int readyCashmachines;
}

@Value
class FirmDto {
    String inn;
    List<GroupDto> groups;
}

@Value
class InnGroupPair {
    String inn;
    String group;
}

public class ReceiptTest {
    private static final WhiteSpiritManager whiteSpiritManager = Context.getWhiteSpiritManager();
    private static final WhiteSpiritClient whiteSpiritClient = whiteSpiritManager.getWhiteSpiritClient();
    private static final HudsuckerClient hudsuckerClient = whiteSpiritManager.getHudsuckerClient();
    private static final TemplatesManager templatesManager = Context.getTemplatesManager();
    private static Set<String> kktSNs;
    private static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat DATE_GENERATOR = new SimpleDateFormat("yyyyMMdd'T'HHmm");

    private static boolean check(String inn, String group) {
        try {
            val firms = hudsuckerClient.hudsucker()
                    .then().extract().jsonPath()
                    .getList("firms", FirmDto.class);
            val count = StreamEx.of(firms)
                    .filter(f -> f.getInn().equals(inn))
                    .flatMap(f -> f.getGroups().stream())
                    .filter(g -> g.getGroup().equals(group))
                    .map(g -> g.getReadyCashmachines())
                    .findAny()
                    .orElse(0);
            return count > 0;
        } catch (ClassCastException e) {
            // Hudsucker serializes empty list of firms as empty object. Hail Lua!
            return false;
        }
    }

    @SneakyThrows
    @BeforeAll
    public static void openShift() {
        kktSNs = StreamEx.of(whiteSpiritManager.getKKTs().values())
                .map(KKT::getKktSN)
                .toImmutableSet();
        kktSNs.forEach(kktSN -> {
            checkResponseCode(whiteSpiritClient.openShift(kktSN));
        });

        val groups = StreamEx.of(whiteSpiritManager.getKKTs().values())
                .map(kkt -> new InnGroupPair(kkt.getInn(), kkt.getGroup()))
                .toArray(InnGroupPair[]::new);
        // Хадсакер обновляет список доступных касс раз в 5-6 секунд. Нужна задержка после открытия смен.
        awaitInnGroups(groups);
    }

    static void awaitInnGroups(InnGroupPair... innGroups) {
        await().pollInterval(5, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> StreamEx.of(innGroups).allMatch(pair -> check(pair.getInn(), pair.getGroup())));
    }

    @AfterAll
    public static void closeShift() {
        kktSNs.forEach(kktSN -> checkResponseCode(whiteSpiritClient.closeShift(kktSN)));
    }

    private String getSerialNumber() {
        return kktSNs.stream().findAny().orElseThrow(() -> new IllegalStateException("No kkt for the firm."));
    }

    @Test
    @DisplayName("Повторное открытие открытой кассы должно приводить к ошибке")
    public void testOpenTwice() {
        val serialNumber = getSerialNumber();
        whiteSpiritClient.openShift(serialNumber).then().statusCode(498)
                .body("error", equalTo("StarrusDeviceException"))
                .body("value", containsString("Смена открыта"));
    }

    @Test
    @DisplayName("/set_datetime должен не срабатывать для открытой кассы")
    public void testSetDatetime() {
        val serialNumber = getSerialNumber();
        whiteSpiritClient.setDatetime(serialNumber, "").then().statusCode(499);
    }

    @DisplayName("/ping с параметрами должен возвращать ожидаемый статус")
    @ParameterizedTest(name = "ИНН - {0}. Ожидаемый статус {1}")
    @MethodSource("pingArgumentsProvider")
    public void testFirms(String inn, int status) {
        given().ignoreExceptions().pollInterval(30, TimeUnit.SECONDS).await().atMost(
                2, TimeUnit.MINUTES
        ).until(() -> {
                whiteSpiritClient.ping(Map.of("firm_inn", inn, "group", "_NOGROUP")).then().statusCode(status);
                return true;
        });
    }

    static Stream<Arguments> pingArgumentsProvider() {
        return Stream.of(Arguments.arguments("13666", 429), Arguments.arguments(HORNS_AND_HOOVES.getInn(), 200));
    }

    private ValidatableResponse getDocument(String kktSN, int receiptId) {
        return executeWithAttempts(() -> hudsuckerClient.getDocument(kktSN, receiptId, false, false)
                .then()
                .statusCode(200), 3);
    }

    @Test
    @DisplayName("item_code должен корректно обрабатываться и возвращаться как в ответе ручки receipts, так и при " +
            "запросе документа по номеру.")
    @RunOnlyIfMGMAreEnabled
    public void receiptWithItemCodeField() {
        val itemCode = "010460043993125621JgXJ5";
        val itemCodePath = "receipt_content.rows[0].item_code";
        val receiptsResponse = makeReceiptWithItemCode(itemCode);
        receiptsResponse.then()
                .statusCode(200)
                .body(itemCodePath, equalTo(itemCode));

        val receiptId = receiptsResponse.jsonPath().getInt("id");
        val kktSN = receiptsResponse.jsonPath().getString("kkt.sn");
        getDocument(kktSN, receiptId).body("fullform." + itemCodePath, equalTo("444D+" + itemCode));
    }

    private void assertUserRequisite(String contentPrefix, String paymentIdType, String paymentId, ValidatableResponse response) {
        response.assertThat()
                .body(contentPrefix + ".additional_user_requisite.name", equalTo(paymentIdType))
                .body(contentPrefix + ".additional_user_requisite.value", equalTo(paymentId))
                .body(contentPrefix, not(hasKey("composite_event_id")));
    }

    @Test
    @DisplayName("Идентификация чека через UserRequisite")
    public void receiptUserRequisite() {
        val requestBody = templatesManager.processTemplate(
                SIMPLE_RECEIPTS, Map.of("firm", HORNS_AND_HOOVES));

        val receiptsResponseResponse = whiteSpiritClient
                .receipt(requestBody).then().statusCode(200);

        assertUserRequisite(
                "receipt_content", "trust_id", "06e62000-9cc1-436c-a6db-d64469fa3ab4",
                receiptsResponseResponse
        );

        val receiptId = receiptsResponseResponse.extract().jsonPath().getInt("id");
        val kktSN = receiptsResponseResponse.extract().jsonPath().getString("kkt.sn");

        val getDocumentResponse = getDocument(kktSN, receiptId).statusCode(200);
        assertUserRequisite(
                "fullform.receipt_content", "trust_id", "06e62000-9cc1-436c-a6db-d64469fa3ab4",
                getDocumentResponse
        );
    }

    private void assertCompositeEventId(String contentPrefix,
                                        String paymentIdType, String paymentId, String eventId,
                                        ValidatableResponse response) {
        response.assertThat()
                .body(contentPrefix + ".additional_user_requisite.name", equalTo(paymentIdType))
                .body(contentPrefix + ".additional_user_requisite.value", equalTo(paymentId+"|"+eventId))
                .body(contentPrefix + ".composite_event_id.payment_id_type", equalTo(paymentIdType))
                .body(contentPrefix + ".composite_event_id.payment_id", equalTo(paymentId))
                .body(contentPrefix + ".composite_event_id.event_id", equalTo(eventId));
    }

    @Test
    @DisplayName("Идентификация чека через CompositePaymentId")
    public void receiptCompositePaymentId() {
        val requestBody = templatesManager.processTemplate(
                SIMPLE_RECEIPTS_WITH_COMPOSITE_ID, Map.of("firm", HORNS_AND_HOOVES));

        val receiptsResponseResponse = whiteSpiritClient
                .receipt(requestBody).then().statusCode(200);

        assertCompositeEventId(
                "receipt_content",
                "trust_id", "06e62000-9cc1-436c-a6db-d64469fa3ab4", "d64469fa3ab4-a6db-436c-9cc1-06e62340",
                receiptsResponseResponse
        );

        val receiptId = receiptsResponseResponse.extract().jsonPath().getInt("id");
        val kktSN = receiptsResponseResponse.extract().jsonPath().getString("kkt.sn");

        val getDocumentResponse = getDocument(kktSN, receiptId).statusCode(200);
        assertCompositeEventId(
                "fullform.receipt_content",
                "trust_id", "06e62000-9cc1-436c-a6db-d64469fa3ab4", "d64469fa3ab4-a6db-436c-9cc1-06e62340",
                getDocumentResponse
        );
    }

    @Test
    @DisplayName("При слишком длинном item_code WhiteSpirit должен обрезать информацию до 32 байт.")
    @RunOnlyIfMGMAreEnabled
    public void receiptWithTooLongItemCodeField() {
        val itemCodePath = "receipt_content.rows[0].item_code";
        val itemCode = "011234567890123421aaaaabbbbbcccccddddd\\u001d8005123456";
        val stripped = "011234567890123421aaaaabbbbbcccccddddd1234";
        val receiptsResponse = makeReceiptWithItemCode(itemCode);
        receiptsResponse.then().statusCode(200);

        val receiptId = receiptsResponse.jsonPath().getInt("id");
        val kktSN = receiptsResponse.jsonPath().getString("kkt.sn");
        getDocument(kktSN, receiptId).body("fullform." + itemCodePath, equalTo("444D+" + stripped));
    }

    private Response makeReceiptWithRetry(String requestBody, String group) {
        return executeWithAttempts(() -> {
            val response = whiteSpiritClient.receipt(requestBody, group);
            response.then().statusCode(200);
            return response;
        }, 3, Duration.ofSeconds(10));
    }

    private Response makeReceiptWithRetry(String requestBody) {
        return executeWithAttempts(() -> {
            val response = whiteSpiritClient.receipt(requestBody);
            response.then().statusCode(200);
            return response;
        }, 5, Duration.ofSeconds(10));
    }


    private Response makeReceiptWithItemCode(String itemCode) {
        val requestBody = templatesManager.processTemplate(
                RECEIPT_WITH_ITEM_CODE_REQUEST_BODY,
                Map.of("firm", HERKULES, "item_code", itemCode));
        val response = makeReceiptWithRetry(requestBody);
        return response;
    }

    @Test
    @DisplayName("Проверка содержимого одного чека")
    public void testSingleReceiptContent() {
        val rows = List.of(new Row(10, 20, TaxType.NDS_20, PaymentTypeType.PREPAYMENT));
        val payments = List.of(new Payment(200, PaymentType.CARD));
        checkEqualContent(rows, payments);
    }

    @SneakyThrows
    private void checkEqualContent(List<Row> rows, List<Payment> payments) {
        val mapper = new ObjectMapper();
        val receipt = Receipt.builder()
                .firm(HORNS_AND_HOOVES)
                .payments(payments)
                .rows(rows)
                .build();

        val requestBody = templatesManager.processTemplate(
                RECEIPTS, Map.of("receipt", receipt)
        );

        val expectedTree = mapper.readTree(requestBody).get("receipt_content");
        val response = makeReceiptWithRetry(requestBody)
                .then()
                .statusCode(200).extract().body().asString();
        val actualTree = mapper.readTree(response).get("receipt_content");
        assertThat(actualTree, equalTo(expectedTree));
    }

    @Test
    @DisplayName("Проверка выбивания множества чеков с общей оплатой")
    public void testMultiplePayments() {
        val rows = List.of(
                new Row(10, 20, TaxType.NDS_20, PaymentTypeType.PREPAYMENT),
                new Row(20, 40, TaxType.NDS_20, PaymentTypeType.PREPAYMENT)

        );
        val payments = List.of(new Payment(1000, PaymentType.CARD));

        checkEqualContent(rows, payments);
    }

    @ParameterizedTest
    @EnumSource(TaxType.class)
    @DisplayName("Проверка различных типов НДС")
    public void testReceiptCalculatedContent(TaxType taxType) {
        val rows = List.of(
                new Row(10, 20, taxType, PaymentTypeType.PREPAYMENT)
        );
        val payments = List.of(
                new Payment(200, PaymentType.CARD)
        );

        checkCalculatedContent(payments, rows);
    }

    @SneakyThrows
    private void checkCalculatedContent(List<Payment> payments, List<Row> rows) {
        val receipt = Receipt.builder()
                .firm(HORNS_AND_HOOVES)
                .payments(payments)
                .rows(rows)
                .build();

        val requestBody = templatesManager.processTemplate(
                RECEIPTS, Map.of("receipt", receipt)
        );

        val responseBody = makeReceiptWithRetry(requestBody)
                .then().statusCode(200).extract().body();
        val responseJson = responseBody.jsonPath();

        val qrDt = DATE_PARSER.parse(responseJson.getString("dt"));
        val calculatedRows = StreamEx.of(rows)
                .map(row -> {
                    if (row.getTaxType() == TaxType.NDS_18) {
                        return row.withTaxType(TaxType.NDS_20);
                    } else if (row.getTaxType() == TaxType.NDS_18_118) {
                        return row.withTaxType(TaxType.NDS_20_120);
                    } else {
                        return row;
                    }
                })
                .toImmutableList();
        val receiptCalculated = ReceiptCalculated.builder()
                .id(responseJson.getInt("id"))
                .rows(calculatedRows)
                .fn(responseJson.getString("fn.sn"))
                .fp(responseJson.getString("fp"))
                .qrDt(DATE_GENERATOR.format(qrDt))
                .build();
        val calculatedBody = templatesManager.processTemplate(RECEIPTS_CALCULATED,
                Map.of("receipt", receiptCalculated));

        val mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module())
                .registerModule(new ParameterNamesModule())
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
                .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true);
        val actualTree = mapper.readTree(responseBody.asString()).get("receipt_calculated_content");
        val calculatedTree = mapper.readTree(calculatedBody).get("receipt_calculated_content");

        val actual = mapper.treeToValue(actualTree, ReceiptDto.class);
        val calculated = mapper.treeToValue(calculatedTree, ReceiptDto.class);
        assertThat(actual, equalTo(calculated));
    }

    @Test
    @DisplayName("Множественные чеки с разными НДС")
    public void testTaxTypes() {
        val rows = new ArrayList<Row>();
        val payments = new ArrayList<Payment>();
        int i = 1;

        int sum = 0;
        for (TaxType taxType : TaxType.values()) {
            rows.add(new Row(10 * i, 20 * i, taxType, PaymentTypeType.PREPAYMENT));
            payments.add(new Payment(10 * 20 * i * i, PaymentType.CARD));
            sum += 10 * 20 * i * i;
            i++;
        }

        checkCalculatedContent(payments, rows);
        checkCalculatedContent(List.of(new Payment(sum, PaymentType.CARD)), rows);
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 300})
    @DisplayName("Проверка на несовпадение")
    public void testAmountMismatch(int amount) {
        val rows = List.of(
                new Row(10, 20, TaxType.NDS_20, PaymentTypeType.PREPAYMENT)
        );
        val payments = List.of(
                new Payment(amount, PaymentType.CARD)
        );

        val receipt = Receipt.builder()
                .firm(HORNS_AND_HOOVES)
                .payments(payments)
                .rows(rows)
                .build();

        val requestBody = templatesManager.processTemplate(
                RECEIPTS, Map.of("receipt", receipt)
        );

        whiteSpiritClient.receipt(requestBody)
                .then().statusCode(400)
                .body("value", equalTo("payments amount != rows amount"));
    }

    static Stream<Arguments> smallValuesProvider() {
        return Stream.of(
                Arguments.arguments(10001, 0.0001, "{'receipt_content': DataError({'payments': DataError({0: " +
                        "DataError({'amount': DataError(precision not 0.01)})}), 'rows': DataError({0: DataError" +
                        "({'qty': DataError(value is less than 0.001)})})})}"),
                Arguments.arguments(0.001, 1001, "{'receipt_content': DataError({'payments': DataError({0: DataError" +
                        "({'amount': DataError(precision not 0.01)})}), 'rows': DataError({0: DataError({'price': " +
                        "DataError(precision not 0.01)})})})}"),
                Arguments.arguments(0.001, 0.0001, "{'receipt_content': DataError({'payments': DataError({0: " +
                        "DataError({'amount': DataError(precision not 0.01)})}), 'rows': DataError({0: DataError" +
                        "({'price': DataError(precision not 0.01), 'qty': DataError(value is less than 0.001)})})})}"));
    }

    private void checkErrorCase(List<Row> rows, List<Payment> payments, String error) {
        val receipt = Receipt.builder()
                .firm(HORNS_AND_HOOVES)
                .payments(payments)
                .rows(rows)
                .build();

        val requestBody = templatesManager.processTemplate(
                RECEIPTS, Map.of("receipt", receipt)
        );

        whiteSpiritClient.receipt(requestBody)
                .then().statusCode(400)
                .body("value", equalTo(error));
    }

    @DisplayName("Слишком маленькие значения должны приводить к ошибке")
    @ParameterizedTest()
    @MethodSource("smallValuesProvider")
    public void testSmallValues(double price, double qty, String error) {
        val rows = List.of(
                new Row(price, qty, TaxType.NDS_20, PaymentTypeType.PREPAYMENT)
        );
        val payments = List.of(
                new Payment(price * qty, PaymentType.CARD)
        );
        checkErrorCase(rows, payments, error);
    }

    @DisplayName("Отрицательные значения должны приводить к ошибке")
    @ParameterizedTest
    @MethodSource("negativeValuesProvider")
    public void testNegativeValues(double price, double qty, String error) {
        val rows = List.of(
                new Row(price, qty, TaxType.NDS_20, PaymentTypeType.PREPAYMENT)
        );
        val payments = List.of(
                new Payment(price * qty, PaymentType.CARD)
        );
        checkErrorCase(rows, payments, error);
    }

    static Stream<Arguments> negativeValuesProvider() {
        return Stream.of(
                Arguments.arguments(-10, 20,
                        "{'receipt_content': DataError({'payments': DataError({0: DataError({'amount': DataError" +
                                "(value is less than 0)})}), 'rows': DataError({0: DataError({'price': DataError" +
                                "(value is less than 0)})})})}"),
                Arguments.arguments(10, -20, "{'receipt_content': DataError({'payments': DataError({0: DataError" +
                        "({'amount': DataError(value is less than 0)})}), 'rows': DataError({0: DataError({'qty': " +
                        "DataError(value is less than 0.001)})})})}"),
                Arguments.arguments(-10, -20, "{'receipt_content': DataError({'rows': DataError({0: DataError" +
                        "({'price': DataError(value is less than 0), 'qty': DataError(value is less than 0.001)})})})}")
        );
    }

    static Stream<Arguments> paymentTypeProvider() {
        return Stream.of(
                Arguments.arguments(PaymentType.CARD, "200.00"),
                Arguments.arguments(PaymentType.PREPAYMENT, "0.00"),
                Arguments.arguments(PaymentType.EXTENSION, "0.00"),
                Arguments.arguments(PaymentType.CREDIT, "0.00")
        );
    }

    @DisplayName("Проверка оплат для различных типов оплаты")
    @ParameterizedTest
    @MethodSource("paymentTypeProvider")
    public void testPaymentTypes(PaymentType paymentType, String amount) {
        val requestBody = prepareReceiptRequestBody(HORNS_AND_HOOVES, paymentType);

        makeReceiptWithRetry(requestBody)
                .then().statusCode(200).body("receipt_calculated_content.money_received_total", equalTo(amount));
    }


    private String getDocumentResponse() {
        val itemCode = "010460043993125621JgXJ5";
        val itemCodePath = "receipt_content.rows[0].item_code";
        val receiptsResponse = makeReceiptWithItemCode(itemCode);
        receiptsResponse.then()
                .statusCode(200)
                .body(itemCodePath, equalTo(itemCode));

        val receiptId = receiptsResponse.jsonPath().getInt("id");
        val kktSN = receiptsResponse.jsonPath().getString("kkt.sn");
        val documentResponse = getDocument(kktSN, receiptId);
        documentResponse.body("fullform." + itemCodePath, equalTo("444D+" + itemCode));

        return documentResponse.extract().body().asString();
    }

    private static boolean compareAmounts(Object o1, Object o2) {
        return Math.abs(Double.valueOf(o1.toString()) - Double.valueOf(o2.toString())) < 0.0001;
    }


    @DisplayName("Чек без позиций приводит к ошибке")
    @Test
    public void testNoItems() {
        List<Row> rows = Collections.emptyList();
        List<Payment> payments = Collections.emptyList();
        val error = "{'receipt_content': DataError({'rows': DataError(list length is less than 1)})}";
        checkErrorCase(rows, payments, error);
    }

    private static String prepareReceiptRequestBody(Firm firm, PaymentType paymentType) {
        return prepareReceiptRequestBody(firm, paymentType, Optional.empty());
    }

    private static String prepareReceiptRequestBody(Firm firm, PaymentType paymentType,
                                                    Optional<FiscalDocumentType> fiscalDocumentType) {
        val rows = List.of(new Row(10, 20, TaxType.NDS_20, PaymentTypeType.PREPAYMENT));
        val payments = List.of(new Payment(200, paymentType));

        val receipt = Receipt.builder()
                .firm(firm)
                .payments(payments)
                .rows(rows)
                .fiscalDocumentType(fiscalDocumentType)
                .build();

        return templatesManager.processTemplate(RECEIPTS, Map.of("receipt", receipt));
    }

    private Set<String> getFirmGroupKkts(Firm firm, String group) {
        return getFirmGroupKkts(firm, group, false);
    }

    private Set<String> getFirmGroupKkts(Firm firm, String group, boolean isBsoKkt) {
        return StreamEx.of(whiteSpiritManager.getKKTs().values())
                .filter(kkt -> kkt.isBsoKkt() == isBsoKkt
                        && kkt.getGroup().equals(group)
                        && kkt.getInn().equals(firm.getInn()))
                .map(KKT::getKktSN)
                .toImmutableSet();
    }

    private static Stream<Arguments> testDefaultGroup() {
        return Stream.of(
                Arguments.arguments(HORNS_AND_HOOVES, "_NOGROUP"),
                Arguments.arguments(HERKULES, "NEW")
        );
    }

    @DisplayName("Чек без указания группы пробивается на кассе дефолтной группы.")
    @ParameterizedTest
    @MethodSource
    public void testDefaultGroup(Firm firm, String group) {
        val requestBody = prepareReceiptRequestBody(firm, PaymentType.CARD, Optional.of(FiscalDocumentType.RECEIPT));

        awaitInnGroups(new InnGroupPair(firm.getInn(), group));
        val defaultGroupKkts = getFirmGroupKkts(firm, group);
        executeWithAttempts(() -> hudsuckerClient.receipt(requestBody)
                .then().statusCode(200), 3, Duration.ofSeconds(10))
                .body("kkt.sn", isIn(defaultGroupKkts));
    }

    @DisplayName("Чек БСО пробивается на кассе БСО с группой BSO.")
    @Test
    public void testBsoGroup() {
        val firm = HERKULES;
        val requestBody = prepareReceiptRequestBody(firm, PaymentType.CARD, Optional.of(FiscalDocumentType.BSO));

        awaitInnGroups(new InnGroupPair(firm.getInn(), "BSO"));
        val bsoKktSNs = getFirmGroupKkts(firm, "BSO", true);
        hudsuckerClient.receipt(requestBody)
                .then().statusCode(200)
                .body("document_type", equalTo("BSO"))
                .body("kkt.sn", isIn(bsoKktSNs));
    }

    @DisplayName("Ручка receipts должна уметь принимать опциональное поле cashregister_params")
    @Test
    public void testCashregisterParams() {
        val requestBody = templatesManager.processTemplate(
                RECEIPTS_WITH_CASHREGISTER_PARAMS,
                Map.of("firm", HORNS_AND_HOOVES)
        );
        whiteSpiritClient.receipt(requestBody)
                .then().statusCode(200);
    }
}
