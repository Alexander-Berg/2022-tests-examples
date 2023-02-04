package ru.yandex.realty.step;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import io.qameta.allure.okhttp3.AllureOkHttp3;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.yandex.realty.api.PersonalApiService;
import ru.yandex.realty.api.Realty3ApiService;
import ru.yandex.realty.api.SelenoidService;
import ru.yandex.realty.api.SuggestService;
import ru.yandex.realty.api.SuggestTextService;
import ru.yandex.realty.api.UnsafeOkHttpClient;
import ru.yandex.realty.api.YqlApiService;
import ru.yandex.realty.beans.SuggestItem;
import ru.yandex.realty.beans.SuggestText;
import ru.yandex.realty.beans.favorites.FavoritesOffers;
import ru.yandex.realty.beans.juridical.JuridicalUserBody;
import ru.yandex.realty.config.RealtyWebConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.util.Lists.newArrayList;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.yandex.realty.lambdas.WatchException.watchException;

/**
 * @author kurau (Yuri Kalinin)
 */
public class RetrofitApiSteps {

    private static final String SUGGEST_URL = "http://realty-bnb-searcher-api.vrts-slb.test.vertis.yandex.net";
    private static final String SUGGEST = "https://realty.test.vertis.yandex.ru";
    private static final String PERSONAL_API_HOST = "http://personal-api-int.vrts-slb.test.vertis.yandex.net";
    private static final String REALTY3_HOST = "http://realty-gateway-api.vrts-slb.test.vertis.yandex.net";
    private static final String DEFAULT_FILTER_REPORT = "offers-sell-apartment-%s.xls";
    public static final String YQL_YANDEX_TEAM_HOST = "https://yql.yandex-team.ru";

    private Retrofit.Builder retrofit;

    private File report;

    @Inject
    private RealtyWebConfig config;

