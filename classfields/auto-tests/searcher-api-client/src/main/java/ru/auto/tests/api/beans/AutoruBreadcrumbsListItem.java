package ru.auto.tests.api.beans;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Getter
public class AutoruBreadcrumbsListItem {

    private String id;
    private String name;
}