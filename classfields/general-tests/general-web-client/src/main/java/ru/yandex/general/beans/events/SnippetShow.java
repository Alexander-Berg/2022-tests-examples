package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class SnippetShow {

    int page;
    int index;
    Offer offerSnippet;

    public static SnippetShow snippetShow() {
        return new SnippetShow();
    }

}
