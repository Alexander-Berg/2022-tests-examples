package ru.auto.tests.commons.bean;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class LocatorBean {

    private String fullPath;
    private String page;
    private String test;
    private List<String> locatorChain = new LinkedList<>();
    private String action;
    private String stepDescription;

}
