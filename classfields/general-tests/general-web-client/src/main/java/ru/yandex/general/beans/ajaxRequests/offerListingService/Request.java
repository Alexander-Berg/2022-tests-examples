package ru.yandex.general.beans.ajaxRequests.offerListingService;

import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.utils.NullStringTypeAdapter;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class Request {

    @JsonAdapter(NullStringTypeAdapter.class)
    String categoryId;
    String text;
    List<String> lockedFields;
    List<Parameter> parameters;
    Area area;


    public static Request request() {
        return new Request();
    }

}
