package ru.auto.tests.cabinet.loyalty;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.model.UpdateResultExt;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;

@DisplayName("POST /loyalty/report/update-cashback-parameters")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class PostUpdateCashbackTest {

    @Inject
    private CabinetApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Test
    public void shouldGetEditedParametersLoyalty() {
        String dealerId = "21397";
        DateFormat format = new SimpleDateFormat("yyyy-MM-01");
        String date = format.format(new Date());
        adaptor.deleteLoyaltyReports(dealerId);
        adaptor.addLoyaltyReports(dealerId);
        UpdateResultExt response = api.loyalty().updateCashbackParameters().clientIdQuery(dealerId).cashbackAmountQuery(30l)
                .cashbackAppliedQuery(true).reportDateQuery(date).executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getRowCount().equals("1"));
    }
}