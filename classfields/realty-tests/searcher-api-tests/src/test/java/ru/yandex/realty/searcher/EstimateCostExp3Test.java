package ru.yandex.realty.searcher;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.test.api.realty.ApiSearcher;
import ru.auto.test.api.realty.RentOfferPlacementPeriod;
import ru.auto.test.api.realty.VosOfferCategory;
import ru.auto.test.api.realty.VosOfferType;
import ru.auto.test.api.realty.estimatecost.json.responses.Datum;
import ru.auto.test.api.realty.estimatecost.json.responses.DatumAssert;
import ru.auto.test.api.realty.estimatecost.json.responses.EstimateCost;
import ru.auto.test.api.realty.estimatecost.json.responses.PremiumCampaign;
import ru.auto.test.api.realty.estimatecost.json.responses.RaisingCampaign;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.module.RealtyApiModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.yandex.realty.PricesSet.getExp1HightRegionParams;
import static ru.yandex.realty.PricesSet.getExp1LowRegionParams;
import static ru.yandex.realty.PricesSet.getExp1MSKParams;
import static ru.yandex.realty.PricesSet.getExp1OtherRegionParams;
import static ru.yandex.realty.PricesSet.getExp1SPBParams;

/**
 * Created by vicdev on 05.07.17.
 */
@DisplayName("Тестирование цен в зависимости от региона. Эксперимент 3")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@Issue("REALTY-12155")
@Ignore
public class EstimateCostExp3Test {


    private static String BUCKET_15 = "15";

    @Inject
    private ApiSearcher searcher;

    //@Parameter("rgid")
    @Parameterized.Parameter(0)
    public String rgid;

    //@Parameter("category")
    @Parameterized.Parameter(1)
    public VosOfferCategory category;

    //@Parameter("type")
    @Parameterized.Parameter(2)
    public VosOfferType type;

    //@Parameter("pricePeriod")
    @Parameterized.Parameter(3)
    public RentOfferPlacementPeriod pricePeriod;

    //@Parameter("raising")
    @Parameterized.Parameter(4)
    public long expectedRaising;

    //@Parameter("premium")
    @Parameterized.Parameter(5)
    public long expectedPricePremium;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0} {1} {2} {3} {4} {5}")
    public static Collection<Object[]> getParameters() throws IOException {
        Collection<Object[]> res = new ArrayList<Object[]>();
        res.addAll(getExp1MSKParams());
        res.addAll(getExp1SPBParams());
        res.addAll(getExp1LowRegionParams());
        res.addAll(getExp1HightRegionParams());
        res.addAll(getExp1OtherRegionParams());
        return res;
    }

    @Test
    public void shouldSeeRaisingAndPremiumPriceExp3() {
        List<Datum> datum = searcher.estimateCostjson().withRgid(rgid)
                .withCategory(category.value()).withType(type.value()).withBucket(BUCKET_15).withPricingPeriod(pricePeriod.value()).get(validatedWith(shouldBe200OkJSON())).as(EstimateCost.class).getData();
        assertThat("data[] не должно быть пустым", datum, hasSize(greaterThan(0)));
        DatumAssert.assertThat(datum.get(0))
                .hasRaisingCampaign(new RaisingCampaign().withPrice(expectedRaising))
                .hasPremiumCampaign(new PremiumCampaign().withPrice(expectedPricePremium));
    }
}
