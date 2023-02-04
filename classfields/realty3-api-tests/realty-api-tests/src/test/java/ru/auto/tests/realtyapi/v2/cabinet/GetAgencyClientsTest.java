package ru.auto.tests.realtyapi.v2.cabinet;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiCabinetAgencyClientsResponse;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v2.testdata.CabinetTestData.INVALID_PAGE;
import static ru.auto.tests.realtyapi.v2.testdata.CabinetTestData.ME;
import static ru.auto.tests.realtyapi.v2.testdata.CabinetTestData.VALID_UID;
import static ru.auto.tests.realtyapi.v2.testdata.CabinetTestData.getBillingDomain;

@Title("GET /cabinet/{billing-domain}/user/{user}/agency/clients")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetAgencyClientsTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameterized.Parameter(0)
    public String domain;

    @Parameterized.Parameters(name = "billing-domain={0}")
    public static Object[] getParameters() {
        return getBillingDomain();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.cabinet().agencyClients()
                .billingDomainPath(domain)
                .userPath(ME)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithINoUid() {
        api.cabinet().agencyClients()
                .reqSpec(authSpec())
                .billingDomainPath(domain)
                .pageSizeQuery(INVALID_PAGE)
                .userPath(ME)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithInvalidPageSize() {
        api.cabinet().agencyClients()
                .reqSpec(authSpec())
                .billingDomainPath(domain)
                .xUidHeader(VALID_UID)
                .pageSizeQuery(INVALID_PAGE)
                .userPath(ME)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithInvalidPageNumber() {
        api.cabinet().agencyClients()
                .reqSpec(authSpec())
                .billingDomainPath(domain)
                .xUidHeader(VALID_UID)
                .pageQuery(INVALID_PAGE)
                .userPath(ME)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400ForNotAgency() {
        RealtyApiCabinetAgencyClientsResponse response = api.cabinet().agencyClients()
                .reqSpec(authSpec())
                .billingDomainPath(domain)
                .xUidHeader(VALID_UID)
                .userPath(ME)
                .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)));

        Assertions.assertThat(response.getError()).hasMessage("Not an agency");
    }
}
