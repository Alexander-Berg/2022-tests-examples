package ru.yandex.general.beans.toponyms;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.statistics.GraphItem;
import ru.yandex.general.consts.BaseConstants;

@Getter
@Setter
@Accessors(chain = true)
public class SuggestItem {

    String type;
    String name;
    String description;
    Position position;
    Region searchableRegion;
    Region settableRegion;
    Line line;
    String stationId;
    String districtId;

    public static SuggestItem suggestItem() {
        return new SuggestItem();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
