package ru.yandex.general.beans.ajaxRequests;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class AddToFavorites {

    List<String> offerIds;

    public static AddToFavorites addToFavorites() {
        return new AddToFavorites();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
