package ru.auto.tests.api.beans;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Accessors(chain = true)
@Getter
public class AutoruBreadcrumbsDataList {

    private List<AutoruBreadcrumbsListItem> data;
}