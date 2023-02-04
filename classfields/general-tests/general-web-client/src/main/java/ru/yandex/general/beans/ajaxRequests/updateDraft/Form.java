package ru.yandex.general.beans.ajaxRequests.updateDraft;

import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.card.Video;
import ru.yandex.general.utils.NullStringTypeAdapter;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class Form {

    @JsonAdapter(NullStringTypeAdapter.class)
    String categoryId;
    @JsonAdapter(NullStringTypeAdapter.class)
    String categoryPreset;
    @JsonAdapter(NullStringTypeAdapter.class)
    String title;
    @JsonAdapter(NullStringTypeAdapter.class)
    String description;
    @JsonAdapter(NullStringTypeAdapter.class)
    String condition;
    List<Address> addresses;
    Contacts contacts;
    Video video;
    List<Attribute> attributes;
    int currentControlNum;
    List<Photo> photos;
    Price price;


    public static Form form() {
        return new Form();
    }

}
