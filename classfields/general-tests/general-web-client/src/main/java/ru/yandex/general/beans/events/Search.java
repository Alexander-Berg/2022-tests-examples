package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class Search {

    List<OfferCountByCategory> offerCountByCategory;
    Page page;
    SearchArea searchArea;
    String searchRequestId;
    String searchText;
    String searchUrl;
    String sorting;

    public static Search search() {
        return new Search();
    }

}
