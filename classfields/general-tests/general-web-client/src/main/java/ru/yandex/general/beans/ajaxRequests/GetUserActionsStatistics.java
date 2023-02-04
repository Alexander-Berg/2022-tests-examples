package ru.yandex.general.beans.ajaxRequests;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class GetUserActionsStatistics {

    String fromDate;
    String toDate;

    public static GetUserActionsStatistics getUserActionsStatistics() {
        return new GetUserActionsStatistics();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
