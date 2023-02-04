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
import ru.auto.tests.publicapi.model.AutoApiPagination;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.NTSH;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.withDefaultSearchQuery;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400NonPositiveValueError;
import static ru.auto.tests.publicapi.testdata.TestData.invalidPaginations;

/**
 * Created by ntsh on 18.04.18.
 */

@DisplayName("GET /search/cars")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@Issue("VERTISTEST-630")
public class SearchCarsInvalidPaginationTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter("Невалидная пагинация")
    @Parameterized.Parameter(0)
    public int invalidPagination;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static List<Object[]> getParameters() {
        return Arrays.asList(invalidPaginations());
    }

    @Test
    @Owner(NTSH)
    public void shouldSee400AfterBadPageNumber() {
        api.search().searchCars().reqSpec(defaultSpec()
                .andThen(withDefaultSearchQuery()))
                .pageQuery(invalidPagination)
                .execute(validatedWith(
                        shouldBe400NonPositiveValueError(AutoApiPagination.SERIALIZED_NAME_PAGE)));
    }

    @Test
    @Owner(NTSH)
    public void shouldSee400AfterBadPageSize() {
        api.search().searchCars().reqSpec(defaultSpec()
                .andThen(withDefaultSearchQuery()))
                .pageQuery(1)
                .pageSizeQuery(invalidPagination)
                .execute(validatedWith(
                        shouldBe400NonPositiveValueError(AutoApiPagination.SERIALIZED_NAME_PAGE_SIZE)));
    }
}
