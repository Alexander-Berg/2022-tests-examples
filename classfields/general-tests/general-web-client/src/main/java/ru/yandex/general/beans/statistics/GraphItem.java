package ru.yandex.general.beans.statistics;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class GraphItem {

    int counterValue;
    String date;

    public static GraphItem graphItem() {
        return new GraphItem();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
