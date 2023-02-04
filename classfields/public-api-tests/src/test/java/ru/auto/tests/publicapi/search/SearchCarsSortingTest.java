package ru.auto.tests.publicapi.search;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.AutoApiOfferListingResponse;
import ru.auto.tests.publicapi.model.AutoApiOfferListingResponseAssert;
import ru.auto.tests.publicapi.model.AutoApiSearchSearchSorting;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.List;

import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.NTSH;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.withDefaultSearchQuery;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.testdata.TestData.defaultCarsSortsWithDirection;

/**
 * Created by ntsh on 18.04.18.
 */

@DisplayName("GET /search/cars")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@Issue("VERTISTEST-630")
public class SearchCarsSortingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameterized.Parameter(0)
    public String sortType;

    @Parameterized.Parameter(1)
    public String divider;

    @Parameterized.Parameter(2)
    public String sortDirection;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "sortType = {0} - divider = {1} - sortDirection = {2}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(defaultCarsSortsWithDirection());
    }

    @Test
    @Owner(NTSH)
    public void shouldGetSorting() {
        AutoApiOfferListingResponse response = api.search().searchCars().reqSpec(defaultSpec()
                .andThen(withDefaultSearchQuery()))
                .sortQuery(sortType + divider + sortDirection)
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiSearchSearchSorting sorting = new AutoApiSearchSearchSorting()
                .name(sortType)
                .desc(sortDirection.equals(AutoApiSearchSearchSorting.SERIALIZED_NAME_DESC));

        AutoApiOfferListingResponseAssert.assertThat(response)
                .hasSorting(sorting);
    }
}
