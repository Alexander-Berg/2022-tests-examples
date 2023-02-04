package ru.auto.tests.realtyapi.v2.village;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyVillageResponseDeveloperSuggestResponse;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.*;
import static ru.auto.tests.commons.util.Utils.getRandomShortLong;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;


@Title("GET /village/developerSuggest")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetVillageDeveloperSuggestTest {
    private static final long RGID = 587795;
    private static final int DEFAULT_MAX_COUNT = 10;
    private static final int ZERO_MAX_COUNT = 0;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;


    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.village().villageDeveloperSuggestRoute()
                .textQuery(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    @Issue("REALTYBACK-1271")
    public void shouldSee500WithoutText() {
        api.village().villageDeveloperSuggestRoute()
                .reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR)));
    }

    @Test
    @Owner(ARTEAMO)
    @Issue("REALTYBACK-1271")
    public void shouldSee500WithZeroMaxCount() {
        api.village().villageDeveloperSuggestRoute()
                .reqSpec(authSpec())
                .textQuery(getRandomString())
                .rgidQuery(getRandomShortLong())
                .maxCountQuery(ZERO_MAX_COUNT)
                .execute(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR)));
    }

    @Test
    @Owner(ARTEAMO)
    @Issue("REALTYBACK-1271")
    public void shouldSee500WithStringRgid() {
        api.village().villageDeveloperSuggestRoute()
                .reqSpec(authSpec())
                .textQuery(getRandomString())
                .rgidQuery(getRandomString())
                .maxCountQuery(DEFAULT_MAX_COUNT)
                .execute(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldDeveloperSuggestHasSize100() {
        int size = 100;

        RealtyVillageResponseDeveloperSuggestResponse response = api.village().villageDeveloperSuggestRoute()
                .reqSpec(authSpec())
                .textQuery(EMPTY)
                .rgidQuery(RGID)
                .maxCountQuery(size)
                .executeAs(validatedWith(shouldBe200Ok()));

        Assertions.assertThat(response.getResponse().getSuggest()).hasSize(size);
    }
}
