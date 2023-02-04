package ru.auto.tests.realtyapi.v1.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.responses.IdResponse;
import ru.auto.tests.realtyapi.adaptor.FtlProcessor;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.assertj.Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeOK;


@Title("PUT /user/offers/draft/<id>")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UpdateDraftOffersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Inject
    private FtlProcessor ftlProcessor;

    @Parameter("Парметр publish")
    @Parameterized.Parameter(0)
    public Boolean publish;

    @Parameter("Оффер")
    @Parameterized.Parameter(1)
    public String path;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "offer = {1} publish = {0}")
    public static Collection<Object[]> getParameters() {
        List<Object[]> results = newArrayList();
        TestData.defaultOffers().forEach(p -> TestData.trueFalse().forEach(f -> results.add(new Object[]{f, p})));
        return results;
    }

    @Test
    public void shouldUpdateDraft() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.createEmptyDraft(token).getResponse().getId();

        IdResponse response = api.draft().updateDraftRoute().offerIdPath(offerId).reqSpec(authSpec())
                .authorizationHeader(token)
                .reqSpec(req -> req.setBody(ftlProcessor.processOffer(path)))
                .publishQuery(publish)
                .execute(validatedWith(shouldBeOK())).as(IdResponse.class, GSON);
        assertThat(response.getResponse()).hasId(offerId);

    }
}
