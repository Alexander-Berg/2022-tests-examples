package ru.yandex.general.beans.ajaxRequests.updateDraft;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Photo {

    String namespace;
    Integer groupId;
    String name;
    String url;
    Integer ratio;

    public static Photo photo() {
        return new Photo();
    }

}
