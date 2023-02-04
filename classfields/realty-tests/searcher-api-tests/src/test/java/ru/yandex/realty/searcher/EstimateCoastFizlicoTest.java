package ru.yandex.realty.searcher;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.test.api.realty.ApiSearcher;
import ru.auto.test.api.realty.RentOfferPlacementPeriod;
import ru.auto.test.api.realty.VosOfferCategory;
import ru.auto.test.api.realty.VosOfferType;
import ru.auto.test.api.realty.estimatecost.json.responses.Datum;
import ru.auto.test.api.realty.estimatecost.json.responses.EstimateCost;
import ru.auto.test.api.realty.estimatecost.json.responses.PremiumCampaignAssert;
import ru.auto.test.api.realty.estimatecost.json.responses.PromotionCampaignAssert;
import ru.auto.test.api.realty.estimatecost.json.responses.RaisingCampaignAssert;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.module.RealtyApiModule;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.yandex.realty.PricesSet.FIZLICO;
import static ru.yandex.realty.PricesSet.getRegionPricesFor;

/**
 * @author kurau (Yuri Kalinin)
 */
@DisplayName("Физлицо. Все категории")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class EstimateCoastFizlicoTest {

    private static final String NATURAL_PERSON = "NATURAL_PERSON";

    private Datum firstDatum;

    @Inject
    private ApiSearcher searcher;

    //@Parameter("rgid")
    @Parameterized.Parameter
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

    //@Parameter("rising")
    @Parameterized.Parameter(4)
    public String rising;

    //@Parameter("premium")
    @Parameterized.Parameter(5)
    public String premium;

    //@Parameter("promotion")
    @Parameterized.Parameter(6)
    public String promotion;


    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0} {1} {2} {3} {4} {5} {6}")
    public static Collection<Object[]> getParameters() {
        return getRegionPricesFor(FIZLICO);
    }


    @Before
    public void before() {
        List<Datum> datum = searcher.estimateCostjson().withRgid(rgid)
                .withCategory(category.value())
                .withType(type.value())
                .withPricingPeriod(pricePeriod.value())
                .withPaymentType(NATURAL_PERSON)
                .get(validatedWith(shouldBe200OkJSON()))
                .as(EstimateCost.class).getData();
        assertThat("Ожидали один набор данных о ценах в ответе (data[] не должно быть пустым)", datum, hasSize(1));
        firstDatum = datum.get(0);
    }

    @Test
    public void shouldSeeRaisingPrice() {
        RaisingCampaignAssert.assertThat(firstDatum.getRaisingCampaign())
                .hasPrice(getPrice(rising)).hasPeriod(1L);
    }

    @Test
    public void shouldSeePremiumPrice() {
        PremiumCampaignAssert.assertThat(firstDatum.getPremiumCampaign()).
                hasPrice(getPrice(premium)).hasPeriod(7L);
    }

    @Test
    public void shouldSeePromotionPrice() {
        PromotionCampaignAssert.assertThat(firstDatum.getPromotionCampaign())
                .hasPrice(getPrice(promotion)).hasPeriod(30L);
    }

    private Long getPrice(String price) {
        return Long.valueOf(price);
    }

}
