package ru.auto.tests.cabinet.adaptor;

import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.FluentJdbcBuilder;
import org.codejargon.fluentjdbc.api.mapper.Mappers;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.SqlConfig;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.config.CabinetApiConfig;
import ru.auto.tests.cabinet.model.BalanceOrder;
import ru.auto.tests.cabinet.model.BalanceOrderClient;
import ru.auto.tests.cabinet.model.BalancePersonResult;
import ru.auto.tests.cabinet.model.BatchResult;
import ru.auto.tests.cabinet.model.ClientSubscription;
import ru.auto.tests.cabinet.model.ClientSubscriptionSeq;
import ru.auto.tests.cabinet.model.DealerSiteCheckEntity;
import ru.auto.tests.cabinet.model.DealerStock;
import ru.auto.tests.cabinet.model.DealerStocks;
import ru.auto.tests.cabinet.model.Group;
import ru.auto.tests.cabinet.model.InvoiceParams;
import ru.auto.tests.cabinet.model.InvoicePdfLink;
import ru.auto.tests.cabinet.model.SubscriptionProto;
import ru.auto.tests.cabinet.model.UpdateResult;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.adaptor.PassportApiAdaptor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.util.Lists.newArrayList;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.model.DealerStock.StockCategoryEnum.*;
import static ru.auto.tests.cabinet.model.DealerStock.StockCategoryEnum.MOTO;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;

@Singleton
public class CabinetApiAdaptor extends AbstractModule {

    @Inject
    @Prod
    private ApiClient api;

    @Inject
    private SqlConfig sqlConfig;

    @Inject
    private PassportApiAdaptor passportApi;

    @Inject
    private CabinetApiConfig config;

    @Override
    protected void configure() {
    }

    @Step("Создаем дилерский аккаунт {account.login}")
    public String createDealerAccount(Account account) {
        String sessionId = passportApi.login(account.getLogin(), account.getPassword()).getSession().getId();

        return RestAssured
                .given()
                .baseUri("http://cabinet-api.test.avto.ru/desktop/v1.0.0/client/post/")
                .header("Host", "cabinet-api.test.avto.ru")
                .queryParam("session_id", sessionId)
                .queryParam("remote_ip", "2a02%3A6b8%3A0%3A421%3A75%3A1029%3A789d%3Ad9d1")
                .queryParam("access_key", "d6ts4q53U244Fy-6n0-Kc3h5A3Z9bnwn")
                .queryParam("user", account.getId())
                .formParam("contact_name", "dsffdaS")
                .formParam("email", account.getLogin())
                .formParam("geo_id", "213")
                .formParam("ya_city_id", "213")
                .formParam("ya_region_id", "1")
                .formParam("ya_country_id", "225")
                .formParam("name", "dsadaf")
                .formParam("phone", "80004443322")
                .when()
                .post().getBody().asString();
    }

    @Step("Очищаем шаги регистрации")
    public void clearRegistrationSteps(String dealerId) {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setDriverClassName(sqlConfig.driver());
        dataSource.setUrl(format(sqlConfig.sqlDataBaseUrl(), "3413"));
        dataSource.setUsername(sqlConfig.login());
        dataSource.setPassword(sqlConfig.password());

        FluentJdbc build = new FluentJdbcBuilder().connectionProvider(dataSource).build();

        build.query().update(format("DELETE FROM cabinet_autoru.registration_step_status WHERE client_id='%s'; ", dealerId))
                .run();
    }

