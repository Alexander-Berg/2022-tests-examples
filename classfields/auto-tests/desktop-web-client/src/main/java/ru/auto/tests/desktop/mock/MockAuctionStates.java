package ru.auto.tests.desktop.mock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.auto.tests.desktop.mock.beans.auctionState.Context;
import ru.auto.tests.desktop.mock.beans.auctionState.State;

import static java.util.Arrays.stream;
import static ru.auto.tests.desktop.mock.beans.auctionState.AutoStrategy.autoStrategy;
import static ru.auto.tests.desktop.mock.beans.auctionState.AutoStrategySettings.settings;
import static ru.auto.tests.desktop.mock.beans.auctionState.Context.context;
import static ru.auto.tests.desktop.mock.beans.auctionState.State.state;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;

public class MockAuctionStates {

    private static final String BMW = "BMW";
    private static final String ER7 = "7ER";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject response;

    private MockAuctionStates() {
        response = new JsonObject();
    }

    public static MockAuctionStates auctionStates() {
        return new MockAuctionStates();
    }

    public MockAuctionStates setStates(State... customStates) {
        JsonArray states = new JsonArray();
        stream(customStates).forEach(state -> states.add(getJsonObject(state)));

        response.add("states", states);
        return this;
    }

    public static State getBmwStateWithAutostrategy() {
        return state().setContext(getBmw7Context())
                .setBasePrice("480000")
                .setOneStep("10000")
                .setMinBid("490000")
                .setCurrentBid("500000")
                .setAutoStrategy(autoStrategy().setAutoStrategySettings(
                        settings().setMaxBid("600000")
                                .setMaxPositionForPrice(new JsonObject())));
    }

    public static Context getBmw7Context() {
        return context().setMarkCode(BMW)
                .setMarkRu("БМВ")
                .setMarkName(BMW)
                .setModelCode(ER7)
                .setModelRu("7 серии")
                .setModelName("7 серии")
                .setRegionId("1");
    }

    public static JsonObject getBmw7ChangeAutostrategyRequest(int maxBid) {
        return getJsonObject(
                state().setContext(context().setMarkCode(BMW).setModelCode(ER7))
                        .setAutoStrategy(autoStrategy()
                                .setMaxBid(String.valueOf(maxBid * 100))
                                .setMaxPositionForPrice(new JsonObject())));
    }

    public static JsonObject getBmw7DeleteAutostrategyRequest() {
        return getJsonObject(
                state().setContext(context().setMarkCode(BMW).setModelCode(ER7)));
    }

}
