package ru.auto.tests.realtyapi.v1.user.offer;

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
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.adaptor.FtlProcessor;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeValid;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultOffers;


@Title("POST /user/offers/validation")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ValidationOfferValidTest {

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
    private FtlProcessor ftlProcessor;

    @Parameter("Оффер")
    @Parameterized.Parameter(0)
    public String body;


    @SuppressWarnings({"unchecked"})
    @Parameterized.Parameters(name = "{0}")
    public static List<String> getParameters() {
        return defaultOffers();
    }

    @Test
    public void shouldSee200AndValidTrue() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.userOffers().validateOfferRoute().reqSpec(authSpec()).authorizationHeader(token)
                .reqSpec(req -> req.setBody(ftlProcessor.processOffer(body))).execute(validatedWith(shouldBeValid()));
    }
}
