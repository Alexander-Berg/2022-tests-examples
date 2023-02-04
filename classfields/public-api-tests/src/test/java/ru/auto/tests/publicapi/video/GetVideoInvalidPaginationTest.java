package ru.auto.tests.publicapi.video;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
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
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400NonPositiveValueError;
import static ru.auto.tests.publicapi.testdata.TestData.invalidPaginations;

@DisplayName("GET /video/search")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetVideoInvalidPaginationTest {

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
    public void shouldSee400AfterInvalidPageNumber() {
        api.video().searchVideo().pageQuery(invalidPagination).categoryPath(CARS.name()).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe400NonPositiveValueError(AutoApiPagination.SERIALIZED_NAME_PAGE)));
    }


    @Test
    public void shouldSee400AfterInvalidPageSize() {
        api.video().searchVideo().pageQuery(1).pageSizeQuery(invalidPagination).categoryPath(CARS.name()).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe400NonPositiveValueError(AutoApiPagination.SERIALIZED_NAME_PAGE_SIZE)));
    }

}
