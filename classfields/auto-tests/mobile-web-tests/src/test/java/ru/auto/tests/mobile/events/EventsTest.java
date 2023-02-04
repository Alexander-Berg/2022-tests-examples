package ru.auto.tests.mobile.events;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static net.javacrumbs.jsonunit.core.Option.TREATING_NULL_AS_ABSENT;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;

@Feature(METRICS)
@DisplayName("Проверка событий, отправляемых с фронта в events/log")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class EventsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.newMock().setRecordRequests(true);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("CARD_VIEW: Открытие карточки б/у легковых")
    public void shouldSeeCardViewUsedCarsEvent() {
        mockRule.with("desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        waitForEvent("card_view_event", "CardViewUsedCars");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("CARD_VIEW: Открытие карточки б/у комтранса")
    public void shouldSeeCardViewUsedTrucksEvent() {
        mockRule.with("desktop/OfferTrucksUsedUser").post();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID).open();
        waitForEvent("card_view_event", "CardViewUsedTrucks");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("CARD_VIEW: Открытие карточки б/у мото")
    public void shouldSeeCardViewUsedMotoEvent() {
        mockRule.with("desktop/OfferMotoUsedUser").post();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();
        waitForEvent("card_view_event", "CardViewUsedMoto");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("PHONE_CALL: Просмотр телефонов на карточке б/у легковых")
    public void shouldSeePhoneCallUsedCarsEvent() {
        mockRule.with("desktop/OfferCarsUsedUser",
                "desktop/OfferCarsPhones").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().floatingContacts().callButton().click();
        waitForEvent("phone_call_event", "PhoneCallUsedCars");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("PHONE_CALL: Просмотр телефонов на карточке б/у комтранса")
    public void shouldSeePhoneCallUsedTrucksEvent() {
        mockRule.with("desktop/OfferTrucksUsedUser",
                "desktop/OfferTrucksPhones").post();

        urlSteps.testing().path(TRUCKS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().floatingContacts().callButton().click();
        waitForEvent("phone_call_event", "PhoneCallUsedTrucks");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("PHONE_CALL: Просмотр телефонов на карточке б/у мото")
    public void shouldSeePhoneCallUsedMotoEvent() {
        mockRule.with("desktop/OfferMotoUsedUser",
                "desktop/OfferMotoPhones").post();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().floatingContacts().callButton().click();
        waitForEvent("phone_call_event", "PhoneCallUsedMoto");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("SEARCH_SHOW: Показ листинга")
    public void shouldSeeSearchShowEvent() {
        mockRule.with("desktop/SearchCarsBreadcrumbsRid213",
                "mobile/SearchCarsAll").post();

        urlSteps.testing().path(CARS).path(ALL).open();
        waitForEvent("search_show_event", "SearchShow");
    }

    @Step("Ждём событие")
    private void waitForEvent(String event, String expectedJsonName) {
        waitSomething(5, TimeUnit.SECONDS);

        JsonObject jsonObject = new Gson().fromJson(mockRule.getMock().toString(), JsonObject.class);
        JsonArray requests = jsonObject.getAsJsonArray("requests");

        String body = "";
        for (JsonElement request : requests) {
            JsonObject req = request.getAsJsonObject();
            if (req.get("path").getAsString().equals("/1.0/events/log") && req.get("body").getAsString().contains(event)) {
                body = req.get("body").getAsString();
            }
        }

        assertThat("Событие не отправилось", body, not(isEmptyString()));

        JsonObject expectedJsonObject = new Gson()
                .fromJson(getResourceAsString(format("events/%s.json", expectedJsonName)), JsonObject.class);
        assertThat("События не совпадают", body, jsonEquals(new Gson().toJson(expectedJsonObject))
                .when(TREATING_NULL_AS_ABSENT));
    }
}
