package ru.auto.tests.desktop.step;

import io.qameta.allure.Step;
import lombok.Getter;
import org.hamcrest.Matcher;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.auto.tests.desktop.DesktopConfig;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Created by vicdev on 28.08.17.
 * Example:
 */
public class RandomOfferSteps extends WebDriverSteps {

    private OfferType currentType;
    private Map<String, Object> params;
    private Matcher offerSearchMatcher = null;
    private String state = "used";

    @Inject
    private SearcherUserSteps searcherSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private DesktopConfig config;

    @Getter
    private UriBuilder uriBuilder;

    public RandomOfferSteps newRandomOffer() {
        uriBuilder = UriBuilder.fromUri(config.getTestingURI());
        params = getParams();
        return this;
    }

    public RandomOfferSteps state(String state) {
        this.state = state;
        params.replace("state", state.toUpperCase());
        return this;
    }

    @Step("Добавляем матчер для проверки оффера")
    public RandomOfferSteps withMatcher(Matcher matcher) {
        this.offerSearchMatcher = matcher;
        return this;
    }

    public RandomOfferSteps open() {
        urlSteps.fromUri(uriBuilder.toString()).open();
        return this;
    }

    public RandomOfferSteps addParam(String key, Object value) {
        params.put(key, value);
        return this;
    }


    private static Map getParams() {
        return new HashMap() {{
            put("only-offers", true);
            put("page_num_offers", 1);
            put("page_size", 80);
            put("rid", "213");
            put("offers_offset", 0);
            put("state", "USED");
            long oneDayAgoTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
            put("creation_date_to", oneDayAgoTimestamp);
        }};
    }

    public enum OfferType {
        CAR("cars", "search", "", "offer"),
        MOTO("motorcycle", "moto", "motorcycle", "motoOffer"),
        SCOOTER("scooters", "moto", "scooters", "motoOffer"),
        ATV("atv", "moto", "atv", "motoOffer"),
        SNOWMOBILE("snowmobile", "moto", "snowmobile", "motoOffer"),
        LIGHT_TRUCK("lcv", "trucks", "LCV", "truckOffer"),
        TRUCK("truck", "trucks", "TRUCK", "truckOffer"),
        ARTIC("artic", "trucks", "ARTIC", "truckOffer"),
        BUS("bus", "trucks", "BUS", "truckOffer"),
        TRAILER("trailer", "trucks", "ARTIC", "truckOffer"),
        AGRICULTURAL("agricultural", "trucks", "AGRICULTURAL", "truckOffer"),
        CONSTRUCTION("construction", "trucks", "CONSTRUCTION", "truckOffer"),
        AUTOLOADER("autoloader", "trucks", "AUTOLOADER", "truckOffer"),
        CRANE("crane", "trucks", "CRANE", "truckOffer"),
        DREDGE("dredge", "trucks", "DREDGE", "truckOffer"),
        BULLDOZER("bulldozers", "trucks", "BULLDOZERS", "truckOffer"),
        CRANE_HYDRAULICS("crane_hydraulics", "trucks", "CRANE_HYDRAULICS", "truckOffer"),
        MUNICIPAL("municipal", "trucks", "MUNICIPAL", "truckOffer");

        @Getter
        private String name;
        @Getter
        private String searchType;
        @Getter
        private String category;
        @Getter
        private String offerType;

        OfferType(String name, String searchType, String category, String offerType) {
            this.name = name;
            this.searchType = searchType;
            this.category = category;
            this.offerType = offerType;
        }
    }
}