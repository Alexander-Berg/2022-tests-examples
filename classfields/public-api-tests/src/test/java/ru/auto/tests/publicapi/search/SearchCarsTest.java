package ru.auto.tests.publicapi.search;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.NTSH;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400SearchQueryNotValidError;

/**
 * Created by ntsh on 18.04.18.
 */

@DisplayName("GET /search/cars")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
@Issue("VERTISTEST-630")
public class SearchCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(NTSH)
    public void shouldSee400AfterSearchCarsInvalidValueError() {

        String invalidValue = Utils.getRandomString();
        api.search().searchCars().reqSpec(defaultSpec())
                .priceFromQuery(invalidValue)
                .execute(validatedWith(
                        shouldBe400SearchQueryNotValidError(AutoApiSearchSearchRequestParameters.SERIALIZED_NAME_PRICE_FROM, invalidValue)));
    }

}
