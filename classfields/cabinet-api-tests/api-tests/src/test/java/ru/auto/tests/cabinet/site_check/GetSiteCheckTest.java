package ru.auto.tests.cabinet.site_check;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonArray;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.model.DealerSiteCheckRecord;
import ru.auto.tests.cabinet.module.CabinetApiModule;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;


@DisplayName("GET /site-check")
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(CabinetApiModule.class)
public class GetSiteCheckTest {

    private static final String DEALER_ID = "20101";
    private static final String USER_ID = "11913489";
    private static final String COMMENT = "comment";
    private static final String TICKET = "ticket";
    private static final String DATE_NOW = DateTime.now().toLocalDate().toString();
    private static final String YESTERDAY = DateTime.now().minusDays(1).toLocalDate().toString();

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private CabinetApiAdaptor adaptor;


    @Parameter("Резолюция")
    @Parameterized.Parameter
    public Boolean resolution;

    @Parameterized.Parameters(name = "[{index}] {0}")
    public static List<Boolean> getResolution() {
        return newArrayList(true, false);
    }

    @Before
    @Description("Очищаем и добавляем site-check дилеру")
    public void clearAddSiteCheck() {
        adaptor.clearSiteCheck(DEALER_ID);
        adaptor.addSiteCheckForDealer(DEALER_ID, USER_ID, COMMENT, TICKET, resolution);
    }

    @Test
    public void shouldGetAllResolution() {
        List<DealerSiteCheckRecord> response = api.siteCheck().getSiteCheck().clientIdQuery(DEALER_ID)
                .fromQuery(YESTERDAY).toQuery(DATE_NOW)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.get(0).getResolution()).isEqualTo(resolution);
        assertThat(response.get(0).getComment()).isEqualTo(COMMENT);
        assertThat(response.get(0).getTicket()).isEqualTo(TICKET);
    }

    @Test
    public void shouldGetResolution() {
        List<DealerSiteCheckRecord> response = api.siteCheck().getSiteCheck().clientIdQuery(DEALER_ID)
                .fromQuery(YESTERDAY).toQuery(DATE_NOW).resolutionQuery(resolution)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.get(0).getResolution()).isEqualTo(resolution);
        assertThat(response.get(0).getComment()).isEqualTo(COMMENT);
        assertThat(response.get(0).getTicket()).isEqualTo(TICKET);
    }

    @Test
    public void shouldGetAllResolutionHasNoDiffWithProduction() {
        Function<ApiClient, JsonArray> request = apiClient -> apiClient.siteCheck().getSiteCheck().clientIdQuery(DEALER_ID)
                .fromQuery(YESTERDAY).toQuery(DATE_NOW)
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonArray.class);
        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}