package ru.auto.tests.desktop.mock.beans.promoCampaign;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class CatalogFilter {

    String mark;
    String model;
    String generation;

    public static CatalogFilter catalogFilter() {
        return new CatalogFilter();
    }

}
