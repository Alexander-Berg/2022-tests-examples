package ru.auto.tests.realtyapi.v2.village;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.http.HttpStatus.*;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;

@Title("GET /village/{id}/offerStat")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetVillageIdOfferStatTest {
    private static final long VALID_VILLAGE_ID = 1773615;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;


    @Test
    public void shouldSee403WithNoAuth() {
        api.village().villageOfferStatRoute()
                .idPath(VALID_VILLAGE_ID)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404WithInvalidId() {
        api.village().villageOfferStatRoute()
                .reqSpec(authSpec())
                .idPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldSee404WithNoId() {
        api.village().villageOfferStatRoute()
                .reqSpec(authSpec())
                .idPath(EMPTY)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }
}
