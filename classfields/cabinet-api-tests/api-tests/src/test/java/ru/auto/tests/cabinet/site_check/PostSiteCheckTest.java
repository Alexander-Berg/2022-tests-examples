package ru.auto.tests.cabinet.site_check;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.model.DealerSiteCheckEntity;
import ru.auto.tests.cabinet.model.DealerSiteCheckRecord;
import ru.auto.tests.cabinet.module.CabinetApiModule;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.time.OffsetDateTime;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /site-check/client/{client_id}")
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(CabinetApiModule.class)
public class PostSiteCheckTest {

    private static final String DEALER_ID = "20101";
    private static final String USER_ID = "11913489";
    private static final String COMMENT = "comment";
    private static final String TICKET = "ticket";
    private static final String DATE_NOW = DateTime.now().toLocalDate().toString();
    private static final String YESTERDAY = DateTime.now().minusDays(1).toLocalDate().toString();
    private static final OffsetDateTime TIME = OffsetDateTime.now();

    @Inject
    private ApiClient api;

    @Inject
    private CabinetApiAdaptor adaptor;


    @Parameter("Резолюция")
    @Parameterized.Parameter
    public Boolean resolution;

    @Parameterized.Parameters(name = "[{index}] {0}")
    public static List<Boolean> getResolution() {
        return newArrayList(true, false);
    }

    @Test
    public void shouldGetResolution() {
        adaptor.clearSiteCheck(DEALER_ID);
        api.siteCheck().addOrUpdateSiteCheck().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader(USER_ID)
                .reqSpec(defaultSpec()).body(new DealerSiteCheckEntity().checkDate(TIME)
                .resolution(resolution).comment(COMMENT).ticket(TICKET))
                .execute(validatedWith(shouldBeCode(SC_OK)));

        List<DealerSiteCheckRecord> response = api.siteCheck().getSiteCheck().clientIdQuery(DEALER_ID)
                .fromQuery(YESTERDAY).toQuery(DATE_NOW)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.get(0).getResolution()).isEqualTo(resolution);
        assertThat(response.get(0).getComment()).isEqualTo(COMMENT);
        assertThat(response.get(0).getTicket()).isEqualTo(TICKET);
    }
}