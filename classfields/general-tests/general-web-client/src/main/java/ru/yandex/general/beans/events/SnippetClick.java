package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class SnippetClick {

    int page;
    int index;
    Offer offerSnippet;

    public static SnippetClick snippetClick() {
        return new SnippetClick();
    }

}
