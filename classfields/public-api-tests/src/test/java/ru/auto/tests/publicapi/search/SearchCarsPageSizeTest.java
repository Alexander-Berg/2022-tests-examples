package ru.auto.tests.publicapi.search;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.AutoApiOfferListingResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.NTSH;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.withDefaultSearchQuery;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by ntsh on 18.04.18.
 */

@DisplayName("GET /search/cars")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@Issue("VERTISTEST-630")
public class SearchCarsPageSizeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter("Число объявлений на странице")
    @Parameterized.Parameter(0)
    public int pageSize;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static List<Object[]> getParameters() {
        return Arrays.asList(providePageSize());
    }

    private static Object[] providePageSize() {
        return new Integer[]{
                10, 20, 30};
    }

    @Test
    @Owner(NTSH)
    public void shouldGetOffersQuantitySameWithPageSize() {

        AutoApiOfferListingResponse response = api.search().searchCars().reqSpec(defaultSpec()
                .andThen(withDefaultSearchQuery()))
                .pageSizeQuery(pageSize)
                .executeAs(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat(response.getOffers().size(), equalTo(pageSize));
    }
}
