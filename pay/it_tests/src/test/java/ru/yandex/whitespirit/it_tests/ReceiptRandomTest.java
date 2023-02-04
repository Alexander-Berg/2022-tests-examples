package ru.yandex.whitespirit.it_tests;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.ArrayValueMatcher;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

import ru.yandex.whitespirit.it_tests.configuration.KKT;
import ru.yandex.whitespirit.it_tests.templates.Firm;
import ru.yandex.whitespirit.it_tests.templates.TemplatesManager;
import ru.yandex.whitespirit.it_tests.whitespirit.WhiteSpiritManager;
import ru.yandex.whitespirit.it_tests.whitespirit.client.HudsuckerClient;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import static org.hamcrest.Matchers.is;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static ru.yandex.whitespirit.it_tests.ReceiptTest.awaitInnGroups;
import static ru.yandex.whitespirit.it_tests.templates.Template.RECEIPT_WITH_ITEM_CODE_REQUEST_BODY;
import static ru.yandex.whitespirit.it_tests.utils.Constants.HERKULES;
import static ru.yandex.whitespirit.it_tests.utils.Constants.HORNS_AND_HOOVES;
import static ru.yandex.whitespirit.it_tests.utils.Utils.checkResponseCode;
import static ru.yandex.whitespirit.it_tests.utils.Utils.executeWithAttempts;

@Slf4j
@RunOnlyIfMGMAreEnabled
public class ReceiptRandomTest {
    private static final String MARK_GROUP = "MARK";
    private static final String DEFAULT_GROUP = "_NOGROUP";
    private static final Firm FFD_1_05_FIRM = HERKULES;
    private static final Firm FFD_1_2_FIRM = HORNS_AND_HOOVES;
    private static final WhiteSpiritManager whiteSpiritManager = Context.getWhiteSpiritManager();
    private static final WhiteSpiritClient whiteSpiritClient = whiteSpiritManager.getWhiteSpiritClient();
    private static final TemplatesManager templatesManager = Context.getTemplatesManager();
    private static KKT newKKT;
    private static KKT oldKKT;
    private static KKT defaultKKT;
    private static final HudsuckerClient hudsuckerClient = whiteSpiritManager.getHudsuckerClient();

    @BeforeAll
    public static void getCorrectionKKT() {
        newKKT = StreamEx.of(whiteSpiritManager.getKKTs().values())
                .findAny(kkt -> kkt.getGroup().equals(MARK_GROUP) && kkt.getInn().equals(FFD_1_2_FIRM.getInn())).orElseThrow();
        oldKKT = StreamEx.of(whiteSpiritManager.getKKTs().values())
                .findAny(kkt -> kkt.getGroup().equals(MARK_GROUP) && kkt.getInn().equals(FFD_1_05_FIRM.getInn())).orElseThrow();
        defaultKKT = StreamEx.of(whiteSpiritManager.getKKTs().values())
                .findAny(kkt -> kkt.getGroup().equals(DEFAULT_GROUP)).orElseThrow();
        Stream.of(oldKKT, newKKT, defaultKKT).forEach(kkt -> checkResponseCode(whiteSpiritClient.openShift(kkt.getKktSN())));

        val groups = StreamEx.of(oldKKT, newKKT, defaultKKT)
                .map(kkt -> new InnGroupPair(kkt.getInn(), kkt.getGroup()))
                .toArray(InnGroupPair[]::new);

        awaitInnGroups(groups);
    }

    @AfterAll
    public static void closeShift() {
        Stream.of(oldKKT, newKKT, defaultKKT).forEach(kkt -> checkResponseCode(whiteSpiritClient.closeShift(kkt.getKktSN())));
    }

    @SneakyThrows
    private static String readContent(File f) {
        return Files.readString(f.toPath());
    }

    @SneakyThrows
    private static Stream<Arguments> getRandomReceipts() {
        val loader = Thread.currentThread().getContextClassLoader();
        val url = loader.getResource("receipts_with_item_code");
        String path = url.getPath();
        return Stream.of(new File(path).listFiles())
                .map(file -> Arguments.of(file.getName(), readContent(file)));
    }

    private ValidatableResponse getDocumentResponse(String kktSN, int receiptId) {
        val documentResponse = hudsuckerClient.getDocument(kktSN, receiptId, false, false);
        return documentResponse.then().statusCode(200);
    }

    private Response makeReceiptWithResponse(String requestBody) {
        val response = whiteSpiritClient.receipt(requestBody, MARK_GROUP);
        response.then().statusCode(200);
        return response;
    }

    private ValidatableResponse makeReceiptAndGetDocumentWithResponse(String requestBody, int attempts) {
        val receiptsResponse = executeWithAttempts(() -> makeReceiptWithResponse(requestBody), attempts,
                Duration.ofSeconds(10));

        val receiptId = receiptsResponse.jsonPath().getInt("id");
        val kktSN = receiptsResponse.jsonPath().getString("kkt.sn");
        return executeWithAttempts(() -> getDocumentResponse(kktSN, receiptId), attempts);
    }

