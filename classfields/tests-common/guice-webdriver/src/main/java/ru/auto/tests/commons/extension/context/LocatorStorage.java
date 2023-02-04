package ru.auto.tests.commons.extension.context;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class LocatorStorage {

    @Getter
    private List<StepContext> stepsList = new ArrayList<>();

}
