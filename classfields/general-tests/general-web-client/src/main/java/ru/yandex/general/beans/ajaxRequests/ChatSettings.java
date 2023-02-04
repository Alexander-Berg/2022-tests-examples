package ru.yandex.general.beans.ajaxRequests;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import static java.util.Arrays.asList;

@Setter
@Getter
@Accessors(chain = true)
public class ChatSettings {

    List<String> chatSettings;

    public static ChatSettings chatSettings(String... settings) {
        return new ChatSettings().setChatSettings(asList(settings));
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