    private String makeReceiptAndGetDocumentAsString(String requestBody, int attempts) {
        return makeReceiptAndGetDocumentWithResponse(requestBody, attempts).extract().body().asString();
    }

    private static boolean compareAmounts(Object o1, Object o2) {
        return Math.abs(Double.valueOf(o1.toString()) - Double.valueOf(o2.toString())) < 0.0001;
    }

    private static ObjectMapper getMapper() {
        val mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module())
                .registerModule(new ParameterNamesModule())
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
                .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true);
        return mapper;
    }

    private static boolean compareAdditionalDocumentRequisites(Object o1, Object o2) {
        // Если реквизит сгенерирован самим ws, то он не будет совпадать для двух чеков, даже если их содержимое
        // идентично
        val s1 = o1.toString();
        val s2 = o2.toString();
        val prefix = "ws:";
        return s1.equals(s2) || (s1.startsWith(prefix) && s2.startsWith(prefix));
    }

    private static boolean compareItemCodes(Object actual, Object expected) {
        val actualStr = actual.toString();
        val expectedStr = expected.toString();

        return itemCodeStrip(actualStr)
                .startsWith(itemCodeStrip(expectedStr));
    }

    private static String itemCodeStrip(String itemCode) {
        return itemCode.replace("\u001d8005", "")
                .replaceFirst("^444D\\+", "");
    }

    private static Customization getRowCustomization(String prefix) {
        val matcher = new ArrayValueMatcher<>(
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization(String.format("%s[*].qty", prefix), ReceiptRandomTest::compareAmounts),
                        new Customization(String.format("%s[*].item_code", prefix), ReceiptRandomTest::compareItemCodes)
                )
        );

        return new Customization(prefix, matcher);
    }

    private JSONComparator getWhitespiritComparator() {
        val sensitiveData = new String[]{"check_url", "fn.sn", "fp", "kkt.rn",
                "kkt.sn", "receipt_extra_content.fp", "fp",
                "document_index", "id", "id", "shift_number",
                "ofd_ticket_received", "ofd_ticket_received",
                "dt", "dt",
                "receipt_extra_content.ffd_version",
                "kkt.version",
                "ofd.inn",
                "ofd.name",
                "receipt_calculated_content.qr",
                "original_receipt_content.firm_inn",
                "firm.inn", "firm.name",
                "location.address", "location.address",
                "receipt_content.firm_inn"
        };
        val customizations = Stream.of(sensitiveData).map(path -> new Customization(path, (o1, o2) -> true));

        // Виртуальный ФН записывает итоговую оплату, как 100.00, реальный ФН записывает ее как 100.
        val amountCustomization = new Customization("amount", ReceiptRandomTest::compareAmounts);
        val docRequisiteCustomization = new Customization("receipt_content.additional_document_requisite",
                ReceiptRandomTest::compareAdditionalDocumentRequisites);


        val comparator = new CustomComparator(JSONCompareMode.LENIENT, Stream.concat(customizations,
                Stream.of(amountCustomization, docRequisiteCustomization,
                        getRowCustomization("receipt_content.rows"),
                        getRowCustomization("receipt_calculated_content.rows"))).toArray(Customization[]::new));

        return comparator;
    }

    private static String fillFirm(String name, String request, Firm firm) {
        return templatesManager.processTemplate(name, request, Map.of("firm", firm));
    }

    @SneakyThrows
    @ParameterizedTest(name = "Проверка чека {0} на то, как отличается результат выдачи выбития чека для обычной " +
            "кассы и кассы в версии 1.2")
    @MethodSource("getRandomReceipts")
    public void compareWhitespiritResponse(String name, String request) {
        val oldResponse =
                makeReceiptWithResponse(fillFirm(name, request, FFD_1_05_FIRM)).then().extract().body().asString();
        val newResponse =
                makeReceiptWithResponse(fillFirm(name, request, FFD_1_2_FIRM)).then().extract().body().asString();

        assertEquals(oldResponse, newResponse, getWhitespiritComparator());
    }

    private JSONComparator getComparator() {
        val sensitiveData = new String[]{"fullform.check_url", "fullform.fn.sn", "fullform.fp", "fullform.kkt.rn",
                "fullform.kkt.sn", "fullform.receipt_extra_content.fp", "fp",
                "fullform.document_index", "fullform.id", "id", "fullform.shift_number",
                "ofd_ticket_received", "fullform.ofd_ticket_received",
                "dt", "fullform.dt",
                "fullform.receipt_extra_content.ffd_version",
                "fullform.firm.inn", "fullform.firm.name",
                "fullform.location.address", "fullform.location.address",
                "fullform.receipt_content.firm_inn"
        };
        val customizations = Stream.of(sensitiveData).map(path -> new Customization(path, (o1, o2) -> true));

        val amountCustomization = new Customization("amount", ReceiptRandomTest::compareAmounts);
        val docRequisiteCustomization = new Customization("fullform.receipt_content.additional_document_requisite",
                ReceiptRandomTest::compareAdditionalDocumentRequisites);

        val comparator = new CustomComparator(JSONCompareMode.LENIENT, Stream.concat(customizations,
                Stream.of(amountCustomization, docRequisiteCustomization,
                        getRowCustomization("fullform.receipt_content.rows"),
                        getRowCustomization("fullform.receipt_calculated_content.rows"))).toArray(Customization[]::new));

        return comparator;
    }

    @SneakyThrows
    @ParameterizedTest(name = "Проверка чека {0} на то, как его рендерит ws для обычной кассы и кассы в версии 1.2")
    @MethodSource("getRandomReceipts")
    public void compareReceipts(String name, String request) {
        val oldResponse = makeReceiptAndGetDocumentAsString(fillFirm(name, request, FFD_1_05_FIRM), 3);
        val newResponse = makeReceiptAndGetDocumentAsString(fillFirm(name, request, FFD_1_2_FIRM), 3);

        assertEquals(oldResponse, newResponse, getComparator());
    }

    private String makeReceiptWithItemCode(String itemCode, Firm firm) {
        val directSubst = "SUBST_THIS_CODE";
        val result = templatesManager.processTemplate(
                        RECEIPT_WITH_ITEM_CODE_REQUEST_BODY,
                        Map.of("firm", firm, "item_code", directSubst))
                .replace("\"supplier_phone\": \"+766699944\",", "")
                .replace(directSubst, itemCode);

        return result;
    }

    @SneakyThrows
    private static Stream<Arguments> getCodes(String filename) {
        val loader = Thread.currentThread().getContextClassLoader();
        val url = loader.getResource(filename);
        val path = url.getPath();
        return Files.readAllLines(new File(path).toPath())
                .stream()
                .map(Arguments::of);
    }


    private static Stream<Arguments> getShoeCodes() {
        return getCodes("marking_shoes.txt");
    }

    private static Stream<Arguments> getMilkCodes() {
        return getCodes("marking_milk.txt");
    }

    private static Stream<Arguments> getTobaccoCodes() {
        return getCodes("marking_tobacco.txt");
    }

    @SneakyThrows
    private void compareMarkings(String code) {
        val oldResponse = makeReceiptAndGetDocumentAsString(makeReceiptWithItemCode(code, FFD_1_05_FIRM), 3);
        val newResponse = makeReceiptAndGetDocumentAsString(makeReceiptWithItemCode(code, FFD_1_2_FIRM), 3);

        assertEquals(oldResponse, newResponse, getComparator());
    }

    @ParameterizedTest(name = "Проверка тестовой маркировки обуви {0}")
    @MethodSource("getShoeCodes")
    public void compareShoeMarkings(String code) {
        compareMarkings(code);
    }

    @ParameterizedTest(name = "Проверка тестовой маркировки молока {0}")
    @MethodSource("getMilkCodes")
    public void compareMilkMarkings(String code) {
        compareMarkings(code);
    }

    @ParameterizedTest(name = "Проверка тестовой маркировки табачных изделий {0}")
    @MethodSource("getTobaccoCodes")
    public void compareTobaccoMarkings(String code) {
        compareMarkings(code);
    }

    private static Stream<Arguments> getGS1Codes() {
        return Stream.of(
                Arguments.of("010304109478744321tE%HqMa_lOQ4D\\u001d93dGVz", "010304109478744321tE%HqMa_lOQ4D",
                        "[M+]"),
                Arguments.of("010460043993125621JgXJ5.T", "010460043993125621JgXJ5.T", "[M-]"));
    }

    @ParameterizedTest(name = "Проверка на отображение маркировки {0}")
    @MethodSource("getGS1Codes")
    public void checkGS1(String code, String stripped, String checkResult) {
        val productCheckResultPath = "fullform.receipt_content.rows[0].product_check_result";
        val itemCodePath = "fullform.receipt_content.rows[0].item_code";
        makeReceiptAndGetDocumentWithResponse(makeReceiptWithItemCode(code, FFD_1_2_FIRM), 3).assertThat()
                .body(itemCodePath, is("444D+" + stripped))
                .body(productCheckResultPath, is(checkResult));
        makeReceiptAndGetDocumentWithResponse(makeReceiptWithItemCode(code, FFD_1_05_FIRM), 3).assertThat()
                .body(itemCodePath, is("444D+" + stripped));
    }

    @Test
    @DisplayName("Проверка того, как ws роутит чеки, содержащие маркировку")
    public void testRouting() {
        val code = "010304109478744321tE%HqMa_lOQ4D\\u001d93dGVz";
        val request = makeReceiptWithItemCode(code, FFD_1_2_FIRM);
        val path = "kkt.sn";
        whiteSpiritClient.receipt(request.replaceAll("\"item_code.*\\n", "")).then().statusCode(200)
                .body(path, is(defaultKKT.getKktSN()));
        whiteSpiritClient.receipt(request).then().statusCode(200).body(path, is(newKKT.getKktSN()));
    }
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Test
@EnabledIfEnvironmentVariable(named = "DISABLE_MGM_TESTS", matches = "False")
@interface RunOnlyIfMGMAreEnabled {
}
