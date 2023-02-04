package ru.yandex.realty.rules;

import com.google.inject.Inject;
import com.ibm.icu.text.Transliterator;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.BaseRequest;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.json.JSONObject;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.mountebank.fluent.ImposterBuilder;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.yandex.realty.anno.WithMock;
import ru.yandex.realty.config.RealtyWebConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockRule extends TestWatcher {

    public static final String DEFAULT_OFFERID = "7777777777777777777777";

    private static final String COOKIE_DOMAIN = ".yandex.ru";
    private static final String UID_REPLACEMENT = "#UID#";
    private static final String OFFERID_REPLACEMENT = "#OFFERID#";

    private static final Transliterator TO_LATIN = Transliterator.getInstance("Russian-Latin/BGN");

    private static List<String> offerIdList = newArrayList();
    private static List<String> accountIdList = newArrayList();

    private String port;
    private String testMethodName;
    private String testClassPathName;
    private boolean withMockFlag;

    @Inject
    private RealtyWebConfig config;

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    private WebDriverSteps webDriverSteps;

    @Inject
    private ImposterBuilder imposterBuilder;

    public static void addOfferId(String id) {
        offerIdList.add(id);
    }

    public void addAccountId(String id) {
        accountIdList.add(id);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        testClassPathName = description.getClassName().replaceAll("\\.", "/");
        testClassPathName = TO_LATIN.transliterate(testClassPathName);
        testMethodName = description.getMethodName();
        testMethodName = TO_LATIN.transliterate(testMethodName);
        testMethodName = testMethodName.replaceAll("\\W", "");
        withMockFlag = Optional.ofNullable(description.getAnnotation(WithMock.class)).isPresent() &&
                config.mockCreate();
        return super.apply(base, description);
    }

    @Override
    protected void starting(Description description) {
        if (!accountKeeper.get().isEmpty()) {
            addAccountId(accountKeeper.get().get(0).getId());
        }
        if (withMockFlag) {
            create();
        }
    }

    @Override
    protected void succeeded(Description description) {
        if (withMockFlag && config.mockRecord()) {
            recordMock();
        }
    }

    @Override
    protected void finished(Description description) {
        if (config.mockAttachImposter()) {
            attachImposter("Imposter");
        }
        if (withMockFlag) {
            deleteMock();
        }
    }

    @Attachment(value = "{attachName}", type = "application/json", fileExtension = ".json")
    private String attachImposter(String attachName) {
        String getImposterRequest = format("%s/imposters/%s", config.getMockritsaURL(), port);
        return retryIf400(Unirest.get(getImposterRequest)).getBody().toString();
    }

    /**
     * https://github.com/YandexClassifieds/vertis-mockritsa
     */
    @Step("Создаём мок")
    public void create() {
        //TODO recordMock.json заменить на ru.auto.tests.commons.mountebank.fluent.ImposterBuilder
        String imposterResource = config.mockRecord() ?
                getResourceAsString("mock/recordMock.json") :
                getResourceAsString(format("mockresource/%s/%s.json", testClassPathName, testMethodName));
        if (accountIdList.size() > 0) {
            imposterResource = imposterResource.replaceAll(UID_REPLACEMENT, accountIdList.get(0));
        }
        imposterResource = imposterResource.replaceAll(OFFERID_REPLACEMENT, DEFAULT_OFFERID);
        HttpResponse<JsonNode> response = retryIf400(Unirest.post(config.getMockritsaURL() + "/imposters")
                .body(imposterResource));

        port = response.getBody().getObject().get("port").toString();
        webDriverSteps.setCookie("mockritsa_imposter", port, COOKIE_DOMAIN);
    }

    @Step("Сохраняем импостер")
    private void recordMock() {
        String getImposterWithoutProxiesRequest =
                format("%s/imposters/%s/?replayabe=true&removeProxies=true", config.getMockritsaURL(), port);
        JSONObject recordedImposter = retryIf400(Unirest.get(getImposterWithoutProxiesRequest)).getBody().getObject();
        recordedImposter.remove("port");
        recordedImposter.remove("numberOfRequests");
        recordedImposter.remove("recordRequests");
        recordedImposter.remove("requests");
        String imposterTemplateToNextTime = recordedImposter.toString(2);

        if (accountIdList.size() > 0) {
            for (String uid : accountIdList) {
                imposterTemplateToNextTime = imposterTemplateToNextTime.replaceAll(uid, UID_REPLACEMENT);
            }
        }

        if (offerIdList.size() > 0) {
            for (String offerId : offerIdList) {
                imposterTemplateToNextTime = imposterTemplateToNextTime.replaceAll(offerId, OFFERID_REPLACEMENT);
            }
        }
        String imposterPathToRecord = "target/test-classes/mockresource/" + testClassPathName;
        String imposterPathFile = format("%s/%s.json", imposterPathToRecord, testMethodName);
        try {
            Files.createDirectories(Paths.get(imposterPathToRecord));
            if (Files.notExists(Paths.get(imposterPathFile))) {
                Files.createFile(Paths.get(imposterPathFile));
            }
            Files.write(Paths.get(imposterPathFile), imposterTemplateToNextTime.getBytes(),
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Удаляем мок")
    private void deleteMock() {
        String deleteImposterRequest = format("%s/imposters/%s", config.getMockritsaURL(), port);
        retryIf400(Unirest.delete(deleteImposterRequest));
    }

    private HttpResponse<JsonNode> retryIf400(BaseRequest request) {
        return given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions()
                .pollInterval(3, SECONDS).alias("Ждем ответа 200ОК")
                .atMost(20, SECONDS).until(() -> {
                    HttpResponse<JsonNode> response = request.asJson();
                    assertThat(response.getStatus())
                            .describedAs("Код ответа должен быть")
                            .isLessThanOrEqualTo(201)
                            .overridingErrorMessage("Код ответа был", response.getStatus());
                    return response;
                }, notNullValue());
    }
}