package ru.auto.tests.realtyapi.v2.village;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.*;
import static ru.auto.tests.commons.util.Utils.getRandomShortLong;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;

@Title("GET /village/pointSearch")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetVillagePointSearchTest {
    private static final int INVALID_PAGE = -1;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.village().villagePointSearchRoute()
                .villageIdQuery(getRandomShortLong())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee200WithNoParameters() {
        api.village().villagePointSearchRoute()
                .reqSpec(authSpec())
                .execute(validatedWith(shouldBe200Ok()));
    }

    @Test
    @Owner(ARTEAMO)
    @Issue("REALTYBACK-1271")
    public void shouldSee500WithInvalidPageSize() {
        api.village().villagePointSearchRoute()
                .reqSpec(authSpec())
                .pageQuery(INVALID_PAGE)
                .execute(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR)));
    }

    @Test
    @Owner(ARTEAMO)
    @Issue("REALTYBACK-1271")
    public void shouldSee500WithInvalidPageNumber() {
        api.village().villagePointSearchRoute()
                .reqSpec(authSpec())
                .pageSizeQuery(INVALID_PAGE)
                .execute(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR)));
    }
}
