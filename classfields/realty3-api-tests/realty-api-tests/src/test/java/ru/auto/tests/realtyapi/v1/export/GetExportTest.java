package ru.auto.tests.realtyapi.v1.export;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.ExportParams;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;

import static java.util.Collections.singletonList;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("POST /export/offers.xls")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetExportTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.export().exportOffersRoute()
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithNoBody() {
        api.export().exportOffersRoute().reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee200WithEmptyIdList() {
        api.export().exportOffersRoute().reqSpec(authSpec())
                .body(new ExportParams()
                        .offerIds(new ArrayList<>())
                        .archive(true)
                        .contacts(true)
                        .link(true))
                .execute(validatedWith(shouldBe200Ok()));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee200WithInvalidOfferId() {
        api.export().exportOffersRoute().reqSpec(authSpec())
                .body(getBody(getRandomString()))
                .execute(validatedWith(shouldBe200Ok()));
    }

    public static ExportParams getBody(String offerId) {
        return new ExportParams()
                .offerIds(singletonList(offerId))
                .archive(true)
                .contacts(true)
                .link(true);
    }
}
