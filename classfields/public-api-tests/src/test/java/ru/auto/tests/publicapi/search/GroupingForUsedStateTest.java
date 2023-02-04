package ru.auto.tests.publicapi.search;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiOfferListingResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /search/cars")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GroupingForUsedStateTest {

    private static final int RID = 213;
    private static final String GROUP_BY = "CONFIGURATION";
    private static final String STATE = "USED";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(TIMONDL)
    public void shouldSeeOnlyUsedCarsInGroupingInfo() {
        AutoApiOfferListingResponse response = api.search().searchCars().reqSpec(defaultSpec())
                .groupByQuery(GROUP_BY)
                .offerGroupingQuery(true)
                .ridQuery(RID)
                .stateQuery(STATE)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        response.getOffers().forEach(autoApiOffer -> {
            assertThat(autoApiOffer.getGrouppingInfo().getOfferCounter().getUsedCars())
                    .isNotNull()
                    .isGreaterThan(0);
            assertThat(autoApiOffer.getGrouppingInfo().getOfferCounter().getNewCars())
                    .isNull();
        });
    }
}
