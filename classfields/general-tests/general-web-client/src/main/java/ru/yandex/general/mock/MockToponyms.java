package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.toponyms.Line;
import ru.yandex.general.beans.toponyms.Position;
import ru.yandex.general.beans.toponyms.Region;
import ru.yandex.general.beans.toponyms.SuggestItem;
import ru.yandex.general.consts.BaseConstants;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.general.beans.toponyms.Line.line;
import static ru.yandex.general.beans.toponyms.Position.position;
import static ru.yandex.general.beans.toponyms.Region.region;
import static ru.yandex.general.beans.toponyms.SuggestItem.suggestItem;
import static ru.yandex.general.consts.BaseConstants.AddressType.ADDRESS;
import static ru.yandex.general.consts.BaseConstants.AddressType.DISTRICT;
import static ru.yandex.general.consts.BaseConstants.AddressType.METRO_STATION;

public class MockToponyms {

    private static final String SUGGEST = "suggest";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject toponyms;

    private MockToponyms() {
        this.toponyms = new JsonObject();
        JsonArray suggest = new JsonArray();
        toponyms.add(SUGGEST, suggest);
    }

    public static MockToponyms mockToponyms() {
        return new MockToponyms();
    }

    @Step("Добавляем элементы саджеста топонимов")
    public MockToponyms setSuggest(SuggestItem... suggestItems) {
        asList(suggestItems).stream().forEach(suggestItem ->
                toponyms.getAsJsonArray(SUGGEST).add(new Gson().toJsonTree(suggestItem).getAsJsonObject()));
        return this;
    }

    public static SuggestItem addressPaveleckaya() {
        return suggestItem().setType(ADDRESS.getAddressType())
                .setName("Павелецкая набережная, 2")
                .setDescription("Южный административный округ, Москва")
                .setPosition(position().setLatitude("55.716022").setLongitude("37.646488"))
                .setSearchableRegion(region().setRegionId("213").setName("Москва"))
                .setSettableRegion(region().setRegionId("213"));
    }

    public static SuggestItem districtZamoskvorechye() {
        return suggestItem().setType(DISTRICT.getAddressType())
                .setName("Замоскворечье")
                .setDescription("район, Центральный административный округ, Москва")
                .setPosition(position().setLatitude("55.734157").setLongitude("37.63429"))
                .setSearchableRegion(region().setRegionId("213").setName("Москва"))
                .setSettableRegion(region().setRegionId("213"))
                .setDistrictId("117067");
    }

    public static SuggestItem subwayParkKulturi() {
        return suggestItem().setType(METRO_STATION.getAddressType())
                .setName("Парк Культуры")
                .setDescription("метро, Сокольническая линия, Центральный административный округ, Москва")
                .setPosition(position().setLatitude("55.736077").setLongitude("37.595061"))
                .setSearchableRegion(region().setRegionId("213").setName("Москва"))
                .setSettableRegion(region().setRegionId("213"))
                .setLine(line().setLineId("213_1").setName("Сокольническая линия").setColor("#e4402d"))
                .setDistrictId("20490");
    }

    public String build() {
        return toponyms.toString();
    }

}
