package ru.auto.tests.desktop.mock;

import com.google.gson.JsonObject;
import ru.auto.tests.desktop.mock.beans.stub.Query;

import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;

public class MockBillingScheduleRequest {

    public static Query getBillingScheduleBoostQuery() {
        return query().setProduct("boost")
                .setCategory("cars")
                .setScheduleType("ONCE_AT_TIME")
                .setTime(".+")
                .setTimezone("\\+03:00")
                .setWeekdays("1");
    }

    public static JsonObject getBillingScheduleBoostBody() {
        JsonObject body = new JsonObject();
        body.addProperty("schedule_type", "ONCE_AT_TIME");
        body.addProperty("time", ".+");
        body.addProperty("timezone", "+03:00");
        return getJsonObject(body);
    }

}