    public RetrofitApiSteps() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);


        OkHttpClient.Builder httpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient().newBuilder();
        httpClient.addInterceptor(logging);
        httpClient.addInterceptor(new AllureOkHttp3());

        retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build());
    }

    @Step("Ищем ЖК по имени «{name}»")
    public List<SuggestItem> nameList(String name) throws IOException {
        return retrofit.baseUrl(SUGGEST_URL).build()
                .create(SuggestService.class)
                .nameList(name, "741964").execute().body().getResult();
    }

    @Step("Ищем застройщика по имени «{name}»")
    public List<SuggestItem> developerList(String name) throws IOException {
        return retrofit.baseUrl(SUGGEST_URL).build()
                .create(SuggestService.class)
                .developerList(name, "741964").execute().body().getResult();
    }

    @Step("Получаем саджест через API по «{name}»")
    public List<SuggestText.Item> suggest(String name, String rgid, String type, String category) throws IOException {
        return retrofit.baseUrl(SUGGEST).build()
                .create(SuggestTextService.class)
                .suggestText(name, rgid, type, category, "YES").execute().body().getResponse();
    }

    @Step("Получаем цену на «{product}» оффера «{offerId}»")
    public String getProductPrice(String product, String uid, String offerId) throws IOException {
        return await().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .ignoreExceptions().pollInterval(2, TimeUnit.SECONDS)
                .atMost(20, TimeUnit.SECONDS)
                .until(() -> {
                    List<JsonElement> jsonElements = newArrayList();
                    retrofit.baseUrl(REALTY3_HOST).build()
                            .create(Realty3ApiService.class)
                            .price(uid, offerId).execute().body()
                            .getAsJsonObject("response").getAsJsonArray("offers").get(0).getAsJsonObject()
                            .getAsJsonObject("productInfo").getAsJsonArray("products")
                            .forEach(jsonElement -> jsonElements.add(jsonElement));
                    String priceWithCents = jsonElements.stream()
                            .filter(e -> e.getAsJsonObject().getAsJsonPrimitive("productType")
                                    .getAsString().equalsIgnoreCase(product)).findFirst().get()
                            .getAsJsonObject().getAsJsonObject("priceContext").getAsJsonObject("availablePrice")
                            .getAsJsonPrimitive("base").getAsString();
                    return priceWithCents.substring(0, priceWithCents.length() - 2);
                }, notNullValue());
    }

    @Step("Проверяем промо подписка юзера «{uid} подтверждена»")
    public void checkPromoSubscriptionAgreed(String uid, boolean status) {
        await().pollInterval(2, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat("Проверяем «isAgreed» = true", retrofit.baseUrl(REALTY3_HOST).build()
                        .create(Realty3ApiService.class)
                        .subscriptions(uid)
                        .execute().body().getAsJsonObject("response").get("isAgreed").getAsBoolean(), equalTo(status)));
    }

    @Step("Проверяем промо подписка юзера «{uid}» отключена")
    public void checkPromoSubscriptionNotFound(String uid) {
        await().pollInterval(8, TimeUnit.SECONDS)
                .atMost(16, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat("Проверяем код ответа = 404", retrofit.baseUrl(REALTY3_HOST).build()
                        .create(Realty3ApiService.class)
                        .subscriptions(uid)
                        .execute().code(), equalTo(404)));
    }

    @Step("Скачиваем report для сессии «{session}»")
    public RetrofitApiSteps downloadReport(Path path, String session, String fileName) {
        ResponseBody body = await()
                .pollInterval(5, TimeUnit.SECONDS)
                .atMost(20, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(format("http://%s:4444", config.getSelenoidUrl().getHost()))
                        .build().create(SelenoidService.class)
                        .downloadFileFromContainer(session, fileName)
                        .execute().body(), notNullValue());

        try (InputStream is = body.byteStream()) {
            Files.copy(is, path);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        report = new File(path.toString());
        attachFile(fileName, path);
        return this;
    }

    @Step("Создаем аккаунт для юр. лица")
    public void juridicalUser(String uid, JuridicalUserBody body) {
        watchException(() -> await().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions().pollInSameThread()
                .pollInterval(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY3_HOST).build()
                                .create(Realty3ApiService.class)
                                .createUser(uid, body).execute().body().getAsJsonPrimitive("userRef").getAsString(),
                        equalTo(format("uid_%s", uid))));
    }

    @Attachment(value = "{description}", type = "application/vnd.ms-excel")
    private byte[] attachFile(String description, Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            return new byte[0];
        }
    }

    @Step("Получаем список офферов из файла")
    public List<String> offersFromReport() {
        List<String> offers = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(report)) {
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet firstSheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = firstSheet.iterator();
            while (iterator.hasNext()) {
                Row nextRow = iterator.next();
                if (nextRow.getPhysicalNumberOfCells() > 1) {
                    offers.add(nextRow.getCell(1).getStringCellValue());
                }
            }
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return offers.stream()
                .filter(offer -> offer.contains("https://realty.yandex.ru/offer"))
                .collect(toList());
    }

    public String getDownloadedFileName() {
        return report.getName();
    }

    public static String reportDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        return String.format(DEFAULT_FILTER_REPORT, sdf.format(new Date()));
    }

    @Step("Получаем «Избранное» юзера «{uid}»")
    public List<FavoritesOffers> getFavoritesOffers(String uid) throws IOException {
        return retrofit.baseUrl(PERSONAL_API_HOST).build()
                .create(PersonalApiService.class)
                .favorites(uid)
                .execute().body().getEntities().getOffers();
    }

    @Step("Проверяем itemId = «{itemId}» у юзера «{uid}» в избранном")
    public void checkItemInFavorites(String uid, String itemId) {
        await().ignoreException(NullPointerException.class)
                .pollInterval(2, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat("В избранном", (int) retrofit.baseUrl(PERSONAL_API_HOST).build()
                                .create(PersonalApiService.class)
                                .favorites(uid)
                                .execute().body().getEntities().getOffers().stream().filter(
                                        offer -> offer.getEntity_id().equals(itemId)).count(), equalTo(1)));
    }

    @Step("Проверяем itemId = «{itemId}» у юзера «{uid}» не в избранном")
    public void checkItemNotInFavorites(String uid, String itemId) {
        await().pollInterval(2, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    boolean asserted = false;
                    List<FavoritesOffers> favoritesOffers = retrofit.baseUrl(PERSONAL_API_HOST).build()
                            .create(PersonalApiService.class)
                            .favorites(uid)
                            .execute().body().getEntities().getOffers();
                    if (favoritesOffers == null)
                        asserted = true;
                    else if (favoritesOffers.stream().noneMatch(offer -> offer.getEntity_id().equals(itemId)))
                        asserted = true;
                    assertThat("Не в избранном", asserted, equalTo(true));
                });
    }

    private String getOAuthToken() {
        return format("OAuth %s", config.getYqlToken());
    }

    @Step("Ждем 10 минут. Запускаем операцию -> получаем id")
    public String runOperation(String yqlOperation) {
        waitSomething(10, TimeUnit.MINUTES);
        String authorizationHeader = getOAuthToken();
        JsonObject body = new GsonBuilder().create().fromJson(yqlOperation, JsonObject.class);
        return await().pollInterval(30, TimeUnit.SECONDS)
                .alias("Ждем запуска джобы в yql")
                .atMost(5, TimeUnit.MINUTES)
                .ignoreExceptions()
                .until(() -> {
                            String id = retrofit.baseUrl(YQL_YANDEX_TEAM_HOST).build()
                                    .create(YqlApiService.class)
                                    .yqlOperationsRun(authorizationHeader, body).execute().body()
                                    .getAsJsonPrimitive("id").getAsString();
                            waitRunning(id);
                            return id;
                        }
                        , notNullValue());
    }

    @Step("Ждем пока операция будет завершена -> получаем данные")
    public String getOperationData(String id) {
        String authorizationHeader = getOAuthToken();
        return await().pollInterval(30, TimeUnit.SECONDS)
                .alias("Ждем получения данных")
                .atMost(10, TimeUnit.MINUTES)
                .ignoreExceptions()
                .until(() -> {
                            JsonObject body = retrofit.baseUrl(YQL_YANDEX_TEAM_HOST).build()
                                    .create(YqlApiService.class)
                                    .yqlOperationsData(authorizationHeader, id).execute().body();
                            assertThat(body.getAsJsonPrimitive("status").getAsString(), equalTo("COMPLETED"));
                            return body.getAsJsonArray("data").get(0).getAsJsonObject().toString();
                        }
                        , notNullValue());
    }

    @Step("Ждем запуска операции")
    public void waitRunning(String id) {
        String authorizationHeader = getOAuthToken();
        await().pollInterval(5, TimeUnit.SECONDS)
                .alias("Ждем запуска операции")
                .atMost(26, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until(() -> {
                    JsonObject body = retrofit.baseUrl(YQL_YANDEX_TEAM_HOST).build()
                            .create(YqlApiService.class)
                            .yqlOperationsData(authorizationHeader, id).execute().body();
                    String status = body.getAsJsonPrimitive("status").getAsString();
                    assertThat(status, not(equalTo("ERROR")));
                    assertThat(status, not(equalTo("PENDING")));
                    return true;
                });
    }

}
