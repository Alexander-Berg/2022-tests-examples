package ru.yandex.realty.beans;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author kurau (Yuri Kalinin)
 */
@Getter
@Setter
public class SuggestText {

    private List<Item> response;

    @Getter
    @Setter
    public class Item {
        private String label;
        private Data data;
    }

    @Getter
    @Setter
    public class Data {
        private String type;
        private Params params;
        private String scope;
        private String changeRegion;
    }

    @Getter
    @Setter
    public class Params {
        private List<String> rgid;
        private List<String> siteId;
        private List<String> subLocality;
        private List<String> unifiedAddress;
    }
}
