package ru.auto.tests.realtyapi.v1.money;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.net.URI;
import java.net.URISyntaxException;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeAcceptableCodeForMissingPathElement;
import static ru.auto.tests.realtyapi.v1.money.PostCreatePersonTest.getBootstrapRequestForUser;
import static ru.auto.tests.realtyapi.v1.money.PostCreatePersonTest.getPostBody;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("POST /money/invoice/pdf/{uid}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class PostInvoiceTest {

    private static final int VALID_MONEY = 1000;
    private static final int INVALID_MONEY = -1000;
    private static final String INVOICE_SIZE = "100000";
    private static final String PDF_CONTENT_TYPE_HEADER = "application/pdf";
    private static final String CONTENT_LENGTH = "content-length";
    private static final String CONTENT_DISPOSITION = "content-disposition";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private OAuth oAuth;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Test
    public void shouldSee403WithNoAuth() {
        api.money().createPdfInvoice()
                .uidPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee4xxWithNoUid() {
        api.money().createPdfInvoice()
                .reqSpec(authSpec())
                .uidPath(EMPTY)
                .totalMoneyQuery(VALID_MONEY)
                .execute(validatedWith(shouldBeAcceptableCodeForMissingPathElement()));
    }

    @Test
    public void shouldSee500WithInvalidMoney() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);
        adaptor.juridicalUser(account, token);

        api.money().bootstrapBilling().reqSpec(authSpec())
                .uidPath(account.getId())
                .body(getBootstrapRequestForUser(account))
                .execute(validatedWith(shouldBe200Ok()));

        api.money().createPdfInvoice().reqSpec(authSpec())
                .uidPath(account.getId())
                .authorizationHeader(token)
                .totalMoneyQuery(INVALID_MONEY)
                .execute(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR)));
    }

    @Test
    public void shouldSee404ForNotBillingUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        api.money().createPdfInvoice().reqSpec(authSpec())
                .uidPath(account.getId())
                .authorizationHeader(token)
                .totalMoneyQuery(VALID_MONEY)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldCreateInvoice() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);
        adaptor.juridicalUser(account, token);
        String uid = account.getId();

        api.money().bootstrapBilling().reqSpec(authSpec())
                .uidPath(uid)
                .body(getBootstrapRequestForUser(account))
                .execute(validatedWith(shouldBe200Ok()));

        api.money().createPerson().reqSpec(authSpec())
                .uidPath(uid)
                .body(getPostBody())
                .execute(validatedWith(shouldBe200Ok()));

        JsonObject response = api.money().createPdfInvoice().reqSpec(authSpec())
                .uidPath(uid)
                .authorizationHeader(token)
                .totalMoneyQuery(VALID_MONEY)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response.get("invoiceId")).isNotNull();
        Assertions.assertThat(response.get("downloadUrl")).isNotNull();
        Assertions.assertThat(response.get("downloadUrl")).isNotEqualTo(EMPTY);
    }

    @Test
    public void shouldGetValidInvoice() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);
        adaptor.juridicalUser(account, token);

        String uid = account.getId();

        api.money().bootstrapBilling().reqSpec(authSpec())
                .uidPath(uid)
                .body(getBootstrapRequestForUser(account))
                .execute(validatedWith(shouldBe200Ok()));

        api.money().createPerson().reqSpec(authSpec())
                .uidPath(uid)
                .body(getPostBody())
                .execute(validatedWith(shouldBe200Ok()));

        JsonObject response = api.money().createPdfInvoice().reqSpec(authSpec())
                .uidPath(uid)
                .authorizationHeader(token)
                .totalMoneyQuery(VALID_MONEY)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        int invoiceId = response.get("invoiceId").getAsInt();
        String downloadUrl = response.get("downloadUrl").getAsString();
        String fileName = getFileName(invoiceId);

        api.money().downloadPdfInvoice().reqSpec(authSpec())
                .uidPath(uid)
                .authorizationHeader(token)
                .invoiceIdPath(invoiceId)
                .execute(validatedWith(shouldBe200Ok()
                        .expectContentType(PDF_CONTENT_TYPE_HEADER)
                        .expectHeader(CONTENT_DISPOSITION,
                                String.format("attachment; filename=\"%s\"", fileName))
                        .expectHeader(CONTENT_LENGTH, greaterThanOrEqualTo(INVOICE_SIZE))));
    }

    private static String getFileName(int invoiceId) {
        return "invoices_" + invoiceId + ".pdf";
    }
}
