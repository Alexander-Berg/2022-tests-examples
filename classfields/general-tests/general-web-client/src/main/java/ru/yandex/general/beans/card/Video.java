package ru.yandex.general.beans.card;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.ajaxRequests.updateDraft.Form;

@Setter
@Getter
@Accessors(chain = true)
public class Video {

    String url;

    public static Video video() {
        return new Video();
    }

}
