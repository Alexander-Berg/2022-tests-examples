package ru.auto.tests.realtyapi.v1.common;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.enums.ComplainReason;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.OldComplain;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collection;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeOK;


@Title("POST /complain.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ComplainReasonsTest {

    //partnerId - это из capa (для vos это фиксированное число 1035218734)
    private static final String PARTNER_ID = "1035218734";

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

    @Parameter("Причина")
    @Parameterized.Parameter
    public ComplainReason reason;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static Collection<ComplainReason> getParameters() {
        return Arrays.asList(ComplainReason.values());
    }

    @Test
    public void shouldSeeOkWhenComplain() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.getOfferIdFromSearcher();

        api.common().complainRoute().reqSpec(authSpec())
                .body(new OldComplain().entityId(offerId)
                        .partnerId(PARTNER_ID)
                        .reason(reason.value())
                        .uid(account.getId())
                        .text(Utils.getRandomString()))
                .authorizationHeader(token).execute(validatedWith(shouldBeOK()));
    }
}
