package ru.auto.tests.realtyapi.v2.usersOffers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiVosOffersResponse;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("GET /user/{uid}/offers/byIds")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetUserOfferSnippetsByIdsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoId() {
        RealtyApiVosOffersResponse response = api.usersOffers().getSnippetsByIdsRoute()
                .uidPath(getRandomString())
                .executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)));

        Assertions.assertThat(response.getError())
                .hasMessage("The supplied authorisation is invalid");
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee404WithInvalidUid() {
        api.usersOffers().getSnippetsByIdsRoute().reqSpec(authSpec())
                .uidPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee404WithNoUid() {
        api.usersOffers().getSnippetsByIdsRoute().reqSpec(authSpec())
                .uidPath(EMPTY)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }
}