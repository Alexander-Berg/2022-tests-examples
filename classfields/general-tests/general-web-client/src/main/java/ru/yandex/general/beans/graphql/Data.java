package ru.yandex.general.beans.graphql;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import ru.yandex.general.beans.card.Card;

@Setter
@Getter
public class Data {

    Card card;
    JsonObject search;
    JsonObject cabinetListing;
    JsonObject favorites;
    JsonObject userById;

}
