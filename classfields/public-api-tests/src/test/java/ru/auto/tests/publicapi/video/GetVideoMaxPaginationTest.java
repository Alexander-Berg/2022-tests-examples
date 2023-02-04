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
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("GET /video/search")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetVideoMaxPaginationTest {

    private static final int MAX_PAGE_NUMBER = 4;
    private static final int MAX_PAGE_SIZE = 40;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_PAGE_NUMBER = 1;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter("Номер страницы")
    @Parameterized.Parameter(0)
    public int pageNumber;

    @Parameter("Число видео")
    @Parameterized.Parameter(1)
    public int pageSize;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static List<Object[]> getParameters() {
        return Arrays.asList(provideMaxPagination());
    }

    private static Object[][] provideMaxPagination() {
        return new Object[][]{
                {MAX_PAGE_NUMBER + 1, DEFAULT_PAGE_SIZE},
                {DEFAULT_PAGE_NUMBER, MAX_PAGE_SIZE + 1}
        };
    }

    @Test
    public void shouldSee200WithMaxVideoCount() {
        api.video().searchVideo().categoryPath(CARS.name()).pageQuery(pageNumber).pageSizeQuery(pageSize)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }
}
