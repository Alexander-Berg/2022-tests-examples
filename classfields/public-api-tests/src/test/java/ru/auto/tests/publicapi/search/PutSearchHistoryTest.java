package ru.auto.tests.publicapi.search;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiSearchMotoSearchRequestParameters;
import ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiSearchInstance.CategoryEnum.*;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("PUT /search/{category}/history")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class PutSearchHistoryTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    public void shouldSee403WhenNonAuth() {
        api.search().addHistorySearch().categoryPath(CARS).executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee200OKWhenAddSearchForEachCategory() {
        api.search().addHistorySearch().categoryPath(CARS).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
        api.search().addHistorySearch().categoryPath(MOTO).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
        api.search().addHistorySearch().categoryPath(TRUCKS).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldSee400WhenAddParamsForWrongCategory() {
        AutoApiErrorResponse errorResponse = api.search().addHistorySearch().categoryPath(CARS)
                .body(new AutoApiSearchSearchRequestParameters()
                        .motoParams(new AutoApiSearchMotoSearchRequestParameters())
                )
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(400)))
                .as(AutoApiErrorResponse.class);

        assertThat(errorResponse.getError()).isEqualByComparingTo(BAD_REQUEST);
    }
}
