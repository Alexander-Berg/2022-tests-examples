package ru.auto.tests.realtyapi.v2.usersOffers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomOfferId;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomUID;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("GET /user/{uid}/offers/{offer_id}/card")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetUserOfferCardInvalidParametersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.usersOffers().getCardRoute()
                .uidPath(getRandomUID())
                .offerIdPath(getRandomOfferId())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee404WithInvalidParameters() {
        api.usersOffers().getCardRoute().reqSpec(authSpec())
                .uidPath(getRandomUID())
                .offerIdPath(getRandomOfferId())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @Owner(ARTEAMO)
    public void should404WithNoPathParameters() {
        api.usersOffers().getCardRoute().reqSpec(authSpec())
                .uidPath(EMPTY)
                .offerIdPath(EMPTY)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

}
