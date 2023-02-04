package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class EventInfo {

    CardView cardView;
    SnippetShow snippetShow;
    SnippetClick snippetClick;
    Search search;
    PhoneCall phoneCall;
    ChatInit chatInit;
    AddOfferToFavorites addOfferToFavorites;
    CategoryChosen categoryChosen;

    public static EventInfo eventInfo() {
        return new EventInfo();
    }

}
