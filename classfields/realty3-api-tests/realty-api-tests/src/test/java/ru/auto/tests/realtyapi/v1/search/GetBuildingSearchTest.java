package ru.auto.tests.realtyapi.v1.search;

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
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("GET /search/buildingSearch")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetBuildingSearchTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.search().buildingSearchRoute()
                .addressQuery(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithEmptyAddress() {
        api.search().buildingSearchRoute()
                .reqSpec(authSpec())
                .addressQuery(EMPTY)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
}
