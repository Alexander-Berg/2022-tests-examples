package ru.auto.tests.cabinet.offer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.*;
import static ru.auto.tests.cabinet.ra.ResponseSpecBuilders.shouldBeCodeAndPDF;


@DisplayName("GET /offer/{offer_id}/{category}/price-pdf")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class GetOfferPricePdfTest {

    @Inject
    private ApiClient api;

    @Test
    public void shouldGetPriceCarsPdfStatusOk() {
        api.offer().getPricePdf().offerIdPath("1094333022-9bf9589f").categoryPath("cars")
                .execute(validatedWith(shouldBeCodeAndPDF(SC_OK)));
    }

    @Test
    public void shouldGetPriceCarsPdfStatusNotFound() {
        api.offer().getPricePdf().offerIdPath("10890386").categoryPath("cars")
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldGetPriceMotoPdfStatusOk() {
        api.offer().getPricePdf().offerIdPath("3304690-a90a48d6").categoryPath("moto")
                .execute(validatedWith(shouldBeCodeAndPDF(SC_OK)));
    }

    @Test
    public void shouldGetPriceTrucksPdfStatusNotFound() {
        api.offer().getPricePdf().offerIdPath("16195790-5c4db946").categoryPath("trucks")
                .execute(validatedWith(shouldBeCodeAndPDF(SC_OK)));
    }

    @Test
    public void shouldGetPriceCarsPdfStatusNotFoundOfferCars() {
        api.offer().getPricePdf().offerIdPath("3210916-37f8ddc3").categoryPath("cars")
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }
}