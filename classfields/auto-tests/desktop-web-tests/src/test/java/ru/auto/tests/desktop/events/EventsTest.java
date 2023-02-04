package ru.auto.tests.desktop.events;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.AUTORU_BILLING_SERVICE_TYPE;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.EXTENDED;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(METRICS)
@DisplayName("Проверка событий, отправляемых с фронта в events/log")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class EventsTest {

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
    @DisplayName("CARD_VIEW: Переход в карточку б/у из листинга")
    public void shouldSeeCardViewUsedCardFromListingEvent() {
        mockRule.with("desktop/SearchCarsUsed",
                "desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(CARS).path(USED).open();
        basePageSteps.onListingPage().getSale(0).click();

        waitForEvent("card_view_event", "CardViewUsedCardFromListing");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("CARD_VIEW: Переход в карточку б/у из листинга, тип листинга «Карусель»")
    public void shouldSeeCardViewUsedCardFromCarouselListingEvent() {
        mockRule.with("desktop/SearchCarsUsed",
                "desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(CARS).path(USED).addParam(OUTPUT_TYPE, CAROUSEL).open();
        basePageSteps.onListingPage().getCarouselSale(0).click();

        waitForEvent("card_view_event", "CardViewUsedCardFromListing");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("CARD_VIEW: Переход на групповую карточку из листинга новых")
    public void shouldSeeCardViewGroupFromNewListingEvent() {
        mockRule.with("desktop/SearchCarsNew",
                "desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SearchCarsBreadcrumbsMarkModelGroup",
                "desktop/SearchCarsGroupContextGroup",
                "desktop/SearchCarsGroupContextListing",
                "desktop/SearchCarsGroupComplectations",
                "desktop/ReferenceCatalogCarsComplectations",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsTechParam").post();

        urlSteps.testing().path(CARS).path(NEW).open();
        basePageSteps.onListingPage().getSale(0).click();

        waitForEvent("card_view_event", "CardViewGroupFromNewListing");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("CARD_VIEW: Переход на групповую карточку c запиненным оффером из смешанного листинга")
    public void shouldSeeCardViewGroupPinnedFromAllListingEvent() {
        mockRule.with("desktop/SearchCarsAll",
                "desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/OfferCarsNewDealer").post();

        urlSteps.testing().path(CARS).path(ALL).open();
        basePageSteps.onListingPage().getSale(0).hover().click();
        waitForEvent("card_view_event", "CardViewGroupPinnedFromAllListing");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("CARD_SHOW: Показ оффера на групповой карточке")
    public void shouldSeeCardShowGroupEvent() {
        mockRule.with("desktop/SearchCarsBreadcrumbsMarkModelGroup",
                "desktop/SearchCarsGroupContextGroupOneOffer",
                "desktop/SearchCarsGroupContextListing",
                "desktop/SearchCarsGroupComplectations",
                "desktop/EventsLog").post();

        basePageSteps.setWindowHeight(3000);
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path("/kia/optima/21342050-21342121/").open();

        waitForEvent("card_show_event", "CardShowGroup");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("PHONE_CALL: Клик по кнопке «Показать телефон» на групповой карточке c запиненным оффером")
    public void shouldSeePhoneCallGroupPinnedEvent() {
        mockRule.with("desktop/OfferCarsPhones",
                "desktop/OfferCarsNewDealer").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path("/kia/optima/21342125/21342344/1076842087-f1e84/")
                .open();
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed());

        waitForEvent("phone_call_event", "PhoneCallGroupPinned");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("PHONE_CALL: Клик по кнопке «Показать телефон» на оффере на групповой карточке")
    public void shouldSeePhoneCallGroupEvent() {
        mockRule.with("desktop/SearchCarsBreadcrumbsMarkModelGroup",
                "desktop/SearchCarsGroupContextGroup",
                "desktop/SearchCarsGroupContextListing",
                "desktop/SearchCarsGroupComplectations",
                "desktop/OfferCarsPhones",
                "desktop/ReferenceCatalogCarsComplectations",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsTechParam").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path("/kia/optima/21342050-21342121/").open();
        basePageSteps.scrollDown(500);
        waitSomething(5, TimeUnit.SECONDS);
        basePageSteps.onGroupPage().getOffer(0).showContactsButton().click();
        basePageSteps.onGroupPage().contactsPopup().waitUntil(isDisplayed());

        waitForEvent("phone_call_event", "PhoneCallGroup");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("PHONE_CALL: Клик по кнопке «Показать телефон» на карточке б/у")
    public void shouldSeePhoneCallUsedEvent() {
        mockRule.with("desktop/OfferCarsPhones",
                "desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/1076842087-f1e84/").open();
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed());

        waitForEvent("phone_call_event", "PhoneCallUsed");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("PHONE_CALL: Клик по кнопке «Показать телефон» на расширенном сниппете")
    public void shouldSeePhoneCallExtendedSnippetEvent() {
        mockRule.with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsExtended",
                "desktop/OfferCarsPhones").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam(AUTORU_BILLING_SERVICE_TYPE, EXTENDED).open();
        basePageSteps.onListingPage().getSale(0).showPhonesButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().contactsPopup().waitUntil(isDisplayed());

        waitForEvent("phone_call_event", "PhoneCallExtendedSnippet");
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("PHONE_CALL: Клик по кнопке «Показать телефон» на расширенном сниппете, тип листинга «Карусель»")
    public void shouldSeePhoneCallExtendedCarouselSnippetEvent() {
        mockRule.with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsExtended",
                "desktop/OfferCarsPhones").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam(AUTORU_BILLING_SERVICE_TYPE, EXTENDED)
                .addParam(OUTPUT_TYPE, CAROUSEL).open();

        basePageSteps.onListingPage().getCarouselSale(0).showPhonesButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().contactsPopup().waitUntil(isDisplayed());

        waitForEvent("phone_call_event", "PhoneCallExtendedSnippet");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("PHONE_CALL: Клик по кнопке «Показать телефон» в поп-апе избранного")
    public void shouldSeePhoneCallFavoritesPopupEvent() {
        mockRule.with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUser",
                "desktop/UserFavoritesAll",
                "desktop/OfferCarsPhones").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/1076842087-f1e84/").open();
        basePageSteps.onCardPage().header().favoritesButton().click();
        basePageSteps.onCardPage().favoritesPopup().getFavorite(0).button("Показать телефон").click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed());

        waitForEvent("phone_call_event", "PhoneCallFavoritesPopup");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("SEARCH_SHOW: Показ листинга")
    public void shouldSeeSearchShowEvent() {
        mockRule.with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsAll").post();

        urlSteps.testing().path(CARS).path(ALL).open();

        waitForEvent("search_show_event", "SearchShow");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("SEARCH_SHOW: Показ листинга, тип листинга «Карусель»")
    public void shouldSeeSearchShowEventCarousel() {
        mockRule.with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsAll").post();

        urlSteps.testing().path(CARS).path(ALL).addParam(OUTPUT_TYPE, CAROUSEL).open();

        waitForEvent("search_show_event", "SearchShow");
    }

    @Step("Ждём событие")
    private void waitForEvent(String event, String expectedJsonName) {
        waitSomething(5, TimeUnit.SECONDS);

        JsonObject jsonObject = new Gson().fromJson(mockRule.getMock().toString(), JsonObject.class);
        JsonArray requests = jsonObject.getAsJsonArray("requests");

        String recordedEvent = "";
        for (JsonElement request : requests) {
            JsonObject req = request.getAsJsonObject();
            if (req.get("path").getAsString().equals("/1.0/events/log") && req.get("body").getAsString().contains(event)) {
                try {
                    JsonArray recordedEvents = new Gson().fromJson(req.get("body").getAsString(), JsonObject.class)
                            .get("events").getAsJsonArray();
                    for (int i = 0; i < recordedEvents.size(); i++) {
                        if (recordedEvents.get(i).toString().contains(event)) {
                            recordedEvent = recordedEvents.get(i).getAsJsonObject().get(event).toString();
                        }
                    }
                } catch (NullPointerException e) {
                    recordedEvent = "";
                }

            }
        }

        assertThat("Событие не отправилось", recordedEvent, not(isEmptyString()));

        JsonObject expectedJsonObject = new Gson()
                .fromJson(getResourceAsString(format("events/%s.json", expectedJsonName)), JsonObject.class);
        assertThat("События не совпадают", recordedEvent, jsonEquals(expectedJsonObject));
    }

}
