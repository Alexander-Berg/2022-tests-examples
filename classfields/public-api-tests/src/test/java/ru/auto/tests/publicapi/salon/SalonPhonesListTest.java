package ru.auto.tests.publicapi.salon;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.mapper.ObjectMapperType;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.SALON_NOT_FOUND;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Created by dskuznetsov on 20.08.18
 */


@DisplayName("GET /salon/{code}/phones")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class SalonPhonesListTest {
    private final String SALON_CODE = "major_expert_moskva_mkad_92km";
    private final String INVALID_SALON_CODE = "not_existing_salon";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee403WhenNoAuth() {
        api.salon().getDealerPhones().codePath(SALON_CODE).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee404WhenNotValidSalonCode() {
        AutoApiErrorResponse response = api.salon().getDealerPhones()
                .reqSpec(defaultSpec())
                .codePath(INVALID_SALON_CODE)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
                .as(AutoApiErrorResponse.class, ObjectMapperType.GSON);

        assertThat(response).hasStatus(ERROR).hasError(SALON_NOT_FOUND)
                .hasDetailedError("Salon not found");
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.salon().getDealerPhones()
                .reqSpec(defaultSpec())
                .codePath(SALON_CODE)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
