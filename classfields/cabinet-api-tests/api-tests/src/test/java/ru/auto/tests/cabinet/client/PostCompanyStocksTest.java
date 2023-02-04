package ru.auto.tests.cabinet.client;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.model.DealerStock;
import ru.auto.tests.cabinet.model.DealerStocks;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.util.Lists.newArrayList;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.model.DealerStock.StockCategoryEnum.*;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /client/{client_id}/stocks")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class PostCompanyStocksTest {

    private static final String COMPANY_ID = "19";
    private static final Integer AMOUNT = 555;
    private static final Boolean RESOLUTION = true;

    @Inject
    private ApiClient api;

    @Test
    public void shouldGetListCompanyStocks() {
        String userId = "19565983";
        api.client().saveCompanyStocks().reqSpec(defaultSpec()).xAutoruOperatorUidHeader(userId).companyIdPath(COMPANY_ID)
                .body(new DealerStocks().stocks(newArrayList(
                        new DealerStock().stockAmount(AMOUNT).stockCategory(CARS_NEW).fullStock(RESOLUTION),
                        new DealerStock().stockAmount(AMOUNT).stockCategory(CARS_USED).fullStock(RESOLUTION),
                        new DealerStock().stockAmount(AMOUNT).stockCategory(COMMERCIAL).fullStock(RESOLUTION),
                        new DealerStock().stockAmount(AMOUNT).stockCategory(MOTO).fullStock(RESOLUTION))))
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetStatusForbidden() {
        String userId = "1956598";
        api.client().saveCompanyStocks().reqSpec(defaultSpec()).xAutoruOperatorUidHeader(userId).companyIdPath(COMPANY_ID)
                .body(new DealerStocks().stocks(newArrayList(
                        new DealerStock().stockAmount(AMOUNT).stockCategory(CARS_NEW).fullStock(RESOLUTION),
                        new DealerStock().stockAmount(AMOUNT).stockCategory(CARS_USED).fullStock(RESOLUTION),
                        new DealerStock().stockAmount(AMOUNT).stockCategory(COMMERCIAL).fullStock(RESOLUTION),
                        new DealerStock().stockAmount(AMOUNT).stockCategory(MOTO).fullStock(RESOLUTION))))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}