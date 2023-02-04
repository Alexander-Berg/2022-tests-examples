package ru.auto.tests.cabinet.site_check;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;


@DisplayName("GET /site-check")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class GetSiteCheckStatusTest {

    private static final String DEALER_ID = "20101";
    private static final String MANAGER_ID = "19565983";
    private static final String COMMENT = "comment";
    private static final String TICKET = "ticket";
    private static final Boolean RESOLUTION = true;
    private static final String DATE_NOW = DateTime.now().toLocalDate().toString();
    private static final String YESTERDAY = DateTime.now().minusDays(1).toLocalDate().toString();

    @Inject
    private ApiClient api;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Before
    @Description("Очищаем и добавляем site-check дилеру")
    public void clearAddSiteCheck() {
        adaptor.clearSiteCheck(DEALER_ID);
        adaptor.addSiteCheckForDealer(DEALER_ID, MANAGER_ID, COMMENT, TICKET, RESOLUTION);
    }

    @Test
    public void shouldGetStatusOkForManager() {
        api.siteCheck().getSiteCheck().clientIdQuery(DEALER_ID).fromQuery(YESTERDAY).toQuery(DATE_NOW)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
    }
}