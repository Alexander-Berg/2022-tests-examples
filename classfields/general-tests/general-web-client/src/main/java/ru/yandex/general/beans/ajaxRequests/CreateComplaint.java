package ru.yandex.general.beans.ajaxRequests;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class CreateComplaint {

    String reason;
    String offerId;
    String text;
    String placement;
    String application;

    public static CreateComplaint createComplaint() {
        return new CreateComplaint();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
