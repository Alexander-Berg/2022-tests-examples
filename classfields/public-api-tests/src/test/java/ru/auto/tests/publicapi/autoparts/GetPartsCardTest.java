package ru.auto.tests.publicapi.autoparts;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /autoparts/offer/{offerId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetPartsCardTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetNoPartsCardInProdAndTest() {
        assertThat(api.autoparts().search()
                .reqSpec(defaultSpec()).pageSizeQuery(1)
                .executeAs(validatedWith(shouldBe200OkJSON())).getOffers()).isNull();

        assertThat(prodApi.autoparts().search()
                .reqSpec(defaultSpec()).pageSizeQuery(1)
                .executeAs(validatedWith(shouldBe200OkJSON())).getOffers()).isNull();
    }
}