    @Step("Проходим шаг {step}")
    public void setRegistrationStepsConfirmed(String dealerId, String userId, String stepPath) {
        api.registration().postForClient().xAutoruOperatorUidHeader(userId)
                .clientIdPath(dealerId).reqSpec(defaultSpec()).stepPath(stepPath)
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Очищаем подписки дилера")
    public void clearSubscriptions(String dealerId, String userId) {
        if (api.subscription().getByClient().clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeCode(SC_OK))).size() == 0) {
            return;
        } else {
            api.subscription().deleteBatch().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                    .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
        }
    }

    @Step("Добавляем подписку дилеру")
    public ClientSubscription addSubscription(String dealerId, String userId, String category, String email) {
        return api.subscription().postById().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new SubscriptionProto().category(category)
                        .emailAddress(email)).executeAs(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Добавляем подписки «{categories}» для dealer:user «{dealerId}:{userId}»")
    public void addArraySubscriptions(String dealerId, String userId, List<String> categories, String email) {
        List<SubscriptionProto> subscriptions = new ArrayList<>();
        categories.forEach(category -> subscriptions.add(new SubscriptionProto().category(category).emailAddress(email)));
        api.subscription().postBatch().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new ClientSubscriptionSeq()
                .subscriptions(subscriptions)).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Добавляем site-check дилеру")
    public void addSiteCheckForDealer(String dealerId, String userId, String comment, String ticket, Boolean resolution) {
        OffsetDateTime time = OffsetDateTime.now();
        api.siteCheck().addOrUpdateSiteCheck().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new DealerSiteCheckEntity().checkDate(time)
                .resolution(resolution).comment(comment).ticket(ticket))
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Получаем все подписки дилера")
    public List<ClientSubscription> getAllSubscriptionDealer(String dealerId, String userId) {
        return api.subscription().getByClient().clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Очищаем site-check у дилера")
    public void clearSiteCheck(String dealerId) {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setDriverClassName(sqlConfig.driver());
        dataSource.setUrl(format(sqlConfig.sqlDataBaseUrl(), "3413"));
        dataSource.setUsername(sqlConfig.login());
        dataSource.setPassword(sqlConfig.password());

        FluentJdbc build = new FluentJdbcBuilder().connectionProvider(dataSource).build();

        build.query().update(format("DELETE FROM cabinet_autoru.dealer_site_check WHERE client_id='%s'; ", dealerId))
                .run();
    }

    @Step("Получаем балансовых плательщиков дилера")
    public BalancePersonResult getBalancePersonsId(String dealerId, String userId) {
        return api.invoice().getBalancePersons().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Выставляем счет дилеру")
    public InvoicePdfLink getInvoiceDealer(String dealerId, String userId, Long balancePersonId) {
        return api.invoice().postInvoice().clientIdPath(dealerId).typeQuery("regular").xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).body(new InvoiceParams().quantity(12345l).balancePersonId(balancePersonId))
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Получаем балансовую информацио о клиенте")
    public BalanceOrderClient getBalanceClientInfo(String dealerId, String userId) {
        return api.invoice().getBalanceClient().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Получаем заказ дилера")
    public BalanceOrder getOrderClient(String dealerId) {
        return api.invoice().createOrder().clientIdPath(dealerId).reqSpec(defaultSpec())
                .requireActualBalanceDataQuery("false").executeAs(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Меняем статус дилеру")
    public void setDealerSatatus(String dealerId, String status) {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setDriverClassName(sqlConfig.driver());
        dataSource.setUrl(format(sqlConfig.sqlDataBaseUrl(), "3405"));
        dataSource.setUsername(sqlConfig.login());
        dataSource.setPassword(sqlConfig.password());

        FluentJdbc build = new FluentJdbcBuilder().connectionProvider(dataSource).build();

        build.query().update(format("UPDATE office7.clients SET status = '%s' WHERE id='%s'; ", status, dealerId))
                .run();
    }

    @Step("Получаем информацию о дилере")
    public JsonObject getClientInfo(String dealerId, String userId) {
        return api.client().getClient().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class);
    }

    @Step("Получаем все стоки дилера")
    public DealerStocks getDealerStocks(String dealerId, String userId) {
        return api.client().getClientStocks().clientIdPath(dealerId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Добавляем все стоки дилеру")
    public BatchResult addAllStocksToDealer(String dealerId, String userId, Integer amount, Boolean resolution) {
        return api.client().saveClientStocks().reqSpec(defaultSpec()).xAutoruOperatorUidHeader(userId).clientIdPath(dealerId)
                .body(new DealerStocks().stocks(newArrayList(
                        new DealerStock().stockAmount(amount).stockCategory(CARS_NEW).fullStock(resolution),
                        new DealerStock().stockAmount(amount).stockCategory(CARS_USED).fullStock(resolution),
                        new DealerStock().stockAmount(amount).stockCategory(COMMERCIAL).fullStock(resolution),
                        new DealerStock().stockAmount(amount).stockCategory(MOTO).fullStock(resolution))))
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Добавляем все стоки компании")
    public BatchResult addAllStocksToCompany(String companyId, String userId, Integer amount, Boolean resolution) {
        return api.client().saveCompanyStocks().reqSpec(defaultSpec()).xAutoruOperatorUidHeader(userId).companyIdPath(companyId)
                .body(new DealerStocks().stocks(newArrayList(
                        new DealerStock().stockAmount(amount).stockCategory(CARS_NEW).fullStock(resolution),
                        new DealerStock().stockAmount(amount).stockCategory(CARS_USED).fullStock(resolution),
                        new DealerStock().stockAmount(amount).stockCategory(COMMERCIAL).fullStock(resolution),
                        new DealerStock().stockAmount(amount).stockCategory(MOTO).fullStock(resolution))))
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Удаляем acl группу дирелу")
    public UpdateResult deleteAclIdFromDealer(String dealerId, String userId, Long groupAclId) {
        return api.acl().deleteClientAccessGroup().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec()).groupIdPath(Long.valueOf(groupAclId))
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Создааем acl группу дирелу")
    public Group addAclIdToDealer(String dealerId, String userId) {
        String body = getResourceAsString("acl_read_write.json");

        return api.acl().putClientAccessGroup().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .reqSpec(req -> req.setBody(body))
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Очищаем отчеты лояльности дилеру")
    public void deleteLoyaltyReports(String dealerId) {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setDriverClassName(sqlConfig.driver());
        dataSource.setUrl(format(sqlConfig.sqlDataBaseUrl(), "3413"));
        dataSource.setUsername(sqlConfig.login());
        dataSource.setPassword(sqlConfig.password());

        FluentJdbc build = new FluentJdbcBuilder().connectionProvider(dataSource).build();
        List<String> reportId = build.query()
                .select(format("SELECT id FROM cabinet_autoru.loyalty_report WHERE client_id='%s'", dealerId)).listResult(Mappers.singleString());
        if (reportId.isEmpty()) {
            return;
        } else {
            build.query().update(format("DELETE FROM cabinet_autoru.loyalty_report_item WHERE report_id in (%s);", StringUtils.join(reportId,',')))
                    .run();
            build.query().update(format("DELETE FROM cabinet_autoru.loyalty_report WHERE client_id='%s';", dealerId))
                    .run();
        }

    }

    @Step("Добавляем текущиий отчет лояльности дилеру")
    public void addLoyaltyReports(String dealerId) {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setDriverClassName(sqlConfig.driver());
        dataSource.setUrl(format(sqlConfig.sqlDataBaseUrl(), "3413"));
        dataSource.setUsername(sqlConfig.login());
        dataSource.setPassword(sqlConfig.password());

        DateFormat format = new SimpleDateFormat("yyyy-MM-01");
        String date = format.format(new Date());

        FluentJdbc build = new FluentJdbcBuilder().connectionProvider(dataSource).build();
        build.query().update(format("INSERT INTO cabinet_autoru.loyalty_report " +
                "(`client_id`, `report_date`, `resolution`, `status`, `cashback_applied`, `epoch`, `cashback_amount`, `cashback_percent`, `period_id`) " +
                "VALUES ('%s', '%s', '0', 'approved', '0', CURRENT_TIMESTAMP(6), '0', '0', '10');", dealerId, date)).run();
        String reportId = build.query()
                .select(format("SELECT id FROM cabinet_autoru.loyalty_report WHERE client_id='%s'", dealerId)).singleResult(Mappers.singleString());
        build.query().update(format("INSERT INTO cabinet_autoru.loyalty_report_item " +
                "(`report_id`, `criterion`, `value`, `resolution`, `epoch`, `required`) " +
                "VALUES ('%s', 'banned-card', '0', '1', CURRENT_TIMESTAMP(6), '1');", reportId)).run();
        build.query().update(format("INSERT INTO cabinet_autoru.loyalty_report_item " +
                "(`report_id`, `criterion`, `value`, `resolution`, `epoch`, `required`) " +
                "VALUES ('%s', 'inactivity', '0', '1', CURRENT_TIMESTAMP(6), '1');", reportId)).run();
        build.query().update(format("INSERT INTO cabinet_autoru.loyalty_report_item " +
                "(`report_id`, `criterion`, `value`, `resolution`, `epoch`, `required`) " +
                "VALUES ('%s', 'rejected-moderation', '0', '1', CURRENT_TIMESTAMP(6), '1');", reportId)).run();
        build.query().update(format("INSERT INTO cabinet_autoru.loyalty_report_item " +
                "(`report_id`, `criterion`, `value`, `resolution`, `epoch`, `required`) " +
                "VALUES ('%s', 'banned-offers', '0', '1', CURRENT_TIMESTAMP(6), '1');", reportId)).run();
        build.query().update(format("INSERT INTO cabinet_autoru.loyalty_report_item " +
                "(`report_id`, `criterion`, `value`, `resolution`, `epoch`, `required`) " +
                "VALUES ('%s', 'site-check', '1', '1', CURRENT_TIMESTAMP(6), '1');", reportId)).run();
        build.query().update(format("INSERT INTO cabinet_autoru.loyalty_report_item " +
                "(`report_id`, `criterion`, `value`, `resolution`, `epoch`, `required`) " +
                "VALUES ('%s', 'lifetime', '12', '1', CURRENT_TIMESTAMP(6), '1');", reportId)).run();
    }
}